# Optional: Wi-Fi watchdog ("Connected, no internet" after waking up)

© EFS Studio

**This is separate from the launcher. Install it only if you have this problem.**

## The problem

Some Sony Android TVs (and other Android TVs) show **"Connected, no internet"** after waking up
from standby. Toggling Wi-Fi in the menu sometimes helps, sometimes only a restart does.
It's a common complaint on forums, often after a firmware update; replacing the Wi-Fi module
usually doesn't help. On the tested Sony (KD-55XG9505, Android 10) the Wi-Fi module sits on
the USB bus (MediaTek MT7663) and does not always come back cleanly after standby.

## Check these first (often enough on their own)

1. **Old saved networks.** The TV may jump to a network it remembers from somewhere else —
   e.g. one with a static IP from another home — and then has "no internet".
   Remove networks you don't use: Settings → Network → saved networks.
   On Android 10, if the menu doesn't allow it: `tools/forget-network.ps1 -Dev <IP>:5555 -List`
   and then `-Ids ...`.
2. **The router's scheduled restart.** Many routers reboot at night (e.g. at 6 or 7 am). If the TV
   is in standby at that time, it can lose the network. That's normal — the watchdog won't help with
   that and doesn't try (it only works while the screen is on).
3. **Firmware updates** for the TV and the router.

## What the watchdog does

`apps/wifi-watchdog` is a tiny app (≈16 KB) with no interface of its own beyond a status screen:

- starts by itself when the TV boots,
- **only while the screen is on**, checks every 20 s whether the network really has internet
  (Android's `VALIDATED` flag),
- after 2 failed checks in a row it turns Wi-Fi off and on again — exactly what you'd do by hand,
- at most **3 resets per screen-on**, so it never loops when the internet is genuinely down,
- writes a log: `/sdcard/Android/data/studio.efs.wifiwatchdog/files/watchdog.log`
  (the app's screen shows the latest entries).

It needs `targetSdk 28` (that's why it is a separate app) — Android only lets apps targeting API 28
or lower switch Wi-Fi on and off.

## Install by hand

```powershell
.\apps\wifi-watchdog\build.ps1
adb -s 192.168.1.50:5555 install -r apps\wifi-watchdog\build\app.apk
# open it once — Android doesn't start an app at boot until it has been launched at least once
adb -s 192.168.1.50:5555 shell am start -n studio.efs.wifiwatchdog/.MainActivity
```

Read the log:

```powershell
adb -s 192.168.1.50:5555 shell cat /sdcard/Android/data/studio.efs.wifiwatchdog/files/watchdog.log
```

Remove it: `adb uninstall studio.efs.wifiwatchdog`.

---

## Prompt for Claude Code (Wi-Fi after waking up)

Open the repository folder in [Claude Code](https://claude.com/claude-code), make sure ADB works
([INSTALL.md](INSTALL.md), steps 1–2), put in your IP and paste:

```text
My TV: <brand and model>. IP address: 192.168.1.50 (change it)
Problem: after waking up from standby the TV often shows "Connected, no internet".

Help me with this using docs/WIFI-WATCHDOG.md from this repository. Talk to me in the language I write in.
Diagnose first, install the watchdog only if it makes sense.

Rules:
1. Read-only checks need no approval. Before any change (removing a network, installing an app,
   changing a setting) tell me what and why, and wait for my approval.
2. Never send data from my TV anywhere. Network names, addresses and logs stay on my computer
   (local/ folder, which is git-ignored).
3. Don't change the router's settings yourself — if something there needs changing, tell me what.

Order:
a) Connect (adb connect <IP>:5555), read the model and Android version.
b) Read the network state: saved networks (dumpsys wifi | grep -E '^ID: [0-9]+ SSID'),
   the current connection, signal strength, IP and DNS, whether the network is VALIDATED
   (dumpsys connectivity), standby and wake-up history if available.
c) Look for causes: old or foreign saved networks (especially with a static IP), weak signal,
   2.4 vs 5 GHz, a router that restarts on a schedule. Tell me what you found.
d) If there are unused saved networks — propose removing them (on Android 10:
   tools/forget-network.ps1).
e) If the problem can be the TV's Wi-Fi not recovering after standby — propose the watchdog:
   build apps/wifi-watchdog (build.ps1), install it, launch it once (am start) and check
   that the log was created.
f) Tell me how to check later whether it helped (where the log is and what the entries mean)
   and how to remove the watchdog.
```
