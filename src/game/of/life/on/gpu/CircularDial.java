import processing.core.PApplet;

/**
 * A large circular dial with glowing arcs, tick marks, and a draggable value.
 * The centrepiece of the futuristic dashboard.
 */
public class CircularDial {
    PApplet p;
    float cx, cy, radius;
    
    float value = 0;       // 0..1 normalised
    float displayValue = 0; // smoothed for animation
    int maxValue;
    String unit;
    
    boolean isDragging = false;
    float pulsePhase = 0;
    
    // Colors
    static final int CYAN  = 0xFF00FFE0;
    static final int MAGENTA = 0xFFFF00AA;
    
    public CircularDial(PApplet p, float cx, float cy, float radius, int maxValue, String unit) {
        this.p = p;
        this.cx = cx; this.cy = cy; this.radius = radius;
        this.maxValue = maxValue;
        this.unit = unit;
    }
    
    public void update() {
        displayValue = PApplet.lerp(displayValue, value, 0.08f);
        pulsePhase += 0.03f;
    }
    
    public void display() {
        p.pushStyle();
        float pulse = PApplet.sin(pulsePhase) * 0.5f + 0.5f;
        
        // ── Background ring ──
        p.noFill();
        p.stroke(255, 255, 255, 15);
        p.strokeWeight(8);
        p.arc(cx, cy, radius * 2, radius * 2, -PApplet.HALF_PI, -PApplet.HALF_PI + PApplet.TWO_PI);
        
        // ── Tick marks (60 ticks) ──
        for (int i = 0; i < 60; i++) {
            float angle = PApplet.map(i, 0, 60, -PApplet.HALF_PI, -PApplet.HALF_PI + PApplet.TWO_PI);
            float inner = (i % 5 == 0) ? radius - 18 : radius - 10;
            float outer = radius - 4;
            float normTick = (float)i / 60f;
            
            if (normTick <= displayValue) {
                p.stroke(0, 255, 224, 150 + pulse * 60);
            } else {
                p.stroke(255, 255, 255, 25);
            }
            p.strokeWeight(i % 5 == 0 ? 2 : 1);
            p.line(
                cx + PApplet.cos(angle) * inner, cy + PApplet.sin(angle) * inner,
                cx + PApplet.cos(angle) * outer, cy + PApplet.sin(angle) * outer
            );
        }
        
        // ── Main arc (value) with multi-layer glow ──
        float startAngle = -PApplet.HALF_PI;
        float endAngle = startAngle + displayValue * PApplet.TWO_PI;
        
        // Glow layers (bloom effect)
        for (int i = 4; i >= 0; i--) {
            p.noFill();
            p.stroke(0, 255, 224, (8 + pulse * 6) - i * 2);
            p.strokeWeight(10 + i * 6);
            p.arc(cx, cy, radius * 2, radius * 2, startAngle, endAngle);
        }
        
        // Sharp arc on top
        p.stroke(0, 255, 224, 220);
        p.strokeWeight(5);
        p.arc(cx, cy, radius * 2, radius * 2, startAngle, endAngle);
        
        // ── Leading edge dot ──
        float edgeAngle = endAngle;
        float dotX = cx + PApplet.cos(edgeAngle) * radius;
        float dotY = cy + PApplet.sin(edgeAngle) * radius;
        p.noStroke();
        p.fill(0, 255, 224, 40 + pulse * 30);
        p.ellipse(dotX, dotY, 22, 22);
        p.fill(0, 255, 224, 200);
        p.ellipse(dotX, dotY, 8, 8);
        
        // ── Center display ──
        int displayInt = Math.round(displayValue * maxValue);
        
        // Inner glow circle
        p.noStroke();
        p.fill(0, 255, 224, 6 + pulse * 4);
        p.ellipse(cx, cy, radius * 1.1f, radius * 1.1f);
        
        // Value text
        p.fill(255, 255, 255, 240);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(radius * 0.38f);
        p.text(formatNumber(displayInt), cx, cy - radius * 0.06f);
        
        // Unit label
        p.fill(0, 255, 224, 160);
        p.textSize(radius * 0.12f);
        p.text(unit, cx, cy + radius * 0.2f);
        
        // Outer subtle ring
        p.noFill();
        p.stroke(255, 255, 255, 12);
        p.strokeWeight(1);
        p.ellipse(cx, cy, radius * 2.2f, radius * 2.2f);
        
        p.popStyle();
    }
    
    public boolean checkPress(float mx, float my) {
        float d = PApplet.dist(mx, my, cx, cy);
        if (d < radius * 1.2f && d > radius * 0.3f) {
            isDragging = true;
            updateValueFromMouse(mx, my);
            return true;
        }
        return false;
    }
    
    public void drag(float mx, float my) {
        if (isDragging) updateValueFromMouse(mx, my);
    }
    
    public void release() { isDragging = false; }
    
    private void updateValueFromMouse(float mx, float my) {
        float angle = PApplet.atan2(my - cy, mx - cx);
        // Map from -PI..PI to 0..TWO_PI, starting from top (-HALF_PI)
        float mapped = angle + PApplet.HALF_PI;
        if (mapped < 0) mapped += PApplet.TWO_PI;
        value = PApplet.constrain(mapped / PApplet.TWO_PI, 0, 1);
    }
    
    public int getValue() { return Math.round(value * maxValue); }
    public void setValue(int v) { value = PApplet.constrain((float)v / maxValue, 0, 1); }
    
    public void setPos(float ncx, float ncy, float nr) {
        cx = ncx; cy = ncy; radius = nr;
    }
    
    private String formatNumber(int n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000f);
        if (n >= 1000) return String.format("%,d", n);
        return String.valueOf(n);
    }
}
