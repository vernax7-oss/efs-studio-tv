# Prompt: install on a Sony TV (Claude Code)

© EFS Studio

1. Install [Claude Code](https://claude.com/claude-code) and open this repository's folder in it.
2. On the TV, turn on Developer options and USB debugging ([INSTALL.md](INSTALL.md), step 1).
3. Find the TV's IP address (Settings → Network).
4. Copy the prompt below, put your IP address in the first line and paste it into Claude.

Claude asks for your permission before every change on the TV. You can write to it in your own language.

---

```text
My TV is a Sony with Android TV / Google TV. IP address: 192.168.1.50  (change to yours)

Install the EFS Studio TV project from this repository on it: the launcher as the home screen,
the optimization and the telemetry cleanup. Follow docs/INSTALL.md and the scripts in tools/.
Talk to me in the language I write in.

Rules (they apply for the whole job):
1. Before any change on the TV (installing, disabling a package, changing a setting) tell me
   what you will do and why, and wait for my approval. Read-only checks need no approval.
2. NEVER disable or restrict: com.android.vending (Play Store), com.google.android.gms,
   com.google.android.gsf, com.google.android.apps.mediashell (Cast), com.google.android.katniss
   (Assistant), com.sony.dtv.tvx (the TV app and HDMI inputs — without it HDMI shows a black screen),
   system update packages, TV settings or keyboards.
3. Disable the stock home screen only after the launcher is installed and you have checked
   that it starts. Always give me the command that brings the stock one back.
4. Everything must be reversible: the undo files from tools/ stay in local/. Don't delete them.
5. After every step check that the TV works (home screen, HDMI, Play Store, Cast) and report.
   If something stops working, stop and offer to undo the last step.
6. Don't send data from my TV anywhere and never commit the local/ folder to Git.

Order:
a) Check the tools on this computer: adb, JDK 17 (javac), Android SDK with build-tools and
   platform android-29+. If something is missing, tell me how to install it, or install it
   with my approval. If PowerShell refuses to run the scripts, use
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass (current window only).
b) Connect: adb connect <IP>:5555 (ask me to confirm the permission prompt on the TV screen).
   If that fails and the TV runs Android 11 or newer, guide me through Wireless debugging:
   adb pair <IP>:<pairing port> with the code from the TV, then adb connect <IP>:<port>.
   Read the model, Android version and whether it is Android TV or Google TV
   (getprop ro.product.model, ro.build.version.release, the stock launcher:
   cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME).
c) Take a snapshot: tools/snapshot.ps1 -Dev <IP>:5555.
d) Build the launcher (apps/launcher/build.ps1), install it (adb install -r) — before installing, tell me
   that the TV may show a Play Protect warning and that I should choose "Install anyway" on the TV
   (the app collects no data; see README "Privacy"). Then grant the permission
   for the "Continue watching" row (pm grant studio.efs.braviahome android.permission.READ_TV_LISTINGS),
   start it with am start and check that it works (screenshot: adb shell screencap).
e) Make it the home screen with pm disable-user on the stock launcher found in step b).
   Check with HOME (adb shell input keyevent KEYCODE_HOME + a screenshot).
f) Optimization: show me the tools/optimize.ps1 switches with a short explanation and ask which
   to use (do I watch antenna TV with HbbTV, do I turn the TV on via Cast, do I live in the UK).
   Also ask whether I want to restrict background work of any apps — show me the installed ones.
g) Telemetry: tools/telemetry.ps1, then tell me what to switch off by hand in the TV menu
   (Google "Usage & diagnostics", Sony consents).
h) Finally a short summary: what changed, what stayed, where the undo files are
   and how to get back to the stock home screen.

If package names differ from the ones in the scripts (e.g. a newer Google TV model), don't guess:
show me a list of candidates with what each one does, and disable only the ones I approve.
```

---

**No internet after the TV wakes up?** That's a separate issue with a separate prompt:
[WIFI-WATCHDOG.md](WIFI-WATCHDOG.md).
