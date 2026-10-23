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
        self.assertIn('TAB_LABELS={"Clock","Reminders","Timer","Stopwatch","Routines","Usage"}', main)
        self.assertIn('"reminders".equals(target))showRoute("reminders")', main)
        # Dispatch moved from switch(tab) to route comparisons in step 1g-2c, so adjacency
        # is now expressed by the route table rather than by case numbers.
        self.assertIn('"reminders".equals(route))buildReminders();', main)
        self.assertIn('"timer".equals(route))buildTimer();', main)

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
        self.assertIn('private void buildSettings(){settingsError=null;try{switch(settingsGroup)', main)
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
        self.assertIn("versionName '3.10.0'", gradle)
        self.assertIn("versionCode 34", gradle)
        # The About page must not hardcode either half. It said "version 2.0.0" through
        # several releases and described a clock-only app that now hosts fifteen, so both
        # were stale in the one place a user can check what they are running.
        self.assertNotIn("version 2.0.0", main)
        self.assertIn("getPackageManager().getPackageInfo(getPackageName(),0).versionName", main)
        self.assertIn("for(SuiteApp a:availableApps())", main)
        self.assertIn("One for many.", main)
        self.assertIn("\" apps in one install: \"", main)

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
        # and hosts it. Step 1g-2 then put the registry into service: navigation state is
        # read and written through :shell, so this assertion was inverted rather than
        # deleted. ShellNavigationContracts covers the behaviour in detail.
        main = read("MainActivity.java")
        self.assertIn("ClockApp", main)
        self.assertIn("SuiteRegistry", main)

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
        self.assertIn('if((settings||!routeIs("stopwatch"))&&stopwatchScreen!=null){stopwatchScreen.onPause();'
                      "stopwatchScreen=null;}", main)
        self.assertIn("if(stopwatchScreen!=null)stopwatchScreen.onPause();", main)
        self.assertIn("if(stopwatchScreen!=null)stopwatchScreen.onResume();", main)
        # The dead duplicate is gone: two copies of one screen is how a fix reaches one of
        # them and not the other.
        self.assertNotIn("buildStopwatchLegacy", main)
        # This guard used to record that :shell had no callers. Step 1g-2 gave it one, so
        # the marker is inverted; keeping it negative would fail every future nav slice.
        self.assertIn("SuiteRegistry", main)

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


class CiDiagnosticsContracts(unittest.TestCase):
    """CI must be able to tell us *why* it failed, not just *that* it failed.

    The review sandbox cannot download run logs or artifacts (both the
    results-receiver host and the artifact blob host EOF), so for most of this
    project a red run carried no diagnostic at all. Check-run annotations are the
    one failure channel that is an ordinary API object, so the workflow re-emits
    gradle's error lines as annotations. Losing this silently returns the project
    to blind iteration.
    """

    def setUp(self):
        self.yml = (ROOT / ".github/workflows/android-ci.yml").read_text()

    def test_build_steps_capture_output_and_keep_their_exit_status(self):
        # tee without pipefail would report success even when gradlew fails.
        self.assertIn("set -o pipefail", self.yml)
        self.assertIn("2>&1 | tee /tmp/unit.log", self.yml)
        self.assertIn("2>&1 | tee /tmp/build.log", self.yml)
        self.assertIn("id: unit", self.yml)
        self.assertIn("id: build", self.yml)

    def test_failures_are_re_emitted_as_annotations(self):
        self.assertIn('echo "::error title=', self.yml)
        self.assertIn(
            "if: steps.unit.outcome == 'failure' || steps.build.outcome == 'failure'",
            self.yml)
        # javac writes "path:line: error: cannot find symbol"; gradle wraps it in
        # "Execution failed for task". Both must be matched or the common case is
        # the one that goes silent.
        for needle in ("error:", "Execution failed", "FAILED", "AssertionError"):
            self.assertIn(needle, self.yml)

    def test_the_annotation_pattern_reports_failures_and_not_passes(self):
        # Behavioural rather than string-presence: the pattern is extracted from the workflow
        # and run against real gradle output lines.
        #
        # History, in both directions. A bare "> Task .* FAILED" matched the gradle task line
        # but not a test-level "ClassName > testName FAILED", so a failing test reported that
        # *something* failed without saying which. The fix added "Test.* > " -- which also
        # matches PASSED lines. Under the 'test' lifecycle every variant prints every passing
        # test, so the 80-line head filled with successes, the one FAILED line fell off the
        # end, and a green build got reported as a red one. Plain "FAILED" covers the task
        # line and the test line without ever matching a pass, so the general-match
        # requirement is satisfied by it alone.
        import re
        m = re.search(r'grep -nE "([^"]+)" "\$f"', self.yml)
        self.assertIsNotNone(m, "could not locate the annotation grep pattern")
        pattern = re.compile(m.group(1))
        failures = [
            "> Task :feature-calculator:testLiteDebugUnitTest FAILED",
            "app.space.feature.calculator.CalculatorRulesTest > "
            "keysThatCannotAppearHereAreRefusedAtTheKey FAILED",
            "app/src/main/java/app/tictic/clock/MainActivity.java:770: error: cannot find symbol",
            "Execution failed for task ':app:compileLiteDistJavaWithJavac'.",
            "java.lang.AssertionError: expected:<null> but was:<.>",
            "Caused by: java.lang.NullPointerException",
            "  What went wrong:",
        ]
        for line in failures:
            self.assertTrue(pattern.search(line), "failure line not matched: %r" % line)
        # These are the lines that used to consume the whole annotation budget.
        passes = [
            "app.space.shell.NavOrderTest > joins ids with a comma PASSED",
            "app.space.core.domain.TimeMathTest > streaksUseEpochDays PASSED",
            "> Task :app:assembleLiteDist",
            "BUILD SUCCESSFUL in 2m 11s",
            "264 tests completed, 0 failed",
        ]
        for line in passes:
            self.assertFalse(pattern.search(line), "success line matched: %r" % line)

    def test_every_build_is_signed_with_one_stable_sideload_key(self):
        # "App not installed as package conflicts with an existing package" is what Android says
        # when the update's certificate differs from the installed app's. The debug keystore is
        # per-machine and CI is a fresh machine every run, so build 30 could not upgrade build
        # 29. One key in the repository is the only fix available without a Play account.
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("sideload {", gradle)
        self.assertIn("storeType 'pkcs12'", gradle)
        self.assertIn("signing/myspace.p12", gradle)
        self.assertIn("signingConfigs.sideload", gradle)
        self.assertIn("Ensure the stable sideload signing key", self.yml)
        self.assertIn("keytool -genkeypair", self.yml)
        # The run that generates the key must also be the run that persists it.
        self.assertIn("git add artifacts signing", self.yml)

    def test_the_annotation_step_dumps_the_unit_log_tail_as_context(self):
        # A grep can only report the shapes it was written for. When the failure is something
        # else entirely -- an OOM, a daemon crash, a task that never printed FAILED -- the
        # summary at the end of the log is what says so.
        annotation = self.yml.split("Surface compile and test errors as annotations")[1].split("      - name:")[0]
        self.assertIn("tail -30 /tmp/unit.log", annotation)
        self.assertIn("unit-log-tail", annotation)
        # Still bounded: an unbounded dump is its own failure mode, because annotations are
        # capped and a huge log would evict the very lines that matter.
        self.assertIn("| head -80", annotation)

    def test_the_annotation_step_is_what_fails_the_job(self):
        # Both build steps are continue-on-error so their output can be captured;
        # without a hard exit in the annotation step a compile failure would go green.
        for step_id in ("unit", "build"):
            block = self.yml.split("        id: %s\n" % step_id)[1].split("      - name:")[0]
            self.assertIn("continue-on-error: true", block,
                          "step %s must not fail the job before its log is captured" % step_id)
        annotation = self.yml.split("Surface compile and test errors as annotations")[1].split("      - name:")[0]
        self.assertNotIn("continue-on-error: true", annotation,
                         "the annotation step must be allowed to fail the job")
        self.assertIn("          exit 1", annotation)

    def test_jvm_tests_are_still_run_explicitly(self):
        # assembleDebug does not run test tasks. This was already lost once.
        # 'test' rather than the old 'testDebugUnitTest test': once :app has flavours there is
        # no per-build-type testDebugUnitTest task on it, and a task name that does not exist
        # fails a step that is continue-on-error -- the JVM suite would stop running in CI and
        # nothing would say so. 'test' aggregates every variant of every module.
        self.assertIn("sh gradlew --no-daemon --stacktrace test 2>&1", self.yml)


class ScreenHostPersistenceContracts(unittest.TestCase):
    """Screens must be able to confirm a save and have it take effect everywhere.

    Step 1h-iv.4 grew ScreenHost from 9 members to 11. Without these, a screen in a
    feature module can persist data but cannot tell the user it did, and cannot make the
    widgets and schedulers that read the same data notice. The clock's editors finish every
    save with a refresh of WidgetEngine plus three schedulers, all :app-only classes.
    """

    def setUp(self):
        self.host = java_code_only((CORE_SUITE / "ScreenHost.java").read_text())
        self.main = java_code_only(read("MainActivity.java"))

    def test_host_declares_toast_and_a_generic_refresh_channel(self):
        self.assertIn("void toast(String message);", self.host)
        self.assertIn("void refreshSurfaces();", self.host)
        # The channel must stay generic. Naming a clock class here would make the contract
        # belong to one app, and the whole point is that apps never learn what surfaces
        # the shell happens to have.
        for leak in ("WidgetEngine", "AlarmScheduler", "ReminderScheduler", "RoutineReceiver"):
            self.assertNotIn(leak, self.host)

    def test_the_activity_implements_both_new_members(self):
        self.assertIn("@Override public void toast(String s)", self.main)
        self.assertIn("@Override public void refreshSurfaces()", self.main)
        # All four derived surfaces must be refreshed, or a save updates the page while
        # the home-screen widget keeps showing the old routine list.
        body = self.main.split("@Override public void refreshSurfaces()")[1].split("}")[0]
        for call in ("WidgetEngine.refreshAll(this)", "AlarmScheduler.rescheduleAll(this)",
                     "ReminderScheduler.rescheduleAll(this)", "RoutineReceiver.scheduleAll(this)"):
            self.assertIn(call, body)

    def test_screen_host_has_exactly_eleven_members(self):
        # Guards silent contract drift in either direction: a new method breaks every
        # implementor, and a removed one breaks every screen that relied on it.
        members = re.findall(r"\b(?:void|boolean|Context|Palette|List<SuiteApp>|String)\s+(\w+)\(", self.host)
        self.assertEqual(len(members), 11, members)

    def test_toast_was_promoted_not_duplicated(self):
        # MainActivity already had a private toast(); it must be the same one, now public.
        # A second implementation would let screens and chrome diverge on duration.
        self.assertEqual(len(re.findall(r"void toast\(String ", self.main)), 1)
        self.assertIn("Toast.makeText(this,s,Toast.LENGTH_SHORT).show()", self.main)


class ShellNavigationContracts(unittest.TestCase):
    """:shell is now actually used by :app — MySpace reopens into the last screen.

    Step 1g-2. Until this, NavMode, SuiteRegistry and NavPrefs were fully built and had
    zero callers: the module graph was correct but nothing a user could observe came from
    it. This is the first behaviour in the product that is driven by :shell.

    Navigation state is stored as *routes* ("clock", "timer"), not tab indices, because
    tab positions are not stable once the user can reorder them (SUITE-PLAN.md §3.3).
    """

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))
        self.nav = java_code_only((SHELL / "NavPrefs.java").read_text())

    def test_the_activity_reaches_navigation_state_through_shell(self):
        self.assertIn("import app.space.shell.NavPrefs;", self.main)
        self.assertIn("import app.space.shell.SuiteRegistry;", self.main)
        self.assertIn("registry=new SuiteRegistry(suiteApps())", self.main)
        # The registry is built from the registered app, so a second app added to the list
        # is all it takes for it to appear in navigation.
        self.assertIn("private SuiteRegistry registry(){", self.main)

    def test_navigation_state_is_stored_as_routes_not_tab_positions(self):
        self.assertIn('private static final String[] TAB_ROUTES='
                      '{"clock","reminders","timer","stopwatch","routines","usage"};', self.main)
        routes = re.search(r'TAB_ROUTES=\{([^}]*)\}', self.main).group(1)
        names = [r.strip('"') for r in routes.split(",")]
        # Six tabs, six routes, and every one must be a route ClockApp actually accepts --
        # an unknown route silently restores to nothing, with no crash to point at it.
        self.assertEqual(len(names), 6)
        clock = java_code_only((FEATURE_CLOCK / "ClockApp.java").read_text())
        declared = re.search(r'ROUTES = Collections\.unmodifiableList\(Arrays\.asList\((.*?)\)\)',
                             clock, re.S)
        assert declared, "ClockApp.ROUTES not found"
        accepted = set(re.findall(r'"([a-z]+)"', declared.group(1)))
        for n in names:
            self.assertIn(n, accepted, "tab route %r is not accepted by ClockApp" % n)

    def test_last_screen_is_restored_only_when_the_intent_names_no_destination(self):
        body = self.main.split("private void applyRoute(Intent intent)")[1].split("private void ")[0]
        # A widget deep link must win over the remembered screen, or tapping a widget
        # would open wherever the user happened to be last.
        self.assertIn('String target=intent.getStringExtra("tab");', body)
        self.assertIn('if(target==null&&NavPrefs.openLastApp(this))target=NavPrefs.lastApp(this);', body)
        self.assertLess(body.index('NavPrefs.openLastApp'), body.index('if("alarms".equals(target)'),
                        "restore must happen before the route is mapped onto a tab")
        # Exactly one null guard, after the restore, so a cold start with nothing recorded
        # still falls through to the default tab instead of throwing.
        self.assertEqual(body.count("if(target==null)return;"), 1)

    def test_the_visible_screen_is_remembered_from_one_place(self):
        body = self.main.split("private void render(){")[1].split("private ")[0]
        self.assertIn("if(!settings&&tab>=0&&tab<TAB_ROUTES.length)NavPrefs.setLastApp(this,TAB_ROUTES[tab]);",
                      body)
        # Recording from render() rather than each tab's click handler is what covers
        # widget deep links and settings exits too. Assert it is the only writer.
        self.assertEqual(self.main.count("NavPrefs.setLastApp("), 1)

    def test_the_settings_toggle_writes_the_key_navprefs_reads(self):
        # Both sides must agree on "nav_open_last" and on the default. A mismatch means the
        # toggle visibly reads one way while the launch behaviour does the other.
        self.assertIn('"nav_open_last",false);', self.main)
        self.assertIn('KEY_OPEN_LAST = "nav_open_last"', self.nav)
        self.assertIn('getBoolean(KEY_OPEN_LAST, false)', self.nav)

    def test_navprefs_and_the_settings_toggles_share_one_preference_store(self):
        # addSettingsToggle writes through DataStore.prefs, which delegates to AppPrefs.
        # If NavPrefs kept its own file, the toggle would be inert -- the switch would move
        # and nothing would change, which is the hardest kind of bug to notice here.
        self.assertIn("return AppPrefs.prefs(c);", self.nav)
        store = java_code_only(read("DataStore.java"))
        self.assertIn("AppPrefs.prefs", store)


class RouteIdentityContracts(unittest.TestCase):
    """Screens are identified by route, never by tab index.

    Prerequisite for user-customisable navigation (SUITE-PLAN.md §3.3). The rail had 17
    places comparing or assigning ``tab`` against a literal index. That is invisible today
    because the order is fixed, but the moment a user hides or reorders a destination every
    index shifts, and each of those comparisons silently retargets: the timer's tick
    cadence would apply to the wrong screen, the stopwatch teardown guard would stop firing,
    and usage time would be attributed to the wrong tab. None of those crash.

    ``currentRoute``/``routeIs``/``showRoute`` replace them. ``tab`` survives only as the
    field default and in two bounds checks.
    """

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))

    def test_no_comparison_against_a_literal_tab_index_survives(self):
        # Only == and != against an index are the bug; >= and <= bounds checks are fine.
        self.assertEqual(re.findall(r"tab *[!]?== *[0-9]", self.main), [])
        # The field default and the bounds guards are the only legitimate index touches.
        self.assertEqual(re.findall(r"tab *[=!<>]=? *[0-9]", self.main),
                         ["tab=0", "tab>=0", "tab>=0"])
        self.assertIn("private int tab=0;", self.main)

    def test_the_three_route_helpers_are_defined_exactly_once(self):
        for sig in ("private String currentRoute(){",
                    "private boolean routeIs(String route){",
                    "private void showRoute(String route){"):
            self.assertEqual(self.main.count(sig), 1, sig)
        # Both must resolve against the VISIBLE list, not the full table: once a
        # destination can be hidden, tab is an index into what the user can see, so
        # consulting TAB_ROUTES would select the wrong screen.
        cr = self.main.split("private String currentRoute(){")[1].split("}")[0]
        self.assertIn("visibleRoutes()", cr)
        sr = self.main.split("private void showRoute(String route){")[1].split("}return;")[0]
        self.assertIn("visibleRoutes()", sr)
        # An unknown route leaves tab untouched rather than crashing; a deep link naming a
        # destination this build lacks -- or the user has hidden -- is an ordinary outcome.
        self.assertIn('if(v.get(i).id().equals(route)){tab=i;return;}', self.main)

    def test_the_tick_cadence_is_still_attached_to_the_right_screens(self):
        """The stopwatch slice recorded these rates; a retargeted guard would change them."""
        body = self.main.split("private long nextTickDelay(){")[1].split("private ")[0]
        self.assertIn('if(routeIs("timer")&&timerRunning())', body)
        self.assertIn('if(routeIs("clock"))return homeSecondsEnabled()?1000L:60000L;', body)
        self.assertIn('if(routeIs("timer")||routeIs("stopwatch"))return 1000L;', body)
        self.assertIn("if(settings)return 60000L;", body)

    def test_stopwatch_teardown_and_usage_accounting_use_routes(self):
        self.assertIn('if((settings||!routeIs("stopwatch"))&&stopwatchScreen!=null)', self.main)
        self.assertIn('if(routeIs("clock")&&!settings)pendingClockMs+=delta;', self.main)
        self.assertIn('if("usage".equals(route.id())&&!routeIs("usage"))activityHistoryLimit=20;',
                      self.main)

    def test_apply_route_still_accepts_every_route_the_clock_app_declares(self):
        body = self.main.split("private void applyRoute(Intent intent)")[1].split("private void ")[0]
        # The route strings are the widget deep-link contract; converting the assignments to
        # showRoute must not have dropped any of them.
        for route in ('"alarms"', '"clock"', '"reminders"', '"timer"',
                      '"stopwatch"', '"routines"', '"log"', '"usage"'):
            self.assertIn(route, body)
        self.assertEqual(len(re.findall(r'showRoute\("', body)), 6)
        self.assertEqual(re.findall(r'tab=[0-9]', body), [])


