package app.space.notes.runtime;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HandbookQualificationTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    @Test public void illustratedHandbookExactBytesRenderAll116PagesAndHaveSelectableText()throws Exception{
        File copy=File.createTempFile("handbook-reader-",".pdf",context().getCacheDir());LocalPdfPreview.Document document=null;
        try{android.content.res.AssetManager assets=InstrumentationRegistry.getInstrumentation().getContext().getAssets();java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");try(InputStream in=assets.open("guide/handbook.pdf");OutputStream out=new FileOutputStream(copy)){byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;){out.write(buffer,0,n);digest.update(buffer,0,n);}}
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));String manifest;try(InputStream in=assets.open("guide/manifest.json")){manifest=new String(LocalOpenFile.bounded(in,100000),StandardCharsets.UTF_8);}org.json.JSONObject expected=new org.json.JSONObject(manifest);assertEquals(expected.getString("sha256"),hash.toString());assertEquals(expected.getLong("bytes"),copy.length());com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context());document=new LocalPdfPreview.Document(copy);assertEquals(116,document.ratios.length);
            android.graphics.Bitmap pageBitmap=android.graphics.Bitmap.createBitmap(120,170,android.graphics.Bitmap.Config.ARGB_8888);try{for(int i=0;i<116;i++){try(android.graphics.pdf.PdfRenderer.Page page=document.renderer.openPage(i)){pageBitmap.eraseColor(android.graphics.Color.WHITE);page.render(pageBitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}String text=document.text(i);assertTrue("Page "+(i+1)+" must have a real embedded text layer",text.length()>200);if((i&15)==15){System.gc();Runtime runtime=Runtime.getRuntime();android.util.Log.i("NotesPublicBenchmark","handbook_pages="+(i+1)+" heap_used="+(runtime.totalMemory()-runtime.freeMemory())+" heap_max="+runtime.maxMemory());}}}finally{pageBitmap.recycle();}
            PdfTextLayer layer=document.selection(0);assertTrue(layer.selected(0,layer.glyphs.size()-1).contains("Your notes"));assertTrue(document.text(115).toLowerCase(java.util.Locale.ROOT).contains("keyboard"));
            final LocalPdfPreview.Document owned=document;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{LocalPdfPreview preview=new LocalPdfPreview(context(),new NotesDesign(context(),false),owned);android.os.Bundle viewport=new android.os.Bundle();viewport.putInt("page",115);viewport.putFloatArray("zoom",new float[]{1,.5f,.5f,0,0});viewport.putString("query","keyboard");preview.restoreViewport(viewport);assertEquals(115,preview.savedViewport().getInt("page"));preview.close();});document=null;
        }finally{if(document!=null){document.closeText();document.close();}copy.delete();}
    }
}
