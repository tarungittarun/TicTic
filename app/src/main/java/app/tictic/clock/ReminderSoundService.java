package app.tictic.clock;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/** Plays an opt-in, bounded reminder tone while the reminder notification is visible. */
public final class ReminderSoundService extends Service {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    private long activeId;
    private int activeNotificationId;
    private static volatile long currentlyPlayingId=-1;
    private final Runnable stopAfterDuration=this::finishTone;

    static Uri resolveTone(Context context,String requested,String customUri){
        String tone=requested==null||requested.isEmpty()?"default":requested;
        String custom=customUri==null?"":customUri;
        if("default".equals(tone)){
            android.content.SharedPreferences prefs=DataStore.prefs(context);
            tone=prefs.getString("reminder_default_tone","notification");
            if("default".equals(tone))tone="notification";
            if(custom.isEmpty())custom=prefs.getString("reminder_default_tone_uri","");
        }
        if("silent".equals(tone))return null;
        if("ping".equals(tone))return resource(context,R.raw.reminder_ping);
        if("dawn".equals(tone))return resource(context,R.raw.alarm_dawn);
        if("golden".equals(tone))return resource(context,R.raw.alarm_golden);
        if("pulse".equals(tone))return resource(context,R.raw.alarm_pulse);
        if("custom".equals(tone)){
            try{return custom.isEmpty()?null:Uri.parse(custom);}catch(Exception ignored){return null;}
        }
        if("notification".equals(tone)){
            Uri uri=android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            return uri!=null?uri:android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
        }
        try{return Uri.parse(tone);}catch(Exception ignored){return null;}
    }
    private static Uri resource(Context context,int resource){return Uri.parse("android.resource://"+context.getPackageName()+"/"+resource);}

    static boolean start(Context context,long id,int notificationId,Uri tone,int seconds,Notification notification){
        if(tone==null||seconds<=0||notification==null)return false;
        Intent intent=new Intent(context,ReminderSoundService.class)
                .putExtra("reminder_id",id).putExtra("notification_id",notificationId)
                .putExtra("tone_uri",tone.toString()).putExtra("duration_seconds",seconds)
                .putExtra("reminder_notification",notification);
        try{
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(intent);else context.startService(intent);
            return true;
        }catch(RuntimeException blocked){return false;}
    }
    static void stop(Context context,long id){
        if(currentlyPlayingId!=id)return;
        try{context.stopService(new Intent(context,ReminderSoundService.class).putExtra("reminder_id",id));}
        catch(RuntimeException ignored){}
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf(startId);return START_NOT_STICKY;}
        Notification notification=intent.getParcelableExtra("reminder_notification");
        if(notification==null){stopSelf(startId);return START_NOT_STICKY;}
        if(player!=null){detachForeground();releasePlayback();}
        activeId=intent.getLongExtra("reminder_id",0);currentlyPlayingId=activeId;activeNotificationId=intent.getIntExtra("notification_id",ReminderReceiver.notificationId(activeId));
        if(Build.VERSION.SDK_INT>=29)startForeground(activeNotificationId,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(activeNotificationId,notification);
        String raw=intent.getStringExtra("tone_uri");int seconds=Math.max(1,Math.min(30,intent.getIntExtra("duration_seconds",5)));
        try{
            player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());player.setDataSource(this,Uri.parse(raw));player.setLooping(true);player.setOnErrorListener((mp,what,extra)->{finishTone();return true;});player.prepare();player.start();
            PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);if(power!=null){wakeLock=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TicTic:ReminderTone");wakeLock.acquire((seconds+3L)*1000L);}
            handler.removeCallbacks(stopAfterDuration);handler.postDelayed(stopAfterDuration,seconds*1000L);
        }catch(Exception failure){finishTone();}
        return START_NOT_STICKY;
    }
    private void finishTone(){handler.removeCallbacks(stopAfterDuration);releasePlayback();detachForeground();stopSelf();}
    private void releasePlayback(){try{if(player!=null){if(player.isPlaying())player.stop();player.reset();player.release();player=null;}}catch(Exception ignored){player=null;}try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}wakeLock=null;}
    private void detachForeground(){try{if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_DETACH);else stopForeground(false);}catch(Exception ignored){}}
    @Override public void onDestroy(){handler.removeCallbacks(stopAfterDuration);releasePlayback();try{detachForeground();}catch(Exception ignored){}if(currentlyPlayingId==activeId)currentlyPlayingId=-1;super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
