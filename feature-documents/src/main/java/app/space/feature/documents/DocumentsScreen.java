package app.space.feature.documents;

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

import app.space.core.data.AppPrefs;
import app.space.core.domain.DocumentRules;
import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Text documents and CSV spreadsheets.
 *
 * <p>A sheet is stored as CSV text and parsed by {@link DocumentRules}, so the quoting rules
 * are applied on every save rather than only on export. Splitting on commas would break the
 * first time a cell contained one, which is as soon as anyone writes prose into a sheet.
 */
public final class DocumentsScreen implements Screen {

    static final String KEY = "documents_items";
    static final int CAP = 300;

    private final ScreenHost host;
    private String query = "";

    public DocumentsScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Documents"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        List<DocumentRules.Document> all = load(context);
        ui.sectionTitle(root, "Documents", all.size() + " stored on this device.");

        LinearLayout actions = ui.hbox();
        LinearLayout.LayoutParams acp = ui.lp(-1, 46);
        acp.topMargin = ui.dp(8);
        root.addView(actions, acp);
        TextView newText = ui.pillButton("New document", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams np = ui.lp(0, 46);
        np.weight = 1f;
        actions.addView(newText, np);
        newText.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context, null, false); }
        });
        TextView newSheet = ui.pillButton("New sheet", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams sp = ui.lp(0, 46);
        sp.weight = 1f;
        sp.leftMargin = ui.dp(6);
        actions.addView(newSheet, sp);
        newSheet.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context, null, true); }
        });

        final EditText search = field(context, ui, "Search documents", InputType.TYPE_CLASS_TEXT);
        search.setText(query);
        LinearLayout.LayoutParams qp = ui.lp(-1, -2);
        qp.topMargin = ui.dp(8);
        root.addView(search, qp);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                query = s.toString();
                host.invalidate();
            }
        });

        List<DocumentRules.Document> shown = DocumentRules.filter(all, query);
        if (shown.isEmpty()) {
            TextView empty = ui.label(query.isEmpty() ? "Nothing written yet." : "Nothing matches that search.",
                    13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }

        for (final DocumentRules.Document document : shown) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(card, cp);
            card.addView(ui.label(document.title, 15, ui.palette().text, true));
            String meta;
            if (document.isSheet()) {
                List<List<String>> rows = DocumentRules.decodeSheet(document.csv.get(0));
                int columns = DocumentRules.columnCount(rows);
                meta = rows.size() + " rows  \u00b7  " + columns + " columns";
                // A total over the first numeric column, which is what a sheet is usually
                // opened to check. Skips the header row and ignores cells that are not
                // numbers rather than failing the whole sheet.
                for (int c = 0; c < columns; c++) {
                    double sum = DocumentRules.sumColumn(rows, c, true);
                    if (sum != 0) {
                        meta = meta + "  \u00b7  column " + (c + 1) + " sums to "
                                + String.format(java.util.Locale.ROOT, "%.2f", sum);
                        break;
                    }
                }
            } else {
                meta = DocumentRules.statsLabel(document.body);
            }
            TextView metaView = ui.label(meta, 11, ui.palette().muted, false);
            metaView.setPadding(0, ui.dp(3), 0, ui.dp(6));
            card.addView(metaView);

            if (!document.isSheet() && !document.body.isEmpty()) {
                TextView preview = ui.label(preview(document.body), 12, ui.palette().muted, false);
                card.addView(preview);
            }

            LinearLayout row = ui.hbox();
            card.addView(row, ui.lp(-1, 40));
            TextView edit = ui.pillButton("Edit", ui.palette().surface, ui.palette().text);
            LinearLayout.LayoutParams epp = ui.lp(0, 40);
            epp.weight = 1f;
            row.addView(edit, epp);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { editor(context, document, document.isSheet()); }
            });
            TextView delete = ui.pillButton("Delete", ui.palette().surface, ui.palette().muted);
            LinearLayout.LayoutParams dpp = ui.lp(0, 40);
            dpp.weight = 1f;
            dpp.leftMargin = ui.dp(6);
            row.addView(delete, dpp);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<DocumentRules.Document> list = new ArrayList<>(load(context));
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).id.equals(document.id)) { list.remove(i); break; }
                    }
                    save(context, list);
                    host.toast("Deleted " + document.title + ".");
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    private static String preview(String body) {
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() <= 140 ? flat : flat.substring(0, 139) + "\u2026";
    }

    private void editor(final Context context, final DocumentRules.Document existing, final boolean sheet) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText title = field(context, ui, "Title", InputType.TYPE_CLASS_TEXT);
        final EditText body = field(context, ui,
                sheet ? "One row per line, comma separated" : "Write here",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        body.setMinLines(sheet ? 6 : 8);
        body.setGravity(Gravity.TOP);
        if (existing != null) {
            title.setText(existing.title);
            body.setText(existing.isSheet() ? existing.csv.get(0) : existing.body);
        } else if (sheet) {
            // A header row to start from, so the first save is already a usable sheet and
            // the column total has something to skip.
            body.setText("Item,Amount\n");
        }
        form.addView(title, ui.lp(-1, -2));
        LinearLayout.LayoutParams bp = ui.lp(-1, -2);
        bp.topMargin = ui.dp(8);
        form.addView(body, bp);

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle(existing == null ? (sheet ? "New sheet" : "New document") : "Edit " + existing.title);
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                String t = title.getText().toString().trim();
                if (t.isEmpty()) { host.toast("Give it a title."); return; }
                String text = body.getText().toString();
                List<String> csv = sheet ? java.util.Collections.singletonList(text)
                        : java.util.Collections.<String>emptyList();
                List<DocumentRules.Document> list = new ArrayList<>(load(context));
                long now = System.currentTimeMillis();
                if (existing == null) {
                    list.add(new DocumentRules.Document(newId(), t, sheet ? "" : text, csv, now));
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).id.equals(existing.id)) {
                            list.set(i, new DocumentRules.Document(existing.id, t, sheet ? "" : text, csv, now));
                            break;
                        }
                    }
                }
                save(context, list);
                host.toast("Saved " + t + ".");
                host.invalidate();
            }
        });
        dialog.show();
    }

    private static EditText field(Context context, UiKit ui, String hint, int inputType) {
        EditText e = new EditText(context);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(inputType == InputType.TYPE_CLASS_TEXT);
        e.setTextColor(ui.palette().text);
        e.setHintTextColor(ui.palette().muted);
        e.setTextSize(14);
        e.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        e.setBackground(ui.shape(ui.palette().surface, 14));
        return e;
    }

    static List<DocumentRules.Document> load(Context c) {
        List<DocumentRules.Document> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                List<String> csv = new ArrayList<>();
                JSONArray rows = o.optJSONArray("csv");
                if (rows != null) for (int j = 0; j < rows.length(); j++) csv.add(rows.optString(j));
                out.add(new DocumentRules.Document(o.optString("id"), o.optString("title"),
                        o.optString("body"), csv, o.optLong("updatedAt")));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        List<DocumentRules.Document> sorted = DocumentRules.sortedByNewest(out);
        return sorted.size() > CAP ? new ArrayList<>(sorted.subList(0, CAP)) : sorted;
    }

    static void save(Context c, List<DocumentRules.Document> documents) {
        JSONArray a = new JSONArray();
        try {
            List<DocumentRules.Document> kept = DocumentRules.sortedByNewest(documents);
            if (kept.size() > CAP) kept = new ArrayList<>(kept.subList(0, CAP));
            for (DocumentRules.Document d : kept) {
                JSONArray csv = new JSONArray();
                for (String row : d.csv) csv.put(row);
                a.put(new JSONObject().put("id", d.id).put("title", d.title).put("body", d.body)
                        .put("csv", csv).put("updatedAt", d.updatedAt));
            }
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString((int) (Math.random() * 1679616), 36);
    }

    @Override public void onResume() { }
    @Override public void onPause() { }
}
