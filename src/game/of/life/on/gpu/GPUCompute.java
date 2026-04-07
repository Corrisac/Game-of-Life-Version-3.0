import processing.core.PApplet;    // Processing core for millis(), createGraphics(), loadShader()
import processing.core.PGraphics;  // Offscreen graphics buffer for ping-pong rendering
import processing.opengl.PShader;  // GLSL shader wrapper for GPU computation
import processing.opengl.PGL;      // Processing's OpenGL abstraction layer
import processing.opengl.PJOGL;    // Processing's JOGL-specific OpenGL implementation

import com.jogamp.opengl.GL;       // OpenGL base constants (GL_TEXTURE_2D, GL_FRAMEBUFFER, etc.)
import com.jogamp.opengl.GL2ES2;   // OpenGL ES 2.0 / GL 2.0+ interface for shaders & FBOs
import com.jogamp.opengl.GL2GL3;   // Additional constants for CLAMP_TO_EDGE, RGBA8, etc.

import java.nio.ByteBuffer;        // Direct byte buffer for pixel transfer to raw GL textures
import java.nio.FloatBuffer;       // Float buffer for fullscreen quad vertex data
import java.nio.ByteOrder;         // Byte order for native-endian buffer allocation
import java.nio.IntBuffer;         // Int buffer for GL object handles (textures, FBOs, VBOs)

