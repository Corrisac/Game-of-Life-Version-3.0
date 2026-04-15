# Game of Life on GPU — V7.0

A high-performance, GPU-accelerated Conway's Game of Life built with **JavaFX** and **JOGL**.

Features a dark-themed dashboard UI, real-time interactive simulation, multiple cellular automaton rule sets, and a GPU Compute Lab powered by a GLSL 4.3 compute shader.

---

## Quick Start

```bash
# Build
.\gradlew.bat build

# Run
.\gradlew.bat run
```

**Requirements:** Java 17+ (tested with OpenJDK 17.0.15 Temurin)

Gradle will automatically download JavaFX 21 and JOGL 2.4.0 — no manual JAR management needed.

---

## Screens

### Menu
Animated background grid with floating particles, neon-styled navigation buttons.

### Background & Theory
Scrollable deep-dive into cellular automata: origins, the four rules, Turing completeness, emergence, famous patterns, GPU acceleration, and alternative rule sets.

### Interactive Simulation
- **Draw** cells with left-click (brush sizes: 1, 3, 5)
- **Pan** with right-click drag
- **Zoom** with scroll wheel (1×–16×, minimap appears at high zoom)
- **Speed** control: 1×, 2×, 5×, 10×, MAX
- **Heatmap** mode: colors cells by age (blue → cyan → magenta → red → white)
- **HUD** overlay: population, peak, born/died, GPS, sparkline graph
- **Rule sets**: Conway, HighLife, Day&Night, Seeds, Diamoeba
- **Patterns**: Glider, Gosper Glider Gun, random

### GPU Compute Lab
- Drag the **circular dial** or type iterations (up to 1,000,000)
- Draw on the input grid, hit **COMPUTE**
- GPU runs the V6.1 single-workgroup bit-parallel compute shader
- Result displayed side-by-side with timing stats
- Automatic CPU fallback if GL4.3 is unavailable

---

## Architecture

```
┌─ JavaFX Application ──────────────────────────────────┐
│                                                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │            ScreenManager (StackPane)            │   │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌──────────┐ │   │
│  │  │  Menu  │ │ Theory │ │  Sim   │ │ GPU Lab  │ │   │
│  │  │  FXML  │ │  FXML  │ │  FXML  │ │   FXML   │ │   │
│  │  │ +CSS   │ │  +CSS  │ │  +CSS  │ │   +CSS   │ │   │
│  │  └────────┘ └────────┘ └───┬────┘ └────┬─────┘ │   │
│  └─────────────────────────────┼───────────┼───────┘   │
│                                │           │           │
│  ┌─────────────────────────────┴───────────┴───────┐   │
│  │              Engine (Pure Java)                  │   │
│  │        Grid + SimulationRules + Patterns         │   │
│  └──────────────────────┬──────────────────────────┘   │
│  ┌──────────────────────┴──────────────────────────┐   │
│  │             GPUBackend (JOGL GL4)                │   │
│  │   Headless GLAutoDrawable → GL4 compute shader   │   │
│  │   conway_compute.glsl (V6.1 bit-parallel)       │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| **Build** | Gradle 8.7 + JavaFX plugin |
| **UI** | JavaFX 21 (FXML + CSS) |
| **GPU** | JOGL 2.4.0 (GL4.3 compute shaders) |
| **Shaders** | GLSL 4.30 (bit-parallel single-workgroup) |
| **Theme** | Custom CSS dark theme with neon accents |

---

## Project Structure

```
├── build.gradle              # Gradle build config
├── settings.gradle           # Project name
├── gradlew.bat               # Gradle wrapper (Windows)
├── data/
│   ├── conway.glsl           # Fragment shader (live sim)
│   └── conway_compute.glsl   # Compute shader (GPU Lab)
└── src/main/
    ├── java/game/of/life/on/gpu/
    │   ├── App.java           # JavaFX entry point
    │   ├── engine/
    │   │   ├── Grid.java      # 256×256 cellular automaton
    │   │   ├── SimulationRules.java
    │   │   └── PatternLibrary.java
    │   ├── gpu/
    │   │   └── GPUBackend.java  # Direct JOGL GL4 compute
    │   └── ui/
    │       ├── ScreenManager.java
    │       ├── MenuController.java
    │       ├── TheoryController.java
    │       ├── SimulationController.java
    │       ├── GPULabController.java
    │       └── controls/
    │           └── IterationDial.java
    └── resources/
        ├── css/dark-theme.css
        └── fxml/
            ├── MenuScreen.fxml
            ├── TheoryScreen.fxml
            ├── SimulationScreen.fxml
            └── GPULabScreen.fxml
```

---

## GPU Compute Performance

The V6.1 compute shader packs the 256×256 grid as bits into shared memory (16 KB) and runs all iterations inside a single work group with barrier synchronization — zero dispatch overhead.

| Version | 900K Iterations |
|---------|----------------|
| V3 (fragment shader, K=4) | 55,783 ms |
| V5 (compute, multi-dispatch) | 37,618 ms |
| **V6.1 (single-WG bit-parallel)** | **~1,300 ms** |

**42× faster** than V3.

---

## License

MIT
