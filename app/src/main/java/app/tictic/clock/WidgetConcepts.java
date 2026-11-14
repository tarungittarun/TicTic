package app.tictic.clock;

import android.app.PendingIntent;
import android.widget.RemoteViews;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import app.space.core.domain.TimeMath;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Live data and interactions for the purpose-built widget concepts, separate from their visual skins. */
final class WidgetConcepts {
    private static final int[] WEEK_CELL_IDS={
            R.id.widget_week_cell_0,R.id.widget_week_cell_1,R.id.widget_week_cell_2,
            R.id.widget_week_cell_3,R.id.widget_week_cell_4,R.id.widget_week_cell_5,
            R.id.widget_week_cell_6};
    private static final int[] WEEK_DAY_IDS={
            R.id.widget_week_day_0,R.id.widget_week_day_1,R.id.widget_week_day_2,
            R.id.widget_week_day_3,R.id.widget_week_day_4,R.id.widget_week_day_5,
            R.id.widget_week_day_6};
    private static final int[] WEEK_DATE_IDS={
            R.id.widget_week_date_0,R.id.widget_week_date_1,R.id.widget_week_date_2,
            R.id.widget_week_date_3,R.id.widget_week_date_4,R.id.widget_week_date_5,
            R.id.widget_week_date_6};
    private static final int[] MONTH_WEEKDAY_IDS={
            R.id.widget_month_weekday_0,R.id.widget_month_weekday_1,R.id.widget_month_weekday_2,
            R.id.widget_month_weekday_3,R.id.widget_month_weekday_4,R.id.widget_month_weekday_5,
            R.id.widget_month_weekday_6};
    private static final int[] MONTH_DAY_IDS={
            R.id.widget_month_day_0,R.id.widget_month_day_1,R.id.widget_month_day_2,
            R.id.widget_month_day_3,R.id.widget_month_day_4,R.id.widget_month_day_5,
            R.id.widget_month_day_6,R.id.widget_month_day_7,R.id.widget_month_day_8,
            R.id.widget_month_day_9,R.id.widget_month_day_10,R.id.widget_month_day_11,
            R.id.widget_month_day_12,R.id.widget_month_day_13,R.id.widget_month_day_14,
            R.id.widget_month_day_15,R.id.widget_month_day_16,R.id.widget_month_day_17,
            R.id.widget_month_day_18,R.id.widget_month_day_19,R.id.widget_month_day_20,
            R.id.widget_month_day_21,R.id.widget_month_day_22,R.id.widget_month_day_23,
            R.id.widget_month_day_24,R.id.widget_month_day_25,R.id.widget_month_day_26,
            R.id.widget_month_day_27,R.id.widget_month_day_28,R.id.widget_month_day_29,
            R.id.widget_month_day_30,R.id.widget_month_day_31,R.id.widget_month_day_32,
            R.id.widget_month_day_33,R.id.widget_month_day_34,R.id.widget_month_day_35,
            R.id.widget_month_day_36,R.id.widget_month_day_37,R.id.widget_month_day_38,
            R.id.widget_month_day_39,R.id.widget_month_day_40,R.id.widget_month_day_41};
    private static final int[] MONTH_COMPACT_WEEKDAY_IDS={
            R.id.widget_month_compact_weekday_0,R.id.widget_month_compact_weekday_1,
            R.id.widget_month_compact_weekday_2,R.id.widget_month_compact_weekday_3,
            R.id.widget_month_compact_weekday_4,R.id.widget_month_compact_weekday_5,
            R.id.widget_month_compact_weekday_6};
    private static final int[] MONTH_COMPACT_DAY_IDS={
            R.id.widget_month_compact_day_0,R.id.widget_month_compact_day_1,
            R.id.widget_month_compact_day_2,R.id.widget_month_compact_day_3,
            R.id.widget_month_compact_day_4,R.id.widget_month_compact_day_5,
            R.id.widget_month_compact_day_6};

    private WidgetConcepts() { }

    private static final class Occurrence {
        final JSONObject item;
        final long at;
        Occurrence(JSONObject item,long at){this.item=item;this.at=at;}
    }

    static boolean isConcept(String layout) {
        switch(layout) {
            case "widget_alarm_countdown": case "widget_alarm_stack": case "widget_wake_switch":
            case "widget_quick_alarm": case "widget_world_grid": case "widget_meeting_window":
            case "widget_week_strip": case "widget_month_grid": case "widget_year_pulse":
            case "widget_day_balance": case "widget_routine_stack": case "widget_routine_countdown":
            case "widget_focus_launch": case "widget_timer_live": case "widget_lap_insights":
                return true;
            default:return false;
        }
    }

