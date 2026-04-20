package game.of.life.on.gpu.ui;

import game.of.life.on.gpu.App;
import game.of.life.on.gpu.engine.DualGrid;
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

import java.util.Random;

/**
 * DualLabController — Interactive dual-species Life vs Antilife screen.
 *
 * Both species run Conway B3/S23 independently. The only cross-species
 * interaction is the contested-birth rule: if a dead cell simultaneously
 * qualifies for a Life birth AND an Antilife birth, both are cancelled.
 */
public class DualLabController {

    // ── FXML Injections ───────────────────────────────────────────
    @FXML private Canvas gridCanvas;
    @FXML private Canvas sparklineCanvas;
    @FXML private StackPane gridPane;
    @FXML private Label statsLabel, statusLabel;
    @FXML private Label hudLifeLabel, hudAntiLabel, hudGenLabel, hudTpsLabel;
    @FXML private VBox hudOverlay;
    @FXML private Button btnPause, btnBrushLife, btnBrushAnti, btnBrushErase;
    @FXML private Button btnBrush, btnSpeed, btnPreset;

    private App app;
    private DualGrid board;

    // ── Simulation State ──────────────────────────────────────────
    private boolean isPaused = true;
    private int brushMode = 0;    // 0=Life, 1=Anti, 2=Erase
    private int brushSize = 1;
    private boolean showHUD = false;

    private static final int[]    SPEED_STEPS = {1, 2, 5, 10, 30, 60};
    private static final String[] SPEED_NAMES = {"1×", "2×", "5×", "10×", "30×", "60×"};
    private int speedIdx = 0;

    private static final String[] PRESET_NAMES = {"DUEL", "FRONTIER", "EMPIRES"};
    private int presetIdx = 0;

    // ── Zoom & Pan ────────────────────────────────────────────────
    private double zoomLevel = 1.0;
    private double panX = 0, panY = 0;
    private boolean isDragging = false;
    private double dragStartX, dragStartY, panStartX, panStartY;

    // ── TPS Tracking ──────────────────────────────────────────────
    private int stepsSinceTPS = 0;
    private long lastTPSCalc = 0;
    private double tps = 0;

    // ── Colors ────────────────────────────────────────────────────
    private static final Color LIFE_COLOR = Color.web("#00FFE0");
    private static final Color ANTI_COLOR = Color.web("#FF3EA5");
    private static final Color DEAD_COLOR = Color.web("#121826");

    // ── Init ──────────────────────────────────────────────────────

    public void init(App app) {
        this.app   = app;
        this.board = app.getDualBoard();

        gridCanvas.setOnMousePressed(this::onMousePressed);
        gridCanvas.setOnMouseDragged(this::onMouseDragged);
        gridCanvas.setOnMouseReleased(e -> isDragging = false);
        gridCanvas.setOnScroll(this::onScroll);

        updateBrushButtons();
    }

    // ── Main Loop ─────────────────────────────────────────────────

