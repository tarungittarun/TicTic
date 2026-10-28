package app.space.notes.runtime;
import android.content.*;
import app.space.notes.domain.Note;
import java.util.*;
public final class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){final PendingResult pending=goAsync();new Thread(()->{try{String action=intent.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){LocalSystemSurfaces.refresh(context,true);}else{String id=intent.getStringExtra("noteId");long at=intent.getLongExtra("at",0);LocalSystemSurfaces.deliver(context,id,at);}}catch(Exception unavailable){android.util.Log.w("MyNotes","Local reminders unavailable; library unchanged",unavailable);}finally{pending.finish();}},"notes-reminder").start();}
}
