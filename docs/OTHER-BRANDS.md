# Other TV brands (Android TV / Google TV)

© EFS Studio

The launcher works on any TV or box with **Android TV or Google TV** (Android 6 or newer).
Three things differ between brands:

1. **The stock home screen's name** — it has to be disabled so that HOME opens EFS Studio TV.
2. **Vendor packages**: `optimize.ps1` and `telemetry.ps1` have lists for Sony.
   On another brand the Sony packages simply aren't there (the script prints "n/a"),
   and the vendor's own packages have to be chosen separately.
3. **The app that handles HDMI and the tuner** — different for every vendor. Never disable it.

## What we know about specific brands

| Brand / device | Stock home screen (usually) | Notes |
|---|---|---|
| Android TV (Philips, Sharp, TCL, Hisense, older Sony) | `com.google.android.tvlauncher` | Some vendors add their own launcher — check with the command below. |
| Google TV (Sony 2021+, TCL, Hisense, Chromecast with Google TV) | `com.google.android.apps.tv.launcherx` | Sometimes also `com.google.android.tungsten.setupwraith` (setup wizard) — leave it alone. |
| Xiaomi Mi TV / Mi Box | `com.mitv.tvhome` (PatchWall) and `com.google.android.tvlauncher` | Usually both need to be disabled. |
| Philips | `com.google.android.tvlauncher` | TV apps are `org.droidtv.*` packages — don't disable them blindly. |
| Nvidia Shield | `com.google.android.tvlauncher` | Works without issues. |
| Amazon Fire TV | `com.amazon.tv.launcher` | Newer Fire OS restores its own launcher — in practice it can't be replaced. |

You can always check the name yourself:

```powershell
adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

The output lists every home screen. Disable the ones that are not `studio.efs.braviahome`.

## Telemetry on other brands

Common to all (Google) — works in `telemetry.ps1` unchanged:
`com.google.android.feedback`, `com.google.android.tv.bugreportsender`.

Everything else depends on the vendor. Typical candidates: packages with `analytics`, `statistics`,
`feedback`, `bugreport`, `logcollect`, `diagnos`, `usage`, `ads`, `recommend`, `promo` in the name.
**Always check that a package doesn't handle picture, sound, HDMI, the tuner, the remote or updates.**
The safest way is the prompt below: Claude reads the package list, explains each candidate
and disables only the ones you approve.

---

## Prompt for Claude Code (other brands)

Open the repository folder in [Claude Code](https://claude.com/claude-code), turn on USB debugging / ADB
on the TV ([INSTALL.md](INSTALL.md), step 1), put in your IP and paste:

```text
My TV / box: <brand and model, e.g. Philips 55PUS8506>. IP address: 192.168.1.50 (change it)

Install the EFS Studio TV project from this repository: the launcher as the home screen, then
optimization and telemetry cleanup adapted to MY brand. The scripts in tools/ are written
for Sony — for another brand build the package lists from scratch, based on what is actually
on my device. Follow docs/INSTALL.md and docs/OTHER-BRANDS.md. Talk to me in the language I write in.

Rules:
1. Before any change on the device tell me what you will do and why, and wait for my approval.
   Read-only checks need no approval.
2. NEVER disable: the Play Store (com.android.vending), Google Play services (com.google.android.gms),
   com.google.android.gsf, Cast (com.google.android.apps.mediashell), the Assistant, the app that
   handles HDMI/tuner/inputs, picture, sound, the remote, Bluetooth, networking, system updates,
   settings or keyboards. If you're not sure what a package does — don't disable it, tell me about it.
3. Disable the stock home screen only after the launcher is installed and you have checked that
   it starts. Give me the command that brings the stock one back.
4. Record every change as an undo command in local/undo-<brand>-<date>.txt
   (the same way the scripts in tools/ do). Never commit the local/ folder to Git.
5. After every step check that the device works (home screen, HDMI/tuner, Play Store, Cast)
   and report. If something stops working — stop and offer to undo.

Order:
a) Check the tools on this computer (adb, JDK 17, Android SDK with build-tools and platform android-29+).
b) Connect (adb connect <IP>:5555). Read: ro.product.brand, ro.product.model,
   ro.build.version.release, whether it's Android TV or Google TV, the list of home screens
   (cmd package query-activities ... category.HOME) and the list of all packages (pm list packages -f).
c) Snapshot: tools/snapshot.ps1 -Dev <IP>:5555.
d) Build and install the launcher (apps/launcher/build.ps1, adb install -r), start it with am start,
   check with a screenshot.
e) Disable the stock home screens found in b) and check with HOME. If the vendor restores its
   launcher (e.g. Fire TV) — tell me and don't fight the system.
f) Optimization: from the package list, prepare a table of candidates to disable
   (package / what it does / risk / your recommendation). Show it and disable only the approved ones.
g) Telemetry: run tools/telemetry.ps1 (the Google packages will work, the Sony ones won't be there),
   then propose vendor telemetry packages the same way as in f).
   Tell me what to switch off by hand in the menu (Google "Usage & diagnostics", vendor consents).
h) Summary: what changed, what stayed, where the undo files are, how to get back to the stock home screen.
   If I want, also prepare the package list for my brand in a form I can contribute to the
   repository (without my data: IP, network names, device name).
```

---

Did it work on your brand? **Share your package list** — add it to this file or add a separate script
to `tools/` and send a pull request ([CONTRIBUTING.md](../CONTRIBUTING.md)).
The next person with the same TV will have an easier time.
