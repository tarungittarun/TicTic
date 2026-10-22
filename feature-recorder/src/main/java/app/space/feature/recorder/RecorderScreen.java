package app.space.feature.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.space.core.design.AppAlertBuilder;
import app.space.core.design.UiKit;
import app.space.core.suite.Screen;
import app.space.core.suite.ScreenHost;

/**
 * Record, list, play and delete voice notes.
 *
 * <p>Files live in app-private storage, so no storage permission is involved.
 * {@code RECORD_AUDIO} is requested only when the user first presses Record, never at
 * launch, which is what keeps the Settings page's "requested only when its feature is used"
 * promise true.
 *
 * <p>The recorder is stopped and released in {@link #onPause}. Leaving a MediaRecorder live
 * across a background transition holds the microphone, which Android may revoke, and the
 * next start then fails in a way that looks like the button doing nothing.
 */
public final class RecorderScreen implements Screen {

    private static final int PERMISSION_REQUEST = 91;

    private final ScreenHost host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaRecorder recorder;
    private MediaPlayer player;
    private File active;
    private long startedAt;
    private long elapsed;
    private boolean recording;
    private Context pauseContext;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!recording) return;
            elapsed = System.currentTimeMillis() - startedAt;
            if (RecorderRules.isPastLimit(elapsed)) {
                host.toast("Reached the one hour limit.");
                if (pauseContext != null) stop(pauseContext);
                return;
            }
            host.invalidate();
            main.postDelayed(this, 500L);
        }
    };

    public RecorderScreen(ScreenHost host) { this.host = host; }

    @Override public String title() { return "Recorder"; }

    @Override
    public View build(final Context context) {
        pauseContext = context;
        final UiKit ui = new UiKit(context, host.palette());
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.vbox();
        root.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(14));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        final List<RecorderRules.Recording> recordings = RecorderRules.sorted(list(context));
        ui.sectionTitle(root, "Recorder", recordings.isEmpty()
                ? "No recordings yet."
                : recordings.size() + " recordings  \u00b7  "
                        + RecorderRules.formatBytes(RecorderRules.totalBytes(recordings)));

        TextView clock = ui.label(RecorderRules.formatDuration(recording ? elapsed : 0L), 40,
                ui.palette().text, true);
        clock.setGravity(Gravity.CENTER);
        root.addView(clock, ui.lp(-1, 76));

        TextView status = ui.label(recording
                ? "Recording \u00b7 about " + RecorderRules.formatBytes(RecorderRules.estimatedBytes(elapsed))
                : (hasPermission(context) ? "Ready." : "Microphone permission needed."),
                11, recording ? 0xffc2564a : ui.palette().muted, recording);
        status.setGravity(Gravity.CENTER);
        root.addView(status, ui.lp(-1, 22));

        TextView toggle = ui.pillButton(recording ? "Stop" : "Record",
                recording ? ui.palette().surface : ui.palette().accent,
                recording ? ui.palette().text : android.graphics.Color.WHITE);
        LinearLayout.LayoutParams tp = ui.lp(-1, 50);
        tp.topMargin = ui.dp(10);
        root.addView(toggle, tp);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (recording) { stop(context); return; }
                if (!hasPermission(context)) { requestPermission(context); return; }
                start(context);
            }
        });

        for (final RecorderRules.Recording item : recordings) {
            LinearLayout card = ui.card();
            LinearLayout.LayoutParams cp = ui.lp(-1, -2);
            cp.topMargin = ui.dp(8);
            root.addView(card, cp);
            LinearLayout row = ui.hbox();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row, ui.lp(-1, -2));
            TextView play = ui.pillButton("\u25b6", ui.palette().accent, android.graphics.Color.WHITE);
            row.addView(play, ui.lp(44, 44));
            play.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { play(context, item); }
            });
            LinearLayout copy = ui.vbox();
            LinearLayout.LayoutParams cpp = ui.lp(0, -2);
            cpp.weight = 1f;
            cpp.leftMargin = ui.dp(10);
            row.addView(copy, cpp);
            copy.addView(ui.label(item.label.isEmpty() ? item.fileName : item.label, 14,
                    ui.palette().text, true));
            copy.addView(ui.label(RecorderRules.formatDuration(item.durationMillis)
                            + "  \u00b7  " + RecorderRules.formatBytes(item.bytes)
                            + "  \u00b7  " + new java.text.SimpleDateFormat("d MMM HH:mm",
                                    java.util.Locale.getDefault()).format(new java.util.Date(item.createdAt)),
                    11, ui.palette().muted, false));
            TextView delete = ui.label("Delete", 12, ui.palette().muted, false);
            row.addView(delete, ui.lp(-2, -2));
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    stopPlayback();
                    File target = new File(folder(context), item.fileName);
                    boolean ok = target.isFile() && target.delete();
                    host.toast(ok ? "Deleted." : "Could not delete that file.");
                    host.invalidate();
                }
            });
        }
        return scroll;
    }

    private static File folder(Context context) {
        File dir = new File(context.getFilesDir(), "recordings");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static boolean hasPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(Context context) {
        // Asked for at the moment of use, and the user presses Record again once granted.
        if (context instanceof Activity) {
            ((Activity) context).requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST);
        } else {
            host.toast("Microphone permission is needed to record.");
        }
    }

    private void start(Context context) {
        long now = System.currentTimeMillis();
        active = new File(folder(context), RecorderRules.fileName(now, ""));
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(active.getAbsolutePath());
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            releaseRecorder();
            if (active != null && active.exists()) active.delete();
            active = null;
            host.toast("Could not start recording.");
            return;
        }
        recording = true;
        startedAt = now;
        elapsed = 0;
        main.postDelayed(ticker, 500L);
    }

    private void stop(Context context) {
        if (!recording) return;
        long duration = System.currentTimeMillis() - startedAt;
        recording = false;
        main.removeCallbacks(ticker);
        releaseRecorder();
        File finished = active;
        active = null;
        if (finished == null || !finished.isFile() || finished.length() == 0) {
            if (finished != null && finished.exists()) finished.delete();
            host.toast("Nothing was recorded.");
            host.invalidate();
            return;
        }
        elapsed = duration;
        // Duration travels with the file name so the list can render without decoding every
        // recording, which would be slow with a lot of them.
        File renamed = new File(finished.getParentFile(),
                RecorderRules.fileName(startedAt, String.valueOf(duration)));
        if (!finished.renameTo(renamed)) renamed = finished;
        index(context, renamed, duration);
        host.toast("Saved " + RecorderRules.formatDuration(duration) + ".");
        host.invalidate();
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) { }
            try { recorder.release(); } catch (Exception ignored) { }
            recorder = null;
        }
    }

    /** Records duration and size beside the file, so listing never touches the media codec. */
    private void index(Context context, File file, long duration) {
        String existing = readIndex(context);
        StringBuilder out = new StringBuilder();
        for (String line : existing.split("\n")) {
            if (!line.startsWith(file.getName() + "\t")) out.append(line).append('\n');
        }
        out.append(file.getName()).append('\t').append(duration).append('\t').append(file.length()).append('\n');
        app.space.core.data.AppPrefs.prefs(context).edit()
                .putString("recorder_index", out.toString()).apply();
    }

    private static String readIndex(Context context) {
        return app.space.core.data.AppPrefs.prefs(context).getString("recorder_index", "");
    }

    private List<RecorderRules.Recording> list(Context context) {
        List<RecorderRules.Recording> out = new ArrayList<>();
        File dir = folder(context);
        File[] files = dir.listFiles();
        if (files == null) return out;
        String index = readIndex(context);
        for (File f : files) {
            if (!f.isFile()) continue;
            long duration = 0;
            long bytes = f.length();
            for (String line : index.split("\n")) {
                String[] parts = line.split("\t");
                if (parts.length >= 2 && parts[0].equals(f.getName())) {
                    try { duration = Long.parseLong(parts[1]); } catch (NumberFormatException ignored) { }
                    if (parts.length >= 3) {
                        try { bytes = Long.parseLong(parts[2]); } catch (NumberFormatException ignored) { }
                    }
                    break;
                }
            }
            // A file with no recorded duration is an interrupted recording; showing it would
            // offer a play button for a file that cannot be played.
            if (duration <= 0) duration = bytes / RecorderRules.BYTES_PER_SECOND * 1000L;
            out.add(new RecorderRules.Recording(f.getName(), labelFor(f.getName()),
                    f.lastModified(), duration, bytes));
        }
        return out;
    }

    private static String labelFor(String fileName) {
        // fileName is recording-<yyyyMMdd-HHmmss>-<duration>.m4a
        String[] parts = fileName.replace(".m4a", "").split("-");
        return parts.length >= 3 ? parts[1] + " " + parts[2] : fileName;
    }

    private void play(Context context, RecorderRules.Recording item) {
        stopPlayback();
        File file = new File(folder(context), item.fileName);
        if (!file.isFile()) { host.toast("That file is missing."); return; }
        try {
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.prepare();
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) { stopPlayback(); }
            });
        } catch (Exception e) {
            stopPlayback();
            host.toast("Could not play that recording.");
        }
    }

    private void stopPlayback() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST) return;
        // Re-render either way: on grant the screen becomes usable, and on denial it has to
        // stop looking like it is waiting for something that will not arrive.
        host.invalidate();
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            if (result != android.content.pm.PackageManager.PERMISSION_GRANTED) { granted = false; break; }
        }
        host.toast(granted ? "Access granted." : "Access refused. The feature stays unavailable.");
    }

    @Override public void onResume() { }

    @Override
    public void onPause() {
        // Both the microphone and the speaker must be released when the screen goes away.
        if (recording && pauseContext != null) stop(pauseContext);
        stopPlayback();
        main.removeCallbacks(ticker);
    }
}
