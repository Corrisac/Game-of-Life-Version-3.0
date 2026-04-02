import processing.core.PApplet; // Processing core for drawing and text
import java.util.ArrayList;      // Dynamic array for GPU lab button list

/**
 * GPULabScreen — Renders the GPU compute laboratory screen with a split-pane layout:
 *   Left sidebar: controls (dial, iteration input, action buttons)
 *   Center: editable CPU grid
 *   Right: GPU computation result / progress bar
 * Owns its UI components and handles all GPU-lab-specific input events.
 */
public class GPULabScreen implements ThemeConstants {

    private Main app;                            // Reference to main app for drawing and shared state
    private CircularDial gpuDial;                // Circular dial to set iteration count
    private ArrayList<DashButton> gpuButtons;    // Action buttons: Compute, Clear, Glider, Gun, Menu
    boolean isTypingIterations = false;          // True when the user is typing into the input box
    String iterationInput = "";                  // Buffer for typed iteration count digits

    /**
     * Constructor — Creates GPU lab UI components.
     * @param app  Main application instance
     */
    public GPULabScreen(Main app) {
        this.app = app;                          // Store reference to main app
        // Create the iteration-count dial (max 1,000,000 iterations)
        gpuDial = new CircularDial(app, 0, 0, 80, 1000000, "ITERATIONS");
        gpuDial.setValue(1000);                   // Default to 1,000 iterations
        gpuButtons = new ArrayList<>();          // Initialize button list
        // Define GPU lab button labels and accent colors
        String[] labels = {"⚡ COMPUTE", "✕  CLEAR", "GLIDER", "GUN", "← MENU"};
        int[] colors = {ORANGE, RED, CYAN, PURPLE, WHITE_DIM};
        for (int i = 0; i < labels.length; i++) // Create each button
            gpuButtons.add(new DashButton(app, 0, 0, 120, 42, labels[i], colors[i]));
    }

    /** Returns the current dial value (number of iterations to compute). */
    public int getIterationCount() {
        return gpuDial.getValue();               // Read the dial's current integer value
    }

