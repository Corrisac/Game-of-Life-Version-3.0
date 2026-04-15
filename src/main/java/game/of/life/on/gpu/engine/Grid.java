package game.of.life.on.gpu.engine;

import static game.of.life.on.gpu.engine.SimulationRules.*;

/**
 * Grid — Core cellular automaton data model.
 * Pure Java — no framework dependencies.
 *
 * Features:
 *   - Double-buffered cell state (zero-allocation swap)
 *   - Cell age tracking for heatmap visualization
 *   - Per-generation statistics (born, died, population)
 *   - Configurable rule sets (Conway, HighLife, Seeds, etc.)
 *   - Population history ring buffer for sparkline
 */
public class Grid {

    public byte[] boardFront;        // Current state (1=alive, 0=dead)
    public byte[] boardBack;         // Double-buffer for next generation
    private int[] ageFront;          // Current cell ages
    private int[] ageBack;           // Double-buffer ages
    private final int rows;
    private final int cols;

    // Per-generation statistics
    public int bornThisGen   = 0;
    public int diedThisGen   = 0;
    public int population    = 0;
    public int peakPop       = 0;
    public int generationNum = 0;

    // Population history ring buffer for sparkline
    public int[] popHistory = new int[200];
    public int popHistIdx   = 0;

    // Configurable rule set
    private int currentRuleSet = 0;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.boardFront = new byte[rows * cols];
        this.boardBack  = new byte[rows * cols];
        this.ageFront   = new int[rows * cols];
        this.ageBack    = new int[rows * cols];
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // ── Rule Set Management ───────────────────────────────────

    public void setRuleSet(int index) {
        currentRuleSet = Math.max(0, Math.min(index, RULE_COUNT - 1));
    }

    public int getRuleSet() { return currentRuleSet; }

    // ── Cell State Accessors ──────────────────────────────────

    public boolean getCellState(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols)
            return boardFront[row * cols + col] != 0;
        return false;
    }

    public void setCellState(int row, int col, boolean isAlive) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            int idx = row * cols + col;
            boardFront[idx] = (byte)(isAlive ? 1 : 0);
            if (isAlive && ageFront[idx] == 0)
                ageFront[idx] = 1;
        }
    }

    public int getAge(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols)
            return ageFront[row * cols + col];
        return 0;
    }

    // ── Next Generation ───────────────────────────────────────

    public void updateToNextGeneration() {
        int born = 0, died = 0, pop = 0;

        boolean[] birthRule = BIRTH_RULES[currentRuleSet];
        boolean[] survRule  = SURVIVE_RULES[currentRuleSet];

        // Fast inner loop (no bounds checking needed)
        for (int r = 1; r < rows - 1; r++) {
            int rowOffset = r * cols;
            for (int c = 1; c < cols - 1; c++) {
                int idx = rowOffset + c;
                int neighbors = boardFront[idx - cols - 1] + boardFront[idx - cols] + boardFront[idx - cols + 1]
                              + boardFront[idx - 1]                                 + boardFront[idx + 1]
                              + boardFront[idx + cols - 1] + boardFront[idx + cols] + boardFront[idx + cols + 1];

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

        // Edge cells (with bounds checking)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r > 0 && r < rows - 1 && c > 0 && c < cols - 1) continue;
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

        // Swap buffers (zero allocation)
        byte[] tempBoard = boardFront; boardFront = boardBack; boardBack = tempBoard;
        int[] tempAge = ageFront; ageFront = ageBack; ageBack = tempAge;

        bornThisGen = born;
        diedThisGen = died;
        population = pop;
        if (pop > peakPop) peakPop = pop;
        generationNum++;

        popHistory[popHistIdx % popHistory.length] = pop;
        popHistIdx++;
    }

    public int countLiveNeighbors(int row, int col) {
        int count = 0;
        for (int r = Math.max(0, row - 1); r <= Math.min(rows - 1, row + 1); r++) {
            int rowOffset = r * cols;
            for (int c = Math.max(0, col - 1); c <= Math.min(cols - 1, col + 1); c++) {
                if (!(r == row && c == col))
                    count += boardFront[rowOffset + c];
            }
        }
        return count;
    }

    public void clearBoard() {
        java.util.Arrays.fill(boardFront, (byte) 0);
        java.util.Arrays.fill(ageFront, 0);
        bornThisGen = 0;
        diedThisGen = 0;
        population = 0;
        peakPop = 0;
        generationNum = 0;
        popHistIdx = 0;
        java.util.Arrays.fill(popHistory, 0);
    }

    public int recount() {
        int n = 0;
        for (byte b : boardFront) if (b != 0) n++;
        population = n;
        return n;
    }
}
