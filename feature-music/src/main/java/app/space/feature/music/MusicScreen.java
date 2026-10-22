package app.space.feature.music;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.space.core.data.AppPrefs;
import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * Plays audio files this app owns and stream URLs the user adds.
 *
 * <p>No catalogue and no partner SDK. That is the licensing constraint respected rather
 * than worked around: streaming a commercial service needs an agreement this app does not
 * have, so what is offered is the user's own files and URLs the user supplies.
 *
 * <p>The queue order comes from {@link MusicRules#shuffled} with a fixed seed rather than
 * from a fresh random draw, so the order cannot change while the user is looking at it.
 * The player is released in {@link #onPause}, because a MediaPlayer left running holds the
 * audio focus and the decode session across a background transition.
 */
public final class MusicScreen implements Screen {

    static final String KEY_STREAMS = "music_streams";
    static final String KEY_REPEAT = "music_repeat";
    static final String KEY_SHUFFLE = "music_shuffle";
    /** Audio the user picked through the system picker, stored as persistable content URIs. */
    static final String KEY_PICKED = "music_picked";
    private static final int PICK_AUDIO = 93;
    /** Fixed seed: the shuffle is reproducible, so the queue is stable across renders. */
    static final long SHUFFLE_SEED = 20260101L;

    private final ScreenHost host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private List<MusicRules.Track> queue = new ArrayList<>();
    private int index = -1;
    private boolean playing;
    private boolean prepared;

    public MusicScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Music"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        final boolean shuffle = AppPrefs.prefs(context).getBoolean(KEY_SHUFFLE, false);
        final int repeat = AppPrefs.prefs(context).getInt(KEY_REPEAT, MusicRules.REPEAT_ALL);

        List<MusicRules.Track> library = new ArrayList<>();
        File[] files = folder(context).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) library.add(new MusicRules.Track(f.getName(), prettyName(f.getName()),
                        f.getAbsolutePath(), false, f.length()));
            }
        }
        for (JSONObject o : streams(context)) {
            library.add(new MusicRules.Track(o.optString("id"), o.optString("title"),
                    o.optString("url"), true, 0));
        }
        for (JSONObject o : picked(context)) {
            library.add(new MusicRules.Track(o.optString("id"), o.optString("title"),
                    o.optString("uri"), false, 0));
        }
        List<MusicRules.Track> playable = MusicRules.playable(library);
        queue = shuffle ? MusicRules.shuffled(playable, SHUFFLE_SEED) : MusicRules.sorted(playable);

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        ui.sectionTitle(root, "Music", queue.size() + (queue.size() == 1 ? " track" : " tracks")
                + "  \u00b7  " + MusicRules.repeatLabel(repeat));

        // ---- now playing ----
        LinearLayout card = ui.card();
        LinearLayout.LayoutParams cp = ui.lp(-1, -2);
        cp.topMargin = ui.dp(8);
        root.addView(card, cp);
        MusicRules.Track current = index >= 0 && index < queue.size() ? queue.get(index) : null;
        TextView title = ui.label(current == null ? "Nothing playing" : current.title, 16,
                ui.palette().text, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView state = ui.label(current == null
                ? "Your own files, and streams you add yourself."
                : (playing ? "Playing" : (prepared ? "Paused" : "Loading\u2026"))
                        + (current.remote ? "  \u00b7  streaming" : "  \u00b7  "
                        + MusicRules.formatBytes(current.bytes)),
                11, ui.palette().muted, false);
        state.setGravity(Gravity.CENTER);
        card.addView(state);

        LinearLayout controls = ui.hbox();
        controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ctp = ui.lp(-1, 52);
        ctp.topMargin = ui.dp(8);
        card.addView(controls, ctp);
        addControl(controls, ui, "\u23ee", new View.OnClickListener() {
            @Override public void onClick(View v) { step(context, -1); }
        });
        addControl(controls, ui, playing ? "\u23f8" : "\u25b6", new View.OnClickListener() {
            @Override public void onClick(View v) { toggle(context); }
        });
        addControl(controls, ui, "\u23ed", new View.OnClickListener() {
            @Override public void onClick(View v) { step(context, 1); }
        });
        addControl(controls, ui, shuffle ? "\u2194" : "\u21c4", new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppPrefs.prefs(context).edit().putBoolean(KEY_SHUFFLE, !shuffle).apply();
                host.invalidate();
            }
        });
        addControl(controls, ui, repeat == MusicRules.REPEAT_ONE ? "\u27f31"
                : (repeat == MusicRules.REPEAT_ALL ? "\u27f3" : "\u27f3\u0338"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                AppPrefs.prefs(context).edit().putInt(KEY_REPEAT,
                        MusicRules.nextRepeatMode(repeat)).apply();
                host.invalidate();
            }
        });

        // ---- add a stream ----
        LinearLayout addRow = ui.hbox();
        LinearLayout.LayoutParams ap = ui.lp(-1, 46);
        ap.topMargin = ui.dp(10);
        root.addView(addRow, ap);
        TextView pick = ui.pillButton("Add music files", ui.palette().accent, android.graphics.Color.WHITE);
        LinearLayout.LayoutParams pkp = ui.lp(0, 46);
        pkp.weight = 1f;
        addRow.addView(pick, pkp);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickAudio(context); }
        });
        TextView add = ui.pillButton("Add a stream URL", ui.palette().surface, ui.palette().text);
        LinearLayout.LayoutParams adp = ui.lp(0, 46);
        adp.weight = 1f;
        adp.leftMargin = ui.dp(6);
        addRow.addView(add, adp);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addStream(context); }
        });

        if (queue.isEmpty()) {
            TextView empty = ui.label("No playable audio yet. Add your own music files, "
                    + "or a stream URL you trust.", 13, ui.palette().muted, false);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = ui.lp(-1, 60);
            ep.topMargin = ui.dp(10);
            root.addView(empty, ep);
        }

        for (int i = 0; i < queue.size(); i++) {
            final MusicRules.Track track = queue.get(i);
            final int position = i;
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = ui.lp(-1, 48);
            rp.topMargin = ui.dp(4);
            root.addView(row, rp);
            TextView marker = ui.label(position == index ? (playing ? "\u25b6" : "\u23f8") : "\u00b7",
                    12, position == index ? ui.palette().accent : ui.palette().muted, position == index);
            row.addView(marker, ui.lp(28, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            row.addView(copy, cpp);
            copy.addView(ui.label(track.title, 13,
                    position == index ? ui.palette().accent : ui.palette().text, position == index));
            copy.addView(ui.label(track.remote ? track.source
                    : MusicRules.formatBytes(track.bytes), 10, ui.palette().muted, false));
            boolean removable = track.remote || MusicRules.isPickedSource(track.source);
            TextView remove = ui.label(removable ? "Remove" : "", 12, ui.palette().muted, false);
            row.addView(remove, ui.lp(-2, -2));
            if (track.remote) {
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { removeStream(context, track.id); }
                });
            } else if (removable) {
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { removePicked(context, track.id); }
                });
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { playAt(context, position); }
            });
        }
        return scroll;
    }

    private void addControl(LinearLayout parent, UiKit ui, String glyph, View.OnClickListener listener) {
        TextView button = ui.label(glyph, 18, ui.palette().text, true);
        button.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = ui.lp(0, 48);
        p.weight = 1f;
        parent.addView(button, p);
        button.setOnClickListener(listener);
    }

    private static String prettyName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base.replace('_', ' ').replace('-', ' ');
    }

    static File folder(Context context) {
        File dir = new File(context.getFilesDir(), "music");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    static List<JSONObject> streams(Context context) {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(context).getString(KEY_STREAMS, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getJSONObject(i));
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return out;
    }

    private void pickAudio(Context context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivityForResult(intent, PICK_AUDIO);
            } else {
                host.toast("Cannot open the picker here.");
            }
        } catch (Exception e) {
            host.toast("No file picker is available.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_AUDIO || data == null) return;
        if (resultCode != android.app.Activity.RESULT_OK) {
            host.toast("No files added.");
            return;
        }
        Context context = host.context();
        List<Uri> uris = new ArrayList<>();
        android.content.ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) uris.add(clip.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        JSONArray array = new JSONArray();
        int added = 0;
        try {
            for (JSONObject o : picked(context)) array.put(o);
            for (Uri uri : uris) {
                // Persisted, or the grant expires and every picked track stops playing after
                // a process restart with no visible cause.
                try {
                    context.getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) { }
                array.put(new JSONObject().put("id", Long.toString(System.nanoTime(), 36))
                        .put("title", displayName(context, uri)).put("uri", uri.toString()));
                added++;
            }
        } catch (Exception e) {
            host.toast("Could not add those files.");
            return;
        }
        AppPrefs.prefs(context).edit().putString(KEY_PICKED, array.toString()).apply();
        host.toast(added == 0 ? "No files in that selection."
                : "Added " + added + (added == 1 ? " track." : " tracks."));
        host.invalidate();
    }

    /** The picker's own display name, falling back to the URI's last segment. */
    private static String displayName(Context context, Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return prettyName(name);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return prettyName(uri.getLastPathSegment() == null ? "track" : uri.getLastPathSegment());
    }

    static List<JSONObject> picked(Context context) {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(AppPrefs.prefs(context).getString(KEY_PICKED, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getJSONObject(i));
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return out;
    }

    private void removePicked(Context context, String id) {
        JSONArray a = new JSONArray();
        try {
            for (JSONObject o : picked(context)) if (!o.optString("id").equals(id)) a.put(o);
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(context).edit().putString(KEY_PICKED, a.toString()).apply();
        release();
        index = -1;
        host.invalidate();
    }

    private void addStream(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        LinearLayout form = ui.vbox();
        form.setPadding(ui.dp(14), ui.dp(6), ui.dp(14), ui.dp(6));
        final EditText title = field(context, ui, "Name");
        final EditText url = field(context, ui, "https://\u2026");
        url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        form.addView(title, ui.lp(-1, -2));
        LinearLayout.LayoutParams up = ui.lp(-1, -2);
        up.topMargin = ui.dp(8);
        form.addView(url, up);

        AppAlertBuilder dialog = new AppAlertBuilder(context, ui);
        dialog.setTitle("Add a stream");
        dialog.setView(form);
        dialog.setNegativeButton("Cancel", null);
        dialog.setPositiveButton("Add", new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int which) {
                String address = url.getText().toString().trim();
                // Only http and https are accepted: a pasted file: or content: string would
                // let this field read arbitrary local paths.
                if (!MusicRules.isStreamableUrl(address)) {
                    host.toast("That must be an http or https URL.");
                    return;
                }
                String name = title.getText().toString().trim();
                if (name.isEmpty()) name = address.substring(address.indexOf("//") + 2);
                List<JSONObject> existing = streams(context);
                JSONArray a = new JSONArray();
                try {
                    for (JSONObject o : existing) a.put(o);
                    a.put(new JSONObject().put("id", Long.toString(System.currentTimeMillis(), 36))
                            .put("title", name).put("url", address));
                } catch (Exception e) {
                    return;
                }
                AppPrefs.prefs(context).edit().putString(KEY_STREAMS, a.toString()).apply();
                host.toast("Added " + name + ".");
                host.invalidate();
            }
        });
        dialog.show();
    }

    private void removeStream(Context context, String id) {
        JSONArray a = new JSONArray();
        try {
            for (JSONObject o : streams(context)) if (!o.optString("id").equals(id)) a.put(o);
        } catch (Exception e) {
            return;
        }
        AppPrefs.prefs(context).edit().putString(KEY_STREAMS, a.toString()).apply();
        release();
        index = -1;
        host.invalidate();
    }

    private void playAt(Context context, int position) {
        release();
        index = MusicRules.clampIndex(position, queue.size());
        if (index < 0) { host.toast("Nothing to play."); return; }
        MusicRules.Track track = queue.get(index);
        try {
            player = new MediaPlayer();
            if (MusicRules.isPickedSource(track.source)) {
                player.setDataSource(context, Uri.parse(track.source));
            } else {
                player.setDataSource(track.source);
            }
            prepared = false;
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    playing = true;
                    mp.start();
                    host.invalidate();
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    playing = false;
                    advance(context, 1);
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    // A stream that fails must say so. Silently stopping looks like the
                    // button did nothing, which is the worst outcome for a player.
                    release();
                    playing = false;
                    prepared = false;
                    host.toast("Could not play " + track.title + ".");
                    host.invalidate();
                    return true;
                }
            });
            // Async for both cases: a remote source cannot be prepared on the main thread,
            // and a large local file should not block it either.
            player.prepareAsync();
        } catch (Exception e) {
            release();
            host.toast("Could not open " + track.title + ".");
        }
        host.invalidate();
    }

    private void toggle(Context context) {
        if (player == null) { playAt(context, index < 0 ? 0 : index); return; }
        if (!prepared) return;
        if (playing) { player.pause(); playing = false; }
        else { player.start(); playing = true; }
        host.invalidate();
    }

    private void step(Context context, int direction) {
        if (queue.isEmpty()) { host.toast("Nothing in the queue."); return; }
        advance(context, direction);
    }

    /** Moves by the repeat rules, stopping when REPEAT_NONE runs off the end. */
    private void advance(Context context, int direction) {
        int repeat = AppPrefs.prefs(context).getInt(KEY_REPEAT, MusicRules.REPEAT_ALL);
        int next = direction < 0
                ? MusicRules.previousIndex(index < 0 ? 0 : index, queue.size())
                : MusicRules.nextIndex(index < 0 ? -1 : index, queue.size(), repeat);
        if (next < 0) { release(); playing = false; index = -1; host.invalidate(); return; }
        playAt(context, next);
    }

    private void release() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
        playing = false;
        prepared = false;
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

    @Override
    public void onPause() {
        // Audio focus and the decode session must not survive a background transition.
        release();
        main.removeCallbacksAndMessages(null);
    }
}