    /** Renders the complete GPU lab screen for one frame. */
    public void draw() {
        GridRenderer.drawBackground(app);                         // Dark gradient base layer

        // --- Layout constants ---
        int sideW = 240;                                          // Left sidebar width in pixels
        int halfW = (app.width - sideW) / 2;                     // Width of each half (grid/result)
        int cs = Math.max(1, Math.min(halfW - 20, app.height - 60) / 60); // Cell size
        int offsetX = sideW + (halfW - cs * 60) / 2;             // Grid X offset (centered in left half)
        int offsetY = 50 + (app.height - 60 - cs * 60) / 2;     // Grid Y offset (centered vertically)

        // --- Left half: editable CPU grid ---
        GridRenderer.drawGrid(app, app.gameBoard, cs, offsetX, offsetY, 1.0f);

        // --- Right half: GPU result / progress / placeholder ---
        int rightX = sideW + halfW;                               // X position of the right pane
        GPUCompute gpu = app.getGpuCompute();                      // Shorthand reference to GPU module
        if (gpu.isComputing) {                                    // Currently computing
            app.noStroke(); app.fill(10, 15, 25);                 // Dark background for progress area
            app.rect(rightX, 0, halfW, app.height);              // Fill the right pane
            float pct = (float) gpu.computeProgress / gpuDial.getValue(); // Progress percentage
            app.fill(255, 255, 255, 200);                         // White text for "COMPUTING" label
            app.textAlign(PApplet.CENTER, PApplet.CENTER);        // Center-align
            app.textSize(20);                                     // Large label font
            app.text("COMPUTING", rightX + halfW / 2f, app.height / 2f - 45); // Label
            // Background track for the progress bar
            app.fill(30, 38, 50);                                 // Dark track color
            app.rect(rightX + 40, app.height/2f - 8, halfW - 80, 16, 8); // Rounded track
            // Multi-layer glow effect behind the progress bar
            for (int i = 2; i >= 0; i--) {                        // 3 glow layers
                app.fill(0, 255, 224, 20 - i * 6);               // Progressively fainter cyan
                app.rect(rightX + 40, app.height/2f - 8 - i*2,
                         (halfW - 80) * pct, 16 + i*4, 8);        // Wider glow layers
            }
            // Solid progress bar on top
            app.fill(0, 255, 224);                                // Solid cyan fill
            app.rect(rightX + 40, app.height/2f - 8, (halfW - 80) * pct, 16, 8);
            // Progress text (e.g., "5,000 / 10,000  (50%)")
            app.fill(0, 255, 224, 180);                           // Slightly transparent cyan
            app.textSize(14);                                     // Medium font
            app.text(String.format("%,d / %,d  (%.0f%%)",
                     gpu.computeProgress, gpuDial.getValue(), pct * 100),
                     rightX + halfW / 2f, app.height / 2f + 30); // Centered below bar
        } else if (gpu.hasComputed) {                              // Result available
            app.image(gpu.getResultBuffer(), rightX, 44, halfW, app.height - 44); // Display result
            app.noStroke(); app.fill(0, 0, 0, 150);              // Semi-dark overlay for label
            app.rect(rightX, 44, halfW, 36);                     // Label background
            app.fill(0, 255, 224, 220);                           // Cyan result text
            app.textAlign(PApplet.CENTER, PApplet.CENTER);        // Center-align
            app.textSize(13);                                     // Result label font
            app.text("✓  " + gpu.computeTime + "ms  ·  "
                     + String.format("%,d", gpuDial.getValue()) + " GEN",
                     rightX + halfW / 2f, 62);                    // Show time and generations
        } else {                                                   // No result yet (placeholder)
            app.noStroke(); app.fill(10, 15, 25);                 // Dark placeholder background
            app.rect(rightX, 44, halfW, app.height - 44);        // Fill the right pane
            app.fill(255, 255, 255, 60);                          // Dim white placeholder title
            app.textAlign(PApplet.CENTER, PApplet.CENTER);        // Center-align
            app.textSize(18);                                     // Placeholder title font
            app.text("GPU RESULT", rightX + halfW/2f, app.height/2f - 15);
            app.fill(255, 255, 255, 30);                          // Dimmer subtitle
            app.textSize(12);                                     // Subtitle font
            app.text("Output renders here after compute",
                     rightX + halfW/2f, app.height/2f + 12);      // Placeholder instructions
        }

        // --- Vertical divider glow between grid and result ---
        for (int i = 2; i >= 0; i--) {                            // 3 glow layers
            app.stroke(0, 255, 224, 15 - i * 4);                 // Progressively fainter cyan
            app.strokeWeight(1 + i * 2);                          // Wider glow layers
            app.line(rightX, 44, rightX, app.height);             // Vertical divider line
        }

        // --- Left sidebar panel ---
        app.noStroke(); app.fill(10, 15, 25, 235);               // Dark sidebar background
        app.rect(0, 0, sideW, app.height);                       // Fill the sidebar
        app.stroke(255, 255, 255, 18); app.strokeWeight(1);      // Faint right-edge border
        app.line(sideW, 0, sideW, app.height);                   // Sidebar right border

        // Sidebar title
        app.noStroke();                                           // No stroke for title
        app.fill(255, 255, 255, 220);                             // Bright white title
        app.textAlign(PApplet.CENTER, PApplet.TOP);               // Center-top alignment
        app.textSize(18);                                         // Title font size
        app.text("GPU LAB", sideW / 2f, 16);                     // "GPU LAB" heading
        app.stroke(0, 255, 224, 50); app.strokeWeight(1);        // Cyan underline
        app.line(20, 44, sideW - 20, 44);                         // Underline below heading

        // --- Dial widget ---
        float dialR = Math.min(65, (sideW - 40) / 3f);           // Responsive dial radius
        gpuDial.setPos(sideW / 2f, 120, dialR);                  // Position dial in sidebar
        gpuDial.update();                                         // Animate dial smoothly
        gpuDial.display();                                        // Render the dial

        // --- Iteration input field ---
        float inputY = 120 + dialR + 18;                          // Y position below the dial
        float inputW = sideW - 60;                                // Input box width
        float inputH = 32;                                        // Input box height
        float inputX = 30;                                        // Input box X position

        // Input label text
        app.fill(255, 255, 255, 80);                              // Dim white label
        app.textAlign(PApplet.CENTER, PApplet.BOTTOM);            // Center-bottom alignment
        app.textSize(10);                                         // Small label font
        app.text("CLICK TO TYPE ITERATIONS", sideW / 2f, inputY - 3); // Label above box

        // Input box styling (active vs inactive)
        if (isTypingIterations) {                                  // Currently typing
            app.stroke(0, 255, 224, 180);                         // Bright cyan border
            app.strokeWeight(1.5f);                                // Slightly thicker border
            app.fill(0, 255, 224, 12);                            // Faint cyan fill
        } else {                                                   // Inactive
            app.stroke(255, 255, 255, 30);                        // Faint white border
            app.strokeWeight(1);                                   // Normal border thickness
            app.fill(255, 255, 255, 8);                           // Very faint white fill
        }
        app.rect(inputX, inputY, inputW, inputH, 8);             // Draw the rounded input box
        app.noStroke();                                           // Clear stroke

        // Input text content
        app.fill(255, 255, 255, 220);                             // Bright white text
        app.textAlign(PApplet.CENTER, PApplet.CENTER);            // Center in the box
        app.textSize(15);                                         // Input text font size
        if (isTypingIterations) {                                  // Show typed text with cursor
            String cursor = (app.frameCount % 60 < 30) ? "|" : ""; // Blinking cursor
            app.text(iterationInput + cursor, sideW / 2f, inputY + inputH / 2f);
        } else {                                                   // Show formatted dial value
            app.text(String.format("%,d", gpuDial.getValue()), sideW / 2f, inputY + inputH / 2f);
        }

        // --- Sidebar action buttons ---
        float by = inputY + inputH + 16;                          // Y position below input
        float btnW2 = sideW - 40;                                // Button width in sidebar
        for (int i = 0; i < gpuButtons.size(); i++) {             // Iterate over each button
            DashButton b = gpuButtons.get(i);                     // Get button at index i
            b.setPos(20, by + i * 48, btnW2, 38);                // Position vertically stacked
            b.update(app.mouseX, app.mouseY);                     // Update hover animation
            b.display();                                          // Render the button
        }

        // --- Top bar across the grid/result area ---
        app.noStroke(); app.fill(10, 15, 25, 220);               // Dark top bar background
        app.rect(sideW, 0, app.width - sideW, 44);               // Draw the top bar
        app.stroke(0, 255, 224, 30); app.strokeWeight(1);        // Cyan accent line
        app.line(sideW, 44, app.width, 44);                       // Bottom edge of top bar
        app.fill(255, 255, 255, 120);                             // Dim white labels
        app.textAlign(PApplet.LEFT, PApplet.CENTER);              // Left-align
        app.textSize(12);                                         // Label font
        app.text("CPU GRID (EDITABLE)", sideW + 14, 22);         // Left label
        app.textAlign(PApplet.RIGHT, PApplet.CENTER);             // Right-align
        app.text("GPU OUTPUT", app.width - 14, 22);               // Right label
    }

