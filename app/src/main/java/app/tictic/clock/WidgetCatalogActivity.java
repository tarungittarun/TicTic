package app.tictic.clock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import app.space.core.design.GelMotion;
import app.space.core.design.AppSurfaceStyle;
import java.util.ArrayList;

/** In-app picker for the 20 curated, resizable home-screen widget identities. */
public final class WidgetCatalogActivity extends Activity {
    private static final String[] NAMES={"Clock","Date","Alarm","Analog","World","Day Arc","Focus","Timers","Stopwatch","Bedside","Mono","Week","Date Stamp","Orbit","Routine","Quiet","Prism","Flip","Console","Halo"};
    private static final String[] LAYOUTS={"widget_clock","widget_date","widget_alarm","widget_analog","widget_world","widget_timeline","widget_focus","widget_timer","widget_stopwatch","widget_bedside","widget_mono","widget_calendar","widget_stacked","widget_orbit","widget_routine","widget_quiet","widget_prism","widget_flip","widget_command","widget_halo"};
    private static final String[] WHAT={
            "Bare seconds over the home wallpaper; date and alarm can expand in larger sizes.",
            "A date-first clock with configurable weekday and date formats.",
            "Next alarm time and label at a glance.",
            "Classic dial paired with a digital readout.",
            "Local time beside a second time zone.",
            "A simple visual read of how far the day has progressed.",
            "One-tap entry to a focused work timer.",
            "Common countdown presets for quick starts.",
            "Fast access to stopwatch and saved laps.",
            "Low-glare bedside clock with next alarm details.",
            "Fixed-width digits for clear, precise time.",
            "Week number and date in a paper-inspired card.",
            "Bold, stacked date hierarchy with time.",
            "Clock with a compact daily progress indicator.",
            "Next scheduled local routine and a direct shortcut.",
            "Quiet, alarm-first display for early starts.",
            "Raised-glass cyan clock with date and alarm controls.",
            "Split-flap-inspired time display and alarm details.",
            "Quick actions for Timer and Alarms from the widget.",
            "A day-progress bar with date and next alarm."};
    private static final int[] PROVIDER_INDEX={0,1,2,3,4,5,6,7,8,9,10,11,15,16,17,19,20,21,22,23};
    private final int[] colors={0xff0b1211,0xfffff2ce,0xfff8e9c4,0xffddefe8,0xffdce7f7,0xffdff1e9,0xffe8e2fa,0xffdeecfa,0xffe6e9ed,0xff263b38,0xff22282f,0xfff2eee2,0xfff7ebcb,0xffd9ecec,0xffe2efe2,0xffe8ece7,0xff12212b,0xff172334,0xff152624,0xff12303a};
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0x33808080);return d;}
    private LinearLayout v(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    @Override public void onCreate(Bundle b){super.onCreate(b);boolean dark=DataStore.dark(this);int page=dark?0xff0c1514:0xffedf2ef,ink=dark?Color.WHITE:0xff18312f,mut=dark?0xffa0b2ac:0xff70827d;LinearLayout root=v();root.setPadding(dp(20),dp(16),dp(20),dp(25));root.setBackground(AppSurfaceStyle.pageBackground(this));ScrollView scroll=new AliveScrollView(this);scroll.addView(root);setContentView(scroll);TextView title=new TextView(this);title.setText("MySpace Widgets");title.setTextSize(27);title.setTextColor(ink);title.setTypeface(null,Typeface.BOLD);root.addView(title);TextView intro=new TextView(this);intro.setText("Five core designs plus fifteen focused utilities and signature looks. Choose one to add it to your home screen.");intro.setTextSize(13);intro.setTextColor(mut);LinearLayout.LayoutParams introP=new LinearLayout.LayoutParams(-1,-2);introP.topMargin=dp(5);root.addView(intro,introP);addCatalogSection(root,"CORE · 5","Clock essentials");
        for(int i=0;i<NAMES.length;i++){if(i==5)addCatalogSection(root,"MORE · 15","Purpose-built utilities and expressive designs");final int ix=i;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(13),dp(10),dp(12),dp(10));int[] radii={36,18,12,22,8,30,32,10,8,12,4,4,10,50,18,12,24,16,18,48};card.setBackground(shape(colors[i],radii[i]));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(94));cp.topMargin=dp(10);root.addView(card,cp);LinearLayout sample=v();sample.setGravity(Gravity.CENTER);sample.setPadding(dp(4),0,dp(6),0);String[] previews={"10:08:24","Sat, 10 Oct","07:30","◷","NYC · LON","DAY 63%","FOCUS 25","1 · 5 · 10 · 25","00:42.18","10:08 PM","10:08:24","WEEK 42","SAT / 10 OCT","DAY 63%","WIND DOWN","NEXT · 07:30","10:58","10:58","SMART","DAY 42%"};int[] previewFonts={0,2,4,0,5,1,6,5,3,1,3,2,4,5,0,5,4,3,5,1};TextView clock=new TextView(this);clock.setText(previews[i]);clock.setTextSize(i==3?25:(i==7?10:16));clock.setTextColor(i==16?0xff41e6e9:(i==9||i==10||i==15||i>=16)?Color.WHITE:0xff18312f);clock.setTypeface(previewTypeface(previewFonts[i]));clock.setGravity(Gravity.CENTER);sample.addView(clock);TextView caption=new TextView(this);caption.setText(new String[]{"NO PANEL","DATE FIRST","ALARM FIRST","HANDS + DIGITAL","TWO ZONES","DAY BAR","FOCUS","PRESETS","LAPS","NIGHT","DIGITS ONLY","PAPER WEEK","BOLD DATE","DAILY ARC","ROUTINE","ALARM","3D GLASS","SPLIT FLIP","TAP TOOLS","DAY PROGRESS"}[i]);caption.setTextSize(7);caption.setTextColor(i==16?0xff9feeea:(i==9||i==10||i==15)?0xffd1ded8:0xff58736b);caption.setGravity(Gravity.CENTER);sample.addView(caption);card.addView(sample,new LinearLayout.LayoutParams(dp(102),dp(64)));LinearLayout copy=v();TextView name=new TextView(this);name.setText(NAMES[i]);name.setTextSize(14);name.setTextColor(ink);name.setTypeface(null,Typeface.BOLD);copy.addView(name);TextView desc=new TextView(this);desc.setText(WHAT[i]);desc.setTextSize(10);desc.setTextColor(mut);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=dp(3);copy.addView(desc,dp);LinearLayout.LayoutParams cp2=new LinearLayout.LayoutParams(0,-2,1);cp2.leftMargin=dp(9);card.addView(copy,cp2);TextView add=new TextView(this);add.setText(Build.VERSION.SDK_INT>=26?"＋":"Add");add.setTextSize(16);add.setTextColor(DataStore.accent(this));add.setTypeface(null,Typeface.BOLD);add.setGravity(Gravity.CENTER);card.addView(add,new LinearLayout.LayoutParams(dp(42),dp(48)));add.setOnClickListener(v->pin(ix));card.setOnClickListener(v->pin(ix));}
        GelMotion.apply(scroll,DataStore.accent(this));
    }
    private Typeface previewTypeface(int i){switch(i){case 1:case 11:case 14:return Typeface.SERIF;case 2:case 12:case 15:return Typeface.create("sans-serif-black",Typeface.NORMAL);case 3:return Typeface.create("sans-serif-medium",Typeface.NORMAL);case 4:case 7:case 16:case 19:return Typeface.create("sans-serif-condensed",Typeface.NORMAL);case 6:return Typeface.create("sans-serif-rounded",Typeface.NORMAL);case 8:case 10:return Typeface.MONOSPACE;case 9:case 13:case 18:return Typeface.create("sans-serif-light",Typeface.NORMAL);default:return Typeface.create("sans-serif-medium",Typeface.NORMAL);}}
    private void addCatalogSection(LinearLayout root,String title,String detail){TextView heading=new TextView(this);heading.setText(title);heading.setTextSize(11);heading.setTextColor(DataStore.accent(this));heading.setLetterSpacing(.13f);heading.setTypeface(null,Typeface.BOLD);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(28));hp.topMargin=dp(17);root.addView(heading,hp);TextView sub=new TextView(this);sub.setText(detail);sub.setTextSize(11);sub.setTextColor(DataStore.dark(this)?0xffa0b2ac:0xff70827d);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(20));root.addView(sub,sp);}
    private void pin(int i){Class<?>[] extra=WidgetVariants.extras();Class<?>[] providers=new Class<?>[3+extra.length];providers[0]=ClockWidgetProvider.class;providers[1]=DateWidgetProvider.class;providers[2]=NextAlarmWidgetProvider.class;for(int n=0;n<extra.length;n++)providers[n+3]=extra[n];if(Build.VERSION.SDK_INT>=26){AppWidgetManager manager=AppWidgetManager.getInstance(this);if(manager.isRequestPinAppWidgetSupported()){try{manager.requestPinAppWidget(new ComponentName(this,providers[PROVIDER_INDEX[i]]),null,null);return;}catch(Exception ignored){}}}Toast.makeText(this,"On your launcher, long-press an empty home-screen area → Widgets → MySpace.",Toast.LENGTH_LONG).show();}
}
