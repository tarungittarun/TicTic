package app.tictic.clock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

/** Touch/drag clock dial for choosing an alarm hour or minute. */
final class AlarmTimeDialView extends View {
    interface Listener { void onTimeChanged(int hour, int minute); void onFieldChanged(int field); }
    static final int HOUR=0, MINUTE=1;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private int hour=7,minute=0,field=HOUR,accent=0xff3f7c73;
    private boolean dark,is24Hour;
    private Listener listener;
    private boolean touchStartedInsideDial;

    AlarmTimeDialView(Context context,boolean dark,int accent,boolean is24Hour){
        super(context);this.dark=dark;this.accent=accent;this.is24Hour=is24Hour;density=getResources().getDisplayMetrics().density;
        setFocusable(true);setContentDescription("Interactive clock dial. Drag the dial to select the highlighted time field.");
    }
    private float d(float value){return value*density;}
    void setListener(Listener listener){this.listener=listener;}
    void setTime(int hour,int minute){this.hour=Math.max(0,Math.min(23,hour));this.minute=Math.max(0,Math.min(59,minute));invalidate();}
    int hour(){return hour;} int minute(){return minute;} int field(){return field;}
    void setField(int field){this.field=field==MINUTE?MINUTE:HOUR;invalidate();if(listener!=null)listener.onFieldChanged(this.field);}
    void setHour(int value){hour=((value%24)+24)%24;changed();}
    void setMinute(int value){minute=((value%60)+60)%60;changed();}
    private void changed(){invalidate();if(listener!=null)listener.onTimeChanged(hour,minute);}

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.405f;
        int face=dark?0xff1b2a27:0xfff7faf8,ring=dark?0xff30423d:0xffd9e5df;
        paint.setStyle(Paint.Style.FILL);paint.setColor(face);canvas.drawCircle(cx,cy,r+d(13),paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d(1));paint.setColor(ring);canvas.drawCircle(cx,cy,r+d(13),paint);
        float handAngle=field==HOUR?((hour%12)*30f-90f):(minute*6f-90f);
        float handR=r*.78f;double ha=Math.toRadians(handAngle);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d(2));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(accent);canvas.drawLine(cx,cy,cx+(float)Math.cos(ha)*handR,cy+(float)Math.sin(ha)*handR,paint);
        paint.setStyle(Paint.Style.FILL);canvas.drawCircle(cx,cy,d(5),paint);
        if(field==HOUR){
            int current=hour%12==0?12:hour%12;int outerCurrent=is24Hour?(hour>=1&&hour<=12?hour:-1):current;
            for(int i=0;i<12;i++){int v=i==0?12:i;drawNumber(canvas,cx,cy,r*.78f,v,v==outerCurrent,false);}
            if(is24Hour){for(int i=0;i<12;i++){int v=i==0?0:12+i;drawNumber(canvas,cx,cy,r*.49f,v,v==hour,true);}}
        }else{
            for(int i=0;i<12;i++){int v=i*5;drawNumber(canvas,cx,cy,r*.78f,v,v==minute,false);}
            if(minute%5!=0){double ma=Math.toRadians(minute*6f-90f);paint.setStyle(Paint.Style.FILL);paint.setColor(accent);canvas.drawCircle(cx+(float)Math.cos(ma)*r*.78f,cy+(float)Math.sin(ma)*r*.78f,d(4),paint);}
        }
    }
    private void drawNumber(Canvas c,float cx,float cy,float radius,int value,boolean selected,boolean inner){
        double a=Math.toRadians((value==0||value==12?0:value)*30f-90f);
        // The minute ring and hour ring both use twelve positions. Zero and twelve sit at the top.
        if(field==MINUTE)a=Math.toRadians((value/5f)*30f-90f);
        float x=cx+(float)Math.cos(a)*radius,y=cy+(float)Math.sin(a)*radius;
        float circle=d(inner?22:27);
        if(selected){paint.setStyle(Paint.Style.FILL);paint.setColor(accent);c.drawCircle(x,y,circle*.64f,paint);}
        paint.setTypeface(selected?Typeface.create("sans-serif-medium",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));
        paint.setTextSize(d(inner?12:14));paint.setTextAlign(Paint.Align.CENTER);paint.setColor(selected?0xffffffff:(dark?0xffeff5f2:0xff18312f));
        String text=field==MINUTE?String.format(Locale.ROOT,"%02d",value):String.valueOf(value);
        Paint.FontMetrics fm=paint.getFontMetrics();c.drawText(text,x,y-(fm.ascent+fm.descent)/2,paint);
    }
    private boolean startsInsideDial(float x,float y){float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.405f+d(13);return Math.hypot(x-cx,y-cy)<=r;}
    private void disallowParentIntercept(boolean disallow){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(disallow);}
    @Override public boolean onTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){if(!startsInsideDial(event.getX(),event.getY())){touchStartedInsideDial=false;return false;}touchStartedInsideDial=true;disallowParentIntercept(true);select(event.getX(),event.getY(),false);return true;}
        if(!touchStartedInsideDial)return false;
        if(action==MotionEvent.ACTION_MOVE){select(event.getX(),event.getY(),false);return true;}
        if(action==MotionEvent.ACTION_UP){select(event.getX(),event.getY(),true);touchStartedInsideDial=false;performClick();disallowParentIntercept(false);return true;}
        if(action==MotionEvent.ACTION_CANCEL){touchStartedInsideDial=false;disallowParentIntercept(false);return true;}
        return true;
    }
    private void select(float x,float y,boolean finish){
        float cx=getWidth()/2f,cy=getHeight()/2f,dx=x-cx,dy=y-cy;
        float degrees=(float)Math.toDegrees(Math.atan2(dy,dx))+90f;if(degrees<0)degrees+=360f;
        if(field==HOUR){
            int slot=Math.round(degrees/30f)%12;
            float distance=(float)Math.hypot(dx,dy),outer=getWidth()<getHeight()?getWidth()*.405f:getHeight()*.405f;
            if(is24Hour&&distance<outer*.63f)hour=slot==0?0:12+slot;
            else {int face=slot==0?12:slot;if(is24Hour)hour=face;else hour=(hour>=12?12:0)+(face%12);}
            if(finish){field=MINUTE;if(listener!=null)listener.onFieldChanged(field);}
        }else{
            minute=Math.round(degrees/6f)%60;
        }
        changed();
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
