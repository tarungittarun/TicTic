package app.space.notes.runtime;
import android.content.Context;
import android.content.res.Configuration;
/** Standalone preference cannot override an embedding host's appearance policy. */
public final class NotesAppearance {
 private NotesAppearance(){}
 public static String choice(Context c){return c.getSharedPreferences("mynotes-ui",0).getString("appearance","system");}
 public static boolean dark(Context c,NotesHost host){String mode=host.managesAppearance()?host.appearanceMode():choice(c);return "dark".equals(mode) || (!"light".equals(mode) && (c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
 public static void choose(Context c,String mode){if(!java.util.Arrays.asList("system","light","dark").contains(mode))throw new IllegalArgumentException("Unknown appearance");c.getSharedPreferences("mynotes-ui",0).edit().putString("appearance",mode).apply();}
}
