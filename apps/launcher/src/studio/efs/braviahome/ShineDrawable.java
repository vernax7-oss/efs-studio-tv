package studio.efs.braviahome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** © EFS Studio — the white focus frame with a soft band of light that sweeps across the tile once. */
final class ShineDrawable extends Drawable {

    private final float radius;
    private final float strokeW;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private float progress = 1f;   // 0..1 sweeps, 1 = finished

    ShineDrawable(float radius, float strokeW) {
        this.radius = radius;
        this.strokeW = strokeW;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(strokeW);
        ring.setColor(0xFFFFFFFF);
    }

    void setProgress(float p) {
        progress = p;
        invalidateSelf();
    }

    @Override public void draw(Canvas c) {
        RectF r = new RectF(getBounds());
        r.inset(strokeW / 2f, strokeW / 2f);
        if (progress < 1f) {
            float w = r.width(), h = r.height();
            float bw = w * 0.32f;
            float x = r.left - bw + progress * (w + bw * 2f);
            band.setShader(new LinearGradient(x, 0, x + bw, 0,
                    new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            clip.reset();
            clip.addRoundRect(r, radius, radius, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            c.rotate(-18f, r.centerX(), r.centerY());
            c.drawRect(x, r.top - h, x + bw, r.bottom + h, band);
            c.restore();
        }
        c.drawRoundRect(r, radius, radius, ring);
    }

    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter f) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
