import processing.core.PApplet; // Processing core for creating UI components
import java.util.ArrayList;      // Dynamic array for the button list

/**
 * MenuUI — Centralized UI configuration for the dashboard menu screen.
 *
 * DESIGN: All visual constants (labels, colors, sizes, layout ratios) are defined
 * here as easy-to-edit static fields. Change the look-and-feel of the menu by
 * editing THIS file only — no need to touch MenuScreen's rendering or logic code.
 *
 * To customize the menu:
 *   1. Change text labels (TITLE, SUBTITLE, BUTTON_LABELS)
 *   2. Change colors (BUTTON_COLORS, TITLE_GLOW_ALPHA, etc.)
 *   3. Change layout ratios (TITLE_Y_RATIO, BUTTONS_Y_RATIO, etc.)
 *   4. Change sizes (BUTTON_HEIGHT, BUTTON_GAP, etc.)
 */
public class MenuUI implements ThemeConstants {

    // V5.0: DPI scale factor (set from Main.uiScale)
    public static float uiScale = 1.0f;

    // ═══════════════════════════════════════════════════════
    //                  TEXT LABELS (editable)
    // ═══════════════════════════════════════════════════════

    // Main title displayed at the center of the menu screen
    public static final String TITLE = "GAME OF LIFE";

    // Subtitle text shown below the title (tech-stack info)
    public static final String SUBTITLE = "GPU-ACCELERATED  ·  CONWAY'S CELLULAR AUTOMATON  ·  OPENGL";

    // Status bar text shown at the bottom (FPS is appended at runtime)
    public static final String STATUS_BAR_PREFIX = "Processing 4  ·  JOGL 2.6  ·  GLSL  ·  ";

    // Labels for the three navigation buttons (order matters: index 0, 1, 2)
    public static final String[] BUTTON_LABELS = {
        "ENTER SIMULATION",       // Index 0 — launches the simulation screen
        "BACKGROUND & THEORY",    // Index 1 — opens the theory/history screen
        "GPU COMPUTE LAB"         // Index 2 — opens the GPU compute lab
    };

    // ═══════════════════════════════════════════════════════
    //                  COLORS (editable)
    // ═══════════════════════════════════════════════════════

    // Accent colors for each navigation button (matches BUTTON_LABELS order)
    public static final int[] BUTTON_COLORS = { CYAN, PURPLE, MAGENTA };

    // Title glow shadow alpha (0 = no glow, 255 = full glow)
    public static final int TITLE_GLOW_ALPHA = 15;

    // Title main text alpha (brightness of the white title)
    public static final int TITLE_TEXT_ALPHA = 240;

    // Subtitle text alpha (dimness of the cyan subtitle)
    public static final int SUBTITLE_ALPHA = 140;

    // Accent line alpha under the title
    public static final int ACCENT_LINE_ALPHA = 60;

    // Dark overlay alpha over the background grid (controls text readability)
    public static final int OVERLAY_ALPHA = 170;

    // Status bar text alpha (dimness of the bottom status text)
    public static final int STATUS_BAR_ALPHA = 35;

    // ═══════════════════════════════════════════════════════
    //                  LAYOUT RATIOS (editable)
    // ═══════════════════════════════════════════════════════

    // Vertical position of the title as a ratio of window height (0.0=top, 1.0=bottom)
    public static final float TITLE_Y_RATIO = 0.32f;

    // Vertical position where the button stack begins (ratio of window height)
    public static final float BUTTONS_Y_RATIO = 0.52f;

    // Maximum width of buttons as a ratio of window width
    public static final float BUTTON_WIDTH_RATIO = 0.28f;

    // Maximum width of the accent line as a ratio of window width
    public static final float ACCENT_LINE_WIDTH_RATIO = 0.28f;

    // Title font size as a ratio of window width (clamped to max pixels)
    public static final float TITLE_SIZE_RATIO = 0.054f;

    // Subtitle font size as a ratio of window width (clamped to max pixels)
    public static final float SUBTITLE_SIZE_RATIO = 0.012f;

    // ═══════════════════════════════════════════════════════
    //                  SIZES (editable, in pixels)
    // ═══════════════════════════════════════════════════════

    // Maximum title font size in pixels
    public static final float TITLE_MAX_SIZE = 70;

    // Maximum glow shadow font size in pixels
    public static final float TITLE_GLOW_MAX_SIZE = 72;

    // Maximum subtitle font size in pixels
    public static final float SUBTITLE_MAX_SIZE = 14;

    // Maximum button width in pixels
    public static final float BUTTON_MAX_WIDTH = 320;

    // Fixed button height in pixels
    public static final float BUTTON_HEIGHT = 52;

    // Vertical gap between buttons in pixels
    public static final float BUTTON_GAP = 18;

    // Default initial width/height for buttons before responsive repositioning
    public static final float BUTTON_INIT_WIDTH = 200;

    // Maximum accent line width in pixels
    public static final float ACCENT_LINE_MAX_WIDTH = 360;

    // Status bar font size in pixels
    public static final float STATUS_BAR_FONT_SIZE = 10;

    // Vertical offset of the status bar from the bottom edge
    public static final float STATUS_BAR_BOTTOM_OFFSET = 18;



    // ═══════════════════════════════════════════════════════
    //                  BACKGROUND ANIMATION (editable)
    // ═══════════════════════════════════════════════════════

    // Frames between random pattern spawns on the background grid
    public static final int SPAWN_INTERVAL = 120;

    // Frames between simulation steps for the background animation
    public static final int STEP_INTERVAL = 10;

    // Opacity of the background grid (0.0 = invisible, 1.0 = fully visible)
    public static final float GRID_ALPHA = 0.15f;

    // ═══════════════════════════════════════════════════════
    //                  COMPONENT INSTANCES
    // ═══════════════════════════════════════════════════════

    // The list of navigation buttons
    ArrayList<DashButton> buttons;

    /**
     * Constructor — Creates all menu UI components using the configuration above.
     * @param p  Processing applet for component initialization
     */
    public MenuUI(PApplet p) {
        // Create all navigation buttons from the configured labels and colors
        buttons = new ArrayList<>();
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttons.add(new DashButton(p, 0, 0, BUTTON_INIT_WIDTH * uiScale,
                                       BUTTON_HEIGHT * uiScale, BUTTON_LABELS[i], BUTTON_COLORS[i]));
        }
    }

    /**
     * layoutButtons — Repositions all buttons responsively based on current window size.
     * Call this every frame before displaying the buttons.
     * @param p  Processing applet for window dimensions
     */
    public void layoutButtons(PApplet p) {
        // V5.0: Scale pixel constants by DPI factor
        float scaledMaxW = BUTTON_MAX_WIDTH * uiScale;
        float scaledH = BUTTON_HEIGHT * uiScale;
        float scaledGap = BUTTON_GAP * uiScale;

        float btnW = Math.min(scaledMaxW, p.width * BUTTON_WIDTH_RATIO);
        float btnX = (p.width - btnW) / 2f;
        float startY = p.height * BUTTONS_Y_RATIO;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setPos(btnX, startY + i * (scaledH + scaledGap),
                                   btnW, scaledH);
        }
    }
}
