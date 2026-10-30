package app.space.notes;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.text.SpannableStringBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;
import app.space.notes.domain.NoteMerge;
import app.space.notes.runtime.DocumentCodec;
import app.space.notes.runtime.BackupCodec;
import app.space.notes.runtime.TextStyles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class StructuredCodecTest {
    @Test public void headerRoleRoundTripsAndRejectsMalformedInput()throws Exception{
        Note old=Note.create(1);app.space.notes.domain.DocumentContent content=app.space.notes.domain.DocumentContent.fromLegacy(old).insert(1,app.space.notes.domain.DocumentContent.Block.table("t","Table01",app.space.notes.domain.TextTable.empty(2,2).withHeader(true)));Note doc=old.withContent(content,2);byte[] bytes=DocumentCodec.encode(Arrays.asList(doc));Note read=DocumentCodec.decode(bytes).get(0);assertTrue(read.content.block("t").table.hasHeader);assertTrue(NoteMerge.same(doc,read));String raw=new String(bytes,StandardCharsets.UTF_8);assertTrue(raw.contains("\"header\":true"));try{DocumentCodec.decode(raw.replace("\"header\":true","\"header\":\"true\"").getBytes(StandardCharsets.UTF_8));fail("Non-boolean header accepted");}catch(java.io.IOException expected){}
    }
    private Note text(){return Note.create(1).rich("नोट","Hello ಕನ್ನಡ",Arrays.asList(new Note.Mark("bold",0,5),new Note.Mark("h1",6,11)),2).properties(8,Arrays.asList("Work","Personal"),2);}
    private Note list(){return Note.create(1).list("Shopping",Arrays.asList(new Note.Item("row1","Milk",false,0),new Note.Item("row2","ಕನ್ನಡ",true,2)),true,2).properties(4,Arrays.asList("Home"),2);}
    @Test public void schema2RetainsRichTextAndLabels() throws Exception {
        Note n=text(),read=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(n))).get(0);
        assertTrue(NoteMerge.same(n,read));assertEquals(2,read.marks.size());assertEquals(2,read.labels.size());
    }
    @Test public void schema2RetainsChecklistIdsChecksAndDepth() throws Exception {
        Note n=list(),read=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(n))).get(0);
        assertTrue(NoteMerge.same(n,read));assertEquals("row2",read.items.get(1).id);assertTrue(read.items.get(1).checked);assertEquals(2,read.items.get(1).depth);assertTrue(read.hideChecked);
    }
    @Test public void backupRestoresBothStructuredKinds() throws Exception {
        List<Note> source=Arrays.asList(text(),list()),read=BackupCodec.decode(BackupCodec.encode(source));
        assertEquals(2,read.size());assertTrue(NoteMerge.same(source.get(0),read.get(0)));assertTrue(NoteMerge.same(source.get(1),read.get(1)));
    }
    @Test public void oldSchema1MigratesWithoutNewProperties() throws Exception {
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(Note.create(1).edit("old","plain",2))),StandardCharsets.UTF_8));
        root.put("version",1);JSONObject n=root.getJSONArray("notes").getJSONObject(0);
        for(String key:new String[]{"checklist","hideChecked","items","marks","labels"})n.remove(key);
        Note migrated=DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals("plain",migrated.body);assertFalse(migrated.checklist);assertTrue(migrated.labels.isEmpty());
        assertEquals(0,migrated.reminderAt);assertEquals(6,new JSONObject(new String(DocumentCodec.encode(Arrays.asList(migrated)),StandardCharsets.UTF_8)).getInt("version"));
    }
    @Test public void schema6FolderBackgroundAndGroupsRoundTrip()throws Exception{
        app.space.notes.domain.Diagram d=app.space.notes.domain.Diagram.empty().put(new app.space.notes.domain.Diagram.Node("a","star","A",0,0,100,100,0)).put(new app.space.notes.domain.Diagram.Node("b","cloud","B",200,200,100,100,0)).group(Arrays.asList("a","b"));
        Note note=text().folder("Projects ಕನ್ನಡ",3).background(15,4).diagram(d,5).reminder(10000,6);Note read=BackupCodec.decode(BackupCodec.encode(Arrays.asList(note))).get(0);assertTrue(NoteMerge.same(note,read));assertEquals("Projects ಕನ್ನಡ",read.folder);assertEquals(15,read.background);assertEquals(d.fingerprint(),read.diagram.fingerprint());
    }
    @Test public void schema5DefaultsFolderAndBackground()throws Exception{
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(text().reminder(10000,3))),StandardCharsets.UTF_8));root.put("version",5);JSONObject fields=root.getJSONArray("notes").getJSONObject(0);fields.remove("folder");fields.remove("background");Note read=DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8)).get(0);assertEquals("",read.folder);assertEquals(0,read.background);assertEquals(10000,read.reminderAt);
    }
    @Test public void imagesAreSelfContainedInBackupAndRestore()throws Exception{
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(20,20,android.graphics.Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.BLUE);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes));bitmap.recycle();app.space.notes.domain.NoteImage image=new app.space.notes.domain.NoteImage(UUID.randomUUID().toString(),"Drawing","image/png",20,20,Base64.getEncoder().encodeToString(bytes.toByteArray()));Note original=text().images(Arrays.asList(image),4);Note read=BackupCodec.decode(BackupCodec.encode(Arrays.asList(original))).get(0);assertTrue(NoteMerge.same(original,read));assertEquals(image.data,read.images.get(0).data);assertEquals(image.id,read.images.get(0).id);
    }
    @Test public void visibleNoteJsonImportsWithoutFlatteningItsStructure()throws Exception{Note n=text().folder("Work",3).background(6,4);assertTrue(NoteMerge.same(n,app.space.notes.runtime.LocalBackupFiles.decodeImport(DocumentCodec.encode(Arrays.asList(n))).get(0)));}
    @Test public void backupImportStillRejectsDamagedEnvelopeChecksum()throws Exception{JSONObject envelope=new JSONObject(new String(BackupCodec.encode(Arrays.asList(text())),StandardCharsets.UTF_8));envelope.put("sha256","broken");try{app.space.notes.runtime.LocalBackupFiles.decodeImport(envelope.toString().getBytes(StandardCharsets.UTF_8));fail("bad checksum accepted");}catch(java.io.IOException expected){}}
    @Test public void outOfRangeMarkRejectsWholeDocument() throws Exception {
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(text())),StandardCharsets.UTF_8));
        root.getJSONArray("notes").getJSONObject(0).getJSONArray("marks").getJSONObject(0).put("end",500);
        try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("invalid marks must fail");}catch(IllegalArgumentException expected){ }
    }
    @Test public void duplicatedRowIdsRejectWholeDocument() throws Exception {
        JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(list())),StandardCharsets.UTF_8));
        JSONArray rows=root.getJSONArray("notes").getJSONObject(0).getJSONArray("items");rows.getJSONObject(1).put("id","row1");
        try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("duplicate rows must fail");}catch(IllegalArgumentException expected){ }
    }
    @Test public void androidSpanRoundTripRetainsAllStyles() {
        SpannableStringBuilder editable=new SpannableStringBuilder("Heading one\nHeading two\nHello world");List<Note.Mark> source=new ArrayList<>();
        source.add(new Note.Mark("h1",0,11));source.add(new Note.Mark("h2",12,23));
        for(String type:new String[]{"bold","italic","underline","strike"})source.add(new Note.Mark(type,24,29));
        TextStyles.write(editable,source);List<Note.Mark> read=TextStyles.read(editable);assertEquals(6,read.size());
        for(Note.Mark mark:read){assertEquals(mark.type.equals("h1")?0:mark.type.equals("h2")?12:24,mark.start);assertEquals(mark.type.equals("h1")?11:mark.type.equals("h2")?23:29,mark.end);}
    }
    @Test public void transientKeyboardCompositionIsNotSavedAsUserFormatting(){
        SpannableStringBuilder text=new SpannableStringBuilder("typing");text.setSpan(new android.text.style.UnderlineSpan(),0,6,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE|android.text.Spanned.SPAN_COMPOSING);assertTrue(TextStyles.read(text).isEmpty());
        text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,6,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);assertEquals("bold",TextStyles.read(text).get(0).type);
    }
    @Test public void spansFollowInsertedTextBeforeSerialization() {
        SpannableStringBuilder editable=new SpannableStringBuilder("Hello world");TextStyles.write(editable,Arrays.asList(new Note.Mark("bold",6,11)));
        editable.insert(0,"New ");Note.Mark mark=TextStyles.read(editable).get(0);assertEquals(10,mark.start);assertEquals(15,mark.end);
    }
    @Test public void schema7CanonicalContentRoundTripsWithoutEditableLegacyDuplicates()throws Exception{
        app.space.notes.domain.DocumentContent document=app.space.notes.domain.DocumentContent.fromLegacy(text());String first=document.blocks.get(0).id;
        document=document.selectionToChecklist(first,0,5).insert(2,app.space.notes.domain.DocumentContent.Block.table("table","Table01",app.space.notes.domain.TextTable.empty(2,2).withCell(1,1,"table ಕನ್ನಡ")));
        Note original=text().withContent(document,5);byte[] encoded=DocumentCodec.encode(Arrays.asList(original,list()));JSONObject root=new JSONObject(new String(encoded,StandardCharsets.UTF_8));assertEquals(7,root.getInt("version"));JSONObject fields=root.getJSONArray("notes").getJSONObject(0);assertFalse(fields.has("body"));assertFalse(fields.has("images"));assertTrue(fields.has("content"));
        List<Note> reopened=BackupCodec.decode(BackupCodec.encode(Arrays.asList(original,list())));assertTrue(NoteMerge.same(original,reopened.get(0)));assertTrue(NoteMerge.same(list(),reopened.get(1)));assertEquals("bold",reopened.get(0).content.blocks.get(1).items.get(0).marks.get(0).type);assertTrue(reopened.get(0).matches("table ಕನ್ನಡ"));
    }
    @Test public void schema7PreservesVisualKindNamePixelsAndAllInlineStyleValues()throws Exception{
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.ARGB_8888);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();app.space.notes.domain.NoteImage image=new app.space.notes.domain.NoteImage(UUID.randomUUID().toString(),"Drawing","image/png",2,2,Base64.getEncoder().encodeToString(bytes.toByteArray()));
        app.space.notes.domain.RichText rich=new app.space.notes.domain.RichText("Styled link",Arrays.asList(new Note.Mark("font",0,11,"serif"),new Note.Mark("size",0,11,"24"),new Note.Mark("link",0,11,"https://example.com/path"),new Note.Mark("number",0,11,"3")));
        app.space.notes.domain.DocumentContent document=new app.space.notes.domain.DocumentContent(Arrays.asList(app.space.notes.domain.DocumentContent.Block.text("text",rich),app.space.notes.domain.DocumentContent.Block.visual("ink","My sketch",app.space.notes.domain.DocumentContent.Kind.DRAWING,image),app.space.notes.domain.DocumentContent.Block.text("after",new app.space.notes.domain.RichText("After image",Collections.emptyList()))));
        Note original=text().withContent(document,6),read=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(original))).get(0);assertTrue(NoteMerge.same(original,read));assertEquals("My sketch",read.content.blocks.get(1).name);assertEquals(app.space.notes.domain.DocumentContent.Kind.DRAWING,read.content.blocks.get(1).kind);assertEquals(image.data,read.images.get(0).data);
        SpannableStringBuilder nativeText=new SpannableStringBuilder(rich.text);TextStyles.write(nativeText,rich.marks);List<Note.Mark> styles=TextStyles.read(nativeText);assertEquals(4,styles.size());for(Note.Mark mark:styles)assertFalse(mark.value.isEmpty());
    }
    @Test public void schema7MalformedTableAndUnknownFutureFormatAreRejected()throws Exception{
        app.space.notes.domain.DocumentContent content=new app.space.notes.domain.DocumentContent(Arrays.asList(app.space.notes.domain.DocumentContent.Block.table("table","Table01",app.space.notes.domain.TextTable.empty(2,2))));JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(text().withContent(content,5))),StandardCharsets.UTF_8));JSONArray table=root.getJSONArray("notes").getJSONObject(0).getJSONObject("content").getJSONArray("blocks").getJSONObject(0).getJSONArray("rows");table.put(1,new JSONArray().put("lost cell"));try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("Ragged table accepted");}catch(IllegalArgumentException | org.json.JSONException expected){}
        root.put("version",8);try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("Future format accepted");}catch(java.io.IOException expected){}
    }
    @Test public void recursiveJsonBombIsRejectedBeforeAndroidParserRuns()throws Exception{
        String nested=String.join("",Collections.nCopies(1000,"["))+"0"+String.join("",Collections.nCopies(1000,"]"));try{app.space.notes.runtime.LocalBackupFiles.decodeImport(nested.getBytes(StandardCharsets.UTF_8));fail("Deep JSON accepted");}catch(java.io.IOException expected){}
    }    @Test public void canonicalUnknownFieldsAndCoercedValuesFailInsteadOfSilentlyLosingData()throws Exception{
        Note old=Note.create(1).edit("Strict","Text",2),note=old.withContent(app.space.notes.domain.DocumentContent.fromLegacy(old),3);byte[] bytes=DocumentCodec.encode(Arrays.asList(note));
        for(int mutation=0;mutation<4;mutation++){JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8)),saved=root.getJSONArray("notes").getJSONObject(0),content=saved.getJSONObject("content"),block=content.getJSONArray("blocks").getJSONObject(0);if(mutation==0)block.put("futurePayload","must not disappear");else if(mutation==1)block.put("text",42);else if(mutation==2)content.put("writingLines","false");else saved.put("body","ambiguous second source");try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("Unsupported fields/types must leave the original file untouched");}catch(java.io.IOException expected){}}
        assertTrue(NoteMerge.same(note,DocumentCodec.decode(bytes).get(0)));
    }
    @Test public void canonicalUnicodeRichTablesExportAcrossCompleteLinesAndColumnPanels()throws Exception{
        String source=String.join("",Collections.nCopies(60,"😀 ಕನ್ನಡ नमस्ते\n"));app.space.notes.domain.RichText rich=new app.space.notes.domain.RichText(source,Arrays.asList(new Note.Mark("size",0,source.length(),"24")));List<app.space.notes.domain.RichText> row=Arrays.asList(rich,rich,rich,rich,rich,rich);app.space.notes.domain.DocumentContent content=new app.space.notes.domain.DocumentContent(Arrays.asList(app.space.notes.domain.DocumentContent.Block.table("wide","Research table",app.space.notes.domain.TextTable.rich(Arrays.asList(row)))));Note note=Note.create(1).withContent(content,2);byte[] before=DocumentCodec.encode(Arrays.asList(note));java.io.File file=java.io.File.createTempFile("canonical-pdf-",".pdf",androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try{int pages;try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){pages=app.space.notes.runtime.PdfExporter.write(Arrays.asList(note),out);}assertTrue("Tall, wide rich cells require real paged output",pages>=4);try(android.os.ParcelFileDescriptor descriptor=android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY);android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(descriptor)){assertEquals(pages,renderer.getPageCount());for(int page:new int[]{0,pages-1})try(android.graphics.pdf.PdfRenderer.Page rendered=renderer.openPage(page)){android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(595,842,android.graphics.Bitmap.Config.ARGB_8888);try{bitmap.eraseColor(android.graphics.Color.WHITE);rendered.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);int[] pixels=new int[595*842];bitmap.getPixels(pixels,0,595,0,0,595,842);int ink=0;for(int pixel:pixels)if((pixel&0xffffff)!=0xffffff)ink++;assertTrue("First and final column-panel pages must contain rendered output",ink>1000);}finally{bitmap.recycle();}}}assertTrue(Arrays.equals(before,DocumentCodec.encode(Arrays.asList(note))));}finally{assertTrue(file.delete());}
    }

    @Test public void canonicalDiagramUnknownFieldsAndFractionalCoordinatesAreNotDiscarded()throws Exception{app.space.notes.domain.Diagram diagram=app.space.notes.domain.Diagram.empty().put(new app.space.notes.domain.Diagram.Node("node","process","Node",0,0,120,80,0));Note note=Note.create(1).withContent(new app.space.notes.domain.DocumentContent(Arrays.asList(app.space.notes.domain.DocumentContent.Block.diagram("diagram","Diagram01",diagram))),2);byte[] bytes=DocumentCodec.encode(Arrays.asList(note));for(int mutation=0;mutation<2;mutation++){JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8)),node=root.getJSONArray("notes").getJSONObject(0).getJSONObject("content").getJSONArray("blocks").getJSONObject(0).getJSONObject("diagram").getJSONArray("nodes").getJSONObject(0);if(mutation==0)node.put("futureStroke","must not disappear");else node.put("x",0.5);try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("Unsupported diagram data must leave the source unchanged");}catch(java.io.IOException expected){}}assertTrue(NoteMerge.same(note,DocumentCodec.decode(bytes).get(0)));}


    @Test public void namedChecklistRoundTripsLibraryBackupAndMergeWithoutDroppingName()throws Exception{
        app.space.notes.domain.DocumentContent.Block list=app.space.notes.domain.DocumentContent.Block.checklist("box","Shopping",Arrays.asList(new Note.Item("rich-row","Keep",true,2,Arrays.asList(new Note.Mark("italic",0,4)))),true);
        Note original=text().withContent(new app.space.notes.domain.DocumentContent(Arrays.asList(list)),5);
        Note stored=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(original))).get(0),backup=BackupCodec.decode(BackupCodec.encode(Arrays.asList(original))).get(0);assertTrue(NoteMerge.same(original,stored));assertTrue(NoteMerge.same(original,backup));Note merged=app.space.notes.domain.NoteOperations.merge(Arrays.asList(original,original),6);assertEquals(2,merged.content.blocks.stream().filter(block->block.name.equals("Shopping")).count());assertEquals("Shopping",stored.content.block("box").name);assertEquals("italic",stored.content.block("box").items.get(0).marks.get(0).type);
    }
}
