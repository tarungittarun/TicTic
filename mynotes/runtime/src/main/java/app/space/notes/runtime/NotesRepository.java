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
    private static final Object SNAPSHOT_LOCK=new Object();
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
        if(!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory())throw new IllegalStateException("Cannot create Notes directory");
        file=new AtomicFile(new File(dir,"library-v1.json"));
    }
    public boolean appOnlyStorage(){return selected==null;}
    public LocalNotebookFolders folders()throws IOException{if(selected==null)throw new IOException("Choose the main local storage folder in Settings → Storage & recovery first.");return new LocalNotebookFolders(context.getContentResolver(),Uri.parse(selected));}
    /** Persist deletion intent before removing notes so provider failures can be retried after restart. */
    public void schedulePortableDeletion(Set<String> ids)throws IOException{
        if(selected==null || folderAccess!=null)return;String key="portable-delete:"+selected;Set<String> pending=new HashSet<>(settings.getStringSet(key,Collections.emptySet()));pending.addAll(ids);if(!settings.edit().putStringSet(key,pending).commit())throw new IOException("Could not journal file cleanup; nothing deleted");
    }
    private void materializeFolders(List<Note> notes)throws Exception{
        if(folderAccess==null && selected!=null){LocalNotebookFolders files=folders();files.reconcile(notes);String key="portable-delete:"+selected;Set<String> pending=new HashSet<>(settings.getStringSet(key,Collections.emptySet()));for(Note n:notes)pending.remove(n.id);files.removeOwned(pending);if(!settings.edit().remove(key).commit())throw new IOException("File cleanup completed but its acknowledgement needs retry");}
    }
    public String status() {
        if(selected==null)return status;
        try {return status+" · "+android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(selected));}
        catch(Exception invalid) {return status;}
    }
    /** Serial-worker only. Location details are UI content and must never enter diagnostics. */
    public String documentLocation(Note note)throws Exception{
        String recovery="Private library file:\n"+file.getBaseFile().getAbsolutePath()+"\n\nThis is a library containing multiple notes, not an individual note file. It is accessible only to MyNotes under normal Android permissions.";
        if(selected==null)return "Storage: app-only\n\n"+recovery;
        String folder;try{folder=folders().location(note);}catch(Exception unavailable){folder="Folder copy cannot be verified now. Reconnect the selected storage folder and retry save. Do not clear app data.";}
        return folder+"\n\nRecovery: "+recovery;
    }
    private boolean exists() {
        return file.getBaseFile().exists() || new File(file.getBaseFile()+".bak").exists();
    }
    private GenerationProtocol protocol(String uri) throws Exception {
        return folderAccess==null?new GenerationProtocol(new LocalTreeBackend(context.getContentResolver(),Uri.parse(uri))):folderAccess.open(uri);
    }
    public List<Note> load() throws Exception {
        JSONObject local=exists()?new JSONObject(new String(atomicRead(file),StandardCharsets.UTF_8)):null;
        byte[] cached=local==null?null:local.toString().getBytes(StandardCharsets.UTF_8);
        if(cached!=null)DocumentCodec.decode(cached); // Never silently initialise over corrupt local data.
        if(selected==null) {status="Device storage";List<Note> notes=cached==null?new ArrayList<>():DocumentCodec.decode(cached);updateSurfaces(notes);return notes;}
        head=local==null?null:local.optString("folderHead",null);
        boolean pending=local!=null && local.optBoolean("pending",false);
        try {
            GenerationProtocol.Snapshot external=protocol(selected).read();
            if(external.payload==null)throw new IOException("Selected library is empty or missing");
            if(pending && !Arrays.equals(DocumentCodec.encode(DocumentCodec.decode(cached)),external.payload)) {
                status="Recovered locally · folder save pending";
                return DocumentCodec.decode(cached); // Preserve expected old head; retry cannot clobber a foreign writer.
            }
            head=external.head; if(pending)materializeFolders(DocumentCodec.decode(external.payload));writeLocal(external.payload,false,head);
            status="Selected local folder"; return DocumentCodec.decode(external.payload);
        } catch(Exception e) {
            if(cached==null)throw e;
            status="Folder unavailable · using private recovery copy";
            return DocumentCodec.decode(cached);
        }
    }
    /** Private atomic snapshot only: receivers/widgets never block on external providers. */
    public List<Note> localSnapshot() throws Exception {return exists()?DocumentCodec.decode(atomicRead(file)):new ArrayList<>();}
    private void updateSurfaces(List<Note> notes){try{LocalSystemSurfaces.refresh(context);}catch(Exception unavailable){android.util.Log.w("MyNotes","OS surfaces unavailable; saved notes retained",unavailable);}}
    public void save(List<Note> notes) throws Exception {
        byte[] payload=DocumentCodec.encode(notes);
        if(selected==null) {writeLocal(payload,false,null);status="Device storage";return;}
        writeLocal(payload,true,head); // Durable local recovery BEFORE trying the external destination.
        try {
            GenerationProtocol.Snapshot saved=protocol(selected).commit(payload,head);
            head=saved.head;materializeFolders(notes);writeLocal(payload,false,head); status="Selected local folder";
        } catch(Exception e) {writeLocal(payload,true,head);status="Saved to private recovery · folder update pending";throw e;}
    }
    public void selectFolder(Uri uri,List<Note> notes) throws Exception {
        String target=uri.toString();
        if(target.equals(selected)) {save(notes);return;}
        GenerationProtocol destination=protocol(target);
        if(destination.read().head!=null)throw new IOException("This folder contains a library. Choose a new empty folder; existing data was not overwritten.");
        byte[] payload=DocumentCodec.encode(notes);
        GenerationProtocol.Snapshot saved=destination.commit(payload,null);
        if(folderAccess==null)new LocalNotebookFolders(context.getContentResolver(),uri).reconcile(notes);
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
    private AtomicFile beforeDocumentUpgrade(){return new AtomicFile(new File(file.getBaseFile().getParentFile(),"before-document-editor-v7.json"));}
    public List<Note> documentUpgradeSnapshot()throws Exception{return DocumentCodec.decode(atomicRead(beforeDocumentUpgrade()));}
    /** Source-pinned migration: verify round trip and immutable old checkpoint before any schema7 save. */
    public boolean upgradeDocument(List<Note> current,List<Note> candidate)throws Exception{
        byte[] expected=DocumentCodec.encode(current),encoded=DocumentCodec.encode(candidate);
        List<Note> reopened=DocumentCodec.decode(encoded);if(reopened.size()!=candidate.size())throw new IOException("Document migration verification failed");
        for(int i=0;i<reopened.size();i++)if(!app.space.notes.domain.NoteMerge.same(candidate.get(i),reopened.get(i)))throw new IOException("Document migration changed content");
        synchronized(SNAPSHOT_LOCK){
            if(!Arrays.equals(expected,DocumentCodec.encode(localSnapshot())))throw new IOException("Library changed; retry document upgrade");
            AtomicFile checkpoint=beforeDocumentUpgrade();
            if(!checkpoint.getBaseFile().exists()){atomicWrite(checkpoint,expected);if(!Arrays.equals(expected,atomicRead(checkpoint)))throw new IOException("Upgrade checkpoint verification failed");}
            DocumentCodec.decode(atomicRead(checkpoint)); // Never replace a corrupt checkpoint silently.
            try{save(candidate);return true;}catch(Exception failure){if(Arrays.equals(encoded,DocumentCodec.encode(localSnapshot())))return false;throw failure;}
        }
    }
    private AtomicFile beforeRestore() {
        return new AtomicFile(new File(file.getBaseFile().getParentFile(),"before-restore-v1.json"));
    }
    public List<Note> previousSnapshot() throws Exception {
        return DocumentCodec.decode(atomicRead(beforeRestore()));
    }
    /** Returns false only when the full imported result is verified in private recovery. */
    public boolean applyRestore(List<Note> current,List<Note> merged) throws Exception {
        atomicWrite(beforeRestore(),DocumentCodec.encode(current));
        try {save(merged);return true;}
        catch(Exception failure) {
            if(exists() && Arrays.equals(DocumentCodec.encode(DocumentCodec.decode(atomicRead(file))),DocumentCodec.encode(merged))) {
                status="Restore retained privately · destination save pending";return false;
            }
            throw failure;
        }
    }
    /** Worker-thread compare-and-apply for multi-file restore coordinators; stale previews never write. */
    public boolean applyRestoreIfCurrent(List<Note> current,List<Note> merged)throws Exception{
        byte[] expected=DocumentCodec.encode(current);DocumentCodec.encode(merged);
        synchronized(SNAPSHOT_LOCK){
            if(!Objects.equals(selected,settings.getString("tree",null)))throw new IOException("Storage selection changed; review restore again");
            if(!Arrays.equals(expected,DocumentCodec.encode(localSnapshot())))throw new IOException("Library changed; review restore again");
            return applyRestore(current,merged);
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
        try {writeLocal(payload,true,head);try{materializeFolders(merged);}catch(Exception pendingFiles){status="Opened folder · note files sync pending";return;}writeLocal(payload,false,head);status="Opened existing local library";}
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
        atomicWrite(file,bytes);updateSurfaces(DocumentCodec.decode(payload));
    }
    private byte[] atomicRead(AtomicFile target) throws IOException {synchronized(SNAPSHOT_LOCK){return target.readFully();}}
    private void atomicWrite(AtomicFile target,byte[] bytes) throws Exception {
      synchronized(SNAPSHOT_LOCK){
        FileOutputStream stream=null;
        try {
            stream=target.startWrite();stream.write(bytes);target.finishWrite(stream);stream=null;
            if(!Arrays.equals(bytes,target.readFully()))throw new IOException("Save readback failed");
        } catch(Exception e) {if(stream!=null)target.failWrite(stream);throw e;}
      }
    }
}
