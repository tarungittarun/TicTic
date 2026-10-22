package app.space.feature.files;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public FilesScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Files"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
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
        List<FileRules.Entry> shown = FileRules.sorted(entries);
        long[] summary = FileRules.summarise(entries);

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
            @Override public void onClick(View v) { relative = ""; host.invalidate(); }
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

        if (shown.isEmpty()) {
            TextView empty = ui.label("This folder is empty.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            column.addView(empty, ep);
        }

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

    @Override public void onResume() { }
    @Override public void onPause() { }
}
