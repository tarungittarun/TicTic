package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class DiagramResizeTest {
 private Diagram.Node node(){return new Diagram.Node("shape","process","Keep label",200,200,200,120,3,"group");}
 @Test public void bottomRightKeepsTopLeftFixed(){Diagram.Node n=DiagramResize.corner(node(),2,100,80);assertEquals(200,n.x);assertEquals(200,n.y);assertEquals(300,n.width);assertEquals(200,n.height);}
 @Test public void topLeftKeepsOppositeCornerFixed(){Diagram.Node n=DiagramResize.corner(node(),0,-100,-80);assertEquals(100,n.x);assertEquals(120,n.y);assertEquals(400,n.x+n.width);assertEquals(320,n.y+n.height);}
 @Test public void draggingPastOppositeCornerCannotInvertOrShrinkBelowMinimum(){for(int corner=0;corner<4;corner++){Diagram.Node n=DiagramResize.corner(node(),corner,(corner==0 || corner==3)?10000:-10000,corner<2?10000:-10000);assertEquals(60,n.width);assertEquals(40,n.height);assertTrue(n.x>=0 && n.y>=0);}}
 @Test public void allShapeTypesAndCornersStayInsidePageAndPreserveMetadata(){for(String shape:Diagram.SHAPES)for(int corner=0;corner<4;corner++){Diagram.Node original=new Diagram.Node("id",shape,"Words",200,200,200,120,7,"g");Diagram.Node n=DiagramResize.corner(original,corner,corner==0 || corner==3?Integer.MIN_VALUE:Integer.MAX_VALUE,corner<2?Integer.MIN_VALUE:Integer.MAX_VALUE);assertEquals("id",n.id);assertEquals(shape,n.shape);assertEquals("Words",n.text);assertEquals("g",n.group);assertEquals(7,n.color);assertTrue(n.x>=0 && n.y>=0 && n.x+n.width<=Diagram.WIDTH && n.y+n.height<=Diagram.HEIGHT);}}
 @Test public void resizePreservesOtherNodesAndConnectorIdentities(){Diagram original=Diagram.empty().put(node()).put(new Diagram.Node("other","decision","Other",500,500,120,80,1)).connect("shape","other","Yes");Diagram resized=original.put(DiagramResize.corner(original.find("shape"),1,100,-50));assertSame(original.find("other"),resized.find("other"));assertEquals(original.edges.get(0).id,resized.edges.get(0).id);assertEquals("Yes",resized.edges.get(0).label);assertEquals(200,original.find("shape").width);}
 @Test public void invalidCornerIsRejected(){assertThrows(IllegalArgumentException.class,()->DiagramResize.corner(node(),4,1,1));assertThrows(IllegalArgumentException.class,()->DiagramResize.corner(null,0,1,1));}
}
