package app.space.feature.clock;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.space.core.data.AppActivityLog;
import app.space.core.data.AppPrefs;
import app.space.core.design.Palette;
import app.space.core.design.UiKit;
import app.space.core.domain.TimeMath;
import app.space.core.platform.PowerState;
import app.space.core.platform.SystemBoot;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * The Stopwatch screen — the first real {@link Screen} in MySpace, and the reference for
 * how every later app screen is written (SUITE-PLAN.md §6.1 step 1h-iv).
 *
 * <p>Moved out of {@code MainActivity.buildStopwatch()} with its eight supporting methods.
 * Chosen as the first screen because it is the only tab with no scheduler, no widget
 * refresh and no editor dialog: all of its state lives in preferences rather than in
 * Activity fields, so it could be lifted without dragging the alarm or routine machinery
 * along.
 *
 * <p>Two things a suite screen must do that an Activity tab never had to:
 *
 * <ul>
 *   <li><b>Own its clock.</b> {@code MainActivity} drove every live readout from one shared
 *       ticker whose rate came from {@code nextTickDelay()}. A screen cannot reach into
 *       that, so this one runs its own {@link Handler} between {@link #onResume()} and
 *       {@link #onPause()}, reproducing the original three rates exactly — 16 ms while
 *       running, which the hundredths display needs, 100 ms under battery saver, and 1 s
 *       paused. Getting this wrong is a visible stutter or a battery drain, and neither
 *       would fail a test.</li>
 *   <li><b>Reach the host only through {@link ScreenHost}.</b> Re-render after a state
 *       change is {@code host.invalidate()}, not a call to the Activity's {@code render()},
 *       and colours come from {@code host.palette()} rather than Activity fields.</li>
 * </ul>
 */
public final class StopwatchScreen implements Screen {

    private final ScreenHost host;
    private final UiKit ui;
    private final Palette palette;
    private final Context context;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView live;

    /** Drives the elapsed readout. Started and stopped with the screen's own lifecycle. */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (live != null) live.setText(display(elapsed()));
            handler.postDelayed(this, tickDelay());
        }
    };

    public StopwatchScreen(ScreenHost host) {
        if (host == null) throw new IllegalArgumentException("StopwatchScreen requires a ScreenHost");
        this.host = host;
        this.context = host.context();
        this.palette = host.palette();
        this.ui = new UiKit(context, palette);
    }

    @Override
    public String title() {
        return "Stopwatch";
    }

    /**
     * Tick rate, matching {@code MainActivity.nextTickDelay()} for this tab.
     *
     * <p>Paused under battery saver drops to once a minute rather than once a second: the
     * original checked power-saver state before the paused-tab branch, so this reproduces
     * that ordering rather than the simpler two-case version.
     */
    private long tickDelay() {
        boolean saver = PowerState.isOptimizing(context);
        if (running()) return saver ? 100L : 16L;
        return saver ? 60000L : 1000L;
    }

    @Override
    public void onResume() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(ticker);
    }

    @Override
    public View build(Context buildContext) {
        LinearLayout root = ui.vbox();
        ui.sectionTitle(root, "Stopwatch", null);
        ui.gap(root, 18);

        LinearLayout c = ui.vbox();
        c.setGravity(Gravity.CENTER);
        c.setPadding(ui.dp(16), ui.dp(25), ui.dp(16), ui.dp(20));
        c.setBackground(ui.shape(palette.surface, 26));
        root.addView(c, ui.lp(-1, 270));

        TextView kicker = ui.label("ELAPSED TIME", 10, palette.muted, true);
        kicker.setLetterSpacing(.15f);
        c.addView(kicker);

        live = ui.label(display(elapsed()), 48, palette.text, true);
        live.setGravity(Gravity.CENTER);
        c.addView(live, ui.lp(-1, 100));

        LinearLayout acts = ui.hbox();
        c.addView(acts, ui.lp(-1, 58));
        TextView primary = ui.pillButton(running() ? "Pause" : "Start", palette.accent, Color.WHITE);
        acts.addView(primary, new LinearLayout.LayoutParams(0, ui.dp(50), 1));
        TextView lap = ui.pillButton(running() ? "Lap" : "Reset",
                palette.dark ? 0xff293b37 : 0xffedf3f1, palette.text);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(0, ui.dp(50), 1);
        ll.leftMargin = ui.dp(10);
        acts.addView(lap, ll);

        primary.setOnClickListener(v -> {
            buttonHaptic(v);
            if (running()) pause(); else start();
            host.invalidate();
        });
        lap.setOnClickListener(v -> {
            if (running()) {
                if (AppPrefs.prefs(context).getBoolean("stopwatch_lap_haptics", true)) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                }
                addLap();
            } else {
                buttonHaptic(v);
                reset();
            }
            host.invalidate();
        });

        ui.gap(root, 18);
        LinearLayout r = ui.hbox();
        TextView lt = ui.label("Laps", 19, palette.text, true);
        r.addView(lt, new LinearLayout.LayoutParams(0, ui.dp(34), 1));
        JSONArray laps = AppPrefs.array(context, "laps");
        r.addView(ui.label(laps.length() + " recorded", 12, palette.muted, false));
        root.addView(r);
        ui.gap(root, 6);

        if (laps.length() == 0) {
            LinearLayout e = ui.card();
            e.addView(ui.label("Your lap times will appear here.", 13, palette.muted, false));
            root.addView(e, ui.lp(-1, 56));
        } else {
            for (int i = laps.length() - 1; i >= 0; i--) {
                JSONObject lapObj = laps.optJSONObject(i);
                if (lapObj == null) continue;
                LinearLayout row = ui.hbox();
                row.setPadding(ui.dp(14), 0, ui.dp(14), 0);
                row.setBackground(ui.shape(palette.surface, 15));
                LinearLayout.LayoutParams p = ui.lp(-1, 52);
                p.bottomMargin = ui.dp(5);
                root.addView(row, p);
                row.addView(ui.label(String.format(Locale.getDefault(), "Lap %02d", lapObj.optInt("n")),
                        13, palette.muted, true), new LinearLayout.LayoutParams(0, -1, 1));
                row.addView(ui.label(TimeMath.formatHundredths(lapObj.optLong("ms")), 15, palette.text, true));
            }
        }
        return root;
    }

    // ---- state, all of it in preferences so a reboot or a process death cannot lose it ----

    private boolean running() {
        return AppPrefs.prefs(context).getBoolean("sw_running", false);
    }

    /**
     * Elapsed time from a monotonic base, corrected across a reboot.
     *
     * <p>{@code SystemClock.elapsedRealtime()} resets to zero on reboot while
     * {@code currentTimeMillis()} does not, so comparing a saved elapsed time against the
     * current one across a restart yields a wildly wrong duration. The boot count is what
     * makes a reboot detectable; {@link TimeMath#stopwatchElapsed} does the arithmetic.
     *
     * <p>The migrate-on-read branch is for installs saved before the monotonic base existed:
     * it seeds the new keys from the wall-clock value once, then behaves normally.
     */
    private long elapsed() {
        android.content.SharedPreferences prefs = AppPrefs.prefs(context);
        long saved = prefs.getLong("sw_elapsed", 0);
        if (!running()) return saved;
        long nowElapsed = SystemClock.elapsedRealtime(), nowWall = System.currentTimeMillis();
        long startElapsed = prefs.getLong("sw_start_elapsed", 0);
        long startWall = prefs.getLong("sw_start_wall", prefs.getLong("sw_start", 0));
        int startBoot = prefs.getInt("sw_boot_count", -1), currentBoot = SystemBoot.count(context);
        if (startElapsed <= 0 && startWall > 0) {
            long migrated = TimeMath.stopwatchElapsed(saved, 0, startWall, nowElapsed, nowWall, -1, currentBoot);
            prefs.edit().putLong("sw_elapsed", migrated).putLong("sw_start_elapsed", nowElapsed)
                    .putLong("sw_start_wall", nowWall).putLong("sw_start", nowWall)
                    .putInt("sw_boot_count", currentBoot).apply();
            return migrated;
        }
        return TimeMath.stopwatchElapsed(saved, startElapsed, startWall, nowElapsed, nowWall, startBoot, currentBoot);
    }

    /**
     * The readout: hundredths by default, or mm:ss / hh:mm:ss when the user turns that off.
     *
     * <p>{@code Locale.getDefault()} is correct here and {@code Locale.ROOT} would be wrong
     * — these are digits shown to a person, but the formatting must not vary with locale in
     * a way that changes the width of the readout mid-run.
     */
    private String display(long ms) {
        if (AppPrefs.prefs(context).getBoolean("stopwatch_show_hundredths", true)) {
            return TimeMath.formatHundredths(ms);
        }
        long seconds = Math.max(0, ms / 1000), minutes = seconds / 60, ss = seconds % 60;
        long h = minutes / 60, m = minutes % 60;
        return h > 0 ? String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, ss)
                : String.format(Locale.getDefault(), "%02d:%02d", minutes, ss);
    }

    private void start() {
        long nowWall = System.currentTimeMillis(), nowElapsed = SystemClock.elapsedRealtime();
        int boot = SystemBoot.count(context);
        AppPrefs.prefs(context).edit().putBoolean("sw_running", true).putLong("sw_start", nowWall)
                .putLong("sw_start_wall", nowWall).putLong("sw_start_elapsed", nowElapsed)
                .putInt("sw_boot_count", boot).apply();
        AppActivityLog.log(context, "Stopwatch", "Stopwatch started", "Timing began");
    }

    private void pause() {
        long e = elapsed();
        AppPrefs.prefs(context).edit().putBoolean("sw_running", false).putLong("sw_elapsed", e)
                .putLong("sw_start", 0).remove("sw_start_wall").remove("sw_start_elapsed")
                .remove("sw_boot_count").apply();
        AppActivityLog.log(context, "Stopwatch", "Stopwatch paused", TimeMath.formatHundredths(e));
    }

    private void reset() {
        long e = elapsed();
        AppPrefs.prefs(context).edit().putBoolean("sw_running", false).putLong("sw_elapsed", 0)
                .putLong("sw_start", 0).putLong("sw_last_lap", 0).remove("sw_start_wall")
                .remove("sw_start_elapsed").remove("sw_boot_count").apply();
        AppPrefs.prefs(context).edit().remove("laps").apply();
        if (e > 0) {
            AppActivityLog.log(context, "Stopwatch", "Stopwatch reset", TimeMath.formatHundredths(e));
        }
    }

    /**
     * Records a lap with both its absolute time and its split from the previous lap.
     *
     * <p>The split is what a lap list is actually for; absolute times alone make each row
     * useless without mental arithmetic. Newest laps render first, so the list reads in the
     * order someone watching a stopwatch expects.
     */
    private void addLap() {
        long e = elapsed(), prev = AppPrefs.prefs(context).getLong("sw_last_lap", 0);
        JSONArray a = AppPrefs.array(context, "laps");
        try {
            a.put(new JSONObject().put("n", a.length() + 1).put("ms", e).put("split", e - prev));
        } catch (Exception ignored) {
        }
        AppPrefs.putArray(context, "laps", a);
        AppPrefs.prefs(context).edit().putLong("sw_last_lap", e).apply();
        AppActivityLog.log(context, "Stopwatch", "Lap recorded",
                "Lap " + a.length() + " · " + TimeMath.formatHundredths(e));
    }

    /**
     * Haptic confirmation on the primary button, off when the user says so.
     *
     * <p>Lap has its own separate switch because the two are pressed in different
     * circumstances: a repeated tick on Start/Pause is noise, while a lap press is a
     * deliberate mark that benefits from confirmation.
     */
    private void buttonHaptic(View view) {
        if (AppPrefs.prefs(context).getBoolean("stopwatch_button_haptics", true)) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        }
    }
}
