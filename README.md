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

### Android versions

The minimum is **Android 6.0**. Tested on Android 10 (Sony KD-55XG9505, build `QTG3.200305.006`).

| Android | Build starts with | Launcher | Scripts |
|---|---|---|---|
| 6.0 (API 23) | `M` | ✅ works — but no "Continue watching" row (Android has no Watch Next before 8) | ✅ optimization and telemetry; `-RestrictBackground` has no effect |
| 7.0 / 7.1 (API 24–25) | `N` | ✅ same as 6.0 | same as 6.0 |
| 8.0 / 8.1 (API 26–27) | `O` | ✅ everything, including Continue watching | same as 6.0 |
| 9 (API 28) | `P` | ✅ everything | ✅ everything |
| **10 (API 29)** | **`Q`** | ✅ **tested** | ✅ **tested**; `forget-network.ps1` works **only** here |
| 11 (API 30) | `R` | ✅ should work (not tested yet) | ✅ should work |
| 12 (API 31–32) | `S` | ✅ should work (Google TV) | ✅ should work |
| 13 (API 33) | `T` | ✅ should work (rare on TVs) | ✅ should work |
| 14 (API 34) | `U` | ✅ should work (Google TV) | ✅ should work |

**How to find your version:** Settings → Device Preferences → **About** → *Android TV OS version*.
Or look at the **build number** on the same screen (e.g. `QTG3.200305.006`) — its **first letter** is the version
from the table: `M` = 6, `N` = 7, `O` = 8, `P` = 9, `Q` = 10, `R` = 11, `S` = 12, `T` = 13, `U` = 14. Over ADB: `adb shell getprop ro.build.version.release`.

> On Android 11 and newer (most Google TV models) ADB over the network uses **Wireless debugging** with
> a pairing code: `adb pair <IP>:<port>` first, then `adb connect`. See [docs/INSTALL.md](docs/INSTALL.md).

Tested on another version or brand? Please report it (an issue or a PR) — this table grows with your help.

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
- **Remote:** the MENU button (or the "Look" pill at the top) opens the panel. ↑↓ select, ←→ change, OK list, BACK exit.
- **Languages:** English, Polish, German, French, Spanish, Italian. Android picks the TV's system language
  automatically (Settings → Device Preferences → Language) — there is nothing to set. Any other language shows English.
  Adding a language is one file — see [CONTRIBUTING.md](CONTRIBUTING.md#add-a-language).
- No Google Play Services, no AndroidX, no Gradle — the APK is about 7 MB, mostly backgrounds.

Layouts and options in detail: [docs/LAYOUTS.md](docs/LAYOUTS.md).

## Privacy — what the apps do with your data

**Nothing leaves your TV.** No analytics, no tracking, no accounts, no server of ours — there isn't one.
You can check it yourself: the whole source code is here, and it's small.

- **Launcher:** reads the list of installed apps and the system's "Continue watching" entries (Watch Next) to show them
  on screen. Its only network use is downloading the **poster images** for that row — from the addresses the
  streaming apps themselves put there (Netflix, YouTube…). Your settings are stored only on the TV.
- **Wi-Fi watchdog:** has **no internet permission at all**. It only checks whether the network works and writes a log
  on the TV.
- **Scripts:** run on your computer and talk only to your TV over ADB. Their output (snapshot, undo files) stays in
  the `local/` folder, which is never committed to Git.

## "Unsafe app" / Play Protect warning when installing

When you install the APK, Android (Google **Play Protect**) may show a warning such as *"Unsafe app blocked"*,
*"This app was built for an older version of Android"* or *"Play Protect doesn't recognize this app"*.
**This is normal for any app installed outside the Play Store** — Play Protect simply doesn't know it.
The apps target an older Android API on purpose (the watchdog needs it to switch Wi-Fi), which also triggers
the "older version" message.

Choose **More details → Install anyway** (the wording differs slightly between TVs). If you're asked whether to
send the app to Google for scanning, you can say no — it isn't needed. If you want to be sure, read the code or
build the APK yourself from this repository.

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

Requirements: Windows (PowerShell scripts — a macOS/Linux port is welcome),
[Android SDK](https://developer.android.com/studio#command-line-tools-only)
(build-tools + platform android-29 or newer, platform-tools), JDK 17.
If PowerShell refuses to run the scripts: `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`.

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
- **your language** — one translated `strings.xml` file,
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
