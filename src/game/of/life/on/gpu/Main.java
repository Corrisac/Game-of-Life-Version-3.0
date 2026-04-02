import processing.core.PApplet;     // Processing core — the main application framework
import processing.event.MouseEvent; // Processing event class for mouse wheel events

/**
 * Main — The central orchestrator of the Game of Life Dashboard application.
 * Extends Processing's PApplet to provide the window, draw loop, and event hooks.
 *
 * DESIGN: This class is now a slim coordinator that delegates rendering and input
 * to dedicated screen classes (MenuScreen, SimulationScreen, GPULabScreen) and
 * utility modules (GPUCompute, ParticleSystem, GridRenderer, PatternLibrary).
 * It holds only the shared state that multiple screens need to access.
 *
 * APPLICATION STATES:
 *   0 = Dashboard Menu
 *   1 = Background & Theory (handled by BackgroundScreen)
 *   2 = Interactive Simulation
 *   3 = GPU Compute Lab
 */
public class Main extends PApplet implements ThemeConstants {

    // ═══════════════════════════════════════════════════════
    //                   SHARED STATE
    // ═══════════════════════════════════════════════════════

    // The 60x60 grid data model holding all cell alive/dead states
    Grid gameBoard;

    // The scrollable theory/background knowledge screen
    BackgroundScreen bgScreen;

    // Current application state: 0=Menu, 1=Theory, 2=Simulation, 3=GPU Lab
    int appState = 0;

    // Whether the simulation is currently paused (applies to Simulation screen)
    boolean isPaused = true;

    // Current brush size for cell painting (1x1 or 3x3)
    int brushSize = 1;

    // ═══════════════════════════════════════════════════════
    //                  MODULE REFERENCES
    // ═══════════════════════════════════════════════════════

    // GPU computation module (shader, buffers, batching logic)
    private GPUCompute gpuCompute;

    /** Returns the GPU compute module (used by GPULabScreen and MenuScreen). */
    public GPUCompute getGpuCompute() { return gpuCompute; }

    // Ambient floating particle effect for the menu background
    ParticleSystem particleSystem;

    // Screen renderers — each owns its own UI components and handles its input
    MenuScreen menuScreen;
    SimulationScreen simScreen;
    GPULabScreen gpuLabScreen;

    // ═══════════════════════════════════════════════════════
    //                   ENTRY POINT
    // ═══════════════════════════════════════════════════════

    /**
     * main — JVM entry point. Loads the AWT native library (required by Processing
     * on some JDK/JOGL configurations) and launches the PApplet.
     */
    public static void main(String[] args) {
        // Attempt to load the Java AWT native library (silently ignore if unavailable)
        try { System.loadLibrary("jawt"); } catch (UnsatisfiedLinkError e) {}
        // Launch this class as a Processing sketch
        PApplet.main("Main");
    }

    // ═══════════════════════════════════════════════════════
    //                  PROCESSING LIFECYCLE
    // ═══════════════════════════════════════════════════════

    /**
     * settings — Called once before setup(). Configures the window size and renderer.
     * Must use P2D (OpenGL) renderer for GPU shader compatibility.
     */
    public void settings() {
        // Set the initial window size to 1280x720 using the OpenGL P2D renderer
        size(1280, 720, P2D);
        // Set pixel density to 1 (no HiDPI scaling) for consistent grid rendering
        pixelDensity(1);
        // Disable anti-aliasing for sharp pixel-level cell rendering
        noSmooth();
    }

