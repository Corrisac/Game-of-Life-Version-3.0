# CHANGES — Dual Lab Feature (Life vs Antilife)

## New Files

### `src/main/java/game/of/life/on/gpu/engine/DualGrid.java`
New data model for the two-species simulation. Mirrors the structure of `Grid.java`
(double-buffered byte arrays, zero-allocation swap, age tracking, ring-buffer population
history) but holds two independent species: Life and Antilife.

The simulation loop computes Conway B3/S23 for each species in isolation, then applies
the single cross-species rule: if a dead cell simultaneously qualifies for a Life birth
**and** an Antilife birth on the same tick, both are cancelled (contested-birth rule).
The invariant `lifeFront[i] & antiFront[i] == 0` is maintained after every step and
enforced by every setter (`setLife`, `setAnti`, `erase`).

### `src/main/java/game/of/life/on/gpu/ui/DualLabController.java`
Interactive controller for the new Dual Lab screen. Features:
- Three-mode brush selector (Life / Antilife / Eraser) with visual active-state feedback
- Brush size cycle (1×, 3×, 5×)
- Play / Pause / Step / Clear playback controls
- Speed levels: 1×, 2×, 5×, 10×, 30×, 60× (steps per frame)
- Zoom (scroll wheel, 1×–16×) and pan (right-click drag) with minimap at zoom > 1.5×
- HUD overlay: live Life/Anti populations, generation counter, TPS, dual sparkline
- Three presets (cycle button):
  - **DUEL** — Life SE-glider vs Anti NW-glider converging on the centre
  - **FRONTIER** — R-pentomino Life (left) vs R-pentomino Anti (right); contested growth zone forms at the boundary
  - **EMPIRES** — Dense random fills (seeded RNG for reproducibility) on each half; chaotic frontier

### `src/main/resources/fxml/DualLabScreen.fxml`
FXML layout for the new screen (BorderPane with toolbar top/bottom, resizable canvas centre,
HUD overlay). References `DualLabController`.

---

## Modified Files

### `src/main/java/game/of/life/on/gpu/App.java`
- Added `DualGrid dualBoard` shared state (created once at startup alongside `gameBoard`).
- Added `DualLabController dualLabController` and `Parent dualLabRoot`.
- `loadDualLabScreen()` loads and inits the new screen.
- `navigateTo()` switch extended to handle index 4.
- `AnimationTimer` switch extended to dispatch `dualLabController.update()` for screen 4.
- Added `getDualBoard()` accessor.

### `src/main/java/game/of/life/on/gpu/ui/MenuController.java`
- Added `@FXML goDualLab()` → `app.navigateTo(4)`.

### `src/main/resources/fxml/MenuScreen.fxml`
- Added fifth button: "DUAL LAB — LIFE VS ANTILIFE" (orange style).
- Reduced VBox spacing from 6→5 and bottom padding from 80→60 to keep the layout
  vertically balanced with 5 buttons.

### `src/main/resources/fxml/TheoryScreen.fxml`
- Inserted **Section IX — DUAL SPECIES: LIFE VS ANTILIFE** before the existing
  "Beyond Conway" section.
- Explains the independent-neighbourhood rule, the contested-birth collision rule,
  the invariant, and three concrete demonstrations (Glider Duel, Contested Frontier,
  Empires).

### `src/main/resources/css/dark-theme.css`
- Added `.neon-button.antilife` — pink (#FF3EA5) border/hover/press variant for the
  Anti brush button.
- Added `.neon-button.brush-active-life` — cyan filled state (Life brush selected).
- Added `.neon-button.brush-active-anti` — pink filled state (Anti brush selected).
- Added `.neon-button.brush-active-erase` — white/dim filled state (Eraser selected).

---

## Existing Files — No Changes

`Grid.java`, `GPUBackend.java`, `conway_compute.glsl`,
`SimulationController.java`, `GPULabController.java`, `TheoryController.java`,
`SimulationRules.java`, `PatternLibrary.java`, `IterationDial.java`,
`ScreenManager.java`, `SimulationScreen.fxml`, `GPULabScreen.fxml`,
`TheoryScreen.fxml` (content only — layout tag unchanged).

The V6.1 GPU compute shader, all GPU Lab functionality, and all four original
screens are untouched and unaffected.

---

## Architecture Notes

The `DualGrid` is intentionally separate from `Grid` rather than generalising `Grid`
into a multi-species abstraction. Reasons:
1. `Grid` supports 5 configurable rule sets; `DualGrid` is hardwired to Conway B3/S23
   (the collision rule only makes sense for identical rulesets).
2. Separation keeps both classes simple and avoids adding conditional branches to
   performance-critical simulation loops in `Grid`.
3. The existing `GPUBackend` / `conway_compute.glsl` pipeline works with `Grid` only;
   extending it for dual species would require a new shader and significant backend
   refactoring. The Dual Lab is an interactive simulation (not a batch-compute tool),
   so CPU stepping per frame is sufficient.
