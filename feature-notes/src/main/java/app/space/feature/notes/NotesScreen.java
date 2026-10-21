package app.space.feature.notes;

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

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * The notes list: search, add, edit and delete.
 *
 * <p>State lives in the store, not in the views, so a re-render cannot lose a note. Every
 * write ends with {@link ScreenHost#invalidate()} because the list is rebuilt rather than
 * patched, matching how the clock renders.
 */
public final class NotesScreen implements Screen {

    private final ScreenHost host;
    private String query = "";

    public NotesScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Notes"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        ui.sectionTitle(root, "Notes", "Search, add and edit. Everything stays on this device.");

        final EditText search = new EditText(context);
        search.setHint("Search notes");
        search.setSingleLine(true);
        search.setTextColor(ui.palette().text);
        search.setHintTextColor(ui.palette().muted);
        search.setTextSize(14);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setText(query);
        search.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        search.setBackground(ui.shape(ui.palette().surface, 14));
        LinearLayout.LayoutParams sp = ui.lp(-1, -2);
        sp.topMargin = ui.dp(8);
        root.addView(search, sp);
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                query = s.toString();
                host.invalidate();
            }
        });

        List<NotesRules.Note> shown = NotesRules.filter(NotesStore.load(context), query);

        TextView add = ui.pillButton("New note", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(10);
        root.addView(add, ap);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editor(context, null); }
        });

        if (shown.isEmpty()) {
            TextView empty = ui.label(query.isEmpty() ? "No notes yet." : "Nothing matches that search.",
                    13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }

        for (final NotesRules.Note note : shown) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(card, cp);
            card.addView(ui.label(NotesRules.displayTitle(note), 15, ui.palette().text, true));
            TextView body = ui.label(NotesRules.preview(note.body, 120), 12, ui.palette().muted, false);
            body.setPadding(0, ui.dp(3), 0, ui.dp(6));
            card.addView(body);
            LinearLayout actions = ui.hbox();
            card.addView(actions, ui.lp(-1, -2));
            TextView edit = ui.pillButton("Edit", ui.palette().surface, ui.palette().text);
            LinearLayout.LayoutParams epp = ui.lp(0, 40);
            epp.weight = 1f;
            actions.addView(edit, epp);
            edit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { editor(context, note); }
            });
            TextView delete = ui.pillButton("Delete", ui.palette().surface, ui.palette().muted);
            LinearLayout.LayoutParams dpp = ui.lp(0, 40);
            dpp.weight = 1f;
            dpp.leftMargin = ui.dp(6);
            actions.addView(delete, dpp);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    List<NotesRules.Note> all = new ArrayList<>(NotesStore.load(context));
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).id.equals(note.id)) { all.remove(i); break; }
                    }
                    NotesStore.save(context, all);
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    /** Add or edit one note. The dialog is the app's own, built from the host's palette. */
    private void editor(final Context context, final NotesRules.Note existing) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText title = field(context, ui, "Title", InputType.TYPE_CLASS_TEXT);
        final EditText body = field(context, ui, "Note",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        body.setMinLines(4);
        if (existing != null) { title.setText(existing.title); body.setText(existing.body); }
        form.addView(title, ui.lp(-1, -2));
        LinearLayout.LayoutParams bp = ui.lp(-1, -2);
        bp.topMargin = ui.dp(8);
        form.addView(body, bp);

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle(existing == null ? "New note" : "Edit note");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                List<NotesRules.Note> all = new ArrayList<>(NotesStore.load(context));
                String t = title.getText().toString();
                String b = body.getText().toString();
                if (existing == null) {
                    all.add(new NotesRules.Note(NotesStore.newId(), t, b, System.currentTimeMillis()));
                } else {
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).id.equals(existing.id)) {
                            all.set(i, new NotesRules.Note(existing.id, t, b, System.currentTimeMillis()));
                            break;
                        }
                    }
                }
                NotesStore.save(context, all);
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

    @Override public void onResume() { }
    @Override public void onPause() { }
}
