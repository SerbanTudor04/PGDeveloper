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

public class TablePlugin implements SidebarPlugin {

    @Override
    public TreeItem<String> createNode(String schema, MetadataService metaService) {
        try {
            var tables = metaService.getTables(schema);
            if (tables.isEmpty()) return null;

            // Root: GRID icon (Visualizes a collection of data grids)
            FontIcon rootIcon = new FontIcon(Feather.GRID);
            rootIcon.setIconColor(Color.web("#61AFEF")); // Blue

            TreeItem<String> root = new TreeItem<>("Tables (" + tables.size() + ")", rootIcon);

            for (var t : tables) {
                // Item: LAYOUT icon (Visualizes the structure of a single table)
                FontIcon icon = new FontIcon(Feather.LAYOUT);
                icon.setIconColor(Color.web("#61AFEF")); // Blue
                root.getChildren().add(new TreeItem<>(t.name(), icon));
            }
            return root;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<SearchResult> getIndexItems(String schema, MetadataService metaService) {
        List<SearchResult> results = new ArrayList<>();
        try {
            for (var t : metaService.getTables(schema)) {
                results.add(new SearchResult(t.name(), "TABLE", schema, null));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return results;
    }
}