/*
 * HomeLayouts — osiem układów ekranu głównego EFS Studio TV.
 * © EFS Studio
 *
 * Każdy układ jest osobnym „launcherem" i buduje cały ekran po swojemu: inne proporcje kafli,
 * inne podpisy, inne rozmieszczenie. Wszystkie korzystają z tych samych danych (aplikacje,
 * „Kontynuuj oglądanie") i z tych samych opcji wyglądu z klasy Look.
 *
 * Uwaga: projekt buduje się bez lambd (brak LambdaMetafactory w android.jar), więc słuchacze
 * są klasami anonimowymi.
 */
package studio.efs.braviahome;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

class HomeLayouts {

    static void build(MainActivity a, LinearLayout host, Look look) {
        host.setClipChildren(false);
        host.setClipToPadding(false);
        host.removeAllViews();
        host.setGravity(Gravity.BOTTOM);
        switch (look.layout) {
            case "grid": grid(a, host, look); break;
            case "icons": icons(a, host, look); break;
            case "minimal": minimalNames(a, host, look); break;
            case "logos": logos(a, host, look); break;
            case "spotlight": spotlight(a, host, look); break;
            case "mosaic": mosaic(a, host, look); break;
            case "sidebar": sidebar(a, host, look); break;
            case "dock": dock(a, host, look); break;
            case "arc": arc(a, host, look); break;
            default: grid(a, host, look); break;
        }
        // fokus dopiero po ulozeniu widokow, inaczej zostaje na pastylkach
        host.post(new Runnable() {
            @Override public void run() { focusFirst(host); }
        });
    }

    // ---------------------------------------------------------------- 1. Siatka

