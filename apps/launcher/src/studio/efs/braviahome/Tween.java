package studio.efs.braviahome;

import android.os.SystemClock;
import android.view.View;

/**
 * © EFS Studio — a frame-by-frame animation driven by the clock. The TV may have system animations
 * turned off (animator scale 0), which silently skips every ValueAnimator and ViewPropertyAnimator;
 * this one always runs, like the Arc carousel's own motion.
 */
final class Tween implements Runnable {

    interface Step { void at(float t); }   // t goes 0..1

    private final View host;
    private final long ms;
    private final boolean pingPong;
    private final Step step;
    private final Runnable end;
    private long start;
    private boolean cancelled;

    Tween(View host, long ms, boolean pingPong, Step step, Runnable end) {
        this.host = host;
        this.ms = ms;
        this.pingPong = pingPong;
        this.step = step;
        this.end = end;
    }

    Tween start() {
        start = SystemClock.uptimeMillis();
        host.postOnAnimation(this);
        return this;
    }

    void cancel() { cancelled = true; }

    @Override public void run() {
        if (cancelled) return;
        float t = (SystemClock.uptimeMillis() - start) / (float) ms;
        if (pingPong) {
            float c = t % 2f;
            step.at(c <= 1f ? c : 2f - c);
            host.postOnAnimation(this);
        } else if (t >= 1f) {
            step.at(1f);
            if (end != null) end.run();
        } else {
            step.at(t);
            host.postOnAnimation(this);
        }
    }

    static float easeOut(float t) { return 1f - (1f - t) * (1f - t) * (1f - t); }

    /** Overshoots past 1 and settles back — the springy "pop". */
    static float easeOutBack(float t) {
        float c1 = 2.2f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
}
