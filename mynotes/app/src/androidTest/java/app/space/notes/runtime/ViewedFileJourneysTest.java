package app.space.notes.runtime;

import android.content.*;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ViewedFileJourneysTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private byte[] zip(Map<String,String> files)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){for(Map.Entry<String,String> file:files.entrySet()){zip.putNextEntry(new ZipEntry(file.getKey()));zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();}}return bytes.toByteArray();}
    @Test public void continuousPdfHasVerticalRecyclerAndOfflineTextWithoutTouchingOriginal()throws Exception{
        File original=File.createTempFile("original-synthetic-pdf-",".pdf",context().getCacheDir()),copy=File.createTempFile("viewed-synthetic-pdf-",".pdf",context().getCacheDir());LocalPdfPreview.Document document=null;
        try{
            try(android.graphics.pdf.PdfDocument generated=new android.graphics.pdf.PdfDocument()){
                for(int page=1;page<=3;page++){android.graphics.pdf.PdfDocument.Page p=generated.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(600,800,page).create());android.graphics.Paint paint=new android.graphics.Paint();paint.setTextSize(24);p.getCanvas().drawText("Synthetic PDF page "+page,40,80,paint);generated.finishPage(p);}try(OutputStream out=new FileOutputStream(original)){generated.writeTo(out);}
            }
            byte[] before=java.nio.file.Files.readAllBytes(original.toPath());java.nio.file.Files.write(copy.toPath(),before);com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context());document=new LocalPdfPreview.Document(copy);assertEquals(3,document.ratios.length);assertTrue(document.text(1),document.text(1).contains("Synthetic"));
            final LocalPdfPreview.Document owned=document;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{LocalPdfPreview view=new LocalPdfPreview(context(),new NotesDesign(context(),false),owned);androidx.recyclerview.widget.RecyclerView pages=null;for(int i=0;i<view.getChildCount();i++)if(view.getChildAt(i) instanceof androidx.recyclerview.widget.RecyclerView)pages=(androidx.recyclerview.widget.RecyclerView)view.getChildAt(i);assertNotNull(pages);assertEquals(3,pages.getAdapter().getItemCount());assertEquals(androidx.recyclerview.widget.RecyclerView.VERTICAL,((androidx.recyclerview.widget.LinearLayoutManager)pages.getLayoutManager()).getOrientation());view.close();});document=null;
            long until=android.os.SystemClock.elapsedRealtime()+5000;while(copy.exists() && android.os.SystemClock.elapsedRealtime()<until)Thread.sleep(20);assertFalse("Both renderer and text owners must release their temporary file",copy.exists());assertArrayEquals(before,java.nio.file.Files.readAllBytes(original.toPath()));
        }finally{if(document!=null){document.closeText();document.close();}copy.delete();original.delete();}
    }
    @Test public void spreadsheetBeyondEditorLimitKeepsWorkbookOrderAndSparsePositions()throws Exception{
        Map<String,String> files=new LinkedHashMap<>();files.put("xl/workbook.xml","<workbook xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"First visible\" r:id=\"r2\"/><sheet name=\"Large\" r:id=\"r1\"/></sheets></workbook>");
        files.put("xl/_rels/workbook.xml.rels","<Relationships><Relationship Id=\"r1\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"r2\" Target=\"worksheets/sheet2.xml\"/></Relationships>");
        files.put("xl/worksheets/sheet2.xml","<worksheet><sheetData><row r=\"2\"><c r=\"C2\" t=\"inlineStr\"><is><t>भारत ಭಾರತ</t></is></c></row></sheetData></worksheet>");
        StringBuilder large=new StringBuilder("<worksheet><sheetData>");for(int row=1;row<=600;row++){large.append("<row r=\"").append(row).append("\"><c r=\"A").append(row).append("\" t=\"inlineStr\"><is><t>");for(int n=0;n<220;n++)large.append('x');large.append("</t></is></c></row>");}large.append("</sheetData></worksheet>");files.put("xl/worksheets/sheet1.xml",large.toString());
        SpreadsheetFile book=SpreadsheetFile.xlsx(zip(files));assertTrue(book.characters>100000);assertEquals(601,book.cells);assertEquals("First visible",book.sheets.get(0).name);assertEquals(3,book.sheets.get(0).columnCount);assertNull(book.sheets.get(0).cell(2,1));assertEquals("भारत ಭಾರತ",book.sheets.get(0).cell(2,3).value);assertEquals(600,book.sheets.get(1).rowCount);
    }
    @Test public void quotedCsvRetainsMultilineValuesAndNeverEvaluatesFormula()throws Exception{
        SpreadsheetFile book=SpreadsheetFile.csv("Name,Value\r\n\"two\nlines\",\"say \"\"yes\"\"\"\r\n=1+2,3\n");SpreadsheetFile.Sheet sheet=book.sheets.get(0);assertEquals(3,sheet.rowCount);assertEquals("two\nlines",sheet.cell(2,1).value);assertEquals("say \"yes\"",sheet.cell(2,2).value);assertEquals("=1+2",sheet.cell(3,1).value);
        try{SpreadsheetFile.csv("\"unfinished");fail("Malformed quotes must fail explicitly");}catch(IOException expected){}
    }
    @Test public void spreadsheetTraversalAndEntityDeclarationsFailClosed()throws Exception{
        Map<String,String> entries=new LinkedHashMap<>();entries.put("../escape.xml","untrusted");try{SpreadsheetFile.xlsx(zip(entries));fail("Traversal must be rejected before parsing");}catch(IOException expected){}
        entries.clear();entries.put("xl/_rels/workbook.xml.rels","<!DOCTYPE x [<!ENTITY y SYSTEM 'file:///private'>]><Relationships/>");try{SpreadsheetFile.xlsx(zip(entries));fail("Entity declarations must be rejected");}catch(IOException expected){}
    }
    @Test public void viewedIndexFindsRealEnglishAndIndicPrefixesAndClearingNeverTouchesOriginals()throws Exception{
        File root=new File(context().getCacheDir(),"viewed-index-test-"+UUID.randomUUID());assertTrue(root.mkdir());Context isolated=new ContextWrapper(context()){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return root;}};
        File original=new File(root,"original.txt");java.nio.file.Files.write(original.toPath(),new byte[]{1,2,3});Uri uri=Uri.parse("content://com.android.externalstorage.documents/document/primary%3Aviewed-fixture.txt");
        try(ViewedFiles index=new ViewedFiles(isolated)){index.remember(uri,"Fixture","text/plain","notes भारत ಭಾರತ हिंदी 😊 %");assertEquals(1,index.search("not").size());assertEquals(1,index.search("ಭಾ").size());assertEquals(1,index.search("हिं").size());assertEquals(1,index.search("😊").size());assertEquals(1,index.search("%").size());assertEquals(0,index.search("missing").size());index.clearIndex();assertTrue(index.search("").isEmpty());assertArrayEquals(new byte[]{1,2,3},java.nio.file.Files.readAllBytes(original.toPath()));}finally{remove(root);}
    }
    @Test public void copyToSameDocumentRejectsBeforeOpeningAnythingForWrite()throws Exception{
        Uri uri=Uri.parse("content://com.android.externalstorage.documents/document/primary%3Anever-write-original.txt");try{ViewedFileCopy.write(context(),uri,uri);fail("Never truncate original URI");}catch(IOException expected){assertTrue(expected.getMessage().contains("different destination"));}
    }
    private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
}
