package app.space.notes.runtime;

import android.view.View;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.NotesActivity;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/** Real attached views: one profile enables app motion; others exercise reduced motion. */
@RunWith(AndroidJUnit4.class)
public class NotesMotionTest {
 @Test public void entryLiftAndSettlementHonorActualSystemAnimationSetting()throws Exception{
  boolean enabled=InstrumentationRegistry.getArguments().getString("expectedMotion","0").equals("1");
  assertEquals("Actual Android animator setting must match the qualification profile",enabled,NotesMotion.enabled());
  AtomicReference<View> target=new AtomicReference<>();
  try(ActivityScenario<NotesActivity> scenario=ActivityScenario.launch(NotesActivity.class)){
   scenario.onActivity(activity->{FrameLayout surface=new FrameLayout(activity);View view=new View(activity);surface.addView(view,new FrameLayout.LayoutParams(160,160));activity.setContentView(surface);target.set(view);NotesMotion.enter(view);assertEquals(enabled?.75f:1f,view.getAlpha(),.001f);});
   Thread.sleep(450);
   scenario.onActivity(activity->{View view=target.get();assertEquals(1f,view.getAlpha(),.001f);assertEquals(0f,view.getTranslationY(),.001f);NotesMotion.lift(view);});
   Thread.sleep(450);
   scenario.onActivity(activity->{View view=target.get();assertEquals(enabled?1.025f:1f,view.getScaleX(),.001f);assertEquals(enabled?.8f:1f,view.getAlpha(),.001f);NotesMotion.settle(view);assertEquals(1f,view.getScaleX(),.001f);assertEquals(1f,view.getScaleY(),.001f);assertEquals(1f,view.getAlpha(),.001f);});
  }
 }
 @SuppressWarnings({"rawtypes","unchecked"}) @Test public void selectionRefreshReusesUnchangedImagePreviewInsteadOfRestartingDecode(){
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();NotesBoard board=new NotesBoard(context,false,new NotesBoard.Listener(){public void open(app.space.notes.domain.Note n){}public void select(app.space.notes.domain.Note n){}},true);android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(8,8,android.graphics.Bitmap.Config.ARGB_8888);app.space.notes.domain.NoteImage image;try{image=NoteImageFiles.encode(pixels,"Preview");}catch(Exception e){throw new AssertionError(e);}finally{pixels.recycle();}app.space.notes.domain.Note n=app.space.notes.domain.Note.create(1).images(java.util.Arrays.asList(image),2);board.show(java.util.Arrays.asList(n),java.util.Collections.emptySet(),false);androidx.recyclerview.widget.RecyclerView.Adapter adapter=board.getAdapter();androidx.recyclerview.widget.RecyclerView.ViewHolder holder=adapter.onCreateViewHolder(board,1);adapter.onBindViewHolder(holder,0);android.widget.FrameLayout preview=(android.widget.FrameLayout)((android.view.ViewGroup)holder.itemView).getChildAt(1);View before=preview.getChildAt(0);org.junit.Assert.assertNotNull(before);board.show(java.util.Arrays.asList(n),java.util.Collections.singleton(n.id),false);adapter.onBindViewHolder(holder,0);org.junit.Assert.assertSame(before,preview.getChildAt(0));});
 }
}