    static void render(Context context,RemoteViews views,int widgetId,String layout,boolean interactive) {
        switch(layout) {
            case "widget_alarm_countdown":
                countdown(context,views,R.id.widget_alarm_countdown,nextAlarm(context,true),"No enabled alarm");
                break;
            case "widget_alarm_stack":
                alarmStack(context,views);
                break;
            case "widget_wake_switch":
                wakeSwitch(context,views,widgetId,interactive);
                break;
            case "widget_quick_alarm":
                quickAlarmButtons(context,views,widgetId,interactive);
                break;
            case "widget_world_grid":
                worldGrid(views);
                break;
            case "widget_meeting_window":
                meetingWindow(context,views);
                break;
            case "widget_week_strip":
                weekStrip(context,views);
                break;
            case "widget_month_grid":
                monthGrid(context,views);
                break;
            case "widget_year_pulse":
                yearPulse(views);
                break;
            case "widget_day_balance":
                dayBalance(views);
                break;
            case "widget_routine_stack":
                routineStack(context,views);
                break;
            case "widget_routine_countdown":
                routineCountdown(context,views);
                break;
            case "widget_focus_launch":
                focusButtons(context,views,widgetId,interactive);
                break;
            case "widget_timer_live":
                timerLive(context,views,widgetId,interactive);
                break;
            case "widget_lap_insights":
                lapInsights(context,views);
                break;
            default:
                break;
        }
    }

