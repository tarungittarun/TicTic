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
   scenario.onActivity(activity->{FrameLayout surface=new FrameLayout(activity);View view=new View(activity);surface.addView(view,new FrameLayout.LayoutParams(160,160));activity.setContentView(surface);target.set(view);NotesMotion.enter(view);assertEquals("Opening must not slide content",0f,view.getTranslationY(),.001f);assertEquals(enabled?.75f:1f,view.getAlpha(),.001f);});
   Thread.sleep(450);
   scenario.onActivity(activity->{View view=target.get();assertEquals(1f,view.getAlpha(),.001f);assertEquals(0f,view.getTranslationY(),.001f);NotesMotion.lift(view);});
   Thread.sleep(450);
   scenario.onActivity(activity->{View view=target.get();assertEquals("Dragging must not resize content",1f,view.getScaleX(),.001f);assertEquals(enabled?.9f:1f,view.getAlpha(),.001f);NotesMotion.settle(view);assertEquals(1f,view.getScaleX(),.001f);assertEquals(1f,view.getScaleY(),.001f);assertEquals(1f,view.getAlpha(),.001f);});
  }
 }
 @SuppressWarnings({"rawtypes","unchecked"}) @Test public void selectionRefreshReusesUnchangedImagePreviewInsteadOfRestartingDecode(){
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();NotesBoard board=new NotesBoard(context,false,new NotesBoard.Listener(){public void open(app.space.notes.domain.Note n){}public void select(app.space.notes.domain.Note n){}},true);android.graphics.Bitmap pixels=android.graphics.Bitmap.createBitmap(8,8,android.graphics.Bitmap.Config.ARGB_8888);app.space.notes.domain.NoteImage image;try{image=NoteImageFiles.encode(pixels,"Preview");}catch(Exception e){throw new AssertionError(e);}finally{pixels.recycle();}app.space.notes.domain.Note n=app.space.notes.domain.Note.create(1).images(java.util.Arrays.asList(image),2);board.show(java.util.Arrays.asList(n),java.util.Collections.emptySet(),false);androidx.recyclerview.widget.RecyclerView.Adapter adapter=board.getAdapter();androidx.recyclerview.widget.RecyclerView.ViewHolder holder=adapter.onCreateViewHolder(board,1);adapter.onBindViewHolder(holder,0);android.widget.FrameLayout preview=(android.widget.FrameLayout)((android.view.ViewGroup)holder.itemView).getChildAt(1);View before=preview.getChildAt(0);org.junit.Assert.assertNotNull(before);board.show(java.util.Arrays.asList(n),java.util.Collections.singleton(n.id),false);adapter.onBindViewHolder(holder,0);org.junit.Assert.assertSame(before,preview.getChildAt(0));});
 }
 @Test public void dragClearDuringRealRecyclerLayoutDefersInvalidationAndRejectsCancelledOrder()throws Exception{
  java.util.concurrent.CountDownLatch completed=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicInteger changes=new java.util.concurrent.atomic.AtomicInteger();
  AtomicReference<NotesBoard> ref=new AtomicReference<>();
  try(ActivityScenario<NotesActivity> scenario=ActivityScenario.launch(NotesActivity.class)){
   scenario.onActivity(activity->{NotesBoard board=new NotesBoard(activity,false,new NotesBoard.Listener(){public void open(app.space.notes.domain.Note note){}public void select(app.space.notes.domain.Note note){changes.incrementAndGet();}public boolean canReorder(){return true;}public void reorder(java.util.List<String> ids){changes.incrementAndGet();}},false);ref.set(board);
    board.show(java.util.Arrays.asList(app.space.notes.domain.Note.create(1).edit("First","Keep",2),app.space.notes.domain.Note.create(3).edit("Second","Keep too",4)),java.util.Collections.emptySet(),false);
    board.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(activity){boolean exercised;@Override public void onLayoutChildren(androidx.recyclerview.widget.RecyclerView.Recycler recycler,androidx.recyclerview.widget.RecyclerView.State state){super.onLayoutChildren(recycler,state);if(exercised || getChildCount()==0)return;exercised=true;org.junit.Assert.assertTrue(board.isComputingLayout());try{java.lang.reflect.Field helper=NotesBoard.class.getDeclaredField("dragHelper");helper.setAccessible(true);Object touch=helper.get(board);java.lang.reflect.Field callback=androidx.recyclerview.widget.ItemTouchHelper.class.getDeclaredField("mCallback");callback.setAccessible(true);androidx.recyclerview.widget.ItemTouchHelper.Callback drag=(androidx.recyclerview.widget.ItemTouchHelper.Callback)callback.get(touch);androidx.recyclerview.widget.RecyclerView.ViewHolder holder=board.getChildViewHolder(getChildAt(0));drag.onSelectedChanged(holder,androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG);drag.clearView(board,holder);board.post(()->completed.countDown());}catch(Exception failure){throw new AssertionError(failure);}}});activity.setContentView(board);
   });
   org.junit.Assert.assertTrue("Real layout must invoke the drop completion",completed.await(10,java.util.concurrent.TimeUnit.SECONDS));InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(activity->{assertEquals(2,ref.get().getAdapter().getItemCount());assertEquals("Cancelled/layout-detached drag must not save or select",0,changes.get());});
  }
 }
 @Test public void colourlessDiagramDoesNotPaintOverCanvas()throws Exception{
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(800,1000,android.graphics.Bitmap.Config.ARGB_8888);try{android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);int paper=android.graphics.Color.MAGENTA;canvas.drawColor(paper);app.space.notes.domain.Diagram diagram=app.space.notes.domain.Diagram.empty().put(new app.space.notes.domain.Diagram.Node("blank","process","",20,20,200,100,0));DiagramRenderer.draw(canvas,diagram,false,null);assertEquals("Colourless is no fill, not white",paper,bitmap.getPixel(100,60));}finally{bitmap.recycle();}});
 }

}
