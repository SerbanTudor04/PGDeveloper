// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/ide/RoutineEditorTab.java
package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;

public class RoutineEditorTab extends Tab {

    private final String schema;
    private final String name;
    private final MetadataService metaService;
    private final QueryExecutor queryExecutor;
    private final TextArea codeArea;

    public RoutineEditorTab(String schema, String name, MetadataService metaService, QueryExecutor queryExecutor) {
        this.schema = schema;
        this.name = name;
        this.metaService = metaService;
        this.queryExecutor = queryExecutor;

        setText(name);
        setGraphic(new FontIcon(Feather.CODE));

        codeArea = new TextArea();
        codeArea.setStyle("-fx-font-family: monospace;");

        Button saveBtn = new Button("Apply", new FontIcon(Feather.SAVE));
        saveBtn.getStyleClass().add(Styles.SUCCESS);
        saveBtn.setOnAction(e -> saveRoutine());

        ToolBar toolbar = new ToolBar(saveBtn);
        BorderPane content = new BorderPane();
        content.setTop(toolbar);
        content.setCenter(codeArea);

        setContent(content);
        loadSource();
    }

    private void loadSource() {
        new Thread(() -> {
            try {
                String src = metaService.getRoutineSource(schema, name);
                Platform.runLater(() -> codeArea.setText(src));
            } catch (Exception e) {
                Platform.runLater(() -> codeArea.setText("-- Error: " + e.getMessage()));
            }
        }).start();
    }

    private void saveRoutine() {
        String sql = codeArea.getText();
        new Thread(() -> {
            try {
                queryExecutor.execute(sql);
                Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "Routine updated.").show());
            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show());
            }
        }).start();
    }
}