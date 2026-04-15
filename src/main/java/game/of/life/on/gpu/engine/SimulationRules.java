package game.of.life.on.gpu.engine;

/**
 * SimulationRules — Cellular automaton rule definitions.
 * Pure data — no framework dependencies.
 */
public final class SimulationRules {

    public static final int RULE_COUNT = 5;
    public static final int GRID_SIZE = 256;

    public static final String[] RULE_NAMES = {
        "CONWAY B3/S23",
        "HIGHLIFE B36/S23",
        "DAY&NIGHT B3678/S34678",
        "SEEDS B2/S",
        "DIAMOEBA B35678/S5678"
    };

    public static final boolean[][] BIRTH_RULES = {
        { false, false, false, true, false, false, false, false, false },
        { false, false, false, true, false, false, true, false, false },
        { false, false, false, true, false, false, true, true, true },
        { false, false, true, false, false, false, false, false, false },
        { false, false, false, true, false, true, true, true, true }
    };

    public static final boolean[][] SURVIVE_RULES = {
        { false, false, true, true, false, false, false, false, false },
        { false, false, true, true, false, false, false, false, false },
        { false, false, false, true, true, false, true, true, true },
        { false, false, false, false, false, false, false, false, false },
        { false, false, false, false, false, true, true, true, true }
    };

    public static final int[] SPEED_LEVELS = { 1, 2, 5, 10, 0 };
    public static final String[] SPEED_NAMES = { "1x", "2x", "5x", "10x", "MAX" };

    private SimulationRules() {} // Prevent instantiation
}