    public void update(double dt, double fps) {
        double w = gridPane.getWidth();
        double h = gridPane.getHeight();
        if (w <= 0 || h <= 0) return;

        gridCanvas.setWidth(w);
        gridCanvas.setHeight(h);

        if (!isPaused) {
            int steps = SPEED_STEPS[speedIdx];
            for (int i = 0; i < steps; i++) {
                board.updateToNextGeneration();
                stepsSinceTPS++;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastTPSCalc > 500) {
            double elapsed = (now - lastTPSCalc) / 1000.0;
            if (elapsed > 0) tps = stepsSinceTPS / elapsed;
            stepsSinceTPS = 0;
            lastTPSCalc = now;
        }

        drawGrid(w, h);

        statsLabel.setText(String.format(
            "GEN: %d  |  LIFE: %,d  |  ANTI: %,d  |  %.0f FPS",
            board.generationNum, board.lifePopulation, board.antiPopulation, fps));
        statusLabel.setText(isPaused ? "⏸ PAUSED" : "▶ RUNNING");
        statusLabel.getStyleClass().setAll(isPaused ? "status-paused" : "status-running");

        if (showHUD) {
            hudLifeLabel.setText("LIFE:  " + String.format("%,d", board.lifePopulation));
            hudAntiLabel.setText("ANTI:  " + String.format("%,d", board.antiPopulation));
            hudGenLabel.setText( "GEN:   " + board.generationNum);
            hudTpsLabel.setText( String.format("TPS:   %.0f", tps));
            drawSparklines();
        }
    }

    // ── Grid Rendering ────────────────────────────────────────────

    private void drawGrid(double w, double h) {
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.setFill(DEAD_COLOR);
        gc.fillRect(0, 0, w, h);

        int gs = SimulationRules.GRID_SIZE;
        double baseCellSize = Math.min(w / gs, h / gs);
        double cellSize = baseCellSize * zoomLevel;
        double gridW    = gs * cellSize;
        double gridH    = gs * cellSize;
        double gridX    = (w - gridW) / 2.0 + panX;
        double gridY    = (h - gridH) / 2.0 + panY;

        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                double cx = gridX + c * cellSize;
                double cy = gridY + r * cellSize;
                if (cx + cellSize < 0 || cx > w || cy + cellSize < 0 || cy > h) continue;

                boolean life = board.getLife(r, c);
                boolean anti = board.getAnti(r, c);

                if (life) {
                    gc.setFill(LIFE_COLOR);
                } else if (anti) {
                    gc.setFill(ANTI_COLOR);
                } else {
                    gc.setFill(DEAD_COLOR);
                }
                gc.fillRect(cx, cy, cellSize, cellSize);
            }
        }

        // Grid boundary outline — makes the 256×256 area visible against the dark canvas
        gc.setStroke(Color.web("#00FFE0", 0.25));
        gc.setLineWidth(1);
        gc.strokeRect(gridX - 0.5, gridY - 0.5, gridW + 1, gridH + 1);

