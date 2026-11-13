"""Source-level contracts for the fifteen task-focused app-widget concepts.

These checks validate source/resource wiring only. They do not replace an Android build,
launcher placement, resize, interaction, accessibility, or device-runtime test.
"""
from pathlib import Path
import re
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


if __name__ == "__main__":
    unittest.main()
