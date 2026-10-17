package app.tictic.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Posts a normal reminder notification; it never starts the alarm ringing service. */
public final class ReminderReceiver extends BroadcastReceiver {
    static final String ACTION_FIRE="app.tictic.clock.action.FIRE_REMINDER";
    private static final String CHANNEL_VIBRATE="tictic_reminders_v197_vibrate",CHANNEL_QUIET="tictic_reminders_v197_quiet";
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;long id=intent.getLongExtra("reminder_id",0);boolean snooze=intent.getBooleanExtra("snooze",false);
        JSONObject reminder=DataStore.find(context,"reminders",id);if(reminder==null)return;
        if(snooze){if(ReminderScheduler.snoozeUntil(context,id)<=0)return;ReminderScheduler.clearSnooze(context,id);}
        else {
            if(!reminder.optBoolean("enabled",true))return;
            if(reminder.optInt("days",0)!=0)ReminderScheduler.schedule(context,reminder);
            else {setFiredOff(context,id);reminder=DataStore.find(context,"reminders",id);if(reminder==null)return;}
        }
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        boolean vibrate=reminder.optBoolean("vibrate",DataStore.prefs(context).getBoolean("reminder_default_vibrate",true));String channelId=vibrate?CHANNEL_VIBRATE:CHANNEL_QUIET;
        if(Build.VERSION.SDK_INT>=26){if(manager.getNotificationChannel(CHANNEL_VIBRATE)==null){NotificationChannel channel=new NotificationChannel(CHANNEL_VIBRATE,"Reminders · vibration",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("Local TicTic reminders; their selected tone plays for the chosen duration");channel.setSound(null,null);channel.enableVibration(true);channel.setVibrationPattern(new long[]{0,120,80,120});manager.createNotificationChannel(channel);}if(manager.getNotificationChannel(CHANNEL_QUIET)==null){NotificationChannel channel=new NotificationChannel(CHANNEL_QUIET,"Reminders · quiet",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("Local TicTic reminders without vibration");channel.setSound(null,null);channel.enableVibration(false);manager.createNotificationChannel(channel);}}
        String title=reminder.optString("title","Reminder");String note=reminder.optString("note","").trim();
        Intent open=new Intent(context,MainActivity.class).putExtra("tab","reminders").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content=PendingIntent.getActivity(context,940000+(int)(id%40000),open,flags);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channelId):new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(note.isEmpty()?"Reminder · "+new SimpleDateFormat("EEE, d MMM · h:mm a",Locale.getDefault()).format(new Date(System.currentTimeMillis())):note)
                .setCategory(Notification.CATEGORY_REMINDER).setPriority(Notification.PRIORITY_HIGH).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(content);
        if(Build.VERSION.SDK_INT<26){b.setSound(null);if(vibrate)b.setVibrate(new long[]{0,120,80,120});else b.setVibrate(null);}
        int snoozeMinutes=DataStore.prefs(context).getInt("reminder_snooze_minutes",10);b.addAction(android.R.drawable.checkbox_on_background,"Dismiss",ReminderActionReceiver.pending(context,ReminderActionReceiver.ACTION_DONE,id,1));
        b.addAction(android.R.drawable.ic_lock_idle_alarm,"Snooze "+snoozeMinutes+" min",ReminderActionReceiver.pending(context,ReminderActionReceiver.ACTION_SNOOZE,id,2));
        int duration=reminder.optInt("sound_seconds",DataStore.prefs(context).getInt("reminder_default_ring_seconds",5));String tone=reminder.optString("tone","default"),toneUri=reminder.optString("tone_uri","");Uri alertTone=ReminderSoundService.resolveTone(context,tone,toneUri);Notification notification=b.build();int notificationId=notificationId(id);if(!ReminderSoundService.start(context,id,notificationId,alertTone,duration,notification))manager.notify(notificationId,notification);DataStore.log(context,"Reminder","Reminder shown",title+(snooze?" · snoozed":"")+" · "+tone+" · "+Math.max(0,duration)+" sec");
    }
    private void setFiredOff(Context c,long id){JSONArray old=DataStore.array(c,"reminders"),next=new JSONArray();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o==null)continue;if(o.optLong("id")==id)try{o.put("enabled",false).put("fired",true);}catch(Exception ignored){}next.put(o);}DataStore.putArray(c,"reminders",next);ReminderScheduler.cancel(c,id);}
    static int notificationId(long id){return (int)(0x41000000^(id^(id>>>32)));}
    static void cancelNotification(Context c,long id){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm!=null)nm.cancel(notificationId(id));}
}
