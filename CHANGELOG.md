# Changelog

© EFS Studio

## Unreleased

**Launcher**
- Grid, Icons and Mosaic show exactly two whole rows and scroll by row; nothing slides over
  "Continue watching" or runs off the screen, and the focused poster is no longer clipped.
- Grid and Icons with "Continue watching" always use the small clock (the large one ran into the row title).
- All app icons get the same rounded corners; round and irregular icons stay as they are.
- The first pill shows the current layout's name instead of "For you".
- Newly installed and removed apps appear and disappear right away, without restarting the launcher.
- Glass tiles are visibly different from "No background" (light pane with a thin edge).
- "Name only" and "Full-size icon" work in every layout; names shrink instead of breaking mid-word.
- Arc hides its bottom tile when "Continue watching" is off; Minimal follows the clock setting.
- Spotlight: a short strip under the big tile that scrolls through all apps and wraps around.
- Focus frame is drawn above banners and posters; long lists fade at the edge instead of ending in a cut item.
- Look panel scrolls, keeps its highlight after a change, and dims options that don't apply to the layout.
- Lighter background overlays; "Black and white" preset uses outline logos.

## 1.0 — first public release

**Launcher (EFS Studio TV)**
- 9 home screen layouts: Grid, Icons, Minimal, Logos, Spotlight, Mosaic, Side panel, Dock, Arc.
- Look panel: presets, 17 backgrounds, 5 tile styles, labels, logo styles (color / outline / full tile),
  4 fonts, clock size, "Continue watching" on/off.
- "Continue watching" from Android TV's Watch Next data (Android 8+); asks once for the TV listings permission.
- Arc layout with its own frame-by-frame animation — smooth even when system animations are turned off.
- Interface in English, Polish, German, French, Spanish and Italian — picked automatically from the TV's language.
- Works on Android 6.0+; tested on Sony KD-55XG9505 (Android 10).

**Scripts**
- `snapshot.ps1` — state of the TV before any change.
- `optimize.ps1` — disables Sony hotel mode, demos, promotions and help; optional switches for HbbTV,
  other regions, remote start, reminders and background restrictions.
- `telemetry.ps1` — disables Google and Sony crash/usage reporting; Play Store and Cast keep full background access.
- `forget-network.ps1` — removes saved Wi-Fi networks on Android 10 without root.
- Every change writes undo commands to `local/`.

**Optional: EFS Wi-Fi Watchdog**
- Restarts Wi-Fi when there is no internet after waking up — only while the screen is on, at most 3 times per screen-on.
