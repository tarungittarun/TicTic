package app.space.feature.calendar;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * A month grid with the selected day's events beneath it.
 *
 * <p>The grid is computed by {@link CalendarRules} in a fixed UTC calendar, so the cell a
 * date lands in cannot differ between the JVM test and the device. Rendering uses the
 * device zone only for the day boundaries that decide which events belong to which cell.
 */
public final class CalendarScreen implements Screen {

    private final ScreenHost host;
    private int year;
    private int month;
    private int selectedDay;
    /** 0 month grid, 1 week columns, 2 day list. The owner's "basic settings" pick the default. */
    private int viewMode;
    /** False until the owner taps a mode pill; until then the stored default view decides. */
    private boolean modeTouched;

    public CalendarScreen(ScreenHost host) {
        this.host = host;
        Calendar now = Calendar.getInstance();
        this.year = now.get(Calendar.YEAR);
        this.month = now.get(Calendar.MONTH);
        this.selectedDay = now.get(Calendar.DAY_OF_MONTH);
    }

    @Override public String title() { return "Calendar"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        final TimeZone zone = TimeZone.getDefault();
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        // ---- month header with paging ----
        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(head, ui.lp(-1, 44));
        // Year jump, the owner's explicit ask: the year is its own button opening a picker, and
        // the double arrows step a whole year for the impatient. Four taps to 2030 and back,
        // not twenty-six.
        TextView prevYear = ui.pillButton("\u00ab", ui.palette().surface, ui.palette().muted);
        head.addView(prevYear, ui.lp(36, 40));
        prevYear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { page(-12); }
        });
        TextView prev = ui.pillButton("\u2039", ui.palette().surface, ui.palette().text);
        head.addView(prev, ui.lp(36, 40));
        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { page(-1); }
        });
        TextView heading = ui.label(CalendarRules.monthName(year, month), 16, ui.palette().text, true);
        heading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = ui.lp(0, -2);
        hp.weight = 1f;
        head.addView(heading, hp);
        TextView yearButton = ui.pillButton(String.valueOf(year), ui.palette().surface, ui.palette().accent);
        head.addView(yearButton, ui.lp(-2, 40));
        yearButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { yearPicker(context, ui); }
        });
        TextView next = ui.pillButton("\u203a", ui.palette().surface, ui.palette().text);
        head.addView(next, ui.lp(36, 40));
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { page(1); }
        });
        TextView nextYear = ui.pillButton("\u00bb", ui.palette().surface, ui.palette().muted);
        head.addView(nextYear, ui.lp(36, 40));
        nextYear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { page(12); }
        });

        LinearLayout modes = ui.hbox();
        LinearLayout.LayoutParams mp = ui.lp(-1, 36);
        mp.topMargin = ui.dp(4);
        root.addView(modes, mp);
        String[] modeLabels = {"Month", "Week", "Day"};
        for (int i = 0; i < modeLabels.length; i++) {
            final int mode = i;
            TextView pill = ui.pillButton(modeLabels[i],
                    viewMode == i ? ui.palette().accent : ui.palette().surface,
                    viewMode == i ? android.graphics.Color.WHITE : ui.palette().muted);
            pill.setTextSize(11);
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, -2, 1f);
            pp.leftMargin = ui.dp(3);
            pp.rightMargin = ui.dp(3);
            modes.addView(pill, pp);
            pill.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    viewMode = mode;
                    modeTouched = true;
                    host.invalidate();
                }
            });
        }
        TextView gear = ui.pillButton("\u2699", ui.palette().surface, ui.palette().muted);
        LinearLayout.LayoutParams gp = ui.lp(40, 36);
        gp.leftMargin = ui.dp(4);
        modes.addView(gear, gp);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { settings(context, ui); }
        });
        TextView today = ui.pillButton("Today", ui.palette().surface, ui.palette().accent);
        LinearLayout.LayoutParams tp2 = ui.lp(-2, 36);
        tp2.leftMargin = ui.dp(4);
        modes.addView(today, tp2);
        today.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                year = now.get(Calendar.YEAR);
                month = now.get(Calendar.MONTH);
                selectedDay = now.get(Calendar.DAY_OF_MONTH);
                host.invalidate();
            }
        });

        if (!modeTouched) viewMode = CalendarStore.getDefaultView(context);

        // ---- weekday headers ----
        Calendar locale = Calendar.getInstance();
        int setting = CalendarStore.getFirstDay(context);
        final int firstDay = setting > 0 ? setting : locale.getFirstDayOfWeek();
        if (viewMode == 1) {
            buildWeek(context, ui, root, zone, firstDay);
        }
        if (viewMode == 2) {
            // The day list below is the whole view; nothing else to draw here.
        } else if (viewMode == 1) {
            // Columns already carry their own day headers.
        } else {
        List<String> headers = CalendarRules.weekdayHeaders(firstDay);
            LinearLayout headRow = ui.hbox();
            root.addView(headRow, ui.lp(-1, 24));
            for (String h : headers) {
                TextView cell = ui.label(h, 10, ui.palette().muted, true);
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams cp = ui.lp(0, -2);
                cp.weight = 1f;
                headRow.addView(cell, cp);
            }

            // ---- the 42-cell grid, with a dot under every day that holds something ----
            List<Integer> grid = CalendarRules.monthGrid(year, month, firstDay);
            long todayStart = startOfDay(zone, 0);
            java.util.HashSet<Integer> busy = new java.util.HashSet<>();
            for (CalendarRules.Event e : CalendarRules.inMonth(CalendarStore.load(context),
                    CalendarRules.dayStart(year, month, 1, zone),
                    CalendarRules.dayStart(year + (month == 11 ? 1 : 0), (month + 1) % 12, 1, zone))) {
                busy.add(CalendarRules.dayOfMonth(e.when, zone));
            }
            for (int week = 0; week < CalendarRules.GRID_CELLS / 7; week++) {
                LinearLayout row = ui.hbox();
                root.addView(row, ui.lp(-1, 48));
                for (int d = 0; d < 7; d++) {
                    final int day = grid.get(week * 7 + d);
                    boolean isToday = day != 0
                            && CalendarRules.isToday(CalendarRules.dayStart(year, month, day, zone),
                                todayStart, todayStart + 86400000L);
                    LinearLayout cellBox = ui.vbox();
                    cellBox.setGravity(Gravity.CENTER);
                    if (day == selectedDay) cellBox.setBackground(ui.shape(ui.palette().accent, 12));
                    TextView cell = ui.label(day == 0 ? "" : String.valueOf(day), 13,
                            day == 0 ? ui.palette().muted
                                    : day == selectedDay ? android.graphics.Color.WHITE
                                    : isToday ? ui.palette().accent : ui.palette().text,
                            day == selectedDay || isToday);
                    cell.setGravity(Gravity.CENTER);
                    cellBox.addView(cell, ui.lp(-2, -2));
                    View dot = new View(context);
                    dot.setBackground(ui.shape(busy.contains(day) ? ui.palette().accent : 0x00000000, 3));
                    LinearLayout.LayoutParams dp = ui.lp(6, 6);
                    dp.topMargin = ui.dp(2);
                    cellBox.addView(dot, dp);
                    LinearLayout.LayoutParams cp = ui.lp(0, 44);
                    cp.weight = 1f;
                    cp.leftMargin = ui.dp(1);
                    cp.rightMargin = ui.dp(1);
                    row.addView(cellBox, cp);
                    if (day != 0) {
                        cellBox.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) { selectedDay = day; host.invalidate(); }
                        });
                    }
                }
            }
        }

        // ---- selected day ----
        final long dayFrom = CalendarRules.dayStart(year, month, selectedDay, zone);
        final long dayTo = dayFrom + 86400000L;
        List<CalendarRules.Event> events = CalendarRules.onDay(CalendarStore.load(context), dayFrom, dayTo);

        ui.sectionTitle(root, dayHeading(), events.size() == 1 ? "1 event" : events.size() + " events");

        TextView add = ui.pillButton("Add event", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(8);
        root.addView(add, ap);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context, dayFrom); }
        });

        for (final CalendarRules.Event event : events) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(card, cp);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, ui.lp(-1, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            row.addView(copy, cpp);
            copy.addView(ui.label(event.title, 14, ui.palette().text, true));
            String sub = event.allDay() ? "All day"
                    : CalendarRules.timeLabel(event.when, zone);
            if (event.reminderMinutes >= 0) {
                sub = sub + "  \u00b7  reminds "
                        + (event.reminderMinutes >= 60
                            ? (event.reminderMinutes / 60) + " h before"
                            : event.reminderMinutes + " min before");
            }
            if (!event.notes.isEmpty()) sub = sub + "  \u00b7  " + event.notes;
            copy.addView(ui.label(sub, 11, ui.palette().muted, false));
            TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
            row.addView(delete, ui.lp(-2, -2));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<CalendarRules.Event> all = new ArrayList<>(CalendarStore.load(context));
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).id.equals(event.id)) { all.remove(i); break; }
                    }
                    CalendarStore.save(context, all);
                    EventReminderReceiver.cancel(context, event.id);
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    private String dayHeading() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, selectedDay);
        return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                + " " + selectedDay + " "
                + c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
    }

    /** Moves the month, carrying the year over, and keeps the selection a valid day. */

    static final int[] REMINDER_OFFSETS = {-1, 5, 10, 30, 60};
    static final String[] REMINDER_LABELS = {"No reminder", "5 min before", "10 min before",
            "30 min before", "1 hour before"};

    /**
     * The calendar's own settings sheet: when the week starts, which view the app opens on, and
     * the reminder new events pre-pick. Stored beside the events, applied on the next build.
     */
    private void settings(final Context context, final UiKit ui) {
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final android.widget.Spinner weekStart = spinner(context,
                new String[]{"Week starts: device default", "Week starts: Sunday",
                        "Week starts: Monday"});
        int storedFirst = CalendarStore.getFirstDay(context);
        weekStart.setSelection(storedFirst == Calendar.SUNDAY ? 1
                : storedFirst == Calendar.MONDAY ? 2 : 0);
        final android.widget.Spinner openOn = spinner(context,
                new String[]{"Open on: month", "Open on: week", "Open on: day"});
        openOn.setSelection(Math.max(0, Math.min(2, CalendarStore.getDefaultView(context))));
        final android.widget.Spinner defaultRemind = spinner(context, REMINDER_LABELS);
        int preset = CalendarStore.getDefaultReminder(context);
        for (int i = 0; i < REMINDER_OFFSETS.length; i++) {
            if (REMINDER_OFFSETS[i] == preset) { defaultRemind.setSelection(i); break; }
        }
        form.addView(weekStart, ui.lp(-1, -2));
        LinearLayout.LayoutParams p2 = ui.lp(-1, -2);
        p2.topMargin = ui.dp(10);
        form.addView(openOn, p2);
        LinearLayout.LayoutParams p3 = ui.lp(-1, -2);
        p3.topMargin = ui.dp(10);
        form.addView(defaultRemind, p3);

        new AppAlertBuilder(context, ui).setTitle("Calendar settings").setView(form)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        int choice = weekStart.getSelectedItemPosition();
                        CalendarStore.setFirstDay(context,
                                choice == 1 ? Calendar.SUNDAY : choice == 2 ? Calendar.MONDAY : -1);
                        CalendarStore.setDefaultView(context, openOn.getSelectedItemPosition());
                        int offset = defaultRemind.getSelectedItemPosition();
                        CalendarStore.setDefaultReminder(context,
                                REMINDER_OFFSETS[Math.max(0,
                                        Math.min(REMINDER_OFFSETS.length - 1, offset))]);
                        if (openOn.getSelectedItemPosition() != viewMode) modeTouched = false;
                        host.invalidate();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private static android.widget.Spinner spinner(Context context, String[] labels) {
        android.widget.Spinner s = new android.widget.Spinner(context);
        s.setAdapter(new android.widget.ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, labels));
        return s;
    }

    /** A year picker: the fast road between 2026 and 2030 the owner asked for. */
    private void yearPicker(final Context context, final UiKit ui) {
        final android.widget.NumberPicker picker = new android.widget.NumberPicker(context);
        picker.setMinValue(1970);
        picker.setMaxValue(2100);
        picker.setValue(year);
        new AppAlertBuilder(context, ui).setTitle("Jump to year").setView(picker)
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        year = picker.getValue();
                        host.invalidate();
                    }
                })
                .setNegativeButton("Stay", null).show();
    }

    /**
     * The week view: seven columns from the start of the week, each carrying its day number and
     * up to three event titles. A month grid answers "what is this month like"; this answers
     * "what is this week like", which is the question a working week actually asks.
     */
    private void buildWeek(Context context, UiKit ui, LinearLayout root, TimeZone zone, int firstDay) {
        long weekStart = CalendarRules.startOfWeek(year, month, selectedDay, firstDay, zone);
        List<CalendarRules.Event> all = CalendarStore.load(context);
        LinearLayout columns = ui.hbox();
        LinearLayout.LayoutParams cp = ui.lp(-1, -2);
        cp.topMargin = ui.dp(6);
        root.addView(columns, cp);
        for (int i = 0; i < 7; i++) {
            final long dayStart = weekStart + i * 86400000L;
            final int day = CalendarRules.dayOfMonth(dayStart, zone);
            LinearLayout column = ui.vbox();
            column.setGravity(Gravity.CENTER_HORIZONTAL);
            column.setPadding(ui.dp(2), ui.dp(6), ui.dp(2), ui.dp(6));
            boolean isToday = dayStart == startOfDay(zone, 0);
            boolean selected = sameDay(dayStart, zone);
            if (selected || isToday) column.setBackground(ui.shape(ui.palette().surface, 12));
            Calendar head = Calendar.getInstance(zone);
            head.setTimeInMillis(dayStart);
            column.addView(ui.label(head.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT,
                    Locale.getDefault()), 9, ui.palette().muted, true), ui.lp(-2, -2));
            TextView num = ui.label(String.valueOf(day), 14,
                    selected || isToday ? ui.palette().accent : ui.palette().text,
                    selected || isToday);
            num.setGravity(Gravity.CENTER);
            column.addView(num, ui.lp(-1, -2));
            List<CalendarRules.Event> dayEvents =
                    CalendarRules.onDay(all, dayStart, dayStart + 86400000L);
            int shown = 0;
            for (CalendarRules.Event e : dayEvents) {
                if (shown >= 3) {
                    column.addView(ui.label("+" + (dayEvents.size() - 3), 9,
                            ui.palette().muted, false), ui.lp(-1, -2));
                    break;
                }
                TextView ev = ui.label(e.title, 9, ui.palette().text, false);
                ev.setMaxLines(2);
                ev.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams ep = ui.lp(-1, -2);
                ep.topMargin = ui.dp(3);
                column.addView(ev, ep);
                shown++;
            }
            LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(0, -2, 1f);
            colp.leftMargin = ui.dp(1);
            colp.rightMargin = ui.dp(1);
            columns.addView(column, colp);
            column.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Calendar c = Calendar.getInstance(zone);
                    c.setTimeInMillis(dayStart);
                    year = c.get(Calendar.YEAR);
                    month = c.get(Calendar.MONTH);
                    selectedDay = c.get(Calendar.DAY_OF_MONTH);
                    viewMode = 2;
                    host.invalidate();
                }
            });
        }
    }

    /** Whether a day start is the selected day, for the week view's highlight. */
    private boolean sameDay(long dayStart, TimeZone zone) {
        return dayStart == CalendarRules.dayStart(year, month, selectedDay, zone);
    }

    private void page(int delta) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, 1);
        c.add(Calendar.MONTH, delta);
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        int max = CalendarRules.daysInMonth(year, month);
        if (selectedDay > max) selectedDay = max;
        host.invalidate();
    }

    private long startOfDay(TimeZone zone, int dayOffset) {
        Calendar c = Calendar.getInstance(zone);
        c.add(Calendar.DAY_OF_MONTH, dayOffset);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Adds an all-day event on the selected day; timed events are a later slice. */
    private void editor(final Context context, final long dayFrom) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText title = field(context, ui, "Event title");
        final EditText notes = field(context, ui, "Notes (optional)");
        form.addView(title, ui.lp(-1, -2));
        LinearLayout.LayoutParams np = ui.lp(-1, -2);
        np.topMargin = ui.dp(8);
        form.addView(notes, np);

        final android.widget.TimePicker time = new android.widget.TimePicker(context);
        time.setIs24HourView(true);
        LinearLayout.LayoutParams tmp = ui.lp(-1, -2);
        tmp.topMargin = ui.dp(8);
        form.addView(time, tmp);
        final android.widget.Spinner remind = new android.widget.Spinner(context);
        remind.setAdapter(new android.widget.ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                REMINDER_LABELS));
        int preset = CalendarStore.getDefaultReminder(context);
        for (int i = 0; i < REMINDER_OFFSETS.length; i++) {
            if (REMINDER_OFFSETS[i] == preset) { remind.setSelection(i); break; }
        }
        LinearLayout.LayoutParams rmp = ui.lp(-1, -2);
        rmp.topMargin = ui.dp(8);
        form.addView(remind, rmp);

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle(dayHeading());
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                String t = title.getText().toString().trim();
                if (t.isEmpty()) { host.toast("Give the event a title."); return; }
                List<CalendarRules.Event> all = new ArrayList<>(CalendarStore.load(context));
                // Stored at noon rather than midnight so the event cannot fall on the
                // neighbouring day if the device's zone or offset changes later.
                int reminder = REMINDER_OFFSETS[Math.max(0,
                        Math.min(REMINDER_OFFSETS.length - 1, remind.getSelectedItemPosition()))];
                String id = CalendarStore.newId();
                long at = dayFrom + time.getHour() * 3600000L + time.getMinute() * 60000L;
                all.add(new CalendarRules.Event(id, t, notes.getText().toString(), at, reminder));
                CalendarStore.save(context, all);
                EventReminderReceiver.schedule(context, id, t, at, reminder);
                host.invalidate();
            }
        });
        dialog.show();
    }

    private static EditText field(Context context, UiKit ui, String hint) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setTextColor(ui.palette().text);
        e.setHintTextColor(ui.palette().muted);
        e.setTextSize(14);
        e.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        e.setBackground(ui.shape(ui.palette().surface, 14));
        return e;
    }

    @Override public void onResume() { }
    @Override public void onPause() { }
}
