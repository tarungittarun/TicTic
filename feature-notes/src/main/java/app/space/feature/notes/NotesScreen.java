package app.space.feature.notes;

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
import java.util.List;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * A note wall in the Google Notes sense: coloured cards two across, pinned notes held at the
 * top, an archive drawer that files instead of deleting, search over everything, and
 * checklists that live inside the note's own text so they survive export and search.
 *
 * <p>The editor is a page, not a dialog: a title row, a colour row, and the body owning what
 * is left of the screen. Leaving the page saves it; an empty new note is discarded rather
 * than stored as litter.
 */
public final class NotesScreen implements Screen {

    private final ScreenHost host;
    private String query = "";
    private boolean showArchived;
    // Editor state. editingId null = the wall; "" = a note being created; otherwise the id.
    private String editingId;
    private String draftTitle = "";
    private String draftBody = "";
    private int draftColor;
    private boolean draftPinned;
    private boolean draftArchived;

    public NotesScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Notes"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        if (editingId != null) return buildEditor(context, ui);
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        List<NotesRules.Note> pool = showArchived
                ? NotesRules.archived(NotesStore.load(context))
                : NotesRules.active(NotesStore.load(context));
        List<NotesRules.Note> shown = NotesRules.sortedForDisplay(filter(pool, query));