class CustomisableNavigationContracts(unittest.TestCase):
    """The rail is data-driven, so hiding a destination cannot break anything else.

    Step 1g-2c. The owner's requirement is that navigation be customisable (§3.3). Before
    this the rail built six fixed slots from three parallel arrays and dispatched screens
    with ``switch(tab)``, so a hidden or reordered destination would have shifted every
    index and built the wrong screen — with no crash to point at it.
    """

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))
        self.nav = java_code_only((SHELL / "NavPrefs.java").read_text())
        self.route = java_code_only((SHELL / "NavRoute.java").read_text())

    def test_the_rail_renders_what_the_user_can_see_not_a_fixed_six(self):
        # Named `destinations`, not `rail`: buildNav already has a HorizontalScrollView
        # called rail, and shadowing it broke rail.addView at compile time.
        self.assertIn("List<NavRoute> destinations=visibleRoutes();", self.main)
        self.assertIn("for(int i=0;i<destinations.size();i++){", self.main)
        self.assertIn("HorizontalScrollView rail=new HorizontalScrollView(this);", self.main)
        # The fixed arrays are gone; a second list of destinations would drift from the
        # first and the rail would show slots that dispatch somewhere else.
        self.assertNotIn('String[] names={"Clock"', self.main)
        self.assertNotIn("private final String[] tabs=", self.main)
        self.assertNotIn("names.length", self.main.split("buildNav")[1] if "buildNav" in self.main else "")

    def test_the_selected_slot_and_its_accent_are_chosen_by_route_not_position(self):
        # i==4 used to mean "Routines" purely by position. After a reorder it would have
        # highlighted whatever sat fourth.
        self.assertNotIn("i==4?accent", self.main)
        self.assertIn('on?Color.WHITE:("routines".equals(route.id())?accent:muted)', self.main)
        self.assertIn("final boolean on=route.id().equals(currentRoute());", self.main)
        self.assertIn("slot.setBackground(shape(on?accent:Color.TRANSPARENT,17));", self.main)

    def test_screen_dispatch_is_route_based(self):
        self.assertNotIn("switch(tab)", self.main)
        body = self.main.split("else{String route=currentRoute();")[1].split("buildNav()")[0]
        for route, builder in (("reminders", "buildReminders()"), ("timer", "buildTimer()"),
                               ("stopwatch", "buildStopwatch()"), ("routines", "buildRoutines()"),
                               ("usage", "buildActivity()")):
            self.assertIn('"%s".equals(route))%s' % (route, builder), body.replace("else if(", "").replace("if(", ""))
        # Unrecognised route falls back to the clock rather than to buildActivity, which is
        # what the old index-default did; falling through to usage would be a visible jump.
        self.assertIn("else buildClock();}", body)

    def test_the_route_cache_is_cleared_on_every_render(self):
        # currentRoute() runs on the stopwatch ticker at up to 16 ms. Reading
        # SharedPreferences each time would be visible as jank, so the list is cached --
        # and a stale cache would keep showing a destination the user just hid.
        self.assertIn("private List<NavRoute> routeCache;", self.main)
        render = self.main.split("private void render(){")[1][:200]
        self.assertIn("routeCache=null;", render)
        self.assertIn("if(routeCache==null)routeCache=NavPrefs.visibleRoutes(this,clockApp.id(),allRoutes());",
                      self.main)

    def test_route_ordering_reuses_the_tested_core_domain_resolver(self):
        # A second ordering rule for routes would be untestable in an Android module, and
        # an ordering bug shows up as a destination that never appears.
        self.assertIn("public final class NavRoute implements NavOrder.Entry", self.route)
        self.assertIn("NavOrder.resolve(", self.nav)
        self.assertIn("NavOrder.parse(", self.nav)
        self.assertEqual(self.nav.count("NavOrder.resolve("), 3)

    def test_route_preferences_are_namespaced_per_app(self):
        # Two apps may each declare a destination called "settings"; without the app id in
        # the key, hiding one would hide both.
        self.assertIn('KEY_ROUTE_ORDER = "nav_route_order_"', self.nav)
        self.assertIn('KEY_ROUTE_HIDDEN = "nav_route_hidden_"', self.nav)
        self.assertIn("KEY_ROUTE_HIDDEN + appId", self.nav)
        self.assertIn("KEY_ROUTE_ORDER + appId", self.nav)

    def test_a_route_id_containing_a_comma_is_rejected_at_construction(self):
        # Ids are comma-joined when persisted. A comma would corrupt the stored order and
        # surface much later as a wrong destination opening -- the same reasoning
        # SuiteRegistry applies to app ids.
        self.assertIn('if (id.indexOf(\',\') >= 0)', self.route)
        self.assertIn('"NavRoute id must not contain a comma: "', self.route)
        self.assertIn('"NavRoute requires an id"', self.route)

    def test_hiding_all_destinations_cannot_crash(self):
        # visibleRoutes may legitimately come back empty if a user hides everything, or if
        # an app declares no destinations. currentRoute must fall back, not index -1.
        self.assertIn("if(v.isEmpty())return TAB_ROUTES[0];", self.main)
        self.assertIn("return tab>=0&&tab<v.size()?v.get(tab).id():v.get(0).id();", self.main)
        self.assertIn("if (all == null || all.isEmpty()) return java.util.Collections.emptyList();",
                      self.nav)


class NavigationEditorContracts(unittest.TestCase):
    """Settings ▸ Navigation: the editor that makes L1 customisation real.

    Step 1g-2d. The rail has been data-driven since 1g-2c but nothing wrote the
    preferences it read, so it always showed all six destinations. This is the slice that
    turns "capable of being customised" into "customisable".
    """

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))
        self.editor = self.main.split("private void buildNavigationSettings(){")[1].split("private TextView moveButton(")[0]

    def test_settings_has_nine_groups_and_the_arrays_still_agree(self):
        headings = re.search(r'String\[\] headings=\{([^}]*)\}', self.main).group(1)
        labels = re.search(r'String\[\] navLabels=\{([^}]*)\}', self.main).group(1)
        h = re.findall(r'"([^"]*)"', headings)
        l = re.findall(r'"([^"]*)"', labels)
        # headings is the caption and navLabels the picker; if they diverge the caption
        # names a different group from the one selected. Both must be 9 and both must end
        # with the About group, which is the switch's default.
        self.assertEqual(len(h), 10, h)
        self.assertEqual(len(l), 10, l)
        self.assertEqual(h[-1], "DIAGNOSTICS")
        self.assertEqual(l[-1], "Diagnostics")
        self.assertEqual(h[7], "NAVIGATION")
        self.assertEqual(l[7], "Navigation")
        self.assertIn("Math.min(9,settingsGroup)", self.main)

    def test_the_group_picker_is_not_hardcoded_to_a_count(self):
        self.assertIn("for(int i=0;i<navLabels.length;i++)", self.main)
        self.assertIn("case 7:buildNavigationSettings();break;case 9:buildDiagnosticsSettings();break;", self.main)
        self.assertIn("settingsError=null;try{switch(settingsGroup){", self.main)
        self.assertIn("}catch(Throwable settingsFailure){", self.main)

    def test_the_editor_writes_both_preferences_and_invalidates_the_cache(self):
        self.assertIn("NavPrefs.setHiddenRoutes(this,clockApp.id(),next)", self.editor)
        self.assertIn("NavPrefs.saveRouteOrder(this,clockApp.id(),swapped(ordered,index,index-1))", self.editor)
        self.assertIn("NavPrefs.saveRouteOrder(this,clockApp.id(),swapped(ordered,index,index+1))", self.editor)
        # currentRoute() is cached for the 16 ms ticker. A write that does not clear the
        # cache changes the preference while the rail keeps rendering the old list, and the
        # user's tap appears to do nothing until some unrelated re-render.
        # Five handlers write: reorder up, reorder down, the visibility switch, reset, and the
        # navigation-style chooser. Reset writes both route preferences, so six writes must be
        # matched by five cache clears and five re-renders -- one per handler, none skipped.
        #
        # The style chooser is counted here on purpose. It writes a preference that changes what
        # is drawn, so a handler that wrote it without clearing the cache would leave the old
        # style on screen until some unrelated re-render, which is precisely the failure this
        # assertion exists to prevent.
        writes = (self.editor.count("NavPrefs.setHiddenRoutes(")
                  + self.editor.count("NavPrefs.saveRouteOrder(")
                  + self.editor.count("NavPrefs.setMode("))
        self.assertEqual(writes, 6, "expected 6 preference writes")
        self.assertEqual(self.editor.count("routeCache=null;"), 5)
        self.assertEqual(self.editor.count("render();"), 5)

    def test_the_last_visible_destination_cannot_be_hidden(self):
        # An empty rail would leave no way back into Settings to undo it.
        self.assertIn('if(visibleCount<=1){toast("At least one destination must stay visible.");'
                      'toggle.setChecked(true);return;}', self.editor)
        # The switch must be put back, or it reads as hidden while the rail still shows it.
        self.assertIn("toggle.setChecked(true);", self.editor)
        self.assertIn("int visible=0;for(NavRoute r:ordered)if(!hidden.contains(r.id()))visible++;",
                      self.editor)

    def test_hidden_destinations_keep_their_data_and_still_open_from_widgets(self):
        # Stated to the user in the editor, so it must actually be true: hiding removes the
        # slot from the rail, not the destination. applyRoute resolves any route regardless
        # of visibility, and the data is untouched.
        self.assertIn("Hidden destinations keep their data and still open from widgets", self.main)
        apply_route = self.main.split("private void applyRoute(Intent intent)")[1].split("private void ")[0]
        self.assertNotIn("visibleRoutes()", apply_route)
        self.assertNotIn("hiddenRoutes(", apply_route)

    def test_reorder_affordances_are_inert_at_the_ends_of_the_list(self):
        self.assertIn("moveButton(\"\\u2191\",index>0&&!isHidden)", self.editor)
        self.assertIn("moveButton(\"\\u2193\",index<ordered.size()-1&&!isHidden)", self.editor)
        # The click guards too: an inert-looking button that still fires would swap a
        # destination with whatever sits outside the list.
        self.assertIn("if(index==0)return;", self.editor)
        self.assertIn("if(index>=ordered.size()-1)return;", self.editor)
        self.assertIn("b.setAlpha(enabled?1f:.3f);b.setFocusable(enabled);b.setClickable(enabled);",
                      self.main)

    def test_reset_clears_order_and_hidden_together(self):
        reset = self.editor.split("reset.setOnClickListener")[1]
        self.assertIn("NavPrefs.saveRouteOrder(this,clockApp.id(),java.util.Collections.<String>emptyList())", reset)
        self.assertIn("NavPrefs.setHiddenRoutes(this,clockApp.id(),java.util.Collections.<String>emptySet())", reset)
        self.assertIn('toast("Navigation reset.")', reset)
        # Clearing only one would leave the user partway back to the default and unable to
        # tell which half had been reset.
        self.assertIn("routeCache=null;", reset)


class CalculatorAppContracts(unittest.TestCase):
    """The second app in the suite: :feature-calculator.

    Deliberately the simplest of the thirteen requested apps -- offline, no permissions, no
    network, no storage, no :app coupling. It exists to prove :core-suite carries a second
    implementor without special cases, and to make NavMode.HUB worth building.
    """

    CALC = ROOT / "feature-calculator/src/main/java/app/space/feature/calculator"

    def setUp(self):
        self.app = java_code_only((self.CALC / "CalculatorApp.java").read_text())
        self.rules = (self.CALC / "CalculatorRules.java").read_text()
        self.screen = java_code_only((self.CALC / "CalculatorScreen.java").read_text())
        self.gradle_code = "\n".join(
            l for l in (ROOT / "feature-calculator/build.gradle").read_text().splitlines()
            if not l.strip().startswith("//"))

    def test_the_module_depends_on_core_only(self):
        # The dependency rule (SUITE-PLAN.md 3.1). Depending on :app, :shell or another
        # feature would make the apps unable to ship independently, which is the whole
        # point of the L2/L3 control model.
        for forbidden in ("project(':app')", "project(':shell')", "project(':feature-clock')"):
            self.assertNotIn(forbidden, self.gradle_code)
        self.assertIn("api project(':core-suite')", self.gradle_code)
        self.assertIn("namespace 'app.space.feature.calculator'", self.gradle_code)
        self.assertIn("include ':feature-calculator'",
                      (ROOT / "settings.gradle").read_text())

    def test_the_arithmetic_is_pure_java_so_it_can_actually_be_tested(self):
        # There is no emulator in this project. Anything importing android.* is effectively
        # untested, which is why the evaluator is separate from the screen.
        for forbidden in ("import android.", "import androidx.", "Context", "View"):
            self.assertNotIn(forbidden, self.rules)
        self.assertIn("public static double evaluate(String expression)", self.rules)

    def test_malformed_input_is_reported_not_guessed(self):
        # A calculator that returns 0 for "3+" teaches the user nothing about the typo.
        self.assertIn('throw new ArithmeticException("division by zero")', self.rules)
        self.assertIn('throw new ArithmeticException("empty expression")', self.rules)
        self.assertIn('throw new ArithmeticException("unbalanced parentheses")', self.rules)
        self.assertIn('throw new ArithmeticException("expected a number")', self.rules)
        self.assertIn("if (!p.atEnd()) throw new ArithmeticException", self.rules)

    def test_the_keypad_translates_typographic_glyphs_before_parsing(self):
        # The display shows divide and minus signs; the parser takes ASCII. Appending the
        # glyph itself makes every operator key silently do nothing, because
        # isInputCharacter rejects it -- so both halves are pinned here.
        rules = (ROOT / "feature-calculator/src/main/java/app/space/feature/calculator/CalculatorRules.java").read_text()
        self.assertIn("public static String normaliseKey(String key)", rules)
        self.assertIn("if (c == '\\u00f7') return \"/\";", rules)
        self.assertIn("if (c == '\\u00d7') return \"*\";", rules)
        self.assertIn("if (c == '\\u2212') return \"-\";", rules)
        self.assertIn("String k = normaliseKey(key);", rules)
        self.assertIn("CalculatorRules.append(expression.toString(), key)", self.screen)

    def test_errors_are_shown_not_thrown_at_the_user(self):
        self.assertIn("catch (Exception e)", self.screen)
        self.assertIn('result = "Error";', self.screen)
        self.assertIn('if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";',
                      self.rules)

    def test_the_keypad_is_laid_out_like_the_one_it_recreates(self):
        # The owner's complaint had two halves: the keys accepted anything, and the keypad sat in
        # the top half of the screen with dead space under it. Fixed heights are what caused the
        # second half, so the rows must take their height from weights, and the display from a
        # larger one, on any screen.
        self.assertIn('{"C", "( )", "\\u232b", "\\u00f7"},', self.screen)
        self.assertIn('{"0", ".", "=", null},', self.screen)
        self.assertIn("root.addView(row, new LinearLayout.LayoutParams(-1, 0, 1f));", self.screen)
        self.assertIn("new LinearLayout.LayoutParams(-1, 0, 2f)", self.screen)
        self.assertIn("params.weight = 2f;", self.screen)  # equals spans the last two cells
        # A live result under the display: silence until equals hides mistakes until it is too
        # late to fix them cheaply.
        self.assertIn('"= " + CalculatorRules.format(', self.screen)

    def test_the_input_rules_refuse_the_sequences_typed_on_device(self):
        rules = (ROOT / "feature-calculator/src/main/java/app/space/feature/calculator/CalculatorRules.java").read_text()
        # "0/0------85522*3*-" reached a real display one tap at a time. The chain rule is the
        # fix: a negation minus appends once, and a minus after a minus replaces.
        self.assertIn("if (c == '-') {", rules)
        self.assertIn("if (last != '-') return current + c;", rules)
        # And swapping operators over a pending negation must drop the negation, or 5*- then *
        # stacks two operators -- expected:<5*[]> but was:<5*[*]> was a red CI run for exactly that.
        self.assertIn("return current.substring(0, current.length() - 2) + c;", rules)
        self.assertIn('if ("-".equals(current)) return null;', rules)
        # The ( ) key needs rules behind both brackets or it is a key that sometimes lies.
        self.assertIn("if (c == '(') {", rules)
        self.assertIn("if (c == ')') {", rules)

    def test_a_settings_crash_renders_a_page_that_describes_itself(self):
        # The net used to wrap only buildSettings; the owner's crash is on the tap that opens
        # settings, so the whole render is inside the try now, with a fallback page as the catch.
        main = read("MainActivity.java")
        self.assertIn("private void renderSettingsPage(){try{renderSettingsPageInner();}catch(Throwable settingsCrash){",
                      main)
        self.assertIn("private void renderSettingsFallback(Throwable failure){", main)
        self.assertIn("AppLog.crash(this,settingsCrash);", main)

    def test_the_apps_tile_holds_the_first_slot_of_the_picker_grid(self):
        # Owner's ordering: the apps icon where the clock app used to be, clock app second. The
        # grid flows from the lead tile, so app i sits at position i+1.
        main = read("MainActivity.java")
        self.assertIn('View appsTile=navTile("Apps",TicIconView.APPS,true);', main)
        self.assertIn("if((i+1)%4==0){row=hbox();sheet.addView(row,lp(-1,-2));}", main)

    def test_the_app_declares_a_stable_id_and_declines_unknown_routes(self):
        self.assertIn('public static final String ID = "calculator";', self.app)
        self.assertIn("public final class CalculatorApp implements SuiteApp", self.app)
        # acceptsRoute must reject a route the app never had; a stale deep link is an
        # ordinary outcome, and createScreen returns null rather than throwing.
        self.assertIn('return route == null || route.isEmpty() || "basic".equals(route);', self.app)
        self.assertIn("return acceptsRoute(route) ? new CalculatorScreen(host) : null;", self.app)
        # The id is persisted in the user's navigation order, so it is a contract.
        self.assertIn("TicIconView.CALCULATOR", self.app)

    def test_the_glyph_was_added_to_the_icon_set(self):
        icons = java_code_only(
            (CORE_DESIGN / "TicIconView.java").read_text(encoding="utf-8"))
        self.assertIn("CALCULATOR=9", icons)
        self.assertIn("case CALCULATOR:", icons)


