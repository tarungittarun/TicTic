package app.space.notes.runtime;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;

/** Version 1 reader also accepts the initial 0.1.0 library; no destructive migration. */
public final class DocumentCodec {
    private DocumentCodec() { }
    public static byte[] encode(List<Note> notes) throws Exception {
        JSONArray array=new JSONArray(); Set<String> ids=new HashSet<>();
        for(Note n:notes) {
            if(!ids.add(n.id))throw new IOException("Duplicate note ID");
            array.put(new JSONObject().put("id",n.id).put("title",n.title)
                .put("body",n.body).put("createdAt",n.createdAt).put("updatedAt",n.updatedAt)
                .put("deletedAt",n.deletedAt).put("pinned",n.pinned).put("archived",n.archived).put("color",n.color));
        }
        return new JSONObject().put("version",1).put("notes",array).toString().getBytes(StandardCharsets.UTF_8);
    }
    public static List<Note> decode(byte[] bytes) throws Exception {
        JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(root.getInt("version")!=1)throw new IOException("Unsupported library format; data left untouched");
        JSONArray array=root.getJSONArray("notes"); List<Note> notes=new ArrayList<>(); Set<String> ids=new HashSet<>();
        for(int i=0;i<array.length();i++) {
            JSONObject n=array.getJSONObject(i);
            Note note=new Note(n.getString("id"),n.getString("title"),n.getString("body"),
                n.getLong("createdAt"),n.getLong("updatedAt"),n.getLong("deletedAt"),
                n.getBoolean("pinned"),n.getBoolean("archived"),n.getInt("color"));
            if(!ids.add(note.id))throw new IOException("Duplicate note ID; data left untouched");
            notes.add(note);
        }
        return notes;
    }
}
