# Changelog

© EFS Studio

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
