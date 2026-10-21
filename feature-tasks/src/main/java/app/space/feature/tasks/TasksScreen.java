package app.space.feature.tasks;

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
import java.util.TimeZone;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The task list. Ordering and overdue arithmetic are {@link TasksRules}, so the screen only
 * renders and persists.
 */
public final class TasksScreen implements Screen {

    private final ScreenHost host;
    private String query = "";

    public TasksScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Tasks"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        final long now = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        Calendar c = Calendar.getInstance(zone);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        final long todayStart = c.getTimeInMillis();

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        List<TasksRules.Task> all = TasksStore.load(context);
        int[] counts = TasksRules.counts(all, now);
        ui.sectionTitle(root, "Tasks", counts[0] + " total  \u00b7  " + counts[1] + " done  \u00b7  "
                + (counts[2] == 0 ? "none overdue" : counts[2] + " overdue"));

        final EditText search = field(context, ui, "Search tasks");
        search.setText(query);
        LinearLayout.LayoutParams sp = ui.lp(-1, -2);
        sp.topMargin = ui.dp(8);
        root.addView(search, sp);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c2) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c2) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                query = s.toString();
                host.invalidate();
            }
        });

        TextView add = ui.pillButton("Add task", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(10);
        root.addView(add, ap);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context, todayStart); }
        });

        List<TasksRules.Task> shown = TasksRules.filter(all, query);
        if (shown.isEmpty()) {
            TextView empty = ui.label(query.isEmpty() ? "No tasks yet." : "Nothing matches that search.",
                    13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }

        for (final TasksRules.Task task : shown) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(card, cp);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, ui.lp(-1, -2));

            TextView tick = ui.pillButton(task.done ? "\u2713" : "\u25cb",
                    task.done ? ui.palette().accent : ui.palette().surface,
                    task.done ? android.graphics.Color.WHITE : ui.palette().muted);
            row.addView(tick, ui.lp(40, 40));
            tick.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { replace(context, task.withDone(!task.done)); }
            });

            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            cpp.leftMargin = ui.dp(10);
            row.addView(copy, cpp);
            copy.addView(ui.label(task.title, 14,
                    task.done ? ui.palette().muted : ui.palette().text, !task.done));
            String due = TasksRules.dueLabel(task, now, todayStart);
            boolean overdue = TasksRules.isOverdue(task, now);
            if (!due.isEmpty() || task.priority == TasksRules.PRIORITY_HIGH) {
                String badge = (task.priority == TasksRules.PRIORITY_HIGH ? "High  \u00b7  " : "") + due;
                TextView b = ui.label(badge, 11, overdue ? 0xffc2564a : ui.palette().muted, overdue);
                b.setPadding(0, ui.dp(2), 0, 0);
                copy.addView(b);
            }

            TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
            row.addView(delete, ui.lp(-2, -2));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<TasksRules.Task> list = new ArrayList<>(TasksStore.load(context));
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).id.equals(task.id)) { list.remove(i); break; }
                    }
                    TasksStore.save(context, list);
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    /** Writes one task back, keeping its id so an edit cannot orphan stored history. */
    private void replace(Context context, TasksRules.Task updated) {
        List<TasksRules.Task> list = new ArrayList<>(TasksStore.load(context));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(updated.id)) { list.set(i, updated); break; }
        }
        TasksStore.save(context, list);
        host.invalidate();
    }

    private void editor(final Context context, final long todayStart) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText title = field(context, ui, "Task");
        final EditText dueDays = field(context, ui, "Due in days (blank for no due date)");
        dueDays.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(title, ui.lp(-1, -2));
        LinearLayout.LayoutParams dp = ui.lp(-1, -2);
        dp.topMargin = ui.dp(8);
        form.addView(dueDays, dp);

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle("Add task");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Add", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                String t = title.getText().toString().trim();
                if (t.isEmpty()) { host.toast("Give the task a name."); return; }
                // A blank due date means NO_DUE, not today: defaulting to today would make
                // every task overdue by tomorrow, which trains the user to ignore the list.
                long due = TasksRules.NO_DUE;
                String days = dueDays.getText().toString().trim();
                if (!days.isEmpty()) {
                    try {
                        due = todayStart + Long.parseLong(days) * 86400000L;
                    } catch (NumberFormatException e) {
                        host.toast("Due in days must be a number.");
                        return;
                    }
                }
                List<TasksRules.Task> list = new ArrayList<>(TasksStore.load(context));
                list.add(new TasksRules.Task(TasksStore.newId(), t, false, due, TasksRules.PRIORITY_NORMAL));
                TasksStore.save(context, list);
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
