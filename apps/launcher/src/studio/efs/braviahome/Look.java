/*
 * Look — wygląd ekranu głównego EFS Studio TV.
 * © EFS Studio
 *
 * Wzorem jest elastyczność, jaką ma Nova TV: układ, styl kafelków, etykiety, kolor logo,
 * czcionka i zegar wybiera się osobno, a każdy układ da się łączyć z każdym stylem.
 * Wszystko trzymamy w SharedPreferences, żeby wybór przeżył restart telewizora.
 */
package studio.efs.braviahome;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

class Look {

    // Ids are stored in preferences; the visible names come from res/values*/strings.xml
    // (English by default, translated per system language) — see loadNames().
    static final String[] LAYOUTS = {"grid", "icons", "minimal", "logos", "spotlight", "mosaic", "sidebar", "dock", "arc"};
    static String[] LAYOUT_NAMES;
    /** Short description under the layout name in the pick list. */
    static String[] LAYOUT_HINTS;

    static final String[] TILES = {"glass", "frost", "dark", "clear", "brand"};
    static String[] TILES_NAMES;

    static final String[] LABELS = {"both", "logo", "name"};
    static String[] LABELS_NAMES;

    static final String[] LOGOS = {"color", "line", "full"};
    static String[] LOGOS_NAMES;

    static final String[] FONTS = {"modern", "elegant", "rounded", "grotesk"};
    static String[] FONTS_NAMES;

    /** Ready-made looks — one click sets layout, tiles, labels, clock and background. */
    static String[] PRESET_NAMES;

    /** Loads the visible names in the system language. Call before building the screen. */
    static void loadNames(android.content.res.Resources r) {
        LAYOUT_NAMES = r.getStringArray(R.array.layout_names);
        LAYOUT_HINTS = r.getStringArray(R.array.layout_hints);
        TILES_NAMES = r.getStringArray(R.array.tile_names);
        LABELS_NAMES = r.getStringArray(R.array.label_names);
        LOGOS_NAMES = r.getStringArray(R.array.logo_names);
        FONTS_NAMES = r.getStringArray(R.array.font_names);
        PRESET_NAMES = r.getStringArray(R.array.preset_names);
        BACKGROUND_NAMES = r.getStringArray(R.array.background_names);
        CONTINUE_NAMES = r.getStringArray(R.array.continue_names);
        CLOCKS_NAMES = r.getStringArray(R.array.clock_names);
    }

    static final String[][] PRESETS = {
            // layout, tiles, labels, logo, font, clock, background, continueRow
            {"grid", "glass", "both", "color", "modern", "show", "bg_03", "on"},
            {"icons", "brand", "both", "color", "modern", "show", "bg_01", "off"},
            {"minimal", "clear", "name", "color", "elegant", "show", "bg_04", "off"},
            {"mosaic", "brand", "both", "color", "modern", "show", "bg_07", "off"},
            {"sidebar", "glass", "both", "color", "modern", "show", "bg_02", "off"},
            {"dock", "brand", "both", "color", "modern", "show", "bg_05", "off"},
            {"grid", "dark", "both", "white", "modern", "show", "bg_09", "off"},
    };

    /** Ustawia wszystkie pola z zestawu. */
    void applyPreset(int index) {
        if (index < 0 || index >= PRESETS.length) return;
        String[] p = PRESETS[index];
        layout = p[0];
        tiles = p[1];
        labels = p[2];
        logo = p[3];
        font = p[4];
        clock = p[5];
        background = p[6];
        continueRow = p[7];
    }
    static final String[] BACKGROUNDS = {"poster", "bg_01", "bg_02", "bg_03", "bg_04", "bg_05",
            "bg_06", "bg_07", "bg_08", "bg_09", "bg_10", "bg_11", "bg_12", "bg_13", "bg_14", "bg_15", "bg_16"};
    static String[] BACKGROUND_NAMES;

    static final String[] CONTINUE = {"on", "off"};
    static String[] CONTINUE_NAMES;

    static final String[] CLOCKS = {"show", "small", "hide"};
    static String[] CLOCKS_NAMES;

    String layout = "grid";
    String tiles = "glass";
    String labels = "both";
    String logo = "color";
    String font = "modern";
    String clock = "show";
    String background = "bg_01";
    String continueRow = "on";

