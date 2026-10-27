/*
 * MainActivity — starts the watchdog and shows the latest entries of its log.
 * © EFS Studio
 *
 * Open it once after installing (from the app list or via ADB):
 * Android does not deliver BOOT_COMPLETED to an app that has never been launched.
 */
package studio.efs.wifiwatchdog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;

public class MainActivity extends Activity {

    private static final int LOG_LINES = 30;

    static void startWatchdog(Context context) {
        Intent svc = new Intent(context, WatchdogService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(svc);
        else context.startService(svc);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startWatchdog(this);

        int pad = Math.round(getResources().getDisplayMetrics().density * 32);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, pad);
        box.setBackgroundColor(0xFF101216);

        TextView title = new TextView(this);
        title.setText(R.string.wd_running);
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText(R.string.wd_info);
        info.setTextSize(15);
        info.setTextColor(0xCCFFFFFF);
        info.setPadding(0, pad / 3, 0, pad / 2);
        box.addView(info);

        TextView log = new TextView(this);
        log.setText(readLogTail());
        log.setTextSize(13);
        log.setTypeface(android.graphics.Typeface.MONOSPACE);
        log.setTextColor(0xFFB8C4D6);
        log.setGravity(Gravity.START);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(log);
        box.addView(scroll);

        setContentView(box);
    }

    private String readLogTail() {
        File f = new File(getExternalFilesDir(null), WatchdogService.LOG_NAME);
        if (!f.exists()) return getString(R.string.wd_log_empty);
        ArrayDeque<String> tail = new ArrayDeque<String>();
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;
            while ((line = r.readLine()) != null) {
                tail.addLast(line);
                if (tail.size() > LOG_LINES) tail.removeFirst();
            }
            r.close();
        } catch (Exception e) {
            return getString(R.string.wd_log_error, e.getMessage());
        }
        StringBuilder sb = new StringBuilder(getString(R.string.wd_log_latest)).append('\n');
        for (String l : tail) sb.append(l).append('\n');
        return sb.toString();
    }
}
