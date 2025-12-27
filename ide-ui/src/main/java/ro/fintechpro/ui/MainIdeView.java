package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;
import ro.fintechpro.core.spi.SidebarPlugin;
import ro.fintechpro.ui.ide.DockLayout;
import ro.fintechpro.ui.ide.ResultTableBuilder;
import ro.fintechpro.ui.ide.SidebarView;
import ro.fintechpro.ui.ide.SqlSyntaxHighlighter;
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

    // UI Components
    private final List<SidebarPlugin> plugins = List.of(new TablePlugin(), new FunctionPlugin());
    private final SidebarView sidebar = new SidebarView(plugins);

    private final CodeArea sqlEditor = new CodeArea();
    private final TableView<List<Object>> resultsTable = new TableView<>();
    private final TextArea messageConsole = new TextArea();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Ready");

    // NEW: Docking System
    private DockLayout dockLayout;

    public Parent getView() {
        // 1. SETUP EDITOR (Center Component)
        // We wrap the editor in a VBox with the Header, so the header stays attached to the code
        VBox editorWrapper = createEditorArea();

        // 2. INITIALIZE DOCKING SYSTEM
        dockLayout = new DockLayout(editorWrapper);

        // 3. DOCK COMPONENTS
        // Sidebar -> Left
        dockLayout.dock(sidebar, "Explorer", DockLayout.Location.LEFT);

        // Results & Messages -> Bottom (Stacked)
        dockLayout.dock(resultsTable, "Query Results", DockLayout.Location.BOTTOM);
        dockLayout.dock(messageConsole, "Console", DockLayout.Location.BOTTOM);

        // 4. SETUP MENU BAR (To re-open closed views)
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

    private VBox createEditorArea() {
        // --- Header (Run Button, Limit, etc.) ---
        Button runBtn = new Button("Run", new FontIcon(Feather.PLAY));
        runBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.SMALL);
        runBtn.setOnAction(e -> executeQuery());

        HBox header = new HBox(10, runBtn, new Separator(javafx.geometry.Orientation.VERTICAL), new Label("Limit: 500"));
        header.setPadding(new Insets(5, 10, 5, 10));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        // --- Code Editor ---
        sqlEditor.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(sqlEditor));
        SqlSyntaxHighlighter.enable(sqlEditor);
        sqlEditor.getStyleClass().add("styled-text-area");
        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(sqlEditor);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new VBox(header, scroll);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu viewMenu = new Menu("View");

        // Helper to add menu items that re-open windows
        MenuItem openExplorer = new MenuItem("Database Explorer");
        openExplorer.setOnAction(e -> dockLayout.dock(sidebar, "Explorer", DockLayout.Location.LEFT));

        MenuItem openResults = new MenuItem("Query Results");
        openResults.setOnAction(e -> dockLayout.dock(resultsTable, "Query Results", DockLayout.Location.BOTTOM));

        viewMenu.getItems().addAll(openExplorer, openResults);
        menuBar.getMenus().add(viewMenu);
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

    // --- LOGIC (Introspection & Execution) ---
    // (This remains largely the same, just targeting the new UI structure)

    private void executeQuery() {
        String sql = sqlEditor.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) sql = sqlEditor.getText();
        if (sql.trim().isEmpty()) return;

        final String finalSql = sql;
        statusLabel.setText("Executing...");
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                var result = queryExecutor.execute(finalSql);
                Platform.runLater(() -> {
                    // Ensure the panels are visible/docked when we get results
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