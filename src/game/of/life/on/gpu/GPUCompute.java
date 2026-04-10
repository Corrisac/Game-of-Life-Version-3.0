import processing.core.PApplet;    // Processing core for millis(), createGraphics(), loadShader()
import processing.core.PGraphics;  // Offscreen graphics buffer for ping-pong rendering
import processing.opengl.PShader;  // GLSL shader wrapper for live simulation
import processing.opengl.PGL;      // Processing's OpenGL abstraction layer
import processing.opengl.PJOGL;    // Processing's JOGL-specific OpenGL implementation

import com.jogamp.opengl.GL;       // OpenGL base constants (GL_TEXTURE_2D, GL_FRAMEBUFFER, etc.)
import com.jogamp.opengl.GL2ES2;   // OpenGL ES 2.0 / GL 2.0+ interface for shader compilation

import java.nio.ByteBuffer;        // Direct byte buffer for pixel transfer to GL textures
import java.nio.ByteOrder;         // Byte order for native-endian buffer allocation
import java.nio.IntBuffer;         // Int buffer for pixel readback from compute results


/**
 * GPUCompute V6.0 — Single-Workgroup Bit-Parallel Compute Shader.
 *
 * TWO EXECUTION PATHS:
 *
 *   1. LIVE SIMULATION (1 step/frame) — Uses Processing's API with conway.glsl
 *      fragment shader in single-step mode (unchanged since V2).
 *
 *   2. GPU LAB BULK COMPUTE — Uses GL4.3 compute shader (conway_compute.glsl)
 *      with a single-workgroup, bit-parallel architecture:
 *
 *      - The 256×256 grid is packed as bits into 2048 uints (8 KB per grid)
 *      - Two shared memory arrays (tileA, tileB) enable in-place ping-pong
 *        (16 KB total — well under the 48 KB shared memory limit)
 *      - ALL iterations execute inside ONE work group (1024 threads) with
 *        barrier() synchronization — ZERO dispatch overhead
 *      - Bit-parallel adder tree computes 32 cells per uint operation
 *      - TDR-safe chunking: max 500K iterations per dispatch, with
 *        glFinish() between chunks to reset the Windows watchdog timer
 *
 *   PERFORMANCE HISTORY:
 *     V3 (K=4 fragment shader):       55,783ms for 900K iterations
 *     V5 (K=16 compute, multi-dispatch): 37,618ms
 *     V6 (single-WG bit-parallel):      1,319ms  (42× faster than V3)
 *
 * GL RESOURCES (created once in initCompute):
 *   - 2 RGBA8 textures (computeTex[0], computeTex[1]) for image I/O
 *   - 1 FBO for readback (computeFbo)
 *   - 1 compiled compute program (computeProgram) from conway_compute.glsl
 */
public class GPUCompute implements ThemeConstants {

    private PApplet p;               // Processing applet reference for GPU/shader operations
    private PShader conwayShader;     // V2 single-step shader for live simulation
    PGraphics buffer1;               // Ping-pong buffer A (current generation — Processing API)
    PGraphics buffer2;               // Ping-pong buffer B (next generation — Processing API)
    boolean hasComputed = false;     // True when a completed GPU Lab result is available
    long computeTime = 0;            // Wall-clock milliseconds of the last computation
    boolean isComputing = false;     // True when a batched computation is in progress
    int computeProgress = 0;         // Iterations completed so far in current run
    private long computeStartTime;   // Timestamp when computation began (for timing)
    private int totalTargetIters;    // Total iterations requested for current compute run

    // ═══════════════════════════════════════════════════════
    //       PRE-ALLOCATED PIXEL BUFFERS
    // ═══════════════════════════════════════════════════════
    // Reuse direct ByteBuffers to avoid expensive allocation per compute run.
    // Direct buffers are costly to allocate (OS-level memory mapping) but
    // cheap to reuse — allocate once, clear and rewrite on each run.
    private ByteBuffer pixelBuffer;              // Reusable buffer for texture upload/download
    private int[] readbackArray;                 // Reusable int array for bulk pixel readback

    /**
     * Constructor — Loads shaders and creates ping-pong buffers.
     * @param p  Processing applet for shader/graphics creation
     */
    public GPUCompute(PApplet p) {
        this.p = p;                                                        // Store Processing reference
        conwayShader = p.loadShader("conway.glsl");                        // V2 single-step shader
        buffer1 = p.createGraphics(GRID_SIZE, GRID_SIZE, PApplet.P2D);    // Buffer A
        buffer1.noSmooth();                                                 // Pixel-accurate rendering
        buffer2 = p.createGraphics(GRID_SIZE, GRID_SIZE, PApplet.P2D);    // Buffer B
        buffer2.noSmooth();                                                 // Pixel-accurate rendering
    }



