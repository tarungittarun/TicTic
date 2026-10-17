package app.tictic.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** Handles notification buttons and keeps the ringing surface in sync. */
public final class AlarmActionReceiver extends BroadcastReceiver {
    static final String ACTION_SNOOZE="app.tictic.clock.action.SNOOZE";
    static final String ACTION_DISMISS="app.tictic.clock.action.DISMISS";
    static final String ACTION_DISABLE="app.tictic.clock.action.DISABLE";
    static final String ACTION_FINISH="app.tictic.clock.action.FINISH_RINGING_UI";
    private static final String SNOOZE_PREFIX="alarm_snooze_";

    static PendingIntent pending(Context context,String action,long id,int offset){
        Intent intent=new Intent(context,AlarmActionReceiver.class).setAction(action)
                .setData(Uri.parse("tictic://alarm-action/"+id+"/"+offset)).putExtra("alarm_id",id);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context,510000+(int)(id%40000)*4+offset,intent,flags);
    }

    static long snoozeUntil(Context c,long id){return DataStore.prefs(c).getLong(SNOOZE_PREFIX+id,0);}
    static void clearSnooze(Context c,long id){DataStore.prefs(c).edit().remove(SNOOZE_PREFIX+id).apply();}

    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;
        long id=intent.getLongExtra("alarm_id",0);
        if(ACTION_SNOOZE.equals(intent.getAction()))snooze(context,id);
        else if(ACTION_DISABLE.equals(intent.getAction()))disable(context,id);
        else if(ACTION_DISMISS.equals(intent.getAction()))dismiss(context,id);
    }

    static void snooze(Context c,long id){
        JSONObject alarm=DataStore.find(c,"alarms",id);
        int minutes=alarm==null?9:Math.max(1,alarm.optInt("snooze",9));
        long until=System.currentTimeMillis()+minutes*60_000L;
        DataStore.prefs(c).edit().putLong(SNOOZE_PREFIX+id,until).apply();
        AlarmScheduler.scheduleSnooze(c,id,until);
        String label=alarm==null?"Alarm":alarm.optString("label","Alarm");
        stopAndNotify(c,id);
        DataStore.log(c,"Alarm","Snoozed",label+" · "+minutes+" min");
        finishRingingUi(c,id);
    }

    static void dismiss(Context c,long id){
        JSONObject alarm=DataStore.find(c,"alarms",id);
        clearSnooze(c,id);
        String label=alarm==null?"Alarm":alarm.optString("label","Alarm");
        stopAndNotify(c,id);
        DataStore.log(c,"Alarm","Stopped",label);
        finishRingingUi(c,id);
    }

    static void disable(Context c,long id){
        JSONArray alarms=DataStore.array(c,"alarms"),updated=new JSONArray();
        JSONObject selected=null;
        for(int i=0;i<alarms.length();i++){
            JSONObject alarm=alarms.optJSONObject(i);
            if(alarm==null)continue;
            if(alarm.optLong("id")==id){selected=alarm;try{alarm.put("enabled",false);}catch(Exception ignored){}}
            updated.put(alarm);
        }
        DataStore.putArray(c,"alarms",updated);
        clearSnooze(c,id);
        AlarmScheduler.cancel(c,id);
        stopAndNotify(c,id);
        DataStore.log(c,"Alarm","Alarm disabled",selected==null?"Upcoming alarm":"Upcoming · "+selected.optString("label","Alarm"));
        WidgetEngine.refreshAll(c);
        finishRingingUi(c,id);
    }

    private static void stopAndNotify(Context c,long id){
        AlarmSoundService.stop(c);
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null){manager.cancel(AlarmSoundService.NOTIFICATION);manager.cancel(PreAlarmReceiver.notificationId(id));}
    }

    private static void finishRingingUi(Context c,long id){
        Intent done=new Intent(ACTION_FINISH).setPackage(c.getPackageName()).putExtra("alarm_id",id);
        c.sendBroadcast(done);
    }
}
