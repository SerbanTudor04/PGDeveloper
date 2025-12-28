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
import ro.fintechpro.core.service.LocalIndexService;
import ro.fintechpro.core.service.MetadataService;
import ro.fintechpro.core.spi.SidebarPlugin;

import java.util.ArrayList;
import java.util.List;

public class SidebarView extends VBox {

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;
    private final TextField searchField;
    private final List<SidebarPlugin> plugins;
    private final List<TreeItem<String>> originalStructure = new ArrayList<>();

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
        header.setPadding(new Insets(0, 5, 5, 5));

        // --- 2. Search ---
        searchField = new TextField();
        searchField.setPromptText("Search objects...");
        searchField.getStyleClass().add(Styles.SMALL);

        // --- 3. Tree ---
        // Root Icon (Database)
        FontIcon dbIcon = new FontIcon(Feather.DATABASE);
        dbIcon.setIconColor(Color.web("#E06C75")); // Red/Pink
        rootItem = new TreeItem<>("Database", dbIcon);
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.getStyleClass().add(Styles.DENSE);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Simple Cell Factory
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
                    // Bold schemas or roots
                    if (getTreeItem().getParent() == rootItem) {
                        getStyleClass().add(Styles.TEXT_BOLD);
                    } else {
                        getStyleClass().remove(Styles.TEXT_BOLD);
                    }
                }
            }
        });

        this.getChildren().addAll(header, searchField, treeView);
    }

    public void populate(MetadataService metaService) {
        Platform.runLater(() -> {
            rootItem.getChildren().clear();
            originalStructure.clear();

            try {
                List<String> schemas = metaService.getSchemas();

                for (String schema : schemas) {
                    // NEW: Schema Icon (Layers) + Gold Color
                    FontIcon schemaIcon = new FontIcon(Feather.LAYERS);
                    schemaIcon.setIconColor(Color.web("#E5C07B")); // Gold
                    TreeItem<String> schemaItem = new TreeItem<>(schema, schemaIcon);

                    for (SidebarPlugin plugin : plugins) {
                        TreeItem<String> pluginNode = plugin.createNode(schema, metaService);
                        if (pluginNode != null) {
                            schemaItem.getChildren().add(pluginNode);
                        }
                    }
                    originalStructure.add(schemaItem);
                }
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
            if (newVal == null || newVal.trim().isEmpty()) {
                rootItem.getChildren().setAll(originalStructure);
                return;
            }

            List<LocalIndexService.SearchResult> results = indexService.search(newVal);
            rootItem.getChildren().clear();

            if (results.isEmpty()) {
                rootItem.getChildren().add(new TreeItem<>("No results found"));
            } else {
                TreeItem<String> searchRoot = new TreeItem<>("Search Results (" + results.size() + ")");
                searchRoot.setExpanded(true);

                for (var res : results) {
                    FontIcon icon;
                    // UPDATED: Icons for Search Results
                    switch (res.type()) {
                        case "TABLE" -> {
                            icon = new FontIcon(Feather.LAYOUT);
                            icon.setIconColor(Color.web("#61AFEF")); // Blue
                        }
                        case "FUNCTION" -> {
                            icon = new FontIcon(Feather.PLAY_CIRCLE);
                            icon.setIconColor(Color.web("#C678DD")); // Purple
                        }
                        case "PROCEDURE" -> {
                            icon = new FontIcon(Feather.CPU);
                            icon.setIconColor(Color.web("#C678DD")); // Purple
                        }
                        default -> icon = new FontIcon(Feather.CIRCLE);
                    }

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

    public void setOnRefresh(java.util.function.Consumer<Runnable> refreshAction) {
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