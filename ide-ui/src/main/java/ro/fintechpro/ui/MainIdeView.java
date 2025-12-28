package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.DatabaseCache;
import ro.fintechpro.core.model.SidebarItem;
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;
import ro.fintechpro.core.service.WorkspaceService;
import ro.fintechpro.core.spi.SidebarPlugin;
import ro.fintechpro.ui.components.CustomTitleBar;
import ro.fintechpro.ui.ide.DockLayout;
import ro.fintechpro.ui.ide.ResultTableBuilder;
import ro.fintechpro.ui.ide.SidebarView;
import ro.fintechpro.ui.ide.SqlConsoleTab;
import ro.fintechpro.ui.plugins.FunctionPlugin;
import ro.fintechpro.ui.plugins.ProcedurePlugin;
import ro.fintechpro.ui.plugins.TablePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MainIdeView {

    // Services
    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final MetadataService metaService = new MetadataService(dbManager);
    private final QueryExecutor queryExecutor = new QueryExecutor();
    private final LocalIndexService indexService = new LocalIndexService();
    private final WorkspaceService workspaceService = new WorkspaceService();

    // UI Components
    private final List<SidebarPlugin> plugins = List.of(
            new TablePlugin(),
            new FunctionPlugin(),
            new ProcedurePlugin()
    );
    private final SidebarView sidebar = new SidebarView(plugins);
    private TabPane editorTabPane;
    private final TableView<List<Object>> resultsTable = new TableView<>();
    private final TextArea messageConsole = new TextArea();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Ready");
    private DockLayout dockLayout;

    private boolean isPreloaded = false;
    private String connectionProfileName = "default";
    public void setConnectionProfileName(String name) {
        this.connectionProfileName = name;
    }


    /**
     * BLOCKING method called by the Splash Screen background thread.
     * It performs all DB queries and Indexing before the UI is shown.
     */
    public void preload(Consumer<String> statusUpdater) {
        try {
            statusUpdater.accept("Connecting to database...");
            if (!dbManager.testConnection()) throw new RuntimeException("Connection lost");

            // --- 1. SMART INTROSPECTION ---
            statusUpdater.accept("Checking metadata cache...");

            // A. Load disk cache
            DatabaseCache diskCache = workspaceService.loadMetadata(connectionProfileName);

            // B. Perform Introspection (Incremental)
            statusUpdater.accept("Introspecting database structure...");
            DatabaseCache freshCache = metaService.introspect(connectionProfileName, diskCache);

            // C. Save back to disk
            workspaceService.saveMetadata(freshCache);

            // --- 2. INDEXING (Now fast because it reads from memory) ---
            statusUpdater.accept("Indexing search...");
            indexService.clearIndex();

            List<LocalIndexService.SearchResult> batch = new ArrayList<>();
            // metaService.getSchemas() now returns from RAM instantly
            List<String> schemas = metaService.getSchemas();

            int count = 0;
            for (String schema : schemas) {
                statusUpdater.accept("Indexing schema: " + schema);
                for (SidebarPlugin p : plugins) {
                    batch.addAll(p.getIndexItems(schema, metaService));
                }
                count++;
            }

            statusUpdater.accept("Finalizing index (" + batch.size() + " objects)...");
            indexService.indexItems(batch);

            // 3. Load Workspace
            statusUpdater.accept("Restoring workspace...");
            workspaceService.loadState();

            isPreloaded = true;
            statusUpdater.accept("Ready!");

        } catch (Exception e) {
            e.printStackTrace();
            statusUpdater.accept("Error: " + e.getMessage());
        }
    }

    public Parent getView(Stage stage) {

        // 1. SETUP EDITOR AREA
        editorTabPane = new TabPane();
        editorTabPane.getStyleClass().add(Styles.DENSE);

        List<WorkspaceService.ConsoleState> savedStates = workspaceService.loadState();
        if (savedStates.isEmpty()) {
            addNewConsole();
        } else {
            for (var state : savedStates) addConsoleTab(state);
        }
        setupAutoSave();

        CustomTitleBar titleBar =
                new CustomTitleBar(stage, "PgDeveloper - " + dbManager.getConnectionInfoOrName());


        // 2. DOCKING SYSTEM
        dockLayout = new DockLayout(editorTabPane);
        if (getClass().getResource("/dock-layout.css") != null) {
            dockLayout.getStylesheets().add(getClass().getResource("/dock-layout.css").toExternalForm());
        }

        dockLayout.dock(sidebar, "Explorer", DockLayout.Location.LEFT);
        dockLayout.dock(resultsTable, "Query Results", DockLayout.Location.BOTTOM);
        dockLayout.dock(messageConsole, "Console", DockLayout.Location.BOTTOM);

        // 3. MENU & ROOT
        MenuBar menuBar = createMenuBar();

        menuBar.setUseSystemMenuBar(false);
        // COMBINE TitleBar and MenuBar
        VBox topContainer = new VBox(titleBar, menuBar);
        topContainer.setStyle("-fx-background-color: -color-bg-default;");
        BorderPane root = new BorderPane();
        if (getClass().getResource("/app-main.css") != null) {
            root.getStylesheets().add(getClass().getResource("/app-main.css").toExternalForm());
        }
        root.setTop(topContainer);
        root.setCenter(dockLayout);
        root.setBottom(createStatusBar());

        sidebar.setOnRefresh(this::runIntrospection);

        // 4. INITIAL POPULATION
        if (isPreloaded) {
            // Data is already in memory/H2, just render the sidebar
            // We must wrap this in runLater to ensure it happens after the scene is attached
            Platform.runLater(() -> {
                sidebar.populate(metaService);
                sidebar.setupSearch(indexService);
                statusLabel.setText("Ready (Preloaded).");
            });
        } else {
            // Fallback if skipped splash screen
            runIntrospection(null);
        }

        sidebar.setOnItemOpen(this::openObjectTab);
        return root;
    }

    // --- CONSOLE MANAGEMENT ---
    private void addNewConsole() {
        var state = workspaceService.createNewConsole("postgres");
        addConsoleTab(state);
    }

    private void addConsoleTab(WorkspaceService.ConsoleState state) {
        SqlConsoleTab tab = new SqlConsoleTab(state, this::executeQuery);
        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
    }

    private void saveWorkspace() {
        List<WorkspaceService.ConsoleState> states = new ArrayList<>();
        for (Tab t : editorTabPane.getTabs()) {
            if (t instanceof SqlConsoleTab sqlTab) {
                states.add(sqlTab.toState());
                workspaceService.saveConsoleContent(sqlTab.getConsoleId(), sqlTab.getSqlContent());
            }
        }
        workspaceService.saveState(states);
        Platform.runLater(() -> statusLabel.setText("Workspace saved."));
    }

    private void setupAutoSave() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveWorkspace));
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
        Menu fileMenu = new Menu("File");
        MenuItem newConsole = new MenuItem("New Console", new FontIcon(Feather.PLUS));
        newConsole.setOnAction(e -> addNewConsole());
        MenuItem saveItem = new MenuItem("Save Workspace", new FontIcon(Feather.SAVE));
        saveItem.setOnAction(e -> saveWorkspace());
        fileMenu.getItems().addAll(newConsole, saveItem);

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

    // --- LOGIC (Execution & Refresh) ---
    private void executeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) return;
        statusLabel.setText("Executing...");
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                var result = queryExecutor.execute(sql);
                Platform.runLater(() -> {
                    dockLayout.dock(result.isResultSet() ? resultsTable : messageConsole,
                            result.isResultSet() ? "Query Results" : "Console",
                            DockLayout.Location.BOTTOM);

                    if (result.isResultSet()) ResultTableBuilder.populate(resultsTable, result);
                    else messageConsole.setText(result.message());

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

    // This handles manual refresh button clicks
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


    private void openObjectTab(SidebarItem item) {
        // 1. Check if tab already exists
        String tabId = item.type() + ":" + item.schema() + "." + item.name();
        for (Tab t : editorTabPane.getTabs()) {
            if (tabId.equals(t.getUserData())) {
                editorTabPane.getSelectionModel().select(t);
                return;
            }
        }

        // 2. Ask plugins to create the tab
        for (SidebarPlugin plugin : plugins) {
            Tab tab = plugin.createTab(item, metaService, queryExecutor);
            if (tab != null) {
                tab.setUserData(tabId); // Tag it for uniqueness
                editorTabPane.getTabs().add(tab);
                editorTabPane.getSelectionModel().select(tab);
                return;
            }
        }
    }
}