package app.tictic.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;

/** Performs the small, explicit actions exposed by task-oriented widgets without opening a form. */
public final class WidgetActionReceiver extends BroadcastReceiver {
    static final String ACTION_TOGGLE_ALARM="app.tictic.clock.widget.TOGGLE_ALARM";
    static final String ACTION_START_FOCUS="app.tictic.clock.widget.START_FOCUS";
    static final String ACTION_TOGGLE_TIMER="app.tictic.clock.widget.TOGGLE_TIMER";
    static final String EXTRA_ALARM_ID="alarm_id";
    static final String EXTRA_MINUTES="minutes";
    static final String EXTRA_WIDGET_ID="widget_id";

    @Override public void onReceive(Context context,Intent intent) {
        if(intent==null)return;
        String action=intent.getAction();
        if(ACTION_TOGGLE_ALARM.equals(action))toggleAlarm(context,intent.getLongExtra(EXTRA_ALARM_ID,-1));
        else if(ACTION_START_FOCUS.equals(action))startFocus(context,intent.getIntExtra(EXTRA_MINUTES,0));
        else if(ACTION_TOGGLE_TIMER.equals(action))toggleTimer(context);
    }

    private static void toggleAlarm(Context context,long alarmId) {
        if(alarmId<0)return;
        JSONArray alarms=DataStore.array(context,"alarms");JSONObject selected=null;
        for(int i=0;i<alarms.length();i++) {
            JSONObject alarm=alarms.optJSONObject(i);
            if(alarm!=null&&alarm.optLong("id",-1)==alarmId) {selected=alarm;break;}
        }
        if(selected==null)return;
        boolean enabled=!selected.optBoolean("enabled",true);
        try{selected.put("enabled",enabled);}catch(Exception ignored){return;}
        DataStore.putArray(context,"alarms",alarms);
        if(enabled)AlarmScheduler.schedule(context,selected);else AlarmScheduler.cancel(context,alarmId);
        DataStore.log(context,"Alarm",enabled?"Alarm enabled":"Alarm disabled",
                selected.optString("label","Alarm")+" · changed from Wake Switch widget");
        WidgetEngine.refreshAll(context);
    }

    private static void startFocus(Context context,int minutes) {
        if(minutes!=25&&minutes!=50&&minutes!=90)return;
        long duration=minutes*60000L;
        DataStore.prefs(context).edit().putLong("timer_duration",duration)
                .putLong("timer_remaining",duration).apply();
        TimerReceiver.start(context,duration);
        DataStore.log(context,"Timer","Focus timer started",minutes+" minute focus preset from widget");
    }

    private static void toggleTimer(Context context) {
        if(TimerReceiver.isRunning(context)) {
            long remaining=TimerReceiver.remaining(context);
            TimerReceiver.pause(context,remaining);
            DataStore.log(context,"Timer","Timer paused","Widget pause · "+remaining+" ms remaining");
        } else {
            long remaining=TimerReceiver.remaining(context);
            if(remaining<=0)remaining=DataStore.prefs(context).getLong("timer_default_duration",300000L);
            TimerReceiver.start(context,remaining);
            DataStore.log(context,"Timer","Timer started","Widget start/resume · "+remaining+" ms");
        }
    }
}
