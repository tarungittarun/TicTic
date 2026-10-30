package app.space.notes.domain;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Ordered immutable content draft. Deliberately NOT written by the schema6 codec.
 * Never install it as a second editable body alongside Note.body/items/images.
 */
public final class DocumentContent {
    public static final int MAX_BLOCKS=4096,MAX_EDITABLE_FIELDS=2048;
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
        long fields=0;for(Block block:blocks){
            if(block==null || !ids.add(block.id))throw new IllegalArgumentException("Duplicate or missing content block");
            fields+=(block.text!=null?1:0)+block.items.size()+(block.table==null?0:(long)block.table.rows.size()*block.table.columns);if(fields>MAX_EDITABLE_FIELDS)throw new IllegalArgumentException("Too many editable fields in document");
            for(Note.Item row:block.items)if(!rows.add(row.id))throw new IllegalArgumentException("Duplicate checklist row across sections");
            if(block.image!=null && !images.add(block.image.id))throw new IllegalArgumentException("Duplicate visual reference");
        }
        this.blocks=Collections.unmodifiableList(new ArrayList<>(blocks));
    }
    /** Read-only deterministic projection. Does not save, rewrite, rename or flatten the source. */
    public static DocumentContent fromLegacy(Note note){
        if(note==null)throw new IllegalArgumentException("Missing source note");
        if(note.content!=null)return note.content;
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
    /** Convert exactly the selected UTF-16 range; retain all surrounding text and inline marks. */
    public DocumentContent selectionToChecklist(String id,int start,int end){
        Block source=block(id);if(source.text==null || start==end)throw new IllegalArgumentException("Select text in a text section first");
        List<RichText> slices=source.text.selection(start,end);RichText selected=slices.get(1);
        List<Note.Item> rows=new ArrayList<>();int at=0;
        for(String line:selected.text.split("\n",-1)){RichText row=selected.slice(at,at+line.length());rows.add(new Note.Item(UUID.randomUUID().toString(),row.text,false,0,row.marks));at+=line.length()+1;}
        List<Block> next=new ArrayList<>(blocks);int position=index(id);next.remove(position);
        next.add(position,Block.text(source.id,slices.get(0)));next.add(position+1,Block.checklist(UUID.randomUUID().toString(),rows,false));next.add(position+2,Block.text(UUID.randomUUID().toString(),slices.get(2)));
        return new DocumentContent(next);
    }
    public DocumentContent remove(String id){List<Block> next=new ArrayList<>(blocks);next.remove(index(id));return new DocumentContent(next);}
    public Block block(String id){return blocks.get(index(id));}
    public List<NoteImage> images(){List<NoteImage> result=new ArrayList<>();for(Block block:blocks)if(block.image!=null)result.add(block.image);return Collections.unmodifiableList(result);}
    /** Legacy preview only. Editors must address a diagram block explicitly. */
    public Diagram diagramProjection(){for(Block block:blocks)if(block.diagram!=null)return block.diagram;return Diagram.empty();}
    public DocumentContent withDiagram(Diagram value){
        Block target=null;for(Block block:blocks)if(block.diagram!=null){if(target!=null)throw new IllegalStateException("Choose a diagram section");target=block;}
        return target==null?insert(blocks.size(),Block.diagram(UUID.randomUUID().toString(),nextVisualName(Kind.DIAGRAM),value)):replace(target.id,Block.diagram(target.id,target.name,value));
    }
    public DocumentContent withImages(List<NoteImage> images){
        if(images==null || images.size()>8)throw new IllegalArgumentException("Maximum eight visuals per note");
        Map<String,NoteImage> remaining=new LinkedHashMap<>();for(NoteImage image:images)if(image==null || remaining.put(image.id,image)!=null)throw new IllegalArgumentException("Duplicate or missing visual");
        List<Block> next=new ArrayList<>();
        for(Block block:blocks){if(block.image==null){next.add(block);continue;}NoteImage image=remaining.remove(block.image.id);if(image!=null)next.add(Block.visual(block.id,block.name,block.kind,image));}
        DocumentContent result=new DocumentContent(next);
        for(NoteImage image:remaining.values()){Kind kind="Drawing".equals(image.name)?Kind.DRAWING:Kind.IMAGE;result=result.insert(result.blocks.size(),Block.visual(UUID.randomUUID().toString(),result.nextVisualName(kind),kind,image));}
        return result;
    }
    public String plainText(){
        List<String> pieces=new ArrayList<>();for(Block block:blocks){
            if(block.text!=null)pieces.add(block.text.text);
            else if(block.kind==Kind.CHECKLIST){StringBuilder rows=new StringBuilder();for(Note.Item item:block.items){if(rows.length()>0)rows.append('\n');for(int i=0;i<item.depth;i++)rows.append("  ");rows.append(item.checked?"[x] ":"[ ] ").append(item.text);}pieces.add(rows.toString());}
            else if(block.diagram!=null)pieces.add(block.name+"\n"+block.diagram.plainText());
            else if(block.table!=null){List<String> rows=new ArrayList<>();for(List<String> row:block.table.rows)rows.add(String.join("\t",row));pieces.add(block.name+"\n"+String.join("\n",rows));}
            else pieces.add(block.name);
        }return String.join("\n",pieces);
    }
    /** Length-delimited canonical representation; never a human-facing export or log payload. */
    public String fingerprint(){
        StringBuilder out=new StringBuilder();part(out,Integer.toString(blocks.size()));for(Block block:blocks){part(out,block.id);part(out,block.kind.name());part(out,block.name);
            if(block.text!=null){part(out,block.text.text);part(out,Integer.toString(block.text.marks.size()));for(Note.Mark mark:block.text.marks){part(out,mark.type);part(out,Integer.toString(mark.start));part(out,Integer.toString(mark.end));part(out,mark.value);}part(out,"end marks");}
            if(block.kind==Kind.CHECKLIST){part(out,Boolean.toString(block.hideChecked));part(out,Integer.toString(block.items.size()));for(Note.Item item:block.items){part(out,item.id);part(out,item.text);part(out,Boolean.toString(item.checked));part(out,Integer.toString(item.depth));part(out,Integer.toString(item.marks.size()));for(Note.Mark mark:item.marks){part(out,mark.type);part(out,Integer.toString(mark.start));part(out,Integer.toString(mark.end));part(out,mark.value);}}part(out,"end rows");}
            if(block.image!=null)part(out,block.image.fingerprint());
            if(block.diagram!=null)part(out,block.diagram.fingerprint());
            if(block.table!=null){part(out,Integer.toString(block.table.columns));part(out,Integer.toString(block.table.rows.size()));for(List<String> row:block.table.rows)for(String cell:row)part(out,cell);}
        }return out.toString();
    }
    private static void part(StringBuilder out,String text){out.append(text.length()).append(':').append(text);}
    private int index(String id){for(int i=0;i<blocks.size();i++)if(blocks.get(i).id.equals(id))return i;throw new IllegalArgumentException("Content is no longer available");}
}
