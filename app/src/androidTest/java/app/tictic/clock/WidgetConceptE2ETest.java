package app.tictic.clock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.app.UiAutomation;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    private static final int MAX_SCREENSHOT_BYTES = 2800;
    private static final int MAX_SCREENSHOT_WIDTH = 320;
    private static final int CALENDAR_TEST_INK = 0xff6855a5;
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
    private WidgetCatalogActivity catalogActivity;
    private File screenshotDirectory;

    @Before public void setUp() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        UiAutomation automation = instrumentation.getUiAutomation();
        AccessibilityServiceInfo accessibility = automation.getServiceInfo();
        if (accessibility != null
                && (accessibility.flags & AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS) == 0) {
            accessibility.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            automation.setServiceInfo(accessibility);
        }
        app = instrumentation.getTargetContext();
        screenshotDirectory = new File(app.getFilesDir(), "widget-e2e");
        if (!screenshotDirectory.exists()) {
            assertTrue("Could not create emulator screenshot directory", screenshotDirectory.mkdirs());
        }
        DataStore.prefs(app).edit().clear().apply();
        seedScenarioData();
        // Launch screens lazily. MainActivity's live ticker can keep the UI queue non-idle,
        // so the E2E helper waits for lifecycle resume rather than a global idle barrier.
        // The catalog journey does not need MainActivity at all.
        activity = null;
        catalogActivity = null;
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
        if (activity != null || catalogActivity != null) {
            instrumentation.runOnMainSync(() -> {
                if (catalogActivity != null && !catalogActivity.isFinishing()) catalogActivity.finish();
                if (activity != null && !activity.isFinishing()) activity.finish();
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
            } else if (testCase.layout == R.layout.widget_week_strip) {
                assertWeekStripCustomInk(capture);
            } else if (testCase.layout == R.layout.widget_month_grid) {
                assertMonthGridCustomInk(capture);
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
        assertCompactMonthInk(capture);
        captureScreenshot("16_month_grid_compact_week", capture);
    }

    @Test public void dWakeSwitchFocusLaunchAndTimerLiveCompleteTheirTapFlows() throws Exception {
        Capture wakeOn = display(R.layout.widget_wake_switch, 190, 340);
        View wakeButton = wakeOn.widget.findViewById(R.id.widget_wake_switch);
        assertTrue("Wake Switch action needs an accessibility label", text(wakeButton).contains("TAP TO PAUSE"));
        assertTrue("Wake Switch action is not clickable", wakeButton.isClickable());
        click(wakeButton);
        await(() -> alarmEnabled(ALARM_ID, false), "Wake Switch did not disable the selected alarm");
        Capture wakeOff = display(R.layout.widget_wake_switch, 190, 340);
        assertTrue(text(wakeOff.widget.findViewById(R.id.widget_wake_switch)).contains("TAP TO ENABLE"));
        captureScreenshot("17_wake_switch_disabled_after_tap", wakeOff);
        View offButton = wakeOff.widget.findViewById(R.id.widget_wake_switch);
        assertTrue(String.valueOf(offButton.getContentDescription()).startsWith("Enable Morning focus at"));
        assertTrue("Wake Switch lost its action after changing the alarm state", offButton.isClickable());
        click(offButton);
        await(() -> alarmEnabled(ALARM_ID, true), "Wake Switch did not re-enable the selected alarm");

        Capture focus = display(R.layout.widget_focus_launch, 190, 340);
        View focus25 = focus.widget.findViewById(R.id.widget_focus_25);
        assertTrue("Focus preset must announce its duration", String.valueOf(focus25.getContentDescription()).contains("25 minute"));
        click(focus25);
        await(() -> TimerReceiver.isRunning(app), "Focus Launch did not start the timer", 20000L);
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

    @Test public void eQuickAlarmTapEntersTheExistingSaveAndPermissionPreflight() throws Exception {
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
        AccessibilityNodeInfo preflight = instrumentation.getUiAutomation().getRootInActiveWindow();
        if (pendingQuickAlarm() != null && accessibilityContains(preflight, "Cancel")) {
            assertTrue("Could not dismiss the Quick Alarm preflight for test cleanup",
                    clickAccessibilityText(preflight, "Cancel"));
            await(() -> pendingQuickAlarm() == null, "Quick Alarm preflight did not cancel cleanly");
        }
    }

    @Test public void zAllTwentyFourEmulatorCapturesArePresentAndDecodable() {
        String[] expected = {
                "01_alarm_countdown", "02_alarm_stack", "03_wake_switch", "04_quick_alarm",
                "05_world_grid", "06_meeting_window", "07_week_strip", "08_month_grid",
                "09_year_pulse", "10_day_balance", "11_routine_stack", "12_routine_countdown",
                "13_focus_launch", "14_timer_live", "15_lap_insights", "16_month_grid_compact_week",
                "17_wake_switch_disabled_after_tap", "18_timer_live_running_after_focus_launch",
                "19_timer_live_paused_after_tap", "20_quick_alarm_save_preflight",
                "21_widget_catalog_new_concepts", "22_quick_alarm_pin_confirmation",
                "23_quick_alarm_widget_studio", "24_quick_alarm_widget_placed"
        };
        StringBuilder missing = new StringBuilder();
        for (String name : expected) {
            File image = new File(screenshotDirectory, name + ".jpg");
            if (!image.isFile()) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(name);
                continue;
            }
            assertTrue("Emulator capture is unexpectedly small: " + name, image.length() > 1000L);
            Bitmap decoded = BitmapFactory.decodeFile(image.getAbsolutePath());
            assertNotNull("Emulator capture is not a decodable JPEG: " + name, decoded);
            decoded.recycle();
        }
        File[] actual = screenshotDirectory.listFiles((dir, filename) -> filename.endsWith(".jpg"));
        assertNotNull("Emulator screenshot directory could not be listed", actual);
        assertTrue("Missing emulator captures: " + missing, missing.length() == 0);
        assertEquals("Expected exactly 24 uniquely named emulator screenshots", 24, actual.length);
    }

    @Test public void aCatalogPinConfigureAndPlacementCompleteTheUserJourney() throws Exception {
        // The catalog is internal, but its UI queue need not go idle before inspection.
        // Wait for its lifecycle resume rather than startActivitySync's global idle barrier.
        WidgetCatalogActivity catalog = launchActivityWithoutWaitingForIdle(
                WidgetCatalogActivity.class,
                new Intent(app, WidgetCatalogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        catalogActivity = catalog;
        assertNotNull("Widget catalog did not open from the app", catalog);
        String[] conceptNames = {"Wake Countdown", "Alarm Stack", "Wake Switch", "Quick Alarm", "World Grid",
                "Meeting Window", "Week Strip", "Month Grid", "Year Pulse", "Day Balance", "Routine Stack",
                "Routine Countdown", "Focus Launch", "Timer Live", "Lap Insights"};
        for (String name : conceptNames) assertNotNull("Catalog is missing " + name, findText(catalog, name));
        assertNotNull("Catalog is missing the new-concepts section", findText(catalog, "NEW CONCEPTS · 15"));

        ScrollView scroll = findScrollView(catalog.getWindow().getDecorView());
        assertNotNull("Catalog must scroll through all 35 widget designs", scroll);
        TextView newConceptsTitle = (TextView) findText(catalog, "NEW CONCEPTS · 15");
        assertNotNull("Catalog is missing the new-concepts section heading", newConceptsTitle);
        instrumentation.runOnMainSync(() ->
                scroll.scrollTo(0, Math.max(0, newConceptsTitle.getTop() - dp(16))));
        await(() -> isViewFullyVisible(newConceptsTitle),
                "New-concepts section heading did not become fully visible after scrolling");
        captureFullScreen("21_widget_catalog_new_concepts");

        TextView quickAlarmTitle = (TextView) findText(catalog, "Quick Alarm");
        assertNotNull(quickAlarmTitle);
        View card = (View) quickAlarmTitle.getParent().getParent();
        assertTrue("Quick Alarm catalog card must start the placement flow", card.isClickable());
        instrumentation.runOnMainSync(() ->
                scroll.scrollTo(0, Math.max(0, card.getTop() - dp(16))));
        await(() -> isTapTargetFullyVisible(card),
                "Quick Alarm catalog card did not become fully visible after scrolling");
        ActivityMonitor configMonitor = instrumentation.addMonitor(WidgetConfigActivity.class.getName(), null, false);
        AppWidgetManager manager = AppWidgetManager.getInstance(app);
        assertTrue("The API 34 emulator launcher must support widget pin requests",
                manager.isRequestPinAppWidgetSupported());
        ComponentName provider = new ComponentName(app, WidgetVariants.QuickAlarm.class);
        int previousCount = manager.getAppWidgetIds(provider).length;
        click(card);

        waitForAccessibilityTexts(30000L, "Add");
        captureFullScreen("22_quick_alarm_pin_confirmation");
        AccessibilityNodeInfo pinPrompt = waitForAccessibilityTexts(10000L, "Add");
        assertTrue("Could not confirm the Android home-screen pin request; active window was "
                        + describeAccessibilityTree(pinPrompt, new StringBuilder(), 0),
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
            // The launcher starts configuration outside the test process, which the first
            // ActivityMonitor can miss. Resume callbacks observe our explicit fallback without
            // requiring Instrumentation's global UI-idle barrier.
            studio = launchActivityWithoutWaitingForIdle(WidgetConfigActivity.class, configure);
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
        assertTrue("Could not return to the Android home screen after placement",
                instrumentation.getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME));
        AccessibilityNodeInfo placedWidget = waitForAccessibilityTexts(15000L, "10 MIN");
        placedWidget.recycle();
        SystemClock.sleep(500L);
        captureFullScreen("24_quick_alarm_widget_placed");
        instrumentation.runOnMainSync(catalog::finish);
        catalogActivity = null;
    }

    private <T extends Activity> T launchActivityWithoutWaitingForIdle(
            Class<T> activityClass, Intent intent) throws Exception {
        Application application = (Application) app.getApplicationContext();
        CountDownLatch resumed = new CountDownLatch(1);
        AtomicReference<T> launched = new AtomicReference<>();
        AtomicReference<RuntimeException> launchFailure = new AtomicReference<>();
        Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity candidate, Bundle state) { }
            @Override public void onActivityStarted(Activity candidate) { }
            @Override public void onActivityResumed(Activity candidate) {
                if (activityClass.isInstance(candidate)) {
                    launched.compareAndSet(null, activityClass.cast(candidate));
                    resumed.countDown();
                }
            }
            @Override public void onActivityPaused(Activity candidate) { }
            @Override public void onActivityStopped(Activity candidate) { }
            @Override public void onActivitySaveInstanceState(Activity candidate, Bundle state) { }
            @Override public void onActivityDestroyed(Activity candidate) { }
        };
        application.registerActivityLifecycleCallbacks(callbacks);
        Thread starter = new Thread(() -> {
            try {
                app.startActivity(intent);
            } catch (RuntimeException error) {
                launchFailure.set(error);
                resumed.countDown();
            }
        }, "widget-e2e-launch-" + activityClass.getSimpleName());
        starter.setDaemon(true);
        try {
            starter.start();
            if (!resumed.await(45000L, TimeUnit.MILLISECONDS)) {
                starter.interrupt();
                AccessibilityNodeInfo active = instrumentation.getUiAutomation().getRootInActiveWindow();
                String window = describeAccessibilityTree(active, new StringBuilder(), 0);
                if (active != null) active.recycle();
                throw new AssertionError("Android did not resume " + activityClass.getSimpleName()
                        + " within 45 seconds; active window was " + window
                        + "; startActivity error=" + launchFailure.get());
            }
            T result = launched.get();
            if (result != null) return result;
            throw new AssertionError("Android could not start " + activityClass.getSimpleName()
                    + "; startActivity error=" + launchFailure.get());
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks);
        }
    }

    private MainActivity ensureMainActivity() throws Exception {
        if (activity != null && !activity.isFinishing()) return activity;
        Intent launch = new Intent(app, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = launchActivityWithoutWaitingForIdle(MainActivity.class, launch);
        assertNotNull("MainActivity did not launch on the emulator", activity);
        SystemClock.sleep(400L);
        return activity;
    }

    private Capture display(int layout, int heightDp, int widthDp) throws Exception {
        ensureMainActivity();
        int widgetId = FIRST_WIDGET_ID + layout;
        String name = app.getResources().getResourceEntryName(layout);
        WidgetEngine.PreviewStyle style = previewStyle(name);
        RemoteViews remoteViews = WidgetEngine.previewViews(app, widgetId, layout, style, widthDp, heightDp);
        WidgetConcepts.render(app, remoteViews, widgetId, name, true);
        // previewViews applies style before interactive PendingIntents are installed. Reapply
        // after the interactive render so calendar refreshes cannot reset configured ink.
        WidgetConcepts.applyStyle(app, remoteViews, widgetId, name, style, widthDp, heightDp);
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

    private void assertWeekStripCustomInk(Capture capture) {
        int highlighted = 0;
        for (int i = 0; i < 7; i++) {
            TextView weekday = (TextView) capture.widget.findViewById(calendarCellId("widget_week_day_", i));
            TextView date = (TextView) capture.widget.findViewById(calendarCellId("widget_week_date_", i));
            assertNotNull("Week Strip weekday cell is missing", weekday);
            assertNotNull("Week Strip date cell is missing", date);
            int dayInk = weekday.getCurrentTextColor();
            int dateInk = date.getCurrentTextColor();
            assertTrue("Week Strip weekday lost configured ink in slot " + i,
                    dayInk == Color.WHITE || dayInk == CALENDAR_TEST_INK);
            assertEquals("Week Strip date/weekday highlight differs in slot " + i, dayInk, dateInk);
            if (dayInk == Color.WHITE) highlighted++;
        }
        assertEquals("Week Strip must highlight today exactly once", 1, highlighted);
    }

    private void assertMonthGridCustomInk(Capture capture) {
        int highlighted = 0;
        int currentMonth = 0;
        int adjacentMonth = 0;
        int muted = mutedCalendarInk();
        for (int i = 0; i < 42; i++) {
            TextView date = (TextView) capture.widget.findViewById(calendarCellId("widget_month_day_", i));
            assertNotNull("Month Grid date cell is missing at slot " + i, date);
            int ink = date.getCurrentTextColor();
            if (ink == Color.WHITE) highlighted++;
            else if (ink == CALENDAR_TEST_INK) currentMonth++;
            else if (ink == muted) adjacentMonth++;
            else assertTrue("Month Grid has unexpected ink at slot " + i + ": " + ink, false);
        }
        assertEquals("Month Grid must highlight today exactly once", 1, highlighted);
        assertTrue("Month Grid must preserve custom ink for current-month dates", currentMonth > 0);
        assertTrue("Month Grid must mute adjacent-month dates", adjacentMonth > 0);
    }

    private void assertCompactMonthInk(Capture capture) {
        int highlighted = 0;
        for (int i = 0; i < 7; i++) {
            TextView date = (TextView) capture.widget.findViewById(calendarCellId("widget_month_compact_day_", i));
            assertNotNull("Compact Month Grid date cell is missing", date);
            int ink = date.getCurrentTextColor();
            assertTrue("Compact Month Grid lost configured ink in slot " + i,
                    ink == Color.WHITE || ink == CALENDAR_TEST_INK);
            if (ink == Color.WHITE) highlighted++;
        }
        assertEquals("Compact Month Grid must highlight today exactly once", 1, highlighted);
    }

    private int mutedCalendarInk() {
        return Color.argb(Math.min(Color.alpha(CALENDAR_TEST_INK), 0x99),
                Color.red(CALENDAR_TEST_INK), Color.green(CALENDAR_TEST_INK), Color.blue(CALENDAR_TEST_INK));
    }

    private int calendarCellId(String prefix, int index) {
        int id = app.getResources().getIdentifier(prefix + index, "id", app.getPackageName());
        assertTrue("Missing calendar view ID " + prefix + index, id != 0);
        return id;
    }

    private WidgetEngine.PreviewStyle previewStyle(String layout) {
        WidgetEngine.PreviewStyle style = new WidgetEngine.PreviewStyle();
        style.textColor = "widget_week_strip".equals(layout) || "widget_month_grid".equals(layout)
                ? CALENDAR_TEST_INK : textColor(layout);
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

    private AccessibilityNodeInfo findSystemPrompt(String... markers) {
        UiAutomation ui = instrumentation.getUiAutomation();
        AccessibilityNodeInfo active = ui.getRootInActiveWindow();
        if (containsAnyAccessibilityText(active, markers)) return active;
        if (active != null) active.recycle();
        for (AccessibilityWindowInfo window : ui.getWindows()) {
            AccessibilityNodeInfo root = window.getRoot();
            window.recycle();
            if (containsAnyAccessibilityText(root, markers)) return root;
            if (root != null) root.recycle();
        }
        return null;
    }

    private boolean containsAnyAccessibilityText(AccessibilityNodeInfo node, String... markers) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        for (String marker : markers) {
            String expected = marker.toLowerCase(java.util.Locale.ROOT);
            if (text != null && text.toString().toLowerCase(java.util.Locale.ROOT).contains(expected)) return true;
            if (description != null && description.toString().toLowerCase(java.util.Locale.ROOT).contains(expected)) return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                if (containsAnyAccessibilityText(child, markers)) return true;
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private boolean promptNamesTargetApp(String details) {
        String label = String.valueOf(app.getApplicationInfo().loadLabel(app.getPackageManager()))
                .toLowerCase(java.util.Locale.ROOT);
        String lower = details.toLowerCase(java.util.Locale.ROOT);
        return (!label.isEmpty() && lower.contains(label))
                || lower.contains(app.getPackageName().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean respondToSystemPrompt() throws Exception {
        AccessibilityNodeInfo prompt = findSystemPrompt(
                "isn't responding", "keeps stopping", "keeps crashing");
        if (prompt == null) return false;
        String details = describeAccessibilityTree(prompt, new StringBuilder(), 0);
        if (accessibilityContains(prompt, "isn't responding")) {
            boolean waited;
            try {
                waited = clickAccessibilityText(prompt, "Wait");
            } finally {
                prompt.recycle();
            }
            assertTrue("Android displayed an ANR prompt without a usable Wait action: " + details, waited);
            SystemClock.sleep(1200L);
            return true;
        }

        if (promptNamesTargetApp(details)) {
            prompt.recycle();
            throw new AssertionError("Android displayed the target app crash dialog: " + details);
        }
        boolean closed;
        try {
            closed = clickAccessibilityText(prompt, "Close app")
                    || clickAccessibilityText(prompt, "OK");
        } finally {
            prompt.recycle();
        }
        assertTrue("Android displayed an unrelated-app crash prompt without a usable close action: "
                + details, closed);
        System.out.println("E2E dismissed unrelated-app crash dialog: " + details);
        SystemClock.sleep(600L);
        return true;
    }

    private void waitForResponsiveAndroidUi() throws Exception {
        int recoveries = 0;
        while (recoveries < 4 && respondToSystemPrompt()) recoveries++;
        if (recoveries < 4) return;
        AccessibilityNodeInfo prompt = findSystemPrompt(
                "isn't responding", "keeps stopping", "keeps crashing");
        if (prompt == null) return;
        String details = describeAccessibilityTree(prompt, new StringBuilder(), 0);
        boolean anr = accessibilityContains(prompt, "isn't responding");
        prompt.recycle();
        if (anr) {
            throw new AssertionError("Android remained unresponsive after four Wait actions: " + details);
        }
        throw new AssertionError("Android kept showing an app crash dialog after four recoveries: " + details);
    }

    private Bitmap takeEmulatorScreenshot() throws Exception {
        UiAutomation ui = instrumentation.getUiAutomation();
        for (int attempt = 0; attempt < 4; attempt++) {
            waitForResponsiveAndroidUi();
            Bitmap screenshot = ui.takeScreenshot();
            if (screenshot == null) {
                SystemClock.sleep(300L);
                continue;
            }
            if (!respondToSystemPrompt()) return screenshot;
            screenshot.recycle();
        }
        waitForResponsiveAndroidUi();
        ParcelFileDescriptor output = ui.executeShellCommand("screencap -p");
        assertNotNull("Android shell did not open an emulator screen capture", output);
        Bitmap screenshot;
        try (ParcelFileDescriptor.AutoCloseInputStream stream =
                     new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            screenshot = BitmapFactory.decodeStream(stream);
        }
        assertNotNull("UiAutomation and screencap both failed to provide an emulator image", screenshot);
        AccessibilityNodeInfo remainingPrompt = findSystemPrompt(
                "isn't responding", "keeps stopping", "keeps crashing");
        if (remainingPrompt != null) {
            String details = describeAccessibilityTree(remainingPrompt, new StringBuilder(), 0);
            boolean anr = accessibilityContains(remainingPrompt, "isn't responding");
            remainingPrompt.recycle();
            screenshot.recycle();
            throw new AssertionError((anr ? "System ANR" : "System crash")
                    + " overlay remained in the emulator capture: " + details);
        }
        return screenshot;
    }

    private void verifyScreenshotFile(File output, String name) {
        assertTrue("Emulator screenshot was not written: " + name, output.length() > 1000L);
        assertTrue("Emulator screenshot exceeds the intact GitHub annotation budget: " + name,
                output.length() <= MAX_SCREENSHOT_BYTES);
        Bitmap decoded = BitmapFactory.decodeFile(output.getAbsolutePath());
        assertNotNull("Emulator screenshot is not a decodable JPEG: " + name, decoded);
        assertTrue("Emulator screenshot has invalid dimensions: " + name,
                decoded.getWidth() > 0 && decoded.getHeight() > 0);
        decoded.recycle();
    }

    private void saveEmulatorScreenshot(Bitmap source, String name) throws Exception {
        Bitmap image = source;
        boolean ownsImage = false;
        if (source.getWidth() > MAX_SCREENSHOT_WIDTH) {
            int height = Math.max(1, source.getHeight() * MAX_SCREENSHOT_WIDTH / source.getWidth());
            image = Bitmap.createScaledBitmap(source, MAX_SCREENSHOT_WIDTH, height, true);
            ownsImage = true;
        }
        try {
            int quality = 40;
            while (true) {
                ByteArrayOutputStream encoded = new ByteArrayOutputStream();
                assertTrue("Could not encode emulator screenshot: " + name,
                        image.compress(Bitmap.CompressFormat.JPEG, quality, encoded));
                byte[] bytes = encoded.toByteArray();
                if (bytes.length <= MAX_SCREENSHOT_BYTES) {
                    File output = new File(screenshotDirectory, name + ".jpg");
                    try (FileOutputStream stream = new FileOutputStream(output)) {
                        stream.write(bytes);
                    }
                    verifyScreenshotFile(output, name);
                    return;
                }
                if (quality > 22) {
                    quality -= 6;
                    continue;
                }
                if (image.getWidth() <= 160) {
                    throw new AssertionError("Emulator screenshot cannot fit intact in a GitHub annotation: "
                            + name + " (" + bytes.length + " bytes)");
                }
                int width = Math.max(160, image.getWidth() * 4 / 5);
                int height = Math.max(1, image.getHeight() * width / image.getWidth());
                Bitmap smaller = Bitmap.createScaledBitmap(image, width, height, true);
                if (ownsImage) image.recycle();
                image = smaller;
                ownsImage = true;
                quality = 40;
            }
        } finally {
            if (ownsImage) image.recycle();
        }
    }

    private void captureFullScreen(String name) throws Exception {
        SystemClock.sleep(180L);
        Bitmap full = takeEmulatorScreenshot();
        try {
            saveEmulatorScreenshot(full, name);
        } finally {
            full.recycle();
        }
    }

    private void captureScreenshot(String name, Capture capture) throws Exception {
        SystemClock.sleep(180L);
        Bitmap full = takeEmulatorScreenshot();
        Bitmap crop = null;
        try {
            int[] location = new int[2];
            capture.column.getLocationOnScreen(location);
            int left = Math.max(0, location[0]);
            int top = Math.max(0, location[1]);
            int right = Math.min(full.getWidth(), left + capture.column.getWidth());
            int bottom = Math.min(full.getHeight(), top + capture.column.getHeight());
            assertTrue("Widget capture bounds are empty", right > left && bottom > top);
            crop = Bitmap.createBitmap(full, left, top, right - left, bottom - top);
            saveEmulatorScreenshot(crop, name);
        } finally {
            if (crop != null) crop.recycle();
            full.recycle();
        }
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
        int promptRecoveries = 0;
        while (SystemClock.uptimeMillis() < end) {
            if (respondToSystemPrompt()) {
                if (++promptRecoveries > 4) {
                    throw new AssertionError("Android repeatedly interrupted the UI wait with system prompts");
                }
                continue;
            }
            AccessibilityNodeInfo root = findAccessibilityRootContaining(expected);
            if (root != null) return root;
            Thread.sleep(150L);
        }
        AccessibilityNodeInfo active = instrumentation.getUiAutomation().getRootInActiveWindow();
        String details = describeAccessibilityTree(active, new StringBuilder(), 0);
        if (active != null) active.recycle();
        throw new AssertionError("Timed out waiting for Android UI text "
                + java.util.Arrays.toString(expected) + "; active window was " + details);
    }

    private AccessibilityNodeInfo findAccessibilityRootContaining(String... expected) {
        UiAutomation ui = instrumentation.getUiAutomation();
        AccessibilityNodeInfo active = ui.getRootInActiveWindow();
        if (containsAccessibilityTexts(active, expected)) return active;
        if (active != null) active.recycle();
        for (AccessibilityWindowInfo window : ui.getWindows()) {
            AccessibilityNodeInfo root = window.getRoot();
            window.recycle();
            if (containsAccessibilityTexts(root, expected)) return root;
            if (root != null) root.recycle();
        }
        return null;
    }

    private boolean containsAccessibilityTexts(AccessibilityNodeInfo node, String... expected) {
        if (node == null) return false;
        for (String value : expected) {
            if (!accessibilityContains(node, value)) return false;
        }
        return true;
    }

    private boolean accessibilityContains(AccessibilityNodeInfo node, String expected) {
        return containsAnyAccessibilityText(node, expected);
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
            boolean ownsCandidate = false;
            while (candidate != null) {
                if (candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    if (ownsCandidate) candidate.recycle();
                    return true;
                }
                AccessibilityNodeInfo parent = candidate.getParent();
                if (ownsCandidate) candidate.recycle();
                candidate = parent;
                ownsCandidate = parent != null;
            }
            if (injectTap(node)) return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            boolean clicked;
            try {
                clicked = clickAccessibilityText(child, expected);
            } finally {
                child.recycle();
            }
            if (clicked) return true;
        }
        return false;
    }

    private boolean injectTap(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return !bounds.isEmpty() && injectTap(bounds.exactCenterX(), bounds.exactCenterY());
    }

    private boolean injectTap(float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean accepted;
        try {
            accepted = instrumentation.getUiAutomation().injectInputEvent(down, true);
        } finally {
            down.recycle();
        }
        if (!accepted) return false;
        SystemClock.sleep(60L);
        MotionEvent up = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            return instrumentation.getUiAutomation().injectInputEvent(up, true);
        } finally {
            up.recycle();
        }
    }

    private String describeAccessibilityTree(AccessibilityNodeInfo node, StringBuilder out, int depth) {
        if (node == null || depth > 12 || out.length() > 2400) return out.toString();
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if ((text != null && text.length() > 0) || (description != null && description.length() > 0)) {
            if (out.length() > 0) out.append(" | ");
            out.append(text == null ? "" : text);
            if (description != null && description.length() > 0) out.append(" [").append(description).append(']');
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            out.append(" <").append(node.getClassName()).append(">");
            out.append(" pkg=").append(node.getPackageName());
            out.append(" clickable=").append(node.isClickable());
            out.append(" bounds=").append(bounds);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                describeAccessibilityTree(child, out, depth + 1);
            } finally {
                child.recycle();
            }
        }
        return out.toString();
    }

    private void click(View view) throws Exception {
        waitForResponsiveAndroidUi();
        AtomicBoolean ready = new AtomicBoolean(false);
        AtomicReference<Rect> bounds = new AtomicReference<>();
        AtomicReference<String> targetState = new AtomicReference<>("uninspected");
        instrumentation.runOnMainSync(() -> {
            Rect visible = new Rect();
            boolean hasVisibleBounds = view.getGlobalVisibleRect(visible);
            targetState.set("class=" + view.getClass().getName()
                    + ", attached=" + view.isAttachedToWindow()
                    + ", shown=" + view.isShown()
                    + ", enabled=" + view.isEnabled()
                    + ", clickable=" + view.isClickable()
                    + ", size=" + view.getWidth() + "x" + view.getHeight()
                    + ", visible=" + visible);
            bounds.set(visible);
            ready.set(view.isAttachedToWindow() && view.isShown() && view.isEnabled()
                    && view.isClickable() && view.getWidth() > 0 && view.getHeight() > 0
                    && hasVisibleBounds && visible.width() == view.getWidth()
                    && visible.height() == view.getHeight());
        });
        assertTrue("Android tap target is detached, hidden, clipped, or not clickable: "
                + targetState.get(), ready.get());
        Rect target = bounds.get();
        assertTrue("Android tap did not reach the widget or app control at " + target,
                injectTap(target.exactCenterX(), target.exactCenterY()));
        SystemClock.sleep(350L);
        waitForResponsiveAndroidUi();
    }

    private boolean isViewFullyVisible(View view) {
        AtomicBoolean visible = new AtomicBoolean(false);
        instrumentation.runOnMainSync(() -> {
            Rect bounds = new Rect();
            visible.set(view.isAttachedToWindow() && view.isShown()
                    && view.getGlobalVisibleRect(bounds)
                    && bounds.width() == view.getWidth() && bounds.height() == view.getHeight());
        });
        return visible.get();
    }

    private boolean isTapTargetFullyVisible(View view) {
        AtomicBoolean visible = new AtomicBoolean(false);
        instrumentation.runOnMainSync(() -> {
            Rect bounds = new Rect();
            visible.set(view.isAttachedToWindow() && view.isShown() && view.isEnabled()
                    && view.isClickable() && view.getGlobalVisibleRect(bounds)
                    && bounds.width() == view.getWidth() && bounds.height() == view.getHeight());
        });
        return visible.get();
    }

    private void await(Check check, String failure) throws Exception {
        await(check, failure, 5000L);
    }

    private void await(Check check, String failure, long timeoutMs) throws Exception {
        long end = SystemClock.uptimeMillis() + timeoutMs;
        int iteration = 0;
        while (SystemClock.uptimeMillis() < end) {
            if (iteration++ % 5 == 0) waitForResponsiveAndroidUi();
            if (check.ok()) return;
            Thread.sleep(100L);
        }
        AccessibilityNodeInfo root = instrumentation.getUiAutomation().getRootInActiveWindow();
        throw new AssertionError(failure + "; timer state={running=" + TimerReceiver.isRunning(app)
                + ", end=" + DataStore.prefs(app).getLong("timer_end", 0L)
                + ", duration=" + DataStore.prefs(app).getLong("timer_duration", 0L)
                + ", remaining=" + DataStore.prefs(app).getLong("timer_remaining", 0L) + "}"
                + "; stored alarms=" + DataStore.array(app, "alarms")
                + "; recent app events=" + DataStore.array(app, "events")
                + "; active window=" + describeAccessibilityTree(root, new StringBuilder(), 0));
    }

    private boolean alarmEnabled(long alarmId, boolean expected) {
        JSONArray alarms = DataStore.array(app, "alarms");
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject alarm = alarms.optJSONObject(i);
            if (alarm != null && alarm.optLong("id", -1) == alarmId) {
                return alarm.optBoolean("enabled", true) == expected;
            }
        }
        return false;
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
