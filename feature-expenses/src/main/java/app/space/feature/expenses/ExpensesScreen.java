package app.space.feature.expenses;

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
import java.util.Map;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/** This month's spending, its category breakdown, and the row list. */
public final class ExpensesScreen implements Screen {

    private final ScreenHost host;

    public ExpensesScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Expenses"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        Calendar from = Calendar.getInstance();
        from.set(Calendar.DAY_OF_MONTH, 1);
        from.set(Calendar.HOUR_OF_DAY, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.SECOND, 0);
        from.set(Calendar.MILLISECOND, 0);
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.MONTH, 1);

        List<ExpenseRules.Expense> all = ExpenseStore.load(context);
        List<ExpenseRules.Expense> month = ExpenseRules.inMonth(all, from.getTimeInMillis(), to.getTimeInMillis());

        ui.sectionTitle(root, "Expenses", "This month, by category. Amounts stay on this device.");

        LinearLayout total = ui.card();
        LinearLayout.LayoutParams tp = ui.lp(-1, -2);
        tp.topMargin = ui.dp(8);
        root.addView(total, tp);
        total.addView(ui.label("Spent this month", 12, ui.palette().muted, false));
        TextView amount = ui.label(ExpenseRules.format(ExpenseRules.total(month)), 30, ui.palette().text, true);
        total.addView(amount);

        List<Map.Entry<String, Long>> breakdown = ExpenseRules.byCategory(month);
        if (!breakdown.isEmpty()) {
            LinearLayout cats = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(cats, cp);
            cats.addView(ui.label("By category", 14, ui.palette().text, true));
            long monthTotal = ExpenseRules.total(month);
            for (Map.Entry<String, Long> e : breakdown) {
                LinearLayout row = ui.hbox();
                row.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rp = ui.lp(-1, -2);
                rp.topMargin = ui.dp(6);
                cats.addView(row, rp);
                TextView name = ui.label(e.getKey(), 13, ui.palette().text, false);
                LinearLayout.LayoutParams np = ui.lp(0, -2);
                np.weight = 1f;
                row.addView(name, np);
                int pct = monthTotal == 0 ? 0 : (int) Math.round(e.getValue() * 100f / monthTotal);
                TextView value = ui.label(ExpenseRules.format(e.getValue()) + "  \u00b7  " + pct + "%",
                        12, ui.palette().muted, true);
                row.addView(value, ui.lp(-2, -2));
            }
        }

        TextView add = ui.pillButton("Add expense", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(10);
        root.addView(add, ap);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context); }
        });

        List<ExpenseRules.Expense> rows = ExpenseRules.sortedByNewest(month);
        if (rows.isEmpty()) {
            TextView empty = ui.label("Nothing recorded this month.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }
        for (final ExpenseRules.Expense expense : rows) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(8);
            root.addView(card, rp);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, ui.lp(-1, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            row.addView(copy, cpp);
            copy.addView(ui.label(expense.category, 14, ui.palette().text, true));
            String sub = expense.note.isEmpty()
                    ? new java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date(expense.occurredAt))
                    : expense.note;
            copy.addView(ui.label(sub, 11, ui.palette().muted, false));
            row.addView(ui.label(ExpenseRules.format(expense.amountMinor), 15, ui.palette().text, true),
                    ui.lp(-2, -2));
            TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
            delete.setGravity(Gravity.CENTER);
            delete.setPadding(0, ui.dp(8), 0, 0);
            card.addView(delete, ui.lp(-1, 32));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<ExpenseRules.Expense> list = new ArrayList<>(ExpenseStore.load(context));
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).id.equals(expense.id)) { list.remove(i); break; }
                    }
                    ExpenseStore.save(context, list);
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
        final EditText amount = field(context, ui, "Amount", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText category = field(context, ui, "Category", InputType.TYPE_CLASS_TEXT);
        final EditText note = field(context, ui, "Note (optional)", InputType.TYPE_CLASS_TEXT);
        form.addView(amount, ui.lp(-1, -2));
        LinearLayout.LayoutParams c1 = ui.lp(-1, -2); c1.topMargin = ui.dp(8);
        form.addView(category, c1);
        LinearLayout.LayoutParams c2 = ui.lp(-1, -2); c2.topMargin = ui.dp(8);
        form.addView(note, c2);

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle("Add expense");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                // parseAmountMinor returns null rather than guessing, so a typo is refused
                // instead of being stored as 0 -- which would silently skew every total.
                Long minor = ExpenseRules.parseAmountMinor(amount.getText().toString());
                if (minor == null) { host.toast("Enter a valid amount, like 120 or 120.50."); return; }
                List<ExpenseRules.Expense> list = new ArrayList<>(ExpenseStore.load(context));
                list.add(new ExpenseRules.Expense(ExpenseStore.newId(),
                        category.getText().toString(), note.getText().toString(),
                        minor.longValue(), System.currentTimeMillis()));
                ExpenseStore.save(context, list);
                host.invalidate();
            }
        });
        dialog.show();
    }

    private static EditText field(Context context, UiKit ui, String hint, int inputType) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(true);
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
