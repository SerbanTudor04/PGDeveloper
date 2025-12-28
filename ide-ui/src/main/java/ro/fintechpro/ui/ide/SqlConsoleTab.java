// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/ide/SqlConsoleTab.java
package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.WorkspaceService;

import java.util.Optional;
import java.util.function.Consumer;

public class SqlConsoleTab extends Tab {

    private final String consoleId;
    private final CodeArea codeArea = new CodeArea();
    private final ComboBox<String> connectionSelector = new ComboBox<>();

    private final Consumer<String> executeAction;
    private final MetadataService metaService; // Required for Autocompletion

    /**
     * @param state The saved state of the console (ID, name, content).
     * @param executeAction The callback to run the SQL.
     * @param metaService The service used to fetch table/column names for Autocompletion.
     */
    public SqlConsoleTab(WorkspaceService.ConsoleState state,
                         Consumer<String> executeAction,
                         MetadataService metaService) {
        super(state.name());
        this.consoleId = state.id();
        this.executeAction = executeAction;
        this.metaService = metaService;
        this.setClosable(true);

        // --- 1. Tab Graphic (Icon) ---
        FontIcon tabIcon = new FontIcon(Feather.TERMINAL);
        tabIcon.setIconColor(Color.web("#5263e3")); // Blue Nuance
        setGraphic(tabIcon);

        // --- 2. Context Menu (Rename, Close) ---
        ContextMenu contextMenu = new ContextMenu();

        MenuItem renameItem = new MenuItem("Rename Console", new FontIcon(Feather.EDIT_2));
        renameItem.setOnAction(e -> promptRename());

        MenuItem closeItem = new MenuItem("Close", new FontIcon(Feather.X));
        closeItem.setOnAction(e -> {
            if (getTabPane() != null) getTabPane().getTabs().remove(this);
        });

        contextMenu.getItems().addAll(renameItem, new SeparatorMenuItem(), closeItem);
        setContextMenu(contextMenu);

        // --- 3. Toolbar (Execute, Connection) ---
        Button runBtn = new Button("Run", new FontIcon(Feather.PLAY));
        runBtn.getStyleClass().add(Styles.SUCCESS);
        runBtn.setOnAction(e -> runQuery());
        runBtn.setTooltip(new Tooltip("Execute Query (Ctrl+Enter)"));

        // Connection Selector
        connectionSelector.setPrefWidth(150);
        connectionSelector.getItems().addAll(DataSourceManager.getInstance().getProfiles().keySet());
        if (state.connectionName() != null && connectionSelector.getItems().contains(state.connectionName())) {
            connectionSelector.setValue(state.connectionName());
        } else if (!connectionSelector.getItems().isEmpty()) {
            connectionSelector.getSelectionModel().selectFirst();
        }

        ToolBar toolbar = new ToolBar(runBtn, new Separator(), new Label("Connection:"), connectionSelector);

        // --- 4. Code Editor Setup ---
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(state.content());

        // A. Enable Syntax Highlighting (Toad Style)
        SqlSyntaxHighlighter.enable(codeArea);

        // B. Enable IntelliSense / Autocompletion
        // (Assuming SqlAutocompletion is created as per previous instructions)
        new SqlAutocompletion(codeArea, metaService);

        // C. Key Bindings
        codeArea.setOnKeyPressed(e -> {
            // Ctrl+Enter or Cmd+Enter to Run
            if (e.getCode().toString().equals("ENTER") && e.isShortcutDown()) {
                runQuery();
            }
        });

        // --- 5. Layout ---
        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        VBox content = new VBox(toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        setContent(content);
    }

    private void promptRename() {
        TextInputDialog dialog = new TextInputDialog(getText());
        dialog.setTitle("Rename Console");
        dialog.setHeaderText("Enter a new name for this console:");
        dialog.setContentText("Name:");

        // Optional: Style the dialog
        // dialog.getDialogPane().getStylesheets().add(getClass().getResource("/sql-keywords.css").toExternalForm());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                this.setText(newName);
            }
        });
    }

    private void runQuery() {
        String sql = codeArea.getSelectedText();
        // If nothing selected, run everything
        if (sql == null || sql.trim().isEmpty()) sql = codeArea.getText();

        if (!sql.trim().isEmpty()) {
            executeAction.accept(sql);
        }
    }

    /**
     * captures the current state for persistence.
     */
    public WorkspaceService.ConsoleState toState() {
        return new WorkspaceService.ConsoleState(
                consoleId,
                getText(),
                connectionSelector.getValue(),
                codeArea.getText()
        );
    }

    public String getConsoleId() { return consoleId; }

    public String getSqlContent() { return codeArea.getText(); }

    public String getConnectionName() { return connectionSelector.getValue(); }
}