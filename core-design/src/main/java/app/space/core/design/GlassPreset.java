package app.space.core.design;

import android.content.Context;
import android.content.SharedPreferences;
import app.space.core.data.AppPrefs;

/** Five editable glass starting points; presets never change foreground text colors. */
public final class GlassPreset {
    public static final String[] NAMES={"Sunlit Bubble","Daylight Pearl","Golden Hour","Moonlit Opal","Aurora Frost"};
    public static final String[] NOTES={"Warm, soft dawn light","Clear cool daylight","Amber evening rim","Silver-blue night glow","Quiet violet iridescence"};
    static final int[] GLOW={0xffffd18c,0xffbdeee5,0xffffb46f,0xffbdccff,0xffc6b0ff};
    static final int[] BASE={0xff536e68,0xff456d72,0xff78604f,0xff394563,0xff57506f};
    public static final int[] OPACITY={92,93,92,95,96};
    public static final int[] FINISH={68,54,82,42,70};
    public static final int[] DEPTH={52,42,60,38,50};
    static final int[] WIDGET_OPACITY={236,238,235,241,244};
    private GlassPreset(){}
    public static int clamp(int value){return Math.max(0,Math.min(NAMES.length-1,value));}
    public static int selected(Context c){return clamp(AppPrefs.prefs(c).getInt("app_glass_preset",0));}
    static int widgetSelected(Context c,String key){return AppPrefs.prefs(c).getInt(key+"glass_preset",0);}
    public static void apply(Context c,int index){int i=clamp(index);SharedPreferences.Editor e=AppPrefs.prefs(c).edit();e.putInt("app_glass_preset",i).putInt("app_glass_opacity",OPACITY[i]).putInt("app_glass_finish",FINISH[i]).putInt("app_glass_depth",DEPTH[i]).putBoolean("app_glass_customized",false).apply();}
    public static int glow(int index){return GLOW[clamp(index)];}
    public static int widgetBase(int index){return BASE[clamp(index)];}
    public static int widgetOpacity(int index){return WIDGET_OPACITY[clamp(index)];}
}
