// ============================================================================
//  V4.0 UNIVERSAL RULE SHADER — Configurable via uniforms
// ============================================================================
//  Replaces conway.glsl for the Simulation screen. Instead of hardcoding
//  B3/S23, it reads birth[0..8] and survive[0..8] uniform arrays that
//  specify which neighbor counts trigger birth/survival.
//
//  Usage: Set birthMask[i] = 1.0 for each neighbor count i that causes birth.
//         Set survMask[i] = 1.0 for each neighbor count i that allows survival.
//
//  Example for Conway (B3/S23):
//    birthMask = {0,0,0,1,0,0,0,0,0}
//    survMask  = {0,0,1,1,0,0,0,0,0}
// ============================================================================

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;    // Current generation cell state image
uniform vec2 texOffset;       // Size of one pixel in UV space (1/gridSize)
uniform float birthMask[9];   // Birth mask: birthMask[n]=1.0 if birth at n neighbors
uniform float survMask[9];    // Survive mask: survMask[n]=1.0 if survive at n neighbors

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main() {
    vec2 uv = vertTexCoord.st;
    float dx = texOffset.x;
    float dy = texOffset.y;

    // Count alive neighbors (toroidal wrap via fract)
    // Color convention: black (0.0) = alive, white (1.0) = dead
    float n = 0.0;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx, -dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(0.0, -dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx, -dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx, 0.0))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx, 0.0))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx,  dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(0.0,  dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx,  dy))).r;

    float cell = 1.0 - texture2D(texture, uv).r;  // 1.0 = alive

    // Round neighbor count to integer for array lookup
    int neighbors = int(n + 0.5);

    // Clamp to valid range
    if (neighbors < 0) neighbors = 0;
    if (neighbors > 8) neighbors = 8;

    // Apply configurable rules via uniform arrays
    float survive = cell * survMask[neighbors];
    float birth   = (1.0 - cell) * birthMask[neighbors];
    float next    = max(survive, birth);

    // Output: alive (1.0) → black, dead (0.0) → white
    gl_FragColor = vec4(vec3(1.0 - next), 1.0);
}
