package app.tictic.clock;

import android.app.PendingIntent;
import app.space.core.platform.PowerState;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import app.space.core.data.UsageTracker;
import app.space.core.design.GlassPreset;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Shared, battery-conscious RemoteViews renderer for 24 registered widget providers; the picker curates 20. */
final class WidgetEngine {
    private static final int[] TIME_IDS={R.id.widget_time,R.id.widget_time_light,R.id.widget_time_serif,R.id.widget_time_mono,R.id.widget_time_black,R.id.widget_time_condensed,R.id.widget_time_rounded,R.id.widget_time_casual};
    private static final int[] TEXT_IDS={R.id.widget_time,R.id.widget_time_light,R.id.widget_time_serif,R.id.widget_time_mono,R.id.widget_time_black,R.id.widget_time_condensed,R.id.widget_time_rounded,R.id.widget_time_casual,R.id.widget_date,R.id.widget_alarm,R.id.widget_secondary,R.id.widget_caption};
    private static final String[] DATE_PATTERNS={"EEE, d MMM","EEEE, d MMMM","d MMM yyyy","EEE · d/M/yy","EEEE"};
    private WidgetEngine(){}

    static void update(Context c,int widgetId,int layout,String route){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);RemoteViews v=new RemoteViews(c.getPackageName(),layout);String name=c.getResources().getResourceEntryName(layout);
        String key="widget_"+widgetId+"_";android.content.SharedPreferences prefs=DataStore.prefs(c);
        Bundle options=manager.getAppWidgetOptions(widgetId);int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,220);int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,110);
        boolean compact=width<155||height<84;boolean expanded=width>=180&&height>=125;
        boolean weather=weatherSupported(name)&&prefs.getBoolean(key+"weather_enabled",false);
        boolean showDate=!compact&&height>=100&&supportsDate(name)&&prefs.getBoolean(key+"show_date",defaultShowDate(name));boolean showAlarm=expanded&&supportsAlarm(name)&&prefs.getBoolean(key+"show_alarm",defaultShowAlarm(name));boolean showCaption=supportsCaption(name)&&prefs.getBoolean(key+"show_caption",defaultShowCaption(name));boolean showContext=expanded&&(weather||supportsContext(name)&&prefs.getBoolean(key+"show_context",defaultShowContext(name)));
        int dateStyle=Math.max(0,Math.min(DATE_PATTERNS.length-1,prefs.getInt(key+"date_format",defaultDateFormat(name))));String datePattern=DATE_PATTERNS[dateStyle];
        v.setCharSequence(R.id.widget_date,"setFormat12Hour",datePattern);v.setCharSequence(R.id.widget_date,"setFormat24Hour",datePattern);
        v.setTextViewText(R.id.widget_alarm,nextAlarmDetail(c,prefs.getInt(key+"alarm_detail",defaultAlarmDetail(name))));
        if(weather){String cached=prefs.getString(key+"weather_summary","");v.setTextViewText(R.id.widget_secondary,cached.isEmpty()?"Weather · waiting for update":cached+" · Open-Meteo");}
        else if(!name.equals("widget_world"))v.setTextViewText(R.id.widget_secondary,secondary(c,name));
        v.setViewVisibility(R.id.widget_date,showDate?View.VISIBLE:View.GONE);v.setViewVisibility(R.id.widget_alarm,showAlarm?View.VISIBLE:View.GONE);
        v.setViewVisibility(R.id.widget_secondary,showContext?View.VISIBLE:View.GONE);v.setViewVisibility(R.id.widget_caption,showCaption?View.VISIBLE:View.GONE);if(name.equals("widget_clock"))v.setTextViewText(R.id.widget_caption,"TICTIC");
        boolean progress=name.equals("widget_timeline")||name.equals("widget_orbit")||name.equals("widget_halo");if(progress){v.setProgressBar(R.id.widget_progress,100,dayProgress(),false);v.setViewVisibility(R.id.widget_progress,expanded?View.VISIBLE:View.GONE);}else v.setViewVisibility(R.id.widget_progress,View.GONE);
        applyPerWidgetStyle(c,v,widgetId,name,null,width,height);
        v.setOnClickPendingIntent(R.id.widget_root,widgetIntent(c,widgetId,name,route,420000+widgetId));
        if(name.equals("widget_command")){v.setOnClickPendingIntent(R.id.widget_action_timer,widgetIntent(c,widgetId,name,"timer",430000+widgetId*2));v.setOnClickPendingIntent(R.id.widget_action_alarm,widgetIntent(c,widgetId,name,"alarms",430001+widgetId*2));}
        UsageTracker.recordWidgetRefresh(c);manager.updateAppWidget(widgetId,v);
    }
    private static PendingIntent widgetIntent(Context c,int widgetId,String name,String route,int requestCode){Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);if(!"clock".equals(route))open.putExtra("tab",route);open.putExtra("tictic_widget_id",widgetId).putExtra("tictic_widget_design",designLabel(name));int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;return PendingIntent.getActivity(c,requestCode,open,flags);}
    static final class PreviewStyle {
        int textColor,background,opacity,font,size,dateFormat,alarmDetail,alignment,panelTreatment,layoutDensity,layoutPriority,glassFinish,glassPreset;
        boolean transparent,showDate,showAlarm,showCaption,showContext,wallpaperColors,weatherEnabled;
    }

    static RemoteViews previewViews(Context c,int widgetId,int layout,PreviewStyle style){
        Bundle options=AppWidgetManager.getInstance(c).getAppWidgetOptions(widgetId);
        AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(widgetId);
        int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,info==null?220:info.minWidth);
        int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,info==null?110:info.minHeight);
        return previewViews(c,widgetId,layout,style,width,height);
    }
    static RemoteViews previewViews(Context c,int widgetId,int layout,PreviewStyle style,int width,int height){
        String name=c.getResources().getResourceEntryName(layout);
        RemoteViews v=new RemoteViews(c.getPackageName(),layout);
        android.content.SharedPreferences prefs=DataStore.prefs(c);
        boolean compact=width<155||height<84,expanded=width>=180&&height>=125;
        boolean showDate=!compact&&height>=100&&style.showDate&&supportsDate(name);
        boolean showAlarm=expanded&&style.showAlarm&&supportsAlarm(name);
        boolean showContext=expanded&&(style.weatherEnabled||style.showContext&&supportsContext(name));
        v.setCharSequence(R.id.widget_date,"setFormat12Hour",DATE_PATTERNS[Math.max(0,Math.min(DATE_PATTERNS.length-1,style.dateFormat))]);
        v.setCharSequence(R.id.widget_date,"setFormat24Hour",DATE_PATTERNS[Math.max(0,Math.min(DATE_PATTERNS.length-1,style.dateFormat))]);
        v.setTextViewText(R.id.widget_alarm,nextAlarmDetail(c,style.alarmDetail));
        if(style.weatherEnabled){String cached=prefs.getString("widget_"+widgetId+"_weather_summary","");v.setTextViewText(R.id.widget_secondary,cached.isEmpty()?"Weather · waiting for update":cached+" · Open-Meteo");}
        else if(!name.equals("widget_world"))v.setTextViewText(R.id.widget_secondary,secondary(c,name));
        v.setViewVisibility(R.id.widget_date,showDate?View.VISIBLE:View.GONE);
        v.setViewVisibility(R.id.widget_alarm,showAlarm?View.VISIBLE:View.GONE);
        v.setViewVisibility(R.id.widget_secondary,showContext?View.VISIBLE:View.GONE);
        v.setViewVisibility(R.id.widget_caption,style.showCaption&&supportsCaption(name)?View.VISIBLE:View.GONE);
        if(name.equals("widget_clock"))v.setTextViewText(R.id.widget_caption,"TICTIC");
        boolean progress=name.equals("widget_timeline")||name.equals("widget_orbit")||name.equals("widget_halo");
        if(progress){v.setProgressBar(R.id.widget_progress,100,dayProgress(),false);v.setViewVisibility(R.id.widget_progress,expanded?View.VISIBLE:View.GONE);}else v.setViewVisibility(R.id.widget_progress,View.GONE);
        applyPerWidgetStyle(c,v,widgetId,name,style,width,height);
        return v;
    }

    private static void applyPerWidgetStyle(Context c,RemoteViews v,int id,String layout,PreviewStyle preview,int width,int height){
        String p="widget_"+id+"_";android.content.SharedPreferences prefs=DataStore.prefs(c);
        boolean configured=preview!=null||prefs.getBoolean(p+"configured",false);
        boolean dark=layout.equals("widget_bedside")||layout.equals("widget_mono")||layout.equals("widget_aurora")||layout.equals("widget_prism")||layout.equals("widget_flip")||layout.equals("widget_command")||layout.equals("widget_halo");
        int color=layout.equals("widget_prism")?0xff39e6e8:layout.equals("widget_halo")?0xffe8fcff:layout.equals("widget_clock")?0xfff7f2ff:(dark?Color.WHITE:0xff183a34);
        if(configured)color=preview!=null?preview.textColor:prefs.getInt(p+"text_color",color);
        boolean wallpaperPalette=preview!=null?preview.wallpaperColors:prefs.getBoolean(p+"wallpaper_colors",false);
        if(wallpaperPalette)color=previewWallpaperPalette(c,defaultBackgroundName(layout))[0];
        float defaultSize=layout.equals("widget_clock")?56f:layout.equals("widget_prism")?54f:layout.equals("widget_flip")?48f:layout.equals("widget_command")?42f:layout.equals("widget_halo")?46f:layout.equals("widget_bedside")?44f:layout.equals("widget_focus")?46f:40f;
        float size=configured?(preview!=null?preview.size:prefs.getFloat(p+"size",defaultSize)):defaultSize;float fitScale=width<155?.68f:width<180?.84f:1f;if(height<90)fitScale=Math.min(fitScale,.72f);else if(height<110)fitScale=Math.min(fitScale,.86f);size*=fitScale;
        int font=configured?(preview!=null?preview.font:prefs.getInt(p+"font",defaultFont(layout))):defaultFont(layout);
        for(int view:TIME_IDS)v.setViewVisibility(view,View.GONE);
        v.setViewVisibility(TIME_IDS[Math.max(0,Math.min(TIME_IDS.length-1,font))],View.VISIBLE);
        boolean saver=powerSaverOptimization(c);boolean seconds=prefs.getBoolean("show_home_seconds",true)&&!saver;
        String twelve=seconds?"h:mm:ss a":"h:mm a",twentyFour=seconds?"HH:mm:ss":"HH:mm";
        for(int view:TIME_IDS){v.setCharSequence(view,"setFormat12Hour",twelve);v.setCharSequence(view,"setFormat24Hour",twentyFour);}
        int priority=configured?(preview!=null?preview.layoutPriority:prefs.getInt(p+"layout_priority",0)):0;
        float timeSize=size,dateSize=Math.max(11,size*.31f),alarmSize=Math.max(11,size*.32f),contextSize=Math.max(10,size*.27f);
        if(priority==1)timeSize=size*1.16f;else if(priority==2)dateSize=Math.max(17,size*.46f);else if(priority==3)alarmSize=Math.max(16,size*.42f);else if(priority==4)contextSize=Math.max(13,size*.36f);
        for(int view:TEXT_IDS){v.setTextColor(view,color);if(configured){if(isTimeId(view))v.setTextViewTextSize(view,TypedValue.COMPLEX_UNIT_SP,timeSize);else if(view==R.id.widget_date)v.setTextViewTextSize(view,TypedValue.COMPLEX_UNIT_SP,dateSize);else if(view==R.id.widget_alarm)v.setTextViewTextSize(view,TypedValue.COMPLEX_UNIT_SP,alarmSize);else if(view==R.id.widget_secondary||view==R.id.widget_caption)v.setTextViewTextSize(view,TypedValue.COMPLEX_UNIT_SP,contextSize);}}
        int alignment=configured?(preview!=null?preview.alignment:prefs.getInt(p+"alignment",-1)):-1;
        if(alignment>=0){int gravity=alignment==0?Gravity.CENTER:alignment==1?Gravity.START|Gravity.CENTER_VERTICAL:Gravity.END|Gravity.CENTER_VERTICAL;for(int view:TEXT_IDS)v.setInt(view,"setGravity",gravity);}
        int density=configured?(preview!=null?preview.layoutDensity:prefs.getInt(p+"layout_density",1)):1;
        int inset=density==0?4:density==2?16:10;v.setViewPadding(R.id.widget_content,dp(c,inset),dp(c,inset),dp(c,inset),dp(c,inset));int verticalGap=density==0?0:density==2?7:3;for(int view:TEXT_IDS)v.setViewPadding(view,0,dp(c,verticalGap),0,dp(c,verticalGap));
        if(configured){
            boolean transparent=preview!=null?preview.transparent:prefs.getBoolean(p+"transparent",false);
            int treatment=Math.max(0,Math.min(6,preview!=null?preview.panelTreatment:prefs.getInt(p+"panel_treatment",0)));
            int back=preview!=null?preview.background:prefs.getInt(p+"background",defaultBackgroundName(layout));
            if(wallpaperPalette)back=previewWallpaperPalette(c,defaultBackgroundName(layout))[1];
            int alpha=Math.max(0,Math.min(255,preview!=null?preview.opacity:prefs.getInt(p+"opacity",220)));
            int fillAlpha=(alpha*Color.alpha(back)+127)/255;
            boolean showFill=!transparent&&treatment<5&&fillAlpha>0;
            boolean edge=treatment==4||treatment==5;
            int fill=showFill?((fillAlpha<<24)|(back&0x00ffffff)):Color.TRANSPARENT;
            int glassFinish=Math.max(0,Math.min(100,preview!=null?preview.glassFinish:prefs.getInt(p+"glass_finish",45)));int glassPreset=preview!=null?preview.glassPreset:prefs.getInt(p+"glass_preset",0);if(glassPreset<0||glassPreset>=GlassPreset.NAMES.length)glassPreset=0;
            int shineAlpha=Math.round(glassFinish*.78f);
            v.setInt(R.id.widget_root,"setBackgroundColor",Color.TRANSPARENT);
            if(layout.equals("widget_prism")||layout.equals("widget_flip")||layout.equals("widget_command")||layout.equals("widget_halo")){int bezel=layout.equals("widget_prism")?R.drawable.widget_bezel_prism:layout.equals("widget_flip")?R.drawable.widget_bezel_flip:layout.equals("widget_command")?R.drawable.widget_bezel_command:R.drawable.widget_bezel_halo;v.setImageViewResource(R.id.widget_bezel,bezel);v.setViewVisibility(R.id.widget_bezel,treatment==0?View.VISIBLE:View.GONE);}
            v.setImageViewResource(R.id.widget_panel,panelDrawable(layout,treatment));v.setInt(R.id.widget_panel,"setColorFilter",fill);v.setViewVisibility(R.id.widget_panel,showFill?View.VISIBLE:View.GONE);
            v.setImageViewResource(R.id.widget_panel_gloss,R.drawable.widget_panel_gloss);v.setInt(R.id.widget_panel_gloss,"setColorFilter",(shineAlpha<<24)|(GlassPreset.glow(glassPreset)&0x00ffffff));v.setViewVisibility(R.id.widget_panel_gloss,treatment==4&&showFill&&shineAlpha>0?View.VISIBLE:View.GONE);
            v.setImageViewResource(R.id.widget_panel_edge,outlineDrawable(treatment));v.setInt(R.id.widget_panel_edge,"setColorFilter",edge?0x88ffffff:Color.TRANSPARENT);v.setViewVisibility(R.id.widget_panel_edge,edge?View.VISIBLE:View.GONE);
        }
    }
    private static int dp(Context c,int value){return (int)(value*c.getResources().getDisplayMetrics().density+.5f);}
