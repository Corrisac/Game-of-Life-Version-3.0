import processing.core.PApplet; // Processing core for drawing primitives and text
import java.util.ArrayList;      // Dynamic array for the simulation button list

/**
 * SimulationScreen — Renders the interactive simulation screen where users can
 * draw cells, play/pause the automaton, change brush size, and spawn patterns.
 * Owns its own button bar and handles simulation-specific input events.
 */
public class SimulationScreen implements ThemeConstants {

    private Main app;                            // Reference to main application for drawing and state
    private ArrayList<DashButton> simButtons;    // Bottom toolbar buttons (Play, Clear, Brush, etc.)

    /**
     * Constructor — Creates the simulation toolbar buttons.
     * @param app  Main application instance
     */
    public SimulationScreen(Main app) {
        this.app = app;                          // Store reference to main application
        simButtons = new ArrayList<>();          // Initialize button list
        // Define button labels and their accent colors
        String[] labels = {"▶  PLAY", "✕  CLEAR", "BRUSH 1x1", "GLIDER", "GUN", "← MENU"};
        int[] colors = {GREEN, RED, WHITE_DIM, CYAN, PURPLE, WHITE_DIM};
        for (int i = 0; i < labels.length; i++) // Create each button with its label and color
            simButtons.add(new DashButton(app, 0, 0, 120, 42, labels[i], colors[i]));
    }

    /** Renders the full simulation screen for one frame. */
    public void draw() {
        GridRenderer.drawBackground(app);                         // Dark gradient base layer

        // --- Calculate grid layout (centered, with margins) ---
        int gridAreaW = app.width - 50;                           // Grid area width with margins
        int gridAreaH = app.height - 90;                          // Grid area height (top/bottom bars)
        int cs = Math.max(1, Math.min(gridAreaW, gridAreaH) / 60); // Cell size to fit 60 cells
        int offsetX = 25 + (gridAreaW - cs * 60) / 2;            // Center grid horizontally
        int offsetY = 50 + (gridAreaH - cs * 60) / 2;            // Center grid vertically

        GridRenderer.drawGrid(app, app.gameBoard, cs, offsetX, offsetY, 1.0f); // Full opacity grid

        // Advance simulation every 5 frames when not paused
        if (!app.isPaused && app.frameCount % 5 == 0)
            app.gameBoard.updateToNextGeneration();                // Step the automaton forward

        // --- Top status bar ---
        app.noStroke();                                           // No outline on bar
        app.fill(10, 15, 25, 220);                                // Semi-transparent dark background
        app.rect(0, 0, app.width, 44);                            // Draw the bar rectangle
        app.stroke(0, 255, 224, 40);                              // Cyan accent line
        app.strokeWeight(1);                                      // 1px thickness
        app.line(0, 44, app.width, 44);                           // Bottom edge of top bar

        app.fill(255, 255, 255, 200);                             // Bright white for title
        app.textAlign(PApplet.LEFT, PApplet.CENTER);              // Left-align the title
        app.textSize(16);                                         // Title font size
        app.text("SIMULATION", 20, 22);                           // Section title

        // Status indicator (red=paused, green=running)
        app.fill(app.isPaused ? 0xFFFF4757 : 0xFF2ED573);        // Red if paused, green if running
        app.textAlign(PApplet.RIGHT, PApplet.CENTER);             // Right-align the indicator
        app.textSize(12);                                         // Indicator font size
        app.text(app.isPaused ? "● PAUSED" : "● RUNNING", app.width - 20, 22); // Status text

        // Center stats display (generation, population, FPS)
        app.fill(255, 255, 255, 80);                              // Dim white for stats
        app.textAlign(PApplet.CENTER, PApplet.CENTER);            // Center-align stats
        app.text("GEN: " + app.frameCount / 5 + "   |   POP: "
                + GridRenderer.countAlive(app.gameBoard) + "   |   "
                + (int) app.frameRate + " FPS", app.width / 2f, 22); // Stats string

        // --- Bottom button bar ---
        app.noStroke();                                           // No outline on bar
        app.fill(10, 15, 25, 220);                                // Semi-transparent dark background
        app.rect(0, app.height - 60, app.width, 60);              // Draw the bar rectangle
        app.stroke(255, 255, 255, 15);                            // Faint white separator
        app.strokeWeight(1);                                      // 1px thickness
        app.line(0, app.height - 60, app.width, app.height - 60); // Top edge of bottom bar

        // Update dynamic button labels based on current state
        simButtons.get(0).label = app.isPaused ? "▶  PLAY" : "⏸  PAUSE"; // Toggle play/pause label
        simButtons.get(0).baseColor = app.isPaused ? GREEN : ORANGE;      // Toggle button color
        simButtons.get(2).label = "BRUSH " + app.brushSize + "x" + app.brushSize; // Show brush size

        // Layout buttons evenly across the bottom bar
        float btnW = Math.min(140, (app.width - 100) / 6f);      // Responsive button width
        float total = btnW * simButtons.size() + 12 * (simButtons.size() - 1); // Total bar width
        float bx = (app.width - total) / 2f;                     // Center the button row
        for (int i = 0; i < simButtons.size(); i++) {             // Iterate over each button
            DashButton b = simButtons.get(i);                     // Get button at index i
            b.setPos(bx + i * (btnW + 12), app.height - 50, btnW, 38); // Position with gaps
            b.update(app.mouseX, app.mouseY);                      // Update hover animation
            b.display();                                           // Render the button
        }
    }

