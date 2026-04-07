// ============================================================================
//  V4.0 HEATMAP RENDER SHADER — Cell-age colormapping
// ============================================================================
//  Takes two textures:
//    1. Cell state texture (black = alive, white = dead)
//    2. Cell age texture   (red channel = normalized age, 0.0 to 1.0)
//
//  Outputs a color-mapped pixel:
//    Dead cells   → dark background (near black)
//    Age 1 (born) → bright cyan flash
//    Age 2-5      → cool blue
//    Age 6-15     → cyan to magenta transition
//    Age 16-40    → magenta to red
//    Age 40+      → white-hot
//
//  When heatmap is disabled (useHeatmap = 0.0), renders classic cyan/dark.
// ============================================================================

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;      // Cell state: black=alive, white=dead
uniform sampler2D ageTexture;   // Cell age: red channel = age/255.0
uniform float useHeatmap;       // 1.0 = heatmap mode, 0.0 = classic mode
uniform vec2 texOffset;         // Pixel size in UV space

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main() {
    vec2 uv = vertTexCoord.st;
    float cellState = 1.0 - texture2D(texture, uv).r;  // 1.0 = alive
    float age = texture2D(ageTexture, uv).r * 255.0;    // Denormalize to 0-255

    if (cellState < 0.5) {
        // Dead cell — dark background
        gl_FragColor = vec4(0.07, 0.09, 0.15, 1.0);
        return;
    }

    if (useHeatmap < 0.5) {
        // Classic mode: alive = cyan
        gl_FragColor = vec4(0.0, 1.0, 0.88, 1.0);
        return;
    }

    // Heatmap mode: color by age
    vec3 color;
    if (age <= 1.0) {
        // Just born — bright cyan flash
        color = vec3(0.0, 1.0, 1.0);
    } else if (age <= 5.0) {
        // Cool blue
        float t = (age - 1.0) / 4.0;
        color = mix(vec3(0.12, 0.56, 1.0), vec3(0.0, 1.0, 0.88), t);
    } else if (age <= 15.0) {
        // Cyan to magenta
        float t = (age - 5.0) / 10.0;
        color = mix(vec3(0.0, 1.0, 0.88), vec3(1.0, 0.0, 0.67), t);
    } else if (age <= 40.0) {
        // Magenta to red
        float t = (age - 15.0) / 25.0;
        color = mix(vec3(1.0, 0.0, 0.67), vec3(1.0, 0.28, 0.34), t);
    } else {
        // White-hot ancient cells
        float t = min((age - 40.0) / 30.0, 1.0);
        color = mix(vec3(1.0, 0.28, 0.34), vec3(1.0, 1.0, 1.0), t);
    }

    gl_FragColor = vec4(color, 1.0);
}