class SecondAppIntegrationContracts(unittest.TestCase):
    """Registering a second app must not special-case it anywhere in the host."""

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))

    def test_both_apps_are_registered_and_available(self):
        self.assertIn("private final SuiteApp calculatorApp=new CalculatorApp();", self.main)
        # One list, read by the registry, availability and the picker, so registering an
        # app is a single entry rather than four separate edits that can disagree.
        self.assertIn("registry=new SuiteRegistry(suiteApps())", self.main)
        self.assertIn("return suiteApps();", self.main)
        self.assertIn("for(SuiteApp a:suiteApps())if(a.id().equals(id))return true;return false;",
                      self.main)
        # Size is deliberately not asserted here. It was pinned in three tests and broke
        # all three every time an app landed; FourMoreAppsContracts owns the count now.
        block = self.main.split("suiteApps(){return")[1].split(";")[0]
        for field in ("clockApp", "calculatorApp"):
            self.assertIn(field, block)

    def test_opening_another_app_goes_through_the_contract_not_a_hardcoded_branch(self):
        body = self.main.split("public boolean openApp(String id,String route){")[1].split("@Override")[0]
        # The clock keeps its route-based deep links; anything else is resolved through the
        # registry, so a third app needs no change here.
        self.assertIn("SuiteApp app=registry().find(id);", body)
        self.assertIn("Screen screen=app.createScreen(this,route);", body)
        # A declined route returns false rather than throwing: under the control model an
        # app being absent and an app declining a route look the same to the caller.
        self.assertEqual(body.count("return false;"), 3)
        self.assertIn("NavPrefs.pushRecent(this,id);AppLog.event(this,\"app\",\"open \"+id", body)
        self.assertIn("activeScreen=screen;activeApp=app;settings=false;routeCache=null;", body)

    def test_a_foreign_app_replaces_the_clock_screen_and_its_rail(self):
        # The clock's rail would otherwise stay on screen over another app's UI, and its
        # slots would switch back mid-use.
        self.assertIn("else if(activeScreen!=null){body.addView(activeScreen.build(this));}", self.main)
        self.assertIn("if(!settings){if(activeScreen==null)buildNav();else buildAppNav();}", self.main)

    def test_there_is_always_a_way_back_to_the_clock(self):
        # The header wordmark is the escape hatch. Without it, opening the calculator would
        # be a one-way trip with no rail left to tap.
        self.assertIn("View.OnClickListener home=v->{activeScreen=null;activeApp=null;showRoute(\"clock\");", self.main)
        clock_branch = self.main.split("if(clockApp.id().equals(id)){")[1].split("}")[0]
        self.assertIn("activeScreen=null;", clock_branch)

    def test_app_module_declares_the_dependency(self):
        gradle = "\n".join(l for l in (ROOT / "app/build.gradle").read_text().splitlines()
                           if not l.strip().startswith("//"))
        self.assertIn("implementation project(':feature-calculator')", gradle)


class ThreeMoreAppsContracts(unittest.TestCase):
    """Notes, Habits and Expenses: apps three, four and five.

    Registered in one slice to prove the host needs no per-app change. Each is offline, with
    no permissions, no network and no :app coupling, and each keeps its rules in pure Java
    so they are actually tested rather than only compiled.
    """

    APPS = (("notes", "NotesApp", "NotesRules", "NotesStore", "notes_items"),
            ("habits", "HabitsApp", "HabitsRules", "HabitsStore", "habits_items"),
            ("expenses", "ExpensesApp", "ExpenseRules", "ExpenseStore", "expenses_items"))

    def src(self, mod, name):
        return (ROOT / ("feature-%s/src/main/java/app/space/feature/%s/%s.java" % (mod, mod, name))).read_text()

    def gradle(self, mod):
        return "\n".join(l for l in (ROOT / ("feature-%s/build.gradle" % mod)).read_text().splitlines()
                         if not l.strip().startswith("//"))

    def test_all_three_modules_are_declared_and_wired(self):
        settings = (ROOT / "settings.gradle").read_text()
        app_gradle = "\n".join(l for l in (ROOT / "app/build.gradle").read_text().splitlines()
                               if not l.strip().startswith("//"))
        for mod, _, _, _, _ in self.APPS:
            self.assertIn("include ':feature-%s'" % mod, settings)
            self.assertIn("implementation project(':feature-%s')" % mod, app_gradle)
            self.assertIn("namespace 'app.space.feature.%s'" % mod, self.gradle(mod))

    def test_no_new_app_may_reach_the_shell_or_another_app(self):
        # The dependency rule (SUITE-PLAN.md 3.1). This is what lets an app be excluded
        # from a lite flavour or not yet downloaded without anything else noticing.
        for mod, _, _, _, _ in self.APPS:
            g = self.gradle(mod)
            for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
                self.assertNotIn(forbidden, g, "%s must not depend on %s" % (mod, forbidden))
            self.assertIn("api project(':core-suite')", g)

    def test_the_rules_are_pure_java_so_they_can_be_tested(self):
        # There is no emulator here. Anything importing android.* is effectively untested,
        # which is why streak maths, note search and money totals all live apart from the
        # screens that render them.
        for mod, _, rules, _, _ in self.APPS:
            body = self.src(mod, rules)
            for forbidden in ("import android.", "import androidx.", "Context"):
                self.assertNotIn(forbidden, body, "%s must stay pure" % rules)

    def test_each_app_has_a_jvm_test_suite(self):
        for mod, _, rules, _, _ in self.APPS:
            path = ROOT / ("feature-%s/src/test/java/app/space/feature/%s/%sTest.java" % (mod, mod, rules))
            self.assertTrue(path.exists(), "missing %s" % path)
            self.assertIn("@Test", path.read_text())

    def test_money_is_held_in_minor_units_never_doubles(self):
        # java_code_only, not the raw file: the javadoc explains *why* doubles are avoided,
        # so it legitimately contains the word. Grepping the whole file would make the
        # documentation fail the assertion it exists to justify.
        rules = java_code_only(self.src("expenses", "ExpenseRules"))
        self.assertIn("public final long amountMinor;", rules)
        self.assertIn("public static long total(List<Expense> expenses)", rules)
        self.assertNotIn("double", rules)
        self.assertNotIn("float", rules)
        # parseAmountMinor returns null on bad input rather than 0: a typo stored as zero
        # would silently skew every total the app reports.
        self.assertIn("public static Long parseAmountMinor(String input)", rules)
        # Two decimal places is the contract; a third would mean fractional paise being
        # silently dropped somewhere downstream.
        self.assertIn("if (fraction.length() > 2 || !fraction.matches(\"[0-9]*\")) return null;", rules)

    def test_habit_streaks_are_computed_in_days_not_milliseconds(self):
        rules = self.src("habits", "HabitsRules")
        self.assertIn("public static long epochDay(long millis)", rules)
        self.assertIn("Math.floorDiv(millis, MILLIS_PER_DAY)", rules)
        # Surviving until the user has had a chance to complete today is the behaviour that
        # stops every streak resetting at midnight.
        self.assertIn("days.contains(todayEpochDay - 1)", rules)

    def test_stored_data_uses_a_stable_key_and_survives_corruption(self):
        # Data must outlive the module (SUITE-PLAN.md 3.4): stable keys, plain JSON, and a
        # corrupt or absent blob reading as empty rather than crashing the screen.
        for mod, _, _, store, key in self.APPS:
            body = self.src(mod, store)
            self.assertIn('KEY = "%s"' % key, body)
            self.assertIn("AppPrefs.prefs(c)", body)
            self.assertIn("catch (Exception e)", body)
            self.assertIn("public static String newId()", body)

    def test_every_app_is_reachable_from_the_rail(self):
        # Registering an app is not the same as shipping it: without a switcher the three
        # new apps would exist in the registry and be impossible to open.
        main = java_code_only(read("MainActivity.java"))
        self.assertIn("private void showAppPicker(){", main)
        self.assertIn("appsSlot.setOnClickListener(v->showAppPicker());", main)
        self.assertIn("List<SuiteApp> apps=availableApps();", main)
        # The picker reads availableApps rather than a second hardcoded list, so a sixth app
        # appears with no change here.
        self.assertIn("dismissAppSheet();openApp(app.id());", main)
        self.assertIn("List<String> recents=NavPrefs.recents(this);", main)
        self.assertIn("NavPrefs.pushRecent(this,id);", main)

    def test_every_app_is_registered_once_in_one_list(self):
        # The count is deliberately NOT pinned here any more: CalendarAndTasksContracts
        # asserts the current size, and pinning it twice meant every new app broke two
        # tests for the same reason. What matters here is that each app is constructed
        # exactly once, in the list, and nowhere else.
        main = java_code_only(read("MainActivity.java"))
        for field, ctor in (("notesApp", "NotesApp"), ("habitsApp", "HabitsApp"),
                            ("expensesApp", "ExpensesApp"), ("calendarApp", "CalendarApp"),
                            ("tasksApp", "TasksApp")):
            self.assertEqual(1, main.count("private final SuiteApp %s=new %s();" % (field, ctor)))
        registry = re.search(r"suiteApps\(\)\{return java\.util\.Arrays\.<SuiteApp>asList\(([^)]*)\)", main)
        self.assertIsNotNone(registry, "suiteApps() not found")
        for field in ("clockApp", "calculatorApp", "notesApp", "habitsApp", "expensesApp",
                      "calendarApp", "tasksApp"):
            self.assertIn(field, registry.group(1))

    def test_each_new_app_declines_routes_it_does_not_have(self):
        for mod, app, _, _, _ in self.APPS:
            body = java_code_only(self.src(mod, app))
            self.assertIn("implements SuiteApp", body)
            self.assertIn("return acceptsRoute(route) ? new ", body)
            self.assertIn(" : null;", body)
            self.assertIn("public static final String ID", body)
            self.assertIn("@Override public int defaultOrder()", body)