    /** Handles mouse clicks: toolbar buttons or cell drawing. */
    public void handleMousePressed(float mx, float my) {
        if (my > app.height - 60) {                                // Click is in the bottom bar
            if (simButtons.get(0).checkClick(mx, my))              // Play/Pause button
                app.isPaused = !app.isPaused;                      // Toggle pause state
            else if (simButtons.get(1).checkClick(mx, my))         // Clear button
                app.gameBoard.clearBoard();                        // Erase all cells
            else if (simButtons.get(2).checkClick(mx, my))         // Brush size toggle
                app.brushSize = (app.brushSize == 1) ? 3 : 1;     // Toggle between 1x1 and 3x3
            else if (simButtons.get(3).checkClick(mx, my))         // Glider button
                PatternLibrary.spawnGlider(app.gameBoard, 30, 30); // Spawn glider at center
            else if (simButtons.get(4).checkClick(mx, my))         // Gun button
                PatternLibrary.spawnGliderGun(app.gameBoard);      // Spawn Gosper's glider gun
            else if (simButtons.get(5).checkClick(mx, my))         // Menu button
                app.appState = 0;                                  // Return to dashboard menu
        } else if (my > 44) {                                      // Click is on the grid area
            drawWithBrush();                                       // Paint cells at mouse position
        }
    }

    /** Handles mouse dragging for continuous cell painting. */
    public void handleMouseDragged(float mx, float my) {
        if (my > 44 && my < app.height - 60)                      // Only paint within grid bounds
            drawWithBrush();                                       // Paint cells under the cursor
    }

    /** Handles keyboard shortcuts (space=play/pause, c=clear). */
    public void handleKeyPressed() {
        if (app.key == ' ') app.isPaused = !app.isPaused;         // Space toggles play/pause
        if (app.key == 'c' || app.key == 'C')                     // C clears the board
            app.gameBoard.clearBoard();                            // Erase all cells
    }

    /** Paints cells at the mouse position using the current brush size. */
    private void drawWithBrush() {
        int gw = app.width - 50;                                  // Grid area width
        int gh = app.height - 90;                                 // Grid area height
        int cs = Math.max(1, Math.min(gw, gh) / 60);              // Cell size in pixels
        int offsetX = 25 + (gw - cs * 60) / 2;                    // Grid X offset
        int offsetY = 50 + (gh - cs * 60) / 2;                    // Grid Y offset
        int baseC = (app.mouseX - offsetX) / cs;                  // Column under the mouse
        int baseR = (app.mouseY - offsetY) / cs;                  // Row under the mouse
        for (int i = 0; i < app.brushSize; i++)                    // Iterate brush rows
            for (int j = 0; j < app.brushSize; j++)                // Iterate brush columns
                if (baseR+i >= 0 && baseR+i < 60 && baseC+j >= 0 && baseC+j < 60) // Bounds check
                    app.gameBoard.setCellState(baseR + i, baseC + j, true); // Set cell alive
    }
}
