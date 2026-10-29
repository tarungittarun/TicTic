package app.space.notes.domain;

/** Corner drags keep the opposite corner fixed, never invert or leave the logical page. */
public final class DiagramResize {
 private DiagramResize() { }
 public static Diagram.Node corner(Diagram.Node node,int corner,int dx,int dy){
  if(node==null || corner<0 || corner>3)throw new IllegalArgumentException("Select a resize corner");
  boolean left=corner==0 || corner==3,top=corner==0 || corner==1;int l=node.x,t=node.y,r=l+node.width,b=t+node.height;
  if(left)l=(int)Math.max(0,Math.min(r-60,(long)l+dx));else r=(int)Math.min(Diagram.WIDTH,Math.max(l+60,(long)r+dx));
  if(top)t=(int)Math.max(0,Math.min(b-40,(long)t+dy));else b=(int)Math.min(Diagram.HEIGHT,Math.max(t+40,(long)b+dy));
  return new Diagram.Node(node.id,node.shape,node.text,l,t,r-l,b-t,node.color,node.group);
 }
}
