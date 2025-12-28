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

public class ProcedurePlugin implements SidebarPlugin {

    @Override
    public TreeItem<SidebarItem> createNode(String schema, MetadataService metaService) {
        try {
            var procs = metaService.getProcedures(schema);
            if (procs.isEmpty()) return null;

            // Root: CPU Icon (Processing)
            FontIcon rootIcon = new FontIcon(Feather.CPU);
            rootIcon.setIconColor(Color.web("#C678DD")); // Purple (Logic)

            SidebarItem rootData = new SidebarItem("Procedures", SidebarItem.TYPE_FOLDER, schema, null);
            TreeItem<SidebarItem> root = new TreeItem<>(rootData, rootIcon);

            for (String p : procs) {
                // Item: Settings/Gear Icon
                FontIcon icon = new FontIcon(Feather.SETTINGS);
                icon.setIconColor(Color.web("#C678DD")); // Purple

                SidebarItem itemData = new SidebarItem(p, SidebarItem.TYPE_PROCEDURE, schema, p);
                root.getChildren().add(new TreeItem<>(itemData, icon));
            }
            return root;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<SearchResult> getIndexItems(String schema, MetadataService metaService) {
        List<SearchResult> results = new ArrayList<>();
        try {
            for (String p : metaService.getProcedures(schema)) {
                results.add(new SearchResult(p, "PROCEDURE", schema, null));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public Tab createTab(SidebarItem item, MetadataService metaService, QueryExecutor queryExecutor) {
        // Reuse RoutineEditorTab for Procedures as well
        if (SidebarItem.TYPE_PROCEDURE.equals(item.type())) {
            return new RoutineEditorTab(item.schema(), item.name(), metaService, queryExecutor);
        }
        return null;
    }
}