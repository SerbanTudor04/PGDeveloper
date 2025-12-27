package ro.fintechpro.launcher;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ro.fintechpro.ui.ConnectionManagerView;


public class IdeApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
//        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        ConnectionManagerView manager = new ConnectionManagerView();

        Scene scene = new Scene(manager.getView(primaryStage), 600, 400);

        primaryStage.setTitle("PgDeveloper - Connection Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}