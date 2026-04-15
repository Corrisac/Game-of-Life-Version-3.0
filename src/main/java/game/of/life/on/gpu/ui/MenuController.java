package game.of.life.on.gpu.ui;

import game.of.life.on.gpu.App;
import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.PatternLibrary;
import game.of.life.on.gpu.engine.SimulationRules;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * MenuController — Drives the animated menu screen.
 * Renders background grid + floating particles on two Canvas layers.
 */
public class MenuController {

    @FXML private Canvas gridCanvas;
    @FXML private Canvas particleCanvas;
    @FXML private Label fpsLabel;

    private App app;
    private Grid menuGrid;
    private int frameCount = 0;

    // Particle system (lightweight — no separate class needed)
    private static final int PARTICLE_COUNT = 80;
    private final float[] ptX = new float[PARTICLE_COUNT];
    private final float[] ptY = new float[PARTICLE_COUNT];
    private final float[] ptVx = new float[PARTICLE_COUNT];
    private final float[] ptVy = new float[PARTICLE_COUNT];
    private final float[] ptAlpha = new float[PARTICLE_COUNT];
    private final Random rng = new Random();

    public void init(App app) {
        this.app = app;

        // Create a dedicated menu grid for the animated background
        menuGrid = new Grid(SimulationRules.GRID_SIZE, SimulationRules.GRID_SIZE);
        int seedCount = (SimulationRules.GRID_SIZE * SimulationRules.GRID_SIZE) / 12;
        for (int i = 0; i < seedCount; i++) {
            menuGrid.setCellState(
                5 + rng.nextInt(SimulationRules.GRID_SIZE - 10),
                5 + rng.nextInt(SimulationRules.GRID_SIZE - 10), true);
        }

        // Initialize particles
        for (int i = 0; i < PARTICLE_COUNT; i++) resetParticle(i, true);
    }

    /** Called every frame by the AnimationTimer. */
    public void update(double dt, double fps) {
        frameCount++;

        double w = gridCanvas.getScene().getWidth();
        double h = gridCanvas.getScene().getHeight();

        // Resize canvases to match window
        gridCanvas.setWidth(w);
        gridCanvas.setHeight(h);
        particleCanvas.setWidth(w);
        particleCanvas.setHeight(h);

        // Evolve background grid
        if (frameCount % 120 == 0) PatternLibrary.spawnRandomPattern(menuGrid);
        if (frameCount % 10 == 0) menuGrid.updateToNextGeneration();

        // Draw background grid
        drawGrid(w, h);

        // Draw particles
        drawParticles(dt, w, h);

        // Update FPS label
        fpsLabel.setText(String.format("JavaFX 21  ·  JOGL 2.5  ·  GLSL  ·  %.0f FPS", fps));
    }

    private void drawGrid(double w, double h) {
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int gs = SimulationRules.GRID_SIZE;
        double displaySize = Math.max(w, h);
        double ox = (w - displaySize) / 2;
        double oy = (h - displaySize) / 2;
        double cellSize = displaySize / gs;

        gc.setGlobalAlpha(0.15);
        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                if (menuGrid.getCellState(r, c)) {
                    gc.setFill(Color.web("#00FFE0"));
                    gc.fillRect(ox + c * cellSize, oy + r * cellSize, cellSize, cellSize);
                }
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    private void drawParticles(double dt, double w, double h) {
        GraphicsContext gc = particleCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        float timeScale = (float)(dt * 60.0);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            ptX[i] += ptVx[i] * timeScale;
            ptY[i] += ptVy[i] * timeScale;
            ptAlpha[i] -= 0.15f * timeScale;

            if (ptAlpha[i] <= 0 || ptX[i] < -10 || ptX[i] > w + 10
                                || ptY[i] < -10 || ptY[i] > h + 10) {
                resetParticle(i, false);
                ptX[i] = rng.nextFloat() * (float) w;
                ptY[i] = rng.nextFloat() * (float) h;
            }

            float pulse = (float) Math.sin(frameCount * 0.04 + i) * 8;
            double alpha = Math.max(0, Math.min(1, (ptAlpha[i] + pulse) / 255.0));
            gc.setFill(Color.web("#00FFE0", alpha));
            gc.fillOval(ptX[i] - 1.25, ptY[i] - 1.25, 2.5, 2.5);
        }
    }

    private void resetParticle(int i, boolean initial) {
        ptVx[i] = rng.nextFloat() * 0.6f - 0.3f;
        ptVy[i] = -(rng.nextFloat() * 0.4f + 0.1f);
        ptAlpha[i] = rng.nextFloat() * 40 + 20;
        if (initial) {
            ptX[i] = rng.nextFloat() * 1280;
            ptY[i] = rng.nextFloat() * 720;
        }
    }

    // ── Navigation ────────────────────────────────────────────

    @FXML private void goSimulation() { app.navigateTo(2); }
    @FXML private void goTheory()     { app.navigateTo(1); }
    @FXML private void goGPULab()     { app.navigateTo(3); }
}
