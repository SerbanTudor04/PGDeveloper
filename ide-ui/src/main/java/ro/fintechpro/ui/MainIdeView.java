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

        // --- LEFT: Sidebar ---
        sidebar.setPrefWidth(250);
        sidebar.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 1 0 0;");
        root.setLeft(sidebar);

        // --- CENTER: Editor & Results ---

        // 1. Toolbar
        Button runBtn = new Button("Execute", new FontIcon(Feather.PLAY));
        runBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.SMALL);
        runBtn.setOnAction(e -> executeQuery());

        ToolBar toolbar = new ToolBar(runBtn, new Separator(), new Label("Limit: 500"));

        // 2. Editor
        // sqlEditor.replaceText(0, 0, "SELECT * FROM public.users;");
        sqlEditor.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(sqlEditor));

        SqlSyntaxHighlighter.enable(sqlEditor);
        sqlEditor.getStyleClass().add("styled-text-area");

        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(sqlEditor);

        // 3. Results Area
        TabPane resultTabs = new TabPane();
        Tab gridTab = new Tab("Grid", resultsTable);
        gridTab.setClosable(false);

        Tab msgTab = new Tab("Messages", messageConsole);
        msgTab.setClosable(false);
        messageConsole.setEditable(false);

        resultTabs.getTabs().addAll(gridTab, msgTab);

        SplitPane splitPane = new SplitPane(editorScroll, resultTabs);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.6);

        VBox centerArea = new VBox(toolbar, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.setCenter(centerArea);

        // --- BOTTOM: Status Bar ---
        HBox statusBar = new HBox(10, statusLabel, progressBar);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: -color-bg-subtle; -fx-font-size: 11px;");
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        progressBar.setVisible(false);
        root.setBottom(statusBar);

        // Start Introspection
        runIntrospection();

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

    private void runIntrospection() {
        progressBar.setVisible(true);
        statusLabel.setText("Indexing database structure...");

        new Thread(() -> {
            try {
                // 1. Clear old index
                indexService.clearIndex();
                List<LocalIndexService.SearchResult> batch = new ArrayList<>();
                List<String> schemas = metaService.getSchemas();

                for (String schema : schemas) {
                    // --- DYNAMIC INDEXING ---
                    // Instead of hardcoding "getTables", we let each plugin provide its items
                    for (SidebarPlugin plugin : availablePlugins) {
                        List<LocalIndexService.SearchResult> items = plugin.getIndexItems(schema, metaService);
                        batch.addAll(items);
                    }
                }

                // 2. Insert into H2
                indexService.indexItems(batch);

                // 3. Update UI
                Platform.runLater(() -> {
                    sidebar.populate(metaService);
                    sidebar.setupSearch(indexService);

                    statusLabel.setText("Introspection & Indexing complete. (" + batch.size() + " objects)");
                    progressBar.setVisible(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }
}