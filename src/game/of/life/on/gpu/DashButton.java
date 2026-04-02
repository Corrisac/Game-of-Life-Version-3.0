import processing.core.PApplet;

/**
 * Futuristic neon-glow button with hover/press animation.
 * Inspired by sci-fi HUDs and PS5 interface cards.
 */
public class DashButton {
    PApplet p;
    float x, y, w, h;
    String label;
    int baseColor;
    
    // Animation state
    float hoverAnim = 0;   // 0..1 lerp for hover glow
    float pressAnim = 0;   // 0..1 lerp for press flash
    boolean isHovered = false;
    boolean wasClicked = false;
    
    public DashButton(PApplet p, float x, float y, float w, float h, String label, int baseColor) {
        this.p = p;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label; this.baseColor = baseColor;
    }
    
    public void update(float mx, float my) {
        isHovered = mx > x && mx < x + w && my > y && my < y + h;
        hoverAnim = PApplet.lerp(hoverAnim, isHovered ? 1 : 0, 0.15f);
        pressAnim *= 0.85f;  // decay press flash
    }
    
    public void display() {
        p.pushMatrix();
        
        // Subtle scale on hover
        float scale = 1f + hoverAnim * 0.03f;
        float cx = x + w / 2f, cy = y + h / 2f;
        p.translate(cx, cy);
        p.scale(scale);
        p.translate(-cx, -cy);
        
        // ── Outer glow bloom ──
        if (hoverAnim > 0.01f) {
            for (int i = 3; i >= 0; i--) {
                p.noFill();
                p.stroke(p.red(baseColor), p.green(baseColor), p.blue(baseColor), (12 + hoverAnim * 18) - i * 5);
                p.strokeWeight(3 + i * 4);
                p.rect(x, y, w, h, h / 2f);
            }
        }
        
        // ── Button body (frosted dark) ──
        float fillAlpha = 20 + hoverAnim * 30 + pressAnim * 80;
        p.fill(p.red(baseColor), p.green(baseColor), p.blue(baseColor), fillAlpha);
        p.stroke(p.red(baseColor), p.green(baseColor), p.blue(baseColor), 80 + hoverAnim * 120);
        p.strokeWeight(1.5f);
        p.rect(x, y, w, h, h / 2f);
        
        // ── Press flash overlay ──
        if (pressAnim > 0.05f) {
            p.fill(255, 255, 255, pressAnim * 60);
            p.noStroke();
            p.rect(x, y, w, h, h / 2f);
        }
        
        // ── Label ──
        p.fill(255, 255, 255, 200 + hoverAnim * 55);
        p.noStroke();
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(PApplet.constrain(h * 0.32f, 11, 18));
        p.text(label, x + w / 2f, y + h / 2f);
        
        p.popMatrix();
    }
    
    public boolean checkClick(float mx, float my) {
        if (mx > x && mx < x + w && my > y && my < y + h) {
            pressAnim = 1;
            wasClicked = true;
            return true;
        }
        return false;
    }
    
    /** Reposition (for responsive layout) */
    public void setPos(float nx, float ny, float nw, float nh) {
        x = nx; y = ny; w = nw; h = nh;
    }
}
