package app.space.notes.runtime;
import android.graphics.Bitmap;
import android.widget.ImageView;
import app.space.notes.domain.NoteImage;
/** Decode bounded image pixels away from the UI; never draw a recycled bitmap after detach. */
final class NoteImageView extends ImageView {
 private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();private volatile long generation;private Bitmap bitmap;private final NoteImage image;
 NoteImageView(android.content.Context c,NoteImage image){super(c);this.image=image;setScaleType(ScaleType.FIT_CENTER);setContentDescription(image.name);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();long ticket=++generation;worker.execute(()->{if(ticket!=generation)return;Bitmap decoded=null;try{decoded=NoteImageFiles.decode(image,768);}catch(Exception | OutOfMemoryError invalid){}final Bitmap value=decoded;post(()->{if(ticket!=generation || !isAttachedToWindow()){if(value!=null)value.recycle();return;}bitmap=value;setImageBitmap(value);if(value==null)setContentDescription(image.name+" · preview unavailable; image data retained");});});}
 @Override protected void onDetachedFromWindow(){generation++;setImageDrawable(null);if(bitmap!=null){bitmap.recycle();bitmap=null;}super.onDetachedFromWindow();}
}
