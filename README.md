# EFS Studio TV

**A custom home screen and a cleanup for Android TV — no ads, no telemetry, no root.**

© EFS Studio · [MIT License](LICENSE)

---

## What's inside

| Part | What it does |
|---|---|
| **Launcher** (`apps/launcher`) | A new home screen instead of the stock one: 9 layouts, backgrounds, tile styles, a "Continue watching" row. No ads, no sponsored rows. |
| **Optimization** (`tools/optimize.ps1`) | Disables unneeded Sony packages: hotel mode, store demos, promotions, on-screen help. More free memory, fewer background processes. |
| **Telemetry** (`tools/telemetry.ps1`) | Disables Google and Sony crash/bug reporting and usage/viewing data collection. Keeps only what the Play Store needs. |
| **Wi-Fi watchdog** (`apps/wifi-watchdog`, *optional*) | For TVs that show "Connected, no internet" after waking up from standby. A separate app — install it only if you have that problem. See [docs/WIFI-WATCHDOG.md](docs/WIFI-WATCHDOG.md). |

Every change on the TV is **reversible**. The scripts write undo commands, and before changing anything
you take a snapshot of the TV's state.

## The easy way: let Claude do it

You don't need to know ADB or PowerShell. Install [Claude Code](https://claude.com/claude-code),
open this folder in it and paste the matching prompt. You can talk to Claude in your own language.

| Your TV | Prompt |
|---|---|
| Sony with Android TV / Google TV | [docs/PROMPT-SONY.md](docs/PROMPT-SONY.md) |
| Another brand with Android TV / Google TV (Philips, TCL, Hisense, Xiaomi, Sharp, Nvidia Shield, Chromecast with Google TV…) | [docs/OTHER-BRANDS.md](docs/OTHER-BRANDS.md) |
| Sony with the "no internet after waking up" problem (optional, separate) | [docs/WIFI-WATCHDOG.md](docs/WIFI-WATCHDOG.md) |

Claude checks the model, takes a snapshot, builds and installs the launcher, and asks for your
permission before every change on the TV.

Prefer doing it yourself? Step by step: [docs/INSTALL.md](docs/INSTALL.md).

## Which TVs it works on

| TV | Launcher | Optimization & telemetry |
|---|---|---|
| **Sony BRAVIA with Android TV 9/10** (e.g. XG/XH/AG, 2019–2020) | ✅ tested (KD-55XG9505, Android 10) | ✅ the scripts are written for Sony |
| **Sony with Google TV** (2021 and newer) | ✅ should work (different stock launcher) | ⚠️ some package names differ — Claude adapts them |
| **Other Android TV / Google TV devices** | ✅ should work (Android 6+) | ⚠️ different vendor packages — use [OTHER-BRANDS](docs/OTHER-BRANDS.md) |
| **Amazon Fire TV** | ⚠️ installs, but newer Fire OS won't let you replace the home screen | ❌ |
| **Samsung (Tizen), LG (webOS), Panasonic, Roku** | ❌ not Android — won't work | ❌ |

## What changes and what stays

**Disabled (reversibly):**
- the stock home screen (replaced by EFS Studio TV),
- Sony: hotel/B2B mode, store demos, promotions, "Sony Select", on-screen help, Living Fit,
- telemetry: Google Feedback, bug report senders (Google and Sony), Sony IDD agent (usage data), Sony viewing context,
- *optional:* HbbTV (red button), other-region features (YouView), turning the TV on over the network, programme reminders,
- *optional:* background work of apps you choose (they still work normally when you open them).

**Kept and working:**
- Play Store (with background updates), Google Play services,
- **Google Cast** (casting from your phone), Google Assistant, phone remote,
- the TV app, HDMI inputs and tuner (`com.sony.dtv.tvx` — without it HDMI shows a black screen; the scripts never touch it),
- system updates, picture and sound settings, AirPlay, Bluetooth,
- all your installed apps (Netflix, YouTube, Prime Video, Disney+…).

**Do this by hand in the TV menu** (ADB cannot change it): turn off Google "Usage & diagnostics"
and withdraw Sony's data collection consents in its privacy policy.

## The launcher at a glance

- **9 layouts:** Grid, Icons, Minimal, Logos, Spotlight, Mosaic, Side panel, Dock, Arc (a carousel along an arc with smooth animation).
- **Look:** 17 backgrounds (16 photos + a blurred poster), tiles Glass/Frost/Dark/Clear/App color,
  logos in color, as an outline or filling the whole tile, 4 fonts, clock large/small/hidden, ready-made presets.
- **Continue watching** from the system's Watch Next data, for apps that provide it.
- **Remote:** the MENU button (or the "Wygląd"/Look pill) opens the panel. ↑↓ select, ←→ change, OK list, BACK exit.
- No Google Play Services, no AndroidX, no Gradle — the APK is about 7 MB, mostly backgrounds.
- The interface is currently in **Polish**. An English version is the first item on the wish list — see below.

Layouts and options in detail: [docs/LAYOUTS.md](docs/LAYOUTS.md).

## Safety — read before installing

- Once the stock home screen is disabled, **EFS Studio TV is the only home screen**. If you uninstall it,
  the TV has no home screen. Recovery over ADB:
  `adb shell pm enable com.google.android.tvlauncher` (Android TV) or
  `adb shell pm enable com.google.android.apps.tv.launcherx` (Google TV).
- Don't turn off ADB debugging until you're done — without it you can't undo anything remotely.
- Nothing here modifies system files, needs root or changes the TV's firmware.
  A factory reset brings everything back to out-of-the-box state.
- Use at your own risk (MIT license, no warranty).

## Building

Requirements: Windows, [Android SDK](https://developer.android.com/studio#command-line-tools-only)
(build-tools + platform android-29 or newer, platform-tools), JDK 17.

```powershell
.\apps\launcher\build.ps1          # -> apps\launcher\build\app.apk
.\apps\wifi-watchdog\build.ps1     # -> apps\wifi-watchdog\build\app.apk (optional)
```

No Gradle: `aapt2` → `javac` → `d8` → `zipalign` → `apksigner` (`tools/build-apk.ps1`).
Signed with the Android debug key by default. Your own key: `keystore.properties` (see `tools/build-apk.ps1`).

## Hack on it! 🛠️

This project is meant to be tinkered with. The code is simple (plain Java and classic Android views,
no frameworks), so a new layout, a new background or support for another TV brand is an evening's work.

Ideas to start with:
- **English interface** (with Polish kept as a translation) — the most wanted change,
- **new home screen layouts** — each one is a single method in `HomeLayouts.java`,
- **other brands** — package lists for optimization on Philips, TCL, Hisense, Xiaomi…,
- **fixing known bugs** — listed in [CONTRIBUTING.md](CONTRIBUTING.md#known-bugs--good-first-issues),
- hiding and reordering apps from the panel, your own photos as backgrounds, themes…

Got an idea or a fix? Fork it, change it, test it on your TV and send a pull request.
How to start: [CONTRIBUTING.md](CONTRIBUTING.md). Claude can help you with code changes too —
the project is set up for that.

## Trademarks

Sony, BRAVIA, Android TV, Google TV and Google Cast are trademarks of their respective owners.
This project is independent and not affiliated with Sony or Google. The backgrounds were created by
EFS Studio and are part of the project under the MIT license.
