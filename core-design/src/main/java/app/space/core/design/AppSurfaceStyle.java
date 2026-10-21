package app.space.core.design;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import app.space.core.data.AppPrefs;
import app.space.core.data.AppTheme;

/** Shared original TicTic glass surfaces. Critical alarm controls request a higher opacity floor. */
public final class AppSurfaceStyle {
    private AppSurfaceStyle() {}
    public static boolean enabled(Context c){return AppPrefs.prefs(c).getBoolean("app_glass_enabled",false);}
    public static Drawable glass(Context c,int base,int radius,int opacityFloor){SharedPreferences p=AppPrefs.prefs(c);return glass(c,base,radius,p.getInt("app_glass_opacity",GlassPreset.OPACITY[0]),p.getInt("app_glass_finish",GlassPreset.FINISH[0]),p.getInt("app_glass_depth",GlassPreset.DEPTH[0]),opacityFloor);}
    public static Drawable glass(Context c,int base,int radius,int opacity,int finish,int depth,int opacityFloor){return glass(c,base,radius,opacity,finish,depth,GlassPreset.selected(c),opacityFloor);}
    public static Drawable glass(Context c,int base,int radius,int opacity,int finish,int depth,int preset,int opacityFloor){return new AppGlassDrawable(c,base,radius,opacity,finish,depth,preset,opacityFloor);}
    public static Drawable criticalGlass(Context c,int fill,int radius){return glass(c,fill,radius,AppPrefs.prefs(c).getInt("app_glass_opacity",GlassPreset.OPACITY[0]),AppPrefs.prefs(c).getInt("app_glass_finish",GlassPreset.FINISH[0]),AppPrefs.prefs(c).getInt("app_glass_depth",GlassPreset.DEPTH[0]),88);}
    public static Drawable pageBackground(Context c){boolean dark=AppTheme.dark(c);int bg=dark?0xff0c1514:0xffedf2ef;if(!enabled(c))return new android.graphics.drawable.ColorDrawable(bg);int accent=AppTheme.accent(c);int glow=Palette.blend(bg,accent,dark?.21f:.12f);return new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{bg,glow,bg});}
}
