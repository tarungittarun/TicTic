package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Looper;
import java.io.IOException;

/** One explicit page, on the PDF renderer's serial executor. Never saves/changes a file. */
final class PdfPageOcr {
    private PdfPageOcr() {}
    static OfflineOcr.Result recognize(Context context,LocalPdfPreview.Document document,int index,String language,OfflineOcr.Cancellation cancellation)throws Exception{
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("PDF OCR must run off the UI thread");
        cancellation.check();
        if(index<0 || index>=document.ratios.length)throw new IOException("Page is no longer available; original unchanged");
        Bitmap bitmap=null;
        try{
            // Caller serializes this with preview rendering and document.close().
            // Release PdfRenderer.Page before invoking Tesseract or any UI callback.
            try(PdfRenderer.Page page=document.renderer.openPage(index)){
                int width=page.getWidth(),height=page.getHeight();
                if(width<1 || height<1)throw new IOException("Unsupported PDF page dimensions");
                double scale=Math.min(4,Math.min(2400.0/Math.max(width,height),Math.sqrt(4000000.0/((long)width*height))));
                bitmap=Bitmap.createBitmap(Math.max(1,(int)(width*scale)),Math.max(1,(int)(height*scale)),Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);cancellation.check();
                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            }
            cancellation.check();
            return OfflineOcr.recognizeBitmap(context,bitmap,language,cancellation);
        }finally{if(bitmap!=null)bitmap.recycle();}
    }
}
