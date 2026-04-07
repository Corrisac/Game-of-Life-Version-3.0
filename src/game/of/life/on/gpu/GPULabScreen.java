import processing.core.PApplet;

/**
 * GPULabScreen V4.0 — Uses fast pixel-buffer rendering. Shows rule set info.
 */
public class GPULabScreen implements ThemeConstants {

    private Main app;
    private GPULabUI ui;
    boolean isTypingIterations = false;
    String iterationInput = "";

    public GPULabScreen(Main app) {
        this.app = app;
        this.ui = new GPULabUI(app);
    }

    public int getIterationCount() { return ui.dial.getValue(); }

    public void draw(float dt) {
        GridRenderer.drawBackground(app);

        int sideW = GPULabUI.sidebarWidth();
        int halfW = (app.width - sideW) / 2;
        GPUCompute gpu = app.getGpuCompute();

        // --- Left half: editable grid (fast pixel buffer) ---
        int gridDisplaySize = Math.min(halfW - 20, app.height - 60);
        int offsetX = sideW + (halfW - gridDisplaySize) / 2;
        int offsetY = GPULabUI.topBarHeight() + 6 + (app.height - 60 - gridDisplaySize) / 2;
        GridRenderer.drawGridAdvanced(app, app.gameBoard,
            offsetX, offsetY, gridDisplaySize, gridDisplaySize, 1.0f, false);

        // --- Right half: GPU result / progress / placeholder ---
        int rightX = sideW + halfW;

        if (gpu.isComputing) {
            app.noStroke(); app.fill(10, 15, 25);
            app.rect(rightX, 0, halfW, app.height);
            float pct = (float) gpu.computeProgress / ui.dial.getValue();
            app.fill(255, 255, 255, 200);
            app.textAlign(PApplet.CENTER, PApplet.CENTER);
            app.textSize(20);
            app.text(GPULabUI.COMPUTING_LABEL, rightX + halfW / 2f, app.height / 2f - 45);

            float barM = GPULabUI.PROGRESS_BAR_MARGIN;
            float barH = GPULabUI.PROGRESS_BAR_HEIGHT;
            app.fill(30, 38, 50);
            app.rect(rightX + barM, app.height/2f - barH/2, halfW - barM*2, barH, 8);
            for (int i = 2; i >= 0; i--) {
                app.fill(0, 255, 224, 20 - i * 6);
                app.rect(rightX + barM, app.height/2f - barH/2 - i*2,
                         (halfW - barM*2) * pct, barH + i*4, 8);
            }
            app.fill(0, 255, 224);
            app.rect(rightX + barM, app.height/2f - barH/2, (halfW - barM*2) * pct, barH, 8);

            app.fill(0, 255, 224, 180);
            app.textSize(18);
            app.text(String.format("%,d / %,d  (%.0f%%)",
                     gpu.computeProgress, ui.dial.getValue(), pct * 100),
                     rightX + halfW / 2f, app.height / 2f + 30);

        } else if (gpu.hasComputed) {
            app.image(gpu.getResultBuffer(), rightX, GPULabUI.topBarHeight(),
                      halfW, app.height - GPULabUI.topBarHeight());
            app.noStroke(); app.fill(0, 0, 0, 150);
            app.rect(rightX, GPULabUI.topBarHeight(), halfW, 36);
            app.fill(0, 255, 224, 220);
            app.textAlign(PApplet.CENTER, PApplet.CENTER);
            app.textSize(16);
            app.text("\u2713  " + gpu.computeTime + "ms  \u00b7  "
                     + String.format("%,d", ui.dial.getValue()) + " GEN  \u00b7  CONWAY",
                     rightX + halfW / 2f, 62);

        } else {
            app.noStroke(); app.fill(10, 15, 25);
            app.rect(rightX, GPULabUI.topBarHeight(), halfW,
                     app.height - GPULabUI.topBarHeight());
            app.fill(255, 255, 255, 60);
            app.textAlign(PApplet.CENTER, PApplet.CENTER);
            app.textSize(18);
            app.text(GPULabUI.PLACEHOLDER_TITLE, rightX + halfW/2f, app.height/2f - 15);
            app.fill(255, 255, 255, 30);
            app.textSize(16);
            app.text(GPULabUI.PLACEHOLDER_SUBTITLE, rightX + halfW/2f, app.height/2f + 12);
        }

        // Divider glow
        for (int i = 2; i >= 0; i--) {
            app.stroke(0, 255, 224, 15 - i * 4);
            app.strokeWeight(1 + i * 2);
            app.line(rightX, GPULabUI.topBarHeight(), rightX, app.height);
        }

        // Sidebar
        app.noStroke(); app.fill(10, 15, 25, 235);
        app.rect(0, 0, sideW, app.height);
        app.stroke(255, 255, 255, 18); app.strokeWeight(1);
        app.line(sideW, 0, sideW, app.height);

        app.noStroke();
        app.fill(255, 255, 255, 220);
        app.textAlign(PApplet.CENTER, PApplet.TOP);
        app.textSize(18);
        app.text(GPULabUI.SCREEN_TITLE, sideW / 2f, 16);
        app.stroke(0, 255, 224, 50); app.strokeWeight(1);
        app.line(20, GPULabUI.topBarHeight(), sideW - 20, GPULabUI.topBarHeight());

        ui.layoutComponents(app);
        ui.dial.update(dt);
        ui.dial.display();

        // Input field
        float inputY = ui.getInputY();
        float inputW = ui.getInputWidth();
        float inputH = GPULabUI.inputHeight();
        float inputX = GPULabUI.inputMargin();

        app.fill(255, 255, 255, 80);
        app.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        app.textSize(14);
        app.text(GPULabUI.INPUT_LABEL, sideW / 2f, inputY - 3);

        if (isTypingIterations) {
            app.stroke(0, 255, 224, 180); app.strokeWeight(1.5f);
            app.fill(0, 255, 224, 12);
        } else {
            app.stroke(255, 255, 255, 30); app.strokeWeight(1);
            app.fill(255, 255, 255, 8);
        }
        app.rect(inputX, inputY, inputW, inputH, GPULabUI.INPUT_CORNER_RADIUS);
        app.noStroke();

        app.fill(255, 255, 255, 220);
        app.textAlign(PApplet.CENTER, PApplet.CENTER);
        app.textSize(GPULabUI.inputFontSize());
        if (isTypingIterations) {
            String cursor = (app.frameCount % 60 < 30) ? "|" : "";
            app.text(iterationInput + cursor, sideW / 2f, inputY + inputH / 2f);
        } else {
            app.text(String.format("%,d", ui.dial.getValue()), sideW / 2f, inputY + inputH / 2f);
        }

        for (int i = 0; i < ui.buttons.size(); i++) {
            ui.buttons.get(i).update(app.mouseX, app.mouseY, dt);
            ui.buttons.get(i).display();
        }

        // Top bar
        app.noStroke(); app.fill(10, 15, 25, 220);
        app.rect(sideW, 0, app.width - sideW, GPULabUI.topBarHeight());
        app.stroke(0, 255, 224, 30); app.strokeWeight(1);
        app.line(sideW, GPULabUI.topBarHeight(), app.width, GPULabUI.topBarHeight());
        app.fill(255, 255, 255, 120);
        app.textAlign(PApplet.LEFT, PApplet.CENTER); app.textSize(16);
        app.text(GPULabUI.LEFT_PANE_LABEL, sideW + 14, 22);
        app.textAlign(PApplet.RIGHT, PApplet.CENTER);
        app.text(GPULabUI.RIGHT_PANE_LABEL, app.width - 14, 22);
    }

