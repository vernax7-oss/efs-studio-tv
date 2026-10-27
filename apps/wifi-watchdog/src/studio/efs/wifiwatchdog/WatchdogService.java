/*
 * WatchdogService — keeps the internet alive after the TV wakes up.
 * © EFS Studio
 *
 * Runs as a foreground service (survives longer than a normal process).
 * Every 20 s it checks whether the active network has validated internet
 * (NetworkCapabilities.VALIDATED). After two failed checks it restarts the Wi-Fi radio —
 * the same thing you would do by hand when the TV says "Connected, no internet" after standby.
 *
 * Only active while the screen is on: in standby the TV drops the network on its own,
 * and many routers reboot at night — resetting the radio then achieves nothing.
 * At most MAX_RESETS resets per screen-on session, so it never loops when the internet
 * is simply down (e.g. an ISP outage).
 */
package studio.efs.wifiwatchdog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WatchdogService extends Service {

    static final String LOG_NAME = "watchdog.log";

    private static final String CHANNEL_ID = "efs_wifi_watchdog";
    private static final int NOTIFICATION_ID = 4242;
    private static final int INTERVAL_MS = 20000;
    private static final int FAILS_BEFORE_RESET = 2;
    private static final int MAX_RESETS = 3;
    private static final long LOG_MAX_BYTES = 256 * 1024;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
    private ConnectivityManager cm;
    private PowerManager pm;
    private NetworkCallbackImpl callback;
    private int failures = 0;
    private int resets = 0;
    private boolean lastValidated = true;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            check();
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    /** Screen on = a new session: reset the counters and start checking again. */
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                log("screen on");
                failures = 0;
                resets = 0;
                handler.removeCallbacks(tick);
                handler.postDelayed(tick, INTERVAL_MS);
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                log("screen off - paused");
                handler.removeCallbacks(tick);
            }
        }
    };

    private class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        @Override public void onLost(Network network) {
            log("network lost");
        }
        @Override public void onAvailable(Network network) {
            log("network available");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= 24) {   // registerDefaultNetworkCallback exists from Android 7
            try {
                callback = new NetworkCallbackImpl();
                cm.registerDefaultNetworkCallback(callback);
            } catch (Exception e) {
                log("cannot register network callback: " + e.getMessage());
            }
        }
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen, f);
        log("watchdog started");
        handler.post(tick);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        try { unregisterReceiver(screen); } catch (Exception ignored) { }
        try {
            if (cm != null && callback != null) cm.unregisterNetworkCallback(callback);
        } catch (Exception ignored) { }
        log("watchdog stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void check() {
        if (pm != null && !pm.isInteractive()) return;   // standby: do nothing

        boolean ok;
        try {
            Network active = cm.getActiveNetwork();
            NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
            ok = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            ok = false;
        }

        if (ok) {
            if (!lastValidated) log("internet is back");
            lastValidated = true;
            failures = 0;
            return;
        }

        lastValidated = false;
        failures++;
        log("no internet (check " + failures + "/" + FAILS_BEFORE_RESET + ")");
        if (failures < FAILS_BEFORE_RESET) return;
        failures = 0;
        if (resets >= MAX_RESETS) {
            log("reset limit (" + MAX_RESETS + ") reached - waiting for the next screen-on");
            return;
        }
        resets++;
        resetWifi();
    }

    private void resetWifi() {
        try {
            final WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            log("ACTION: Wi-Fi radio reset (" + resets + "/" + MAX_RESETS + ")");
            wm.setWifiEnabled(false);
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    try {
                        wm.setWifiEnabled(true);
                        log("radio back on");
                    } catch (Exception e) {
                        log("cannot enable Wi-Fi: " + e.getMessage());
                    }
                }
            }, 6000);
        } catch (Exception e) {
            log("reset failed: " + e.getMessage());
        }
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, getString(R.string.wd_channel),
                    NotificationManager.IMPORTANCE_MIN);
            ch.setDescription(getString(R.string.wd_channel_desc));
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return b.setContentTitle("EFS Wi-Fi Watchdog")
                .setContentText(getString(R.string.wd_notification))
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true)
                .build();
    }

    private void log(String message) {
        try {
            File f = new File(getExternalFilesDir(null), LOG_NAME);
            if (f.length() > LOG_MAX_BYTES) {
                File old = new File(f.getParentFile(), LOG_NAME + ".1");
                old.delete();
                f.renameTo(old);
            }
            FileWriter w = new FileWriter(f, true);
            w.write(fmt.format(new Date()) + " " + message + "\n");
            w.close();
        } catch (Exception ignored) { }
    }
}
