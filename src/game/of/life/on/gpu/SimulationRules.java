/**
 * SimulationRules — V5.0: Domain-specific interface for cellular automaton rules.
 * Extracted from ThemeConstants to cleanly separate game logic from visual design.
 */
public interface SimulationRules {

    int RULE_COUNT = 5;

    // Display names for each rule set
    String[] RULE_NAMES = {
        "CONWAY B3/S23",
        "HIGHLIFE B36/S23",
        "DAY&NIGHT B3678/S34678",
        "SEEDS B2/S",
        "DIAMOEBA B35678/S5678"
    };

    // Birth rules: which neighbor counts cause birth (array of booleans per rule)
    // Index = neighbor count (0-8). true = birth at that count.
    boolean[][] BIRTH_RULES = {
        // Conway:    B3
        { false, false, false, true, false, false, false, false, false },
        // HighLife:  B36
        { false, false, false, true, false, false, true, false, false },
        // Day&Night: B3678
        { false, false, false, true, false, false, true, true, true },
        // Seeds:     B2
        { false, false, true, false, false, false, false, false, false },
        // Diamoeba:  B35678
        { false, false, false, true, false, true, true, true, true }
    };

    // Survival rules: which neighbor counts let a live cell survive
    boolean[][] SURVIVE_RULES = {
        // Conway:    S23
        { false, false, true, true, false, false, false, false, false },
        // HighLife:  S23
        { false, false, true, true, false, false, false, false, false },
        // Day&Night: S34678
        { false, false, false, true, true, false, true, true, true },
        // Seeds:     S (none — all cells die after 1 gen)
        { false, false, false, false, false, false, false, false, false },
        // Diamoeba:  S5678
        { false, false, false, false, false, true, true, true, true }
    };
}