    /** Handles mouse clicks for the GPU lab screen. */
    public void handleMousePressed(float mx, float my) {
        int sideW = 240;                                          // Sidebar width
        float dialR2 = Math.min(65, (sideW - 40) / 3f);          // Dial radius for layout calc
        float inputY2 = 120 + dialR2 + 18;                       // Input box Y position
        float inputH2 = 32;                                       // Input box height

        // Close typing mode if clicked outside the input box
        if (isTypingIterations) {                                  // Currently typing
            boolean inBox = mx > 30 && mx < sideW - 30            // Check X bounds
                         && my > inputY2 && my < inputY2 + inputH2; // Check Y bounds
            if (!inBox) {                                          // Clicked outside
                isTypingIterations = false;                        // Exit typing mode
                if (iterationInput.length() > 0) {                 // If text was entered
                    int val = Math.min(Integer.parseInt(iterationInput), 1000000); // Clamp max
                    gpuDial.setValue(val);                          // Apply typed value to dial
                }
            }
        }

        if (mx < sideW) {                                         // Click is in the sidebar
            // Check if clicked the iteration input field
            if (my > inputY2 && my < inputY2 + inputH2 && mx > 30 && mx < sideW - 30) {
                isTypingIterations = true;                         // Enter typing mode
                iterationInput = "";                               // Clear the input buffer
            }
            else if (gpuDial.checkPress(mx, my)) { /* dial drag */ } // Start dial drag
            else if (gpuButtons.get(0).checkClick(mx, my) && !app.getGpuCompute().isComputing)
                app.getGpuCompute().startCompute(app.gameBoard, gpuDial.getValue()); // Start GPU compute
            else if (gpuButtons.get(1).checkClick(mx, my)) {      // Clear button
                app.gameBoard.clearBoard();                        // Erase all cells
                app.getGpuCompute().hasComputed = false;            // Reset result
            }
            else if (gpuButtons.get(2).checkClick(mx, my)) {      // Glider button
                PatternLibrary.spawnGlider(app.gameBoard, 30, 30); // Spawn glider at center
                app.getGpuCompute().hasComputed = false;            // Invalidate old result
            }
            else if (gpuButtons.get(3).checkClick(mx, my))        // Gun button
                PatternLibrary.spawnGliderGun(app.gameBoard);      // Spawn glider gun
            else if (gpuButtons.get(4).checkClick(mx, my))        // Menu button
                app.appState = 0;                                  // Return to dashboard
        }
        else if (mx < sideW + (app.width - sideW) / 2 && my > 44) { // Click on grid area
            drawWithBrush();                                       // Paint cells
        }
    }

