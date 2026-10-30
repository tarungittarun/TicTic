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
    public interface BlobBackend extends Backend {
        byte[] readBlob(String hash)throws Exception;
        void createBlob(String hash,byte[] bytes)throws Exception;
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
    /** Both legacy and canonical visual payloads share immutable content-addressed blobs. */
    private static List<JSONObject> images(JSONObject root)throws Exception{
        List<JSONObject> result=new ArrayList<>();org.json.JSONArray notes=root.getJSONArray("notes");
        for(int i=0;i<notes.length();i++){JSONObject note=notes.getJSONObject(i);int before=result.size();org.json.JSONArray legacy=note.optJSONArray("images");if(legacy!=null)for(int j=0;j<legacy.length();j++)result.add(legacy.getJSONObject(j));
            if(note.has("content")){org.json.JSONArray blocks=note.getJSONObject("content").getJSONArray("blocks");if(blocks.length()>app.space.notes.domain.DocumentContent.MAX_BLOCKS)throw new IOException("Too many document sections");for(int j=0;j<blocks.length();j++){JSONObject block=blocks.getJSONObject(j);String kind=block.getString("kind");if(kind.equals("IMAGE") || kind.equals("DRAWING"))result.add(block.getJSONObject("image"));}}
            if(result.size()-before>8)throw new IOException("Too many visual references in note");
        }return result;
    }
    private byte[] externalize(byte[] payload)throws Exception{
        JSONObject root=new JSONObject(app.space.notes.domain.JsonLimits.text(payload));for(JSONObject image:images(root)){byte[] bytes=Base64.getDecoder().decode(image.getString("data"));String hash=digest(bytes);((BlobBackend)backend).createBlob(hash,bytes);image.remove("data");image.put("blob",hash);}return root.toString().getBytes(StandardCharsets.UTF_8);
    }
    private byte[] hydrate(byte[] payload,Map<String,String> cache)throws Exception{
        if(!(backend instanceof BlobBackend))throw new IOException("This backend cannot read media snapshots");JSONObject root=new JSONObject(app.space.notes.domain.JsonLimits.text(payload));
        for(JSONObject image:images(root)){
            // Earlier schema7 development snapshots embedded canonical images in format2 records.
            if(image.has("data")){if(image.has("blob"))throw new IOException("Ambiguous embedded/media reference");continue;}
            String hash=image.getString("blob");if(!hash.matches("[0-9a-f]{64}"))throw new IOException("Invalid media reference");String data=cache.get(hash);
            if(data==null){byte[] bytes=((BlobBackend)backend).readBlob(hash);if(bytes==null || bytes.length>1024*1024 || !digest(bytes).equals(hash))throw new IOException("Media checksum/size mismatch; recovery retained");data=Base64.getEncoder().encodeToString(bytes);cache.put(hash,data);int size=0;for(String value:cache.values())size+=value.length();Iterator<Map.Entry<String,String>> entries=cache.entrySet().iterator();while(size>8*1024*1024 && entries.hasNext()){Map.Entry<String,String> oldest=entries.next();size-=oldest.getValue().length();entries.remove();}}
            image.remove("blob");image.put("data",data);
        }return root.toString().getBytes(StandardCharsets.UTF_8);
    }
    public Snapshot read() throws Exception {try{return readVerified();}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to verify this folder safely; private recovery is retained",pressure);}}
    private Snapshot readVerified() throws Exception {
        Map<String,byte[]> files=backend.readAll();
        if(files.isEmpty())return new Snapshot(null,null);
        Map<String,JSONObject> valid=new HashMap<>();Map<String,String> media=new LinkedHashMap<>(16,.75f,true);
        for(Map.Entry<String,byte[]> file:files.entrySet()) {
            JSONObject record;
            try { record=new JSONObject(app.space.notes.domain.JsonLimits.text(file.getValue())); }
            catch(org.json.JSONException | IOException incomplete) { continue; }
            if(!record.has("format") || !record.has("payload") || !record.has("sha256"))continue;
            if(record.getInt("format")!=1 && record.getInt("format")!=2)throw new IOException("Unknown folder format");
            byte[] payload=record.getString("payload").getBytes(StandardCharsets.UTF_8);
            if(!digest(payload).equals(record.getString("sha256")))continue;
            String id=record.getString("id");
            if(!file.getKey().equals("mynotes-gen-"+id+".json"))throw new IOException("Commit identity mismatch");
            DocumentCodec.decode(record.getInt("format")==2?hydrate(payload,media):payload);
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
        JSONObject last=valid.get(head);byte[] payload=last.getString("payload").getBytes(StandardCharsets.UTF_8);return new Snapshot(head,last.getInt("format")==2?hydrate(payload,media):payload);
    }
    public Snapshot commit(byte[] payload,String expected) throws Exception {try{return commitVerified(payload,expected);}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to save this folder safely; private recovery is retained",pressure);}}
    private Snapshot commitVerified(byte[] payload,String expected) throws Exception {
        DocumentCodec.decode(payload); Snapshot current=read();
        // A closed write may have succeeded before acknowledgement was interrupted.
        if(current.payload!=null && Arrays.equals(current.payload,payload))return current;
        if(!Objects.equals(current.head,expected))throw new IOException("Folder changed externally; refusing overwrite");
        java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("yyyyMMdd-HHmmss-SSS'Z'",Locale.ROOT);format.setTimeZone(TimeZone.getTimeZone("UTC"));long clock=System.currentTimeMillis();String id=format.format(new Date(clock));Map<String,byte[]> existing=backend.readAll();int collision=0;while(existing.containsKey("mynotes-gen-"+id+".json")){if(++collision>10000)throw new IOException("Too many timestamp collisions; recovery retained");id=format.format(new Date(++clock));}
        byte[] stored=backend instanceof BlobBackend?externalize(payload):payload;
        byte[] encoded=new JSONObject().put("format",backend instanceof BlobBackend?2:1).put("id",id).put("parent",expected==null?"":expected)
            .put("payload",new String(stored,StandardCharsets.UTF_8)).put("sha256",digest(stored))
            .toString().getBytes(StandardCharsets.UTF_8);
        backend.create("mynotes-gen-"+id+".json",encoded);
        Snapshot actual=read();
        if(!id.equals(actual.head) || !Arrays.equals(payload,actual.payload))throw new IOException("Folder save readback failed");
        return actual;
    }
}
