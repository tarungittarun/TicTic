package app.space.notes.runtime;
import android.content.Context;
import android.content.SharedPreferences;
import app.space.notes.domain.TypingStyle;
import app.space.notes.domain.RichText;
import java.util.*;
import java.io.IOException;
import org.json.*;
/** App-private, text-free presets; versioned separately from note/checkpoint schemas. */
public final class StylePresetStore {
    public static final int MAX_PRESETS=16;
    private static final java.util.concurrent.ExecutorService WRITER=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final Object LOCK=new Object();
    private final SharedPreferences prefs;
    public StylePresetStore(Context context){prefs=context.getSharedPreferences("mynotes-style-presets",Context.MODE_PRIVATE);}
    public Map<String,TypingStyle> load()throws IOException{
        String source;try{source=prefs.getString("styles",null);}catch(ClassCastException corrupt){throw new IOException("Saved styles have an invalid type; not overwritten",corrupt);}Map<String,TypingStyle> result=new LinkedHashMap<>();if(source==null)return result;
        try{if(source.length()>8192)throw new IOException("Saved styles exceed safety limit; not overwritten");JSONObject root=new JSONObject(source);if(root.length()!=2 || root.getInt("version")!=1)throw new IOException("Unsupported saved styles; not overwritten");JSONArray array=root.getJSONArray("presets");if(array.length()>MAX_PRESETS)throw new IOException("Too many saved styles");Set<String> names=new HashSet<>();for(int i=0;i<array.length();i++){JSONObject item=array.getJSONObject(i);if(item.length()!=2)throw new IOException("Unsupported saved style");String name=name(item.getString("name"));if(!names.add(name.toLowerCase(Locale.ROOT)))throw new IOException("Duplicate saved style");result.put(name,TypingStyle.decode(item.getString("style")));}return result;}catch(JSONException|IllegalArgumentException invalid){throw new IOException("Saved styles could not be verified; not overwritten",invalid);}
    }
    public void save(String label,TypingStyle style)throws IOException{synchronized(LOCK){String name=name(label);Map<String,TypingStyle> all=load();for(String existing:all.keySet())if(existing.equalsIgnoreCase(name))throw new IOException("That name exists. Delete it first or choose another name.");if(all.size()>=MAX_PRESETS)throw new IOException("Keep at most 16 styles; delete an unused style first");all.put(name,style);write(all);}}
    public void delete(String label)throws IOException{synchronized(LOCK){Map<String,TypingStyle> all=load();if(all.remove(label)==null)throw new IOException("Saved style no longer exists");write(all);}}
    public void saveAsync(String label,TypingStyle style,java.util.function.Consumer<IOException> done){submit(()->save(label,style),done);}
    public void deleteAsync(String label,java.util.function.Consumer<IOException> done){submit(()->delete(label),done);}
    private interface Write {void run()throws IOException;}
    private void submit(Write action,java.util.function.Consumer<IOException> done){WRITER.execute(()->{IOException failure=null;try{action.run();}catch(IOException invalid){failure=invalid;}catch(IllegalArgumentException invalid){failure=new IOException(invalid.getMessage(),invalid);}final IOException result=failure;new android.os.Handler(android.os.Looper.getMainLooper()).post(()->done.accept(result));});}
    private void write(Map<String,TypingStyle> styles)throws IOException{try{JSONArray array=new JSONArray();for(Map.Entry<String,TypingStyle> entry:styles.entrySet())array.put(new JSONObject().put("name",entry.getKey()).put("style",entry.getValue().encode()));if(!prefs.edit().putString("styles",new JSONObject().put("version",1).put("presets",array).toString()).commit())throw new IOException("Style could not be written; retry when storage is available");}catch(JSONException invalid){throw new IOException("Style was not saved",invalid);}}
    private static String name(String value){if(value==null)throw new IllegalArgumentException("Enter a style name");String name=value.trim();RichText.requireText(name);if(name.isEmpty() || name.length()>48)throw new IllegalArgumentException("Use 1–48 characters for the style name");for(int i=0;i<name.length();i++)if(Character.isISOControl(name.charAt(i)))throw new IllegalArgumentException("Style names cannot contain control characters");return name;}
}
