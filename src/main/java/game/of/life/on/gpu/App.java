package game.of.life.on.gpu;

import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.SimulationRules;
import game.of.life.on.gpu.ui.MenuController;
import game.of.life.on.gpu.ui.ScreenManager;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * App V7.0 — JavaFX entry point.
 * Replaces the Processing PApplet with a proper JavaFX Application.
 */
public class App extends Application {

    // Shared game state
    private Grid gameBoard;
    private ScreenManager screenManager;

    // Screen controllers (initialized lazily)
    private MenuController menuController;
    private Parent menuRoot;

    // Frame timing
    private long lastFrameNanos = 0;
    private double fps = 60;
    private int frameCount = 0;

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize game state
        gameBoard = new Grid(SimulationRules.GRID_SIZE, SimulationRules.GRID_SIZE);

        // Initialize screen manager
        screenManager = new ScreenManager();

        // Load menu screen
        FXMLLoader menuLoader = new FXMLLoader(
            getClass().getResource("/fxml/MenuScreen.fxml"));
        menuRoot = menuLoader.load();
        menuController = menuLoader.getController();
        menuController.init(this);

        // Show initial screen
        screenManager.showImmediate(menuRoot);

        // Create scene
        Scene scene = new Scene(screenManager.getContainer(), 1280, 720);
        scene.setFill(Color.web("#0A0F19"));

        // Apply CSS theme
        scene.getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm());

        // Configure stage
        stage.setTitle("GAME OF LIFE V7.0 — DASHBOARD");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();

        // Start game loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos == 0) {
                    lastFrameNanos = now;
                    return;
                }

                double dt = (now - lastFrameNanos) / 1_000_000_000.0;
                dt = Math.max(0.001, Math.min(dt, 0.1));  // Clamp 1ms..100ms
                lastFrameNanos = now;
                frameCount++;

                // Smooth FPS calculation
                fps = fps * 0.95 + (1.0 / dt) * 0.05;

                // Update active screen
                if (menuController != null) {
                    menuController.update(dt, fps);
                }
            }
        }.start();
    }

    /** Navigate to a screen by index (0=Menu, 1=Theory, 2=Sim, 3=Lab). */
    public void navigateTo(int screen) {
        if (screenManager.isTransitioning()) return;

        switch (screen) {
            case 0 -> screenManager.transitionTo(menuRoot);
            // TODO: Phases 4-6 will add Theory, Simulation, GPULab screens
            default -> System.out.println("[App] Screen " + screen + " not yet implemented");
        }
    }

    public Grid getGameBoard() { return gameBoard; }

    public static void main(String[] args) {
        launch(args);
    }
}
