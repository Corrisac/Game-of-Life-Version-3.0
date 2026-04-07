import processing.core.PApplet; // Processing core for creating UI components
import java.util.ArrayList;      // Dynamic array for the button list

/**
 * GPULabUI — Centralized UI configuration for the GPU compute lab screen.
 *
 * DESIGN: All visual constants (labels, colors, sizes, layout values) are defined
 * here. Change the GPU lab's look-and-feel by editing THIS file only — no need
 * to touch GPULabScreen's rendering or logic code.
 *
 * To customize the GPU lab screen:
 *   1. Change button labels (BUTTON_LABELS) and colors (BUTTON_COLORS)
 *   2. Change dial settings (DIAL_LABEL, DIAL_MAX, DIAL_DEFAULT)
 *   3. Change sidebar layout (SIDEBAR_WIDTH, INPUT_HEIGHT, BUTTON_SPACING)
 *   4. Change text labels (SCREEN_TITLE, INPUT_LABEL, PLACEHOLDER_*, COMPUTING_LABEL)
 */
public class GPULabUI implements ThemeConstants {

    // V5.0: DPI scale factor (set from Main.uiScale)
    public static float uiScale = 1.0f;

    // ═══════════════════════════════════════════════════════
    //                  TEXT LABELS (editable)
    // ═══════════════════════════════════════════════════════

    // Sidebar heading text
    public static final String SCREEN_TITLE = "GPU LAB";

    // Label above the iteration input box
    public static final String INPUT_LABEL = "CLICK TO TYPE ITERATIONS";

    // Right-pane placeholder title (before any computation)
    public static final String PLACEHOLDER_TITLE = "GPU RESULT";

    // Right-pane placeholder subtitle
    public static final String PLACEHOLDER_SUBTITLE = "Output renders here after compute";

    // Text shown during computation progress
    public static final String COMPUTING_LABEL = "COMPUTING";

    // Top bar labels for the two panes
    public static final String LEFT_PANE_LABEL = "CPU GRID (EDITABLE)";
    public static final String RIGHT_PANE_LABEL = "GPU OUTPUT";

    // Button labels (order determines button index for input handling)
    public static final String[] BUTTON_LABELS = {
        "⚡ COMPUTE",    // Index 0 — starts GPU computation
        "✕  CLEAR",      // Index 1 — clears all cells and result
        "GLIDER",        // Index 2 — spawns a glider pattern
        "GUN",           // Index 3 — spawns Gosper's glider gun
        "← MENU"         // Index 4 — returns to the dashboard menu
    };

    // ═══════════════════════════════════════════════════════
    //                  COLORS (editable)
    // ═══════════════════════════════════════════════════════

    // Accent colors for each sidebar button (matches BUTTON_LABELS order)
    public static final int[] BUTTON_COLORS = { ORANGE, RED, CYAN, PURPLE, WHITE_DIM };

    // ═══════════════════════════════════════════════════════
    //                  DIAL SETTINGS (editable)
    // ═══════════════════════════════════════════════════════

    // Label displayed inside the dial
    public static final String DIAL_LABEL = "ITERATIONS";

    // Maximum value the dial can represent
    public static final int DIAL_MAX = 1000000;

    // Default number of iterations shown on first load
    public static final int DIAL_DEFAULT = 1000;

    // Initial dial radius in pixels
    public static final float DIAL_INITIAL_RADIUS = 80;

    // Dial vertical center position in pixels from the top
    public static final float DIAL_CENTER_Y = 120;

    // ═══════════════════════════════════════════════════════
    //                  LAYOUT SIZES (editable, in pixels)
    // ═══════════════════════════════════════════════════════

