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
    @Test public void orientationMatchesRequestedLane(){String expected=InstrumentationRegistry.getArguments().getString("expectedOrientation","portrait");int actual=InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getConfiguration().orientation;if(!expected.equals("any"))assertEquals(expected.equals("landscape")?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT,actual);else assertTrue(actual==Configuration.ORIENTATION_LANDSCAPE || actual==Configuration.ORIENTATION_PORTRAIT);}
    @Test public void nightResourcesMatchRequestedLane(){Configuration c=InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getConfiguration();String expected=InstrumentationRegistry.getArguments().getString("expectedNight","no");assertEquals(expected.equals("yes")?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO,c.uiMode & Configuration.UI_MODE_NIGHT_MASK);}
}
