package ro.fintechpro.ui.ide;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ro.fintechpro.core.service.QueryExecutor;

import java.util.List;

public class ResultTableBuilder {

    public static void populate(TableView<List<Object>> table, QueryExecutor.QueryResult result) {
        table.getColumns().clear();
        table.getItems().clear();

        if (!result.isResultSet()) {
            return; // Nothing to show for UPDATEs
        }

        // 1. Create Columns dynamically
        for (int i = 0; i < result.columns().size(); i++) {
            final int colIndex = i;
            String colName = result.columns().get(i);

            TableColumn<List<Object>, String> col = new TableColumn<>(colName);

            // Map the row data (List<Object>) to this column
            col.setCellValueFactory(cellData -> {
                List<Object> row = cellData.getValue();
                Object value = (row.size() > colIndex) ? row.get(colIndex) : null;
                return new SimpleStringProperty(value == null ? "<null>" : value.toString());
            });

            table.getColumns().add(col);
        }

        // 2. Add Data
        table.getItems().addAll(result.data());
    }
}