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

    static final String[] LAYOUTS = {"grid", "icons", "minimal", "logos", "spotlight", "mosaic", "sidebar", "dock", "arc"};
    static final String[] LAYOUT_NAMES = {"Siatka", "Ikony", "Minimal", "Logotypy", "Reflektor", "Mozaika", "Panel boczny", "Dok", "Łuk"};

    /** Krotki opis pod nazwa ukladu w liscie wyboru. */
    static final String[] LAYOUT_HINTS = {
            "5 kafli w rzędzie, podpisy pod kaflami",
            "kwadratowe ikony 6 w rzędzie",
            "same nazwy aplikacji, bez kafli",
            "tylko znaki, wielki zegar",
            "jeden duży kafel i pasek znaków",
            "duży kafel + kolumna + rząd czterech",
            "lista aplikacji i duży podgląd",
            "rząd pięciu kafli przy dolnej krawędzi",
            "kafle po łuku, wybrany na środku",
    };

    static final String[] TILES = {"glass", "frost", "dark", "clear", "brand"};
    static final String[] TILES_NAMES = {"Szkło", "Szron", "Ciemne", "Bez tła", "Kolor aplikacji"};

    static final String[] LABELS = {"both", "logo", "name"};
    static final String[] LABELS_NAMES = {"Logo i nazwa", "Tylko logo", "Tylko nazwa"};

    static final String[] LOGOS = {"color", "line", "full"};
    static final String[] LOGOS_NAMES = {"Kolor aplikacji", "Kontur (linia)", "Ikona na całość"};

    static final String[] FONTS = {"modern", "elegant", "rounded", "grotesk"};
    static final String[] FONTS_NAMES = {"Nowoczesna", "Elegancka", "Zaokrąglona", "Zwarta"};

    /** Gotowe zestawy wyglądu — jedno kliknięcie ustawia układ, kafle, etykiety, zegar i tło. */
    static final String[] PRESET_NAMES = {
            "Siatka klasyczna", "Ikony", "Minimal", "Mozaika", "Panel boczny", "Dok", "Czarno-białe",
    };
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
    static final String[] BACKGROUND_NAMES = {"Rozmyty plakat (nasz)", "Mglisty brzeg", "Cicha zatoka", "Las we mgle",
            "Dolina górska", "Wydmy", "Granitowe szczyty", "Miasto o zmierzchu", "Złota godzina",
            "Czarno-białe miasto", "Czarno-białe góry", "Miasto nocą nad rzeką", "Biały kabriolet",
            "Czarno-białe sekwoje", "Łąka stokrotek", "Tulipanowe pola", "Górskie jezioro"};

    static final String[] CONTINUE = {"on", "off"};
    static final String[] CONTINUE_NAMES = {"Pokazuj", "Ukryj"};

    static final String[] CLOCKS = {"show", "small", "hide"};
    static final String[] CLOCKS_NAMES = {"Duży", "Mały", "Ukryty"};

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













