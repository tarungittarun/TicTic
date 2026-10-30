package app.space.notes;

import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import app.space.notes.domain.Note;
import app.space.notes.runtime.BackupCodec;
import app.space.notes.runtime.LocalBackupFiles;
import static org.junit.Assert.assertEquals;
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
}
