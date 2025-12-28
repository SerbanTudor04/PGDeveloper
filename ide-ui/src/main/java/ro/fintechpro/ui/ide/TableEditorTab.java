// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/ide/TableEditorTab.java
package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.MetadataService.ColumnInfo;
import ro.fintechpro.core.service.MetadataService.IndexInfo;
import ro.fintechpro.core.service.QueryExecutor;

public class TableEditorTab extends Tab {

    private final String schema;
    private final String table;
    private final MetadataService metaService;
    private final QueryExecutor queryExecutor;

    private final TableView<ColumnInfo> columnsTable = new TableView<>();
    private final TableView<IndexInfo> indexesTable = new TableView<>();

    public TableEditorTab(String schema, String table, MetadataService metaService, QueryExecutor queryExecutor) {
        this.schema = schema;
        this.table = table;
        this.metaService = metaService;
        this.queryExecutor = queryExecutor;

        setText(table);
        setGraphic(new FontIcon(Feather.LAYOUT));

        TabPane detailsPane = new TabPane();
        detailsPane.getTabs().addAll(createColumnsTab(), createIndexesTab());

        setContent(detailsPane);
        refreshData();
    }

    private Tab createColumnsTab() {
        Tab tab = new Tab("Columns");
        tab.setClosable(false);

        // Columns Definition
        TableColumn<ColumnInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().name()));

        TableColumn<ColumnInfo, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().type()));

        TableColumn<ColumnInfo, Integer> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().size()).asObject());

        TableColumn<ColumnInfo, Boolean> nullCol = new TableColumn<>("Nullable");
        nullCol.setCellValueFactory(p -> new SimpleBooleanProperty(p.getValue().isNullable()));

        columnsTable.getColumns().addAll(nameCol, typeCol, sizeCol, nullCol);
        columnsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Actions
        Button addBtn = new Button("Add Column", new FontIcon(Feather.PLUS));
        addBtn.setOnAction(e -> showAddColumnDialog());

        Button dropBtn = new Button("Drop Column", new FontIcon(Feather.TRASH));
        dropBtn.getStyleClass().add(Styles.DANGER);
        dropBtn.setOnAction(e -> dropSelectedColumn());

        ToolBar toolbar = new ToolBar(addBtn, dropBtn);

        BorderPane p = new BorderPane();
        p.setTop(toolbar);
        p.setCenter(columnsTable);
        tab.setContent(p);
        return tab;
    }

    private Tab createIndexesTab() {
        Tab tab = new Tab("Indexes");
        tab.setClosable(false);

        TableColumn<IndexInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().name()));

        TableColumn<IndexInfo, Boolean> uniqCol = new TableColumn<>("Unique");
        uniqCol.setCellValueFactory(p -> new SimpleBooleanProperty(p.getValue().isUnique()));

        indexesTable.getColumns().addAll(nameCol, uniqCol);
        indexesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button dropBtn = new Button("Drop Index", new FontIcon(Feather.TRASH));
        dropBtn.setOnAction(e -> dropSelectedIndex());

        ToolBar toolbar = new ToolBar(dropBtn);
        BorderPane p = new BorderPane();
        p.setTop(toolbar);
        p.setCenter(indexesTable);
        tab.setContent(p);
        return tab;
    }

    private void refreshData() {
        new Thread(() -> {
            try {
                var cols = metaService.getColumns(schema, table);
                var idxs = metaService.getIndexes(schema, table);
                Platform.runLater(() -> {
                    columnsTable.getItems().setAll(cols);
                    indexesTable.getItems().setAll(idxs);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // --- Action Logic ---

    private void showAddColumnDialog() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Add Column");
        d.setHeaderText("Enter column definition (e.g. 'age INT')");
        d.showAndWait().ifPresent(def -> {
            String sql = "ALTER TABLE " + schema + "." + table + " ADD COLUMN " + def;
            executeDdl(sql);
        });
    }

    private void dropSelectedColumn() {
        var sel = columnsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String sql = "ALTER TABLE " + schema + "." + table + " DROP COLUMN " + sel.name();
        executeDdl(sql);
    }

    private void dropSelectedIndex() {
        var sel = indexesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String sql = "DROP INDEX " + schema + "." + sel.name();
        executeDdl(sql);
    }

    private void executeDdl(String sql) {
        new Thread(() -> {
            try {
                queryExecutor.execute(sql);
                Platform.runLater(() -> {
                    refreshData(); // Refresh UI
                    new Alert(Alert.AlertType.INFORMATION, "Success").show();
                });
            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
            }
        }).start();
    }
}