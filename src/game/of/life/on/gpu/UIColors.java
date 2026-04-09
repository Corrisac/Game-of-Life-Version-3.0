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

}
