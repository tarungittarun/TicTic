package app.tictic.clock;
import android.appwidget.*;import android.content.*;
public class NextAlarmWidgetProvider extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)WidgetEngine.update(c,id,R.layout.widget_alarm,"alarms");}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,android.os.Bundle options){WidgetEngine.update(c,id,R.layout.widget_alarm,"alarms");}
 @Override public void onDeleted(Context c,int[] ids){for(int id:ids)WidgetEngine.removeWidget(c,id);}
 static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,NextAlarmWidgetProvider.class)))WidgetEngine.update(c,id,R.layout.widget_alarm,"alarms");}
}
