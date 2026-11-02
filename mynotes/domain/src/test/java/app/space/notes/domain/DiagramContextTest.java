package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class DiagramContextTest {
 private Diagram value(){return new Diagram(Arrays.asList(new Diagram.Node("a","process","First",5,5,100,60,2,"g"),new Diagram.Node("b","decision","Second",200,200,100,60,3,"g"),new Diagram.Node("c","ellipse","Other",650,900,100,60,0)),Arrays.asList(new Diagram.Edge("ab","a","b","Yes"),new Diagram.Edge("bc","b","c","External")));}
 @Test public void duplicateGroupIsIndependentAndCopiesOnlyInternalArrows(){Diagram before=value(),after=before.duplicateUnit("a");assertEquals(5,after.nodes.size());assertEquals(3,after.edges.size());Diagram.Node a=after.nodes.get(3),b=after.nodes.get(4);assertNotEquals("g",a.group);assertEquals(a.group,b.group);assertEquals(195,b.x-a.x);assertEquals("First",a.text);assertEquals(2,a.color);assertEquals(a.id,after.edges.get(2).from);assertEquals(b.id,after.edges.get(2).to);assertEquals(3,before.nodes.size());}
 @Test public void removeGroupDoesNotDeleteOtherNodesOrSource(){Diagram before=value(),after=before.removeUnit("b");assertEquals(1,after.nodes.size());assertEquals("c",after.nodes.get(0).id);assertTrue(after.edges.isEmpty());assertEquals(3,before.nodes.size());}
 @Test public void splitRetainsOriginalEndpointsAndLabel(){Diagram before=value(),after=before.splitEdge("ab");assertEquals(4,after.nodes.size());assertEquals(3,after.edges.size());Diagram.Node j=after.nodes.get(3);Diagram.Edge start=after.edges.get(1),end=after.edges.get(2);assertEquals("a",start.from);assertEquals(j.id,start.to);assertEquals(j.id,end.from);assertEquals("b",end.to);assertEquals("Yes",start.label);assertEquals("ab",start.id);assertEquals(2,before.edges.size());assertEquals("bc",after.edges.get(0).id);}
 @Test public void duplicateSingleNearBoundaryStaysValid(){Diagram after=value().duplicateUnit("c");Diagram.Node n=after.nodes.get(3);assertTrue(n.x+n.width<=Diagram.WIDTH);assertTrue(n.y+n.height<=Diagram.HEIGHT);assertTrue(n.group.isEmpty());assertEquals(2,after.edges.size());}
 @Test(expected=IllegalArgumentException.class) public void missingConnectorRejected(){value().splitEdge("missing");}
}
