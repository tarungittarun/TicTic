package app.space.notes.runtime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.util.Map;
import java.util.WeakHashMap;

/** Coalesces transient notices so rapid rejected taps replace, rather than queue, messages. */
public final class NotesNotice {
    private static final long REPEAT_WINDOW_MS=3200;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<Context,State> STATES=new WeakHashMap<>();

    private static final class State {
        Toast toast;
        String lastMessage;
        long lastShownAt;
    }

    private NotesNotice() { }

    public static void show(Context context,String message){
        if(context==null || message==null || message.trim().isEmpty())return;
        Context application=context.getApplicationContext();
        Context key=application==null?context:application;
        Runnable show=()->showOnMain(key,message);
        if(Looper.myLooper()==Looper.getMainLooper())show.run();else MAIN.post(show);
    }

    private static void showOnMain(Context context,String message){
        final State state;
        synchronized(STATES){
            State existing=STATES.get(context);
            state=existing==null?new State():existing;
            STATES.put(context,state);
        }
        long now=SystemClock.elapsedRealtime();
        if(message.equals(state.lastMessage) && now-state.lastShownAt<REPEAT_WINDOW_MS)return;
        if(state.toast!=null)state.toast.cancel();
        state.lastMessage=message;
        state.lastShownAt=now;
        state.toast=Toast.makeText(context,message,Toast.LENGTH_SHORT);
        state.toast.show();
    }
}
