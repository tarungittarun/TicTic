"""Source-contract smoke checks. These are NOT Android runtime/UI/audio tests.
Run: python3 -m unittest discover -s qa -v
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/tictic/clock"
# Extracted MySpace modules (SUITE-PLAN.md §3.1). Files that move out of :app
# are read from here instead, so the contracts keep guarding them.
CORE_DOMAIN = ROOT / "core-domain/src/main/java/app/space/core/domain"
CORE_DESIGN = ROOT / "core-design/src/main/java/app/space/core/design"
CORE_DATA = ROOT / "core-data/src/main/java/app/space/core/data"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def read_core_domain(name):
    return (CORE_DOMAIN / name).read_text(encoding="utf-8")


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
        self.assertIn('alarm.put("reminder_minutes",30)', datastore)

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
        self.assertIn("ACCENT_COLORS=AppTheme.ACCENT_COLORS", datastore)

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
        self.assertIn("private final class TicAlertBuilder", main)
        self.assertIn('close.setContentDescription("Close dialog")', main)
        self.assertIn("dialog.setCanceledOnTouchOutside(true)", main)
        self.assertIn("window.setBackgroundDrawable(panel)", main)
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
        datastore=read("DataStore.java")
        self.assertIn('i<old.length() && i<99',datastore)

    def test_home_clock_offers_digital_and_analog_style_variants(self):
        main=read("MainActivity.java")
        analog=read("AnalogClockView.java")
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
        motion = read("GelMotion.java")
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
        self.assertIn("static int alarmVolumeAction(Context c,boolean volumeUp)", store)
        self.assertIn("Control volume becomes Snooze; Turn off becomes Stop", store)
        self.assertIn("int action=(old==2||old==3)?2:1", store)
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
        self.assertIn("cardFillForElevation(int level)", main)
        self.assertIn("elevation==2?9f+glassDepth*.05f:4f+glassDepth*.025f", main)
        self.assertIn("deeper shadow and clearer edge separation", main)

    def test_glass_surface_exposes_presets_customization_and_legibility(self):
        main = read("MainActivity.java")
        presets = read("GlassPreset.java")
        for key in ('"app_glass_enabled"','"app_glass_opacity"','"app_glass_finish"','"app_glass_depth"'):
            self.assertIn(key, main)
        self.assertIn("glassSurfaceDrawable", main)
        self.assertIn('"Surface opacity"', main)
        self.assertIn('"Sheen"', main)
        self.assertIn('"Depth"', main)
        self.assertIn("Five ready-made light moods", main)
        self.assertIn("GlassPreset.apply(this,index)", main)
        self.assertIn('"app_glass_customized"', main)
        self.assertIn("Math.max(82,opacityFloor)", read("AppGlassDrawable.java"))
        for name in ("Sunlit Bubble","Daylight Pearl","Golden Hour","Moonlit Opal","Aurora Frost"):
            self.assertIn(name, presets)
        self.assertIn("AppSurfaceStyle.pageBackground(this)", main)
        self.assertIn("AppSurfaceStyle.criticalGlass(this,fill,17)", read("AlarmActivity.java"))
        self.assertIn("AppSurfaceStyle.glass(this,base,radius,opacity,finish,depth,preset,82)", main)
        studio=read("WidgetConfigActivity.java")
        self.assertIn("buildGlassPresetChoices(root,ink)", studio)
        self.assertIn('"glass_customized"', studio)
        self.assertIn("GlassPreset.widgetBase(preset)", studio)
        self.assertIn('"glass_preset"', read("WidgetEngine.java"))
        renderer = read("AppGlassDrawable.java")
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
        self.assertIn("stopwatchButtonHaptic(v)", main)

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
        self.assertIn("tab==3&&swRunning()", main)
        self.assertIn("?100L:16L", main)
        self.assertIn("TimeMath.formatHundredths(ms)", main)
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
        self.assertIn('"alarm_volume_up_action"', store)
        self.assertIn('"alarm_volume_down_action"', store)
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
        style = read("AppSurfaceStyle.java")
        main = read("MainActivity.java")
        alarm = read("AlarmActivity.java")
        studio = read("WidgetConfigActivity.java")
        catalog = read("WidgetCatalogActivity.java")
        presets = read("GlassPreset.java")
        for token in ('app_glass_opacity','app_glass_finish','app_glass_depth'):
            self.assertIn(token, style)
        for token in ('Sunlit Bubble','Daylight Pearl','Golden Hour','Moonlit Opal','Aurora Frost'):
            self.assertIn(token, presets)
        self.assertIn('AppSurfaceStyle.pageBackground(this)',main)
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

    def test_core_design_holds_only_decoupled_classes(self):
        """TicIconView moved first because it has no DataStore coupling. This fails if
        a coupled class is moved before :core-data exists, or if a moved class keeps
        reaching back into the legacy package."""
        sources = sorted((ROOT / "core-design/src/main/java").rglob("*.java"))
        self.assertTrue(sources, "core-design has no sources")
        for path in sources:
            code = java_code_only(path.read_text(encoding="utf-8"))
            self.assertNotIn("DataStore", code,
                             f"{path.name} touches DataStore; it belongs in :core-data's wake")
            self.assertNotIn("import app.tictic.clock.", code,
                             f"{path.name} imports the legacy :app package")

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
        pattern = re.compile(r'PREF\s*=\s*"([^"]+)"')
        datastore = pattern.search(read("DataStore.java"))
        apptheme = pattern.search((CORE_DATA / "AppTheme.java").read_text(encoding="utf-8"))
        self.assertIsNotNone(datastore, "DataStore.PREF literal not found")
        self.assertIsNotNone(apptheme, "AppTheme.PREF literal not found")
        self.assertEqual(datastore.group(1), apptheme.group(1))
        self.assertEqual("tictic_local_v1", apptheme.group(1),
                         "changing the preference file name strands existing user data")

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


if __name__ == "__main__":
    unittest.main()