        // Minimap when zoomed > 1.5×
        if (zoomLevel > 1.5) {
            double mmSize = 80;
            double mmX    = w - mmSize - 10;
            double mmY    = 10;
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            gc.fillRect(mmX - 2, mmY - 2, mmSize + 4, mmSize + 4);
            double mmCell = mmSize / gs;
            for (int r = 0; r < gs; r++) {
                for (int c = 0; c < gs; c++) {
                    boolean life = board.getLife(r, c);
                    boolean anti = board.getAnti(r, c);
                    if (life) {
                        gc.setFill(Color.web("#00FFE0", 0.8));
                        gc.fillRect(mmX + c * mmCell, mmY + r * mmCell, mmCell, mmCell);
                    } else if (anti) {
                        gc.setFill(Color.web("#FF3EA5", 0.8));
                        gc.fillRect(mmX + c * mmCell, mmY + r * mmCell, mmCell, mmCell);
                    }
                }
            }
            double vpRatio = 1.0 / zoomLevel;
            double vpW     = mmSize * vpRatio;
            double vpH     = mmSize * vpRatio;
            double vpOffX  = -panX / gridW * mmSize;
            double vpOffY  = -panY / gridH * mmSize;
            gc.setStroke(Color.rgb(255, 255, 255, 0.7));
            gc.setLineWidth(1);
            gc.strokeRect(mmX + (mmSize - vpW) / 2 + vpOffX,
                          mmY + (mmSize - vpH) / 2 + vpOffY, vpW, vpH);
        }
    }

    private void drawSparklines() {
        GraphicsContext gc = sparklineCanvas.getGraphicsContext2D();
        double sw = sparklineCanvas.getWidth();
        double sh = sparklineCanvas.getHeight();
        gc.setFill(Color.rgb(255, 255, 255, 0.03));
        gc.fillRect(0, 0, sw, sh);

        int[] lh  = board.lifeHistory;
        int[] ah  = board.antiHistory;
        int   idx = board.histIdx;
        int   len = lh.length;

        int maxPop = 1;
        for (int v : lh) if (v > maxPop) maxPop = v;
        for (int v : ah) if (v > maxPop) maxPop = v;

        // Life sparkline — cyan
        gc.setStroke(Color.web("#00FFE0", 0.75));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < len; i++) {
            int di = (idx - len + i + len * 2) % len;
            double x = i * (sw / len);
            double y = sh * (1.0 - (double) lh[di] / maxPop);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        // Anti sparkline — magenta-pink
        gc.setStroke(Color.web("#FF3EA5", 0.75));
        gc.beginPath();
        for (int i = 0; i < len; i++) {
            int di = (idx - len + i + len * 2) % len;
            double x = i * (sw / len);
            double y = sh * (1.0 - (double) ah[di] / maxPop);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    // ── Mouse Handlers ────────────────────────────────────────────

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY || e.isMiddleButtonDown()) {
            isDragging = true;
            dragStartX = e.getX(); dragStartY = e.getY();
            panStartX  = panX;     panStartY  = panY;
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
        int    gs          = SimulationRules.GRID_SIZE;
        double w           = gridPane.getWidth();
        double h           = gridPane.getHeight();
        double cellSize    = Math.min(w / gs, h / gs) * zoomLevel;
        double gridW       = gs * cellSize;
        double gridH       = gs * cellSize;
        double gridX       = (w - gridW) / 2.0 + panX;
        double gridY       = (h - gridH) / 2.0 + panY;
        int    baseC       = (int) ((mx - gridX) / cellSize);
        int    baseR       = (int) ((my - gridY) / cellSize);

        for (int i = 0; i < brushSize; i++) {
            for (int j = 0; j < brushSize; j++) {
                int r = baseR + i, c = baseC + j;
                if (r < 0 || r >= gs || c < 0 || c >= gs) continue;
                switch (brushMode) {
                    case 0 -> board.setLife(r, c, true);
                    case 1 -> board.setAnti(r, c, true);
                    case 2 -> board.erase(r, c);
                }
            }
        }
        board.recount();
    }

    // ── FXML Button Handlers ──────────────────────────────────────

    @FXML private void togglePause() {
        isPaused = !isPaused;
        btnPause.setText(isPaused ? "⏯ PLAY" : "⏸ PAUSE");
    }

    @FXML private void stepOnce() {
        board.updateToNextGeneration();
    }

    @FXML private void clearGrid() {
        board.clear();
        isPaused = true;
        btnPause.setText("⏯ PLAY");
    }

    @FXML private void selectLifeBrush()  { brushMode = 0; updateBrushButtons(); }
    @FXML private void selectAntiBrush()  { brushMode = 1; updateBrushButtons(); }
    @FXML private void selectEraseBrush() { brushMode = 2; updateBrushButtons(); }

    @FXML private void cycleBrush() {
        brushSize = (brushSize == 1) ? 3 : (brushSize == 3) ? 5 : 1;
        btnBrush.setText("✏ BRUSH: " + brushSize);
    }

    @FXML private void cycleSpeed() {
        speedIdx = (speedIdx + 1) % SPEED_STEPS.length;
        btnSpeed.setText("⚡ " + SPEED_NAMES[speedIdx]);
    }

    /**
     * Loads the currently shown preset, then advances the label to the next one.
     * First click → DUEL, second → FRONTIER, third → EMPIRES, wraps around.
     */
    @FXML private void loadPreset() {
        board.clear();
        isPaused = true;
        btnPause.setText("⏯ PLAY");

        switch (presetIdx) {
            case 0 -> applyGliderDuel();
            case 1 -> applyFrontier();
            case 2 -> applyEmpires();
        }

        presetIdx = (presetIdx + 1) % PRESET_NAMES.length;
        btnPreset.setText("⬡ " + PRESET_NAMES[presetIdx]);
    }

    @FXML private void toggleHud() {
        showHUD = !showHUD;
        hudOverlay.setVisible(showHUD);
    }

    @FXML private void zoomIn() {
        double old = zoomLevel;
        zoomLevel = Math.min(zoomLevel * 1.25, 16.0);
        panX *= zoomLevel / old;
        panY *= zoomLevel / old;
    }

    @FXML private void zoomOut() {
        double old = zoomLevel;
        zoomLevel = Math.max(zoomLevel / 1.25, 1.0);
        if (zoomLevel <= 1.01) { panX = 0; panY = 0; }
        else { panX *= zoomLevel / old; panY *= zoomLevel / old; }
    }

    @FXML private void resetZoom() {
        zoomLevel = 1.0;
        panX = 0;
        panY = 0;
    }

    @FXML private void goMenu() {
        board.clear();
        isPaused = true;
        app.navigateTo(0);
    }

    // ── Brush Button Visual State ─────────────────────────────────

    private void updateBrushButtons() {
        setBrushActive(btnBrushLife,  brushMode == 0, "brush-active-life");
        setBrushActive(btnBrushAnti,  brushMode == 1, "brush-active-anti");
        setBrushActive(btnBrushErase, brushMode == 2, "brush-active-erase");
    }

    private void setBrushActive(Button btn, boolean active, String activeClass) {
        btn.getStyleClass().removeAll("brush-active-life", "brush-active-anti", "brush-active-erase");
        if (active) btn.getStyleClass().add(activeClass);
    }

    // ── Preset Patterns ───────────────────────────────────────────

    /**
     * GLIDER DUEL — Life SE-glider (top-left) converging with Anti NW-glider (bottom-right).
     * Demonstrates that the two species are transparent to each other's neighbour counts;
     * the only possible interaction is contested births at the crossing point.
     */
    private void applyGliderDuel() {
        // SE-glider: same orientation as PatternLibrary.spawnGlider, moves (+1,+1)/4 gens
        int[][] se = {{-1, 0}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        int lr = 80, lc = 80;
        for (int[] p : se) board.setLife(lr + p[0], lc + p[1], true);

        // NW-glider: 180° rotation of SE-glider, moves (-1,-1)/4 gens
        int[][] nw = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {1, 0}};
        int ar = 172, ac = 172;
        for (int[] p : nw) board.setAnti(ar + p[0], ac + p[1], true);

        board.recount();
    }

    /**
     * CONTESTED FRONTIER — R-pentomino Life (left) vs R-pentomino Anti (right).
     * Each R-pentomino takes ~1100 generations to stabilise and generates enormous
     * debris. As the two expanding fronts meet at the centre, contested births
     * form a visible dead zone along the boundary.
     */
    private void applyFrontier() {
        // R-pentomino: .XX / XX. / .X.
        int[][] rPent = {{-1, 0}, {-1, 1}, {0, -1}, {0, 0}, {1, 0}};

        int lr = 128, lc = 72;
        for (int[] p : rPent) board.setLife(lr + p[0], lc + p[1], true);

        int ar = 128, ac = 184;
        for (int[] p : rPent) board.setAnti(ar + p[0], ac + p[1], true);

        board.recount();
    }

    /**
     * EMPIRES — Dense random Life on the left half, dense random Anti on the right half.
     * Both populations collapse into complex stable/oscillating territories and their
     * growth fronts contest the central corridor continuously.
     */
    private void applyEmpires() {
        Random rng = new Random(42);
        int gs = SimulationRules.GRID_SIZE;

        // Life: left 40% of the grid, 28% density
        for (int r = 8; r < gs - 8; r++) {
            for (int c = 8; c < gs * 2 / 5; c++) {
                if (rng.nextFloat() < 0.28f) board.setLife(r, c, true);
            }
        }

        // Anti: right 40% of the grid, 28% density
        for (int r = 8; r < gs - 8; r++) {
            for (int c = gs * 3 / 5; c < gs - 8; c++) {
                if (rng.nextFloat() < 0.28f) board.setAnti(r, c, true);
            }
        }

        board.recount();
    }
}
