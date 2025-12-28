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

public class FunctionPlugin implements SidebarPlugin {

    @Override
    public TreeItem<String> createNode(String schema, MetadataService metaService) {
        try {
            var funcs = metaService.getFunctions(schema);
            if (funcs.isEmpty()) return null;

            // Root: Box (Package)
            FontIcon rootIcon = new FontIcon(Feather.BOX);
            rootIcon.setIconColor(Color.web("#C678DD")); // Purple

            TreeItem<String> root = new TreeItem<>("Functions", rootIcon);

            for (String f : funcs) {
                // Item: Play Circle
                FontIcon icon = new FontIcon(Feather.PLAY_CIRCLE);
                icon.setIconColor(Color.web("#C678DD")); // Purple
                root.getChildren().add(new TreeItem<>(f, icon));
            }
            return root;
        } catch (Exception e) { return null; }
    }

    @Override
    public List<SearchResult> getIndexItems(String schema, MetadataService metaService) {
        List<SearchResult> results = new ArrayList<>();
        try {
            for (String f : metaService.getFunctions(schema)) {
                results.add(new SearchResult(f, "FUNCTION", schema, null));
            }
        } catch (Exception e) { }
        return results;
    }
}