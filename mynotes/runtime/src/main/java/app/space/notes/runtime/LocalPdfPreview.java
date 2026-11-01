package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

/** Native read-only PDF pages: no WebView, script execution, cloud reader or source writes. */
final class LocalPdfPreview extends LinearLayout implements AutoCloseable {
    static final class Document implements AutoCloseable {
        final File temporary;final ParcelFileDescriptor descriptor;final PdfRenderer renderer;
        Document(File file)throws IOException{temporary=file;ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer pdf;try{pdf=new PdfRenderer(fd);if(pdf.getPageCount()<1 || pdf.getPageCount()>1000){pdf.close();throw new IOException("PDF page count exceeds safe preview limit");}}catch(IOException | RuntimeException error){try{fd.close();}catch(IOException ignored){}throw error;}descriptor=fd;renderer=pdf;}
        @Override public void close(){renderer.close();try{descriptor.close();}catch(IOException ignored){}temporary.delete();}
    }
    static Document open(Context context,Uri source)throws Exception{
        LocalDeviceDocuments.requireReadable(source);File temporary=File.createTempFile("pdf-preview-",".pdf",context.getCacheDir());boolean done=false;try{try(InputStream in=context.getContentResolver().openInputStream(source);OutputStream out=new FileOutputStream(temporary)){if(in==null)throw new IOException("Cannot read PDF");byte[] buffer=new byte[8192];long bytes=0;for(int n;(n=in.read(buffer))!=-1;){if((bytes+=n)>32*1024*1024)throw new IOException("PDF preview is limited to 32 MiB");out.write(buffer,0,n);}}Document document=new Document(temporary);done=true;return document;}finally{if(!done)temporary.delete();}
    }
    private final Document document;private final ExecutorService worker=Executors.newSingleThreadExecutor();private final ImageView image;private final TextView position;private final Button previous,next;private volatile boolean closed;private int page,generation;
    LocalPdfPreview(Context context,NotesDesign design,Document document){super(context);this.document=document;setOrientation(VERTICAL);LinearLayout controls=new LinearLayout(context);controls.setGravity(Gravity.CENTER_VERTICAL);previous=design.textAction("Previous page",()->show(page-1));next=design.textAction("Next page",()->show(page+1));position=design.label("",14,design.ink);controls.addView(previous,new LinearLayout.LayoutParams(0,-2,1));controls.addView(position);controls.addView(next,new LinearLayout.LayoutParams(0,-2,1));addView(controls);image=new ImageView(context);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Read-only PDF page");addView(image,new LinearLayout.LayoutParams(-1,0,1));show(0);}
    private void show(int index){if(closed || index<0 || index>=document.renderer.getPageCount())return;page=index;int token=++generation;previous.setEnabled(index>0);next.setEnabled(index+1<document.renderer.getPageCount());position.setText((index+1)+" / "+document.renderer.getPageCount());worker.execute(()->{Bitmap bitmap=null;try(PdfRenderer.Page source=document.renderer.openPage(index)){double scale=Math.min(2.0,Math.sqrt(2000000.0/Math.max(1L,(long)source.getWidth()*source.getHeight())));int width=Math.max(1,(int)(source.getWidth()*scale)),height=Math.max(1,(int)(source.getHeight()*scale));bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);source.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);Bitmap ready=bitmap;post(()->{if(closed || token!=generation)ready.recycle();else{image.setImageBitmap(ready);image.setContentDescription("PDF page "+(index+1)+" of "+document.renderer.getPageCount());}});}catch(Exception | OutOfMemoryError error){if(bitmap!=null)bitmap.recycle();Diagnostics.failure("pdf.preview.failure",error);post(()->{if(!closed && token==generation)position.setText("Page unavailable · original unchanged");});}});}
    @Override public void close(){if(closed)return;closed=true;generation++;image.setImageDrawable(null);worker.execute(document::close);worker.shutdown();}
}
