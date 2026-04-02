import processing.core.PApplet;    // Processing core for millis(), createGraphics(), loadShader()
import processing.core.PGraphics;  // Offscreen graphics buffer for ping-pong rendering
import processing.opengl.PShader;  // GLSL shader wrapper for GPU computation

/**
 * GPUCompute — Encapsulates all GPU-accelerated computation logic for running
 * Conway's Game of Life through GLSL fragment shaders. Uses the ping-pong buffer
 * technique: two offscreen buffers alternate as source/destination each iteration.
 * Computation is batched (BATCH_SIZE per frame) to keep the UI responsive.
 */
public class GPUCompute implements ThemeConstants {

    private PApplet p;               // Processing applet for GPU/shader operations
    private PShader conwayShader;     // GLSL shader implementing Conway's 4 rules
    PGraphics buffer1;               // Ping-pong buffer A (current state)
    PGraphics buffer2;               // Ping-pong buffer B (next state target)
    boolean hasComputed = false;     // True when a completed GPU result is available
    long computeTime = 0;            // Wall-clock milliseconds of the last computation
    boolean isComputing = false;     // True when a batched computation is in progress
    int computeProgress = 0;         // Iterations completed so far in current run
    private long computeStartTime;   // Timestamp when computation began (for timing)
    private static final int BATCH_SIZE = 500; // Max iterations per frame (prevents UI freeze)

    /**
     * Constructor — Loads the Conway shader and creates two 60x60 ping-pong buffers.
     * @param p  Processing applet for shader/graphics creation
     */
    public GPUCompute(PApplet p) {
        this.p = p;                                        // Store the Processing applet reference
        conwayShader = p.loadShader("conway.glsl");         // Load the GLSL Conway rules shader
        buffer1 = p.createGraphics(60, 60, PApplet.P2D);   // Create 60x60 OpenGL buffer A
        buffer1.noSmooth();                                 // Disable anti-aliasing for pixel accuracy
        buffer2 = p.createGraphics(60, 60, PApplet.P2D);   // Create 60x60 OpenGL buffer B
        buffer2.noSmooth();                                 // Disable anti-aliasing for pixel accuracy
    }

    /**
     * Copies the grid state into buffer1 and starts a new batched GPU computation.
     * @param board       Grid data model with current cell states
     * @param iterations  Total number of generations to compute
     */
    public void startCompute(Grid board, int iterations) {
        if (iterations <= 0) return;                       // Abort if no iterations requested
        System.out.println("[GPU] Starting compute: " + iterations + " iterations"); // Debug log
        buffer1.beginDraw();                               // Begin drawing to buffer1
        buffer1.background(255);                           // Fill with white (dead cells)
        buffer1.noStroke();                                // No outlines on cell pixels
        buffer1.fill(0);                                   // Set fill to black (alive cells)
        for (int r = 0; r < 60; r++)                       // Iterate over all grid rows
            for (int c = 0; c < 60; c++)                   // Iterate over all grid columns
                if (board.getCellState(r, c))               // If this cell is alive
                    buffer1.rect(c, r, 1, 1);              // Draw a 1x1 black pixel at its position
        buffer1.endDraw();                                 // Finalize buffer1 drawing
        computeProgress = 0;                               // Reset iteration counter
        computeStartTime = p.millis();                     // Record the start timestamp
        isComputing = true;                                // Flag that computation is active
        hasComputed = false;                               // Invalidate any previous result
    }

    /**
     * Processes one batch of GPU iterations. Called each frame while isComputing==true.
     * @param targetIterations  Total iteration target to reach before stopping
     */
    public void processBatch(int targetIterations) {
        int remaining = targetIterations - computeProgress;           // How many iterations left
        int batch = Math.min(remaining, BATCH_SIZE);                  // Clamp to batch size
        for (int i = 0; i < batch; i++) {                             // Process this batch
            buffer2.beginDraw();                                      // Begin writing to buffer2
            buffer2.clear();                                          // Clear buffer2
            buffer2.shader(conwayShader);                             // Apply Conway shader
            buffer2.image(buffer1, 0, 0);                             // Draw buffer1 through shader
            buffer2.resetShader();                                    // Remove shader
            buffer2.endDraw();                                        // Finalize buffer2
            PGraphics t = buffer1; buffer1 = buffer2; buffer2 = t;   // Swap buffers (ping-pong)
        }
        computeProgress += batch;                                     // Update progress counter
        if (computeProgress >= targetIterations) {                    // Check if done
            computeTime = p.millis() - computeStartTime;              // Calculate elapsed time
            isComputing = false;                                      // Stop computing
            hasComputed = true;                                       // Mark result as ready
            System.out.println("[GPU] Compute done in " + computeTime + "ms"); // Debug log
        }
    }

    /** Returns the buffer containing the most recent computed result image. */
    public PGraphics getResultBuffer() {
        return buffer1;  // buffer1 always holds the latest state after ping-pong swaps
    }
}
