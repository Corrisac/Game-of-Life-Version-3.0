package game.of.life.on.gpu.ui;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * ScreenManager — Manages screen switching with smooth fade transitions.
 * Wraps a StackPane and handles fade-out → swap → fade-in animation.
 */
public class ScreenManager {

    private final StackPane container;
    private Node currentScreen;
    private boolean transitioning = false;

    private static final Duration FADE_DURATION = Duration.millis(250);

    public ScreenManager() {
        container = new StackPane();
        container.setStyle("-fx-background-color: #0A0F19;");
    }

    /** Returns the root StackPane for embedding in the scene. */
    public StackPane getContainer() {
        return container;
    }

    /** Shows a screen immediately (no transition). Used for initial screen. */
    public void showImmediate(Node screen) {
        container.getChildren().setAll(screen);
        currentScreen = screen;
    }

    /** Transitions to a new screen with a fade-out/fade-in animation. */
    public void transitionTo(Node newScreen) {
        if (transitioning) return;
        transitioning = true;

        if (currentScreen == null) {
            showImmediate(newScreen);
            transitioning = false;
            return;
        }

        // Phase 1: Fade out current screen
        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, currentScreen);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            // Phase 2: Swap content
            container.getChildren().setAll(newScreen);
            newScreen.setOpacity(0);
            currentScreen = newScreen;

            // Phase 3: Fade in new screen
            FadeTransition fadeIn = new FadeTransition(FADE_DURATION, newScreen);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(e2 -> transitioning = false);
            fadeIn.play();
        });
        fadeOut.play();
    }

    public boolean isTransitioning() {
        return transitioning;
    }
}
