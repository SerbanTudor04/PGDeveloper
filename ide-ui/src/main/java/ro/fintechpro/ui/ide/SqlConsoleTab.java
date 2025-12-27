package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.WorkspaceService;

import java.util.function.Consumer;

public class SqlConsoleTab extends Tab {

    private final String consoleId;
    private final CodeArea codeArea = new CodeArea();
    private final ComboBox<String> connectionSelector = new ComboBox<>();

    private final Consumer<String> executeAction;

    public SqlConsoleTab(WorkspaceService.ConsoleState state, Consumer<String> executeAction) {
        super(state.name());
        this.consoleId = state.id();
        this.executeAction = executeAction;
        this.setClosable(true);

        // 1. Editor Setup
        codeArea.replaceText(state.content());
        codeArea.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(codeArea));
        SqlSyntaxHighlighter.enable(codeArea);
        codeArea.getStyleClass().add("styled-text-area");

        // 2. Toolbar
        Button runBtn = new Button("Run", new FontIcon(Feather.PLAY));
        runBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.SMALL);
        runBtn.setOnAction(e -> runQuery());

        connectionSelector.getItems().addAll("postgres", "test_db", "analytics");
        connectionSelector.setValue(state.connectionName() != null ? state.connectionName() : "postgres");
        connectionSelector.getStyleClass().add(Styles.SMALL);

        Label limitLabel = new Label("Limit: 500");
        limitLabel.getStyleClass().add(Styles.TEXT_MUTED);

        HBox toolbar = new HBox(10, runBtn, new Separator(javafx.geometry.Orientation.VERTICAL), connectionSelector, new Separator(javafx.geometry.Orientation.VERTICAL), limitLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new javafx.geometry.Insets(5));
        toolbar.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        // 3. Layout
        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        VBox content = new VBox(toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        setContent(content);
    }

    private void runQuery() {
        String sql = codeArea.getSelectedText();
        if (sql == null || sql.trim().isEmpty()) sql = codeArea.getText();

        if (!sql.trim().isEmpty()) {
            executeAction.accept(sql);
        }
    }

    public WorkspaceService.ConsoleState toState() {
        return new WorkspaceService.ConsoleState(consoleId, getText(), connectionSelector.getValue(), codeArea.getText());
    }

    public String getConsoleId() { return consoleId; }

    // --- RENAMED METHOD HERE ---
    public String getSqlContent() { return codeArea.getText(); }

    public String getConnectionName() { return connectionSelector.getValue(); }
}