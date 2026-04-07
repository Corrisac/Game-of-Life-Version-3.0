import processing.core.PApplet;

/**
 * SimulationScreen V4.0 — GPU-quality rendering, zoom/pan, analytics HUD,
 * multiple rule sets, adjustable speed, and heatmap toggle.
 *
 * V4.0 CHANGES:
 *   - CPU simulation via Grid.updateToNextGeneration() (supports all rule sets + age tracking)
 *   - Pixel-buffer rendering (no more 65K rect() calls)
 *   - Mouse-wheel zoom (1x to 16x) with click-drag panning
 *   - Minimap when zoomed in
 *   - Live analytics HUD (population sparkline, born/died, GPS)
 *   - Speed multiplier (1x, 2x, 5x, 10x, MAX)
 */
public class SimulationScreen implements ThemeConstants {

    private Main app;
    private SimulationUI ui;

    // V4.0: Zoom and pan
    float zoomLevel = 1.0f;
    float panX = 0, panY = 0;
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float panStartX, panStartY;

    // V4.0: Speed and display
    int speedLevel = 0;          // Index into SPEED_LEVELS
    boolean showHeatmap = false;
    boolean showHUD = false;

    // V4.0: Analytics
    private long lastStepTime = 0;
    private float gensPerSec = 0;
    private int stepsSinceLastCalc = 0;
    private long lastGPSCalc = 0;

    public SimulationScreen(Main app) {
        this.app = app;
        this.ui = new SimulationUI(app);
    }

    public void draw(float dt) {
        GridRenderer.drawBackground(app);

        // --- Simulation stepping ---
        if (!app.isPaused) {
            int stepsPerFrame = SPEED_LEVELS[speedLevel];
            if (stepsPerFrame == 0) stepsPerFrame = 1; // MAX = every frame
            boolean shouldStep = (stepsPerFrame == 1 && app.frameCount % SimulationUI.STEP_INTERVAL == 0)
                || stepsPerFrame > 1;
            if (shouldStep) {
                int count = (stepsPerFrame <= 1) ? 1 : stepsPerFrame;
                for (int i = 0; i < count; i++) {
                    app.gameBoard.updateToNextGeneration();
                    stepsSinceLastCalc++;
                }
            }
        }

        // GPS calculation
        long now = System.currentTimeMillis();
        if (now - lastGPSCalc > 500) {
            float elapsed = (now - lastGPSCalc) / 1000f;
            if (elapsed > 0) gensPerSec = stepsSinceLastCalc / elapsed;
            stepsSinceLastCalc = 0;
            lastGPSCalc = now;
        }

        // --- Grid rendering area ---
        int topH = SimulationUI.topBarHeight();
        int botH = SimulationUI.bottomBarHeight();
        int mx = SimulationUI.gridMarginX();
        int gw = app.width - mx * 2;
        int gh = app.height - topH - botH;

        // Base cell size (fits grid in view)
        float baseCellSize = Math.min((float) gw / GRID_SIZE, (float) gh / GRID_SIZE);
        float cellSize = baseCellSize * zoomLevel;
        float gridW = GRID_SIZE * cellSize;
        float gridH = GRID_SIZE * cellSize;
        float gridX = mx + (gw - gridW) / 2f + panX;
        float gridY = topH + (gh - gridH) / 2f + panY;

        // Clip to grid area
        app.clip(mx, topH, gw, gh);

        // Draw grid using fast pixel buffer
        GridRenderer.drawGridAdvanced(app, app.gameBoard,
            (int) gridX, (int) gridY, (int) gridW, (int) gridH, 1.0f, showHeatmap);

        app.noClip();

        // --- Minimap when zoomed ---
        if (zoomLevel > 1.5f) {
            float mmSize = 80;
            float mmX = app.width - mx - mmSize - 5;
            float mmY = topH + 5;
            app.noStroke();
            app.fill(0, 0, 0, 180);
            app.rect(mmX - 2, mmY - 2, mmSize + 4, mmSize + 4, 4);
            GridRenderer.drawGridAdvanced(app, app.gameBoard,
                (int) mmX, (int) mmY, (int) mmSize, (int) mmSize, 0.7f, showHeatmap);
            // Viewport indicator
            float vpRatio = 1f / zoomLevel;
            float vpW = mmSize * vpRatio;
            float vpH = mmSize * vpRatio;
            float vpOffX = -panX / gridW * mmSize;
            float vpOffY = -panY / gridH * mmSize;
            app.noFill();
            app.stroke(CYAN, 200);
            app.strokeWeight(1);
            app.rect(mmX + (mmSize - vpW) / 2f + vpOffX,
                     mmY + (mmSize - vpH) / 2f + vpOffY, vpW, vpH);
        }

        // --- Top bar ---
        app.noStroke();
        app.fill(10, 15, 25, 220);
        app.rect(0, 0, app.width, topH);
        app.stroke(0, 255, 224, 40);
        app.strokeWeight(1);
        app.line(0, topH, app.width, topH);

        app.fill(255, 255, 255, 200);
        app.textAlign(PApplet.LEFT, PApplet.CENTER);
        app.textSize(20);
        app.text(SimulationUI.SCREEN_TITLE, 20, 22);

        app.fill(app.isPaused ? SimulationUI.PAUSED_INDICATOR_COLOR
                              : SimulationUI.RUNNING_INDICATOR_COLOR);
        app.textAlign(PApplet.RIGHT, PApplet.CENTER);
        app.textSize(16);
        app.text(app.isPaused ? SimulationUI.PAUSED_TEXT
                              : SimulationUI.RUNNING_TEXT, app.width - 20, 22);

        // Center stats
        app.fill(255, 255, 255, 80);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.textSize(15);
        String rn = RULE_NAMES[app.gameBoard.getRuleSet()];
        app.text("GEN: " + app.gameBoard.generationNum
            + "  |  POP: " + String.format("%,d", app.gameBoard.population)
            + "  |  " + rn
            + "  |  " + (int) app.frameRate + " FPS", app.width / 2f, 22);

        // --- Analytics HUD ---
        if (showHUD) drawAnalyticsHUD();

        // --- Bottom bar ---
        app.noStroke();
        app.fill(10, 15, 25, 220);
        app.rect(0, app.height - botH, app.width, botH);
        app.stroke(255, 255, 255, 15);
        app.strokeWeight(1);
        app.line(0, app.height - botH, app.width, app.height - botH);

        ui.updateDynamicLabels(app.isPaused, app.brushSize,
            app.gameBoard.getRuleSet(), speedLevel);
        ui.layoutButtons(app);
        for (int i = 0; i < ui.buttons.size(); i++) {
            ui.buttons.get(i).update(app.mouseX, app.mouseY, dt);
            ui.buttons.get(i).display();
        }
    }