    static Look load(SharedPreferences p) {
        Look l = new Look();
        l.layout = pick(p.getString("layout", l.layout), LAYOUTS, l.layout);
        l.tiles = pick(p.getString("tiles", l.tiles), TILES, l.tiles);
        l.labels = pick(p.getString("labels", l.labels), LABELS, l.labels);
        l.logo = pick(p.getString("logo", l.logo), LOGOS, l.logo);
        l.font = pick(p.getString("font", l.font), FONTS, l.font);
        l.clock = pick(p.getString("clock", l.clock), CLOCKS, l.clock);
        l.background = pick(p.getString("background", l.background), BACKGROUNDS, l.background);
        l.continueRow = pick(p.getString("continueRow", l.continueRow), CONTINUE, l.continueRow);
        return l;
    }

    private static String pick(String value, String[] allowed, String fallback) {
        for (String a : allowed) if (a.equals(value)) return value;
        return fallback;
    }

    void save(SharedPreferences p) {
        p.edit()
                .putString("layout", layout)
                .putString("tiles", tiles)
                .putString("labels", labels)
                .putString("logo", logo)
                .putString("font", font)
                .putString("clock", clock)
                .putString("background", background)
                .putString("continueRow", continueRow)
                .apply();
    }

    /** Nazwa wybranej opcji do pokazania w Ustawieniach. */
    static String nameOf(String[] values, String[] names, String current) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return names[i];
        return names[0];
    }

    /** Następna wartość z listy (kliknięcie w wierszu przełącza dalej). */
    static String nextOf(String[] values, String current) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return values[(i + 1) % values.length];
        return values[0];
    }

    boolean labelsShown() { return !"logo".equals(labels); }

    /** Czy w kaflu ma być ikona (tryb etykiet "tylko nazwa" ją ukrywa). */
    boolean logoShown() { return !"name".equals(labels); }

    boolean iconsOnly() { return "logo".equals(labels); }

    Typeface typeface() {
        switch (font) {
            case "elegant": return Typeface.create("serif", Typeface.NORMAL);
            case "rounded": return Typeface.create("sans-serif-rounded", Typeface.NORMAL);
            case "grotesk": return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            default: return Typeface.create("sans-serif", Typeface.NORMAL);
        }
    }

    Typeface typefaceBold() {
        switch (font) {
            case "elegant": return Typeface.create("serif", Typeface.BOLD);
            case "rounded": return Typeface.create("sans-serif-rounded", Typeface.BOLD);
            case "grotesk": return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            default: return Typeface.create("sans-serif", Typeface.BOLD);
        }
    }

    /**
     * Tło kafelka. Kolor „brand" bierzemy z palety aplikacji, żeby każdy kafel miał swój akcent,
     * a „clear" nie rysuje nic — wtedy widoczny jest tylko obraz i podpis.
     */
    GradientDrawable tileBackground(int radiusPx, int brandColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radiusPx);
        if (lineLogo()) { shape.setColor(Color.TRANSPARENT); return shape; }
        switch (tiles) {
            case "frost":
                shape.setColor(0x4DFFFFFF);
                break;
            case "dark":
                shape.setColor(0xB3000000);
                shape.setStroke(Math.max(1, radiusPx / 12), 0x33FFFFFF);
                break;
            case "clear":
                shape.setColor(Color.TRANSPARENT);
                break;
            case "brand":
                shape.setColor(brandColor);
                break;
            default: // glass — ledwo widoczna tafla; bez obrysu, bo fokus i tak go zdejmuje
                shape.setColor(0x12FFFFFF);
                break;
        }
        return shape;
    }

    /** Czy ikonę pokazywać jako biały kształt (opcja „białe logo"). */
    boolean whiteLogo() { return "white".equals(logo); }

    /** Tryb "kontur": białe ikony rysowane linią, kafle przezroczyste. */
    boolean lineLogo() { return "line".equals(logo); }

    /** Rozmiar ikony w dp: w trybie "kontur" o 20% większy — same cienkie linie gubią się w oczach. */
    int logoSize(int dp) { return lineLogo() ? Math.round(dp * 1.2f) : dp; }

    /** Ikona rozciagnieta na caly kafel (duze logo na kolorowym tle). */
    boolean fullLogo() { return "full".equals(logo); }
}













