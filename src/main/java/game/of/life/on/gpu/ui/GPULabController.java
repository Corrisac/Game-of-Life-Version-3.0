package game.of.life.on.gpu.ui;

import game.of.life.on.gpu.App;
import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.PatternLibrary;
import game.of.life.on.gpu.engine.SimulationRules;
import game.of.life.on.gpu.gpu.GPUBackend;
import game.of.life.on.gpu.ui.controls.IterationDial;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * GPULabController — Manages the GPU Compute Lab screen.
 * Editable input grid, iteration dial, GPU compute dispatch, result display.
 */
public class GPULabController {

    @FXML private StackPane dialContainer, inputGridPane, resultPane;
    @FXML private Canvas inputGridCanvas, resultCanvas;
    @FXML private TextField iterInput;
    @FXML private Button btnCompute;
    @FXML private VBox placeholderBox, progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel, resultLabel;
    @FXML private HBox resultBanner;

    private App app;
    private Grid board;
    private GPUBackend gpuBackend;
    private IterationDial dial;

    // Compute state
    private boolean isComputing = false;
    private boolean hasResult = false;
    private int brushSize = 1;

    public void init(App app) {
        this.app = app;
        this.board = app.getGameBoard();
        this.gpuBackend = app.getGPUBackend();

        // Create and embed the iteration dial
        dial = new IterationDial(1_000_000, "ITERATIONS");
        dial.setValue(100_000);
        dialContainer.getChildren().add(dial);

        // Wire mouse events for the input grid
        inputGridCanvas.setOnMousePressed(this::onGridMousePressed);
        inputGridCanvas.setOnMouseDragged(this::onGridMouseDragged);

        // Sync textfield with dial
        iterInput.setText("100,000");
    }

    /** Called every frame. */
    public void update(double dt, double fps) {
        // Resize input grid canvas
        double igW = inputGridPane.getWidth();
        double igH = inputGridPane.getHeight();
        if (igW > 0 && igH > 0) {
            inputGridCanvas.setWidth(igW);
            inputGridCanvas.setHeight(igH);
            drawInputGrid(igW, igH);
        }

        // Update dial animation
        dial.update(dt);

        // Process GPU compute batch if running
        if (isComputing && gpuBackend != null) {
            gpuBackend.processBatch(dial.getValue());
            double pct = (double) gpuBackend.getComputeProgress() / dial.getValue();
            progressBar.setProgress(pct);
            progressLabel.setText(String.format("%,d / %,d  (%.0f%%)",
                gpuBackend.getComputeProgress(), dial.getValue(), pct * 100));

            if (!gpuBackend.isComputing()) {
                // Compute finished
                isComputing = false;
                hasResult = true;
                progressBox.setVisible(false);
                resultCanvas.setVisible(true);
                resultBanner.setVisible(true);
                placeholderBox.setVisible(false);
                resultLabel.setText("✓  " + gpuBackend.getComputeTime() + "ms  ·  "
                    + String.format("%,d", dial.getValue()) + " GEN  ·  CONWAY");
                drawResultGrid();
            }
        }
    }

