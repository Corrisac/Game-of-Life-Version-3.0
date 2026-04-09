import processing.core.PApplet;

/**
 * MenuScreen V4.0 — Uses fast pixel-buffer rendering for background grid.
 * Transitions use Main.transitionTo() for smooth fades.
 */
public class MenuScreen implements ThemeConstants {

    private Main app;
    private MenuUI ui;

    public MenuScreen(Main app) {
        this.app = app;
        this.ui = new MenuUI(app);
    }

    public void draw(ParticleSystem particles, float dt) {
        GridRenderer.drawBackground(app);

        // Animated background grid
        if (app.frameCount % MenuUI.SPAWN_INTERVAL == 0)
            PatternLibrary.spawnRandomPattern(app.gameBoard, app);
        if (app.frameCount % MenuUI.STEP_INTERVAL == 0)
            app.gameBoard.updateToNextGeneration();

        // V4.0: Fast pixel-buffer rendering instead of per-cell rects
        int displaySize = Math.max(app.width, app.height);
        int ox = (app.width - displaySize) / 2;
        int oy = (app.height - displaySize) / 2;
        GridRenderer.drawGridAdvanced(app, app.gameBoard,
            ox, oy, displaySize, displaySize, MenuUI.GRID_ALPHA, false);

        // Dark overlay
        app.noStroke();
        app.fill(10, 15, 25, MenuUI.OVERLAY_ALPHA);
        app.rect(0, 0, app.width, app.height);

        particles.updateAndDraw(dt);

        // Title with glow
        float titleY = app.height * MenuUI.TITLE_Y_RATIO;
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.fill(0, 255, 224, MenuUI.TITLE_GLOW_ALPHA);
        app.textSize(Math.min(MenuUI.TITLE_GLOW_MAX_SIZE, app.width * 0.055f));
        app.text(MenuUI.TITLE, app.width / 2f + 2, titleY + 2);
        app.fill(255, 255, 255, MenuUI.TITLE_TEXT_ALPHA);
        app.textSize(Math.min(MenuUI.TITLE_MAX_SIZE, app.width * MenuUI.TITLE_SIZE_RATIO));
        app.text(MenuUI.TITLE, app.width / 2f, titleY);

        // Version badge
        app.fill(0, 255, 224, 100);
        app.textSize(11);
        app.text("V4.0", app.width / 2f, titleY + 32);

        // Accent line
        app.stroke(0, 255, 224, MenuUI.ACCENT_LINE_ALPHA);
        app.strokeWeight(1);
        float lineW = Math.min(MenuUI.ACCENT_LINE_MAX_WIDTH,
                               app.width * MenuUI.ACCENT_LINE_WIDTH_RATIO);
        app.line(app.width/2f - lineW/2, titleY + 45,
                 app.width/2f + lineW/2, titleY + 45);
        app.noStroke();

        // Subtitle
        app.fill(0, 255, 224, MenuUI.SUBTITLE_ALPHA);
        app.textSize(Math.min(MenuUI.SUBTITLE_MAX_SIZE, app.width * MenuUI.SUBTITLE_SIZE_RATIO));
        app.text(MenuUI.SUBTITLE, app.width / 2f, titleY + 65);

        // Buttons
        ui.layoutButtons(app);
        for (int i = 0; i < ui.buttons.size(); i++) {
            ui.buttons.get(i).update(app.mouseX, app.mouseY, dt);
            ui.buttons.get(i).display();
        }

        // Status bar
        app.fill(255, 255, 255, MenuUI.STATUS_BAR_ALPHA);
        app.textSize(MenuUI.STATUS_BAR_FONT_SIZE);
        app.text(MenuUI.STATUS_BAR_PREFIX + (int) app.frameRate + " FPS",
                 app.width / 2f, app.height - MenuUI.STATUS_BAR_BOTTOM_OFFSET);

    }

    public boolean handleMousePressed(float mx, float my) {
        for (int i = 0; i < ui.buttons.size(); i++) {
            if (ui.buttons.get(i).checkClick(mx, my)) {
                if (i == 0) app.transitionTo(2);       // Simulation
                else if (i == 1) app.transitionTo(1);  // Theory
                else if (i == 2) app.transitionTo(3);  // GPU Lab
                return true;
            }
        }
        return false;
    }
}
