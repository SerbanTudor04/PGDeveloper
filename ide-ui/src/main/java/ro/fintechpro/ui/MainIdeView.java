package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;

import java.util.List;

public class MainIdeView {

    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final MetadataService metaService = new MetadataService(dbManager);
    private final QueryExecutor queryExecutor = new QueryExecutor();

    private final SidebarView sidebar = new SidebarView();
    private final TextArea sqlEditor = new TextArea("SELECT * FROM public.users;"); // Default text
    private final TableView<List<Object>> resultsTable = new TableView<>();
    private final TextArea messageConsole = new TextArea(); // For messages like "Rows updated: 5"

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
        sqlEditor.setFont(javafx.scene.text.Font.font("Monospaced", 14));

        // 3. Results Area (TabPane to switch between Grid and Messages)
        TabPane resultTabs = new TabPane();
        Tab gridTab = new Tab("Grid", resultsTable);
        gridTab.setClosable(false);

        Tab msgTab = new Tab("Messages", messageConsole);
        msgTab.setClosable(false);
        messageConsole.setEditable(false);

        resultTabs.getTabs().addAll(gridTab, msgTab);

        // Split Pane (Editor on Top, Results on Bottom)
        SplitPane splitPane = new SplitPane(sqlEditor, resultTabs);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.6); // Editor takes 60% height

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
            sql = sqlEditor.getText(); // Run everything if nothing selected
        }

        if (sql.trim().isEmpty()) return;

        final String finalSql = sql;
        statusLabel.setText("Executing query...");
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                // Execute in Core
                var result = queryExecutor.execute(finalSql);

                Platform.runLater(() -> {
                    if (result.isResultSet()) {
                        // Show Grid
                        ResultTableBuilder.populate(resultsTable, result);
                        messageConsole.setText(result.message());
                        // Select Grid Tab
                        ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(0);
                    } else {
                        // Show Message only
                        messageConsole.setText(result.message());
                        // Select Message Tab
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
                    // Switch to message tab to see error
                    ((TabPane)resultsTable.getParent().getParent()).getSelectionModel().select(1);
                });
            }
        }).start();
    }

    private void runIntrospection() {
        progressBar.setVisible(true);
        statusLabel.setText("Introspecting database structure...");

        new Thread(() -> {
            try {
                // Simulate a slight delay or heavy work so you see the bar
                Thread.sleep(500);

                // Fetch data inside the background thread is tricky for UI updates
                // Ideally, we fetch raw data here, then build TreeItems on UI thread.
                // For simplicity, we delegate to Sidebar but wrap UI calls in runLater inside it?
                // Actually, let's fetch here and update UI.

                // Let's rely on Sidebar's populate method but be careful about UI threads.
                // NOTE: In production, fetch Data Objects here, create TreeItems in Platform.runLater

                Platform.runLater(() -> {
                    sidebar.populate(metaService);
                    statusLabel.setText("Introspection complete.");
                    progressBar.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error loading structure: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: -color-danger-fg;");
                    progressBar.setVisible(false);
                });
            }
        }).start();
    }
}