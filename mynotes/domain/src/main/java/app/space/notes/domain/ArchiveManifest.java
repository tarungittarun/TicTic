package app.space.notes.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Versioned metadata for a streaming archive. Digests detect corruption, not authenticity. */
public final class ArchiveManifest {
    public static final int VERSION=1, MAX_MANIFEST_BYTES=2*1024*1024, MAX_LIBRARY_BYTES=8*1024*1024;
    public static final int MAX_RECORDINGS=10000, MAX_METADATA_BYTES=1024*1024;
    public static final long MAX_AUDIO_BYTES=512L*1024*1024, MAX_TOTAL_BYTES=2L*1024*1024*1024;
    public static final String MANIFEST_PATH="manifest.bin", LIBRARY_PATH="library.json";
    private static final byte[] MAGIC="MYNOTES-ARCHIVE\n".getBytes(StandardCharsets.US_ASCII);
    public final int libraryBytes;
    public final String librarySha256;
    public final List<Recording> recordings;
    public final long payloadBytes;

    public static final class Recording {
        public final String id, audioSha256, metadataSha256;
        public final long createdAt, audioBytes;
        public final int metadataBytes;
        public final boolean noteWasCreated;
        public Recording(String id,long createdAt,long audioBytes,String audioSha256,int metadataBytes,String metadataSha256,boolean noteWasCreated){
            requireId(id);
            if(createdAt<0 || createdAt>253402300799999L)throw new IllegalArgumentException("Invalid recording date");
            if(audioBytes<44 || audioBytes>MAX_AUDIO_BYTES)throw new IllegalArgumentException("Recording exceeds archive bounds");
            requireDigest(audioSha256);
            if(metadataBytes<0 || metadataBytes>MAX_METADATA_BYTES)throw new IllegalArgumentException("Transcript exceeds archive bounds");
            if(metadataBytes==0){if(metadataSha256!=null)throw new IllegalArgumentException("Absent transcript must not have a digest");}
            else requireDigest(metadataSha256);
            this.id=id;this.createdAt=createdAt;this.audioBytes=audioBytes;this.audioSha256=audioSha256;
            this.metadataBytes=metadataBytes;this.metadataSha256=metadataSha256;this.noteWasCreated=noteWasCreated;
        }
    }
    public ArchiveManifest(int libraryBytes,String librarySha256,List<Recording> recordings){
        if(libraryBytes<1 || libraryBytes>MAX_LIBRARY_BYTES)throw new IllegalArgumentException("Library exceeds archive bounds");
        requireDigest(librarySha256);
        if(recordings==null || recordings.size()>MAX_RECORDINGS)throw new IllegalArgumentException("Too many recordings");
        List<Recording> copy=new ArrayList<>();Set<String> ids=new HashSet<>();long total=libraryBytes;
        for(Recording entry:recordings){
            if(entry==null || !ids.add(entry.id))throw new IllegalArgumentException("Missing or duplicate recording");
            total+=entry.audioBytes+entry.metadataBytes;
            if(total>MAX_TOTAL_BYTES-MAX_MANIFEST_BYTES)throw new IllegalArgumentException("Archive exceeds total bounds");
            copy.add(entry);
        }
        copy.sort(Comparator.comparing(entry->entry.id));this.recordings=Collections.unmodifiableList(copy);
        this.libraryBytes=libraryBytes;this.librarySha256=librarySha256;this.payloadBytes=total;
    }
    public static void requireId(String id){
        if(id==null || id.length()!=36)throw new IllegalArgumentException("Invalid recording ID");
        try{if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException("Noncanonical recording ID");}
        catch(IllegalArgumentException invalid){throw new IllegalArgumentException("Invalid recording ID");}
    }
    public static String audioPath(String id){requireId(id);return "audio/"+id+".wav";}
    public static String metadataPath(String id){requireId(id);return "audio/"+id+".json";}
    private static void requireDigest(String value){if(value==null || !value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid SHA-256 digest");}
    private static void digest(DataOutputStream out,String value)throws IOException{for(int i=0;i<64;i+=2)out.writeByte(Integer.parseInt(value.substring(i,i+2),16));}
    private static String digest(DataInputStream in)throws IOException{StringBuilder value=new StringBuilder(64);for(int i=0;i<32;i++){int b=in.readUnsignedByte();value.append(Character.forDigit(b>>>4,16)).append(Character.forDigit(b&15,16));}return value.toString();}
    public byte[] encode()throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.write(MAGIC);out.writeInt(VERSION);out.writeInt(libraryBytes);digest(out,librarySha256);out.writeInt(recordings.size());
        for(Recording entry:recordings){out.writeUTF(entry.id);out.writeLong(entry.createdAt);out.writeLong(entry.audioBytes);digest(out,entry.audioSha256);out.writeInt(entry.metadataBytes);if(entry.metadataBytes>0)digest(out,entry.metadataSha256);out.writeBoolean(entry.noteWasCreated);}
        out.flush();if(bytes.size()>MAX_MANIFEST_BYTES)throw new IOException("Manifest exceeds archive bounds");return bytes.toByteArray();
    }
    public static ArchiveManifest decode(byte[] bytes)throws IOException{
        if(bytes==null || bytes.length>MAX_MANIFEST_BYTES)throw new IOException("Manifest exceeds archive bounds");
        try{
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));byte[] magic=new byte[MAGIC.length];in.readFully(magic);
            if(!Arrays.equals(MAGIC,magic) || in.readInt()!=VERSION)throw new IOException("Unsupported archive manifest");
            int length=in.readInt();String hash=digest(in);int count=in.readInt();if(count<0 || count>MAX_RECORDINGS)throw new IOException("Too many recordings");
            List<Recording> recordings=new ArrayList<>();
            for(int i=0;i<count;i++){
                String id=in.readUTF();long created=in.readLong(),audio=in.readLong();String audioHash=digest(in);int metadata=in.readInt();String metadataHash=metadata>0?digest(in):null;int marker=in.readUnsignedByte();
                if(marker>1)throw new IOException("Invalid source-application marker");
                recordings.add(new Recording(id,created,audio,audioHash,metadata,metadataHash,marker==1));
            }
            if(in.read()!=-1)throw new IOException("Trailing manifest data");return new ArchiveManifest(length,hash,recordings);
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid archive manifest",invalid);}
    }
}
