import processing.core.PApplet;

/**
 * GridRenderer — Static utility methods for rendering the game grid,
 * dark gradient background, corner accent brackets, and cell counting.
 * Stateless: all methods are static since no instance data is needed.
 */
public class GridRenderer implements ThemeConstants {

    /** Renders a two-tone vertical gradient with faint CRT scan lines. */
    public static void drawBackground(PApplet p) {
        p.noStroke();                                              // No outlines on gradient strips
        int c1 = p.color(10, 15, 25);                             // Top gradient color (dark navy)
        int c2 = p.color(16, 22, 40);                             // Bottom gradient color (slightly lighter)
        for (int y = 0; y < p.height; y += 2) {                   // Iterate top-to-bottom in 2px steps
            p.fill(p.lerpColor(c1, c2, (float) y / p.height));    // Interpolate between top and bottom colors
            p.rect(0, y, p.width, 2);                             // Draw a 2px horizontal strip
        }
        for (int y = 0; y < p.height; y += 4) {                   // Overlay scan lines every 4px
            p.fill(0, 0, 0, 15);                                  // Very subtle semi-transparent black
            p.rect(0, y, p.width, 1);                             // Draw a 1px scan line
        }
    }

    /** Renders the 60x60 grid. Alive=cyan, dead=dark. Grid lines only when alpha>0.5. */
    public static void drawGrid(PApplet p, Grid board, int cs, int ox, int oy, float alpha) {
        for (int r = 0; r < 60; r++) {                            // Iterate over all 60 rows
            for (int c = 0; c < 60; c++) {                        // Iterate over all 60 columns
                if (board.getCellState(r, c)) {                    // Check if cell is alive
                    p.fill(0, 255, 224, alpha * 200);              // Alive: bright cyan, alpha-scaled
                } else {
                    p.fill(18, 24, 38, alpha * 180);               // Dead: dark blue-gray, alpha-scaled
                }
                p.noStroke();                                      // No cell border outlines
                p.rect(ox + c * cs, oy + r * cs, cs, cs);         // Draw cell rectangle at grid position
            }
        }
        if (alpha > 0.5f) {                                        // Only draw grid lines when prominently visible
            p.stroke(255, 255, 255, alpha * 12);                   // Faint white grid line color
            p.strokeWeight(1);                                     // 1px line thickness
            for (int i = 0; i <= 60; i++) {                        // Draw 61 lines for 60 cells
                p.line(ox + i * cs, oy, ox + i * cs, oy + 60 * cs);   // Vertical grid line
                p.line(ox, oy + i * cs, ox + 60 * cs, oy + i * cs);   // Horizontal grid line
            }
        }
    }

    /** Draws decorative L-shaped brackets in each window corner for a sci-fi HUD look. */
    public static void drawCornerAccents(PApplet p) {
        p.stroke(0, 255, 224, 40);                                // Subtle cyan glow color
        p.strokeWeight(1);                                         // 1px line thickness
        int s = 30;                                                // Length of each bracket arm in pixels
        p.line(10, 10, 10 + s, 10);                               // Top-left horizontal arm
        p.line(10, 10, 10, 10 + s);                                // Top-left vertical arm
        p.line(p.width - 10, 10, p.width - 10 - s, 10);           // Top-right horizontal arm
        p.line(p.width - 10, 10, p.width - 10, 10 + s);           // Top-right vertical arm
        p.line(10, p.height - 10, 10 + s, p.height - 10);         // Bottom-left horizontal arm
        p.line(10, p.height - 10, 10, p.height - 10 - s);         // Bottom-left vertical arm
        p.line(p.width-10, p.height-10, p.width-10-s, p.height-10);  // Bottom-right horizontal arm
        p.line(p.width-10, p.height-10, p.width-10, p.height-10-s);  // Bottom-right vertical arm
    }

    /** Counts all alive cells on the grid (0 to 3600). */
    public static int countAlive(Grid board) {
        int n = 0;                                                 // Initialize alive counter
        for (int r = 0; r < 60; r++)                               // Iterate over all rows
            for (int c = 0; c < 60; c++)                           // Iterate over all columns
                if (board.getCellState(r, c)) n++;                 // Increment if cell is alive
        return n;                                                  // Return total population
    }
}
