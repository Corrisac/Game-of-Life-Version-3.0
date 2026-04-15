package game.of.life.on.gpu.ui;

import game.of.life.on.gpu.App;
import javafx.fxml.FXML;

/**
 * TheoryController — Minimal controller for the theory/history screen.
 * All content is declarative in the FXML. ScrollPane handles scrolling natively.
 */
public class TheoryController {

    private App app;

    public void init(App app) {
        this.app = app;
    }

    @FXML
    private void goBack() {
        app.navigateTo(0);
    }
}
