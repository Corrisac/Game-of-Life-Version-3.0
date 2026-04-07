/**
 * UILayout — V5.0: Domain-specific interface for grid and speed configuration.
 * Extracted from ThemeConstants to cleanly separate layout config from logic.
 */
public interface UILayout {

    // ═══════════════════════════════════════════════════════
    //                  GRID CONFIGURATION
    // ═══════════════════════════════════════════════════════

    int GRID_SIZE = 256;

    // ═══════════════════════════════════════════════════════
    //        SPEED LEVELS
    // ═══════════════════════════════════════════════════════

    // Steps per frame at each speed level
    int[] SPEED_LEVELS = { 1, 2, 5, 10, 0 };  // 0 = MAX (every frame, no skip)
    String[] SPEED_NAMES = { "1×", "2×", "5×", "10×", "MAX" };
}
