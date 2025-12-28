// File: pgdeveloper/ide-ui/src/main/java/ro/fintechpro/ui/plugins/ProcedurePlugin.java
package ro.fintechpro.ui.plugins;

import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.LocalIndexService.SearchResult;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.spi.SidebarPlugin;

import java.util.ArrayList;
import java.util.List;

public class ProcedurePlugin implements SidebarPlugin {

    @Override
    public TreeItem<String> createNode(String schema, MetadataService metaService) {
        try {
            var procs = metaService.getProcedures(schema);
            if (procs.isEmpty()) return null;

            // Root: CPU Icon (Processing)
            FontIcon rootIcon = new FontIcon(Feather.CPU);
            rootIcon.setIconColor(Color.web("#C678DD")); // Purple (Logic)

            TreeItem<String> root = new TreeItem<>("Procedures", rootIcon);

            for (String p : procs) {
                // Item: Settings/Gear Icon
                FontIcon icon = new FontIcon(Feather.SETTINGS);
                icon.setIconColor(Color.web("#C678DD")); // Purple
                root.getChildren().add(new TreeItem<>(p, icon));
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
}