package ro.fintechpro.core.spi;

import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import ro.fintechpro.core.model.SidebarItem;
import ro.fintechpro.core.service.LocalIndexService.SearchResult;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.service.QueryExecutor;

import java.util.List;

public interface SidebarPlugin {

    // CHANGED: Returns TreeItem<SidebarItem> instead of TreeItem<String>
    TreeItem<SidebarItem> createNode(String schema, MetadataService metaService);

    List<SearchResult> getIndexItems(String schema, MetadataService metaService);

    default Tab createTab(SidebarItem item, MetadataService metaService, QueryExecutor queryExecutor) {
        return null;
    }
}