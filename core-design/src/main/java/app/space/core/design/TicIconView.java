package app.space.core.design;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Compact, consistent bold line icons drawn locally so the interface needs no icon
 * pack or third-party art.
 *
 * <p>The first class extracted into {@code :core-design} (SUITE-PLAN.md §3.1, §6.1
 * step 1c). It was chosen because it is the only design class with zero coupling to
 * the data layer — it reads no preferences and no theme state — so it can move
 * without inverting the module dependency rule. The class and its members were
 * already {@code public}; only the package changed.
 *
 * <p>Kept free of data-layer access on purpose: a contract test in
 * {@code qa/test_source_contracts.py} fails the build if any class in this module
 * starts reaching back into {@code :app} for state. That test matches the store's
 * class name literally, so this comment must not spell it out.
 *
 * <p>MySpace app glyphs (calculator, notes, habits, expenses, weather, files,
 * recorder, music, calendar and the rest) are added to this same enum-style set so
 * every app in the suite inherits one icon language instead of importing packs.
 */
public final class TicIconView extends View {
    public static final int CLOCK=0,TIMER=1,STOPWATCH=2,ROUTINE=3,ACTIVITY=4,SETTINGS=5,ALARM=6,WORLD=7,REMINDER=8,CALCULATOR=9;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int kind,color;private final float den;
    public TicIconView(Context c,int kind,int color){super(c);this.kind=kind;this.color=color;den=getResources().getDisplayMetrics().density;setContentDescription(kind==CLOCK?"Clock":kind==TIMER?"Timer":kind==STOPWATCH?"Stopwatch":kind==ROUTINE?"Routines":kind==ACTIVITY?"Usage":kind==SETTINGS?"Settings":kind==ALARM?"Alarm":kind==REMINDER?"Reminders":"World time");}
    private float d(float x){return x*den;}
    private void stroke(float w){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d(w));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float s=Math.min(getWidth(),getHeight())/24f;c.translate((getWidth()-24*s)/2f,(getHeight()-24*s)/2f);c.scale(s,s);stroke(.96f);
        switch(kind){
            case CLOCK: case ALARM: c.drawCircle(12,13,8.4f,p);c.drawLine(12,13,12,8,p);c.drawLine(12,13,16.2f,15.5f,p);c.drawCircle(12,13,1.1f,p);if(kind==ALARM){c.drawLine(5,3.5f,8,5.4f,p);c.drawLine(19,3.5f,16,5.4f,p);c.drawLine(8,22,6,24,p);c.drawLine(16,22,18,24,p);}break;
            case TIMER: c.drawCircle(12,13,8.5f,p);c.drawLine(12,13,12,8,p);c.drawLine(12,13,16,13,p);c.drawLine(9,2.8f,15,2.8f,p);c.drawLine(12,3,12,4.6f,p);c.drawLine(18.3f,6,20,4.5f,p);break;
            case STOPWATCH: c.drawCircle(12,14,8.1f,p);c.drawLine(9,3,15,3,p);c.drawLine(12,3,12,5.4f,p);c.drawLine(12,14,12,8.5f,p);c.drawLine(12,14,16,16.3f,p);c.drawLine(18.5f,6,20.5f,4,p);break;
            case ROUTINE: Path star=new Path();star.moveTo(12,2);star.lineTo(14.2f,9.2f);star.lineTo(21,12);star.lineTo(14.2f,14.1f);star.lineTo(12,22);star.lineTo(9.8f,14.1f);star.lineTo(3,12);star.lineTo(9.8f,9.2f);star.close();c.drawPath(star,p);c.drawCircle(19.5f,4.5f,1.4f,p);break;
            case ACTIVITY: stroke(1.11f);c.drawLine(4,20,4,5,p);c.drawLine(4,20,21,20,p);c.drawRoundRect(7,14,9.8f,19,1.2f,1.2f,p);c.drawRoundRect(11.1f,10,13.9f,19,1.2f,1.2f,p);c.drawRoundRect(15.2f,6,18,19,1.2f,1.2f,p);Path trend=new Path();trend.moveTo(6.2f,12.1f);trend.lineTo(10.2f,9.1f);trend.lineTo(13.8f,11f);trend.lineTo(19,5.2f);c.drawPath(trend,p);c.drawLine(16.2f,5.2f,19,5.2f,p);c.drawLine(19,5.2f,19,8,p);p.setStyle(Paint.Style.FILL);c.drawCircle(10.2f,9.1f,1.1f,p);c.drawCircle(13.8f,11f,1.1f,p);p.setStyle(Paint.Style.STROKE);break;
            case SETTINGS: c.drawCircle(12,12,7.1f,p);c.drawCircle(12,12,2.8f,p);for(int i=0;i<8;i++){double a=Math.PI*i/4;float x=(float)Math.cos(a),y=(float)Math.sin(a);c.drawLine(12+x*8.2f,12+y*8.2f,12+x*10.3f,12+y*10.3f,p);}break;
            case CALCULATOR: c.drawRoundRect(5,3,19,21,2,2,p);c.drawLine(7.5f,7.6f,16.5f,7.6f,p);c.drawCircle(9,12,1.05f,p);c.drawCircle(12,12,1.05f,p);c.drawCircle(15,12,1.05f,p);c.drawCircle(9,16.2f,1.05f,p);c.drawCircle(12,16.2f,1.05f,p);c.drawCircle(15,16.2f,1.05f,p);break;
            case WORLD: c.drawCircle(12,12,9,p);c.drawOval(8,3,16,21,p);c.drawLine(3,12,21,12,p);c.drawArc(4,6,20,18,0,180,false,p);c.drawArc(4,6,20,18,180,180,false,p);break;
            case REMINDER: Path bell=new Path();bell.moveTo(5,17);bell.lineTo(7,15);bell.lineTo(7,10);c.drawArc(7,4,17,16,180,180,false,p);bell.lineTo(17,15);bell.lineTo(19,17);bell.close();c.drawPath(bell,p);c.drawLine(9,17,15,17,p);c.drawArc(9,16,15,21,0,180,false,p);break;
            default: c.drawCircle(12,12,8,p);
        }c.restore();}
}
