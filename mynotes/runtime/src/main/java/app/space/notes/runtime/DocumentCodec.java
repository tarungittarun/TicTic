package app.space.notes.runtime;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;
import app.space.notes.domain.Diagram;

/** Schemas12–13 add diagram styling/shapes and retained pre-edit image pixels. Reads 1–13. */
public final class DocumentCodec {
    private DocumentCodec() { }
    public static byte[] encode(List<Note> notes) throws Exception {
        int schema=6;
        for(Note note:notes){
            if(note.content!=null){
                schema=Math.max(schema,7);
                if(note.content.notesWorkspace)schema=Math.max(schema,10);
                for(app.space.notes.domain.DocumentContent.Block block:note.content.blocks){
                    if(block.table!=null && block.table.columnWidth!=160)schema=Math.max(schema,9);
                    if(block.kind==app.space.notes.domain.DocumentContent.Kind.CHECKLIST && !block.checklistAppearance.isDefault())schema=Math.max(schema,11);
                    if(block.diagram!=null && requiresDiagramSchema(block.diagram))schema=Math.max(schema,12);
                    if(block.image!=null && block.image.original!=null)schema=Math.max(schema,13);
                }
            }else{
                if(requiresDiagramSchema(note.diagram))schema=Math.max(schema,12);
                for(app.space.notes.domain.NoteImage image:note.images)if(image.original!=null)schema=Math.max(schema,13);
            }
            if(!note.contentNames.isEmpty())schema=Math.max(schema,8);
            if(note.content==null)for(Note.Mark mark:note.marks)if(!mark.value.isEmpty())schema=Math.max(schema,7);
        }
        JSONArray array=new JSONArray(); Set<String> ids=new HashSet<>();
        for(Note n:notes) {
            if(!ids.add(n.id))throw new IOException("Duplicate note ID");
            if(n.content!=null){JSONArray voices=new JSONArray(),labels=new JSONArray();for(String source:n.voiceSources)voices.put(source);for(String label:n.labels)labels.put(label);
                array.put(new JSONObject().put("id",n.id).put("title",n.title).put("createdAt",n.createdAt).put("updatedAt",n.updatedAt).put("deletedAt",n.deletedAt).put("pinned",n.pinned).put("archived",n.archived).put("color",n.color).put("labels",labels).put("voiceSources",voices).put("reminderAt",n.reminderAt).put("folder",n.folder).put("background",n.background).put("content",ContentCodec.encode(n.content,schema)));continue;}
            JSONArray items=new JSONArray(),marks=new JSONArray(),labels=new JSONArray(),voices=new JSONArray();
            JSONArray images=new JSONArray();for(app.space.notes.domain.NoteImage image:n.images)images.put(encodeImage(image,schema>=13));
            for(String source:n.voiceSources)voices.put(source);
            for(Note.Item item:n.items)items.put(new JSONObject().put("id",item.id).put("text",item.text).put("checked",item.checked).put("depth",item.depth));
            for(Note.Mark mark:n.marks)marks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));
            for(String label:n.labels)labels.put(label);
            array.put(new JSONObject().put("id",n.id).put("title",n.title)
                .put("body",n.body).put("createdAt",n.createdAt).put("updatedAt",n.updatedAt)
                .put("deletedAt",n.deletedAt).put("pinned",n.pinned).put("archived",n.archived).put("color",n.color).put("checklist",n.checklist).put("hideChecked",n.hideChecked).put("items",items).put("marks",marks).put("labels",labels).put("diagram",encodeDiagram(n.diagram,schema>=12)).put("voiceSources",voices).put("reminderAt",n.reminderAt).put("folder",n.folder).put("background",n.background).put("images",images));
        }
        for(int i=0;i<notes.size();i++)if(!notes.get(i).contentNames.isEmpty())array.getJSONObject(i).put("contentNames",new JSONObject(notes.get(i).contentNames));
        return new JSONObject().put("version",schema).put("notes",array).toString().getBytes(StandardCharsets.UTF_8);
    }
    private static Map<String,String> decodeNames(JSONObject note,int version)throws Exception{
        Map<String,String> names=new TreeMap<>();if(!note.has("contentNames"))return names;
        if(version<8 || !(note.get("contentNames") instanceof JSONObject))throw new IOException("Unsupported content names");JSONObject object=note.getJSONObject("contentNames");if(object.length()>10)throw new IOException("Too many content names");Iterator<String> keys=object.keys();while(keys.hasNext()){String key=keys.next();Object name=object.get(key);if(!(name instanceof String))throw new IOException("Content name must be text");names.put(key,(String)name);}return names;
    }
    private static boolean requiresDiagramSchema(Diagram diagram){for(Diagram.Node node:diagram.nodes)if(node.font!=0 || node.strokeWidth!=3 || Diagram.SHAPES.indexOf(node.shape)>=21)return true;return false;}
    static JSONObject encodeImage(app.space.notes.domain.NoteImage image,boolean includeOriginal)throws Exception{
        JSONObject out=new JSONObject().put("id",image.id).put("name",image.name).put("mime",image.mime).put("width",image.width).put("height",image.height).put("data",image.data);
        if(includeOriginal && image.original!=null)out.put("original",new JSONObject().put("mime",image.original.mime).put("width",image.original.width).put("height",image.original.height).put("data",image.original.data));return out;
    }
    static app.space.notes.domain.NoteImage decodeImage(JSONObject value,int version)throws Exception{
        Set<String> base=new HashSet<>(Arrays.asList("id","name","mime","width","height","data"));
        if(value.has("original")){if(version<13 || !(value.get("original") instanceof JSONObject))throw new IOException("Unsupported retained image source");ContentCodec.keys(value,"id","name","mime","width","height","data","original");JSONObject original=value.getJSONObject("original");ContentCodec.keys(original,"mime","width","height","data");app.space.notes.domain.NoteImage.Original source=new app.space.notes.domain.NoteImage.Original(ContentCodec.string(original,"mime"),ContentCodec.integer(original,"width"),ContentCodec.integer(original,"height"),ContentCodec.string(original,"data"));return new app.space.notes.domain.NoteImage(ContentCodec.string(value,"id"),ContentCodec.string(value,"name"),ContentCodec.string(value,"mime"),ContentCodec.integer(value,"width"),ContentCodec.integer(value,"height"),ContentCodec.string(value,"data"),source);}
        ContentCodec.keys(value,"id","name","mime","width","height","data");return new app.space.notes.domain.NoteImage(ContentCodec.string(value,"id"),ContentCodec.string(value,"name"),ContentCodec.string(value,"mime"),ContentCodec.integer(value,"width"),ContentCodec.integer(value,"height"),ContentCodec.string(value,"data"));
    }
    static JSONObject encodeDiagram(Diagram diagram,boolean includeStyle) throws Exception {
        JSONArray nodes=new JSONArray(),edges=new JSONArray();
        for(Diagram.Node n:diagram.nodes){JSONObject node=new JSONObject().put("id",n.id).put("shape",n.shape).put("text",n.text).put("x",n.x).put("y",n.y).put("width",n.width).put("height",n.height).put("color",n.color).put("group",n.group);if(includeStyle)node.put("font",n.font).put("strokeWidth",n.strokeWidth);nodes.put(node);}
        for(Diagram.Edge e:diagram.edges)edges.put(new JSONObject().put("id",e.id).put("from",e.from).put("to",e.to).put("label",e.label));
        return new JSONObject().put("nodes",nodes).put("edges",edges);
    }
    static Diagram decodeDiagram(JSONObject value,int version) throws Exception {
        List<Diagram.Node> nodes=new ArrayList<>();List<Diagram.Edge> edges=new ArrayList<>();JSONArray ns=value.getJSONArray("nodes"),es=value.getJSONArray("edges");
        for(int i=0;i<ns.length();i++){JSONObject n=ns.getJSONObject(i);nodes.add(new Diagram.Node(n.getString("id"),n.getString("shape"),n.getString("text"),n.getInt("x"),n.getInt("y"),n.getInt("width"),n.getInt("height"),n.getInt("color"),n.optString("group",""),version>=12?n.getInt("font"):0,version>=12?n.getInt("strokeWidth"):3));}
        for(int i=0;i<es.length();i++){JSONObject e=es.getJSONObject(i);edges.add(new Diagram.Edge(e.getString("id"),e.getString("from"),e.getString("to"),e.getString("label")));}
        return new Diagram(nodes,edges);
    }
    public static List<Note> decode(byte[] bytes) throws Exception {

        JSONObject root=new JSONObject(app.space.notes.domain.JsonLimits.text(bytes));
        int version=root.getInt("version");
        if(version<1 || version>13)throw new IOException("Unsupported library format; data left untouched");
        JSONArray array=root.getJSONArray("notes"); List<Note> notes=new ArrayList<>(); Set<String> ids=new HashSet<>();
        for(int i=0;i<array.length();i++) {
            JSONObject n=array.getJSONObject(i);
            if(version>=7 && n.has("content")){
                ContentCodec.metadata(n);
                List<String> labels=new ArrayList<>(),voices=new ArrayList<>();JSONArray ls=n.getJSONArray("labels"),vs=n.getJSONArray("voiceSources");for(int j=0;j<ls.length();j++)labels.add(ls.getString(j));for(int j=0;j<vs.length();j++)voices.add(vs.getString(j));
                Note canonical=new Note(n.getString("id"),n.getString("title"),"",n.getLong("createdAt"),n.getLong("updatedAt"),n.getLong("deletedAt"),n.getBoolean("pinned"),n.getBoolean("archived"),n.getInt("color"),false,false,Collections.emptyList(),Collections.emptyList(),labels,Diagram.empty(),voices,n.getLong("reminderAt"),n.getString("folder"),n.getInt("background"),Collections.emptyList(),ContentCodec.decode(n.getJSONObject("content"),version));
                if(version<10 && canonical.content.notesWorkspace)throw new IOException("Mixed Notes require schema10; data left untouched");
                if(version<9)for(app.space.notes.domain.DocumentContent.Block block:canonical.content.blocks)if(block.table!=null && block.table.columnWidth!=160)throw new IOException("Table sizing requires schema9; data left untouched");
                if(!ids.add(canonical.id))throw new IOException("Duplicate note ID; data left untouched");notes.add(canonical.withContentNames(decodeNames(n,version),canonical.updatedAt));continue;
            }
            List<Note.Item> items=new ArrayList<>();List<Note.Mark> marks=new ArrayList<>();List<String> labels=new ArrayList<>(),voices=new ArrayList<>();
            if(version>=2) {
                JSONArray rows=n.getJSONArray("items"),spans=n.getJSONArray("marks"),tags=n.getJSONArray("labels");
                for(int j=0;j<rows.length();j++){JSONObject r=rows.getJSONObject(j);items.add(new Note.Item(r.getString("id"),r.getString("text"),r.getBoolean("checked"),r.getInt("depth")));}
                for(int j=0;j<spans.length();j++){JSONObject m=spans.getJSONObject(j);marks.add(new Note.Mark(m.getString("type"),m.getInt("start"),m.getInt("end"),m.optString("value","")));}
                for(int j=0;j<tags.length();j++)labels.add(tags.getString(j));
            }
            if(version>=4){JSONArray sources=n.getJSONArray("voiceSources");for(int j=0;j<sources.length();j++)voices.add(sources.getString(j));}
            List<app.space.notes.domain.NoteImage> images=new ArrayList<>();JSONArray media=version>=6?n.optJSONArray("images"):null;if(media!=null){if(media.length()>8)throw new IOException("Too many images in note");for(int j=0;j<media.length();j++){JSONObject image=media.getJSONObject(j);images.add(decodeImage(image,version));}}
            Note note=new Note(n.getString("id"),n.getString("title"),n.getString("body"),
                n.getLong("createdAt"),n.getLong("updatedAt"),n.getLong("deletedAt"),
                n.getBoolean("pinned"),n.getBoolean("archived"),n.getInt("color"),version>=2 && n.getBoolean("checklist"),
                version>=2 && n.getBoolean("hideChecked"),items,marks,labels,version>=3?decodeDiagram(n.getJSONObject("diagram"),version):Diagram.empty(),voices,version>=5?n.getLong("reminderAt"):0,version>=6?n.getString("folder"):"",version>=6?n.getInt("background"):0,images);
            if(!ids.add(note.id))throw new IOException("Duplicate note ID; data left untouched");
            notes.add(note.withContentNames(decodeNames(n,version),note.updatedAt));
        }
        return notes;
    }
}
