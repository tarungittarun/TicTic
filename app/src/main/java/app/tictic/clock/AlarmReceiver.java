package app.tictic.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent in) {
        long id=in.getLongExtra("alarm_id",0); boolean snooze=in.getBooleanExtra("snooze",false);PreAlarmReceiver.cancelNotification(c,id);
        JSONObject o=DataStore.find(c,"alarms",id);
        // AlarmManager.cancel cannot retract a broadcast already queued for delivery.
        // Re-check persisted state here so a stale/already-queued alarm cannot start ringing.
        if(o==null){if(snooze)AlarmActionReceiver.clearSnooze(c,id);return;}
        if(snooze){if(AlarmActionReceiver.snoozeUntil(c,id)<=0)return;AlarmActionReceiver.clearSnooze(c,id);}
        else if(!o.optBoolean("enabled",true)){AlarmScheduler.cancel(c,id);return;}
        String label=o.optString("label","Alarm");
        if(!snooze && o!=null) {
            if(o.optInt("days",0)==0) {
                JSONArray all=DataStore.array(c,"alarms"), updated=new JSONArray();
                for(int i=0;i<all.length();i++){JSONObject x=all.optJSONObject(i);if(x!=null){if(x.optLong("id")==id){try{x.put("enabled",false);}catch(Exception ignored){}}updated.put(x);}}
                DataStore.putArray(c,"alarms",updated);
            } else AlarmScheduler.schedule(c,o);
        }
        if(!snooze)DataStore.log(c,"Alarm","Alarm rang",label);
        String sound=(o!=null&&o.has("sound"))?o.optString("sound",""):DataStore.prefs(c).getString("alarm_sound_uri","");Intent service=new Intent(c,AlarmSoundService.class).putExtra("alarm_id",id).putExtra("label",label).putExtra("vibrate",o==null||o.optBoolean("vibrate",true)).putExtra("sound",sound).putExtra("gentle",o!=null&&o.optBoolean("gentle",false));
        try { if(Build.VERSION.SDK_INT>=26)c.startForegroundService(service); else c.startService(service); } catch(Exception ignored) {}
        Intent show=new Intent(c,AlarmActivity.class).putExtra("alarm_id",id).putExtra("label",label).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try { c.startActivity(show); } catch(Exception ignored) {}
        WidgetEngine.refreshAll(c);
    }
}