    /** Checks shader compilation status and prints errors if any. */
    private boolean checkShaderCompile(GL2ES2 gl, int shader, String label) {
        int[] compiled = new int[1];
        gl.glGetShaderiv(shader, GL2ES2.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == GL.GL_FALSE) {
            int[] logLen = new int[1];
            gl.glGetShaderiv(shader, GL2ES2.GL_INFO_LOG_LENGTH, logLen, 0);
            byte[] log = new byte[logLen[0]];
            gl.glGetShaderInfoLog(shader, logLen[0], null, 0, log, 0);
            System.err.println("[GPU V3] " + label + " shader compile error: " + new String(log));
            return false;
        }
        return true;
    }

    /** Reads a shader source file from the data/ directory. */
    private String loadShaderSource(String filename) {
        String[] lines = p.loadStrings(filename);  // Processing's file loader (searches data/)
        if (lines == null) return null;
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════
    //       GPU LAB: BULK COMPUTATION (V6.0 Single-WG Bit-Parallel)
    // ═══════════════════════════════════════════════════════
    //
    // V6.0: Runs ALL iterations inside a SINGLE work group using shared memory.
    // The 256×256 grid is packed as bits into 2048 uints (8 KB per grid),
    // with two shared arrays for ping-pong (16 KB total — well under 48 KB).
    // barrier() provides global sync within the work group — ZERO dispatch
    // overhead. Bit-parallel adder tree computes 32 cells per uint operation.
    //
    // PERFORMANCE vs V5 (K=16 multi-dispatch):
    //   V5: 58K dispatches × 0.6ms overhead = 35 seconds
    //   V6: ~9 dispatches × 0.1s compute    = ~1 second

    // GL 4.3 constants (not exposed in GL2ES2 interface)
    private static final int GL_COMPUTE_SHADER = 0x91B9;
    private static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020;
    private static final int GL_READ_ONLY  = 0x88B8;
    private static final int GL_WRITE_ONLY = 0x88B9;
    private static final int GL_RGBA8      = 0x8058;

    // TDR safety: max iterations per dispatch (Windows TDR timeout = 2 seconds)
    // Each chunk takes ~0.1s on a mid-range GPU, well under the 2s limit.
    private static final int TDR_CHUNK = 500000;

    // Compute shader resources
    private boolean computeReady = false;
    private int   computeProgram;
    private int[] computeTex = new int[2];     // Ping-pong textures for compute
    private int[] computeFbo = new int[1];     // FBO for readback only
    private int   computeUIter;                // Uniform location for iteration count

    /**
     * V6.0: Initializes GL4.3 compute shader resources.
     * Creates RGBA8 textures, compiles conway_compute.glsl, and sets up
     * a readback FBO. Called once on first compute run.
     */
    private void initCompute() {
        if (computeReady) return;

        System.out.println("[GPU V6] Initializing compute shader...");

        buffer1.beginDraw();
        PGL pgl = buffer1.beginPGL();
        com.jogamp.opengl.GL glBase = ((PJOGL) pgl).gl;

        // ── Check GL4 support ───────────────────────────────────────────
        if (!glBase.isGL4()) {
            System.err.println("[GPU V6] GL4 not supported — compute shaders unavailable.");
            buffer1.endPGL();
            buffer1.endDraw();
            return;
        }

        com.jogamp.opengl.GL4 gl4 = glBase.getGL4();

        // ── Create two RGBA8 textures for compute ping-pong ─────────────
        gl4.glGenTextures(2, computeTex, 0);
        for (int i = 0; i < 2; i++) {
            gl4.glBindTexture(GL.GL_TEXTURE_2D, computeTex[i]);
            gl4.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL_RGBA8, GRID_SIZE, GRID_SIZE,
                             0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
            gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        }
        gl4.glBindTexture(GL.GL_TEXTURE_2D, 0);

        // ── Create FBO for final readback ───────────────────────────────
        gl4.glGenFramebuffers(1, computeFbo, 0);

        // ── Load and compile compute shader ─────────────────────────────
        String src = loadShaderSource("conway_compute.glsl");
        if (src == null) {
            System.err.println("[GPU V6] Failed to load conway_compute.glsl!");
            buffer1.endPGL();
            buffer1.endDraw();
            return;
        }

        int cs = gl4.glCreateShader(GL_COMPUTE_SHADER);
        gl4.glShaderSource(cs, 1, new String[]{src}, null);
        gl4.glCompileShader(cs);
        if (!checkShaderCompile(gl4, cs, "compute")) {
            buffer1.endPGL();
            buffer1.endDraw();
            return;
        }

        computeProgram = gl4.glCreateProgram();
        gl4.glAttachShader(computeProgram, cs);
        gl4.glLinkProgram(computeProgram);

        int[] linked = new int[1];
        gl4.glGetProgramiv(computeProgram, GL2ES2.GL_LINK_STATUS, linked, 0);
        if (linked[0] == GL.GL_FALSE) {
            int[] logLen = new int[1];
            gl4.glGetProgramiv(computeProgram, GL2ES2.GL_INFO_LOG_LENGTH, logLen, 0);
            byte[] log = new byte[Math.max(1, logLen[0])];
            gl4.glGetProgramInfoLog(computeProgram, log.length, null, 0, log, 0);
            System.err.println("[GPU V6] Compute program link error: " + new String(log));
            buffer1.endPGL();
            buffer1.endDraw();
            return;
        }
        gl4.glDeleteShader(cs);

        computeUIter = gl4.glGetUniformLocation(computeProgram, "uIterations");

        // ── Pre-allocate pixel buffer if not already done ───────────────
        if (pixelBuffer == null) {
            int bufSize = GRID_SIZE * GRID_SIZE * 4;
            pixelBuffer = ByteBuffer.allocateDirect(bufSize);
            pixelBuffer.order(ByteOrder.nativeOrder());
            readbackArray = new int[GRID_SIZE * GRID_SIZE];
        }

        buffer1.endPGL();
        buffer1.endDraw();

        computeReady = true;
        System.out.println("[GPU V6] Compute shader ready. Program=" + computeProgram
                + " Textures=" + computeTex[0] + "," + computeTex[1]);
    }

    /**
     * V6.0: Starts bulk GPU computation using single-workgroup compute shader.
     * Uploads grid → texture, dispatches in TDR-safe chunks (each chunk runs
     * up to 100K iterations entirely in shared memory), reads back result.
     */
    public void startCompute(Grid board, int iterations) {
        if (iterations <= 0) return;

        // Initialize compute resources on first use
        if (!computeReady) initCompute();
        if (!computeReady) {
            System.err.println("[GPU V6] Compute init failed — cannot run.");
            return;
        }

        int numChunks = (iterations + TDR_CHUNK - 1) / TDR_CHUNK;
        System.out.println("[GPU V6] Starting compute: " + iterations + " iters → "
                + numChunks + " chunks (max " + TDR_CHUNK + " iters/chunk)");

        computeProgress = 0;
        totalTargetIters = iterations;
        computeStartTime = p.millis();
        isComputing = true;
        hasComputed = false;

        // ── Upload grid state to computeTex[0] ──────────────────────────
        pixelBuffer.clear();
        byte[] arr = board.boardFront;
        for (int i = 0; i < arr.length; i++) {
            byte val = arr[i] != 0 ? (byte) 0 : (byte) 0xFF;  // alive=black(0), dead=white(FF)
            pixelBuffer.put(val);  // R
            pixelBuffer.put(val);  // G
            pixelBuffer.put(val);  // B
            pixelBuffer.put((byte) 0xFF);  // A
        }
        pixelBuffer.flip();

        buffer1.beginDraw();
        PGL pgl = buffer1.beginPGL();
        com.jogamp.opengl.GL4 gl4 = ((PJOGL) pgl).gl.getGL4();

        gl4.glBindTexture(GL.GL_TEXTURE_2D, computeTex[0]);
        gl4.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, GRID_SIZE, GRID_SIZE,
                            GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
        gl4.glBindTexture(GL.GL_TEXTURE_2D, 0);

        // ── Bind compute program ────────────────────────────────────────
        gl4.glUseProgram(computeProgram);

        // ══════════════════════════════════════════════════════════════════
        //  TDR-SAFE CHUNKED DISPATCH — single work group per dispatch
        //  Each dispatch: shader loads image → shared mem, runs N iterations
        //  with barrier() sync, writes result back to image.
        //  ~9 dispatches for 900K iterations. Zero per-iteration overhead.
        // ══════════════════════════════════════════════════════════════════
        int remaining = iterations;
        int src = 0;   // Which texture has the current state

        while (remaining > 0) {
            int chunk = Math.min(remaining, TDR_CHUNK);

            // Bind source as readonly (imgIn), other as writeonly (imgOut)
            gl4.glBindImageTexture(0, computeTex[src], 0, false, 0, GL_READ_ONLY, GL_RGBA8);
            gl4.glBindImageTexture(1, computeTex[1 - src], 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);

            // Set iteration count for this chunk
            gl4.glUniform1i(computeUIter, chunk);

            // SINGLE work group dispatch — ALL iterations in shared memory
            gl4.glDispatchCompute(1, 1, 1);

            // Ensure image writes are visible for next chunk's read
            gl4.glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            // GPU finish resets TDR watchdog timer between chunks
            gl4.glFinish();

            // Swap: output of this chunk becomes input for next
            src = 1 - src;
            remaining -= chunk;
        }

        gl4.glUseProgram(0);

        // Result is in computeTex[src] (the last dst, now swapped to src)
        int resultTex = src;

        // ── Read result back from result texture into buffer1 ────────────
        gl4.glBindFramebuffer(GL.GL_FRAMEBUFFER, computeFbo[0]);
        gl4.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
                                    GL.GL_TEXTURE_2D, computeTex[resultTex], 0);

        pixelBuffer.clear();
        gl4.glReadPixels(0, 0, GRID_SIZE, GRID_SIZE, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
        gl4.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

        buffer1.endPGL();

        // ── Copy pixels into Processing buffer (flip Y: GL is bottom-up) ─
        buffer1.loadPixels();
        pixelBuffer.rewind();
        IntBuffer intView = pixelBuffer.asIntBuffer();
        intView.get(readbackArray);

        for (int r = 0; r < GRID_SIZE; r++) {
            int srcRow = (GRID_SIZE - 1 - r) * GRID_SIZE;
            int dstRow = r * GRID_SIZE;
            for (int c = 0; c < GRID_SIZE; c++) {
                int rgba = readbackArray[srcRow + c];
                int a = (rgba >> 24) & 0xFF;
                int b = (rgba >> 16) & 0xFF;
                int g = (rgba >> 8) & 0xFF;
                int red = rgba & 0xFF;
                buffer1.pixels[dstRow + c] = (a << 24) | (red << 16) | (g << 8) | b;
            }
        }
        buffer1.updatePixels();
        buffer1.endDraw();

        // ── Done ────────────────────────────────────────────────────────
        computeProgress = iterations;
        computeTime = p.millis() - computeStartTime;
        isComputing = false;
        hasComputed = true;

        System.out.println("[GPU V6] Done in " + computeTime + "ms (" + numChunks
                + " chunks × " + TDR_CHUNK + " max = " + iterations + " gens)");
    }

    /**
     * V6.0: No-op — all work completes inside startCompute() now.
     * Kept for API compatibility with GPULabScreen.
     */
    public void processBatch(int targetIterations) {
        // Everything completes in startCompute() — nothing to do here.
    }

    // ═══════════════════════════════════════════════════════
    //       LIVE SIMULATION (single-step mode — unchanged)
    // ═══════════════════════════════════════════════════════

    /**
     * Runs exactly ONE generation of Conway's rules on the GPU.
     * Used by SimulationScreen for real-time GPU-accelerated gameplay.
     * This path uses Processing's API and the V2 single-step shader
     * (no performance issue for 1 step per frame).
     *
     * @param board  The Grid to read current state from
     */
    public void stepOneGeneration(Grid board) {
        buffer1.beginDraw();
        buffer1.loadPixels();
        byte[] arr = board.boardFront;
        for (int i = 0; i < arr.length; i++) {
            buffer1.pixels[i] = arr[i] != 0 ? 0xFF000000 : 0xFFFFFFFF;
        }
        buffer1.updatePixels();
        buffer1.endDraw();

        buffer2.beginDraw();
        buffer2.shader(conwayShader);
        buffer2.clear();
        buffer2.image(buffer1, 0, 0);
        buffer2.resetShader();
        buffer2.endDraw();

        PGraphics t = buffer1; buffer1 = buffer2; buffer2 = t;
    }

    /**
     * Reads the GPU result buffer back into the Grid data model.
     * Called after stepOneGeneration() to keep the Grid in sync.
     *
     * @param board  The Grid to write the GPU result back into
     */
    public void readBackToGrid(Grid board) {
        buffer1.loadPixels();
        int cols = GRID_SIZE;
        for (int i = 0; i < buffer1.pixels.length; i++) {
            int pixel = buffer1.pixels[i];
            boolean alive = (pixel & 0xFF) < 128;
            board.setCellState(i / cols, i % cols, alive);
        }
    }

    /** Returns the buffer containing the most recent computed result image. */
    public PGraphics getResultBuffer() {
        return buffer1;
    }
}
