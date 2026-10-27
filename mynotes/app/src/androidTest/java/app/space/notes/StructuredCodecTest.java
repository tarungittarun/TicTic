package app.space.notes;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;
import app.space.notes.domain.NoteMerge;
import app.space.notes.runtime.DocumentCodec;
import app.space.notes.runtime.BackupCodec;
import app.space.notes.runtime.TextStyles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class StructuredCodecTest {
    private Note text(){return Note.create(1).rich("नोट","Hello ಕನ್ನಡ",Arrays.asList(new Note.Mark("bold",0,5),new Note.Mark("h1",6,11)),2).properties(8,Arrays.asList("Work","Personal"),2);}
    private Note list(){return Note.create(1).list("Shopping",Arrays.asList(new Note.Item("row1","Milk",false,0),new Note.Item("row2","ಕನ್ನಡ",true,2)),true,2).properties(4,Arrays.asList("Home"),2);}
    @Test public void schema2RetainsRichTextAndLabels() throws Exception {
        Note n=text(),read=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(n))).get(0);
        assertTrue(NoteMerge.same(n,read));assertEquals(2,read.marks.size());assertEquals(2,read.labels.size());
    }
    @Test public void schema2RetainsChecklistIdsChecksAndDepth() throws Exception {
        Note n=list(),read=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(n))).get(0);
        assertTrue(NoteMerge.same(n,read));assertEquals("row2",read.items.get(1).id);assertTrue(read.items.get(1).checked);assertEquals(2,read.items.get(1).depth);assertTrue(read.hideChecked);
    }
    @Test public void backupRestoresBothStructuredKinds() throws Exception {
        List<Note> source=Arrays.asList(text(),list()),read=BackupCodec.decode(BackupCodec.encode(source));
        assertEquals(2,read.size());assertTrue(NoteMerge.same(source.get(0),read.get(0)));assertTrue(NoteMerge.same(source.get(1),read.get(1)));
    }
    @Test public void oldSchema1MigratesWithoutNewProperties() throws Exception {
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(Note.create(1).edit("old","plain",2))),StandardCharsets.UTF_8));
        root.put("version",1);JSONObject n=root.getJSONArray("notes").getJSONObject(0);
        for(String key:new String[]{"checklist","hideChecked","items","marks","labels"})n.remove(key);
        Note migrated=DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals("plain",migrated.body);assertFalse(migrated.checklist);assertTrue(migrated.labels.isEmpty());
        assertEquals(4,new JSONObject(new String(DocumentCodec.encode(Arrays.asList(migrated)),StandardCharsets.UTF_8)).getInt("version"));
    }
    @Test public void outOfRangeMarkRejectsWholeDocument() throws Exception {
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(text())),StandardCharsets.UTF_8));
        root.getJSONArray("notes").getJSONObject(0).getJSONArray("marks").getJSONObject(0).put("end",500);
        try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("invalid marks must fail");}catch(IllegalArgumentException expected){ }
    }
    @Test public void duplicatedRowIdsRejectWholeDocument() throws Exception {
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(list())),StandardCharsets.UTF_8));
        JSONArray rows=root.getJSONArray("notes").getJSONObject(0).getJSONArray("items");rows.getJSONObject(1).put("id","row1");
        try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("duplicate rows must fail");}catch(IllegalArgumentException expected){ }
    }
    @Test public void androidSpanRoundTripRetainsAllStyles() {
        SpannableStringBuilder editable=new SpannableStringBuilder("Hello world");List<Note.Mark> source=new ArrayList<>();
        for(String type:new String[]{"bold","italic","underline","strike","h1","h2"})source.add(new Note.Mark(type,0,5));
        TextStyles.write(editable,source);List<Note.Mark> read=TextStyles.read(editable);assertEquals(6,read.size());
        for(Note.Mark mark:read){assertEquals(0,mark.start);assertEquals(5,mark.end);}
    }
    @Test public void spansFollowInsertedTextBeforeSerialization() {
        SpannableStringBuilder editable=new SpannableStringBuilder("Hello world");TextStyles.write(editable,Arrays.asList(new Note.Mark("bold",6,11)));
        editable.insert(0,"New ");Note.Mark mark=TextStyles.read(editable).get(0);assertEquals(10,mark.start);assertEquals(15,mark.end);
    }
}
