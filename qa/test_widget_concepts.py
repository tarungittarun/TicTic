"""Source-level contracts for the fifteen task-focused app-widget concepts.

These checks validate source/resource wiring and evidence packaging only. They do not
replace an Android build, launcher placement, resize, interaction, accessibility, or
emulator-runtime test.
"""
from pathlib import Path
import base64
import os
import re
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/app/tictic/clock"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


class WidgetConceptContracts(unittest.TestCase):
    CONCEPTS = {
        "widget_alarm_countdown": "AlarmCountdown",
        "widget_alarm_stack": "AlarmStack",
        "widget_wake_switch": "WakeSwitch",
        "widget_quick_alarm": "QuickAlarm",
        "widget_world_grid": "WorldGrid",
        "widget_meeting_window": "MeetingWindow",
        "widget_week_strip": "WeekStrip",
        "widget_month_grid": "MonthGrid",
        "widget_year_pulse": "YearPulse",
        "widget_day_balance": "DayBalance",
        "widget_routine_stack": "RoutineStack",
        "widget_routine_countdown": "RoutineCountdown",
        "widget_focus_launch": "FocusLaunch",
        "widget_timer_live": "TimerLive",
        "widget_lap_insights": "LapInsights",
    }

    def test_catalog_has_35_entries_and_matching_provider_indices(self):
        catalog = read("WidgetCatalogActivity.java")
        for name in ("NAMES", "LAYOUTS", "WHAT", "PROVIDER_INDEX"):
            match = re.search(r"\b" + name + r"\s*=\s*\{([^}]*)\}", catalog, re.S)
            self.assertIsNotNone(match, name)
            body = match.group(1)
            values = re.findall(r'"[^"]*"', body) if name != "PROVIDER_INDEX" else re.findall(r"\b\d+\b", body)
            self.assertEqual(len(values), 35, name)
        self.assertIn('label("35 widget designs"', read("MainActivity.java"))
        self.assertIn('35 resizable home-screen widget designs', catalog)
        self.assertIn('boolean darkSample=', catalog)
        self.assertNotIn('i>=16', catalog)
        board = (ROOT / "TICTIC-Design-Board.html").read_text(encoding="utf-8")
        self.assertEqual(board.count('<div class="widget">'), 35)
        self.assertIn("35 designs · 39 providers", board)
        layout_body = re.search(r"LAYOUTS\s*=\s*\{([^}]*)\}", catalog, re.S).group(1)
        layouts = re.findall(r'"([^"]+)"', layout_body)
        index_body = re.search(r"PROVIDER_INDEX\s*=\s*\{([^}]*)\}", catalog, re.S).group(1)
        provider_indices = [int(value) for value in re.findall(r"\b\d+\b", index_body)]
        variants = read("WidgetVariants.java")
        extra_body = re.search(r"static Class<\?>\[\] extras\(\)\{return new Class<\?>\[\]\{([^}]*)\};\}", variants).group(1)
        extra_classes = re.findall(r"WidgetVariants\.(\w+)\.class", extra_body)
        provider_layouts = ["widget_clock", "widget_date", "widget_alarm"]
        for class_name in extra_classes:
            provider = re.search(r"class " + re.escape(class_name) + r" extends Base \{.*?layoutId\(\)\{return R\.layout\.(widget_[a-z_]+);", variants, re.S)
            self.assertIsNotNone(provider, class_name)
            provider_layouts.append(provider.group(1))
        self.assertEqual(len(provider_layouts), 39)
        for layout, provider_index in zip(layouts, provider_indices):
            self.assertEqual(provider_layouts[provider_index], layout, layout)

    def test_each_new_concept_has_layout_metadata_and_registered_provider(self):
        variants = read("WidgetVariants.java")
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        engine = read("WidgetEngine.java")
        concepts = read("WidgetConcepts.java")
        for layout, provider in self.CONCEPTS.items():
            with self.subTest(layout=layout):
                self.assertTrue((ROOT / "app/src/main/res/layout" / (layout + ".xml")).exists())
                info = (ROOT / "app/src/main/res/xml" / (layout + "_info.xml")).read_text(encoding="utf-8")
                self.assertIn('android:initialLayout="@layout/' + layout + '"', info)
                self.assertIn('android:resizeMode="horizontal|vertical"', info)
                self.assertIn("WidgetVariants$" + provider, manifest)
                self.assertIn("class " + provider + " extends Base", variants)
                self.assertIn('case "' + layout + '":', concepts)
                self.assertIn('"' + layout + '"', engine)
        self.assertEqual(len(list((ROOT / "app/src/main/res/xml").glob("widget_*_info.xml"))), 39)

    def test_concept_renderer_references_only_defined_remoteviews(self):
        concepts = read("WidgetConcepts.java")
        ids = set()
        for path in (ROOT / "app/src/main/res/layout").glob("widget_*.xml"):
            root = ET.parse(path).getroot()
            for node in root.iter():
                resource_id = node.attrib.get("{http://schemas.android.com/apk/res/android}id", "")
                if resource_id.startswith("@+id/"):
                    ids.add(resource_id[5:])
        referenced = set(re.findall(r"R\.id\.(widget_[A-Za-z0-9_]+)", concepts))
        self.assertEqual(referenced - ids, set())
        for i in range(7):
            self.assertIn(f"@+id/widget_month_weekday_{i}", (ROOT / "app/src/main/res/layout/widget_month_grid.xml").read_text())
        self.assertIn('@+id/widget_timer_live_action', (ROOT / "app/src/main/res/layout/widget_timer_live.xml").read_text())
        res = ROOT / "app/src/main/res"
        available = {
            "layout": {p.stem for p in (res / "layout").glob("*.xml")},
            "drawable": {p.stem for p in (res / "drawable").glob("*")},
            "string": set(),
        }
        for values_file in (res / "values").glob("*.xml"):
            available["string"].update(re.findall(r'<string\s+name="([^"]+)"', values_file.read_text(encoding="utf-8")))
        for layout in self.CONCEPTS:
            files = [res / "layout" / (layout + ".xml"), res / "xml" / (layout + "_info.xml")]
            for path in files:
                for resource_type, name in re.findall(r"@(layout|drawable|string)/([a-zA-Z0-9_]+)", path.read_text(encoding="utf-8")):
                    self.assertIn(name, available[resource_type], f"{path}: missing @{resource_type}/{name}")

    def test_concepts_adapt_to_compact_launcher_sizes(self):
        concepts = read("WidgetConcepts.java")
        self.assertIn("boolean compact=width<155||height<110", concepts)
        self.assertIn("boolean compactMonth=width<165||height<200", concepts)
        self.assertIn("views.setViewVisibility(R.id.widget_month_grid,compactMonth?View.GONE:View.VISIBLE)", concepts)
        self.assertIn("views.setViewVisibility(R.id.widget_month_compact,compactMonth?View.VISIBLE:View.GONE)", concepts)
        for layout in self.CONCEPTS:
            with self.subTest(layout=layout):
                root = ET.parse(ROOT / "app/src/main/res/layout" / (layout + ".xml")).getroot()
                ids = {node.attrib.get("{http://schemas.android.com/apk/res/android}id", "") for node in root.iter()}
                expected = "@+id/widget_month_title" if layout == "widget_month_grid" else "@+id/widget_concept_heading"
                self.assertIn(expected, ids)
        month_info = ET.parse(ROOT / "app/src/main/res/xml/widget_month_grid_info.xml").getroot()
        self.assertEqual(month_info.attrib["{http://schemas.android.com/apk/res/android}minHeight"], "200dp")
        month_layout = (ROOT / "app/src/main/res/layout/widget_month_grid.xml").read_text(encoding="utf-8")
        for i in range(7):
            self.assertIn(f"@+id/widget_month_compact_day_{i}", month_layout)

    def test_full_month_grid_uses_weighted_linear_rows_for_visible_dates(self):
        android = "{http://schemas.android.com/apk/res/android}"
        root = ET.parse(ROOT / "app/src/main/res/layout/widget_month_grid.xml").getroot()
        grid = next(node for node in root.iter()
                    if node.attrib.get(android + "id") == "@+id/widget_month_grid")
        self.assertEqual(grid.tag, "LinearLayout")
        self.assertEqual(grid.attrib.get(android + "orientation"), "vertical")
        self.assertEqual(len(grid), 7)
        for row_index, row in enumerate(grid):
            with self.subTest(row=row_index):
                self.assertEqual(row.tag, "LinearLayout")
                self.assertEqual(row.attrib.get(android + "orientation"), "horizontal")
                self.assertEqual(len(row), 7)
                for cell in row:
                    self.assertEqual(cell.tag, "TextView")
                    self.assertEqual(cell.attrib.get(android + "layout_width"), "0dp")
                    self.assertEqual(cell.attrib.get(android + "layout_weight"), "1")

    def test_new_concepts_include_every_shared_engine_view(self):
        engine = read("WidgetEngine.java")
        shared_ids = set()
        for name in ("TIME_IDS", "TEXT_IDS"):
            match = re.search(r"private static final int\s*\[\]\s*" + name + r"\s*=\s*\{([^}]*)\}", engine)
            self.assertIsNotNone(match, name)
            shared_ids.update(re.findall(r"R\.id\.(widget_[A-Za-z0-9_]+)", match.group(1)))

        def expanded_ids(layout, visited=None):
            visited = set() if visited is None else visited
            if layout in visited:
                return set()
            visited.add(layout)
            root = ET.parse(ROOT / "app/src/main/res/layout" / (layout + ".xml")).getroot()
            result = set()
            for node in root.iter():
                rid = node.attrib.get("{http://schemas.android.com/apk/res/android}id", "")
                if rid.startswith("@+id/") or rid.startswith("@id/"):
                    result.add(rid.split("/", 1)[1])
                include = node.attrib.get("layout", "") if node.tag == "include" else ""
                if include.startswith("@layout/"):
                    result.update(expanded_ids(include.split("/", 1)[1], visited))
            return result

        for layout in self.CONCEPTS:
            with self.subTest(layout=layout):
                self.assertEqual(shared_ids - expanded_ids(layout), set(), layout)

    def test_concept_studio_does_not_offer_unsupported_font_or_misalign_action_chips(self):
        studio = read("WidgetConfigActivity.java")
        concepts = read("WidgetConcepts.java")
        self.assertIn('if(!WidgetConcepts.isConcept(widgetName))', studio)
        self.assertIn('PURPOSE-BUILT TYPE', studio)
        start = concepts.index("private static int[] alignmentIds")
        end = concepts.index("private static int baseTextSize", start)
        alignment = concepts[start:end]
        for action_id in ("R.id.widget_wake_switch", "R.id.widget_quick_alarm_10", "R.id.widget_focus_25", "R.id.widget_timer_live_action"):
            self.assertNotIn(action_id, alignment)
        self.assertIn('case "widget_month_grid":return new int[]{R.id.widget_caption,R.id.widget_month_title};', alignment)

    def test_widget_actions_toggle_alarm_add_quick_alarm_and_control_timers(self):
        receiver = read("WidgetActionReceiver.java")
        concepts = read("WidgetConcepts.java")
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:name=".WidgetActionReceiver" android:exported="false"', manifest)
        for action in ("ACTION_TOGGLE_ALARM", "ACTION_START_FOCUS", "ACTION_TOGGLE_TIMER"):
            self.assertIn(action, receiver)
            self.assertIn("WidgetActionReceiver." + action, concepts)
        self.assertNotIn("ACTION_QUICK_ALARM", receiver)
        self.assertNotIn("Quick alarm created", receiver)
        self.assertIn("AlarmScheduler.schedule(context,selected)", receiver)
        self.assertIn("AlarmScheduler.cancel(context,alarmId)", receiver)
        self.assertIn("TimerReceiver.start(context,duration)", receiver)
        self.assertIn("TimerReceiver.pause(context,remaining)", receiver)
        self.assertIn("WidgetEngine.refreshAll(context)", receiver)
        self.assertIn("new Intent(context,MainActivity.class)", concepts)
        self.assertIn("activity(context,intent,450000+widgetId*3+i)", concepts)
        quick_buttons = concepts[concepts.index("private static void quickAlarmButtons"):concepts.index("private static void worldGrid")]
        self.assertNotIn("broadcast(context,intent)", quick_buttons)
        self.assertIn("MainActivity.EXTRA_WIDGET_QUICK_ALARM_MINUTES", concepts)
        main = read("MainActivity.java")
        self.assertIn("beginAlarmSave(alarm,false,null)", main)
        self.assertIn('put("_widget_quick_alarm_minutes",minutes)', main)
        self.assertIn('alarm.remove("_widget_quick_alarm_minutes")', main)
        self.assertIn("anchorWidgetQuickAlarm(alarm)", main)
        self.assertIn("requestNotificationAccessInContext()", main)
        self.assertIn("requestExactAccessBeforeSave()", main)
        self.assertIn("checkFullScreenBeforeSave", main)
        self.assertIn('.put("label","Quick · "+minutes+" min")', main)
        self.assertIn('prefs.getBoolean("alarm_default_vibrate",true)', main)
        self.assertIn('prefs.getInt("alarm_default_snooze",9)', main)
        self.assertIn("AlarmScheduler.schedule(this,alarm)", main)

    def test_countdown_fallback_and_timer_widget_refresh_are_explicit(self):
        concepts = read("WidgetConcepts.java")
        timer = read("TimerReceiver.java")
        self.assertIn("import app.space.core.domain.TimeMath;", concepts)
        self.assertIn("import android.widget.RemoteViews;", concepts)
        self.assertNotIn("import android.appwidget.RemoteViews;", concepts)
        self.assertIn("if(Build.VERSION.SDK_INT>=24)", concepts)
        self.assertIn("setChronometerCountDown(viewId,true)", concepts)
        self.assertIn('setTextViewText(viewId,"in "+formatDuration(remaining))', concepts)
        self.assertIn('views.setProgressBar(R.id.widget_timer_live_progress,100,progress,false)', concepts)
        main = read("MainActivity.java")
        self.assertIn('consumeWidgetQuickAlarm(getIntent())', main)
        choose = main[main.index("private void chooseTimerLength()"):main.index("private NumberPicker timerNumberPicker")]
        self.assertIn("WidgetEngine.refreshAll(this)", choose)
        self.assertGreaterEqual(timer.count("WidgetEngine.refreshAll(context)"), 4)
        self.assertIn("WidgetConcepts.render(c,v,widgetId,name,true)", read("WidgetEngine.java"))
        self.assertIn("WidgetConcepts.render(c,v,widgetId,name,false)", read("WidgetEngine.java"))

    def test_week_strip_reapply_resets_previous_day_highlight_text(self):
        concepts = read("WidgetConcepts.java")
        render_start = concepts.index("private static void weekStrip")
        render_end = concepts.index("private static void monthGrid", render_start)
        render = concepts[render_start:render_end]
        self.assertIn("views.setTextColor(WEEK_DAY_IDS[i],today?Color.WHITE:0xff65756e)", render)
        self.assertIn("views.setTextColor(WEEK_DATE_IDS[i],today?Color.WHITE:0xff183a34)", render)
        accents_start = concepts.index("private static void applyCalendarAccents")
        accents = concepts[accents_start:]
        self.assertIn("boolean customInk,int ink", accents)
        self.assertIn("views.setTextColor(WEEK_DAY_IDS[i],today?Color.WHITE:dayColor)", accents)
        self.assertIn("views.setTextColor(WEEK_DATE_IDS[i],today?Color.WHITE:dateColor)", accents)
        self.assertIn("int currentMonthInk=customInk?ink:0xff574b39", accents)
        self.assertIn("int adjacentMonthInk=customInk?mutedInk(ink):0xff9caaa4", accents)
        self.assertIn("private static int mutedInk(int color)", concepts)
        self.assertIn("configured||wallpaper,ink", concepts)

    def test_calendar_and_world_clock_data_respects_local_time_rules(self):
        concepts = read("WidgetConcepts.java")
        variants = read("WidgetVariants.java")
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("getFirstDayOfWeek()", concepts)
        self.assertIn("getActualMaximum(Calendar.DAY_OF_YEAR)", concepts)
        self.assertIn("end.add(Calendar.DAY_OF_YEAR,1)", concepts)
        self.assertIn('setTimeZone",TimeZone.getDefault().getID()', concepts)
        self.assertIn('setTimeZone","Europe/London"', concepts)
        world = ET.parse(ROOT / "app/src/main/res/layout/widget_world_grid.xml").getroot()
        meeting = ET.parse(ROOT / "app/src/main/res/layout/widget_meeting_window.xml").getroot()
        android_id = "{http://schemas.android.com/apk/res/android}id"
        for layout in (world, meeting):
            for node in layout.iter("TextClock"):
                if node.attrib.get(android_id, "").endswith("widget_date"):
                    continue
                self.assertTrue(node.attrib.get("{http://schemas.android.com/apk/res/android}format12Hour", "").startswith("'"))
                self.assertTrue(node.attrib.get("{http://schemas.android.com/apk/res/android}format24Hour", "").startswith("'"))
        self.assertIn("Intent.ACTION_DATE_CHANGED", variants)
        self.assertIn('android.intent.action.DATE_CHANGED', manifest)

    def test_alarm_and_routine_stacks_expand_repeating_schedules(self):
        concepts = read("WidgetConcepts.java")
        occurrences = concepts[concepts.index("private static List<Occurrence> occurrences"):concepts.index("private static String alarmLabel")]
        self.assertIn('item.optInt("days",0)==0?1:3', occurrences)
        self.assertIn("DataStore.nextOccurrence(item,reference)", occurrences)
        self.assertIn("Collections.sort(result", occurrences)

    def test_routine_widgets_refresh_after_create_edit_delete_and_occurrences(self):
        main = read("MainActivity.java")
        receiver = read("RoutineReceiver.java")
        save_start = main.index('DataStore.putArray(this,"routines",list);RoutineReceiver.scheduleAll(this);')
        save_end = main.index('DataStore.log(this,"Routine",edit?', save_start)
        self.assertIn("WidgetEngine.refreshAll(this)", main[save_start:save_end])
        delete_start = main.index('DataStore.putArray(this,"routines",keep);')
        delete_end = main.index('DataStore.log(this,"Routine","Routine deleted"', delete_start)
        self.assertIn("WidgetEngine.refreshAll(this)", main[delete_start:delete_end])
        self.assertIn("WidgetEngine.refreshAll(c)", receiver)

    def test_widget_ci_runs_on_branch_push_without_publishing(self):
        workflow = (ROOT / ".github/workflows/widget-concepts-ci.yml").read_text(encoding="utf-8")
        self.assertIn("  push:\n    branches:\n      - arena/3caca636-tictic", workflow)
        self.assertIn("  workflow_dispatch:", workflow)
        self.assertIn("contents: read", workflow)
        self.assertIn("id: unit", workflow)
        self.assertIn("test 2>&1 | tee /tmp/widget-unit.log", workflow)
        self.assertIn("id: build", workflow)
        self.assertIn(":app:assembleLiteDebug 2>&1 | tee /tmp/widget-build.log", workflow)
        self.assertIn("GITHUB_STEP_SUMMARY", workflow)
        self.assertIn("error:|symbol:|location:", workflow)
        self.assertIn("ReactiveCircus/android-emulator-runner@v2", workflow)
        self.assertIn("target: default", workflow)
        self.assertIn(":app:assembleLiteDebugAndroidTest", workflow)
        self.assertIn("script: bash qa/run_widget_emulator_e2e.sh", workflow)
        self.assertIn("emulator-boot-timeout: 900", workflow)
        self.assertNotIn("actions/upload-artifact", workflow)
        self.assertNotIn("git push", workflow)
        runner = (ROOT / "qa/run_widget_emulator_e2e.sh").read_text(encoding="utf-8")
        self.assertIn('readonly TEST_PACKAGE="app.space.one.test"', runner)
        self.assertIn('adb install -r "$main_apk" 2>&1 | tee "$TARGET_INSTALL_LOG"', runner)
        self.assertIn('adb install -r "$test_apk" 2>&1 | tee "$TEST_INSTALL_LOG"', runner)
        self.assertIn("install_instrumentation_apk_with_recovery", runner)
        self.assertIn('phase="recover emulator after instrumentation APK install failure"', runner)
        self.assertIn("adb reboot", runner)
        self.assertIn("sys.boot_completed", runner)
        self.assertIn("Lost network stack", runner)
        self.assertIn("No recoverable system_server failure was found", runner)
        self.assertIn("retrying the two APK installs once", runner)
        self.assertIn("Emulator setup diagnostics", runner)
        self.assertIn("INSTALL_FAILED", runner)
        self.assertIn('"$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner"', runner)
        self.assertIn('run-as "$APP_PACKAGE" rm -rf files/widget-e2e', runner)
        self.assertIn("widget-e2e-screenshots", runner)
        self.assertIn("ls -1 files/widget-e2e", runner)
        self.assertIn("capture emulator failure diagnostics", runner)
        self.assertIn("Emulator setup state", runner)
        self.assertIn("Emulator package and system signals", runner)
        self.assertIn("Host emulator processes", runner)
        self.assertIn("command_label=\"adb install -r", runner)
        self.assertIn('head -n 30 "$setup_log"', runner)
        self.assertIn('tail -n 100 "$INSTRUMENTATION_LOG"', runner)
        self.assertIn("adb logcat -d -b crash", runner)
        self.assertIn("WidgetActionReceiver|TimerReceiver|START_FOCUS", runner)
        self.assertIn("-t 4000", runner)
        self.assertIn("INSTRUMENTATION_ABORTED", runner)
        self.assertIn('if [ "$test_status" -ne 0 ] || ! grep -q \'OK (\' "$INSTRUMENTATION_LOG"; then', runner)
        self.assertIn("if adb shell am instrument", runner)
        self.assertNotIn("chunk_size=2500", runner)
        emitter = (ROOT / "qa/emit_widget_e2e_screenshots.sh").read_text(encoding="utf-8")
        self.assertIn("notice title=E2E screenshot", emitter)
        self.assertIn("payload_length > 3800", emitter)
        self.assertIn("ordinal=${name%%_*}", emitter)
        self.assertIn("%s/24 %s", emitter)
        self.assertIn("01_alarm_countdown.jpg", emitter)
        self.assertIn("${expected[$index]}", emitter)
        for group in ("01–08", "09–16", "17–24"):
            self.assertIn("Surface emulator screenshots " + group, workflow)
        self.assertIn('grep -q \'OK (\' "$INSTRUMENTATION_LOG"', runner)

    def test_screenshot_emitter_preserves_names_ordinals_and_payloads_when_partial(self):
        emitter = ROOT / "qa/emit_widget_e2e_screenshots.sh"
        names = [
            "01_alarm_countdown.jpg", "02_alarm_stack.jpg", "03_wake_switch.jpg", "04_quick_alarm.jpg",
            "05_world_grid.jpg", "06_meeting_window.jpg", "07_week_strip.jpg", "08_month_grid.jpg",
            "09_year_pulse.jpg", "10_day_balance.jpg", "11_routine_stack.jpg", "12_routine_countdown.jpg",
            "13_focus_launch.jpg", "14_timer_live.jpg", "15_lap_insights.jpg", "16_month_grid_compact_week.jpg",
            "17_wake_switch_disabled_after_tap.jpg", "18_timer_live_running_after_focus_launch.jpg",
            "19_timer_live_paused_after_tap.jpg", "20_quick_alarm_save_preflight.jpg",
            "21_widget_catalog_new_concepts.jpg", "22_quick_alarm_pin_confirmation.jpg",
            "23_quick_alarm_widget_studio.jpg", "24_quick_alarm_widget_placed.jpg",
        ]
        with tempfile.TemporaryDirectory(prefix="widget-e2e-emitter-") as temp:
            screen_dir = Path(temp)
            payloads = {}
            for index, name in enumerate(names, 1):
                payload = bytes([index]) * 2800
                payloads[name] = payload
                (screen_dir / name).write_bytes(payload)
            env = os.environ.copy()
            env["WIDGET_E2E_SCREEN_DIR"] = str(screen_dir)
            instrumentation = (ROOT / "app/src/androidTest/java/app/tictic/clock/WidgetConceptE2ETest.java").read_text(encoding="utf-8")
            expected_block = instrumentation[instrumentation.index("String[] expected = {", instrumentation.index("zAllTwentyFourEmulatorCapturesArePresentAndDecodable")):]
            expected_block = expected_block[:expected_block.index("};")]
            self.assertEqual([Path(name).stem for name in names],
                             re.findall(r'"([^"]+)"', expected_block))
            for start in (0, 8, 16):
                result = subprocess.run(
                    ["bash", str(emitter), str(start), "8"],
                    check=True, capture_output=True, text=True, env=env)
                lines = result.stdout.splitlines()
                self.assertEqual(8, len(lines))
                for offset, line in enumerate(lines):
                    expected_name = names[start + offset]
                    ordinal = start + offset + 1
                    self.assertIn(f"E2E screenshot {ordinal:02d}/24 {expected_name}::", line)
                    encoded = line.rsplit("::", 1)[1]
                    self.assertLessEqual(len(encoded), 3800)
                    self.assertEqual(payloads[expected_name], base64.b64decode(encoded, validate=True))

            # Missing 01–15 must not cause screenshot 16 to be mislabeled 01/05 or
            # move it into the 01–08 annotation step. Its filename owns its ordinal.
            for missing in names[:15] + names[20:]:
                (screen_dir / missing).unlink()
            first = subprocess.run(["bash", str(emitter), "0", "8"],
                                   check=True, capture_output=True, text=True, env=env)
            self.assertEqual([], [line for line in first.stdout.splitlines() if "::" in line])
            middle = subprocess.run(["bash", str(emitter), "8", "8"],
                                    check=True, capture_output=True, text=True, env=env)
            middle_lines = [line for line in middle.stdout.splitlines() if "::" in line]
            self.assertEqual(1, len(middle_lines))
            self.assertIn("E2E screenshot 16/24 16_month_grid_compact_week.jpg::", middle_lines[0])
            self.assertEqual(payloads[names[15]], base64.b64decode(middle_lines[0].rsplit("::", 1)[1], validate=True))
            last = subprocess.run(["bash", str(emitter), "16", "8"],
                                  check=True, capture_output=True, text=True, env=env)
            last_lines = [line for line in last.stdout.splitlines() if "::" in line]
            self.assertEqual(4, len(last_lines))
            for line, ordinal, name in zip(last_lines, range(17, 21), names[16:20]):
                self.assertIn(f"E2E screenshot {ordinal:02d}/24 {name}::", line)
                self.assertEqual(payloads[name], base64.b64decode(line.rsplit("::", 1)[1], validate=True))

            unexpected = screen_dir / "unmapped_capture.jpg"
            unexpected.write_bytes(b"not part of the numbered journey")
            rejected = subprocess.run(["bash", str(emitter), "16", "8"],
                                      capture_output=True, text=True, env=env)
            self.assertNotEqual(0, rejected.returncode)
            self.assertIn("Unexpected emulator screenshot filename: unmapped_capture.jpg", rejected.stderr)

    def test_e2e_reapplies_interactive_calendar_ink_after_render(self):
        test = (ROOT / "app/src/androidTest/java/app/tictic/clock/WidgetConceptE2ETest.java").read_text(encoding="utf-8")
        display = test[test.index("private Capture display("):test.index("private void assertWeekStripCustomInk")]
        interactive_render = display.index("WidgetConcepts.render(app, remoteViews, widgetId, name, true)")
        style_reapply = display.index("WidgetConcepts.applyStyle(app, remoteViews, widgetId, name, style, widthDp, heightDp)")
        self.assertLess(interactive_render, style_reapply)
        self.assertIn("CALENDAR_TEST_INK = 0xff6855a5", test)
        self.assertIn("assertWeekStripCustomInk(capture)", test)
        self.assertIn("assertMonthGridCustomInk(capture)", test)
        self.assertIn("assertCompactMonthInk(capture)", test)
        self.assertIn("private boolean isCaptureReady(Capture capture)", test)
        self.assertIn("capture.page.hasWindowFocus()", test)
        self.assertIn("takeVerifiedEmulatorScreenshot(name", test)
        self.assertIn("capture.eyebrow, capture.title, expectedWidgetText", test)
        self.assertIn("private Bitmap takeVerifiedEmulatorScreenshot(String name, TextView... expectedText)", test)
        self.assertIn("captureFullScreenWithAccessibilityText", test)
        self.assertNotIn("instrumentation.waitForIdleSync()", test)

    def test_e2e_dismisses_external_crash_dialogs_but_keeps_target_crashes_fatal(self):
        test = (ROOT / "app/src/androidTest/java/app/tictic/clock/WidgetConceptE2ETest.java").read_text(encoding="utf-8")
        self.assertIn('"isn\'t responding", "keeps stopping", "keeps crashing"', test)
        self.assertIn('String action = anr ? "Wait" : "Close app"', test)
        self.assertIn("clickAccessibilityText(prompt, attemptAction)", test)
        self.assertIn("injectTapViaShell(prompt, attemptAction)", test)
        self.assertIn("promptNamesTargetApp(details)", test)
        self.assertIn('Android displayed the target app crash dialog: ', test)
        self.assertIn('E2E dismissed unrelated-app crash dialog: ', test)

    def test_e2e_fails_target_app_anr_instead_of_waiting_it_out(self):
        test = (ROOT / "app/src/androidTest/java/app/tictic/clock/WidgetConceptE2ETest.java").read_text(encoding="utf-8")
        response = test[test.index("private boolean respondToSystemPrompt"):test.index("private void waitForResponsiveAndroidUi")]
        self.assertIn('"Android displayed the target app ANR dialog: "', response)
        self.assertIn('String action = anr ? "Wait" : "Close app"', response)
        self.assertLess(response.index("promptNamesTargetApp(initialDetails)"),
                        response.index("clickAccessibilityText(prompt, attemptAction)"))
        self.assertIn("injectTapViaShell(prompt, attemptAction)", response)
        self.assertIn("remaining = findSystemPrompt(markers)", response)
        self.assertIn("after three recovery inputs", response)
        self.assertIn('contains("system ui")', response)
        self.assertIn('systemUiAnr && attempt > 1 ? "Close app" : action', response)
        self.assertIn("coordinateTap = actionBounds != null", response)
        self.assertIn("A successful input injection is not proof the system dialog consumed the tap", response)
        self.assertIn("recovery=", response)

    def test_e2e_system_prompt_recovery_runs_during_ui_waits_and_capture(self):
        test = (ROOT / "app/src/androidTest/java/app/tictic/clock/WidgetConceptE2ETest.java").read_text(encoding="utf-8")
        responsive = test[test.index("private void waitForResponsiveAndroidUi"):test.index("private Bitmap takeEmulatorScreenshot")]
        self.assertIn("respondToSystemPrompt()", responsive)
        screenshot = test[test.index("private Bitmap takeEmulatorScreenshot"):test.index("private void verifyScreenshotFile")]
        self.assertIn("if (!respondToSystemPrompt()) return screenshot", screenshot)
        wait = test[test.index("private AccessibilityNodeInfo waitForAccessibilityTexts"):test.index("private AccessibilityNodeInfo findAccessibilityRootContaining")]
        self.assertIn("if (respondToSystemPrompt())", wait)
        prompt_scan = test[test.index("private boolean containsAnyAccessibilityText"):test.index("private boolean promptNamesTargetApp")]
        self.assertIn("child.recycle()", prompt_scan)
        system_scan = test[test.index("private AccessibilityNodeInfo findSystemPrompt"):test.index("private boolean containsAnyAccessibilityText")]
        self.assertIn("isTargetAppWindow(active)", system_scan)
        self.assertIn("if (isTargetAppWindow(root))", system_scan)
        self.assertNotIn("if (targetAppWindow) return null", system_scan)
        self.assertIn("window.getId() == activeWindowId", system_scan)
        self.assertIn("window.getType() != AccessibilityWindowInfo.TYPE_SYSTEM", system_scan)
        self.assertIn('lowerTitle.contains("application error")', system_scan)
        self.assertIn("ui.getWindows()", system_scan)
        window_scan = test[test.index("private AccessibilityNodeInfo findAccessibilityRootContaining"):test.index("private boolean containsAccessibilityTexts")]
        self.assertIn("boolean includeTargetAppWindow", window_scan)
        self.assertIn("if (!includeTargetAppWindow && isTargetAppWindow(root))", window_scan)
        self.assertIn("window.getId() == activeWindowId", window_scan)
        self.assertIn("Thread.sleep(300L)", wait)
        self.assertIn("waitForAccessibilityTexts(10000L, true", test)
        self.assertIn("waitForAccessibilityTexts(30000L, false, \"Add\")", test)
        self.assertIn("nextPromptCheck = now + 900L", wait)
        await_flow = test[test.index("private void await(Check check, String failure, long timeoutMs)"):test.index("private boolean alarmEnabled")]
        self.assertIn("waitForResponsiveAndroidUi()", await_flow)
        self.assertIn("nextPromptCheck = SystemClock.uptimeMillis() + 900L", await_flow)

    def test_emulator_e2e_captures_each_concept_and_exercises_direct_actions(self):
        test = (ROOT / "app/src/androidTest/java/app/tictic/clock/WidgetConceptE2ETest.java").read_text(encoding="utf-8")
        self.assertIn("@FixMethodOrder(MethodSorters.NAME_ASCENDING)", test)
        self.assertIn("bAllFifteenProductionWidgetsRenderAndCaptureOnEmulator", test)
        self.assertEqual(test.count("new WidgetCase("), 15)
        self.assertIn("cMonthGridUsesCompactWeekStripAtCompactWidgetHeight", test)
        self.assertIn("dWakeSwitchFocusLaunchAndTimerLiveCompleteTheirTapFlows", test)
        self.assertIn("eQuickAlarmTapEntersTheExistingSaveAndPermissionPreflight", test)
        self.assertIn("zAllTwentyFourEmulatorCapturesArePresentAndDecodable", test)
        self.assertIn("MAX_SCREENSHOT_BYTES = 2800", test)
        self.assertIn("saveEmulatorScreenshot", test)
        self.assertIn("findAccessibilityRootContaining", test)
        self.assertIn("injectTap(node)", test)
        self.assertIn("injectTap(target.exactCenterX(), target.exactCenterY())", test)
        self.assertIn("visible.height() == view.getHeight()", test)
        self.assertIn("scrollTargetToTop(ScrollView scroll, View target, int inset)", test)
        self.assertIn("content.getHeight() > scroll.getHeight()", test)
        self.assertIn("view.getWidth() > 0 && view.getHeight() > 0", test)
        self.assertIn("matchingPixels >= 8", test)
        self.assertIn("expectedText.getGlobalVisibleRect(screenBounds)", test)
        catalog_journey = test[test.index("@Test public void aCatalogPinConfigureAndPlacementCompleteTheUserJourney"):test.index("private <T extends Activity>")]
        self.assertNotIn("scroll.smoothScrollTo", catalog_journey)
        self.assertNotIn("instrumentation.waitForIdleSync", catalog_journey)
        self.assertIn("MainActivity host = ensureMainActivity()", catalog_journey)
        self.assertIn('findContentDescription(host, "Settings")', catalog_journey)
        self.assertIn("Settings page did not render its Browse widgets action", catalog_journey)
        self.assertIn('findText(host, "Browse widgets")', catalog_journey)
        self.assertIn("click(browseWidgets)", catalog_journey)
        self.assertIn("ActivityMonitor catalogMonitor = instrumentation.addMonitor(", catalog_journey)
        self.assertIn("waitForMonitorWithTimeout(catalogMonitor, 15000L)", catalog_journey)
        self.assertIn('findText(catalog, "NEW CONCEPTS · 15")', catalog_journey)
        self.assertIn("await(() -> isScrollContentLaidOut(scroll, newConceptsTitle, card)", catalog_journey)
        self.assertIn("scrollTargetToTop(scroll, newConceptsTitle, dp(16))", catalog_journey)
        self.assertIn("captureFullScreenWithVisibleText(\"21_widget_catalog_new_concepts\", newConceptsTitle)", catalog_journey)
        self.assertIn("scrollTargetToTop(scroll, card, dp(16))", catalog_journey)
        self.assertIn("isScrollTargetReady(scroll, newConceptsTitle, dp(16), false)", catalog_journey)
        self.assertIn("isScrollTargetReady(scroll, card, dp(16), true)", catalog_journey)
        scroll_helpers = test[test.index("private void scrollTargetToTop"):test.index("private boolean isTargetInsideViewportOnMain")]
        self.assertIn("private boolean isScrollTargetReady(ScrollView scroll, View target, int inset,", scroll_helpers)
        self.assertIn("if (!isTargetInsideScrollViewport(scroll, target, inset))", scroll_helpers)
        self.assertIn("target.getLocationOnScreen(targetLocation)", scroll_helpers)
        self.assertIn("scroll.scrollBy(0, delta)", scroll_helpers)
        self.assertIn("target.requestRectangleOnScreen", scroll_helpers)
        self.assertIn("scrollY=\" + scroll.getScrollY() + \"/\" + range", scroll_helpers)
        self.assertIn("Focus Launch did not start the timer\", 20000L)", test)
        self.assertIn('timer state={running=', test)
        self.assertIn("alarmEnabled(ALARM_ID, true)", test)
        self.assertIn("Could not confirm the Android home-screen pin request; active window was", test)
        self.assertIn("waitForResponsiveAndroidUi", test)
        self.assertIn("FLAG_RETRIEVE_INTERACTIVE_WINDOWS", test)
        self.assertIn('"isn\'t responding"', test)
        self.assertIn("BitmapFactory.decodeFile", test)
        self.assertIn("screencap -p", test)
        self.assertIn("aCatalogPinConfigureAndPlacementCompleteTheUserJourney", test)
        self.assertIn("launchActivityWithoutWaitingForIdle", test)
        self.assertIn("application.registerActivityLifecycleCallbacks(callbacks)", test)
        self.assertIn("onActivityResumed(Activity candidate)", test)
        self.assertIn("resumed.await(45000L, TimeUnit.MILLISECONDS)", test)
        self.assertIn("waitForMonitorWithTimeout(configMonitor, 15000L)", test)
        self.assertIn("WidgetCatalogActivity catalog = (WidgetCatalogActivity) instrumentation", catalog_journey)
        self.assertNotIn("launchActivityWithoutWaitingForIdle(WidgetCatalogActivity.class", catalog_journey)
        self.assertNotIn("instrumentation.startActivitySync(", catalog_journey)
        setup = test[test.index("@Before public void setUp"):test.index("@After public void tearDown")]
        self.assertNotIn("MainActivity.class", setup)
        ensure_main = test[test.index("private MainActivity ensureMainActivity"):test.index("private Capture display")]
        self.assertIn("launchActivityWithoutWaitingForIdle(MainActivity.class, launch)", ensure_main)
        self.assertIn("await(this::isMainActivityForeground", ensure_main)
        self.assertIn("private boolean isMainActivityForeground()", ensure_main)
        self.assertIn("isTargetAppWindow(active)", ensure_main)
        self.assertIn("isRequestPinAppWidgetSupported", test)
        self.assertIn("performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)", test)
        self.assertIn('waitForAccessibilityTexts(15000L, false, "10 MIN")', test)
        self.assertIn("launchActivityWithoutWaitingForIdle(WidgetConfigActivity.class, configure)", test)
        self.assertNotIn("instrumentation.startActivitySync(configure)", test)
        self.assertNotIn("app.startActivity(configure)", test)
        self.assertIn("WidgetConfigActivity", test)
        self.assertIn("ui.takeScreenshot()", test)
        self.assertIn("WidgetConcepts.render(app, remoteViews, widgetId, name, true)", test)
        self.assertIn('new File(app.getFilesDir(), "widget-e2e")', test)
        self.assertIn("expected 24", (ROOT / "qa/run_widget_emulator_e2e.sh").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
