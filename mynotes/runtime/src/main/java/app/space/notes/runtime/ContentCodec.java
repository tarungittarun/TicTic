package app.space.notes.runtime;

import app.space.notes.domain.*;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Canonical schema7 content: pixels and tables occur once, at their ordered block position. */
final class ContentCodec {
    private ContentCodec(){}
    static JSONObject encode(DocumentContent content)throws Exception{
        JSONArray array=new JSONArray();
        for(DocumentContent.Block block:content.blocks){
            JSONObject out=new JSONObject().put("id",block.id).put("kind",block.kind.name()).put("name",block.name);
            switch(block.kind){
                case TEXT:
                    JSONArray marks=new JSONArray();for(Note.Mark mark:block.text.marks)marks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));
                    out.put("text",block.text.text).put("marks",marks);break;
                case CHECKLIST:
                    JSONArray rows=new JSONArray();for(Note.Item item:block.items){JSONArray rowMarks=new JSONArray();for(Note.Mark mark:item.marks)rowMarks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));rows.put(new JSONObject().put("id",item.id).put("text",item.text).put("checked",item.checked).put("depth",item.depth).put("marks",rowMarks));}
                    out.put("rows",rows).put("hideChecked",block.hideChecked);break;
                case IMAGE:case DRAWING:
                    NoteImage image=block.image;out.put("image",new JSONObject().put("id",image.id).put("name",image.name).put("mime",image.mime).put("width",image.width).put("height",image.height).put("data",image.data));break;
                case DIAGRAM:out.put("diagram",DocumentCodec.encodeDiagram(block.diagram));break;
                case TABLE:
                    if(block.table.hasHeader)out.put("header",true);
                    JSONArray table=new JSONArray();for(List<RichText> row:block.table.cells){JSONArray cells=new JSONArray();for(RichText cell:row){JSONArray cellMarks=new JSONArray();for(Note.Mark mark:cell.marks)cellMarks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));cells.put(new JSONObject().put("text",cell.text).put("marks",cellMarks));}table.put(cells);}out.put("rows",table);break;
                default:throw new IOException("Unsupported content kind");
            }array.put(out);
        }return new JSONObject().put("writingLines",content.writingLines).put("blocks",array);
    }
    static DocumentContent decode(JSONObject content)throws Exception{
        keys(content,"writingLines","blocks");JSONArray array=content.getJSONArray("blocks");
        if(array.length()>DocumentContent.MAX_BLOCKS)throw new IOException("Too many content sections");
        List<DocumentContent.Block> blocks=new ArrayList<>();
        for(int i=0;i<array.length();i++){
            JSONObject in=array.getJSONObject(i);String id=string(in,"id"),name=string(in,"name");DocumentContent.Kind kind=DocumentContent.Kind.valueOf(string(in,"kind"));
            switch(kind){
                case TEXT:
                    keys(in,"id","kind","name","text","marks");if(!name.isEmpty())throw new IOException("Unexpected text section name");blocks.add(DocumentContent.Block.text(id,new RichText(string(in,"text"),marks(in.getJSONArray("marks")))));break;
                case CHECKLIST:
                    keys(in,"id","kind","name","rows","hideChecked");JSONArray rs=in.getJSONArray("rows");if(rs.length()>DocumentContent.MAX_EDITABLE_FIELDS)throw new IOException("Too many checklist rows");List<Note.Item> items=new ArrayList<>();
                    for(int j=0;j<rs.length();j++){JSONObject row=rs.getJSONObject(j);keys(row,"id","text","checked","depth","marks");items.add(new Note.Item(string(row,"id"),string(row,"text"),bool(row,"checked"),integer(row,"depth"),marks(row.getJSONArray("marks"))));}
                    blocks.add(DocumentContent.Block.checklist(id,name,items,bool(in,"hideChecked")));break;
                case IMAGE:case DRAWING:
                    keys(in,"id","kind","name","image");JSONObject image=in.getJSONObject("image");keys(image,"id","name","mime","width","height","data");blocks.add(DocumentContent.Block.visual(id,name,kind,new NoteImage(string(image,"id"),string(image,"name"),string(image,"mime"),integer(image,"width"),integer(image,"height"),string(image,"data"))));break;
                case DIAGRAM:keys(in,"id","kind","name","diagram");diagram(in.getJSONObject("diagram"));blocks.add(DocumentContent.Block.diagram(id,name,DocumentCodec.decodeDiagram(in.getJSONObject("diagram"))));break;
                case TABLE:
                    if(in.has("header"))keys(in,"id","kind","name","rows","header");else keys(in,"id","kind","name","rows");JSONArray table=in.getJSONArray("rows");if(table.length()<1 || table.length()>TextTable.MAX_ROWS)throw new IOException("Invalid table dimensions");List<List<RichText>> rows=new ArrayList<>();
                    for(int j=0;j<table.length();j++){JSONArray cells=table.getJSONArray(j);if(cells.length()<1 || cells.length()>TextTable.MAX_COLUMNS || table.length()*cells.length()>TextTable.MAX_CELLS)throw new IOException("Invalid table dimensions");List<RichText> values=new ArrayList<>();for(int c=0;c<cells.length();c++){JSONObject cell=cells.getJSONObject(c);keys(cell,"text","marks");values.add(new RichText(string(cell,"text"),marks(cell.getJSONArray("marks"))));}rows.add(values);}blocks.add(DocumentContent.Block.table(id,name,TextTable.rich(rows,in.has("header") && bool(in,"header"))));break;
                default:throw new IOException("Unsupported content kind");
            }
        }return new DocumentContent(blocks,bool(content,"writingLines"));
    }
    private static void diagram(JSONObject diagram)throws Exception{
        keys(diagram,"nodes","edges");JSONArray nodes=diagram.getJSONArray("nodes"),edges=diagram.getJSONArray("edges");if(nodes.length()>DocumentContent.MAX_DIAGRAM_NODES || edges.length()>DocumentContent.MAX_DIAGRAM_EDGES)throw new IOException("Diagram safety limit exceeded");
        for(int i=0;i<nodes.length();i++){JSONObject node=nodes.getJSONObject(i);keys(node,"id","shape","text","x","y","width","height","color","group");for(String key:Arrays.asList("id","shape","text","group"))string(node,key);for(String key:Arrays.asList("x","y","width","height","color"))integer(node,key);}
        for(int i=0;i<edges.length();i++){JSONObject edge=edges.getJSONObject(i);keys(edge,"id","from","to","label");for(String key:Arrays.asList("id","from","to","label"))string(edge,key);}
    }
    private static List<Note.Mark> marks(JSONArray array)throws Exception{
        if(array.length()>RichText.MAX_MARKS)throw new IOException("Too many text styles");List<Note.Mark> marks=new ArrayList<>();for(int i=0;i<array.length();i++){JSONObject mark=array.getJSONObject(i);keys(mark,"type","start","end","value");marks.add(new Note.Mark(string(mark,"type"),integer(mark,"start"),integer(mark,"end"),string(mark,"value")));}return marks;
    }
    static void keys(JSONObject object,String... expected)throws IOException{
        Set<String> actual=new HashSet<>();Iterator<String> names=object.keys();while(names.hasNext())actual.add(names.next());if(!actual.equals(new HashSet<>(Arrays.asList(expected))))throw new IOException("Unsupported or incomplete note fields; source left untouched");
    }
    static String string(JSONObject object,String key)throws Exception{Object value=object.get(key);if(!(value instanceof String))throw new IOException("Expected note text: "+key);RichText.requireText((String)value);return (String)value;}
    static boolean bool(JSONObject object,String key)throws Exception{Object value=object.get(key);if(!(value instanceof Boolean))throw new IOException("Expected note switch: "+key);return (Boolean)value;}
    static long whole(JSONObject object,String key)throws Exception{Object value=object.get(key);if(!(value instanceof Integer) && !(value instanceof Long))throw new IOException("Expected whole note number: "+key);return ((Number)value).longValue();}
    static int integer(JSONObject object,String key)throws Exception{long value=whole(object,key);if(value<Integer.MIN_VALUE || value>Integer.MAX_VALUE)throw new IOException("Note number out of range: "+key);return (int)value;}
    static void metadata(JSONObject object)throws Exception{
        keys(object,"id","title","createdAt","updatedAt","deletedAt","pinned","archived","color","labels","voiceSources","reminderAt","folder","background","content");
        for(String key:Arrays.asList("id","title","folder"))string(object,key);for(String key:Arrays.asList("createdAt","updatedAt","deletedAt","reminderAt"))whole(object,key);for(String key:Arrays.asList("color","background"))integer(object,key);for(String key:Arrays.asList("pinned","archived"))bool(object,key);
        for(String key:Arrays.asList("labels","voiceSources")){JSONArray array=object.getJSONArray(key);for(int i=0;i<array.length();i++)if(!(array.get(i) instanceof String))throw new IOException("Expected note tag text");else RichText.requireText((String)array.get(i));}
    }
}
