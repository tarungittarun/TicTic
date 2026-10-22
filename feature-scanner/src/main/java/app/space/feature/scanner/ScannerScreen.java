package app.space.feature.scanner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * Assembles picked images into a PDF stored in app-private storage.
 *
 * <p>Uses the framework {@link PdfDocument}, so there are no new dependencies. CameraX plus
 * ML Kit text detection would add roughly 50 MB to the install, which is what a {@code lite}
 * flavour cannot carry, and it is not needed to produce a usable document. Live capture with
 * full-resolution output would need a {@code FileProvider} to hand the camera app a writable
 * URI, and {@code FileProvider} ships with AndroidX, which this project has disabled -- so
 * pages are picked rather than shot. That limit is stated on screen rather than hidden.
 *
 * <p>The fit arithmetic is {@link ScannerRules#fitInside}: taking the minimum of the width
 * and height ratios. Scaling by width alone overflows the sheet height, and the framework
 * clips rather than reporting, so the bottom of every page would be missing with nothing to
 * indicate why.
 */
public final class ScannerScreen implements Screen {

    private static final int PICK_IMAGES = 90;
    /** Longest edge decoded when drawing. Larger bitmaps risk an OutOfMemoryError mid-write. */
    private static final int MAX_DECODE_EDGE = 1600;

    private final ScreenHost host;
    private final List<ScannerRules.Page> pages = new ArrayList<>();
    private boolean a4 = true;

    public ScannerScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Scanner"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        List<ScannerRules.Page> usable = ScannerRules.usable(pages);
        ui.sectionTitle(root, "Scanner", usable.isEmpty() ? "No pages yet."
                : ScannerRules.pageLabel(usable.size()) + " ready");

        LinearLayout card = ui.card();
        LinearLayout.LayoutParams cp = ui.lp(-1, -2);
        cp.topMargin = ui.dp(8);
        root.addView(card, cp);
        card.addView(ui.label("Pick images, then save them as one PDF.", 13, ui.palette().text, true));
        TextView note = ui.label("Pages come from the picker rather than the camera: full-resolution "
                + "capture needs a FileProvider, which ships with AndroidX, and this project has "
                + "AndroidX disabled. Nothing here adds the 50 MB a CameraX and ML Kit build would.",
                11, ui.palette().muted, false);
        note.setPadding(0, ui.dp(4), 0, ui.dp(8));
        card.addView(note);

        LinearLayout actions = ui.hbox();
        card.addView(actions, ui.lp(-1, 46));
        TextView pick = ui.pillButton("Pick images", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams pp = ui.lp(0, 46);
        pp.weight = 1f;
        actions.addView(pick, pp);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickImages(context); }
        });
        TextView sheet = ui.pillButton(a4 ? "A4" : "Letter", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams spp = ui.lp(0, 46);
        spp.weight = 1f;
        spp.leftMargin = ui.dp(6);
        actions.addView(sheet, spp);
        sheet.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { a4 = !a4; host.invalidate(); }
        });

        TextView save = ui.pillButton(usable.isEmpty() ? "Save PDF" : "Save " + usable.size()
                        + (usable.size() == 1 ? " page" : " pages") + " as PDF",
                usable.isEmpty() ? ui.palette().surface : ui.palette().accent,
                usable.isEmpty() ? ui.palette().muted : android.graphics.Color.WHITE);
        LinearLayout.LayoutParams svp = ui.lp(-1, 48);
        svp.topMargin = ui.dp(8);
        root.addView(save, svp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (usable.isEmpty()) { host.toast("Pick at least one image first."); return; }
                saveAsPdf(context, usable);
            }
        });

        for (int i = 0; i < usable.size(); i++) {
            final ScannerRules.Page page = usable.get(i);
            final int position = i;
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout rowCard = ui.card();
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(6);
            root.addView(rowCard, rp);
            rowCard.addView(row, ui.lp(-1, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            row.addView(copy, cpp);
            copy.addView(ui.label("Page " + (position + 1), 13, ui.palette().text, true));
            copy.addView(ui.label(page.pixelWidth + " \u00d7 " + page.pixelHeight + " px"
                    + (page.rotation == 0 ? "" : "  \u00b7  rotated " + page.rotation + "\u00b0")
                    + (ScannerRules.isLandscape(page) ? "  \u00b7  landscape" : ""),
                    11, ui.palette().muted, false));
            addStep(row, ui, "\u2191", position > 0, new View.OnClickListener() {
                @Override public void onClick(View v) { reorder(position, position - 1); }
            });
            addStep(row, ui, "\u2193", position < usable.size() - 1, new View.OnClickListener() {
                @Override public void onClick(View v) { reorder(position, position + 1); }
            });
            addStep(row, ui, "\u21bb", true, new View.OnClickListener() {
                @Override public void onClick(View v) { rotate(page); }
            });
            addStep(row, ui, "\u2715", true, new View.OnClickListener() {
                @Override public void onClick(View v) { remove(page); }
            });
        }

        // ---- saved documents ----
        File[] saved = folder(context).listFiles();
        List<File> documents = new ArrayList<>();
        if (saved != null) for (File f : saved) if (f.isFile() && f.getName().endsWith(".pdf")) documents.add(f);
        ui.sectionTitle(root, "Saved PDFs", documents.isEmpty() ? "None yet."
                : documents.size() + " on this device");
        for (final File document : documents) {
            LinearLayout row = ui.card();
            LinearLayout.LayoutParams rp = ui.lp(-1, -2);
            rp.topMargin = ui.dp(6);
            root.addView(row, rp);
            LinearLayout inner = ui.hbox();
            inner.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(inner, ui.lp(-1, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            inner.addView(copy, cpp);
            copy.addView(ui.label(document.getName(), 13, ui.palette().text, true));
            copy.addView(ui.label(ScannerRules.formatBytes(document.length()) + "  \u00b7  "
                    + new java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date(document.lastModified())),
                    11, ui.palette().muted, false));
            TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
            inner.addView(delete, ui.lp(-2, -2));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean ok = document.isFile() && document.delete();
                    host.toast(ok ? "Deleted." : "Could not delete that file.");
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    private void addStep(LinearLayout parent, UiKit ui, String glyph, boolean enabled,
                        View.OnClickListener listener) {
        TextView button = ui.label(glyph, 15, enabled ? ui.palette().text : ui.palette().muted, false);
        button.setGravity(Gravity.CENTER);
        button.setAlpha(enabled ? 1f : 0.3f);
        LinearLayout.LayoutParams p = ui.lp(38, 40);
        parent.addView(button, p);
        if (enabled) button.setOnClickListener(listener);
    }

    private void pickImages(Context context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        // Persisted so the page can still be read after a process restart; without it the
        // grant expires and the PDF fails to build later with no obvious cause.
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivityForResult(intent, PICK_IMAGES);
            } else {
                host.toast("Cannot open the picker here.");
            }
        } catch (Exception e) {
            host.toast("No document picker is available.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_IMAGES || data == null) return;
        if (resultCode != android.app.Activity.RESULT_OK) {
            // A cancelled pick must still reach here, or the screen stays in a state where
            // it looks like it is waiting for something that will never arrive.
            host.toast("No images added.");
            return;
        }
        Context context = host.context();
        int added = 0;
        android.content.ClipData clip = data.getClipData();
        List<Uri> uris = new ArrayList<>();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        for (Uri uri : uris) {
            ScannerRules.Page page = measure(context, uri);
            if (page != null) {
                pages.add(page);
                try {
                    context.getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                    // Not every provider offers persistable grants; the page still works for
                    // this session, and the save step reports if the read then fails.
                }
                added++;
            }
        }
        host.toast(added == 0 ? "No readable images in that selection."
                : "Added " + added + (added == 1 ? " page." : " pages."));
        host.invalidate();
    }

    /** Reads only the dimensions, without decoding pixels. */
    private ScannerRules.Page measure(Context context, Uri uri) {
        InputStream in = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            in = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) return null;
            return new ScannerRules.Page(Long.toString(System.nanoTime(), 36), uri.toString(),
                    options.outWidth, options.outHeight, 0);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    private void reorder(int from, int to) {
        List<ScannerRules.Page> reordered = ScannerRules.moved(pages, from, to);
        pages.clear();
        pages.addAll(reordered);
        host.invalidate();
    }

    private void rotate(ScannerRules.Page page) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).id.equals(page.id)) { pages.set(i, ScannerRules.rotated(page, 90)); break; }
        }
        host.invalidate();
    }

    private void remove(ScannerRules.Page page) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).id.equals(page.id)) { pages.remove(i); break; }
        }
        host.invalidate();
    }

    private void saveAsPdf(final Context context, final List<ScannerRules.Page> selected) {
        nameDialog(context, selected);
    }

    private void nameDialog(final Context context, final List<ScannerRules.Page> selected) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText label = field(context, ui, "Document name (optional)");
        form.addView(label, ui.lp(-1, -2));
        long estimate = 0;
        for (ScannerRules.Page p : selected) estimate += (long) p.pixelWidth * p.pixelHeight;
        TextView hint = ui.label("About " + ScannerRules.formatBytes(
                ScannerRules.estimatedBytes(selected.size(),
                        (int) Math.sqrt(estimate / Math.max(1, selected.size())),
                        (int) Math.sqrt(estimate / Math.max(1, selected.size())))) + " once written.",
                11, ui.palette().muted, false);
        form.addView(hint, ui.lp(-1, -2));

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle("Save PDF");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                write(context, selected, label.getText().toString());
            }
        });
        dialog.show();
    }

    /** Draws every page into one PDF. Runs inline: the work is short and mostly decoding. */
    private void write(Context context, List<ScannerRules.Page> selected, String label) {
        float[] sheet = ScannerRules.sheetSize(a4);
        PdfDocument document = new PdfDocument();
        File output = new File(folder(context),
                ScannerRules.fileName(System.currentTimeMillis(), label));
        int written = 0;
        try {
            for (int i = 0; i < selected.size(); i++) {
                ScannerRules.Page page = selected.get(i);
                Bitmap bitmap = decode(context, page);
                if (bitmap == null) continue;
                PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                        Math.round(sheet[0]), Math.round(sheet[1]), i + 1).create();
                PdfDocument.Page pdfPage = document.startPage(info);
                Canvas canvas = pdfPage.getCanvas();
                float[] fit = ScannerRules.fitInside(page, sheet[0], sheet[1]);
                float[] size = ScannerRules.rotatedSize(page);
                float drawnWidth = size[0] * fit[0];
                float drawnHeight = size[1] * fit[0];
                RectF target = new RectF(fit[1], fit[2], fit[1] + drawnWidth, fit[2] + drawnHeight);
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                if (page.rotation == 0) {
                    canvas.drawBitmap(bitmap, null, target, paint);
                } else {
                    // Rotated pages are drawn with the canvas rotated rather than by
                    // re-encoding the bitmap, which would cost memory and lose quality.
                    canvas.save();
                    canvas.rotate(page.rotation, target.centerX(), target.centerY());
                    RectF source = new RectF(target.centerX() - drawnHeight / 2f,
                            target.centerY() - drawnWidth / 2f,
                            target.centerX() + drawnHeight / 2f,
                            target.centerY() + drawnWidth / 2f);
                    canvas.drawBitmap(bitmap, null, source, paint);
                    canvas.restore();
                }
                bitmap.recycle();
                document.finishPage(pdfPage);
                written++;
            }
            if (written == 0) {
                host.toast("None of those images could be read.");
                document.close();
                return;
            }
            FileOutputStream out = new FileOutputStream(output);
            try {
                document.writeTo(out);
                out.flush();
            } finally {
                out.close();
            }
        } catch (Exception e) {
            if (output.exists()) output.delete();
            host.toast("Could not build the PDF: " + e.getClass().getSimpleName());
            return;
        } finally {
            document.close();
        }
        pages.clear();
        host.toast("Saved " + output.getName() + " (" + written
                + (written == 1 ? " page)." : " pages)."));
        host.invalidate();
    }

    /** Decodes at a bounded size so a very large photo cannot exhaust memory mid-write. */
    private Bitmap decode(Context context, ScannerRules.Page page) {
        InputStream in = null;
        try {
            Uri uri = Uri.parse(page.sourceUri);
            BitmapFactory.Options probe = new BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            in = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, probe);
            if (in != null) in.close();
            int sample = 1;
            while (Math.max(probe.outWidth, probe.outHeight) / (sample * 2) >= MAX_DECODE_EDGE) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            in = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    static File folder(Context context) {
        File dir = new File(context.getFilesDir(), "scans");
        if (!dir.exists()) dir.mkdirs();
        return dir;
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
