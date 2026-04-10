# 🧬 Game of Life on GPU

A high-performance, GPU-accelerated implementation of **Conway's Game of Life** built with **Processing** and **JOGL** (Java OpenGL). Features a cinematic PS5-inspired dashboard UI, real-time simulation, and a GPU compute lab that crunches **900,000 generations in 1.3 seconds** using a bit-parallel compute shader.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![GLSL](https://img.shields.io/badge/GLSL-4.3%20Compute-green?style=flat-square)
![Processing](https://img.shields.io/badge/Processing-4.5.2-blue?style=flat-square)
![OpenGL](https://img.shields.io/badge/OpenGL-JOGL%202.6-purple?style=flat-square)

---

## ✨ Features

- **GPU Compute Lab** — Batch-process up to 1,000,000 generations using a GL 4.3 compute shader. Runs entirely in on-chip shared memory with zero dispatch overhead. Benchmarked at **1.3 seconds for 900K iterations** (42× faster than the V3 fragment shader approach).
- **Bit-Parallel Architecture** — The 256×256 grid is packed as bits into 2048 `uint` values (32 cells per word). A full-adder tree computes all 32 cells simultaneously per bitwise operation.
- **Single Work Group** — All iterations execute inside ONE work group (1024 threads) with `barrier()` synchronization, eliminating the driver-level dispatch overhead that bottlenecks traditional multi-pass approaches.
- **Live GPU Simulation** — Real-time simulation runs on the GPU via a GLSL fragment shader with branchless `step()` math and toroidal wrapping.
- **Interactive Dashboard** — A multi-screen UI with smooth fade transitions, particle effects, and a futuristic dark theme.
- **Pattern Library** — Pre-built cellular automata patterns (gliders, glider guns, random) for quick experimentation.
- **DPI-Aware UI** — Responsive layout that scales properly on high-DPI displays.
- **Frame-Rate Independent** — All animations use delta-time for consistent behavior across hardware.

---

## 📸 Screens

| Screen | Description |
|--------|-------------|
| **Menu** | Animated dashboard with particle system background and navigation buttons |
| **Theory** | Scrollable screen explaining the significance of cellular automata |
| **Simulation** | Real-time interactive Game of Life with draw/erase, zoom, and play/pause |
| **GPU Lab** | Batch compute lab — run up to 1M generations and benchmark GPU performance |

---

## 🏗️ Architecture

```
src/game/of/life/on/gpu/
├── Main.java              # Entry point, screen routing, transitions
├── Grid.java              # Cell state storage (CPU-side byte array grid)
├── GridRenderer.java      # Renders grid to Processing PGraphics
├── GPUCompute.java        # GPU pipeline — live shader + V6 compute shader
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
├── ThemeConstants.java     # Theme configuration interface
├── DashButton.java        # Reusable dashboard button component
└── CircularDial.java      # Reusable circular dial UI component

data/
├── conway.glsl            # Fragment shader (single-step for live simulation)
├── conway_compute.glsl    # GL 4.3 compute shader (bit-parallel bulk compute)
└── explanation.txt        # Detailed shader explanation
```

---

## 🚀 How It Works

### Live Simulation Pipeline (conway.glsl)

1. **CPU** creates a texture where each pixel = one cell (black = alive, white = dead).
2. **CPU** activates the `conway.glsl` fragment shader and draws a full-screen quad.
3. **GPU** executes `main()` for every pixel in parallel — branchless Conway rules via `step()` math.
4. **CPU** swaps textures (ping-pong) and repeats at 60 FPS.

### GPU Lab Compute Pipeline (conway_compute.glsl)

1. **CPU** uploads the 256×256 grid state to a GL texture.
2. **GPU** loads the texture into shared memory, packing 32 cells per `uint` (2048 words total = 8 KB).
3. **GPU** runs N iterations entirely in shared memory with `barrier()` sync between generations:
   - Bit-parallel adder tree counts all 8 neighbors simultaneously for 32 cells per `uint`.
   - Two shared arrays (`tileA`, `tileB`) enable zero-copy ping-pong.
   - Unrolled 2-step loop eliminates dynamic array indexing overhead.
4. **GPU** unpacks the result and writes it back to an output texture.
5. **CPU** reads back via `glReadPixels()` and displays the result.

TDR safety: dispatches are chunked to max 500K iterations with `glFinish()` between chunks to reset the Windows GPU watchdog timer.

### Conway's Rules (Branchless — Fragment Shader)

```glsl
// SURVIVE: alive cell with 2 or 3 neighbors lives
float survive = cell * step(2.0, n) * step(n, 3.0);

// BIRTH: dead cell with exactly 3 neighbors is born
float birth = (1.0 - cell) * step(3.0, n) * step(n, 3.0);

// Result: alive if either condition is met
float next = max(survive, birth);
```

### Conway's Rules (Bit-Parallel — Compute Shader)

```glsl
// 8 neighbors are summed across 32 cells simultaneously via a 4-stage
// full-adder tree. The final Conway rule reduces to:
//   alive = ~bit3 & ~bit2 & bit1 & (bit0 | current_cell)
// This is: count ∈ {2,3} if alive, count == 3 if dead — in one expression.
return ~bit3 & ~(x2^cr1) & bit1 & (bit0 | c_c);
```

---

## ⚡ Performance

| Version | Strategy | 900K Iterations | Ratio |
|---------|----------|-----------------|-------|
| V3 | K=4 fragment shader + raw JOGL | 55,783ms | baseline |
| V5 | K=16 compute shader, multi-dispatch | 37,618ms | 1.5× |
| **V6** | **Single-WG bit-parallel compute** | **1,319ms** | **42×** |

---

## 🔧 Prerequisites

- **Java 17** (JDK)
- **Processing 4.5.2** core library
- **JOGL 2.6.0** (included in `dist/lib/`)
- **GPU with OpenGL 4.3+** support (for compute shader)

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

- [`data/conway.glsl`](data/conway.glsl) — Fragment shader for live real-time simulation (single-step, branchless).
- [`data/conway_compute.glsl`](data/conway_compute.glsl) — GL 4.3 compute shader for bulk GPU Lab computation (bit-parallel, single work group).
- [`data/explanation.txt`](data/explanation.txt) — Detailed line-by-line breakdown of the fragment shader.

---

## 📝 License

This project is for educational purposes — demonstrating GPU-parallel cellular automata simulation with a polished interactive frontend.