        LinearLayout head = ui.hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(head, ui.lp(-1, 40));
        head.addView(ui.label(showArchived ? "Archive" : "Notes", 20, ui.palette().text, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView drawer = ui.pillButton(showArchived ? "Notes" : "Archive",
                ui.palette().surface, ui.palette().muted);
        head.addView(drawer, ui.lp(-2, 34));
        drawer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showArchived = !showArchived; host.invalidate(); }
        });

        LinearLayout searchRow = ui.hbox();
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srp = ui.lp(-1, 44);
        srp.topMargin = ui.dp(6);
        root.addView(searchRow, srp);
        final EditText search = new EditText(context);
        search.setHint("Search notes");
        search.setSingleLine(true);
        search.setTextColor(ui.palette().text);
        search.setHintTextColor(ui.palette().muted);
        search.setTextSize(14);
        search.setText(query);
        search.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        search.setBackground(ui.shape(ui.palette().surface, 14));
        searchRow.addView(search, new LinearLayout.LayoutParams(0, -2, 1f));
        // Submit, not per-keystroke: rebuilding on every character hands the keyboard back to
        // nothing and the search box loses focus mid-word.
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                query = search.getText().toString();
                host.invalidate();
                return true;
            }
        });
        TextView clear = ui.pillButton("×", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams clp = ui.lp(40, 38);
        clp.leftMargin = ui.dp(6);
        searchRow.addView(clear, clp);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { query = ""; host.invalidate(); }
        });

        TextView add = ui.pillButton("New note", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(8);
        root.addView(add, ap);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editingId = "";
                draftTitle = "";
                draftBody = "";
                draftColor = 0;
                draftPinned = false;
                draftArchived = showArchived;
                host.invalidate();
            }
        });

        if (shown.isEmpty()) {
            root.addView(ui.label(query.isEmpty()
                    ? (showArchived ? "Nothing archived." : "No notes yet.")
                    : "Nothing matches that search.", 13, ui.palette().muted, false),
                    centerParams(ui, 60));
            return scroll;
        }

        LinearLayout row = null;
        for (int i = 0; i < shown.size(); i++) {
            if (i % 2 == 0) {
                row = ui.hbox();
                LinearLayout.LayoutParams rp = ui.lp(-1, -2);
                rp.topMargin = ui.dp(8);
                root.addView(row, rp);
            }
            final NotesRules.Note note = shown.get(i);
            View card = noteCard(context, ui, note);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            cp.leftMargin = ui.dp(4);
            cp.rightMargin = ui.dp(4);
            row.addView(card, cp);
            if (shown.size() % 2 == 1 && i == shown.size() - 1) {
                row.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
        return scroll;
    }

    private View noteCard(final Context context, final UiKit ui, final NotesRules.Note note) {
        LinearLayout card = ui.vbox();
        card.setBackground(ui.shape(noteColor(ui, note.color), 16));
        card.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(12));
        if (note.pinned) {
            card.addView(ui.label("\ud83d\udccc", 11, ui.palette().muted, false), ui.lp(-2, -2));
        }
        TextView title = ui.label(NotesRules.displayTitle(note), 14, ui.palette().text, true);
        title.setMaxLines(2);
        card.addView(title, ui.lp(-1, -2));
        String preview = NotesRules.preview(note.body, 90);
        if (!preview.isEmpty()) {
            TextView body = ui.label(preview, 12, ui.palette().text, false);
            body.setMaxLines(3);
            LinearLayout.LayoutParams bp = ui.lp(-1, -2);
            bp.topMargin = ui.dp(4);
            card.addView(body, bp);
        }
        int[] tally = NotesRules.checklistTally(note.body);
        if (tally[1] > 0) {
            LinearLayout.LayoutParams tp = ui.lp(-2, -2);
            tp.topMargin = ui.dp(6);
            card.addView(ui.label(tally[0] + " of " + tally[1] + " done", 10,
                    ui.palette().muted, false), tp);
        }
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editingId = note.id;
                draftTitle = note.title;
                draftBody = note.body;
                draftColor = note.color;
                draftPinned = note.pinned;
                draftArchived = note.archived;
                host.invalidate();
            }
        });
        return card;
    }

    private View buildEditor(final Context context, final UiKit ui) {
        LinearLayout page = ui.vbox();
        page.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(10));

        LinearLayout top = ui.hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, ui.lp(-1, 44));
        TextView back = ui.label("‹", 30, ui.palette().accent, true);
        back.setGravity(Gravity.CENTER);
        top.addView(back, ui.lp(40, 40));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveDraft(context);
                editingId = null;
                host.invalidate();
            }
        });
        final EditText titleView = new EditText(context);
        titleView.setHint("Title");
        titleView.setSingleLine(true);
        titleView.setText(draftTitle);
        titleView.setTextSize(17);
        titleView.setTextColor(ui.palette().text);
        titleView.setHintTextColor(ui.palette().muted);
        titleView.setBackground(null);
        titleView.setPadding(ui.dp(6), 0, ui.dp(6), 0);
        top.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1f));
        titleView.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                draftTitle = s.toString();
            }
        });

        final EditText[] bodyView = new EditText[1];
        LinearLayout actions = ui.hbox();
        page.addView(actions, ui.lp(-1, 38));
        actions.addView(actionPill(context, ui, draftPinned ? "Pinned" : "Pin", draftPinned),
                slotParams(ui));
        actions.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { draftPinned = !draftPinned; host.invalidate(); }
        });
        actions.addView(actionPill(context, ui, draftArchived ? "Archived" : "Archive", draftArchived),
                slotParams(ui));
        actions.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { draftArchived = !draftArchived; host.invalidate(); }
        });
        actions.addView(actionPill(context, ui, "✓ line", false), slotParams(ui));
        actions.getChildAt(2).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (bodyView[0] == null) return;
                String text = bodyView[0].getText().toString();
                int line = NotesRules.lineIndexAt(text, bodyView[0].getSelectionStart());
                String toggled = NotesRules.toggleChecklistLineAt(text, line);
                if (toggled.equals(text)) {
                    // Not a checklist line: make it one, so the button always does something.
                    toggled = markLineAsChecklist(text, line);
                }
                draftBody = toggled;
                bodyView[0].setText(toggled);
            }
        });
        actions.addView(actionPill(context, ui, "Delete", false), slotParams(ui));
        actions.getChildAt(3).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmDelete(context, ui); }
        });

        LinearLayout colours = ui.hbox();
        LinearLayout.LayoutParams clp = ui.lp(-1, 34);
        clp.topMargin = ui.dp(4);
        page.addView(colours, clp);
        for (int i = 0; i < NotesRules.COLOR_COUNT; i++) {
            final int index = i;
            TextView dot = new TextView(context);
            dot.setBackground(ui.shape(noteColor(ui, i), 16));
            if (i == draftColor) {
                dot.setPadding(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3));
                dot.setText("•");
                dot.setGravity(Gravity.CENTER);
                dot.setTextColor(ui.palette().text);
            }
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, -2, 1f);
            dp.leftMargin = ui.dp(3);
            dp.rightMargin = ui.dp(3);
            colours.addView(dot, dp);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { draftColor = index; host.invalidate(); }
            });
        }

        final EditText body = new EditText(context);
        bodyView[0] = body;
        body.setHint("Note, or a checklist: start a line with [ ] ");
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        body.setText(draftBody);
        body.setTextSize(14);
        body.setTextColor(ui.palette().text);
        body.setHintTextColor(ui.palette().muted);
        body.setBackground(null);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, 0, 1f);
        bp.topMargin = ui.dp(6);
        page.addView(body, bp);
        body.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                draftBody = s.toString();
            }
        });
        return page;
    }

    private TextView actionPill(Context context, UiKit ui, String label, boolean on) {
        TextView pill = ui.pillButton(label, on ? ui.palette().accent : ui.palette().surface,
                on ? android.graphics.Color.WHITE : ui.palette().muted);
        pill.setTextSize(11);
        return pill;
    }

    private LinearLayout.LayoutParams slotParams(UiKit ui) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = ui.dp(3);
        p.rightMargin = ui.dp(3);
        return p;
    }

    private LinearLayout.LayoutParams centerParams(UiKit ui, int height) {
        LinearLayout.LayoutParams p = ui.lp(-1, height);
        p.topMargin = ui.dp(10);
        return p;
    }

    /** Turns the line under the caret into an open checklist item, keeping its text. */
    private static String markLineAsChecklist(String body, int lineIndex) {
        String[] lines = body.split("\n", -1);
        if (lineIndex < 0 || lineIndex >= lines.length) return body;
        lines[lineIndex] = "[ ] " + lines[lineIndex];
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            out.append(lines[i]);
        }
        return out.toString();
    }

    private void confirmDelete(final Context context, final UiKit ui) {
        new AppAlertBuilder(context, ui).setTitle("Delete this note?")
                .setMessage("It leaves the device. There is no bin.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        List<NotesRules.Note> all = new ArrayList<>(NotesStore.load(context));
                        List<NotesRules.Note> kept = new ArrayList<>();
                        for (NotesRules.Note n : all) if (!n.id.equals(editingId)) kept.add(n);
                        NotesStore.save(context, kept);
                        editingId = null;
                        host.invalidate();
                    }
                })
                .setNegativeButton("Keep", null).show();
    }

    /** Leaving the page is saving the page; an untouched empty new note is discarded. */
    private void saveDraft(Context context) {
        if (editingId == null) return;
        String title = draftTitle.trim();
        String bodyText = draftBody;
        List<NotesRules.Note> all = new ArrayList<>(NotesStore.load(context));
        if (editingId.isEmpty()) {
            if (title.isEmpty() && bodyText.trim().isEmpty()) return;
            all.add(new NotesRules.Note(NotesStore.newId(), title, bodyText,
                    System.currentTimeMillis(), draftColor, draftPinned, draftArchived));
        } else {
            for (int i = 0; i < all.size(); i++) {
                NotesRules.Note n = all.get(i);
                if (n.id.equals(editingId)) {
                    all.set(i, new NotesRules.Note(n.id, title, bodyText,
                            System.currentTimeMillis(), draftColor, draftPinned, draftArchived));
                    break;
                }
            }
        }
        NotesStore.save(context, all);
    }

    private static List<NotesRules.Note> filter(List<NotesRules.Note> pool, String query) {
        if (query == null || query.trim().isEmpty()) return pool;
        List<NotesRules.Note> out = new ArrayList<>();
        for (NotesRules.Note n : pool) if (n.searchable().toLowerCase(java.util.Locale.ROOT)
                .contains(query.trim().toLowerCase(java.util.Locale.ROOT))) out.add(n);
        return out;
    }

    /** The colour row: paper tints in light theme, deep tints in dark, index-stable. */
    private int noteColor(UiKit ui, int index) {
        int[] light = {0xfff4f7f5, 0xfffff3bf, 0xffd9f0d6, 0xffd8e9ff, 0xffffdcd8, 0xffe9dcff, 0xffffe4c4};
        int[] dark = {0xff20302b, 0xff453f22, 0xff25402a, 0xff253a4e, 0xff4e2e2b, 0xff392e4e, 0xff4e3a24};
        int i = Math.max(0, Math.min(NotesRules.COLOR_COUNT - 1, index));
        return ui.palette().dark ? dark[i] : light[i];
    }

    @Override public void onResume() { }

    @Override public void onPause() { }
}
