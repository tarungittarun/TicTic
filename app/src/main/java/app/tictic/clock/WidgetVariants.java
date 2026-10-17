package app.tictic.clock;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

/** Twenty-one additional named widget provider components (24 designs total with the original providers). */
public final class WidgetVariants {
    private WidgetVariants() {}
    public abstract static class Base extends android.appwidget.AppWidgetProvider {
        protected abstract int layoutId();
        protected abstract String route();
        @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids){ for(int id:ids)WidgetEngine.update(c,id,layoutId(),route()); }
        @Override public void onDeleted(Context c,int[] ids){for(int id:ids)WidgetEngine.removeWidget(c,id);}
        @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options){WidgetEngine.update(c,id,layoutId(),route());}
    }
    public static final class Analog extends Base {
        @Override protected int layoutId(){return R.layout.widget_analog;}
        @Override protected String route(){return "clock";}
    }
    public static final class WorldPair extends Base {
        @Override protected int layoutId(){return R.layout.widget_world;}
        @Override protected String route(){return "clock";}
    }
    public static final class DayArc extends Base {
        @Override protected int layoutId(){return R.layout.widget_timeline;}
        @Override protected String route(){return "clock";}
    }
    public static final class FocusSprint extends Base {
        @Override protected int layoutId(){return R.layout.widget_focus;}
        @Override protected String route(){return "timer";}
    }
    public static final class TimerTiles extends Base {
        @Override protected int layoutId(){return R.layout.widget_timer;}
        @Override protected String route(){return "timer";}
    }
    public static final class LapTrack extends Base {
        @Override protected int layoutId(){return R.layout.widget_stopwatch;}
        @Override protected String route(){return "stopwatch";}
    }
    public static final class Nightstand extends Base {
        @Override protected int layoutId(){return R.layout.widget_bedside;}
        @Override protected String route(){return "clock";}
    }
    public static final class MonoGrid extends Base {
        @Override protected int layoutId(){return R.layout.widget_mono;}
        @Override protected String route(){return "clock";}
    }
    public static final class PaperWeek extends Base {
        @Override protected int layoutId(){return R.layout.widget_calendar;}
        @Override protected String route(){return "clock";}
    }
    public static final class CoralPulse extends Base {
        @Override protected int layoutId(){return R.layout.widget_coral;}
        @Override protected String route(){return "clock";}
    }
    public static final class Skyline extends Base {
        @Override protected int layoutId(){return R.layout.widget_sky;}
        @Override protected String route(){return "clock";}
    }
    public static final class LilacStack extends Base {
        @Override protected int layoutId(){return R.layout.widget_lavender;}
        @Override protected String route(){return "clock";}
    }
    public static final class DateStamp extends Base {
        @Override protected int layoutId(){return R.layout.widget_stacked;}
        @Override protected String route(){return "clock";}
    }
    public static final class Orbit extends Base {
        @Override protected int layoutId(){return R.layout.widget_orbit;}
        @Override protected String route(){return "clock";}
    }
    public static final class RoutineLoop extends Base {
        @Override protected int layoutId(){return R.layout.widget_routine;}
        @Override protected String route(){return "routines";}
    }
    public static final class Aurora extends Base {
        @Override protected int layoutId(){return R.layout.widget_aurora;}
        @Override protected String route(){return "clock";}
    }
    public static final class QuietAlarm extends Base {
        @Override protected int layoutId(){return R.layout.widget_quiet;}
        @Override protected String route(){return "alarms";}
    }
    public static final class NeonPrism extends Base {
        @Override protected int layoutId(){return R.layout.widget_prism;}
        @Override protected String route(){return "clock";}
    }
    public static final class FlipDeck extends Base {
        @Override protected int layoutId(){return R.layout.widget_flip;}
        @Override protected String route(){return "clock";}
    }
    public static final class SmartConsole extends Base {
        @Override protected int layoutId(){return R.layout.widget_command;}
        @Override protected String route(){return "clock";}
    }
    public static final class HaloOrbit extends Base {
        @Override protected int layoutId(){return R.layout.widget_halo;}
        @Override protected String route(){return "clock";}
    }
    static Class<?>[] extras(){return new Class<?>[]{WidgetVariants.Analog.class, WidgetVariants.WorldPair.class, WidgetVariants.DayArc.class, WidgetVariants.FocusSprint.class, WidgetVariants.TimerTiles.class, WidgetVariants.LapTrack.class, WidgetVariants.Nightstand.class, WidgetVariants.MonoGrid.class, WidgetVariants.PaperWeek.class, WidgetVariants.CoralPulse.class, WidgetVariants.Skyline.class, WidgetVariants.LilacStack.class, WidgetVariants.DateStamp.class, WidgetVariants.Orbit.class, WidgetVariants.RoutineLoop.class, WidgetVariants.Aurora.class, WidgetVariants.QuietAlarm.class, WidgetVariants.NeonPrism.class, WidgetVariants.FlipDeck.class, WidgetVariants.SmartConsole.class, WidgetVariants.HaloOrbit.class};}
}