    public void handleMousePressed(float mx, float my) {
        int sideW = GPULabUI.sidebarWidth();
        float inputY = ui.getInputY();
        float inputH = GPULabUI.inputHeight();
        float inputM = GPULabUI.inputMargin();

        if (isTypingIterations) {
            boolean inBox = mx > inputM && mx < sideW - inputM
                         && my > inputY && my < inputY + inputH;
            if (!inBox) {
                isTypingIterations = false;
                if (iterationInput.length() > 0) {
                    int val = Math.min(Integer.parseInt(iterationInput), GPULabUI.DIAL_MAX);
                    ui.dial.setValue(val);
                }
            }
        }

        if (mx < sideW) {
            if (my > inputY && my < inputY + inputH && mx > inputM && mx < sideW - inputM) {
                isTypingIterations = true; iterationInput = "";
            }
            else if (ui.dial.checkPress(mx, my)) { /* dial drag */ }
            else if (ui.buttons.get(0).checkClick(mx, my) && !app.getGpuCompute().isComputing)
                app.getGpuCompute().startCompute(app.gameBoard, ui.dial.getValue());
            else if (ui.buttons.get(1).checkClick(mx, my)) {
                app.gameBoard.clearBoard(); app.getGpuCompute().hasComputed = false;
            }
            else if (ui.buttons.get(2).checkClick(mx, my)) {
                PatternLibrary.spawnGlider(app.gameBoard, GRID_SIZE/2, GRID_SIZE/2);
                app.getGpuCompute().hasComputed = false;
            }
            else if (ui.buttons.get(3).checkClick(mx, my))
                PatternLibrary.spawnGliderGun(app.gameBoard);
            else if (ui.buttons.get(4).checkClick(mx, my))
                app.transitionTo(0);
        }
        else if (mx < sideW + (app.width - sideW) / 2 && my > GPULabUI.topBarHeight()) {
            drawWithBrush();
        }
    }

