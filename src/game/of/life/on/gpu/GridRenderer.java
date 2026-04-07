import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * GridRenderer V4.0 — Fast pixel-buffer rendering replacing per-cell rect() calls.
 * At 256x256 (65K cells), pixel buffer is 50x faster than drawing individual rectangles.
 */
public class GridRenderer implements ThemeConstants {

    private static PGraphics gridBuf;

    // V5.0: Background gradient cache (avoids per-frame gradient loops)
    private static PGraphics bgCache;
    private static int bgCacheW = -1, bgCacheH = -1;

    /**
     * V5.0: Cached background gradient with CRT scan lines.
     * Rebuilds only when window dimensions change.
     */
    public static void drawBackground(PApplet p) {
        if (bgCache == null || bgCacheW != p.width || bgCacheH != p.height) {
            rebuildBgCache(p);
        }
        p.image(bgCache, 0, 0);
    }

    /** V5.0: Pre-renders gradient + CRT lines into off-screen buffer. */
    private static void rebuildBgCache(PApplet p) {
        bgCacheW = p.width;
        bgCacheH = p.height;
        bgCache = p.createGraphics(bgCacheW, bgCacheH);
        bgCache.beginDraw();
        bgCache.noStroke();
        int c1 = bgCache.color(10, 15, 25);
        int c2 = bgCache.color(16, 22, 40);
        for (int y = 0; y < bgCacheH; y += 2) {
            bgCache.fill(bgCache.lerpColor(c1, c2, (float) y / bgCacheH));
            bgCache.rect(0, y, bgCacheW, 2);
        }
        for (int y = 0; y < bgCacheH; y += 4) {
            bgCache.fill(0, 0, 0, 15);
            bgCache.rect(0, y, bgCacheW, 1);
        }
        bgCache.endDraw();
    }

    /**
     * V4.0: Fast grid rendering using pixel buffer.
     * Renders the entire grid to a PGraphics, then displays it scaled.
     */
    public static void drawGrid(PApplet p, Grid board, int cs, int ox, int oy, float alpha) {
        drawGridAdvanced(p, board, ox, oy, cs * GRID_SIZE, cs * GRID_SIZE, alpha, false);
    }

    /**
     * V4.0: Advanced grid rendering with heatmap support.
     * @param heatmap  true to color cells by age, false for classic cyan
     */
    public static void drawGridAdvanced(PApplet p, Grid board,
            int dx, int dy, int dw, int dh, float alpha, boolean heatmap) {
        int gs = GRID_SIZE;
        float cellSize = (float) dw / gs;

        if (cellSize >= 2.5f) {
            p.noStroke();
            int startC = Math.max(0, (int) (-dx / cellSize));
            int endC = Math.min(gs, (int) ((p.width - dx) / cellSize) + 1);
            int startR = Math.max(0, (int) (-dy / cellSize));
            int endR = Math.min(gs, (int) ((p.height - dy) / cellSize) + 1);

            for (int r = startR; r < endR; r++) {
                for (int c = startC; c < endC; c++) {
                    int col = board.getCellState(r, c) 
                                ? (heatmap ? ageToColor(board.getAge(r, c)) : 0xFF00FFE0) 
                                : 0xFF121826;
                    if (alpha < 0.99f) {
                        int baseA = (col >> 24) & 0xFF;
                        col = (col & 0x00FFFFFF) | ((int)(baseA * alpha) << 24);
                    }
                    p.fill(col);
                    p.rect(dx + c * cellSize, dy + r * cellSize, cellSize, cellSize);
                }
            }
            return;
        }

        if (gridBuf == null || gridBuf.width != gs) {
            gridBuf = p.createGraphics(gs, gs);
        }
        gridBuf.beginDraw();
        gridBuf.loadPixels();
        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                if (board.getCellState(r, c)) {
                    gridBuf.pixels[r * gs + c] = heatmap
                        ? ageToColor(board.getAge(r, c))
                        : 0xFF00FFE0;
                } else {
                    gridBuf.pixels[r * gs + c] = 0xFF121826;
                }
            }
        }
        gridBuf.updatePixels();
        gridBuf.endDraw();

        if (alpha < 0.99f) p.tint(255, alpha * 255);
        p.image(gridBuf, dx, dy, dw, dh);
        if (alpha < 0.99f) p.noTint();
    }

    /** Returns the internal pixel buffer for minimap/direct use. */
    public static PGraphics getGridBuffer() { return gridBuf; }

    /** Maps cell age to a heatmap color gradient. */
    public static int ageToColor(int age) {
        if (age <= 0) return 0xFF121826;
        if (age == 1) return 0xFF00FFFF;     // Flash: bright cyan
        if (age <= 5) return lerpColorInt(0xFF1E90FF, 0xFF00FFE0, (age - 2) / 3f);
        if (age <= 15) return lerpColorInt(0xFF00FFE0, 0xFFFF00AA, (age - 5) / 10f);
        if (age <= 40) return lerpColorInt(0xFFFF00AA, 0xFFFF4757, (age - 15) / 25f);
        return lerpColorInt(0xFFFF4757, 0xFFFFFFFF, Math.min((age - 40) / 30f, 1f));
    }

    /** Linear interpolation between two ARGB colors. */
    private static int lerpColorInt(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Draws decorative L-shaped brackets in each window corner. */
    public static void drawCornerAccents(PApplet p) {
        p.stroke(0, 255, 224, 40);
        p.strokeWeight(1);
        int s = 30;
        p.line(10, 10, 10 + s, 10); p.line(10, 10, 10, 10 + s);
        p.line(p.width - 10, 10, p.width - 10 - s, 10);
        p.line(p.width - 10, 10, p.width - 10, 10 + s);
        p.line(10, p.height - 10, 10 + s, p.height - 10);
        p.line(10, p.height - 10, 10, p.height - 10 - s);
        p.line(p.width-10, p.height-10, p.width-10-s, p.height-10);
        p.line(p.width-10, p.height-10, p.width-10, p.height-10-s);
    }

    /** Counts all alive cells on the grid. */
    public static int countAlive(Grid board) {
        int n = 0;
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (board.getCellState(r, c)) n++;
        return n;
    }
}
