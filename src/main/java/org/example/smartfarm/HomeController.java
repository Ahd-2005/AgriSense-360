package org.example.smartfarm;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class HomeController {

    private MainLayoutController mainLayout;

    public void setMainLayout(MainLayoutController mainLayout) {
        this.mainLayout = mainLayout;
    }

    @FXML
    private void onAffectations() {
        if (mainLayout != null) mainLayout.showAffectations();
    }

    @FXML
    private void onEvaluations() {
        if (mainLayout != null) mainLayout.showEvaluations();
    }

    @FXML
    private void onAffectationsCard(MouseEvent e) {
        onAffectations();
    }

    @FXML
    private void onEvaluationsCard(MouseEvent e) {
        onEvaluations();
    }
}
