package game.of.life.on.gpu.engine;

/**
 * DualGrid — Two-species cellular automaton data model.
 *
 * Both species follow Conway B3/S23 independently.
 * The ONLY interaction: if a dead cell would receive both a Life birth
 * (exactly 3 Life neighbours) AND an Antilife birth (exactly 3 Anti
 * neighbours) on the same tick, both are cancelled — the cell stays dead.
 *
 * Invariant: lifeFront[i] & antiFront[i] == 0 for all i, always.
 */
public class DualGrid {

    // Conway B3/S23 tables (index = neighbour count)
    private static final boolean[] BIRTH   = {false,false,false,true,false,false,false,false,false};
    private static final boolean[] SURVIVE = {false,false,true,true,false,false,false,false,false};

    public byte[] lifeFront, lifeBack;
    public byte[] antiFront, antiBack;
    private int[] lifeAgeFront, lifeAgeBack;
    private int[] antiAgeFront, antiAgeBack;

    // Per-generation stats
    public int lifePopulation, antiPopulation;
    public int lifeBorn, lifeDied, antiBorn, antiDied;
    public int generationNum;
    public int peakLife, peakAnti;

    // Population history ring buffers for sparklines (200 samples each)
    public final int[] lifeHistory = new int[200];
    public final int[] antiHistory = new int[200];
    public int histIdx = 0;

    private final int rows, cols;

    public DualGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        lifeFront    = new byte[rows * cols];
        lifeBack     = new byte[rows * cols];
        antiFront    = new byte[rows * cols];
        antiBack     = new byte[rows * cols];
        lifeAgeFront = new int[rows * cols];
        lifeAgeBack  = new int[rows * cols];
        antiAgeFront = new int[rows * cols];
        antiAgeBack  = new int[rows * cols];
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // ── Cell Accessors ────────────────────────────────────────────

    public boolean getLife(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        return lifeFront[r * cols + c] != 0;
    }

    public boolean getAnti(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        return antiFront[r * cols + c] != 0;
    }

    public int getLifeAge(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return 0;
        return lifeAgeFront[r * cols + c];
    }

    public int getAntiAge(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return 0;
        return antiAgeFront[r * cols + c];
    }

