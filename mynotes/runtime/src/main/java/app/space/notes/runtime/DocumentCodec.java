package app.space.notes.runtime;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;
import app.space.notes.domain.Diagram;

/** Legacy-only libraries stay schema6; canonical content uses schema7. Reads 1–7 and refuses future formats. */
public final class DocumentCodec {
    private DocumentCodec() { }
    public static byte[] encode(List<Note> notes) throws Exception {
        JSONArray array=new JSONArray(); Set<String> ids=new HashSet<>();int schema=6;
        for(Note n:notes) {
            if(!ids.add(n.id))throw new IOException("Duplicate note ID");
            if(n.content!=null){schema=7;JSONArray voices=new JSONArray(),labels=new JSONArray();for(String source:n.voiceSources)voices.put(source);for(String label:n.labels)labels.put(label);
                array.put(new JSONObject().put("id",n.id).put("title",n.title).put("createdAt",n.createdAt).put("updatedAt",n.updatedAt).put("deletedAt",n.deletedAt).put("pinned",n.pinned).put("archived",n.archived).put("color",n.color).put("labels",labels).put("voiceSources",voices).put("reminderAt",n.reminderAt).put("folder",n.folder).put("background",n.background).put("content",ContentCodec.encode(n.content)));continue;}
            JSONArray items=new JSONArray(),marks=new JSONArray(),labels=new JSONArray(),voices=new JSONArray();
            JSONArray images=new JSONArray();for(app.space.notes.domain.NoteImage image:n.images)images.put(new JSONObject().put("id",image.id).put("name",image.name).put("mime",image.mime).put("width",image.width).put("height",image.height).put("data",image.data));
            for(String source:n.voiceSources)voices.put(source);
            for(Note.Item item:n.items)items.put(new JSONObject().put("id",item.id).put("text",item.text).put("checked",item.checked).put("depth",item.depth));
            for(Note.Mark mark:n.marks){if(!mark.value.isEmpty())schema=7;marks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));}
            for(String label:n.labels)labels.put(label);
            array.put(new JSONObject().put("id",n.id).put("title",n.title)
                .put("body",n.body).put("createdAt",n.createdAt).put("updatedAt",n.updatedAt)
                .put("deletedAt",n.deletedAt).put("pinned",n.pinned).put("archived",n.archived).put("color",n.color).put("checklist",n.checklist).put("hideChecked",n.hideChecked).put("items",items).put("marks",marks).put("labels",labels).put("diagram",encodeDiagram(n.diagram)).put("voiceSources",voices).put("reminderAt",n.reminderAt).put("folder",n.folder).put("background",n.background).put("images",images));
        }
        return new JSONObject().put("version",schema).put("notes",array).toString().getBytes(StandardCharsets.UTF_8);
    }
    static JSONObject encodeDiagram(Diagram diagram) throws Exception {
        JSONArray nodes=new JSONArray(),edges=new JSONArray();
        for(Diagram.Node n:diagram.nodes)nodes.put(new JSONObject().put("id",n.id).put("shape",n.shape).put("text",n.text).put("x",n.x).put("y",n.y).put("width",n.width).put("height",n.height).put("color",n.color).put("group",n.group));
        for(Diagram.Edge e:diagram.edges)edges.put(new JSONObject().put("id",e.id).put("from",e.from).put("to",e.to).put("label",e.label));
        return new JSONObject().put("nodes",nodes).put("edges",edges);
    }
    static Diagram decodeDiagram(JSONObject value) throws Exception {
        List<Diagram.Node> nodes=new ArrayList<>();List<Diagram.Edge> edges=new ArrayList<>();JSONArray ns=value.getJSONArray("nodes"),es=value.getJSONArray("edges");
        for(int i=0;i<ns.length();i++){JSONObject n=ns.getJSONObject(i);nodes.add(new Diagram.Node(n.getString("id"),n.getString("shape"),n.getString("text"),n.getInt("x"),n.getInt("y"),n.getInt("width"),n.getInt("height"),n.getInt("color"),n.optString("group","")));}
        for(int i=0;i<es.length();i++){JSONObject e=es.getJSONObject(i);edges.add(new Diagram.Edge(e.getString("id"),e.getString("from"),e.getString("to"),e.getString("label")));}
        return new Diagram(nodes,edges);
    }
    public static List<Note> decode(byte[] bytes) throws Exception {
        app.space.notes.domain.JsonLimits.require(bytes);
        JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        int version=root.getInt("version");
        if(version!=1 && version!=2 && version!=3 && version!=4 && version!=5 && version!=6 && version!=7)throw new IOException("Unsupported library format; data left untouched");
        JSONArray array=root.getJSONArray("notes"); List<Note> notes=new ArrayList<>(); Set<String> ids=new HashSet<>();
        for(int i=0;i<array.length();i++) {
            JSONObject n=array.getJSONObject(i);
            if(version==7 && n.has("content")){
                ContentCodec.metadata(n);
                List<String> labels=new ArrayList<>(),voices=new ArrayList<>();JSONArray ls=n.getJSONArray("labels"),vs=n.getJSONArray("voiceSources");for(int j=0;j<ls.length();j++)labels.add(ls.getString(j));for(int j=0;j<vs.length();j++)voices.add(vs.getString(j));
                Note canonical=new Note(n.getString("id"),n.getString("title"),"",n.getLong("createdAt"),n.getLong("updatedAt"),n.getLong("deletedAt"),n.getBoolean("pinned"),n.getBoolean("archived"),n.getInt("color"),false,false,Collections.emptyList(),Collections.emptyList(),labels,Diagram.empty(),voices,n.getLong("reminderAt"),n.getString("folder"),n.getInt("background"),Collections.emptyList(),ContentCodec.decode(n.getJSONObject("content")));
                if(!ids.add(canonical.id))throw new IOException("Duplicate note ID; data left untouched");notes.add(canonical);continue;
            }
            List<Note.Item> items=new ArrayList<>();List<Note.Mark> marks=new ArrayList<>();List<String> labels=new ArrayList<>(),voices=new ArrayList<>();
            if(version>=2) {
                JSONArray rows=n.getJSONArray("items"),spans=n.getJSONArray("marks"),tags=n.getJSONArray("labels");
                for(int j=0;j<rows.length();j++){JSONObject r=rows.getJSONObject(j);items.add(new Note.Item(r.getString("id"),r.getString("text"),r.getBoolean("checked"),r.getInt("depth")));}
                for(int j=0;j<spans.length();j++){JSONObject m=spans.getJSONObject(j);marks.add(new Note.Mark(m.getString("type"),m.getInt("start"),m.getInt("end"),m.optString("value","")));}
                for(int j=0;j<tags.length();j++)labels.add(tags.getString(j));
            }
            if(version>=4){JSONArray sources=n.getJSONArray("voiceSources");for(int j=0;j<sources.length();j++)voices.add(sources.getString(j));}
            List<app.space.notes.domain.NoteImage> images=new ArrayList<>();JSONArray media=version>=6?n.optJSONArray("images"):null;if(media!=null){if(media.length()>8)throw new IOException("Too many images in note");for(int j=0;j<media.length();j++){JSONObject image=media.getJSONObject(j);images.add(new app.space.notes.domain.NoteImage(image.getString("id"),image.getString("name"),image.getString("mime"),image.getInt("width"),image.getInt("height"),image.getString("data")));}}
            Note note=new Note(n.getString("id"),n.getString("title"),n.getString("body"),
                n.getLong("createdAt"),n.getLong("updatedAt"),n.getLong("deletedAt"),
                n.getBoolean("pinned"),n.getBoolean("archived"),n.getInt("color"),version>=2 && n.getBoolean("checklist"),
                version>=2 && n.getBoolean("hideChecked"),items,marks,labels,version>=3?decodeDiagram(n.getJSONObject("diagram")):Diagram.empty(),voices,version>=5?n.getLong("reminderAt"):0,version>=6?n.getString("folder"):"",version>=6?n.getInt("background"):0,images);
            if(!ids.add(note.id))throw new IOException("Duplicate note ID; data left untouched");
            notes.add(note);
        }
        return notes;
    }
}
