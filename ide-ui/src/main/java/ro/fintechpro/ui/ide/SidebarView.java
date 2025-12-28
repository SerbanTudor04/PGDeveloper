package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color; // Import Color
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.model.SidebarItem;
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.spi.SidebarPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SidebarView extends VBox {

    private final TreeView<SidebarItem> treeView;
    private final TreeItem<SidebarItem> rootItem;
    private final TextField searchField;
    private final List<SidebarPlugin> plugins;
    private final List<TreeItem<SidebarItem>> originalStructure = new ArrayList<>();
    private Consumer<SidebarItem> onItemOpen;

    public SidebarView(List<SidebarPlugin> plugins) {
        this.plugins = plugins;

        this.setSpacing(5);
        this.setPadding(new Insets(5));
        this.getStyleClass().add("sidebar-container");

        // --- 1. Header ---
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

        // --- 2. Search ---
        searchField = new TextField();
        searchField.setPromptText("Search objects...");
        searchField.getStyleClass().add(Styles.SMALL);

        // --- 3. Tree ---
        FontIcon dbIcon = new FontIcon(Feather.DATABASE);
        // FIX: Use setIconColor instead of setStyle
        dbIcon.setIconColor(Color.web("#E06C75")); // Red/Pink

        SidebarItem rootData = new SidebarItem("Database", SidebarItem.TYPE_ROOT, null, null);
        rootItem = new TreeItem<>(rootData, dbIcon);
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.getStyleClass().add(Styles.DENSE);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Cell Factory
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(SidebarItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.label());
                    setGraphic(getTreeItem().getGraphic());

                    if (getTreeItem().getParent() == rootItem) {
                        getStyleClass().add(Styles.TEXT_BOLD);
                    } else {
                        getStyleClass().remove(Styles.TEXT_BOLD);
                    }
                }
            }
        });

        // Double Click Handler
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<SidebarItem> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null && onItemOpen != null) {
                    onItemOpen.accept(selected.getValue());
                }
            }
        });

        this.getChildren().addAll(header, searchField, treeView);
    }

    public void setOnItemOpen(Consumer<SidebarItem> listener) {
        this.onItemOpen = listener;
    }

    public void populate(MetadataService metaService) {
        Platform.runLater(() -> {
            rootItem.getChildren().clear();
            originalStructure.clear();

            try {
                List<String> schemas = metaService.getSchemas();

                for (String schema : schemas) {
                    FontIcon schemaIcon = new FontIcon(Feather.LAYERS);
                    // FIX: Use setIconColor for Schemas (Gold)
                    schemaIcon.setIconColor(Color.web("#E5C07B"));

                    SidebarItem schemaData = new SidebarItem(schema, SidebarItem.TYPE_SCHEMA, schema, null);
                    TreeItem<SidebarItem> schemaItem = new TreeItem<>(schemaData, schemaIcon);

                    for (SidebarPlugin plugin : plugins) {
                        TreeItem<SidebarItem> pluginNode = plugin.createNode(schema, metaService);
                        if (pluginNode != null) {
                            schemaItem.getChildren().add(pluginNode);
                        }
                    }
                    originalStructure.add(schemaItem);
                }
                rootItem.getChildren().setAll(originalStructure);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setupSearch(LocalIndexService indexService) {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                rootItem.getChildren().setAll(originalStructure);
                return;
            }

            List<LocalIndexService.SearchResult> results = indexService.search(newVal);
            rootItem.getChildren().clear();

            if (results.isEmpty()) {
                FontIcon infoIcon = new FontIcon(Feather.INFO);
                infoIcon.setIconColor(Color.GRAY);
                rootItem.getChildren().add(new TreeItem<>(new SidebarItem("No results", "INFO", null, null), infoIcon));
            } else {
                FontIcon searchIcon = new FontIcon(Feather.SEARCH);
                searchIcon.setIconColor(Color.web("#E06C75"));
                TreeItem<SidebarItem> searchRoot = new TreeItem<>(new SidebarItem("Results", "ROOT", null, null), searchIcon);
                searchRoot.setExpanded(true);

                for (var res : results) {
                    FontIcon icon;
                    // FIX: Use setIconColor for Search Results
                    switch (res.type()) {
                        case "TABLE" -> {
                            icon = new FontIcon(Feather.LAYOUT);
                            icon.setIconColor(Color.web("#5263e3")); // Blue
                        }
                        case "FUNCTION" -> {
                            icon = new FontIcon(Feather.PLAY_CIRCLE);
                            icon.setIconColor(Color.web("#C678DD")); // Purple
                        }
                        case "PROCEDURE" -> {
                            icon = new FontIcon(Feather.CPU);
                            icon.setIconColor(Color.web("#C678DD")); // Purple
                        }
                        default -> {
                            icon = new FontIcon(Feather.CIRCLE);
                            icon.setIconColor(Color.GRAY);
                        }
                    }

                    SidebarItem itemData = new SidebarItem(res.schema() + "." + res.name(), res.type(), res.schema(), res.name());
                    searchRoot.getChildren().add(new TreeItem<>(itemData, icon));
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

    public void setOnRefresh(Consumer<Runnable> refreshAction) {
        HBox header = (HBox) getChildren().get(0);
        HBox buttons = (HBox) header.getChildren().get(1);
        Button refreshBtn = (Button) buttons.getChildren().get(0);

        refreshBtn.setOnAction(e -> {
            var originalIcon = refreshBtn.getGraphic();
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(16, 16);
            refreshBtn.setGraphic(spinner);
            refreshBtn.setDisable(true);

            refreshAction.accept(() -> {
                refreshBtn.setGraphic(originalIcon);
                refreshBtn.setDisable(false);
            });
        });
    }
}