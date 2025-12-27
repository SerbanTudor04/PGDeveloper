package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import org.fxmisc.flowless.VirtualizedScrollPane;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;
import ro.fintechpro.core.spi.SidebarPlugin;
import ro.fintechpro.ui.ide.ResultTableBuilder;
import ro.fintechpro.ui.ide.SidebarView;
import ro.fintechpro.ui.ide.SqlSyntaxHighlighter;
import ro.fintechpro.ui.plugins.FunctionPlugin;
import ro.fintechpro.ui.plugins.TablePlugin;
// import ro.fintechpro.ui.plugins.ProcedurePlugin; // Add this if you created it

import java.util.ArrayList;
import java.util.List;

public class MainIdeView {

    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final MetadataService metaService = new MetadataService(dbManager);
    private final QueryExecutor queryExecutor = new QueryExecutor();
    private final LocalIndexService indexService = new LocalIndexService();

    // --- PLUGIN CONFIGURATION ---
    // This is where you register new Sidebar components!
    private final List<SidebarPlugin> availablePlugins = List.of(
            new TablePlugin(),
            new FunctionPlugin()
            // new ProcedurePlugin()
    );

    // Inject plugins into Sidebar
    private final SidebarView sidebar = new SidebarView(availablePlugins);

    private final CodeArea sqlEditor = new CodeArea();
    private final TableView<List<Object>> resultsTable = new TableView<>();
    private final TextArea messageConsole = new TextArea();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Ready");

    public Parent getView() {
        BorderPane root = new BorderPane();

        // --- 1. SETUP SIDEBAR (New: Constraints for resizing) ---
        // Don't set a fixed PrefWidth, instead set Min/Max so it doesn't break
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(600);

        // --- 2. SETUP CENTER AREA (Toolbar + Editor + Results) ---
        // (This part stays mostly the same, just creating the object)

        Button runBtn = new Button("Execute", new FontIcon(Feather.PLAY));
        runBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.SMALL);
        runBtn.setOnAction(e -> executeQuery());

        ToolBar toolbar = new ToolBar(runBtn, new Separator(), new Label("Limit: 500"));

        sqlEditor.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(sqlEditor));
        SqlSyntaxHighlighter.enable(sqlEditor);
        sqlEditor.getStyleClass().add("styled-text-area");
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(sqlEditor);

        TabPane resultTabs = new TabPane();
        Tab gridTab = new Tab("Grid", resultsTable);
        gridTab.setClosable(false);
        Tab msgTab = new Tab("Messages", messageConsole);
        msgTab.setClosable(false);
        messageConsole.setEditable(false);
        resultTabs.getTabs().addAll(gridTab, msgTab);

        // Vertical Split: Editor (Top) vs Results (Bottom)
        SplitPane verticalSplit = new SplitPane(editorScroll, resultTabs);
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.setDividerPositions(0.6);
        VBox.setVgrow(verticalSplit, Priority.ALWAYS);

        VBox centerArea = new VBox(toolbar, verticalSplit);

        // --- 3. NEW: HORIZONTAL SPLIT PANE (Sidebar vs Center) ---
        // This replaces root.setLeft() and root.setCenter()
        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(sidebar, centerArea);

        // Set initial divider position (e.g., 20% for sidebar)
        mainSplit.setDividerPositions(0.2);

        // Add styling to make the divider look nice (optional)
        mainSplit.getStyleClass().add(Styles.DENSE);

        // --- 4. ASSEMBLE ROOT ---
        // The SplitPane is now the center of the application
        root.setCenter(mainSplit);

        // Wire up the sidebar refresh logic
        sidebar.setOnRefresh(this::runIntrospection);

        // --- BOTTOM: Status Bar ---
        HBox statusBar = new HBox(10, statusLabel, progressBar);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: -color-bg-subtle; -fx-font-size: 11px;");
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        progressBar.setVisible(false);
        root.setBottom(statusBar);

        // Start Introspection
        runIntrospection(null);

        return root;
    }

    private void executeQuery() {
        String sql = sqlEditor.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) {
            sql = sqlEditor.getText();
        }

        if (sql.trim().isEmpty()) return;

        final String finalSql = sql;
        statusLabel.setText("Executing query...");
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                var result = queryExecutor.execute(finalSql);

                Platform.runLater(() -> {
                    if (result.isResultSet()) {
                        ResultTableBuilder.populate(resultsTable, result);
                        messageConsole.setText(result.message());
                        ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(0);
                    } else {
                        messageConsole.setText(result.message());
                        ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(1);
                    }
                    statusLabel.setText("Execution finished.");
                    progressBar.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageConsole.setText("Error:\n" + e.getMessage());
                    statusLabel.setText("Execution failed.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    progressBar.setVisible(false);
                    ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(1);
                });
            }
        }).start();
    }

    private void runIntrospection(Runnable onComplete) {
        // Only show bottom bar if it's the initial load (optional, keeps it "silent" for refresh)
        if (onComplete == null) {
            progressBar.setVisible(true);
            statusLabel.setText("Indexing database structure...");
        } else {
            statusLabel.setText("Refreshing structure...");
        }

        new Thread(() -> {
            try {
                // 1. Clear & Re-Index
                indexService.clearIndex();
                List<LocalIndexService.SearchResult> batch = new ArrayList<>();
                List<String> schemas = metaService.getSchemas();

                for (String schema : schemas) {
                    for (SidebarPlugin plugin : availablePlugins) {
                        batch.addAll(plugin.getIndexItems(schema, metaService));
                    }
                }

                indexService.indexItems(batch);

                // 2. Update UI
                Platform.runLater(() -> {
                    sidebar.populate(metaService);
                    sidebar.setupSearch(indexService);

                    statusLabel.setText("Ready. (" + batch.size() + " objects indexed)");
                    progressBar.setVisible(false);

                    // Trigger the callback (e.g., stop the refresh button spinner)
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    progressBar.setVisible(false);
                    if (onComplete != null) onComplete.run(); // Ensure button resets even on error
                });
            }
        }).start();
    }
}