package app.tictic.clock;

import android.content.BroadcastReceiver;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** Handles Dismiss and Snooze controls on a reminder notification. */
public final class ReminderActionReceiver extends BroadcastReceiver {
    static final String ACTION_DONE="app.tictic.clock.action.REMINDER_DONE";
    static final String ACTION_SNOOZE="app.tictic.clock.action.REMINDER_SNOOZE";
    static PendingIntent pending(Context c,String action,long id,int offset){Intent i=new Intent(c,ReminderActionReceiver.class).setAction(action).setData(Uri.parse("tictic://reminder-action/"+id+"/"+offset)).putExtra("reminder_id",id);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;return PendingIntent.getBroadcast(c,950000+(int)(id%40000)*2+offset,i,flags);}
    @Override public void onReceive(Context c,Intent intent){if(intent==null)return;long id=intent.getLongExtra("reminder_id",0);ReminderSoundService.stop(c,id);if(ACTION_SNOOZE.equals(intent.getAction())){int minutes=DataStore.prefs(c).getInt("reminder_snooze_minutes",10);ReminderScheduler.scheduleSnooze(c,id,System.currentTimeMillis()+minutes*60_000L);ReminderReceiver.cancelNotification(c,id);DataStore.log(c,"Reminder","Reminder snoozed",minutes+" minutes");}else if(ACTION_DONE.equals(intent.getAction())){ReminderReceiver.cancelNotification(c,id);JSONArray all=DataStore.array(c,"reminders"),updated=new JSONArray();String title="Reminder";for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o==null)continue;if(o.optLong("id")==id){title=o.optString("title","Reminder");if(o.optInt("days",0)==0){try{o.put("completed",true).put("enabled",false);}catch(Exception ignored){}}}updated.put(o);}DataStore.putArray(c,"reminders",updated);ReminderScheduler.clearSnooze(c,id);DataStore.log(c,"Reminder","Reminder dismissed",title);}}
}
