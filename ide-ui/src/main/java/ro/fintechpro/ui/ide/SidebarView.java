package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.MetadataService;

import java.util.List;

public class SidebarView extends VBox {

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;

    public SidebarView() {
        // Root Node (The Database)
        rootItem = new TreeItem<>("Database", new FontIcon(Feather.DATABASE));
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.getStyleClass().add(Styles.DENSE); // Tight spacing like DataGrip
        VBox.setVgrow(treeView, Priority.ALWAYS);

        this.getChildren().add(treeView);
    }

    /**
     * Called by the background loader to populate the tree
     */
    public void populate(MetadataService metaService) {
        try {
            List<String> schemas = metaService.getSchemas();

            for (String schema : schemas) {
                TreeItem<String> schemaItem = new TreeItem<>(schema, new FontIcon(Feather.FOLDER));

                // Fetch Tables
                List<MetadataService.DbObject> tables = metaService.getTables(schema);
                if (!tables.isEmpty()) {
                    TreeItem<String> tablesGroup = new TreeItem<>("Tables", new FontIcon(Feather.LIST));
                    for (MetadataService.DbObject table : tables) {
                        tablesGroup.getChildren().add(new TreeItem<>(table.name(), new FontIcon(Feather.LAYOUT)));
                    }
                    schemaItem.getChildren().add(tablesGroup);
                }

                // Fetch Functions
                List<String> procs = metaService.getProcedures(schema);
                if (!procs.isEmpty()) {
                    TreeItem<String> funcGroup = new TreeItem<>("Procedures", new FontIcon(Feather.CODE));
                    for (String proc : procs) {
                        funcGroup.getChildren().add(new TreeItem<>(proc, new FontIcon(Feather.PLAY_CIRCLE)));
                    }
                    schemaItem.getChildren().add(funcGroup);
                }

                rootItem.getChildren().add(schemaItem);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle gracefully in real app
        }
    }
}