package app.space.notes;

import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import app.space.notes.domain.Note;
import app.space.notes.domain.NoteMerge;
import app.space.notes.domain.ChecklistAppearance;
import app.space.notes.domain.DocumentContent;
import app.space.notes.runtime.BackupCodec;
import app.space.notes.runtime.LocalBackupFiles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BackupCodecTest {
    private Note note() {return new Note("id","नोट","ಕನ್ನಡ",1,2,100,true,true,11);}
    @Test public void allTextMetadataRoundTrips() throws Exception {
        List<Note> read=BackupCodec.decode(BackupCodec.encode(Arrays.asList(note())));
        assertEquals(1,read.size());Note n=read.get(0);assertEquals("id",n.id);
        assertEquals("नोट",n.title);assertEquals("ಕನ್ನಡ",n.body);assertEquals(1,n.createdAt);
        assertEquals(2,n.updatedAt);assertEquals(100,n.deletedAt);assertTrue(n.pinned);assertTrue(n.archived);assertEquals(11,n.color);
    }
    @Test public void checklistJsonRoundTripPreservesStatesAndPlainProjection() throws Exception {
        Note legacy=Note.create(1).withId("legacy-checklist").list("Legacy shopping",Arrays.asList(
            new Note.Item("legacy-milk","Milk",true,1),new Note.Item("legacy-bread","Bread",false,0)),true,2);
        Note.Item done=new Note.Item("canonical-milk","Milk",true,1,Arrays.asList(new Note.Mark("bold",0,4)));
        Note.Item open=new Note.Item("canonical-bread","Bread",false,2);
        DocumentContent.Block checklist=DocumentContent.Block.checklist("canonical-list","Canonical shopping",
            Arrays.asList(done,open),true,new ChecklistAppearance(ChecklistAppearance.Glyph.STAR,5));
        Note canonical=Note.create(3).withId("canonical-checklist")
            .withContent(new DocumentContent(Arrays.asList(checklist)),4);

        byte[] bytes=BackupCodec.encode(Arrays.asList(legacy,canonical));
        JSONObject envelope=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        assertTrue(envelope.has("sha256"));
        JSONObject library=new JSONObject(envelope.getString("payload"));
        JSONObject savedCanonical=library.getJSONArray("notes").getJSONObject(1);
        assertTrue(savedCanonical.has("content"));assertFalse(savedCanonical.has("body"));
        JSONObject savedList=savedCanonical.getJSONObject("content").getJSONArray("blocks").getJSONObject(0);
        assertEquals("canonical-list",savedList.getString("id"));
        assertEquals("Canonical shopping",savedList.getString("name"));
        assertEquals("CHECKLIST",savedList.getString("kind"));assertTrue(savedList.getBoolean("hideChecked"));
        assertEquals("star",savedList.getString("glyph"));assertEquals(5,savedList.getInt("themeColor"));
        JSONArray rows=savedList.getJSONArray("rows");assertEquals(2,rows.length());
        assertEquals("canonical-milk",rows.getJSONObject(0).getString("id"));
        assertEquals("Milk",rows.getJSONObject(0).getString("text"));assertTrue(rows.getJSONObject(0).getBoolean("checked"));
        assertEquals(1,rows.getJSONObject(0).getInt("depth"));assertEquals("bold",rows.getJSONObject(0).getJSONArray("marks").getJSONObject(0).getString("type"));
        assertEquals("canonical-bread",rows.getJSONObject(1).getString("id"));
        assertEquals("Bread",rows.getJSONObject(1).getString("text"));assertFalse(rows.getJSONObject(1).getBoolean("checked"));
        assertEquals(2,rows.getJSONObject(1).getInt("depth"));

        List<Note> restored=BackupCodec.decode(bytes);
        assertEquals(2,restored.size());assertTrue(NoteMerge.same(legacy,restored.get(0)));
        assertTrue(NoteMerge.same(canonical,restored.get(1)));
        assertEquals(legacy.plainText(),restored.get(0).plainText());
        assertEquals(canonical.plainText(),restored.get(1).plainText());
        assertTrue(restored.get(0).hideChecked);assertEquals("legacy-milk",restored.get(0).items.get(0).id);
        assertTrue(restored.get(0).items.get(0).checked);assertEquals(1,restored.get(0).items.get(0).depth);
        assertEquals("canonical-list",restored.get(1).content.block("canonical-list").id);
        assertEquals(new ChecklistAppearance(ChecklistAppearance.Glyph.STAR,5),restored.get(1).content.block("canonical-list").checklistAppearance);
    }
    @Test public void checksumTamperingRejected() throws Exception {
        JSONObject root=new JSONObject(new String(BackupCodec.encode(Arrays.asList(note())),StandardCharsets.UTF_8));
        root.put("sha256","wrong");
        try {BackupCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("must reject");}
        catch(IOException expected) { }
    }
    @Test public void futureFormatRejected() throws Exception {
        JSONObject root=new JSONObject(new String(BackupCodec.encode(Arrays.asList(note())),StandardCharsets.UTF_8));
        root.put("version",2);
        try {BackupCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("must reject");}
        catch(IOException expected) { }
    }
    @Test public void truncatedBackupRejected() throws Exception {
        try {BackupCodec.decode("{".getBytes(StandardCharsets.UTF_8));fail("must reject");}
        catch(org.json.JSONException | java.io.IOException expected) { }
    }
    @Test public void boundedReadRejectsOversize() throws Exception {
        try {BackupCodec.read(new java.io.InputStream(){int remaining=BackupCodec.MAX_BYTES+1;public int read(){if(remaining--<=0)return -1;return 0;}public int read(byte[] b,int offset,int length){if(remaining<=0)return -1;int count=Math.min(length,remaining);java.util.Arrays.fill(b,offset,offset+count,(byte)0);remaining-=count;return count;}});fail("must bound memory");}
        catch(IOException expected) { }
    }
    @Test public void providerMemoryPressureBecomesRecoverableIOException() throws Exception {
        try {BackupCodec.read(new java.io.InputStream(){public int read(){throw new OutOfMemoryError("synthetic pressure");}});fail("must report a recoverable failure");}
        catch(IOException expected) {assertTrue(expected.getCause() instanceof OutOfMemoryError);assertTrue(expected.getMessage().contains("current library unchanged"));}
    }
    @Test public void duplicateIdsRejectedBeforeExport() throws Exception {
        try {BackupCodec.encode(Arrays.asList(note(),note()));fail("must reject");}
        catch(IOException expected) { }
    }
    @Test public void noteCountMismatchRejected() throws Exception {
        JSONObject root=new JSONObject(new String(BackupCodec.encode(Arrays.asList(note())),StandardCharsets.UTF_8));
        root.put("noteCount",50);
        try {BackupCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("must reject");}
        catch(IOException expected) { }
    }
    @Test public void cloudBackupIsRefused() throws Exception {
        try {
            LocalBackupFiles.read(InstrumentationRegistry.getInstrumentation().getTargetContext().getContentResolver(),Uri.parse("content://cloud.example/backup.json"));
            fail("must be local only");
        }catch(IOException expected) { }
    }
    @Test public void restoredRecordingLinksKeepContentNamesThroughBackupCodec() throws Exception {
        String id="00000000-0000-0000-0000-000000000001",hash=String.join("",java.util.Collections.nCopies(64,"a")),otherHash=String.join("",java.util.Collections.nCopies(64,"b"));
        Note source=Note.create(1).withId(app.space.notes.domain.VoiceTranscripts.noteId(id)).edit("Named note","Original writing",2).voiceSource(id,3).reminder(9999,4).namedContent("reminder","Call Mum",5).namedContent("diagram","Plan",5);
        app.space.notes.domain.ArchiveManifest manifest=new app.space.notes.domain.ArchiveManifest(2,hash,Arrays.asList(new app.space.notes.domain.ArchiveManifest.Recording(id,1,44,hash,0,null,false)));
        for(boolean canonical:new boolean[]{false,true}){
            Note seed=canonical?source.withContent(app.space.notes.domain.DocumentContent.fromLegacy(source),6):source;
            Note decoded=BackupCodec.decode(BackupCodec.encode(Arrays.asList(seed))).get(0);
            for(boolean conflict:new boolean[]{false,true}){
                app.space.notes.domain.ArchiveMerge.Plan restore=app.space.notes.domain.ArchiveMerge.preview(manifest,java.util.Collections.emptyMap(),java.util.Collections.emptyList(),Arrays.asList(decoded),key->conflict&&key.equals(id)?new app.space.notes.domain.ArchiveMerge.Content(44,otherHash,null):null);
                Note restored=BackupCodec.decode(BackupCodec.encode(restore.notes.notes)).get(0);
                assertEquals(seed.contentNames,restored.contentNames);assertEquals(seed.reminderAt,restored.reminderAt);assertEquals(seed.body,restored.body);assertEquals(seed.content==null?null:seed.content.fingerprint(),restored.content==null?null:restored.content.fingerprint());assertEquals("Call Mum",restored.contentName("reminder",""));assertEquals(Arrays.asList(restore.recordings.get(0).targetId),restored.voiceSources);
            }
        }
    }
}
