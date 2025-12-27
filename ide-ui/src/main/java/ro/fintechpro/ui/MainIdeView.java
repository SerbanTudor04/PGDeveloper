package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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
import ro.fintechpro.ui.ide.ResultTableBuilder;
import ro.fintechpro.ui.ide.SidebarView;
import ro.fintechpro.ui.ide.SqlSyntaxHighlighter;
import ro.fintechpro.ui.plugins.FunctionPlugin;
import ro.fintechpro.ui.plugins.TablePlugin;

import java.util.ArrayList;
import java.util.List;

public class MainIdeView {

    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final MetadataService metaService = new MetadataService(dbManager);
    private final QueryExecutor queryExecutor = new QueryExecutor();
    private final LocalIndexService indexService = new LocalIndexService();

    // --- PLUGINS ---
    // Register your Sidebar components here
    private final List<SidebarPlugin> availablePlugins = List.of(
            new TablePlugin(),
            new FunctionPlugin()
    );

    // Inject plugins into Sidebar
    private final SidebarView sidebar = new SidebarView(availablePlugins);

    // UI Components
    private final CodeArea sqlEditor = new CodeArea();
    private final TableView<List<Object>> resultsTable = new TableView<>();
    private final TextArea messageConsole = new TextArea();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Ready");
    private final ComboBox<Integer> limitBox = new ComboBox<>(); // Limit selector

    public Parent getView() {
        BorderPane root = new BorderPane();

        // 1. SETUP SIDEBAR RESIZING
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(600);
        // Wire up the refresh logic (Sidebar controls UI, we control logic)
        sidebar.setOnRefresh(this::runIntrospection);

        // 2. MODERN HEADER BAR
        // A. Left: Execution Controls
        Button runBtn = new Button("Run", new FontIcon(Feather.PLAY));
        runBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.SMALL);
        runBtn.setOnAction(e -> executeQuery());

        Button cancelBtn = new Button(null, new FontIcon(Feather.SQUARE));
        cancelBtn.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_ICON, Styles.SMALL);
        cancelBtn.setTooltip(new Tooltip("Cancel Query"));
        cancelBtn.setDisable(true); // Placeholder

        HBox actions = new HBox(5, runBtn, cancelBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        // B. Center: Context Information
        // Shows "Active Connection" or DB name
        Label contextLabel = new Label("Active Connection", new FontIcon(Feather.DATABASE));
        contextLabel.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.SMALL);

        // C. Right: Settings (Limit Selector)
        Label limitLabel = new Label("Limit:");
        limitLabel.getStyleClass().add(Styles.TEXT_MUTED);

        limitBox.getItems().addAll(100, 500, 1000, 5000, 10000);
        limitBox.setValue(500);
        limitBox.getStyleClass().add(Styles.SMALL);
        limitBox.setPrefWidth(80);

        HBox settings = new HBox(8, limitLabel, limitBox);
        settings.setAlignment(Pos.CENTER_RIGHT);

        // Spacer to push settings to right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Assemble Header
        HBox header = new HBox(15, actions, new Separator(Orientation.VERTICAL), contextLabel, spacer, settings);
        header.setPadding(new Insets(8, 15, 8, 15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        // 3. EDITOR & RESULTS (Center Area)
        // Setup CodeArea
        sqlEditor.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(sqlEditor));
        SqlSyntaxHighlighter.enable(sqlEditor);
        sqlEditor.getStyleClass().add("styled-text-area");
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(sqlEditor);

        // Setup Results
        TabPane resultTabs = new TabPane();
        Tab gridTab = new Tab("Grid", resultsTable);
        gridTab.setClosable(false);
        Tab msgTab = new Tab("Messages", messageConsole);
        msgTab.setClosable(false);
        messageConsole.setEditable(false);
        resultTabs.getTabs().addAll(gridTab, msgTab);

        // Vertical Split (Code vs Results)
        SplitPane verticalSplit = new SplitPane(editorScroll, resultTabs);
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.setDividerPositions(0.6); // Code takes 60%
        VBox.setVgrow(verticalSplit, Priority.ALWAYS);

        // Combine Header + Vertical Split
        VBox centerContent = new VBox(header, verticalSplit);

        // 4. MAIN SPLIT (Sidebar vs Content)
        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(sidebar, centerContent);
        mainSplit.setDividerPositions(0.2); // Sidebar takes 20%
        mainSplit.getStyleClass().add(Styles.DENSE);

        // 5. FINALIZE ROOT
        root.setCenter(mainSplit);

        // Bottom Status Bar
        HBox statusBar = new HBox(10, statusLabel, progressBar);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: -color-bg-subtle; -fx-font-size: 11px;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        progressBar.setVisible(false);
        root.setBottom(statusBar);

        // Initial Load (No callback needed)
        runIntrospection(null);

        return root;
    }

    // --- LOGIC ---

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
                // Execute logic
                var result = queryExecutor.execute(finalSql);

                Platform.runLater(() -> {
                    if (result.isResultSet()) {
                        ResultTableBuilder.populate(resultsTable, result);
                        messageConsole.setText(result.message());
                        ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(0); // Grid Tab
                    } else {
                        messageConsole.setText(result.message());
                        ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(1); // Message Tab
                    }
                    statusLabel.setText("Execution finished.");
                    progressBar.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageConsole.setText("Error:\n" + e.getMessage());
                    statusLabel.setText("Execution failed.");
                    statusLabel.setStyle("-fx-text-fill: -color-danger-fg;");
                    progressBar.setVisible(false);
                    ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(1);
                });
            }
        }).start();
    }

    private void runIntrospection(Runnable onComplete) {
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
                    statusLabel.setStyle(""); // Reset color
                    progressBar.setVisible(false);

                    if (onComplete != null) onComplete.run();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: -color-danger-fg;");
                    progressBar.setVisible(false);
                    if (onComplete != null) onComplete.run();
                });
            }
        }).start();
    }
}