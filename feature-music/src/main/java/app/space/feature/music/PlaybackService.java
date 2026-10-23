package app.space.feature.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

/**
 * Playback that survives leaving the screen -- the piece whose absence made the old player read
 * as "worse than before". The service owns the one MediaPlayer; screens ask it to play and read
 * its state. Everything is static-entry-point driven because the suite has no dependency
 * injection and screens are rebuilt, not retained.
 *
 * <p>Framework-only by rule: useAndroidX=false until Sprint 4 owns the migration.
 */
public final class PlaybackService extends Service {

    static final String CHANNEL = "myspace_music_v1";
    static final int NOTIFICATION_ID = 9001;

    /** The screen registers this so transport changes repaint it without polling. */
    public interface StateListener {
        void onPlaybackStateChanged();
    }

    private static MediaPlayer player;
    private static List<MusicRules.Track> queue = new ArrayList<>();
    private static int index = -1;
    private static boolean playing;
    private static boolean prepared;
    private static int repeatMode = MusicRules.REPEAT_ALL;
    private static StateListener listener;
    private static AudioFocusRequest focusRequest;
    /** The running instance, so static transport changes can refresh the notification. */
    private static PlaybackService instance;
    /** Consecutive open failures; a queue of broken files must not loop forever. */
    private static int failures;

    public static void setListener(StateListener l) { listener = l; }
    public static boolean isPlaying() { return playing; }
    public static boolean isPrepared() { return prepared; }
    public static int currentIndex() { return index; }
    public static int queueSize() { return queue.size(); }
    public static int repeat() { return repeatMode; }

    public static String currentTrackId() {
        return index >= 0 && index < queue.size() ? queue.get(index).id : "";
    }

    public static int currentPosition() {
        MediaPlayer p = player;
        if (p == null || !prepared) return 0;
        try { return p.getCurrentPosition(); } catch (Exception e) { return 0; }
    }

    public static int currentDuration() {
        MediaPlayer p = player;
        if (p == null || !prepared) return 0;
        try { return p.getDuration(); } catch (Exception e) { return 0; }
    }

    public static void setRepeat(int mode) { repeatMode = mode; }

    /** Hands the service a queue and opens one position in it. */
    public static void play(Context context, List<MusicRules.Track> tracks, int position) {
        if (context == null || tracks == null || tracks.isEmpty()) return;
        queue = new ArrayList<>(tracks);
        int start = MusicRules.clampIndex(position, queue.size());
        context.startForegroundService(new Intent(context, PlaybackService.class)
                .setAction(MusicActionReceiver.ACTION_PLAY_AT).putExtra("position", start));
    }

    public static void toggle(Context context) {
        if (context == null) return;
        context.startForegroundService(new Intent(context, PlaybackService.class)
                .setAction(MusicActionReceiver.ACTION_TOGGLE));
    }

    public static void step(Context context, int direction) {
        if (context == null) return;
        context.startForegroundService(new Intent(context, PlaybackService.class)
                .setAction(MusicActionReceiver.ACTION_STEP).putExtra("direction", direction));
    }

    public static void seekTo(Context context, int millis) {
        MediaPlayer p = player;
        if (p == null || !prepared) return;
        try {
            p.seekTo(Math.max(0, millis));
        } catch (Exception ignored) { }
        notifyState();
    }

