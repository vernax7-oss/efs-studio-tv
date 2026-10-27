# Layouts and look options

© EFS Studio

Nine home screen layouts. Each one builds the whole screen on its own (tiles, labels, placement)
in `HomeLayouts.java`, using the same app data and the same look options from `Look.java`.
The interface is in Polish for now — Polish names are shown in parentheses.

## Switching with the remote

1. Press **MENU** (or go ↑ to the pills at the top and choose "Wygląd" / Look) — the panel slides in from the right.
2. In the panel: **↑/↓** move between rows, **←/→** change the value, **OK** on "Układ" (Layout),
   "Tło" (Background) and "Projekt" (Preset) opens a list.
3. **BACK** closes the list, a second BACK closes the panel.

## Layouts

| # | Layout (id) | What it looks like |
|---|---|---|
| 1 | **Grid** (`grid`, *Siatka*) | 5 tiles per row with labels under them, scrolls vertically, "Continue watching" row on top |
| 2 | **Icons** (`icons`, *Ikony*) | square icons, 6 per row, scrolls vertically |
| 3 | **Minimal** (`minimal`) | app names only, large text, no tiles |
| 4 | **Logos** (`logos`, *Logotypy*) | logos only in one row at the bottom, big clock in the middle |
| 5 | **Spotlight** (`spotlight`, *Reflektor*) | one big tile in the middle and a strip of logos |
| 6 | **Mosaic** (`mosaic`, *Mozaika*) | one big tile + a column + a row of four, mixed sizes |
| 7 | **Side panel** (`sidebar`, *Panel boczny*) | app list on the left, large preview on the right |
| 8 | **Dock** (`dock`, *Dok*) | a row of tiles along the bottom edge, big clock in the middle |
| 9 | **Arc** (`arc`, *Łuk*) | a carousel along an arc, selected app on top, "Continue watching" tile below; smooth animation |

## Look options (panel)

| Row (Polish) | Values |
|---|---|
| Preset (*Projekt*) | ready-made combinations of layout, tiles, labels, font, clock and background |
| Layout (*Układ*) | the 9 layouts above |
| Background (*Tło*) | blurred poster of the selected app + 16 photos, with a thumbnail |
| Tiles (*Kafelki*) | Glass · Frost · Dark · Clear · App color |
| Labels (*Etykiety*) | Logo and name · Logo only · Name only |
| Logo color (*Kolor logo*) | App color · Outline (white line drawing) · Full tile (app banner) |
| Font (*Czcionka*) | Modern · Elegant · Rounded · Condensed |
| Clock (*Zegar*) | Large · Small · Hidden |
| Continue watching (*Kontynuuj oglądanie*) | Show · Hide |
| About (*O programie*) | version, model, system, number of apps |
| Sony interface / TV settings | open the stock interface (if enabled) or the system settings |

## Testing layouts over ADB

Every option can be set without the remote — handy for screenshots and development:

```powershell
adb shell am start -n studio.efs.braviahome/.MainActivity --es layout arc --es bg bg_03 --es tiles glass
```

Keys: `layout`, `bg` (`poster`, `bg_01`…`bg_16`), `tiles`, `labels`, `logo`, `font`, `clock`, `continue`
(values are the ids from `Look.java`).

## Design rules worth keeping

1. **The screen margin ("mask") is the padding of the main container** in `res/layout/activity_main.xml`.
   It decides how close content gets to the TV's edge — not tile margins or row paddings.
2. **Don't shrink tiles to make things fit.** Manage space with the mask and spacing instead.
3. **Focused tile:** a subtle lift/scale, a shadow and a 2 dp white frame. Rows, columns, ScrollViews and the
   container need `clipChildren=false`, otherwise the frame gets clipped.
4. **The "Continue watching" row is rebuilt on every focus change** from the entries that match the focused
   app (Watch Next + PreviewPrograms) — don't build it once at startup.
5. **When a photo background is selected**, `updateBackdrop()` and `setBackdropPoster()` must return early —
   otherwise the blurred poster covers the photo.
6. **System animations may be turned off** on the TV (`animator_duration_scale = 0`). `View.animate()` then
   finishes instantly. The Arc layout uses its own frame-by-frame animation (`HomeLayouts.RingMotion`) —
   do the same for any animation that matters.

## Brand colors used for tiles

Netflix `#141414`, YouTube/SmartTube `#E50914`, Prime Video `#00A8E1`, Disney+ `#0E1B4D`,
HBO Max `#3A0CA3`, Hulu `#1CE783`, Spotify `#121212`, SkyShowtime `#2B0B4F`,
Eleven Sports `#E23B00`, CANAL+ `#0D0D0D`, PLAY NOW `#6C2BD9`; others come from a 12-color palette.
