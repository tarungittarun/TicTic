package app.tictic.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

final class AlarmScheduler {
    static final String ACTION_FIRE="app.tictic.clock.FIRE_ALARM";
    static PendingIntent pending(Context c, long id, boolean snooze, long stamp) {
        Intent i=new Intent(c,AlarmReceiver.class).setAction(ACTION_FIRE).setData(Uri.parse("tictic://"+(snooze?"snooze/":"alarm/")+id+(snooze?"/"+stamp:"")));
        i.putExtra("alarm_id",id); i.putExtra("snooze",snooze);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT; if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c,(int)(id ^ (snooze?stamp:0)),i,flags);
    }
    static boolean canExact(Context c) {
        if(Build.VERSION.SDK_INT<31)return true;
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        return a!=null && a.canScheduleExactAlarms();
    }
    static void schedule(Context c, JSONObject o) {
        if(o==null)return;
        long id=o.optLong("id");PreAlarmReceiver.cancelNotification(c,id);
        if(!o.optBoolean("enabled",true)){cancelScheduled(c,id);return;}
        long now=System.currentTimeMillis();long at=DataStore.nextOccurrence(o,now);
        scheduleReminder(c,o,at,now);
        PendingIntent operation=pending(c,id,false,0);
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(a==null)return;
        try {
            if(canExact(c)){
                Intent show=new Intent(c,MainActivity.class).putExtra("tab","alarms").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
                PendingIntent showIntent=PendingIntent.getActivity(c,510000+(int)(id%40000),show,flags);
                a.setAlarmClock(new AlarmManager.AlarmClockInfo(at,showIntent),operation);
            }else set(c,at,operation);
        }catch(SecurityException ex){set(c,at,operation);}
    }
    static void scheduleSnooze(Context c,long id,long at) { set(c,at,pending(c,id,true,at)); }
    static void cancelSnooze(Context c,long id,long at){AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(manager!=null&&at>0)manager.cancel(pending(c,id,true,at));}
    /** Exact when allowed, otherwise uses the platform's inexact idle-friendly fallback. Timers/routines must not advertise as a system alarm clock. */
    static boolean set(Context c,long at,PendingIntent p) {
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(a==null)return false;
        try{if(canExact(c)){a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);return true;}a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);return false;}
        catch(SecurityException ex){try{a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);}catch(Exception ignored){}return false;}
    }
    private static void scheduleReminder(Context c,JSONObject o,long alarmAt,long now){
        long id=o.optLong("id");PendingIntent reminder=PreAlarmReceiver.pending(c,id);
        if(!o.optBoolean("reminder_enabled",true)){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(reminder);DataStore.prefs(c).edit().remove("alarm_reminder_status_"+id).apply();return;}
        int minutes=Math.max(1,Math.min(180,o.optInt("reminder_minutes",30)));long at=alarmAt-minutes*60000L;
        if(at<=now){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null)am.cancel(reminder);DataStore.prefs(c).edit().putString("alarm_reminder_status_"+id,"No ping this occurrence: the configured lead time has already passed.").apply();return;}
        boolean exact=set(c,at,reminder);DataStore.prefs(c).edit().putString("alarm_reminder_status_"+id,exact?"Scheduled exactly.":"Scheduled inexactly; Android may deliver it late.").apply();
    }
    static void cancelScheduled(Context c,long id){AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(a!=null){a.cancel(pending(c,id,false,0));a.cancel(PreAlarmReceiver.pending(c,id));}PreAlarmReceiver.cancelNotification(c,id);}
    static void cancel(Context c,long id) { cancelScheduled(c,id);long snoozeAt=AlarmActionReceiver.snoozeUntil(c,id);cancelSnooze(c,id,snoozeAt);AlarmActionReceiver.clearSnooze(c,id); }
    static void rescheduleAll(Context c) {
        JSONArray a=DataStore.array(c,"alarms");long now=System.currentTimeMillis();for(int i=0;i<a.length();i++){JSONObject alarm=a.optJSONObject(i);schedule(c,alarm);if(alarm!=null){long id=alarm.optLong("id"),snoozeAt=AlarmActionReceiver.snoozeUntil(c,id);if(snoozeAt>now)scheduleSnooze(c,id,snoozeAt);else if(snoozeAt>0)AlarmActionReceiver.clearSnooze(c,id);}}
        long end=DataStore.prefs(c).getLong("timer_end",0);
        if(end>0) {
            long left=TimerReceiver.remaining(c);
            if(left<=0)c.sendBroadcast(new Intent(c,TimerReceiver.class).setData(Uri.parse("tictic://timer")));
            else TimerReceiver.scheduleRemaining(c,left);
        }
    }
}
