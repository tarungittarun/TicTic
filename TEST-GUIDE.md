# MySpace 3.12.24 (61) · MyCalc 1.0.22 (23) — your test guide

This file ships with **every build**. It lists, from your seat as the user, every claim the
build makes: what to tap, what you should see, and which automated test already checks it.
If anything below misbehaves, tell me the section number and share the log
(Settings ▸ Diagnostics ▸ Share) — the log records whether each action actually happened.

Install: [MySpace-v3.12.24-61-lite-dist.apk](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/artifacts/MySpace-v3.12.24-61-lite-dist.apk) ·
[MyCalc-v1.0.22-23.apk](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/artifacts/MyCalc-v1.0.22-23.apk)
(standalone calculator, same engine).

---

## A. The new logo

| # | Do this | Expected |
|---|---|---|
| A1 | Look at the app icon on your home screen / app drawer (uninstall-reinstall or reboot if the launcher caches icons) | The **honeycomb** mark you picked: slate tile, six grey hexagons around one gold one |
| A2 | Open MySpace, look at the top-left of the header | The same honeycomb mark, small, before the word MySpace; tapping it goes home |

Automated: `test_the_honeycomb_mark_the_owner_picked_is_the_icon`.

## B. Selecting and changing the selected portion

| # | Do this | Expected |
|---|---|---|
| B1 | Type `123456`, then **hold your finger down** on the number | The whole number gets a bright pink highlight and a floating Copy/Cut/Paste/Select all pill appears |
| B2 | **Without lifting**, slide your finger left or right | The held portion grows and shrinks with your finger, character by character — this is the gesture that changes the selected portion |
| B3 | Lift, then **press anywhere on the held band** (or just past either end) and drag | The nearer boundary follows your finger; the other end stays put -- this is how you select more or less |
| B4 | With a portion held, tap **Copy**, then open Notes or Messages and long-press ▸ Paste | Exactly the held characters arrive (no commas — commas are display-only) |
| B5 | Hold a portion, tap **Cut**, then type a digit | The held part disappears and your digit lands in the gap |
| B6 | With a selection held, **tap once** anywhere in the expression box | The selection clears, the menu closes, and the caret parks exactly where you tapped -- ready to type there |
| B7 | Hold a portion, then tap **AC** | Held text and menu both clear |
| B8 | While a selection is held, swipe the display horizontally | The selection drag wins over scrolling (the scroller yields while handles are up); with **no** selection the long expression still scrolls sideways |

The selection itself is the framework's (real spans, framework highlight, framework
floating menu); the hold and drag gestures are the app's, because your phone's stock
long-press never raises a selection — your own logs proved it fifty-five times.
Automated: `longPressSelectsTheWholeNumberAndOffersTheMenu`,
`holdThenDragExtendsTheHeldPortion`, `draggingEitherHandleMovesThatBoundary`, `copyPutsExactlyTheHeldSliceInTheClipboard`,
`cutRemovesExactlyTheHeldPortionAndLetsYouTypeThere`,
`aTapElsewhereClosesTheMenuAndClearsTheSelection`, `acWhileHeldClearsTheHeldTextAndTheMenu`.

## C. The expression box

