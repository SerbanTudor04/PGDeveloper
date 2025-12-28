package ro.fintechpro.launcher;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ro.fintechpro.ui.ConnectionManagerView;
import ro.fintechpro.ui.LoadingView;
import ro.fintechpro.ui.MainIdeView;
import ro.fintechpro.ui.util.MacWindowStyler;
import ro.fintechpro.ui.util.ResizeHelper;

public class IdeApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // 1. Configure the Primary Stage Style (Unified/Undecorated)
        configureStage(primaryStage);

        showConnectionManager();
        primaryStage.setTitle("PgDeveloper");
        primaryStage.show();
    }

    private void configureStage(Stage stage) {
        // Apply Mac styling (Transparency)
        // We use UNIFIED for Mac to allow the traffic lights to blend if supported,
        // otherwise UNDECORATED for Windows to remove the default bar.
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            stage.initStyle(StageStyle.UNIFIED);
            MacWindowStyler.makeTitleBarTransparent(stage);
        } else {
            stage.initStyle(StageStyle.UNDECORATED);
            ResizeHelper.addResizeListener(stage);
        }
    }

    public void showConnectionManager() {
        // Pass 'primaryStage' to the view so it can attach the TitleBar
        ConnectionManagerView connectionView = new ConnectionManagerView(this::showLoadingScreen);
        primaryStage.setScene(new Scene(connectionView.getView(primaryStage), 900, 650));
        primaryStage.centerOnScreen();
    }

    public void showLoadingScreen() {
        LoadingView loadingView = new LoadingView();
        // Pass 'primaryStage' to the view
        primaryStage.setScene(new Scene(loadingView.getView(primaryStage), 1200, 800));
        primaryStage.centerOnScreen();

        Task<MainIdeView> initTask = new Task<>() {
            @Override
            protected MainIdeView call() throws Exception {
                MainIdeView ide = new MainIdeView();
                ide.preload(this::updateMessage);
                return ide;
            }
        };

        initTask.messageProperty().addListener((obs, old, msg) -> loadingView.updateMessage(msg));
        initTask.exceptionProperty().addListener((obs, old, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                loadingView.updateMessage("Error: " + ex.getMessage());
            }
        });

        initTask.setOnSucceeded(e -> {
            loadingView.updateMessage("Building User Interface...");
            MainIdeView readyIde = initTask.getValue();

            Platform.runLater(() -> {
                Stage mainStage = new Stage();
                mainStage.setTitle("PgDeveloper");

                // Configure the new Main Stage exactly like the Primary Stage
                configureStage(mainStage);

                Scene scene = new Scene(readyIde.getView(mainStage), 1200, 800);
                mainStage.setScene(scene);
                mainStage.centerOnScreen();
                mainStage.show();

                primaryStage.close();
            });
        });

        new Thread(initTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}