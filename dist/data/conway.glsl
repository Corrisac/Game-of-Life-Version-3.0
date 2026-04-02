#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

// Processing automatically passes these variables to the shader
uniform sampler2D texture; // The image of the current generation
uniform vec2 texOffset;    // The mathematical size of a single pixel (1.0/width)

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main() {
    // uv represents the exact (x, y) coordinate of the current pixel being processed
    vec2 uv = vertTexCoord.st;

    // Calculate the coordinates of the 8 neighbors based on the size of one pixel
    vec2 up = vec2(0.0, -texOffset.y);
    vec2 down = vec2(0.0, texOffset.y);
    vec2 left = vec2(-texOffset.x, 0.0);
    vec2 right = vec2(texOffset.x, 0.0);

    // Count live neighbors
    // In our system: White (Dead) has a color value of 1.0. Black (Alive) has a color value of 0.0.
    // By subtracting the color from 1.0, we get a 1 for Alive and a 0 for Dead.
    int aliveNeighbors = 0;
    
    aliveNeighbors += int(1.0 - texture2D(texture, uv + up + left).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + up).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + up + right).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + left).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + right).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + down + left).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + down).r);
    aliveNeighbors += int(1.0 - texture2D(texture, uv + down + right).r);

    // Get the current cell's state
    int currentState = int(1.0 - texture2D(texture, uv).r);
    int nextState = 0;

    // The 4 Rules of Life
    if (currentState == 1) {
        if (aliveNeighbors == 2 || aliveNeighbors == 3) {
            nextState = 1; // Survival
        }
    } else {
        if (aliveNeighbors == 3) {
            nextState = 1; // Reproduction
        }
    }

    // Output the final color for this pixel to the screen (or memory buffer)
    if (nextState == 1) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0); // Output Black (Alive)
    } else {
        gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0); // Output White (Dead)
    }
}
