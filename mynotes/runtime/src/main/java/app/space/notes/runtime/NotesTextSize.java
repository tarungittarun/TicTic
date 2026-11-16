package app.space.notes.runtime;

import android.content.Context;
import android.content.res.Configuration;

/** Per-app presentation scale; the Android system font-size preference remains untouched. */
public final class NotesTextSize {
    public static final String SMALL="small",MEDIUM="medium",LARGE="large";
    private static final String PREFS="mynotes-ui",KEY="font-size";
    private NotesTextSize() { }

    public static String choice(Context context){
        String stored=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,MEDIUM);
        return SMALL.equals(stored)||MEDIUM.equals(stored)||LARGE.equals(stored)?stored:MEDIUM;
    }
    public static float multiplier(Context context){
        switch(choice(context)){
            case SMALL:return .90f;
            case LARGE:return 1.15f;
            default:return 1.0f;
        }
    }
    public static String label(Context context){
        switch(choice(context)){
            case SMALL:return "Small";
            case LARGE:return "Large";
            default:return "Medium";
        }
    }
    public static void choose(Context context,String value){
        if(!SMALL.equals(value)&&!MEDIUM.equals(value)&&!LARGE.equals(value))throw new IllegalArgumentException("Choose Small, Medium, or Large");
        if(!context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,value).commit())throw new IllegalStateException("Font size could not be saved");
    }
    /** Apply only to this MyNotes Activity. Existing SP layouts and spans scale together. */
    public static Context wrap(Context base){
        float multiplier=multiplier(base);
        if(multiplier==1.0f)return base;
        Configuration configuration=new Configuration(base.getResources().getConfiguration());
        configuration.fontScale*=multiplier;
        return base.createConfigurationContext(configuration);
    }
}
