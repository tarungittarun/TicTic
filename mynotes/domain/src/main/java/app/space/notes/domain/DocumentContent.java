package app.space.notes.domain;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Ordered immutable content draft. Deliberately NOT written by the schema6 codec.
 * Never install it as a second editable body alongside Note.body/items/images.
 */
public final class DocumentContent {
    public static final int MAX_BLOCKS=4096;
    public enum Kind {TEXT, CHECKLIST, IMAGE, DRAWING, DIAGRAM, TABLE}
    public static final class Block {
        public final String id,name;
        public final Kind kind;
        public final RichText text;
        public final List<Note.Item> items;
        public final boolean hideChecked;
        public final NoteImage image;
        public final Diagram diagram;
        public final TextTable table;
        private Block(String id,String name,Kind kind,RichText text,List<Note.Item> items,boolean hideChecked,NoteImage image,Diagram diagram,TextTable table){
            if(id==null || id.isEmpty() || id.length()>256 || name==null || name.length()>200)
                throw new IllegalArgumentException("Invalid content identity or name");
            this.id=id;this.name=name;this.kind=kind;this.text=text;this.hideChecked=hideChecked;
            this.items=items==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(items));
            this.image=image;this.diagram=diagram;this.table=table;
        }
        public static Block text(String id,RichText text){
            if(text==null)throw new IllegalArgumentException("Missing text");
            return new Block(id,"",Kind.TEXT,text,null,false,null,null,null);
        }
        public static Block checklist(String id,List<Note.Item> items,boolean hideChecked){
            if(items==null)throw new IllegalArgumentException("Missing checklist");
            Set<String> ids=new HashSet<>();for(Note.Item item:items)if(item==null || !ids.add(item.id))throw new IllegalArgumentException("Invalid checklist identities");
            return new Block(id,"",Kind.CHECKLIST,null,items,hideChecked,null,null,null);
        }
        public static Block visual(String id,String name,Kind kind,NoteImage image){
            if(image==null || (kind!=Kind.IMAGE && kind!=Kind.DRAWING))throw new IllegalArgumentException("Invalid visual kind");
            return new Block(id,name,kind,null,null,false,image,null,null);
        }
        public static Block diagram(String id,String name,Diagram diagram){
            if(diagram==null)throw new IllegalArgumentException("Missing diagram");
            return new Block(id,name,Kind.DIAGRAM,null,null,false,null,diagram,null);
        }
        public static Block table(String id,String name,TextTable table){
            if(table==null)throw new IllegalArgumentException("Missing table");
            return new Block(id,name,Kind.TABLE,null,null,false,null,null,table);
        }
        public Block renamed(String name){
            if(kind==Kind.TEXT || kind==Kind.CHECKLIST)throw new IllegalArgumentException("Text sections do not have visual names");
            return new Block(id,name,kind,text,items,hideChecked,image,diagram,table);
        }
    }
    public final List<Block> blocks;
    public DocumentContent(List<Block> blocks){
        if(blocks==null || blocks.size()>MAX_BLOCKS)throw new IllegalArgumentException("Content block limit exceeded");
        Set<String> ids=new HashSet<>(),images=new HashSet<>(),rows=new HashSet<>();
        for(Block block:blocks){
            if(block==null || !ids.add(block.id))throw new IllegalArgumentException("Duplicate or missing content block");
            for(Note.Item row:block.items)if(!rows.add(row.id))throw new IllegalArgumentException("Duplicate checklist row across sections");
            if(block.image!=null && !images.add(block.image.id))throw new IllegalArgumentException("Duplicate visual reference");
        }
        this.blocks=Collections.unmodifiableList(new ArrayList<>(blocks));
    }
    /** Read-only deterministic projection. Does not save, rewrite, rename or flatten the source. */
    public static DocumentContent fromLegacy(Note note){
        if(note==null)throw new IllegalArgumentException("Missing source note");
        List<Block> blocks=new ArrayList<>();
        if(note.checklist)blocks.add(Block.checklist(legacyId(note.id,"list"),note.items,note.hideChecked));
        else blocks.add(Block.text(legacyId(note.id,"text"),new RichText(note.body,note.marks)));
        for(NoteImage image:note.images)blocks.add(Block.visual(legacyId(note.id,"image:"+image.id),image.name,"Drawing".equals(image.name)?Kind.DRAWING:Kind.IMAGE,image));
        if(!note.diagram.nodes.isEmpty())blocks.add(Block.diagram(legacyId(note.id,"diagram"),"Diagram01",note.diagram));
        return new DocumentContent(blocks);
    }
    private static String legacyId(String note,String suffix){return UUID.nameUUIDFromBytes((note.length()+":"+note+":"+suffix).getBytes(StandardCharsets.UTF_8)).toString();}
    public DocumentContent insert(int edge,Block block){
        if(edge<0 || edge>blocks.size())throw new IllegalArgumentException("Invalid content insertion edge");
        List<Block> next=new ArrayList<>(blocks);next.add(edge,block);return new DocumentContent(next);
    }
    public DocumentContent replace(String id,Block replacement){
        int at=index(id);if(replacement==null || !id.equals(replacement.id))throw new IllegalArgumentException("Replacement must preserve block identity");
        if(blocks.get(at)==replacement)return this;
        List<Block> next=new ArrayList<>(blocks);next.set(at,replacement);return new DocumentContent(next);
    }
    /** Edge semantics match checklist drag: the insertion edge refers to the original order. */
    public DocumentContent move(String id,int edge){
        if(edge<0 || edge>blocks.size())throw new IllegalArgumentException("Invalid content insertion edge");
        int from=index(id),to=edge>from?edge-1:edge;if(from==to)return this;
        List<Block> next=new ArrayList<>(blocks);Block block=next.remove(from);next.add(to,block);return new DocumentContent(next);
    }
    public String nextVisualName(Kind kind){
        String prefix;
        switch(kind){case IMAGE:prefix="Image";break;case DRAWING:prefix="Drawing";break;case DIAGRAM:prefix="Diagram";break;case TABLE:prefix="Table";break;default:throw new IllegalArgumentException("Not a named visual");}
        Set<String> names=new HashSet<>();for(Block block:blocks)if(block.kind==kind)names.add(block.name);
        for(int i=1;i<=MAX_BLOCKS+1;i++){String candidate=prefix+String.format(Locale.ROOT,"%02d",i);if(!names.contains(candidate))return candidate;}
        throw new IllegalStateException("No visual name available");
    }
    private int index(String id){for(int i=0;i<blocks.size();i++)if(blocks.get(i).id.equals(id))return i;throw new IllegalArgumentException("Content is no longer available");}
}
