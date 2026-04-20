package game.of.life.on.gpu;

import game.of.life.on.gpu.engine.DualGrid;
import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.SimulationRules;
import game.of.life.on.gpu.gpu.GPUBackend;
import game.of.life.on.gpu.ui.DualLabController;
import game.of.life.on.gpu.ui.GPULabController;
import game.of.life.on.gpu.ui.MenuController;
import game.of.life.on.gpu.ui.ScreenManager;
import game.of.life.on.gpu.ui.SimulationController;
import game.of.life.on.gpu.ui.TheoryController;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * App V7.0 — JavaFX entry point.
 * Manages all screens, shared game state, GPU backend, and the main game loop.
 */
public class App extends Application {

    // Shared state
    private Grid gameBoard;
    private DualGrid dualBoard;
    private GPUBackend gpuBackend;
    private ScreenManager screenManager;

    // Screens & controllers
    private Parent menuRoot, theoryRoot, simRoot, gpuLabRoot, dualLabRoot;
    private MenuController menuController;
    private TheoryController theoryController;
    private SimulationController simController;
    private GPULabController gpuLabController;
    private DualLabController dualLabController;

    // Currently active screen index (0=Menu, 1=Theory, 2=Sim, 3=Lab, 4=DualLab)
    private int activeScreen = 0;

    // Frame timing
    private long lastFrameNanos = 0;
    private double fps = 60;

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize shared state
        gameBoard  = new Grid(SimulationRules.GRID_SIZE, SimulationRules.GRID_SIZE);
        dualBoard  = new DualGrid(SimulationRules.GRID_SIZE, SimulationRules.GRID_SIZE);
        gpuBackend = new GPUBackend();
        screenManager = new ScreenManager();

        // Load all screens
        loadMenuScreen();
        loadTheoryScreen();
        loadSimulationScreen();
        loadGPULabScreen();
        loadDualLabScreen();

        // Show menu first
        screenManager.showImmediate(menuRoot);

        // Create scene with CSS theme
        Scene scene = new Scene(screenManager.getContainer(), 1280, 720);
        scene.setFill(Color.web("#0A0F19"));
        scene.getStylesheets().add(
            getClass().getResource("/css/dark-theme.css").toExternalForm());

        // Configure stage
        stage.setTitle("GAME OF LIFE V7.0 — DASHBOARD");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();

        // Main game loop
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos == 0) { lastFrameNanos = now; return; }
                double dt = (now - lastFrameNanos) / 1_000_000_000.0;
                dt = Math.max(0.001, Math.min(dt, 0.1));
                lastFrameNanos = now;
                fps = fps * 0.95 + (1.0 / dt) * 0.05;

                // Update active screen only
                switch (activeScreen) {
                    case 0 -> menuController.update(dt, fps);
                    case 2 -> simController.update(dt, fps);
                    case 3 -> gpuLabController.update(dt, fps);
                    case 4 -> dualLabController.update(dt, fps);
                    // Theory screen (1) is static — no per-frame updates needed
                }
            }
        }.start();
    }

    // ── Screen Loading ────────────────────────────────────

    private void loadMenuScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuScreen.fxml"));
        menuRoot = loader.load();
        menuController = loader.getController();
        menuController.init(this);
    }

    private void loadTheoryScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TheoryScreen.fxml"));
        theoryRoot = loader.load();
        theoryController = loader.getController();
        theoryController.init(this);
    }

    private void loadSimulationScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SimulationScreen.fxml"));
        simRoot = loader.load();
        simController = loader.getController();
        simController.init(this);
    }

    private void loadGPULabScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GPULabScreen.fxml"));
        gpuLabRoot = loader.load();
        gpuLabController = loader.getController();
        gpuLabController.init(this);
    }

    private void loadDualLabScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DualLabScreen.fxml"));
        dualLabRoot = loader.load();
        dualLabController = loader.getController();
        dualLabController.init(this);
    }

    // ── Navigation ────────────────────────────────────────

    /** Navigate to screen by index: 0=Menu, 1=Theory, 2=Sim, 3=Lab, 4=DualLab */
    public void navigateTo(int screen) {
        if (screenManager.isTransitioning()) return;
        activeScreen = screen;
        Parent target = switch (screen) {
            case 0 -> menuRoot;
            case 1 -> theoryRoot;
            case 2 -> simRoot;
            case 3 -> gpuLabRoot;
            case 4 -> dualLabRoot;
            default -> menuRoot;
        };
        screenManager.transitionTo(target);
    }

    // ── Shared State Accessors ────────────────────────────

    public Grid getGameBoard()     { return gameBoard; }
    public DualGrid getDualBoard() { return dualBoard; }
    public GPUBackend getGPUBackend() { return gpuBackend; }

    public static void main(String[] args) {
        launch(args);
    }
}
