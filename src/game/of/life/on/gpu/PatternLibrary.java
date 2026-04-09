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

}
