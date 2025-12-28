// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/plugins/FunctionPlugin.java
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
import ro.fintechpro.ui.ide.RoutineEditorTab;

import java.util.ArrayList;
import java.util.List;

public class FunctionPlugin implements SidebarPlugin {

    @Override
    public TreeItem<SidebarItem> createNode(String schema, MetadataService metaService) {
        try {
            var funcs = metaService.getFunctions(schema);
            if (funcs.isEmpty()) return null;

            FontIcon rootIcon = new FontIcon(Feather.BOX);
            rootIcon.setIconColor(Color.web("#C678DD")); // Purple

            SidebarItem rootData = new SidebarItem("Functions", SidebarItem.TYPE_FOLDER, schema, null);
            TreeItem<SidebarItem> root = new TreeItem<>(rootData, rootIcon);

            for (String f : funcs) {
                FontIcon icon = new FontIcon(Feather.PLAY_CIRCLE);
                icon.setIconColor(Color.web("#C678DD")); // FIX: Apply color to item icon

                SidebarItem itemData = new SidebarItem(f, SidebarItem.TYPE_FUNCTION, schema, f);
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
        if (SidebarItem.TYPE_FUNCTION.equals(item.type())) {
            return new RoutineEditorTab(item.schema(), item.name(), metaService, queryExecutor);
        }
        return null;
    }
}