package app.space.notes.runtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import app.space.notes.domain.Note;

/** Serial-worker only. Private recovery journal survives a failed selected-folder save. */
public final class NotesRepository {
    private final AtomicFile file;
    private final Context context;
    private final SharedPreferences settings;
    private String selected, head;
    private volatile String status="Device storage";
    public NotesRepository(Context context) {
        this.context=context;
        settings=context.getSharedPreferences("mynotes-storage-v1",Context.MODE_PRIVATE);
        selected=settings.getString("tree",null);
        File dir=new File(context.getFilesDir(),"notes-library");
        if(!dir.exists() && !dir.mkdirs())throw new IllegalStateException("Cannot create Notes directory");
        file=new AtomicFile(new File(dir,"library-v1.json"));
    }
    public String status() { return status; }
    private boolean exists() {
        return file.getBaseFile().exists() || new File(file.getBaseFile()+".bak").exists();
    }
    private GenerationProtocol protocol(String uri) throws Exception {
        return new GenerationProtocol(new LocalTreeBackend(context.getContentResolver(),Uri.parse(uri)));
    }
    public List<Note> load() throws Exception {
        JSONObject local=exists()?new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8)):null;
        byte[] cached=local==null?null:local.toString().getBytes(StandardCharsets.UTF_8);
        if(cached!=null)DocumentCodec.decode(cached); // Never silently initialise over corrupt local data.
        if(selected==null) { status="Device storage";return cached==null?new ArrayList<>():DocumentCodec.decode(cached); }
        head=local==null?null:local.optString("folderHead",null);
        boolean pending=local!=null && local.optBoolean("pending",false);
        try {
            GenerationProtocol.Snapshot external=protocol(selected).read();
            if(external.payload==null)throw new IOException("Selected library is empty or missing");
            if(pending && !Arrays.equals(DocumentCodec.encode(DocumentCodec.decode(cached)),external.payload)) {
                status="Recovered locally · folder save pending";
                return DocumentCodec.decode(cached); // Preserve expected old head; retry cannot clobber a foreign writer.
            }
            head=external.head; writeLocal(external.payload,false,head);
            status="Selected local folder"; return DocumentCodec.decode(external.payload);
        } catch(Exception e) {
            if(cached==null)throw e;
            status="Folder unavailable · using private recovery copy";
            return DocumentCodec.decode(cached);
        }
    }
    public void save(List<Note> notes) throws Exception {
        byte[] payload=DocumentCodec.encode(notes);
        if(selected==null) {writeLocal(payload,false,null);status="Device storage";return;}
        writeLocal(payload,true,head); // Durable local recovery BEFORE trying the external destination.
        try {
            GenerationProtocol.Snapshot saved=protocol(selected).commit(payload,head);
            head=saved.head; writeLocal(payload,false,head); status="Selected local folder";
        } catch(Exception e) {status="Saved to private recovery · folder update pending";throw e;}
    }
    public void selectFolder(Uri uri,List<Note> notes) throws Exception {
        String target=uri.toString();
        if(target.equals(selected)) {save(notes);return;}
        GenerationProtocol destination=protocol(target);
        if(destination.read().head!=null)throw new IOException("This folder contains a library. Choose a new empty folder; existing data was not overwritten.");
        byte[] payload=DocumentCodec.encode(notes);
        GenerationProtocol.Snapshot saved=destination.commit(payload,null);
        // Destination verified first. Source generations are never deleted automatically.
        if(!settings.edit().putString("tree",target).commit())throw new IOException("Could not store folder selection; previous destination retained");
        selected=target;head=saved.head;
        writeLocal(payload,false,head);status="Selected local folder";
    }
    public void usePrivate(List<Note> notes) throws Exception {
        writeLocal(DocumentCodec.encode(notes),false,null);
        if(!settings.edit().remove("tree").commit())throw new IOException("Could not change storage setting");
        selected=null;head=null;status="Device storage · previous folder retained";
    }
    private void writeLocal(byte[] payload,boolean pending,String folderHead) throws Exception {
        JSONObject root=new JSONObject(new String(payload,StandardCharsets.UTF_8));
        root.put("pending",pending).put("folderHead",folderHead==null?JSONObject.NULL:folderHead);
        byte[] bytes=root.toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream stream=null;
        try {
            stream=file.startWrite();stream.write(bytes);file.finishWrite(stream);stream=null;
            if(!Arrays.equals(bytes,file.readFully()))throw new IOException("Save readback failed");
        } catch(Exception e) {if(stream!=null)file.failWrite(stream);throw e;}
    }
}
