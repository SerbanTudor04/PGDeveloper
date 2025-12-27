package ro.fintechpro.ui;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class MainIdeView {
    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setCenter(new Label("Welcome to the IDE Mode! Connection Active."));
        return root;
    }
}