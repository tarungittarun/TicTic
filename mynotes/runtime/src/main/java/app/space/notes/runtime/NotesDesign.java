package app.space.notes.runtime;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Paper & Ink: quiet writing surfaces, warm honey accents, one visual vocabulary. */
public final class NotesDesign {
 public final Context context; public final boolean dark;
 public final int background,surface,ink,muted,outline,accent,accentInk,primaryFill,primaryInk;
 public NotesDesign(Context c,boolean dark){this(c,dark,null);}
 public NotesDesign(Context c,boolean dark,NotesPalette palette){context=c;this.dark=dark;
  if(palette!=null){background=palette.background;surface=palette.surface;ink=palette.ink;muted=palette.muted;outline=palette.outline;accent=palette.accent;accentInk=palette.accentInk;primaryFill=palette.accent;primaryInk=palette.accentInk;return;}
  background=Color.parseColor(dark?"#17191D":"#FAFAF8");surface=Color.parseColor(dark?"#24272D":"#F0F1EE");
  ink=Color.parseColor(dark?"#EBEDF0":"#24282D");muted=Color.parseColor(dark?"#AEB5BF":"#59636F");
  outline=Color.parseColor(dark?"#3B414A":"#D9DDD8");accent=Color.parseColor(dark?"#E8C58E":"#76551D");accentInk=Color.parseColor("#241A0B");primaryFill=Color.parseColor("#E8C58E");primaryInk=accentInk;
 }
 public int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
 public GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 public android.graphics.drawable.Drawable ripple(int color,int radius){return new RippleDrawable(ColorStateList.valueOf((accent&0x00ffffff)|(dark?0x33000000:0x22000000)),shape(color,radius),null);}
 public android.graphics.drawable.Drawable choice(boolean selected){int tinted=Color.rgb((int)(Color.red(surface)*.84+Color.red(accent)*.16),(int)(Color.green(surface)*.84+Color.green(accent)*.16),(int)(Color.blue(surface)*.84+Color.blue(accent)*.16));GradientDrawable fill=shape(selected?tinted:Color.TRANSPARENT,14);if(selected)fill.setStroke(dp(2),accent);return new RippleDrawable(ColorStateList.valueOf((accent&0x00ffffff)|0x22000000),fill,null);}
 public TextView label(String text,int sp,int color){TextView v=new TextView(context);v.setText(text);v.setTextColor(color);v.setTextSize(sp);v.setIncludeFontPadding(true);return v;}
 public Button icon(String name,String symbol,Runnable action){Button v=new Button(context);v.setText("");v.setTextSize(0);v.setIncludeFontPadding(false);v.setMinWidth(dp(48));v.setMinHeight(dp(48));v.setGravity(Gravity.CENTER);v.setPadding(dp(12),dp(12),dp(12),dp(12));v.setContentDescription(name);v.setTooltipText(name);v.setBackground(ripple(Color.TRANSPARENT,24));NotesIcon d=new NotesIcon(symbol,ink);d.setBounds(0,0,dp(24),dp(24));v.setCompoundDrawables(null,d,null,null);v.setOnClickListener(w->action.run());return v;}
 public Button textAction(String name,Runnable action){Button b=new Button(context);b.setText(name);b.setAllCaps(false);b.setTextColor(ink);b.setTextSize(15);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setMinHeight(dp(52));b.setPadding(dp(16),dp(10),dp(16),dp(10));b.setSingleLine(false);b.setBackground(ripple(Color.TRANSPARENT,12));b.setOnClickListener(v->action.run());return b;}
 public void primary(Button button){button.setTextColor(primaryInk);button.setBackground(ripple(primaryFill,18));}
 public View divider(){View v=new View(context);v.setBackgroundColor(outline);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
 public void entry(EditText v,int sp){v.setSelectAllOnFocus(false);v.setTextClassifier(android.view.textclassifier.TextClassifier.NO_OP);v.setImeOptions(v.getImeOptions()|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);v.setMinHeight(dp(48));v.setTextSize(sp);v.setTextColor(ink);v.setHintTextColor(muted);v.setBackground(null);v.setPadding(0,dp(8),0,dp(8));v.setHighlightColor(dark?0x667E6035:0x66D8B16D);v.setLineSpacing(dp(3),1.08f);v.setIncludeFontPadding(true);}
 public TextView heading(String title){TextView v=label(title,23,ink);v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));v.setPadding(0,dp(12),0,dp(12));return v;}
}
