/*
 * EFS Studio TV — ekran główny dla Sony BRAVIA (Android TV 10).
 * © EFS Studio
 *
 * Wersja 0.4:
 *  - jeden rząd „Moje aplikacje" — wszystkie zainstalowane, w kolejności właściciela,
 *  - długie przytrzymanie kafla: strzałki ◀ ▶ i przesuwanie kolejności (zapis w preferencjach),
 *  - kafelki w szklanej ramce (półprzezroczyste tło + jasna obwódka), minimalnie mniejsze,
 *  - podgląd w bohaterze delikatnie „oddycha" (Ken Burns), jak ruchome obrazy w Google TV,
 *  - siatka aplikacji wyśrodkowana.
 */
package studio.efs.braviahome;

import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int SCREEN_YOU = 0;
    private static final int SCREEN_SETTINGS = 2;

    private static final String PREFS = "efs_home_v5";
    private static final String PREF_ORDER = "order";
    private static final String PREF_STYLE = "style";
    private static final String STYLE_GLASS = "glass";
    private static final String STYLE_SHARP = "sharp";

    /** Aplikacje, których nie pokazujemy. */
    private static final Set<String> HIDDEN = new HashSet<>(Arrays.asList(
            "com.sony.dtv.tvx"          // wejście TV / antena — nieużywane
    ));

    static final int[] TILE_COLORS = {
            0xFFD7263D, 0xFF1B6CA8, 0xFF7B2FF7, 0xFF0E9594,
            0xFFE36414, 0xFF3D5A80, 0xFF9B287B, 0xFF2A9D8F,
            0xFFB5179E, 0xFF4361EE, 0xFFD62828, 0xFF5F0F40,
    };

    private ImageView backdrop;
    private Look look;
    private LinearLayout homeHost;
    private TextView clock, clockDate, tabYou, tabSettings;
    private View screenHome, screenSettings;
    private LinearLayout rowCards, rowContinue, settingsList;
    private View continueTitle, continueScroll;
    final List<ContentEntry> continueItems = new ArrayList<>();
    LinearLayout homeContinueRow;      // rząd "Kontynuuj" budowany przez HomeLayouts
    View homeContinueTitle;            // jego tytuł

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final List<AppEntry> apps = new ArrayList<>();
    final List<AppEntry> orderedApps = new ArrayList<>();
    private final Map<String, Drawable> bannerCache = new HashMap<>();
    private final Map<String, Bitmap> backdropCache = new HashMap<>();

    private int editingIndex = -1;
    private SharedPreferences prefs;
    private TextView rowTitle;
    private View overlayDim, overlayLeft, overlayBottom, gradientBg;
    private View clockHero;
    private TextView clockHeroTime, clockHeroDate;
    private Panel panel;
    private View panelView, panelDim;
    private LinearLayout panelRows, pickerBox;
    private boolean sharp;
    private String appVersion = "?";

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(timeFormat.format(new Date()));
            String dateText = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
            if (clockDate != null) clockDate.setText(dateText);
            if (clockHeroDate != null) clockHeroDate.setText(dateText);
            if (clockHeroTime != null) clockHeroTime.setText(timeFormat.format(new Date()));
            handler.postDelayed(this, 20000);
        }
    };

    static class AppEntry {
        String label;
        String packageName;
        ComponentName component;
        Drawable icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Look.loadNames(getResources());

        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) { }
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ImageLoader.get().init(this);

        backdrop = findViewById(R.id.backdrop);
        clock = findViewById(R.id.clock);
        clockDate = findViewById(R.id.clockDate);
        clockHero = findViewById(R.id.clockHero);
        clockHeroTime = findViewById(R.id.clockHeroTime);
        clockHeroDate = findViewById(R.id.clockHeroDate);
        tabYou = findViewById(R.id.tabYou);
        tabSettings = findViewById(R.id.tabSettings);
        screenHome = findViewById(R.id.screenHome);
        screenSettings = findViewById(R.id.screenSettings);
        rowCards = findViewById(R.id.rowCards);
        rowContinue = findViewById(R.id.rowContinue);
        continueTitle = findViewById(R.id.continueTitle);
        continueScroll = findViewById(R.id.continueScroll);
        settingsList = findViewById(R.id.settingsList);
        rowTitle = findViewById(R.id.rowTitle);
        homeHost = findViewById(R.id.homeHost);
        gradientBg = findViewById(R.id.gradientBg);
        panelView = findViewById(R.id.panel);
        panelDim = findViewById(R.id.panelDim);
        panelRows = findViewById(R.id.panelRows);
        pickerBox = findViewById(R.id.picker);
        overlayDim = findViewById(R.id.overlayDim);
        overlayLeft = findViewById(R.id.overlayLeft);
        overlayBottom = findViewById(R.id.overlayBottom);

        sharp = STYLE_SHARP.equals(prefs.getString(PREF_STYLE, STYLE_GLASS));
        look = Look.load(prefs);
        applyDebugLayout(getIntent());
        applyStyle();

        loadApps();
        applySavedOrder();
        buildAppRow(0);
        buildSettings();
        buildPanel();

        bindTab(tabYou, SCREEN_YOU);
        tabSettings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openPanel(); }
        });
        tabSettings.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                styleTabState(tabSettings, hasFocus, false);
            }
        });

        loadContinueWatching();

        showScreen(SCREEN_YOU);
        renderHome();
        handler.post(clockTick);
    }

    /** Podgląd układu z ADB: am start ... --es layout dock (do zdjęć i testów, bez wpływu na UI). */
    /** Jednorazowa migracja: po tej wersji startujemy z czystymi ustawieniami wyglądu. */
    private void migrateLook() {
        if (prefs.getInt("schema", 0) < 2) {
            look = new Look();
            look.save(prefs);
            prefs.edit().putInt("schema", 2).apply();
        }
    }

    private void applyReset(Intent intent) {
        if (intent != null && intent.getBooleanExtra("reset", false)) {
            look = new Look();
            look.save(prefs);
        }
    }

    private static boolean debugLayoutApplied = false;

    /** Podgląd/ustawianie wyglądu z ADB: --es layout grid --es bg bg_03 --es continue off itd. */
    private void applyDebugLayout(Intent intent) {
        if (intent == null) return;
        boolean changed = false;
        changed |= setIfPresent(intent, "layout", Look.LAYOUTS, 0);
        changed |= setIfPresent(intent, "bg", Look.BACKGROUNDS, 1);
        changed |= setIfPresent(intent, "tiles", Look.TILES, 2);
        changed |= setIfPresent(intent, "labels", Look.LABELS, 3);
        changed |= setIfPresent(intent, "logo", Look.LOGOS, 4);
        changed |= setIfPresent(intent, "font", Look.FONTS, 5);
        changed |= setIfPresent(intent, "clock", Look.CLOCKS, 6);
        changed |= setIfPresent(intent, "continue", Look.CONTINUE, 7);
        if (changed) {
            debugLayoutApplied = true;   // znacznik tylko informacyjny
            look.save(prefs);
        }
    }

    private boolean setIfPresent(Intent intent, String key, String[] allowed, int field) {
        String value = intent.getStringExtra(key);
        if (value == null) return false;
        boolean ok = false;
        for (String candidate : allowed) if (candidate.equals(value)) ok = true;
        if (!ok) return false;
        switch (field) {
            case 0: look.layout = value; break;
            case 1: look.background = value; break;
            case 2: look.tiles = value; break;
            case 3: look.labels = value; break;
            case 4: look.logo = value; break;
            case 5: look.font = value; break;
            case 6: look.clock = value; break;
            default: look.continueRow = value; break;
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyDebugLayout(intent);
        renderHome();
    }

    protected void onDestroy() {
        handler.removeCallbacks(clockTick);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Panel i lista wyboru obsługują pilota same, niezależnie od tego, gdzie wypadł fokus.
        if (panel != null && (panel.isOpen() || panel.isPickerOpen())
                && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (panel.isPickerOpen()) panel.closePicker(); else panel.close();
                return true;
            }
            if (panel.handleKey(event.getKeyCode())) return true;
        }

        // Przechwytujemy strzałki w trybie edycji PRZED nawigacją fokusu,
        // bo inaczej system przesuwa fokus zamiast zamieniać kafelki miejscami.
        if (editingIndex >= 0 && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int kc = event.getKeyCode();
            if (kc == KeyEvent.KEYCODE_DPAD_LEFT) { moveItem(-1); return true; }
            if (kc == KeyEvent.KEYCODE_DPAD_RIGHT) { moveItem(1); return true; }
            if (kc == KeyEvent.KEYCODE_BACK || kc == KeyEvent.KEYCODE_DPAD_CENTER
                    || kc == KeyEvent.KEYCODE_ENTER) { exitEdit(); return true; }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() {
        if (editingIndex >= 0) { exitEdit(); return; }
        if (panel != null && panel.isPickerOpen()) { panel.closePicker(); return; }
        if (panel != null && panel.isOpen()) { panel.close(); return; }
        /* ekran główny */
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Z pastylek u góry strzałka w dół zawsze schodzi do kafli bieżącego układu.
        View current = getCurrentFocus();
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && current != null) {
            int id = current.getId();
            if (id == R.id.tabYou || id == R.id.tabSettings) {
                focusContentFromTabs();
                return true;
            }
        }
        // Jeden przycisk do panelu wyglądu — niezależnie od tego, gdzie jest fokus.
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            openPanel();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (panel != null && panel.isPickerOpen()) { panel.closePicker(); return true; }
            if (panel != null && panel.isOpen()) { panel.close(); return true; }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    /** Unoszenie kafla: przesunięcie w górę + cień (bez skalowania, żeby nie ucinać sąsiadów). */
    void animateFocus(View v, boolean hasFocus) {
        v.animate()
                .translationY(hasFocus ? -dp(6) : 0)
                .translationZ(dp(hasFocus ? 18 : 0))
                .setDuration(140)
                .start();
    }

    // ---------- zakładki ----------

    /** Zakładka przełącza ekran dopiero po OK — samo przesunięcie fokusu nic nie zmienia. */
    private void bindTab(final TextView tab, final int screen) {
        tab.setFocusable(true);
        tab.setFocusableInTouchMode(true);
        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showScreen(screen); }
        });
        tab.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                styleTabState((TextView) v, hasFocus, screen == currentScreen);
            }
        });
    }

    /** Wygląd zakładki: aktywna jest biała, sfokusowana tym bardziej (żeby pilot było widać). */
    private void styleTabState(TextView tab, boolean focused, boolean active) {
        // tło pastylki ustawia selektor (state_focused); tu tylko kolor tekstu
        tab.setTextColor(focused ? Color.BLACK : (active ? Color.WHITE : 0xCCFFFFFF));
        tab.animate().scaleX(focused ? 1.06f : 1f).scaleY(focused ? 1.06f : 1f).setDuration(110).start();
    }

    private int currentScreen = SCREEN_YOU;

    void showScreen(int screen) {
        currentScreen = screen;
        screenHome.setVisibility(screen == SCREEN_YOU ? View.VISIBLE : View.GONE);
        screenSettings.setVisibility(screen == SCREEN_SETTINGS ? View.VISIBLE : View.GONE);
        boolean home = screen == SCREEN_YOU;
        backdrop.setVisibility(home ? View.VISIBLE : View.GONE);
        gradientBg.setVisibility(home ? View.GONE : View.VISIBLE);
        styleTab(tabYou, screen == SCREEN_YOU);
        styleTab(tabSettings, screen == SCREEN_SETTINGS);
    }

    private void styleTab(TextView tab, boolean active) {
        styleTabState(tab, tab.isFocused(), active);
    }

    // ---------- dane ----------

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> byPackage = new LinkedHashMap<>();

        Intent leanback = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (Intent intent : new Intent[]{leanback, launcher}) {
            List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
            if (resolved == null) continue;
            for (ResolveInfo ri : resolved) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (HIDDEN.contains(pkg)) continue;
                if (!byPackage.containsKey(pkg)) byPackage.put(pkg, ri);
            }
        }

        for (ResolveInfo ri : byPackage.values()) {
            AppEntry e = new AppEntry();
            e.packageName = ri.activityInfo.packageName;
            e.component = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            CharSequence label = ri.loadLabel(pm);
            e.label = label != null ? label.toString() : e.packageName;
            try {
                e.icon = ri.loadIcon(pm);
            } catch (Exception ex) {
                e.icon = getDrawable(R.drawable.ic_launcher);
            }
            apps.add(e);
        }

        final Collator collator = Collator.getInstance(new Locale("pl", "PL"));
        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                return collator.compare(a.label, b.label);
            }
        });
    }

    /** Kolejność: najpierw zapisana przez właściciela, potem reszta wg użycia. */
    private void applySavedOrder() {
        List<AppEntry> usageOrder = orderedByUsage();
        if (usageOrder.isEmpty()) usageOrder = new ArrayList<>(apps);

        orderedApps.clear();
        String saved = prefs.getString(PREF_ORDER, "");
        if (saved != null && !saved.isEmpty()) {
            for (String pkg : saved.split(",")) {
                for (AppEntry e : usageOrder) {
                    if (e.packageName.equals(pkg) && !orderedApps.contains(e)) {
                        orderedApps.add(e);
                        break;
                    }
                }
            }
        }
        for (AppEntry e : usageOrder) {
            if (!orderedApps.contains(e)) orderedApps.add(e);
        }
    }

    private void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (AppEntry e : orderedApps) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.packageName);
        }
        prefs.edit().putString(PREF_ORDER, sb.toString()).apply();
    }

    Drawable bannerOf(AppEntry app) {
        if (bannerCache.containsKey(app.packageName)) return bannerCache.get(app.packageName);
        Drawable banner = null;
        try {
            banner = getPackageManager().getApplicationBanner(app.packageName);
        } catch (Exception ignored) { }
        bannerCache.put(app.packageName, banner);
        return banner;
    }

    private List<AppEntry> orderedByUsage() {
        List<AppEntry> result = new ArrayList<>();
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 30L * 24 * 3600 * 1000, now);
            if (stats == null) return result;
            final Map<String, Long> usage = new HashMap<>();
            for (UsageStats s : stats) {
                Long prev = usage.get(s.getPackageName());
                usage.put(s.getPackageName(), (prev == null ? 0 : prev) + s.getTotalTimeInForeground());
            }
            List<AppEntry> sorted = new ArrayList<>(apps);
            Collections.sort(sorted, new Comparator<AppEntry>() {
                @Override public int compare(AppEntry a, AppEntry b) {
                    long x = usage.containsKey(a.packageName) ? usage.get(a.packageName) : 0;
                    long y = usage.containsKey(b.packageName) ? usage.get(b.packageName) : 0;
                    return Long.compare(y, x);
                }
            });
            result.addAll(sorted);
        } catch (Exception ignored) { }
        return result;
    }

    // ---------- rząd aplikacji ----------

    private void buildAppRow(int focusIndex) {
        rowCards.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < orderedApps.size(); i++) {
            final int index = i;
            final AppEntry app = orderedApps.get(i);
            View card = inflater.inflate(R.layout.item_card, rowCards, false);
            RoundedImageView image = card.findViewById(R.id.cardImage);
            image.setCornerRadius(dp(14));
            image.setImageDrawable(app.icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(dp(52), dp(20), dp(52), dp(30));
            // kafel w kolorze marki, jak w Novej: logo na środku, nazwa w lewym dolnym rogu
            int brand = brandColor(app.packageName);
            GradientDrawable tileBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, new int[]{brand, shade(brand, 0.70f)});
            tileBg.setCornerRadius(dp(16));
            card.setBackground(tileBg);
            TextView tileLabel = new TextView(this);
            tileLabel.setText(app.label);
            tileLabel.setTextSize(14);
            tileLabel.setTextColor(Color.WHITE);
            tileLabel.setSingleLine(true);
            tileLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout imageFrame = (FrameLayout) image.getParent();
            FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            labelLp.leftMargin = dp(14);
            labelLp.rightMargin = dp(14);
            labelLp.bottomMargin = dp(10);
            imageFrame.addView(tileLabel, labelLp);
            card.setNextFocusUpId(R.id.tabYou);

            boolean editing = (i == editingIndex);
            card.findViewById(R.id.arrowLeft).setVisibility(editing ? View.VISIBLE : View.GONE);
            card.findViewById(R.id.arrowRight).setVisibility(editing ? View.VISIBLE : View.GONE);

            final GradientDrawable focusBg = tileBg;
            card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    focusBg.setStroke(hasFocus ? dp(3) : 0, 0xFFFFFFFF);
                    animateFocus(v, hasFocus);
                    if (hasFocus) { updateBackdrop(app); buildContentRowFor(app); }
                }
            });
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (editingIndex >= 0) { exitEdit(); return; }
        if (panel != null && panel.isPickerOpen()) { panel.closePicker(); return; }
        if (panel != null && panel.isOpen()) { panel.close(); return; }
                    launch(app);
                }
            });
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    startEdit(index);
                    return true;
                }
            });

            rowCards.addView(card);
        }

        if (focusIndex >= 0 && focusIndex < rowCards.getChildCount()) {
            rowCards.getChildAt(focusIndex).requestFocus();
        }
    }

    /** Kolor marki — jak kafle w Novej: Netflix czarny, YouTube czerwony, Prime niebieski. */
    int brandColor(String pkg) {
        switch (pkg) {
            case "com.netflix.ninja": return 0xFF141414;
            case "com.google.android.youtube.tv":
            case "org.smarttube.beta": return 0xFFE50914;
            case "com.amazon.amazonvideo.livingroom": return 0xFF00A8E1;
            case "com.disney.disneyplus": return 0xFF0E1B4D;
            case "com.wbd.stream":
            case "com.hbo.hbonow": return 0xFF3A0CA3;
            case "com.hulu.plus": return 0xFF1CE783;
            case "com.spotify.tv.android": return 0xFF121212;
            case "com.skyshowtime.skyshowtime.google": return 0xFF2B0B4F;
            case "com.elevensportsnetwork.elevenpoland": return 0xFFE23B00;
            case "com.canal.android.canal": return 0xFF0D0D0D;
            case "com.play.playnow.tv": return 0xFF6C2BD9;
            default: return TILE_COLORS[Math.abs(pkg.hashCode()) % TILE_COLORS.length];
        }
    }

    /** Ciemniejszy wariant koloru do gradientu kafla. */
    int shade(int color, float factor) {
        int r = (int) (((color >> 16) & 0xff) * factor);
        int g = (int) (((color >> 8) & 0xff) * factor);
        int b = (int) ((color & 0xff) * factor);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private void startEdit(int index) {
        editingIndex = index;
        buildAppRow(index);
    }

    private void exitEdit() {
        int keep = editingIndex;
        editingIndex = -1;
        buildAppRow(keep);
    }

    private void moveItem(int direction) {
        int to = editingIndex + direction;
        if (to < 0 || to >= orderedApps.size()) return;
        Collections.swap(orderedApps, editingIndex, to);
        editingIndex = to;
        saveOrder();
        buildAppRow(to);
    }



    /** Ustawia tlo z rozmytego plakatu (cache per adres). */
    void setBackdropPoster(String url) {
        // Wybrane zdjęcie ma priorytet: plakat nie może podmieniać tła.
        if (look != null && !"poster".equals(look.background)) return;
        if (url == null || url.isEmpty()) return;
        final String key = "blur:" + url;
        android.graphics.Bitmap cached = backdropCache.get(key);
        if (cached != null) {
            backdrop.setImageBitmap(cached);
            return;
        }
        ImageLoader.get().loadBitmap(url, new ImageLoader.BitmapCallback() {
            @Override public void onBitmap(android.graphics.Bitmap bitmap) {
                android.graphics.Bitmap blurred = blurBackdrop(bitmap);
                if (blurred != null) {
                    backdropCache.put(key, blurred);
                    backdrop.setImageBitmap(blurred);
                }
            }
        });
    }
    /** Tlo w stylu szklo: rozmyty podglad grafiki aplikacji z fokusu. */
    void updateBackdrop(AppEntry app) {
        // Wybrane zdjęcie ma priorytet: plakat nie może podmieniać tła.
        if (look != null && !"poster".equals(look.background)) return;
        if (app == null) return;
        Drawable banner = bannerOf(app);
        Bitmap bg = backdropCache.get(app.packageName);
        if (bg == null) {
            Drawable source = banner != null ? banner : app.icon;
            bg = blurBackdrop(bitmapOf(source));
            backdropCache.put(app.packageName, bg);
        }
        if (bg != null) backdrop.setImageBitmap(bg);
        else backdrop.setImageResource(R.drawable.wp_main);
    }
    // ---------- styl interfejsu ----------

    private void applyStyle() {
        int tabSize = sharp ? 19 : 17;
        tabYou.setTextSize(tabSize);
        tabSettings.setTextSize(tabSize);

        backdrop.setVisibility(View.VISIBLE);
        overlayDim.setVisibility(View.GONE);
        overlayLeft.setVisibility(View.VISIBLE);
        overlayBottom.setVisibility(View.VISIBLE);

    }

    private void toggleStyle() {
        sharp = !sharp;
        prefs.edit().putString(PREF_STYLE, sharp ? STYLE_SHARP : STYLE_GLASS).apply();
        recreate();
    }
    // ---------- rozmyte tło ----------

    private Bitmap bitmapOf(Drawable d) {
        if (d == null) return null;
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (b != null) return b;
        }
        int w = Math.max(1, d.getIntrinsicWidth());
        int h = Math.max(1, d.getIntrinsicHeight());
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return bmp;
    }

    private Bitmap blurBackdrop(Bitmap src) {
        if (src == null || src.getWidth() < 2) return null;
        int sw = Math.max(40, src.getWidth() / 4);
        int sh = Math.max(24, src.getHeight() / 4);
        Bitmap small = Bitmap.createScaledBitmap(src, sw, sh, true);
        int[] px = new int[sw * sh];
        small.getPixels(px, 0, sw, 0, 0, sw, sh);
        int[] tmp = new int[px.length];
        for (int pass = 0; pass < 1; pass++) {
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    int r = 0, g = 0, b = 0, n = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int xx = x + dx, yy = y + dy;
                            if (xx < 0 || yy < 0 || xx >= sw || yy >= sh) continue;
                            int c = px[yy * sw + xx];
                            r += (c >> 16) & 0xff;
                            g += (c >> 8) & 0xff;
                            b += c & 0xff;
                            n++;
                        }
                    }
                    tmp[y * sw + x] = 0xff000000
                            | (((r / n) * 100 / 100) << 16)
                            | (((g / n) * 100 / 100) << 8)
                            | ((b / n) * 100 / 100);
                }
            }
            System.arraycopy(tmp, 0, px, 0, px.length);
        }
        Bitmap blurred = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        blurred.setPixels(px, 0, sw, 0, 0, sw, sh);
        return Bitmap.createScaledBitmap(blurred, 1920, 1080, true);
    }


    // ---------- układ ekranu głównego ----------

    /** Buduje ekran „Dla Ciebie" według wybranego układu i wyglądu. */
    /** Tło: albo nasz rozmyty plakat (dynamiczny), albo wybrane zdjęcie z assets. */
    void applyBackground() {
        if ("poster".equals(look.background)) {
            if (!orderedApps.isEmpty()) updateBackdrop(orderedApps.get(0));
            return;
        }
        try {
            android.content.res.AssetManager assets = getAssets();
            java.io.InputStream in = assets.open(look.background + ".jpg");
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
            in.close();
            if (bitmap != null) backdrop.setImageBitmap(bitmap);
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "brak tla " + look.background, e);
        }
    }

    void renderHome() {
        boolean centeredClock = "logos".equals(look.layout) || "dock".equals(look.layout);
        boolean hideClock = "hide".equals(look.clock) || "minimal".equals(look.layout);
        clock.setVisibility(hideClock || centeredClock ? View.GONE : View.VISIBLE);
        clock.setTextSize("small".equals(look.clock) ? 28 : 54);
        if (clockDate != null) clockDate.setVisibility(hideClock || centeredClock ? View.GONE : View.VISIBLE);
        clockHero.setVisibility(hideClock || !centeredClock ? View.GONE : View.VISIBLE);

        // Stare widoki siatki nie są już używane — każdy launcher buduje HomeLayouts.
        rowTitle.setVisibility(View.GONE);
        rowCards.setVisibility(View.GONE);
        View cardsParent = (View) rowCards.getParent();
        if (cardsParent != null) cardsParent.setVisibility(View.GONE);
        continueTitle.setVisibility(View.GONE);
        continueScroll.setVisibility(View.GONE);

        homeHost.setVisibility(View.VISIBLE);
        applyBackground();
        HomeLayouts.build(this, homeHost, look);
    }

    /** Wypełnia kafel: baner aplikacji (albo ikona) i podpis w lewym dolnym rogu. */
    void fillTile(FrameLayout tile, final AppEntry app, Look look, boolean withText) {
        tile.removeAllViews();
        // Duży kafel to ikona aplikacji na przygaszonym tle w jej kolorze — banery Netflixa i spółki
        // są białymi logo i wyglądają jak plakat, a nie jak ekran główny.
        int brand = TILE_COLORS[Math.abs(app.packageName.hashCode()) % TILE_COLORS.length];
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{
                blend(brand, 0xFF0A0A0E, 0.60f), blend(brand, 0xFF0A0A0E, 0.86f)});
        bg.setCornerRadius(dp(22));
        tile.setBackground(bg);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (look.whiteLogo()) icon.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(96), dp(96));
        lp.gravity = Gravity.CENTER;
        tile.addView(icon, lp);

        if (withText && look.labelsShown()) {
            TextView name = new TextView(this);
            name.setText(app.label);
            name.setTextSize(17);
            name.setTypeface(look.typefaceBold());
            name.setTextColor(Color.WHITE);
            name.setShadowLayer(8f, 0f, 2f, 0xB3000000);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            labelLp.leftMargin = dp(16);
            labelLp.rightMargin = dp(16);
            labelLp.bottomMargin = dp(10);
            tile.addView(name, labelLp);
        }
    }

    /** Reflektor: podmienia zawartość dużego kafla na wybraną aplikację. */
    void showSpotlight(FrameLayout tile, AppEntry app, Look look) {
        fillTile(tile, app, look, true);
    }

    /** Wspólna reakcja kafli na fokus: animacja, tło i podgląd. */
    /** Obwódka fokusu dla kafli budowanych w HomeLayouts. */
    View.OnFocusChangeListener focusRing(final GradientDrawable bg, final AppEntry app) {
        return new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                bg.setStroke(hasFocus ? dp(3) : 0, 0xFFFFFFFF);
                animateFocus(v, hasFocus);
                if (hasFocus && app != null) updateBackdrop(app);
            }
        };
    }

    void focusTile(View v, boolean hasFocus, AppEntry app) {
        animateFocus(v, hasFocus);
        if (hasFocus && app != null) {
            // Rząd "Kontynuuj oglądanie" pokazuje pozycje aplikacji, na którą najechaliśmy.
            filterContinue(app.packageName);
            if ("grid".equals(look.layout)) updateBackdrop(app);
        }
    }

    /** Buduje karty rzędu "Kontynuuj" dla podanej listy pozycji (max 6). */
    void populateContinue(LinearLayout row, List<ContentEntry> items, Look look) {
        row.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < Math.min(6, items.size()); i++) {
            final ContentEntry entry = items.get(i);
            View card = inflater.inflate(R.layout.item_content, row, false);
            RoundedImageView image = card.findViewById(R.id.contentImage);
            image.setCornerRadius(dp(13));
            boolean hasPoster = entry.poster != null && !entry.poster.isEmpty();
            if (hasPoster) ImageLoader.get().load(entry.poster, image);
            TextView title = card.findViewById(R.id.contentTitle);
            title.setText(entry.title);
            if (hasPoster) title.setVisibility(View.GONE);
            card.setTag((entry.packageName == null ? "" : entry.packageName) + "|"
                    + (entry.intentUri == null ? "" : entry.intentUri));
            card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    animateFocus(v, hasFocus);
                    if (hasFocus) setBackdropPoster(entry.poster);
                }
            });
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openContent(entry); }
            });
            row.addView(card);
        }
    }

    /** Pozycje pasujące do aplikacji (nazwa pakietu lub słowo z pakietu w intencie). */
    List<ContentEntry> continueFor(String pkg) {
        List<ContentEntry> out = new ArrayList<>();
        if (pkg == null) {
            out.addAll(continueItems);
            return out;
        }
        for (ContentEntry e : continueItems) {
            String tag = ((e.packageName == null ? "" : e.packageName) + "|"
                    + (e.intentUri == null ? "" : e.intentUri)).toLowerCase();
            if (tag.contains(pkg.toLowerCase()) || matchesKeyword(pkg, tag)) out.add(e);
            if (out.size() >= 6) break;
        }
        return out;
    }
    private static final String[] STOP = {"com", "android", "google", "tv", "ninja", "livingroom", "amazonvideo", "firetv", "mediaclient"};

    /** Czy tag karty pasuje do aplikacji po słowie z nazwy pakietu (netflix, amazon, sky...). */
    private boolean matchesKeyword(String pkg, String tag) {
        for (String part : pkg.toLowerCase().split("\\.")) {
            if (part.length() < 4) continue;
            boolean skip = false;
            for (String s : STOP) if (s.equals(part)) { skip = true; break; }
            if (skip) continue;
            if (tag.contains(part)) return true;
        }
        return false;
    }

    /** Pokazuje w rzędzie "Kontynuuj" tylko karty danej aplikacji (null = wszystkie). */
    void filterContinue(String packageName) {
        if (homeContinueRow == null) return;
        List<ContentEntry> items = continueFor(packageName);
        populateContinue(homeContinueRow, items, look);
        boolean visible = !items.isEmpty();
        if (homeContinueTitle != null) homeContinueTitle.setVisibility(visible ? View.VISIBLE : View.GONE);
        View parent = (View) homeContinueRow.getParent();
        if (parent != null) parent.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Mieszanie kolorów: share=0 to kolor bazowy, share=1 to drugi kolor. */
    int blend(int from, int to, float share) {
        int r = (int) (((from >> 16) & 0xff) * (1 - share) + ((to >> 16) & 0xff) * share);
        int g = (int) (((from >> 8) & 0xff) * (1 - share) + ((to >> 8) & 0xff) * share);
        int b = (int) ((from & 0xff) * (1 - share) + (to & 0xff) * share);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }


    /** Przebudowa po zmianie wyglądu — recreate() odświeża wszystko bez restartu aplikacji. */
    private void applyLookChange() {
        look.save(prefs);
        recreate();
    }

    // ---------- ekran Ustawienia ----------

    // ---------- panel wyglądu (wysuwany z prawej) ----------

    // ---------- pasek launcherów na ekranie głównym ----------

    private LinearLayout layoutChips;

    /** Pasek u góry: każdy launcher jako pastylka — jedno OK i cały układ się zmienia. */

    private GradientDrawable chipBackground(boolean active, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(16));
        d.setColor(focused ? 0xFFFFFFFF : (active ? 0x99FFFFFF : 0x1FFFFFFF));
        return d;
    }

    void openPanel() {
        panel.open();
    }

    /** Fokus na pastylki u góry (z układów, które same obsługują strzałki). */
    void focusTabs() {
        View tab = findViewById(R.id.tabYou);
        if (tab != null) tab.requestFocus();
    }

    /** Z pastylek w dół: pierwszy widoczny kafel bieżącego układu. */
    void focusContentFromTabs() {
        View host = findViewById(R.id.homeHost);
        if (host instanceof ViewGroup) {
            View first = firstFocusable((ViewGroup) host);
            if (first != null) { first.requestFocus(); return; }
        }
        if (rowCards.getChildCount() > 0) rowCards.getChildAt(0).requestFocus();
    }

    private View firstFocusable(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            if (child.isFocusable() && child.getWidth() > 0) return child;
            if (child instanceof ViewGroup) {
                View inner = firstFocusable((ViewGroup) child);
                if (inner != null) return inner;
            }
        }
        return null;
    }

    void onPanelClosed() {
        if (!orderedApps.isEmpty()) updateBackdrop(orderedApps.get(0));
    }

    /** Miniatura wybranego tła (mały odczyt z assets, cache w pamięci). */
    private android.graphics.drawable.Drawable backgroundThumb() {
        if ("poster".equals(look.background)) return null;
        if (thumbCache == null || !look.background.equals(thumbKey)) {
            try {
                java.io.InputStream in = getAssets().open(look.background + ".jpg");
                android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                o.inSampleSize = 8;
                thumbCache = android.graphics.BitmapFactory.decodeStream(in, null, o);
                in.close();
                thumbKey = look.background;
            } catch (Exception e) {
                android.util.Log.e("EFSHome", "miniatura tla", e);
                return null;
            }
        }
        return thumbCache == null ? null
                : new android.graphics.drawable.BitmapDrawable(getResources(), thumbCache);
    }

    private String bgApplied;
    private android.graphics.Bitmap thumbCache;
    private String thumbKey;

    private int indexOf(String[] values, String current) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return i;
        return 0;
    }

    /** Paseczki panelu: układ, kafelki, etykiety, logo, czcionka, zegar, o programie, Sony, ustawienia TV. */
    private void buildPanel() {
        panel = new Panel(this, panelView, panelRows, pickerBox, panelDim);

        panel.add(new Panel.Item() {
            @Override String label() { return getString(R.string.row_preset); }
            @Override String value() { return getString(R.string.preset_value); }
            @Override void open() {
                panel.showPicker(getString(R.string.preset_title), Look.PRESET_NAMES, indexOf(Look.PRESET_NAMES, ""),
                        new Panel.OnPick() {
                            @Override public void picked(int which) {
                                look.applyPreset(which);
                                look.save(prefs);
                                applyBackground();
                                renderHome();
                            }
                        });
            }
        });

        panel.add(new Panel.Item() {
            @Override String label() { return getString(R.string.row_layout); }
            @Override String value() { return Look.nameOf(Look.LAYOUTS, Look.LAYOUT_NAMES, look.layout); }
            @Override void open() {
                panel.showPicker(getString(R.string.layout_title), Look.LAYOUT_NAMES, Look.LAYOUT_HINTS, indexOf(Look.LAYOUTS, look.layout),
                        new Panel.OnPick() {
                            @Override public void picked(int which) {
                                look.layout = Look.LAYOUTS[which];
                                look.save(prefs);
                                renderHome();
                            }
                        });
            }
        });

        panel.add(new Panel.Item() {
            @Override String label() { return getString(R.string.row_background); }
            @Override String value() { return Look.nameOf(Look.BACKGROUNDS, Look.BACKGROUND_NAMES, look.background); }
            @Override android.graphics.drawable.Drawable thumb() { return backgroundThumb(); }
            @Override void open() {
                panel.showPicker(getString(R.string.background_title), Look.BACKGROUND_NAMES, indexOf(Look.BACKGROUNDS, look.background),
                        new Panel.OnPick() {
                            @Override public void picked(int which) {
                                look.background = Look.BACKGROUNDS[which];
                                look.save(prefs);
                                applyBackground();
                            }
                        });
            }
            @Override void left() { step(-1); }
            @Override void right() { step(1); }
            private void step(int direction) {
                int at = indexOf(Look.BACKGROUNDS, look.background);
                at = (at + direction + Look.BACKGROUNDS.length) % Look.BACKGROUNDS.length;
                look.background = Look.BACKGROUNDS[at];
                look.save(prefs);
                applyBackground();
            }
        });

        panel.add(cycle(R.string.row_tiles, "tiles", Look.TILES, Look.TILES_NAMES));
        panel.add(cycle(R.string.row_labels, "labels", Look.LABELS, Look.LABELS_NAMES));
        panel.add(cycle(R.string.row_logo, "logo", Look.LOGOS, Look.LOGOS_NAMES));
        panel.add(cycle(R.string.row_font, "font", Look.FONTS, Look.FONTS_NAMES));
        panel.add(cycle(R.string.row_clock, "clock", Look.CLOCKS, Look.CLOCKS_NAMES));
        panel.add(cycle(R.string.row_continue, "continue", Look.CONTINUE, Look.CONTINUE_NAMES));

        panel.add(new Panel.Item() {
            @Override String label() { return getString(R.string.row_about); }
            @Override String value() { return getString(R.string.about_value, appVersion); }
            @Override void open() {
                String[] info = {
                        getString(R.string.about_app, appVersion),
                        getString(R.string.about_model, Build.MODEL),
                        getString(R.string.about_system, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
                        getString(R.string.about_home,
                                getString(isOurHome() ? R.string.app_name : R.string.home_other)),
                        getString(R.string.about_apps, orderedApps.size()),
                        getString(R.string.close),
                };
                panel.showPicker(getString(R.string.row_about), info, info.length - 1, new Panel.OnPick() {
                    @Override public void picked(int which) { }
                });
            }
        });

        panel.add(new Panel.Item() {
            @Override String label() { return getString(R.string.row_stock_ui); }
            @Override String value() { return getString(R.string.open); }
            @Override void open() { closePanelAndRun(new Runnable() { @Override public void run() { openSonyUi(); } }); }
        });

        panel.add(new Panel.Item() {
            @Override String label() { return getString(R.string.row_tv_settings); }
            @Override String value() { return getString(R.string.open); }
            @Override void open() { closePanelAndRun(new Runnable() { @Override public void run() { openSystemSettings(); } }); }
        });

        panel.build();
    }

    /**
     * Panel row that cycles a value with the arrows and saves it.
     * field = which Look field it edits (tiles/labels/logo/font/clock/continue) — never the
     * visible label, which is translated.
     */
    private Panel.Item cycle(final int labelRes, final String field, final String[] values, final String[] names) {
        return new Panel.Item() {
            @Override String label() { return getString(labelRes); }
            @Override String value() { return Look.nameOf(values, names, get()); }
            @Override void left() { step(-1); }
            @Override void right() { step(1); }
            private void step(int direction) {
                int at = 0;
                for (int i = 0; i < values.length; i++) if (values[i].equals(get())) at = i;
                at = (at + direction + values.length) % values.length;
                set(values[at]);
                look.save(prefs);
                renderHome();
            }
            private String get() {
                switch (field) {
                    case "tiles": return look.tiles;
                    case "labels": return look.labels;
                    case "logo": return look.logo;
                    case "font": return look.font;
                    case "continue": return look.continueRow;
                    default: return look.clock;
                }
            }
            private void set(String value) {
                switch (field) {
                    case "tiles": look.tiles = value; break;
                    case "labels": look.labels = value; break;
                    case "logo": look.logo = value; break;
                    case "font": look.font = value; break;
                    case "continue": look.continueRow = value; break;
                    default: look.clock = value; break;
                }
            }
        };
    }

    private void closePanelAndRun(Runnable action) {
        panel.close();
        action.run();
    }

    private void buildSettings() {
        addSetting(getString(R.string.settings_version), appVersion);
        addSetting(getString(R.string.settings_home),
                getString(isOurHome() ? R.string.app_name : R.string.home_other_check));
        addSetting(getString(R.string.settings_model), Build.MODEL);
        addSetting(getString(R.string.settings_system), "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addSetting(getString(R.string.settings_apps), String.valueOf(orderedApps.size()));
        addSetting(getString(R.string.settings_order),
                getString(prefs.getString(PREF_ORDER, "").isEmpty() ? R.string.no : R.string.yes));
        addAction(getString(R.string.row_layout) + ": " + Look.nameOf(Look.LAYOUTS, Look.LAYOUT_NAMES, look.layout), new Runnable() {
            @Override public void run() {
                look.layout = Look.nextOf(Look.LAYOUTS, look.layout);
                applyLookChange();
            }
        });
        addAction(getString(R.string.row_tiles) + ": " + Look.nameOf(Look.TILES, Look.TILES_NAMES, look.tiles), new Runnable() {
            @Override public void run() {
                look.tiles = Look.nextOf(Look.TILES, look.tiles);
                applyLookChange();
            }
        });
        addAction(getString(R.string.row_labels) + ": " + Look.nameOf(Look.LABELS, Look.LABELS_NAMES, look.labels), new Runnable() {
            @Override public void run() {
                look.labels = Look.nextOf(Look.LABELS, look.labels);
                applyLookChange();
            }
        });
        addAction(getString(R.string.row_logo) + ": " + Look.nameOf(Look.LOGOS, Look.LOGOS_NAMES, look.logo), new Runnable() {
            @Override public void run() {
                look.logo = Look.nextOf(Look.LOGOS, look.logo);
                applyLookChange();
            }
        });
        addAction(getString(R.string.row_font) + ": " + Look.nameOf(Look.FONTS, Look.FONTS_NAMES, look.font), new Runnable() {
            @Override public void run() {
                look.font = Look.nextOf(Look.FONTS, look.font);
                applyLookChange();
            }
        });
        addAction(getString(R.string.row_clock) + ": " + Look.nameOf(Look.CLOCKS, Look.CLOCKS_NAMES, look.clock), new Runnable() {
            @Override public void run() {
                look.clock = Look.nextOf(Look.CLOCKS, look.clock);
                applyLookChange();
            }
        });
        addAction(getString(sharp ? R.string.style_sharp : R.string.style_glass), new Runnable() {
            @Override public void run() { toggleStyle(); }
        });
        addAction(getString(R.string.action_home_role), new Runnable() {
            @Override public void run() { requestHomeRole(); }
        });
        addAction(getString(R.string.row_stock_ui), new Runnable() {
            @Override public void run() { openSonyUi(); }
        });
        addAction(getString(R.string.row_tv_settings), new Runnable() {
            @Override public void run() { openSystemSettings(); }
        });
    }

    /** Czy nasza aplikacja jest domyslnym ekranem glownym. */
    private boolean isOurHome() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ComponentName c = home.resolveActivity(getPackageManager());
            return c != null && getPackageName().equals(c.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /** Otwiera prawdziwe ustawienia Android TV; Wstecz/HOME wracaja do EFS Studio TV. */
    private void openSystemSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.MainSettings"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e2) {
                android.util.Log.e("EFSHome", "nie mozna otworzyc ustawien", e2);
            }
        }
    }

    /** Wyjasnia, ze przelaczenie ekranu glownego wymaga ADB — aplikacja nie ma tego uprawnienia. */
    private void showHomeSwitchInfo() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.home_switch_title)
                .setMessage(R.string.home_switch_msg)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    /** The stock home screen is disabled (that's how EFS Studio TV becomes the home screen) — explain how to bring it back. */
    private void showStockUiDisabled() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.stock_ui_off_title)
                .setMessage(R.string.stock_ui_off_msg)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    /** Systemowy dialog "ustaw jako ekran główny" (RoleManager) — to jedyny legalny sposób z aplikacji. */
    private void requestHomeRole() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.app.role.RoleManager rm =
                        (android.app.role.RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)
                        && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), 1001);
                    return;
                }
                android.widget.Toast.makeText(this, R.string.toast_already_home,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            showHomeSwitchInfo();
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "rola HOME", e);
            showHomeSwitchInfo();
        }
    }

    /** Uruchamia fabryczny interfejs Sony jako zwykłą aplikację (bez zmiany ekranu głównego). */
    private void openSonyUi() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.google.android.tvlauncher",
                    "com.google.android.tvlauncher.MainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            android.widget.Toast.makeText(this, R.string.toast_home_returns, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "stock launcher not available", e);
            showStockUiDisabled();
        }
    }
    /** Wiersz ustawien, ktory da sie kliknac (akcja). */
    private void addAction(String label, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setBackgroundResource(R.drawable.bg_glass);
        row.setPadding(dp(24), 0, dp(24), 0);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(17);
        l.setTextColor(Color.WHITE);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(l);

        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextSize(22);
        chevron.setTextColor(Color.parseColor("#9AA0A6"));
        row.addView(chevron);

        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) { animateFocus(v, hasFocus); }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.topMargin = dp(4);
        row.setLayoutParams(lp);

        settingsList.addView(row);
    }

    private void addSetting(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(16);
        l.setTextColor(Color.parseColor("#E8EAED"));
        l.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(16);
        v.setTextColor(Color.parseColor("#9AA0A6"));
        row.addView(v);

        settingsList.addView(row);
    }

    // ---------- uruchamianie ----------


    // ---------- Kontynuuj ogladanie (Watch Next) ----------

    static class ContentEntry {
        String title;
        String poster;
        String intentUri;
        String packageName;
        float progress;
    }

    private void loadContinueWatching() {
        continueItems.clear();
        // Watch Next and PreviewPrograms exist from Android 8 (API 26). On older systems the
        // classes are missing (NoClassDefFoundError is not an Exception) — the row stays empty.
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            String[] projection = {
                    android.media.tv.TvContract.WatchNextPrograms._ID,
                    android.media.tv.TvContract.WatchNextPrograms.COLUMN_TITLE,
                    android.media.tv.TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
                    android.media.tv.TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
                    android.media.tv.TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
                    "last_playback_position_ms",
                    "duration_ms",
            };
            android.database.Cursor c = getContentResolver().query(
                    android.media.tv.TvContract.WatchNextPrograms.CONTENT_URI, projection, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String title = c.getString(1);
                    String intentUri = c.getString(3);
                    if (title == null || intentUri == null) continue;
                    ContentEntry e = new ContentEntry();
                    e.title = title;
                    e.poster = c.getString(2);
                    e.intentUri = intentUri;
                    e.packageName = c.getString(4);
                    long pos = c.getLong(5);
                    long dur = c.getLong(6);
                    e.progress = dur > 0 ? Math.min(1f, (float) pos / dur) : 0f;
                    continueItems.add(e);
                    if (continueItems.size() >= 200) break;
                }
                c.close();
            }
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "watch_next blad", e);
        }

        // Top picks (PreviewPrograms) — dla aplikacji, ktore nie publikuja do Watch Next
        try {
            android.database.Cursor p = getContentResolver().query(
                    android.media.tv.TvContract.PreviewPrograms.CONTENT_URI,
                    new String[]{"title", "package_name", "intent_uri", "poster_art_uri"}, null, null, null);
            if (p != null) {
                while (p.moveToNext()) {
                    String title = p.getString(0);
                    String pkg = p.getString(1);
                    if (title == null || pkg == null) continue;
                    ContentEntry e = new ContentEntry();
                    e.title = title;
                    e.packageName = pkg;
                    e.intentUri = p.getString(2);
                    e.poster = p.getString(3);
                    continueItems.add(e);
                }
                p.close();
            }
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "preview blad", e);
        }
    }

    /** Rzad "Kontynuuj ogladanie" tylko dla wybranej aplikacji (max 4 pozycje). */
    private void buildContentRowFor(AppEntry app) {
        rowContinue.removeAllViews();
        List<ContentEntry> filtered = new ArrayList<>();
        if (app != null) {
            for (ContentEntry e : continueItems) {
                if (app.packageName.equals(e.packageName)) filtered.add(e);
                if (filtered.size() >= 6) break;
            }
        }
        boolean visible = !filtered.isEmpty();
        continueTitle.setVisibility(visible ? View.VISIBLE : View.GONE);
        continueScroll.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            updateBackdrop(app);
            return;
        }
        // tlo = plakat pierwszego kafelka; zmienia sie przy kazdym fokusie
        setBackdropPoster(filtered.get(0).poster);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final ContentEntry entry : filtered) {
            View card = inflater.inflate(R.layout.item_content, rowContinue, false);
            RoundedImageView image = card.findViewById(R.id.contentImage);
            image.setCornerRadius(sharp ? dp(8) : dp(13));
            boolean hasPoster = entry.poster != null && !entry.poster.isEmpty();
            if (hasPoster) {
                ImageLoader.get().load(entry.poster, image);
            } else {
                image.setBackgroundColor(TILE_COLORS[Math.abs(entry.title.hashCode()) % TILE_COLORS.length]);
            }
            TextView contentTitle = card.findViewById(R.id.contentTitle);
            contentTitle.setText(entry.title);
            if (hasPoster) contentTitle.setVisibility(View.GONE);

            card.setBackgroundResource(sharp ? R.drawable.bg_flat : R.drawable.bg_glass);
            card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    if (sharp) {
                        v.setBackgroundResource(hasFocus ? R.drawable.bg_flat_focus : R.drawable.bg_flat);
                    } else {
                        animateFocus(v, hasFocus);
                    }
                    if (hasFocus) setBackdropPoster(entry.poster);
                }
            });
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openContent(entry); }
            });
            rowContinue.addView(card);
        }
    }
    void openContent(ContentEntry entry) {
        try {
            Intent intent = Intent.parseUri(entry.intentUri, Intent.URI_INTENT_SCHEME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            // awaryjnie: otworz sama aplikacje
            try {
                Intent byPackage = getPackageManager().getLaunchIntentForPackage(entry.packageName);
                if (byPackage != null) {
                    byPackage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(byPackage);
                }
            } catch (Exception e2) {
                android.util.Log.e("EFSHome", "nie mozna otworzyc " + entry.title, e2);
            }
        }
    }

    void launch(AppEntry app) {
        if (app == null) return;
        try {
            Intent intent = new Intent();
            intent.setComponent(app.component);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("EFSHome", "nie mozna uruchomic " + app.packageName, e);
        }
    }
}




































































