    public static void stopAll(Context context) {
        if (context != null) {
            context.startService(new Intent(context, PlaybackService.class)
                    .setAction(MusicActionReceiver.ACTION_STOP));
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        String action = intent == null ? null : intent.getAction();
        if (action == null) action = MusicActionReceiver.ACTION_TOGGLE;
        switch (action) {
            case MusicActionReceiver.ACTION_PLAY_AT:
                open(intent.getIntExtra("position", 0));
                break;
            case MusicActionReceiver.ACTION_TOGGLE:
                toggleNow();
                break;
            case MusicActionReceiver.ACTION_STEP:
                advance(intent.getIntExtra("direction", 1));
                break;
            case MusicActionReceiver.ACTION_STOP:
                releasePlayer();
                abandonFocus();
                index = -1;
                failures = 0;
                notifyState();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            default:
                break;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        releasePlayer();
        abandonFocus();
        super.onDestroy();
    }

    // ---- transport ----

    private void toggleNow() {
        MediaPlayer p = player;
        if (p == null) {
            if (index < 0 && !queue.isEmpty()) index = 0;
            if (!queue.isEmpty()) open(index);
            return;
        }
        try {
            if (playing) { p.pause(); playing = false; }
            else if (prepared) { requestFocus(); p.start(); playing = true; }
        } catch (Exception ignored) { }
        notifyState();
    }

    private void advance(int direction) {
        if (queue.isEmpty()) return;
        int size = queue.size();
        int next = direction >= 0
                ? MusicRules.nextIndex(index, size, repeatMode)
                : MusicRules.previousIndex(index, size);
        if (next < 0) {
            // Repeat off and the queue ended: stop cleanly instead of hanging on the last row.
            playing = false;
            notifyState();
            return;
        }
        open(next);
    }

    private void open(int position) {
        index = MusicRules.clampIndex(position, queue.size());
        if (index < 0) return;
        final MusicRules.Track track = queue.get(index);
        releasePlayer();
        prepared = false;
        playing = false;
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            if (MusicRules.isPickedSource(track.source)) {
                player.setDataSource(this, Uri.parse(track.source));
            } else {
                player.setDataSource(track.source);
            }
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    prepared = true;
                    failures = 0;
                    requestFocus();
                    mp.start();
                    playing = true;
                    notifyState();
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    playing = false;
                    advance(1);
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    playing = false;
                    prepared = false;
                    skipOrFailQuietly();
                    return true;
                }
            });
            player.prepareAsync();
        } catch (Exception e) {
            releasePlayer();
            skipOrFailQuietly();
        }
        notifyState();
    }

    /**
     * Advances past a track that refused to open -- unless the whole queue is refusing, in
     * which case playback stops instead of burning the battery through an error loop.
     */
    private void skipOrFailQuietly() {
        failures++;
        notifyState();
        if (queue.isEmpty() || failures > queue.size()) {
            failures = 0;
            releasePlayer();
            return;
        }
        advance(1);
    }

    private void releasePlayer() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
        prepared = false;
        playing = false;
    }

    private static void notifyState() {
        PlaybackService self = instance;
        if (self != null) self.refreshNotification();
        StateListener l = listener;
        if (l != null) {
            try { l.onPlaybackStateChanged(); } catch (Exception ignored) { }
        }
    }

    // ---- audio focus ----

    private void requestFocus() {
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) return;
            if (focusRequest == null) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setOnAudioFocusChangeListener(focusListener)
                        .build();
            }
            audio.requestAudioFocus(focusRequest);
        } catch (Exception ignored) { }
    }

    private void abandonFocus() {
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
        } catch (Exception ignored) { }
    }

    private final AudioManager.OnAudioFocusChangeListener focusListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                            || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        MediaPlayer p = player;
                        if (p != null && playing) {
                            try { p.pause(); } catch (Exception ignored) { }
                            playing = false;
                            notifyState();
                        }
                    }
                }
            };

    // ---- notification ----

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null && manager.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Music playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("The now-playing controls for MySpace Music");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        MusicRules.Track track = index >= 0 && index < queue.size() ? queue.get(index) : null;
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(track == null ? "Music" : track.title)
                .setContentText(track == null ? "Nothing queued"
                        : track.artist.isEmpty() ? MusicRules.formatBytes(track.bytes)
                        : track.artist + (track.album.isEmpty() ? "" : " \u00b7 " + track.album))
                .setOnlyAlertOnce(true)
                .setOngoing(playing)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (open != null) {
            builder.setContentIntent(PendingIntent.getActivity(this, 9002,
                    open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), immutableFlags(0)));
        }
        builder.addAction(action(android.R.drawable.ic_media_previous, "Previous",
                MusicActionReceiver.ACTION_STEP, -1, 9003));
        builder.addAction(action(playing ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play,
                playing ? "Pause" : "Play", MusicActionReceiver.ACTION_TOGGLE, 0, 9004));
        builder.addAction(action(android.R.drawable.ic_media_next, "Next",
                MusicActionReceiver.ACTION_STEP, 1, 9005));
        builder.addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Stop",
                MusicActionReceiver.ACTION_STOP, 0, 9006));
        return builder.build();
    }

    private Notification.Action action(int iconRes, String title, String action,
                                       int direction, int requestCode) {
        Intent intent = new Intent(this, MusicActionReceiver.class).setAction(action)
                .putExtra("direction", direction);
        PendingIntent pending = PendingIntent.getBroadcast(this, requestCode, intent,
                immutableFlags(PendingIntent.FLAG_UPDATE_CURRENT));
        return new Notification.Action.Builder(
                Icon.createWithResource(this, iconRes), title, pending).build();
    }

    private static int immutableFlags(int base) {
        return Build.VERSION.SDK_INT >= 23 ? base | PendingIntent.FLAG_IMMUTABLE : base;
    }

    /** Refreshes the notification in place after a transport change. */
    void refreshNotification() {
        if (playing || prepared) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }
}