    public void handleMouseDragged(float mx, float my) {
        if (ui.dial.isDragging) { ui.dial.drag(mx, my); return; }
        int sideW = GPULabUI.sidebarWidth();
        if (mx > sideW && mx < sideW + (app.width - sideW) / 2
            && my > GPULabUI.topBarHeight()) {
            drawWithBrush();
            app.getGpuCompute().hasComputed = false;
        }
    }

    public void handleMouseReleased() { ui.dial.release(); }

    public void handleKeyPressed() {
        if (!isTypingIterations) return;
        if (app.key >= '0' && app.key <= '9'
            && iterationInput.length() < GPULabUI.INPUT_MAX_DIGITS)
            iterationInput += app.key;
        else if (app.key == PApplet.BACKSPACE && iterationInput.length() > 0)
            iterationInput = iterationInput.substring(0, iterationInput.length() - 1);
        else if (app.key == PApplet.ENTER || app.key == PApplet.RETURN) {
            isTypingIterations = false;
            if (iterationInput.length() > 0) {
                int val = Math.min(Integer.parseInt(iterationInput), GPULabUI.DIAL_MAX);
                ui.dial.setValue(val);
            }
        }
    }

    private void drawWithBrush() {
        int gs = GRID_SIZE;
        int sideW = GPULabUI.sidebarWidth();
        int halfW = (app.width - sideW) / 2;
        int gridDisplaySize = Math.min(halfW - 20, app.height - 60);
        int offsetX = sideW + (halfW - gridDisplaySize) / 2;
        int offsetY = GPULabUI.topBarHeight() + 6 + (app.height - 60 - gridDisplaySize) / 2;
        float cellSize = (float) gridDisplaySize / gs;
        int baseC = (int) ((app.mouseX - offsetX) / cellSize);
        int baseR = (int) ((app.mouseY - offsetY) / cellSize);
        for (int i = 0; i < app.brushSize; i++)
            for (int j = 0; j < app.brushSize; j++)
                if (baseR+i >= 0 && baseR+i < gs && baseC+j >= 0 && baseC+j < gs)
                    app.gameBoard.setCellState(baseR + i, baseC + j, true);
    }
}
