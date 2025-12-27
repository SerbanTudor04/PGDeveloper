package ro.fintechpro.launcher;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ro.fintechpro.ui.ConnectionManagerView;
import ro.fintechpro.ui.MainIdeView;
import ro.fintechpro.ui.LoadingView;


public class IdeApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        showConnectionManager();
        primaryStage.setTitle("PgDeveloper");
        primaryStage.show();
    }

    public void showConnectionManager() {
        // Pass 'this' so the manager can call showLoading()
        ConnectionManagerView connectionView = new ConnectionManagerView(this::showLoadingScreen);
        primaryStage.setScene(new Scene(connectionView.getView(), 900, 650));
        primaryStage.centerOnScreen();
    }

    /**
     * TRANSITION LOGIC:
     * 1. Show Loading Screen
     * 2. Initialize IDE in background (simulated or real heavy lifting)
     * 3. Switch to IDE
     */
    public void showLoadingScreen() {
        LoadingView loadingView = new LoadingView();
        primaryStage.setScene(new Scene(loadingView.getView(), 1200, 800));
        primaryStage.centerOnScreen();

        // Run heavy initialization on a background thread
        Task<Parent> initTask = new Task<>() {
            @Override
            protected javafx.scene.Parent call() throws Exception {
                updateMessage("Loading workspace configuration...");
                Thread.sleep(300); // Tiny pause for visual smoothness

                updateMessage("Connecting to database...");
                // Note: We construct MainIdeView here.
                // Since MainIdeView creates JavaFX nodes, we usually need to be careful.
                // However, constructing nodes *off-scene* is often safe, or we use Platform.runLater inside.
                // For safety, we will do the heavy Logic here, but create the View in 'succeeded'.

                // If you want to force the UI construction to happen later:
                return null;
            }
        };

        // UI Updates must be on JavaFX Thread
        initTask.messageProperty().addListener((obs, old, msg) -> loadingView.updateMessage(msg));

        initTask.setOnSucceeded(e -> {
            // NOW we build the actual IDE UI on the JavaFX Thread
            // This prevents "Not on FX application thread" errors
            loadingView.updateMessage("Building User Interface...");

            // Allow the text to render before freezing for UI build
            Platform.runLater(() -> {
                MainIdeView ide = new MainIdeView();
                primaryStage.setScene(new Scene(ide.getView(), 1200, 800));
                primaryStage.centerOnScreen();
                primaryStage.setMaximized(true);
            });
        });

        new Thread(initTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}