    // V5.0: These are base values — use scaled accessors below
    public static final int BASE_SIDEBAR_WIDTH = 240;
    public static final int BASE_TOP_BAR_HEIGHT = 44;
    public static final float BASE_INPUT_HEIGHT = 32;
    public static final float BASE_INPUT_MARGIN = 30;
    public static final float BASE_INPUT_BUTTON_GAP = 16;
    public static final float BASE_BUTTON_SPACING = 48;
    public static final float BASE_BUTTON_HEIGHT = 38;
    public static final float BASE_BUTTON_MARGIN = 20;
    public static final float BUTTON_INIT_SIZE = 120;
    public static final float INPUT_CORNER_RADIUS = 8;
    public static final float BASE_INPUT_FONT_SIZE = 22;
    public static final int INPUT_MAX_DIGITS = 7;
    public static final float PROGRESS_BAR_MARGIN = 40;
    public static final float PROGRESS_BAR_HEIGHT = 16;

    // V5.0: Scaled accessor methods for DPI-aware layout
    public static int sidebarWidth()   { return (int)(BASE_SIDEBAR_WIDTH * uiScale); }
    public static int topBarHeight()   { return (int)(BASE_TOP_BAR_HEIGHT * uiScale); }
    public static float inputHeight()  { return BASE_INPUT_HEIGHT * uiScale; }
    public static float inputMargin()  { return BASE_INPUT_MARGIN * uiScale; }
    public static float inputFontSize(){ return BASE_INPUT_FONT_SIZE * uiScale; }

    // ═══════════════════════════════════════════════════════
    //                  COMPONENT INSTANCES
    // ═══════════════════════════════════════════════════════

    // The circular dial for selecting iteration count
    CircularDial dial;

    // The list of sidebar action buttons
    ArrayList<DashButton> buttons;

    /**
     * Constructor — Creates all GPU lab UI components from the config above.
     * @param p  Processing applet for component initialization
     */
    public GPULabUI(PApplet p) {
        // Create the iteration-count dial with configured parameters
        dial = new CircularDial(p, 0, 0, DIAL_INITIAL_RADIUS, DIAL_MAX, DIAL_LABEL);
        // Set the dial to its configured default value
        dial.setValue(DIAL_DEFAULT);

        // Create all sidebar buttons from the configured labels and colors
        buttons = new ArrayList<>();
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttons.add(new DashButton(p, 0, 0, BUTTON_INIT_SIZE,
                                       BASE_BUTTON_HEIGHT, BUTTON_LABELS[i], BUTTON_COLORS[i]));
        }
    }

    /**
     * layoutComponents — Repositions the dial and buttons in the sidebar.
     * @param p  Processing applet for window dimensions
     */
    public void layoutComponents(PApplet p) {
        int sideW = sidebarWidth();
        float dialR = Math.min(65 * uiScale, (sideW - 40 * uiScale) / 3f);
        dial.setPos(sideW / 2f, DIAL_CENTER_Y * uiScale, dialR);

        float inputY = DIAL_CENTER_Y * uiScale + dialR + 18 * uiScale;
        float scaledInputH = inputHeight();
        float scaledGap = BASE_INPUT_BUTTON_GAP * uiScale;
        float scaledSpacing = BASE_BUTTON_SPACING * uiScale;
        float scaledBtnH = BASE_BUTTON_HEIGHT * uiScale;
        float scaledMargin = BASE_BUTTON_MARGIN * uiScale;
        float by = inputY + scaledInputH + scaledGap;
        float btnW = sideW - scaledMargin * 2;

        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setPos(scaledMargin, by + i * scaledSpacing, btnW, scaledBtnH);
        }
    }

    /**
     * getInputY — Returns the Y position of the iteration input box (derived from dial).
     * @return  The input box top Y coordinate in pixels
     */
    public float getInputY() {
        int sideW = sidebarWidth();
        float dialR = Math.min(65 * uiScale, (sideW - 40 * uiScale) / 3f);
        return DIAL_CENTER_Y * uiScale + dialR + 18 * uiScale;
    }

    /**
     * getInputWidth — Returns the width of the iteration input box.
     * @return  Input box width in pixels
     */
    public float getInputWidth() {
        return sidebarWidth() - inputMargin() * 2;
    }
}
