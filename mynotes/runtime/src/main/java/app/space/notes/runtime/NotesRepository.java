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
    public interface FolderAccess { GenerationProtocol open(String uri) throws Exception; }
    private final FolderAccess folderAccess;
    private final AtomicFile file;
    private final Context context;
    private final SharedPreferences settings;
    private volatile String selected;
    private String head;
    private volatile String status="Device storage";
    public NotesRepository(Context context) { this(context,null); }
    /** Injected backend makes repository transactions testable without pretending to test SAF. */
    public NotesRepository(Context context,FolderAccess folderAccess) {
        this.context=context;this.folderAccess=folderAccess;
        settings=context.getSharedPreferences("mynotes-storage-v1",Context.MODE_PRIVATE);
        selected=settings.getString("tree",null);
        File dir=new File(context.getFilesDir(),"notes-library");
        if(!dir.exists() && !dir.mkdirs())throw new IllegalStateException("Cannot create Notes directory");
        file=new AtomicFile(new File(dir,"library-v1.json"));
    }
    public String status() {
        if(selected==null)return status;
        try {return status+" · "+android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(selected));}
        catch(Exception invalid) {return status;}
    }
    private boolean exists() {
        return file.getBaseFile().exists() || new File(file.getBaseFile()+".bak").exists();
    }
    private GenerationProtocol protocol(String uri) throws Exception {
        return folderAccess==null?new GenerationProtocol(new LocalTreeBackend(context.getContentResolver(),Uri.parse(uri))):folderAccess.open(uri);
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
    public GenerationProtocol.Snapshot inspectFolder(Uri uri) throws Exception {
        GenerationProtocol.Snapshot snapshot=protocol(uri.toString()).read();
        if(snapshot.payload==null)throw new IOException("No MyNotes library in this folder. Use Choose folder for a new library.");
        DocumentCodec.decode(snapshot.payload);return snapshot;
    }
    private AtomicFile beforeRestore() {
        return new AtomicFile(new File(file.getBaseFile().getParentFile(),"before-restore-v1.json"));
    }
    public List<Note> previousSnapshot() throws Exception {
        return DocumentCodec.decode(beforeRestore().readFully());
    }
    /** Returns false only when the full imported result is verified in private recovery. */
    public boolean applyRestore(List<Note> current,List<Note> merged) throws Exception {
        atomicWrite(beforeRestore(),DocumentCodec.encode(current));
        try {save(merged);return true;}
        catch(Exception failure) {
            if(exists() && Arrays.equals(DocumentCodec.encode(DocumentCodec.decode(file.readFully())),DocumentCodec.encode(merged))) {
                status="Restore retained privately · destination save pending";return false;
            }
            throw failure;
        }
    }
    /** Preview/confirmation is required by the controller. Older target generations remain. */
    public void openExisting(Uri uri,String expected,List<Note> current,List<Note> merged) throws Exception {
        String target=uri.toString();byte[] payload=DocumentCodec.encode(merged);
        atomicWrite(beforeRestore(),DocumentCodec.encode(current));
        GenerationProtocol.Snapshot committed=protocol(target).commit(payload,expected);
        if(!settings.edit().putString("tree",target).commit())
            throw new IOException("Destination verified, but selection could not be stored. Current library retained; target history also retained.");
        selected=target;head=committed.head;
        try {writeLocal(payload,false,head);status="Opened existing local library";}
        catch(Exception localFailure) {
            // Target and selection are already durable. Do not let stale UI state overwrite them.
            status="Opened folder · private recovery copy unavailable; check device space";
        }
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
        atomicWrite(file,bytes);
    }
    private void atomicWrite(AtomicFile target,byte[] bytes) throws Exception {
        FileOutputStream stream=null;
        try {
            stream=target.startWrite();stream.write(bytes);target.finishWrite(stream);stream=null;
            if(!Arrays.equals(bytes,target.readFully()))throw new IOException("Save readback failed");
        } catch(Exception e) {if(stream!=null)target.failWrite(stream);throw e;}
    }
}
