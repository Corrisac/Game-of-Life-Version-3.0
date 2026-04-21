package game.of.life.on.gpu.gpu;

import game.of.life.on.gpu.engine.Grid;
import game.of.life.on.gpu.engine.SimulationRules;

import com.jogamp.opengl.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.stream.Collectors;

/**
 * GPUBackend V7.0 — Direct JOGL GL4 compute shader pipeline.
 * No Processing dependency. Uses headless GLAutoDrawable for GPU compute.
 *
 * TWO MODES:
 *   1. GPU COMPUTE (if GL4.3 available): Full V6 single-WG bit-parallel pipeline
 *   2. CPU FALLBACK (if no GL4.3): Batched CPU simulation
 */
public class GPUBackend {

    // GL4 constants not in all JOGL interfaces
    private static final int GL_COMPUTE_SHADER = 0x91B9;
    private static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020;
    private static final int GL_READ_ONLY  = 0x88B8;
    private static final int GL_WRITE_ONLY = 0x88B9;
    private static final int GL_RGBA8      = 0x8058;
    private static final int TDR_CHUNK     = 500_000;
    private static final int GS = SimulationRules.GRID_SIZE;

    // GL resources
    private GLAutoDrawable glDrawable;
    private GL4 gl4;
    private boolean gpuReady = false;
    private int computeProgram;
    private int[] computeTex = new int[2];
    private int[] computeFbo = new int[1];
    private int computeUIter;

    // Pre-allocated buffers
    private ByteBuffer pixelBuffer;
    private int[] readbackArray;

    // Compute state
    private Grid resultGrid;
    private boolean computing = false;
    private boolean hasComputed = false;
    private int computeProgress = 0;
    private int targetIterations = 0;
    private long computeStartTime = 0;
    private long computeTime = 0;

    // CPU fallback
    private boolean useCPU = false;
    private static final int CPU_BATCH_SIZE = 5000;

    public GPUBackend() {
        resultGrid = new Grid(GS, GS);
        initGPU();
    }

    // ═══════════════════════════════════════════════════════════
    //       GPU INITIALIZATION
    // ═══════════════════════════════════════════════════════════

    private void initGPU() {
        try {
            System.out.println("[GPU V7] Initializing headless GL4 context...");

            // Create a headless GL4 context (no window needed for compute)
            GLProfile profile = GLProfile.get(GLProfile.GL4);
            GLCapabilities caps = new GLCapabilities(profile);
            caps.setOnscreen(false);
            caps.setHardwareAccelerated(true);

            GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
            glDrawable = factory.createOffscreenAutoDrawable(
                factory.getDefaultDevice(), caps, null, GS, GS);
            glDrawable.display(); // Force context creation

            GLContext ctx = glDrawable.getContext();
            int result = ctx.makeCurrent();
            if (result == GLContext.CONTEXT_NOT_CURRENT) {
                throw new RuntimeException("Could not make GL context current");
            }

            GL glBase = glDrawable.getGL();
            if (!glBase.isGL4()) {
                System.err.println("[GPU V7] GL4 not available, falling back to CPU.");
                useCPU = true;
                ctx.release();
                return;
            }

            gl4 = glBase.getGL4();
            System.out.println("[GPU V7] GL4 context created. Renderer: "
                + gl4.glGetString(GL.GL_RENDERER));

            // Create compute textures
            gl4.glGenTextures(2, computeTex, 0);
            for (int i = 0; i < 2; i++) {
                gl4.glBindTexture(GL.GL_TEXTURE_2D, computeTex[i]);
                gl4.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL_RGBA8, GS, GS,
                                 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
                gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
                gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
                gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
                gl4.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            }
            gl4.glBindTexture(GL.GL_TEXTURE_2D, 0);

            // Create readback FBO
            gl4.glGenFramebuffers(1, computeFbo, 0);

            // Load and compile compute shader
            String src = loadShaderSource("shaders/conway_compute.glsl");
            if (src == null) {
                System.err.println("[GPU V7] Failed to load conway_compute.glsl, falling back to CPU.");
                useCPU = true;
                ctx.release();
                return;
            }

            int cs = gl4.glCreateShader(GL_COMPUTE_SHADER);
            gl4.glShaderSource(cs, 1, new String[]{src}, null);
            gl4.glCompileShader(cs);

            int[] compiled = new int[1];
            gl4.glGetShaderiv(cs, GL2ES2.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == GL.GL_FALSE) {
                int[] logLen = new int[1];
                gl4.glGetShaderiv(cs, GL2ES2.GL_INFO_LOG_LENGTH, logLen, 0);
                byte[] log = new byte[Math.max(1, logLen[0])];
                gl4.glGetShaderInfoLog(cs, log.length, null, 0, log, 0);
                System.err.println("[GPU V7] Compute shader compile error: " + new String(log));
                useCPU = true;
                ctx.release();
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
                System.err.println("[GPU V7] Compute program link error: " + new String(log));
                useCPU = true;
                ctx.release();
                return;
            }
            gl4.glDeleteShader(cs);

            computeUIter = gl4.glGetUniformLocation(computeProgram, "uIterations");

            // Pre-allocate pixel buffers
            int bufSize = GS * GS * 4;
            pixelBuffer = ByteBuffer.allocateDirect(bufSize);
            pixelBuffer.order(ByteOrder.nativeOrder());
            readbackArray = new int[GS * GS];

            ctx.release();
            gpuReady = true;
            System.out.println("[GPU V7] Compute shader ready. Program=" + computeProgram
                + " Textures=" + computeTex[0] + "," + computeTex[1]);

        } catch (Exception e) {
            System.err.println("[GPU V7] GPU init failed: " + e.getMessage()
                + " — falling back to CPU.");
            useCPU = true;
        }
    }

