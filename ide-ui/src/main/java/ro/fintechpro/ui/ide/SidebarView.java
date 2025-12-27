package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.spi.SidebarPlugin;

import java.util.ArrayList;
import java.util.List;

public class SidebarView extends VBox {

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;
    private final TextField searchField;

    // Injected Plugins
    private final List<SidebarPlugin> plugins;

    // Backup list to restore tree after clearing search
    private final List<TreeItem<String>> originalStructure = new ArrayList<>();

    public SidebarView(List<SidebarPlugin> plugins) {
        this.plugins = plugins;

        this.setSpacing(5);
        this.setPadding(new Insets(5));
        this.getStyleClass().add("sidebar-container");

        // --- 1. Header (Title + Actions) ---
        Label title = new Label("Explorer");
        title.getStyleClass().add(Styles.TEXT_BOLD);

        Button refreshBtn = new Button(null, new FontIcon(Feather.REFRESH_CW));
        refreshBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        refreshBtn.setTooltip(new Tooltip("Reload Structure"));

        Button collapseBtn = new Button(null, new FontIcon(Feather.MINUS_SQUARE));
        collapseBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        collapseBtn.setTooltip(new Tooltip("Collapse All"));
        collapseBtn.setOnAction(e -> collapseAll());

        HBox header = new HBox(10, title, new HBox(refreshBtn, collapseBtn));
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        ((HBox)header.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 5, 5, 5));

        // --- 2. Search Bar ---
        searchField = new TextField();
        searchField.setPromptText("Search objects...");
        searchField.getStyleClass().add(Styles.SMALL);

        // --- 3. The Tree ---
        rootItem = new TreeItem<>("Database", new FontIcon(Feather.DATABASE));
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.getStyleClass().add(Styles.DENSE);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Custom Cell Factory
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(getTreeItem().getGraphic());
                    if (getTreeItem().getParent() == null) {
                        getStyleClass().add(Styles.TEXT_BOLD);
                    }
                }
            }
        });

        this.getChildren().addAll(header, searchField, treeView);
    }

    public void populate(MetadataService metaService) {
        Platform.runLater(() -> {
            rootItem.getChildren().clear();
            originalStructure.clear(); // Clear backup

            try {
                List<String> schemas = metaService.getSchemas();

                for (String schema : schemas) {
                    TreeItem<String> schemaItem = new TreeItem<>(schema, new FontIcon(Feather.FOLDER));

                    // --- DYNAMIC PLUGIN LOADING ---
                    for (SidebarPlugin plugin : plugins) {
                        // Ask the plugin to create its node (e.g., "Tables")
                        TreeItem<String> pluginNode = plugin.createNode(schema, metaService);

                        // If the plugin has content, add it to the schema folder
                        if (pluginNode != null) {
                            schemaItem.getChildren().add(pluginNode);
                        }
                    }

                    // Save to backup list
                    originalStructure.add(schemaItem);
                }

                // Add backup items to the actual tree
                rootItem.getChildren().setAll(originalStructure);

            } catch (Exception e) {
                TreeItem<String> errorItem = new TreeItem<>("Error: " + e.getMessage(), new FontIcon(Feather.ALERT_TRIANGLE));
                rootItem.getChildren().add(errorItem);
                e.printStackTrace();
            }
        });
    }

    public void setupSearch(LocalIndexService indexService) {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // IF SEARCH IS EMPTY -> RESTORE BACKUP
            if (newVal == null || newVal.trim().isEmpty()) {
                rootItem.getChildren().setAll(originalStructure);
                return;
            }

            // IF SEARCH HAS TEXT -> QUERY INDEX
            List<LocalIndexService.SearchResult> results = indexService.search(newVal);

            rootItem.getChildren().clear();

            if (results.isEmpty()) {
                rootItem.getChildren().add(new TreeItem<>("No results found"));
            } else {
                TreeItem<String> searchRoot = new TreeItem<>("Search Results (" + results.size() + ")");
                searchRoot.setExpanded(true);

                for (var res : results) {
                    // Choose icon based on type (simple switch for visual clarity)
                    FontIcon icon = switch (res.type()) {
                        case "TABLE" -> new FontIcon(Feather.LAYOUT);
                        case "FUNCTION" -> new FontIcon(Feather.ACTIVITY);
                        case "PROCEDURE" -> new FontIcon(Feather.PLAY_CIRCLE);
                        default -> new FontIcon(Feather.CIRCLE);
                    };

                    String label = res.schema() + "." + res.name();
                    TreeItem<String> item = new TreeItem<>(label, icon);
                    searchRoot.getChildren().add(item);
                }
                rootItem.getChildren().add(searchRoot);
            }
        });
    }

    private void collapseAll() {
        if (rootItem != null && !rootItem.getChildren().isEmpty()) {
            for (TreeItem<?> child : rootItem.getChildren()) {
                child.setExpanded(false);
            }
        }
    }

    public Button getRefreshButton() {
        HBox header = (HBox) getChildren().get(0);
        HBox buttons = (HBox) header.getChildren().get(1);
        return (Button) buttons.getChildren().get(0);
    }
}