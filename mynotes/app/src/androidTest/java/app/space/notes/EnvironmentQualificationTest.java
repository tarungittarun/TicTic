package app.space.notes;
import android.content.res.Configuration;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks actual Android resources, not only shell setting values recorded by the harness. */
@RunWith(AndroidJUnit4.class)
public class EnvironmentQualificationTest {
    @Test public void fontScaleMatchesRequestedLane(){float expected=Float.parseFloat(InstrumentationRegistry.getArguments().getString("expectedFont","1.0"));float actual=InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getConfiguration().fontScale;assertEquals(expected,actual,.05f);}
    @Test public void orientationMatchesRequestedLane(){
        String expected=InstrumentationRegistry.getArguments().getString("expectedOrientation","portrait");int wanted=expected.equals("landscape")?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
        java.util.concurrent.atomic.AtomicInteger actual=new java.util.concurrent.atomic.AtomicInteger(),width=new java.util.concurrent.atomic.AtomicInteger(),height=new java.util.concurrent.atomic.AtomicInteger();
        try(androidx.test.core.app.ActivityScenario<NotesActivity> scenario=androidx.test.core.app.ActivityScenario.launch(NotesActivity.class)){
            long deadline=android.os.SystemClock.uptimeMillis()+12000;do{scenario.onActivity(activity->{actual.set(activity.getResources().getConfiguration().orientation);android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);width.set(metrics.widthPixels);height.set(metrics.heightPixels);});if(expected.equals("any") || (actual.get()==wanted && (wanted==Configuration.ORIENTATION_LANDSCAPE?width.get()>height.get():height.get()>width.get())))break;android.os.SystemClock.sleep(40);}while(android.os.SystemClock.uptimeMillis()<deadline);
            if(!expected.equals("any")){assertEquals("Real Notes Activity resources at "+width.get()+"x"+height.get(),wanted,actual.get());assertTrue("Physical display aspect does not match "+expected,wanted==Configuration.ORIENTATION_LANDSCAPE?width.get()>height.get():height.get()>width.get());}else assertTrue(actual.get()==Configuration.ORIENTATION_LANDSCAPE || actual.get()==Configuration.ORIENTATION_PORTRAIT);
        }
    }
    @Test public void nightResourcesMatchRequestedLane(){Configuration c=InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getConfiguration();String expected=InstrumentationRegistry.getArguments().getString("expectedNight","no");assertEquals(expected.equals("yes")?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO,c.uiMode & Configuration.UI_MODE_NIGHT_MASK);}
}
