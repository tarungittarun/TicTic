package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Native preview gestures, annotation rendering/serialization, and pixel privacy checks. */
@RunWith(AndroidJUnit4.class)
public class ImageEditViewTest {
    private static final int MAGIC=0x4d4e4931;
    private final android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private Context context(){return instrumentation.getTargetContext();}

    @Test public void pinchZoomAndPanWorkWithoutPaintingWhileAStrokeToolIsSelected(){
        instrumentation.runOnMainSync(()->{
            Bitmap source=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);new Canvas(source).drawColor(Color.WHITE);
            ImageEditView view=new ImageEditView(context());view.layout(0,0,500,400);view.loaded(source,new app.space.notes.domain.ImageEditPlan(200,100));
            float centerX=view.previewOriginX()+100*view.previewScale(),centerY=view.previewOriginY()+50*view.previewScale();long down=android.os.SystemClock.uptimeMillis();
            assertTrue(sendSingle(view,down,down,MotionEvent.ACTION_DOWN,centerX-20,centerY));
            assertTrue(sendMulti(view,down,down+20,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[]{centerX-20,centerX+20},new float[]{centerY,centerY}));
            assertTrue(sendMulti(view,down,down+40,MotionEvent.ACTION_MOVE,new float[]{centerX-60,centerX+60},new float[]{centerY,centerY}));
            assertTrue("Pinch must scale the image, not add an annotation",view.zoomScale()>1.2f);assertEquals(0,view.annotationCount());
            assertTrue(sendMulti(view,down,down+60,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[]{centerX-60,centerX+60},new float[]{centerY,centerY}));
            assertTrue(sendSingle(view,down,down+80,MotionEvent.ACTION_UP,centerX+60,centerY));float before=view.panOffsetX();assertTrue(view.zoomScale()>1);
            long pan=down+100;assertTrue(sendSingle(view,pan,pan,MotionEvent.ACTION_DOWN,centerX,centerY));assertTrue(sendSingle(view,pan,pan+20,MotionEvent.ACTION_MOVE,centerX+35,centerY+10));assertTrue(sendSingle(view,pan,pan+40,MotionEvent.ACTION_UP,centerX+35,centerY+10));assertTrue(Math.abs(before-view.panOffsetX())>.01f);
            view.drawingTool(ImageEditView.Tool.PEN,Color.RED);float beforePenPinch=view.zoomScale(),x=view.previewOriginX()+100*view.previewScale(),y=view.previewOriginY()+50*view.previewScale();long mark=pan+60;
            assertTrue(sendSingle(view,mark,mark,MotionEvent.ACTION_DOWN,x,y));
            assertTrue(sendMulti(view,mark,mark+20,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[]{x-12,x+12},new float[]{y,y}));
            assertTrue(sendMulti(view,mark,mark+40,MotionEvent.ACTION_MOVE,new float[]{x-28,x+28},new float[]{y,y}));
            assertTrue(sendMulti(view,mark,mark+60,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[]{x-28,x+28},new float[]{y,y}));
            assertTrue(sendSingle(view,mark,mark+80,MotionEvent.ACTION_UP,x+28,y));assertTrue("Pinching with PEN selected still zooms the image",view.zoomScale()>beforePenPinch+.1f);assertEquals("Two-finger navigation cancels the in-progress pen stroke",0,view.annotationCount());
            float beforeTwoFingerX=view.panOffsetX(),beforeTwoFingerY=view.panOffsetY();long move=mark+100;
            assertTrue(sendMulti(view,move,move,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[]{x-20,x+20},new float[]{y,y}));
            assertTrue(sendMulti(view,move,move+20,MotionEvent.ACTION_MOVE,new float[]{x+10,x+50},new float[]{y+15,y+15}));
            assertTrue(sendMulti(view,move,move+40,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new float[]{x+10,x+50},new float[]{y+15,y+15}));
            assertTrue(sendSingle(view,move,move+60,MotionEvent.ACTION_UP,x+10,y+15));assertTrue("Two-finger translation pans the zoomed source",Math.abs(beforeTwoFingerX-view.panOffsetX())>5 || Math.abs(beforeTwoFingerY-view.panOffsetY())>5);assertEquals(0,view.annotationCount());
            view.fitToScreen();assertEquals(1f,view.zoomScale(),.001f);assertEquals(0f,view.panOffsetX(),.01f);assertEquals(0f,view.panOffsetY(),.01f);
            source.recycle();
        });
    }

    @Test public void textAndRedactionPersistAsBoundedAnnotationsAndRenderWithoutChangingSource(){
        instrumentation.runOnMainSync(()->{
            Bitmap source=Bitmap.createBitmap(200,120,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);canvas.drawColor(Color.WHITE);Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);ink.setColor(Color.BLUE);canvas.drawRect(80,45,145,80,ink);
            ImageEditView view=new ImageEditView(context());view.layout(0,0,500,400);app.space.notes.domain.ImageEditPlan plan=new app.space.notes.domain.ImageEditPlan(200,120);view.loaded(source,plan);
            view.drawingTool(ImageEditView.Tool.REDACT,Color.BLACK);float sx=view.previewOriginX()+80*view.previewScale(),sy=view.previewOriginY()+45*view.previewScale(),ex=view.previewOriginX()+145*view.previewScale(),ey=view.previewOriginY()+80*view.previewScale();long now=android.os.SystemClock.uptimeMillis();
            assertTrue(sendSingle(view,now,now,MotionEvent.ACTION_DOWN,sx,sy));assertTrue(sendSingle(view,now,now+20,MotionEvent.ACTION_MOVE,ex,ey));assertTrue(sendSingle(view,now,now+40,MotionEvent.ACTION_UP,ex,ey));assertEquals(1,view.annotationCount());
            view.drawingTool(ImageEditView.Tool.TEXT,Color.RED);view.drawingWidthDp(14);view.drawingFont(1);view.addText("Popup label",8,8);assertTrue("New text stays selected as a movable editing box",view.textSelected());int[] created=view.annotationState();assertEquals(3,created[1]);assertEquals(2,view.annotationCount());
            view.drawingTool(ImageEditView.Tool.PEN,Color.GREEN);view.drawingTool(ImageEditView.Tool.TEXT,Color.WHITE);float fit=view.previewScale(),selectX=view.previewOriginX()+20*fit,selectY=view.previewOriginY()+16*fit;long select=now+50;assertTrue(sendSingle(view,select,select,MotionEvent.ACTION_DOWN,selectX,selectY));assertTrue(sendSingle(view,select,select+20,MotionEvent.ACTION_UP,selectX,selectY));assertEquals("Selecting existing text loads its color",Color.RED,view.drawingColor());assertEquals("Selecting existing text loads its size",14f,view.drawingWidthDp(),.1f);assertEquals("Selecting existing text loads its font",1,view.drawingFont());view.drawingColor(Color.GREEN);view.drawingWidthDp(8);view.drawingFont(3);assertFalse("Text controls update the selected text annotation",java.util.Arrays.equals(created,view.annotationState()));assertTrue(view.textSelected());view.undo();view.undo();view.undo();assertArrayEquals("Undo restores the text style without losing the text box",created,view.annotationState());assertEquals(Color.RED,view.drawingColor());assertEquals(14f,view.drawingWidthDp(),.1f);assertEquals(1,view.drawingFont());view.drawingWidthDp(64);assertArrayEquals("An oversized text style is rejected without moving or clipping the text box",created,view.annotationState());assertEquals(14f,view.drawingWidthDp(),.1f);
            float scale=view.previewScale(),moveX=view.previewOriginX()+20*scale,moveY=view.previewOriginY()+16*scale;long drag=now+100;
            assertTrue(sendSingle(view,drag,drag,MotionEvent.ACTION_DOWN,moveX,moveY));assertTrue(sendSingle(view,drag,drag+20,MotionEvent.ACTION_MOVE,moveX+18*scale,moveY+8*scale));assertTrue(sendSingle(view,drag,drag+40,MotionEvent.ACTION_UP,moveX+18*scale,moveY+8*scale));int[] moved=view.annotationState();assertFalse("Text-box drag changes its saved position",java.util.Arrays.equals(created,moved));assertTrue(view.textSelected());
            Bitmap output=ImageEditView.render(source,plan,moved);try{assertEquals("Opaque cover is black in the saved pixels",Color.BLACK,output.getPixel(100,60));assertEquals("Source pixels remain untouched before explicit save",Color.BLUE,source.getPixel(100,60));int red=0;for(int y=0;y<50;y++)for(int x=0;x<100;x++){int pixel=output.getPixel(x,y);if(Color.red(pixel)>180 && Color.green(pixel)<100 && Color.blue(pixel)<100)red++;}assertTrue("Text annotation is rasterized into the saved copy",red>0);for(int y=0;y<output.getHeight();y++)for(int x=0;x<output.getWidth();x++)assertNotEquals("Editing selection handles are not baked into the saved image",0xff5577dd,output.getPixel(x,y));}finally{output.recycle();}
            Bitmap copy=source.copy(Bitmap.Config.ARGB_8888,true);ImageEditView restored=new ImageEditView(context());restored.layout(0,0,500,400);restored.loaded(copy,plan);restored.restoreAnnotations(moved);assertEquals("Text style and cover survive preview state restoration",2,restored.annotationCount());assertArrayEquals("Versioned state preserves text font, color, size and position",moved,restored.annotationState());assertFalse("Saved/restored text is not left with editing handles",restored.textSelected());view.undo();assertArrayEquals("Undo restores the earlier movable text position",created,view.annotationState());view.undo();assertEquals("Annotation undo removes only the newest text label",1,view.annotationCount());
            source.recycle();copy.recycle();
        });
    }

    @Test public void rotatedSubpixelRedactionCoversWholeSourcePixelsWithoutChangingOriginal(){
        instrumentation.runOnMainSync(()->{
            Bitmap source=Bitmap.createBitmap(100,60,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);canvas.drawColor(Color.WHITE);Paint blue=new Paint();blue.setColor(Color.BLUE);canvas.drawRect(20,10,40,30,blue);
            app.space.notes.domain.ImageEditPlan rotated=new app.space.notes.domain.ImageEditPlan(100,60).rotateRight();ImageEditView view=new ImageEditView(context());view.layout(0,0,300,240);view.loaded(source,rotated);view.addRedaction(20.2f,10.2f,39.8f,29.8f);
            Bitmap output=ImageEditView.render(source,rotated,view.annotationState());try{assertEquals("Subpixel coverage is rounded outward before rotation",Color.BLACK,output.getPixel(30,20));assertEquals("Pixels outside the redaction remain unchanged",Color.WHITE,output.getPixel(29,20));assertEquals(Color.BLUE,source.getPixel(25,15));}finally{output.recycle();source.recycle();}
        });
    }

    @Test public void versionOneDrawingStateStillLoadsAfterAddingNewTools(){
        instrumentation.runOnMainSync(()->{
            Bitmap source=Bitmap.createBitmap(100,80,Bitmap.Config.ARGB_8888);new Canvas(source).drawColor(Color.WHITE);ImageEditView view=new ImageEditView(context());view.layout(0,0,300,240);app.space.notes.domain.ImageEditPlan plan=new app.space.notes.domain.ImageEditPlan(100,80);view.loaded(source,plan);
            int[] old={MAGIC,1,1,100,80,0,0,0,0,100,80,1,0,Color.RED,Float.floatToIntBits(3f),1,Float.floatToIntBits(12f),Float.floatToIntBits(14f)};
            view.restoreAnnotations(old);assertEquals(1,view.annotationCount());int[] upgraded=view.annotationState();assertEquals(3,upgraded[1]);source.recycle();
        });
    }

    private static boolean sendSingle(ImageEditView view,long down,long time,int action,float x,float y){MotionEvent event=MotionEvent.obtain(down,time,action,x,y,0);try{event.setSource(InputDevice.SOURCE_TOUCHSCREEN);return view.onTouchEvent(event);}finally{event.recycle();}}
    private static boolean sendMulti(ImageEditView view,long down,long time,int action,float[] xs,float[] ys){
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[xs.length];MotionEvent.PointerCoords[] coordinates=new MotionEvent.PointerCoords[xs.length];
        for(int i=0;i<xs.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coordinates[i]=new MotionEvent.PointerCoords();coordinates[i].x=xs[i];coordinates[i].y=ys[i];coordinates[i].pressure=1;coordinates[i].size=1;}
        MotionEvent event=MotionEvent.obtain(down,time,action,xs.length,properties,coordinates,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);try{return view.onTouchEvent(event);}finally{event.recycle();}
    }
}
