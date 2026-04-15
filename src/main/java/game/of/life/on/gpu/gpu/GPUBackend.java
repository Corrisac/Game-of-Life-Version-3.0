package game.of.life.on.gpu.gpu;

import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.SimulationRules;

/**
 * GPUBackend — Manages GPU compute shader dispatch.
 *
 * CURRENT STATE: CPU-based fallback implementation.
 * TODO: Wire direct JOGL GL4 context with conway_compute.glsl for GPU acceleration.
 *
 * The public API is designed for the future GPU pipeline:
 *   - startCompute(grid, iterations): Begins async compute
 *   - processBatch(totalIterations): Processes a batch each frame
 *   - getResultGrid(): Returns the computed result
 */
public class GPUBackend {

    private Grid resultGrid;
    private boolean computing = false;
    private boolean hasComputed = false;
    private int computeProgress = 0;
    private int targetIterations = 0;
    private long computeStartTime = 0;
    private long computeTime = 0;

    // Batch size per frame (CPU fallback)
    private static final int BATCH_SIZE = 5000;

    public GPUBackend() {
        resultGrid = new Grid(SimulationRules.GRID_SIZE, SimulationRules.GRID_SIZE);
    }

    /**
     * Starts a compute job: copies the input grid state and begins
     * iterating toward the target iteration count.
     */
    public void startCompute(Grid inputGrid, int iterations) {
        // Copy input grid state into result grid
        System.arraycopy(inputGrid.boardFront, 0,
                         resultGrid.boardFront, 0,
                         inputGrid.boardFront.length);
        resultGrid.clearBoard();
        System.arraycopy(inputGrid.boardFront, 0,
                         resultGrid.boardFront, 0,
                         inputGrid.boardFront.length);
        resultGrid.recount();

        targetIterations = iterations;
        computeProgress = 0;
        computing = true;
        hasComputed = false;
        computeStartTime = System.currentTimeMillis();
    }

    /**
     * Processes a batch of iterations. Call once per frame.
     * Returns true when compute is complete.
     */
    public boolean processBatch(int totalIterations) {
        if (!computing) return false;

        int remaining = targetIterations - computeProgress;
        int batchCount = Math.min(remaining, BATCH_SIZE);

        for (int i = 0; i < batchCount; i++) {
            resultGrid.updateToNextGeneration();
        }
        computeProgress += batchCount;

        if (computeProgress >= targetIterations) {
            computing = false;
            hasComputed = true;
            computeTime = System.currentTimeMillis() - computeStartTime;
            return true;
        }
        return false;
    }

    // ── Accessors ─────────────────────────────────────────

    public boolean isComputing() { return computing; }
    public boolean hasComputed() { return hasComputed; }
    public int getComputeProgress() { return computeProgress; }
    public long getComputeTime() { return computeTime; }
    public Grid getResultGrid() { return resultGrid; }
}
