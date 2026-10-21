package app.tictic.clock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.space.core.design.GelMotion;
import app.space.core.design.AppSurfaceStyle;
import app.space.core.design.Palette;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Large, unambiguous full-screen ringing UI with safe Snooze/Stop actions. */
public final class AlarmActivity extends Activity {
    private long id;
    private String alarmLabel;
    private TextView clock;
    private final Handler handler=new Handler();
    private final Runnable ticker=new Runnable(){@Override public void run(){if(!isFinishing()){updateClock();handler.postDelayed(this,1000);}}};
    private boolean finishReceiverRegistered;
    private final BroadcastReceiver finishReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){if(intent!=null&&intent.getLongExtra("alarm_id",Long.MIN_VALUE)==id)finish();}
    };

    private int background(){return DataStore.dark(this)?0xff102321:0xff183b39;}
    /** Delegates to {@link Palette#blend}. This copy did not clamp its amount; the one
     *  call site passes .20f, so clamping is a no-op and the ringing background is
     *  unchanged. */
    private int blend(int from,int to,float amount){return Palette.blend(from,to,amount);}
    private android.graphics.drawable.Drawable ringingBackground(){int base=background();if(!AppSurfaceStyle.enabled(this))return new android.graphics.drawable.ColorDrawable(base);return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{base,blend(base,DataStore.accent(this),.20f),base});}
    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);recreate();}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        id=getIntent().getLongExtra("alarm_id",0);
        alarmLabel=getIntent().getStringExtra("label");if(alarmLabel==null||alarmLabel.trim().isEmpty())alarmLabel="Time to wake up";
        getWindow().setStatusBarColor(background());getWindow().setNavigationBarColor(background());

        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(24),dp(18),dp(24),dp(22));root.setBackground(ringingBackground());
        TextView brand=new TextView(this);brand.setText("TICTIC  ·  ALARM");brand.setTextColor(0xffa9dccc);brand.setTextSize(12);brand.setLetterSpacing(.16f);brand.setGravity(Gravity.CENTER);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(28)));
        View topSpace=new View(this);root.addView(topSpace,new LinearLayout.LayoutParams(1,0,1));
        clock=new TextView(this);clock.setTextColor(Color.WHITE);clock.setTextSize(64);clock.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));clock.setGravity(Gravity.CENTER);clock.setIncludeFontPadding(false);root.addView(clock,new LinearLayout.LayoutParams(-1,dp(88)));updateClock();
        TextView title=new TextView(this);title.setText(alarmLabel);title.setTextColor(Color.WHITE);title.setTextSize(24);title.setGravity(Gravity.CENTER);title.setMaxLines(2);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));LinearLayout.LayoutParams titleP=new LinearLayout.LayoutParams(-1,-2);titleP.topMargin=dp(12);root.addView(title,titleP);
        TextView subtitle=new TextView(this);subtitle.setText("Your alarm is ringing");subtitle.setTextColor(0xffbfd4cd);subtitle.setTextSize(15);subtitle.setGravity(Gravity.CENTER);LinearLayout.LayoutParams subP=new LinearLayout.LayoutParams(-1,dp(34));root.addView(subtitle,subP);
        View middleSpace=new View(this);root.addView(middleSpace,new LinearLayout.LayoutParams(1,0,1));

        JSONObject alarm=DataStore.find(this,"alarms",id);int minutes=alarm==null?9:Math.max(1,alarm.optInt("snooze",9));
        if(DataStore.showRingingSnoozeAction(this)){TextView snooze=action("Snooze  ·  "+minutes+" min",0xfff3ca72,0xff173b38,20);root.addView(snooze,new LinearLayout.LayoutParams(-1,dp(66)));snooze.setOnClickListener(v->AlarmActionReceiver.snooze(this,id));}
        TextView stop=action("Stop",0xffdbeae5,0xff183b39,18);LinearLayout.LayoutParams stopP=new LinearLayout.LayoutParams(-1,dp(60));stopP.topMargin=dp(12);root.addView(stop,stopP);stop.setOnClickListener(v->AlarmActionReceiver.dismiss(this,id));
        int volumeUp=DataStore.alarmVolumeAction(this,true),volumeDown=DataStore.alarmVolumeAction(this,false);
        String keys="Volume Up: "+(volumeUp==1?"Snooze":"Stop")+"  ·  Volume Down: "+(volumeDown==1?"Snooze":"Stop");
        TextView hint=new TextView(this);hint.setText(keys+" while this screen is open. Power is controlled by Android; use lock-screen notification actions if notifications are allowed.");hint.setTextColor(0xffb7ccc5);hint.setTextSize(12);hint.setGravity(Gravity.CENTER);hint.setMaxLines(3);LinearLayout.LayoutParams hintP=new LinearLayout.LayoutParams(-1,dp(52));hintP.topMargin=dp(4);root.addView(hint,hintP);
        View bottomSpace=new View(this);root.addView(bottomSpace,new LinearLayout.LayoutParams(1,0,.35f));
        setContentView(root);
        GelMotion.apply(root,DataStore.accent(this));
    }

    private TextView action(String text,int fill,int ink,int size){TextView view=new TextView(this);view.setText(text);view.setTextColor(ink);view.setTextSize(size);view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));view.setGravity(Gravity.CENTER);view.setMinHeight(dp(52));view.setPadding(dp(14),dp(8),dp(14),dp(8));android.graphics.drawable.Drawable surface=AppSurfaceStyle.enabled(this)?AppSurfaceStyle.criticalGlass(this,fill,17):rounded(fill,0);view.setBackground(new RippleDrawable(ColorStateList.valueOf((ink&0x00ffffff)|0x28000000),surface,null));view.setClickable(true);view.setFocusable(true);view.setContentDescription(text);view.setElevation(dp(2));return view;}
    private GradientDrawable rounded(int fill,int stroke){GradientDrawable shape=new GradientDrawable();shape.setColor(fill);shape.setCornerRadius(dp(17));if(stroke!=0)shape.setStroke(dp(1.5f),stroke);return shape;}
    private void updateClock(){if(clock!=null){String format=android.text.format.DateFormat.is24HourFormat(this)?"HH:mm":"h:mm";clock.setText(new SimpleDateFormat(format,Locale.getDefault()).format(new Date()));}}

    @Override protected void onStart(){super.onStart();if(!finishReceiverRegistered){IntentFilter filter=new IntentFilter(AlarmActionReceiver.ACTION_FINISH);if(Build.VERSION.SDK_INT>=33)registerReceiver(finishReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(finishReceiver,filter);finishReceiverRegistered=true;}}
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onStop(){if(finishReceiverRegistered){unregisterReceiver(finishReceiver);finishReceiverRegistered=false;}super.onStop();}

    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int code=event.getKeyCode();
        if(code==KeyEvent.KEYCODE_VOLUME_DOWN||code==KeyEvent.KEYCODE_VOLUME_UP){
            int mode=DataStore.alarmVolumeAction(this,code==KeyEvent.KEYCODE_VOLUME_UP);
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){if(mode==1)AlarmActionReceiver.snooze(this,id);else AlarmActionReceiver.dismiss(this,id);}
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed(){AlarmActionReceiver.dismiss(this,id);}
}