    /** Siatka: 5 kafli w rzędzie (145×84 dp), podpis pod kaflem. */
    private static void grid(MainActivity a, LinearLayout host, Look look) {
        // Wszystkie aplikacje w siatce, przewijane w pionie (wczesniej ucinalo sie na 10).
        ScrollView scroll = new ScrollView(a);
        // Wysokosc = jeden rzad kafli + polowa nastepnego (wiecej wolnego pola na ekranie).
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(372)));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, a.dp(8), 0, a.dp(8));

        LinearLayout column = new LinearLayout(a);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);

        LinearLayout row = null;
        for (int i = 0; i < a.orderedApps.size(); i++) {
            if (i % 5 == 0) {
                row = newRow(a, 12);
                column.addView(row);
            }
            final MainActivity.AppEntry app = a.orderedApps.get(i);

            LinearLayout cell = new LinearLayout(a);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setFocusable(true);
            cell.setFocusableInTouchMode(true);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    a.dp(160), ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(a.dp(12), a.dp(7), a.dp(12), a.dp(7));
            cell.setLayoutParams(clp);

            final GradientDrawable bg = look.tileBackground(a.dp(10), a.brandColor(app.packageName));

            FrameLayout tile = new FrameLayout(a);
            tile.setLayoutParams(new LinearLayout.LayoutParams(a.dp(160), a.dp(92)));
            tile.setBackground(bg);

            android.graphics.drawable.Drawable banner = look.fullLogo() ? a.bannerOf(app) : null;
            if (banner != null) {
                // Tryb "na całość": cały kafel to grafika aplikacji (banner), przycięta do rogów.
                RoundedImageView art = new RoundedImageView(a);
                art.setImageDrawable(banner);
                art.setScaleType(ImageView.ScaleType.CENTER_CROP);
                art.setCornerRadius(a.dp(10));
                FrameLayout.LayoutParams alp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                art.setLayoutParams(alp);
                tile.addView(art);
            } else {
                tile.addView(logo(a, look, app, 38));
            }
            cell.addView(tile);

            if (look.labelsShown()) cell.addView(caption(a, look, app.label, 14, true));

            cell.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    bg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    v.setElevation(hasFocus ? a.dp(10) : 0);
                    v.animate().scaleX(hasFocus ? 1.06f : 1f).scaleY(hasFocus ? 1.06f : 1f)
                            .setDuration(130).start();
                    a.focusTile(v, hasFocus, app);
                }
            });
            cell.setOnClickListener(click(a, app));
            row.addView(cell);
        }
        View cont = continueRow(a, look);
        if (cont != null) {
            cont.setPadding(0, a.dp(6), 0, 0);   // tytuł wyżej, żeby nie przycinał karty
            column.addView(cont, 0);
        }
        scroll.addView(column);
        host.addView(scroll);
        // start zawsze od góry listy
        scroll.post(new Runnable() {
            @Override public void run() { scroll.scrollTo(0, 0); }
        });
    }


    // ---------------------------------------------------------------- 2. Ikony

    /** Ikony: kwadratowe kafle z logo i nazwą pod spodem, 6 w rzędzie. */
    private static void icons(MainActivity a, LinearLayout host, Look look) {
        // Wszystkie aplikacje, przewijane w pionie (wczesniej dzialalo tylko to, co widac).
        ScrollView scroll = new ScrollView(a);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(372)));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, a.dp(6), 0, a.dp(6));

        LinearLayout column = new LinearLayout(a);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);

        View cont = continueRow(a, look);
        if (cont != null) column.addView(cont);

        LinearLayout row = null;
        for (int i = 0; i < a.orderedApps.size(); i++) {
            if (i % 6 == 0) {
                row = newRow(a, 8);
                column.addView(row);
            }
            final MainActivity.AppEntry app = a.orderedApps.get(i);

            LinearLayout cell = new LinearLayout(a);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setFocusable(true);
            cell.setFocusableInTouchMode(true);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    a.dp(96), ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(a.dp(10), a.dp(4), a.dp(10), a.dp(6));
            cell.setLayoutParams(clp);

            FrameLayout tile = new FrameLayout(a);
            tile.setLayoutParams(new LinearLayout.LayoutParams(a.dp(96), a.dp(96)));
            tile.setBackground(look.tileBackground(a.dp(22), a.brandColor(app.packageName)));
            tile.addView(logo(a, look, app, 46));
            cell.addView(tile);

            if (look.labelsShown()) cell.addView(caption(a, look, app.label, 11, true));

            cell.setOnFocusChangeListener(focus(a, app));
            cell.setOnClickListener(click(a, app));
            row.addView(cell);
        }
        scroll.addView(column);
        host.addView(scroll);
        // start zawsze od góry listy
        scroll.post(new Runnable() {
            @Override public void run() { scroll.scrollTo(0, 0); }
        });
    }


    // ---------------------------------------------------------------- 3. Minimal (same nazwy)

    /** Minimal: tylko nazwy aplikacji, dużym tekstem, bez kafli i ikon. */
    private static void minimalNames(MainActivity a, LinearLayout host, Look look) {
        // Wszystkie aplikacje, przewijane; fokus uwydatnia nazwe powiekszeniem, bez bialej pastylki.
        ScrollView scroll = new ScrollView(a);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(372)));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);
        col.setPadding(a.dp(16), 0, a.dp(16), a.dp(4));

        for (int i = 0; i < a.orderedApps.size(); i++) {
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            final TextView name = new TextView(a);
            name.setText(app.label);
            name.setTextSize(24);
            name.setTypeface(look.typeface());
            name.setTextColor(Color.WHITE);   // wszystkie jednakowo jasne
            name.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            name.setPadding(a.dp(6), a.dp(3), a.dp(6), a.dp(3));
            name.setFocusable(true);
            name.setFocusableInTouchMode(true);
            name.setShadowLayer(8f, 0f, 2f, 0x99000000);
            name.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    v.animate().scaleX(hasFocus ? 1.14f : 1f).scaleY(hasFocus ? 1.14f : 1f)
                            .setDuration(120).start();
                    name.setTypeface(look.typeface(), hasFocus ? android.graphics.Typeface.BOLD
                            : android.graphics.Typeface.NORMAL);
                    name.setAlpha(1f);
                    a.focusTile(v, hasFocus, app);
                }
            });
            name.setOnClickListener(click(a, app));
            col.addView(name);
        }
        scroll.addView(col);
        host.addView(scroll);
        scroll.post(new Runnable() {
            @Override public void run() { scroll.scrollTo(0, 0); }
        });
    }


    // ---------------------------------------------------------------- 4. Logotypy

    /** Logotypy: same znaki bez nazw, jeden rząd przy dolnej krawędzi. */
    private static void logos(MainActivity a, LinearLayout host, Look look) {
        LinearLayout row = new LinearLayout(a);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < Math.min(a.orderedApps.size(), 10); i++) {
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            FrameLayout cell = new FrameLayout(a);
            cell.setFocusable(true);
            cell.setFocusableInTouchMode(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(86), a.dp(72));
            lp.setMargins(a.dp(9), 0, a.dp(9), 0);
            cell.setLayoutParams(lp);
            cell.setBackgroundResource(R.drawable.bg_logo_focus);
            ImageView iv = new ImageView(a);
            iv.setImageDrawable(app.icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (look.whiteLogo()) iv.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(a.dp(54), a.dp(54));
            ilp.gravity = Gravity.CENTER;
            iv.setLayoutParams(ilp);
            cell.addView(iv);
            cell.setOnFocusChangeListener(focus(a, app));
            cell.setOnClickListener(click(a, app));
            row.addView(cell);
        }
        host.addView(row);
    }

    // ---------------------------------------------------------------- 5. Reflektor

    /** Reflektor: jeden duży kafel na środku; reszta to małe znaki pod nim. */
    private static void spotlight(MainActivity a, LinearLayout host, Look look) {
        if (a.orderedApps.isEmpty()) return;
        final MainActivity.AppEntry[] current = {a.orderedApps.get(0)};

        final FrameLayout big = new FrameLayout(a);
        big.setLayoutParams(new LinearLayout.LayoutParams(a.dp(470), a.dp(264)));
        big.setFocusable(true);
        big.setFocusableInTouchMode(true);
        int brand = a.brandColor(current[0].packageName);
        final GradientDrawable bigBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{brand, a.shade(brand, 0.66f)});
        bigBg.setCornerRadius(a.dp(22));
        big.setBackground(bigBg);
        fillTileContent(a, big, current[0], look, 132, true);

        LinearLayout center = new LinearLayout(a);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        center.addView(big);

        LinearLayout strip = newRow(a, 18);
        for (int i = 0; i < Math.min(a.orderedApps.size(), 9); i++) {
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            FrameLayout chip = new FrameLayout(a);
            chip.setFocusable(true);
            chip.setFocusableInTouchMode(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(60), a.dp(60));
            lp.setMargins(a.dp(6), 0, a.dp(6), 0);
            chip.setLayoutParams(lp);
            ImageView iv = new ImageView(a);
            iv.setImageDrawable(app.icon);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(a.dp(40), a.dp(40));
            ilp.gravity = Gravity.CENTER;
            iv.setLayoutParams(ilp);
            chip.addView(iv);
            chip.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    v.setAlpha(hasFocus ? 1f : 0.7f);
                    if (hasFocus) {
                        current[0] = app;
                        int b = a.brandColor(app.packageName);
                        bigBg.setColors(new int[]{b, a.shade(b, 0.66f)});
                        big.removeAllViews();
                        fillTileContent(a, big, app, look, 132, true);
                        a.updateBackdrop(app);
                    }
                }
            });
            chip.setOnClickListener(click(a, app));
            strip.addView(chip);
        }
        center.addView(strip);
        host.addView(center);

        big.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { a.launch(current[0]); }
        });
        big.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                bigBg.setStroke(hasFocus ? a.dp(3) : 0, 0xFFFFFFFF);
            }
        });
    }

    // ---------------------------------------------------------------- 6. Mozaika

    /** Mozaika: duży kafel po lewej, dwa mniejsze po prawej, pod nimi rząd czterech. */
    private static void mosaic(MainActivity a, LinearLayout host, Look look) {
        ScrollView scroll = new ScrollView(a);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(372)));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);

        LinearLayout column = new LinearLayout(a);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        column.setClipToPadding(false);

        LinearLayout top = newRow(a, 10);
        if (!a.orderedApps.isEmpty()) {
            final MainActivity.AppEntry big = a.orderedApps.get(0);
            FrameLayout bigTile = new FrameLayout(a);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(a.dp(380), a.dp(232));
            blp.setMargins(a.dp(6), a.dp(6), a.dp(8), a.dp(6));
            bigTile.setLayoutParams(blp);
            final GradientDrawable bgBig = look.tileBackground(a.dp(14), a.brandColor(big.packageName));
            bigTile.setBackground(bgBig);
            bigTile.setFocusable(true);
            bigTile.setFocusableInTouchMode(true);
            fillTileContent(a, bigTile, big, look, 76, true);
            bigTile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    bgBig.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    a.focusTile(v, hasFocus, big);
                }
            });
            bigTile.setOnClickListener(click(a, big));
            top.addView(bigTile);
        }

        LinearLayout rightCol = new LinearLayout(a);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 1; i < Math.min(a.orderedApps.size(), 3); i++) {
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            FrameLayout tile = new FrameLayout(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(180), a.dp(112));
            lp.setMargins(0, a.dp(4), 0, a.dp(4));
            tile.setLayoutParams(lp);
            final GradientDrawable bg = look.tileBackground(a.dp(12), a.brandColor(app.packageName));
            tile.setBackground(bg);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            fillTileContent(a, tile, app, look, 40, true);
            tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    bg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    a.focusTile(v, hasFocus, app);
                }
            });
            tile.setOnClickListener(click(a, app));
            rightCol.addView(tile);
        }
        top.addView(rightCol);
        column.addView(top);

        // Prawdziwa mozaika: kafle roznych rozmiarow mieszaja sie w tych samych rzedach.
        int[][] pattern = {{240, 144}, {180, 112}, {300, 180}, {160, 100}, {200, 120},
                {180, 112}, {260, 156}, {140, 100}};
        int maxRow = 930;
        int idx = 3;
        int p = 0;
        LinearLayout row = null;
        int rowW = 0;
        while (idx < a.orderedApps.size()) {
            int[] sz = pattern[p % pattern.length];
            if (row == null || rowW + sz[0] > maxRow) {
                row = newRow(a, 8);
                row.setGravity(Gravity.BOTTOM);   // mniejsze kafle siadaja przy dolnej linii
                column.addView(row);
                rowW = 0;
            }
            final MainActivity.AppEntry app = a.orderedApps.get(idx);
            FrameLayout tile = new FrameLayout(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(sz[0]), a.dp(sz[1]));
            lp.setMargins(a.dp(6), a.dp(4), a.dp(6), a.dp(4));
            tile.setLayoutParams(lp);
            final GradientDrawable bg = look.tileBackground(a.dp(12), a.brandColor(app.packageName));
            tile.setBackground(bg);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            fillTileContent(a, tile, app, look, Math.round(sz[1] * 0.34f), true);
            tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    bg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    a.focusTile(v, hasFocus, app);
                }
            });
            tile.setOnClickListener(click(a, app));
            row.addView(tile);
            rowW += sz[0] + 12;
            idx++;
            p++;
        }

        scroll.addView(column);
        host.addView(scroll);
        scroll.post(new Runnable() {
            @Override public void run() { scroll.scrollTo(0, 0); }
        });
    }



    // ---------------------------------------------------------------- 7. Panel boczny

    /** Panel boczny: lista aplikacji po lewej, duży podgląd po prawej. */
    private static void sidebar(MainActivity a, LinearLayout host, Look look) {
        if (a.orderedApps.isEmpty()) return;

        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setGravity(Gravity.BOTTOM);
        wrap.setPadding(0, a.dp(10), 0, a.dp(16));
        wrap.setClipChildren(false);
        wrap.setClipToPadding(false);
        wrap.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(a.dp(330), ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.rightMargin = a.dp(26);
        llp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        llp.rightMargin = 0;
        list.setLayoutParams(llp);
        list.setClipChildren(false);
        list.setClipToPadding(false);

        final FrameLayout preview = new FrameLayout(a);
        preview.setLayoutParams(new LinearLayout.LayoutParams(a.dp(430), a.dp(280)));
        final MainActivity.AppEntry first = a.orderedApps.get(0);
        final GradientDrawable pbg = look.tileBackground(a.dp(16), a.brandColor(first.packageName));
        preview.setBackground(pbg);
        fillTileContent(a, preview, first, look, 96, true);

        for (int i = 0; i < a.orderedApps.size(); i++) {
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            LinearLayout row = new LinearLayout(a);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, a.dp(52));
            lp.bottomMargin = a.dp(6);
            row.setLayoutParams(lp);
            row.setPadding(a.dp(14), 0, a.dp(12), 0);
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            // tło z ustawień wyglądu (szkło/szron/ciemne/bez tła/kolor marki)
            final GradientDrawable rbg = look.tileBackground(a.dp(12), a.brandColor(app.packageName));
            row.setBackground(rbg);

            if (look.logoShown()) {
                ImageView iv = new ImageView(a);
                int iconDp = look.logoSize(30);
                if (look.lineLogo()) {
                    android.graphics.Bitmap line = LineIcons.get(a, app.packageName, app.icon, a.dp(iconDp));
                    if (line != null) iv.setImageBitmap(line); else iv.setImageDrawable(app.icon);
                } else {
                    iv.clearColorFilter();
                    iv.setImageDrawable(app.icon);
                }
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(a.dp(iconDp), a.dp(iconDp));
                ilp.rightMargin = a.dp(12);
                iv.setLayoutParams(ilp);
                row.addView(iv);
            }

            final TextView name = new TextView(a);
            name.setText(app.label);
            name.setTextSize(15);
            name.setTypeface(look.typeface());
            name.setTextColor(Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            name.setVisibility(look.labelsShown() ? View.VISIBLE : View.GONE);
            row.addView(name);

            row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    rbg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    a.animateFocus(v, hasFocus);
                    if (hasFocus) {
                        pbg.setColors(new int[]{a.brandColor(app.packageName),
                                a.shade(a.brandColor(app.packageName), 0.6f)});
                        if (look.lineLogo()) pbg.setColor(Color.TRANSPARENT);
                        preview.removeAllViews();
                        fillTileContent(a, preview, app, look, 96, true);
                        a.focusTile(v, hasFocus, app);
                    }
                }
            });
            row.setOnClickListener(click(a, app));
            list.addView(row);
        }

        // lista przewijana pilotem do samego dolu
        ScrollView listScroll = new ScrollView(a);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(a.dp(330), a.dp(300));
        slp.rightMargin = a.dp(26);
        listScroll.setLayoutParams(slp);
        listScroll.setVerticalScrollBarEnabled(false);
        listScroll.setClipChildren(false);
        listScroll.setClipToPadding(false);
        listScroll.setPadding(0, a.dp(6), 0, a.dp(6));
        listScroll.addView(list);

        wrap.addView(listScroll);
        wrap.addView(preview);
        host.addView(wrap);
    }


    // ---------------------------------------------------------------- 8. Dok

    /** Dok: rząd pięciu dużych kafli marek przy dolnej krawędzi. */
    private static void dock(MainActivity a, LinearLayout host, Look look) {
        if (a.orderedApps.isEmpty()) return;

        // Dock: wszystkie aplikacje w poziomym pasku, przewijane pilotem.
        HorizontalScrollView scroll = new HorizontalScrollView(a);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(a.dp(8), a.dp(10), a.dp(8), a.dp(10));

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);

        for (int i = 0; i < a.orderedApps.size(); i++) {
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            FrameLayout tile = new FrameLayout(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(a.dp(160), a.dp(112));
            lp.setMargins(a.dp(5), 0, a.dp(5), 0);
            tile.setLayoutParams(lp);
            final GradientDrawable bg = look.tileBackground(a.dp(14), a.brandColor(app.packageName));
            tile.setBackground(bg);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            fillTileContent(a, tile, app, look, 62, true);
            tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    bg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    v.animate().scaleX(hasFocus ? 1.05f : 1f).scaleY(hasFocus ? 1.05f : 1f)
                            .setDuration(120).start();
                    a.focusTile(v, hasFocus, app);
                }
            });
            tile.setOnClickListener(click(a, app));
            row.addView(tile);
        }
        scroll.addView(row);
        host.addView(scroll);
    }


    // ---------------------------------------------------------------- pomocnicze

    /** Wypełnia kafel: znak na środku i nazwa w lewym dolnym rogu. */
    private static void fillTileContent(MainActivity a, FrameLayout tile, MainActivity.AppEntry app,
                                        Look look, int logoDp, boolean withName) {
        if (look.logoShown()) {
            ImageView iv = new ImageView(a);
            int size = look.fullLogo() ? Math.round(logoDp * 1.7f) : look.logoSize(logoDp);
            if (look.lineLogo()) {
                android.graphics.Bitmap line = LineIcons.get(a, app.packageName, app.icon, a.dp(size));
                if (line != null) iv.setImageBitmap(line);
                else iv.setImageDrawable(app.icon);
            } else {
                iv.clearColorFilter();
                iv.setImageDrawable(app.icon);
            }
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(a.dp(size), a.dp(size));
            ilp.gravity = Gravity.CENTER;
            tile.addView(iv, ilp);
        }

        if (withName && look.labelsShown()) {
            TextView name = new TextView(a);
            name.setText(app.label);
            name.setTextSize(14);
            name.setTypeface(look.typeface());
            name.setTextColor(Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setShadowLayer(6f, 0f, 1f, 0x99000000);
            FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            nlp.leftMargin = a.dp(14);
            nlp.rightMargin = a.dp(14);
            nlp.bottomMargin = a.dp(8);
            tile.addView(name, nlp);
        }
    }


    /** Znak aplikacji wyśrodkowany w kaflu (do siatki). */
    private static ImageView logo(MainActivity a, Look look, MainActivity.AppEntry app, int sizeDp) {
        ImageView iv = new ImageView(a);
        int shown = look.fullLogo() ? Math.round(sizeDp * 2.2f) : look.logoSize(sizeDp);
        if (look.lineLogo()) {
            android.graphics.Bitmap line = LineIcons.get(a, app.packageName, app.icon, a.dp(shown));
            if (line != null) iv.setImageBitmap(line);
            else iv.setImageDrawable(app.icon);
        } else {
            iv.clearColorFilter();          // powrót z konturu/bieli do kolorów
            iv.setImageDrawable(app.icon);
        }
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(a.dp(shown), a.dp(shown));
        lp.gravity = Gravity.CENTER;
        iv.setLayoutParams(lp);
        return iv;
    }

    private static TextView caption(MainActivity a, Look look, String text, int sizeSp, boolean center) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(sizeSp);
        t.setTypeface(look.typeface());
        t.setTextColor(Color.WHITE);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setGravity(center ? Gravity.CENTER : Gravity.START);
        t.setPadding(0, a.dp(6), 0, 0);
        t.setShadowLayer(6f, 0f, 1f, 0x99000000);
        return t;
    }

    private static LinearLayout newRow(MainActivity a, int topMarginDp) {
        LinearLayout row = new LinearLayout(a);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = a.dp(topMarginDp);
        row.setLayoutParams(lp);
        return row;
    }

    private static View.OnFocusChangeListener focus(final MainActivity a, final MainActivity.AppEntry app) {
        return new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.animate().scaleX(hasFocus ? 1.04f : 1f).scaleY(hasFocus ? 1.04f : 1f)
                        .setDuration(110).start();
                a.focusTile(v, hasFocus, app);
            }
        };
    }

    private static View.OnClickListener click(final MainActivity a, final MainActivity.AppEntry app) {
        return new View.OnClickListener() {
            @Override public void onClick(View v) { a.launch(app); }
        };
    }

    /** Rząd „Kontynuuj oglądanie" (plakaty z Watch Next). */
    static View continueRow(MainActivity a, Look look) {
        if ("off".equals(look.continueRow)) return null;
        List<MainActivity.ContentEntry> items = a.continueItems;
        if (items == null || items.isEmpty()) return null;

        LinearLayout column = new LinearLayout(a);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = caption(a, look, "Kontynuuj oglądanie", 15, false);
        title.setTypeface(look.typeface());
        title.setPadding(a.dp(24), a.dp(4), 0, a.dp(14));
        column.addView(title);

        HorizontalScrollView scroll = new HorizontalScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(a.dp(18), 0, a.dp(18), 0);

        LinearLayout row = new LinearLayout(a);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(row);

        a.populateContinue(row, items, look);
        column.addView(scroll);
        a.homeContinueRow = row;
        a.homeContinueTitle = title;
        return column;
    }

    /** Łuk: pięć kafli po okręgu, wybrany zawsze na środku; pod nimi kafel "ostatnio oglądane". */
    /** Karuzela: wszystkie aplikacje w pętli po łuku; wybrany zawsze na wierzchu, na środku. */
    private static void arc(final MainActivity a, LinearLayout host, final Look look) {
        if (a.orderedApps.isEmpty()) return;

        final int count = a.orderedApps.size();
        final int[] focus = {0};
        final Runnable[] refreshHolder = new Runnable[1];   // odświeżanie dolnego kafla
        final View[] lastHolder = new View[1];              // dolny kafel (cel strzałki w dół)

        final FrameLayout stage = new FrameLayout(a);
        stage.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(480)));
        stage.setClipChildren(false);

        final java.util.List<FrameLayout> tiles = new java.util.ArrayList<FrameLayout>();
        final float stepDeg = 20f;      // kąt między sąsiednimi kaflami
        final float radiusDp = 760f;    // promień łuku (duży = delikatny łuk)
        final float tileW = 190f, tileH = 112f;

        for (int i = 0; i < count; i++) {
            final int index = i;
            final MainActivity.AppEntry app = a.orderedApps.get(i);
            FrameLayout tile = new FrameLayout(a);
            tile.setLayoutParams(new FrameLayout.LayoutParams(a.dp((int) tileW), a.dp((int) tileH)));
            final GradientDrawable bg = look.tileBackground(a.dp(16), a.brandColor(app.packageName));
            tile.setBackground(bg);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            fillTileContent(a, tile, app, look, 56, false);
            tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    bg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                    if (!hasFocus) return;
                    focus[0] = index;
                    // przestawienie po zakończeniu obsługi fokusu — bez szarpania przy szybkim klikaniu
                    stage.post(new Runnable() {
                        @Override public void run() {
                            layoutRing(stage, tiles, index, stepDeg, radiusDp, tileW, tileH);
                        }
                    });
                    // dolny kafel (plakat) przebudowujemy po ruchu, żeby nie gubić klatek animacji
                    if (refreshHolder[0] != null) {
                        stage.removeCallbacks(refreshHolder[0]);
                        stage.postDelayed(refreshHolder[0], RingMotion.DURATION_MS);
                    }
                }
            });
            tile.setOnKeyListener(new View.OnKeyListener() {
                @Override public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                    if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
                    if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                        tiles.get((index - 1 + count) % count).requestFocus();   // pętla
                        return true;
                    }
                    if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                        tiles.get((index + 1) % count).requestFocus();           // pętla
                        return true;
                    }
                    if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                        a.focusTabs();
                        return true;
                    }
                    if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && lastHolder[0] != null) {
                        lastHolder[0].requestFocus();
                        return true;
                    }
                    return false;
                }
            });
            tile.setOnClickListener(click(a, app));
            tiles.add(tile);
            stage.addView(tile);
        }
        host.addView(stage);

        // Kafel "Kontynuuj oglądanie" dla wybranej aplikacji (pod łukiem, na środku).
        final FrameLayout last = new FrameLayout(a);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(a.dp(322), a.dp(130));
        llp.topMargin = a.dp(14);
        llp.gravity = Gravity.CENTER_HORIZONTAL;
        last.setLayoutParams(llp);
        last.setTranslationY(-a.dp(26));
        final GradientDrawable lbg = look.tileBackground(a.dp(14), 0xFF141414);
        last.setBackground(lbg);
        last.setFocusable(true);
        last.setFocusableInTouchMode(true);
        host.addView(last);
        lastHolder[0] = last;

        final Runnable[] refreshLast = refreshHolder;
        refreshLast[0] = new Runnable() {
            @Override public void run() {
                last.removeAllViews();
                MainActivity.AppEntry app = a.orderedApps.get(focus[0]);
                java.util.List<MainActivity.ContentEntry> items = a.continueFor(app.packageName);
                // pomijamy pozycje, których tytuł to tylko nazwa aplikacji (np. sam "Netflix")
                MainActivity.ContentEntry pick = null;
                for (MainActivity.ContentEntry e : items) {
                    if (e.title == null) continue;
                    if (e.title.trim().equalsIgnoreCase(app.label.trim())) continue;
                    pick = e;
                    break;
                }
                if (pick == null && !items.isEmpty()) pick = items.get(0);
                final MainActivity.ContentEntry recent = pick;
                TextView label = new TextView(a);
                label.setTextSize(13);
                label.setTextColor(Color.WHITE);
                label.setShadowLayer(6f, 0f, 1f, 0xCC000000);
                // długie tytuły (np. odcinki z YouTube) nie mogą zasłaniać plakatu
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                nlp.leftMargin = a.dp(12);
                nlp.rightMargin = a.dp(12);
                nlp.bottomMargin = a.dp(8);

                if (recent == null) {
                    label.setText("Kontynuuj oglądanie — brak pozycji");
                    last.addView(label, nlp);
                    return;
                }
                if (recent.poster != null && !recent.poster.isEmpty()) {
                    RoundedImageView img = new RoundedImageView(a);
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setCornerRadius(a.dp(12));
                    ImageLoader.get().load(recent.poster, img);
                    last.addView(img, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                label.setText("Kontynuuj: " + recent.title);
                last.addView(label, nlp);
                last.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { a.openContent(recent); }
                });
                last.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override public void onFocusChange(View v, boolean hasFocus) {
                        lbg.setStroke(hasFocus ? a.dp(2) : 0, 0xFFFFFFFF);
                        if (hasFocus) {
                            // ramka nad plakatem (plakat przykrywa tło kafla, więc obrys idzie na wierzch)
                            GradientDrawable ring = new GradientDrawable();
                            ring.setCornerRadius(a.dp(14));
                            ring.setColor(Color.TRANSPARENT);
                            ring.setStroke(a.dp(3), 0xFFFFFFFF);
                            last.setForeground(ring);
                            v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).start();
                            a.setBackdropPoster(recent.poster);
                        } else {
                            last.setForeground(null);
                            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        }
                    }
                });
                last.setOnKeyListener(new View.OnKeyListener() {
                    @Override public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN
                                && keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                            tiles.get(focus[0]).requestFocus();
                            return true;
                        }
                        return false;
                    }
                });
            }
        };
        refreshLast[0].run();

        stage.post(new Runnable() {
            @Override public void run() {
                layoutRing(stage, tiles, 0, stepDeg, radiusDp, tileW, tileH);
            }
        });
    }

    /** Łuk jak na rysunku: aktywny na górze, boki opadają łagodnie, kafle poziome i szerokie. */
    private static void layoutRing(FrameLayout stage, java.util.List<FrameLayout> tiles, int focused,
                                   float stepDeg, float radiusDp, float tileW, float tileH) {
        int n = tiles.size();
        int w = stage.getWidth();
        float cx = w > 0 ? w / 2f : 960f;

        int halfN = n / 2;
        int half = Math.min(halfN, 4);  // widoczne -4..4

        RingMotion motion = (RingMotion) stage.getTag();
        if (motion == null || motion.size() != n) {
            motion = new RingMotion(stage, tiles);
            stage.setTag(motion);
        }

        for (int i = 0; i < n; i++) {
            int slot = ((i - focused) % n + n) % n;
            if (slot > halfN) slot -= n;
            boolean shown = Math.abs(slot) <= half;
            // Niewidoczne kafle czekają tuż za krawędzią łuku — stamtąd wjeżdżają i tam znikają.
            int at = shown ? slot : (int) Math.signum((float) slot) * (half + 1);
            int dist = Math.abs(at);
            float scale = Math.max(0.66f, 1f - dist * 0.05f);
            int edge = (slot < 0 ? -1 : 1) * (half + 1);
            float edgeScale = Math.max(0.66f, 1f - (half + 1) * 0.05f);
            motion.target(i, slot, shown,
                    ringX(cx, at, stepDeg, tileW), ringY(at, stepDeg, tileH), scale,
                    ringX(cx, edge, stepDeg, tileW), ringY(edge, stepDeg, tileH), edgeScale,
                    40f - dist * 8f);   // środek na wierzchu, bez bringToFront
        }
        motion.start();
    }

    // Łuk to wycinek elipsy: kafel "slot" stoi pod kątem slot * stepDeg od szczytu.
    // Równy kąt = równe odstępy wzdłuż łuku, a sin/cos dają lustrzane boki.
    private static final float RING_RX = 770f;   // półoś pozioma (px) — szerokość łuku
    private static final float RING_RY = 660f;   // półoś pionowa (px) — jak nisko schodzą boki
    private static final float RING_TOP = 576f;  // środek kafla na szczycie (px od góry sceny)

    /** Lewa krawędź kafla (przed skalowaniem; skala działa wokół środka kafla). */
    private static float ringX(float cx, int slot, float stepDeg, float tileW) {
        double angle = Math.toRadians(slot * stepDeg);
        return cx + RING_RX * (float) Math.sin(angle) - tileW;
    }

    /** Górna krawędź kafla. */
    private static float ringY(int slot, float stepDeg, float tileH) {
        double angle = Math.toRadians(slot * stepDeg);
        return RING_TOP + RING_RY * (1f - (float) Math.cos(angle)) - tileH;
    }

    /**
     * Ruch kafli łuku liczony klatka po klatce (postOnAnimation).
     * Telewizor ma wyłączone animacje systemowe (animator_duration_scale = 0), przy których
     * View.animate() kończy się natychmiast i kafle przeskakują. Tu czas liczymy sami,
     * więc ruch jest płynny niezależnie od tego ustawienia.
     */
    static final class RingMotion implements Runnable {
        private static final long DURATION_MS = 280;

        private final View stage;
        private final java.util.List<FrameLayout> tiles;
        private final float[] fromX, fromY, fromS, fromA;
        private final float[] toX, toY, toS, toA;
        private final int[] lastSlot;
        private final boolean[] shown;
        private boolean placed = false;   // pierwsze ułożenie bez animacji
        private long startMs;

        RingMotion(View stage, java.util.List<FrameLayout> tiles) {
            this.stage = stage;
            this.tiles = tiles;
            int n = tiles.size();
            fromX = new float[n]; fromY = new float[n]; fromS = new float[n]; fromA = new float[n];
            toX = new float[n]; toY = new float[n]; toS = new float[n]; toA = new float[n];
            lastSlot = new int[n];
            shown = new boolean[n];
        }

        int size() { return tiles.size(); }

        /** Cel kafla i: (x, y, scale) na łuku; (ex, ey, es) to miejsce tuż za krawędzią, skąd wjeżdża. */
        void target(int i, int slot, boolean visible, float x, float y, float scale,
                    float ex, float ey, float es, float z) {
            FrameLayout tile = tiles.get(i);
            boolean wasShown = placed && tile.getVisibility() == View.VISIBLE;
            // Skok przez "szew" pętli (np. z prawego końca na lewy) — nie przelatuje przez cały ekran,
            // tylko wjeżdża od właściwej strony.
            boolean wrapped = wasShown && Math.abs(slot - lastSlot[i]) > 1;
            if (!wasShown || wrapped) {
                tile.setX(ex);
                tile.setY(ey);
                tile.setScaleX(es);
                tile.setScaleY(es);
                tile.setAlpha(0f);
            }
            fromX[i] = tile.getX();
            fromY[i] = tile.getY();
            fromS[i] = tile.getScaleX();
            fromA[i] = tile.getAlpha();
            toX[i] = x;
            toY[i] = y;
            toS[i] = scale;
            toA[i] = visible ? 1f : 0f;
            shown[i] = visible;
            lastSlot[i] = slot;
            tile.setRotation(0f);
            tile.setTranslationZ(z);
            if (visible || wasShown) tile.setVisibility(View.VISIBLE);
        }

        void start() {
            stage.removeCallbacks(this);
            if (!placed) {
                placed = true;
                finish();
                return;
            }
            // Na czas ruchu każdy kafel to gotowa tekstura — GPU tylko ją przesuwa i przyciemnia.
            for (FrameLayout tile : tiles) {
                if (tile.getVisibility() == View.VISIBLE && tile.getLayerType() != View.LAYER_TYPE_HARDWARE) {
                    tile.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
            }
            startMs = android.os.SystemClock.uptimeMillis();
            stage.postOnAnimation(this);
        }

        @Override public void run() {
            float t = Math.min(1f, (android.os.SystemClock.uptimeMillis() - startMs) / (float) DURATION_MS);
            float e = 1f - (1f - t) * (1f - t) * (1f - t);   // ease-out: szybki start, miękkie hamowanie
            for (int i = 0; i < tiles.size(); i++) {
                FrameLayout tile = tiles.get(i);
                if (tile.getVisibility() != View.VISIBLE) continue;
                tile.setX(fromX[i] + (toX[i] - fromX[i]) * e);
                tile.setY(fromY[i] + (toY[i] - fromY[i]) * e);
                float s = fromS[i] + (toS[i] - fromS[i]) * e;
                tile.setScaleX(s);
                tile.setScaleY(s);
                tile.setAlpha(fromA[i] + (toA[i] - fromA[i]) * e);
            }
            if (t < 1f) stage.postOnAnimation(this);
            else finish();
        }

        private void finish() {
            for (int i = 0; i < tiles.size(); i++) {
                FrameLayout tile = tiles.get(i);
                tile.setX(toX[i]);
                tile.setY(toY[i]);
                tile.setScaleX(toS[i]);
                tile.setScaleY(toS[i]);
                tile.setAlpha(toA[i]);
                tile.setVisibility(shown[i] ? View.VISIBLE : View.GONE);
                if (tile.getLayerType() != View.LAYER_TYPE_NONE) tile.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }





    /** Rozstawia kafle po łuku wokół wybranego (środkowy największy, skrajne mniejsze i niżej). */
    private static void layoutArc(FrameLayout stage, java.util.List<FrameLayout> tiles, int focused,
                                  float centerX, float spacing, float baseY, float dip,
                                  float tileW, float tileH) {
        int w = stage.getWidth();
        float cx = w > 0 ? w / 2f : centerX * 2f;
        for (int i = 0; i < tiles.size(); i++) {
            FrameLayout tile = tiles.get(i);
            int slot = i - focused;
            if (Math.abs(slot) > 2) {
                tile.setVisibility(View.GONE);
                continue;
            }
            tile.setVisibility(View.VISIBLE);
            float scale = slot == 0 ? 1f : (Math.abs(slot) == 1 ? 0.82f : 0.66f);
            float wpx = tileW * 2f * scale;
            float hpx = tileH * 2f * scale;
            float cy = stage.getHeight() > 0 ? stage.getHeight() / 2f : 260f;
            float tx = cx + slot * spacing * 2f - wpx / 2f;
            float ty = cy - hpx / 2f + slot * slot * dip * 2f;
            tile.animate().x(tx).y(ty).scaleX(scale).scaleY(scale).setDuration(180).start();
        }
    }
    /** Nadaje fokus pierwszemu kaflowi, żeby pilot działał od razu. */
    static void focusFirst(ViewGroup parent) {
        View found = findFocusable(parent, 0);
        if (found != null) found.requestFocus();
    }

    private static View findFocusable(ViewGroup parent, int depth) {
        if (depth > 4) return null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.isFocusable() && child.getVisibility() == View.VISIBLE) return child;
            if (child instanceof ViewGroup) {
                View inner = findFocusable((ViewGroup) child, depth + 1);
                if (inner != null) return inner;
            }
        }
        return null;
    }
}



















































