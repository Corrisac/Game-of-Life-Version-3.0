import processing.core.PApplet; // Processing core for random number generation

/**
 * PatternLibrary — Static factory methods for spawning well-known Game of Life patterns.
 * Encapsulates all pattern coordinate data in one place, keeping pattern logic
 * out of UI code and promoting reuse across screens.
 */
public class PatternLibrary {

    /**
     * Spawns a standard glider (5-cell diagonal spaceship) at the given position.
     * @param board  The Grid to place the pattern on
     * @param midR   Center row for placement
     * @param midC   Center column for placement
     */
    public static void spawnGlider(Grid board, int midR, int midC) {
        board.setCellState(midR, midC, true);           // Top-center cell of the glider
        board.setCellState(midR + 1, midC + 1, true);   // Middle-right cell
        board.setCellState(midR + 2, midC - 1, true);   // Bottom-left cell
        board.setCellState(midR + 2, midC, true);        // Bottom-center cell
        board.setCellState(midR + 2, midC + 1, true);   // Bottom-right cell
    }

    /**
     * Clears the board and places Gosper's Glider Gun — a period-30 oscillator
     * that emits a new glider every 30 generations. First known infinite-growth pattern.
     * @param board  The Grid to place the gun pattern on
     */
    public static void spawnGliderGun(Grid board) {
        board.clearBoard();                              // Wipe all cells before placing the gun
        int r = 5;                                       // Anchor row for the gun's top-left
        int c = 5;                                       // Anchor column for the gun's top-left
        // All 36 cell coordinates that form Gosper's Glider Gun
        int[][] g = {
            {4,0},{5,0},{4,1},{5,1},                     // Left square block ("magazine")
            {4,10},{5,10},{6,10},                         // Left wing column
            {3,11},{7,11},                                // Left wing outer edges
            {2,12},{8,12},{2,13},{8,13},                  // Far outer edges
            {5,14},                                       // Center connector
            {3,15},{7,15},                                // Right wing inner edges
            {4,16},{5,16},{6,16},                         // Right wing column
            {5,17},                                       // Right wing tip
            {2,20},{3,20},{4,20},                         // Right structure left column
            {2,21},{3,21},{4,21},                         // Right structure right column
            {1,22},{5,22},                                // Right structure outer edges
            {0,24},{1,24},{5,24},{6,24},                  // Far outer cells
            {2,34},{3,34},{2,35},{3,35}                   // Right square block ("muzzle")
        };
        for (int[] co : g) {                             // Iterate over each gun cell coordinate
            board.setCellState(r + co[0], c + co[1], true); // Place the cell at the offset position
        }
    }

    /**
     * Spawns a small random R-pentomino-like cluster for the menu background animation.
     * @param board  The Grid to place the cluster on
     * @param p      Processing applet (for random number generation)
     */
    public static void spawnRandomPattern(Grid board, PApplet p) {
        int r = (int) p.random(10, 40);                  // Random row in the safe interior
        int c = (int) p.random(10, 40);                  // Random column in the safe interior
        board.setCellState(r, c + 1, true);              // Cluster cell 1 (top-center)
        board.setCellState(r, c + 2, true);              // Cluster cell 2 (top-right)
        board.setCellState(r + 1, c, true);              // Cluster cell 3 (middle-left)
        board.setCellState(r + 1, c + 1, true);          // Cluster cell 4 (middle-center)
        board.setCellState(r + 2, c + 1, true);          // Cluster cell 5 (bottom-center)
    }
}
