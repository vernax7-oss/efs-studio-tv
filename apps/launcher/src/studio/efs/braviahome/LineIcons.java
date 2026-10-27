/*
 * LineIcons — z ikony aplikacji robi biały kontur (jedna linia, bez kolorów).
 * © EFS Studio
 *
 * Ikona jest rysowana do bitmapy, potem liczymy krawędzie (Sobel na jasności)
 * i zostawiamy tylko je — białe, na przezroczystym tle. Wynik jest cache'owany,
 * bo liczenie odbywa się raz na aplikację i rozmiar.
 */
package studio.efs.braviahome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.util.HashMap;

public final class LineIcons {

    private static final HashMap<String, Bitmap> CACHE = new HashMap<String, Bitmap>();
    private static final int THRESHOLD = 42;
    // Kontur liczymy w większej rozdzielczości i pomniejszamy: Sobel daje linię ~2 px,
    // po pomniejszeniu wychodzi ~1,2 px — cienka, wygładzona, nie zlewa się na literach.
    private static final float OVERSAMPLE = 1.65f;

    private LineIcons() { }

    /** Biały kontur ikony w rozmiarze sizePx (tak, jak będzie wyświetlona). key = nazwa pakietu. */
    public static Bitmap get(Context context, String key, Drawable icon, int sizePx) {
        if (icon == null || sizePx <= 0) return null;
        String cacheKey = key + "@" + sizePx;
        Bitmap cached = CACHE.get(cacheKey);
        if (cached != null) return cached;

        Bitmap big = render(context, icon, Math.round(sizePx * OVERSAMPLE));
        Bitmap out = Bitmap.createScaledBitmap(big, sizePx, sizePx, true);
        if (out != big) big.recycle();
        CACHE.put(cacheKey, out);
        return out;
    }

    private static Bitmap render(Context context, Drawable icon, int sizePx) {
        Bitmap src = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(src);
        Drawable copy = icon.getConstantState() != null
                ? icon.getConstantState().newDrawable(context.getResources())
                : icon;
        // margines, żeby kształt dotykający brzegu (np. koło) miał krawędź także na brzegu bitmapy
        int pad = Math.max(2, sizePx / 48);
        copy.setBounds(pad, pad, sizePx - pad, sizePx - pad);
        copy.draw(canvas);

        Bitmap out = edges(src, sizePx);
        src.recycle();
        return out;
    }

    /** Sobel na jasności: zostają same krawędzie, białe. */
    private static Bitmap edges(Bitmap src, int n) {
        int[] pix = new int[n * n];
        src.getPixels(pix, 0, n, 0, 0, n, n);

        int transparent = 0;
        for (int p : pix) if (((p >>> 24) & 0xff) < 16) transparent++;
        boolean useAlpha = transparent > pix.length / 5;   // ikona ma przezroczyste tło

        // Ikona na pełnym tle (np. zaokrąglony kwadrat): kładziemy ją na jej własny kolor tła,
        // żeby obrys samego kafelka nie dawał krawędzi — inaczej w rogach zostają "nawiasy".
        int bg = useAlpha ? 0 : backgroundColor(pix, n);
        float bgR = (bg >> 16) & 0xff, bgG = (bg >> 8) & 0xff, bgB = bg & 0xff;

        float[] lum = new float[n * n];
        for (int i = 0; i < pix.length; i++) {
            int p = pix[i];
            int a = (p >>> 24) & 0xff;
            if (useAlpha) {
                lum[i] = a;                               // kontur z kształtu (alfa)
                continue;
            }
            float k = a / 255f;
            float r = ((p >> 16) & 0xff) * k + bgR * (1f - k);
            float g = ((p >> 8) & 0xff) * k + bgG * (1f - k);
            float b = (p & 0xff) * k + bgB * (1f - k);
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b;
        }

        Bitmap out = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        int[] res = new int[n * n];
        for (int y = 1; y < n - 1; y++) {
            for (int x = 1; x < n - 1; x++) {
                int i = y * n + x;
                float gx = -lum[i - n - 1] - 2 * lum[i - 1] - lum[i + n - 1]
                        + lum[i - n + 1] + 2 * lum[i + 1] + lum[i + n + 1];
                float gy = -lum[i - n - 1] - 2 * lum[i - n] - lum[i - n + 1]
                        + lum[i + n - 1] + 2 * lum[i + n] + lum[i + n + 1];
                float mag = (float) Math.sqrt(gx * gx + gy * gy);
                if (mag < THRESHOLD) continue;
                int alpha = (int) Math.min(255f, (mag - THRESHOLD) * 2.6f);
                res[i] = (alpha << 24) | 0x00FFFFFF;
            }
        }
        out.setPixels(res, 0, n, 0, 0, n, n);
        return out;
    }

    /**
     * Kolor tła ikony: najczęstszy nieprzezroczysty kolor na pierścieniu tuż przy brzegu
     * (kolory zgrubnie zaokrąglone, żeby drobne różnice i wygładzanie się nie liczyły).
     */
    private static int backgroundColor(int[] pix, int n) {
        int inset = Math.max(3, n / 12);
        HashMap<Integer, Integer> votes = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> sample = new HashMap<Integer, Integer>();
        for (int t = inset; t < n - inset; t++) {
            int[] ring = {
                    inset * n + t, (n - 1 - inset) * n + t,   // góra, dół
                    t * n + inset, t * n + (n - 1 - inset)    // lewo, prawo
            };
            for (int i : ring) {
                int p = pix[i];
                if (((p >>> 24) & 0xff) < 200) continue;
                int key = (p & 0xF0F0F0);
                Integer c = votes.get(key);
                votes.put(key, c == null ? 1 : c + 1);
                if (!sample.containsKey(key)) sample.put(key, p);
            }
        }
        int best = 0, bestKey = 0;
        for (java.util.Map.Entry<Integer, Integer> e : votes.entrySet()) {
            if (e.getValue() > best) { best = e.getValue(); bestKey = e.getKey(); }
        }
        return best == 0 ? 0 : sample.get(bestKey);
    }
}