    /**
     * setup — Called once after the window is created. Initializes all game state,
     * UI modules, and GPU compute resources.
     */
    public void setup() {
        // Allow the user to resize the application window
        surface.setResizable(true);
        // Set the window title bar text
        surface.setTitle("GAME OF LIFE — DASHBOARD");

        // Create the 60x60 game grid with all cells initially dead
        gameBoard = new Grid(60, 60);
        // Create the theory/background knowledge screen
        bgScreen = new BackgroundScreen(this);

        // Seed the grid with 300 random alive cells for the menu background animation
        for (int i = 0; i < 300; i++)
            gameBoard.setCellState((int) random(5, 55), (int) random(5, 55), true);

        // Initialize the GPU compute module (loads shader, creates ping-pong buffers)
        gpuCompute = new GPUCompute(this);
        // Initialize the ambient particle system with 80 floating particles
        particleSystem = new ParticleSystem(this, 80);

        // Create each screen module — each owns its own UI components
        menuScreen = new MenuScreen(this);
        simScreen = new SimulationScreen(this);
        gpuLabScreen = new GPULabScreen(this);
    }

    // ═══════════════════════════════════════════════════════
    //                     DRAW LOOP
    // ═══════════════════════════════════════════════════════

    /**
     * draw — Called every frame (~60 FPS). Processes any active GPU computation,
     * then delegates rendering to the current screen based on appState.
     */
    public void draw() {
        // If a GPU computation is in progress, process one batch of iterations this frame
        if (gpuCompute.isComputing)
            gpuCompute.processBatch(gpuLabScreen.getIterationCount());

        // Delegate rendering to the active screen
        if (appState == 0)      menuScreen.draw(particleSystem);  // Dashboard menu
        else if (appState == 1) bgScreen.drawScreen();            // Theory & background
        else if (appState == 2) simScreen.draw();                 // Interactive simulation
        else if (appState == 3) gpuLabScreen.draw();              // GPU compute lab
    }

    // ═══════════════════════════════════════════════════════
    //                   INPUT DELEGATION
    // ═══════════════════════════════════════════════════════

    /**
     * mousePressed — Processing callback for mouse clicks.
     * Delegates to the current screen's input handler based on appState.
     */
    public void mousePressed() {
        if (appState == 0)                                        // Menu screen
            menuScreen.handleMousePressed(mouseX, mouseY);
        else if (appState == 1) {                                 // Theory screen — back button
            if (mouseX > 20 && mouseX < 140 && mouseY > 20 && mouseY < 60)
                appState = 0;                                     // Return to menu
        }
        else if (appState == 2)                                   // Simulation screen
            simScreen.handleMousePressed(mouseX, mouseY);
        else if (appState == 3)                                   // GPU lab screen
            gpuLabScreen.handleMousePressed(mouseX, mouseY);
    }

    /**
     * mouseDragged — Processing callback for mouse drag events.
     * Delegates to the current screen for cell painting or dial dragging.
     */
    public void mouseDragged() {
        if (appState == 2)                                        // Simulation: paint cells
            simScreen.handleMouseDragged(mouseX, mouseY);
        else if (appState == 3)                                   // GPU lab: dial or paint cells
            gpuLabScreen.handleMouseDragged(mouseX, mouseY);
    }

    /**
     * mouseReleased — Processing callback for mouse release.
     * Releases the GPU dial drag handle if active.
     */
    public void mouseReleased() {
        gpuLabScreen.handleMouseReleased();                       // End any active dial drag
    }

    /**
     * keyPressed — Processing callback for keyboard input.
     * Delegates to the current screen's key handler.
     */
    public void keyPressed() {
        if (appState == 2)                                        // Simulation shortcuts
            simScreen.handleKeyPressed();
        else if (appState == 3)                                   // GPU lab iteration input
            gpuLabScreen.handleKeyPressed();
        else if (appState == 1) {                                 // Theory screen scrolling
            if (keyCode == DOWN) bgScreen.scroll(3);              // Scroll down
            else if (keyCode == UP) bgScreen.scroll(-3);          // Scroll up
        }
    }

    /**
     * mouseWheel — Processing callback for mouse scroll wheel.
     * Scrolls the theory screen content when on that screen.
     */
    public void mouseWheel(MouseEvent event) {
        if (appState == 1)                                        // Theory screen only
            bgScreen.scroll(event.getCount());                    // Scroll by wheel delta
    }
}