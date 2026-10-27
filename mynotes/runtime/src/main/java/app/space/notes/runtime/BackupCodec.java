package app.space.notes.runtime;

import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import app.space.notes.domain.Note;

/** Portable text-library backup; explicitly bounded until a streaming media format exists. */
public final class BackupCodec {
    public static final int MAX_BYTES=8*1024*1024;
    private BackupCodec() { }
    public static byte[] encode(List<Note> notes) throws Exception {
        String payload=new String(DocumentCodec.encode(notes),StandardCharsets.UTF_8);
        byte[] bytes=new JSONObject().put("format","MyNotesBackup").put("version",1)
            .put("noteCount",notes.size()).put("payload",payload)
            .put("sha256",GenerationProtocol.digest(payload.getBytes(StandardCharsets.UTF_8)))
            .toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Text backup exceeds this milestone's 8 MiB safety limit; no file was written");
        return bytes;
    }
    public static List<Note> decode(byte[] bytes) throws Exception {
        if(bytes.length>MAX_BYTES)throw new IOException("Backup exceeds 8 MiB safety limit");
        JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!"MyNotesBackup".equals(root.getString("format")) || root.getInt("version")!=1)
            throw new IOException("Unsupported backup format; current library unchanged");
        byte[] payload=root.getString("payload").getBytes(StandardCharsets.UTF_8);
        if(!GenerationProtocol.digest(payload).equals(root.getString("sha256")))
            throw new IOException("Backup checksum mismatch; current library unchanged");
        List<Note> notes=DocumentCodec.decode(payload);
        if(root.getInt("noteCount")!=notes.size())throw new IOException("Backup note count mismatch");
        return notes;
    }
    public static byte[] read(InputStream in) throws IOException {
        if(in==null)throw new IOException("Cannot open backup");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1) {
            if(out.size()+n>MAX_BYTES)throw new IOException("Backup exceeds 8 MiB safety limit");
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }
}
