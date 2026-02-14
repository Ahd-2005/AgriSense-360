package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    @FXML private GridPane mainRootPane;
    @FXML private VBox sidebar;
    @FXML private StackPane contentStack;

    private Node animalsView;
    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_WIDTH_EXPANDED = 260.0;
    private static final double SIDEBAR_WIDTH_COLLAPSED = 0.0;

    @FXML
    private void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebar.widthProperty());
        clip.heightProperty().bind(sidebar.heightProperty());
        sidebar.setClip(clip);
        sidebar.setMinWidth(0);
        ColumnConstraints col = mainRootPane.getColumnConstraints().get(0);
        col.setMaxWidth(SIDEBAR_WIDTH_EXPANDED);
    }

    @FXML
    private void navigateAnimals() {
        try {
            if (animalsView == null) {
                animalsView = FXMLLoader.load(getClass().getResource("/animals-management-view.fxml"));
            }
            if (!contentStack.getChildren().contains(animalsView)) {
                contentStack.getChildren().add(animalsView);
            }
            contentStack.getChildren().get(0).setVisible(false);
            animalsView.setVisible(true);
        } catch (IOException e) {
            throw new RuntimeException("Could not load Animals Management view", e);
        }
    }

    @FXML
    private void navigateEquipment() {
        showHome();
    }

    @FXML
    private void navigateStock() {
        showHome();
    }

    @FXML
    private void navigateCulture() {
        showHome();
    }

    @FXML
    private void navigateUsers() {
        showHome();
    }

    @FXML
    private void navigateWorkers() {
        showHome();
    }

    private void showHome() {
        if (contentStack.getChildren().isEmpty()) return;
        contentStack.getChildren().get(0).setVisible(true);
        if (contentStack.getChildren().size() > 1) {
            contentStack.getChildren().get(1).setVisible(false);
        }
    }

    @FXML
    private void toggleSidebar() {
        ColumnConstraints col = mainRootPane.getColumnConstraints().get(0);
        double targetWidth = sidebarExpanded ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH_EXPANDED;
        col.setMinWidth(0);
        if (sidebarExpanded) {
            col.setMaxWidth(SIDEBAR_WIDTH_EXPANDED);
        } else {
            sidebar.setVisible(true);
        }
        KeyValue kvPref = new KeyValue(col.prefWidthProperty(), targetWidth);
        KeyValue kvMin = new KeyValue(col.minWidthProperty(), targetWidth);
        KeyValue kvMax = new KeyValue(col.maxWidthProperty(), targetWidth);
        KeyFrame kf = new KeyFrame(Duration.millis(280), kvPref, kvMin, kvMax);
        Timeline timeline = new Timeline(kf);
        timeline.setOnFinished(e -> {
            sidebarExpanded = !sidebarExpanded;
            if (sidebarExpanded) {
                col.setMaxWidth(SIDEBAR_WIDTH_EXPANDED);
            } else {
                col.setMaxWidth(SIDEBAR_WIDTH_COLLAPSED);
                sidebar.setVisible(false);
            }
        });
        timeline.play();
    }
}