    /** Draws the analytics overlay with sparkline and stats. */
    private void drawAnalyticsHUD() {
        float hx = 20, hy = SimulationUI.topBarHeight() + 10;
        float hw = 220, hh = 130;

        // Background panel
        app.noStroke();
        app.fill(10, 15, 25, 210);
        app.rect(hx, hy, hw, hh, 8);
        app.stroke(0, 255, 224, 40);
        app.strokeWeight(1);
        app.noFill();
        app.rect(hx, hy, hw, hh, 8);

        // Stats text
        app.fill(255, 255, 255, 180);
        app.textAlign(PApplet.LEFT, PApplet.TOP);
        app.textSize(14);
        float ty = hy + 8;
        app.text("POP: " + String.format("%,d", app.gameBoard.population), hx + 10, ty);
        app.text("PEAK: " + String.format("%,d", app.gameBoard.peakPop), hx + 120, ty);
        ty += 14;
        app.fill(0, 255, 224, 160);
        app.text("BORN: +" + app.gameBoard.bornThisGen, hx + 10, ty);
        app.fill(255, 71, 87, 160);
        app.text("DIED: -" + app.gameBoard.diedThisGen, hx + 120, ty);
        ty += 14;
        app.fill(255, 159, 67, 160);
        app.text("GPS: " + String.format("%.0f", gensPerSec), hx + 10, ty);
        app.fill(255, 255, 255, 80);
        app.text("ZOOM: " + String.format("%.1f×", zoomLevel), hx + 120, ty);

        // Sparkline
        float sx = hx + 10, sy = ty + 18, sw = hw - 20, sh = 45;
        app.fill(255, 255, 255, 8);
        app.noStroke();
        app.rect(sx, sy, sw, sh, 4);

        int[] hist = app.gameBoard.popHistory;
        int idx = app.gameBoard.popHistIdx;
        int maxPop = 1;
        for (int i = 0; i < hist.length; i++)
            if (hist[i] > maxPop) maxPop = hist[i];

        app.noFill();
        app.stroke(0, 255, 224, 150);
        app.strokeWeight(1);
        app.beginShape();
        for (int i = 0; i < hist.length; i++) {
            int di = (idx - hist.length + i + hist.length * 2) % hist.length;
            float val = (float) hist[di] / maxPop;
            app.vertex(sx + i * (sw / hist.length), sy + sh * (1 - val));
        }
        app.endShape();
    }

