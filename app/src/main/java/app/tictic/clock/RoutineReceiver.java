package app.tictic.clock;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public class RoutineReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        long id=i.getLongExtra("routine_id",0); boolean ending=i.getBooleanExtra("routine_end",false); JSONObject r=DataStore.find(c,"routines",id); if(r==null||!r.optBoolean("enabled",true))return;
        String key="routine_active_"+id; boolean active=DataStore.prefs(c).getBoolean(key,false);
        if(ending){if(active){DataStore.prefs(c).edit().putBoolean(key,false).remove("routine_end_"+id).apply();DataStore.log(c,"Routine","Routine ended",r.optString("name"));notify(c,r.optString("name","Routine"),false);}scheduleStart(c,r);}
        else if(!active){long end=System.currentTimeMillis()+Math.max(1,r.optInt("duration",60))*60000L;DataStore.prefs(c).edit().putBoolean(key,true).putLong("routine_end_"+id,end).apply();DataStore.log(c,"Routine","Routine started",r.optString("name"));notify(c,r.optString("name","Routine"),true);scheduleEnd(c,id,end);}
        else scheduleStart(c,r);
        WidgetEngine.refreshAll(c);
    }
    static void scheduleAll(Context c){JSONArray a=DataStore.array(c,"routines");for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(r==null)continue;long id=r.optLong("id");if(!r.optBoolean("enabled",true)){cancel(c,id);if(DataStore.prefs(c).getBoolean("routine_active_"+id,false))DataStore.prefs(c).edit().putBoolean("routine_active_"+id,false).remove("routine_end_"+id).apply();continue;}long end=DataStore.prefs(c).getLong("routine_end_"+id,0);if(DataStore.prefs(c).getBoolean("routine_active_"+id,false)&&end>System.currentTimeMillis())scheduleEnd(c,id,end);else{DataStore.prefs(c).edit().putBoolean("routine_active_"+id,false).remove("routine_end_"+id).apply();scheduleStart(c,r);}}}
    private static PendingIntent pi(Context c,long id,boolean ending){Intent in=new Intent(c,RoutineReceiver.class).putExtra("routine_id",id).putExtra("routine_end",ending).setAction(ending?"app.tictic.clock.ROUTINE_END":"app.tictic.clock.ROUTINE_START").setData(Uri.parse("tictic://routine/"+(ending?"end/":"start/")+id));int f=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)f|=PendingIntent.FLAG_IMMUTABLE;int request=(int)(id^(id>>>32))+(ending?140000:130000);return PendingIntent.getBroadcast(c,request,in,f);}
    private static void scheduleStart(Context c,JSONObject r){JSONObject fake=new JSONObject();try{fake.put("hour",r.optInt("hour",21)).put("minute",r.optInt("minute",0)).put("days",r.optInt("days",127)).put("enabled",true);}catch(Exception ignored){}long at=DataStore.nextOccurrence(fake,System.currentTimeMillis());AlarmScheduler.set(c,at,pi(c,r.optLong("id"),false));}
    private static void scheduleEnd(Context c,long id,long at){AlarmScheduler.set(c,at,pi(c,id,true));}
    private static void cancel(Context c,long id){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am!=null){am.cancel(pi(c,id,false));am.cancel(pi(c,id,true));}}
    private void notify(Context c,String name,boolean active){if(!DataStore.prefs(c).getBoolean("routine_notifications_enabled",true))return;String ch="tictic_routine";NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(ch,"TicTic routines",NotificationManager.IMPORTANCE_DEFAULT));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,ch):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.ic_menu_recent_history).setContentTitle(name+(active?" is on":" ended")).setContentText(active?"Your routine is active":"Routine window finished").setAutoCancel(true);nm.notify((int)(920000+(System.currentTimeMillis()%10000)),b.build());}
}
