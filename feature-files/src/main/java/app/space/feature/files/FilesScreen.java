package app.space.feature.files;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * A browser for the files this app owns.
 *
 * <p>App-private storage only, reached with {@code getFilesDir} and
 * {@code getExternalFilesDir}. That needs no permission and no Storage Access Framework,
 * which matters here because AndroidX is disabled in this project and SAF's
 * {@code DocumentFile} helper ships with it. Every path is checked with
 * {@link FileRules#isInside} before it is opened, so no navigation step can reach outside
 * the root.
 */
public final class FilesScreen implements Screen {

    private final ScreenHost host;
    private String relative = "";
    // Editor state. It lives in the screen object, not in the views, so the text survives a
    // re-render the way a Notepad session survives a window resize.
    private boolean browsing;
    private String category;
    private String query = "";
    private int sortMode = FileRules.SORT_NAME;
    private String viewingPath;
    private android.graphics.pdf.PdfRenderer pdfRenderer;
    private int pdfPage;
    private String editingPath;
    private String editorText = "";
    private String savedText = "";
    private final java.util.ArrayDeque<String> undoStack = new java.util.ArrayDeque<>();
    private long lastUndoPush;
    private boolean wrap = true;
    private boolean mono;
    private boolean findOpen;
    private boolean replaceOpen;
    private String findText = "";
    private String replaceText = "";
    private EditText findInputView;

    public FilesScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Files"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        if (editingPath != null) return buildEditor(context, ui);
        if (viewingPath != null) return buildViewer(context, ui);
        // Samsung My Files opens on categories, not on a folder: Images, Videos, Audio,
        // Documents. The folder browser is one row down, where that app keeps storage locations.
        if (category != null) return inScroll(buildCategoryPage(context, ui));
        if (!browsing) return inScroll(buildHome(context, ui));
        final File root = context.getFilesDir();
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout column = ui.vbox();
        column.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(column, new LinearLayout.LayoutParams(-1, -2));

        final File current = relative.isEmpty() ? root : new File(root, relative);
        File[] children = current.listFiles();
        List<FileRules.Entry> entries = new ArrayList<>();
        if (children != null) {
            for (File f : children) {
                entries.add(new FileRules.Entry(f.getName(), f.isDirectory(), f.length(), f.lastModified()));
            }
        }
        List<FileRules.Entry> shown = FileRules.sortBy(entries, sortMode);
        long[] summary = FileRules.summarise(entries);

        final String folder = relative;
        ui.sectionTitle(column, "Files",
                summary[0] + (summary[0] == 1 ? " folder" : " folders") + "  \u00b7  "
                        + summary[1] + (summary[1] == 1 ? " file" : " files") + "  \u00b7  "
                        + FileRules.formatBytes(summary[2]));

        // ---- breadcrumb ----
        LinearLayout crumbs = ui.hbox();
        crumbs.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cp = ui.lp(-1, 40);
        cp.topMargin = ui.dp(6);
        column.addView(crumbs, cp);
        TextView homeCrumb = ui.label("Files", 12, relative.isEmpty() ? ui.palette().accent : ui.palette().muted, true);
        crumbs.addView(homeCrumb, ui.lp(-2, -2));
        homeCrumb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                relative = "";
                browsing = false;
                category = null;
                host.invalidate();
            }
        });
        String walked = "";
        for (String part : FileRules.crumbs(relative)) {
            walked = FileRules.join(walked, part);
            final String target = walked;
            TextView sep = ui.label(" \u203a ", 12, ui.palette().muted, false);
            crumbs.addView(sep, ui.lp(-2, -2));
            TextView crumb = ui.label(part, 12, ui.palette().accent, false);
            crumbs.addView(crumb, ui.lp(-2, -2));
            crumb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { relative = target; host.invalidate(); }
            });
        }

        TextView create = ui.pillButton("New text file", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams createParams = ui.lp(-1, 46);
        createParams.topMargin = ui.dp(10);
        column.addView(create, createParams);
        create.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { promptNewFile(context, ui, root, folder); }
        });

        if (shown.isEmpty()) {
            // Worded without assuming any other app is installed: the suite is modular, so a
            // build may not contain the apps that would write here.
            TextView empty = ui.label("Nothing here yet. Create a text file below, and files "
                    + "saved by other apps in this suite appear here too.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            column.addView(empty, ep);
        }

        column.addView(sortRow(context, ui), ui.lp(-1, 40));
        for (final FileRules.Entry entry : shown) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(6);
            column.addView(card, rp);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, ui.lp(-1, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            row.addView(copy, cpp);
            copy.addView(ui.label(entry.name, 14, ui.palette().text, true));
            String meta = entry.directory ? "Folder"
                    : FileRules.kindOf(entry.name) + "  \u00b7  " + FileRules.formatBytes(entry.bytes);
            copy.addView(ui.label(meta, 11, ui.palette().muted, false));
            if (entry.directory) {
                TextView open = ui.label("Open \u203a", 12, ui.palette().accent, true);
                row.addView(open, ui.lp(-2, -2));
                open.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String next = FileRules.join(relative, entry.name);
                        // Never navigate somewhere the guard rejects, even if a name on disk
                        // somehow contains a separator.
                        if (!FileRules.isInside(next)) { host.toast("Cannot open that folder."); return; }
                        relative = next;
                        host.invalidate();
                    }
                });
            } else {
                if (EditorRules.isEditableName(entry.name)) {
                    // A text file that cannot be opened is a shelf, not a files app.
                    TextView edit = ui.label("Edit \u203a", 12, ui.palette().accent, true);
                    row.addView(edit, ui.lp(-2, -2));
                    edit.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            openEditor(context, FileRules.join(relative, entry.name));
                        }
                    });
                }
                TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
                row.addView(delete, ui.lp(-2, -2));
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String path = FileRules.join(relative, entry.name);
                        if (!FileRules.isInside(path)) return;
                        File target = new File(root, path);
                        boolean ok = target.isFile() && target.delete();
                        host.toast(ok ? "Deleted " + entry.name : "Could not delete " + entry.name);
                        host.invalidate();
                    }
                });
            }
        }
        return scroll;
    }

    /**
     * Creates a text file in the folder being browsed.
     *
     * <p>Without this the screen is a read-only browser over app-private storage, which on a
     * fresh install is empty: the summary, the list and the empty state all report that
     * accurately, and the result is still an app that appears to do nothing. Other apps write
     * here only when they produce something, so nothing exists until a recording or a scan is
     * saved -- and a tester reaching Files first would reasonably conclude it was broken.
     *
     * <p>Name and path go through the same rules the browser uses, so the one place this screen
     * writes cannot be used to write outside the root.
     */
    private void promptNewFile(final Context context, final UiKit ui, final File root, final String folder) {
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = ui.dp(20);
        form.setPadding(pad, pad / 2, pad, 0);
        final EditText name = new EditText(context);
        name.setHint("name, e.g. notes.txt");
        name.setSingleLine(true);
        form.addView(name, new LinearLayout.LayoutParams(-1, -2));
        final EditText body = new EditText(context);
        body.setHint("contents (optional)");
        body.setMinLines(3);
        form.addView(body, new LinearLayout.LayoutParams(-1, -2));
        new AppAlertBuilder(context, ui).setTitle("New text file").setView(form)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String wanted = name.getText().toString().trim();
                        if (!FileRules.isSafeName(wanted)) {
                            host.toast("That name is not allowed.");
                            return;
                        }
                        if (!FileRules.isInside(folder)) {
                            host.toast("That folder is outside this app's files.");
                            return;
                        }
                        File target = new File(folder.isEmpty() ? root : new File(root, folder), wanted);
                        if (target.exists()) {
                            host.toast("Something is already named " + wanted);
                            return;
                        }
                        OutputStream out = null;
                        try {
                            out = new FileOutputStream(target);
                            out.write(body.getText().toString().getBytes("UTF-8"));
                        } catch (Exception e) {
                            host.toast("Could not create the file: " + e.getClass().getSimpleName());
                            return;
                        } finally {
                            if (out != null) {
                                try { out.close(); } catch (Exception ignored) { }
                            }
                        }
                        // Straight into the editor: a create dialog that closes onto a file
                        // list invites the owner's exact complaint, that this was a notes stub
                        // and not an editor.
                        openEditor(context, FileRules.join(folder, wanted));
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }


    /**
     * A plain-text editor in the Windows Notepad sense: the page is the text, the chrome is two
     * thin rows, and save means save. Find and replace are literal and wrap at both ends; undo
     * groups keystrokes the way a typist expects, roughly a word at a time; wrap and monospace
     * are the two view modes Notepad users reach for first.
     */
    private View buildEditor(final Context context, final UiKit ui) {
        final File target = new File(context.getFilesDir(), editingPath);
        LinearLayout page = ui.vbox();
        page.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(10));

        final TextView status = ui.label(statusNow(editorText, 0), 11, ui.palette().muted, false);

        LinearLayout top = ui.hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, ui.lp(-1, 44));
        TextView back = ui.label("\u2039", 30, ui.palette().accent, true);
        back.setGravity(Gravity.CENTER);
        top.addView(back, ui.lp(40, 40));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editingPath = null;
                findOpen = false;
                replaceOpen = false;
                host.invalidate();
            }
        });
        TextView nameView = ui.label(editingPath, 14, ui.palette().text, true);
        nameView.setMaxLines(1);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
        nameParams.leftMargin = ui.dp(8);
        nameParams.rightMargin = ui.dp(8);
        top.addView(nameView, nameParams);
        TextView save = ui.pillButton("Save", ui.palette().accent, android.graphics.Color.WHITE);
        top.addView(save, ui.lp(-2, 38));
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { writeEditor(context, target, status); }
        });
        save.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                promptSaveAs(context, ui, target);
                return true;
            }
        });
        save.setContentDescription("Save. Long press for save as.");

        LinearLayout tools = ui.hbox();
        LinearLayout.LayoutParams toolParams = ui.lp(-1, 40);
        toolParams.topMargin = ui.dp(4);
        page.addView(tools, toolParams);
        final EditText[] box = new EditText[1];
        tools.addView(toolButton(context, ui, "Find", findOpen), toolSlot(ui));
        tools.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { findOpen = !findOpen; replaceOpen = false; host.invalidate(); }
        });
        tools.addView(toolButton(context, ui, "Replace", replaceOpen), toolSlot(ui));
        tools.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                replaceOpen = !replaceOpen;
                if (replaceOpen) findOpen = true;
                host.invalidate();
            }
        });
        tools.addView(toolButton(context, ui, "Wrap", wrap), toolSlot(ui));
        tools.getChildAt(2).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { wrap = !wrap; host.invalidate(); }
        });
        tools.addView(toolButton(context, ui, "Mono", mono), toolSlot(ui));
        tools.getChildAt(3).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mono = !mono; host.invalidate(); }
        });
        tools.addView(toolButton(context, ui, "Undo", false), toolSlot(ui));
        tools.getChildAt(4).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (box[0] == null || undoStack.isEmpty()) { host.toast("Nothing to undo."); return; }
                editorText = undoStack.removeFirst();
                box[0].setText(editorText);
                box[0].setSelection(Math.min(editorText.length(), box[0].getSelectionStart()));
                status.setText(statusNow(editorText, box[0].getSelectionStart()));
            }
        });

        if (findOpen) page.addView(findRow(context, ui, box, status), ui.lp(-1, 42));
        if (replaceOpen) page.addView(replaceRow(context, ui, box, status), ui.lp(-1, 42));

        final EditText text = new EditText(context);
        box[0] = text;
        text.setText(editorText);
        text.setGravity(Gravity.TOP | Gravity.START);
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        text.setHorizontallyScrolling(!wrap);
        text.setVerticalScrollBarEnabled(true);
        if (mono) text.setTypeface(android.graphics.Typeface.MONOSPACE);
        text.setBackgroundColor(0x00000000);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        textParams.topMargin = ui.dp(6);
        page.addView(text, textParams);
        final String[] before = new String[1];
        text.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                before[0] = s.toString();
            }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                long now = System.currentTimeMillis();
                // Grouped undo: a snapshot per pause in typing rather than per keystroke, so the
                // button undoes a word or a paste, not a letter. Notepad users press undo once
                // per mistake, not once per character.
                if (before[0] != null && now - lastUndoPush > 1500) {
                    pushUndo(before[0]);
                    lastUndoPush = now;
                }
                editorText = s.toString();
                status.setText(statusNow(editorText, text.getSelectionStart()));
            }
        });

        LinearLayout.LayoutParams sp = ui.lp(-1, 22);
        sp.topMargin = ui.dp(4);
        page.addView(status, sp);
        return page;
    }

    private LinearLayout.LayoutParams toolSlot(UiKit ui) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.leftMargin = ui.dp(3);
        p.rightMargin = ui.dp(3);
        return p;
    }

    private TextView toolButton(Context context, UiKit ui, String label, boolean on) {
        TextView b = ui.pillButton(label, on ? ui.palette().accent : ui.palette().surface,
                on ? android.graphics.Color.WHITE : ui.palette().text);
        b.setTextSize(12);
        return b;
    }

    private View findRow(final Context context, final UiKit ui, final EditText[] box,
                         final TextView status) {
        LinearLayout row = ui.hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        final EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint("find");
        input.setText(findText);
        input.setTextSize(13);
        findInputView = input;
        row.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView count = ui.label("", 11, ui.palette().muted, false);
        row.addView(count, ui.lp(-2, -2));
        TextView prev = ui.pillButton("\u2039", ui.palette().surface, ui.palette().text);
        row.addView(prev, ui.lp(40, 36));
        TextView next = ui.pillButton("\u203a", ui.palette().surface, ui.palette().text);
        row.addView(next, ui.lp(40, 36));
        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stepFind(box[0], input, count, false); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stepFind(box[0], input, count, true); }
        });
        return row;
    }

    private void stepFind(EditText box, EditText input, TextView count, boolean forward) {
        findText = input.getText().toString();
        if (findText.isEmpty() || box == null) { count.setText(""); return; }
        int from = forward ? box.getSelectionEnd() : box.getSelectionStart();
        int at = forward
                ? EditorRules.nextOccurrence(editorText, findText, from)
                : EditorRules.previousOccurrence(editorText, findText, from);
        if (at < 0) { count.setText("0 of " + EditorRules.countOccurrences(editorText, findText)); return; }
        box.setSelection(at, at + findText.length());
        int here = EditorRules.countOccurrences(editorText.substring(0, at), findText) + 1;
        count.setText(here + " of " + EditorRules.countOccurrences(editorText, findText));
    }

    private View replaceRow(final Context context, final UiKit ui, final EditText[] box,
                            final TextView status) {
        LinearLayout row = ui.hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        final EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint("replace with");
        input.setText(replaceText);
        input.setTextSize(13);
        row.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView one = ui.pillButton("One", ui.palette().surface, ui.palette().text);
        row.addView(one, ui.lp(56, 36));
        TextView all = ui.pillButton("All", ui.palette().accent, android.graphics.Color.WHITE);
        row.addView(all, ui.lp(56, 36));
        one.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (box[0] == null || findText.isEmpty()) return;
                replaceText = input.getText().toString();
                int a = box[0].getSelectionStart();
                int b = box[0].getSelectionEnd();
                if (b <= a) { host.toast("Find a match first, then replace it."); return; }
                String selected = editorText.substring(a, b);
                if (!selected.equals(findText)) { host.toast("The selection is not a match."); return; }
                pushUndo(editorText);
                box[0].getText().replace(a, b, replaceText);
            }
        });
        all.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (box[0] == null) return;
                replaceText = input.getText().toString();
                if (findInputView != null) findText = findInputView.getText().toString();
                int n = EditorRules.countOccurrences(editorText, findText);
                if (n == 0) { host.toast("Nothing to replace."); return; }
                pushUndo(editorText);
                editorText = EditorRules.replaceAllLiteral(editorText, findText, replaceText);
                box[0].setText(editorText);
                host.toast(n + (n == 1 ? " replacement." : " replacements."));
                status.setText(statusNow(editorText, box[0].getSelectionStart()));
            }
        });
        return row;
    }

    private void pushUndo(String snapshot) {
        undoStack.addFirst(snapshot);
        while (undoStack.size() > 25) undoStack.removeLast();
    }

    private String statusNow(String text, int caret) {
        return EditorRules.statusLabel(text, caret) + "  \u00b7  " + text.length()
                + (text.length() == 1 ? " char" : " chars")
                + (text.equals(savedText) ? "" : "  \u00b7  modified");
    }

    private void openEditor(Context context, String path) {
        File f = new File(context.getFilesDir(), path);
        if (!FileRules.isInside(path) || !f.isFile()) { host.toast("Cannot open that file."); return; }
        editingPath = path;
        editorText = readText(f);
        savedText = editorText;
        undoStack.clear();
        lastUndoPush = 0;
        findOpen = false;
        replaceOpen = false;
        host.invalidate();
    }

    private static String readText(File f) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            StringBuilder out = new StringBuilder();
            char[] buf = new char[8192];
            int read;
            while ((read = in.read(buf)) > 0) out.append(buf, 0, read);
            return out.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (in != null) { try { in.close(); } catch (Exception ignored) { } }
        }
    }

    private void writeEditor(Context context, File target, TextView status) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(target);
            out.write(editorText.getBytes("UTF-8"));
        } catch (Exception e) {
            host.toast("Could not save: " + e.getClass().getSimpleName());
            return;
        } finally {
            if (out != null) { try { out.close(); } catch (Exception ignored) { } }
        }
        savedText = editorText;
        host.toast("Saved " + target.getName());
        status.setText(statusNow(editorText, 0));
    }

    private void promptSaveAs(final Context context, final UiKit ui, final File current) {
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = ui.dp(20);
        form.setPadding(pad, pad / 2, pad, 0);
        final EditText name = new EditText(context);
        name.setHint("new name, e.g. draft-2.txt");
        name.setSingleLine(true);
        form.addView(name, new LinearLayout.LayoutParams(-1, -2));
        new AppAlertBuilder(context, ui).setTitle("Save as").setView(form)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String wanted = name.getText().toString().trim();
                        if (!FileRules.isSafeName(wanted)) { host.toast("That name is not allowed."); return; }
                        String parent = editingPath.contains("/")
                                ? editingPath.substring(0, editingPath.lastIndexOf('/')) : "";
                        String path = FileRules.join(parent, wanted);
                        if (!FileRules.isInside(path)) { host.toast("That folder is outside this app's files."); return; }
                        File target = new File(context.getFilesDir(), path);
                        if (target.exists()) { host.toast("Something is already named " + wanted); return; }
                        writeEditor(context, target, new TextView(context));
                        editingPath = path;
                        host.invalidate();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }


    private View inScroll(LinearLayout page) {
        ScrollView scroll = new ScrollView(page.getContext());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(page, new LinearLayout.LayoutParams(-1, -2));
        return scroll;
    }

    /** A file found by the recursive scan, with the path the editor and the guards need. */
    private static final class Found {
        final String path;
        final FileRules.Entry entry;
        Found(String path, FileRules.Entry entry) { this.path = path; this.entry = entry; }
    }

    private List<Found> scan(Context context) {
        List<Found> out = new ArrayList<>();
        walk(context.getFilesDir(), "", 0, out);
        return out;
    }

    // Bounded on purpose: a category page that walks ten thousand files on every render is a
    // stutter the user feels, and app-private storage has no business being that deep.
    private void walk(File dir, String rel, int depth, List<Found> out) {
        if (depth > 6 || out.size() > 1500) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File f : children) {
            String path = FileRules.join(rel, f.getName());
            if (f.isDirectory()) { walk(f, path, depth + 1, out); continue; }
            out.add(new Found(path, new FileRules.Entry(f.getName(), false, f.length(), f.lastModified())));
        }
    }

    private View buildHome(final Context context, final UiKit ui) {
        LinearLayout page = ui.vbox();
        page.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        final List<Found> all = scan(context);
        long bytes = 0;
        for (Found f : all) bytes += f.entry.bytes;
        ui.sectionTitle(page, "Files",
                all.size() + (all.size() == 1 ? " file" : " files") + "  \u00b7  "
                        + FileRules.formatBytes(bytes));

        LinearLayout searchRow = ui.hbox();
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(searchRow, ui.lp(-1, 44));
        final EditText q = new EditText(context);
        q.setHint("search files");
        q.setSingleLine(true);
        q.setText(query);
        q.setTextSize(14);
        searchRow.addView(q, new LinearLayout.LayoutParams(0, -2, 1f));
        q.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                query = q.getText().toString();
                host.invalidate();
                return true;
            }
        });
        TextView clear = ui.pillButton("\u00d7", ui.palette().surface, ui.palette().text);
        searchRow.addView(clear, ui.lp(40, 38));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { query = ""; host.invalidate(); }
        });

        if (!query.trim().isEmpty()) {
            List<Found> hits = new ArrayList<>();
            for (Found f : all) if (FileRules.matchesQuery(f.entry.name, query)) hits.add(f);
            page.addView(ui.label(hits.size() + (hits.size() == 1 ? " result for " : " results for ")
                    + query, 12, ui.palette().muted, false), ui.lp(-1, -2));
            page.addView(sortRow(context, ui), ui.lp(-1, 40));
            if (hits.isEmpty()) {
                page.addView(ui.label("Nothing matches. Search looks at every file this app owns, "
                        + "in every folder.", 13, ui.palette().muted, false), ui.lp(-1, -2));
            }
            for (Found f : sortFound(hits)) page.addView(fileCard(context, ui, f), cardParams(ui));
            return page;
        }

        for (String cat : new String[]{"Images", "Videos", "Audio", "Documents", "Sheets", "Other"}) {
            int n = 0;
            for (Found f : all) if (FileRules.categoryOfName(f.entry.name).equals(cat)) n++;
            LinearLayout card = ui.card();
            page.addView(card, cardParams(ui));
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, ui.lp(-1, -2));
            row.addView(ui.label(cat, 15, ui.palette().text, true),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(ui.label(n + (n == 1 ? " file" : " files"), 11, ui.palette().muted, false),
                    ui.lp(-2, -2));
            row.addView(ui.label("\u203a", 16, ui.palette().accent, true), ui.lp(-2, -2));
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { category = cat; host.invalidate(); }
            });
        }

        LinearLayout store = ui.card();
        page.addView(store, cardParams(ui));
        LinearLayout srow = ui.hbox();
        srow.setGravity(Gravity.CENTER_VERTICAL);
        store.addView(srow, ui.lp(-1, -2));
        srow.addView(ui.label("Internal storage", 15, ui.palette().text, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        srow.addView(ui.label("\u203a", 16, ui.palette().accent, true), ui.lp(-2, -2));
        store.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { browsing = true; relative = ""; host.invalidate(); }
        });
        return page;
    }

    private View buildCategoryPage(final Context context, final UiKit ui) {
        LinearLayout page = ui.vbox();
        page.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        LinearLayout top = ui.hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, ui.lp(-1, 44));
        TextView back = ui.label("\u2039", 30, ui.palette().accent, true);
        back.setGravity(Gravity.CENTER);
        top.addView(back, ui.lp(40, 40));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { category = null; host.invalidate(); }
        });
        List<Found> mine = new ArrayList<>();
        for (Found f : scan(context)) {
            if (FileRules.categoryOfName(f.entry.name).equals(category)) mine.add(f);
        }
        TextView title = ui.label(category, 20, ui.palette().text, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.leftMargin = ui.dp(8);
        top.addView(title, tp);
        top.addView(ui.label(mine.size() + "", 13, ui.palette().muted, false), ui.lp(-2, -2));
        page.addView(sortRow(context, ui), ui.lp(-1, 40));
        if (mine.isEmpty()) {
            page.addView(ui.label("Nothing in this category yet. Files land here when another app "
                    + "in the suite saves them, or when you create them.", 13, ui.palette().muted, false),
                    ui.lp(-1, -2));
        }
        for (Found f : sortFound(mine)) page.addView(fileCard(context, ui, f), cardParams(ui));
        return page;
    }

    private List<Found> sortFound(List<Found> in) {
        List<Found> out = new ArrayList<>(in);
        final int mode = sortMode;
        java.util.Collections.sort(out, new java.util.Comparator<Found>() {
            @Override public int compare(Found a, Found b) {
                int c = 0;
                if (mode == FileRules.SORT_DATE) c = Long.compare(b.entry.modifiedAt, a.entry.modifiedAt);
                else if (mode == FileRules.SORT_SIZE) c = Long.compare(b.entry.bytes, a.entry.bytes);
                if (c == 0) c = a.entry.name.compareToIgnoreCase(b.entry.name);
                return c != 0 ? c : a.entry.name.compareTo(b.entry.name);
            }
        });
        return out;
    }

    private View sortRow(final Context context, final UiKit ui) {
        LinearLayout row = ui.hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        String[] labels = {"Name", "Date", "Size"};
        for (int i = 0; i < labels.length; i++) {
            final int mode = i;
            TextView pill = ui.pillButton(labels[i],
                    sortMode == i ? ui.palette().accent : ui.palette().surface,
                    sortMode == i ? android.graphics.Color.WHITE : ui.palette().muted);
            pill.setTextSize(11);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
            p.leftMargin = ui.dp(3);
            p.rightMargin = ui.dp(3);
            row.addView(pill, p);
            pill.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { sortMode = mode; host.invalidate(); }
            });
        }
        return row;
    }

    private LinearLayout.LayoutParams cardParams(UiKit ui) {
        LinearLayout.LayoutParams p = ui.lp(-1, -2);
        p.topMargin = ui.dp(6);
        return p;
    }

    private View fileCard(final Context context, final UiKit ui, final Found f) {
        LinearLayout card = ui.card();
        LinearLayout row = ui.hbox();
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row, ui.lp(-1, -2));
        LinearLayout copy = ui.vbox();
        copy.addView(ui.label(f.entry.name, 14, ui.palette().text, true));
        String parent = f.path.contains("/") ? f.path.substring(0, f.path.lastIndexOf('/')) : "";
        copy.addView(ui.label((parent.isEmpty() ? "root" : parent) + "  \u00b7  "
                + FileRules.kindOf(f.entry.name) + "  \u00b7  "
                + FileRules.formatBytes(f.entry.bytes), 11, ui.palette().muted, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        if (EditorRules.isEditableName(f.entry.name)) {
            TextView edit = ui.label("Edit \u203a", 12, ui.palette().accent, true);
            row.addView(edit, ui.lp(-2, -2));
            edit.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { openEditor(context, f.path); }
            });
        }
        // The viewing half of the owner's fold: spreadsheets as tables, documents and images
        // in place, inside Files rather than in an app of their own.
        String kind = FileRules.kindOf(f.entry.name);
        if (kind.equals("Sheet") || kind.equals("Image") || kind.equals("PDF")) {
            TextView open = ui.label(kind.equals("Sheet") ? "View \u203a" : "Open \u203a",
                    12, ui.palette().accent, true);
            row.addView(open, ui.lp(-2, -2));
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    viewingPath = f.path;
                    pdfPage = 0;
                    host.invalidate();
                }
            });
        }
        return card;
    }


    // ---- viewers: the document half of "fold spreadsheet/doc viewing into Files" ----

    private View buildViewer(final Context context, final UiKit ui) {
        final File target = new File(context.getFilesDir(), viewingPath);
        final String kind = FileRules.kindOf(viewingPath);
        LinearLayout page = ui.vbox();
        page.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(10));
        LinearLayout top = ui.hbox();
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, ui.lp(-1, 44));
        TextView back = ui.label("‹", 30, ui.palette().accent, true);
        back.setGravity(Gravity.CENTER);
        top.addView(back, ui.lp(40, 40));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeViewer(); }
        });
        TextView nameView = ui.label(viewingPath, 14, ui.palette().text, true);
        nameView.setMaxLines(1);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        np.leftMargin = ui.dp(8);
        np.rightMargin = ui.dp(8);
        top.addView(nameView, np);
        top.addView(ui.label(kind, 11, ui.palette().muted, false), ui.lp(-2, -2));
        if (!target.isFile()) {
            page.addView(ui.label("That file is missing.", 13, ui.palette().muted, false),
                    ui.lp(-1, -2));
            return page;
        }
        if (kind.equals("Sheet")) {
            page.addView(sheetTable(context, ui, readText(target)),
                    new LinearLayout.LayoutParams(-1, 0, 1f));
            return page;
        }
        if (kind.equals("Image")) {
            page.addView(imageView(context, ui, target), new LinearLayout.LayoutParams(-1, 0, 1f));
            return page;
        }
        if (kind.equals("PDF")) {
            return pdfViewer(context, ui, page, target);
        }
        page.addView(ui.label("No viewer for " + kind + " files. Text and markup open in the "
                + "editor instead.", 13, ui.palette().muted, false), ui.lp(-1, -2));
        return page;
    }

    private void closeViewer() {
        viewingPath = null;
        closePdf();
        host.invalidate();
    }

    /**
     * A spreadsheet as an actual table: one row of cells per record, header emphasised, wide
     * enough to scroll sideways. Parsed with the suite's RFC-4180 decoder, so a quoted comma
     * stays inside its cell instead of shifting every column after it.
     */
    private View sheetTable(Context context, UiKit ui, String text) {
        List<List<String>> rows = app.space.core.domain.DocumentRules.decodeSheet(text);
        HorizontalScrollView h = new HorizontalScrollView(context);
        h.setHorizontalScrollBarEnabled(true);
        LinearLayout table = ui.vbox();
        table.setPadding(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(12));
        int shown = 0;
        for (List<String> row : rows) {
            if (shown >= 200) {
                table.addView(ui.label((rows.size() - 200) + " more rows not shown", 11,
                        ui.palette().muted, false), ui.lp(-2, -2));
                break;
            }
            LinearLayout r = ui.hbox();
            for (String cell : row) {
                TextView c = ui.label(cell == null ? "" : cell, 12,
                        shown == 0 ? ui.palette().text : ui.palette().muted, shown == 0);
                c.setMinWidth(ui.dp(96));
                c.setPadding(ui.dp(8), ui.dp(5), ui.dp(8), ui.dp(5));
                r.addView(c, ui.lp(-2, -2));
            }
            table.addView(r, ui.lp(-2, -2));
            shown++;
        }
        if (shown == 0) {
            table.addView(ui.label("The sheet is empty.", 13, ui.palette().muted, false),
                    ui.lp(-2, -2));
        }
        h.addView(table, new LinearLayout.LayoutParams(-2, -2));
        return h;
    }

    /** Decoded downscaled to fit memory: a 12-megapixel photo must not take the app down. */
    private View imageView(Context context, UiKit ui, File target) {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(target.getAbsolutePath(), o);
        int scale = 1;
        while (o.outWidth / scale > 2048 || o.outHeight / scale > 2048) scale *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = scale;
        android.graphics.Bitmap bmp =
                android.graphics.BitmapFactory.decodeFile(target.getAbsolutePath(), o);
        if (bmp == null) {
            return ui.label("Could not decode that image.", 13, ui.palette().muted, false);
        }
        ImageView view = new ImageView(context);
        view.setImageBitmap(bmp);
        view.setAdjustViewBounds(true);
        view.setContentDescription(target.getName());
        return view;
    }

    /**
     * PDF pages through the framework renderer -- no AndroidX, no third-party kit. One page on
     * screen with prev/next under it, which is what a phone-sized document viewer needs.
     */
    private View pdfViewer(Context context, UiKit ui, LinearLayout page, File target) {
        closePdf();
        try {
            pdfRenderer = new android.graphics.pdf.PdfRenderer(
                    android.os.ParcelFileDescriptor.open(target,
                            android.os.ParcelFileDescriptor.MODE_READ_ONLY));
        } catch (Exception e) {
            page.addView(ui.label("Could not open that PDF: " + e.getClass().getSimpleName(),
                    13, ui.palette().muted, false), ui.lp(-1, -2));
            return page;
        }
        final ImageView pageView = new ImageView(context);
        pageView.setBackgroundColor(0xffffffff);
        page.addView(pageView, new LinearLayout.LayoutParams(-1, 0, 1f));
        final TextView pageLabel = ui.label("", 12, ui.palette().muted, false);
        pageLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = ui.lp(-1, 26);
        lp.topMargin = ui.dp(6);
        page.addView(pageLabel, lp);
        LinearLayout nav = ui.hbox();
        page.addView(nav, ui.lp(-1, 44));
        TextView prev = ui.pillButton("‹ previous", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0, -2, 1f);
        pp.rightMargin = ui.dp(4);
        nav.addView(prev, pp);
        TextView next = ui.pillButton("next ›", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams npp = new LinearLayout.LayoutParams(0, -2, 1f);
        npp.leftMargin = ui.dp(4);
        nav.addView(next, npp);
        renderPdfPage(pageView, pageLabel);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pdfPage > 0) { pdfPage--; renderPdfPage(pageView, pageLabel); }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pdfRenderer != null && pdfPage < pdfRenderer.getPageCount() - 1) {
                    pdfPage++;
                    renderPdfPage(pageView, pageLabel);
                }
            }
        });
        return page;
    }

    private void renderPdfPage(ImageView view, TextView label) {
        if (pdfRenderer == null) return;
        android.graphics.pdf.PdfRenderer.Page page = null;
        try {
            page = pdfRenderer.openPage(pdfPage);
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                    page.getWidth(), page.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
            bmp.eraseColor(0xffffffff);
            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            view.setImageBitmap(bmp);
            label.setText("Page " + (pdfPage + 1) + " of " + pdfRenderer.getPageCount());
        } catch (Exception e) {
            label.setText("Page " + (pdfPage + 1) + " could not be rendered.");
        } finally {
            if (page != null) { try { page.close(); } catch (Exception ignored) { } }
        }
    }

    private void closePdf() {
        if (pdfRenderer != null) {
            try { pdfRenderer.close(); } catch (Exception ignored) { }
            pdfRenderer = null;
        }
        pdfPage = 0;
    }

    @Override public void onResume() { }
    @Override public void onPause() { closePdf(); }
}