    // --- Input handling ---

    public void handleMousePressed(float mx, float my) {
        if (my > app.height - SimulationUI.bottomBarHeight()) {
            if (ui.buttons.get(0).checkClick(mx, my)) app.isPaused = !app.isPaused;
            else if (ui.buttons.get(1).checkClick(mx, my)) app.gameBoard.clearBoard();
            else if (ui.buttons.get(2).checkClick(mx, my))
                app.brushSize = (app.brushSize == 1) ? 3 : (app.brushSize == 3) ? 5 : 1;
            else if (ui.buttons.get(3).checkClick(mx, my))
                PatternLibrary.spawnGlider(app.gameBoard, GRID_SIZE/2, GRID_SIZE/2);
            else if (ui.buttons.get(4).checkClick(mx, my))
                PatternLibrary.spawnGliderGun(app.gameBoard);
            else if (ui.buttons.get(5).checkClick(mx, my)) {
                int next = (app.gameBoard.getRuleSet() + 1) % RULE_COUNT;
                app.gameBoard.setRuleSet(next);
            }
            else if (ui.buttons.get(6).checkClick(mx, my))
                speedLevel = (speedLevel + 1) % SPEED_LEVELS.length;
            else if (ui.buttons.get(7).checkClick(mx, my)) {
                showHUD = !showHUD;
            }
            else if (ui.buttons.get(8).checkClick(mx, my))
                app.transitionTo(0);
        } else if (my > SimulationUI.topBarHeight()) {
            // Check if alt/shift held for panning, otherwise draw
            if (app.mouseButton == PApplet.RIGHT || app.keyPressed && app.key == ' ') {
                isDragging = true;
                dragStartX = mx; dragStartY = my;
                panStartX = panX; panStartY = panY;
            } else {
                drawWithBrush();
            }
        }
    }

    public void handleMouseDragged(float mx, float my) {
        if (isDragging) {
            panX = panStartX + (mx - dragStartX);
            panY = panStartY + (my - dragStartY);
        } else if (my > SimulationUI.topBarHeight()
                && my < app.height - SimulationUI.bottomBarHeight()) {
            drawWithBrush();
        }
    }

    public void handleMouseReleased() {
        isDragging = false;
    }

    public void handleMouseWheel(float count) {
        float oldZoom = zoomLevel;
        zoomLevel *= (count < 0) ? 1.15f : 0.87f;
        zoomLevel = PApplet.constrain(zoomLevel, 1.0f, 16.0f);
        if (zoomLevel <= 1.01f) { panX = 0; panY = 0; }
        // Zoom toward center
        panX *= zoomLevel / oldZoom;
        panY *= zoomLevel / oldZoom;
    }

    public void handleKeyPressed() {
        if (app.key == ' ') app.isPaused = !app.isPaused;
        if (app.key == 'c' || app.key == 'C') app.gameBoard.clearBoard();
        if (app.key == 'h' || app.key == 'H') showHUD = !showHUD;
        if (app.key == 'm' || app.key == 'M') showHeatmap = !showHeatmap;
        if (app.key == 'r' || app.key == 'R') {
            int next = (app.gameBoard.getRuleSet() + 1) % RULE_COUNT;
            app.gameBoard.setRuleSet(next);
        }
        if (app.key == '+' || app.key == '=')
            speedLevel = Math.min(speedLevel + 1, SPEED_LEVELS.length - 1);
        if (app.key == '-')
            speedLevel = Math.max(speedLevel - 1, 0);
    }

    private void drawWithBrush() {
        int gs = GRID_SIZE;
        int mx2 = SimulationUI.gridMarginX();
        int gw = app.width - mx2 * 2;
        int gh = app.height - SimulationUI.topBarHeight() - SimulationUI.bottomBarHeight();
        float baseCellSize = Math.min((float) gw / gs, (float) gh / gs);
        float cellSize = baseCellSize * zoomLevel;
        float gridW = gs * cellSize;
        float gridH = gs * cellSize;
        float gridX = mx2 + (gw - gridW) / 2f + panX;
        float gridY = SimulationUI.topBarHeight() + (gh - gridH) / 2f + panY;
        int baseC = (int) ((app.mouseX - gridX) / cellSize);
        int baseR = (int) ((app.mouseY - gridY) / cellSize);
        for (int i = 0; i < app.brushSize; i++)
            for (int j = 0; j < app.brushSize; j++)
                if (baseR+i >= 0 && baseR+i < gs && baseC+j >= 0 && baseC+j < gs)
                    app.gameBoard.setCellState(baseR + i, baseC + j, true);
        app.gameBoard.recount();
    }
}
