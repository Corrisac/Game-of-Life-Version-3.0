#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform sampler2D texture;
uniform vec2 texOffset;

varying vec4 vertColor;
varying vec4 vertTexCoord;


#ifdef MULTI_STEP_K4

float conway(float cell, float neighbors) {
    float survive = cell * step(2.0, neighbors) * step(neighbors, 3.0);
    float birth   = (1.0 - cell) * step(3.0, neighbors) * step(neighbors, 3.0);
    return max(survive, birth);
}

void main() {
    vec2 uv = vertTexCoord.st;
    float dx = texOffset.x;
    float dy = texOffset.y;

    float g0[81];
    for (int row = -4; row <= 4; row++) {
        for (int col = -4; col <= 4; col++) {
            vec2 offset = vec2(float(col) * dx, float(row) * dy);
            float texVal = texture2D(texture, fract(uv + offset)).r;
            g0[(row + 4) * 9 + (col + 4)] = 1.0 - texVal;
        }
    }

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

    float centerCell = g3[4];
    float neighbors  = g3[0] + g3[1] + g3[2]
                      + g3[3]          + g3[5]
                      + g3[6] + g3[7] + g3[8];
    float result = conway(centerCell, neighbors);

    gl_FragColor = vec4(vec3(1.0 - result), 1.0);
}


#else

void main() {
    vec2 uv = vertTexCoord.st;
    float dx = texOffset.x;
    float dy = texOffset.y;

    float n = 0.0;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx, -dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(0.0, -dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx, -dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx, 0.0))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx, 0.0))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(-dx,  dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2(0.0,  dy))).r;
    n += 1.0 - texture2D(texture, fract(uv + vec2( dx,  dy))).r;

    float cell = 1.0 - texture2D(texture, uv).r;

    float survive = cell * step(2.0, n) * step(n, 3.0);
    float birth   = (1.0 - cell) * step(3.0, n) * step(n, 3.0);
    float next    = max(survive, birth);

    gl_FragColor = vec4(vec3(1.0 - next), 1.0);
}

#endif
