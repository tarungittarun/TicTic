package app.space.notes.runtime;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.content.pm.PackageManager;
import app.space.notes.domain.*;
import java.util.*;

/** Inexact local reminders: no exact-alarm exemption, network, or background microphone. */
public final class LocalReminders {
    public static final String CHANNEL="mynotes-reminders-v1";
    private static final String INDEX="mynotes-alarms-v1";
    private LocalReminders() { }
    public static PendingIntent alarm(Context context,String id,long at){Intent intent=new Intent(context,ReminderReceiver.class).setData(Uri.parse("mynotes://reminder/"+Uri.encode(id))).putExtra("noteId",id).putExtra("at",at);return PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static Intent openIntent(Context context,String id){return new Intent(Intent.ACTION_VIEW,Uri.parse("mynotes://note/"+Uri.encode(id))).setClassName(context.getPackageName(),"app.space.notes.NotesActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("noteId",id);}
    public static PendingIntent open(Context context,String id){return PendingIntent.getActivity(context,0,openIntent(context,id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    public static void reconcile(Context context,List<Note> notes){
        AlarmManager manager=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);if(manager==null)return;
        NotificationManager notifications=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);Map<String,Note> byId=new HashMap<>();for(Note note:notes)byId.put(note.id,note);
        for(android.service.notification.StatusBarNotification active:notifications.getActiveNotifications())if(active.getId()==1 && CHANNEL.equals(active.getNotification().getChannelId())){Note note=byId.get(active.getTag());if(note==null || note.deletedAt>0 || note.reminderAt==0 || note.reminderAt!=active.getNotification().extras.getLong("mynotes.reminder_at",0))notifications.cancel(active.getTag(),1);}
        SharedPreferences prefs=context.getSharedPreferences(INDEX,Context.MODE_PRIVATE);Set<String> old=prefs.getStringSet("ids",Collections.emptySet());Set<String> current=new HashSet<>();long now=System.currentTimeMillis();
        for(Note n:notes)if(LocalSurfaces.scheduled(n,now)){current.add(n.id);manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.reminderAt,alarm(context,n.id,n.reminderAt));}
        for(String id:old)if(!current.contains(id)){manager.cancel(alarm(context,id,0));}
        prefs.edit().putStringSet("ids",current).apply();
    }
    public static boolean notifyDue(Context context,Note note,long expected,long now){
        if(note.deletedAt>0 || note.reminderAt==0 || note.reminderAt!=expected || now<expected)return false;
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        SharedPreferences delivered=context.getSharedPreferences("mynotes-delivered-v1",Context.MODE_PRIVATE);if(delivered.getLong(note.id,0)==expected)return false;
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Note reminders",NotificationManager.IMPORTANCE_DEFAULT));if(!manager.areNotificationsEnabled() || manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE)return false;
        android.os.Bundle extras=new android.os.Bundle();extras.putLong("mynotes.reminder_at",expected);
        Notification notification=new Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(note.title.isEmpty()?"MyNotes reminder":note.title).setContentText("Tap to open your local note").setContentIntent(open(context,note.id)).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setExtras(extras).build();
        manager.notify(note.id,1,notification);delivered.edit().putLong(note.id,expected).commit();return true;
    }
}
