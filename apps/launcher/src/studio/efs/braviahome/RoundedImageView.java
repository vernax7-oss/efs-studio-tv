/*
 * RoundedImageView — obrazek przycięty do zaokrąglonego prostokąta (lub koła).
 * © EFS Studio
 *
 * clipToOutline na ImageView z tłem <shape> nie przycinał bitmapy na tym urządzeniu,
 * dlatego tniemy ścieżką. Dodatkowo obsługuje powiększenie treści (efekt Ken Burns):
 * przycinanie liczone jest w współrzędnych widoku, więc rogi zawsze zostają zaokrąglone,
 * a powiększony obraz nie wychodzi poza ramkę.
 */
package studio.efs.braviahome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RoundedImageView extends ImageView {

    private final Path path = new Path();
    private final RectF rect = new RectF();
    private float radius = 0f;
    private float contentScale = 1f;

    public RoundedImageView(Context context) {
        super(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCornerRadius(float r) {
        radius = r;
        applyOutline();
        invalidate();
    }

    /** Tnie takze przez outline — na tym TV clipPath bywa ignorowany. */
    private void applyOutline() {
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(android.view.View v, android.graphics.Outline o) {
                if (radius > 0f) o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        setClipToOutline(radius > 0f);
    }

    /** Powiększenie rysowanej treści (bez zmiany rozmiaru widoku). */
    public void setContentScale(float s) {
        contentScale = s;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyOutline();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (radius <= 0f && contentScale == 1f) {
            super.onDraw(canvas);
            return;
        }
        rect.set(0, 0, getWidth(), getHeight());
        path.reset();
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(path);
        if (contentScale != 1f) {
            canvas.scale(contentScale, contentScale, getWidth() / 2f, getHeight() / 2f);
        }
        super.onDraw(canvas);
        canvas.restore();
    }
}

