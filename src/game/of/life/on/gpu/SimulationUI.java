import processing.core.PApplet;
import java.util.ArrayList;

/**
 * SimulationUI V4.0 — Adds Rules, Speed, and HUD toggle buttons.
 */
public class SimulationUI implements ThemeConstants {

    // V5.0: DPI scale factor (set from Main.uiScale)
    public static float uiScale = 1.0f;

    public static final String SCREEN_TITLE = "SIMULATION";
    public static final String PAUSED_TEXT = "● PAUSED";
    public static final String RUNNING_TEXT = "● RUNNING";
    public static final String PLAY_LABEL = "▶  PLAY";
    public static final String PAUSE_LABEL = "⏸  PAUSE";

    // V4.0: Added RULES, SPEED, HUD buttons
    public static final String[] BUTTON_LABELS = {
        "▶  PLAY",       // 0 — play/pause
        "✕  CLEAR",      // 1 — clear
        "BRUSH 1x1",     // 2 — brush size
        "GLIDER",        // 3 — pattern: glider
        "GUN",           // 4 — pattern: gun
        "RULES",         // 5 — V4.0: cycle rule set
        "SPEED 1×",      // 6 — V4.0: cycle speed
        "HUD",           // 7 — V4.0: toggle analytics
        "← MENU"         // 8 — return to menu
    };

    public static final int[] BUTTON_COLORS = {
        GREEN, RED, WHITE_DIM, CYAN, PURPLE,
        MAGENTA, ORANGE, CYAN, WHITE_DIM
    };

    public static final int PLAY_COLOR = GREEN;
    public static final int PAUSE_COLOR = ORANGE;
    public static final int PAUSED_INDICATOR_COLOR = RED;
    public static final int RUNNING_INDICATOR_COLOR = GREEN;

    // V5.0: These are base values — use scaled() getters for actual dimensions
    public static final int BASE_TOP_BAR_HEIGHT = 44;
    public static final int BASE_BOTTOM_BAR_HEIGHT = 60;
    public static final int BASE_GRID_MARGIN_X = 25;
    public static final float BASE_BUTTON_MAX_WIDTH = 110;
    public static final float BASE_BUTTON_HEIGHT = 38;
    public static final float BASE_BUTTON_GAP = 8;
    public static final float BASE_BUTTON_BOTTOM_OFFSET = 50;
    public static final float BUTTON_INIT_WIDTH = 100;
    public static final float BUTTON_INIT_HEIGHT = 38;
    public static final int STEP_INTERVAL = 5;

    // V5.0: Scaled accessor methods for DPI-aware layout
    public static int topBarHeight()    { return (int)(BASE_TOP_BAR_HEIGHT * uiScale); }
    public static int bottomBarHeight() { return (int)(BASE_BOTTOM_BAR_HEIGHT * uiScale); }
    public static int gridMarginX()     { return (int)(BASE_GRID_MARGIN_X * uiScale); }

    ArrayList<DashButton> buttons;

    public SimulationUI(PApplet p) {
        buttons = new ArrayList<>();
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            buttons.add(new DashButton(p, 0, 0, BUTTON_INIT_WIDTH,
                BUTTON_INIT_HEIGHT, BUTTON_LABELS[i], BUTTON_COLORS[i]));
        }
    }

    public void layoutButtons(PApplet p) {
        float scaledMaxW = BASE_BUTTON_MAX_WIDTH * uiScale;
        float scaledH = BASE_BUTTON_HEIGHT * uiScale;
        float scaledGap = BASE_BUTTON_GAP * uiScale;
        float scaledOffset = BASE_BUTTON_BOTTOM_OFFSET * uiScale;

        float btnW = Math.min(scaledMaxW, (p.width - 60 * uiScale) / (float) buttons.size());
        float total = btnW * buttons.size() + scaledGap * (buttons.size() - 1);
        float bx = (p.width - total) / 2f;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setPos(bx + i * (btnW + scaledGap),
                p.height - scaledOffset, btnW, scaledH);
        }
    }

    public void updateDynamicLabels(boolean isPaused, int brushSize,
                                     int ruleSet, int speedLevel) {
        buttons.get(0).label = isPaused ? PLAY_LABEL : PAUSE_LABEL;
        buttons.get(0).baseColor = isPaused ? PLAY_COLOR : PAUSE_COLOR;
        buttons.get(2).label = "BRUSH " + brushSize + "x" + brushSize;
        buttons.get(5).label = RULE_NAMES[ruleSet].substring(0,
            Math.min(RULE_NAMES[ruleSet].length(), 12));
        buttons.get(6).label = "SPEED " + SPEED_NAMES[speedLevel];
    }
}