    /** Handles mouse dragging for dial and cell painting. */
    public void handleMouseDragged(float mx, float my) {
        if (gpuDial.isDragging) {                                  // Dial is being dragged
            gpuDial.drag(mx, my);                                  // Update dial value from mouse
            return;                                                // Skip cell painting
        }
        int sideW = 240;                                          // Sidebar width
        if (mx > sideW && mx < sideW + (app.width - sideW) / 2 && my > 44) { // On grid area
            drawWithBrush();                                       // Paint cells under cursor
            app.getGpuCompute().hasComputed = false;                // Invalidate old result
        }
    }

    /** Releases the dial drag handle. */
    public void handleMouseReleased() {
        gpuDial.release();                                         // End dial drag interaction
    }

    /** Handles keyboard input for the iteration text field. */
    public void handleKeyPressed() {
        if (!isTypingIterations) return;                            // Only process when typing
        if (app.key >= '0' && app.key <= '9' && iterationInput.length() < 7) // Digit, max 7 chars
            iterationInput += app.key;                             // Append digit to buffer
        else if (app.key == PApplet.BACKSPACE && iterationInput.length() > 0) // Backspace
            iterationInput = iterationInput.substring(0, iterationInput.length() - 1); // Remove last
        else if (app.key == PApplet.ENTER || app.key == PApplet.RETURN) { // Enter confirms
            isTypingIterations = false;                            // Exit typing mode
            if (iterationInput.length() > 0) {                     // If text was entered
                int val = Math.min(Integer.parseInt(iterationInput), 1000000); // Clamp max
                gpuDial.setValue(val);                              // Apply value to dial
            }
        }
    }

    /** Paints cells at the mouse position using the current brush size. */
    private void drawWithBrush() {
        int sideW = 240;                                          // Sidebar width
        int halfW = (app.width - sideW) / 2;                     // Half-width for grid pane
        int cs = Math.max(1, Math.min(halfW - 20, app.height - 60) / 60); // Cell size
        int offsetX = sideW + (halfW - cs * 60) / 2;             // Grid X offset
        int offsetY = 50 + (app.height - 60 - cs * 60) / 2;     // Grid Y offset
        int baseC = (app.mouseX - offsetX) / cs;                  // Column under the mouse
        int baseR = (app.mouseY - offsetY) / cs;                  // Row under the mouse
        for (int i = 0; i < app.brushSize; i++)                    // Iterate brush rows
            for (int j = 0; j < app.brushSize; j++)                // Iterate brush columns
                if (baseR+i >= 0 && baseR+i < 60 && baseC+j >= 0 && baseC+j < 60) // Bounds check
                    app.gameBoard.setCellState(baseR + i, baseC + j, true); // Set cell alive
    }
}