    /** Apply user-selected ink, size and alignment to concept-specific RemoteViews. */
    static void applyStyle(Context context,RemoteViews views,int widgetId,String layout,
                           WidgetEngine.PreviewStyle preview,int width,int height) {
        if(!isConcept(layout))return;
        android.content.SharedPreferences prefs=DataStore.prefs(context);
        String key="widget_"+widgetId+"_";
        boolean configured=preview!=null||prefs.getBoolean(key+"configured",false);
        boolean wallpaper=preview!=null?preview.wallpaperColors:prefs.getBoolean(key+"wallpaper_colors",false);
        int ink=0;
        if(configured||wallpaper) {
            ink=preview!=null?preview.textColor:prefs.getInt(key+"text_color",WidgetEngine.defaultTextColor(context,widgetId));
            if(wallpaper)ink=WidgetEngine.previewWallpaperPalette(context,WidgetEngine.defaultBackground(context,widgetId))[0];
            for(int id:textIds(layout))views.setTextColor(id,ink);
        }

        float defaultSize=WidgetEngine.defaultSize(context,widgetId);
        float size=configured?(preview!=null?preview.size:prefs.getFloat(key+"size",defaultSize)):defaultSize;
        float scale=Math.max(.65f,Math.min(1.5f,size/Math.max(1f,defaultSize)));
        float fit=width<155?.82f:width<180?.92f:1f;
        if(height<90)fit=Math.min(fit,.82f);else if(height<110)fit=Math.min(fit,.92f);
        for(int id:textIds(layout)) {
            int base=baseTextSize(layout,id);
            views.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_SP,Math.max(7f,base*scale*fit));
        }
        int alignment=preview!=null?preview.alignment:prefs.getInt(key+"alignment",-1);
        if(configured&&alignment>=0) {
            int gravity=alignment==0?Gravity.CENTER:alignment==1?Gravity.START|Gravity.CENTER_VERTICAL:Gravity.END|Gravity.CENTER_VERTICAL;
            for(int id:alignmentIds(layout))views.setInt(id,"setGravity",gravity);
        }
        applyCalendarAccents(context,views,layout,configured||wallpaper,ink);
        applyActionContrast(views,layout);
        applyResponsiveLayout(context,views,layout,width,height);
    }

    private static int dp(Context context,int value){return (int)(value*context.getResources().getDisplayMetrics().density+.5f);}

    private static void applyResponsiveLayout(Context context,RemoteViews views,String layout,int width,int height) {
        boolean compact=width<155||height<110;
        if(compact) {
            views.setViewVisibility(R.id.widget_caption,View.GONE);
            if(!"widget_month_grid".equals(layout))views.setViewVisibility(R.id.widget_concept_heading,View.GONE);
            int inset=height<90?4:6;
            views.setViewPadding(R.id.widget_content,dp(context,inset),dp(context,inset),dp(context,inset),dp(context,inset));
        }
        if("widget_month_grid".equals(layout)) {
            boolean compactMonth=width<165||height<200;
            views.setViewVisibility(R.id.widget_month_grid,compactMonth?View.GONE:View.VISIBLE);
            views.setViewVisibility(R.id.widget_month_compact,compactMonth?View.VISIBLE:View.GONE);
            if(compactMonth) {
                views.setViewVisibility(R.id.widget_caption,View.GONE);
                int inset=height<90?4:6;
                views.setViewPadding(R.id.widget_content,dp(context,inset),dp(context,inset),dp(context,inset),dp(context,inset));
            }
        }
    }

    private static void applyActionContrast(RemoteViews views,String layout) {
        if("widget_wake_switch".equals(layout))views.setTextColor(R.id.widget_wake_switch,0xff173a35);
        else if("widget_quick_alarm".equals(layout)) {
            views.setTextColor(R.id.widget_quick_alarm_10,0xff082c2b);
            views.setTextColor(R.id.widget_quick_alarm_30,0xff082c2b);
            views.setTextColor(R.id.widget_quick_alarm_60,0xff082c2b);
        } else if("widget_focus_launch".equals(layout)) {
            views.setTextColor(R.id.widget_focus_25,Color.WHITE);
            views.setTextColor(R.id.widget_focus_50,Color.WHITE);
            views.setTextColor(R.id.widget_focus_90,Color.WHITE);
        } else if("widget_timer_live".equals(layout))views.setTextColor(R.id.widget_timer_live_action,0xff173a52);
    }

    private static int[] textIds(String layout) {
        switch(layout) {
            case "widget_alarm_countdown":return new int[]{R.id.widget_caption,R.id.widget_alarm_countdown};
            case "widget_alarm_stack":return new int[]{R.id.widget_caption,R.id.widget_alarm_row_0,R.id.widget_alarm_row_1,R.id.widget_alarm_row_2};
            case "widget_wake_switch":return new int[]{R.id.widget_caption,R.id.widget_wake_status,R.id.widget_wake_switch};
            case "widget_quick_alarm":return new int[]{R.id.widget_caption,R.id.widget_quick_alarm_10,R.id.widget_quick_alarm_30,R.id.widget_quick_alarm_60};
            case "widget_world_grid":return new int[]{R.id.widget_caption,R.id.widget_zone_local,R.id.widget_zone_london,R.id.widget_zone_new_york,R.id.widget_zone_tokyo};
            case "widget_meeting_window":return new int[]{R.id.widget_caption,R.id.widget_meeting_london,R.id.widget_meeting_new_york,R.id.widget_meeting_status};
            case "widget_week_strip":return new int[]{R.id.widget_caption,R.id.widget_week_day_0,R.id.widget_week_day_1,R.id.widget_week_day_2,R.id.widget_week_day_3,R.id.widget_week_day_4,R.id.widget_week_day_5,R.id.widget_week_day_6,R.id.widget_week_date_0,R.id.widget_week_date_1,R.id.widget_week_date_2,R.id.widget_week_date_3,R.id.widget_week_date_4,R.id.widget_week_date_5,R.id.widget_week_date_6};
            case "widget_month_grid": {
                int[] ids=new int[1+MONTH_WEEKDAY_IDS.length+MONTH_DAY_IDS.length+MONTH_COMPACT_WEEKDAY_IDS.length+MONTH_COMPACT_DAY_IDS.length];ids[0]=R.id.widget_caption;
                int next=1;System.arraycopy(MONTH_WEEKDAY_IDS,0,ids,next,MONTH_WEEKDAY_IDS.length);next+=MONTH_WEEKDAY_IDS.length;
                System.arraycopy(MONTH_DAY_IDS,0,ids,next,MONTH_DAY_IDS.length);next+=MONTH_DAY_IDS.length;
                System.arraycopy(MONTH_COMPACT_WEEKDAY_IDS,0,ids,next,MONTH_COMPACT_WEEKDAY_IDS.length);next+=MONTH_COMPACT_WEEKDAY_IDS.length;
                System.arraycopy(MONTH_COMPACT_DAY_IDS,0,ids,next,MONTH_COMPACT_DAY_IDS.length);
                int[] all=new int[ids.length+1];all[0]=R.id.widget_month_title;System.arraycopy(ids,0,all,1,ids.length);return all;
            }
            case "widget_year_pulse":return new int[]{R.id.widget_caption,R.id.widget_year_day,R.id.widget_year_remaining};
            case "widget_day_balance":return new int[]{R.id.widget_caption,R.id.widget_day_elapsed,R.id.widget_day_remaining};
            case "widget_routine_stack":return new int[]{R.id.widget_caption,R.id.widget_routine_row_0,R.id.widget_routine_row_1,R.id.widget_routine_row_2};
            case "widget_routine_countdown":return new int[]{R.id.widget_caption,R.id.widget_routine_name,R.id.widget_routine_countdown};
            case "widget_focus_launch":return new int[]{R.id.widget_caption,R.id.widget_focus_25,R.id.widget_focus_50,R.id.widget_focus_90};
            case "widget_timer_live":return new int[]{R.id.widget_caption,R.id.widget_timer_live_countdown,R.id.widget_timer_live_action};
            case "widget_lap_insights":return new int[]{R.id.widget_caption,R.id.widget_lap_last,R.id.widget_lap_best,R.id.widget_lap_average};
            default:return new int[0];
        }
    }

    private static int[] alignmentIds(String layout) {
        switch(layout) {
            case "widget_wake_switch":return new int[]{R.id.widget_caption,R.id.widget_wake_status};
            case "widget_quick_alarm":case "widget_focus_launch":case "widget_week_strip":
                return new int[]{R.id.widget_caption};
            case "widget_month_grid":return new int[]{R.id.widget_caption,R.id.widget_month_title};
            case "widget_timer_live":return new int[]{R.id.widget_caption,R.id.widget_timer_live_countdown};
            default:return textIds(layout);
        }
    }

    private static int baseTextSize(String layout,int id) {
        if(id==R.id.widget_caption)return 9;
        switch(layout) {
            case "widget_alarm_countdown":return 26;
            case "widget_alarm_stack":return id==R.id.widget_alarm_row_0?15:12;
            case "widget_wake_switch":return id==R.id.widget_wake_status?19:10;
            case "widget_quick_alarm":case "widget_focus_launch":return 10;
            case "widget_world_grid":return id==R.id.widget_zone_local?18:12;
            case "widget_meeting_window":return id==R.id.widget_meeting_status?11:17;
            case "widget_week_strip":return contains(WEEK_DATE_IDS,id)?12:8;
            case "widget_month_grid":return id==R.id.widget_month_title?13:(contains(MONTH_COMPACT_DAY_IDS,id)?12:(contains(MONTH_DAY_IDS,id)?9:8));
            case "widget_year_pulse":return id==R.id.widget_year_day?22:11;
            case "widget_day_balance":return 14;
            case "widget_routine_stack":return id==R.id.widget_routine_row_0?14:11;
            case "widget_routine_countdown":return id==R.id.widget_routine_countdown?23:(id==R.id.widget_routine_name?15:11);
            case "widget_timer_live":return id==R.id.widget_timer_live_countdown?27:10;
            case "widget_lap_insights":return id==R.id.widget_lap_last?14:12;
            default:return 11;
        }
    }

    private static boolean contains(int[] values,int value){for(int candidate:values)if(candidate==value)return true;return false;}

    private static void alarmStack(Context context,RemoteViews views) {
        List<Occurrence> alarms=occurrences(context,"alarms",true);
        int[] rowIds={R.id.widget_alarm_row_0,R.id.widget_alarm_row_1,R.id.widget_alarm_row_2};
        for(int i=0;i<rowIds.length;i++) {
            String line=i<alarms.size()?alarmLabel(context,alarms.get(i)):(i==0?"No enabled alarms":"");
            views.setTextViewText(rowIds[i],line);
        }
    }

    private static void wakeSwitch(Context context,RemoteViews views,int widgetId,boolean interactive) {
        Occurrence next=nextAlarm(context,false);
        if(next==null) {
            views.setTextViewText(R.id.widget_wake_status,"No saved alarm");
            views.setTextViewText(R.id.widget_wake_switch,"OPEN ALARMS");
            views.setContentDescription(R.id.widget_wake_switch,"No saved alarm. Open MySpace alarms.");
            return;
        }
        long alarmId=next.item.optLong("id",-1);
        boolean enabled=next.item.optBoolean("enabled",true);
        String time=DataStore.formatTime(context,next.item.optInt("hour",7),next.item.optInt("minute",0));
        String label=next.item.optString("label","Alarm");
        views.setTextViewText(R.id.widget_wake_status,time+" · "+label);
        views.setTextViewText(R.id.widget_wake_switch,enabled?"ALARM ON · TAP TO PAUSE":"ALARM OFF · TAP TO ENABLE");
        views.setContentDescription(R.id.widget_wake_switch,(enabled?"Disable ":"Enable ")+label+" at "+time);
        if(interactive&&alarmId>=0) {
            Intent intent=new Intent(context,WidgetActionReceiver.class)
                    .setAction(WidgetActionReceiver.ACTION_TOGGLE_ALARM)
                    .setData(Uri.parse("tictic://widget/wake/"+widgetId+"/"+alarmId))
                    .putExtra(WidgetActionReceiver.EXTRA_ALARM_ID,alarmId);
            views.setOnClickPendingIntent(R.id.widget_wake_switch,broadcast(context,intent));
        }
    }

    private static void quickAlarmButtons(Context context,RemoteViews views,int widgetId,boolean interactive) {
        int[] viewIds={R.id.widget_quick_alarm_10,R.id.widget_quick_alarm_30,R.id.widget_quick_alarm_60};
        int[] minutes={10,30,60};
        for(int i=0;i<viewIds.length;i++) {
            int mins=minutes[i];
            views.setTextViewText(viewIds[i],mins+" MIN");
            views.setContentDescription(viewIds[i],"Create an alarm in "+mins+" minutes");
            if(interactive) {
                Intent intent=new Intent(context,MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setData(Uri.parse("tictic://widget/quick-alarm/"+widgetId+"/"+mins))
                        .putExtra("tab","alarms")
                        .putExtra("tictic_widget_id",widgetId)
                        .putExtra("tictic_widget_design","Quick Alarm")
                        .putExtra(MainActivity.EXTRA_WIDGET_QUICK_ALARM_MINUTES,mins);
                views.setOnClickPendingIntent(viewIds[i],activity(context,intent,450000+widgetId*3+i));
            }
        }
    }

    private static void worldGrid(RemoteViews views) {
        views.setString(R.id.widget_zone_local,"setTimeZone",TimeZone.getDefault().getID());
        views.setString(R.id.widget_zone_london,"setTimeZone","Europe/London");
        views.setString(R.id.widget_zone_new_york,"setTimeZone","America/New_York");
        views.setString(R.id.widget_zone_tokyo,"setTimeZone","Asia/Tokyo");
    }

    private static void meetingWindow(Context context,RemoteViews views) {
        Calendar london=Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        Calendar newYork=Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        boolean londonWork=withinWorkWindow(london),newYorkWork=withinWorkWindow(newYork);
        String status=londonWork&&newYorkWork?"Inside work hours in both cities"
                :londonWork?"London work hours · New York is outside"
                :newYorkWork?"New York work hours · London is outside"
                :"Outside work hours in both cities";
        views.setTextViewText(R.id.widget_meeting_status,status);
    }

    private static boolean withinWorkWindow(Calendar local) {
        int day=local.get(Calendar.DAY_OF_WEEK),hour=local.get(Calendar.HOUR_OF_DAY);
        return day!=Calendar.SATURDAY&&day!=Calendar.SUNDAY&&hour>=9&&hour<17;
    }

    private static void weekStrip(Context context,RemoteViews views) {
        Calendar cursor=Calendar.getInstance();
        Calendar now=(Calendar)cursor.clone();
        cursor.set(Calendar.HOUR_OF_DAY,0);cursor.set(Calendar.MINUTE,0);cursor.set(Calendar.SECOND,0);cursor.set(Calendar.MILLISECOND,0);
        int offset=(cursor.get(Calendar.DAY_OF_WEEK)-cursor.getFirstDayOfWeek()+7)%7;
        cursor.add(Calendar.DAY_OF_YEAR,-offset);
        SimpleDateFormat dayFormat=new SimpleDateFormat("EEE",Locale.getDefault());
        for(int i=0;i<7;i++) {
            String day=dayFormat.format(cursor.getTime());
            views.setTextViewText(WEEK_DAY_IDS[i],day);
            views.setTextViewText(WEEK_DATE_IDS[i],String.valueOf(cursor.get(Calendar.DAY_OF_MONTH)));
            boolean today=isSameLocalDay(cursor,now);
            views.setInt(WEEK_CELL_IDS[i],"setBackgroundColor",today?DataStore.accent(context):Color.TRANSPARENT);
            views.setTextColor(WEEK_DAY_IDS[i],today?Color.WHITE:0xff65756e);
            views.setTextColor(WEEK_DATE_IDS[i],today?Color.WHITE:0xff183a34);
            cursor.add(Calendar.DAY_OF_YEAR,1);
        }
    }

    private static void monthGrid(Context context,RemoteViews views) {
        Calendar now=Calendar.getInstance();
        views.setTextViewText(R.id.widget_month_title,
                new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(now.getTime()).toUpperCase(Locale.getDefault()));
        Calendar start=(Calendar)now.clone();
        start.set(Calendar.DAY_OF_MONTH,1);start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);
        start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
        int offset=(start.get(Calendar.DAY_OF_WEEK)-start.getFirstDayOfWeek()+7)%7;
        start.add(Calendar.DAY_OF_YEAR,-offset);
        SimpleDateFormat dayFormat=new SimpleDateFormat("EEE",Locale.getDefault());
        Calendar weekday=(Calendar)start.clone();
        for(int i=0;i<7;i++) {
            views.setTextViewText(MONTH_WEEKDAY_IDS[i],firstCodePoint(dayFormat.format(weekday.getTime())));
            weekday.add(Calendar.DAY_OF_YEAR,1);
        }
        Calendar cursor=(Calendar)start.clone();
        for(int i=0;i<MONTH_DAY_IDS.length;i++) {
            boolean sameMonth=cursor.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&cursor.get(Calendar.MONTH)==now.get(Calendar.MONTH);
            boolean today=sameMonth&&cursor.get(Calendar.DAY_OF_MONTH)==now.get(Calendar.DAY_OF_MONTH);
            views.setTextViewText(MONTH_DAY_IDS[i],String.valueOf(cursor.get(Calendar.DAY_OF_MONTH)));
            views.setTextColor(MONTH_DAY_IDS[i],today?Color.WHITE:(sameMonth?0xff574b39:0xff9caaa4));
            views.setInt(MONTH_DAY_IDS[i],"setBackgroundColor",today?DataStore.accent(context):Color.TRANSPARENT);
            cursor.add(Calendar.DAY_OF_YEAR,1);
        }
        Calendar compactStart=(Calendar)now.clone();
        compactStart.set(Calendar.HOUR_OF_DAY,0);compactStart.set(Calendar.MINUTE,0);
        compactStart.set(Calendar.SECOND,0);compactStart.set(Calendar.MILLISECOND,0);
        compactStart.add(Calendar.DAY_OF_YEAR,-(compactStart.get(Calendar.DAY_OF_WEEK)-compactStart.getFirstDayOfWeek()+7)%7);
        for(int i=0;i<7;i++) {
            views.setTextViewText(MONTH_COMPACT_WEEKDAY_IDS[i],firstCodePoint(dayFormat.format(compactStart.getTime())));
            views.setTextViewText(MONTH_COMPACT_DAY_IDS[i],String.valueOf(compactStart.get(Calendar.DAY_OF_MONTH)));
            boolean today=isSameLocalDay(compactStart,now);
            views.setTextColor(MONTH_COMPACT_DAY_IDS[i],today?Color.WHITE:0xff574b39);
            views.setInt(MONTH_COMPACT_DAY_IDS[i],"setBackgroundColor",today?DataStore.accent(context):Color.TRANSPARENT);
            compactStart.add(Calendar.DAY_OF_YEAR,1);
        }
    }

    private static String firstCodePoint(String value) {
        if(value==null||value.isEmpty())return "";
        int end=value.offsetByCodePoints(0,1);
        return value.substring(0,end).toUpperCase(Locale.getDefault());
    }

    private static boolean isSameLocalDay(Calendar one,Calendar two) {
        return one.get(Calendar.ERA)==two.get(Calendar.ERA)&&one.get(Calendar.YEAR)==two.get(Calendar.YEAR)
                &&one.get(Calendar.DAY_OF_YEAR)==two.get(Calendar.DAY_OF_YEAR);
    }

    private static int mutedInk(int color) {
        return Color.argb(Math.min(Color.alpha(color),0x99),Color.red(color),Color.green(color),Color.blue(color));
    }

    private static void yearPulse(RemoteViews views) {
        Calendar now=Calendar.getInstance();
        int day=now.get(Calendar.DAY_OF_YEAR),total=now.getActualMaximum(Calendar.DAY_OF_YEAR),left=total-day;
        views.setTextViewText(R.id.widget_year_day,String.format(Locale.getDefault(),"DAY %03d / %d",day,total));
        views.setProgressBar(R.id.widget_year_progress,100,Math.min(100,day*100/total),false);
        views.setTextViewText(R.id.widget_year_remaining,left==0?"Last day of the year":left+" "+(left==1?"day":"days")+" remaining");
    }

    private static void dayBalance(RemoteViews views) {
        Calendar now=Calendar.getInstance();
        Calendar start=(Calendar)now.clone();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);
        start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
        Calendar end=(Calendar)start.clone();end.add(Calendar.DAY_OF_YEAR,1);
        long dayMillis=end.getTimeInMillis()-start.getTimeInMillis();
        long elapsed=Math.max(0,now.getTimeInMillis()-start.getTimeInMillis());
        long remaining=Math.max(0,end.getTimeInMillis()-now.getTimeInMillis());
        views.setTextViewText(R.id.widget_day_elapsed,hoursMinutes(elapsed)+" elapsed");
        views.setTextViewText(R.id.widget_day_remaining,hoursMinutes(remaining)+" left");
        views.setProgressBar(R.id.widget_day_progress,100,dayMillis<=0?0:(int)Math.min(100,elapsed*100/dayMillis),false);
    }

    private static String hoursMinutes(long millis) {
        long minutes=Math.max(0,millis)/60000L;
        return (minutes/60)+"h "+String.format(Locale.getDefault(),"%02d",minutes%60)+"m";
    }

    private static void routineStack(Context context,RemoteViews views) {
        List<Occurrence> routines=occurrences(context,"routines",true);
        int[] rowIds={R.id.widget_routine_row_0,R.id.widget_routine_row_1,R.id.widget_routine_row_2};
        for(int i=0;i<rowIds.length;i++) {
            String line=i<routines.size()?routineLabel(context,routines.get(i)):(i==0?"No routine scheduled":"");
            views.setTextViewText(rowIds[i],line);
        }
    }

    private static void routineCountdown(Context context,RemoteViews views) {
        Occurrence next=nextRoutine(context);
        if(next==null) {
            views.setTextViewText(R.id.widget_routine_name,"No routine scheduled");
            stopChronometer(views,R.id.widget_routine_countdown,"—");
            return;
        }
        String name=next.item.optString("name","Routine");
        String time=DataStore.formatTime(context,next.item.optInt("hour",21),next.item.optInt("minute",0));
        views.setTextViewText(R.id.widget_routine_name,name+" · "+time+" · "+DataStore.formatDate(next.at));
        countdown(context,views,R.id.widget_routine_countdown,next,"No routine scheduled");
    }

    private static void focusButtons(Context context,RemoteViews views,int widgetId,boolean interactive) {
        int[] viewIds={R.id.widget_focus_25,R.id.widget_focus_50,R.id.widget_focus_90};
        int[] minutes={25,50,90};
        for(int i=0;i<viewIds.length;i++) {
            int mins=minutes[i];
            views.setContentDescription(viewIds[i],"Start a "+mins+" minute focus timer");
            if(interactive) {
                Intent intent=new Intent(context,WidgetActionReceiver.class)
                        .setAction(WidgetActionReceiver.ACTION_START_FOCUS)
                        .setData(Uri.parse("tictic://widget/focus/"+widgetId+"/"+mins))
                        .putExtra(WidgetActionReceiver.EXTRA_MINUTES,mins);
                views.setOnClickPendingIntent(viewIds[i],broadcast(context,intent));
            }
        }
    }

    private static void timerLive(Context context,RemoteViews views,int widgetId,boolean interactive) {
        boolean running=TimerReceiver.isRunning(context);
        long remaining=TimerReceiver.remaining(context);
        android.content.SharedPreferences prefs=DataStore.prefs(context);
        long duration=prefs.getLong("timer_duration",prefs.getLong("timer_default_duration",300000L));
        duration=Math.max(1L,duration);
        if(running)timerCountdown(views,R.id.widget_timer_live_countdown,remaining);
        else stopChronometer(views,R.id.widget_timer_live_countdown,formatDuration(remaining));
        int progress=(int)Math.max(0,Math.min(100,(duration-remaining)*100/duration));
        views.setProgressBar(R.id.widget_timer_live_progress,100,progress,false);
        views.setTextViewText(R.id.widget_timer_live_action,running?"PAUSE TIMER":"START / RESUME");
        views.setContentDescription(R.id.widget_timer_live_action,running?"Pause the active timer":"Start or resume the timer");
        if(interactive) {
            Intent intent=new Intent(context,WidgetActionReceiver.class)
                    .setAction(WidgetActionReceiver.ACTION_TOGGLE_TIMER)
                    .setData(Uri.parse("tictic://widget/timer/"+widgetId))
                    .putExtra(WidgetActionReceiver.EXTRA_WIDGET_ID,widgetId);
            views.setOnClickPendingIntent(R.id.widget_timer_live_action,broadcast(context,intent));
        }
    }

    private static void lapInsights(Context context,RemoteViews views) {
        JSONArray laps=DataStore.array(context,"laps");
        if(laps.length()==0) {
            views.setTextViewText(R.id.widget_lap_last,"Latest · no laps yet");
            views.setTextViewText(R.id.widget_lap_best,"Fastest · —");
            views.setTextViewText(R.id.widget_lap_average,"Average · —");
            return;
        }
        long last=0,best=Long.MAX_VALUE,total=0;int count=0;
        for(int i=0;i<laps.length();i++) {
            JSONObject lap=laps.optJSONObject(i);if(lap==null)continue;
            long split=lap.optLong("split",0);if(split<=0)continue;
            last=split;best=Math.min(best,split);total+=split;count++;
        }
        views.setTextViewText(R.id.widget_lap_last,"Latest · "+(last>0?TimeMath.formatHundredths(last):"—"));
        views.setTextViewText(R.id.widget_lap_best,"Fastest · "+(best<Long.MAX_VALUE?TimeMath.formatHundredths(best):"—"));
        views.setTextViewText(R.id.widget_lap_average,"Average · "+(count>0?TimeMath.formatHundredths(total/count):"—"));
    }

    private static void countdown(Context context,RemoteViews views,int viewId,Occurrence occurrence,String empty) {
        if(occurrence==null) {stopChronometer(views,viewId,empty);return;}
        long remaining=Math.max(0,occurrence.at-System.currentTimeMillis());
        if(Build.VERSION.SDK_INT>=24) {
            views.setChronometer(viewId,SystemClock.elapsedRealtime()+remaining,"in %s",true);
            views.setChronometerCountDown(viewId,true);
        } else {
            views.setChronometer(viewId,SystemClock.elapsedRealtime(),"in %s",false);
            views.setTextViewText(viewId,"in "+formatDuration(remaining));
        }
    }

    private static void timerCountdown(RemoteViews views,int viewId,long remaining) {
        if(Build.VERSION.SDK_INT>=24) {
            views.setChronometer(viewId,SystemClock.elapsedRealtime()+Math.max(0,remaining),"%s",true);
            views.setChronometerCountDown(viewId,true);
        } else {
            views.setChronometer(viewId,SystemClock.elapsedRealtime(),"%s",false);
            views.setTextViewText(viewId,formatDuration(remaining));
        }
    }

    private static void stopChronometer(RemoteViews views,int viewId,String text) {
        views.setChronometer(viewId,SystemClock.elapsedRealtime(),"%s",false);
        if(Build.VERSION.SDK_INT>=24)views.setChronometerCountDown(viewId,false);
        views.setTextViewText(viewId,text);
    }

    private static String formatDuration(long millis) {
        long totalSeconds=Math.max(0,(millis+999)/1000);
        long hours=totalSeconds/3600,minutes=(totalSeconds%3600)/60,seconds=totalSeconds%60;
        return hours>0?String.format(Locale.getDefault(),"%d:%02d:%02d",hours,minutes,seconds)
                :String.format(Locale.getDefault(),"%d:%02d",minutes,seconds);
    }

    private static PendingIntent broadcast(Context context,Intent intent) {
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context,0,intent,flags);
    }

    private static PendingIntent activity(Context context,Intent intent,int requestCode) {
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(context,requestCode,intent,flags);
    }

    private static Occurrence nextAlarm(Context context,boolean enabledOnly) {
        List<Occurrence> alarms=occurrences(context,"alarms",enabledOnly);
        return alarms.isEmpty()?null:alarms.get(0);
    }

    private static Occurrence nextRoutine(Context context) {
        List<Occurrence> routines=occurrences(context,"routines",true);
        return routines.isEmpty()?null:routines.get(0);
    }

    private static List<Occurrence> occurrences(Context context,String key,boolean enabledOnly) {
        JSONArray saved=DataStore.array(context,key);List<Occurrence> result=new ArrayList<>();
        long now=System.currentTimeMillis();
        for(int i=0;i<saved.length();i++) {
            JSONObject item=saved.optJSONObject(i);if(item==null)continue;
            if(enabledOnly&&!item.optBoolean("enabled",true))continue;
            int maximum=item.optInt("days",0)==0?1:3;
            long reference=now;
            for(int n=0;n<maximum;n++) {
                long at=DataStore.nextOccurrence(item,reference);
                if(at<=reference)break;
                result.add(new Occurrence(item,at));
                reference=at;
            }
        }
        Collections.sort(result,new Comparator<Occurrence>() {
            @Override public int compare(Occurrence one,Occurrence two){return Long.compare(one.at,two.at);}
        });
        return result;
    }

    private static String alarmLabel(Context context,Occurrence occurrence) {
        return DataStore.formatDate(occurrence.at)+" · "+DataStore.formatTime(context,
                occurrence.item.optInt("hour",7),occurrence.item.optInt("minute",0))+" · "+
                occurrence.item.optString("label","Alarm");
    }

    private static String routineLabel(Context context,Occurrence occurrence) {
        return DataStore.formatDate(occurrence.at)+" · "+DataStore.formatTime(context,
                occurrence.item.optInt("hour",21),occurrence.item.optInt("minute",0))+" · "+
                occurrence.item.optString("name","Routine");
    }

    private static void applyCalendarAccents(Context context,RemoteViews views,String layout,
                                             boolean customInk,int ink) {
        if("widget_week_strip".equals(layout)) {
            Calendar now=Calendar.getInstance();
            Calendar cursor=(Calendar)now.clone();
            cursor.set(Calendar.HOUR_OF_DAY,0);cursor.set(Calendar.MINUTE,0);cursor.set(Calendar.SECOND,0);cursor.set(Calendar.MILLISECOND,0);
            int offset=(cursor.get(Calendar.DAY_OF_WEEK)-cursor.getFirstDayOfWeek()+7)%7;
            cursor.add(Calendar.DAY_OF_YEAR,-offset);
            for(int i=0;i<7;i++) {
                boolean today=isSameLocalDay(cursor,now);
                int dayColor=customInk?ink:0xff65756e;
                int dateColor=customInk?ink:0xff183a34;
                views.setInt(WEEK_CELL_IDS[i],"setBackgroundColor",today?DataStore.accent(context):Color.TRANSPARENT);
                views.setTextColor(WEEK_DAY_IDS[i],today?Color.WHITE:dayColor);
                views.setTextColor(WEEK_DATE_IDS[i],today?Color.WHITE:dateColor);
                cursor.add(Calendar.DAY_OF_YEAR,1);
            }
        } else if("widget_month_grid".equals(layout)) {
            Calendar now=Calendar.getInstance();
            Calendar cursor=(Calendar)now.clone();cursor.set(Calendar.DAY_OF_MONTH,1);cursor.set(Calendar.HOUR_OF_DAY,0);
            cursor.set(Calendar.MINUTE,0);cursor.set(Calendar.SECOND,0);cursor.set(Calendar.MILLISECOND,0);
            int offset=(cursor.get(Calendar.DAY_OF_WEEK)-cursor.getFirstDayOfWeek()+7)%7;
            cursor.add(Calendar.DAY_OF_YEAR,-offset);
            int currentMonthInk=customInk?ink:0xff574b39;
            int adjacentMonthInk=customInk?mutedInk(ink):0xff9caaa4;
            for(int i=0;i<MONTH_DAY_IDS.length;i++) {
                boolean sameMonth=cursor.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&cursor.get(Calendar.MONTH)==now.get(Calendar.MONTH);
                boolean today=sameMonth&&cursor.get(Calendar.DAY_OF_MONTH)==now.get(Calendar.DAY_OF_MONTH);
                views.setInt(MONTH_DAY_IDS[i],"setBackgroundColor",today?DataStore.accent(context):Color.TRANSPARENT);
                views.setTextColor(MONTH_DAY_IDS[i],today?Color.WHITE:(sameMonth?currentMonthInk:adjacentMonthInk));
                cursor.add(Calendar.DAY_OF_YEAR,1);
            }
            Calendar compact=(Calendar)now.clone();compact.set(Calendar.HOUR_OF_DAY,0);compact.set(Calendar.MINUTE,0);
            compact.set(Calendar.SECOND,0);compact.set(Calendar.MILLISECOND,0);
            compact.add(Calendar.DAY_OF_YEAR,-(compact.get(Calendar.DAY_OF_WEEK)-compact.getFirstDayOfWeek()+7)%7);
            for(int i=0;i<MONTH_COMPACT_DAY_IDS.length;i++) {
                boolean today=isSameLocalDay(compact,now);
                views.setInt(MONTH_COMPACT_DAY_IDS[i],"setBackgroundColor",today?DataStore.accent(context):Color.TRANSPARENT);
                views.setTextColor(MONTH_COMPACT_DAY_IDS[i],today?Color.WHITE:currentMonthInk);
                compact.add(Calendar.DAY_OF_YEAR,1);
            }
        }
    }
}
