package app.space.feature.expenses;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;

/**
 * Expense persistence. Amounts are stored as minor units (longs), never as decimal
 * strings or doubles -- see {@link ExpenseRules} for why that matters.
 */
public final class ExpenseStore {

    public static final String KEY = "expenses_items";
    public static final int CAP = 2000;

    private ExpenseStore() {}

    public static List<ExpenseRules.Expense> load(Context c) {
        List<ExpenseRules.Expense> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new ExpenseRules.Expense(o.optString("id"), o.optString("category"),
                        o.optString("note"), o.optLong("amountMinor"), o.optLong("occurredAt")));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return ExpenseRules.capped(out, CAP);
    }

    public static void save(Context c, List<ExpenseRules.Expense> expenses) {
        JSONArray a = new JSONArray();
        try {
            for (ExpenseRules.Expense e : ExpenseRules.capped(expenses, CAP)) {
                a.put(new JSONObject().put("id", e.id).put("category", e.category)
                        .put("note", e.note).put("amountMinor", e.amountMinor)
                        .put("occurredAt", e.occurredAt));
            }
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    public static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }
}
