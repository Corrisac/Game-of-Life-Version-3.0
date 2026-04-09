# 🧬 Game of Life on GPU

A high-performance, GPU-accelerated implementation of **Conway's Game of Life** built with **Processing** and **JOGL** (Java OpenGL). Features a cinematic PS5-inspired dashboard UI, real-time simulation, and a GPU compute lab that can crunch millions of generations in seconds.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![GLSL](https://img.shields.io/badge/GLSL-Fragment%20Shader-green?style=flat-square)
![Processing](https://img.shields.io/badge/Processing-4.5.2-blue?style=flat-square)
![OpenGL](https://img.shields.io/badge/OpenGL-JOGL%202.6-purple?style=flat-square)

---

## ✨ Features

- **GPU-Accelerated Simulation** — The entire Game of Life runs on the GPU via a GLSL fragment shader. Every cell is computed in parallel, enabling smooth real-time simulation at high grid resolutions.
- **Multi-Step Shader (K=4)** — A specialized shader mode computes **4 generations per render pass** by reading a 9×9 neighborhood and cascading through 7×7 → 5×5 → 3×3 → center. Cuts GPU render passes by 4×.
- **Branchless Conway Rules** — Uses `step()` math instead of `if/else` to avoid GPU warp divergence. Zero branching in the hot path.
- **Toroidal Grid** — The grid wraps around (top↔bottom, left↔right) using `fract()` on texture coordinates.
- **Interactive Dashboard** — A multi-screen UI with smooth fade transitions, particle effects, and a futuristic dark theme.
- **GPU Compute Lab** — Batch-process thousands of generations with configurable iteration counts via raw JOGL calls.
- **Pattern Library** — Pre-built cellular automata patterns for quick experimentation.
- **DPI-Aware UI** — Responsive layout that scales properly on high-DPI displays.
- **Frame-Rate Independent** — All animations use delta-time for consistent behavior across hardware.

---

## 📸 Screens

| Screen | Description |
|--------|-------------|
| **Menu** | Animated dashboard with particle system background and navigation buttons |
| **Theory** | Scrollable screen explaining the significance of cellular automata |
| **Simulation** | Real-time interactive Game of Life with draw/erase, zoom, and play/pause |
| **GPU Lab** | Batch compute lab — run thousands of generations and benchmark GPU performance |

---

## 🏗️ Architecture

```
src/game/of/life/on/gpu/
├── Main.java              # Entry point, screen routing, transitions
├── Grid.java              # Cell state storage (CPU-side boolean grid)
├── GridRenderer.java      # Renders grid to Processing PGraphics
├── GPUCompute.java        # GPU pipeline — shader loading, JOGL batch compute
├── SimulationScreen.java  # Real-time simulation screen
├── SimulationUI.java      # Simulation screen UI layout constants
├── GPULabScreen.java      # GPU batch compute lab screen
├── GPULabUI.java          # GPU Lab UI layout constants
├── MenuScreen.java        # Main menu screen
├── MenuUI.java            # Menu UI layout constants
├── BackgroundScreen.java  # Theory/info screen
├── ParticleSystem.java    # Animated particle effects
├── PatternLibrary.java    # Pre-built GoL patterns (gliders, etc.)
├── SimulationRules.java   # Conway rule definitions
├── UIColors.java          # Color palette
├── UILayout.java          # Global layout constants
├── ThemeConstants.java    # Theme configuration interface
├── DashButton.java        # Reusable dashboard button component
└── CircularDial.java      # Reusable circular dial UI component

data/
├── conway.glsl            # The GPU shader (single-step + multi-step K=4)
└── explanation.txt        # Detailed line-by-line shader explanation
```

---

## 🚀 How It Works

### The Simulation Pipeline

1. **CPU** creates a texture (image) where each pixel = one cell. Black = alive, white = dead.
2. **CPU** binds the texture and activates the `conway.glsl` fragment shader.
3. **CPU** draws a full-screen quad. The GPU executes `main()` for **every pixel in parallel**.
4. **GPU** — each pixel reads its 8 neighbors from the texture using `texture2D()`.
5. **GPU** — applies Conway's rules via branchless `step()` math.
6. **GPU** — writes the next-generation state to `gl_FragColor`.
7. **CPU** swaps textures (output becomes the next input) and repeats.

### Conway's Rules (Branchless)

```glsl
// SURVIVE: alive cell with 2 or 3 neighbors lives
float survive = cell * step(2.0, n) * step(n, 3.0);

// BIRTH: dead cell with exactly 3 neighbors is born
float birth = (1.0 - cell) * step(3.0, n) * step(n, 3.0);

// Result: alive if either condition is met
float next = max(survive, birth);
```

---

## 🔧 Prerequisites

- **Java 17** (JDK)
- **Processing 4.5.2** core library
- **JOGL 2.6.0** (included in `dist/lib/`)

All required `.jar` files are included in the `dist/lib/` directory.

---

## ▶️ Running

### Windows (one-click)

```batch
run.bat
```

This script compiles the source, builds the JAR, and launches the application.

### Manual

```bash
# Compile
javac --release 17 -cp "dist/lib/core-4.5.2.jar;dist/lib/jogl-all-2.6.0.jar;dist/lib/gluegen-rt-2.6.0.jar" -d build/classes src/game/of/life/on/gpu/*.java

# Run
java --enable-native-access=ALL-UNNAMED -jar dist/Game_Of_Life_On_GPU.jar
```

---

## 🎮 Controls

| Key / Action | Screen | Function |
|---|---|---|
| **Click & Drag** | Simulation | Draw/erase cells |
| **Space** | Simulation | Play / Pause |
| **Mouse Wheel** | Simulation | Zoom in / out |
| **Arrow Keys** | Theory | Scroll content |

---

## 📄 Shader Documentation

See [`data/explanation.txt`](data/explanation.txt) for a complete line-by-line breakdown of the `conway.glsl` shader, covering every function call, data flow, GLSL built-ins, and the branchless optimization technique.

---

## 📝 License

This project is for educational purposes — demonstrating GPU-parallel cellular automata simulation with a polished interactive frontend.
