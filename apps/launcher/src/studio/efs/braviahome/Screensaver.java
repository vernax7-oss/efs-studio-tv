package studio.efs.braviahome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** © EFS Studio — idle slideshow: photos fade into each other, drift slowly, and a small clock wanders between corners. */
class Screensaver extends FrameLayout {

    private static final long SLIDE_MS = 14000;
    private static final long FADE_MS = 1800;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> photos = new ArrayList<>();
    private final ImageView[] layers = new ImageView[2];
    private final TextView clock;
    private int next;
    private int top;
    private boolean running;
    private Tween motion;

    Screensaver(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        setVisibility(View.GONE);
        for (int i = 0; i < 2; i++) {
            layers[i] = new ImageView(c);
            layers[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            layers[i].setAlpha(0f);
            addView(layers[i], new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        clock = new TextView(c);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(64);
        clock.setShadowLayer(14f, 0f, 2f, 0xCC000000);
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-thin", android.graphics.Typeface.NORMAL));
        addView(clock, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        try {
            for (String n : c.getAssets().list("")) {
                if (n.endsWith(".jpg") && (n.startsWith("bg_") || n.startsWith("saver_"))) photos.add(n);
            }
        } catch (Exception ignored) { }
        Collections.shuffle(photos);
    }

    boolean showing() { return running; }

    void start() {
        if (running || photos.isEmpty()) return;
        running = true;
        setVisibility(View.VISIBLE);
        slide();
    }

    void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (motion != null) motion.cancel();
        setVisibility(View.GONE);
    }

    private void slide() {
        if (!running) return;
        Bitmap bmp = load(photos.get(next++ % photos.size()));
        if (bmp != null) {
            final ImageView in = layers[top ^= 1];
            final ImageView out = layers[top ^ 1];
            in.setImageBitmap(bmp);
            in.setScaleX(1f);
            in.setScaleY(1f);
            if (motion != null) motion.cancel();
            final float fadeShare = FADE_MS / (float) (SLIDE_MS + FADE_MS);
            // one frame-driven motion for the whole photo: fade in, fade the old one out, drift
            motion = new Tween(this, SLIDE_MS + FADE_MS, false, new Tween.Step() {
                @Override public void at(float t) {
                    float f = Math.min(1f, t / fadeShare);
                    in.setAlpha(f);
                    out.setAlpha(1f - f);
                    in.setScaleX(1f + 0.12f * t);
                    in.setScaleY(1f + 0.12f * t);
                }
            }, null).start();
            placeClock();
        }
        handler.postDelayed(new Runnable() { @Override public void run() { slide(); } }, SLIDE_MS);
    }

    /** The clock hops to another corner with each photo, so nothing stays lit in one place. */
    private void placeClock() {
        clock.setText(android.text.format.DateFormat.getTimeFormat(getContext()).format(new Date()));
        LayoutParams lp = (LayoutParams) clock.getLayoutParams();
        int m = (int) (48 * getResources().getDisplayMetrics().density);
        int[] g = {Gravity.BOTTOM | Gravity.START, Gravity.BOTTOM | Gravity.END,
                Gravity.TOP | Gravity.END, Gravity.TOP | Gravity.START};
        lp.gravity = g[next % g.length];
        lp.setMargins(m, m, m, m);
        clock.setLayoutParams(lp);
    }

    private Bitmap load(String name) {
        try (InputStream in = getContext().getAssets().open(name)) {
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        }
    }
}
