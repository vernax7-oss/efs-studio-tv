# Contributing

© EFS Studio

**Yes, please — tinker with it, break it, improve it.** New layouts, support for more TV brands,
translations, bug fixes, wild ideas — all welcome. The code is deliberately simple: plain Java,
classic Android views, no Gradle, no frameworks. If you can read Java, you can change this launcher.

## How to start

1. Fork the repository and clone it.
2. Build: `.\apps\launcher\build.ps1` (requirements in [README](README.md#building)).
3. Install on your TV: `adb install -r apps\launcher\build\app.apk`.
4. Test every option over ADB without touching the remote — see [docs/LAYOUTS.md](docs/LAYOUTS.md#testing-layouts-over-adb).
5. Send a pull request with a short description and screenshots (`adb shell screencap`).

You can also ask Claude (e.g. [Claude Code](https://claude.com/claude-code)) to help — open the repo
and describe what you want to change. The project is small enough for it to understand as a whole.

## Project map

```
apps/launcher/                 the home screen (package studio.efs.braviahome)
  src/.../MainActivity.java    app list, Continue watching data, tabs, panel wiring, backgrounds
  src/.../HomeLayouts.java     the 9 layouts — one method each, plus the Arc animation (RingMotion)
  src/.../Look.java            all look options (layouts, tiles, labels, logos, fonts, backgrounds, presets)
  src/.../Panel.java           the slide-in "Look" panel and its pick lists
  src/.../LineIcons.java       "Outline" logos (Sobel edges, oversampled for thin lines)
  src/.../ImageLoader.java     poster loading for Continue watching
  assets/bg_01..16.jpg         backgrounds (1920x1080 JPEG)
apps/wifi-watchdog/            optional Wi-Fi watchdog (separate app, targetSdk 28)
tools/build-apk.ps1            build without Gradle (aapt2 -> javac -> d8 -> zipalign -> apksigner)
tools/*.ps1                    snapshot, optimization, telemetry, forget-network (ADB scripts)
docs/                          install guide, prompts for Claude, layouts, watchdog
```

## Recipes

**Add a layout**
1. `Look.java`: add an id to `LAYOUTS`, a name to `LAYOUT_NAMES` and a one-line hint to `LAYOUT_HINTS`.
2. `HomeLayouts.java`: add a `case` in the dispatcher and a method that builds the screen into `host`.
   Reuse `fillTileContent()`, `logo()`, `caption()`, `focus()` and `click()` — they already respect the
   look options.
3. Make sure focus is visible, nothing gets clipped at the screen edge and every app is reachable.

**Add a background**
1. Add `assets/bg_17.jpg` (1920x1080, JPEG, ~300–700 KB).
2. `Look.java`: add `"bg_17"` to `BACKGROUNDS` and its name to `BACKGROUND_NAMES`.
3. Only images you made yourself or with a license that allows commercial use and redistribution.

**Support another TV brand**
Add the vendor's package lists (bloat, telemetry, the stock launcher) to
[docs/OTHER-BRANDS.md](docs/OTHER-BRANDS.md), or a separate script in `tools/` modeled on `optimize.ps1`:
always reversible (write undo commands), skip missing packages, never touch the protected list.

## Rules

- **Write your own code.** Learn from other projects, but don't copy them 1:1.
- **Licenses:** everything that goes into the app (libraries, fonts, icons, images) must allow
  redistribution and commercial use — MIT, Apache 2.0, BSD, ISC and similar. No GPL/AGPL,
  no "non-commercial", nothing without a clear license.
- **No personal data** in commits, issues or screenshots: no IP addresses, Wi-Fi names, account e-mails,
  device names, or "Continue watching" rows with your viewing history. The `local/` folder is git-ignored
  for that reason — keep it that way.
- **TV safety:** never disable the Play Store, Google Play services, Cast, the Assistant, the TV/HDMI app,
  system updates or settings in any script. Every change must be reversible.
- Test on a real TV before sending a PR, and say which model and Android version you used.

## Known bugs — good first issues

Found during a full remote-control test on a Sony KD-55XG9505 (Android 10). Each one is small and well located.

| Where | Bug |
|---|---|
| Panel | The "Continue watching" row reads and writes `look.clock` instead of `look.continueRow` (the `default` branch of `cycle()` in `MainActivity`) — "Hide" doesn't hide the row and corrupts the clock setting. |
| Panel | The last two rows (Sony interface, TV settings) are below the screen edge; the panel doesn't scroll. |
| Panel | After changing a value with ←/→ the row highlight disappears. |
| Pick list | `Panel.focusPickerRow()` uses `pickerIndex + 1` on a column that no longer has a header — the highlight is one row below the item that OK selects. |
| Presets | "Czarno-białe" (Black & white) sets `logo = "white"`, which no longer exists in `Look.LOGOS` — logos stay in color. |
| Grid | The "Continue watching" row lives inside the scrolling column and changes height with focus — the focused tile can jump off screen. |
| Logos | Hard-coded to 10 apps with no scrolling; the row is wider than the screen, both ends are clipped. |
| Spotlight | Only 9 apps reachable; → at the end of the strip jumps to the "Look" pill. |
| Side panel | → from the list jumps to the pills instead of the preview. |
| Icons, Spotlight | With clear tiles, the focused tile is barely visible (no frame). |
| Labels | "Name only" is ignored in Grid and Icons (they don't check `logoShown()`). |
| Minimal, Logos | The panel lets you change options these layouts ignore (clock, tiles). |
| Sony interface | With the stock launcher disabled, the dialog suggests `pm disable-user` — the opposite of what's needed. |
| Everywhere | The interface is Polish only. Move strings to `res/values/strings.xml` (English) + `res/values-pl/` (Polish). Code comments are partly Polish too — translations welcome. |

## Ideas

- English interface (and more languages),
- hide/reorder apps from the panel,
- your own photo as a background (from a USB stick or the network),
- per-layout clock styles, weather, a screensaver,
- support for Google TV's stock launcher switching without ADB,
- package lists for more brands.
