// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/plugins/TablePlugin.java
package ro.fintechpro.ui.plugins;

import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.model.SidebarItem;
import ro.fintechpro.core.service.LocalIndexService.SearchResult;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;
import ro.fintechpro.core.spi.SidebarPlugin;
import ro.fintechpro.ui.ide.TableEditorTab;

import java.util.ArrayList;
import java.util.List;

public class TablePlugin implements SidebarPlugin {

    @Override
    public TreeItem<SidebarItem> createNode(String schema, MetadataService metaService) {
        try {
            var tables = metaService.getTables(schema);
            if (tables.isEmpty()) return null;

            FontIcon rootIcon = new FontIcon(Feather.GRID);
            rootIcon.setStyle("-fx-icon-color: #5263e3;");

            SidebarItem rootData = new SidebarItem("Tables (" + tables.size() + ")", SidebarItem.TYPE_FOLDER, schema, null);
            TreeItem<SidebarItem> root = new TreeItem<>(rootData, rootIcon);

            for (var t : tables) {
                FontIcon icon = new FontIcon(Feather.LAYOUT);
                rootIcon.setStyle("-fx-icon-color: #5263e3;");

                SidebarItem itemData = new SidebarItem(t.name(), SidebarItem.TYPE_TABLE, schema, t.name());
                root.getChildren().add(new TreeItem<>(itemData, icon));
            }
            return root;
        } catch (Exception e) { return null; }
    }

    @Override
    public List<SearchResult> getIndexItems(String schema, MetadataService metaService) {
        return new ArrayList<>();
    }

    @Override
    public Tab createTab(SidebarItem item, MetadataService metaService, QueryExecutor queryExecutor) {
        if (SidebarItem.TYPE_TABLE.equals(item.type())) {
            return new TableEditorTab(item.schema(), item.name(), metaService, queryExecutor);
        }
        return null;
    }
}