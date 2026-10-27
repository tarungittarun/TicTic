package app.space.notes;
import android.content.Context;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.runtime.Diagnostics;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class DiagnosticsJourneysTest {
 private Context context;
 @Before public void setup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();Diagnostics.initialize(context);Diagnostics.setEnabled(false);Diagnostics.clear();}
 @After public void cleanup(){Diagnostics.setEnabled(false);Diagnostics.clear();}
 private String snapshot() throws Exception{File file=new File(context.getCacheDir(),"diagnostic-test.txt");CountDownLatch done=new CountDownLatch(1);AtomicReference<Exception> error=new AtomicReference<>();Diagnostics.export(context,Uri.fromFile(file),done::countDown,e->{error.set(e);done.countDown();});assertTrue(done.await(10,TimeUnit.SECONDS));assertNull(error.get());byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());assertTrue(file.delete());return new String(bytes,"UTF-8");}
 @Test public void disabledByDefaultEnablePersistsAndDisableStopsCollection() throws Exception{assertFalse(Diagnostics.enabled());Diagnostics.record("must.not.appear","off");assertEquals(0,Diagnostics.bytes());Diagnostics.setEnabled(true);Diagnostics.record("test.enabled","safe counters only");assertTrue(snapshot().contains("test.enabled"));Diagnostics.initialize(context);assertTrue(Diagnostics.enabled());Diagnostics.setEnabled(false);Diagnostics.record("must.not.appear","off again");String report=snapshot();assertFalse(report.contains("must.not.appear"));assertTrue(report.contains("environment"));assertTrue(report.contains("sdk"));assertTrue(report.contains("model"));}
 @Test public void clearInvalidatesQueuedEventsAndDeletesRetainedFiles() throws Exception{Diagnostics.setEnabled(true);for(int i=0;i<200;i++)Diagnostics.record("test.old","old");Diagnostics.clear();assertEquals(0,Diagnostics.bytes());assertFalse(snapshot().contains("test.old"));}
 @Test public void boundedRotationAndExportPreserveValidJsonRecords() throws Exception{Diagnostics.setEnabled(true);String detail=new String(new char[1000]).replace('\0','x');for(int batch=0;batch<16;batch++){for(int i=0;i<100;i++)Diagnostics.record("test.rotation",detail);snapshot();}assertTrue(Diagnostics.bytes()>0);assertTrue(Diagnostics.bytes()<=1024*1024);String text=snapshot();int lines=0;for(String line:text.split("\n")){if(line.startsWith("{")){new org.json.JSONObject(line);lines++;}}assertTrue(lines>0);}
 @Test public void privateDynamicControlCaptionsAreNotRecorded(){assertEquals("content-control",Diagnostics.controlName("PRIVATE_CUSTOM_LABEL"));assertEquals("Share",Diagnostics.controlName("Share"));assertEquals("+ Text",Diagnostics.controlName("+ Text"));}
 @Test public void failureStacksExcludePrivateExceptionMessage() throws Exception{Diagnostics.setEnabled(true);Diagnostics.failure("test.failure",new IOException("PRIVATE_NOTE_AND_PATH"));String text=snapshot();assertTrue(text.contains("IOException"));assertTrue(text.contains("failureStacksExcludePrivateExceptionMessage"));assertFalse(text.contains("PRIVATE_NOTE_AND_PATH"));}
}
