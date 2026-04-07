import processing.core.PApplet;
import processing.core.PGraphics;
/**
 * Frosted-glass panel with optional scanning line, title bar, and scrolling telemetry text.
 */
public class GlassPanel {
    PApplet p;
    float x, y, w, h;
    String title;
    int accentColor;
    
    // Scan line
    float scanY = 0;
    float scanSpeed = 48f;    // V5.0: pixels per second (was 0.8f per frame @ 60fps)
    
    // Data lines
    String[] dataLines;
    float scrollOffset = 0;
    float scrollSpeed = 18f;  // V5.0: pixels per second (was 0.3f per frame @ 60fps)

    // V5.0: PGraphics cache for frosted glass body
    private PGraphics bodyCache;
    private boolean bodyDirty = true;
    
    public GlassPanel(PApplet p, float x, float y, float w, float h, String title, int accentColor) {
        this.p = p;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.title = title; this.accentColor = accentColor;
    }
    
    public void setDataLines(String[] lines) {
        this.dataLines = lines;
    }
    
    /** V5.0: Time-based update for scan line and scroll. */
    public void update(float dt) {
        scanY += scanSpeed * dt;
        if (scanY > h) scanY = 0;
        scrollOffset += scrollSpeed * dt;
    }
    
    public void display() {
        p.pushStyle();

        // V5.0: Draw cached frosted glass body (static layers)
        if (bodyDirty || bodyCache == null) {
            rebuildBodyCache();
        }
        if (bodyCache != null) {
            p.image(bodyCache, x, y);
        }
        
        // ── Scan line effect ──
        float scanWorldY = y + scanY;
        if (scanWorldY > y && scanWorldY < y + h) {
            for (int i = 0; i < 20; i++) {
                float alpha = PApplet.map(i, 0, 20, 25, 0);
                p.stroke(p.red(accentColor), p.green(accentColor), p.blue(accentColor), alpha);
                p.strokeWeight(1);
                p.line(x + 4, scanWorldY - i, x + w - 4, scanWorldY - i);
            }
        }
        
        // ── Data lines ──
        if (dataLines != null) {
            p.textAlign(PApplet.LEFT, PApplet.TOP);
            p.textSize(11);
            float lineH = 18;
            float startY = y + 44;
            float visibleH = h - 56;
            
            // Clip manually
            for (int i = 0; i < dataLines.length; i++) {
                float ly = startY + i * lineH - (scrollOffset % (dataLines.length * lineH));
                if (ly < startY - lineH) ly += dataLines.length * lineH;
                
                if (ly >= startY - 2 && ly < y + h - 12) {
                    // Fade at edges
                    float fadeAlpha = 1;
                    if (ly < startY + 10) fadeAlpha = (ly - startY + 2) / 12f;
                    if (ly > y + h - 30) fadeAlpha = (y + h - 12 - ly) / 18f;
                    fadeAlpha = PApplet.constrain(fadeAlpha, 0, 1);
                    
                    p.noStroke();
                    p.fill(200, 210, 220, fadeAlpha * 140);
                    p.text(dataLines[i], x + 16, ly);
                }
            }
        }
        
        p.popStyle();
    }
    
    /** V5.0: Invalidates body cache on resize. */
    public void setPos(float nx, float ny, float nw, float nh) {
        if (nw != w || nh != h) bodyDirty = true;
        x = nx; y = ny; w = nw; h = nh;
    }

    /**
     * V5.0: Pre-renders the frosted glass body, border, accent bar, and title
     * into an off-screen PGraphics buffer.
     */
    private void rebuildBodyCache() {
        int cw = (int) w;
        int ch = (int) h;
        if (cw <= 0 || ch <= 0) return;

        if (bodyCache == null || bodyCache.width != cw || bodyCache.height != ch) {
            bodyCache = p.createGraphics(cw, ch);
        }
        bodyCache.beginDraw();
        bodyCache.clear();

        // Frosted glass body
        bodyCache.noStroke();
        bodyCache.fill(255, 255, 255, 8);
        bodyCache.rect(0, 0, cw, ch, 14);

        // Border
        bodyCache.noFill();
        bodyCache.stroke(255, 255, 255, 25);
        bodyCache.strokeWeight(1);
        bodyCache.rect(0, 0, cw, ch, 14);

        // Top accent bar
        bodyCache.noStroke();
        bodyCache.fill(p.red(accentColor), p.green(accentColor), p.blue(accentColor), 60);
        bodyCache.rect(0, 0, cw, 3, 14, 14, 0, 0);

        // Title
        if (title != null) {
            bodyCache.fill(255, 255, 255, 200);
            bodyCache.textAlign(PApplet.LEFT, PApplet.TOP);
            bodyCache.textSize(13);
            bodyCache.text(title, 16, 14);

            bodyCache.stroke(255, 255, 255, 20);
            bodyCache.strokeWeight(1);
            bodyCache.line(12, 36, cw - 12, 36);
        }

        bodyCache.endDraw();
        bodyDirty = false;
    }
}
