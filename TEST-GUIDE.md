# MySpace 3.12.37 (74) · MyCalc 1.0.35 (36) — your test guide

This file ships with **every build**. It lists, from your seat as the user, every claim the
build makes: what to tap, what you should see, and which automated test already checks it.
If anything below misbehaves, tell me the section number and share the log
(Settings ▸ Diagnostics ▸ Share) — the log records whether each action actually happened.

Install: [MySpace-v3.12.37-74-lite-dist.apk](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/artifacts/MySpace-v3.12.37-74-lite-dist.apk) ·
[MyCalc-v1.0.35-36.apk](https://github.com/tarungittarun/TicTic/raw/arena/3caca636-tictic/artifacts/MyCalc-v1.0.35-36.apk)
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
| B1 | Type `123456`, then **hold your finger still** on the number (about a third of a second) and lift | The whole number gets a bright pink highlight, a floating Cut/Copy/MAdd/MUse/Paste/Select all pill, and **white bars with a bulb under each end** -- and it **stays selected after you lift**, every time: the app's own hold timer raises it, not the phone's temperamental long-press |
| B2 | **Without lifting**, slide your finger left or right | The held portion grows and shrinks with your finger, character by character — this is the gesture that changes the selected portion |
| B3 | Lift, then **press anywhere on the held band** (or just past either end) and drag | The nearer boundary follows your finger; the other end stays put -- this is how you select more or less. **Both ends work even when the selection touches the last digit**: the right teardrop now sits 10dp inside the box edge, on the glass, never off-screen |
| B4 | With a portion held, tap **Copy**, then open Notes or Messages and long-press ▸ Paste | Exactly the held characters arrive (no commas — commas are display-only) |
| B5 | Hold a portion, tap **Cut**, then type a digit | The held part disappears and your digit lands in the gap |
| B6 | With a selection held, **tap once anywhere in the expression box -- on the held rectangle or off it** -- even if your thumb wobbles | The selection and menu release, and the caret parks exactly where you tapped; selection and cursor never coexist |
| B7 | Hold a portion, then tap **AC** | Held text and menu both clear |
| B8 | While a selection is held, swipe the display horizontally | The selection drag wins over scrolling; with **no** selection the long expression still slides sideways inside its own line |
| B9 | With nothing selected, **tap inside a typed number** | The caret jumps to that exact spot -- your reference for where typing and editing happen; the next key lands there |
| B10 | With a selection held, tap **away from the expression line** (the live-answer row or the box margins) | Selection and menu both go; the caret stands at the end |
| B11 | **Hold the EMPTY part of the expression box** -- left of the written expression, or the **whole-character of empty glass kept on the right** | **Nothing gets selected.** The caret parks at the near end and a small caret menu rises: **MUse, Paste, Select all** (Google's own caret trio) -- no operator jumps into a highlight anymore, and the right margin is wide enough to hold for Paste/MUse |
| B12 | Hold a number, tap **MAdd**; then tap to park the caret mid-expression, hold the empty box, tap **MUse** | MAdd stores the held value in memory (toast "Added to memory"); MUse inserts the recently saved value **at the caret**, mid-expression, exactly like the Google calculator screenshot you shared. With a portion held, MUse replaces just that portion |

The selection itself is the framework's (real spans, framework highlight, framework
floating menu); the hold and drag gestures are the app's, because your phone's stock
long-press never raises a selection — your own logs proved it fifty-five times.
Automated: `longPressSelectsTheWholeNumberAndOffersTheMenu`,
`holdThenDragExtendsTheHeldPortion`, `draggingEitherHandleMovesThatBoundary`, `aPlainTapParksTheCaretAndReleasesTheSelection`, `jitterTapStillClearsTheHeldSelection`,
`tapAwayFromTheExpressionClears`, `aLongPressOnEmptyBoxParksTheCaretAndRaisesTheCaretMenu`,
`memStoresTheHeldNumberAndMrInsertsItAtTheCaret`, `theCaretStandsSolidThenBlinksLikeAndroidsOwn`, `aTapOnTheHeldPortionParksTheCaretAndReleasesTheSelection`,
`aHoldInTheRightMarginRaisesTheCaretMenuNotTheLastOperand`, `theKeyShapePickerShowsLivePreviewsAndPersistsThePick`,
`noShapeKeysArePlainGlassAndCircleIsARoundDisc`, `theKeyThemePickerRecoloursByGroupAndFollowsMySpace`,
`aSingleStrokeFromTheDotsRunsTheHighlightedLeaf`, `aSingleStrokeTravelsIntoKeyShapeWithoutLifting`,
`theRightBoundaryStaysOnGlassWhenTheSelectionReachesTheEnd`, `theCaretBarIsARealViewThatBlinksWithThePhase`,
`copyPutsExactlyTheHeldSliceInTheClipboard`,
`cutRemovesExactlyTheHeldPortionAndLetsYouTypeThere`,
`aTapElsewhereClosesTheMenuAndClearsTheSelection`, `acWhileHeldClearsTheHeldTextAndTheMenu`.

## C. The expression box

| # | Do this | Expected |
|---|---|---|
| C1 | Type a long expression | It sits at the **top** of the box; the live answer glows beneath it in pink-amber |
| C2 | Look at the typed expression | LED tubes: digits glow **cyan**, operators glow **pink**, brackets **violet**, each with a soft halo; operators glow amber on the pad, `=` is a pink-gradient key |
| C3 | **Tap mid-number** (e.g. inside `12345`) and type | The digit inserts where you tapped, not at the end -- the tap parks the caret there |
| C4 | Type `10×`, move the caret after the `×`, type `−` | You get `10×−` (negation inserted) — the `×` is **not** eaten; at the end of the line the same keys behave identically |
| C5 | Type `200+10%` then `=` | `220` (Google's percent semantics) |
| C6 | Type `5+3×2` without closing anything, press `=` | `11`; unclosed brackets auto-balance: `(5+3` `=` → `8` |
| C7 | Press `( )` repeatedly with the caret mid-expression | The key offers `(` or `)` whichever fits at the caret |
| C8 | Watch the caret | A bar — white in dark, black in light — stands at the exact point the next key will land, **including at the very end of the line** (the box keeps 10dp of side padding so the end caret is on the glass), and it **blinks like Android's own cursor**: solid while you type or tap, half-second blink at rest, hidden only while a selection is held (the boundary bars stand then). From this build the caret is also **a real view** layered over the display -- the framework itself composites it, so no drawing path can swallow it -- and every placement and blink writes a line to the log; type an expression longer than the screen and the expression line **scrolls itself** to keep the caret in view. The display is now a plain single-line editor (no scroll wrapper), so its caret, handles and scrolling are the platform's own |
| C9 | Never at any point | No device keyboard appears |
| C10 | Three dots ▸ **Key shape** | The picker opens in the app's own dark pill style, each row showing a live swatch of its silhouette: **Circle** (a true round disc), **Squircle**, **Pill**, **Rounded square**, **No shape** (plain keys, no border -- pressing one flashes a rectangle). Your pick persists and the pad rebuilds instantly. Hexagon is gone by your order. (Add-to-memory left the three dots last round; storing lives in the selection menu as **MAdd**.) |
| C11 | Three dots ▸ **Key theme** | Five colour themes for the pad, applied by group -- numbers, operations (+ − × ÷ % brackets), scientific keys, and AC/backspace: **MySpace settings** (follows the app appearance, the default), **Graphite**, **Ocean**, **Sunset**, **Mint**. Every row previews its own three group chips; every theme keeps digits and symbols clearly readable in dark AND light mode (a contrast guard flips any ink that sits too close to its fill). Your pick persists and the pad rebuilds instantly. |
| C12 | **One stroke through the three dots**: press the dots and KEEP HOLDING, drag down the list (the row under your finger highlights), lift on the row you want | The action fires on lift -- no second tap. Hold still on a row that has a child (**Memory, Key theme, Key shape, Effects**) and the child's list swaps in under your finger; keep dragging into it and lift there (e.g. dots ▸ hold on Key shape ▸ drag to Pill ▸ lift = Pill applied, one unbroken stroke). **A plain tap on the dots still opens the old tap-through menu**, exactly as before -- both accesses live side by side. Memory's child also holds **Clear memory** at the bottom now. |

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
| D10 | Swipe the history open and tap the expression line in the same breath | Nothing crashes: a tap that lands while the popup repopulates is guarded, logged, and ignored |

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

Every claim above is also pinned by an automated test: 389 Python contracts, 21 JVM suites
and a 57-story instrumented suite run in CI on every push; the instrumented stories inject
real finger gestures at window level, through the same view hierarchy your thumb crosses.
