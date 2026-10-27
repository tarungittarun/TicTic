package app.space.notes.runtime;

import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Provider-independent immutable commit protocol, exercised with an in-memory backend. */
public final class GenerationProtocol {
    public interface Backend {
        Map<String,byte[]> readAll() throws Exception;
        void create(String name,byte[] bytes) throws Exception;
    }
    public static final class Snapshot {
        public final String head;
        public final byte[] payload;
        Snapshot(String head,byte[] payload) { this.head=head;this.payload=payload; }
    }
    private final Backend backend;
    public GenerationProtocol(Backend backend) { this.backend=backend; }
    public static String digest(byte[] bytes) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder out=new StringBuilder();
        for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    public Snapshot read() throws Exception {
        Map<String,byte[]> files=backend.readAll();
        if(files.isEmpty())return new Snapshot(null,null);
        Map<String,JSONObject> valid=new HashMap<>();
        for(Map.Entry<String,byte[]> file:files.entrySet()) {
            JSONObject record;
            try { record=new JSONObject(new String(file.getValue(),StandardCharsets.UTF_8)); }
            catch(org.json.JSONException incomplete) { continue; }
            if(!record.has("format") || !record.has("payload") || !record.has("sha256"))continue;
            if(record.getInt("format")!=1)throw new IOException("Unknown folder format");
            byte[] payload=record.getString("payload").getBytes(StandardCharsets.UTF_8);
            if(!digest(payload).equals(record.getString("sha256")))continue;
            String id=record.getString("id");
            if(!file.getKey().equals("mynotes-gen-"+id+".json"))throw new IOException("Commit identity mismatch");
            DocumentCodec.decode(payload);
            valid.put(id,record);
        }
        if(valid.isEmpty())throw new IOException("No valid folder commit; existing data left untouched");
        Set<String> heads=new HashSet<>(valid.keySet());
        for(JSONObject record:valid.values()) {
            String parent=record.optString("parent","");
            if(!parent.isEmpty() && !valid.containsKey(parent))throw new IOException("Missing folder generation");
            if(!parent.isEmpty())heads.remove(parent);
            Set<String> seen=new HashSet<>(); JSONObject cursor=record;
            while(cursor!=null) {
                if(!seen.add(cursor.getString("id")))throw new IOException("Folder generation cycle");
                cursor=valid.get(cursor.optString("parent",""));
            }
        }
        if(heads.size()!=1)throw new IOException("Folder has conflicting writers; choose a new empty folder to preserve recovery");
        String head=heads.iterator().next();
        return new Snapshot(head,valid.get(head).getString("payload").getBytes(StandardCharsets.UTF_8));
    }
    public Snapshot commit(byte[] payload,String expected) throws Exception {
        DocumentCodec.decode(payload); Snapshot current=read();
        // A closed write may have succeeded before acknowledgement was interrupted.
        if(current.payload!=null && Arrays.equals(current.payload,payload))return current;
        if(!Objects.equals(current.head,expected))throw new IOException("Folder changed externally; refusing overwrite");
        String id=UUID.randomUUID().toString();
        byte[] encoded=new JSONObject().put("format",1).put("id",id).put("parent",expected==null?"":expected)
            .put("payload",new String(payload,StandardCharsets.UTF_8)).put("sha256",digest(payload))
            .toString().getBytes(StandardCharsets.UTF_8);
        backend.create("mynotes-gen-"+id+".json",encoded);
        Snapshot actual=read();
        if(!id.equals(actual.head) || !Arrays.equals(payload,actual.payload))throw new IOException("Folder save readback failed");
        return actual;
    }
}
