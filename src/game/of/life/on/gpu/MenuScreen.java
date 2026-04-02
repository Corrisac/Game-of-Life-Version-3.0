import processing.core.PApplet; // Processing core for drawing and text rendering
import java.util.ArrayList;      // Dynamic array for the navigation button list

/**
 * MenuScreen — Renders the main dashboard/menu screen with animated background grid,
 * floating particles, centered title, navigation buttons, and decorative accents.
 * Also handles mouse click events specific to the menu screen.
 */
public class MenuScreen implements ThemeConstants {

    private Main app;                            // Reference to the main PApplet for drawing and state
    private CircularDial menuDial;               // Decorative "ENERGY" dial on the menu (cosmetic)
    private ArrayList<DashButton> menuButtons;   // Navigation buttons: Sim, Theory, GPU Lab

    /**
     * Constructor — Creates menu UI components (dial and 3 navigation buttons).
     * @param app  The Main application instance (provides PApplet drawing + shared state)
     */
    public MenuScreen(Main app) {
        this.app = app;                          // Store reference to main application
        // Create the decorative circular dial widget
        menuDial = new CircularDial(app, app.width / 2f, app.height * 0.42f, 120, 100, "ENERGY");
        menuDial.setValue(73);                   // Set cosmetic initial value to 73%
        menuButtons = new ArrayList<>();         // Initialize the button list
        // Create three navigation buttons with distinct accent colors
        menuButtons.add(new DashButton(app, 0, 0, 200, 52, "ENTER SIMULATION", CYAN));
        menuButtons.add(new DashButton(app, 0, 0, 200, 52, "BACKGROUND & THEORY", PURPLE));
        menuButtons.add(new DashButton(app, 0, 0, 200, 52, "GPU COMPUTE LAB", MAGENTA));
    }

    /**
     * draw — Renders the complete menu screen for one frame.
     * @param particles  The particle system for ambient floating particles
     */
    public void draw(ParticleSystem particles) {
        GridRenderer.drawBackground(app);                          // Dark gradient base layer

        // --- Animated background grid (auto-plays for visual effect) ---
        if (app.frameCount % 120 == 0)                             // Every ~2 seconds
            PatternLibrary.spawnRandomPattern(app.gameBoard, app);  // Seed a new random cluster
        if (app.frameCount % 10 == 0)                              // Every 10 frames
            app.gameBoard.updateToNextGeneration();                // Advance simulation one step
        int cs = Math.max(1, Math.max(app.width, app.height) / 60); // Cell size fills entire window
        GridRenderer.drawGrid(app, app.gameBoard, cs, 0, 0, 0.15f); // Draw grid at 15% opacity

        // --- Dark overlay for text readability ---
        app.noStroke();                                            // No outline on overlay
        app.fill(10, 15, 25, 170);                                 // Semi-transparent dark fill
        app.rect(0, 0, app.width, app.height);                     // Cover the entire window

        particles.updateAndDraw();                                 // Draw ambient floating particles

        // --- Title with glow shadow ---
        app.textAlign(PApplet.CENTER, PApplet.CENTER);             // Center-align text
        app.fill(0, 255, 224, 15);                                 // Cyan glow shadow color
        app.textSize(Math.min(72, app.width * 0.055f));            // Responsive title size
        app.text("GAME OF LIFE", app.width / 2f + 2, app.height * 0.32f + 2); // Shadow offset
        app.fill(255, 255, 255, 240);                              // Bright white main title
        app.textSize(Math.min(70, app.width * 0.054f));            // Slightly smaller than shadow
        app.text("GAME OF LIFE", app.width / 2f, app.height * 0.32f); // Main title text

        // --- Accent line under title ---
        app.stroke(0, 255, 224, 60);                               // Subtle cyan line color
        app.strokeWeight(1);                                       // 1px thickness
        float lineW = Math.min(360, app.width * 0.28f);           // Responsive accent line width
        app.line(app.width/2f - lineW/2, app.height * 0.32f + 40,
                 app.width/2f + lineW/2, app.height * 0.32f + 40); // Centered horizontal line
        app.noStroke();                                            // Clear stroke for next elements

        // --- Subtitle ---
        app.fill(0, 255, 224, 140);                                // Dimmer cyan for subtitle
        app.textSize(Math.min(14, app.width * 0.012f));            // Small responsive font
        app.text("GPU-ACCELERATED  ·  CONWAY'S CELLULAR AUTOMATON  ·  OPENGL",
                 app.width / 2f, app.height * 0.32f + 60);        // Tech-stack subtitle

        // --- Navigation buttons (vertically stacked, centered) ---
        float btnW = Math.min(320, app.width * 0.28f);            // Responsive button width
        float btnH = 52;                                           // Fixed button height
        float gap = 18;                                            // Vertical gap between buttons
        float btnX = (app.width - btnW) / 2f;                     // Centered horizontally
        float startY = app.height * 0.52f;                         // Starting Y below center
        for (int i = 0; i < menuButtons.size(); i++) {             // Iterate over all buttons
            DashButton b = menuButtons.get(i);                     // Get button at index i
            b.setPos(btnX, startY + i * (btnH + gap), btnW, btnH); // Position responsively
            b.update(app.mouseX, app.mouseY);                      // Update hover state
            b.display();                                           // Render the button
        }

        // --- Bottom status bar ---
        app.fill(255, 255, 255, 35);                               // Very dim white text
        app.textSize(10);                                          // Small status font
        app.text("Processing 4  ·  JOGL 2.6  ·  GLSL  ·  " + (int) app.frameRate + " FPS",
                 app.width / 2f, app.height - 18);                 // Status bar at bottom

        GridRenderer.drawCornerAccents(app);                       // Draw decorative corner brackets
    }

    /**
     * handleMousePressed — Checks navigation button clicks and transitions app state.
     * @return true if a button was clicked
     */
    public boolean handleMousePressed(float mx, float my) {
        for (int i = 0; i < menuButtons.size(); i++) {             // Check each button
            if (menuButtons.get(i).checkClick(mx, my)) {           // Test for click hit
                if (i == 0) {                                      // "ENTER SIMULATION"
                    app.gameBoard.clearBoard();                    // Clear for fresh start
                    app.isPaused = true;                           // Start paused
                    app.appState = 2;                              // Switch to simulation
                } else if (i == 1) {                               // "BACKGROUND & THEORY"
                    app.appState = 1;                              // Switch to theory screen
                } else if (i == 2) {                               // "GPU COMPUTE LAB"
                    app.gameBoard.clearBoard();                    // Clear for fresh start
                    app.appState = 3;                              // Switch to GPU lab
                    app.getGpuCompute().hasComputed = false;    // Reset previous result
                }
                return true;                                       // Button was clicked
            }
        }
        return false;                                              // No button clicked
    }
}