/**
 * GPUCompute V3.0 — Raw JOGL compute loop + K=4 multi-step shader.
 *
 * V3.0 IMPROVEMENTS OVER V2.0:
 *   1. MULTI-STEP SHADER (K=4) — conway4.glsl computes 4 generations per
 *      fragment pass. Cuts render passes by 4×.
 *   2. RAW JOGL LOOP — Bypasses Processing's beginDraw/endDraw overhead.
 *      Uses direct GL calls: 3 per pass instead of ~25. Combined with K=4,
 *      reduces total GL overhead from 25M to 750K for 1M iterations (33× less).
 *   3. ZERO-COPY INIT — Uploads grid state directly to a raw GL texture
 *      via glTexSubImage2D, bypassing Processing's pixel[] copy.
 *
 * ARCHITECTURE:
 *   - Live simulation (stepOneGeneration): Uses Processing API + conway.glsl
 *     (unchanged from V2.0 — only 1 step/frame, no performance issue).
 *   - GPU Lab bulk compute (startCompute/processBatch): Uses raw JOGL +
 *     conway4.glsl for maximum throughput.
 *
 * RAW GL RESOURCES (created once in initRawGL):
 *   - 2 GL textures (rawTex[0], rawTex[1]) for ping-pong
 *   - 2 GL framebuffers (rawFbo[0], rawFbo[1]) attached to those textures
 *   - 1 compiled GL program (rawProgram) from conway4.glsl
 *   - 1 fullscreen quad VBO (quadVBO)
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
    //       V3.0: RAW JOGL RESOURCES
    // ═══════════════════════════════════════════════════════
    private boolean rawGLReady = false;   // True once initRawGL() has successfully completed
    private int[] rawTex = new int[2];    // Two raw GL texture handles for ping-pong
    private int[] rawFbo = new int[2];    // Two raw GL framebuffer handles for ping-pong
    private int rawProgram;               // Compiled GL shader program (conway4.glsl)
    private int quadVBO;                  // Vertex buffer object for the fullscreen quad
    private int rawPingPong = 0;          // Current ping-pong index (0 or 1)
    private int uTexture;                 // Uniform location for 'texture' sampler
    private int uTexOffset;               // Uniform location for 'texOffset' vec2
    private int aPosition;                // Attribute location for vertex position
    private int aTexCoord;                // Attribute location for texture coordinate

    // ═══════════════════════════════════════════════════════
    //       V2.0 ADAPTIVE BATCH SIZING (retained for progress updates)
    // ═══════════════════════════════════════════════════════
    // V3.0 uses raw GL batching, but we still chunk iterations to allow
    // progress bar updates between chunks.
    private int batchSize = 2000;                 // Iterations per progress-update chunk
    private static final int MIN_BATCH = 500;     // Floor for adaptive sizing
    private static final int MAX_BATCH = 100000;  // Ceiling for adaptive sizing

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

    // ═══════════════════════════════════════════════════════
    //       V3.0: RAW JOGL INITIALIZATION
    // ═══════════════════════════════════════════════════════

    /**
     * V3.0: Initializes raw JOGL resources — called once on first compute.
     * Extracts the GL2ES2 context from Processing, creates textures, FBOs,
     * compiles conway4.glsl as a raw GL program, and sets up the quad VBO.
     */
    private void initRawGL() {
        if (rawGLReady) return;  // Already initialized

        System.out.println("[GPU V3] Initializing raw JOGL resources...");

        // ── Must open draw context before accessing PGL ─────────────────
        buffer1.beginDraw();                                // Open draw context (required for GL)
        PGL pgl = buffer1.beginPGL();                       // Access Processing's OpenGL layer
        GL2ES2 gl = ((PJOGL) pgl).gl.getGL2ES2();          // Get the raw JOGL GL2ES2 interface

        // ── Create two raw GL textures for ping-pong ────────────────────
        gl.glGenTextures(2, rawTex, 0);                     // Allocate 2 texture handles
        for (int i = 0; i < 2; i++) {
            gl.glBindTexture(GL.GL_TEXTURE_2D, rawTex[i]);
            // Allocate RGBA8 storage at GRID_SIZE × GRID_SIZE
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, GRID_SIZE, GRID_SIZE,
                            0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
            // NEAREST filtering for pixel-perfect cellular automaton
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            // REPEAT wrapping for toroidal boundary conditions
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        }

        // ── Create two raw GL framebuffers, each attached to a texture ──
        gl.glGenFramebuffers(2, rawFbo, 0);                 // Allocate 2 FBO handles
        for (int i = 0; i < 2; i++) {
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, rawFbo[i]);
            gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
                                      GL.GL_TEXTURE_2D, rawTex[i], 0);
            // Verify FBO completeness
            int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
            if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
                System.err.println("[GPU V3] FBO " + i + " incomplete! Status: " + status);
            }
        }
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);         // Unbind FBO

        // ── Compile conway4.glsl as a raw GL program ────────────────────
        rawProgram = compileShaderProgram(gl);
        if (rawProgram == 0) {
            System.err.println("[GPU V3] Shader compilation failed! Falling back to V2 path.");
            buffer1.endPGL();
            buffer1.endDraw();
            return;
        }

        // ── Get uniform and attribute locations ─────────────────────────
        uTexture   = gl.glGetUniformLocation(rawProgram, "texture");
        uTexOffset = gl.glGetUniformLocation(rawProgram, "texOffset");
        aPosition  = gl.glGetAttribLocation(rawProgram, "position");
        aTexCoord  = gl.glGetAttribLocation(rawProgram, "texCoord");

        // If the shader uses Processing's attribute names, try those
        if (aPosition < 0) aPosition = gl.glGetAttribLocation(rawProgram, "vertex");
        if (aTexCoord < 0) aTexCoord = gl.glGetAttribLocation(rawProgram, "texCoord");

        // ── Create fullscreen quad VBO ──────────────────────────────────
        // Two triangles covering the full screen with matching UVs
        float[] quadData = {
            // x,    y,    u,    v
            -1.0f, -1.0f, 0.0f, 0.0f,   // Bottom-left
             1.0f, -1.0f, 1.0f, 0.0f,   // Bottom-right
            -1.0f,  1.0f, 0.0f, 1.0f,   // Top-left
             1.0f,  1.0f, 1.0f, 1.0f    // Top-right
        };
        FloatBuffer quadBuf = FloatBuffer.wrap(quadData);
        int[] vbo = new int[1];
        gl.glGenBuffers(1, vbo, 0);
        quadVBO = vbo[0];
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, quadData.length * 4, quadBuf, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        buffer1.endPGL();  // Release Processing's GL context
        buffer1.endDraw(); // Close draw context

        rawGLReady = true;
        System.out.println("[GPU V3] Raw JOGL init complete. Textures: "
                + rawTex[0] + "," + rawTex[1] + " FBOs: " + rawFbo[0] + "," + rawFbo[1]);
    }

    /**
     * Compiles conway4.glsl vertex+fragment shaders into a linked GL program.
     * Uses a minimal pass-through vertex shader that forwards position and UVs.
     */
    private int compileShaderProgram(GL2ES2 gl) {
        // ── Minimal vertex shader ───────────────────────────────────────
        String vertSrc =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "attribute vec2 position;\n" +
            "attribute vec2 texCoord;\n" +
            "varying vec4 vertTexCoord;\n" +
            "varying vec4 vertColor;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(position, 0.0, 1.0);\n" +
            "  vertTexCoord = vec4(texCoord, 0.0, 1.0);\n" +
            "  vertColor = vec4(1.0);\n" +
            "}\n";

        // ── Load fragment shader source from file ───────────────────────
        String fragSrc = loadShaderSource("conway4.glsl");
        if (fragSrc == null) {
            System.err.println("[GPU V3] Could not load conway4.glsl!");
            return 0;
        }

        // ── Compile vertex shader ───────────────────────────────────────
        int vs = gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER);
        gl.glShaderSource(vs, 1, new String[]{vertSrc}, null);
        gl.glCompileShader(vs);
        if (!checkShaderCompile(gl, vs, "vertex")) return 0;

        // ── Compile fragment shader ─────────────────────────────────────
        int fs = gl.glCreateShader(GL2ES2.GL_FRAGMENT_SHADER);
        gl.glShaderSource(fs, 1, new String[]{fragSrc}, null);
        gl.glCompileShader(fs);
        if (!checkShaderCompile(gl, fs, "fragment")) return 0;

        // ── Link program ────────────────────────────────────────────────
        int prog = gl.glCreateProgram();
        gl.glAttachShader(prog, vs);
        gl.glAttachShader(prog, fs);
        gl.glLinkProgram(prog);

        int[] linked = new int[1];
        gl.glGetProgramiv(prog, GL2ES2.GL_LINK_STATUS, linked, 0);
        if (linked[0] == GL.GL_FALSE) {
            int[] logLen = new int[1];
            gl.glGetProgramiv(prog, GL2ES2.GL_INFO_LOG_LENGTH, logLen, 0);
            byte[] log = new byte[logLen[0]];
            gl.glGetProgramInfoLog(prog, logLen[0], null, 0, log, 0);
            System.err.println("[GPU V3] Program link error: " + new String(log));
            return 0;
        }

        // Clean up individual shaders (they're now part of the program)
        gl.glDeleteShader(vs);
        gl.glDeleteShader(fs);

        System.out.println("[GPU V3] Shader program compiled & linked successfully (ID=" + prog + ")");
        return prog;
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
    //       GPU LAB: BULK COMPUTATION (V3.0 raw JOGL path)
    // ═══════════════════════════════════════════════════════

    /**
     * V3.0: Copies grid state into raw GL texture and starts bulk computation.
     * Uses direct pixel upload via glTexSubImage2D.
     *
     * @param board       Grid data model with current cell states
     * @param iterations  Total number of generations to compute
     */
    public void startCompute(Grid board, int iterations) {
        if (iterations <= 0) return;                                       // Abort if no work

        // Initialize raw GL resources on first use
        if (!rawGLReady) {
            initRawGL();
        }

        // If raw GL init failed, fall back to V2 Processing path
        if (!rawGLReady) {
            startComputeV2(board, iterations);
            return;
        }

        System.out.println("[GPU V3] Starting compute: " + iterations + " iterations"
                + " (K=4, effective passes=" + ((iterations + 3) / 4) + ")");

        // ── Upload grid state to rawTex[0] via glTexSubImage2D ──────────
        ByteBuffer pixelBuf = ByteBuffer.allocateDirect(GRID_SIZE * GRID_SIZE * 4);
        pixelBuf.order(ByteOrder.nativeOrder());
        byte[] arr = board.boardFront;
        for (int i = 0; i < arr.length; i++) {
            byte val = arr[i] != 0 ? (byte) 0 : (byte) 0xFF;  // alive=black, dead=white
            pixelBuf.put(val);  // R
            pixelBuf.put(val);  // G
            pixelBuf.put(val);  // B
            pixelBuf.put((byte) 0xFF);  // A = fully opaque
        }
        pixelBuf.flip();

        buffer1.beginDraw();                              // Open draw context for GL access
        PGL pgl = buffer1.beginPGL();
        GL2ES2 gl = ((PJOGL) pgl).gl.getGL2ES2();
        gl.glBindTexture(GL.GL_TEXTURE_2D, rawTex[0]);
        gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, GRID_SIZE, GRID_SIZE,
                           GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuf);
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        buffer1.endPGL();
        buffer1.endDraw();                                // Close draw context

        rawPingPong = 0;                   // Source = rawTex[0]
        computeProgress = 0;               // Reset progress
        totalTargetIters = iterations;     // Store total target
        computeStartTime = p.millis();     // Record start time
        isComputing = true;                // Begin computing
        hasComputed = false;               // Invalidate previous result
    }

    /**
     * V3.0: Processes one batch of iterations using the raw JOGL compute loop.
     * Called every frame from the draw loop while isComputing is true.
     *
     * The K=4 shader computes 4 generations per pass, so we divide the batch
     * by 4 to get the number of actual render passes.
     *
     * Each render pass uses only 3 GL calls:
     *   1. glBindFramebuffer (set destination)
     *   2. glBindTexture (set source)
     *   3. glDrawArrays (execute shader)
     *
     * @param targetIterations  Total iteration target to reach before stopping
     */
    public void processBatch(int targetIterations) {
        // Fall back to V2 path if raw GL isn't available
        if (!rawGLReady) {
            processBatchV2(targetIterations);
            return;
        }

        int remaining = targetIterations - computeProgress;                // How many generations left
        int batchGens = Math.min(remaining, batchSize);                    // Generations this batch
        // Round up to multiple of 4 (K=4 shader does 4 per pass)
        int passes = (batchGens + 3) / 4;                                  // Render passes needed
        int actualGens = passes * 4;                                       // Actual generations computed

        long batchStart = p.millis();

        // ── Open Processing's GL context ────────────────────────────────
        buffer1.beginDraw();                              // Open draw context for GL access
        PGL pgl = buffer1.beginPGL();
        GL2ES2 gl = ((PJOGL) pgl).gl.getGL2ES2();

        // ── Bind shader program ONCE for entire batch ───────────────────
        gl.glUseProgram(rawProgram);
        gl.glUniform1i(uTexture, 0);                                       // Texture unit 0
        gl.glUniform2f(uTexOffset, 1.0f / GRID_SIZE, 1.0f / GRID_SIZE);   // Pixel step size

        // ── Bind quad VBO and set up vertex attributes ONCE ─────────────
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, quadVBO);
        if (aPosition >= 0) {
            gl.glEnableVertexAttribArray(aPosition);
            gl.glVertexAttribPointer(aPosition, 2, GL.GL_FLOAT, false, 16, 0);  // stride=16, offset=0
        }
        if (aTexCoord >= 0) {
            gl.glEnableVertexAttribArray(aTexCoord);
            gl.glVertexAttribPointer(aTexCoord, 2, GL.GL_FLOAT, false, 16, 8);  // stride=16, offset=8
        }

        gl.glViewport(0, 0, GRID_SIZE, GRID_SIZE);                        // Match texture dimensions
        gl.glDisable(GL.GL_BLEND);                                         // No blending needed
        gl.glDisable(GL.GL_DEPTH_TEST);                                    // No depth testing

        // ══════════════════════════════════════════════════════════════════
        //  THE NUCLEAR LOOP — 3 GL calls per pass, K=4 gens per pass
        // ══════════════════════════════════════════════════════════════════
        for (int i = 0; i < passes; i++) {
            int src = rawPingPong;                                          // Current source texture index
            int dst = 1 - rawPingPong;                                     // Destination texture index

            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, rawFbo[dst]);          // 1. Set destination FBO
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, rawTex[src]);               // 2. Set source texture
            gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);                   // 3. Execute shader

            rawPingPong = dst;                                              // Swap for next pass
        }

        // ── Clean up GL state ───────────────────────────────────────────
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);                        // Unbind FBO
        if (aPosition >= 0) gl.glDisableVertexAttribArray(aPosition);
        if (aTexCoord >= 0) gl.glDisableVertexAttribArray(aTexCoord);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glUseProgram(0);                                                // Unbind shader

        buffer1.endPGL();                                                  // Release Processing's GL
        buffer1.endDraw();                                                 // Close draw context

        // ── Update progress (clamp to target) ───────────────────────────
        computeProgress = Math.min(computeProgress + actualGens, targetIterations);

        // ── Adaptive batch sizing ───────────────────────────────────────
        long batchMs = p.millis() - batchStart;
        if (batchMs < 12 && batchSize < MAX_BATCH)
            batchSize = Math.min(batchSize * 2, MAX_BATCH);
        else if (batchMs > 14 && batchSize > MIN_BATCH)
            batchSize = Math.max(batchSize / 2, MIN_BATCH);

        // ── Check completion ────────────────────────────────────────────
        if (computeProgress >= targetIterations) {
            computeTime = p.millis() - computeStartTime;                   // Total elapsed
            isComputing = false;                                           // Done
            hasComputed = true;                                            // Result ready

            // Read result back into Processing's buffer1 for display
            readRawTexToBuffer();

            System.out.println("[GPU V3] Compute done in " + computeTime + "ms"
                    + " (" + ((targetIterations + 3) / 4) + " passes, final batch=" + batchSize + ")");
        }
    }

    /**
     * V3.0: Reads the raw GL result texture back into Processing's buffer1
     * so it can be displayed by GPULabScreen.
     */
    private void readRawTexToBuffer() {
        ByteBuffer pixelBuf = ByteBuffer.allocateDirect(GRID_SIZE * GRID_SIZE * 4);
        pixelBuf.order(ByteOrder.nativeOrder());

        buffer1.beginDraw();                              // Open draw context for GL access
        PGL pgl = buffer1.beginPGL();
        GL2ES2 gl = ((PJOGL) pgl).gl.getGL2ES2();

        // Read pixels from the current result FBO
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, rawFbo[rawPingPong]);
        gl.glReadPixels(0, 0, GRID_SIZE, GRID_SIZE, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuf);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

        buffer1.endPGL();

        // Copy pixel data into Processing's buffer1 for rendering
        buffer1.loadPixels();
        pixelBuf.rewind();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int red   = pixelBuf.get() & 0xFF;
                int green = pixelBuf.get() & 0xFF;
                int blue  = pixelBuf.get() & 0xFF;
                int alpha = pixelBuf.get() & 0xFF;
                // GL reads bottom-up, Processing is top-down — flip Y
                buffer1.pixels[(GRID_SIZE - 1 - r) * GRID_SIZE + c] =
                    (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        buffer1.updatePixels();
        buffer1.endDraw();  // Close draw context (opened at top of method)
    }

    // ═══════════════════════════════════════════════════════
    //       V2.0 FALLBACK (Processing API path)
    // ═══════════════════════════════════════════════════════

    /**
     * V2.0 fallback: Copies grid state into Processing buffer and starts compute.
     * Used only if raw GL initialization fails.
     */
    private void startComputeV2(Grid board, int iterations) {
        System.out.println("[GPU V2 FALLBACK] Starting compute: " + iterations + " iterations");
        buffer1.beginDraw();
        buffer1.loadPixels();
        byte[] arr = board.boardFront;
        for (int i = 0; i < arr.length; i++) {
            buffer1.pixels[i] = arr[i] != 0 ? 0xFF000000 : 0xFFFFFFFF;
        }
        buffer1.updatePixels();
        buffer1.endDraw();
        computeProgress = 0;
        computeStartTime = p.millis();
        totalTargetIters = iterations;
        isComputing = true;
        hasComputed = false;
    }

    /**
     * V2.0 fallback: Processes one batch using Processing's API.
     * Used only if raw GL initialization fails.
     */
    private void processBatchV2(int targetIterations) {
        int remaining = targetIterations - computeProgress;
        int batch = Math.min(remaining, batchSize);
        long batchStart = p.millis();

        buffer2.beginDraw();
        buffer2.shader(conwayShader);

        for (int i = 0; i < batch; i++) {
            buffer2.clear();
            buffer2.image(buffer1, 0, 0);
            buffer2.endDraw();
            PGraphics t = buffer1; buffer1 = buffer2; buffer2 = t;
            if (i < batch - 1) {
                buffer2.beginDraw();
                buffer2.shader(conwayShader);
            }
        }

        computeProgress += batch;
        long batchMs = p.millis() - batchStart;
        if (batchMs < 12 && batchSize < MAX_BATCH)
            batchSize = Math.min(batchSize * 2, MAX_BATCH);
        else if (batchMs > 14 && batchSize > MIN_BATCH)
            batchSize = Math.max(batchSize / 2, MIN_BATCH);

        if (computeProgress >= targetIterations) {
            computeTime = p.millis() - computeStartTime;
            isComputing = false;
            hasComputed = true;
            System.out.println("[GPU V2 FALLBACK] Done in " + computeTime + "ms");
        }
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
