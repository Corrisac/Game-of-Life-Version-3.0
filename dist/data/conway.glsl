// ============================================================================
//  CONWAY'S GAME OF LIFE — UNIFIED GPU SHADER
// ============================================================================
//  Two modes selected via preprocessor:
//
//    DEFAULT (no defines):  SINGLE-STEP MODE
//      Computes 1 generation per pass. Used by live simulation (Processing API).
//      Branchless, float-only, toroidal wrapping.
//
//    #define MULTI_STEP_K4:  K=4 MULTI-STEP MODE
//      Computes 4 generations per pass. Used by GPU Lab (raw JOGL loop).
//      Reads 9x9 neighborhood (81 fetches) and cascades through
//      7x7 → 5x5 → 3x3 → center.  Cuts render passes by 4x.
//
//  COLOR CONVENTION:  black (0.0) = alive,  white (1.0) = dead
//  WRAPPING:          toroidal via fract() on all texture lookups
// ============================================================================

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

// Uniforms passed automatically by Processing / set manually by raw JOGL:
uniform sampler2D texture;  // Current generation cell state image
uniform vec2 texOffset;     // Size of one pixel in UV space (1.0 / gridSize)

// Varyings from the vertex shader:
varying vec4 vertColor;
varying vec4 vertTexCoord;


// ════════════════════════════════════════════════════════════════════════════
#ifdef MULTI_STEP_K4
// ════════════════════════════════════════════════════════════════════════════
//  K=4 MULTI-STEP MODE — 4 generations per fragment shader pass
//
//  Dependency radius grows by 1 each generation (1→3x3 ... 4→9x9).
//  Step 0: Read 9x9 from texture → g0[81]
//  Step 1: Conway on 7x7 using g0 → g1[49]
//  Step 2: Conway on 5x5 using g1 → g2[25]
//  Step 3: Conway on 3x3 using g2 → g3[9]
//  Step 4: Conway on center using g3 → result
// ════════════════════════════════════════════════════════════════════════════

// Branchless Conway rule function
float conway(float cell, float neighbors) {
    float survive = cell * step(2.0, neighbors) * step(neighbors, 3.0);
    float birth   = (1.0 - cell) * step(3.0, neighbors) * step(neighbors, 3.0);
    return max(survive, birth);
}

void main() {
    vec2 uv = vertTexCoord.st;
    float dx = texOffset.x;
    float dy = texOffset.y;

    // Step 0: Read 9x9 neighborhood (81 texel fetches)
    float g0[81];
    for (int row = -4; row <= 4; row++) {
        for (int col = -4; col <= 4; col++) {
            vec2 offset = vec2(float(col) * dx, float(row) * dy);
            float texVal = texture2D(texture, fract(uv + offset)).r;
            g0[(row + 4) * 9 + (col + 4)] = 1.0 - texVal;
        }
    }

    // Step 1: Gen 1 — Conway on 7x7 inner cells
    float g1[49];
    for (int r = 1; r <= 7; r++) {
        for (int c = 1; c <= 7; c++) {
            float cell = g0[r * 9 + c];
            float n = g0[(r-1)*9+(c-1)] + g0[(r-1)*9+c] + g0[(r-1)*9+(c+1)]
                    + g0[ r   *9+(c-1)]                  + g0[ r   *9+(c+1)]
                    + g0[(r+1)*9+(c-1)] + g0[(r+1)*9+c] + g0[(r+1)*9+(c+1)];
            g1[(r - 1) * 7 + (c - 1)] = conway(cell, n);
        }
    }

    // Step 2: Gen 2 — Conway on 5x5 inner cells
    float g2[25];
    for (int r = 1; r <= 5; r++) {
        for (int c = 1; c <= 5; c++) {
            float cell = g1[r * 7 + c];
            float n = g1[(r-1)*7+(c-1)] + g1[(r-1)*7+c] + g1[(r-1)*7+(c+1)]
                    + g1[ r   *7+(c-1)]                  + g1[ r   *7+(c+1)]
                    + g1[(r+1)*7+(c-1)] + g1[(r+1)*7+c] + g1[(r+1)*7+(c+1)];
            g2[(r - 1) * 5 + (c - 1)] = conway(cell, n);
        }
    }

    // Step 3: Gen 3 — Conway on 3x3 inner cells
    float g3[9];
    for (int r = 1; r <= 3; r++) {
        for (int c = 1; c <= 3; c++) {
            float cell = g2[r * 5 + c];
            float n = g2[(r-1)*5+(c-1)] + g2[(r-1)*5+c] + g2[(r-1)*5+(c+1)]
                    + g2[ r   *5+(c-1)]                  + g2[ r   *5+(c+1)]
                    + g2[(r+1)*5+(c-1)] + g2[(r+1)*5+c] + g2[(r+1)*5+(c+1)];
            g3[(r - 1) * 3 + (c - 1)] = conway(cell, n);
        }
    }

    // Step 4: Gen 4 — Conway on center cell
    float centerCell = g3[4];
    float neighbors  = g3[0] + g3[1] + g3[2]
                      + g3[3]          + g3[5]
                      + g3[6] + g3[7] + g3[8];
    float result = conway(centerCell, neighbors);

    gl_FragColor = vec4(vec3(1.0 - result), 1.0);
}


// ════════════════════════════════════════════════════════════════════════════
#else
// ════════════════════════════════════════════════════════════════════════════
//  SINGLE-STEP MODE — 1 generation per pass (branchless, float-only)
//
//  Used by real-time live simulation via Processing's shader API.
//  Conway's 4 rules collapsed into two branchless expressions:
//    SURVIVE: alive AND neighbors ∈ {2, 3}
//    BIRTH:   dead  AND neighbors == 3
// ════════════════════════════════════════════════════════════════════════════

void main() {
    vec2 uv = vertTexCoord.st;
    float dx = texOffset.x;
    float dy = texOffset.y;

    // Count 8 neighbors (toroidal wrap via fract)
    float n = 0.0;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx, -dy))).r;  // top-left
    n += 1.0 - texture2D(texture, fract(uv + vec2(0.0, -dy))).r;  // top
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx, -dy))).r;  // top-right
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx, 0.0))).r;  // left
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx, 0.0))).r;  // right
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx,  dy))).r;  // bottom-left
    n += 1.0 - texture2D(texture, fract(uv + vec2(0.0,  dy))).r;  // bottom
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx,  dy))).r;  // bottom-right

    float cell = 1.0 - texture2D(texture, uv).r;  // 1.0 = alive, 0.0 = dead

    // Branchless Conway rules
    float survive = cell * step(2.0, n) * step(n, 3.0);
    float birth   = (1.0 - cell) * step(3.0, n) * step(n, 3.0);
    float next    = max(survive, birth);

    gl_FragColor = vec4(vec3(1.0 - next), 1.0);
}

#endif
