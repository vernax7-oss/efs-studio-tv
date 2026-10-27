/*
 * ImageLoader — minimalny loader plakatow (bez zadnych bibliotek).
 * © EFS Studio
 *
 * Kolejnosc dzialania:
 *   1. cache w pamieci,
 *   2. plik pobrany wczesniej przez systemowy DownloadManager,
 *   3. bezposrednie polaczenie (na tym telewizorze blokowane: EPERM),
 *   4. DownloadManager — pobiera inny proces systemowy, wiec firewall aplikacji go nie dotyczy.
 * Dzieki temu plakaty dociagaja sie same i zmieniaja razem z danymi z Watch Next.
 */
package studio.efs.braviahome;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final ImageLoader INSTANCE = new ImageLoader();

    public static ImageLoader get() { return INSTANCE; }

    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private final Handler main = new Handler(Looper.getMainLooper());
    private Context appContext;

    public void init(Context ctx) {
        appContext = ctx.getApplicationContext();
    }

    public void load(final String url, final ImageView target) {
        if (url == null || url.isEmpty()) return;
        Bitmap cached = cache.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        target.setTag(url);
        pool.execute(new Runnable() {
            @Override public void run() {
                Bitmap bmp = fromDisk(url);
                if (bmp == null) bmp = fetch(url);
                if (bmp == null && appContext != null) bmp = viaDownloadManager(url);
                if (bmp == null) return;
                final Bitmap ready = bmp;
                cache.put(url, ready);
                main.post(new Runnable() {
                    @Override public void run() {
                        if (url.equals(target.getTag())) target.setImageBitmap(ready);
                    }
                });
            }
        });
    }


    /** Podglad bitmapy (np. do rozmycia tla) — ten sam potok co dla kafelkow. */
    public interface BitmapCallback { void onBitmap(Bitmap bitmap); }

    public void loadBitmap(final String url, final BitmapCallback callback) {
        if (url == null || url.isEmpty()) return;
        Bitmap cached = cache.get(url);
        if (cached != null) { callback.onBitmap(cached); return; }
        pool.execute(new Runnable() {
            @Override public void run() {
                Bitmap bmp = fromDisk(url);
                if (bmp == null) bmp = fetch(url);
                if (bmp == null && appContext != null) bmp = viaDownloadManager(url);
                if (bmp == null) return;
                final Bitmap ready = bmp;
                cache.put(url, ready);
                main.post(new Runnable() {
                    @Override public void run() { callback.onBitmap(ready); }
                });
            }
        });
    }
    private File posterFile(String url) {
        String name = "p_" + Integer.toHexString(url.hashCode()) + ".jpg";
        File dir = new File(appContext.getExternalFilesDir(null), "posters");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, name);
    }

    private Bitmap fromDisk(String url) {
        try {
            if (appContext == null) return null;
            File f = posterFile(url);
            if (f.exists() && f.length() > 1000) return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception ignored) { }
        return null;
    }

    private Bitmap fetch(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            InputStream in = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        } catch (Exception e) {
            android.util.Log.i("EFSHome", "siec bezposrednio niedostepna (" + e.getMessage() + "), probuje DownloadManager");
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Pobranie przez systemowy DownloadManager: inny proces, wiec firewall aplikacji go nie blokuje. */
    private Bitmap viaDownloadManager(String url) {
        try {
            DownloadManager dm = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) return null;
            File target = posterFile(url);
            String relative = "posters/" + target.getName();

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setDestinationInExternalFilesDir(appContext, null, relative);
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            long id = dm.enqueue(req);

            for (int i = 0; i < 80; i++) {
                Thread.sleep(250);
                Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
                if (c == null) continue;
                boolean moved = c.moveToFirst();
                int status = moved ? c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)) : -1;
                c.close();
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    return BitmapFactory.decodeFile(target.getAbsolutePath());
                }
                if (status == DownloadManager.STATUS_FAILED) {
                    android.util.Log.e("EFSHome", "DownloadManager nie pobral: " + url);
                    return null;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "DownloadManager blad", e);
        }
        return null;
    }
}

