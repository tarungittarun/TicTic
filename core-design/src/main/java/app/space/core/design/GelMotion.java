package app.space.core.design;

import android.animation.ValueAnimator;
import android.content.Context;
import app.space.core.data.AppPrefs;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;

/**
 * Shared, selectable tactile feedback for clickable app surfaces.
 *
 * <p>Moved into {@code :core-design} in step 1c-2 (SUITE-PLAN.md §6.1) so every MySpace
 * app gets the same five press styles, intensity control and Reduce-motion handling
 * instead of each feature reinventing touch feedback. Became {@code public} for the
 * module boundary; only {@link #apply} is used outside this module, so the style
 * constants stay package-private.
 *
 * <p>Its preference reads moved from the legacy store to {@code AppPrefs}/{@code AppTheme}
 * in {@code :core-data} — motion style and intensity are settings, and dark/accent are
 * appearance state. Behaviour is unchanged.
 */
public final class GelMotion {
    static final int WATER_RIPPLE=0, GEL_SQUEEZE=1, EXPRESSIVE_SPRING=2, GLASS_WAVE=3, MAGNETIC_LIFT=4;
    private GelMotion(){}

    /** Attaches the user's selected press style to every pressable view in a tree. */
    public static void apply(View root,int accent){if(root!=null)visit(root,accent);}

    private static void visit(View view,int accent){
        if(view.isClickable()&&isPressable(view))attach(view,accent);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)visit(group.getChildAt(i),accent);}
    }

    private static boolean isPressable(View view){
        return !view.isLongClickable()&&!(view instanceof CompoundButton)&&!(view instanceof EditText)
                &&!(view instanceof SeekBar)&&!(view instanceof Spinner)&&!(view instanceof AdapterView)
                &&!(view instanceof NumberPicker)&&!(view instanceof ProgressBar)
                &&!(view instanceof ScrollView)&&!(view instanceof HorizontalScrollView);
    }

    private static int motionMode(Context c){return Math.max(0,Math.min(4,AppPrefs.prefs(c).getInt("app_motion_style",GEL_SQUEEZE)));}
    private static float intensity(Context c){return Math.max(0,Math.min(100,AppPrefs.prefs(c).getInt("app_motion_intensity",68)))/100f;}
    private static boolean reducedMotion(Context c){
        if(AppPrefs.prefs(c).getBoolean("app_reduce_motion",false))return true;
        try{if(Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled())return true;return Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)<=0f;}catch(RuntimeException ignored){return false;}
    }
    private static int rippleColor(int accent,int mode){int alpha=mode==WATER_RIPPLE?0x35:mode==EXPRESSIVE_SPRING?0x28:mode==GLASS_WAVE?0x24:0x20;return (alpha<<24)|(accent&0x00ffffff);}

    private static void attach(View view,int accent){
        Context context=view.getContext();int mode=motionMode(context);float strength=intensity(context);boolean reduced=reducedMotion(context);
        if(view.getForeground()==null&&!(view.getBackground() instanceof RippleDrawable)){
            GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(view.getResources().getDisplayMetrics().density*22f);
            view.setForeground(new RippleDrawable(ColorStateList.valueOf(rippleColor(accent,mode)),null,mask));
        }
        final float density=view.getResources().getDisplayMetrics().density;final int touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();final float[] down={0f,0f};final boolean[] tracking={false};
        view.setOnTouchListener((target,event)->{
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN&&target.isEnabled()){
                down[0]=event.getX();down[1]=event.getY();tracking[0]=true;target.setPressed(true);
                if(!reduced&&strength>0f&&mode!=WATER_RIPPLE){
                    target.animate().cancel();target.setLayerType(View.LAYER_TYPE_HARDWARE,null);target.setPivotX(target.getWidth()*.5f);target.setPivotY(target.getHeight()*.5f);
                    float sx=1f,sy=1f,ty=0f,tz=0f;
                    switch(mode){
                        case GEL_SQUEEZE:sx=1f+.022f*strength;sy=1f-.065f*strength;ty=1.2f*density*strength;break;
                        case EXPRESSIVE_SPRING:sx=1f-.035f*strength;sy=1f-.035f*strength;ty=1f*density*strength;break;
                        case GLASS_WAVE:sx=1f-.012f*strength;sy=1f-.012f*strength;ty=-.4f*density*strength;break;
                        case MAGNETIC_LIFT:sx=1f+.018f*strength;sy=1f+.018f*strength;ty=-2.2f*density*strength;tz=3f*density*strength;break;
                    }
                    target.animate().scaleX(sx).scaleY(sy).translationY(ty).translationZ(tz).setDuration(88).setInterpolator(new DecelerateInterpolator(1.4f)).start();
                }
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE&&tracking[0]){
                boolean within=Math.abs(event.getX()-down[0])<=touchSlop*1.5f&&Math.abs(event.getY()-down[1])<=touchSlop*1.5f&&event.getX()>=0f&&event.getX()<=target.getWidth()&&event.getY()>=0f&&event.getY()<=target.getHeight();
                if(!within){tracking[0]=false;target.setPressed(false);release(target,mode,reduced);}
                return true;
            }
            if(action==MotionEvent.ACTION_UP){boolean click=tracking[0]&&target.isEnabled();tracking[0]=false;target.setPressed(false);release(target,mode,reduced);if(click)target.performClick();return true;}
            if(action==MotionEvent.ACTION_CANCEL){tracking[0]=false;target.setPressed(false);release(target,mode,reduced);return true;}
            return tracking[0];
        });
    }

    private static void release(View target,int mode,boolean reduced){
        target.animate().cancel();
        if(reduced||mode==WATER_RIPPLE){target.setScaleX(1f);target.setScaleY(1f);target.setTranslationY(0f);target.setTranslationZ(0f);target.setLayerType(View.LAYER_TYPE_NONE,null);return;}
        float tension=mode==EXPRESSIVE_SPRING?1.2f:mode==GEL_SQUEEZE?.78f:mode==MAGNETIC_LIFT?.42f:.22f;
        long duration=mode==GLASS_WAVE?210:mode==MAGNETIC_LIFT?250:330;
        target.animate().scaleX(1f).scaleY(1f).translationY(0f).translationZ(0f).setDuration(duration)
                .setInterpolator(mode==GLASS_WAVE?new DecelerateInterpolator(1.2f):new OvershootInterpolator(tension))
                .withEndAction(()->target.setLayerType(View.LAYER_TYPE_NONE,null)).start();
    }
}
