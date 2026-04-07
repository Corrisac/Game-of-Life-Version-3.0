/**
 * UIColors — V5.0: Domain-specific interface for all color constants.
 * Extracted from ThemeConstants to cleanly separate visual design from logic.
 */
public interface UIColors {

    // ═══════════════════════════════════════════════════════
    //                  CORE PALETTE
    // ═══════════════════════════════════════════════════════

    int BG      = 0xFF0A0F19;   // Dark navy background
    int CYAN    = 0xFF00FFE0;   // Primary accent (alive cells, glow)
    int MAGENTA = 0xFFFF00AA;   // Secondary accent
    int PURPLE  = 0xFF7B61FF;   // Tertiary accent
    int ORANGE  = 0xFFFF9F43;   // GPU compute / attention
    int RED     = 0xFFFF4757;   // Destructive / paused
    int GREEN   = 0xFF2ED573;   // Play / success
    int WHITE_DIM = 0x40FFFFFF; // Subtle UI elements

    // ═══════════════════════════════════════════════════════
    //        HEATMAP GRADIENT COLORS
    // ═══════════════════════════════════════════════════════

    // Cell-age gradient: cold → warm → hot → white
    int HEAT_COLD   = 0xFF1E90FF;  // Age 1-3:   dodger blue (just born)
    int HEAT_COOL   = 0xFF00FFE0;  // Age 4-8:   cyan
    int HEAT_WARM   = 0xFFFF00AA;  // Age 9-20:  magenta
    int HEAT_HOT    = 0xFFFF4757;  // Age 21-50: red
    int HEAT_WHITE  = 0xFFFFFFFF;  // Age 50+:   white (ancient)
    int HEAT_FLASH  = 0xFF00FFFF;  // Just-born flash color
}
