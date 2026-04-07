/**
 * ThemeConstants — V5.0: Composite interface extending all domain-specific interfaces.
 *
 * ZERO BREAKING CHANGES: All classes that `implements ThemeConstants` still compile
 * unchanged and gain access to all constants from UIColors, UILayout, and SimulationRules.
 *
 * For new code, prefer implementing the specific interface you need:
 *   - UIColors for color constants
 *   - UILayout for grid/speed configuration
 *   - SimulationRules for cellular automaton rule definitions
 */
public interface ThemeConstants extends UIColors, UILayout, SimulationRules {
    // V5.0: All constants are now organized in their respective sub-interfaces.
    // This composite interface exists solely for backward compatibility.
}
