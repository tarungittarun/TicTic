package app.space.notes.runtime;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import app.space.notes.domain.*;
import java.util.*;
/** Re-placeable, resizable local-library widget; pinned-first projection, no network services. */
public final class NotesWidget extends AppWidgetProvider {
 public static RemoteViews render(Context context,List<Note> notes){RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.notes_widget);views.setOnClickPendingIntent(R.id.widget_header,LocalReminders.open(context,""));int[] ids={R.id.widget_row0,R.id.widget_row1,R.id.widget_row2,R.id.widget_row3};List<Note> visible=LocalSurfaces.widget(notes);for(int i=0;i<ids.length;i++){views.setViewVisibility(ids[i],i<visible.size()?View.VISIBLE:View.GONE);if(i<visible.size()){Note n=visible.get(i);String title=n.title.isEmpty()?(n.checklist?"Checklist":"Untitled note"):n.title;views.setTextViewText(ids[i],(n.pinned?"★ ":"")+title.substring(0,Math.min(160,title.length())));views.setOnClickPendingIntent(ids[i],LocalReminders.open(context,n.id));}}if(visible.isEmpty()){views.setViewVisibility(ids[0],View.VISIBLE);views.setTextViewText(ids[0],"No active notes · open MyNotes to create one");views.setOnClickPendingIntent(ids[0],LocalReminders.open(context,""));}return views;}
 public static void updateAll(Context context,List<Note> notes){AppWidgetManager manager=AppWidgetManager.getInstance(context);int[] ids=manager.getAppWidgetIds(new ComponentName(context,NotesWidget.class));if(ids.length>0)manager.updateAppWidget(ids,render(context,notes));}
 private void refresh(Context context){final PendingResult pending=goAsync();new Thread(()->{try{LocalSystemSurfaces.refresh(context);}catch(Exception unavailable){android.util.Log.w("MyNotes","Widget refresh unavailable; library unchanged",unavailable);}finally{pending.finish();}},"notes-widget").start();}
 @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){refresh(context);}
 @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,Bundle options){refresh(context);}
}
