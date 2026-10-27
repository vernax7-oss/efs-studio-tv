/*
 * Panel — wysuwany z prawej strony panel wyglądu, wzorowany na „Make it yours" z Novej.
 * © EFS Studio
 *
 * Wiersze („paseczki") są ciemnymi pastylkami: nazwa po lewej, wartość po prawej.
 * Strzałki lewo/prawo zmieniają wartość, OK otwiera listę wyboru (układ, tło, informacje).
 * Panel jest częścią ekranu głównego — nie ma osobnej zakładki Ustawienia.
 *
 * Uwaga: brak lambd (android.jar bez LambdaMetafactory), więc wszystko na klasach anonimowych.
 */
package studio.efs.braviahome;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

class Panel {

    /** Jeden wiersz panelu. */
    abstract static class Item {
        abstract String label();
        abstract String value();
        void left() { }
        void right() { }
        void open() { }
        /** Miniatura po prawej (np. podgląd tła); null = brak. */
        android.graphics.drawable.Drawable thumb() { return null; }
    }

    private final MainActivity a;
    private final LinearLayout rowsBox;
    private final LinearLayout picker;
    private final View dim;
    private final View panel;
    private final List<Item> items = new ArrayList<>();
    private final List<View> rowViews = new ArrayList<>();
    private final List<ImageView> thumbViews = new ArrayList<>();
    private int index = 0;

    Panel(MainActivity activity, View panelView, LinearLayout rows, LinearLayout pickerView, View dimView) {
        this.a = activity;
        this.panel = panelView;
        this.rowsBox = rows;
        this.picker = pickerView;
        this.dim = dimView;
    }

    boolean isOpen() { return panel.getVisibility() == View.VISIBLE; }

    boolean isPickerOpen() { return picker.getVisibility() == View.VISIBLE; }

    void add(Item item) {
        items.add(item);
    }

    /** Buduje paseczki — wywoływane po dodaniu wszystkich pozycji. */
    void build() {
        rowsBox.removeAllViews();
        rowViews.clear();
        thumbViews.clear();
        addHint(rowsBox);   // podpowiedź na górze, pod tytułem panelu
        for (int i = 0; i < items.size(); i++) {
            final int position = i;
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, a.dp(37));
            lp.bottomMargin = a.dp(6);
            row.setLayoutParams(lp);
            row.setPadding(a.dp(16), 0, a.dp(14), 0);
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            row.setBackground(pill(false));

            TextView name = new TextView(a);
            name.setText(items.get(i).label());
            name.setTextSize(15);
            name.setTextColor(Color.WHITE);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);

            ImageView thumb = new ImageView(a);
            thumb.setLayoutParams(new LinearLayout.LayoutParams(a.dp(64), a.dp(36)));
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setPadding(0, 0, a.dp(10), 0);
            thumb.setVisibility(View.GONE);
            row.addView(thumb);
            thumbViews.add(thumb);

            TextView arrows = new TextView(a);
            arrows.setText("‹");
            arrows.setTextSize(16);
            arrows.setTextColor(0xB3FFFFFF);
            arrows.setPadding(0, 0, a.dp(8), 0);
            row.addView(arrows);

            TextView value = new TextView(a);
            value.setText(items.get(i).value());
            value.setTextSize(14);
            value.setTextColor(0xCCFFFFFF);
            row.addView(value);

            TextView arrowsRight = new TextView(a);
            arrowsRight.setText("›");
            arrowsRight.setTextSize(16);
            arrowsRight.setTextColor(0xB3FFFFFF);
            arrowsRight.setPadding(a.dp(8), 0, 0, 0);
            row.addView(arrowsRight);

            final TextView valueView = value;
            final TextView leftArrow = arrows;
            final TextView rightArrow = arrowsRight;

