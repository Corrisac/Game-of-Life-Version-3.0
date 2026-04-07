import processing.core.PApplet;

/**
 * Comprehensive theory & history screen with scrollable content.
 * Deep dive into why Conway's Game of Life matters.
 */
public class BackgroundScreen {
    PApplet w;
    float scrollY = 0;
    float scrollTarget = 0;
    float maxScroll = 2200;  // total content height beyond viewport

    public BackgroundScreen(PApplet window) { this.w = window; }

    public void drawScreen(float dt) {
        // V5.0: Time-based smooth scroll (framerate-independent)
        float scrollRate = 1.0f - (float) Math.exp(-7.0 * dt);  // ~0.12 at 60fps
        scrollY += (scrollTarget - scrollY) * scrollRate;

        // ── Background gradient ──
        w.noStroke();
        int c1 = w.color(10, 15, 25);
        int c2 = w.color(16, 22, 40);
        for (int y = 0; y < w.height; y += 2) {
            w.fill(w.lerpColor(c1, c2, (float) y / w.height));
            w.rect(0, y, w.width, 2);
        }

        // ── Back button ──
        boolean hov = w.mouseX > 20 && w.mouseX < 150 && w.mouseY > 14 && w.mouseY < 52;
        if (hov) { w.fill(231, 76, 60, 25); w.rect(17, 11, 136, 44, 22); }
        w.fill(hov ? w.color(245, 90, 75) : w.color(231, 76, 60));
        w.noStroke();
        w.rect(20, 14, 130, 38, 19);
        w.fill(255); w.textAlign(PApplet.CENTER, PApplet.CENTER); w.textSize(14);
        w.text("\u2190 BACK", 85, 32);

        // ── Scroll indicator ──
        float scrollPct = scrollTarget / maxScroll;
        w.fill(0, 255, 224, 50);
        w.rect(w.width - 8, 60, 4, w.height - 70, 2);
        w.fill(0, 255, 224, 180);
        float barH = Math.max(30, (w.height - 70) * ((float)w.height / (w.height + maxScroll)));
        w.rect(w.width - 8, 60 + scrollPct * (w.height - 70 - barH), 4, barH, 2);

        // ── Content (offset by scrollY) ──
        float cx = 50;                     // content left margin
        float cw = w.width - 110;          // content width
        float y  = 70 - scrollY;           // starting Y position

        // ════════════════════════════════════════════════
        //  SECTION 1: HERO TITLE
        // ════════════════════════════════════════════════
        w.fill(0, 255, 224, 200);
        w.textAlign(PApplet.CENTER, PApplet.TOP);
        w.textSize(Math.min(38, w.width / 30));
        w.text("The Theory of Cellular Automata", w.width / 2f, y);
        y += 50;

        w.fill(255, 255, 255, 100);
        w.textSize(12);
        w.text("A deep dive into one of mathematics' most elegant discoveries", w.width / 2f, y);
        y += 45;

        // ════════════════════════════════════════════════
        //  SECTION 2: ORIGINS & HISTORY
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "I. ORIGINS & HISTORY");
        y = drawCard(cx, y, cw, 200,
            "In 1970, British mathematician John Horton Conway, a professor at Cambridge University, " +
            "invented the Game of Life while exploring a question first posed by the legendary " +
            "John von Neumann in the 1940s: Is it possible to create a simple machine that can " +
            "build copies of itself?\n\n" +
            "Von Neumann's original self-replicating automaton required 29 possible cell states and " +
            "incredibly complex rules. Conway's genius was in distilling the concept down to just " +
            "TWO states (alive or dead) and FOUR rules, while preserving the profound computational " +
            "power that made von Neumann's work so groundbreaking.\n\n" +
            "The game was first brought to the world's attention through Martin Gardner's " +
            "Mathematical Games column in Scientific American (October 1970), and it immediately " +
            "captured the imagination of mathematicians, computer scientists, and hobbyists worldwide.");
        y += 20;

