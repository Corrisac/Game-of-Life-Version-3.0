import processing.core.PApplet;
import processing.core.PGraphics;

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

    // V5.0: PGraphics glow cache (eliminates per-frame bloom loops)
    private PGraphics glowCache;
    private boolean glowDirty = true; // True when cache needs rebuild
    
    public DashButton(PApplet p, float x, float y, float w, float h, String label, int baseColor) {
        this.p = p;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label; this.baseColor = baseColor;
    }
    
    /** V5.0: Time-based update — animations run at consistent speed regardless of framerate. */
    public void update(float mx, float my, float dt) {
        isHovered = mx > x && mx < x + w && my > y && my < y + h;
        // Exponential decay lerp: target convergence rate is independent of frame rate
        float hoverRate = 1.0f - (float) Math.exp(-12.0 * dt);  // ~0.18 at 60fps, ~0.33 at 30fps
        hoverAnim += (( isHovered ? 1f : 0f) - hoverAnim) * hoverRate;
        pressAnim *= (float) Math.exp(-10.0 * dt);  // ~0.85 at 60fps, ~0.72 at 30fps
    }
    
    public void display() {
        p.pushMatrix();

        // Subtle scale on hover
        float scale = 1f + hoverAnim * 0.03f;
        float cx = x + w / 2f, cy = y + h / 2f;
        p.translate(cx, cy);
        p.scale(scale);
        p.translate(-cx, -cy);

        // ── Outer glow bloom (V5.0: cached into PGraphics) ──
        if (hoverAnim > 0.01f) {
            if (glowDirty || glowCache == null) {
                rebuildGlowCache();
            }
            p.pushStyle();
            p.imageMode(PApplet.CORNER);
            p.tint(255, 255, 255, (int)(hoverAnim * 255));
            p.image(glowCache, x - 16, y - 16);
            p.noTint();
            p.popStyle();
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
        p.textSize(PApplet.constrain(h * 0.45f, 16, 28));
        p.text(label, x + w / 2f, y + h / 2f);

        p.popMatrix();
    }

    /**
     * V5.0: Pre-renders the glow bloom layers into an off-screen PGraphics buffer.
     * This eliminates the per-frame for-loop that drew 4 overlapping rectangles.
     */
    private void rebuildGlowCache() {
        int cacheW = (int)(w + 32);
        int cacheH = (int)(h + 32);
        if (cacheW <= 0 || cacheH <= 0) return;

        if (glowCache == null || glowCache.width != cacheW || glowCache.height != cacheH) {
            glowCache = p.createGraphics(cacheW, cacheH);
        }
        glowCache.beginDraw();
        glowCache.clear();
        glowCache.noFill();
        float lx = 16, ly = 16; // Local offset within cache
        for (int i = 3; i >= 0; i--) {
            glowCache.stroke(p.red(baseColor), p.green(baseColor), p.blue(baseColor), 30 - i * 5);
            glowCache.strokeWeight(3 + i * 4);
            glowCache.rect(lx, ly, w, h, h / 2f);
        }
        glowCache.endDraw();
        glowDirty = false;
    }
    
    public boolean checkClick(float mx, float my) {
        if (mx > x && mx < x + w && my > y && my < y + h) {
            pressAnim = 1;
            wasClicked = true;
            return true;
        }
        return false;
    }
    
    /** Reposition (for responsive layout). V5.0: Invalidates glow cache on resize. */
    public void setPos(float nx, float ny, float nw, float nh) {
        if (nw != w || nh != h) glowDirty = true; // Only invalidate if size changed
        x = nx; y = ny; w = nw; h = nh;
    }
}
