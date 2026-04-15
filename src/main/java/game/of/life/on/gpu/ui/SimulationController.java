package game.of.life.on.gpu.ui;

import game.of.life.on.gpu.App;
import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.PatternLibrary;
import game.of.life.on.gpu.engine.SimulationRules;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * SimulationController — Handles the interactive simulation screen.
 * Canvas-based grid rendering with zoom/pan, brush drawing, analytics HUD.
 */
public class SimulationController {

    @FXML private Canvas gridCanvas;
    @FXML private Canvas sparklineCanvas;
    @FXML private StackPane gridPane;
    @FXML private Label statsLabel, statusLabel;
    @FXML private Label hudPopLabel, hudPeakLabel, hudBornLabel, hudDiedLabel, hudGpsLabel, hudZoomLabel;
    @FXML private VBox hudOverlay;
    @FXML private Button btnPause, btnBrush, btnRule, btnSpeed, btnHud, btnHeat;

    private App app;
    private Grid board;
    private boolean isPaused = true;
    private int brushSize = 1;
    private int speedLevel = 0;
    private boolean showHeatmap = false;
    private boolean showHUD = false;
    private int frameCount = 0;

    // Zoom & pan
    private double zoomLevel = 1.0;
    private double panX = 0, panY = 0;
    private boolean isDragging = false;
    private double dragStartX, dragStartY, panStartX, panStartY;

    // GPS tracking
    private int stepsSinceLastCalc = 0;
    private long lastGPSCalc = 0;
    private double gensPerSec = 0;

    public void init(App app) {
        this.app = app;
        this.board = app.getGameBoard();

        // Mouse events on the grid canvas
        gridCanvas.setOnMousePressed(this::onMousePressed);
        gridCanvas.setOnMouseDragged(this::onMouseDragged);
        gridCanvas.setOnMouseReleased(e -> isDragging = false);
        gridCanvas.setOnScroll(this::onScroll);
    }

    /** Called every frame by the AnimationTimer in App. */
    public void update(double dt, double fps) {
        frameCount++;

        double w = gridPane.getWidth();
        double h = gridPane.getHeight();
        if (w <= 0 || h <= 0) return;

        gridCanvas.setWidth(w);
        gridCanvas.setHeight(h);

        // Simulation stepping
        if (!isPaused) {
            int stepsPerFrame = SimulationRules.SPEED_LEVELS[speedLevel];
            boolean shouldStep = (stepsPerFrame <= 1 && frameCount % 6 == 0) || stepsPerFrame > 1;
            if (shouldStep) {
                int count = Math.max(1, stepsPerFrame);
                for (int i = 0; i < count; i++) {
                    board.updateToNextGeneration();
                    stepsSinceLastCalc++;
                }
            }
        }

        // GPS calculation
        long now = System.currentTimeMillis();
        if (now - lastGPSCalc > 500) {
            double elapsed = (now - lastGPSCalc) / 1000.0;
            if (elapsed > 0) gensPerSec = stepsSinceLastCalc / elapsed;
            stepsSinceLastCalc = 0;
            lastGPSCalc = now;
        }

        // Draw grid
        drawGrid(w, h);

        // Update stats bar
        String ruleName = SimulationRules.RULE_NAMES[board.getRuleSet()];
        statsLabel.setText(String.format("GEN: %d  |  POP: %,d  |  %s  |  %.0f FPS",
            board.generationNum, board.population, ruleName, fps));
        statusLabel.setText(isPaused ? "⏸ PAUSED" : "▶ RUNNING");
        statusLabel.getStyleClass().setAll(isPaused ? "status-paused" : "status-running");

        // Update HUD
        if (showHUD) {
            hudPopLabel.setText("POP: " + String.format("%,d", board.population));
            hudPeakLabel.setText("PEAK: " + String.format("%,d", board.peakPop));
            hudBornLabel.setText("BORN: +" + board.bornThisGen);
            hudDiedLabel.setText("DIED: -" + board.diedThisGen);
            hudGpsLabel.setText(String.format("GPS: %.0f", gensPerSec));
            hudZoomLabel.setText(String.format("ZOOM: %.1f×", zoomLevel));
            drawSparkline();
        }
    }

