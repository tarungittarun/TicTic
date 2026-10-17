package app.tictic.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local on-device schedule for user reminders. */
final class ReminderScheduler {
    private static final String SNOOZE_PREFIX="reminder_snooze_";
    private ReminderScheduler(){}
    static PendingIntent pending(Context c,long id,boolean snooze,long stamp){
        Intent i=new Intent(c,ReminderReceiver.class).setAction(ReminderReceiver.ACTION_FIRE)
                .setData(Uri.parse("tictic://reminder/"+(snooze?"snooze/":"item/")+id+(snooze?"/"+stamp:"")))
                .putExtra("reminder_id",id).putExtra("snooze",snooze);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c,930000+(int)(id%30000)*2+(snooze?1:0),i,flags);
    }
    static long snoozeUntil(Context c,long id){return DataStore.prefs(c).getLong(SNOOZE_PREFIX+id,0);}
    static void scheduleSnooze(Context c,long id,long at){DataStore.prefs(c).edit().putLong(SNOOZE_PREFIX+id,at).apply();AlarmScheduler.set(c,at,pending(c,id,true,at));}
    static void clearSnooze(Context c,long id){long at=snoozeUntil(c,id);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null&&at>0)am.cancel(pending(c,id,true,at));DataStore.prefs(c).edit().remove(SNOOZE_PREFIX+id).apply();}
    static void schedule(Context c,JSONObject reminder){
        if(reminder==null)return;long id=reminder.optLong("id");if(!reminder.optBoolean("enabled",true)){cancelMain(c,id);ReminderReceiver.cancelNotification(c,id);return;}
        long now=System.currentTimeMillis();long after=Math.max(now,reminder.optLong("skip_through",0));long at=reminder.optInt("days",0)!=0?DataStore.nextOccurrence(reminder,after):reminder.optLong("at",0);
        if(at<=now){cancelMain(c,id);return;}AlarmScheduler.set(c,at,pending(c,id,false,0));
    }
    private static void cancelMain(Context c,long id){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(pending(c,id,false,0));}
    static void cancel(Context c,long id){cancelMain(c,id);clearSnooze(c,id);ReminderSoundService.stop(c,id);ReminderReceiver.cancelNotification(c,id);}
    static void rescheduleAll(Context c){JSONArray all=DataStore.array(c,"reminders");long now=System.currentTimeMillis();for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o==null)continue;schedule(c,o);long snooze=snoozeUntil(c,o.optLong("id"));if(snooze>now)AlarmScheduler.set(c,snooze,pending(c,o.optLong("id"),true,snooze));else if(snooze>0)clearSnooze(c,o.optLong("id"));}}
}
