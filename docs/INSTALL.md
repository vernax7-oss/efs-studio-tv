# Step-by-step install (manual)

© EFS Studio

These steps are for a **Sony TV with Android TV**. Another brand? See [OTHER-BRANDS.md](OTHER-BRANDS.md) —
steps 1–5 are the same, only the stock home screen name and the package lists differ.

Want Claude to do it for you? Use [PROMPT-SONY.md](PROMPT-SONY.md).

In the examples the TV's address is `192.168.1.50` — use yours.

---

## 0. What you need

- A Windows PC on the same network as the TV.
- **Android SDK Platform-Tools** (`adb`) — [download](https://developer.android.com/tools/releases/platform-tools).
- To build the APKs: **JDK 17** and the **Android SDK** with build-tools and platform android-29+
  (easiest through Android Studio or `sdkmanager`).

## 1. Turn on debugging on the TV

1. Settings → **Device Preferences** → **About** → click **Build** 7 times.
   A message says you are now a developer.
2. Settings → Device Preferences → **Developer options** → turn on **USB debugging**.
   On Sony this is enough for ADB over the network. On Google TV also turn on **Wireless debugging**.
3. Find the TV's IP address: Settings → Network & Internet → your network.

## 2. Connect

```powershell
adb connect 192.168.1.50:5555
```

The TV asks for permission — tick **Always allow from this computer** and confirm.
Check: `adb devices` should list the TV as `device`.

## 3. Take a snapshot ("before" copy)

```powershell
.\tools\snapshot.ps1 -Dev 192.168.1.50:5555
```

It goes to `local\state-before-<date>` (git-ignored — it contains your network names).
Keep it: it's your reference point if you ever need to restore something.

## 4. Build and install the launcher

```powershell
.\apps\launcher\build.ps1
adb -s 192.168.1.50:5555 install -r apps\launcher\build\app.apk
```

Check that it starts (the new home screen should appear on the TV):

```powershell
adb -s 192.168.1.50:5555 shell am start -n studio.efs.braviahome/.MainActivity
```

**Don't continue if the launcher did not open.**

## 5. Make it the home screen

Android TV on Sony ignores the home screen choice while the stock launcher is enabled,
so we disable it (reversibly):

```powershell
# Android TV (Sony 2015–2020 and most other brands)
adb -s 192.168.1.50:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
```

Press **HOME** on the remote — EFS Studio TV should appear.

> Google TV (Sony 2021+, Chromecast with Google TV): the stock launcher is `com.google.android.apps.tv.launcherx`.
> Not sure what yours is called? Check:
> `adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME`

**Back to the stock home screen:** `adb shell pm enable com.google.android.tvlauncher`

## 6. Optimization (Sony)

```powershell
.\tools\optimize.ps1 -Dev 192.168.1.50:5555
```

By default it only disables safe things: hotel mode, demos, promotions, help. Options:

| Switch | What it disables | Don't use it if |
|---|---|---|
| `-NoHbbTV` | HbbTV, the red button | you watch antenna TV and use HbbTV |
| `-OtherRegions` | YouView and operator features | you live in the UK |
| `-NoRemoteStart` | turning the TV on over the network | you turn the TV on from your phone (Cast, Sony app) |
| `-NoReminders` | programme reminders | you set reminders in the TV guide |
| `-RestrictBackground pkg1,pkg2` | background work of the given apps | — (the script won't restrict the Play Store, Cast or the TV app) |

## 7. Telemetry

```powershell
.\tools\telemetry.ps1 -Dev 192.168.1.50:5555
```

Then by hand in the TV menu:
- **Google:** Settings → Privacy → **Usage & diagnostics** → off.
- **Sony:** Settings → About / Privacy → **Sony Privacy Policy** → withdraw data collection consents.

## 8. Check that everything works

- HOME on the remote → EFS Studio TV.
- An HDMI input and antenna TV — the picture is there.
- The Play Store opens.
- On your phone: the Cast icon in YouTube sees the TV.

## Undoing changes

Every script writes undo commands to `local\undo-*.txt`. Open the file and run the commands one by one
(set `$adb = "path\to\adb.exe"` first). The most important manual commands:

```powershell
adb shell pm enable com.google.android.tvlauncher        # stock home screen
adb uninstall studio.efs.braviahome                       # remove the launcher (enable the stock one first!)
adb shell pm enable <package.name>                        # any disabled package
```

A factory reset also restores everything (but it also removes your apps and sign-ins).

## Troubleshooting

| Symptom | What to do |
|---|---|
| `adb connect` — `failed to authenticate` | Confirm the permission prompt on the TV; if there is none, turn USB debugging off and on. |
| HOME still opens the old screen | The stock launcher is still enabled — step 5. Some models have a second vendor launcher — check with the command in step 5. |
| HDMI shows a black screen | Something disabled the TV app: `adb shell pm enable com.sony.dtv.tvx`. |
| `install` — `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | The TV has a version signed with a different key. Build with the same key as before (`keystore.properties`). Uninstalling the launcher is only safe after re-enabling the stock one (step 5). |
| Animations "jump" | The TV has system animations turned off (Developer options → animation scale). The Arc layout has its own animation and stays smooth anyway. |
