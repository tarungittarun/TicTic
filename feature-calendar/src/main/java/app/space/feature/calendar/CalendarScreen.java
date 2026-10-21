package app.space.feature.calendar;

import android.content.Context;
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
        TextView prev = ui.pillButton("\u2039", ui.palette().surface, ui.palette().text);
        head.addView(prev, ui.lp(44, 40));
        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { page(-1); }
        });
        TextView heading = ui.label(CalendarRules.monthLabel(year, month), 17, ui.palette().text, true);
        heading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = ui.lp(0, -2);
        hp.weight = 1f;
        head.addView(heading, hp);
        TextView next = ui.pillButton("\u203a", ui.palette().surface, ui.palette().text);
        head.addView(next, ui.lp(44, 40));
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { page(1); }
        });

        // ---- weekday headers ----
        Calendar locale = Calendar.getInstance();
        int firstDay = locale.getFirstDayOfWeek();
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

        // ---- the 42-cell grid ----
        List<Integer> grid = CalendarRules.monthGrid(year, month, firstDay);
        long todayStart = startOfDay(zone, 0);
        for (int week = 0; week < CalendarRules.GRID_CELLS / 7; week++) {
            LinearLayout row = ui.hbox();
            root.addView(row, ui.lp(-1, 42));
            for (int d = 0; d < 7; d++) {
                final int day = grid.get(week * 7 + d);
                TextView cell = ui.label(day == 0 ? "" : String.valueOf(day), 13,
                        day == 0 ? ui.palette().muted : (day == selectedDay ? android.graphics.Color.WHITE : ui.palette().text),
                        day == selectedDay);
                cell.setGravity(Gravity.CENTER);
                if (day == selectedDay) cell.setBackground(ui.shape(ui.palette().accent, 12));
                LinearLayout.LayoutParams cp = ui.lp(0, 40);
                cp.weight = 1f;
                cp.leftMargin = ui.dp(1);
                cp.rightMargin = ui.dp(1);
                row.addView(cell, cp);
                if (day != 0) {
                    cell.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) { selectedDay = day; host.invalidate(); }
                    });
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
                    : new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(event.when));
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
                all.add(new CalendarRules.Event(CalendarStore.newId(), t,
                        notes.getText().toString(), dayFrom + 43200000L));
                CalendarStore.save(context, all);
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
