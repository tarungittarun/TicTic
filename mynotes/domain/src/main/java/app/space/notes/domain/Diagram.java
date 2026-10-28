package app.space.notes.domain;

import java.util.*;

/** Immutable flowchart on an 800 x 1000 logical canvas. Edges reference stable node IDs. */
public final class Diagram {
    public static final int WIDTH=800,HEIGHT=1000;
    public static final List<String> SHAPES=Collections.unmodifiableList(Arrays.asList("process","decision","terminator","data","ellipse","text","triangle","rightTriangle","pentagon","hexagon","octagon","star","trapezoid","cylinder","cube","document","cloud","arrow","chevron","semicircle","heart"));
    /** Stored legacy IDs remain valid; presentation uses geometric names. */
    public static String shapeName(String id){
        String[] names={"Rectangle","Diamond","Capsule","Parallelogram","Oval","Text annotation","Triangle","Right triangle","Pentagon","Hexagon","Octagon","Star","Trapezoid","Cylinder","Cube","Wavy rectangle","Cloud","Right arrow","Chevron","Semicircle","Heart"};
        int index=SHAPES.indexOf(id);if(index<0)throw new IllegalArgumentException("Unknown shape");return names[index];
    }
    public final List<Node> nodes;
    public final List<Edge> edges;
    public static final class Node {
        public final String id,shape,text,group;
        public final int x,y,width,height,color;
        public Node(String id,String shape,String text,int x,int y,int width,int height,int color) {
            this(id,shape,text,x,y,width,height,color,"");
        }
        public Node(String id,String shape,String text,int x,int y,int width,int height,int color,String group) {
            if(group==null || id==null || id.isEmpty() || !SHAPES.contains(shape) || text==null || x<0 || y<0 || width<60 || height<40 || width>WIDTH || height>HEIGHT
                || x>WIDTH-width || y>HEIGHT-height || color<0 || color>11)throw new IllegalArgumentException("Invalid diagram shape");
            this.id=id;this.shape=shape;this.text=text;this.x=x;this.y=y;this.width=width;this.height=height;this.color=color;this.group=group;
        }
        public Node move(int x,int y){return new Node(id,shape,text,Math.max(0,Math.min(WIDTH-width,x)),Math.max(0,Math.min(HEIGHT-height,y)),width,height,color,group);}
        public Node edit(String text,int color){return new Node(id,shape,text,x,y,width,height,color,group);}
        public Node resize(int width,int height){int w=Math.max(60,Math.min(WIDTH,width)),h=Math.max(40,Math.min(HEIGHT,height));return new Node(id,shape,text,Math.min(x,WIDTH-w),Math.min(y,HEIGHT-h),w,h,color,group);}
            public Node grouped(String value){return new Node(id,shape,text,x,y,width,height,color,value);}
    }
    public static final class Edge {
        public final String id,from,to,label;
        public Edge(String id,String from,String to,String label){
            if(id==null || id.isEmpty() || from==null || to==null || from.equals(to) || label==null)throw new IllegalArgumentException("Invalid diagram arrow");
            this.id=id;this.from=from;this.to=to;this.label=label;
        }
    }
    public Diagram(List<Node> nodes,List<Edge> edges){
        Set<String> ids=new HashSet<>();for(Node n:nodes)if(!ids.add(n.id))throw new IllegalArgumentException("Duplicate diagram node ID");
        Set<String> arrows=new HashSet<>();for(Edge edge:edges){
            if(!ids.contains(edge.from) || !ids.contains(edge.to))throw new IllegalArgumentException("Dangling diagram arrow");
            if(!arrows.add(edge.id))throw new IllegalArgumentException("Duplicate diagram arrow ID");
        }
        this.nodes=Collections.unmodifiableList(new ArrayList<>(nodes));this.edges=Collections.unmodifiableList(new ArrayList<>(edges));
    }
    public static Diagram empty(){return new Diagram(Collections.emptyList(),Collections.emptyList());}
    public Node find(String id){for(Node n:nodes)if(n.id.equals(id))return n;return null;}
    public Diagram put(Node node){List<Node> copy=new ArrayList<>(nodes);for(int i=0;i<copy.size();i++)if(copy.get(i).id.equals(node.id)){copy.set(i,node);return new Diagram(copy,edges);}copy.add(node);return new Diagram(copy,edges);}
    public Diagram remove(String id){List<Node> copy=new ArrayList<>(nodes);copy.removeIf(n->n.id.equals(id));List<Edge> links=new ArrayList<>(edges);links.removeIf(e->e.from.equals(id)||e.to.equals(id));return new Diagram(copy,links);}
    /** Selected existing groups are merged as units; grouping never changes geometry. */
    public Diagram group(Collection<String> selected){
        Set<String> ids=new HashSet<>(selected),groups=new HashSet<>();for(String id:ids){Node n=find(id);if(n==null)throw new IllegalArgumentException("Unknown group member");if(!n.group.isEmpty())groups.add(n.group);}
        for(Node n:nodes)if(groups.contains(n.group))ids.add(n.id);if(ids.size()<2)throw new IllegalArgumentException("Select at least two shapes");
        String group=UUID.randomUUID().toString();List<Node> result=new ArrayList<>();for(Node n:nodes)result.add(ids.contains(n.id)?n.grouped(group):n);return new Diagram(result,edges);
    }
    public Diagram ungroup(String id){Node member=find(id);if(member==null || member.group.isEmpty())return this;List<Node> result=new ArrayList<>();for(Node n:nodes)result.add(n.group.equals(member.group)?n.grouped(""):n);return new Diagram(result,edges);}
    /** Clamp the whole group together, preserving relative distances at every canvas edge. */
    public Diagram move(String id,int dx,int dy){Node member=find(id);if(member==null)return this;List<Node> members=new ArrayList<>();for(Node n:nodes)if(n.id.equals(id) || (!member.group.isEmpty() && n.group.equals(member.group)))members.add(n);
        int left=WIDTH,top=HEIGHT,right=0,bottom=0;for(Node n:members){left=Math.min(left,n.x);top=Math.min(top,n.y);right=Math.max(right,n.x+n.width);bottom=Math.max(bottom,n.y+n.height);}int x=Math.max(-left,Math.min(WIDTH-right,dx)),y=Math.max(-top,Math.min(HEIGHT-bottom,dy));
        Set<String> ids=new HashSet<>();for(Node n:members)ids.add(n.id);List<Node> result=new ArrayList<>();for(Node n:nodes)result.add(ids.contains(n.id)?n.move(n.x+x,n.y+y):n);return new Diagram(result,edges);
    }
    public Diagram connect(String from,String to,String label){List<Edge> copy=new ArrayList<>(edges);copy.add(new Edge(UUID.randomUUID().toString(),from,to,label));return new Diagram(nodes,copy);}
    public Diagram removeEdge(String id){List<Edge> copy=new ArrayList<>(edges);copy.removeIf(e->e.id.equals(id));return new Diagram(nodes,copy);}
    public Diagram labelEdge(String id,String label){List<Edge> copy=new ArrayList<>();for(Edge edge:edges)copy.add(edge.id.equals(id)?new Edge(edge.id,edge.from,edge.to,label):edge);return new Diagram(nodes,copy);}
    public String plainText(){StringBuilder out=new StringBuilder();for(Node node:nodes)out.append(node.text).append('\n');for(Edge edge:edges)if(!edge.label.isEmpty())out.append(edge.label).append('\n');return out.toString();}
    /** Length-delimited, deterministic representation for merge conflict identity. */
    public String fingerprint(){
        StringBuilder out=new StringBuilder();out.append(nodes.size()).append('#');
        for(Node n:nodes){field(out,n.id);field(out,n.shape);field(out,n.text);out.append(n.x).append(',').append(n.y).append(',').append(n.width).append(',').append(n.height).append(',').append(n.color).append(';');if(!n.group.isEmpty()){out.append('@');field(out,n.group);}}
        out.append(edges.size()).append('#');for(Edge e:edges){field(out,e.id);field(out,e.from);field(out,e.to);field(out,e.label);}return out.toString();
    }
    private static void field(StringBuilder out,String value){out.append(value.length()).append(':').append(value);}
}
