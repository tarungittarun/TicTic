package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DiagramEditingTest {
 private Diagram diagram(){return Diagram.empty().put(new Diagram.Node("a","process","A",10,10,100,100,0)).put(new Diagram.Node("b","star","B",200,200,100,100,0)).put(new Diagram.Node("c","ellipse","C",400,400,100,100,0));}
 @Test public void twentyNamedShapesRemainDistinct(){assertEquals(21,Diagram.SHAPES.size());assertEquals(20,Diagram.SHAPES.stream().filter(id->!id.equals("text")).count());Set<String> names=new HashSet<>();for(String id:Diagram.SHAPES){assertTrue(names.add(Diagram.shapeName(id)));assertEquals(id,new Diagram.Node("s",id,"",0,0,100,100,0).shape);}assertEquals("Rectangle",Diagram.shapeName("process"));assertEquals("Diamond",Diagram.shapeName("decision"));}
 @Test public void groupedMovementPreservesOffsetsAtRightAndBottom(){Diagram d=diagram().group(Arrays.asList("a","b"));Diagram moved=d.move("a",9999,9999);assertEquals(190,moved.find("b").x-moved.find("a").x);assertEquals(190,moved.find("b").y-moved.find("a").y);assertEquals(800,moved.find("b").x+100);assertEquals(1000,moved.find("b").y+100);assertEquals(400,moved.find("c").x);}
 @Test public void groupedMovementClampsAtTopAndLeft(){Diagram moved=diagram().group(Arrays.asList("a","b")).move("b",-9999,-9999);assertEquals(0,moved.find("a").x);assertEquals(0,moved.find("a").y);assertEquals(190,moved.find("b").x);}
 @Test public void groupingNeverMutatesOriginal(){Diagram d=diagram(),grouped=d.group(Arrays.asList("a","b"));assertEquals("",d.find("a").group);assertFalse(grouped.find("a").group.isEmpty());assertEquals(grouped.find("a").group,grouped.find("b").group);assertEquals("",grouped.find("c").group);}
 @Test public void regroupMergesExistingGroups(){Diagram d=diagram().group(Arrays.asList("a","b")).group(Arrays.asList("b","c"));assertEquals(d.find("a").group,d.find("c").group);}
 @Test public void ungroupPreservesGeometryAndHistoryIdentity(){Diagram d=diagram(),grouped=d.group(Arrays.asList("a","b"));assertNotEquals(d.fingerprint(),grouped.fingerprint());assertEquals(d.fingerprint(),grouped.ungroup("a").fingerprint());}
 @Test public void nodeEditsKeepGroup(){Diagram d=diagram().group(Arrays.asList("a","b"));Diagram.Node n=d.find("a");assertEquals(n.group,n.edit("Changed",1).resize(200,200).move(20,20).group);}
 @Test(expected=IllegalArgumentException.class) public void singleShapeCannotBecomeGroup(){diagram().group(Arrays.asList("a"));}
 @Test public void historyUndoRedoAndBranching(){DiagramHistory h=new DiagramHistory();Diagram a=diagram(),b=a.move("a",10,10),c=b.remove("b");h.record(a,b);h.record(b,c);assertEquals(b.fingerprint(),h.undo(c).fingerprint());assertEquals(a.fingerprint(),h.undo(b).fingerprint());assertEquals(b.fingerprint(),h.redo(a).fingerprint());Diagram next=b.move("a",1,1);h.record(b,next);assertFalse(h.canRedo(next));}
 @Test public void staleHistoryCannotOverwriteExternalEdits(){DiagramHistory h=new DiagramHistory();Diagram a=diagram(),b=a.move("a",10,0);h.record(a,b);Diagram external=b.remove("c");assertFalse(h.canUndo(external));assertSame(external,h.undo(external));}
 @Test public void noOpDoesNotConsumeHistory(){DiagramHistory h=new DiagramHistory();Diagram a=diagram();h.record(a,a);assertFalse(h.canUndo(a));}
 @Test public void boundedHistoryRetainsFiftyActions(){DiagramHistory h=new DiagramHistory();Diagram value=diagram();for(int i=0;i<60;i++){Diagram next=value.move("a",1,0);h.record(value,next);value=next;}int count=0;while(h.canUndo(value)){value=h.undo(value);count++;}assertEquals(50,count);}
}
