package game.of.life.on.gpu.ui.controls;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * IterationDial — Custom JavaFX control: a large circular dial with glowing arcs,
 * tick marks, and a draggable value. Renders on a Canvas inside a Region.
 */
public class IterationDial extends Region {

    private final Canvas canvas;
    private double value = 0;        // 0..1 normalized
    private double displayValue = 0; // Smoothed for animation
    private int maxValue;
    private String unit;
    private boolean isDragging = false;
    private double pulsePhase = 0;

    public IterationDial(int maxValue, String unit) {
        this.maxValue = maxValue;
        this.unit = unit;
        this.canvas = new Canvas();
        getChildren().add(canvas);

        // Wire mouse events
        canvas.setOnMousePressed(this::onPress);
        canvas.setOnMouseDragged(this::onDrag);
        canvas.setOnMouseReleased(e -> isDragging = false);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        canvas.setWidth(w);
        canvas.setHeight(h);
        render();
    }

    /** Called each frame by the parent controller. */
    public void update(double dt) {
        double rate = 1.0 - Math.exp(-5.0 * dt);
        displayValue += (value - displayValue) * rate;
        pulsePhase += 1.8 * dt;
        render();
    }

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        double cx = w / 2, cy = h / 2;
        double radius = Math.min(w, h) / 2 - 10;
        double pulse = Math.sin(pulsePhase) * 0.5 + 0.5;

        // Background ring
        gc.setStroke(Color.rgb(255, 255, 255, 0.06));
        gc.setLineWidth(8);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                     90, -360, javafx.scene.shape.ArcType.OPEN);

        // Tick marks (60 ticks)
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(-90 + i * 6);
            double inner = (i % 5 == 0) ? radius - 18 : radius - 10;
            double outer = radius - 4;
            double normTick = (double) i / 60.0;

            if (normTick <= displayValue) {
                gc.setStroke(Color.rgb(0, 255, 224, 0.6 + pulse * 0.24));
            } else {
                gc.setStroke(Color.rgb(255, 255, 255, 0.1));
            }
            gc.setLineWidth(i % 5 == 0 ? 2 : 1);
            gc.strokeLine(
                cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner,
                cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer);
        }

        // Main value arc with glow layers
        double startAngle = 90;
        double extent = -displayValue * 360;

        for (int i = 4; i >= 0; i--) {
            gc.setStroke(Color.rgb(0, 255, 224, (0.03 + pulse * 0.02) - i * 0.008));
            gc.setLineWidth(10 + i * 6);
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                         startAngle, extent, javafx.scene.shape.ArcType.OPEN);
        }
        // Sharp arc on top
        gc.setStroke(Color.rgb(0, 255, 224, 0.86));
        gc.setLineWidth(5);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2,
                     startAngle, extent, javafx.scene.shape.ArcType.OPEN);

        // Leading edge dot
        double edgeAngle = Math.toRadians(-90 + displayValue * 360);
        double dotX = cx + Math.cos(edgeAngle) * radius;
        double dotY = cy + Math.sin(edgeAngle) * radius;
        gc.setFill(Color.rgb(0, 255, 224, 0.16 + pulse * 0.12));
        gc.fillOval(dotX - 11, dotY - 11, 22, 22);
        gc.setFill(Color.rgb(0, 255, 224, 0.78));
        gc.fillOval(dotX - 4, dotY - 4, 8, 8);

        // Inner glow
        gc.setFill(Color.rgb(0, 255, 224, 0.02 + pulse * 0.015));
        gc.fillOval(cx - radius * 0.55, cy - radius * 0.55, radius * 1.1, radius * 1.1);

        // Center value text
        int displayInt = (int) Math.round(displayValue * maxValue);
        gc.setFill(Color.rgb(255, 255, 255, 0.94));
        gc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, radius * 0.35));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(formatNumber(displayInt), cx, cy - radius * 0.06);

        // Unit label
        gc.setFill(Color.rgb(0, 255, 224, 0.63));
        gc.setFont(javafx.scene.text.Font.font("Consolas", radius * 0.12));
        gc.fillText(unit, cx, cy + radius * 0.2);

        // Outer subtle ring
        gc.setStroke(Color.rgb(255, 255, 255, 0.05));
        gc.setLineWidth(1);
        gc.strokeOval(cx - radius * 1.1, cy - radius * 1.1, radius * 2.2, radius * 2.2);
    }

    // ── Mouse interaction ─────────────────────────────────

    private void onPress(MouseEvent e) {
        double cx = canvas.getWidth() / 2;
        double cy = canvas.getHeight() / 2;
        double radius = Math.min(canvas.getWidth(), canvas.getHeight()) / 2 - 10;
        double dist = Math.sqrt(Math.pow(e.getX() - cx, 2) + Math.pow(e.getY() - cy, 2));
        if (dist < radius * 1.2 && dist > radius * 0.3) {
            isDragging = true;
            updateValueFromMouse(e.getX(), e.getY());
        }
    }

    private void onDrag(MouseEvent e) {
        if (isDragging) updateValueFromMouse(e.getX(), e.getY());
    }

    private void updateValueFromMouse(double mx, double my) {
        double cx = canvas.getWidth() / 2;
        double cy = canvas.getHeight() / 2;
        double angle = Math.atan2(my - cy, mx - cx);
        double mapped = angle + Math.PI / 2;
        if (mapped < 0) mapped += Math.PI * 2;
        value = Math.max(0, Math.min(1, mapped / (Math.PI * 2)));
    }

    // ── Public API ────────────────────────────────────────

    public int getValue() { return (int) Math.round(value * maxValue); }

    public void setValue(int v) {
        value = Math.max(0, Math.min(1, (double) v / maxValue));
    }

    public boolean dialIsDragging() { return isDragging; }

    private String formatNumber(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%,d", n);
        return String.valueOf(n);
    }
}