    private void drawInputGrid(double w, double h) {
        GraphicsContext gc = inputGridCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0A0F19"));
        gc.fillRect(0, 0, w, h);

        int gs = SimulationRules.GRID_SIZE;
        double gridDisplaySize = Math.min(w - 10, h - 10);
        double offsetX = (w - gridDisplaySize) / 2;
        double offsetY = (h - gridDisplaySize) / 2;
        double cellSize = gridDisplaySize / gs;

        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                gc.setFill(board.getCellState(r, c) ? Color.web("#00FFE0") : Color.web("#121826"));
                gc.fillRect(offsetX + c * cellSize, offsetY + r * cellSize, cellSize, cellSize);
            }
        }
    }

    private void drawResultGrid() {
        double w = resultPane.getWidth();
        double h = resultPane.getHeight();
        if (w <= 0 || h <= 0) return;
        resultCanvas.setWidth(w);
        resultCanvas.setHeight(h);

        GraphicsContext gc = resultCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0A0F19"));
        gc.fillRect(0, 0, w, h);

        int gs = SimulationRules.GRID_SIZE;
        double cellSize = Math.min(w, h) / gs;
        double offsetX = (w - gs * cellSize) / 2;
        double offsetY = (h - gs * cellSize) / 2;

        Grid resultBoard = gpuBackend != null ? gpuBackend.getResultGrid() : board;
        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                gc.setFill(resultBoard.getCellState(r, c) ? Color.web("#00FFE0") : Color.web("#121826"));
                gc.fillRect(offsetX + c * cellSize, offsetY + r * cellSize, cellSize, cellSize);
            }
        }
    }

    // ── Mouse handlers for input grid ─────────────────────

    private void onGridMousePressed(MouseEvent e) {
        drawOnGrid(e.getX(), e.getY());
        hasResult = false;
    }

    private void onGridMouseDragged(MouseEvent e) {
        drawOnGrid(e.getX(), e.getY());
    }

    private void drawOnGrid(double mx, double my) {
        int gs = SimulationRules.GRID_SIZE;
        double w = inputGridPane.getWidth();
        double h = inputGridPane.getHeight();
        double gridDisplaySize = Math.min(w - 10, h - 10);
        double offsetX = (w - gridDisplaySize) / 2;
        double offsetY = (h - gridDisplaySize) / 2;
        double cellSize = gridDisplaySize / gs;
        int baseC = (int) ((mx - offsetX) / cellSize);
        int baseR = (int) ((my - offsetY) / cellSize);
        for (int i = 0; i < brushSize; i++)
            for (int j = 0; j < brushSize; j++)
                if (baseR + i >= 0 && baseR + i < gs && baseC + j >= 0 && baseC + j < gs)
                    board.setCellState(baseR + i, baseC + j, true);
    }

    // ── FXML Button Handlers ──────────────────────────────

    @FXML private void compute() {
        if (isComputing) return;

        // Parse iteration count from textfield or dial
        int iterations = dial.getValue();
        try {
            String text = iterInput.getText().replaceAll("[^0-9]", "");
            if (!text.isEmpty()) {
                iterations = Math.min(Integer.parseInt(text), 1_000_000);
                dial.setValue(iterations);
            }
        } catch (NumberFormatException ignored) {}

        if (gpuBackend != null) {
            gpuBackend.startCompute(board, iterations);

            // GPU path completes synchronously — check immediately
            if (!gpuBackend.isComputing()) {
                // Already done (GPU was fast)
                hasResult = true;
                String mode = gpuBackend.isGPUAvailable() ? "GPU" : "CPU";
                resultLabel.setText("✓  " + gpuBackend.getComputeTime() + "ms (" + mode + ")  ·  "
                    + String.format("%,d", iterations) + " GEN  ·  CONWAY");
                progressBox.setVisible(false);
                resultCanvas.setVisible(true);
                resultBanner.setVisible(true);
                placeholderBox.setVisible(false);
                drawResultGrid();
            } else {
                // CPU fallback — show progress, processBatch() handles the rest
                isComputing = true;
                progressBox.setVisible(true);
                resultCanvas.setVisible(false);
                resultBanner.setVisible(false);
                placeholderBox.setVisible(false);
                progressBar.setProgress(0);
            }
        }
    }

    @FXML private void onIterationEnter() {
        try {
            String text = iterInput.getText().replaceAll("[^0-9]", "");
            if (!text.isEmpty()) {
                int val = Math.min(Integer.parseInt(text), 1_000_000);
                dial.setValue(val);
                iterInput.setText(String.format("%,d", val));
            }
        } catch (NumberFormatException ignored) {}
    }

    @FXML private void clear() {
        board.clearBoard();
        hasResult = false;
        resultCanvas.setVisible(false);
        resultBanner.setVisible(false);
        placeholderBox.setVisible(true);
        progressBox.setVisible(false);
    }

    @FXML private void spawnGlider() {
        PatternLibrary.spawnGlider(board, SimulationRules.GRID_SIZE / 2, SimulationRules.GRID_SIZE / 2);
        hasResult = false;
    }

    @FXML private void spawnGun() {
        PatternLibrary.spawnGliderGun(board);
        hasResult = false;
    }

    @FXML private void goMenu() {
        board.clearBoard();
        app.navigateTo(0);
    }
}
