import processing.core.PApplet; // Import the Processing core library for drawing primitives and math utilities

/**
 * ParticleSystem — Manages and renders a collection of floating ambient particles
 * that create a living, breathing atmosphere on the dashboard menu screen.
 * Each particle drifts slowly upward with slight horizontal sway and fades over time.
 * When a particle fades out or leaves the screen boundaries, it is recycled to a new
 * random position, creating an endless gentle snowfall-like effect.
 *
 * ENCAPSULATION: All particle arrays and logic are private to this class.
 * The outside world only needs to call updateAndDraw() each frame.
 */
public class ParticleSystem implements ThemeConstants {

    // Reference to the Processing applet for drawing operations and random number generation
    private PApplet p;

    // Array storing the X position of each particle in the system
    private float[] ptX;

    // Array storing the Y position of each particle in the system
    private float[] ptY;

    // Array storing the horizontal velocity (drift speed) of each particle
    private float[] ptVx;

    // Array storing the vertical velocity (rise speed) of each particle
    private float[] ptVy;

    // Array storing the current alpha (opacity) of each particle (fades over time)
    private float[] ptAlpha;

    // Total number of ambient particles managed by this system
    private int numParticles;

    /**
     * Constructor — Initializes the particle system with the specified particle count.
     * Allocates all parallel arrays and seeds each particle with random initial values.
     *
     * @param p      The Processing applet used for drawing and randomness
     * @param count  The number of particles to create and manage
     */
    public ParticleSystem(PApplet p, int count) {
        // Store the Processing applet reference for later drawing calls
        this.p = p;
        // Store the desired particle count for loop bounds
        this.numParticles = count;
        // Allocate the X position array for all particles
        ptX = new float[count];
        // Allocate the Y position array for all particles
        ptY = new float[count];
        // Allocate the horizontal velocity array for all particles
        ptVx = new float[count];
        // Allocate the vertical velocity array for all particles
        ptVy = new float[count];
        // Allocate the alpha (opacity) array for all particles
        ptAlpha = new float[count];
        // Initialize each particle with a random starting position and velocity
        for (int i = 0; i < count; i++) {
            // Reset particle i to a random position with random drift values
            resetParticle(i);
        }
    }

    /**
     * updateAndDraw — Moves every particle by its velocity, fades it out, and draws it.
     * Particles that go off-screen or become fully transparent are recycled.
     * Should be called once per frame in the draw loop.
     */
    public void updateAndDraw() {
        // Disable stroke outlines so particles are drawn as solid filled circles
        p.noStroke();
        // Iterate over every particle in the system
        for (int i = 0; i < numParticles; i++) {
            // Advance the particle's X position by its horizontal velocity
            ptX[i] += ptVx[i];
            // Advance the particle's Y position by its vertical velocity (negative = upward)
            ptY[i] += ptVy[i];
            // Gradually reduce the particle's opacity to create a smooth fade-out effect
            ptAlpha[i] -= 0.15f;
            // Check if the particle has fully faded out or drifted beyond the screen edges
            if (ptAlpha[i] <= 0 || ptX[i] < -10 || ptX[i] > p.width + 10 || ptY[i] < -10 || ptY[i] > p.height + 10) {
                // Recycle the expired particle to a fresh random position
                resetParticle(i);
            }
            // Calculate a sinusoidal pulse offset for a subtle twinkling/breathing effect
            float pulse = PApplet.sin(p.frameCount * 0.04f + i) * 8;
            // Set the fill color to theme cyan with the calculated pulsing alpha value
            p.fill(0, 255, 224, ptAlpha[i] + pulse);
            // Draw the particle as a small 2.5-pixel circle at its current position
            p.ellipse(ptX[i], ptY[i], 2.5f, 2.5f);
        }
    }

    /**
     * resetParticle — Reinitializes a single particle with a fresh random position,
     * random drift velocity, and random starting opacity.
     *
     * @param i  The index of the particle to reset within the parallel arrays
     */
    private void resetParticle(int i) {
        // Place the particle at a random X coordinate within the window width
        ptX[i] = p.random(p.width);
        // Place the particle at a random Y coordinate within the window height
        ptY[i] = p.random(p.height);
        // Assign a small random horizontal drift velocity (left or right)
        ptVx[i] = p.random(-0.3f, 0.3f);
        // Assign a slow upward vertical drift velocity (negative Y = upward in Processing)
        ptVy[i] = p.random(-0.5f, -0.1f);
        // Assign a random starting opacity (subtle range, never fully opaque)
        ptAlpha[i] = p.random(20, 60);
    }
}
