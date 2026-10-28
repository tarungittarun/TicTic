package app.space.notes;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DiagramCodecTest {
    private Note note(){Diagram d=Diagram.empty();int index=0;for(String shape:Diagram.SHAPES)d=d.put(new Diagram.Node("n"+index,shape,"ಕನ್ನಡ "+index,20+(index++%4)*195,20+((index-1)/4)*190,180,140,8));d=d.connect("n0","n1","Yes").connect("n1","n2","No");return Note.create(1).edit("Flow","Body",2).diagram(d,3);}
    @Test public void allShapesArrowsAndUnicodeRoundTrip() throws Exception {Note original=note(),read=DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(original))).get(0);assertTrue(NoteMerge.same(original,read));assertEquals(20,read.diagram.nodes.size());assertEquals(2,read.diagram.edges.size());assertEquals("Yes",read.diagram.edges.get(0).label);}
    @Test public void backupPreservesFullEditableDiagram() throws Exception {Note original=note(),read=BackupCodec.decode(BackupCodec.encode(Arrays.asList(original))).get(0);assertEquals(original.diagram.fingerprint(),read.diagram.fingerprint());}
    @Test public void schema2GetsEmptyDiagramWithoutLosingRichText() throws Exception {JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(note().rich("Old","Hello",Arrays.asList(new Note.Mark("bold",0,5)),4))),StandardCharsets.UTF_8));root.put("version",2);root.getJSONArray("notes").getJSONObject(0).remove("diagram");Note read=DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8)).get(0);assertTrue(read.diagram.nodes.isEmpty());assertEquals(1,read.marks.size());assertEquals("Old",read.title);}
    @Test public void danglingArrowRefusesEntireLibrary() throws Exception {JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(note())),StandardCharsets.UTF_8));root.getJSONArray("notes").getJSONObject(0).getJSONObject("diagram").getJSONArray("edges").getJSONObject(0).put("to","missing");try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("dangling edge");}catch(IllegalArgumentException expected){}}
    @Test public void outOfCanvasGeometryRefused() throws Exception {JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(note())),StandardCharsets.UTF_8));root.getJSONArray("notes").getJSONObject(0).getJSONObject("diagram").getJSONArray("nodes").getJSONObject(0).put("x",900);try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("out of canvas");}catch(IllegalArgumentException expected){}}
    @Test public void diagramConflictAndRepeatedImportAreLossless() throws Exception {Note original=note(),changed=original.diagram(original.diagram.put(original.diagram.nodes.get(0).edit("Changed",2)),4);NoteMerge.Plan first=NoteMerge.preview(Arrays.asList(original),Arrays.asList(changed));assertEquals(1,first.conflicts);assertEquals(2,first.notes.size());List<Note> read=DocumentCodec.decode(DocumentCodec.encode(first.notes));assertEquals(0,NoteMerge.preview(read,Arrays.asList(changed)).conflicts);}
    @Test public void nodeMovementChangesEncodedPayload() throws Exception {Note original=note(),changed=original.diagram(original.diagram.put(original.diagram.nodes.get(0).move(300,400)),4);assertFalse(Arrays.equals(DocumentCodec.encode(Arrays.asList(original)),DocumentCodec.encode(Arrays.asList(changed))));assertEquals(300,DocumentCodec.decode(DocumentCodec.encode(Arrays.asList(changed))).get(0).diagram.nodes.get(0).x);}
    @Test public void v3MissingDiagramFailsInsteadOfSilentlyDroppingData() throws Exception {JSONObject root=new JSONObject(new String(DocumentCodec.encode(Arrays.asList(note())),StandardCharsets.UTF_8));root.getJSONArray("notes").getJSONObject(0).remove("diagram");try{DocumentCodec.decode(root.toString().getBytes(StandardCharsets.UTF_8));fail("missing diagram field");}catch(org.json.JSONException expected){}}
}
