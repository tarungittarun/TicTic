package app.tictic.clock;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import org.json.JSONObject;

public class AlarmSoundService extends Service {
    static final String CHANNEL="tictic_alarm"; static final int NOTIFICATION=4401;
    private Ringtone ringtone; private Vibrator vibrator; private PowerManager.WakeLock wakeLock; private final Handler rampHandler=new Handler(); private float ringVolume=.20f;
    @Override public void onCreate(){super.onCreate(); createChannel();}
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"TicTic alarms",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Ringing alarms");ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);ch.enableVibration(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);} }
    @Override public int onStartCommand(Intent in,int flags,int startId){
        if(in!=null && "STOP".equals(in.getAction())){stopRing();stopSelf();return START_NOT_STICKY;}
        if(ringtone!=null||vibrator!=null||wakeLock!=null)stopRing();
        long id=in==null?0:in.getLongExtra("alarm_id",0);String label=in==null?"Alarm":in.getStringExtra("label");if(label==null||label.isEmpty())label="Time to wake up";
        Intent full=new Intent(this,AlarmActivity.class).putExtra("alarm_id",id).putExtra("label",label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int f=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)f|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fullPi=PendingIntent.getActivity(this,(int)(id%100000),full,f);
        Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        JSONObject alarm=DataStore.find(this,"alarms",id);int snoozeMinutes=alarm==null?9:Math.max(1,alarm.optInt("snooze",9));
        String actionHint=DataStore.showRingingSnoozeAction(this)?"Alarm is ringing · Snooze or Stop":"Alarm is ringing · Stop";
        nb.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("TicTic · "+label).setContentText(actionHint).setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX).setVisibility(Notification.VISIBILITY_PUBLIC).setOngoing(true).setAutoCancel(false).setContentIntent(fullPi);
        if(DataStore.showRingingSnoozeAction(this))nb.addAction(android.R.drawable.ic_lock_idle_alarm,"Snooze · "+snoozeMinutes+" min",AlarmActionReceiver.pending(this,AlarmActionReceiver.ACTION_SNOOZE,id,1));
        nb.addAction(android.R.drawable.ic_menu_close_clear_cancel,"Stop",AlarmActionReceiver.pending(this,AlarmActionReceiver.ACTION_DISMISS,id,2));
        boolean allowFullScreen=Build.VERSION.SDK_INT<34||((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).canUseFullScreenIntent();
        if(allowFullScreen)nb.setFullScreenIntent(fullPi,true);
        startForeground(NOTIFICATION,nb.build()); startRing(in!=null&&in.getBooleanExtra("vibrate",true),in==null?null:in.getStringExtra("sound"),in!=null&&in.getBooleanExtra("gentle",false)); return START_STICKY;
    }
    private void startRing(boolean vibrate,String selected,boolean gentle){
        try {
            if(ringtone==null){
                Uri uri=null;
                try { if(selected!=null&&!selected.isEmpty())uri=Uri.parse(selected); } catch(Exception ignored) {}
                if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                ringtone=RingtoneManager.getRingtone(this,uri);
                if(ringtone!=null){
                    if(Build.VERSION.SDK_INT>=21)ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                    if(Build.VERSION.SDK_INT>=28)ringtone.setLooping(true);
                }
            }
            if(ringtone!=null&&!ringtone.isPlaying())ringtone.play();
        } catch(Exception ignored) {}
        if(Build.VERSION.SDK_INT>=28&&ringtone!=null){
            rampHandler.removeCallbacksAndMessages(null);
            if(gentle){
                ringVolume=.20f;
                try{ringtone.setVolume(ringVolume);}catch(Exception ignored){}
                rampHandler.postDelayed(new Runnable(){@Override public void run(){
                    if(ringtone!=null&&ringtone.isPlaying()){
                        ringVolume=Math.min(1f,ringVolume+.15f);
                        try{ringtone.setVolume(ringVolume);}catch(Exception ignored){}
                        if(ringVolume<1f)rampHandler.postDelayed(this,15000);
                    }
                }},15000);
            }else try{ringtone.setVolume(1f);}catch(Exception ignored){}
        }
        if(vibrate){try{vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(vibrator!=null&&vibrator.hasVibrator()){long[] p={0,800,500,800};if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createWaveform(p,0));else vibrator.vibrate(p,0);}}catch(Exception ignored){}}
        try{PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TicTic:Alarm");wakeLock.acquire(10*60*1000L);}catch(Exception ignored){}
    }
    private void stopRing(){rampHandler.removeCallbacksAndMessages(null);try{if(ringtone!=null){if(ringtone.isPlaying())ringtone.stop();ringtone=null;}}catch(Exception ignored){}try{if(vibrator!=null)vibrator.cancel();}catch(Exception ignored){}try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}stopForeground(true);}
    @Override public void onDestroy(){stopRing();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
    static void stop(Context c){try{c.stopService(new Intent(c,AlarmSoundService.class));}catch(Exception ignored){}}
}
