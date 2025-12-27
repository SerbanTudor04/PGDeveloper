package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;
import ro.fintechpro.core.service.WorkspaceService;
import ro.fintechpro.core.spi.SidebarPlugin;
import ro.fintechpro.ui.ide.DockLayout;
import ro.fintechpro.ui.ide.ResultTableBuilder;
import ro.fintechpro.ui.ide.SidebarView;
import ro.fintechpro.ui.ide.SqlConsoleTab;
import ro.fintechpro.ui.plugins.FunctionPlugin;
import ro.fintechpro.ui.plugins.TablePlugin;

import java.util.ArrayList;
import java.util.List;

public class MainIdeView {

    // Services
    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final MetadataService metaService = new MetadataService(dbManager);
    private final QueryExecutor queryExecutor = new QueryExecutor();
    private final LocalIndexService indexService = new LocalIndexService();
    private final WorkspaceService workspaceService = new WorkspaceService();

    // UI Components
    private final List<SidebarPlugin> plugins = List.of(new TablePlugin(), new FunctionPlugin());
    private final SidebarView sidebar = new SidebarView(plugins);

    // REPLACED: Single CodeArea -> TabPane for multiple consoles
    private TabPane editorTabPane;

    private final TableView<List<Object>> resultsTable = new TableView<>();
    private final TextArea messageConsole = new TextArea();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Ready");

    // Docking System
    private DockLayout dockLayout;

    public Parent getView() {
        // 1. SETUP EDITOR AREA (Consoles)
        editorTabPane = new TabPane();
        editorTabPane.getStyleClass().add(Styles.DENSE);

        // LOAD SAVED CONSOLES
        List<WorkspaceService.ConsoleState> savedStates = workspaceService.loadState();
        if (savedStates.isEmpty()) {
            addNewConsole(); // Default if empty
        } else {
            for (var state : savedStates) {
                addConsoleTab(state);
            }
        }

        // AUTO-SAVE Setup
        setupAutoSave();

        // 2. INITIALIZE DOCKING SYSTEM
        // We pass the editorTabPane as the central component
        dockLayout = new DockLayout(editorTabPane);

        // Load CSS if you created it
        if (getClass().getResource("/dock-layout.css") != null) {
            dockLayout.getStylesheets().add(getClass().getResource("/dock-layout.css").toExternalForm());
        }

        // 3. DOCK COMPONENTS
        dockLayout.dock(sidebar, "Explorer", DockLayout.Location.LEFT);
        dockLayout.dock(resultsTable, "Query Results", DockLayout.Location.BOTTOM);
        dockLayout.dock(messageConsole, "Console", DockLayout.Location.BOTTOM);

        // 4. SETUP MENU BAR
        MenuBar menuBar = createMenuBar();

        // 5. ROOT LAYOUT
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(dockLayout);
        root.setBottom(createStatusBar());

        // Wire Logic
        sidebar.setOnRefresh(this::runIntrospection);
        runIntrospection(null);

        return root;
    }

    // --- CONSOLE MANAGEMENT ---

    private void addNewConsole() {
        var state = workspaceService.createNewConsole("postgres");
        addConsoleTab(state);
    }

    private void addConsoleTab(WorkspaceService.ConsoleState state) {
        // We pass 'this::executeQuery' as the callback so the tab can run queries
        SqlConsoleTab tab = new SqlConsoleTab(state, this::executeQuery);
        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
    }

    private void saveWorkspace() {
        List<WorkspaceService.ConsoleState> states = new ArrayList<>();
        for (Tab t : editorTabPane.getTabs()) {
            if (t instanceof SqlConsoleTab sqlTab) {
                states.add(sqlTab.toState());
                // --- FIX IS HERE: use getSqlContent() ---
                workspaceService.saveConsoleContent(sqlTab.getConsoleId(), sqlTab.getSqlContent());
            }
        }
        workspaceService.saveState(states);
        Platform.runLater(() -> statusLabel.setText("Workspace saved."));
    }

    private void setupAutoSave() {
        // Save on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveWorkspace));

        // Save every 30 seconds
        Thread saveThread = new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(30000);
                    Platform.runLater(this::saveWorkspace);
                } catch (InterruptedException e) { break; }
            }
        });
        saveThread.setDaemon(true);
        saveThread.start();
    }

    // --- UI HELPERS ---

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem newConsole = new MenuItem("New Console", new FontIcon(Feather.PLUS));
        newConsole.setOnAction(e -> addNewConsole());
        MenuItem saveItem = new MenuItem("Save Workspace", new FontIcon(Feather.SAVE));
        saveItem.setOnAction(e -> saveWorkspace());
        fileMenu.getItems().addAll(newConsole, saveItem);

        // View Menu
        Menu viewMenu = new Menu("View");
        MenuItem openExplorer = new MenuItem("Database Explorer");
        openExplorer.setOnAction(e -> dockLayout.dock(sidebar, "Explorer", DockLayout.Location.LEFT));
        MenuItem openResults = new MenuItem("Query Results");
        openResults.setOnAction(e -> dockLayout.dock(resultsTable, "Query Results", DockLayout.Location.BOTTOM));
        viewMenu.getItems().addAll(openExplorer, openResults);

        menuBar.getMenus().addAll(fileMenu, viewMenu);
        return menuBar;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(10, statusLabel, progressBar);
        bar.setPadding(new Insets(3));
        bar.setStyle("-fx-font-size: 11px; -fx-background-color: -color-bg-subtle;");
        bar.setAlignment(Pos.CENTER_LEFT);
        progressBar.setVisible(false);
        return bar;
    }

    // --- LOGIC (Execution) ---

    // Note: This method now accepts SQL string directly (passed from the active Tab)
    private void executeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) return;

        statusLabel.setText("Executing...");
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                var result = queryExecutor.execute(sql);
                Platform.runLater(() -> {
                    // Show results dock
                    dockLayout.dock(result.isResultSet() ? resultsTable : messageConsole,
                            result.isResultSet() ? "Query Results" : "Console",
                            DockLayout.Location.BOTTOM);

                    if (result.isResultSet()) {
                        ResultTableBuilder.populate(resultsTable, result);
                    } else {
                        messageConsole.setText(result.message());
                    }
                    statusLabel.setText("Done.");
                    progressBar.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dockLayout.dock(messageConsole, "Console", DockLayout.Location.BOTTOM);
                    messageConsole.setText("Error: " + e.getMessage());
                    statusLabel.setText("Failed.");
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }

    private void runIntrospection(Runnable onComplete) {
        if (onComplete == null) {
            progressBar.setVisible(true);
            statusLabel.setText("Indexing...");
        }

        new Thread(() -> {
            try {
                indexService.clearIndex();
                List<LocalIndexService.SearchResult> batch = new ArrayList<>();
                for (String s : metaService.getSchemas()) {
                    for (SidebarPlugin p : plugins) batch.addAll(p.getIndexItems(s, metaService));
                }
                indexService.indexItems(batch);

                Platform.runLater(() -> {
                    sidebar.populate(metaService);
                    sidebar.setupSearch(indexService);
                    statusLabel.setText("Ready.");
                    progressBar.setVisible(false);
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}