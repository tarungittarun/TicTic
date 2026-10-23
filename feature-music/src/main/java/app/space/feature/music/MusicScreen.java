package app.space.feature.music;

import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
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
    /** In the 90-99 range the Screen contract reserves for screens. */
    private static final int PERMISSION_REQUEST = 95;
    /** Fixed seed: the shuffle is reproducible, so the queue is stable across renders. */
    static final long SHUFFLE_SEED = 20260101L;

    private final ScreenHost host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private List<MusicRules.Track> queue = new ArrayList<>();
    /** Live views the seek ticker updates in place; rebuilding per tick would flicker. */
    private android.widget.SeekBar seekView;
    private TextView positionView;

    public MusicScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Music"; }

    @Override
    public View build(final Context context) {
        final UiKit ui = new UiKit(context, host.palette());
        final boolean shuffle = AppPrefs.prefs(context).getBoolean(KEY_SHUFFLE, false);
        final int repeat = AppPrefs.prefs(context).getInt(KEY_REPEAT, MusicRules.REPEAT_ALL);

        List<MusicRules.Track> library = new ArrayList<>();
        // The device's own library first: MediaStore knows every song on the phone, which is
        // the difference between a player and a file list with a play button.
        boolean canScan = hasAudioPermission(context);
        if (canScan) library.addAll(scanMediaStore(context));
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
        PlaybackService.setRepeat(repeat);
        final String currentId = PlaybackService.currentTrackId();
        final boolean playing = PlaybackService.isPlaying();
        final boolean prepared = PlaybackService.isPrepared();

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        ui.sectionTitle(root, "Music", queue.size() + (queue.size() == 1 ? " track" : " tracks")
                + "  \u00b7  " + MusicRules.repeatLabel(repeat));

        if (!canScan) {
            TextView grant = ui.pillButton("Grant music access", ui.palette().accent,
                    android.graphics.Color.WHITE);
            LinearLayout.LayoutParams gtp = ui.lp(-1, 46);
            gtp.topMargin = ui.dp(8);
            root.addView(grant, gtp);
            grant.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { requestAudioPermission(context); }
            });
            TextView why = ui.label("Without it the player can only see files you add by hand.",
                    11, ui.palette().muted, false);
            LinearLayout.LayoutParams wp = ui.lp(-1, -2);
            wp.topMargin = ui.dp(4);
            root.addView(why, wp);
        }

        // ---- now playing ----
        LinearLayout card = ui.card();
        LinearLayout.LayoutParams cp = ui.lp(-1, -2);
        cp.topMargin = ui.dp(8);
        root.addView(card, cp);
        MusicRules.Track current = null;
        for (MusicRules.Track candidate : queue) {
            if (candidate.id.equals(currentId)) { current = candidate; break; }
        }
        TextView title = ui.label(current == null ? "Nothing playing" : current.title, 16,
                ui.palette().text, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView state = ui.label(current == null
                ? "Your library, your own files, and streams you add yourself."
                : (playing ? "Playing" : (prepared ? "Paused" : "Loading\u2026"))
                        + (current.remote ? "  \u00b7  streaming" : "  \u00b7  "
                        + MusicRules.formatBytes(current.bytes)),
                11, ui.palette().muted, false);
        state.setGravity(Gravity.CENTER);
        card.addView(state);

        if (current != null) {
            final android.widget.SeekBar seek = new android.widget.SeekBar(context);
            card.addView(seek, ui.lp(-1, -2));
            TextView positionLabel = ui.label("0:00 / 0:00", 10, ui.palette().muted, false);
            positionLabel.setGravity(Gravity.CENTER);
            card.addView(positionLabel, ui.lp(-1, -2));
            seekView = seek;
            positionView = positionLabel;
            seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(android.widget.SeekBar sb, int progress,
                                                        boolean fromUser) {
                    if (fromUser) PlaybackService.seekTo(context, progress);
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar sb) { }
                @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {
                    if (positionView != null) {
                        positionView.setText(MusicRules.formatDuration(PlaybackService.currentPosition())
                                + " / " + MusicRules.formatDuration(PlaybackService.currentDuration()));
                    }
                }
            });
        }

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
                int next = MusicRules.nextRepeatMode(repeat);
                AppPrefs.prefs(context).edit().putInt(KEY_REPEAT, next).apply();
                PlaybackService.setRepeat(next);
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

        int rendered = Math.min(queue.size(), MusicRules.MAX_RENDER);
        for (int i = 0; i < rendered; i++) {
            final MusicRules.Track track = queue.get(i);
            final int position = i;
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = ui.lp(-1, 48);
            rp.topMargin = ui.dp(4);
            root.addView(row, rp);
            boolean isCurrent = track.id.equals(currentId);
            TextView marker = ui.label(isCurrent ? (playing ? "\u25b6" : "\u23f8") : "\u00b7",
                    12, isCurrent ? ui.palette().accent : ui.palette().muted, isCurrent);
            row.addView(marker, ui.lp(28, -2));
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            row.addView(copy, cpp);
            copy.addView(ui.label(track.title, 13,
                    isCurrent ? ui.palette().accent : ui.palette().text, isCurrent));
            copy.addView(ui.label(track.remote ? track.source
                    : MusicRules.isLibrarySource(track.source)
                        ? MusicRules.librarySubtitle(track)
                        : MusicRules.formatBytes(track.bytes), 10, ui.palette().muted, false));
            boolean removable = track.remote
                    || (MusicRules.isPickedSource(track.source)
                        && !MusicRules.isLibrarySource(track.source));
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
        if (queue.size() > rendered) {
            TextView more = ui.label("Showing the first " + rendered + " of " + queue.size()
                    + " \u2014 the queue still plays them all. Search is a later slice.",
                    11, ui.palette().muted, false);
            more.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams mlp = ui.lp(-1, -2);
            mlp.topMargin = ui.dp(10);
            root.addView(more, mlp);
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
        // The removed source might be the one playing; the service stops rather than
        // advancing into a row that no longer exists.
        PlaybackService.stopAll(context);
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
        // The removed source might be the one playing; the service stops rather than
        // advancing into a row that no longer exists.
        PlaybackService.stopAll(context);
        host.invalidate();
    }

    /** READ_MEDIA_AUDIO on 33+, the capped storage permission below it. */
    private static boolean hasAudioPermission(Context context) {
        String permission = android.os.Build.VERSION.SDK_INT >= 33
                ? "android.permission.READ_MEDIA_AUDIO"
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return context.checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission(Context context) {
        // Asked at the moment of use; the answer arrives in onRequestPermissionsResult below.
        if (context instanceof Activity) {
            String permission = android.os.Build.VERSION.SDK_INT >= 33
                    ? "android.permission.READ_MEDIA_AUDIO"
                    : Manifest.permission.READ_EXTERNAL_STORAGE;
            ((Activity) context).requestPermissions(new String[]{permission}, PERMISSION_REQUEST);
        } else {
            host.toast("Music access is needed to scan your library.");
        }
    }

    /**
     * Every audio row MediaStore calls music. Queried on the UI thread because the screen is
     * built there anyway and the projection is narrow; a device with a pathological library
     * pays once per open, and the render cap keeps the view tree sane.
     */
    private static List<MusicRules.Track> scanMediaStore(Context context) {
        List<MusicRules.Track> out = new ArrayList<>();
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            android.provider.MediaStore.Audio.Media._ID,
                            android.provider.MediaStore.Audio.Media.TITLE,
                            android.provider.MediaStore.Audio.Media.ARTIST,
                            android.provider.MediaStore.Audio.Media.ALBUM,
                            android.provider.MediaStore.Audio.Media.DURATION,
                            android.provider.MediaStore.Audio.Media.SIZE},
                    android.provider.MediaStore.Audio.Media.IS_MUSIC + "!=0",
                    null,
                    android.provider.MediaStore.Audio.Media.TITLE);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    out.add(new MusicRules.Track("media:" + id, cursor.getString(1),
                            MusicRules.mediaUri(id), false, cursor.getLong(5),
                            cursor.getString(2), cursor.getString(3), cursor.getLong(4)));
                }
            }
        } catch (Exception ignored) {
            // A device that refuses the query still has the hand-added files and streams.
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) { }
            }
        }
        return out;
    }

    /** Hands the queue to the service; playback continues when this screen goes away. */
    private void playAt(Context context, int position) {
        if (queue.isEmpty()) { host.toast("Nothing to play."); return; }
        PlaybackService.play(context, queue, position);
    }

    private void toggle(Context context) {
        if (queue.isEmpty() && PlaybackService.queueSize() == 0) {
            host.toast("Nothing to play.");
            return;
        }
        PlaybackService.toggle(context);
    }

    private void step(Context context, int direction) {
        if (queue.isEmpty() && PlaybackService.queueSize() == 0) {
            host.toast("Nothing in the queue.");
            return;
        }
        PlaybackService.step(context, direction);
    }

    /** The seek ticker: updates two views in place. Rebuilding the screen per tick is the
     *  blink defect the recorder had; this is the pattern that fixed it there. */
    private final Runnable seekTicker = new Runnable() {
        @Override public void run() {
            int position = PlaybackService.currentPosition();
            int duration = PlaybackService.currentDuration();
            if (seekView != null) {
                seekView.setMax(Math.max(1, duration));
                seekView.setProgress(position);
            }
            if (positionView != null) {
                positionView.setText(MusicRules.formatDuration(position)
                        + " / " + MusicRules.formatDuration(duration));
            }
            main.postDelayed(this, 500L);
        }
    };

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST) return;
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) { granted = false; break; }
        }
        host.toast(granted ? "Scanning your library." : "Without access, only hand-added files play.");
        host.invalidate();
    }

    @Override
    public void onResume() {
        PlaybackService.setListener(new PlaybackService.StateListener() {
            @Override public void onPlaybackStateChanged() { host.invalidate(); }
        });
        main.postDelayed(seekTicker, 500L);
    }

    @Override
    public void onPause() {
        // Playback deliberately survives this screen -- that is what the service is for.
        // The screen only stops listening and stops its own ticker.
        PlaybackService.setListener(null);
        main.removeCallbacksAndMessages(null);
        seekView = null;
        positionView = null;
    }
}