private static boolean powerSaverOptimization(Context c){return PowerState.isOptimizing(c);}
    static void refreshForPowerSaverChange(Context c){android.content.SharedPreferences p=DataStore.prefs(c);android.os.PowerManager pm=(android.os.PowerManager)c.getSystemService(Context.POWER_SERVICE);boolean now=pm!=null&&pm.isPowerSaveMode();boolean known=p.getBoolean("power_mode_known",false),before=p.getBoolean("power_mode_last",false);if(!known||before!=now){p.edit().putBoolean("power_mode_known",true).putBoolean("power_mode_last",now).apply();refreshAll(c);}}
    private static int panelDrawable(String n,int treatment){if(treatment==1)return R.drawable.widget_panel_fill_round;if(treatment==2)return R.drawable.widget_panel_fill_square;if(treatment==3)return R.drawable.widget_panel_fill_pill;if(treatment==4||treatment==5)return R.drawable.widget_panel_fill_soft;switch(n){case "widget_clock":return R.drawable.widget_bg_widget_clock;case "widget_date":return R.drawable.widget_bg_widget_date;case "widget_alarm":return R.drawable.widget_bg_widget_alarm;case "widget_analog":return R.drawable.widget_bg_widget_analog;case "widget_world":return R.drawable.widget_bg_widget_world;case "widget_timeline":return R.drawable.widget_bg_widget_timeline;case "widget_focus":return R.drawable.widget_bg_widget_focus;case "widget_timer":return R.drawable.widget_bg_widget_timer;case "widget_stopwatch":return R.drawable.widget_bg_widget_stopwatch;case "widget_bedside":return R.drawable.widget_bg_widget_bedside;case "widget_mono":return R.drawable.widget_bg_widget_mono;case "widget_calendar":return R.drawable.widget_bg_widget_calendar;case "widget_coral":return R.drawable.widget_bg_widget_coral;case "widget_sky":return R.drawable.widget_bg_widget_sky;case "widget_lavender":return R.drawable.widget_bg_widget_lavender;case "widget_stacked":return R.drawable.widget_bg_widget_stacked;case "widget_orbit":return R.drawable.widget_bg_widget_orbit;case "widget_prism":return R.drawable.widget_bg_widget_prism;case "widget_flip":return R.drawable.widget_bg_widget_flip;case "widget_command":return R.drawable.widget_bg_widget_command;case "widget_halo":return R.drawable.widget_bg_widget_halo;case "widget_routine":return R.drawable.widget_bg_widget_routine;case "widget_aurora":return R.drawable.widget_bg_widget_aurora;default:return R.drawable.widget_bg_widget_quiet;}}
    private static int outlineDrawable(int treatment){switch(treatment){case 1:return R.drawable.widget_panel_outline_round;case 2:return R.drawable.widget_panel_outline_square;case 3:return R.drawable.widget_panel_outline_pill;default:return R.drawable.widget_panel_outline_soft;}}
    static boolean supportsDate(String n){return !(n.equals("widget_focus")||n.equals("widget_timer")||n.equals("widget_stopwatch")||n.equals("widget_mono")||n.equals("widget_sky")||n.equals("widget_bedside")||n.equals("widget_aurora")||n.equals("widget_command"));}
    static boolean supportsAlarm(String n){return defaultShowAlarm(n)||n.equals("widget_world")||n.equals("widget_orbit")||n.equals("widget_routine");}
    static boolean supportsCaption(String n){return !n.equals("widget_clock")&&!n.equals("widget_mono")&&!n.equals("widget_sky")&&!n.equals("widget_stopwatch")&&!n.equals("widget_timer");}
    static boolean supportsContext(String n){return !n.equals("widget_clock")&&!n.equals("widget_mono")&&!n.equals("widget_sky")&&!n.equals("widget_focus")&&!n.equals("widget_stopwatch")&&!n.equals("widget_timer");}
    static boolean weatherSupported(String n){return n.equals("widget_clock")||n.equals("widget_date")||n.equals("widget_alarm")||n.equals("widget_analog")||n.equals("widget_sky")||n.equals("widget_coral")||n.equals("widget_quiet")||n.equals("widget_aurora")||n.equals("widget_prism")||n.equals("widget_halo");}
    static int[] previewWallpaperPalette(Context c,int fallback){int fallbackInk=Color.WHITE,fallbackPanel=fallback; if(Build.VERSION.SDK_INT<27)return new int[]{fallbackInk,fallbackPanel};try{WallpaperColors colors=WallpaperManager.getInstance(c).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);if(colors==null||colors.getPrimaryColor()==null)return new int[]{fallbackInk,fallbackPanel};int primary=colors.getPrimaryColor().toArgb();int secondary=colors.getSecondaryColor()!=null?colors.getSecondaryColor().toArgb():primary;int luminance=(int)(.299*Color.red(secondary)+.587*Color.green(secondary)+.114*Color.blue(secondary));int text=luminance>155?0xff172c29:0xfff7f6ef;return new int[]{text,secondary};}catch(RuntimeException ignored){return new int[]{fallbackInk,fallbackPanel};}}
    static int[] previewWallpaperSceneColors(Context c){int[] sample={0xff193d42,0xff426f6b,0xffa2aaa0};if(Build.VERSION.SDK_INT<27)return sample;try{WallpaperColors colors=WallpaperManager.getInstance(c).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);if(colors==null||colors.getPrimaryColor()==null)return sample;int primary=colors.getPrimaryColor().toArgb();int secondary=colors.getSecondaryColor()!=null?colors.getSecondaryColor().toArgb():primary;int tertiary=colors.getTertiaryColor()!=null?colors.getTertiaryColor().toArgb():secondary;return new int[]{primary,secondary,tertiary};}catch(RuntimeException ignored){return sample;}}
    static String shortName(String layout){String s=layout.replace("widget_","").replace('_',' ');switch(s){case "date":return "Date";case "alarm":return "Alarm";case "analog":return "Analog";case "world":return "World";case "timeline":return "Day Arc";case "focus":return "Focus";case "timer":return "Timers";case "stopwatch":return "Stopwatch";case "bedside":return "Bedside";case "mono":return "Mono";case "calendar":return "Week";case "coral":return "Coral";case "sky":return "Sky";case "lavender":return "Lilac";case "stacked":return "Stamp";case "orbit":return "Orbit";case "routine":return "Routine";case "aurora":return "Aurora";case "quiet":return "Quiet";case "prism":return "Prism";case "flip":return "Flip";case "command":return "Console";case "halo":return "Halo";default:return "Clock";}}
    private static String designLabel(String n){String value=n.replace("widget_","").replace("_"," ");return value.length()==0?"Widget":value.substring(0,1).toUpperCase(Locale.ROOT)+value.substring(1);}
    private static boolean isTimeId(int id){for(int view:TIME_IDS)if(view==id)return true;return false;}
    static void refreshAll(Context c){refreshComponent(c,ClockWidgetProvider.class,R.layout.widget_clock,"clock");refreshComponent(c,DateWidgetProvider.class,R.layout.widget_date,"clock");refreshComponent(c,NextAlarmWidgetProvider.class,R.layout.widget_alarm,"alarms");for(Class<?> cl:WidgetVariants.extras())refreshClass(c,cl);}
    private static void refreshComponent(Context c,Class<?> cl,int layout,String route){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,cl)))update(c,id,layout,route);}
    private static void refreshClass(Context c,Class<?> cl){try{WidgetVariants.Base base=(WidgetVariants.Base)cl.newInstance();refreshComponent(c,cl,base.layoutId(),base.route());}catch(Exception ignored){}}
    static void refreshWidget(Context c,int widgetId){try{AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(widgetId);if(info==null)return;String cn=info.provider.getClassName();if(cn.equals(ClockWidgetProvider.class.getName()))update(c,widgetId,R.layout.widget_clock,"clock");else if(cn.equals(DateWidgetProvider.class.getName()))update(c,widgetId,R.layout.widget_date,"clock");else if(cn.equals(NextAlarmWidgetProvider.class.getName()))update(c,widgetId,R.layout.widget_alarm,"alarms");else for(Class<?> cl:WidgetVariants.extras())if(cl.getName().equals(cn)){refreshClassOne(c,widgetId,cl);break;}}catch(Exception ignored){}}
    static void removeWidget(Context c,int id){WeatherRefresh.cancel(c,id);String prefix="widget_"+id+"_";android.content.SharedPreferences p=DataStore.prefs(c);android.content.SharedPreferences.Editor e=p.edit();for(String key:p.getAll().keySet())if(key.startsWith(prefix))e.remove(key);e.apply();DataStore.log(c,"Widget","Widget removed","Home-screen widget #"+id+" removed");}
    private static void refreshClassOne(Context c,int id,Class<?> cl){try{WidgetVariants.Base b=(WidgetVariants.Base)cl.newInstance();update(c,id,b.layoutId(),b.route());}catch(Exception ignored){}}
    static String nextAlarmDetail(Context c){return nextAlarmDetail(c,0);}
    static String nextAlarmDetail(Context c,int style){JSONArray a=DataStore.array(c,"alarms");JSONObject best=null;long at=Long.MAX_VALUE;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("enabled",true)){long t=DataStore.nextOccurrence(o,System.currentTimeMillis());if(t<at){at=t;best=o;}}}if(best==null)return "◴  No alarm · tap to add";String day=new SimpleDateFormat("EEE",Locale.getDefault()).format(new Date(at));String time=DataStore.formatTime(c,best.optInt("hour"),best.optInt("minute"));String label=best.optString("label","Alarm");switch(style){case 1:return "◴  "+time+" · "+label;case 2:return "◴  "+time;case 3:return label+" · "+time;case 4:return "◴  "+day+" · "+time;default:return "◴  "+day+", "+time+" · "+label;}}
    static String designName(Context c,int id){try{AppWidgetProviderInfo i=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);return i==null?"widget_clock":c.getResources().getResourceEntryName(i.initialLayout);}catch(Exception e){return "widget_clock";}}
    static int defaultSize(Context c,int id){String n=designName(c,id);return n.equals("widget_clock")?56:n.equals("widget_prism")?54:n.equals("widget_flip")?48:n.equals("widget_command")?42:n.equals("widget_halo")?46:n.equals("widget_bedside")?44:n.equals("widget_focus")?46:40;}
    static int defaultFont(Context c,int id){return defaultFont(designName(c,id));}
    static int defaultFont(String n){switch(n){case "widget_date":case "widget_calendar":case "widget_lavender":return 2;case "widget_alarm":case "widget_coral":case "widget_stacked":return 4;case "widget_world":case "widget_timer":case "widget_orbit":case "widget_quiet":return 5;case "widget_focus":return 6;case "widget_stopwatch":case "widget_mono":return 3;case "widget_prism":return 4;case "widget_flip":return 3;case "widget_command":return 5;case "widget_halo":return 0;case "widget_clock":case "widget_timeline":case "widget_sky":case "widget_bedside":case "widget_aurora":return 1;default:return 0;}}
    static boolean defaultShowDate(Context c,int id){return defaultShowDate(designName(c,id));}
    static boolean defaultShowDate(String n){return !(n.equals("widget_focus")||n.equals("widget_timer")||n.equals("widget_stopwatch")||n.equals("widget_mono")||n.equals("widget_sky")||n.equals("widget_bedside")||n.equals("widget_aurora"));}
    static boolean defaultShowAlarm(Context c,int id){return defaultShowAlarm(designName(c,id));}
    static boolean defaultShowAlarm(String n){return n.equals("widget_clock")||n.equals("widget_alarm")||n.equals("widget_analog")||n.equals("widget_bedside")||n.equals("widget_coral")||n.equals("widget_stacked")||n.equals("widget_quiet")||n.equals("widget_prism")||n.equals("widget_flip")||n.equals("widget_command")||n.equals("widget_halo");}
    static boolean defaultShowCaption(Context c,int id){return defaultShowCaption(designName(c,id));}
    static boolean defaultShowCaption(String n){return !(n.equals("widget_clock")||n.equals("widget_mono")||n.equals("widget_sky"));}
    static boolean defaultShowContext(Context c,int id){return defaultShowContext(designName(c,id));}
    static boolean defaultShowContext(String n){return !(n.equals("widget_clock")||n.equals("widget_mono")||n.equals("widget_sky")||n.equals("widget_focus"));}
    static int defaultDateFormat(Context c,int id){return defaultDateFormat(designName(c,id));}
    static int defaultDateFormat(String n){if(n.equals("widget_prism"))return 2;if(n.equals("widget_date")||n.equals("widget_calendar")||n.equals("widget_lavender"))return 1;if(n.equals("widget_mono")||n.equals("widget_sky"))return 3;return 0;}
    static int defaultAlarmDetail(Context c,int id){return defaultAlarmDetail(designName(c,id));}
    static int defaultAlarmDetail(String n){return n.equals("widget_alarm")||n.equals("widget_quiet")?0:1;}
    static boolean defaultTransparent(Context c,int id){return designName(c,id).equals("widget_clock");}
    static int defaultTextColor(Context c,int id){String n=designName(c,id);return n.equals("widget_prism")?0xff39e6e8:n.equals("widget_halo")?0xffe8fcff:n.equals("widget_clock")?0xfff7f2ff:(n.equals("widget_bedside")||n.equals("widget_mono")||n.equals("widget_aurora")||n.equals("widget_flip")||n.equals("widget_command")?Color.WHITE:0xff183a34);}
    static int defaultBackground(Context c,int id){return defaultBackgroundName(designName(c,id));}
    private static int defaultBackgroundName(String n){switch(n){case "widget_clock":return 0xff193d38;case "widget_date":return 0xfffff1c7;case "widget_alarm":return 0xfff6e9c9;case "widget_analog":return 0xffddefe8;case "widget_world":return 0xffdde7f7;case "widget_timeline":return 0xffddf1e9;case "widget_focus":return 0xffe7e2fa;case "widget_timer":return 0xffdcebf8;case "widget_stopwatch":return 0xffe6e9ed;case "widget_bedside":return 0xff263b38;case "widget_mono":return 0xff22282f;case "widget_calendar":return 0xfff2eee2;case "widget_coral":return 0xfff5ddd8;case "widget_sky":return 0xffddebf8;case "widget_lavender":return 0xffe9e1f4;case "widget_stacked":return 0xfff7ebcb;case "widget_orbit":return 0xffd9ecec;case "widget_routine":return 0xffe2efe2;case "widget_aurora":return 0xff1d2b39;case "widget_prism":return 0xff101920;case "widget_flip":return 0xff172334;case "widget_command":return 0xff152624;case "widget_halo":return 0xff12303a;default:return 0xffe8ece7;}}
    private static String secondary(Context c,String layout){switch(layout){case "widget_clock":return "NEXT  ·  "+nextAlarmDetail(c);case "widget_date":return "TODAY  ·  "+new SimpleDateFormat("EEEE",Locale.getDefault()).format(new Date());case "widget_alarm":return nextAlarmDetail(c);case "widget_analog":return "SECONDS HAND  ·  NEXT "+nextAlarmDetail(c);case "widget_world":return "LONDON  ·  "+zoneTime("Europe/London",c);case "widget_timeline":return "DAY ARC  ·  "+dayProgress()+"%";case "widget_orbit":return "ORBIT COMPLETED  ·  "+dayProgress()+"%";case "widget_focus":return "FOCUS  ·  OPEN TIMER";case "widget_timer":return "PRESETS   1  ·  5  ·  10  ·  25 MIN";case "widget_stopwatch":return "LAPS RECORDED  ·  "+DataStore.array(c,"laps").length();case "widget_bedside":return "WAKE  ·  "+nextAlarmDetail(c);case "widget_mono":return "HH:MM:SS   /   LOCAL";case "widget_calendar":return new SimpleDateFormat("'WEEK' w  ·  yyyy",Locale.getDefault()).format(new Date());case "widget_coral":return "UP NEXT  ·  "+nextAlarmDetail(c);case "widget_sky":return "LOCAL ZONE  ·  "+TimeZone.getDefault().getID().replace('_',' ');case "widget_lavender":return new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());case "widget_stacked":return "NEXT  ·  "+nextAlarmDetail(c);case "widget_routine":return nextRoutine(c);case "widget_aurora":return "QUIET HOURS  ·  ALARM READY";case "widget_quiet":return new SimpleDateFormat("EEE",Locale.getDefault()).format(new Date())+"  ·  "+nextAlarmDetail(c);case "widget_prism":return new SimpleDateFormat("EEE",Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault())+"  ·  LOCAL";case "widget_flip":return "FOLIO · LIVE LOCAL TIME";case "widget_command":return "SMART · TAP A SHORTCUT";case "widget_halo":return "TODAY · "+dayProgress()+"% COMPLETE";default:return "LOCAL TIME";}}
    private static String nextRoutine(Context c){JSONArray a=DataStore.array(c,"routines");JSONObject best=null;long soon=Long.MAX_VALUE;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optBoolean("enabled",true)){long t=DataStore.nextOccurrence(o,System.currentTimeMillis());if(t<soon){soon=t;best=o;}}}if(best==null)return "NO ROUTINE SCHEDULED";return best.optString("name","Routine")+"  ·  "+DataStore.formatTime(c,best.optInt("hour"),best.optInt("minute"));}
    private static String zoneTime(String id,Context c){SimpleDateFormat f=new SimpleDateFormat(DateFormat.is24HourFormat(c)?"HH:mm":"h:mm a",Locale.getDefault());f.setTimeZone(TimeZone.getTimeZone(id));return f.format(new Date());}
    private static int dayProgress(){Calendar c=Calendar.getInstance();int sec=c.get(Calendar.HOUR_OF_DAY)*3600+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND);return (int)(sec*100/86400f);}
}
