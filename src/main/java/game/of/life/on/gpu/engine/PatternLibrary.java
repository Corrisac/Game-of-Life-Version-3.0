package game.of.life.on.gpu.engine;

import java.util.Random;

import static game.of.life.on.gpu.engine.SimulationRules.GRID_SIZE;

/**
 * PatternLibrary — Spawns Game of Life patterns on the grid.
 * Pure Java — no framework dependencies.
 */
public final class PatternLibrary {

    private static final Random rng = new Random();

    /** Spawns a glider centered at (midR, midC). */
    public static void spawnGlider(Grid board, int midR, int midC) {
        int[][] cells = { {0,1}, {1,2}, {2,0}, {2,1}, {2,2} };
        for (int[] c : cells)
            board.setCellState(midR + c[0] - 1, midC + c[1] - 1, true);
    }

    /** Spawns a Gosper glider gun at the top-left region. */
    public static void spawnGliderGun(Grid board) {
        int r = 5, c = 2;
        int[][] cells = {
            {4,0},{4,1},{5,0},{5,1},
            {4,10},{5,10},{6,10},{3,11},{7,11},{2,12},{8,12},{2,13},{8,13},
            {5,14},{3,15},{7,15},{4,16},{5,16},{6,16},{5,17},
            {2,20},{3,20},{4,20},{2,21},{3,21},{4,21},{1,22},{5,22},
            {0,24},{1,24},{5,24},{6,24},
            {2,34},{3,34},{2,35},{3,35}
        };
        for (int[] cell : cells)
            board.setCellState(r + cell[0], c + cell[1], true);
    }

    /** Spawns a random 30% density pattern across the grid. */
    public static void spawnRandomPattern(Grid board) {
        int gs = GRID_SIZE;
        int count = (gs * gs) / 3;
        for (int i = 0; i < count; i++) {
            board.setCellState(rng.nextInt(gs), rng.nextInt(gs), true);
        }
    }

    private PatternLibrary() {}
}
