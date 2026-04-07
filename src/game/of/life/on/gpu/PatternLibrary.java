import processing.core.PApplet;

public class PatternLibrary implements ThemeConstants {

    public static void spawnGlider(Grid board, int midR, int midC) {
        board.setCellState(midR, midC, true);
        board.setCellState(midR + 1, midC + 1, true);
        board.setCellState(midR + 2, midC - 1, true);
        board.setCellState(midR + 2, midC, true);
        board.setCellState(midR + 2, midC + 1, true);
    }

    public static void spawnGliderGun(Grid board) {
        board.clearBoard();
        int r = 5, c = 5;
        int[][] g = {
            {4,0},{5,0},{4,1},{5,1},{4,10},{5,10},{6,10},
            {3,11},{7,11},{2,12},{8,12},{2,13},{8,13},{5,14},
            {3,15},{7,15},{4,16},{5,16},{6,16},{5,17},
            {2,20},{3,20},{4,20},{2,21},{3,21},{4,21},
            {1,22},{5,22},{0,24},{1,24},{5,24},{6,24},
            {2,34},{3,34},{2,35},{3,35}
        };
        for (int[] co : g)
            board.setCellState(r + co[0], c + co[1], true);
    }

    public static void spawnRandomPattern(Grid board, PApplet p) {
        int margin = 10;
        int maxBound = Math.max(margin + 1, GRID_SIZE - margin);
        int r = (int) p.random(margin, maxBound);
        int c = (int) p.random(margin, maxBound);
        board.setCellState(r, c + 1, true);
        board.setCellState(r, c + 2, true);
        board.setCellState(r + 1, c, true);
        board.setCellState(r + 1, c + 1, true);
        board.setCellState(r + 2, c + 1, true);
    }

    public static void spawnPulsar(Grid board, int midR, int midC) {
        int[][] offsets = {
            {-6,-4},{-6,-3},{-6,-2},{-1,-4},{-1,-3},{-1,-2},
            {-4,-6},{-3,-6},{-2,-6},{-4,-1},{-3,-1},{-2,-1}
        };
        for (int[] o : offsets) {
            board.setCellState(midR + o[0], midC + o[1], true);
            board.setCellState(midR + o[0], midC - o[1], true);
            board.setCellState(midR - o[0], midC + o[1], true);
            board.setCellState(midR - o[0], midC - o[1], true);
        }
    }

    public static void spawnLWSS(Grid board, int r, int c) {
        board.setCellState(r, c + 1, true);
        board.setCellState(r, c + 4, true);
        board.setCellState(r + 1, c, true);
        board.setCellState(r + 2, c, true);
        board.setCellState(r + 2, c + 4, true);
        board.setCellState(r + 3, c, true);
        board.setCellState(r + 3, c + 1, true);
        board.setCellState(r + 3, c + 2, true);
        board.setCellState(r + 3, c + 3, true);
    }

    public static void spawnRPentomino(Grid board, int r, int c) {
        board.setCellState(r, c + 1, true);
        board.setCellState(r, c + 2, true);
        board.setCellState(r + 1, c, true);
        board.setCellState(r + 1, c + 1, true);
        board.setCellState(r + 2, c + 1, true);
    }

    public static void spawnAcorn(Grid board, int r, int c) {
        board.setCellState(r, c + 1, true);
        board.setCellState(r + 1, c + 3, true);
        board.setCellState(r + 2, c, true);
        board.setCellState(r + 2, c + 1, true);
        board.setCellState(r + 2, c + 4, true);
        board.setCellState(r + 2, c + 5, true);
        board.setCellState(r + 2, c + 6, true);
    }

    public static void spawnRandomSoup(Grid board, PApplet p) {
        int size = 20;
        int startR = GRID_SIZE / 2 - size / 2;
        int startC = GRID_SIZE / 2 - size / 2;
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                if (p.random(1) > 0.5f)
                    board.setCellState(startR + r, startC + c, true);
    }
}
