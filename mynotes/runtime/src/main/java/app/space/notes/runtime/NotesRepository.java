package app.space.notes.runtime;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;

/** Private-storage first slice. Call only on the serial worker. SAF adapter is pending. */
public final class NotesRepository {
    private final AtomicFile file;
    public NotesRepository(Context context) {
        File dir=new File(context.getFilesDir(),"notes-library");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create Notes directory");
        file=new AtomicFile(new File(dir,"library-v1.json"));
    }
    public List<Note> load() throws Exception {
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile()+".bak").exists())
            return new ArrayList<>();
        JSONObject root=new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8));
        if(root.getInt("version")!=1) throw new IOException("Unsupported library format; data left untouched");
        JSONArray array=root.getJSONArray("notes"); List<Note> notes=new ArrayList<>();
        Set<String> ids=new HashSet<>();
        for(int i=0;i<array.length();i++) {
            JSONObject n=array.getJSONObject(i);
            Note note=new Note(n.getString("id"),n.getString("title"),n.getString("body"),
                n.getLong("createdAt"),n.getLong("updatedAt"),n.getLong("deletedAt"),
                n.getBoolean("pinned"),n.getBoolean("archived"),n.getInt("color"));
            if(!ids.add(note.id)) throw new IOException("Duplicate note ID; data left untouched");
            notes.add(note);
        }
        return notes;
    }
    public void save(List<Note> notes) throws Exception {
        JSONArray array=new JSONArray();
        for(Note n:notes) array.put(new JSONObject().put("id",n.id).put("title",n.title)
            .put("body",n.body).put("createdAt",n.createdAt).put("updatedAt",n.updatedAt)
            .put("deletedAt",n.deletedAt).put("pinned",n.pinned).put("archived",n.archived).put("color",n.color));
        byte[] bytes=new JSONObject().put("version",1).put("notes",array).toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream stream=null;
        try {
            stream=file.startWrite(); stream.write(bytes); file.finishWrite(stream); stream=null;
            if(!Arrays.equals(bytes,file.readFully())) throw new IOException("Save readback failed");
        } catch(Exception e) { if(stream!=null) file.failWrite(stream); throw e; }
    }
}