    /** Sets a Life cell. Clears Antilife at same position to maintain invariant. */
    public void setLife(int r, int c, boolean alive) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        int idx = r * cols + c;
        if (alive) {
            lifeFront[idx]    = 1;
            antiFront[idx]    = 0;
            antiAgeFront[idx] = 0;
            if (lifeAgeFront[idx] == 0) lifeAgeFront[idx] = 1;
        } else {
            lifeFront[idx]    = 0;
            lifeAgeFront[idx] = 0;
        }
    }

    /** Sets an Antilife cell. Clears Life at same position to maintain invariant. */
    public void setAnti(int r, int c, boolean alive) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        int idx = r * cols + c;
        if (alive) {
            antiFront[idx]    = 1;
            lifeFront[idx]    = 0;
            lifeAgeFront[idx] = 0;
            if (antiAgeFront[idx] == 0) antiAgeFront[idx] = 1;
        } else {
            antiFront[idx]    = 0;
            antiAgeFront[idx] = 0;
        }
    }

    /** Erases both species at (r, c). */
    public void erase(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        int idx = r * cols + c;
        lifeFront[idx] = antiFront[idx] = 0;
        lifeAgeFront[idx] = antiAgeFront[idx] = 0;
    }

    // ── Simulation Step ───────────────────────────────────────────

    public void updateToNextGeneration() {
        int lBorn = 0, lDied = 0, aBorn = 0, aDied = 0, lPop = 0, aPop = 0;

        // Fast inner loop — no bounds check needed for rows 1..N-2, cols 1..M-2
        for (int r = 1; r < rows - 1; r++) {
            int ro = r * cols;
            for (int c = 1; c < cols - 1; c++) {
                int idx = ro + c;

                int lN = lifeFront[idx - cols - 1] + lifeFront[idx - cols] + lifeFront[idx - cols + 1]
                       + lifeFront[idx - 1]                                 + lifeFront[idx + 1]
                       + lifeFront[idx + cols - 1] + lifeFront[idx + cols] + lifeFront[idx + cols + 1];

                int aN = antiFront[idx - cols - 1] + antiFront[idx - cols] + antiFront[idx - cols + 1]
                       + antiFront[idx - 1]                                 + antiFront[idx + 1]
                       + antiFront[idx + cols - 1] + antiFront[idx + cols] + antiFront[idx + cols + 1];

                boolean lAlive = lifeFront[idx] != 0;
                boolean aAlive = antiFront[idx] != 0;
                boolean dead   = !lAlive && !aAlive;

                boolean lNext = (lAlive && SURVIVE[lN]) || (dead && BIRTH[lN]);
                boolean aNext = (aAlive && SURVIVE[aN]) || (dead && BIRTH[aN]);

                // Contested birth rule: both want to be born on same dead cell → neither does
                if (dead && BIRTH[lN] && BIRTH[aN]) { lNext = false; aNext = false; }

                if (lNext) {
                    lifeBack[idx]    = 1;
                    lifeAgeBack[idx] = lAlive ? lifeAgeFront[idx] + 1 : 1;
                    lPop++; if (!lAlive) lBorn++;
                } else {
                    lifeBack[idx]    = 0;
                    lifeAgeBack[idx] = 0;
                    if (lAlive) lDied++;
                }

                if (aNext) {
                    antiBack[idx]    = 1;
                    antiAgeBack[idx] = aAlive ? antiAgeFront[idx] + 1 : 1;
                    aPop++; if (!aAlive) aBorn++;
                } else {
                    antiBack[idx]    = 0;
                    antiAgeBack[idx] = 0;
                    if (aAlive) aDied++;
                }
            }
        }

        // Edge cells — slow path with bounds checking
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r > 0 && r < rows - 1 && c > 0 && c < cols - 1) continue;
                int idx = r * cols + c;
                int lN = 0, aN = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r + dr, nc = c + dc;
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                            int ni = nr * cols + nc;
                            lN += lifeFront[ni];
                            aN += antiFront[ni];
                        }
                    }
                }

                boolean lAlive = lifeFront[idx] != 0;
                boolean aAlive = antiFront[idx] != 0;
                boolean dead   = !lAlive && !aAlive;

                boolean lNext = (lAlive && SURVIVE[lN]) || (dead && BIRTH[lN]);
                boolean aNext = (aAlive && SURVIVE[aN]) || (dead && BIRTH[aN]);

                if (dead && BIRTH[lN] && BIRTH[aN]) { lNext = false; aNext = false; }

                if (lNext) {
                    lifeBack[idx]    = 1;
                    lifeAgeBack[idx] = lAlive ? lifeAgeFront[idx] + 1 : 1;
                    lPop++; if (!lAlive) lBorn++;
                } else {
                    lifeBack[idx]    = 0;
                    lifeAgeBack[idx] = 0;
                    if (lAlive) lDied++;
                }

                if (aNext) {
                    antiBack[idx]    = 1;
                    antiAgeBack[idx] = aAlive ? antiAgeFront[idx] + 1 : 1;
                    aPop++; if (!aAlive) aBorn++;
                } else {
                    antiBack[idx]    = 0;
                    antiAgeBack[idx] = 0;
                    if (aAlive) aDied++;
                }
            }
        }

        // Swap buffers (zero allocation — reference swap only)
        byte[] tb; int[] ti;
        tb = lifeFront;    lifeFront    = lifeBack;    lifeBack    = tb;
        tb = antiFront;    antiFront    = antiBack;    antiBack    = tb;
        ti = lifeAgeFront; lifeAgeFront = lifeAgeBack; lifeAgeBack = ti;
        ti = antiAgeFront; antiAgeFront = antiAgeBack; antiAgeBack = ti;

        lifeBorn = lBorn; lifeDied = lDied;
        antiBorn = aBorn; antiDied = aDied;
        lifePopulation = lPop;
        antiPopulation = aPop;
        if (lPop > peakLife) peakLife = lPop;
        if (aPop > peakAnti) peakAnti = aPop;
        generationNum++;

        lifeHistory[histIdx % 200] = lPop;
        antiHistory[histIdx % 200] = aPop;
        histIdx++;
    }

    public void clear() {
        java.util.Arrays.fill(lifeFront,    (byte) 0);
        java.util.Arrays.fill(antiFront,    (byte) 0);
        java.util.Arrays.fill(lifeAgeFront, 0);
        java.util.Arrays.fill(antiAgeFront, 0);
        lifeBorn = lifeDied = antiBorn = antiDied = 0;
        lifePopulation = antiPopulation = 0;
        peakLife = peakAnti = 0;
        generationNum = 0;
        histIdx = 0;
        java.util.Arrays.fill(lifeHistory, 0);
        java.util.Arrays.fill(antiHistory, 0);
    }

    public void recount() {
        int l = 0, a = 0;
        for (int i = 0; i < rows * cols; i++) { l += lifeFront[i]; a += antiFront[i]; }
        lifePopulation = l;
        antiPopulation = a;
    }
}
