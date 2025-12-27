package ro.fintechpro.ui.plugins;

import javafx.scene.control.TreeItem;
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

            TreeItem<String> root = new TreeItem<>("Functions", new FontIcon(Feather.ACTIVITY));
            for (String f : funcs) {
                root.getChildren().add(new TreeItem<>(f, new FontIcon(Feather.CODE)));
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