class CalendarAndTasksContracts(unittest.TestCase):
    """Apps six and seven: Calendar and Tasks.

    Both are offline with no permissions. Calendar deliberately does not read the device
    calendar provider -- that would need READ_CALENDAR, which the Settings page's
    "minimum access, and why" promise would then have to justify.
    """

    MODS = ("weather", "recorder", "files", "documents")
    RULES = {"weather": "WeatherRules", "recorder": "RecorderRules",
             "files": "FileRules", "documents": "DocumentRules"}

    def src(self, mod, name):
        return (ROOT / ("feature-%s/src/main/java/app/space/feature/%s/%s.java" % (mod, mod, name))).read_text()

    def gradle(self, mod):
        return "\n".join(l for l in (ROOT / ("feature-%s/build.gradle" % mod)).read_text().splitlines()
                         if not l.strip().startswith("//"))

    def test_both_modules_are_declared_and_wired(self):
        settings = (ROOT / "settings.gradle").read_text()
        app_gradle = "\n".join(l for l in (ROOT / "app/build.gradle").read_text().splitlines()
                               if not l.strip().startswith("//"))
        for mod in ("calendar", "tasks"):
            self.assertIn("include ':feature-%s'" % mod, settings)
            self.assertIn("implementation project(':feature-%s')" % mod, app_gradle)
            self.assertIn("namespace 'app.space.feature.%s'" % mod, self.gradle(mod))
            for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
                self.assertNotIn(forbidden, self.gradle(mod))

    def test_seven_apps_are_registered_in_one_list(self):
        main = java_code_only(read("MainActivity.java"))
        registry = re.search(r"suiteApps\(\)\{return java\.util\.Arrays\.<SuiteApp>asList\(([^)]*)\)", main)
        self.assertIsNotNone(registry)
        ids = registry.group(1).split(",")
        for field in ('calendarApp', 'tasksApp'):
            self.assertIn(field, ids)  # size owned by FourMoreAppsContracts
        self.assertIn("private final SuiteApp calendarApp=new CalendarApp();", main)
        self.assertIn("private final SuiteApp tasksApp=new TasksApp();", main)

    def test_the_calendar_rules_are_pure_java(self):
        rules = self.src("calendar", "CalendarRules")
        for forbidden in ("import android.", "import androidx.", "Context"):
            self.assertNotIn(forbidden, rules)
        # A fixed zone for the grid maths: letting the default zone in would make the
        # leading-blank count differ between the JVM test and a device elsewhere.
        self.assertIn('TimeZone.getTimeZone("UTC")', rules)

    def test_the_grid_is_a_fixed_size_and_undated_tasks_do_not_float_to_the_top(self):
        rules = java_code_only(self.src("calendar", "CalendarRules"))
        self.assertIn("GRID_CELLS = 42", rules)
        tasks = java_code_only(self.src("tasks", "TasksRules"))
        # NO_DUE is -1, so the comparator must branch on hasDue() rather than compare the
        # sentinel as a timestamp.
        self.assertIn("NO_DUE = -1L", tasks)
        self.assertIn("if (a.hasDue() != b.hasDue()) return a.hasDue() ? -1 : 1;", tasks)

    def test_day_ranges_are_half_open(self):
        cal = java_code_only(self.src("calendar", "CalendarRules"))
        self.assertIn("e.when >= fromInclusive && e.when < toExclusive", cal)
        tasks = java_code_only(self.src("tasks", "TasksRules"))
        self.assertIn("task.dueAt >= todayStart && task.dueAt < tomorrowStart", tasks)

    def test_both_apps_have_jvm_test_suites(self):
        for mod, rules in (("calendar", "CalendarRules"), ("tasks", "TasksRules")):
            path = ROOT / ("feature-%s/src/test/java/app/space/feature/%s/%sTest.java" % (mod, mod, rules))
            self.assertTrue(path.exists(), "missing %s" % path)
            self.assertIn("@Test", path.read_text())

    def test_a_blank_due_date_is_not_defaulted_to_today(self):
        # Defaulting to today would make every task overdue by tomorrow, which trains the
        # user to ignore the list entirely.
        screen = self.src("tasks", "TasksScreen")
        self.assertIn("long due = TasksRules.NO_DUE;", screen)
        self.assertIn("A blank due date means NO_DUE, not today", screen)

    def test_events_are_stored_at_the_time_the_user_picked(self):
        # The midday constant this pinned was a stand-in for "the editor had no clock". The
        # editor has one now, so the stored instant is the chosen hour and minute, and the
        # boundary risk the constant guarded is the user's own choice rather than a default.
        screen = java_code_only(
            (ROOT / "feature-calendar/src/main/java/app/space/feature/calendar/CalendarScreen.java").read_text())
        self.assertIn("time.setIs24HourView(true);", screen)
        self.assertIn("dayFrom + time.getHour() * 3600000L + time.getMinute() * 60000L", screen)

    def src(self, mod, name):
        # Documents' rules moved to core-domain when the owner folded the app into Files; the
        # resolver follows the code rather than the module list, so the purity and CSV contracts
        # keep guarding it wherever it lives.
        if mod == "documents":
            return (ROOT / ("core-domain/src/main/java/app/space/core/domain/%s.java" % name)).read_text()
        return (ROOT / ("feature-%s/src/main/java/app/space/feature/%s/%s.java" % (mod, mod, name))).read_text()

    def gradle(self, mod):
        return "\n".join(l for l in (ROOT / ("feature-%s/build.gradle" % mod)).read_text().splitlines()
                         if not l.strip().startswith("//"))

    def test_all_four_modules_are_declared_and_wired(self):
        settings = (ROOT / "settings.gradle").read_text()
        app_gradle = "\n".join(l for l in (ROOT / "app/build.gradle").read_text().splitlines()
                               if not l.strip().startswith("//"))
        for mod in self.MODS:
            self.assertIn("include ':feature-%s'" % mod, settings)
            self.assertIn("implementation project(':feature-%s')" % mod, app_gradle)
            self.assertIn("namespace 'app.space.feature.%s'" % mod, self.gradle(mod))

    def test_no_new_app_may_reach_the_shell_or_another_app(self):
        for mod in self.MODS:
            g = self.gradle(mod)
            for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
                self.assertNotIn(forbidden, g, "%s must not depend on %s" % (mod, forbidden))
            self.assertIn("api project(':core-suite')", g)

    def test_eleven_apps_are_registered_in_one_list(self):
        main = java_code_only(read("MainActivity.java"))
        registry = re.search(r"suiteApps\(\)\{return java\.util\.Arrays\.<SuiteApp>asList\(([^)]*)\)", main)
        self.assertIsNotNone(registry, "suiteApps() not found")
        ids = [x.strip() for x in registry.group(1).split(",")]
        # Not the whole-suite count: FinalFourAppsContracts owns that, and pinning it in two
        # places means every new app breaks both for the same reason.
        self.assertIn("weatherApp", ids)
        self.assertIn("documentsApp", ids)
        self.assertTrue(len(ids) >= 11, ids)
        for mod, field in (("weather", "weatherApp"), ("recorder", "recorderApp"),
                           ("files", "filesApp"), ("documents", "documentsApp")):
            self.assertIn(field, ids)
            self.assertEqual(1, main.count("new %sApp()" % mod.capitalize().replace("Files", "Files")))

    def test_the_rules_are_pure_java_so_they_are_actually_tested(self):
        for mod in self.MODS:
            body = self.src(mod, self.RULES[mod])
            for forbidden in ("import android.", "import androidx.", "Context", "View"):
                self.assertNotIn(forbidden, body, "%s must stay pure" % self.RULES[mod])

    def test_each_new_app_has_a_jvm_test_suite(self):
        for mod in self.MODS:
            if mod == "documents":
                path = ROOT / ("core-domain/src/test/java/app/space/core/domain/%sTest.java"
                               % self.RULES[mod])
            else:
                path = ROOT / ("feature-%s/src/test/java/app/space/feature/%s/%sTest.java"
                               % (mod, mod, self.RULES[mod]))
            self.assertTrue(path.exists(), "missing %s" % path)
            self.assertIn("@Test", path.read_text())

    def test_csv_quoting_is_the_contract_not_a_split_on_commas(self):
        rules = java_code_only(self.src("documents", "DocumentRules"))
        # A field is quoted when it holds a comma, a quote or a newline, and embedded quotes
        # are doubled. Splitting on commas would shift every column after the first
        # unquoted comma, and the damage appears only when the file is opened elsewhere.
        self.assertIn('value.indexOf(\',\') >= 0 || value.indexOf(\'"\') >= 0', rules)
        self.assertIn('value.replace("\\"", "\\"\\"")', rules)

    def test_the_file_browser_cannot_walk_out_of_app_private_storage(self):
        rules = java_code_only(self.src("files", "FileRules"))
        self.assertIn('if (part.equals("..")) return false;', rules)
        self.assertIn("if (relativePath.startsWith(\"/\")) return false;", rules)
        # java_code_only: the screen's javadoc explains WHY DocumentFile is not used, so
        # grepping the raw file matches the explanation and fails the assertion that exists
        # to justify it. Fifth time this trap has appeared in this suite.
        screen = java_code_only(self.src("files", "FilesScreen"))
        self.assertIn("FileRules.isInside(", screen)
        # AndroidX is disabled in this project, so SAF's DocumentFile is unavailable; the
        # browser uses app-private storage, which needs no permission at all.
        self.assertIn("context.getFilesDir()", screen)
        self.assertNotIn("DocumentFile", screen)

    def test_record_audio_is_declared_and_requested_only_at_the_moment_of_use(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn("android.permission.RECORD_AUDIO", manifest)
        screen = self.src("recorder", "RecorderScreen")
        # Asking at launch would break the Settings page's promise that optional access is
        # requested only when its feature is used.
        self.assertIn("Manifest.permission.RECORD_AUDIO", screen)
        self.assertIn("if (!hasPermission(context)) { requestPermission(context); return; }", screen)
        # The recorder must be released when the screen goes away, or it holds the
        # microphone in the background and the next start fails silently.
        self.assertIn("if (recording && pauseContext != null) stop(pauseContext);", screen)

    def test_weather_needs_no_key_and_no_location_and_fetches_only_when_asked(self):
        rules = java_code_only(self.src("weather", "WeatherRules"))
        self.assertIn("geocoding-api.open-meteo.com", rules)
        self.assertIn("api.open-meteo.com", rules)
        # A typed city is what lets Settings keep promising no location permission.
        screen = self.src("weather", "WeatherScreen")
        self.assertNotIn("ACCESS_FINE_LOCATION", screen)
        self.assertNotIn("getLastKnownLocation", screen)
        self.assertNotIn("LocationManager", screen)
        # Open-Meteo needs no key, so a key appearing here would mean the provider had been
        # switched to one that bills per call and needs a secret baked into the build.
        self.assertNotIn("apikey", screen.lower())
        self.assertIn("new Thread(", screen)
        self.assertIn("main.post(", screen)

    def test_the_network_fetch_runs_off_the_main_thread_and_posts_back(self):
        # Inline would throw NetworkOnMainThreadException; a bare thread touching views
        # would crash only sometimes, which is worse.
        screen = self.src("weather", "WeatherScreen")
        self.assertIn("private final Handler main = new Handler(Looper.getMainLooper());", screen)
        self.assertIn("connection.disconnect();", screen)
        self.assertIn("setConnectTimeout(", screen)
        self.assertIn("setReadTimeout(", screen)

    def test_a_malformed_forecast_response_does_not_blank_the_screen(self):
        screen = self.src("weather", "WeatherScreen")
        self.assertIn("catch (Exception e)", screen)
        self.assertIn("Could not read that response.", screen)


class FinalFourAppsContracts(unittest.TestCase):
    """Apps twelve to fifteen: Messages, Music, Scanner and Email.

    These are the last four requested apps. Each ships the half that is buildable without an
    owner-side obligation, and each states on screen which half that is rather than implying
    a capability it does not have:

      Messages  reads and sends SMS through the framework. Play restricts SMS permissions to
                apps whose core function is messaging and requires a declaration form, so
                this ships on the direct-APK path.
      Music     plays the user's own files and URLs the user supplies. No catalogue and no
                partner SDK, which is the licensing constraint respected rather than worked
                around.
      Scanner   assembles picked images into a PDF with the framework PdfDocument, so it adds
                no dependencies. CameraX and ML Kit would add about 50 MB, which a lite
                flavour cannot carry.
      Email     composes and archives locally, sending through the device's own mail app, so
                no credential is stored and no OAuth verification is needed. Receiving over
                IMAP is the half that stays blocked.
    """

    MODS = ("messages", "music", "scanner", "email")
    RULES = {"messages": "MessagesRules", "music": "MusicRules",
             "scanner": "ScannerRules", "email": "EmailRules"}

    def src(self, mod, name):
        return (ROOT / ("feature-%s/src/main/java/app/space/feature/%s/%s.java" % (mod, mod, name))).read_text()

    def gradle(self, mod):
        return "\n".join(l for l in (ROOT / ("feature-%s/build.gradle" % mod)).read_text().splitlines()
                         if not l.strip().startswith("//"))

    def test_all_four_modules_are_declared_and_wired(self):
        settings = (ROOT / "settings.gradle").read_text()
        app_gradle = "\n".join(l for l in (ROOT / "app/build.gradle").read_text().splitlines()
                               if not l.strip().startswith("//"))
        for mod in self.MODS:
            self.assertIn("include ':feature-%s'" % mod, settings)
            self.assertIn("implementation project(':feature-%s')" % mod, app_gradle)
            self.assertIn("namespace 'app.space.feature.%s'" % mod, self.gradle(mod))
            for forbidden in ("project(':app')", "project(':shell')", "project(':feature-"):
                self.assertNotIn(forbidden, self.gradle(mod), "%s must not depend on %s" % (mod, forbidden))

    def test_fifteen_apps_are_registered_in_one_list(self):
        # This is the ONLY place the app count is asserted. It was previously pinned in three
        # tests, so every new app broke all three for the same reason.
        main = java_code_only(read("MainActivity.java"))
        registry = re.search(r"suiteApps\(\)\{return java\.util\.Arrays\.<SuiteApp>asList\(([^)]*)\)", main)
        self.assertIsNotNone(registry, "suiteApps() not found")
        ids = [x.strip() for x in registry.group(1).split(",")]
        self.assertEqual(15, len(ids), ids)
        for mod in self.MODS:
            self.assertIn(mod + "App", ids)
            self.assertEqual(1, main.count("private final SuiteApp %sApp=new %sApp();"
                                           % (mod, mod.capitalize())))

    def test_the_rules_are_pure_java_so_they_are_actually_tested(self):
        for mod in self.MODS:
            body = self.src(mod, self.RULES[mod])
            for forbidden in ("import android.", "import androidx.", "Context", "View"):
                self.assertNotIn(forbidden, body, "%s must stay pure" % self.RULES[mod])

    def test_each_new_app_has_a_jvm_test_suite(self):
        for mod in self.MODS:
            if mod == "documents":
                path = ROOT / ("core-domain/src/test/java/app/space/core/domain/%sTest.java"
                               % self.RULES[mod])
            else:
                path = ROOT / ("feature-%s/src/test/java/app/space/feature/%s/%sTest.java"
                               % (mod, mod, self.RULES[mod]))
            self.assertTrue(path.exists(), "missing %s" % path)
            self.assertIn("@Test", path.read_text())

    def test_a_screen_can_receive_an_activity_result(self):
        # The scanner picks images, and a screen has no activity of its own. Without this the
        # result goes to MainActivity and the picker appears to do nothing.
        screen = (ROOT / "core-suite/src/main/java/app/space/core/suite/Screen.java").read_text()
        self.assertIn("default void onActivityResult(int requestCode, int resultCode, "
                      "android.content.Intent data)", screen)
        main = java_code_only(read("MainActivity.java"))
        # Forwarded BEFORE the RESULT_OK guard, or a cancelled pick never reaches the screen
        # and it stays waiting for a result that will not come.
        self.assertIn("if(req>=90&&req<100&&activeScreen!=null)"
                      "{activeScreen.onActivityResult(req,result,data);return;}", main)
        self.assertTrue(main.index("req>=90") < main.index("if(result!=RESULT_OK||data==null)return;"),
                        "the forward must precede the RESULT_OK guard")
        scanner = self.src("scanner", "ScannerScreen")
        self.assertIn("public void onActivityResult(int requestCode, int resultCode, Intent data)", scanner)
        self.assertIn("PICK_IMAGES = 90", scanner)

    def test_sms_permissions_are_declared_and_the_play_restriction_is_recorded(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn("android.permission.READ_SMS", manifest)
        self.assertIn("android.permission.SEND_SMS", manifest)
        # The restriction must be written down where a reviewer will find it, not discovered
        # at Play review time.
        self.assertIn("core function is messaging", manifest)
        screen = java_code_only(self.src("messages", "MessagesScreen"))
        # Asked for when the feature is used, never at launch.
        self.assertIn("Manifest.permission.READ_SMS", screen)
        self.assertIn("requestPermissions(new String[]{", screen)
        self.assertIn("Google Play restricts SMS", screen)

    def test_a_long_sms_is_sent_multipart_rather_than_truncated(self):
        screen = java_code_only(self.src("messages", "MessagesScreen"))
        # sendTextMessage truncates past one segment silently; divideMessage plus
        # sendMultipartTextMessage is the only correct path.
        self.assertIn("manager.divideMessage(text)", screen)
        self.assertIn("sendMultipartTextMessage(", screen)
        rules = java_code_only(self.src("messages", "MessagesRules"))
        # UCS-2 halves the segment size, so the count shown before sending must reflect it
        # or the user is told a smaller charge than the carrier applies.
        self.assertIn("isSevenBit(body) ? 160 : 70", rules)

    def test_threading_cannot_merge_two_people_or_every_service_sender(self):
        rules = java_code_only(self.src("messages", "MessagesRules"))
        # Ten digits, not nine: a nine-digit tail merges distinct Indian mobiles, so two
        # people would share one thread and read each other's messages.
        self.assertIn("COMPARE_DIGITS = 10", rules)
        # An alphanumeric DLT header has no digits; reducing it to "" would fold every
        # service sender into a single Unknown thread.
        self.assertIn("if (hasLetter) return alphanumeric.toString();", rules)

    def test_the_sms_query_is_capped_and_survives_a_refusing_provider(self):
        screen = java_code_only(self.src("messages", "MessagesScreen"))
        self.assertIn("ROW_LIMIT = 500", screen)
        self.assertIn("LIMIT \" + ROW_LIMIT", screen)
        self.assertIn("cursor.close()", screen)
        self.assertIn("catch (Exception e)", screen)

    def test_only_http_and_https_may_be_streamed(self):
        rules = java_code_only(self.src("music", "MusicRules"))
        # The field is user input from a dialog; accepting file: or content: would let a
        # pasted string read arbitrary local paths.
        self.assertIn('trimmed.startsWith("http://") || trimmed.startsWith("https://")', rules)
        screen = java_code_only(self.src("music", "MusicScreen"))
        self.assertIn("MusicRules.isStreamableUrl(address)", screen)

    def test_the_shuffle_is_seeded_so_the_queue_cannot_move_under_the_finger(self):
        screen = java_code_only(self.src("music", "MusicScreen"))
        self.assertIn("SHUFFLE_SEED = 20260101L", screen)
        self.assertIn("MusicRules.shuffled(playable, SHUFFLE_SEED)", screen)
        rules = java_code_only(self.src("music", "MusicRules"))
        self.assertIn("Collections.shuffle(out, new Random(seed))", rules)

    def test_music_playback_outlives_the_screen_and_dies_with_the_service(self):
        # This test once pinned the opposite rule: release the player in onPause, because a
        # MediaPlayer left running holds the decode session. Sprint 3 replaced the screen-owned
        # player with a foreground service -- leaving the screen must NOT stop the song now;
        # stopping is an explicit transport action that tears the service down.
        screen = java_code_only(self.src("music", "MusicScreen"))
        service = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/PlaybackService.java").read_text())
        self.assertNotIn("private void release()", screen)
        self.assertIn("PlaybackService.setListener(null);", screen)
        self.assertIn("public void onDestroy()", service)
        self.assertIn("releasePlayer();", service)
        self.assertIn("abandonFocus();", service)
        self.assertIn("case MusicActionReceiver.ACTION_STOP:", service)

    def test_the_scanner_adds_no_dependencies(self):
        # CameraX plus ML Kit would add roughly 50 MB, which a lite flavour cannot carry --
        # and integrating a live camera preview with no device to see it on is a gamble.
        # AndroidX arriving in Sprint 4 removes the technical blocker, not the blind one.
        for mod in self.MODS:
            g = self.gradle(mod)
            for forbidden in ("camera", "mlkit", "ml-kit", "androidx"):
                self.assertNotIn(forbidden, g.lower())
        screen = java_code_only(self.src("scanner", "ScannerScreen"))
        self.assertIn("android.graphics.pdf.PdfDocument", self.src("scanner", "ScannerScreen"))
        self.assertIn("new PdfDocument()", screen)
        self.assertIn("MAX_DECODE_EDGE = 1600", screen)

    def test_the_pdf_fit_takes_the_minimum_ratio_so_nothing_is_clipped(self):
        rules = java_code_only(self.src("scanner", "ScannerRules"))
        # Scaling by width alone overflows the sheet height and the framework clips rather
        # than reports, so the bottom of every page would be missing with no error.
        self.assertIn("float scale = Math.min(byWidth, byHeight);", rules)
        self.assertIn("MARGIN = 24f", rules)
        self.assertIn("A4_WIDTH = 595f", rules)
        # A rotated page must be fitted using its rotated size.
        self.assertIn("boolean swap = page.rotation == 90 || page.rotation == 270;", rules)
        self.assertIn("float[] size = rotatedSize(page);", rules)

    def test_picked_uris_take_a_persistable_grant(self):
        screen = java_code_only(self.src("scanner", "ScannerScreen"))
        # Without it the grant expires and the PDF fails to build later with no cause shown.
        self.assertIn("takePersistableUriPermission(", screen)
        self.assertIn("EXTRA_ALLOW_MULTIPLE", screen)
        self.assertIn("getClipData()", screen)

    def test_email_states_that_receiving_is_not_implemented(self):
        # The screen must not imply an inbox that is always empty.
        screen = java_code_only(self.src("email", "EmailScreen"))
        self.assertIn("Receiving mail is not implemented", screen)
        self.assertIn("ACTION_SENDTO", screen)
        self.assertIn("resolveActivity(", screen)
        # No credential is stored, which is the property that makes the mailto path need no
        # OAuth verification. Asserting merely that the word "password" is absent was wrong:
        # the disclosure text on screen says "no password is stored here" and the javadoc
        # explains that IMAP would need an app password. Both are the point, not a leak.
        lowered = screen.lower()
        self.assertNotIn("javax.mail", lowered)
        self.assertNotIn("jakarta.mail", lowered)
        self.assertNotIn("putstring(\"password", lowered)
        self.assertNotIn("putstring(\"email_password", lowered)
        self.assertNotIn("getpassword", lowered)

    def test_the_mailto_uri_encodes_characters_that_would_truncate_it(self):
        rules = java_code_only(self.src("email", "EmailRules"))
        # An unencoded ampersand or newline truncates the URI and silently drops the rest of
        # the message.
        self.assertIn("URLEncoder.encode(value, \"UTF-8\")", rules)
        self.assertIn('.replace("+", "%20")', rules)
        # The address goes through a lighter encoder: full encoding turns the at-sign into
        # %40, which many mail apps will not resolve.
        self.assertIn("encodeAddress(addressOf(recipients.get(i)))", rules)
        self.assertIn("c == '@'", rules)
        # A comma inside a display name must not split the recipient.
        self.assertIn("if (c == ',' && !inQuotes && !inAngle)", rules)

    def test_androidx_is_adopted_by_declaration_and_never_by_accident(self):
        # Sprint 4 flipped a long-standing rule: useAndroidX is now true, because the owner
        # asked for AndroidX adoption and the plan gave it its own sprint. The ban this
        # replaces was load-bearing -- it kept blind-written code off APIs nobody could
        # compile locally -- so the replacement keeps the discipline: the flag is declared
        # exactly once, no module reaches for the dead support library, and a module may
        # import androidx only if its own build.gradle declares the dependency, so an
        # import can never compile by accident on another module's classpath.
        props = (ROOT / "gradle.properties").read_text()
        self.assertIn("android.useAndroidX=true", props)
        for path in sorted(ROOT.rglob("*.java")):
            if ".git" in path.parts:
                continue
            body = path.read_text(encoding="utf-8")
            self.assertNotIn("import android.support.", body,
                             "%s imports the dead support library" % path.name)
            if "import androidx." not in body:
                continue
            module = path
            while module.parent != ROOT and not (module / "build.gradle").exists():
                module = module.parent
            gradle = (module / "build.gradle").read_text(encoding="utf-8")
            self.assertIn("androidx", gradle,
                          "%s imports androidx without declaring the dependency" % path.name)


class RebrandAndLauncherIconContracts(unittest.TestCase):
    """The rebrand and the adaptive launcher icon.

    Both were parked while the apps were built and both block a Play submission: Play
    requires an icon, and a listing called MySpace whose header, About page, widget names and
    notification channels all said TicTic would not survive review or a refund request.

    The rename was deliberately surgical. Notification channel DISPLAY NAMES were changed but
    channel IDs were not: changing an ID creates a new channel, orphans the user's existing
    notification settings, and leaves the dead channel listed in system Settings forever.
    """

    def test_the_launcher_icon_is_adaptive(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn('android:icon="@mipmap/ic_launcher"', manifest)
        self.assertIn('android:roundIcon="@mipmap/ic_launcher_round"', manifest)
        self.assertNotIn("@drawable/ic_app", manifest)
        for name in ("ic_launcher", "ic_launcher_round"):
            path = ROOT / ("app/src/main/res/mipmap-anydpi-v26/%s.xml" % name)
            self.assertTrue(path.exists(), "missing %s" % path)
            body = path.read_text()
            self.assertIn("<adaptive-icon", body)
            self.assertIn("@color/ic_launcher_background", body)
            self.assertIn("@drawable/ic_launcher_foreground", body)
            # Android 13+ themed icons. Without a monochrome layer the launcher shows a
            # full-colour mark in a row of themed ones, or leaves a gap.
            self.assertIn("<monochrome", body)
        self.assertIn('name="ic_launcher_background"',
                      (ROOT / "app/src/main/res/values/colors.xml").read_text())

    def test_the_foreground_mark_sits_inside_the_adaptive_safe_zone(self):
        # The launcher mask crops everything outside a circle of radius 33 in the 108-unit
        # canvas, so a mark drawn edge to edge loses its ring on a circular launcher.
        foreground = (ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml").read_text()
        self.assertIn('android:viewportWidth="108"', foreground)
        self.assertIn('android:scaleX="1.3"', foreground)
        self.assertIn('android:translateX="22.8"', foreground)
        # minSdk 26 means every device supports adaptive icons, so anydpi-v26 alone is
        # sufficient and no density PNG fallbacks are required.
        self.assertNotIn("mipmap-hdpi", (ROOT / "app/build.gradle").read_text())

    def test_no_display_text_still_says_tictic(self):
        """Walks every string literal in :app and in res/values.

        Identifiers are allowed to keep the old name: the package, the R.style themes and the
        private TicTicPrefs class are not user-visible, and renaming a theme means touching
        every R.style reference for no gain a user can see.
        """
        allowed = ("TicTic:Alarm", "TicTic:ReminderTone", "TicTic-weather-")
        offenders = []
        for path in (ROOT / "app/src/main/java").rglob("*.java"):
            body = path.read_text()
            for literal in re.findall(r'"(?:[^"\\]|\\.)*"', body):
                if "TicTic" in literal and not any(keep in literal for keep in allowed):
                    offenders.append("%s: %s" % (path.name, literal[:70]))
        for path in (ROOT / "app/src/main/res/values").glob("*.xml"):
            if path.name == "styles.xml":
                continue  # style identifiers, not display text
            for line in path.read_text().splitlines():
                if "TicTic" in line:
                    offenders.append("%s: %s" % (path.name, line.strip()[:70]))
        self.assertEqual([], offenders)

    def test_notification_channel_ids_were_not_renamed(self):
        # A renamed channel ID orphans the user's notification settings and leaves the old
        # channel in system Settings permanently. The display name changes; the ID must not.
        alarm = (ROOT / "app/src/main/java/app/tictic/clock/AlarmSoundService.java").read_text()
        self.assertIn('"TicTic:Alarm"', alarm)
        self.assertIn('"MySpace alarms"', alarm)
        reminder = (ROOT / "app/src/main/java/app/tictic/clock/ReminderSoundService.java").read_text()
        self.assertIn('"TicTic:ReminderTone"', reminder)

    def test_the_widget_names_the_launcher_shows_are_rebranded(self):
        strings = (ROOT / "app/src/main/res/values/strings.xml").read_text()
        self.assertIn('<string name="widget_clock">MySpace Glass</string>', strings)
        self.assertIn('<string name="widget_date">MySpace Sunrise</string>', strings)
        self.assertIn('<string name="widget_next_alarm">MySpace Wake Desk</string>', strings)
        self.assertNotIn("TicTic", strings)

    def test_the_header_wordmark_and_its_content_description_are_rebranded(self):
        main = java_code_only(read("MainActivity.java"))
        self.assertIn('label("MySpace",23,text,true)', main)
        self.assertIn('setContentDescription("MySpace home")', main)
        self.assertNotIn('label("TicTic"', main)


class DeviceTestReadinessContracts(unittest.TestCase):
    """The four partial apps must survive a real install before the owner tests them.

    Found by reading the four screens against what a device actually does at targetSdk 34,
    not by anything CI reported: three defects that would each have made a working feature
    look broken to a tester, with no crash and no log to point at the cause.
    """

    def manifest(self):
        return (ROOT / "app/src/main/AndroidManifest.xml").read_text()

    def src(self, mod, name):
        return (ROOT / ("feature-%s/src/main/java/app/space/feature/%s/%s.java" % (mod, mod, name))).read_text()

    def test_mailto_is_visible_to_the_package_manager(self):
        manifest = self.manifest()
        # targetSdk 34 with no <queries> means resolveActivity() returns null for a mailto
        # intent even with Gmail installed, so Email reports "no mail app is set up" on every
        # modern device -- breaking the one action the module can perform, silently.
        self.assertIn("<queries>", manifest)
        self.assertIn('android:name="android.intent.action.SENDTO"', manifest)
        self.assertIn('android:scheme="mailto"', manifest)
        self.assertTrue(manifest.index("<queries>") < manifest.index("<application"),
                        "<queries> must be a manifest child, not inside <application>")

    def test_a_screen_can_hear_the_answer_to_a_permission_it_asked_for(self):
        screen = (ROOT / "core-suite/src/main/java/app/space/core/suite/Screen.java").read_text()
        self.assertIn("default void onRequestPermissionsResult(int requestCode, "
                      "String[] permissions, int[] grantResults)", screen)
        main = java_code_only(read("MainActivity.java"))
        self.assertIn("requestCode>=90&&requestCode<100&&activeScreen!=null", main)
        self.assertIn("activeScreen.onRequestPermissionsResult(requestCode,permissions,grantResults)",
                      main)
        # Both requesting screens must react, or the user taps Allow and nothing changes --
        # which reads as the permission having failed rather than the screen not refreshing.
        for mod, cls in (("messages", "MessagesScreen"), ("recorder", "RecorderScreen")):
            body = self.src(mod, cls)
            self.assertIn("public void onRequestPermissionsResult(int requestCode", body)
            self.assertIn("host.invalidate();", body)

    def test_music_can_obtain_audio_on_a_fresh_install(self):
        body = java_code_only(self.src("music", "MusicScreen"))
        # The app-private music folder cannot be written to by any file manager without root,
        # so without a picker a fresh install has exactly one way to get audio in.
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT", body)
        self.assertIn('intent.setType("audio/*")', body)
        self.assertIn("PICK_AUDIO = 93", body)
        self.assertIn("public void onActivityResult(int requestCode, int resultCode, Intent data)", body)
        # Persisted, or every picked track silently stops playing after a process restart.
        self.assertIn("takePersistableUriPermission(", body)
        # A content URI needs the Context overload; the String one handles paths and URLs only.
        # The player moved into PlaybackService when playback learned to outlive the screen.
        service = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/PlaybackService.java").read_text())
        self.assertIn("player.setDataSource(this, Uri.parse(track.source))", service)

    def test_every_screen_request_code_is_in_the_reserved_range(self):
        # The host forwards 90 to 99. A screen using a code outside that range gets its result
        # swallowed by the clock's own handler, and the failure is a picker that does nothing.
        codes = []
        for path in (ROOT).glob("feature-*/src/main/java/app/space/feature/*/*Screen.java"):
            body = path.read_text()
            for match in re.finditer(r"(PICK_IMAGES|PICK_AUDIO|PERMISSION_REQUEST)\s*=\s*(\d+)", body):
                codes.append((path.name, match.group(1), int(match.group(2))))
        self.assertTrue(codes, "no screen request codes found")
        for name, constant, code in codes:
            self.assertTrue(90 <= code < 100,
                            "%s.%s = %d is outside the 90-99 range the host forwards"
                            % (name, constant, code))
        # Two screens must not share a code, or a result is delivered to the wrong one.
        self.assertEqual(len(set(c for _, _, c in codes)), len(codes), codes)

    def test_the_clocks_own_request_codes_do_not_collide_with_screens(self):
        main = java_code_only(read("MainActivity.java"))
        for code in re.findall(r"req==(\d+)", main):
            self.assertFalse(90 <= int(code) < 100,
                             "the clock uses request code %s, which is reserved for screens" % code)


class ApkPublishWorkflowContracts(unittest.TestCase):
    """The owner tests on a real device, so an installable APK has to be reachable.

    Workflow artifacts are only downloadable from the Actions UI and they expire, and they
    cannot be fetched from the review sandbox at all. This repository already keeps installable
    debug builds in artifacts/ with a SHA-256 sidecar and an index, so publishing follows that
    convention rather than inventing one.

    Publishing lives in the push-triggered Android CI workflow, not in a workflow_dispatch one:
    GitHub only exposes workflow_dispatch for files present on the DEFAULT branch, and this
    work happens on a branch that is never merged, so a dispatch-only workflow could be written
    and committed and still be impossible to run. That was built once before being discovered.
    """

    def setUp(self):
        self.path = ROOT / ".github/workflows/android-ci.yml"
        self.assertTrue(self.path.exists(), "android-ci.yml is missing")
        self.yml = self.path.read_text()
        self.assertFalse((ROOT / ".github/workflows/publish-apk.yml").exists(),
                         "a dispatch-only workflow cannot be triggered from this branch")

    def test_it_can_push_to_the_branch(self):
        self.assertIn("contents: write", self.yml)
        self.assertIn("fetch-depth: 0", self.yml)

    def test_it_does_not_commit_on_every_run(self):
        # An APK differs byte-for-byte between builds with no source change, so publishing
        # unconditionally would add a commit per run. The guard is the version-and-code
        # filename already being present.
        self.assertIn('if [ -f "artifacts/$NAME" ]; then', self.yml)
        self.assertIn("[skip ci]", self.yml)
        self.assertIn("if git diff --cached --quiet; then", self.yml)

    def test_it_follows_the_existing_artifacts_convention(self):
        self.assertIn("artifacts/", self.yml)
        self.assertIn(".sha256", self.yml)
        self.assertIn("APK-INDEX.md", self.yml)
        # The name carries the version and versionCode, so two builds of the same versionName
        # cannot overwrite each other silently.
        # The flavour name is part of it now: publishing a build that Play Protect refuses at
        # install would be publishing something nobody can use, and the name is what tells the
        # two apart in artifacts/.
        self.assertIn('NAME="MySpace-v${VERSION}-${CODE}-lite-dist.apk"', self.yml)

    def test_it_fails_loudly_if_no_apk_was_produced(self):
        self.assertIn('if [ -z "$BUILT" ]; then', self.yml)
        self.assertIn("no APK was produced", self.yml)
        self.assertIn("set -euo pipefail", self.yml)

    def test_it_does_not_reintroduce_the_action_that_broke_ci(self):
        # android-actions/setup-android was tried early in this project and broke the build;
        # the runners already ship the SDK.
        #
        # Comment lines are stripped first. The workflow's own comment explains why the action
        # must NOT be used, so grepping the raw file matches the warning and fails the
        # assertion the warning exists to justify. That is the sixth time this trap has
        # appeared in this suite, which is enough to say the rule outright: an assertion that
        # greps whole-file text will eventually match its own documentation.
        live = "\n".join(l for l in self.yml.splitlines() if not l.strip().startswith("#"))
        self.assertNotIn("setup-android", live)


class DistinctGlyphAndAppChromeContracts(unittest.TestCase):
    """Every app gets its own icon, and the header identifies the app on screen.

    Fifteen apps were sharing ten glyphs: Notes and Calendar both borrowed WORLD, Habits,
    Expenses, Music and Recorder all borrowed ACTIVITY, and Email borrowed ALARM. The rail and
    the picker were legible, but apps sharing a glyph were hard to tell apart at a glance --
    which is the entire job of an icon.

    The chrome defect was worse than cosmetic. Over a foreign app the header still showed the
    clock's own route label, so it told the user they were in the clock while another app owned
    the body beneath it.
    """

    def apps(self):
        return sorted(p.parent.name for p in (ROOT).glob("feature-*/src/main/java/app/space/feature/*/*App.java"))

    def glyph_of(self, mod):
        for path in (ROOT / ("feature-" + mod)).rglob("*App.java"):
            match = re.search(r"iconGlyph\(\)\s*\{?\s*return TicIconView\.([A-Z]+);",
                              path.read_text())
            if match:
                return match.group(1)
        return None

    def test_no_two_apps_share_an_icon_glyph(self):
        glyphs = {}
        for mod in self.apps():
            glyph = self.glyph_of(mod)
            self.assertIsNotNone(glyph, "%s declares no glyph" % mod)
            self.assertNotIn(glyph, glyphs, "%s and %s both use %s" % (mod, glyphs.get(glyph), glyph))
            glyphs[glyph] = mod
        self.assertEqual(len(self.apps()), len(glyphs))

    def test_every_glyph_constant_is_declared_and_drawn(self):
        view = (ROOT / "core-design/src/main/java/app/space/core/design/TicIconView.java").read_text()
        for mod in self.apps():
            glyph = self.glyph_of(mod)
            if glyph == "CLOCK":
                continue
            self.assertRegex(view, r"\b%s=\d+" % glyph, "%s is not declared" % glyph)
            # A declared constant with no case falls through to the default circle, so two
            # apps would look identical again and nothing would report it.
            self.assertIn("case %s:" % glyph, view, "%s has no drawing code" % glyph)

    def test_the_header_names_the_app_that_owns_the_body(self):
        main = java_code_only(read("MainActivity.java"))
        self.assertIn("private SuiteApp activeApp;", main)
        self.assertIn("activeApp=app", main)
        self.assertIn("if(activeScreen!=null&&activeApp!=null){TextView sub=label("
                      "activeApp.label().toUpperCase(Locale.ROOT),10,accent,true);", main)
        # Cleared on every path that clears activeScreen, or a stale label would name an app
        # that is no longer on screen.
        # Three paths clear it: opening the clock, the header wordmark, and the in-app bar's
        # Clock button. Every path that clears the screen clears the app too, or a stale label
        # would name an app that is no longer on screen.
        self.assertEqual(main.count("activeApp=null"), 3)
        self.assertEqual(main.count("activeScreen=null"), main.count("activeApp=null"))

    def test_the_settings_gear_is_clock_only(self):
        # It opens clock settings, which do not describe a foreign app's screen.
        main = java_code_only(read("MainActivity.java"))
        self.assertIn("if(activeScreen==null){TicIconView gear=new TicIconView(this,"
                      "TicIconView.SETTINGS,accent);", main)


class AppsInventoryDocContracts(unittest.TestCase):
    """APPS.md is the inventory handed to the owner for on-device testing.

    It is generated from the source rather than typed, and these assertions hold it to that: a
    hand-written inventory of fifteen apps drifts the first time an app is added, and a stale
    inventory is worse than none because it is read as authoritative.
    """

    def setUp(self):
        self.path = ROOT / "APPS.md"
        self.assertTrue(self.path.exists(), "APPS.md is missing")
        self.doc = self.path.read_text()

    def registered(self):
        main = java_code_only(read("MainActivity.java"))
        registry = re.search(r"suiteApps\(\)\{return java\.util\.Arrays\.<SuiteApp>asList\(([^)]*)\)", main)
        return [x.strip().replace("App", "") for x in registry.group(1).split(",")]

    def test_every_registered_app_is_listed(self):
        for app in self.registered():
            self.assertIn("`%s`" % app, self.doc, "%s is registered but not documented" % app)

    def test_the_documented_count_matches_the_registry(self):
        match = re.search(r"\| (\d+) \| \*\*Email\*\*", self.doc)
        self.assertIsNotNone(match, "the app table's last row is not Email")
        self.assertEqual(len(self.registered()), int(match.group(1)))

    def test_the_documented_version_matches_the_build(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        version = re.search(r"versionName '([^']+)'", gradle).group(1)
        code = re.search(r"versionCode (\d+)", gradle).group(1)
        self.assertIn("version **%s** (versionCode %s)" % (version, code), self.doc)

    def test_it_records_what_is_not_in_the_build_and_why(self):
        # The gaps are owner-side obligations. Documenting them is what stops a partial app
        # being read as a finished one.
        for gap in ("Email — receiving", "Messages — Play distribution",
                    "Music — commercial catalogue", "Scanner — live camera capture"):
            self.assertIn(gap, self.doc)

    def test_it_records_which_permissions_are_requested_and_when(self):
        for permission in ("RECORD_AUDIO", "READ_SMS", "SEND_SMS", "INTERNET"):
            self.assertIn(permission, self.doc)
        # The promise the Settings page makes has to be written down where it can be checked.
        self.assertIn("never at launch", self.doc)
        for absent in ("location", "contacts", "camera", "calendar"):
            self.assertIn(absent, self.doc)


class NavigationModesContracts(unittest.TestCase):
    """All three declared navigation modes must actually render, and be reachable.

    NavPrefs stored a mode and buildNav never read it: the only reference to NavMode anywhere in
    MainActivity was inside a comment saying hub mode was not rendered yet. Reordering, pinning
    and hiding worked, so "navigation must be customisable" looked satisfied while a third of it
    was inert -- and inert silently, because choosing a mode that draws nothing looks exactly
    like the setting having been saved.
    """

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))

    def test_buildnav_dispatches_on_the_stored_mode(self):
        self.assertIn("NavMode mode=NavPrefs.mode(this);", self.main)
        self.assertIn("if(mode==NavMode.HUB){buildHubNav();return;}", self.main)
        self.assertIn("if(mode==NavMode.DRAWER){buildDrawerNav();return;}", self.main)
        self.assertIn("buildRailNav();", self.main)

    def test_each_mode_has_its_own_builder(self):
        for builder in ("void buildRailNav(){", "void buildHubNav(){", "void buildDrawerNav(){"):
            self.assertEqual(self.main.count(builder), 1, builder)

    def test_the_rail_body_is_unchanged_by_the_split(self):
        # The rail is the default and the mode every existing user has. Splitting buildNav into
        # a dispatcher must not alter it, so the declaration the older contracts pin survives.
        self.assertEqual(self.main.count("HorizontalScrollView rail=new HorizontalScrollView(this);"), 1)
        self.assertIn("rail.post(()->rail.scrollTo(dp(tab*item),0));", self.main)

    def test_hub_lists_apps_directly_rather_than_behind_a_picker(self):
        hub = self.main[self.main.index("void buildHubNav(){"):self.main.index("void buildAppNav(){")]
        self.assertIn("for(SuiteApp app:availableApps())", hub)
        # Reading availableApps() is what lets a newly registered app appear with no edit here.
        self.assertNotIn("showAppPicker()", hub)
        self.assertIn("openApp(id)", hub)
        self.assertIn("root.addView(scroll,lp(-1,118));", hub)

    def test_drawer_and_hub_share_one_tile_implementation(self):
        # Two hand-rolled tile layouts would drift, and the same destination would read
        # differently depending on which mode the user had chosen. Ten: the definition, one call
        # in hub, two in the drawer list, two in the in-app bar, the picker grid, its recents
        # row, and the lead Apps tile the owner asked to see in the clock app's old first slot.
        self.assertEqual(self.main.count("navTile("), 10)
        hub = self.main[self.main.index("void buildHubNav(){"):self.main.index("void buildAppNav(){")]
        drawer = self.main[self.main.index("private void showDrawer("):]
        self.assertIn("navTile(", hub)
        self.assertIn("navTile(", drawer[:drawer.index("\n    }\n")])

    def test_the_mode_is_choosable_in_settings(self):
        self.assertIn('sectionTitle("Navigation","Choose how destinations are reached', self.main)
        self.assertIn("NavMode[] modes={NavMode.RAIL,NavMode.HUB,NavMode.DRAWER};", self.main)
        self.assertIn("NavPrefs.setMode(MainActivity.this,option);routeCache=null;render();", self.main)
        # Clearing routeCache matters: the mode changes what is drawn, and a stale cache would
        # render the previous style until something else invalidated it.
        self.assertIn("final NavMode currentMode=NavPrefs.mode(this);", self.main)

    def test_the_drawer_handle_has_a_glyph_of_its_own(self):
        view = (ROOT / "core-design/src/main/java/app/space/core/design/TicIconView.java").read_text()
        self.assertRegex(view, r"\bMENU=\d+")
        self.assertIn("case MENU:", view)
        self.assertIn("new TicIconView(this,TicIconView.MENU,accent)", self.main)

    def test_every_mode_honours_hidden_destinations(self):
        # Hiding a destination must hide it in all three styles, or the control only works in
        # the default one and the user cannot tell which they are in.
        for builder in ("void buildRailNav(){", "void buildHubNav(){"):
            start = self.main.index(builder)
            body = self.main[start:start + 2600]
            self.assertIn("visibleRoutes()", body, builder + " ignores hidden destinations")
        drawer = self.main[self.main.index("private void showDrawer("):]
        self.assertIn("visibleRoutes()", drawer[:1200])


class InAppNavigationContracts(unittest.TestCase):
    """Opening an app other than the clock must not leave the user stranded.

    buildNav was gated on activeScreen==null, so while any of the fourteen other apps owned the
    body there was no navigation at all. Reaching a second app meant tapping the wordmark back to
    the clock and starting over: three taps, and the only control on screen was the one that
    takes you home. With fifteen apps installed that is the difference between a suite and
    fifteen dead ends, and it is invisible in every test that renders nothing.
    """

    def setUp(self):
        self.main = java_code_only(read("MainActivity.java"))

    def test_a_nav_bar_is_rendered_in_every_state(self):
        self.assertIn("if(!settings){if(activeScreen==null)buildNav();else buildAppNav();}", self.main)
        self.assertEqual(self.main.count("void buildAppNav(){"), 1)
        bar = self.main[self.main.index("void buildAppNav(){"):]
        bar = bar[:bar.index("\n    }\n")]
        self.assertIn("root.addView(bar,lp(-1,64));", bar, "the in-app bar is never added to the tree")

    def test_the_in_app_bar_offers_home_and_the_switcher(self):
        bar = self.main[self.main.index("void buildAppNav(){"):]
        bar = bar[:bar.index("\n    }\n")]
        self.assertIn('navTile("Clock",TicIconView.CLOCK,false)', bar)
        self.assertIn('navTile("Apps",TicIconView.APPS,false)', bar)
        self.assertIn("showAppPicker()", bar)
        # Going home must clear the foreign screen and its owner, or the header keeps naming an
        # app that is no longer on screen.
        self.assertIn("activeScreen=null;activeApp=null;", bar)

    def test_the_in_app_bar_does_not_offer_clock_destinations(self):
        # They are routes into the clock. Offering them over another app would move the user
        # into the clock without saying so -- the same wrong-context failure as the settings gear.
        bar = self.main[self.main.index("void buildAppNav(){"):]
        bar = bar[:bar.index("\n    }\n")]
        self.assertNotIn("visibleRoutes()", bar)
        self.assertNotIn("showRoute(route", bar)

    def test_the_apps_switcher_does_not_use_the_settings_gear(self):
        # A gear labelled "Apps" reads as settings. The switcher now has its own glyph.
        self.assertIn("TicIconView appsIcon=new TicIconView(this,TicIconView.APPS,muted)", self.main)
        view = (ROOT / "core-design/src/main/java/app/space/core/design/TicIconView.java").read_text()
        self.assertRegex(view, r"\bAPPS=\d+")
        self.assertIn("case APPS:", view)


class CalendarSettingsAndRingingContracts(unittest.TestCase):
    """The owner: "Basic settings are also not there." And a reminder that never rings is a lie,
    so the settings sheet and the alarm half of reminders are pinned together -- the sheet stores
    three choices, and the receiver is the reason one of them means anything.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-calendar/src/main/java/app/space/feature/calendar/CalendarScreen.java").read_text())
        self.store = java_code_only(
            (ROOT / "feature-calendar/src/main/java/app/space/feature/calendar/CalendarStore.java").read_text())
        self.receiver = java_code_only(
            (ROOT / "feature-calendar/src/main/java/app/space/feature/calendar/EventReminderReceiver.java").read_text())
        self.manifest = (
            ROOT / "feature-calendar/src/main/AndroidManifest.xml").read_text()
        self.boot = java_code_only(
            (ROOT / "app/src/main/java/app/tictic/clock/BootReceiver.java").read_text())

    def test_the_settings_sheet_stores_week_start_open_view_and_default_reminder(self):
        self.assertIn("private void settings(final Context context, final UiKit ui)", self.screen)
        self.assertIn('"Week starts: device default", "Week starts: Sunday",', self.screen)
        self.assertIn("CalendarStore.setFirstDay(context,", self.screen)
        self.assertIn("CalendarStore.setDefaultView(context, openOn.getSelectedItemPosition());", self.screen)
        self.assertIn("CalendarStore.setDefaultReminder(context,", self.screen)
        for key in ("calendar_first_day", "calendar_default_view", "calendar_default_reminder"):
            self.assertIn(key, self.store)

    def test_stored_defaults_drive_the_next_open(self):
        self.assertIn("if (!modeTouched) viewMode = CalendarStore.getDefaultView(context);", self.screen)
        self.assertIn("final int firstDay = setting > 0 ? setting : locale.getFirstDayOfWeek();", self.screen)
        self.assertIn("int preset = CalendarStore.getDefaultReminder(context);", self.screen)

    def test_saving_arms_the_alarm_and_deleting_disarms_it(self):
        self.assertIn("EventReminderReceiver.schedule(context, id, t, at, reminder);", self.screen)
        self.assertIn("EventReminderReceiver.cancel(context, event.id);", self.screen)
        self.assertIn("alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, fire);", self.receiver)
        self.assertIn("long fireAt = when - reminderMinutes * 60000L;", self.receiver)
        self.assertIn("if (fireAt <= System.currentTimeMillis()) return;", self.receiver)

    def test_the_receiver_is_declared_and_survives_a_reboot(self):
        self.assertIn('android:name=".EventReminderReceiver"', self.manifest)
        self.assertIn('android:name="app.space.feature.calendar.EVENT_REMINDER"', self.manifest)
        self.assertIn("EventReminderReceiver.rescheduleAll(c);", self.boot)
        self.assertIn("public static void rescheduleAll(Context context)", self.receiver)

    def test_the_calendar_receiver_stays_framework_only(self):
        # AndroidX is adopted repo-wide as of Sprint 4, but this receiver has no reason to
        # grow a dependency: framework notifications are all it posts, and every dependency
        # a lite flavour carries is a dependency the owner installs.
        self.assertNotIn("import androidx.", self.receiver)
        self.assertNotIn("NotificationCompat", self.receiver)


class CalendarParityContracts(unittest.TestCase):
    """The owner: "what if I need to change the year quickly. Basic settings are also not there.
    Recreate popular calender app." A month grid with month-only paging is the answer to none of
    that; these pin the year jump, the three views and the reminder the editor now stores.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-calendar/src/main/java/app/space/feature/calendar/CalendarScreen.java").read_text())
        self.rules = java_code_only(
            (ROOT / "feature-calendar/src/main/java/app/space/feature/calendar/CalendarRules.java").read_text())

    def test_the_year_is_a_button_and_the_arrows_jump_twelve_months(self):
        self.assertIn("private void yearPicker(final Context context, final UiKit ui)", self.screen)
        self.assertIn("picker.setMinValue(1970);", self.screen)
        self.assertIn("@Override public void onClick(View v) { page(-12); }", self.screen)
        self.assertIn("@Override public void onClick(View v) { page(12); }", self.screen)

    def test_three_views_and_a_today_button(self):
        self.assertIn('String[] modeLabels = {"Month", "Week", "Day"};', self.screen)
        self.assertIn('TextView today = ui.pillButton("Today"', self.screen)
        self.assertIn("private void buildWeek(", self.screen)

    def test_days_carry_a_dot_when_they_hold_something(self):
        self.assertIn("java.util.HashSet<Integer> busy = new java.util.HashSet<>();", self.screen)
        self.assertIn("dot.setBackground(ui.shape(busy.contains(day) ? ui.palette().accent : 0x00000000, 3));", self.screen)

    def test_the_editor_stores_a_time_and_a_reminder_offset(self):
        self.assertIn("time.setIs24HourView(true);", self.screen)
        self.assertIn("dayFrom + time.getHour() * 3600000L + time.getMinute() * 60000L", self.screen)
        self.assertIn("static final int[] REMINDER_OFFSETS = {-1, 5, 10, 30, 60};", self.screen)
        self.assertIn("public final int reminderMinutes;", self.rules)

    def test_week_math_lives_in_the_rules_where_the_jvm_can_reach_it(self):
        self.assertIn("public static long startOfWeek(", self.rules)
        self.assertIn("public static int dayOfMonth(", self.rules)
        self.assertIn("public static String timeLabel(", self.rules)


class ScannerCropEnhanceContracts(unittest.TestCase):
    """CamScanner's loop is capture-crop-enhance-PDF. Capture shipped last slice and the PDF
    writer already existed; these pin the middle two: a four-handle crop frame whose corners
    live normalised, the perspective map applied through the rules' canonical ordering, and
    enhance modes whose colour matrices are exact, JVM-tested numbers rather than vibes.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-scanner/src/main/java/app/space/feature/scanner/ScannerScreen.java").read_text())
        self.rules = java_code_only(
            (ROOT / "feature-scanner/src/main/java/app/space/feature/scanner/ScannerRules.java").read_text())

    def test_the_editor_opens_from_the_page_row_and_owns_the_screen(self):
        self.assertIn("addStep(row, ui, \"\\u2702\", true,", self.screen)
        self.assertIn("if (editingPage != null) {", self.screen)
        self.assertIn("private View buildEditor(final Context context, final UiKit ui)", self.screen)

    def test_corners_are_draggable_and_normalised(self):
        self.assertIn("private final class CropView extends View", self.screen)
        self.assertIn("private final float[] corners =", self.screen)
        self.assertIn("ScannerRules.clamp01((x - content.left) / content.width())", self.screen)
        self.assertIn("private int nearestHandle(float x, float y)", self.screen)

    def test_the_perspective_map_goes_through_the_ordered_quad(self):
        self.assertIn("float[] ordered = ScannerRules.orderQuad(crop.quadPixels());", self.screen)
        self.assertIn("int[] size = ScannerRules.correctedSize(ordered);", self.screen)
        self.assertIn("map.setPolyToPoly(ordered, 0,", self.screen)
        self.assertIn("public static float[] orderQuad(float[] pts)", self.rules)

    def test_enhance_modes_are_pinned_numbers_not_vibes(self):
        self.assertIn("public static float[] correctionMatrix(int mode)", self.rules)
        self.assertIn("0.2126f, 0.7152f, 0.0722f", self.rules)
        self.assertIn("1.4f, 0, 0, 0, -50f,", self.rules)
        self.assertIn("String[] modeLabels = {\"Original\", \"Gray\", \"Magic colour\"};", self.screen)
        self.assertIn("ScannerRules.correctionMatrix(enhanceMode)", self.screen)

    def test_switching_modes_keeps_the_dragged_corners(self):
        # A rebuild that resets the crop frame teaches the owner to hate the enhance row.
        self.assertIn("editQuad = crop.quad();", self.screen)
        self.assertIn("crop.setQuad(editQuad);", self.screen)

    def test_the_straightened_page_replaces_the_old_one_by_id(self):
        self.assertIn("ScannerRules.Page updated = ScannerRules.resourced(editingPage,", self.screen)
        self.assertIn("if (pages.get(i).id.equals(editingPage.id)) { pages.set(i, updated); break; }", self.screen)
        self.assertIn("public static Page resourced(Page page, String newUri, int width, int height)", self.rules)


class ScannerCaptureContracts(unittest.TestCase):
    """Defect 20: "Scanner does not capture like CamScanner." The capture half of the
    capture-crop-enhance-PDF loop: the system camera writes into a private inbox. Sprint 3
    did that through a hand-rolled writable provider; Sprint 4 adopted AndroidX and the
    inbox URI is minted by the standard FileProvider, whose paths table is the containment
    rule. No CAMERA permission, then or now.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-scanner/src/main/java/app/space/feature/scanner/ScannerScreen.java").read_text())
        self.share = java_code_only(
            (ROOT / "core-data/src/main/java/app/space/core/data/FileShare.java").read_text())
        self.manifest = (ROOT / "feature-scanner/src/main/AndroidManifest.xml").read_text()
        self.paths = (
            ROOT / "core-data/src/main/res/xml/myspace_paths.xml").read_text()

    def test_the_capture_destination_is_minted_by_the_standard_provider(self):
        self.assertIn("public static Uri newCapture(Context context)", self.share)
        self.assertIn("public static File scannerInbox(Context context)", self.share)
        self.assertIn('new File(context.getFilesDir(), "scanner" + File.separator + "inbox")', self.share)
        # The inbox lives under getFilesDir(), which the paths table is the only rule for.
        self.assertIn('<files-path name="files" path="." />', self.paths)
        self.assertIn("app.space.core.data.FileShare.newCapture(context)", self.screen)

    def test_package_visibility_cannot_make_the_camera_button_lie(self):
        # API 30+: resolveActivity returns null without this, on devices that have a camera.
        self.assertIn('<action android:name="android.media.action.IMAGE_CAPTURE" />', self.manifest)
        self.assertIn("intent.resolveActivity(context.getPackageManager())", self.screen)

    def test_the_capture_round_trip_lands_the_page_in_the_list(self):
        self.assertIn("private static final int TAKE_PHOTO = 91;", self.screen)
        self.assertIn("intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, destination);", self.screen)
        self.assertIn("Intent.FLAG_GRANT_WRITE_URI_PERMISSION", self.screen)
        self.assertIn("if (requestCode == TAKE_PHOTO) {", self.screen)
        self.assertIn("ScannerRules.Page page = measure(photoContext, captured);", self.screen)

    def test_the_inbox_empties_once_the_pdf_holds_the_pages(self):
        self.assertIn("private void cleanInbox(Context context, List<ScannerRules.Page> selected)", self.screen)
        self.assertIn("cleanInbox(context, selected);", self.screen)
        self.assertIn("page.sourceUri.contains(app.space.core.data.FileShare.AUTHORITY_SUFFIX)", self.screen)

    def test_no_camera_permission_is_declared_for_delegated_capture(self):
        # Delegating to the system camera needs no CAMERA permission; declaring it would
        # force a runtime prompt every install has to answer for nothing.
        self.assertNotIn("android.permission.CAMERA", self.manifest)
        app_manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertNotIn("android.permission.CAMERA", app_manifest)


class MusicPlaybackServiceContracts(unittest.TestCase):
    """The plan's music player: "queue with shuffle/repeat, seek bar with position, foreground
    service with a notification transport so playback survives leaving the app." Surviving the
    screen is the whole point, so the delegation lines and the onPause that must NOT stop audio
    are pinned as hard as the service's own guard rails.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/MusicScreen.java").read_text())
        self.service = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/PlaybackService.java").read_text())
        self.receiver = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/MusicActionReceiver.java").read_text())
        self.manifest = (ROOT / "feature-music/src/main/AndroidManifest.xml").read_text()

    def test_the_service_and_its_buttons_are_declared(self):
        self.assertIn('android:name=".PlaybackService"', self.manifest)
        self.assertIn('android:foregroundServiceType="mediaPlayback"', self.manifest)
        self.assertIn('android:name=".MusicActionReceiver"', self.manifest)
        self.assertIn('android:name="app.space.feature.music.TOGGLE"', self.manifest)

    def test_the_notification_carries_a_transport(self):
        self.assertIn('"myspace_music_v1"', self.service)
        self.assertIn("NotificationManager.IMPORTANCE_LOW", self.service)
        self.assertIn('android.R.drawable.ic_media_previous, "Previous"', self.service)
        self.assertIn('android.R.drawable.ic_media_next, "Next"', self.service)
        self.assertIn('.setOngoing(playing)', self.service)

    def test_a_queue_of_broken_files_cannot_loop_forever(self):
        # Every open failure advances; without this guard a dead queue spins the battery.
        self.assertIn("private void skipOrFailQuietly()", self.service)
        self.assertIn("if (queue.isEmpty() || failures > queue.size())", self.service)
        self.assertIn("failures = 0;", self.service)

    def test_audio_focus_pauses_instead_of_fighting(self):
        self.assertIn("AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)", self.service)
        self.assertIn("AUDIOFOCUS_LOSS_TRANSIENT", self.service)

    def test_the_screen_delegates_and_never_kills_playback_on_pause(self):
        self.assertIn("PlaybackService.play(context, queue, position);", self.screen)
        self.assertIn("PlaybackService.toggle(context);", self.screen)
        self.assertIn("PlaybackService.step(context, direction);", self.screen)
        self.assertNotIn("private void release()", self.screen)
        self.assertIn("PlaybackService.setListener(null);", self.screen)
        # onResume wires the repaint listener; onPause unwires it and nothing else about audio.
        self.assertIn("PlaybackService.setListener(new PlaybackService.StateListener()", self.screen)

    def test_the_seek_bar_updates_in_place_instead_of_rebuilding(self):
        self.assertIn("private final Runnable seekTicker = new Runnable()", self.screen)
        self.assertIn("seekView.setProgress(position);", self.screen)
        self.assertIn("PlaybackService.seekTo(context, progress);", self.screen)
        self.assertIn("main.postDelayed(this, 500L);", self.screen)
        # Rebuilding per tick is the blink defect; the ticker touches views only.
        self.assertNotIn("host.invalidate();\n            main.postDelayed(this, 500L);", self.screen)


class MusicLibraryContracts(unittest.TestCase):
    """Defect 19: "music player is worse than before." A player with no library is the reason:
    it could only see files added by hand. MediaStore scan, the permission ask, the render cap
    and the not-removable library rows are the load-bearing lines; the rules behind them are
    JVM-tested in feature-music.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/MusicScreen.java").read_text())
        self.rules = java_code_only(
            (ROOT / "feature-music/src/main/java/app/space/feature/music/MusicRules.java").read_text())
        self.manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()

    def test_the_library_is_scanned_from_mediastore(self):
        self.assertIn("private static List<MusicRules.Track> scanMediaStore(Context context)", self.screen)
        self.assertIn("android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI", self.screen)
        self.assertIn("android.provider.MediaStore.Audio.Media.IS_MUSIC + \"!=0\"", self.screen)
        self.assertIn("if (canScan) library.addAll(scanMediaStore(context));", self.screen)
        self.assertIn('return "content://media/external/audio/media/" + mediaStoreId;', self.rules)

    def test_the_permission_is_declared_capped_and_asked_with_a_reason(self):
        self.assertIn('android:name="android.permission.READ_MEDIA_AUDIO"', self.manifest)
        self.assertIn('android:name="android.permission.READ_EXTERNAL_STORAGE"', self.manifest)
        self.assertIn('android:maxSdkVersion="32"', self.manifest)
        self.assertIn('"Grant music access"', self.screen)
        self.assertIn("Without it the player can only see files you add by hand.", self.screen)
        self.assertIn("public void onRequestPermissionsResult(", self.screen)

    def test_ten_thousand_songs_cannot_hang_the_ui_thread(self):
        self.assertIn("public static final int MAX_RENDER = 500;", self.rules)
        self.assertIn("int rendered = Math.min(queue.size(), MusicRules.MAX_RENDER);", self.screen)
        self.assertIn('"Showing the first " + rendered + " of " + queue.size()', self.screen)

    def test_library_rows_do_not_offer_a_remove_that_means_nothing(self):
        self.assertIn("public static boolean isLibrarySource(String source)", self.rules)
        self.assertIn("&& !MusicRules.isLibrarySource(track.source));", self.screen)
        self.assertIn("MusicRules.librarySubtitle(track)", self.screen)


class RecorderClosureAndSharingContracts(unittest.TestCase):
    """Defect 15 (recorded file would not play) was instrumented, not fixed, and the owner's
    log still has not arrived. This closes what can be closed blind: the recorder proves at
    save time that the device can decode its own recording, names a silent media stream
    instead of letting it read as broken, and grows the rename and share the plan asked for.
    Sharing went through two eras: Sprint 3 hand-rolled a provider because useAndroidX=false;
    Sprint 4 adopted AndroidX and the standard FileProvider replaced it -- the pins above the
    fold are the Sprint 4 shape, and the rename/mime rules stay JVM-tested where providers
    cannot be.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-recorder/src/main/java/app/space/feature/recorder/RecorderScreen.java").read_text())
        self.share = java_code_only(
            (ROOT / "core-data/src/main/java/app/space/core/data/FileShare.java").read_text())
        self.rules = java_code_only(
            (ROOT / "core-data/src/main/java/app/space/core/data/ShareRules.java").read_text())
        self.manifest = (
            ROOT / "core-data/src/main/AndroidManifest.xml").read_text()
        self.paths = (
            ROOT / "core-data/src/main/res/xml/myspace_paths.xml").read_text()

    def test_the_standard_provider_is_declared_once_for_the_whole_suite(self):
        self.assertIn('android:name="androidx.core.content.FileProvider"', self.manifest)
        self.assertIn('android:authorities="${applicationId}.myspace_files"', self.manifest)
        self.assertIn('android:exported="false"', self.manifest)
        self.assertIn('android:grantUriPermissions="true"', self.manifest)
        self.assertIn('android:name="android.support.FILE_PROVIDER_PATHS"', self.manifest)
        gradle = (ROOT / "core-data/build.gradle").read_text()
        self.assertIn("implementation 'androidx.core:core:1.12.0'", gradle)

    def test_containment_is_the_paths_table_and_nothing_else(self):
        # One rule for what a URI may expose: everything under getFilesDir(), nothing beyond.
        self.assertIn('<files-path name="files" path="." />', self.paths)
        self.assertNotIn("external-path", self.paths)
        self.assertNotIn("root-path", self.paths)
        self.assertIn("FileProvider.getUriForFile(context, authority(context), file)", self.share)

    def test_share_grants_read_and_names_a_mime(self):
        self.assertIn("Intent.FLAG_GRANT_READ_URI_PERMISSION", self.share)
        self.assertIn("send.setType(ShareRules.mimeFor(file.getName()));", self.share)
        self.assertIn('return "application/octet-stream";', self.rules)
        self.assertIn("FileShare.shareIntent(context, target,", self.screen)

    def test_the_sharing_rules_are_jvm_tested_where_they_can_be_reached(self):
        test = (ROOT / "core-data/src/test/java/app/space/core/data/ShareRulesTest.java").read_text()
        self.assertIn("mimeLookupHasAFloorNotAHole", test)
        self.assertIn("renamesKeepTheExtensionAndRefuseRubbish", test)
        self.assertIn("sanitiseDropsWhatCannotTravelInAFileName", test)
        gradle = (ROOT / "core-data/build.gradle").read_text()
        self.assertIn("testImplementation 'junit:junit:4.13.2'", gradle)

    def test_the_recorder_proves_its_own_recordings_decode(self):
        self.assertIn("private void selfTest(Context context, File file)", self.screen)
        self.assertIn("selfTest(context, renamed);", self.screen)
        self.assertIn('"selftest FAIL "', self.screen)
        self.assertIn('"selftest ok "', self.screen)

    def test_silent_playback_is_named_instead_of_mysterious(self):
        self.assertIn("private void logMediaVolume(Context context)", self.screen)
        self.assertIn("if (level == 0)", self.screen)
        self.assertIn("ADJUST_RAISE", self.screen)
        self.assertIn("player.setOnErrorListener(", self.screen)
        self.assertIn("USAGE_MEDIA", self.screen)

    def test_rename_keeps_the_index_and_the_extension(self):
        self.assertIn("private void renameDialog(final Context context,", self.screen)
        self.assertIn("ShareRules.renamedFile(", self.screen)
        self.assertIn("private void renameIndex(Context context, String oldName, String newName)", self.screen)
        self.assertIn("if (to.exists())", self.screen)
        self.assertIn("private void forgetIndex(Context context, String name)", self.screen)
        self.assertIn("forgetIndex(context, item.fileName);", self.screen)


class GoogleNotesParityContracts(unittest.TestCase):
    """The owner's line: "For notes, recreate google notes." Google Notes is a wall of coloured
    cards with pins, an archive drawer and checklists -- not a list of titled rows, which is what
    this was. These pin the pieces of that parity, structure here and behaviour in the JVM suite.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-notes/src/main/java/app/space/feature/notes/NotesScreen.java").read_text())
        self.rules = java_code_only(
            (ROOT / "feature-notes/src/main/java/app/space/feature/notes/NotesRules.java").read_text())

    def test_the_wall_is_a_two_column_grid_of_coloured_cards(self):
        self.assertIn("if (i % 2 == 0) {", self.screen)
        self.assertIn("card.setBackground(ui.shape(noteColor(ui, note.color), 16));", self.screen)
        self.assertIn("public static final int COLOR_COUNT = 7;", self.rules)
        self.assertIn("ui.palette().dark ? dark[i] : light[i]", self.screen)

    def test_pins_reach_the_top_and_the_archive_files_rather_than_deletes(self):
        self.assertIn("if (a.pinned != b.pinned) return a.pinned ? -1 : 1;", self.rules)
        self.assertIn("public static List<Note> archived(List<Note> notes)", self.rules)
        self.assertIn('ui.pillButton(showArchived ? "Notes" : "Archive"', self.screen)

    def test_checklists_live_in_the_note_text_so_they_survive_everything(self):
        # Plain-text markers mean export, search and sync-free storage all keep working.
        self.assertIn('line.startsWith("[ ] ")', self.rules)
        self.assertIn("public static String toggleChecklistLineAt(String body, int lineIndex)", self.rules)
        self.assertIn("NotesRules.toggleChecklistLineAt(text, line)", self.screen)

    def test_search_submits_instead_of_rebuilding_per_keystroke(self):
        # A search box that loses focus after one character is a search box nobody uses.
        self.assertIn("search.setOnEditorActionListener(", self.screen)
        self.assertNotIn("search.addTextChangedListener(", self.screen)

    def test_leaving_the_editor_saves_and_an_empty_new_note_is_discarded(self):
        self.assertIn("private void saveDraft(Context context)", self.screen)
        self.assertIn("if (title.isEmpty() && bodyText.trim().isEmpty()) return;", self.screen)


class DocumentViewerContracts(unittest.TestCase):
    """The owner withdrew the Documents app and folded its viewing into Files: "just add that
    into files app". A spreadsheet must render as a table, an image as an image, a PDF as pages --
    inside the files browser, with no app of their own.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-files/src/main/java/app/space/feature/files/FilesScreen.java").read_text())

    def test_sheets_render_as_tables_through_the_rfc4180_decoder(self):
        self.assertIn("private View sheetTable(", self.screen)
        self.assertIn("app.space.core.domain.DocumentRules.decodeSheet(text)", self.screen)
        # Bounded rendering: a ten-thousand-row export must not become a ten-thousand-view layout.
        self.assertIn("if (shown >= 200) {", self.screen)

    def test_images_are_downscaled_before_they_are_decoded(self):
        # A 12-megapixel photo decoded at full size is an out-of-memory crash on a cheap phone.
        self.assertIn("o.inJustDecodeBounds = true;", self.screen)
        self.assertIn("o.inSampleSize = scale;", self.screen)

    def test_pdfs_render_through_the_framework_without_androidx(self):
        self.assertIn("android.graphics.pdf.PdfRenderer", self.screen)
        self.assertIn("RENDER_MODE_FOR_DISPLAY", self.screen)
        # The renderer holds a file descriptor; leaving it open across navigation leaks it.
        self.assertIn("@Override public void onPause() { closePdf(); }", self.screen)

    def test_the_csv_contract_moved_with_the_code(self):
        rules = (ROOT / "core-domain/src/main/java/app/space/core/domain/DocumentRules.java").read_text()
        self.assertIn("package app.space.core.domain;", rules)
        screen = (ROOT / "feature-documents/src/main/java/app/space/feature/documents/DocumentsScreen.java").read_text()
        self.assertIn("import app.space.core.domain.DocumentRules;", screen)


class CategoryBrowserContracts(unittest.TestCase):
    """The owner's ask for Files was twofold: a Notepad-grade editor (pinned above) and "recreate
    samsung files". That app opens on categories with search and sort, not on a bare folder tree,
    so the top level here is categories and the folder browser sits one row down.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-files/src/main/java/app/space/feature/files/FilesScreen.java").read_text())
        self.rules = (ROOT / "feature-files/src/main/java/app/space/feature/files/FileRules.java").read_text()

    def test_the_top_level_is_categories_with_a_storage_row(self):
        self.assertIn("if (!browsing) return inScroll(buildHome(context, ui));", self.screen)
        self.assertIn('"Images", "Videos", "Audio", "Documents", "Sheets", "Other"', self.screen)
        self.assertIn('ui.label("Internal storage", 15, ui.palette().text, true)', self.screen)

    def test_search_covers_every_folder_and_sort_has_three_modes(self):
        self.assertIn("public static boolean matchesQuery(", self.rules)
        self.assertIn("public static List<Entry> sortBy(", self.rules)
        self.assertIn("String[] labels = {\"Name\", \"Date\", \"Size\"};", self.screen)
        # The scan behind categories and search must be bounded, or a full render walks everything.
        self.assertIn("if (depth > 6 || out.size() > 1500) return;", self.screen)

    def test_folders_stay_on_top_in_every_sort_mode(self):
        # A folder sinking between two large files makes the list unscannable; the rule lives in
        # FileRules so the JVM test, not a screenshot, is what guards it.
        self.assertIn("if (a.directory != b.directory) return a.directory ? -1 : 1;", self.rules)

    def test_category_pages_keep_the_edit_action_and_the_guards(self):
        self.assertIn("EditorRules.isEditableName(f.entry.name)", self.screen)
        self.assertIn("openEditor(context, f.path);", self.screen)


class TextEditorContracts(unittest.TestCase):
    """The owner's verdict on Files' text creation: "it's not editor, it's like just a notes.
    Take reference of note in windows and recreate as much as possible." These pin the shape of
    the editor that answers that: a page, not a dialog, with the modes Notepad is remembered for.
    """

    def setUp(self):
        self.screen = java_code_only(
            (ROOT / "feature-files/src/main/java/app/space/feature/files/FilesScreen.java").read_text())
        self.rules = (ROOT / "feature-files/src/main/java/app/space/feature/files/EditorRules.java").read_text()

    def test_the_editor_is_a_page_not_a_dialog(self):
        # A create dialog with a three-line EditText is the notes stub that was rejected. The
        # editor owns the whole screen and the text owns the editor.
        self.assertIn("private View buildEditor(", self.screen)
        self.assertIn("if (editingPath != null) return buildEditor(context, ui);", self.screen)
        self.assertIn("new LinearLayout.LayoutParams(-1, 0, 1f)", self.screen)

    def test_the_notepad_view_modes_exist(self):
        self.assertIn("text.setHorizontallyScrolling(!wrap);", self.screen)
        self.assertIn("android.graphics.Typeface.MONOSPACE", self.screen)

    def test_save_and_save_as_are_both_reachable(self):
        self.assertIn("setOnLongClickListener", self.screen)
        self.assertIn('setTitle("Save as")', self.screen)
        self.assertIn("private void writeEditor(", self.screen)

    def test_undo_groups_keystrokes_instead_of_letters(self):
        # One press per mistake is the expectation; one press per character is an insult.
        self.assertIn("now - lastUndoPush > 1500", self.screen)
        self.assertIn("while (undoStack.size() > 25) undoStack.removeLast();", self.screen)

    def test_find_and_replace_are_literal_and_wrap(self):
        self.assertIn("public static int nextOccurrence(", self.rules)
        self.assertIn("public static int previousOccurrence(", self.rules)
        self.assertIn("public static String replaceAllLiteral(", self.rules)
        # Wrapping: a find that dies at the bottom of the file reads as a broken find.
        self.assertIn("return text.indexOf(needle);", self.rules)
        # Stepping past each match: how replace-all of "a" with "aa" terminates.
        self.assertIn("from = at + needle.length();", self.rules)

    def test_text_files_open_into_the_editor_and_creation_lands_in_it(self):
        self.assertIn("EditorRules.isEditableName(entry.name)", self.screen)
        self.assertIn("openEditor(context, FileRules.join(relative, entry.name));", self.screen)
        self.assertIn("openEditor(context, FileRules.join(folder, wanted));", self.screen)

    def test_the_status_line_names_caret_and_dirt(self):
        self.assertIn("public static String statusLabel(", self.rules)
        self.assertIn("modified", self.screen)


class FilesCanCreateContracts(unittest.TestCase):
    """Files was a read-only browser over app-private storage, which is empty on a fresh install.

    The summary, the list and the empty state all reported that accurately, and the result was
    still an app that appeared to do nothing. Other apps write here only when they produce
    something, so nothing exists until a recording or a scan is saved.
    """

    def setUp(self):
        self.body = java_code_only(
            (ROOT / "feature-files/src/main/java/app/space/feature/files/FilesScreen.java").read_text())

    def test_there_is_a_create_action(self):
        self.assertIn('ui.pillButton("New text file"', self.body)
        self.assertIn("promptNewFile(context, ui, root, folder)", self.body)
        self.assertIn("new FileOutputStream(target)", self.body)

    def test_the_create_path_uses_the_same_guards_as_the_browser(self):
        # The one place this screen writes must not become the one place it can escape the root.
        self.assertIn("FileRules.isSafeName(wanted)", self.body)
        self.assertIn("FileRules.isInside(folder)", self.body)
        self.assertIn("target.exists()", self.body)

    def test_dialogs_use_the_two_argument_builder_so_they_are_fitted(self):
        # AppAlertBuilder(Context, UiKit) is the only constructor. The UiKit is what carries the
        # palette into the dialog, and AppAlertBuilder.show() is the single funnel through which
        # DialogFit clamps every dialog in the suite. Building one without a UiKit does not
        # compile, which is the cheapest possible guard -- but only if every call site is checked,
        # and a grep across all feature modules is how this one was caught after CI named it.
        import subprocess
        bad = subprocess.run(
            ["grep", "-rhoE", r"new AppAlertBuilder\([^,)]*\)", "feature-calculator", "feature-clock",
             "feature-notes", "feature-habits", "feature-expenses", "feature-calendar", "feature-tasks",
             "feature-weather", "feature-recorder", "feature-files", "feature-documents",
             "feature-messages", "feature-music", "feature-scanner", "feature-email"],
            capture_output=True, text=True, cwd=str(ROOT)).stdout.strip()
        self.assertEqual(bad, "", "AppAlertBuilder call sites missing the UiKit: " + bad)

    def test_the_stream_is_closed_on_every_path(self):
        create = self.body[self.body.index("private void promptNewFile("):]
        create = create[:create.index("    @Override public void onResume()")]
        self.assertIn("} finally {", create)
        self.assertIn("out.close();", create)

    def test_the_empty_state_does_not_assume_another_app_is_installed(self):
        # The suite is modular: a build may not contain the apps that would write here, so the
        # copy offers them as a possibility rather than naming them as present.
        self.assertIn("Nothing here yet. Create a text file below", self.body)
        self.assertNotIn("This folder is empty.", self.body)
        self.assertIn("appear here too", self.body)


class SideloadableBuildContracts(unittest.TestCase):
    """A build that cannot be installed is worse than a feature that is missing.

    Google Play Protect refuses -- not warns about -- any APK installed outside the Play Store
    that declares READ_SMS or SEND_SMS. While the manifest carried them, every sideloaded build
    of the suite was blocked at install, so the Messages app took all fifteen apps down with it.
    Found on a real device, by the owner, from a screenshot: nothing in CI can observe an install
    being refused, and nothing in the contract suite was looking at installability at all.
    """

    def setUp(self):
        self.gradle = (ROOT / "app/build.gradle").read_text()
        self.main_manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.lite_manifest = (ROOT / "app/src/lite/AndroidManifest.xml")
        self.workflow = (ROOT / ".github/workflows/android-ci.yml").read_text()

    def test_two_flavours_exist(self):
        self.assertIn('flavorDimensions "scope"', self.gradle)
        self.assertIn("lite { dimension", self.gradle)
        self.assertIn("full { dimension", self.gradle)

    def test_the_main_manifest_still_declares_sms_for_the_full_build(self):
        # Removing them outright would abandon Messages, which the owner kept deliberately.
        self.assertIn('android.permission.READ_SMS', self.main_manifest)
        self.assertIn('android.permission.SEND_SMS', self.main_manifest)

    def test_the_lite_flavour_strips_both_permissions(self):
        self.assertTrue(self.lite_manifest.exists(), "app/src/lite/AndroidManifest.xml missing")
        body = self.lite_manifest.read_text()
        for permission in ("READ_SMS", "SEND_SMS"):
            self.assertRegex(body,
                             r'<uses-permission android:name="android\.permission\.%s" tools:node="remove" />'
                             % permission)

    def test_messages_tells_the_truth_when_its_permission_is_undeclared(self):
        screen = java_code_only(
            (ROOT / "feature-messages/src/main/java/app/space/feature/messages/MessagesScreen.java").read_text())
        self.assertIn("static boolean declared(Context context, String permission)", screen)
        self.assertIn("if (!declared(context, Manifest.permission.READ_SMS)) {", screen)
        self.assertIn("This build does not include messaging.", screen)
        # The gate must precede the grant button, or the button is offered in a state where it
        # can never succeed.
        self.assertLess(screen.index("declared(context, Manifest.permission.READ_SMS)"),
                        screen.index("Allow message access"))

    def test_ci_publishes_the_sideloadable_build_not_whichever_find_reaches_first(self):
        self.assertIn("-path '*lite*'", self.workflow)
        self.assertIn('NAME="MySpace-v${VERSION}-${CODE}-lite-dist.apk"', self.workflow)

    def test_the_unit_test_task_survives_the_flavour_split(self):
        # With flavours there is no per-build-type testDebugUnitTest on :app; using a task name
        # that does not exist would fail the step, and the step is continue-on-error, so the JVM
        # suite would silently stop running in CI. 'test' aggregates every variant.
        self.assertIn("sh gradlew --no-daemon --stacktrace test 2>&1", self.workflow)
        self.assertNotIn("testDebugUnitTest", self.workflow)


class InstallabilityIsCheckedContracts(unittest.TestCase):
    """The Play Protect block could not be seen by any existing check, so now one exists.

    The refusal happens before any code in the APK runs, which is why 81 slices of green CI and
    252 contracts did not notice that the published build could not be installed. A script now
    reads the built APK's binary manifest and fails the publish if the SMS pair is present.
    """

    def setUp(self):
        self.script = (ROOT / "qa/check_apk_permissions.py")
        self.assertTrue(self.script.exists())
        self.body = self.script.read_text()
        self.workflow = (ROOT / ".github/workflows/android-ci.yml").read_text()

    def test_the_scan_is_validated_by_controls(self):
        # Without controls, a scan that finds nothing would pass for the wrong reason -- the
        # same failure mode as every vacuous assertion this suite has ever had.
        self.assertIn('for control in ("INTERNET", "WAKE_LOCK"):', self.body)
        self.assertIn("SCAN BROKEN", self.body)

    def test_it_refuses_to_publish_an_uninstallable_apk(self):
        self.assertIn("REFUSED AT INSTALL", self.body)
        self.assertIn("return 1", self.body)

    def test_ci_runs_it_before_committing_the_apk(self):
        self.assertIn('python3 qa/check_apk_permissions.py "$BUILT"', self.workflow)
        # It must run before the copy, or the blocked build is already in artifacts/ by the
        # time anyone looks.
        self.assertLess(self.workflow.index('python3 qa/check_apk_permissions.py "$BUILT"'),
                        self.workflow.index('cp "$BUILT" "artifacts/$NAME"'))

    def test_the_late_flavour_build_still_emits_both_apks_for_the_artifact(self):
        # The upload glob is unchanged, so both flavours land in the Actions artifact and the
        # lite one is additionally committed; losing the full build from CI would hide whether
        # it still compiles.
        self.assertIn("path: '**/build/outputs/apk/**/*.apk'", self.workflow)


class NotDebuggableContracts(unittest.TestCase):
    """The second Play Protect block was correct, and the fix is to stop shipping the defect.

    'Harmful app blocked' is what Play Protect says about a debuggable APK, because anything
    holding an adb cable can attach to it and read its data. The first build published was the
    implicit debug type: there was no buildTypes block at all, so debuggable=true came free with
    the build and nobody chose it. dist is signed so it installs and not debuggable so it is safe
    to hand over.
    """

    def setUp(self):
        self.gradle = (ROOT / "app/build.gradle").read_text()
        self.workflow = (ROOT / ".github/workflows/android-ci.yml").read_text()
        self.script = (ROOT / "qa/check_apk_permissions.py").read_text()

    def test_a_dist_build_type_exists_and_is_not_debuggable(self):
        self.assertIn("dist {", self.gradle)
        self.assertIn("debuggable false", self.gradle)
        # Signed with one stable sideload key, falling back to the debug key only where the
        # repository key is absent. The old pin demanded signingConfigs.debug outright, which is
        # exactly the per-machine key that made build 30 refuse to upgrade build 29.
        self.assertIn("? signingConfigs.sideload : signingConfigs.debug", self.gradle)
        # Library modules declare only debug and release; without a fallback the variant graph
        # cannot resolve and the build fails at configuration time.
        self.assertIn("matchingFallbacks = ['release', 'debug']", self.gradle)

    def test_ci_builds_and_publishes_the_dist_variant(self):
        self.assertIn("assembleDebug assembleLiteDist", self.workflow)
        self.assertIn("-path '*dist*'", self.workflow)
        self.assertIn('NAME="MySpace-v${VERSION}-${CODE}-lite-dist.apk"', self.workflow)

    def test_the_scan_refuses_a_debuggable_apk(self):
        self.assertIn('if "debuggable".encode("utf-16-le") in blob:', self.script)
        self.assertIn("DEBUGGABLE", self.script)


class JUnitImportContracts(unittest.TestCase):
    """Every JVM test file must import the assertion helpers it calls.

    There is no JDK in this workspace, so nothing compiles locally: a missing static import is
    invisible until CI, where it presents as a failing 'test' step whose exit status is masked by
    continue-on-error. That is exactly how CalculatorRulesTest went unnoticed -- it called
    assertNull four times and imported three helpers.

    Rather than pin one file, this scans all of them, so the next test written cannot reintroduce
    the same silent failure. It is a poor substitute for javac, but it covers the one javac
    diagnostic this project has actually shipped.
    """

    HELPERS = ("assertEquals", "assertNull", "assertNotNull", "assertTrue", "assertFalse",
               "assertSame", "assertNotSame", "assertArrayEquals", "assertThat", "fail")

    def test_files(self):
        found = sorted(ROOT.glob("*/src/test/java/**/*.java"))
        self.assertGreater(len(found), 10, "expected the JVM test corpus to be discovered")
        return found

    def test_every_assertion_helper_used_is_statically_imported(self):
        for path in self.test_files():
            text = path.read_text(encoding="utf-8")
            imported = set(re.findall(r"import static org\.junit\.Assert\.(\w+);", text))
            used = {h for h in self.HELPERS if re.search(r"\b%s\s*\(" % h, text)}
            self.assertEqual(set(), used - imported,
                             "%s calls helpers it does not import" % path.name)

    def test_assertNull_is_imported_where_the_input_refusal_rules_are_pinned(self):
        path = ROOT / (
            "feature-calculator/src/test/java/app/space/feature/calculator/CalculatorRulesTest.java")
        text = path.read_text(encoding="utf-8")
        self.assertIn("import static org.junit.Assert.assertNull;", text)
        # The refusal rules are the point of the test: a key that cannot appear here must return
        # null, not an expression that only fails at equals time.
        self.assertIn("keysThatCannotAppearHereAreRefusedAtTheKey", text)
        self.assertGreaterEqual(text.count("assertNull(CalculatorRules.append("), 3)


class DiagnosticsLogContracts(unittest.TestCase):
    """A screenshot says a crash happened; the log says where.

    Two Play Protect blocks and one settings crash were diagnosed from screenshots alone. The log
    collector records navigation, app opens, permission outcomes and errors while enabled, and
    crashes whether or not it is enabled -- a crash is exactly when nobody thought to turn it on.
    """

    def setUp(self):
        self.log = (ROOT / "core-data/src/main/java/app/space/core/data/AppLog.java").read_text()
        self.main = java_code_only(read("MainActivity.java"))

    def test_the_collector_exists_and_cannot_grow_without_bound(self):
        for api in ("public static boolean enabled(Context c)", "public static void event(",
                    "public static void crash(", "public static String tail(", "public static void clear("):
            self.assertIn(api, self.log)
        self.assertIn("MAX_BYTES = 1000000", self.log)

    def test_crashes_are_recorded_even_when_logging_is_off(self):
        self.assertIn("if (c == null || t == null) return;", self.log)
        crash = self.log[self.log.index("public static void crash("):]
        self.assertNotIn("enabled(c)", crash[:crash.index("private static")])

    def test_the_host_installs_a_crash_handler_first(self):
        self.assertIn("Thread.setDefaultUncaughtExceptionHandler(", self.main)
        self.assertIn("AppLog.crash(MainActivity.this,thrown);", self.main)
        self.assertLess(self.main.index("setDefaultUncaughtExceptionHandler"),
                        self.main.index("super.onCreate(state);"))

    def test_settings_has_a_diagnostics_page_with_share_and_clear(self):
        self.assertIn("private void buildDiagnosticsSettings(){", self.main)
        self.assertIn('pillButton("Share log"', self.main)
        self.assertIn('pillButton("Clear log"', self.main)
        self.assertIn("Intent.ACTION_SEND", self.main)

    def test_the_recorder_no_longer_rebuilds_its_screen_on_every_tick(self):
        body = (ROOT / "feature-recorder/src/main/java/app/space/feature/recorder/RecorderScreen.java").read_text()
        ticker = body[body.index("private final Runnable ticker"):body.index("public RecorderScreen(")]
        # Comment lines stripped: the fix's own comment names host.invalidate() to explain why it
        # is gone, which is the seventh time an assertion in this suite has matched the
        # documentation of the thing it asserts. The rule stands on its own now.
        ticker = "\n".join(l for l in ticker.splitlines() if not l.strip().startswith("//"))
        self.assertNotIn("host.invalidate()", ticker, "the ticker must update views in place")
        self.assertIn("clockView.setText(", ticker)
        self.assertIn("statusView.setText(", ticker)
