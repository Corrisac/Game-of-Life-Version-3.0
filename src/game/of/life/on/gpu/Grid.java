import processing.core.PApplet;

/**
 * Grid — V4.0 data model for the cellular automaton.
 *
 * V4.0 ADDITIONS:
 *   - Cell age tracking (how many consecutive generations each cell has been alive)
 *   - Per-generation statistics (born, died, peak population)
 *   - Configurable rule set (birth/survival neighbor counts)
 *   - Population history ring buffer for sparkline rendering
 */
public class Grid implements ThemeConstants {

    public byte[] boardFront;        // Current alive/dead state (1=alive, 0=dead)
    public byte[] boardBack;         // Double-buffer
    private int[] ageFront;          // Current age state
    private int[] ageBack;           // Double-buffer age
    private int rows;                // Number of rows in the grid
    private int cols;                // Number of columns in the grid

    // ═══════════════════════════════════════════════════════
    //        V4.0: PER-GENERATION STATISTICS
    // ═══════════════════════════════════════════════════════
    int bornThisGen   = 0;           // Cells that were born last step
    int diedThisGen   = 0;           // Cells that died last step
    int population    = 0;           // Current alive count
    int peakPop       = 0;           // All-time peak population
    int generationNum = 0;           // Total generations elapsed

    // Population history for sparkline (ring buffer)
    int[] popHistory = new int[200]; // Last 200 data points
    int popHistIdx   = 0;            // Current write index in ring buffer

    // ═══════════════════════════════════════════════════════
    //        V4.0: CONFIGURABLE RULE SET
    // ═══════════════════════════════════════════════════════
    private int currentRuleSet = 0;  // Index into ThemeConstants rule arrays

