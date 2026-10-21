"""Source-contract smoke checks. These are NOT Android runtime/UI/audio tests.
Run: python3 -m unittest discover -s qa -v
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/tictic/clock"
# Extracted MySpace modules (SUITE-PLAN.md §3.1). Files that move out of :app
# are read from here instead, so the contracts keep guarding them.
CORE_DOMAIN = ROOT / "core-domain/src/main/java/app/space/core/domain"
CORE_DESIGN = ROOT / "core-design/src/main/java/app/space/core/design"
CORE_DATA = ROOT / "core-data/src/main/java/app/space/core/data"
CORE_SUITE = ROOT / "core-suite/src/main/java/app/space/core/suite"
SHELL = ROOT / "shell/src/main/java/app/space/shell"
FEATURE_CLOCK = ROOT / "feature-clock/src/main/java/app/space/feature/clock"
CORE_PLATFORM = ROOT / "core-platform/src/main/java/app/space/core/platform"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def read_core_domain(name):
    return (CORE_DOMAIN / name).read_text(encoding="utf-8")


def read_feature_clock(name):
    return (FEATURE_CLOCK / name).read_text(encoding="utf-8")


def read_core_design(name):
    return (CORE_DESIGN / name).read_text(encoding="utf-8")


def java_code_only(text):
    """Strip Java comments so dependency and coupling contracts test code, not prose.

    Module javadoc legitimately names the classes a module must *not* depend on —
    explaining the rule is the point of the comment. Without this, a contract that
    asserts "core-data must not reference DataStore" fails on the very sentence that
    documents why. This is a character state machine rather than a regex because
    string literals may legally contain "//" (a URL) or "/*", and naively cutting at
    those would remove real code and hide an actual violation.
    """
    out = []
    i, n = 0, len(text)
    in_string = in_char = in_line_comment = in_block_comment = False
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if in_line_comment:
            if c == "\n":
                in_line_comment = False
                out.append(c)
        elif in_block_comment:
            if c == "*" and nxt == "/":
                in_block_comment = False
                i += 1
        elif in_string:
            out.append(c)
            if c == "\\":
                if nxt:
                    out.append(nxt)
                    i += 1
            elif c == '"':
                in_string = False
        elif in_char:
            out.append(c)
            if c == "\\":
                if nxt:
                    out.append(nxt)
                    i += 1
            elif c == "'":
                in_char = False
        else:
            if c == "/" and nxt == "/":
                in_line_comment = True
                i += 1
            elif c == "/" and nxt == "*":
                in_block_comment = True
                i += 1
            elif c == '"':
                in_string = True
                out.append(c)
            elif c == "'":
                in_char = True
                out.append(c)
            else:
                out.append(c)
        i += 1
    return "".join(out)


class AlarmContracts(unittest.TestCase):
    def test_new_and_migrated_reminders_default_to_30_minutes(self):
        main = read("MainActivity.java")
        scheduler = read("AlarmScheduler.java")
        datastore = read("DataStore.java")
        self.assertIn('prefs.getInt("alarm_default_reminder_minutes",30)', main)
        self.assertIn('putInt("alarm_default_reminder_minutes",useCustom[0]?customLead[0]:leads[leadPos])', main)
        self.assertIn('o.optInt("reminder_minutes",30)', scheduler)
        # Step 1d-4: the v1.9.2 reminder-defaults migration moved to :feature-clock.
        rules = "".join(java_code_only(read_feature_clock("AlarmRules.java")).split())
        self.assertIn('alarm.put("reminder_minutes",30)', rules)
        self.assertIn("AlarmRules.migrateReminderDefaults(c)", datastore)

    def test_reminder_default_is_distinct_bundled_ping(self):
        main = read("MainActivity.java")
        receiver = read("PreAlarmReceiver.java")
        self.assertIn('editorReminderSoundValue="ping"', main)
        self.assertIn('R.raw.reminder_ping', receiver)
        self.assertIn('setContentTitle("Reminder · "+label)', receiver)

    def test_pre_alarm_receiver_posts_notification_not_alarm_service(self):
        receiver = read("PreAlarmReceiver.java")
        self.assertIn("manager.notify(notificationId(id),b.build())", receiver)
        self.assertNotIn("AlarmSoundService", receiver)

    def test_alarm_service_keeps_actual_alarm_separate(self):
        service = read("AlarmSoundService.java")
        self.assertIn("startRing(", service)
        self.assertIn('setCategory(Notification.CATEGORY_ALARM)', service)

    def test_existing_default_is_migrated_on_package_update(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("android.intent.action.MY_PACKAGE_REPLACED", manifest)
        self.assertIn("DataStore.migrateReminderDefaults(c)", read("BootReceiver.java"))

    def test_volume_key_modes_are_consumed_before_framework_volume_change(self):
        activity = read("AlarmActivity.java")
        self.assertIn("dispatchKeyEvent(KeyEvent event)", activity)
        self.assertIn("AlarmActionReceiver.snooze(this,id)", activity)
        self.assertIn("AlarmActionReceiver.dismiss(this,id)", activity)
        self.assertNotIn("FLAG_DISMISS_KEYGUARD", activity)

    def test_ringing_notification_only_offers_configured_snooze_and_stop(self):
        service = read("AlarmSoundService.java")
        self.assertIn('if(DataStore.showRingingSnoozeAction(this))nb.addAction', service)
        self.assertIn('"Stop",AlarmActionReceiver.pending(this,AlarmActionReceiver.ACTION_DISMISS', service)
        self.assertNotIn('"Turn off",AlarmActionReceiver.pending(this,AlarmActionReceiver.ACTION_DISABLE', service)
        self.assertIn('"Alarm is ringing · Stop"', service)

    def test_snooze_state_is_saved_and_cancellable(self):
        actions = read("AlarmActionReceiver.java")
        main = read("MainActivity.java")
        self.assertIn('putLong(SNOOZE_PREFIX+id,until)', actions)
        self.assertIn("Cancel snooze", main)
        self.assertIn("cancelSnooze(this,alarm.optLong(\"id\"),snoozeUntil)", main)

    def test_pre_alarm_notification_can_turn_off_its_alarm(self):
        prealarm = read("PreAlarmReceiver.java")
        actions = read("AlarmActionReceiver.java")
        self.assertIn('"Turn off alarm"', prealarm)
        self.assertIn("AlarmActionReceiver.ACTION_DISABLE", prealarm)
        self.assertIn("static void disable(Context c,long id)", actions)

    def test_disabled_or_deleted_alarm_cannot_ring_from_queued_delivery(self):
        receiver = read("AlarmReceiver.java")
        self.assertIn("if(o==null)", receiver)
        self.assertIn('!o.optBoolean("enabled",true)', receiver)
        self.assertIn("AlarmScheduler.cancel(c,id);return;", receiver)
        self.assertIn("snoozeUntil(c,id)<=0", receiver)

    def test_edit_updates_existing_alarm_and_schedules_saved_object(self):
        main = read("MainActivity.java")
        self.assertIn('saved.optLong("id")==alarm.optLong("id")', main)
        self.assertIn("alarms.put(i,alarm)", main)
        self.assertIn("AlarmScheduler.schedule(this,alarm)", main)

    def test_custom_reminder_editor_is_always_available_for_custom_value(self):
        main = read("MainActivity.java")
        self.assertIn('"Edit custom minutes"', main)
        self.assertIn('"Set custom minutes…"', main)
        self.assertIn('customLeadSelected[0]?customLead[0]:leadValues[leadPos]', main)


class DailyFeatureContracts(unittest.TestCase):
    def test_clock_uses_live_local_time(self):
        main = read("MainActivity.java")
        self.assertIn("clockLive.setText(currentClockTime())", main)

    def test_timer_has_start_pause_reset_and_completion_notification(self):
        main = read("MainActivity.java")
        timer = read("TimerReceiver.java")
        self.assertIn("private void startTimer()", main)
        self.assertIn("private void pauseTimer()", main)
        self.assertIn("private void cancelTimer()", main)
        self.assertIn("Timer complete", timer)

    def test_stopwatch_has_lap_pause_and_reset(self):
        main = read("MainActivity.java")
        self.assertIn("private void addLap()", main)
        self.assertIn("private void stopwatchPause()", main)
        self.assertIn("private void resetStopwatch()", main)


class NewFeatureContracts(unittest.TestCase):
    def test_alarm_editor_uses_interactive_dial_and_repeat_days(self):
        main = read("MainActivity.java")
        editor = main[main.index("private void showAlarmEditor"):main.index("private String reminderSoundSummary")]
        self.assertIn("AlarmTimeDialView", editor)
        self.assertIn("dial.setListener", editor)
        self.assertIn("repeatSwitch", editor)
        self.assertIn("Choose at least one repeat day", editor)
        self.assertIn('o.put("date_millis",day.getTimeInMillis())', editor)
        self.assertIn("More settings", editor)

    def test_new_alarm_defaults_are_saved_and_used_without_removing_overrides(self):
        main = read("MainActivity.java")
        self.assertIn('buildAlarmDefaultsCard()', main)
        self.assertIn('putInt("alarm_default_snooze",snoozeValues[snoozePos])', main)
        self.assertIn('prefs.getInt("alarm_default_snooze",9)', main)
        self.assertIn('prefs.getBoolean("alarm_default_vibrate",true)', main)
        self.assertIn('.put("snooze",snoozeValues[snooze.getSelectedItemPosition()])', main)

    def test_reminders_are_persisted_scheduled_and_rescheduled_on_boot(self):
        main = read("MainActivity.java")
        scheduler = read("ReminderScheduler.java")
        receiver = read("ReminderReceiver.java")
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        boot = read("BootReceiver.java")
        self.assertIn('private void buildReminders()', main)
        self.assertIn('DataStore.putArray(this,"reminders",list)', main)
        self.assertIn("ReminderScheduler.schedule(this,o)", main)
        self.assertIn('DataStore.nextOccurrence(reminder,after)', scheduler)
        self.assertIn('reminder.optLong("skip_through",0)', scheduler)
        self.assertIn('ReminderScheduler.rescheduleAll(c)', boot)
        self.assertIn('android:name=".ReminderReceiver"', manifest)
        self.assertIn('ACTION_SNOOZE', receiver)

    def test_widget_studio_reports_text_contrast_and_uses_theme_aware_dialogs(self):
        studio = read("WidgetConfigActivity.java")
        self.assertIn("updateContrast()", studio)
        self.assertIn("contrastRatio(textColor,base)", studio)
        self.assertIn("dialogBuilder()", studio)


    def test_alarm_editor_content_is_attached_to_dimmed_dialog(self):
        main = read("MainActivity.java")
        editor = main[main.index("private void showAlarmEditor"):main.index("private String leadNamesFor")]
        self.assertIn("dlg.setContentView(shell)", editor)
        self.assertIn("shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1))", editor)
        self.assertIn("window.setLayout(", editor)

    def test_reminder_tab_is_adjacent_to_clock(self):
        main = read("MainActivity.java")
        self.assertIn('tabs={"Clock","Reminders","Timer","Stopwatch","Routines","Usage"}', main)
        self.assertIn('"reminders".equals(target))tab=1', main)
        self.assertIn('case 1:buildReminders();break;case 2:buildTimer();break;', main)

    def test_reminder_list_has_single_add_and_row_opens_editor(self):
        main = read("MainActivity.java")
        screen = main[main.index("private void buildReminders"):main.index("private String timeAt")]
        self.assertEqual(screen.count('"＋  Add reminder"'), 1)
        self.assertIn('pillButton("Dismiss"', screen)
        self.assertIn('c.setOnClickListener(v->showReminderEditor(reminder))', screen)
        self.assertNotIn('pillButton("Edit"', screen)
        self.assertNotIn('emptyState(', screen)

    def test_settings_is_a_full_page_with_required_group_navigation(self):
        main = read("MainActivity.java")
        self.assertIn("private void renderSettingsPage()", main)
        for title in ('"Design"','"Alarms"','"Reminders"','"Routines"','"Timer"','"Stopwatch"','"Efficiency\\n& data"','"About\\n& permissions"'): 
            self.assertIn(title, main)
        self.assertIn('private void buildSettings(){switch(settingsGroup)', main)
        self.assertNotIn("openSettingsPopup", main)

    def test_reminder_snooze_default_and_usage_events_are_wired(self):
        main = read("MainActivity.java")
        action = read("ReminderActionReceiver.java")
        receiver = read("ReminderReceiver.java")
        self.assertIn('putInt("reminder_snooze_minutes",mins[ix])', main)
        self.assertIn('getInt("reminder_snooze_minutes",10)', action)
        self.assertIn('"Snooze "+snoozeMinutes+" min"', receiver)
        self.assertIn('"Reminder dismissed"', main)
        self.assertIn('DataStore.log(this,"Settings","Alarm defaults updated"', main)


    def test_alarm_ampm_buttons_reliably_update_hour_and_dial_selection(self):
        main = read("MainActivity.java")
        editor = main[main.index("private void showAlarmEditor"):main.index("private String reminderSoundSummary")]
        self.assertIn('TextView amButton=label("AM"', editor)
        self.assertIn('pmButton=label("PM"', editor)
        self.assertIn('amButton.setOnClickListener(v->{dial.setHour(dial.hour()%12)', editor)
        self.assertIn('pmButton.setOnClickListener(v->{dial.setHour(dial.hour()%12+12)', editor)

    def test_alarm_routine_and_reminder_time_pick_share_the_clock_dial(self):
        main = read("MainActivity.java")
        self.assertIn('new AlarmTimeDialView(this,DataStore.dark(this),accent,is24)', main)
        self.assertIn('showTimeDialPicker("Routine start time"', main)
        self.assertIn('showTimeDialPicker("Reminder time"', main)
        self.assertIn('showTimeDialPicker("Routine default start"', main)

    def test_reminder_sound_duration_and_cancel_stop_audio(self):
        main = read("MainActivity.java")
        receiver = read("ReminderReceiver.java")
        scheduler = read("ReminderScheduler.java")
        action = read("ReminderActionReceiver.java")
        service = read("ReminderSoundService.java")
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('REMINDER_TONE_NAMES={"Use settings default"', main)
        self.assertIn('REMINDER_RING_SECONDS={1,3,5,10,15,30}', main)
        self.assertIn('"reminder_default_ring_seconds"', main)
        self.assertIn('ReminderSoundService.start(context,id,notificationId,alertTone,duration,notification)', receiver)
        self.assertIn('ReminderSoundService.stop(c,id)', scheduler)
        self.assertIn('ReminderSoundService.stop(c,id)', action)
        self.assertIn('FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK', service)
        self.assertIn('android:name=".ReminderSoundService"', manifest)

    def test_settings_has_master_routine_timer_stopwatch_controls(self):
        main = read("MainActivity.java")
        self.assertIn('case 3:buildRoutineSettings();break;case 4:buildTimerSettings();break;case 5:buildStopwatchSettings()', main)
        for key in ('routine_default_hour','routine_default_duration','routine_default_days','routine_notifications_enabled',
                    'timer_default_duration','timer_alerts_enabled','timer_vibrate_enabled',
                    'stopwatch_show_hundredths','stopwatch_lap_haptics'):
            self.assertIn(key, main)

    def test_design_exposes_more_colors_fonts_and_card_corner_choices(self):
        main = read("MainActivity.java")
        datastore = read("DataStore.java")
        self.assertIn('"Cobalt","Plum","Citrus","Lagoon"', main)
        self.assertIn('"APP TYPEFACE"', main)
        self.assertIn('"Card corners",new String[]{"Signature","Soft","Crisp","Capsule"}', main)
        # The 16-accent palette moved to :core-data in step 1d; DataStore keeps a
        # delegating reference. Count the colours where they now live.
        theme = (CORE_DATA / "AppTheme.java").read_text(encoding="utf-8")
        start = theme.index("ACCENT_COLORS")
        self.assertEqual(theme[start:theme.index("};", start)].count("0xff"), 16)
        self.assertIn("ACCENT_COLORS=AppTheme.ACCENT_COLORS", "".join(datastore.split()))

    def test_every_widget_provider_supports_launcher_resize_and_studio_spacing_preview(self):
        providers=list((ROOT / "app/src/main/res/xml").glob("widget_*_info.xml"))
        self.assertEqual(len(providers),24)
        for provider in providers:
            xml=provider.read_text()
            self.assertIn('android:resizeMode="horizontal|vertical"',xml)
            self.assertIn('android:minResizeWidth="150dp" android:minResizeHeight="84dp"',xml)
        engine=read("WidgetEngine.java")
        studio=read("WidgetConfigActivity.java")
        self.assertIn('int verticalGap=density==0?0:density==2?7:3',engine)
        self.assertIn('v.setViewPadding(view,0,dp(c,verticalGap),0,dp(c,verticalGap))',engine)
        self.assertIn('float fitScale=width<155?.68f:width<180?.84f:1f',engine)
        self.assertIn('previewViews(this,id,provider.initialLayout,style,previewWidthDp,previewHeightDp)',studio)
        self.assertIn('previewWidthDp=Math.max(80,actualW)',studio)

    def test_timer_and_routine_notification_controls_are_applied_in_receivers(self):
        timer=read("TimerReceiver.java")
        routine=read("RoutineReceiver.java")
        self.assertIn('timer_alerts_enabled',timer)
        self.assertIn('timer_vibrate_enabled',timer)
        self.assertIn('routine_notifications_enabled',routine)


    def test_app_dialogs_share_close_control_rounded_surface_and_outside_dismiss(self):
        main = read("MainActivity.java")
        # The shared alert dialog moved to :core-design (step 1h-iv.3); these four
        # assertions follow it. The two "Close ..." dialogs below are separate,
        # hand-built dialogs that stayed in MainActivity.
        builder = read_core_design("AppAlertBuilder.java")
        self.assertIn("public final class AppAlertBuilder extends AlertDialog.Builder", builder)
        self.assertIn('close.setContentDescription("Close dialog")', builder)
        self.assertIn("dialog.setCanceledOnTouchOutside(true)", builder)
        self.assertIn("window.setBackgroundDrawable(panel)", builder)
        self.assertIn('close.setContentDescription("Close time picker")', main)
        self.assertIn('close.setContentDescription("Close alarm editor")', main)
        studio = read("WidgetConfigActivity.java")
        self.assertIn('close.setContentDescription("Close Widget Studio")', studio)
        self.assertIn("setFinishOnTouchOutside(true)", studio)
        self.assertIn("private final class StudioAlertBuilder", studio)

    def test_usage_history_is_twenty_by_default_with_one_hundred_cap(self):
        main=read("MainActivity.java")
        self.assertIn("private int activityHistoryLimit=20",main)
        self.assertIn('Math.min(100,Math.max(20,activityHistoryLimit))',main)
        self.assertIn('pillButton("Show 80 more"',main)
        self.assertIn('activityHistoryLimit=100',main)
        # The history cap moved to :core-data in step 1d-2 (DataStore.log now
        # delegates). MAX_ENTRIES-1 previous rows plus the new one keeps the
        # original 100-entry bound.
        history=(CORE_DATA / "AppActivityLog.java").read_text(encoding="utf-8")
        # Whitespace-normalised so the assertion cannot break on formatting alone.
        compact="".join(history.split())
        self.assertIn('publicstaticfinalintMAX_ENTRIES=100',compact)
        self.assertIn('i<old.length()&&i<MAX_ENTRIES-1',compact)
        self.assertIn('AppPrefs.prefs(c).getBoolean("activity_history_enabled",true)',compact)
        self.assertIn('UsageTracker.recordEvent(c,type)',compact)
        self.assertIn('AppActivityLog.log(c, type, title, detail)',read("DataStore.java"))

    def test_home_clock_offers_digital_and_analog_style_variants(self):
        main=read("MainActivity.java")
        analog=read_core_design("AnalogClockView.java")
        self.assertIn('new String[]{"Soft","Serif","Mono","Rounded","Condensed"}',main)
        self.assertIn('new String[]{"Classic markers","Minimal","Numerals","Accent ring"}',main)
        self.assertIn('"clock_digital_style"',main)
        self.assertIn('"clock_analog_style"',analog)
        self.assertIn('STYLE_NAMES={"classic markers","minimal","numerals","accent ring"}',analog)

    def test_card_border_elevation_and_hardware_action_settings_are_applied(self):
        main=read("MainActivity.java")
        alarm=read("AlarmActivity.java")
        self.assertIn('"app_card_border"',main)
        self.assertIn('"app_card_elevation"',main)
        self.assertIn('"Volume Up"',main)
        self.assertIn('"Volume Down"',main)
        self.assertIn('String[] options={"Snooze","Stop"}',main)
        self.assertIn('"app_glass_enabled"',main)
        self.assertIn('"app_glass_opacity"',main)
        self.assertIn('"app_glass_finish"',main)
        self.assertIn('"app_glass_depth"',main)
        self.assertIn('Android reserves the Power key',main)
        self.assertIn('DataStore.alarmVolumeAction(this,code==KeyEvent.KEYCODE_VOLUME_UP)',alarm)
        self.assertIn('AlarmActionReceiver.dismiss(this,id)',alarm)


class UserFeedbackRegressionContracts(unittest.TestCase):
    def test_gel_press_and_elastic_scroll_feedback_are_wired(self):
        motion = read_core_design("GelMotion.java")
        scroll = read("AliveScrollView.java")
        main = read("MainActivity.java")
        self.assertIn("WATER_RIPPLE=0, GEL_SQUEEZE=1, EXPRESSIVE_SPRING=2, GLASS_WAVE=3, MAGNETIC_LIFT=4", motion)
        self.assertIn("target.animate().scaleX(sx).scaleY(sy)", motion)
        self.assertIn("new OvershootInterpolator(tension)", motion)
        self.assertIn("Settings.Global.ANIMATOR_DURATION_SCALE", motion)
        self.assertIn("target.performClick()", motion)
        self.assertIn("String[] names={\"Water ripple\",\"Gel squeeze\",\"Expressive spring\",\"Glass wave\",\"Magnetic lift\"}", main)
        self.assertIn("Reduce motion", main)
        self.assertIn("performClick()", scroll)
        self.assertIn("new AliveScrollView(this)", main)
        self.assertIn("new AliveScrollView(this)", read("WidgetConfigActivity.java"))
        self.assertIn("new AliveScrollView(this)", read("WidgetCatalogActivity.java"))
        self.assertIn("GelMotion.apply(root,accent)", main)
        self.assertIn("edgePull+fingerDelta*.22f", scroll)
        self.assertIn("content.setTranslationY(edgePull)", scroll)
        self.assertIn("Math.min(.032f", scroll)
        self.assertIn("new OvershootInterpolator(.72f)", scroll)

    def test_alarm_editor_is_opaque_fullscreen_with_fixed_actions(self):
        main=read("MainActivity.java")
        start=main.index("private void showAlarmEditor(JSONObject existing)")
        end=main.index("private interface TimeChoice",start)
        editor=main[start:end]
        self.assertIn("requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)",editor)
        self.assertIn("shell.setBackgroundColor(bg)",editor)
        self.assertIn("shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1))",editor)
        self.assertIn("configureFormDialogWindow(dlg)",editor)
        self.assertIn("window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)",main)
        self.assertIn("window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)",main)
        self.assertIn("boolean compact=getResources().getConfiguration().screenWidthDp<600||getResources().getConfiguration().screenHeightDp<500",main)
        self.assertIn("dlg.setCanceledOnTouchOutside(false)",editor)
        self.assertNotIn("widthPixels*.93f",editor)
        scroll=read("AliveScrollView.java")
        self.assertIn("setClipChildren(true)",scroll)

    def test_ringing_actions_and_per_key_volume_choices_are_safe(self):
        main = read("MainActivity.java")
        alarm = read("AlarmActivity.java")
        store = read("DataStore.java")
        service = read("AlarmSoundService.java")
        self.assertIn('new String[]{"Snooze + Stop","Stop only"}', main)
        self.assertIn('String[] options={"Snooze","Stop"}', main)
        self.assertIn('alarm_volume_up_action', main)
        self.assertIn('alarm_volume_down_action', main)
        self.assertIn('alarm_ringing_actions', main)
        self.assertIn("Ring-screen & notification actions", main)
        self.assertIn("full-screen alarm and its ringing notification", main)
        self.assertIn("Stop ends this ring only; the alarm and any repeating schedule stay enabled.", main)
        self.assertIn("do not change the on-screen or notification buttons", main)
        self.assertIn("Turn off alarm remains a separate action", main)
        self.assertIn('"Stop"', alarm)
        self.assertNotIn("Turn off this repeating alarm", alarm)
        self.assertNotIn('"Turn off",AlarmActionReceiver.pending(this,AlarmActionReceiver.ACTION_DISABLE', service)
        # Step 1d-4: the ringing-control settings moved to :feature-clock. DataStore keeps
        # the delegate and the doc comment its call sites were written against; the mapping
        # rule itself is asserted where it now lives.
        ringing = "".join(java_code_only(read_feature_clock("RingingControls.java")).split())
        self.assertIn("static int alarmVolumeAction(Context c, boolean volumeUp)", store)
        self.assertIn("RingingControls.volumeAction(c, volumeUp)", store)
        self.assertIn("Control volume becomes Snooze; Turn off becomes Stop", store)
        self.assertIn("intaction=(old==2||old==3)?2:1", ringing)
        self.assertIn("AlarmActionReceiver.snooze(this,id)", alarm)
        self.assertIn("AlarmActionReceiver.dismiss(this,id)", alarm)
        self.assertNotIn("setVolumeControlStream", alarm)
        self.assertNotIn("KEYCODE_POWER", alarm)
        self.assertIn('"Turn off alarm"', read("PreAlarmReceiver.java"))

    def test_settings_option_rerender_restores_scroll_position(self):
        main = read("MainActivity.java")
        self.assertIn("settingsScrollY=settingsScrollView.getScrollY()", main)
        self.assertIn("scroll.post(()->scroll.scrollTo(0,restoreY))", main)
        self.assertIn("settingsScrollY=0;settingsScrollView=null;render()", main)

    def test_card_elevation_has_visible_surface_and_shadow_differences(self):
        main = read("MainActivity.java")
        kit = read_core_design("UiKit.java")
        self.assertIn("cardFillForElevation(int level)", main)
        # Step 1h-ii moved card() and cardFillForElevation() into UiKit, so the elevation
        # curve lives there now. Compared whitespace-normalised: this asserts the shape of
        # the curve, not its formatting.
        compact = "".join(java_code_only(kit).split())
        self.assertIn("elevation==0?0f:elevation==2?9f+glassDepth*.05f:4f+glassDepth*.025f",
                      compact)
        self.assertIn("deeper shadow and clearer edge separation", main)

    def test_glass_surface_exposes_presets_customization_and_legibility(self):
        main = read("MainActivity.java")
        presets = read_core_design("GlassPreset.java")
        for key in ('"app_glass_enabled"','"app_glass_opacity"','"app_glass_finish"','"app_glass_depth"'):
            self.assertIn(key, main)
        self.assertIn("glassSurfaceDrawable", main)
        self.assertIn('"Surface opacity"', main)
        self.assertIn('"Sheen"', main)
        self.assertIn('"Depth"', main)
        self.assertIn("Five ready-made light moods", main)
        self.assertIn("GlassPreset.apply(this,index)", main)
        self.assertIn('"app_glass_customized"', main)
        self.assertIn("Math.max(82,opacityFloor)", read_core_design("AppGlassDrawable.java"))
        for name in ("Sunlit Bubble","Daylight Pearl","Golden Hour","Moonlit Opal","Aurora Frost"):
            self.assertIn(name, presets)
        kit = read_core_design("UiKit.java")
        self.assertIn("AppSurfaceStyle.pageBackground(context)", kit)
        self.assertIn("AppSurfaceStyle.glass(context, base, radius, opacity, finish, depth, preset, 82)", kit)
        # MainActivity reaches both through its delegates.
        self.assertIn("ui().pageBackground()", main)
        self.assertIn("ui().glassSurfaceDrawable(base,radius,opacity,finish,depth,preset)", main)
        self.assertIn("AppSurfaceStyle.criticalGlass(this,fill,17)", read("AlarmActivity.java"))
        studio=read("WidgetConfigActivity.java")
        self.assertIn("buildGlassPresetChoices(root,ink)", studio)
        self.assertIn('"glass_customized"', studio)
        self.assertIn("GlassPreset.widgetBase(preset)", studio)
        self.assertIn('"glass_preset"', read("WidgetEngine.java"))
        renderer = read_core_design("AppGlassDrawable.java")
        self.assertIn("extends Drawable", renderer)
        self.assertIn("new LinearGradient", renderer)
        self.assertIn("boolean dark", renderer)
        self.assertIn("new RadialGradient", renderer)
        self.assertIn("canvas.drawPath(softBand,reflection)", renderer)
        self.assertNotIn("glint", renderer)

    def test_timer_and_stopwatch_precision_and_haptics_are_configurable(self):
        main = read("MainActivity.java")
        math = read_core_domain("TimeMath.java")
        for key in ('"timer_show_hundredths"','"timer_button_haptics"','"timer_vibrate_enabled"','"stopwatch_show_hundredths"','"stopwatch_button_haptics"','"stopwatch_lap_haptics"'):
            self.assertIn(key, main)
        self.assertIn("formatCountdownHundredths", math)
        self.assertIn("timerButtonHaptic(v)", main)
        # Step 1h-iv·2: the stopwatch UI moved into StopwatchScreen, so its haptic call is
        # asserted there. The preference key is unchanged and still asserted above.
        self.assertIn("buttonHaptic(v)", read_feature_clock("StopwatchScreen.java"))

    def test_blank_reminder_and_routine_names_get_next_available_sequence(self):
        main = read("MainActivity.java")
        self.assertIn('title.setHint("Reminder title (optional)")', main)
        self.assertIn('name.setHint("Routine name (optional)")', main)
        self.assertIn('nextSequenceName("Reminder",list,"title",edit?id:-1)', main)
        self.assertIn('nextSequenceName("Routine",list,"name",edit?id:-1)', main)
        self.assertNotIn('title.setError("Add a reminder title")', main)
        self.assertIn('String.format(Locale.ROOT,"%02d",i)', main)

    def test_alarm_dial_rejects_touches_that_start_outside_the_face(self):
        dial = read("AlarmTimeDialView.java")
        self.assertIn("startsInsideDial(float x,float y)", dial)
        self.assertIn("if(!startsInsideDial(event.getX(),event.getY()))", dial)
        self.assertIn("if(!touchStartedInsideDial)return false", dial)

    def test_stopwatch_uses_monotonic_elapsed_time_and_fast_visible_refresh(self):
        main = read("MainActivity.java")
        math = read_core_domain("TimeMath.java")
        self.assertIn("SystemClock.elapsedRealtime()", main)
        self.assertIn("TimeMath.stopwatchElapsed", main)
        # Step 1h-iv·2: the screen owns its ticker now, so the fast-refresh guarantee is
        # asserted where the cadence lives. MainActivity's shared ticker no longer drives
        # the stopwatch at all — leaving that branch in would run it at 60 fps to update a
        # field that is permanently null.
        screen = read_feature_clock("StopwatchScreen.java")
        self.assertIn("TimeMath.stopwatchElapsed", screen)
        self.assertIn("if (running()) return saver ? 100L : 16L;", screen)
        self.assertIn("TimeMath.formatHundredths(ms)", screen)
        self.assertNotIn("tab==3&&swRunning()", main)
        self.assertIn("removeCallbacks(ticker);handler.post(ticker)", main)

    def test_timer_uses_monotonic_deadline_with_boot_and_wall_change_recovery(self):
        timer = read("TimerReceiver.java")
        scheduler = read("AlarmScheduler.java")
        main = read("MainActivity.java")
        self.assertIn('"timer_end_elapsed"', timer)
        self.assertIn('"timer_boot_count"', timer)
        self.assertIn("TimeMath.countdownRemaining", timer)
        self.assertIn("scheduleRemaining(context, left)", timer)
        self.assertIn("TimerReceiver.scheduleRemaining(c,left)", scheduler)
        self.assertIn('"android.intent.action.TIME_SET"', (ROOT / "app/src/main/AndroidManifest.xml").read_text())
        self.assertIn("requestTimerStart()", main)
        self.assertIn("Start with possible delay", main)

    def test_timer_custom_number_pickers_receive_theme_aware_styles(self):
        main = read("MainActivity.java")
        self.assertIn("styleNumberPicker(picker);return picker", main)
        self.assertIn("stylePickerDescendants(picker)", main)
        self.assertIn("field.setTextColor(text)", main)
        self.assertIn("new android.view.ContextThemeWrapper(this,theme)", main)
        styles=(ROOT / "app/src/main/res/values/styles.xml").read_text()
        self.assertIn('style name="TicTicDarkTheme"',styles)
        self.assertIn('android:colorControlActivated',styles)

    def test_alarm_and_reminder_tones_can_be_previewed_and_preview_is_bounded(self):
        main = read("MainActivity.java")
        preview = read("TonePreview.java")
        self.assertIn('"▶  Hear alarm sound"', main)
        self.assertIn('"▶  Hear reminder sound"', main)
        self.assertIn("previewDefaultReminder", main)
        self.assertIn("handler.postDelayed(stopTask, duration)", preview)
        self.assertIn("Math.min(8000L, maxMillis)", preview)

    def test_ringing_ui_and_notification_never_offer_repeating_alarm_turn_off(self):
        alarm = read("AlarmActivity.java")
        service = read("AlarmSoundService.java")
        main = read("MainActivity.java")
        self.assertNotIn("Turn off this repeating alarm", alarm)
        self.assertNotIn('"Turn off",AlarmActionReceiver.pending(this,AlarmActionReceiver.ACTION_DISABLE', service)
        self.assertIn('new String[]{"Snooze + Stop","Stop only"}', main)
        self.assertIn('if(DataStore.showRingingSnoozeAction(this))', service)
        self.assertIn('TextView stop=action("Stop"', alarm)
        self.assertIn('DataStore.log(c,"Alarm","Stopped",label)', read("AlarmActionReceiver.java"))
        self.assertIn('"Turn off alarm"', read("PreAlarmReceiver.java"))

    def test_volume_up_and_down_have_independent_snooze_stop_preferences(self):
        store = read("DataStore.java")
        main = read("MainActivity.java")
        alarm = read("AlarmActivity.java")
        ringing = read_feature_clock("RingingControls.java")
        self.assertIn('"alarm_volume_up_action"', ringing)
        self.assertIn('"alarm_volume_down_action"', ringing)
        # The facade still exposes the per-key accessor its call sites use.
        self.assertIn("static int alarmVolumeAction(Context c, boolean volumeUp)", store)
        self.assertIn('"alarm_volume_up_action",true', main)
        self.assertIn('"alarm_volume_down_action",false', main)
        self.assertIn('String[] options={"Snooze","Stop"}', main)
        self.assertIn('alarmVolumeAction(this,code==KeyEvent.KEYCODE_VOLUME_UP)', alarm)

    def test_usage_report_is_user_saved_csv_with_range_and_history(self):
        main = read("MainActivity.java")
        self.assertIn('Save usage report · CSV', main)
        self.assertIn('Intent.ACTION_CREATE_DOCUMENT', main)
        self.assertIn('intent.setType("text/csv")', main)
        self.assertIn('startActivityForResult(intent,87)', main)
        self.assertIn('writeUsageReport(data.getData())', main)
        self.assertIn('"Recent activity in selected window', main)
        self.assertIn('"Clock screen time"', main)
        self.assertIn('labels/details may be included', main)

    def test_usage_timeline_uses_distinct_segmented_range_and_filters_history(self):
        main = read("MainActivity.java")
        self.assertIn('"REPORTING WINDOW"', main)
        self.assertIn('String[] labels={"Today","7 days","30 days"}', main)
        self.assertIn('rail.setBackground(shape(DataStore.dark(this)?0xff101c1a:0xffe4ebe7,18))', main)
        self.assertIn('activityEventsInRange()', main)
        self.assertIn('activityRangeCaption()', main)

    def test_glass_character_is_shared_across_app_pages(self):
        style = read_core_design("AppSurfaceStyle.java")
        main = read("MainActivity.java")
        alarm = read("AlarmActivity.java")
        studio = read("WidgetConfigActivity.java")
        catalog = read("WidgetCatalogActivity.java")
        presets = read_core_design("GlassPreset.java")
        for token in ('app_glass_opacity','app_glass_finish','app_glass_depth'):
            self.assertIn(token, style)
        for token in ('Sunlit Bubble','Daylight Pearl','Golden Hour','Moonlit Opal','Aurora Frost'):
            self.assertIn(token, presets)
        # MainActivity reaches the page background through UiKit since step 1h-ii. The
        # other pages still call AppSurfaceStyle directly and are asserted below, which is
        # the point of this test: one shared material, several callers.
        self.assertIn('ui().pageBackground()',main)
        self.assertIn('AppSurfaceStyle.pageBackground(context)',read_core_design("UiKit.java"))
        self.assertIn('ringingBackground()',alarm)
        self.assertIn('AppSurfaceStyle.criticalGlass(this,fill,17)',alarm)
        self.assertIn('buildGlassPresetChoices(root,ink)',studio)
        self.assertIn('GlassPreset.glow(glassPreset)',read("WidgetEngine.java"))
        self.assertIn('AppSurfaceStyle.pageBackground(this)',catalog)

    def test_alarm_defaults_editor_and_settings_keep_content_bounded(self):
        main = read("MainActivity.java")
        start=main.index("private void showAlarmDefaultsEditor()")
        end=main.index("private void buildSettings()",start)
        editor=main[start:end]
        self.assertIn("shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1))",editor)
        self.assertIn("shell.addView(footer,footerP)",editor)
        self.assertIn("TextView save=pillButton(\"Save defaults\"",editor)
        self.assertIn("configureFormDialogWindow(dlg)",editor)
        self.assertIn("boolean compact=getResources().getConfiguration().screenWidthDp<600||getResources().getConfiguration().screenHeightDp<500",main)
        self.assertIn("scroll.setClipChildren(true);scroll.setClipToPadding(true)",main)
        self.assertIn("content.setClipChildren(true);content.setClipToPadding(true)",main)

    def test_widget_studio_pins_preview_actions_and_bounds_its_form(self):
        studio=read("WidgetConfigActivity.java")
        start=studio.index("private void render()")
        end=studio.index("private interface Selection",start)
        render=studio[start:end]
        self.assertIn("screen.addView(fixed",render)
        self.assertIn("screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1))",render)
        self.assertIn("screen.addView(actions,actionsP)",render)
        self.assertIn("sceneHeight=Math.max(112,Math.min(218",render)
        self.assertIn("scroll.setClipChildren(true);scroll.setClipToPadding(true)",render)
        self.assertIn("screenWidthDp<600",studio)
        styles=(ROOT / "app/src/main/res/values/styles.xml").read_text()
        self.assertIn("TicTicWidgetStudioDarkFull",styles)
        self.assertIn("TicTicWidgetStudioLightFull",styles)

    def test_widget_glass_preset_customization_persists_and_renders(self):
        studio=read("WidgetConfigActivity.java")
        engine=read("WidgetEngine.java")
        self.assertIn("buildGlassPresetChoices(root,ink)",studio)
        self.assertIn("glassCustomized=true",studio)
        self.assertIn("glassCustomized=false",studio)
        self.assertIn('putBoolean(k+\"glass_customized\",glassCustomized)',studio)
        self.assertIn('putInt(k+\"glass_preset\",glassPreset)',studio)
        self.assertIn('p+\"glass_preset\"',engine)
        self.assertIn("GlassPreset.glow(glassPreset)",engine)

    def test_about_version_matches_release_version(self):
        main = read("MainActivity.java")
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("versionName '2.0.0'", gradle)
        self.assertIn("versionCode 23", gradle)
        self.assertIn("TicTic · version 2.0.0", main)

    def test_myspace_rebrand_is_applied(self):
        """MySpace / app.space.one — SUITE-PLAN.md §4.

        The namespace intentionally stays app.tictic.clock until :feature-clock
        is extracted, because it drives the R class and resolves the manifest's
        relative component names. Asserting both facts keeps a later refactor
        from silently breaking the 24 widget providers.
        """
        gradle = (ROOT / "app/build.gradle").read_text()
        strings = (ROOT / "app/src/main/res/values/strings.xml").read_text()
        self.assertIn("applicationId 'app.space.one'", gradle)
        self.assertIn("namespace 'app.tictic.clock'", gradle)
        self.assertIn('<string name="app_name">MySpace</string>', strings)
        self.assertIn('<string name="app_tagline">One for many</string>', strings)

    def test_min_sdk_is_26(self):
        """Raised from 23 to unlock CameraX/ML Kit and modern AndroidX — SUITE-PLAN.md §5.1."""
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("minSdk 26", gradle)
        self.assertIn("targetSdk 34", gradle)
        self.assertNotIn("minSdk 23", gradle)


class ModuleGraphContracts(unittest.TestCase):
    """Guards the MySpace modularisation (SUITE-PLAN.md §3.1).

    The whole point of the module graph is that adding app N stays cheap and that
    no app can assume another is installed. These contracts fail loudly if a later
    refactor shortcuts that — e.g. by depending across features, by letting a core
    module import Android, or by moving a file out of :app without repointing the
    contracts that guard it.
    """

    def test_core_domain_is_registered_and_consumed_by_app(self):
        settings = (ROOT / "settings.gradle").read_text()
        app_gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("rootProject.name = 'MySpace'", settings)
        self.assertIn("include ':core-domain'", settings)
        self.assertIn("implementation project(':core-domain')", app_gradle)

    def test_time_formatting_moved_to_core_data_and_locale_rules_hold(self):
        """SUITE-PLAN.md §6.1 step 1d-3.

        Formatting lives in the data layer, not the design system, because what it
        encodes is a reading of user settings: formatTime switches 12/24-hour purely
        from the system preference, and every suite app must honour it identically or
        the device looks inconsistent.

        Also pins the locale rule that separates the two extracted modules: strings
        shown to a person follow the device locale, while numeric readouts must not
        vary by locale. Getting this backwards produces either untranslated month
        names or a locale-dependent stopwatch display.
        """
        fmt_src = (CORE_DATA / "AppFormat.java").read_text(encoding="utf-8")
        datastore = read("DataStore.java")
        self.assertIn("package app.space.core.data;", fmt_src)
        self.assertIn("public final class AppFormat", fmt_src)
        self.assertIn("public static String formatTime(Context c, int hour, int minute)", fmt_src)
        self.assertIn("public static String formatDate(long millis)", fmt_src)
        # The 12/24-hour switch is the whole reason this is shared.
        self.assertIn("DateFormat.is24HourFormat(c)", fmt_src)
        # Delegation, not duplication.
        self.assertIn("return AppFormat.formatTime(c, hour, minute);", datastore)
        self.assertIn("return AppFormat.formatDate(millis);", datastore)
        self.assertNotIn("is24HourFormat", datastore)
        self.assertNotIn('"EEE, d MMM"', datastore)
        # Locale rule: user-facing text follows the device; numeric readouts do not.
        self.assertIn("Locale.getDefault()", fmt_src)
        self.assertIn("Locale.ROOT", read_core_domain("TimeMath.java"))

    def test_glass_trio_moved_into_core_design_with_a_deliberately_narrow_surface(self):
        """Step 1c-2b: GlassPreset, AppSurfaceStyle and AppGlassDrawable.

        These three moved together because they reference each other; splitting them
        would have put two of them in different modules with a cyclic feel.

        The public surface is asserted member by member because it was chosen
        deliberately, not by blanket-widening. Two specific things are guarded:

        1. All THREE AppSurfaceStyle.glass() overloads must be public. A scripted
           visibility edit that replaced only the first occurrence left the 7- and
           8-arg overloads package-private, which breaks MainActivity at compile time
           — and there is no local compiler to catch that before CI.
        2. AppGlassDrawable must stay PACKAGE-PRIVATE. Only AppSurfaceStyle
           constructs it, and screens receive it typed as Drawable. Widening it would
           invite callers to bypass the opacity floor that keeps text legible.
        """
        presets = read_core_design("GlassPreset.java")
        style = read_core_design("AppSurfaceStyle.java")
        renderer = read_core_design("AppGlassDrawable.java")
        for name in ("GlassPreset.java", "AppSurfaceStyle.java", "AppGlassDrawable.java"):
            self.assertFalse((JAVA / name).exists(), f"{name} must not remain in :app")
            self.assertIn(f"package app.space.core.design;",
                          read_core_design(name))

        # 1. GlassPreset: consumers read the tuning arrays and the accessors directly.
        self.assertIn("public final class GlassPreset", presets)
        for member in ("public static final String[] NAMES", "public static final String[] NOTES",
                       "public static final int[] OPACITY", "public static final int[] FINISH",
                       "public static final int[] DEPTH", "public static int clamp(",
                       "public static int selected(", "public static void apply(",
                       "public static int glow(", "public static int widgetBase(",
                       "public static int widgetOpacity("):
            self.assertIn(member, presets)
        # Palette arrays stay behind their accessors so a caller cannot index them
        # without the clamp that prevents an out-of-range preset.
        for hidden in ("static final int[] GLOW=", "static final int[] BASE=",
                       "static final int[] WIDGET_OPACITY="):
            self.assertIn(hidden, presets)
            self.assertNotIn("public " + hidden, presets)

        # 2. AppSurfaceStyle: all three glass() overloads public. Its private colour blend
        #    was deleted in step 1h-ii and unified into Palette.blend — see
        #    test_colour_blend_is_defined_exactly_once.
        self.assertIn("public final class AppSurfaceStyle", style)
        self.assertEqual(3, style.count("public static Drawable glass("),
                         "every glass() overload must be public across the module boundary")
        self.assertNotIn("\n    static Drawable glass(", style)
        self.assertIn("public static boolean enabled(", style)
        self.assertIn("public static Drawable criticalGlass(", style)
        self.assertIn("public static Drawable pageBackground(", style)
        self.assertNotIn("private static int mix(", style)
        self.assertIn("Palette.blend(", style)

        # 3. AppGlassDrawable stays package-private; screens only ever see Drawable.
        self.assertIn("final class AppGlassDrawable extends Drawable", renderer)
        self.assertNotIn("public final class AppGlassDrawable", renderer)
        self.assertIn("new AppGlassDrawable(", style)

        # Consumers import what they use, and nothing they do not: GlassPreset reads
        # settings but no theme state, so an AppTheme import there would be dead.
        # This is the blanket-substitution bug that hit GelMotion in step 1c-2a.
        self.assertNotIn("AppTheme", java_code_only(presets))
        self.assertIn("import app.space.core.data.AppPrefs;", presets)
        # Step 1h-ii changed two of these: MainActivity now reaches AppSurfaceStyle only
        # through UiKit, so its direct import is gone and asserting it would demand a dead
        # import. AlarmActivity gained Palette for the unified blend.
        for name, wanted in (("AlarmActivity.java", ("AppSurfaceStyle", "Palette")),
                             ("MainActivity.java", ("GlassPreset", "Palette", "UiKit")),
                             ("WidgetCatalogActivity.java", ("AppSurfaceStyle",)),
                             ("WidgetConfigActivity.java", ("GlassPreset",)),
                             ("WidgetEngine.java", ("GlassPreset",))):
            src = read(name)
            for cls in wanted:
                self.assertIn(f"import app.space.core.design.{cls};", src, f"{name} needs {cls}")
            self.assertNotIn("import app.space.core.design.AppGlassDrawable;", src,
                             f"{name} must not construct the renderer directly")

    def test_glass_opacity_floor_survives_the_move(self):
        """The 82% floor is a documented legibility guarantee in README.md and
        PRODUCT.md ("critical actions remain solid", "82% opacity floor keeps glass
        behind text"). It lives in the renderer, so it moved with it — assert both the
        floor and the higher 88% critical-control floor are still enforced."""
        renderer = read_core_design("AppGlassDrawable.java")
        style = read_core_design("AppSurfaceStyle.java")
        self.assertIn("Math.max(82,opacityFloor)", renderer)
        self.assertIn("clamp(Math.max(82,opacityFloor),100,", renderer)
        self.assertIn(",88)", style)

    def test_suite_contract_module_holds_interfaces_only(self):
        """SUITE-PLAN.md §3.2. :core-suite is the contract between an app and the chrome.

        It must stay free of implementation. If it grew preferences or rendering code,
        every feature module would pull in behaviour it does not use, and the shell could
        no longer be rewritten independently — which matters because navigation is
        user-customisable, so it will be rewritten.
        """
        sources = sorted(CORE_SUITE.rglob("*.java"))
        names = {p.stem for p in sources}
        self.assertEqual({"SuiteApp", "Screen", "ScreenHost", "SettingsGroup"}, names)
        for path in sources:
            text = path.read_text(encoding="utf-8")
            code = java_code_only(text)
            self.assertIn(f"public interface {path.stem}", code)
            # No persistence and no preference access in a contract module.
            self.assertNotIn("SharedPreferences", code, f"{path.name} touches preferences")
            self.assertNotIn("AppPrefs", code, f"{path.name} touches the data layer")
            self.assertNotIn("import app.tictic.clock.", code, f"{path.name} imports :app")
        self.assertIn("include ':core-suite'", (ROOT / "settings.gradle").read_text())

    def test_screen_host_encodes_the_no_app_may_assume_another_is_installed_rule(self):
        """SUITE-PLAN.md §3.4: an app can be hidden (L1), excluded by a lite flavour (L2)
        or not yet downloaded (L3). The contract therefore reports absence rather than
        throwing, and any cross-app affordance must handle the negative case.

        Asserted as signatures, because a later widening to void or to a non-null return
        would quietly remove the ability to handle a missing app.
        """
        host = java_code_only((CORE_SUITE / "ScreenHost.java").read_text(encoding="utf-8"))
        for signature in ("boolean isAppAvailable(String id)",
                          "List<SuiteApp> availableApps()",
                          "boolean openApp(String id)",
                          "boolean openApp(String id, String route)",
                          "Palette palette()",
                          "Context context()",
                          "void invalidate()",
                          "void openSettings()",
                          "void openSettings(String groupId)"):
            self.assertIn(signature, host)
        # Palette is handed out rather than recomputed, so one theme edit cannot leave a
        # single app looking wrong.
        self.assertIn("import app.space.core.design.Palette;",
                      (CORE_SUITE / "ScreenHost.java").read_text(encoding="utf-8"))

    def test_suite_app_extends_the_pure_ordering_entry(self):
        """SuiteApp is-a NavOrder.Entry so :shell can pass registered apps straight into
        the pure, unit-tested ordering rules without an adapter. @Override is required on
        the three shared members so the compiler catches signature drift instead of
        silently declaring unrelated methods."""
        app_src = (CORE_SUITE / "SuiteApp.java").read_text(encoding="utf-8")
        code = java_code_only(app_src)
        self.assertIn("public interface SuiteApp extends NavOrder.Entry", code)
        for member in ("String id();", "String label();", "int defaultOrder();"):
            self.assertIn("@Override\n    " + member, code, f"{member} must override Entry")
        for member in ("String tagline();", "int iconGlyph();", "boolean supportsWidgets();",
                       "boolean acceptsRoute(String route);",
                       "List<SettingsGroup> settingsGroups();",
                       "Screen createScreen(ScreenHost host, String route);"):
            self.assertIn(member, code)
        # Both types appear in public signatures, so both must be api, not implementation.
        gradle = (ROOT / "core-suite/build.gradle").read_text(encoding="utf-8")
        self.assertIn("api project(':core-design')", gradle)
        self.assertIn("api project(':core-domain')", gradle)
        self.assertNotIn("implementation project(':core-design')", gradle)

    def test_shell_implements_the_contract_and_nothing_depends_on_it_except_app(self):
        """The separation that lets navigation change without recompiling any app:
        features implement :core-suite, the shell implements :core-suite, and neither
        depends on the other. Only :app, which assembles the registry, may see :shell.
        """
        settings = (ROOT / "settings.gradle").read_text()
        self.assertIn("include ':shell'", settings)
        shell_gradle = (ROOT / "shell/build.gradle").read_text(encoding="utf-8")
        self.assertIn("api project(':core-suite')", shell_gradle)
        self.assertIn("com.android.library", shell_gradle)

        # The contract must not know about its implementor.
        suite_gradle = (ROOT / "core-suite/build.gradle").read_text(encoding="utf-8")
        self.assertNotIn("project(':shell')", suite_gradle)

        # Only :app may depend on the shell. Features depending on it would couple every
        # app to the navigation implementation.
        for gradle_file in sorted(ROOT.glob("*/build.gradle")):
            module = gradle_file.parent.name
            if module == "app":
                continue
            code = [ln.strip() for ln in gradle_file.read_text(encoding="utf-8").splitlines()
                    if not ln.strip().startswith("//")]
            for line in code:
                self.assertNotIn("project(':shell')", line,
                                 f"{module} must not depend on :shell")

    def test_registry_validates_ids_at_construction(self):
        """A comma or duplicate id would corrupt the comma-joined order and hidden lists,
        and would surface much later as the wrong app opening. Validating at construction
        turns that into a startup failure naming the offending app."""
        registry = java_code_only((SHELL / "SuiteRegistry.java").read_text(encoding="utf-8"))
        self.assertIn("public final class SuiteRegistry", registry)
        for guard in ('"SuiteRegistry requires a non-null app list"',
                      '"SuiteRegistry contains a null SuiteApp"',
                      "has an empty id",
                      "must not contain a comma",
                      "Duplicate SuiteApp id"):
            self.assertIn(guard, registry)
        # Route resolution asks the apps, so the shell never learns an app's id or its
        # destination names. Without this, existing widget deep links would need the
        # clock hardcoded into the chrome.
        self.assertIn("acceptsRoute(route)", registry)
        self.assertIn("public SuiteApp resolveRoute(String route)", registry)

    def test_shell_hardcodes_no_app_identity(self):
        """The chrome must be app-agnostic. Any literal app id in :shell is a coupling
        that will have to be edited every time an app is added or renamed — and there is
        no local compiler to catch what that breaks."""
        for path in sorted(SHELL.rglob("*.java")):
            code = java_code_only(path.read_text(encoding="utf-8"))
            for forbidden in ('"clock"', '"calculator"', '"alarms"', '"usage"', '"tictic"'):
                self.assertNotIn(forbidden, code, f"{path.name} hardcodes {forbidden}")

    def test_nav_prefs_holds_no_ordering_logic_of_its_own(self):
        """The ordering rules live in :core-domain where they are pure JVM and unit
        tested. There is no emulator here, so logic left inside an Android module would
        be logic nothing can test — and a subtle ordering bug presents as a new app that
        never appears, not as a crash."""
        nav = java_code_only((SHELL / "NavPrefs.java").read_text(encoding="utf-8"))
        self.assertIn("NavOrder.resolve(stored, registry.apps())", nav)
        self.assertIn("NavOrder.parse(", nav)
        self.assertIn("NavOrder.join(", nav)
        self.assertNotIn("Collections.sort", nav)
        self.assertNotIn("defaultOrder()", nav)
        # Reads must not write: persisting on read would hit disk on every navigation
        # render and would destroy the property that an absent app returns to its
        # original position.
        for reader in ("orderedApps", "visibleApps", "hidden", "mode", "lastApp"):
            self.assertIn(reader, nav)

    def test_nav_mode_persists_explicit_codes_not_ordinals(self):
        """Three customisable modes (SUITE-PLAN.md §3.3). Ordinals must not be persisted:
        reordering the constants in a later release would silently change every user's
        saved choice."""
        mode_src = (SHELL / "NavMode.java").read_text(encoding="utf-8")
        code = java_code_only(mode_src)
        self.assertIn("public enum NavMode", code)
        for name in ("HUB(0)", "RAIL(1)", "DRAWER(2)"):
            self.assertIn(name, code)
        self.assertIn("public int code()", code)
        self.assertIn("public static NavMode fromCode(int code)", code)
        self.assertIn("public static NavMode defaultValue()", code)
        self.assertNotIn("ordinal()", code)
        # The default must be the rail: extracting the clock into the shell should not
        # change how the existing app looks.
        self.assertIn("return RAIL;", code)

    def test_palette_is_the_single_source_of_the_six_theme_colours(self):
        """Palette centralises colours that were computed inline in MainActivity.

        Step 1h-i completed the move: applyPalette() now resolves a Palette and copies
        its fields, so the ten literals live only in Palette. Before that they existed in
        both places, which is how a theme edit silently reaches half the app.

        Two of the literals still appear elsewhere in MainActivity, and that is expected
        — they are dialog field fills of the form `dark ? 0xff1b2926 : <light value>`,
        where only the dark branch coincides with the palette surface. The assertion is
        therefore scoped to the applyPalette line rather than the whole file.
        """
        palette = java_code_only((ROOT / "core-design/src/main/java/app/space/core/design"
                                  / "Palette.java").read_text(encoding="utf-8"))
        main = read("MainActivity.java")
        values = ("0xff0c1514", "0xffedf2ef", "0xff1b2926", "0xfffcfdfc",
                  "0xfff1f5f3", "0xff18312f", "0xffaabbb5", "0xff5d7069",
                  "0xff354a44", "0xffd5e0db")
        for value in values:
            self.assertIn(value, palette)
        for field in ("public final int accent", "public final int background",
                      "public final int surface", "public final int text",
                      "public final int muted", "public final int border",
                      "public final boolean dark"):
            self.assertIn(field, palette)

        # MainActivity resolves rather than recomputes.
        self.assertIn("Palette.resolve(DataStore.dark(this),DataStore.accent(this))", main)
        self.assertIn("accent=p.accent;bg=p.background;surface=p.surface;"
                      "text=p.text;muted=p.muted;border=p.border", main)
        apply_line = next(l for l in main.split("\n") if "private void applyPalette()" in l)
        for value in values:
            self.assertNotIn(value, apply_line,
                             f"applyPalette still hardcodes {value}; it must come from Palette")

    def test_drawing_primitives_live_in_uikit_and_main_activity_delegates(self):
        """SUITE-PLAN.md §6.1 step 1h-i: the prerequisite for moving any screen.

        Every one of the clock's six tabs builds content from dp/vbox/hbox/lp/label/
        shape/typeface, and all were private to one Activity — so no screen could move
        into a :feature-* module until they had a shared home. MainActivity keeps
        one-line delegates so its hundreds of call sites are untouched.
        """
        kit = java_code_only((ROOT / "core-design/src/main/java/app/space/core/design"
                              / "UiKit.java").read_text(encoding="utf-8"))
        main = read("MainActivity.java")
        # Signatures are compared whitespace-normalised: these assert the shape of an
        # API, not its formatting, and a reformatted file must not read as a break.
        compact = "".join(kit.split())
        self.assertIn("publicfinalclassUiKit", compact)
        for member in ("public int dp(float x)", "public LinearLayout vbox()",
                       "public LinearLayout hbox()",
                       "public LinearLayout.LayoutParams lp(int w,int h)",
                       "public Typeface typeface()",
                       "public TextView label(String s,float size,int color,boolean bold)",
                       "public Drawable shape(int color,int radius)",
                       "public Context context()", "public Palette palette()"):
            self.assertIn("".join(member.split()), compact, f"missing from UiKit: {member}")
        # A UiKit without a palette could mix stale colours with fresh ones.
        self.assertIn('"UiKit requires a Palette"', kit)
        self.assertIn("import app.space.core.design.UiKit;", main)
        self.assertIn("import app.space.core.design.Palette;", main)
        for delegate in ("private int dp(float x){return ui().dp(x);}",
                         "private LinearLayout vbox(){return ui().vbox();}",
                         "private LinearLayout hbox(){return ui().hbox();}",
                         "private LinearLayout.LayoutParams lp(int w,int h){return ui().lp(w,h);}",
                         "private TextView label(String s,float size,int color,boolean bold)"
                         "{return ui().label(s,size,color,bold);}",
                         "private Typeface appTypeface(){return ui().typeface();}",
                         "private android.graphics.drawable.Drawable shape(int color,int radius)"
                         "{return ui().shape(color,radius);}"):
            self.assertIn(delegate, main)
        # Lazy accessor: dp() is reachable from a dialog before applyPalette() has run.
        self.assertIn("private UiKit ui(){if(ui==null)", main)

        # Step 1h-ii: the four helpers that were still entangled with the Activity, plus
        # the glass and elevation support they need. sectionTitle/gap now TAKE a parent —
        # the versions in MainActivity wrote into its own `body` field, which is exactly
        # what stopped a screen from being lifted out of it.
        for member in ("public TextView pillButton(String title,int color,int ink)",
                       "public LinearLayout card()",
                       "public void sectionTitle(LinearLayout parent,String title,String caption)",
                       "public void gap(LinearLayout parent,int h)",
                       "public boolean glassEnabled()",
                       "public int cardFillForElevation(int level)",
                       "public Drawable glassSurfaceDrawable(int base,int radius,int opacity,int finish,int depth)",
                       "public Drawable glassSurfaceDrawable(int base,int radius,int opacity,int finish,int depth,int preset)",
                       "public Drawable pageBackground()",
                       "public Switch appSwitch()"):
            self.assertIn("".join(member.split()), compact, f"missing from UiKit: {member}")
        for delegate in ("private LinearLayout card(){return ui().card();}",
                         "private void sectionTitle(String title,String caption)"
                         "{ui().sectionTitle(body,title,caption);}",
                         "private void gap(int h){ui().gap(body,h);}",
                         "private TextView pillButton(String title,int color,int ink)"
                         "{return ui().pillButton(title,color,ink);}",
                         "private boolean glassEnabled(){return ui().glassEnabled();}",
                         "private int cardFillForElevation(int level)"
                         "{return ui().cardFillForElevation(level);}",
                         "private android.graphics.drawable.Drawable pageBackground()"
                         "{return ui().pageBackground();}",
                         "private Switch appSwitch(){return ui().appSwitch();}"):
            self.assertIn(delegate, main, f"MainActivity must delegate: {delegate}")
        # appSwitch tints with three palette colours; a stock Switch is grey in both states
        # and disappears against a dark glass card, so every toggle goes through this.
        self.assertIn("palette.accent", kit)
        self.assertIn("palette.muted", kit)
        self.assertIn("palette.border", kit)
        # appSwitch's distinctive two-state tint must not survive in the Activity. The
        # plain setThumbTintList calls that remain there belong to sliders and the alarm
        # and reminder editors, which tint a single colour with ColorStateList.valueOf —
        # a different control, not a copy of this one, so banning the method name outright
        # would be wrong.
        # Both markers of the two-state tint are verified absent from MainActivity, while
        # setThumbTintList/setTrackTintList legitimately remain there for sliders and the
        # alarm and reminder editors, which tint a single colour via ColorStateList.valueOf.
        self.assertNotIn("int[][] states", main)
        self.assertNotIn("android.R.attr.state_checked", main)
        self.assertIn("android.R.attr.state_checked", kit)
        # Both glass overloads delegate, and the Activity no longer builds glass itself.
        self.assertEqual(2, main.count("{return ui().glassSurfaceDrawable("),
                         "both glassSurfaceDrawable overloads must delegate")
        self.assertNotIn("AppSurfaceStyle.", main)
        # card() read five preferences through the Activity; UiKit reads them itself.
        for pref in ('"app_layout_density"', '"app_card_elevation"', '"app_card_border"',
                     '"app_glass_opacity"', '"app_glass_finish"'):
            self.assertIn(pref, kit)
        # The elevation curve and the density paddings are design decisions, not Activity
        # state: they must not survive in MainActivity.
        self.assertNotIn("9f+glassDepth", main)
        self.assertNotIn("density==0?13:density==2?22:18", main)

    def test_corner_style_factors_are_defined_exactly_once(self):
        """MainActivity.shape() and AppGlassDrawable.cornerScale() each carried their own
        copy of the four corner factors. Two copies of one design decision is how a solid
        card and a glass panel end up with visibly different corners after someone tunes
        one of them, so both now read UiKit.cornerFactor."""
        kit = (ROOT / "core-design/src/main/java/app/space/core/design" / "UiKit.java").read_text(encoding="utf-8")
        renderer = read_core_design("AppGlassDrawable.java")
        self.assertIn("public static float cornerFactor(int style)", kit)
        self.assertIn("public static int cornerStyle(Context c)", kit)
        self.assertIn("UiKit.cornerFactor(UiKit.cornerStyle(context))", renderer)
        self.assertNotIn("cornerScale", renderer)
        # Repo-wide: the factor table must exist in one file only.
        holders = []
        for path in ROOT.rglob("*.java"):
            if ".git" in path.parts:
                continue
            if "1.35f" in java_code_only(path.read_text(encoding="utf-8")):
                holders.append(path.name)
        self.assertEqual(["UiKit.java"], holders,
                         f"corner factors duplicated in: {holders}")

    def test_colour_blend_is_defined_exactly_once(self):
        """Three character-identical copies of the same colour blend existed.

        AppSurfaceStyle.mix(), MainActivity.blendColor(), and the arithmetic UiKit needed
        for raised card fills were the same four lines under three names. Found while
        extracting card() in step 1h-ii. Unified into Palette.blend because colour maths
        belongs on the colour class; the other two now call it.
        """
        palette = (ROOT / "core-design/src/main/java/app/space/core/design"
                   / "Palette.java").read_text(encoding="utf-8")
        self.assertIn("public static int blend(int from, int to, float amount)", palette)
        holders = []
        for path in ROOT.rglob("*.java"):
            if ".git" in path.parts:
                continue
            compact = "".join(java_code_only(path.read_text(encoding="utf-8")).split())
            if "Color.rgb(Math.round(Color.red(" in compact:
                holders.append(path.name)
        self.assertEqual(["Palette.java"], holders, f"colour blend duplicated in: {holders}")
        self.assertNotIn("blendColor", read("MainActivity.java"))

    def test_feature_clock_is_the_first_app_module_and_depends_only_downward(self):
        """SUITE-PLAN.md §6.1 step 1d-4. :feature-clock is the first app module and the
        reference shape for every later one, so its dependency direction matters more than
        its contents: a feature that reaches sideways or upward cannot be removed from a
        build, which is the one thing §3.4 promises users they can do."""
        settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
        self.assertIn("include ':feature-clock'", settings)
        app_build = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("implementation project(':feature-clock')", app_build)

        build = (ROOT / "feature-clock/build.gradle").read_text(encoding="utf-8")
        self.assertIn("id 'com.android.library'", build)
        self.assertIn("namespace 'app.space.feature.clock'", build)
        self.assertIn("api project(':core-suite')", build)
        # A feature implements the contract, so :core-suite must be api, not
        # implementation: SuiteApp and Screen appear in its public surface.
        self.assertNotIn("implementation project(':core-suite')", build)
        for forbidden in ("project(':app')", "project(':shell')"):
            self.assertNotIn(forbidden, build, f"a feature module may not depend on {forbidden}")

        # Nor may its code reach back into the legacy package it is being extracted from.
        for name in ("AlarmRules.java", "RingingControls.java"):
            src = read_feature_clock(name)
            self.assertNotIn("app.tictic.clock", src, f"{name} must not import the legacy package")
            self.assertNotIn("DataStore", java_code_only(src), f"{name} must not use DataStore")

    def test_data_store_is_now_pure_delegation_with_no_clock_logic(self):
        """The point of the facade strategy: implementations move, call sites do not.

        DataStore held the clock's recurrence rules, ringing-control settings and two
        migrations. As of 1d-4 it holds no logic at all — every body is a one-line
        delegate — which is what makes it safe for a :feature-* module to exist while
        ~140 call sites in :app still read through this class.
        """
        store = java_code_only((JAVA / "DataStore.java").read_text(encoding="utf-8"))
        rules = read_feature_clock("AlarmRules.java")
        ringing = read_feature_clock("RingingControls.java")

        # The recurrence arithmetic must be gone from DataStore and present in the module.
        self.assertNotIn("Calendar.DAY_OF_YEAR", store)
        self.assertNotIn("86400000L", store)
        self.assertIn("Calendar.DAY_OF_YEAR", rules)
        self.assertIn("86400000L", rules)
        # The weekday initials table is a clock display rule, not shared data.
        self.assertNotIn('"S","M","T","W","T","F","S"', store.replace(" ", ""))
        self.assertIn('"S", "M", "T", "W", "T", "F", "S"', rules)

        # Preference keys owned by the clock live in the clock module only.
        for key in ('"ringing_controls_v111_migrated"', '"alarm_volume_up_action"',
                    '"alarm_volume_down_action"', '"alarm_ringing_actions"',
                    '"reminder_defaults_v192_migrated"', '"reminder_minutes"'):
            self.assertNotIn(key, store, f"{key} must not remain in the DataStore facade")
        for key in ('"ringing_controls_v111_migrated"', '"alarm_volume_up_action"',
                    '"alarm_ringing_actions"'):
            self.assertIn(key, ringing)
        for key in ('"reminder_defaults_v192_migrated"', '"reminder_minutes"'):
            self.assertIn(key, rules)

        # Every remaining DataStore body is a delegate, not an implementation.
        self.assertIn("RingingControls.migrate(c)", store)
        self.assertIn("AlarmRules.nextOccurrence(alarm, from)", store)
        self.assertIn("AlarmRules.daysLabel(mask)", store)
        self.assertIn("AlarmRules.nextAlarmText(c)", store)
        self.assertIn("AlarmRules.migrateReminderDefaults(c)", store)
        self.assertIn("RingingControls.showSnoozeAction(c)", store)

    def test_alarm_recurrence_rule_is_unit_testable_without_json(self):
        """nextOccurrence decides when every alarm in the product fires, and was untested
        because its only entry point took a JSONObject — which android.jar stubs as "not
        mocked", so testing it would have meant putting a real JSON jar on the test
        classpath and hoping it beat android.jar. The pure overload removes that fight."""
        rules = java_code_only(read_feature_clock("AlarmRules.java"))
        compact = "".join(rules.split())
        for sig in ("public static long nextOccurrence(int hour, int minute, int daysMask, "
                    "long savedDateMillis, long from)",
                    "public static long nextOccurrence(JSONObject alarm, long from)"):
            self.assertIn("".join(sig.split()), compact, f"missing overload: {sig}")
        # The JSON form must be a thin extraction, not a second copy of the arithmetic.
        json_body = rules[rules.index("public static long nextOccurrence(JSONObject alarm"):
                          rules.index("public static long nextOccurrence(int hour")]
        self.assertNotIn("Calendar", json_body,
                         "the JSON overload must delegate, not reimplement the rule")

        test = (ROOT / "feature-clock/src/test/java/app/space/feature/clock"
                / "AlarmRulesTest.java").read_text(encoding="utf-8")
        self.assertGreaterEqual(test.count("@Test"), 12)
        # The bug this rule is most likely to grow: a weekly alarm whose time has already
        # passed today must come round in seven days, not fire tomorrow on the wrong day.
        self.assertIn("WaitsAWeekNotADay", test)
        # And it must never schedule into the past.
        self.assertIn("assertTrue(result > from)", test)
        # A test that recomputes the expected value the same way the implementation does
        # would pass even if the rule were wrong, so no org.json on the test classpath.
        # Comment lines are stripped before this check: the build file explains in prose
        # why org.json is deliberately absent, and an assertion that matched its own
        # explanation would fail for the right reason and pass for the wrong one.
        build = (ROOT / "feature-clock/build.gradle").read_text(encoding="utf-8")
        declared = "\n".join(l for l in build.split("\n") if not l.strip().startswith("//"))
        self.assertNotIn("org.json", declared)
        self.assertIn("testImplementation 'junit:junit:4.13.2'", declared)

    def test_clock_app_declares_identity_the_registry_can_sort_and_render(self):
        """SUITE-PLAN.md §6.1 step 1h-iii: the contract's first real implementor.

        Until now SuiteApp had no consumer, so its identity and routing surface had never
        been checked against anything. These assertions are what turn the interface from a
        sketch into a requirement a second app can be written against.
        """
        src = java_code_only(read_feature_clock("ClockApp.java"))
        self.assertIn("public final class ClockApp implements SuiteApp", src)
        # Every SuiteApp member overridden, so the compiler catches signature drift
        # instead of ClockApp quietly declaring unrelated methods.
        self.assertEqual(src.count("@Override"), 9, "ClockApp must override all nine members")
        for member in ('return "clock";', 'return "Clock";', "return TicIconView.CLOCK;",
                       "return 0;", "return true;"):
            self.assertIn(member, src)
        # First in the hub: the clock is what this install started as.
        self.assertIn("public int defaultOrder()", src)
        # A suite app is instantiated by the registry, so it must not need an Activity.
        self.assertNotIn("import android.app.", src)
        self.assertNotIn("import android.content.Context;", src)

    def test_clock_routes_match_the_widget_intents_that_already_send_them(self):
        """The cross-module agreement that keeps 24 placed widgets working.

        The clock's widget providers and notification actions put a `tab` extra on their
        intents, and MainActivity.applyRoute maps those strings to tabs. ClockApp.acceptsRoute
        must accept exactly the same set. A route present in one and missing from the other
        fails silently: the widget opens the app and shows nothing, with no crash to explain
        why. Neither side can be unit-tested against the other, so the parity is asserted
        here. Legacy "log" is included because widgets placed before the rename still send
        it.
        """
        main = java_code_only(read("MainActivity.java"))
        start = main.index("private void applyRoute(Intent intent)")
        body = main[start:main.index("}", main.index("activityHistoryLimit=20", start)) + 1]
        in_activity = set(re.findall(r'"([a-z]+)"\.equals\(target\)', body))

        clock = java_code_only(read_feature_clock("ClockApp.java"))
        routes_decl = clock[clock.index("ROUTES = "):clock.index(";", clock.index("ROUTES = "))]
        in_clock = set(re.findall(r'"([a-z]+)"', routes_decl))

        self.assertEqual(in_activity, in_clock,
                         "ClockApp.ROUTES and MainActivity.applyRoute disagree; a widget deep "
                         f"link would break. only-in-activity={in_activity - in_clock} "
                         f"only-in-clock={in_clock - in_activity}")
        self.assertEqual({"clock", "alarms", "reminders", "timer", "stopwatch", "routines",
                          "usage", "log"}, in_clock)
        # acceptsRoute must be null-safe: the registry asks it about arbitrary strings.
        self.assertIn("route != null && ROUTES.contains(route)", clock)

    def test_clock_app_screen_migration_is_explicitly_incomplete_not_silently_empty(self):
        """createScreen returns null and settingsGroups is empty until step 1h-iv.

        Both are temporary and both are pinned, because the failure mode of an unfinished
        migration is a blank page rather than an error. Null is the convention ScreenHost
        already uses for availability, so the host can keep rendering the tab inline exactly
        as it does today; a placeholder screen would render blank and look like a
        regression. When the screens land, these assertions must be updated deliberately —
        they are the marker that says the migration is not finished.
        """
        src = java_code_only(read_feature_clock("ClockApp.java"))
        sig = "public Screen createScreen(ScreenHost host, String route)"
        self.assertIn("".join(sig.split()), "".join(src.split()))
        body = src[src.index(sig):]
        self.assertIn("return null;", body)
        self.assertIn("return Collections.emptyList();", src)
        # Step 1h-iv·2 cut the stopwatch over, so MainActivity now does reference ClockApp
        # and hosts it. The registry is still unused: nav rendering is step 1g-2.
        main = read("MainActivity.java")
        self.assertIn("ClockApp", main)
        self.assertNotIn("SuiteRegistry", main)

    def test_core_platform_holds_os_state_reads_and_the_boot_count_moved_there(self):
        """SUITE-PLAN.md §6.1 step 1e (first slice).

        The boot count was package-private inside an :app broadcast receiver, which made
        it unreachable from :feature-clock — and the stopwatch needs it, because
        SystemClock.elapsedRealtime() resets on reboot while currentTimeMillis() does not.
        Without it a timer that spans a reboot reports a wildly wrong duration.
        """
        settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
        self.assertIn("include ':core-platform'", settings)
        for consumer in ("app/build.gradle", "feature-clock/build.gradle"):
            self.assertIn("implementation project(':core-platform')",
                          (ROOT / consumer).read_text(encoding="utf-8"), consumer)

        build = (ROOT / "core-platform/build.gradle").read_text(encoding="utf-8")
        self.assertIn("id 'com.android.library'", build)
        self.assertIn("namespace 'app.space.core.platform'", build)
        # SystemBoot needs nothing but framework APIs; PowerState reads the user's
        # adaptive-power-saving preference before consulting the OS, so the module does
        # depend on :core-data. Core -> core is permitted by the 3.1 rule and the
        # constraint that actually matters is asserted below: nothing here may point at
        # :app, :shell or a :feature-*.
        self.assertIn("implementation project(':core-data')", build)
        for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
            self.assertNotIn(forbidden, build, f":core-platform may not depend on {forbidden}")

        boot = java_code_only((CORE_PLATFORM / "SystemBoot.java").read_text(encoding="utf-8"))
        self.assertIn("public final class SystemBoot", boot)
        self.assertIn("public static int count(Context context)".replace(" ", ""),
                      "".join(boot.split()))
        self.assertIn("Settings.Global.BOOT_COUNT", boot)
        # A device that cannot report a boot count must not throw, and must not look as
        # though a reboot happened: -1 compares equal to itself.
        self.assertIn("catch (RuntimeException ignored)", boot)
        # Two early returns of -1 (the SDK guard and the catch), plus -1 as the default
        # when the setting is absent. All three must be the same value: a boot count that
        # cannot be read has to compare equal to itself rather than look like a reboot.
        self.assertEqual(2, boot.count("return -1;"))
        self.assertIn("Settings.Global.BOOT_COUNT,-1", boot.replace(" ", ""))
        self.assertIn("private SystemBoot()", boot)

        # TimerReceiver keeps a delegate so its three internal callers and MainActivity's
        # two are untouched, and no longer reads Settings itself.
        receiver = read("TimerReceiver.java")
        self.assertIn("static int bootCount(Context context) { return SystemBoot.count(context); }",
                      receiver)
        self.assertIn("import app.space.core.platform.SystemBoot;", receiver)
        self.assertNotIn("import android.provider.Settings;", receiver)
        self.assertNotIn("Settings.Global", java_code_only(receiver))
        self.assertEqual(3, java_code_only(receiver).count("bootCount(context)"))

    def test_power_saver_rule_is_defined_exactly_once_across_three_modules(self):
        """The third copy-pasted rule found in four slices.

        MainActivity.batterySaverOptimization(), WidgetEngine.powerSaverOptimization() and
        AnalogClockView.powerSaverOptimization() were logically identical under three names
        in three modules. Drift here is concrete rather than cosmetic: widgets would keep
        animating while the app throttled itself, or the analog clock would freeze while the
        rest of the UI stayed live, and both read as a rendering bug rather than a
        preference disagreement. Unified as PowerState.isOptimizing in :core-platform, which
        is also where :feature-clock can reach it — the stopwatch screen needs it to pick
        its tick rate.
        """
        state = java_code_only((CORE_PLATFORM / "PowerState.java").read_text(encoding="utf-8"))
        self.assertIn("public final class PowerState", state)
        self.assertIn("public static boolean isOptimizing(Context context)".replace(" ", ""),
                      "".join(state.split()))
        # The preference gates the OS read, and defaults to on: a user who turns adaptive
        # throttling off is asking for full fidelity regardless of what Android reports.
        self.assertIn('"adaptive_power_saver"', state)
        self.assertIn("isPowerSaveMode()", state)
        gate = state.index('"adaptive_power_saver"')
        self.assertLess(gate, state.index("isPowerSaveMode()"),
                        "the user preference must be checked before the OS read")

        # The rule must exist in one file only.
        holders = []
        for path in ROOT.rglob("*.java"):
            if ".git" in path.parts:
                continue
            compact = "".join(java_code_only(path.read_text(encoding="utf-8")).split())
            if "isPowerSaveMode()" in compact and "getBoolean(\"adaptive_power_saver\",true)" in compact:
                holders.append(path.name)
        self.assertEqual(["PowerState.java"], holders, f"power-saver rule duplicated in: {holders}")

        # All three former copies are now delegates, so their call sites are untouched.
        self.assertIn("private boolean batterySaverOptimization(){return PowerState.isOptimizing(this);}",
                      read("MainActivity.java"))
        self.assertIn("private static boolean powerSaverOptimization(Context c)"
                      "{return PowerState.isOptimizing(c);}", read("WidgetEngine.java"))
        self.assertIn("private boolean powerSaverOptimization()"
                      "{return PowerState.isOptimizing(getContext());}",
                      read_core_design("AnalogClockView.java"))
        for name in ("MainActivity.java", "WidgetEngine.java"):
            self.assertIn("import app.space.core.platform.PowerState;", read(name), name)
        self.assertIn("import app.space.core.platform.PowerState;",
                      read_core_design("AnalogClockView.java"))

    def test_stopwatch_screen_is_the_first_real_screen_and_keeps_every_pref_key(self):
        """SUITE-PLAN.md §6.1 step 1h-iv: the first Screen implementation.

        The assertion that matters most here is key parity. All stopwatch state lives in
        preferences rather than Activity fields, so a renamed or dropped key does not crash
        — it silently strands whatever the user had running, and a paused stopwatch comes
        back as zero. Nothing else about this move would fail so quietly.
        """
        screen = java_code_only(read_feature_clock("StopwatchScreen.java"))
        main = java_code_only(read("MainActivity.java"))

        self.assertIn("public final class StopwatchScreen implements Screen", screen)
        compact = "".join(screen.split())
        for member in ("public String title()", "public View build(Context buildContext)",
                       "public void onResume()", "public void onPause()",
                       "public StopwatchScreen(ScreenHost host)"):
            self.assertIn("".join(member.split()), compact, f"missing: {member}")

        keys = {"sw_running", "sw_elapsed", "sw_start", "sw_start_wall", "sw_start_elapsed",
                "sw_boot_count", "sw_last_lap", "laps", "stopwatch_show_hundredths",
                "stopwatch_lap_haptics", "stopwatch_button_haptics"}
        for key in keys:
            self.assertIn('"' + key + '"', screen, f"StopwatchScreen lost pref key {key}")
            self.assertIn('"' + key + '"', main, f"MainActivity no longer holds {key} to compare against")

        # A screen owns its clock: the shared Activity ticker is unreachable from here.
        self.assertIn("new Handler(Looper.getMainLooper())", screen)
        self.assertIn("handler.post(ticker)", screen)
        self.assertIn("handler.removeCallbacks(ticker)", screen)
        # And it must reproduce nextTickDelay()'s four rates for this tab exactly, or the
        # hundredths display stutters or the battery drains. 60 s while paused under
        # battery saver is the easy one to lose, because the original checks power-saver
        # state before the paused-tab branch.
        for rate in ("16L", "100L", "1000L", "60000L"):
            self.assertIn(rate, screen, f"tick rate {rate} missing")
        self.assertIn("PowerState.isOptimizing(context)", screen)

        # Re-render goes through the host, never by reaching back into the Activity.
        self.assertIn("host.invalidate()", screen)
        self.assertNotIn("render()", screen)
        self.assertNotIn("DataStore", screen)
        self.assertNotIn("app.tictic.clock", screen)
        # Reachable modules only: boot count and power state came from :core-platform,
        # which is why 1e had to precede this screen.
        self.assertIn("SystemBoot.count(context)", screen)
        self.assertIn("TimeMath.stopwatchElapsed", screen)
        self.assertIn("TimeMath.formatHundredths", screen)
        # Colours from the host palette, not from Activity fields.
        self.assertIn("palette.accent", screen)
        self.assertIn("palette.surface", screen)
        self.assertIn("palette.dark ? 0xff293b37 : 0xffedf3f1", screen)
        # A lap records its split, not just its absolute time.
        self.assertIn('.put("split", e - prev)', screen)

    def test_only_the_stopwatch_route_is_migrated_and_the_rest_stay_inline(self):
        """The migration marker, updated deliberately rather than inherited.

        createScreen returns a real screen for stopwatch and null for the other five, which
        is what lets the host keep rendering them inline. Nothing is wired into MainActivity
        yet, so behaviour cannot have changed; that cutover is the next slice and it carries
        five existing assertions that point at MainActivity's copies of this logic.
        """
        clock = java_code_only(read_feature_clock("ClockApp.java"))
        self.assertIn('if ("stopwatch".equals(route)) return new StopwatchScreen(host);', clock)
        body = clock[clock.index("public Screen createScreen(ScreenHost host, String route)"):]
        self.assertIn("return null;", body)
        main = read("MainActivity.java")
        compact = "".join(java_code_only(main).split())
        # Step 1h-iv·2: the cutover. MainActivity is now the ScreenHost and tab 3 renders
        # through the screen instead of building its own view tree.
        self.assertIn("public class MainActivity extends Activity implements ScreenHost", main)
        self.assertIn("private final SuiteApp clockApp=new ClockApp();", main)
        self.assertIn("stopwatchScreen=new StopwatchScreen(this)", main)
        self.assertIn("body.addView(stopwatchScreen.build(this),lp(-1,-2))", main)
        # All nine ScreenHost members, or the class would not compile.
        for member in ("public Context context()", "public Palette palette()",
                       "public void invalidate()", "public boolean isAppAvailable(String id)",
                       "public List<SuiteApp> availableApps()", "public boolean openApp(String id)",
                       "public boolean openApp(String id,String route)", "public void openSettings()",
                       "public void openSettings(String groupId)"):
            self.assertIn("".join(member.split()), compact, f"missing ScreenHost member: {member}")
        # openApp routes through applyRoute rather than duplicating the mapping, so widget
        # intents and suite navigation cannot disagree about what a route means.
        self.assertIn('i.putExtra("tab",route)', main)
        # The screen's ticker is stopped when its tab is left and when the Activity pauses,
        # or it would keep redrawing a detached view.
        self.assertIn("if((settings||tab!=3)&&stopwatchScreen!=null){stopwatchScreen.onPause();"
                      "stopwatchScreen=null;}", main)
        self.assertIn("if(stopwatchScreen!=null)stopwatchScreen.onPause();", main)
        self.assertIn("if(stopwatchScreen!=null)stopwatchScreen.onResume();", main)
        # The dead duplicate is gone: two copies of one screen is how a fix reaches one of
        # them and not the other.
        self.assertNotIn("buildStopwatchLegacy", main)
        self.assertNotIn("SuiteRegistry", main)

    def test_java_code_only_strips_comments_but_preserves_string_literals(self):
        """The coupling contracts depend on this helper being correct: if it stripped
        real code it would hide a genuine violation, and if it kept comments the module
        javadoc would keep tripping it."""
        source = (
            'package p;\n'
            '// import android.widget.Toast;\n'
            '/* DataStore line\n   still block */\n'
            'class A {\n'
            '  String url = "https://example.com/a";\n'
            '  String glob = "/* not a comment */";\n'
            '  char slash = \'/\';\n'
            '  String esc = "a \\" /* b";\n'
            '  void f() { g(); } // trailing\n'
            '}\n'
        )
        code = java_code_only(source)
        self.assertNotIn("import android.widget.Toast", code)
        self.assertNotIn("DataStore", code)
        self.assertNotIn("trailing", code)
        # String and char literals survive intact, including "//" and "/*" inside them.
        self.assertIn('"https://example.com/a"', code)
        self.assertIn('"/* not a comment */"', code)
        self.assertIn("g()", code)
        self.assertIn("class A", code)

    def test_core_domain_is_pure_jvm(self):
        """A core-domain Android import would break JVM-only verification, which is
        the only fast feedback loop available (there is no local Android build)."""
        sources = sorted(CORE_DOMAIN.rglob("*.java"))
        self.assertTrue(sources, "core-domain has no sources")
        for path in sources:
            code = java_code_only(path.read_text(encoding="utf-8"))
            self.assertNotIn("import android.", code, f"{path.name} imports Android")
            self.assertNotIn("import androidx.", code, f"{path.name} imports AndroidX")

    def test_extracted_classes_are_public_and_namespaced(self):
        math = read_core_domain("TimeMath.java")
        self.assertIn("package app.space.core.domain;", math)
        self.assertIn("public final class TimeMath", math)
        for member in ("countdownRemaining", "stopwatchElapsed",
                       "formatHundredths", "formatCountdownHundredths"):
            self.assertIn(f"public static", math)
            self.assertIn(member, math)

    def test_consumers_import_time_math_across_the_module_boundary(self):
        """TimeMath left package app.tictic.clock, so same-package access no longer
        applies. Without these imports the build breaks — assert them so a future
        move back into :app is a deliberate act, not an accident."""
        for name in ("MainActivity.java", "TimerReceiver.java"):
            self.assertIn("import app.space.core.domain.TimeMath;", read(name), name)
        self.assertFalse((JAVA / "TimeMath.java").exists(),
                         "TimeMath must not be duplicated back into :app")

    def test_feature_modules_never_depend_on_each_other(self):
        """SUITE-PLAN.md §3.1 dependency rule. Scans every module build file."""
        offenders = []
        for gradle_file in sorted(ROOT.glob("*/build.gradle")):
            module = gradle_file.parent.name
            if not module.startswith("feature-"):
                continue
            for line in gradle_file.read_text(encoding="utf-8").splitlines():
                stripped = line.strip()
                if stripped.startswith("//"):
                    continue
                if "project(':feature-" in stripped:
                    offenders.append(f"{module}: {stripped}")
        self.assertEqual([], offenders, "cross-feature dependencies found")

    def test_core_design_is_an_android_library_with_no_upward_dependency(self):
        """SUITE-PLAN.md §3.1: :core-* must never depend on :app, :shell or :feature-*.

        :core-design is the module most at risk of this, because most design classes
        read theme/accent values through DataStore, which still lives in :app. Only
        genuinely decoupled classes may move until :core-data exists.
        """
        gradle_file = ROOT / "core-design/build.gradle"
        self.assertTrue(gradle_file.exists(), "core-design/build.gradle missing")
        settings = (ROOT / "settings.gradle").read_text()
        self.assertIn("include ':core-design'", settings)
        self.assertIn("implementation project(':core-design')",
                      (ROOT / "app/build.gradle").read_text())
        # Comment lines are excluded so explanatory prose cannot satisfy the check.
        code = [ln.strip() for ln in gradle_file.read_text(encoding="utf-8").splitlines()
                if not ln.strip().startswith("//")]
        for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
            for line in code:
                self.assertNotIn(forbidden, line, f"core-design depends upward: {line}")
        # Checked against the whole file: `plugins { id '...' }` is a single line,
        # so a list-of-lines membership test would never match.
        self.assertIn("com.android.library", gradle_file.read_text(encoding="utf-8"))

    def test_core_design_uses_core_data_and_never_the_legacy_store(self):
        """The rule changed as the extraction progressed, so the assertion did too.

        Originally :core-design was required to be entirely decoupled, because
        :core-data did not exist yet and any preference read would have had to reach
        back into :app. Now that :core-data holds the appearance and settings state,
        depending on it is correct and expected — what must never happen is the design
        system reaching past it into the legacy :app store or package.

        Note this checks code only (see java_code_only), so module javadoc is free to
        name the legacy class while explaining why it is forbidden. An earlier version
        of this suite failed on exactly that.
        """
        sources = sorted((ROOT / "core-design/src/main/java").rglob("*.java"))
        self.assertTrue(sources, "core-design has no sources")
        uses_core_data = False
        for path in sources:
            code = java_code_only(path.read_text(encoding="utf-8"))
            self.assertNotIn("DataStore", code,
                             f"{path.name} uses the legacy :app store; read via :core-data instead")
            self.assertNotIn("import app.tictic.clock.", code,
                             f"{path.name} imports the legacy :app package")
            if "import app.space.core.data." in code:
                uses_core_data = True
        # GelMotion and AnalogClockView read settings and appearance state, so the
        # dependency must be declared or the build breaks.
        self.assertTrue(uses_core_data, "expected :core-design to consume :core-data")
        self.assertIn("project(':core-data')",
                      (ROOT / "core-design/build.gradle").read_text(encoding="utf-8"))

    def test_design_system_classes_moved_into_core_design(self):
        """Step 1c-2: GelMotion and AnalogClockView. Both became reachable across a
        module boundary, so their public surface is pinned — a later refactor that
        narrows it would break every consuming screen at compile time, and there is
        no local compiler to catch that before CI."""
        motion = read_core_design("GelMotion.java")
        analog = read_core_design("AnalogClockView.java")
        self.assertIn("package app.space.core.design;", motion)
        self.assertIn("public final class GelMotion", motion)
        self.assertIn("public static void apply(View root,int accent)", motion)
        self.assertIn("package app.space.core.design;", analog)
        self.assertIn("public final class AnalogClockView extends View", analog)
        self.assertIn("public AnalogClockView(Context c,AttributeSet a)", analog)
        for name in ("GelMotion.java", "AnalogClockView.java"):
            self.assertFalse((JAVA / name).exists(), f"{name} must not remain in :app")
        # The four screens that apply press styles must import across the boundary.
        for name in ("MainActivity.java", "AlarmActivity.java",
                     "WidgetCatalogActivity.java", "WidgetConfigActivity.java"):
            self.assertIn("import app.space.core.design.GelMotion;", read(name), name)
        self.assertIn("import app.space.core.design.AnalogClockView;", read("MainActivity.java"))
        # Appearance/settings reads went to :core-data, not to a duplicated copy.
        # Asserted per file because the two classes need different parts of it:
        # GelMotion reads only settings, AnalogClockView also reads theme state.
        self.assertIn("AppPrefs.prefs", motion)
        self.assertNotIn("AppTheme", java_code_only(motion),
                         "GelMotion reads no theme state; an AppTheme import here would be dead")
        self.assertIn("AppPrefs.prefs", analog)
        self.assertIn("AppTheme.dark", analog)
        self.assertIn("AppTheme.accent", analog)

    def test_tic_icon_view_is_public_and_namespaced(self):
        view = (CORE_DESIGN / "TicIconView.java").read_text(encoding="utf-8")
        self.assertIn("package app.space.core.design;", view)
        self.assertIn("public final class TicIconView extends View", view)
        # The glyph set must stay public: consumers now reach it across a module
        # boundary, and every suite app will add its icon here.
        for glyph in ("CLOCK", "TIMER", "STOPWATCH", "ROUTINE", "ACTIVITY",
                      "SETTINGS", "ALARM", "WORLD", "REMINDER"):
            self.assertIn(glyph, view)

    def test_consumers_import_tic_icon_view_across_the_module_boundary(self):
        main = read("MainActivity.java")
        self.assertIn("import app.space.core.design.TicIconView;", main)
        self.assertFalse((JAVA / "TicIconView.java").exists(),
                         "TicIconView must not be duplicated back into :app")

    def test_core_data_is_registered_with_no_upward_dependency(self):
        gradle_file = ROOT / "core-data/build.gradle"
        self.assertTrue(gradle_file.exists(), "core-data/build.gradle missing")
        text = gradle_file.read_text(encoding="utf-8")
        self.assertIn("include ':core-data'", (ROOT / "settings.gradle").read_text())
        self.assertIn("project(':core-data')", (ROOT / "app/build.gradle").read_text())
        self.assertIn("com.android.library", text)
        for line in text.splitlines():
            stripped = line.strip()
            if stripped.startswith("//"):
                continue
            for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
                self.assertNotIn(forbidden, stripped, f"core-data depends upward: {stripped}")

    def test_preference_file_name_is_identical_in_both_modules(self):
        """The single most dangerous constant in the 1d extraction.

        AppTheme reads SharedPreferences directly, so it carries its own copy of the
        file name. If the two literals ever diverge, theme and accent settings would
        be read from a different file than the one DataStore writes — silently
        stranding every user's appearance settings on upgrade.
        """
        import re
        literal = re.compile(r'PREF\s*=\s*"([^"]+)"')
        reference = re.compile(r'PREF\s*=\s*AppPrefs\.PREF')

        prefs_src = (CORE_DATA / "AppPrefs.java").read_text(encoding="utf-8")
        theme_src = (CORE_DATA / "AppTheme.java").read_text(encoding="utf-8")
        datastore = read("DataStore.java")

        # Exactly one definition, in AppPrefs.
        found = literal.search(prefs_src)
        self.assertIsNotNone(found, "AppPrefs.PREF literal not found")
        self.assertEqual("tictic_local_v1", found.group(1),
                         "changing the preference file name strands existing user data")
        self.assertEqual(1, len(literal.findall(prefs_src + theme_src + datastore)),
                         "the file name must be defined once, in AppPrefs")

        # Everyone else references it, so the copies cannot drift apart.
        self.assertIsNotNone(reference.search(theme_src), "AppTheme must reference AppPrefs.PREF")
        self.assertIsNotNone(reference.search(datastore), "DataStore must reference AppPrefs.PREF")

    def test_theme_primitives_moved_to_core_data_behind_a_facade(self):
        """SUITE-PLAN.md §6.1 step 1d.

        The implementation must live in AppTheme, DataStore must delegate rather than
        keep a second copy, and no call site may change — there are 452 DataStore
        references across 25 files, so the facade is what keeps this slice small
        enough to verify in one CI run.
        """
        theme = (CORE_DATA / "AppTheme.java").read_text(encoding="utf-8")
        datastore = read("DataStore.java")
        self.assertIn("package app.space.core.data;", theme)
        self.assertIn("public final class AppTheme", theme)
        for member in ("public static int themeMode(", "public static boolean dark(",
                       "public static int accent(", "public static final int[] ACCENT_COLORS"):
            self.assertIn(member, theme)
        # Delegation, not duplication: DataStore must not reimplement these.
        self.assertIn("import app.space.core.data.AppTheme;", datastore)
        self.assertIn("return AppTheme.themeMode(c);", datastore)
        self.assertIn("return AppTheme.dark(c);", datastore)
        self.assertIn("return AppTheme.accent(c);", datastore)
        self.assertIn("AppTheme.ACCENT_COLORS", datastore)
        self.assertNotIn("uiMode & 0x30", datastore)
        self.assertNotIn("0xff3f7c73", datastore)
        # The palette itself, so a future edit cannot quietly drop accents.
        self.assertEqual(16, theme.count("0xff"), "expected 16 accent colours")

    def test_core_data_does_not_reach_back_into_app(self):
        sources = sorted(CORE_DATA.rglob("*.java"))
        self.assertTrue(sources, "core-data has no sources")
        for path in sources:
            code = java_code_only(path.read_text(encoding="utf-8"))
            self.assertNotIn("import app.tictic.clock.", code,
                             f"{path.name} imports the legacy :app package")
            self.assertNotIn("DataStore", code,
                             f"{path.name} references DataStore; core-data must not depend on :app")

    def test_generic_storage_moved_to_core_data_behind_the_facade(self):
        """SUITE-PLAN.md §6.1 step 1d-2: prefs/array/putArray/find.

        These four account for 242 of DataStore's 452 references, so the facade is what
        keeps the slice small — no call site changes, and the existing assertions on
        DataStore.xxx(...) strings in other files stay valid.
        """
        prefs_src = (CORE_DATA / "AppPrefs.java").read_text(encoding="utf-8")
        datastore = read("DataStore.java")
        self.assertIn("package app.space.core.data;", prefs_src)
        self.assertIn("public final class AppPrefs", prefs_src)
        for member in ("public static SharedPreferences prefs(", "public static JSONArray array(",
                       "public static void putArray(", "public static JSONObject find("):
            self.assertIn(member, prefs_src)
        self.assertIn("return AppPrefs.prefs(c);", datastore)
        self.assertIn("return AppPrefs.array(c, key);", datastore)
        self.assertIn("AppPrefs.putArray(c, key, a);", datastore)
        self.assertIn("return AppPrefs.find(c, key, id);", datastore)
        # Delegation, not duplication: the implementations must no longer be in DataStore.
        self.assertNotIn('new JSONArray(prefs(c).getString', datastore)
        self.assertNotIn("o.optLong(\"id\")", datastore)

    def test_usage_tracker_moved_whole_to_core_data(self):
        """Moved in the same slice as AppPrefs because the two are mutually coupled:
        AppActivityLog records an event here, and this class reads its enabled flag and
        every counter through preferences. Splitting them would have left a
        cross-module cycle routed through :app.
        """
        tracker = (CORE_DATA / "UsageTracker.java").read_text(encoding="utf-8")
        self.assertFalse((JAVA / "UsageTracker.java").exists(),
                         "UsageTracker must not remain in :app")
        self.assertIn("package app.space.core.data;", tracker)
        self.assertIn("public final class UsageTracker", tracker)
        for member in ("purgeLegacyBatterySnapshots", "addForeground", "recordWidgetRefresh",
                       "recordEvent", "usageMillis", "clockMillis", "widgetRefreshes",
                       "eventCount", "formatDuration", "dayKey"):
            self.assertIn(member, tracker)
        # Reading through AppPrefs is what keeps core-data free of :app.
        self.assertIn("AppPrefs.prefs(c)", tracker)
        self.assertNotIn("DataStore", java_code_only(tracker))
        # Both switch names are user-visible promises in README/PRODUCT.md.
        self.assertIn('"usage_metrics_enabled"', tracker)

    def test_usage_tracker_consumers_import_it_across_the_boundary(self):
        # DataStore was dropped from this list in step 1d-4. It imported UsageTracker
        # without ever calling it, so the assertion was pinning a dead import rather than a
        # real consumer; the facade no longer references the tracker at all.
        for name in ("MainActivity.java", "WidgetEngine.java"):
            self.assertIn("import app.space.core.data.UsageTracker;", read(name), name)
        self.assertNotIn("UsageTracker", read("DataStore.java"))


if __name__ == "__main__":
    unittest.main()


class DialogSystemContracts(unittest.TestCase):
    """The themed alert dialog is design-system code, and 22 call sites depend on it.

    Step 1h-iv.3 moved MainActivity's private inner class ``TicAlertBuilder`` into
    :core-design as ``AppAlertBuilder``, with its two window-fitting helpers following as
    ``DialogFit``. It was the largest single blocker to extracting screens: a private inner
    class of the Activity cannot be reached from a feature module, and four of the five
    remaining clock screens open an editor dialog.
    """

    def test_the_dialog_builder_lives_in_core_design_and_nowhere_else(self):
        builder = (CORE_DESIGN / "AppAlertBuilder.java").read_text()
        self.assertIn("package app.space.core.design;", builder)
        self.assertIn("public final class AppAlertBuilder extends AlertDialog.Builder", builder)
        self.assertIn("public AppAlertBuilder(Context context, UiKit ui)", builder)
        # The old private inner class must not survive anywhere: two themed alert dialogs
        # would drift apart, and only one would keep the close button and rounded panel.
        main = read("MainActivity.java")
        self.assertNotIn("TicAlertBuilder", main)
        self.assertNotIn("class TicAlertBuilder", builder)

    def test_alert_builder_still_themes_from_the_palette_not_hardcoded_colours(self):
        builder = java_code_only((CORE_DESIGN / "AppAlertBuilder.java").read_text())
        # Heading and close glyph must come from the resolved palette. Hardcoding them here
        # would make every dialog ignore an accent or dark-mode change.
        self.assertIn("ui.label(title.toString(), 18, p.text, true)", builder)
        self.assertIn("ui.label(\"\\u00d7\", 27, p.muted, true)", builder)
        self.assertIn("ui.shape(p.surface, 24)", builder)
        self.assertIn("setStroke(ui.dp(1), p.border)", builder)
        self.assertIn("GelMotion.apply(dialog.getWindow().getDecorView(), ui.palette().accent)", builder)

    def test_dialog_title_row_keeps_its_own_dismiss_control(self):
        builder = (CORE_DESIGN / "AppAlertBuilder.java").read_text()
        # A dialog without this is undismissable on a device with no back key gesture.
        self.assertIn("super.setCustomTitle(row)", builder)
        self.assertIn("if (dialogRef[0] != null) dialogRef[0].dismiss();", builder)
        self.assertIn("close.setFocusable(true)", builder)
        self.assertIn("close.setClickable(true)", builder)

    def test_alert_builder_fits_the_window_exactly_once_on_show(self):
        builder = (CORE_DESIGN / "AppAlertBuilder.java").read_text()
        # The original called fitAlertDialog twice back to back. It is idempotent, so the
        # duplicate was dropped; a third or a missing call would both be regressions.
        self.assertEqual(builder.count("DialogFit.fit(ui, dialog);"), 1)
        self.assertIn("AlertDialog dialog = create();", builder)
        self.assertIn("dialog.show();", builder)

    def test_dialog_fit_clamps_width_and_reserves_room_for_buttons(self):
        fit = java_code_only((CORE_DESIGN / "DialogFit.java").read_text())
        self.assertIn("package app.space.core.design;", (CORE_DESIGN / "DialogFit.java").read_text())
        # 280..560dp wide: without the floor a form overflows a small phone, without the
        # ceiling a dialog stretches across a tablet.
        self.assertIn("Math.min(ui.dp(560)", fit)
        self.assertIn("Math.max(ui.dp(280), width)", fit)
        # The 220dp reserve is what keeps the dialog's own buttons on screen. Losing it
        # means a long routine or alarm form pushes Save off the bottom.
        self.assertIn("heightPixels - ui.dp(220)", fit)
        self.assertIn("Math.max(ui.dp(180)", fit)
        self.assertIn("window.getDecorView().requestLayout();", fit)

    def test_scroll_views_inside_dialogs_are_capped_recursively(self):
        fit = java_code_only((CORE_DESIGN / "DialogFit.java").read_text())
        self.assertIn("if (view instanceof ScrollView)", fit)
        self.assertIn("params.height = params.height > 0 ? Math.min(params.height, maxHeight) : maxHeight", fit)
        # Recursion is the point: editors nest scroll views inside cards.
        self.assertEqual(fit.count("capScrollViews(group.getChildAt(i), maxHeight)"), 1)

    def test_the_two_fitting_helpers_are_no_longer_private_to_the_activity(self):
        main = read("MainActivity.java")
        self.assertNotIn("private void fitAlertDialog", main)
        self.assertNotIn("private void capAlertScrollViews", main)
        # Every remaining call site must go through DialogFit, or it silently keeps the
        # platform's default dialog size.
        self.assertEqual(len(re.findall(r"fitAlertDialog\(", main)), 0)
        self.assertEqual(len(re.findall(r"DialogFit\.fit\(ui\(\), ", main)), 3)
        self.assertIn("import app.space.core.design.DialogFit;", main)

    def test_every_editor_dialog_still_obtains_its_builder_from_one_place(self):
        main = java_code_only(read("MainActivity.java"))
        # alertBuilder() is the single funnel for all six editor dialogs. 22 call sites is
        # the measured invariant: if a dialog starts constructing AlertDialog.Builder
        # directly it loses the close button, the rounded panel and the motion.
        self.assertEqual(len(re.findall(r"alertBuilder\(\)", main)), 22)
        self.assertIn(
            "return new AppAlertBuilder(new android.view.ContextThemeWrapper(this,style),ui());", main)
        self.assertIn("import app.space.core.design.AppAlertBuilder;", main)
        self.assertNotIn("new AlertDialog.Builder(", main)

    def test_core_design_may_depend_on_core_data_for_the_dialog_theme_choice(self):
        # AppAlertBuilder picks its close-button fill from AppTheme.dark, so :core-design
        # must keep its :core-data dependency. This is the same coupling that reordered the
        # module plan in step 1c.
        builder = (CORE_DESIGN / "AppAlertBuilder.java").read_text()
        self.assertIn("import app.space.core.data.AppTheme;", builder)
        self.assertIn("AppTheme.dark(themedContext) ? 0xff263632 : 0xffe5ece8", builder)
        gradle = (ROOT / "core-design/build.gradle").read_text()
        self.assertIn(":core-data", gradle)
