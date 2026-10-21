package app.space.feature.habits;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The habit list, with today's toggle front and centre.
 *
 * <p>Streaks are computed by {@link HabitsRules} in epoch days, so the number shown cannot
 * drift across midnight or a daylight-saving change -- the failure mode of doing this in
 * raw millis, and one that shows up as a silently wrong count.
 */
public final class HabitsScreen implements Screen {

    private final ScreenHost host;

    public HabitsScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Habits"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        final long today = HabitsRules.epochDay(System.currentTimeMillis());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        ui.sectionTitle(root, "Habits", "Tap a habit to mark today. Streaks survive until midnight.");

        final List<HabitsStore.Habit> habits = HabitsStore.load(context);

        TextView add = ui.pillButton("Add habit", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(10);
        root.addView(add, ap);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context); }
        });

        if (habits.isEmpty()) {
            TextView empty = ui.label("No habits yet.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }

        for (final HabitsStore.Habit habit : habits) {
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
            copy.addView(ui.label(habit.name, 15, ui.palette().text, true));

            int streak = HabitsRules.currentStreak(habit.days, today);
            int longest = HabitsRules.longestStreak(habit.days);
            String detail = (HabitsRules.doneToday(habit.days, today) ? "Done today. " : "Not yet today. ")
                    + "Streak " + streak + (streak == 1 ? " day" : " days")
                    + " \u00b7 best " + longest + " \u00b7 week "
                    + (int) Math.round(HabitsRules.weeklyRate(habit.days, today) * 100) + "%";
            TextView note = ui.label(detail, 11, ui.palette().muted, false);
            note.setPadding(0, ui.dp(3), 0, 0);
            copy.addView(note);

            TextView toggle = ui.pillButton(HabitsRules.doneToday(habit.days, today) ? "Undo" : "Done",
                    HabitsRules.doneToday(habit.days, today) ? ui.palette().surface : ui.palette().accent,
                    HabitsRules.doneToday(habit.days, today) ? ui.palette().text : android.graphics.Color.WHITE);
            row.addView(toggle, ui.lp(-2, 40));
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<HabitsStore.Habit> all = new ArrayList<>(HabitsStore.load(context));
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).id.equals(habit.id)) {
                            Set<Long> days = HabitsRules.toggled(all.get(i).days, today);
                            all.set(i, new HabitsStore.Habit(habit.id, habit.name, days));
                            break;
                        }
                    }
                    HabitsStore.save(context, all);
                    host.invalidate();
                }
            });

            TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
            delete.setGravity(Gravity.CENTER);
            delete.setPadding(0, ui.dp(8), 0, 0);
            card.addView(delete, ui.lp(-1, 32));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<HabitsStore.Habit> all = new ArrayList<>(HabitsStore.load(context));
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).id.equals(habit.id)) { all.remove(i); break; }
                    }
                    HabitsStore.save(context, all);
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    private void editor(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText name = new EditText(context);
        name.setHint("Habit name");
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setTextColor(ui.palette().text);
        name.setHintTextColor(ui.palette().muted);
        name.setTextSize(14);
        name.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        name.setBackground(ui.shape(ui.palette().surface, 14));
        form.addView(name, ui.lp(-1, -2));

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle("Add habit");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Add", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) { host.toast("Give the habit a name."); return; }
                List<HabitsStore.Habit> all = new ArrayList<>(HabitsStore.load(context));
                all.add(new HabitsStore.Habit(HabitsStore.newId(), n, new java.util.HashSet<Long>()));
                HabitsStore.save(context, all);
                host.invalidate();
            }
        });
        dialog.show();
    }

    @Override public void onResume() { }
    @Override public void onPause() { }
}