| # | Do this | Expected |
|---|---|---|
| C1 | Type a long expression | It sits at the **top** of the box; the live answer glows beneath it in pink-amber |
| C2 | Look at the typed expression | LED tubes: digits glow **cyan**, operators glow **pink**, brackets **violet**, each with a soft halo; operators glow amber on the pad, `=` is a pink-gradient key |
| C3 | Tap **mid-number** (e.g. inside `12345`) and type | The digit inserts where you tapped, not at the end |
| C4 | Type `10×`, move the caret after the `×`, type `−` | You get `10×−` (negation inserted) — the `×` is **not** eaten; at the end of the line the same keys behave identically |
| C5 | Type `200+10%` then `=` | `220` (Google's percent semantics) |
| C6 | Type `5+3×2` without closing anything, press `=` | `11`; unclosed brackets auto-balance: `(5+3` `=` → `8` |
| C7 | Press `( )` repeatedly with the caret mid-expression | The key offers `(` or `)` whichever fits at the caret |
| C8 | Watch the caret | A solid bar — white in dark, black in light — stands at the exact point the next key will land; type an expression longer than the screen and the box **scrolls itself** to keep the caret in view. Its position is now measured from the glyphs themselves, not from the layout maths your phone mis-reports |
| C9 | Never at any point | No device keyboard appears |

Automated: `minusUnderAMultiplyMidExpressionKeepsBoth`, `percentChainsAndNegationExactlyAsTheOwnersScreenshot`, `theBracketKeyFitsTheCaretNotJustTheEnd`, `midNumberTapInsertsThere`-family, `theCaretExistsIsFocusedAndIsVisible`.

## D. Calculator behaviour (Google parity)

| # | Do this | Expected |
|---|---|---|
| D1 | Keys `2 ( ) 5 + 2 ( ) 2 =` | `28` (implicit multiplication) |
| D2 | `10+` then `−` | `10−` — a pending operator is a changeable mind |
| D3 | `5+` then `=` | `10` (Google doubles the operand) |
| D4 | `123456` | Displays `1,23,456` (Indian grouping, display-only) |
| D5 | Chevron above the pad | Grows `√ π ^ ! / Deg sin cos tan / Inv e ln log`; tap again to fold |
| D6 | `Inv` then `sin` | `sin⁻¹` and it evaluates; `Deg` toggles Deg/Rad |
| D7 | `AC` | Clears everything, including any held selection |
| D8 | `⌫` | Deletes one character at the caret |
| D9 | Tap any **history card** | Its expression lands in the box AND still evaluates: press `=` and the answer returns (no `Error`) -- commas never re-enter the maths |

## E. History, memory, effects

| # | Do this | Expected |
|---|---|---|
| E1 | Header ▸ history icon (left) | Popup of **all** your past calculations, newest at the bottom, soft cards, expression left / time right; tap a card → its expression returns to the box |
| E2 | Swipe down on the expression | Same history popup |
| E3 | 3-dot menu (right) | Exactly five items plus Effects; Memory opens a small popup you can reorder |
| E4 | Effects | Choose an animation; every keypress animates (≥4 styles) |
| E5 | The whole screen | Never scrolls as a page; only the expression line and popups scroll inside themselves |

## F. MyCalc standalone

Same calculator, its own icon, zero permissions. Everything in B, C, D applies.

## G. The rest of MySpace (quick pass)

Notes card wall with pins/colours/archive · Calendar year picker + week/day views + reminders ·
Music library with notification transport that survives leaving the screen · Recorder that
self-tests every save · Scanner capture → four-handle crop → PDF · Files with a real text
editor · Log collector in Settings ▸ Diagnostics with local save + share.

---

## H. Light mode, dark mode, and the caret

| # | Do this | Expected |
|---|---|---|
| H1 | Settings ▸ Theme ▸ **Light** | Pad digits near-black on white keys; the typed expression and the live answer are **pitch black**; the caret is a black bar; operators deep amber; AC label black on lavender |
| H2 | Settings ▸ Theme ▸ **Dark** | The typed expression glows LED-bright — cyan digits, pink operators, violet brackets, each with a halo; the live answer glows amber; operators bright amber; the caret is a white bar |
| H3 | In either theme, tap into the expression | The caret bar stands visibly at the exact point the next key will land -- never parked out in the empty margin |
| H4 | Switch themes with an expression typed | Every colour re-resolves at once; nothing keeps the other theme's ink |

Automated: `theCaretAndInkAreActuallyPaintedInBothThemes` draws the display to a bitmap in
each theme and counts caret and ink pixels -- a colour that exists in code but not on the
canvas fails the build.

## If something fails

1. Note the section code (e.g. **B2**).
2. Settings ▸ Diagnostics ▸ Share the log — it records each gesture's *outcome*
   (e.g. `selection started: chars 0..6 held by the app gesture`,
   `hold-then-drag began`, `selection drag ended: chars 0..4 held`,
   `caret at x=… viewW=… scrollX=… viewport=…`).
3. A screenshot of the screen as it looks.

Every claim above is also pinned by an automated test: 387 Python contracts, 21 JVM suites
and a 37-story instrumented suite run in CI on every push; the instrumented stories inject
real finger gestures at window level, through the same view hierarchy your thumb crosses.
