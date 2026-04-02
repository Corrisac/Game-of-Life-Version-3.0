import processing.core.PApplet;

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
    float scanSpeed = 0.8f;
    
    // Data lines
    String[] dataLines;
    float scrollOffset = 0;
    float scrollSpeed = 0.3f;
    
    public GlassPanel(PApplet p, float x, float y, float w, float h, String title, int accentColor) {
        this.p = p;
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.title = title; this.accentColor = accentColor;
    }
    
    public void setDataLines(String[] lines) {
        this.dataLines = lines;
    }
    
    public void update() {
        scanY += scanSpeed;
        if (scanY > h) scanY = 0;
        scrollOffset += scrollSpeed;
    }
    
    public void display() {
        p.pushStyle();
        
        // ── Frosted glass body ──
        p.noStroke();
        p.fill(255, 255, 255, 8);
        p.rect(x, y, w, h, 14);
        
        // Border
        p.noFill();
        p.stroke(255, 255, 255, 25);
        p.strokeWeight(1);
        p.rect(x, y, w, h, 14);
        
        // ── Top accent bar ──
        p.noStroke();
        p.fill(p.red(accentColor), p.green(accentColor), p.blue(accentColor), 60);
        p.rect(x, y, w, 3, 14, 14, 0, 0);
        
        // ── Title ──
        if (title != null) {
            p.fill(255, 255, 255, 200);
            p.textAlign(PApplet.LEFT, PApplet.TOP);
            p.textSize(13);
            p.text(title, x + 16, y + 14);
            
            // Thin separator
            p.stroke(255, 255, 255, 20);
            p.strokeWeight(1);
            p.line(x + 12, y + 36, x + w - 12, y + 36);
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
    
    public void setPos(float nx, float ny, float nw, float nh) {
        x = nx; y = ny; w = nw; h = nh;
    }
}
