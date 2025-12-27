package ro.fintechpro.core.spi;

import javafx.scene.control.TreeItem;
import ro.fintechpro.core.service.LocalIndexService.SearchResult;
import ro.fintechpro.core.service.MetadataService;

import java.util.List;

public interface SidebarPlugin {

    /**
     * Creates the UI node for this plugin (e.g., the "Tables" folder).
     * @param schema The database schema we are currently processing.
     * @param metaService Access to the database metadata.
     * @return The root TreeItem for this plugin, or null if no items found.
     */
    TreeItem<String> createNode(String schema, MetadataService metaService);

    /**
     * Returns a list of items to add to the global Search Index (H2).
     */
    List<SearchResult> getIndexItems(String schema, MetadataService metaService);
}