    private String loadShaderSource(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            System.err.println("[GPU V7] Error loading shader: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //       COMPUTE DISPATCH
    // ═══════════════════════════════════════════════════════════

    /**
     * Starts a compute job. If GPU is available, runs the full V6
     * single-WG bit-parallel compute shader pipeline. Otherwise CPU fallback.
     */
    public void startCompute(Grid inputGrid, int iterations) {
        if (iterations <= 0) return;

        // Copy input grid to result grid
        resultGrid.clearBoard();
        System.arraycopy(inputGrid.boardFront, 0, resultGrid.boardFront, 0,
                         inputGrid.boardFront.length);
        resultGrid.recount();

        targetIterations = iterations;
        computeProgress = 0;
        computeStartTime = System.currentTimeMillis();
        computing = true;
        hasComputed = false;

        if (gpuReady && !useCPU) {
            // GPU path: run everything now (blocking but fast)
            runGPUCompute(inputGrid, iterations);
        }
        // CPU path: processBatch() will handle it frame by frame
    }

    private void runGPUCompute(Grid inputGrid, int iterations) {
        try {
            GLContext ctx = glDrawable.getContext();
            ctx.makeCurrent();

            int numChunks = (iterations + TDR_CHUNK - 1) / TDR_CHUNK;
            System.out.println("[GPU V7] Starting compute: " + iterations + " iters → "
                + numChunks + " chunks (max " + TDR_CHUNK + " iters/chunk)");

            // Upload grid state to computeTex[0]
            pixelBuffer.clear();
            byte[] arr = inputGrid.boardFront;
            for (int i = 0; i < arr.length; i++) {
                byte val = arr[i] != 0 ? (byte) 0 : (byte) 0xFF;
                pixelBuffer.put(val); pixelBuffer.put(val);
                pixelBuffer.put(val); pixelBuffer.put((byte) 0xFF);
            }
            pixelBuffer.flip();

            gl4.glBindTexture(GL.GL_TEXTURE_2D, computeTex[0]);
            gl4.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, GS, GS,
                                GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
            gl4.glBindTexture(GL.GL_TEXTURE_2D, 0);

            // Bind compute program
            gl4.glUseProgram(computeProgram);

            // TDR-safe chunked dispatch
            int remaining = iterations;
            int src = 0;

            while (remaining > 0) {
                int chunk = Math.min(remaining, TDR_CHUNK);

                gl4.glBindImageTexture(0, computeTex[src], 0, false, 0, GL_READ_ONLY, GL_RGBA8);
                gl4.glBindImageTexture(1, computeTex[1 - src], 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);
                gl4.glUniform1i(computeUIter, chunk);
                gl4.glDispatchCompute(1, 1, 1);
                gl4.glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
                gl4.glFinish();

                src = 1 - src;
                remaining -= chunk;
                computeProgress += chunk;
            }

            gl4.glUseProgram(0);

            // Read result back
            int resultTex = src;
            gl4.glBindFramebuffer(GL.GL_FRAMEBUFFER, computeFbo[0]);
            gl4.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
                                        GL.GL_TEXTURE_2D, computeTex[resultTex], 0);

            pixelBuffer.clear();
            gl4.glReadPixels(0, 0, GS, GS, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
            gl4.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

            // Convert pixels to grid (flip Y: GL is bottom-up)
            pixelBuffer.rewind();
            IntBuffer intView = pixelBuffer.asIntBuffer();
            intView.get(readbackArray);

            for (int r = 0; r < GS; r++) {
                int srcRow = (GS - 1 - r) * GS;
                for (int c = 0; c < GS; c++) {
                    int rgba = readbackArray[srcRow + c];
                    boolean alive = (rgba & 0xFF) < 128;
                    resultGrid.setCellState(r, c, alive);
                }
            }

            ctx.release();

            computeTime = System.currentTimeMillis() - computeStartTime;
            computing = false;
            hasComputed = true;

            System.out.println("[GPU V7] Done in " + computeTime + "ms ("
                + numChunks + " chunks × " + TDR_CHUNK + " max = " + iterations + " gens)");

        } catch (Exception e) {
            System.err.println("[GPU V7] GPU compute failed: " + e.getMessage()
                + " — switching to CPU fallback.");
            useCPU = true;
            // Reset for CPU fallback
            computing = true;
            computeProgress = 0;
        }
    }

    /**
     * Processes a batch of iterations (CPU fallback only).
     * GPU path completes everything in startCompute().
     */
    public boolean processBatch(int totalIterations) {
        if (!computing) return false;

        // GPU path already completed in startCompute()
        if (gpuReady && !useCPU) return false;

        // CPU fallback: process a batch per frame
        int remaining = targetIterations - computeProgress;
        int batchCount = Math.min(remaining, CPU_BATCH_SIZE);

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

    public boolean isComputing()       { return computing; }
    public boolean hasComputed()       { return hasComputed; }
    public int getComputeProgress()    { return computeProgress; }
    public long getComputeTime()       { return computeTime; }
    public Grid getResultGrid()        { return resultGrid; }
    public boolean isGPUAvailable()    { return gpuReady && !useCPU; }
}
