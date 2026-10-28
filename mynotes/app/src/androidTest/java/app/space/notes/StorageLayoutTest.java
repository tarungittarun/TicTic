package app.space.notes;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks actual laid-out action visibility; explanation scrolls separately from controls. */
@RunWith(AndroidJUnit4.class)
public class StorageLayoutTest {
    private TextView find(View root,String label) {
        if(root instanceof TextView && (((TextView)root).getText().toString().equals(label) || UiActions.matches(root,label)))return (TextView)root;
        if(root instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++) { TextView result=find(group.getChildAt(i),label);if(result!=null)return result; }
        }
        return null;
    }
    @Test public void storageActionsRemainVisibleBelowScrollableExplanation() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Intent intent=new Intent(instrumentation.getTargetContext(),NotesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity=instrumentation.startActivitySync(intent);
        try {
            // Wait for library-load completion via UI condition, not a fixed sleep.
            CountDownLatch loaded=new CountDownLatch(1);
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if(find(activity.getWindow().getDecorView(),"Library")!=null)loaded.countDown();
                    else activity.getWindow().getDecorView().postDelayed(this,50);
                }
            });
            assertTrue("Library should load",loaded.await(10,TimeUnit.SECONDS));
            instrumentation.runOnMainSync(()->{TextView settings=find(activity.getWindow().getDecorView(),"Settings");UiActions.reveal(settings);settings.performClick();});
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->find(activity.getWindow().getDecorView(),"Storage & recovery").performClick());instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->{
                View decor=activity.getWindow().getDecorView();
                for(String label:new String[]{"Choose folder","Open library","Backups","Use device storage","Back"}) {
                    TextView action=find(decor,label);assertNotNull(action);UiActions.reveal(action);
                    Rect bounds=new Rect();assertTrue(label+" visible",action.getGlobalVisibleRect(bounds));
                    assertTrue(label+" fully visible after scrolling",bounds.height()>=action.getHeight() && bounds.width()>=action.getWidth());
                    assertTrue(label+" inside window",bounds.bottom<=decor.getHeight());
                }
            });
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
}
