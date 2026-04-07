import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * Main V4.0 — Adds smooth screen transitions, mouse wheel zoom delegation,
 * and shared state for rule sets and heatmap.
 */
public class Main extends PApplet implements ThemeConstants {

    Grid gameBoard;
    BackgroundScreen bgScreen;
    int appState = 0;       // 0=Menu, 1=Theory, 2=Simulation, 3=GPU Lab
    boolean isPaused = true;
    int brushSize = 1;

    private GPUCompute gpuCompute;
    public GPUCompute getGpuCompute() { return gpuCompute; }
    ParticleSystem particleSystem;
    MenuScreen menuScreen;
    SimulationScreen simScreen;
    GPULabScreen gpuLabScreen;

    // V5.0: Frame-rate independent timing
    float deltaTime = 1f / 60f;     // Seconds since last frame (initialized to 60fps)
    private long lastFrameNanos = 0; // Nanosecond timestamp of previous frame

    // V5.0: DPI-aware UI scale factor
    public float uiScale = 1.0f;

    // V4.0: Screen transitions
    int transPhase = 0;     // 0=idle, 1=fading out, 2=fading in
    float transAlpha = 0;   // 0..1 overlay darkness
    int pendingState = -1;

    public static void main(String[] args) {
        try { System.loadLibrary("jawt"); } catch (UnsatisfiedLinkError e) {}
        PApplet.main("Main");
    }

    public void settings() {
        size(1280, 720, P2D);
        pixelDensity(1);
        noSmooth();
    }

    public void setup() {
        surface.setResizable(true);
        surface.setTitle("GAME OF LIFE V5.0 — DASHBOARD");

        // V5.0: Compute DPI scale factor for high-resolution displays
        uiScale = (float) displayDensity();
        if (uiScale < 1.0f) uiScale = 1.0f;

        // V5.0: Propagate scale factor to all UI configuration classes
        MenuUI.uiScale = uiScale;
        SimulationUI.uiScale = uiScale;
        GPULabUI.uiScale = uiScale;

        gameBoard = new Grid(GRID_SIZE, GRID_SIZE);
        bgScreen = new BackgroundScreen(this);

        // Seed menu background
        int seedCount = (GRID_SIZE * GRID_SIZE) / 12;
        for (int i = 0; i < seedCount; i++)
            gameBoard.setCellState((int) random(5, GRID_SIZE - 5),
                                   (int) random(5, GRID_SIZE - 5), true);

        gpuCompute = new GPUCompute(this);
        particleSystem = new ParticleSystem(this, 80);
        menuScreen = new MenuScreen(this);
        simScreen = new SimulationScreen(this);
        gpuLabScreen = new GPULabScreen(this);

        lastFrameNanos = System.nanoTime(); // Initialize timing baseline
    }

    /** V4.0: Triggers a smooth fade transition to the target screen. */
    public void transitionTo(int newState) {
        if (transPhase != 0) return;
        pendingState = newState;
        transPhase = 1;
        transAlpha = 0;
    }

    public void draw() {
        // V5.0: Compute deltaTime (clamped to prevent physics explosions on lag spikes)
        long now = System.nanoTime();
        deltaTime = (now - lastFrameNanos) / 1_000_000_000f;
        deltaTime = Math.max(0.001f, Math.min(deltaTime, 0.1f)); // Clamp 1ms..100ms
        lastFrameNanos = now;

        // V5.0: Time-based transition phases (~0.3s fade each direction)
        float transSpeed = 3.5f; // 1/0.3 ≈ 3.3, slightly faster for snappiness
        if (transPhase == 1) {
            transAlpha += transSpeed * deltaTime;
            if (transAlpha >= 1.0f) {
                transAlpha = 1.0f;
                appState = pendingState;
                // State-entry setup
                if (appState == 2) { gameBoard.clearBoard(); isPaused = true; }
                else if (appState == 3) { gameBoard.clearBoard(); gpuCompute.hasComputed = false; }
                transPhase = 2;
            }
        } else if (transPhase == 2) {
            transAlpha -= transSpeed * deltaTime;
            if (transAlpha <= 0) { transAlpha = 0; transPhase = 0; pendingState = -1; }
        }

        // GPU Lab batch processing
        if (gpuCompute.isComputing)
            gpuCompute.processBatch(gpuLabScreen.getIterationCount());

        // Render current screen
        if (appState == 0)      menuScreen.draw(particleSystem, deltaTime);
        else if (appState == 1) bgScreen.drawScreen(deltaTime);
        else if (appState == 2) simScreen.draw(deltaTime);
        else if (appState == 3) gpuLabScreen.draw(deltaTime);

        // V4.0: Transition overlay
        if (transAlpha > 0.001f) {
            noStroke();
            fill(10, 15, 25, transAlpha * 255);
            rect(0, 0, width, height);
        }
    }

    public void mousePressed() {
        if (transPhase != 0) return; // Block input during transitions
        if (appState == 0)
            menuScreen.handleMousePressed(mouseX, mouseY);
        else if (appState == 1) {
            if (mouseX > 20 && mouseX < 150 && mouseY > 14 && mouseY < 52)
                transitionTo(0);
        }
        else if (appState == 2)
            simScreen.handleMousePressed(mouseX, mouseY);
        else if (appState == 3)
            gpuLabScreen.handleMousePressed(mouseX, mouseY);
    }

    public void mouseDragged() {
        if (transPhase != 0) return;
        if (appState == 2) simScreen.handleMouseDragged(mouseX, mouseY);
        else if (appState == 3) gpuLabScreen.handleMouseDragged(mouseX, mouseY);
    }

    public void mouseReleased() {
        if (appState == 2) simScreen.handleMouseReleased();
        gpuLabScreen.handleMouseReleased();
    }

    public void keyPressed() {
        if (transPhase != 0) return;
        if (appState == 2) simScreen.handleKeyPressed();
        else if (appState == 3) gpuLabScreen.handleKeyPressed();
        else if (appState == 1) {
            if (keyCode == DOWN) bgScreen.scroll(3);
            else if (keyCode == UP) bgScreen.scroll(-3);
        }
    }

    public void mouseWheel(MouseEvent event) {
        if (transPhase != 0) return;
        if (appState == 1) bgScreen.scroll(event.getCount());
        else if (appState == 2) simScreen.handleMouseWheel(event.getCount());
    }
}