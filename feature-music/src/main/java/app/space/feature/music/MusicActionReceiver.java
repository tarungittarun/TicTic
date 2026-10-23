package app.space.feature.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The notification transport's buttons. Broadcasts rather than service starts because a
 * notification action is cheapest this way, and the service may not be running yet when the
 * owner taps Play on a stale notification -- the intent simply starts it.
 */
public final class MusicActionReceiver extends BroadcastReceiver {

    static final String ACTION_TOGGLE = "app.space.feature.music.TOGGLE";
    static final String ACTION_STEP = "app.space.feature.music.STEP";
    static final String ACTION_STOP = "app.space.feature.music.STOP";
    static final String ACTION_PLAY_AT = "app.space.feature.music.PLAY_AT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        Intent forward = new Intent(context, PlaybackService.class).setAction(intent.getAction())
                .putExtra("direction", intent.getIntExtra("direction", 1))
                .putExtra("position", intent.getIntExtra("position", 0));
        if (ACTION_STOP.equals(intent.getAction())) {
            context.startService(forward);
        } else {
            context.startForegroundService(forward);
        }
    }
}
