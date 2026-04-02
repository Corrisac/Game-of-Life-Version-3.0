/**
 * ThemeConstants — Centralized interface holding all color constants used throughout the application.
 * Implementing this interface grants direct access to all theme colors without class prefixing.
 * Uses the 0xAARRGGBB hex format required by Processing's color system.
 *
 * DESIGN DECISION: An interface (not a class) is used so that any class can simply
 * "implement ThemeConstants" and gain direct access to all color values without
 * needing to prefix them with a class name (e.g., CYAN instead of ThemeConstants.CYAN).
 */
public interface ThemeConstants {

    // Dark navy background color used as the base for the entire application
    int BG = 0xFF0A0F19;

    // Bright cyan/teal used for primary accents, active elements, and glow effects
    int CYAN = 0xFF00FFE0;

    // Vivid magenta/pink used for secondary accents and highlights
    int MAGENTA = 0xFFFF00AA;

    // Rich purple used for tertiary UI elements and decorative buttons
    int PURPLE = 0xFF7B61FF;

    // Warm orange used for GPU compute actions and attention-grabbing elements
    int ORANGE = 0xFFFF9F43;

    // Alert red used for destructive actions (clear, delete) and paused state indicator
    int RED = 0xFFFF4757;

    // Success green used for play/resume state and positive status indicators
    int GREEN = 0xFF2ED573;

    // Dim semi-transparent white used for subtle, low-priority UI elements
    int WHITE_DIM = 0x40FFFFFF;
}