    /**
     * Constructor — Creates a rows×cols grid with all cells dead.
     */
    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.boardFront = new byte[rows * cols];
        this.boardBack = new byte[rows * cols];
        this.ageFront = new int[rows * cols];
        this.ageBack = new int[rows * cols];
    }

    // ═══════════════════════════════════════════════════════
    //        RULE SET MANAGEMENT
    // ═══════════════════════════════════════════════════════

    /** Sets the active rule set (index into BIRTH_RULES/SURVIVE_RULES). */
    public void setRuleSet(int index) {
        currentRuleSet = Math.max(0, Math.min(index, RULE_COUNT - 1));
    }

    /** Returns the current rule set index. */
    public int getRuleSet() { return currentRuleSet; }

    // ═══════════════════════════════════════════════════════
    //        CELL STATE ACCESSORS
    // ═══════════════════════════════════════════════════════

    /** Returns true if the cell at (row, col) is alive. */
    public boolean getCellState(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols)
            return boardFront[row * cols + col] != 0;
        return false;
    }

    /** Sets the alive/dead state of the cell at (row, col). */
    public void setCellState(int row, int col, boolean isAlive) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            int idx = row * cols + col;
            boardFront[idx] = (byte)(isAlive ? 1 : 0);
            if (isAlive && ageFront[idx] == 0)
                ageFront[idx] = 1;  // Initialize age for manually placed cells
        }
    }

    /** Returns consecutive generations alive for cell at (row, col). 0 = dead. */
    public int getAge(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols)
            return ageFront[row * cols + col];
        return 0;
    }

    // ═══════════════════════════════════════════════════════
    //        V4.0: NEXT GENERATION (with age + stats)
    // ═══════════════════════════════════════════════════════

    /**
     * Computes the next generation using the current rule set.
     * Updates cell ages and per-generation statistics.
     */
    public void updateToNextGeneration() {
        int born = 0, died = 0, pop = 0;

        boolean[] birthRule = BIRTH_RULES[currentRuleSet];
        boolean[] survRule  = SURVIVE_RULES[currentRuleSet];

        // Core inner loop: entirely branchless neighbor checking (skips edges for max speed)
        for (int r = 1; r < rows - 1; r++) {
            int rowOffset = r * cols;
            for (int c = 1; c < cols - 1; c++) {
                int idx = rowOffset + c;
                int neighbors = boardFront[idx - cols - 1] + boardFront[idx - cols] + boardFront[idx - cols + 1] +
                                boardFront[idx - 1]                                 + boardFront[idx + 1] +
                                boardFront[idx + cols - 1] + boardFront[idx + cols] + boardFront[idx + cols + 1];

                boolean alive = boardFront[idx] != 0;

                if (alive && survRule[neighbors]) {
                    boardBack[idx] = 1;
                    ageBack[idx] = ageFront[idx] + 1;
                    pop++;
                } else if (!alive && birthRule[neighbors]) {
                    boardBack[idx] = 1;
                    ageBack[idx] = 1;
                    born++;
                    pop++;
                } else {
                    if (alive) died++;
                    boardBack[idx] = 0;
                    ageBack[idx] = 0;
                }
            }
        }

        // Processing edges separately
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r > 0 && r < rows - 1 && c > 0 && c < cols - 1) continue; // Skip inner
                int idx = r * cols + c;
                int neighbors = countLiveNeighbors(r, c);
                boolean alive = boardFront[idx] != 0;

                if (alive && survRule[neighbors]) {
                    boardBack[idx] = 1;
                    ageBack[idx] = ageFront[idx] + 1;
                    pop++;
                } else if (!alive && birthRule[neighbors]) {
                    boardBack[idx] = 1;
                    ageBack[idx] = 1;
                    born++;
                    pop++;
                } else {
                    if (alive) died++;
                    boardBack[idx] = 0;
                    ageBack[idx] = 0;
                }
            }
        }

        // Swap buffers to avoid GC thrashing!
        byte[] tempBoard = boardFront; boardFront = boardBack; boardBack = tempBoard;
        int[] tempAge = ageFront; ageFront = ageBack; ageBack = tempAge;

        bornThisGen = born;
        diedThisGen = died;
        population = pop;
        if (pop > peakPop) peakPop = pop;
        generationNum++;

        // Record in ring buffer for sparkline
        popHistory[popHistIdx % popHistory.length] = pop;
        popHistIdx++;
    }

    /** Counts alive neighbors (8-connected, bounded). */
    public int countLiveNeighbors(int row, int col) {
        int count = 0;
        int rStart = Math.max(0, row - 1);
        int rEnd = Math.min(rows - 1, row + 1);
        int cStart = Math.max(0, col - 1);
        int cEnd = Math.min(cols - 1, col + 1);

        for (int r = rStart; r <= rEnd; r++) {
            int rowOffset = r * cols;
            for (int c = cStart; c <= cEnd; c++) {
                if (!(r == row && c == col)) {
                    count += boardFront[rowOffset + c];
                }
            }
        }
        return count;
    }

    /** Clears all cells and resets ages and statistics. */
    public void clearBoard() {
        for (int i = 0; i < boardFront.length; i++) {
            boardFront[i] = 0;
            ageFront[i] = 0;
        }
        bornThisGen = 0;
        diedThisGen = 0;
        population = 0;
        peakPop = 0;
        generationNum = 0;
        popHistIdx = 0;
        for (int i = 0; i < popHistory.length; i++) popHistory[i] = 0;
    }

    /** Recounts the current population (useful after manual edits). */
    public int recount() {
        int n = 0;
        for (int i = 0; i < boardFront.length; i++) {
            if (boardFront[i] != 0) n++;
        }
        population = n;
        return n;
    }

    /** Legacy draw method — kept for compatibility. */
    public void draw(PApplet window, int cellSize) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                window.fill(boardFront[r * cols + c] != 0 ? 0 : 255);
                window.stroke(150);
                window.rect(c * cellSize, r * cellSize, cellSize, cellSize);
            }
        }
    }
}