    private void drawGrid(double w, double h) {
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0A0F19"));
        gc.fillRect(0, 0, w, h);

        int gs = SimulationRules.GRID_SIZE;
        double baseCellSize = Math.min(w / gs, h / gs);
        double cellSize = baseCellSize * zoomLevel;
        double gridW = gs * cellSize;
        double gridH = gs * cellSize;
        double gridX = (w - gridW) / 2.0 + panX;
        double gridY = (h - gridH) / 2.0 + panY;

        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                double cx = gridX + c * cellSize;
                double cy = gridY + r * cellSize;
                if (cx + cellSize < 0 || cx > w || cy + cellSize < 0 || cy > h) continue;

                if (board.getCellState(r, c)) {
                    gc.setFill(showHeatmap ? ageToColor(board.getAge(r, c)) : Color.web("#00FFE0"));
                } else {
                    gc.setFill(Color.web("#121826"));
                }
                gc.fillRect(cx, cy, cellSize, cellSize);
            }
        }

        // Minimap when zoomed
        if (zoomLevel > 1.5) {
            double mmSize = 80;
            double mmX = w - mmSize - 10;
            double mmY = 10;
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(mmX - 2, mmY - 2, mmSize + 4, mmSize + 4);
            double mmCell = mmSize / gs;
            for (int r = 0; r < gs; r++) {
                for (int c = 0; c < gs; c++) {
                    if (board.getCellState(r, c)) {
                        gc.setFill(Color.web("#00FFE0", 0.7));
                        gc.fillRect(mmX + c * mmCell, mmY + r * mmCell, mmCell, mmCell);
                    }
                }
            }
            // Viewport indicator
            double vpRatio = 1.0 / zoomLevel;
            double vpW = mmSize * vpRatio;
            double vpH = mmSize * vpRatio;
            double vpOffX = -panX / gridW * mmSize;
            double vpOffY = -panY / gridH * mmSize;
            gc.setStroke(Color.web("#00FFE0", 0.8));
            gc.setLineWidth(1);
            gc.strokeRect(mmX + (mmSize - vpW) / 2 + vpOffX,
                          mmY + (mmSize - vpH) / 2 + vpOffY, vpW, vpH);
        }
    }

    private void drawSparkline() {
        GraphicsContext gc = sparklineCanvas.getGraphicsContext2D();
        double sw = sparklineCanvas.getWidth();
        double sh = sparklineCanvas.getHeight();
        gc.setFill(Color.rgb(255, 255, 255, 0.03));
        gc.fillRect(0, 0, sw, sh);

        int[] hist = board.popHistory;
        int idx = board.popHistIdx;
        int maxPop = 1;
        for (int v : hist) if (v > maxPop) maxPop = v;

        gc.setStroke(Color.web("#00FFE0", 0.6));
        gc.setLineWidth(1);
        gc.beginPath();
        for (int i = 0; i < hist.length; i++) {
            int di = (idx - hist.length + i + hist.length * 2) % hist.length;
            double val = (double) hist[di] / maxPop;
            double x = i * (sw / hist.length);
            double y = sh * (1 - val);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private Color ageToColor(int age) {
        if (age <= 0) return Color.web("#121826");
        if (age == 1) return Color.web("#00FFFF");
        if (age <= 5) return Color.web("#1E90FF").interpolate(Color.web("#00FFE0"), (age - 2) / 3.0);
        if (age <= 15) return Color.web("#00FFE0").interpolate(Color.web("#FF00AA"), (age - 5) / 10.0);
        if (age <= 40) return Color.web("#FF00AA").interpolate(Color.web("#FF4757"), (age - 15) / 25.0);
        return Color.web("#FF4757").interpolate(Color.WHITE, Math.min((age - 40) / 30.0, 1.0));
    }

    // ── Mouse Event Handlers ──────────────────────────────────

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY || e.isMiddleButtonDown()) {
            isDragging = true;
            dragStartX = e.getX(); dragStartY = e.getY();
            panStartX = panX; panStartY = panY;
        } else {
            drawWithBrush(e.getX(), e.getY());
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (isDragging) {
            panX = panStartX + (e.getX() - dragStartX);
            panY = panStartY + (e.getY() - dragStartY);
        } else {
            drawWithBrush(e.getX(), e.getY());
        }
    }

    private void onScroll(ScrollEvent e) {
        double oldZoom = zoomLevel;
        zoomLevel *= (e.getDeltaY() > 0) ? 1.15 : 0.87;
        zoomLevel = Math.max(1.0, Math.min(zoomLevel, 16.0));
        if (zoomLevel <= 1.01) { panX = 0; panY = 0; }
        panX *= zoomLevel / oldZoom;
        panY *= zoomLevel / oldZoom;
    }

    private void drawWithBrush(double mx, double my) {
        int gs = SimulationRules.GRID_SIZE;
        double w = gridPane.getWidth();
        double h = gridPane.getHeight();
        double baseCellSize = Math.min(w / gs, h / gs);
        double cellSize = baseCellSize * zoomLevel;
        double gridW = gs * cellSize;
        double gridH = gs * cellSize;
        double gridX = (w - gridW) / 2.0 + panX;
        double gridY = (h - gridH) / 2.0 + panY;
        int baseC = (int) ((mx - gridX) / cellSize);
        int baseR = (int) ((my - gridY) / cellSize);
        for (int i = 0; i < brushSize; i++)
            for (int j = 0; j < brushSize; j++)
                if (baseR + i >= 0 && baseR + i < gs && baseC + j >= 0 && baseC + j < gs)
                    board.setCellState(baseR + i, baseC + j, true);
        board.recount();
    }

    // ── FXML Button Handlers ──────────────────────────────────

    @FXML private void togglePause() {
        isPaused = !isPaused;
        btnPause.setText(isPaused ? "⏯ PLAY" : "⏸ PAUSE");
    }

    @FXML private void clearGrid() {
        board.clearBoard();
        isPaused = true;
        btnPause.setText("⏯ PLAY");
    }

    @FXML private void cycleBrush() {
        brushSize = (brushSize == 1) ? 3 : (brushSize == 3) ? 5 : 1;
        btnBrush.setText("✏ BRUSH: " + brushSize);
    }

    @FXML private void spawnGlider() {
        PatternLibrary.spawnGlider(board, SimulationRules.GRID_SIZE / 2, SimulationRules.GRID_SIZE / 2);
    }

    @FXML private void spawnGun() {
        PatternLibrary.spawnGliderGun(board);
    }

    @FXML private void cycleRule() {
        int next = (board.getRuleSet() + 1) % SimulationRules.RULE_COUNT;
        board.setRuleSet(next);
        btnRule.setText(SimulationRules.RULE_NAMES[next].split(" ")[0]);
    }

    @FXML private void cycleSpeed() {
        speedLevel = (speedLevel + 1) % SimulationRules.SPEED_LEVELS.length;
        btnSpeed.setText(SimulationRules.SPEED_NAMES[speedLevel]);
    }

    @FXML private void toggleHud() {
        showHUD = !showHUD;
        hudOverlay.setVisible(showHUD);
    }

    @FXML private void toggleHeatmap() {
        showHeatmap = !showHeatmap;
    }

    @FXML private void goMenu() {
        board.clearBoard();
        isPaused = true;
        app.navigateTo(0);
    }
}
