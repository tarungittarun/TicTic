package app.tictic.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.util.Calendar;

/** Lightweight native analog face with user-selectable dial styling. */
public final class AnalogClockView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar clock=Calendar.getInstance();
    private final float density;private boolean lastSecondsValue,hasSecondsValue;private int lastStyle=-1;
    private static final String[] STYLE_NAMES={"classic markers","minimal","numerals","accent ring"};
    public AnalogClockView(Context c){this(c,null);}
    public AnalogClockView(Context c,AttributeSet a){super(c,a);density=getResources().getDisplayMetrics().density;setContentDescription("Analog clock with selectable dial style");}
    private float d(float x){return x*density;}
    private boolean powerSaverOptimization(){if(!DataStore.prefs(getContext()).getBoolean("adaptive_power_saver",true))return false;android.os.PowerManager pm=(android.os.PowerManager)getContext().getSystemService(Context.POWER_SERVICE);return pm!=null&&pm.isPowerSaveMode();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.46f;
        boolean dark=DataStore.dark(getContext());int ink=dark?0xfff1f4f2:0xffffffff;int soft=dark?0xffd3e0db:0xffd9e8e1;int accent=DataStore.accent(getContext());
        int style=Math.max(0,Math.min(STYLE_NAMES.length-1,DataStore.prefs(getContext()).getInt("clock_analog_style",0)));
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(style==1?1.5f:style==3?3.4f:2.8f));p.setColor(style==3?accent:0x66ffffff);canvas.drawCircle(cx,cy,r,p);
        if(style!=1){p.setStyle(Paint.Style.FILL);p.setColor(0x1effffff);canvas.drawCircle(cx,cy,r-d(2),p);}
        int stride=style==1?5:1;
        for(int i=0;i<60;i+=stride){boolean major=i%5==0;if(style==2&&major)continue;double a=Math.PI*i/30-Math.PI/2;float outer=r-d(8);float inner=outer-(major?d(10):d(4));p.setColor(style==3&&major?accent:major?ink:soft);p.setStrokeWidth(major?d(2):d(1));p.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,cx+(float)Math.cos(a)*outer,cy+(float)Math.sin(a)*outer,p);}
        if(style==2){p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(d(11));p.setTextAlign(Paint.Align.CENTER);p.setColor(ink);for(int n=1;n<=12;n++){double a=Math.PI*n/6-Math.PI/2;canvas.drawText(String.valueOf(n),cx+(float)Math.cos(a)*r*.72f,cy+(float)Math.sin(a)*r*.72f+d(4),p);}}
        clock.setTimeInMillis(System.currentTimeMillis());int h=clock.get(Calendar.HOUR);int m=clock.get(Calendar.MINUTE);boolean saver=powerSaverOptimization();boolean seconds=DataStore.prefs(getContext()).getBoolean("show_home_seconds",true)&&!saver;int s=clock.get(Calendar.SECOND);float sec=seconds?s+clock.get(Calendar.MILLISECOND)/1000f:0f;
        hand(canvas,cx,cy,r*.52f,(h+m/60f)/12f,d(5.5f),ink);
        hand(canvas,cx,cy,r*.77f,(m+sec/60f)/60f,d(3.7f),ink);
        if(seconds)hand(canvas,cx,cy,r*.82f,sec/60f,d(1.6f),accent);
        p.setStyle(Paint.Style.FILL);p.setColor(accent);canvas.drawCircle(cx,cy,d(5),p);
        if(!hasSecondsValue||lastSecondsValue!=seconds||lastStyle!=style){setContentDescription("Analog clock, "+STYLE_NAMES[style]+(seconds?", with seconds":""));lastSecondsValue=seconds;lastStyle=style;hasSecondsValue=true;}
    }
    private void hand(Canvas c,float cx,float cy,float len,float fraction,float stroke,int color){double a=fraction*2*Math.PI-Math.PI/2;p.setColor(color);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);c.drawLine(cx,cy,cx+(float)Math.cos(a)*len,cy+(float)Math.sin(a)*len,p);p.setStyle(Paint.Style.FILL);}
}