            row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    index = position;
                    v.setBackground(pill(hasFocus));
                    ((TextView) ((LinearLayout) v).getChildAt(0)).setTextColor(hasFocus ? Color.BLACK : Color.WHITE);
                    valueView.setTextColor(hasFocus ? 0xCC000000 : 0xCCFFFFFF);
                    leftArrow.setTextColor(hasFocus ? 0x99000000 : 0xB3FFFFFF);
                    rightArrow.setTextColor(hasFocus ? 0x99000000 : 0xB3FFFFFF);
                }
            });
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { items.get(position).open(); refresh(); }
            });
            row.setOnKeyListener(new View.OnKeyListener() {
                @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { items.get(position).left(); refresh(); return true; }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { items.get(position).right(); refresh(); return true; }
                    return false;
                }
            });

            rowsBox.addView(row);
            rowViews.add(row);
        }
    }

    /** Odświeża wartości po zmianie ustawienia. */
    void refresh() {
        for (int i = 0; i < items.size() && i < rowViews.size(); i++) {
            LinearLayout row = (LinearLayout) rowViews.get(i);
            TextView value = (TextView) row.getChildAt(3);
            value.setText(items.get(i).value());
            ImageView thumb = thumbViews.get(i);
            android.graphics.drawable.Drawable d = items.get(i).thumb();
            if (d != null) { thumb.setImageDrawable(d); thumb.setVisibility(View.VISIBLE); }
            else { thumb.setVisibility(View.GONE); }
        }
    }

    void open() {
        panel.setVisibility(View.VISIBLE);
        dim.setVisibility(View.VISIBLE);
        refresh();
        index = 0;
        highlight();
        final View first = rowViews.isEmpty() ? null : rowViews.get(0);
        if (first != null) panel.post(new Runnable() {
            @Override public void run() { first.requestFocus(); }
        });
    }

    /** Obsługa pilota niezależna od tego, gdzie system położy fokus. */
    boolean handleKey(int keyCode) {
        if (isPickerOpen()) return handlePickerKey(keyCode);
        if (!isOpen()) return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN: index = (index + 1) % items.size(); highlight(); return true;
            case KeyEvent.KEYCODE_DPAD_UP: index = (index - 1 + items.size()) % items.size(); highlight(); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: items.get(index).left(); refresh(); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: items.get(index).right(); refresh(); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: items.get(index).open(); refresh(); return true;
            default: return false;
        }
    }

    /** Podświetla wybrany paseczek i przenosi na niego fokus. */
    private void highlight() {
        for (int i = 0; i < rowViews.size(); i++) {
            LinearLayout row = (LinearLayout) rowViews.get(i);
            boolean active = (i == index);
            row.setBackground(pill(active));
            ((TextView) row.getChildAt(0)).setTextColor(active ? Color.BLACK : Color.WHITE);
            ((TextView) row.getChildAt(3)).setTextColor(active ? 0xCC000000 : 0xCCFFFFFF);
            ((TextView) row.getChildAt(2)).setTextColor(active ? 0x99000000 : 0xB3FFFFFF);
            ((TextView) row.getChildAt(4)).setTextColor(active ? 0x99000000 : 0xB3FFFFFF);
        }
        if (index < rowViews.size()) rowViews.get(index).requestFocus();
    }

    private LinearLayout pickerColumn;
    private int pickerIndex = 0;
    private int pickerCount = 0;
    private OnPick pickerCallback;

    private boolean handlePickerKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                pickerIndex = Math.min(pickerIndex + 1, pickerCount - 1);
                focusPickerRow();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                pickerIndex = Math.max(pickerIndex - 1, 0);
                focusPickerRow();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (pickerCallback != null) pickerCallback.picked(pickerIndex);
                closePicker();
                refresh();
                return true;
            default:
                return true;
        }
    }

    private void focusPickerRow() {
        if (pickerColumn != null && pickerIndex + 1 < pickerColumn.getChildCount()) {
            pickerColumn.getChildAt(pickerIndex + 1).requestFocus();
        }
    }

    void close() {
        panel.setVisibility(View.GONE);
        dim.setVisibility(View.GONE);
        closePicker();
        a.onPanelClosed();
    }

    // ---------------------------------------------------------------- lista wyboru

    interface OnPick { void picked(int which); }

    /** Podpowiedź na dole panelu: jak chodzić pilotem. */
    private void addHint(LinearLayout box) {
        TextView hint = new TextView(box.getContext());
        hint.setText("↑↓ wybór   ←→ zmiana   OK lista   BACK wyjście");
        hint.setTextSize(10);
        hint.setTextColor(0x8AFFFFFF);
        hint.setPadding(a.dp(14), 0, 0, a.dp(6));
        box.addView(hint);
    }

    void showPicker(String title, final String[] options, int current, final OnPick onPick) {
        showPicker(title, options, null, current, onPick);
    }


    void showPicker(String title, final String[] options, final String[] hints, int current, final OnPick onPick) {
        picker.removeAllViews();
        pickerIndex = current;
        pickerCount = options.length;
        pickerCallback = onPick;

        TextView head = new TextView(a);
        head.setText(title);
        head.setTextSize(18);
        head.setTextColor(Color.WHITE);
        head.setPadding(a.dp(6), a.dp(6), 0, a.dp(10));
        // nagłówek przypięty nad przewijaną listą, żeby pozycje nie wjeżdżały na tytuł
        head.setBackgroundColor(0xFF101216);
        head.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        head.setTranslationZ(20f);
        picker.addView(head);

        // Długie listy (układy, tła) przewijają się pod nagłówkiem.
        ScrollView scroll = new ScrollView(a);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(430)));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(true);
        scroll.setPadding(0, a.dp(12), 0, a.dp(6));
        pickerColumn = new LinearLayout(a);
        pickerColumn.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(pickerColumn);
        picker.addView(scroll);

        for (int i = 0; i < options.length; i++) {
            final int which = i;
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    hints != null ? ViewGroup.LayoutParams.WRAP_CONTENT : a.dp(38));
            lp.bottomMargin = a.dp(3);
            row.setLayoutParams(lp);
            row.setPadding(a.dp(14), a.dp(6), a.dp(14), a.dp(6));
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            row.setBackground(pill(i == current));

            TextView text = new TextView(a);
            text.setText(hints != null && i < hints.length && hints[i] != null
                    ? options[i] + "\n" + hints[i] : options[i]);
            text.setTextSize(15);
            text.setTextColor(i == current ? Color.BLACK : Color.WHITE);
            row.addView(text);

            final TextView textView = text;
            row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(final View v, boolean hasFocus) {
                    v.setBackground(pill(hasFocus || which == -1));
                    textView.setTextColor(hasFocus ? Color.BLACK : (which == -1 ? Color.BLACK : Color.WHITE));
                    if (hasFocus) {
                        // przewiń tak, żeby pod wybraną pozycją był jeszcze kawałek następnej
                        v.post(new Runnable() {
                            @Override public void run() {
                                v.requestRectangleOnScreen(
                                        new Rect(0, 0, v.getWidth(), v.getHeight() + a.dp(40)), false);
                            }
                        });
                    }
                }
            });
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    onPick.picked(which);
                    closePicker();
                    refresh();
                }
            });
            pickerColumn.addView(row);
            if (i == current) row.requestFocus();
        }
        picker.setVisibility(View.VISIBLE);
        dim.setVisibility(View.VISIBLE);
    }

    void closePicker() {
        picker.setVisibility(View.GONE);
        if (isOpen() && !rowViews.isEmpty() && index < rowViews.size()) rowViews.get(index).requestFocus();
        else if (!isOpen()) dim.setVisibility(View.GONE);
    }

    // ---------------------------------------------------------------- wygląd paseczka

    private GradientDrawable pill(boolean active) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(a.dp(14));
        d.setColor(active ? 0xFFF2F2F4 : 0xFF1B1D22);
        return d;
    }
}









