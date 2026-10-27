package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class DiagramTest {
    private Diagram.Node node(String id){return new Diagram.Node(id,"process","Step "+id,10,10,180,90,0);}
    private Diagram diagram(){return Diagram.empty().put(node("a")).put(node("b")).connect("a","b","Yes");}
    @Test public void moveIsClampedAndEdgesKeepIdentity(){Diagram moved=diagram().put(node("a").move(10000,-10));assertEquals(620,moved.find("a").x);assertEquals(0,moved.find("a").y);assertEquals("a",moved.edges.get(0).from);}
    @Test public void removingNodeCascadesOnlyItsArrows(){Diagram d=diagram().put(node("c"));Diagram next=d.remove("a");assertEquals(2,next.nodes.size());assertTrue(next.edges.isEmpty());assertEquals(3,d.nodes.size());}
    @Test(expected=IllegalArgumentException.class) public void danglingArrowRefused(){new Diagram(Arrays.asList(node("a")),Arrays.asList(new Diagram.Edge("e","a","missing","")));}
    @Test(expected=IllegalArgumentException.class) public void duplicateNodesRefused(){new Diagram(Arrays.asList(node("a"),node("a")),Collections.emptyList());}
    @Test(expected=IllegalArgumentException.class) public void selfArrowRefused(){new Diagram.Edge("e","a","a","");}
    @Test(expected=IllegalArgumentException.class) public void invalidGeometryRefused(){new Diagram.Node("a","process","",790,0,100,100,0);}
    @Test(expected=IllegalArgumentException.class) public void unknownShapeRefused(){new Diagram.Node("a","unsafe","",0,0,100,100,0);}
    @Test public void allCommonShapesAreAccepted(){for(String shape:Diagram.SHAPES)assertEquals(shape,new Diagram.Node("a",shape,"",0,0,100,100,0).shape);}
    @Test public void resizeStaysWithinCanvas(){Diagram.Node n=node("a").move(620,910).resize(800,1000);assertEquals(0,n.x);assertEquals(0,n.y);assertEquals(800,n.width);}
    @Test public void textAndArrowLabelsAreSearchable(){Note n=Note.create(1).diagram(diagram(),2);assertTrue(n.matches("Step a"));assertTrue(n.matches("yes"));assertFalse(n.matches("missing"));}
    @Test public void editsAndDuplicationPreserveDiagram(){Note n=Note.create(1).diagram(diagram(),2).edit("Title","Body",3).properties(1,Arrays.asList("Work"),4).state(true,false,0,5);assertEquals(diagram().nodes.size(),n.diagram.nodes.size());assertEquals(n.diagram.fingerprint(),NoteOperations.duplicate(n,6).diagram.fingerprint());assertEquals(n.diagram.fingerprint(),n.withId("new").diagram.fingerprint());}
    @Test public void diagramDifferenceCreatesStableRestoreConflict(){Diagram d=diagram();Note a=Note.create(1).diagram(d,2),b=a.diagram(d.put(node("a").edit("Changed",2)),2);NoteMerge.Plan first=NoteMerge.preview(Arrays.asList(a),Arrays.asList(b));assertEquals(1,first.conflicts);assertEquals(2,first.notes.size());assertEquals(0,NoteMerge.preview(first.notes,Arrays.asList(b)).conflicts);}
    @Test(expected=IllegalArgumentException.class) public void mergingDiagramCannotLoseVisualData(){Note n=Note.create(1).diagram(diagram(),2);NoteOperations.merge(Arrays.asList(n,Note.create(2)),3);}
    @Test public void arrowLabelUpdateAndRemoveAreImmutable(){Diagram d=diagram();String id=d.edges.get(0).id;assertEquals("No",d.labelEdge(id,"No").edges.get(0).label);assertEquals("Yes",d.edges.get(0).label);assertTrue(d.removeEdge(id).edges.isEmpty());}
}
