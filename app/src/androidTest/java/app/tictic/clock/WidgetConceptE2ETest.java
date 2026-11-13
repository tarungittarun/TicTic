package app.tictic.clock;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Executes production RemoteViews on an Android emulator and retains real screen captures. */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class WidgetConceptE2ETest {
    private static final int FIRST_WIDGET_ID = 8100;
    private static final int ALARM_ID = 92001;
    private static final int SECOND_ALARM_ID = 92002;
    private static final int ROUTINE_ID = 93001;
    private static boolean screenshotDirectoryPrepared;

    private static final WidgetCase[] CASES = {
            new WidgetCase("alarm_countdown", R.layout.widget_alarm_countdown,
                    R.id.widget_concept_heading, "NEXT WAKE IN", 202),
            new WidgetCase("alarm_stack", R.layout.widget_alarm_stack,
                    R.id.widget_alarm_row_0, "Morning focus", 202),
            new WidgetCase("wake_switch", R.layout.widget_wake_switch,
                    R.id.widget_wake_status, "Morning focus", 190),
            new WidgetCase("quick_alarm", R.layout.widget_quick_alarm,
                    R.id.widget_quick_alarm_10, "10 MIN", 174),
            new WidgetCase("world_grid", R.layout.widget_world_grid,
                    R.id.widget_zone_london, "LONDON", 222),
            new WidgetCase("meeting_window", R.layout.widget_meeting_window,
                    R.id.widget_meeting_status, "work hours", 188),
            new WidgetCase("week_strip", R.layout.widget_week_strip,
                    R.id.widget_week_date_0, "", 176),
            new WidgetCase("month_grid", R.layout.widget_month_grid,
                    R.id.widget_month_title, "", 238),
            new WidgetCase("year_pulse", R.layout.widget_year_pulse,
                    R.id.widget_year_day, "DAY", 186),
            new WidgetCase("day_balance", R.layout.widget_day_balance,
                    R.id.widget_day_elapsed, "elapsed", 186),
            new WidgetCase("routine_stack", R.layout.widget_routine_stack,
                    R.id.widget_routine_row_0, "Deep work", 202),
            new WidgetCase("routine_countdown", R.layout.widget_routine_countdown,
                    R.id.widget_routine_name, "Deep work", 196),
            new WidgetCase("focus_launch", R.layout.widget_focus_launch,
                    R.id.widget_focus_25, "25 MIN", 190),
            new WidgetCase("timer_live", R.layout.widget_timer_live,
                    R.id.widget_timer_live_action, "START / RESUME", 212),
            new WidgetCase("lap_insights", R.layout.widget_lap_insights,
                    R.id.widget_lap_last, "Latest", 190)
    };

    private Instrumentation instrumentation;
    private Context app;
    private MainActivity activity;
    private File screenshotDirectory;

    @Before public void setUp() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        app = instrumentation.getTargetContext();
        screenshotDirectory = new File(app.getFilesDir(), "widget-e2e");
        if (!screenshotDirectory.exists()) {
            assertTrue("Could not create emulator screenshot directory", screenshotDirectory.mkdirs());
        }
        synchronized (WidgetConceptE2ETest.class) {
            if (!screenshotDirectoryPrepared) {
                File[] oldCaptures = screenshotDirectory.listFiles((dir, name) -> name.endsWith(".jpg"));
                if (oldCaptures != null) {
                    for (File oldCapture : oldCaptures) assertTrue("Could not clear stale emulator evidence", oldCapture.delete());
                }
                screenshotDirectoryPrepared = true;
            }
        }
        DataStore.prefs(app).edit().clear().apply();
        seedScenarioData();

        Intent launch = new Intent(app, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = (MainActivity) instrumentation.startActivitySync(launch);
        assertNotNull("MainActivity did not launch on the emulator", activity);
        instrumentation.waitForIdleSync();
        SystemClock.sleep(400L);
    }

    @After public void tearDown() {
        if (app != null) {
            AlarmScheduler.cancel(app, ALARM_ID);
            AlarmScheduler.cancel(app, SECOND_ALARM_ID);
            TimerReceiver.cancelAlarm(app);
            try {
                JSONArray disabledRoutine = new JSONArray().put(new JSONObject()
                        .put("id", ROUTINE_ID).put("enabled", false));
                DataStore.putArray(app, "routines", disabledRoutine);
                RoutineReceiver.scheduleAll(app);
            } catch (Exception e) {
                throw new AssertionError("Could not clean up the test routine alarm", e);
            }
            DataStore.prefs(app).edit().clear().apply();
        }
        if (activity != null) {
            instrumentation.runOnMainSync(() -> {
                if (!activity.isFinishing()) activity.finish();
            });
            instrumentation.waitForIdleSync();
        }
    }

    @Test public void bAllFifteenProductionWidgetsRenderAndCaptureOnEmulator() throws Exception {
        assertEquals("Expected the complete new-concept traversal", 15, CASES.length);
        for (int i = 0; i < CASES.length; i++) {
            WidgetCase testCase = CASES[i];
            Capture capture = display(testCase.layout, testCase.heightDp, 340);
            assertTrue(testCase.name + " RemoteViews root is not attached", capture.widget.isAttachedToWindow());
            assertTrue(testCase.name + " has no rendered dimensions", capture.widget.getWidth() > 0 && capture.widget.getHeight() > 0);
            View marker = capture.widget.findViewById(testCase.markerId);
            assertNotNull(testCase.name + " missing expected rendered view", marker);
            String actual = text(marker);
            if (testCase.expectedText.isEmpty()) {
                assertTrue(testCase.name + " rendered an empty marker", !actual.trim().isEmpty());
            } else {
                assertTrue(testCase.name + " expected '" + testCase.expectedText + "' in '" + actual + "'",
                        actual.toLowerCase(java.util.Locale.ROOT).contains(testCase.expectedText.toLowerCase(java.util.Locale.ROOT)));
            }
            if (testCase.layout == R.layout.widget_world_grid) {
                assertEquals("Europe/London", ((TextClock) marker).getTimeZone());
                assertEquals("America/New_York", ((TextClock) capture.widget.findViewById(R.id.widget_zone_new_york)).getTimeZone());
                assertEquals("Asia/Tokyo", ((TextClock) capture.widget.findViewById(R.id.widget_zone_tokyo)).getTimeZone());
            }
            captureScreenshot(String.format(java.util.Locale.ROOT, "%02d_%s", i + 1, testCase.name), capture);
        }
        assertTrue("One emulator screenshot per new widget concept",
                screenshotDirectory.listFiles((dir, name) -> name.matches("\\d{2}_.+\\.jpg")).length >= 15);
    }

    @Test public void cMonthGridUsesCompactWeekStripAtCompactWidgetHeight() throws Exception {
        Capture capture = display(R.layout.widget_month_grid, 176, 340);
        View fullGrid = capture.widget.findViewById(R.id.widget_month_grid);
        View compactWeek = capture.widget.findViewById(R.id.widget_month_compact);
        assertNotNull(fullGrid);
        assertNotNull(compactWeek);
        assertEquals(View.GONE, fullGrid.getVisibility());
        assertEquals(View.VISIBLE, compactWeek.getVisibility());
        for (int id : new int[]{R.id.widget_month_compact_day_0, R.id.widget_month_compact_day_1,
                R.id.widget_month_compact_day_2, R.id.widget_month_compact_day_3,
                R.id.widget_month_compact_day_4, R.id.widget_month_compact_day_5,
                R.id.widget_month_compact_day_6}) {
            assertTrue("Compact month strip has an empty day cell", !text(capture.widget.findViewById(id)).isEmpty());
        }
        captureScreenshot("16_month_grid_compact_week", capture);
    }

    @Test public void eWakeSwitchFocusLaunchAndTimerLiveCompleteTheirTapFlows() throws Exception {
        Capture wakeOn = display(R.layout.widget_wake_switch, 190, 340);
        View wakeButton = wakeOn.widget.findViewById(R.id.widget_wake_switch);
        assertTrue("Wake Switch action needs an accessibility label", text(wakeButton).contains("TAP TO PAUSE"));
        assertTrue("Wake Switch action is not clickable", wakeButton.isClickable());
        click(wakeButton);
        await(() -> !DataStore.array(app, "alarms").optJSONObject(0).optBoolean("enabled", true), "Wake Switch did not disable the selected alarm");
        Capture wakeOff = display(R.layout.widget_wake_switch, 190, 340);
        assertTrue(text(wakeOff.widget.findViewById(R.id.widget_wake_switch)).contains("TAP TO ENABLE"));
        captureScreenshot("17_wake_switch_disabled_after_tap", wakeOff);
        View offButton = wakeOff.widget.findViewById(R.id.widget_wake_switch);
        assertTrue(String.valueOf(offButton.getContentDescription()).startsWith("Enable Morning focus at"));
        click(offButton);
        await(() -> DataStore.array(app, "alarms").optJSONObject(0).optBoolean("enabled", false), "Wake Switch did not re-enable the alarm");

        Capture focus = display(R.layout.widget_focus_launch, 190, 340);
        View focus25 = focus.widget.findViewById(R.id.widget_focus_25);
        assertTrue("Focus preset must announce its duration", String.valueOf(focus25.getContentDescription()).contains("25 minute"));
        click(focus25);
        await(() -> TimerReceiver.isRunning(app), "Focus Launch did not start the timer");
        assertEquals(25L * 60_000L, DataStore.prefs(app).getLong("timer_duration", 0L));
        Capture running = display(R.layout.widget_timer_live, 212, 340);
        assertTrue(text(running.widget.findViewById(R.id.widget_timer_live_action)).contains("PAUSE TIMER"));
        captureScreenshot("18_timer_live_running_after_focus_launch", running);
        View timerAction = running.widget.findViewById(R.id.widget_timer_live_action);
        assertTrue("Timer Live action must be clickable", timerAction.isClickable());
        click(timerAction);
        await(() -> !TimerReceiver.isRunning(app), "Timer Live did not pause the running timer");
        Capture paused = display(R.layout.widget_timer_live, 212, 340);
        assertTrue(text(paused.widget.findViewById(R.id.widget_timer_live_action)).contains("START / RESUME"));
        captureScreenshot("19_timer_live_paused_after_tap", paused);
    }

    @Test public void dQuickAlarmTapEntersTheExistingSaveAndPermissionPreflight() throws Exception {
        int originalAlarms = DataStore.array(app, "alarms").length();
        Capture quick = display(R.layout.widget_quick_alarm, 174, 340);
        View tenMinutes = quick.widget.findViewById(R.id.widget_quick_alarm_10);
        assertTrue("Quick Alarm button must announce its action", String.valueOf(tenMinutes.getContentDescription()).contains("10 minutes"));
        assertTrue("Quick Alarm action is not clickable", tenMinutes.isClickable());
        click(tenMinutes);
        await(() -> pendingQuickAlarm() != null, "Quick Alarm tap did not enter MainActivity alarm preflight");
        JSONObject pending = pendingQuickAlarm();
        assertNotNull(pending);
        assertEquals("Quick · 10 min", pending.optString("label"));
        assertEquals("Quick Alarm must not silently bypass the existing save flow", originalAlarms,
                DataStore.array(app, "alarms").length());
        captureFullScreen("20_quick_alarm_save_preflight");
    }

    @Test public void aCatalogPinConfigureAndPlacementCompleteTheUserJourney() throws Exception {
        WidgetCatalogActivity catalog = (WidgetCatalogActivity) instrumentation.startActivitySync(
                new Intent(app, WidgetCatalogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        assertNotNull("Widget catalog did not open from the app", catalog);
        String[] conceptNames = {"Wake Countdown", "Alarm Stack", "Wake Switch", "Quick Alarm", "World Grid",
                "Meeting Window", "Week Strip", "Month Grid", "Year Pulse", "Day Balance", "Routine Stack",
                "Routine Countdown", "Focus Launch", "Timer Live", "Lap Insights"};
        for (String name : conceptNames) assertNotNull("Catalog is missing " + name, findText(catalog, name));
        assertNotNull("Catalog is missing the new-concepts section", findText(catalog, "NEW CONCEPTS · 15"));

        ScrollView scroll = findScrollView(catalog.getWindow().getDecorView());
        assertNotNull("Catalog must scroll through all 35 widget designs", scroll);
        instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN));
        instrumentation.waitForIdleSync();
        captureFullScreen("21_widget_catalog_new_concepts");

        TextView quickAlarmTitle = (TextView) findText(catalog, "Quick Alarm");
        assertNotNull(quickAlarmTitle);
        View card = (View) quickAlarmTitle.getParent().getParent();
        assertTrue("Quick Alarm catalog card must start the placement flow", card.isClickable());
        instrumentation.runOnMainSync(() -> scroll.smoothScrollTo(0, card.getTop()));
        instrumentation.waitForIdleSync();
        ActivityMonitor configMonitor = instrumentation.addMonitor(WidgetConfigActivity.class.getName(), null, false);
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        assertTrue("The API 34 emulator launcher must support widget pin requests",
                manager.isRequestPinAppWidgetSupported());
        ComponentName provider = new ComponentName(app, WidgetVariants.QuickAlarm.class);
        int previousCount = manager.getAppWidgetIds(provider).length;
        click(card);

        AccessibilityNodeInfo pinPrompt = waitForAccessibilityTexts(30000L, "Add");
        captureFullScreen("22_quick_alarm_pin_confirmation");
        assertTrue("Could not confirm the Android home-screen pin request",
                clickAccessibilityText(pinPrompt, "Add"));

        WidgetConfigActivity studio = (WidgetConfigActivity) instrumentation.waitForMonitorWithTimeout(configMonitor, 15000L);
        instrumentation.removeMonitor(configMonitor);
        if (studio == null) {
            int[] placedIds = manager.getAppWidgetIds(provider);
            await(() -> manager.getAppWidgetIds(provider).length > previousCount,
                    "Launcher did not allocate the Quick Alarm widget after pin confirmation");
            placedIds = manager.getAppWidgetIds(provider);
            assertTrue("No Quick Alarm widget ID was allocated", placedIds.length > previousCount);
            int widgetId = placedIds[placedIds.length - 1];
            Intent configure = new Intent(app, WidgetConfigActivity.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            studio = (WidgetConfigActivity) instrumentation.startActivitySync(configure);
        }
        assertNotNull("Android did not launch Widget Studio for the pinned concept", studio);
        final WidgetConfigActivity studioActivity = studio;
        int widgetId = studioActivity.getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        assertTrue("Widget Studio did not receive the allocated launcher widget ID", widgetId >= 0);
        assertNotNull("Widget Studio is showing the wrong provider", findText(studioActivity, "Quick Alarm"));

        CheckBox label = (CheckBox) findText(studioActivity, "Widget label");
        assertNotNull("Widget Studio must expose the concept label setting", label);
        assertTrue("Widget label should start enabled for Quick Alarm", label.isChecked());
        instrumentation.runOnMainSync(() -> label.setChecked(false));
        Field surfaceField = WidgetConfigActivity.class.getDeclaredField("surfaceSpinner");
        surfaceField.setAccessible(true);
        AtomicReference<Spinner> surface = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            try {
                surface.set((Spinner) surfaceField.get(studioActivity));
                surface.get().setSelection(6);
            } catch (Exception e) {
                throw new AssertionError("Could not configure the widget surface", e);
            }
        });
        instrumentation.waitForIdleSync();
        captureFullScreen("23_quick_alarm_widget_studio");
        View apply = findText(studioActivity, "Apply");
        assertNotNull("Widget Studio must provide an Apply action", apply);
        click(apply);

        await(() -> manager.getAppWidgetIds(provider).length > previousCount,
                "Quick Alarm was not placed on the Android launcher");
        String key = "widget_" + widgetId + "_";
        assertTrue("Applying Widget Studio must persist configured state",
                DataStore.prefs(app).getBoolean(key + "configured", false));
        assertEquals("Widget Studio surface selection was not saved", 6,
                DataStore.prefs(app).getInt(key + "panel_treatment", -1));
        assertFalse("Widget label choice was not saved", DataStore.prefs(app).getBoolean(key + "show_caption", true));
        assertNotNull("Launcher did not bind the requested Quick Alarm provider", manager.getAppWidgetInfo(widgetId));
        assertEquals("Launcher bound the wrong widget provider", R.layout.widget_quick_alarm,
                manager.getAppWidgetInfo(widgetId).initialLayout);
        instrumentation.waitForIdleSync();
        SystemClock.sleep(500L);
        captureFullScreen("24_quick_alarm_widget_placed");
        instrumentation.runOnMainSync(catalog::finish);
    }

    private Capture display(int layout, int heightDp, int widthDp) throws Exception {
        int widgetId = FIRST_WIDGET_ID + layout;
        String name = app.getResources().getResourceEntryName(layout);
        WidgetEngine.PreviewStyle style = previewStyle(name);
        RemoteViews remoteViews = WidgetEngine.previewViews(app, widgetId, layout, style, widthDp, heightDp);
        WidgetConcepts.render(app, remoteViews, widgetId, name, true);
        AtomicReference<Capture> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            FrameLayout page = new FrameLayout(activity);
            page.setBackgroundColor(0xffeaf0ed);
            LinearLayout column = new LinearLayout(activity);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setPadding(dp(18), dp(14), dp(18), dp(14));
            column.setBackgroundColor(0xffeaf0ed);
            column.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView eyebrow = new TextView(activity);
            eyebrow.setText("TICTIC  /  WIDGET E2E");
            eyebrow.setTextColor(0xff477168);
            eyebrow.setTextSize(10);
            eyebrow.setLetterSpacing(.14f);
            eyebrow.setTypeface(null, android.graphics.Typeface.BOLD);
            column.addView(eyebrow, new LinearLayout.LayoutParams(-1, -2));
            TextView title = new TextView(activity);
            title.setText(prettyName(name));
            title.setTextColor(0xff18312f);
            title.setTextSize(23);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
            titleParams.topMargin = dp(3);
            column.addView(title, titleParams);
            TextView device = new TextView(activity);
            device.setText("ANDROID EMULATOR  ·  API " + Build.VERSION.SDK_INT + "  ·  LIVE REMOTEVIEWS");
            device.setTextColor(0xff60726c);
            device.setTextSize(9);
            device.setLetterSpacing(.06f);
            LinearLayout.LayoutParams deviceParams = new LinearLayout.LayoutParams(-1, -2);
            deviceParams.topMargin = dp(3);
            deviceParams.bottomMargin = dp(12);
            column.addView(device, deviceParams);

            FrameLayout container = new FrameLayout(activity);
            container.setBackgroundColor(Color.TRANSPARENT);
            column.addView(container, new LinearLayout.LayoutParams(-1, dp(heightDp)));
            FrameLayout.LayoutParams columnParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
            page.addView(column, columnParams);
            activity.setContentView(page);
            View widget = remoteViews.apply(app, container);
            container.addView(widget, new FrameLayout.LayoutParams(-1, -1));
            result.set(new Capture(page, column, container, widget));
        });
        instrumentation.waitForIdleSync();
        SystemClock.sleep(180L);
        assertNotNull("RemoteViews host did not attach", result.get());
        assertTrue("Widget card has no width", result.get().widget.getWidth() > 0);
        assertTrue("Widget card has no height", result.get().widget.getHeight() > 0);
        return result.get();
    }

    private WidgetEngine.PreviewStyle previewStyle(String layout) {
        WidgetEngine.PreviewStyle style = new WidgetEngine.PreviewStyle();
        style.textColor = textColor(layout);
        style.background = background(layout);
        style.opacity = 255;
        style.font = 0;
        style.size = 56;
        style.dateFormat = 0;
        style.alarmDetail = 0;
        style.alignment = -1;
        style.panelTreatment = 0;
        style.layoutDensity = 1;
        style.layoutPriority = 0;
        style.glassFinish = 45;
        style.glassPreset = 0;
        style.transparent = false;
        style.showDate = false;
        style.showAlarm = false;
        style.showCaption = true;
        style.showContext = false;
        style.wallpaperColors = false;
        style.weatherEnabled = false;
        return style;
    }

    private int textColor(String layout) {
        switch (layout) {
            case "widget_wake_switch": case "widget_quick_alarm": case "widget_routine_countdown": return Color.WHITE;
            case "widget_month_grid": return 0xff574b39;
            case "widget_timer_live": return 0xff173a52;
            case "widget_lap_insights": return 0xff202b32;
            default: return 0xff183a34;
        }
    }

    private int background(String layout) {
        switch (layout) {
            case "widget_alarm_countdown": return 0xfff6e9c9;
            case "widget_alarm_stack": return 0xfff7ebcb;
            case "widget_wake_switch": return 0xff263b38;
            case "widget_quick_alarm": return 0xff101920;
            case "widget_world_grid": return 0xffdde7f7;
            case "widget_meeting_window": return 0xffddebf8;
            case "widget_week_strip": return 0xfff2eee2;
            case "widget_month_grid": return 0xfffff1c7;
            case "widget_year_pulse": return 0xffddf1e9;
            case "widget_day_balance": return 0xffd9ecec;
            case "widget_routine_stack": return 0xffe2efe2;
            case "widget_routine_countdown": return 0xff1d2b39;
            case "widget_focus_launch": return 0xffe7e2fa;
            case "widget_timer_live": return 0xffdcebf8;
            case "widget_lap_insights": return 0xffe6e9ed;
            default: return 0xffe8ece7;
        }
    }

    private void captureFullScreen(String name) throws Exception {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(180L);
        Bitmap full = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("Emulator did not provide a screen capture", full);
        Bitmap image = full;
        int maxWidth = 400;
        if (full.getWidth() > maxWidth) {
            int scaledHeight = Math.max(1, full.getHeight() * maxWidth / full.getWidth());
            image = Bitmap.createScaledBitmap(full, maxWidth, scaledHeight, true);
        }
        File output = new File(screenshotDirectory, name + ".jpg");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            assertTrue("Could not encode emulator screenshot: " + name,
                    image.compress(Bitmap.CompressFormat.JPEG, 42, stream));
        }
        if (image != full) image.recycle();
        full.recycle();
        assertTrue("Emulator screenshot was not written: " + name, output.length() > 1000L);
    }

    private void captureScreenshot(String name, Capture capture) throws Exception {
        instrumentation.waitForIdleSync();
        SystemClock.sleep(180L);
        Bitmap full = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull("Emulator did not provide a screen capture", full);
        int[] location = new int[2];
        capture.column.getLocationOnScreen(location);
        int left = Math.max(0, location[0]);
        int top = Math.max(0, location[1]);
        int right = Math.min(full.getWidth(), left + capture.column.getWidth());
        int bottom = Math.min(full.getHeight(), top + capture.column.getHeight());
        assertTrue("Widget capture bounds are empty", right > left && bottom > top);
        Bitmap crop = Bitmap.createBitmap(full, left, top, right - left, bottom - top);
        int maxWidth = 400;
        if (crop.getWidth() > maxWidth) {
            int scaledHeight = Math.max(1, crop.getHeight() * maxWidth / crop.getWidth());
            Bitmap scaled = Bitmap.createScaledBitmap(crop, maxWidth, scaledHeight, true);
            crop.recycle();
            crop = scaled;
        }
        File output = new File(screenshotDirectory, name + ".jpg");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            assertTrue("Could not encode emulator screenshot: " + name,
                    crop.compress(Bitmap.CompressFormat.JPEG, 42, stream));
        }
        full.recycle();
        crop.recycle();
        assertTrue("Emulator screenshot was not written: " + name, output.length() > 1000L);
    }

    private View findText(Activity target, String expected) {
        return target == null || target.getWindow() == null ? null
                : findText(target.getWindow().getDecorView(), expected);
    }

    private View findText(View root, String expected) {
        if (root instanceof TextView
                && expected.contentEquals(((TextView) root).getText())) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findText(group.getChildAt(i), expected);
                if (found != null) return found;
            }
        }
        return null;
    }

    private ScrollView findScrollView(View root) {
        if (root instanceof ScrollView) return (ScrollView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView found = findScrollView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo waitForAccessibilityTexts(long timeoutMs, String... expected) throws Exception {
        long end = SystemClock.uptimeMillis() + timeoutMs;
        AccessibilityNodeInfo lastRoot = null;
        boolean waitedForSystemUi = false;
        while (SystemClock.uptimeMillis() < end) {
            instrumentation.waitForIdleSync();
            lastRoot = instrumentation.getUiAutomation().getRootInActiveWindow();
            if (!waitedForSystemUi && accessibilityContains(lastRoot, "System UI isn't responding")) {
                waitedForSystemUi = clickAccessibilityText(lastRoot, "Wait");
                Thread.sleep(750L);
                continue;
            }
            boolean foundAll = lastRoot != null;
            if (foundAll) {
                for (String value : expected) {
                    if (!accessibilityContains(lastRoot, value)) {
                        foundAll = false;
                        break;
                    }
                }
            }
            if (foundAll) return lastRoot;
            Thread.sleep(150L);
        }
        throw new AssertionError("Timed out waiting for Android UI text "
                + java.util.Arrays.toString(expected) + "; active window was "
                + describeAccessibilityTree(lastRoot, new StringBuilder(), 0));
    }

    private boolean accessibilityContains(AccessibilityNodeInfo node, String expected) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if (text != null && text.toString().toLowerCase(java.util.Locale.ROOT)
                .contains(expected.toLowerCase(java.util.Locale.ROOT))) return true;
        if (description != null && description.toString().toLowerCase(java.util.Locale.ROOT)
                .contains(expected.toLowerCase(java.util.Locale.ROOT))) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (accessibilityContains(node.getChild(i), expected)) return true;
        }
        return false;
    }

    private boolean clickAccessibilityText(AccessibilityNodeInfo node, String expected) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        boolean matches = text != null && text.toString().toLowerCase(java.util.Locale.ROOT)
                .contains(expected.toLowerCase(java.util.Locale.ROOT));
        matches |= description != null && description.toString().toLowerCase(java.util.Locale.ROOT)
                .contains(expected.toLowerCase(java.util.Locale.ROOT));
        if (matches) {
            AccessibilityNodeInfo candidate = node;
            while (candidate != null) {
                if (candidate.isClickable()
                        && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                candidate = candidate.getParent();
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (clickAccessibilityText(node.getChild(i), expected)) return true;
        }
        return false;
    }

    private String describeAccessibilityTree(AccessibilityNodeInfo node, StringBuilder out, int depth) {
        if (node == null || depth > 8 || out.length() > 1600) return out.toString();
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if ((text != null && text.length() > 0) || (description != null && description.length() > 0)) {
            if (out.length() > 0) out.append(" | ");
            out.append(text == null ? "" : text);
            if (description != null && description.length() > 0) out.append(" [").append(description).append(']');
        }
        for (int i = 0; i < node.getChildCount(); i++) describeAccessibilityTree(node.getChild(i), out, depth + 1);
        return out.toString();
    }

    private void click(View view) throws Exception {
        AtomicBoolean clicked = new AtomicBoolean(false);
        instrumentation.runOnMainSync(() -> clicked.set(view.performClick()));
        assertTrue("RemoteViews click was not dispatched", clicked.get());
        instrumentation.waitForIdleSync();
        SystemClock.sleep(350L);
    }

    private void await(Check check, String failure) throws Exception {
        long end = SystemClock.uptimeMillis() + 5000L;
        while (SystemClock.uptimeMillis() < end) {
            instrumentation.waitForIdleSync();
            if (check.ok()) return;
            Thread.sleep(100L);
        }
        assertTrue(failure, check.ok());
    }

    private JSONObject pendingQuickAlarm() {
        AtomicReference<JSONObject> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            try {
                Field field = MainActivity.class.getDeclaredField("pendingAlarmSave");
                field.setAccessible(true);
                result.set((JSONObject) field.get(activity));
            } catch (Exception e) {
                failure.set(e);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Could not inspect normal Quick Alarm save flow", failure.get());
        }
        return result.get();
    }

    private void seedScenarioData() throws Exception {
        Calendar now = Calendar.getInstance();
        Calendar firstAlarm = (Calendar) now.clone();
        firstAlarm.add(Calendar.MINUTE, 72);
        Calendar secondAlarm = (Calendar) now.clone();
        secondAlarm.add(Calendar.MINUTE, 128);
        JSONArray alarms = new JSONArray();
        alarms.put(new JSONObject().put("id", ALARM_ID).put("hour", firstAlarm.get(Calendar.HOUR_OF_DAY))
                .put("minute", firstAlarm.get(Calendar.MINUTE)).put("days", 127).put("enabled", true)
                .put("label", "Morning focus").put("reminder_enabled", false).put("vibrate", false));
        alarms.put(new JSONObject().put("id", SECOND_ALARM_ID).put("hour", secondAlarm.get(Calendar.HOUR_OF_DAY))
                .put("minute", secondAlarm.get(Calendar.MINUTE)).put("days", 127).put("enabled", true)
                .put("label", "Stretch break").put("reminder_enabled", false).put("vibrate", false));
        DataStore.putArray(app, "alarms", alarms);

        Calendar nextRoutine = (Calendar) now.clone();
        nextRoutine.add(Calendar.MINUTE, 42);
        JSONArray routines = new JSONArray();
        routines.put(new JSONObject().put("id", ROUTINE_ID).put("name", "Deep work")
                .put("hour", nextRoutine.get(Calendar.HOUR_OF_DAY)).put("minute", nextRoutine.get(Calendar.MINUTE))
                .put("days", 127).put("enabled", true).put("duration", 45));
        DataStore.putArray(app, "routines", routines);

        JSONArray laps = new JSONArray();
        laps.put(new JSONObject().put("n", 1).put("ms", 83_420L).put("split", 83_420L));
        laps.put(new JSONObject().put("n", 2).put("ms", 158_920L).put("split", 75_500L));
        laps.put(new JSONObject().put("n", 3).put("ms", 247_820L).put("split", 88_900L));
        DataStore.putArray(app, "laps", laps);
        DataStore.prefs(app).edit().putLong("timer_duration", 25L * 60_000L)
                .putLong("timer_remaining", 25L * 60_000L).putLong("timer_end", 0L)
                .putBoolean("adaptive_power_saver", false).apply();
    }

    private String text(View view) {
        if (view instanceof TextView) return String.valueOf(((TextView) view).getText());
        return "";
    }

    private String prettyName(String layout) {
        String name = layout.replace("widget_", "").replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private int dp(int value) {
        return Math.round(value * app.getResources().getDisplayMetrics().density);
    }

    private interface Check { boolean ok(); }

    private static final class WidgetCase {
        final String name;
        final int layout;
        final int markerId;
        final String expectedText;
        final int heightDp;
        WidgetCase(String name, int layout, int markerId, String expectedText, int heightDp) {
            this.name = name;
            this.layout = layout;
            this.markerId = markerId;
            this.expectedText = expectedText;
            this.heightDp = heightDp;
        }
    }

    private static final class Capture {
        final View page;
        final View column;
        final View container;
        final View widget;
        Capture(View page, View column, View container, View widget) {
            this.page = page;
            this.column = column;
            this.container = container;
            this.widget = widget;
        }
    }
}
