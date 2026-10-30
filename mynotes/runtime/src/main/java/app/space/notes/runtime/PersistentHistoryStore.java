package app.space.notes.runtime;

import android.content.Context;
import android.util.AtomicFile;
import app.space.notes.domain.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Private, optional recovery sidecar. Never an authority for current notes. Serial disk-worker only. */
public final class PersistentHistoryStore {
    public static final int MAX_BYTES=16*1024*1024,MAX_NOTES=64;
    private static final Object LOCK=new Object();
    private final AtomicFile file;private boolean blocked;private String notice;
    public PersistentHistoryStore(Context context){file=new AtomicFile(new File(context.getFilesDir(),"notes-library/edit-history-v1.json"));}
    public String notice(){return notice;}
    public void reset(){synchronized(LOCK){file.delete();blocked=false;notice=null;}}
    public static final class Entry {
        public final Note current;public final DocumentHistory.State state;
        public Entry(Note current,DocumentHistory.State state){this.current=current;this.state=state;}
    }
    private static String digest(byte[] bytes)throws Exception{return GenerationProtocol.digest(bytes);}
    private static byte[] encode(Note n)throws Exception{return DocumentCodec.encode(Collections.singletonList(n));}
    public Map<String,DocumentHistory> load(List<Note> current){synchronized(LOCK){
        Map<String,DocumentHistory> result=new LinkedHashMap<>();
        try{
            if(!file.getBaseFile().exists() && !new File(file.getBaseFile()+".bak").exists())return result;
            byte[] bytes;try(InputStream in=file.openRead()){bytes=read(in);}
            JSONObject envelope=new JSONObject(JsonLimits.text(bytes));if(envelope.length()!=3 || envelope.getInt("version")!=1)throw new IOException("Unsupported history format");
            String payload=envelope.getString("payload");byte[] data=payload.getBytes(StandardCharsets.UTF_8);if(!digest(data).equals(envelope.getString("sha256")))throw new IOException("History checksum mismatch");
            JSONObject root=new JSONObject(JsonLimits.text(data));JSONArray records=root.getJSONArray("histories");JSONObject objects=root.getJSONObject("objects");if(records.length()>MAX_NOTES || objects.length()>MAX_NOTES*(DocumentHistory.MAX_STEPS*2+1))throw new IOException("History count limit");
            Map<String,Note> decoded=new HashMap<>();Set<String> ids=new HashSet<>();Map<String,Note> live=new HashMap<>();for(Note n:current)live.put(n.id,n);
            for(int i=records.length()-1;i>=0;i--){
                JSONObject record=records.getJSONObject(i);String id=record.getString("id");if(!ids.add(id))throw new IOException("Duplicate history identity");Note note=live.get(id);
                if(note==null || !digest(encode(note)).equals(record.getString("anchor"))){notice="Some older edit history no longer matches the saved notes and was not restored. Current notes were kept.";continue;}
                Map<Note,DocumentHistory.Cursor> cursors=new IdentityHashMap<>();List<Note> undo=states(record.getJSONArray("undo"),objects,decoded,cursors),redo=states(record.getJSONArray("redo"),objects,decoded,cursors);
                result.put(id,DocumentHistory.restore(id,new DocumentHistory.State(undo,redo,cursors)));
            }
        }catch(Exception | OutOfMemoryError invalid){blocked=true;notice="Saved edit history could not be verified. Notes are unchanged. Use Reset edit history in Storage & recovery to start a new history; the unreadable file is retained until then.";result.clear();}
        return result;
    }}
    private static List<Note> states(JSONArray array,JSONObject objects,Map<String,Note> decoded,Map<Note,DocumentHistory.Cursor> cursors)throws Exception{
        if(array.length()>DocumentHistory.MAX_STEPS)throw new IOException("History step limit");List<Note> result=new ArrayList<>();
        for(int i=0;i<array.length();i++){JSONObject item=array.getJSONObject(i);String key=item.getString("note");Note n=decoded.get(key);if(n==null){String json=objects.getString(key);byte[] bytes=json.getBytes(StandardCharsets.UTF_8);if(!digest(bytes).equals(key))throw new IOException("History object checksum mismatch");List<Note> notes=DocumentCodec.decode(bytes);if(notes.size()!=1)throw new IOException("Invalid history note");n=notes.get(0);decoded.put(key,n);}result.add(n);if(item.has("field")){String field=item.getString("field");int a=item.getInt("anchor"),f=item.getInt("focus");if(field.length()>256 || a<0 || f<0 || a>2*1024*1024 || f>2*1024*1024)throw new IOException("Invalid history cursor");cursors.put(n,new DocumentHistory.Cursor(field,a,f));}}
        return result;
    }
    public void save(List<Entry> entries)throws Exception{synchronized(LOCK){
        if(blocked)throw new IOException("Edit history is paused until Reset edit history; notes remain saved");
        JSONObject objects=new JSONObject();JSONArray records=new JSONArray();int[] budget={0};boolean trimmed=false;
        // Most recently accessed histories arrive last. Bounded deduplicated snapshots; never trim current notes.
        for(int i=entries.size()-1;i>=0 && records.length()<MAX_NOTES;i--){Entry e=entries.get(i);if(!e.state.undo.isEmpty() || !e.state.redo.isEmpty()){
            JSONArray undo=pack(e.state.undo,e.state.cursors,objects,budget),redo=pack(e.state.redo,e.state.cursors,objects,budget);
            if(undo.length()!=e.state.undo.size() || redo.length()!=e.state.redo.size())trimmed=true;
            if(undo.length()+redo.length()>0)records.put(new JSONObject().put("id",e.current.id).put("anchor",digest(encode(e.current))).put("undo",undo).put("redo",redo));
        }}
        String payload=new JSONObject().put("objects",objects).put("histories",records).toString();byte[] data=payload.getBytes(StandardCharsets.UTF_8);
        byte[] bytes=new JSONObject().put("version",1).put("payload",payload).put("sha256",digest(data)).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Edit history reached its disk limit; current notes are saved");JsonLimits.require(bytes);JsonLimits.require(data);
        FileOutputStream out=file.startWrite();try{out.write(bytes);file.finishWrite(out);}catch(Exception failure){file.failWrite(out);throw failure;}
        if(trimmed)notice="Older edit history was trimmed to its private storage limit. Current notes were kept.";
    }}
    private static JSONArray pack(List<Note> notes,Map<Note,DocumentHistory.Cursor> cursors,JSONObject objects,int[] budget)throws Exception{
        List<JSONObject> retained=new ArrayList<>();for(int i=notes.size()-1;i>=0;i--){Note note=notes.get(i);byte[] bytes=encode(note);String key=digest(bytes);if(!objects.has(key)){// Reserve space for JSON escaping, metadata and checksum envelope.
            if(bytes.length>MAX_BYTES/4 || budget[0]+bytes.length>MAX_BYTES/4)break;objects.put(key,new String(bytes,StandardCharsets.UTF_8));budget[0]+=bytes.length;}
            JSONObject item=new JSONObject().put("note",key);DocumentHistory.Cursor c=cursors.get(note);if(c!=null)item.put("field",c.field).put("anchor",c.anchor).put("focus",c.focus);retained.add(item);
        }Collections.reverse(retained);return new JSONArray(retained);
    }
    private static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("History size limit");out.write(buffer,0,n);}return out.toByteArray();}
}
