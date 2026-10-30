package app.space.notes;
import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.*;
import app.space.notes.runtime.StylePresetStore;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class StylePresetStoreTest {
    private final Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final String prefix="style-test-"+UUID.randomUUID()+"-";
    private final Context context=new ContextWrapper(base){@Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(prefix+name,mode);}};
    @After public void cleanup(){base.deleteSharedPreferences(prefix+"mynotes-style-presets");}
    @Test public void presetPersistsPrivatelyWithoutNoteTextOrDestinations()throws Exception{RichText rich=new RichText("Private customer words",Arrays.asList(new Note.Mark("bold",0,22),new Note.Mark("font",0,22,"serif"),new Note.Mark("link",0,22,"https://example.com/private")));StylePresetStore store=new StylePresetStore(context);store.save("Emphasis",TypingStyle.capture(rich,0,22));SharedPreferences preferences=context.getSharedPreferences("mynotes-style-presets",Context.MODE_PRIVATE);assertTrue(preferences.edit().putBoolean("flush-test",true).commit());String raw=preferences.getString("styles","");assertFalse(raw.contains("customer"));assertFalse(raw.contains("example.com"));assertEquals("bold=;font=serif;",new StylePresetStore(context).load().get("Emphasis").encode());assertTrue(new File(base.getApplicationInfo().dataDir,"shared_prefs/"+prefix+"mynotes-style-presets.xml").isFile());store.delete("Emphasis");assertTrue(new StylePresetStore(context).load().isEmpty());}
    @Test public void duplicatesBoundsAndCorruptionNeverOverwriteExistingPresets()throws Exception{StylePresetStore store=new StylePresetStore(context);store.save("First",TypingStyle.normal());try{store.save("first",TypingStyle.normal());fail("Case duplicate accepted");}catch(IOException expected){}for(int i=1;i<16;i++)store.save("Style"+i,TypingStyle.normal());try{store.save("Overflow",TypingStyle.normal());fail("Exceeded preset bound");}catch(IOException expected){}assertEquals(16,store.load().size());SharedPreferences preferences=context.getSharedPreferences("mynotes-style-presets",Context.MODE_PRIVATE);String corrupt="{\"version\":2,\"presets\":[]}";assertTrue(preferences.edit().putString("styles",corrupt).commit());try{store.save("New",TypingStyle.normal());fail("Unknown version overwritten");}catch(IOException expected){}assertEquals(corrupt,preferences.getString("styles",null));}
}
