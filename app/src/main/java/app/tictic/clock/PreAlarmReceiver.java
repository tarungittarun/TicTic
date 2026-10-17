package app.tictic.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import org.json.JSONObject;

/** Delivers a configurable, user-dismissable notification before an alarm rings. */
public final class PreAlarmReceiver extends BroadcastReceiver {
    static final String ACTION="app.tictic.clock.PRE_ALARM_REMINDER";
    static PendingIntent pending(Context c,long id){
        Intent i=new Intent(c,PreAlarmReceiver.class).setAction(ACTION).setData(Uri.parse("tictic://prealarm/"+id)).putExtra("alarm_id",id);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c,710000+(int)(id%100000),i,flags);
    }
    @Override public void onReceive(Context c,Intent in){
        if(in==null)return;DataStore.migrateReminderDefaults(c);long id=in.getLongExtra("alarm_id",0);JSONObject alarm=DataStore.find(c,"alarms",id);
        if(alarm==null||!alarm.optBoolean("enabled",true)||!alarm.optBoolean("reminder_enabled",true))return;
        int minutes=Math.max(1,alarm.optInt("reminder_minutes",30));String selected=alarm.optString("reminder_sound","notification");boolean vibrate=alarm.optBoolean("reminder_vibrate",true);
        String alarmSound=alarm.optString("sound",DataStore.prefs(c).getString("alarm_sound_uri",""));Uri sound=resolveSound(c,selected,alarmSound);
        String soundKey=sound==null?"silent":sound.toString();String channel="tictic_pre_"+Integer.toHexString((soundKey+"|"+vibrate).hashCode());
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        if(Build.VERSION.SDK_INT>=24&&!manager.areNotificationsEnabled()){DataStore.log(c,"Alarm","Pre-alarm reminder not shown",alarm.optString("label","Alarm")+" · app notifications are disabled");return;}
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(channel,"Alarm reminders · "+soundName(selected),NotificationManager.IMPORTANCE_HIGH);ch.setDescription("A reminder before a scheduled TicTic alarm");ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);ch.setSound(sound,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());ch.enableVibration(vibrate);if(vibrate)ch.setVibrationPattern(new long[]{0,140});manager.createNotificationChannel(ch);NotificationChannel existing=manager.getNotificationChannel(channel);if(existing!=null&&existing.getImportance()==NotificationManager.IMPORTANCE_NONE){DataStore.log(c,"Alarm","Pre-alarm reminder not shown",alarm.optString("label","Alarm")+" · reminder channel is disabled in Android settings");return;}if(existing!=null&&(existing.getImportance()<=NotificationManager.IMPORTANCE_LOW||(sound!=null&&existing.getSound()==null)))DataStore.log(c,"Alarm","Reminder channel may be silent",alarm.optString("label","Alarm")+" · check TicTic alarm-reminder notification channel settings");}
        Intent open=new Intent(c,MainActivity.class).putExtra("tab","alarms").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content=PendingIntent.getActivity(c,720000+(int)(id%100000),open,flags);
        String label=alarm.optString("label","Alarm");String time=DataStore.formatTime(c,alarm.optInt("hour",7),alarm.optInt("minute",0));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Reminder · "+label).setContentText("Alarm at "+time+" · in "+minutes+(minutes==1?" minute":" minutes")).setCategory(Notification.CATEGORY_REMINDER).setPriority(Notification.PRIORITY_HIGH).setVisibility(Notification.VISIBILITY_PUBLIC).setAutoCancel(true).setContentIntent(content);
        b.addAction(android.R.drawable.ic_menu_close_clear_cancel,"Turn off alarm",AlarmActionReceiver.pending(c,AlarmActionReceiver.ACTION_DISABLE,id,3));
        if(Build.VERSION.SDK_INT<26){if(sound!=null)b.setSound(sound,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());if(vibrate)b.setVibrate(new long[]{0,140});}
        manager.notify(notificationId(id),b.build());DataStore.log(c,"Alarm","Pre-alarm reminder posted",label+" · "+time+" · "+minutes+" minutes before (Android may still suppress it)");
    }
    static int notificationId(long id){return (int)(id^(id>>>32));}
    static void cancelNotification(Context c,long id){NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(manager!=null)manager.cancel(notificationId(id));}
    static Uri resolveSound(Context c,String selected,String alarmSound){
        try{
            if("silent".equals(selected))return null;
            if("ping".equals(selected)||"same".equals(selected)||selected==null||selected.isEmpty())return Uri.parse("android.resource://"+c.getPackageName()+"/"+R.raw.reminder_ping);
            if("notification".equals(selected))return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if("alarm".equals(selected)){if(alarmSound!=null&&!alarmSound.isEmpty())return Uri.parse(alarmSound);return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);}
            if("default".equals(selected))return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            return Uri.parse(selected);
        }catch(Exception e){return Uri.parse("android.resource://"+c.getPackageName()+"/"+R.raw.reminder_ping);}
    }
    private static String soundName(String value){if("silent".equals(value))return "Silent";if("ping".equals(value)||value==null||value.isEmpty()||"same".equals(value))return "TicTic ping";if("notification".equals(value))return "Device notification tone";if("alarm".equals(value))return "Same as alarm";if("default".equals(value))return "Device alarm tone";if(value.contains("/"+R.raw.alarm_dawn))return "TicTic Dawn";if(value.contains("/"+R.raw.alarm_golden))return "Golden Hour";if(value.contains("/"+R.raw.alarm_pulse))return "Pulse";return "Custom tone";}
}
