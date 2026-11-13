package app.space.notes;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.webkit.MimeTypeMap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Captures the real picker Intent without selecting, granting, or modifying any file. */
@RunWith(AndroidJUnit4.class)
public class LocalFilePickerTest {
    @Test public void fullArchivePickersAreLocalOpenableZipDocuments() {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        java.util.List<Intent> captured=new java.util.ArrayList<>();
        Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if(Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction()) || Intent.ACTION_CREATE_DOCUMENT.equals(intent.getAction())){captured.add(new Intent(intent));return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}
                return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try(ActivityScenario<NotesActivity> scenario=ActivityScenario.launch(NotesActivity.class)) {
            scenario.onActivity(activity->{activity.importFullBackup();activity.exportFullBackup();});instrumentation.waitForIdleSync();
            assertEquals("Both full-archive pickers must be intercepted",2,captured.size());Intent input=captured.get(0),output=captured.get(1);
            assertEquals(Intent.ACTION_OPEN_DOCUMENT,input.getAction());assertEquals("*/*",input.getType());assertTrue(input.hasCategory(Intent.CATEGORY_OPENABLE));assertTrue(input.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY,false));
            assertTrue(Arrays.asList(input.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)).contains("application/zip"));
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,output.getAction());assertEquals("application/zip",output.getType());assertTrue(output.hasCategory(Intent.CATEGORY_OPENABLE));assertTrue(output.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY,false));assertTrue(output.getStringExtra(Intent.EXTRA_TITLE).endsWith(".zip"));
        } finally {instrumentation.removeMonitor(monitor);}
    }
    @Test public void localPickerAcceptsThePlatformsCsvMimeAndRetainsExistingFormats() {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        AtomicReference<Intent> captured=new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if(!Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction()))return null;
                captured.set(new Intent(intent));
                return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
            }
        };
        instrumentation.addMonitor(monitor);
        try(ActivityScenario<NotesActivity> scenario=ActivityScenario.launch(NotesActivity.class)) {
            scenario.onActivity(NotesActivity::chooseNoteDocument);
            instrumentation.waitForIdleSync();
            Intent intent=captured.get();assertNotNull("The actual local Open file picker must be intercepted",intent);
            assertEquals("*/*",intent.getType());
            assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE));
            assertTrue(intent.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY,false));
            String[] allowed=intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);assertNotNull(allowed);
            String csv=MimeTypeMap.getSingleton().getMimeTypeFromExtension("csv");assertNotNull(csv);
            assertTrue("Accept platform CSV MIME: "+csv,Arrays.asList(allowed).contains(csv));
            for(String mime:new String[]{"text/csv","text/comma-separated-values","application/csv","text/plain","application/pdf","application/json","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/msword","text/markdown","text/html","application/rtf","text/rtf","application/vnd.oasis.opendocument.text","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.openxmlformats-officedocument.presentationml.presentation","application/vnd.ms-excel","application/vnd.ms-powerpoint"})
                assertTrue("Preserve supported MIME: "+mime,Arrays.asList(allowed).contains(mime));
        } finally {instrumentation.removeMonitor(monitor);}
    }
}