        // ════════════════════════════════════════════════
        //  SECTION 3: THE FOUR RULES
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "II. THE FOUR RULES OF SURVIVAL");

        float ruleCardW = (cw - 20) / 2f;

        // Row 1
        y = drawRuleCard(cx, y, ruleCardW, 1,
            "UNDERPOPULATION", "A live cell with fewer than 2 live\nneighbors dies of loneliness.",
            w.color(231, 76, 60));
        drawRuleCard(cx + ruleCardW + 20, y - 105, ruleCardW, 2,
            "SURVIVAL", "A live cell with 2 or 3 live\nneighbors continues to thrive.",
            w.color(46, 204, 113));
        y += 10;

        // Row 2
        y = drawRuleCard(cx, y, ruleCardW, 3,
            "OVERPOPULATION", "A live cell with more than 3 live\nneighbors dies of overcrowding.",
            w.color(255, 159, 67));
        drawRuleCard(cx + ruleCardW + 20, y - 105, ruleCardW, 4,
            "REPRODUCTION", "A dead cell with exactly 3 live\nneighbors springs to life.",
            w.color(0, 255, 224));
        y += 15;

        // ════════════════════════════════════════════════
        //  SECTION 4: WHY IT MATTERS
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "III. WHY THE GAME OF LIFE MATTERS");
        y = drawCard(cx, y, cw, 250,
            "TURING COMPLETENESS\n\n" +
            "The Game of Life is Turing Complete. This is a staggering fact: a grid of black and " +
            "white squares with four trivial rules has the same raw computational power as any " +
            "modern computer. Logic gates (AND, OR, NOT), memory registers, and even entire CPUs " +
            "have been constructed entirely within the Game of Life.\n\n" +
            "In 2000, Paul Rendell built a working Turing machine inside the game. In 2010, a " +
            "complete programmable computer was constructed. This means, in theory, you could run " +
            "Windows, play video games, or simulate another Game of Life — all inside a Game of Life.\n\n" +
            "THE UNDECIDABILITY PROBLEM\n\n" +
            "Because it is Turing Complete, the fate of any starting pattern is formally " +
            "'undecidable.' There is no shortcut algorithm that can predict whether a given pattern " +
            "will eventually die out, stabilize, or grow forever, without actually simulating " +
            "every single step. This connects directly to Alan Turing's Halting Problem (1936).");
        y += 20;

        // ════════════════════════════════════════════════
        //  SECTION 5: EMERGENCE & COMPLEXITY
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "IV. EMERGENCE & COMPLEXITY SCIENCE");
        y = drawCard(cx, y, cw, 200,
            "The Game of Life is the most famous demonstration of EMERGENCE: complex, unpredictable " +
            "behavior arising from extremely simple rules. No single cell 'knows' anything about " +
            "the global pattern, yet together they produce structures that move, replicate, compute, " +
            "and even evolve.\n\n" +
            "This principle underpins modern complexity science and has profound implications:\n\n" +
            "\u2022 BIOLOGY: How do trillions of simple cells create consciousness?\n" +
            "\u2022 PHYSICS: How do elementary particles give rise to galaxies?\n" +
            "\u2022 ECONOMICS: How do billions of individual decisions create market crashes?\n" +
            "\u2022 NEUROSCIENCE: How do networks of neurons produce thought?\n\n" +
            "Stephen Wolfram's 'A New Kind of Science' (2002) argues that simple rules like " +
            "Conway's may be the fundamental mechanism underlying all of nature's complexity.");
        y += 20;

        // ════════════════════════════════════════════════
        //  SECTION 6: FAMOUS PATTERNS
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "V. FAMOUS PATTERNS & AUTOMATA");

        float thirdW = (cw - 40) / 3f;
        float patY = y;

        // Still lifes
        y = drawPatternCard(cx, patY, thirdW, "STILL LIFES",
            "Stable — never change.\nBlock, Beehive, Loaf, Boat.",
            w.color(241, 196, 15));

        // Oscillators
        drawPatternCard(cx + thirdW + 20, patY, thirdW, "OSCILLATORS",
            "Flip between states.\nBlinker (p2), Pulsar (p3),\nPentadecathlon (p15).",
            w.color(46, 204, 113));

        // Spaceships
        drawPatternCard(cx + 2*(thirdW + 20), patY, thirdW, "SPACESHIPS",
            "Move across the grid.\nGlider, LWSS, MWSS, HWSS.\nGlider = the icon of the game.",
            w.color(0, 212, 255));
        y += 15;

        // More patterns card
        y = drawCard(cx, y, cw, 160,
            "ADVANCED PATTERNS\n\n" +
            "\u2022 METHUSELAHS: Tiny patterns that take thousands of generations to stabilize. " +
            "The R-pentomino (just 5 cells) takes 1,103 generations and produces 6 gliders.\n\n" +
            "\u2022 GUNS: Infinite engines that periodically shoot spaceships. Bill Gosper's Glider " +
            "Gun (1970) was the first, proving that unbounded growth is possible — winning a $50 " +
            "prize from Conway himself.\n\n" +
            "\u2022 PUFFERS & RAKES: Moving objects that leave debris trails or shoot gliders behind them.\n\n" +
            "\u2022 GARDEN OF EDEN: Patterns that cannot arise from any predecessor — they can only " +
            "exist as initial conditions, never as evolved states.");
        y += 20;

        // ════════════════════════════════════════════════
        //  SECTION 7: REAL-WORLD APPLICATIONS
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "VI. REAL-WORLD APPLICATIONS");
        y = drawCard(cx, y, cw, 200,
            "The Game of Life is far more than a mathematical curiosity:\n\n" +
            "\u2022 CRYPTOGRAPHY: Cellular automata (Rule 30) power the random number generator in " +
            "Mathematica and have been used in stream cipher design.\n\n" +
            "\u2022 BIOLOGY: CA models simulate tumor growth, neural networks, epidemic spread, " +
            "and ecological population dynamics.\n\n" +
            "\u2022 PHYSICS: Lattice gas automata simulate fluid dynamics. CAs model crystal growth, " +
            "forest fires, and seismic wave propagation.\n\n" +
            "\u2022 COMPUTER SCIENCE: CAs are used in parallel computing research, VLSI circuit " +
            "testing, image processing, and procedural content generation in video games.\n\n" +
            "\u2022 PHILOSOPHY: The Game of Life fuels debates about determinism, free will, and " +
            "whether our universe itself might be a cellular automaton (Digital Physics).");
        y += 20;

        // ════════════════════════════════════════════════
        //  SECTION 8: GPU ACCELERATION
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "VII. GPU ACCELERATION & THIS PROJECT");
        y = drawCard(cx, y, cw, 180,
            "This application demonstrates how the Game of Life maps perfectly onto GPU hardware.\n\n" +
            "Each pixel on the GPU processes one cell independently and simultaneously through " +
            "a GLSL fragment shader. The 'conway.glsl' shader samples 8 texture neighbors, counts " +
            "alive cells, and outputs the next state — all in parallel across thousands of GPU cores.\n\n" +
            "The ping-pong buffer technique alternates between two framebuffers: one is read as a " +
            "texture (current generation), the other is written to (next generation). After each " +
            "iteration, they swap. This allows the GPU to compute 100,000+ generations in seconds, " +
            "a task that would take minutes on the CPU.\n\n" +
            "This is the same parallel computing paradigm used in machine learning, physics " +
            "simulation, ray tracing, and scientific computing.");
        y += 20;

        // ════════════════════════════════════════════════
        //  SECTION 9: V4.0 — BEYOND CONWAY
        // ════════════════════════════════════════════════
        y = drawSectionTitle(y, "VIII. V4.0 — BEYOND CONWAY'S RULES");
        y = drawCard(cx, y, cw, 220,
            "Version 4.0 of this project introduces MULTIPLE RULE SETS, proving that Conway's " +
            "rules are just one point in an infinite space of cellular automata:\n\n" +
            "\u2022 HIGHLIFE (B36/S23): Identical to Conway except birth also occurs at 6 neighbors. " +
            "This single change creates a 'replicator' — a pattern that copies itself!\n\n" +
            "\u2022 DAY & NIGHT (B3678/S34678): A symmetric rule where dead and alive cells are " +
            "interchangeable. Produces beautiful, organic, expanding patterns.\n\n" +
            "\u2022 SEEDS (B2/S): Every cell dies after exactly one generation. Creates explosive, " +
            "fractal-like growth patterns — pure ephemeral beauty.\n\n" +
            "\u2022 DIAMOEBA (B35678/S5678): Grows diamond-shaped amoeba-like blobs that merge " +
            "and split unpredictably. Demonstrates how tiny rule changes create alien worlds.\n\n" +
            "V4.0 also adds a HEATMAP mode that colors cells by age, revealing which structures " +
            "are ancient (white-hot) versus freshly born (cool blue).");

        // Update max scroll based on actual content height
        maxScroll = Math.max(0, y + scrollY - w.height + 80);
    }

    // ─────────── Scroll handling (called from Main) ───────────
    public void scroll(float amount) {
        scrollTarget = PApplet.constrain(scrollTarget + amount * 40, 0, maxScroll);
    }

    // ─────────── Drawing helpers ───────────

    float drawSectionTitle(float y, String title) {
        if (y > w.height + 30 || y < -60) return y + 40;
        w.fill(0, 255, 224, 180);
        w.textAlign(PApplet.LEFT, PApplet.TOP);
        w.textSize(16);
        w.text(title, 50, y);
        // Accent line
        w.stroke(0, 255, 224, 50);
        w.strokeWeight(1);
        w.line(50, y + 24, w.width - 60, y + 24);
        w.noStroke();
        return y + 38;
    }

    float drawCard(float x, float y, float cw, float minH, String content) {
        if (y > w.height + 30) return y + minH + 10;

        w.noStroke();
        w.fill(255, 255, 255, 7);
        w.rect(x, y, cw, minH, 12);
        // Top accent
        w.fill(0, 120, 212, 30);
        w.rect(x, y, cw, 3, 12, 12, 0, 0);
        // Border
        w.noFill();
        w.stroke(255, 255, 255, 18);
        w.strokeWeight(1);
        w.rect(x, y, cw, minH, 12);
        w.noStroke();

        // Text
        w.fill(200, 210, 220, 200);
        w.textAlign(PApplet.LEFT, PApplet.TOP);
        w.textSize(13);
        w.text(content, x + 20, y + 16, cw - 40, minH - 20);

        return y + minH + 10;
    }

    float drawRuleCard(float x, float y, float cw, int num, String title, String desc, int col) {
        if (y > w.height + 30) return y + 105;

        float h = 95;
        w.noStroke();
        w.fill(255, 255, 255, 6);
        w.rect(x, y, cw, h, 10);
        // Accent
        w.fill(w.red(col), w.green(col), w.blue(col), 40);
        w.rect(x, y, cw, 3, 10, 10, 0, 0);

        // Number badge
        w.fill(w.red(col), w.green(col), w.blue(col), 50);
        w.ellipse(x + 28, y + 32, 32, 32);
        w.fill(255, 255, 255, 220);
        w.textAlign(PApplet.CENTER, PApplet.CENTER);
        w.textSize(16);
        w.text(String.valueOf(num), x + 28, y + 31);

        // Title & desc
        w.fill(w.red(col), w.green(col), w.blue(col), 200);
        w.textAlign(PApplet.LEFT, PApplet.TOP);
        w.textSize(12);
        w.text(title, x + 52, y + 14);

        w.fill(200, 210, 220, 160);
        w.textSize(11);
        w.text(desc, x + 52, y + 34, cw - 70, 60);

        return y + h + 10;
    }

    float drawPatternCard(float x, float y, float cw, String title, String desc, int col) {
        if (y > w.height + 30) return y + 130;

        float h = 120;
        w.noStroke();
        w.fill(255, 255, 255, 6);
        w.rect(x, y, cw, h, 10);
        w.fill(w.red(col), w.green(col), w.blue(col), 40);
        w.rect(x, y, cw, 3, 10, 10, 0, 0);

        w.fill(w.red(col), w.green(col), w.blue(col), 200);
        w.textAlign(PApplet.LEFT, PApplet.TOP);
        w.textSize(13);
        w.text(title, x + 16, y + 16);

        w.fill(200, 210, 220, 160);
        w.textSize(11);
        w.text(desc, x + 16, y + 38, cw - 32, 80);

        return y + h + 10;
    }
}