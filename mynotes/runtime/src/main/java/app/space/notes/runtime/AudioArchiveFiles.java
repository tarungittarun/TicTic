package app.space.notes.runtime;

import android.content.Context;
import android.net.Uri;
import app.space.notes.domain.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Archive staging/verification only. Importing a stage into live notes/audio requires a separate transaction. */
public final class AudioArchiveFiles {
    private AudioArchiveFiles() { }
    private static final long HEADROOM=64L*1024*1024;
    public interface Progress { void update(String phase,long completed,long total); }
    public static final class Stage implements Closeable {
        public final ArchiveManifest manifest;
        public final List<Note> notes;
        private final File directory;
        private boolean closed;
        private Stage(File directory,ArchiveManifest manifest,List<Note> notes){this.directory=directory;this.manifest=manifest;this.notes=Collections.unmodifiableList(new ArrayList<>(notes));}
        public String token(){return directory.getName();}
        public File audio(String id)throws IOException{return staged(directory,ArchiveManifest.audioPath(id));}
        public File metadata(String id)throws IOException{return staged(directory,ArchiveManifest.metadataPath(id));}
        @Override public void close()throws IOException{if(!closed){remove(directory);closed=true;}}
    }
    private static void check(BooleanSupplier cancelled)throws InterruptedIOException{if(Thread.currentThread().isInterrupted() || (cancelled!=null && cancelled.getAsBoolean()))throw new InterruptedIOException("Archive cancelled; originals retained");}
    private static File directory(Context context)throws IOException{
        File parent=new File(context.getFilesDir(),"archive-staging-v1");if(!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())throw new IOException("Cannot prepare private archive staging");File directory=new File(parent,UUID.randomUUID().toString());if(!directory.mkdir())throw new IOException("Cannot create private archive stage");return directory;
    }
    private static void space(File directory,long required)throws IOException{if(required<0 || directory.getUsableSpace()<required+HEADROOM)throw new IOException("Not enough free space for verified archive staging; originals retained");}
    private static File staged(File directory,String path)throws IOException{
        if(path.equals(ArchiveManifest.LIBRARY_PATH) || path.equals(ArchiveManifest.MANIFEST_PATH))return new File(directory,path);
        if(!path.startsWith("audio/") || !(path.endsWith(".wav") || path.endsWith(".json")))throw new IOException("Invalid staged archive path");int suffix=path.lastIndexOf('.');String id=path.substring(6,suffix);try{ArchiveManifest.requireId(id);}catch(IllegalArgumentException invalid){throw new IOException("Invalid staged recording ID");}File audio=new File(directory,"audio");if(!audio.isDirectory() && !audio.mkdir() && !audio.isDirectory())throw new IOException("Cannot stage recording files");return new File(audio,id+path.substring(suffix));
    }
    private static void remove(File file)throws IOException{
        if(file.isDirectory()){File[] children=file.listFiles();if(children==null)throw new IOException("Cannot clean private archive stage");for(File child:children){if(!child.getCanonicalFile().getParentFile().equals(file.getCanonicalFile()))throw new IOException("Refusing unsafe archive-stage cleanup");remove(child);}}
        if(file.exists() && !file.delete())throw new IOException("Private archive cleanup needs retry");
    }
    private static byte[] small(File file,int limit)throws IOException{
        if(file.length()<1 || file.length()>limit)throw new IOException("Archive entry exceeds semantic bounds");try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>limit)throw new IOException("Archive entry exceeds semantic bounds");out.write(buffer,0,count);}return out.toByteArray();}
    }
    private static void bytes(File file,byte[] bytes)throws IOException{try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);out.getFD().sync();}}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static String hex(byte[] bytes){StringBuilder value=new StringBuilder(bytes.length*2);for(byte b:bytes)value.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));return value.toString();}
    private static String hash(File file,long limit,BooleanSupplier cancelled)throws IOException{MessageDigest hash=digest();try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[64*1024];long total=0;int count;while((count=in.read(buffer))!=-1){check(cancelled);total+=count;if(total>limit)throw new IOException("Recording exceeds archive bounds");hash.update(buffer,0,count);}}return hex(hash.digest());}
    private static void wave(File file)throws IOException{try(RandomAccessFile in=new RandomAccessFile(file,"r")){PcmWave.Data data=PcmWave.inspect(in);if(data.offset+data.bytes!=in.length())throw new IOException("Recording has unframed trailing bytes");}}
    private static String transfer(InputStream in,File destination,long limit,File stage,BooleanSupplier cancelled,Progress progress,String phase)throws IOException{
        if(in==null)throw new IOException("Archive stream unavailable");MessageDigest hash=digest();try(InputStream source=in;FileOutputStream out=new FileOutputStream(destination)){byte[] buffer=new byte[64*1024];long total=0,nextSpace=0;int count,stalled=0;while((count=source.read(buffer))!=-1){check(cancelled);if(count==0){if(++stalled>32)throw new IOException("Archive stream made no progress");continue;}stalled=0;total+=count;if(total>limit)throw new IOException("Archive container exceeds bounds");if(total>=nextSpace){space(stage,count);nextSpace=total+1024*1024;}out.write(buffer,0,count);hash.update(buffer,0,count);if(progress!=null)progress.update(phase,total,limit);}out.getFD().sync();}return hex(hash.digest());
    }
    /** Streaming snapshot; completed WAVs are immutable sources, transcript bytes are bounded atomic copies. */
    private static ArchiveManifest prepare(Context context,List<Note> notes,File directory,Map<String,File> sources,BooleanSupplier cancelled,Progress progress)throws Exception{
        check(cancelled);byte[] library=BackupCodec.encode(notes);ArchiveJson.library(library);space(directory,library.length);File noteFile=staged(directory,ArchiveManifest.LIBRARY_PATH);bytes(noteFile,library);sources.put(ArchiveManifest.LIBRARY_PATH,noteFile);
        VoiceFiles voices=new VoiceFiles(context);List<String> ids=voices.list();if(ids.size()>ArchiveManifest.MAX_RECORDINGS)throw new IOException("Too many recordings for one archive");List<ArchiveManifest.Recording> recordings=new ArrayList<>();long total=library.length;
        for(int i=0;i<ids.size();i++){
            check(cancelled);String id=ids.get(i);ArchiveManifest.requireId(id);File audio=voices.audio(id);long length=audio.length();if(length<44 || length>ArchiveManifest.MAX_AUDIO_BYTES)throw new IOException("Recording exceeds archive bounds; export original separately");wave(audio);String audioHash=hash(audio,ArchiveManifest.MAX_AUDIO_BYTES,cancelled);byte[] metadata=voices.archiveMetadata(id);String metadataHash=null;int metadataBytes=0;
            if(metadata!=null){ArchiveJson.metadata(metadata,id);metadataBytes=metadata.length;space(directory,metadataBytes);File target=staged(directory,ArchiveManifest.metadataPath(id));bytes(target,metadata);sources.put(ArchiveManifest.metadataPath(id),target);metadataHash=hex(digest().digest(metadata));}
            total+=length+metadataBytes;if(total>ArchiveManifest.MAX_TOTAL_BYTES-ArchiveManifest.MAX_MANIFEST_BYTES)throw new IOException("Recordings exceed one archive's total bounds");sources.put(ArchiveManifest.audioPath(id),audio);recordings.add(new ArchiveManifest.Recording(id,audio.lastModified(),length,audioHash,metadataBytes,metadataHash,voices.noteWasCreated(id)));if(progress!=null)progress.update("Preparing recordings",i+1,ids.size());
        }
        return new ArchiveManifest(library.length,hex(digest().digest(library)),recordings);
    }
    /** Does not report success until local-provider output reopens and verifies byte-for-byte and internally. */
    public static ArchiveManifest write(Context context,Uri destination,List<Note> notes,BooleanSupplier cancelled,Progress progress)throws Exception{
        LocalBackupFiles.requireLocal(destination);File directory=directory(context);try{
            Map<String,File> sources=new HashMap<>();ArchiveManifest manifest=prepare(context,notes,directory,sources,cancelled,progress);space(directory,manifest.payloadBytes+manifest.encode().length);File archive=new File(directory,"archive.zip");
            try(OutputStream out=new FileOutputStream(archive)){ArchiveZip.write(out,manifest,path->{File source=sources.get(path);if(source==null)throw new IOException("Archive source disappeared");return new FileInputStream(source);},cancelled,done->{if(progress!=null)progress.update("Packing backup",done,manifest.payloadBytes+ArchiveManifest.MAX_MANIFEST_BYTES);});}
            String written;MessageDigest hash=digest();check(cancelled);try(OutputStream out=context.getContentResolver().openOutputStream(destination,"rwt");InputStream in=new FileInputStream(archive)){if(out==null)throw new IOException("Cannot write local archive");byte[] buffer=new byte[64*1024];long done=0;int count;while((count=in.read(buffer))!=-1){check(cancelled);out.write(buffer,0,count);hash.update(buffer,0,count);done+=count;if(progress!=null)progress.update("Writing backup",done,archive.length());}out.flush();written=hex(hash.digest());}
            check(cancelled);long length=archive.length();String reopened=transfer(context.getContentResolver().openInputStream(destination),archive,Math.min(ArchiveZip.MAX_CONTAINER_BYTES,length),directory,cancelled,progress,"Reopening backup");if(!written.equals(reopened) || archive.length()!=length)throw new IOException("Archive readback differs; do not rely on this destination");ArchiveManifest verified=ArchiveZip.read(archive,null,cancelled,done->{if(progress!=null)progress.update("Verifying backup",done,manifest.payloadBytes+ArchiveManifest.MAX_MANIFEST_BYTES);});if(!Arrays.equals(manifest.encode(),verified.encode()))throw new IOException("Archive readback manifest changed");return verified;
        }finally{remove(directory);}
    }
    /** Copies/validates into private staging. No current note, recording, source marker or transcript is mutated. */
    public static Stage inspect(Context context,Uri source,BooleanSupplier cancelled,Progress progress)throws Exception{
        LocalBackupFiles.requireLocal(source);File directory=directory(context);boolean accepted=false;try{
            space(directory,0);File archive=new File(directory,"archive.zip");transfer(context.getContentResolver().openInputStream(source),archive,ArchiveZip.MAX_CONTAINER_BYTES,directory,cancelled,progress,"Reading archive");
            ArchiveManifest manifest=ArchiveZip.read(archive,(path,length)->{space(directory,length);return new FileOutputStream(staged(directory,path));},cancelled,done->{if(progress!=null)progress.update("Checking archive",done,ArchiveManifest.MAX_TOTAL_BYTES);});
            List<Note> notes=ArchiveJson.library(small(staged(directory,ArchiveManifest.LIBRARY_PATH),ArchiveManifest.MAX_LIBRARY_BYTES));
            for(ArchiveManifest.Recording recording:manifest.recordings){check(cancelled);wave(staged(directory,ArchiveManifest.audioPath(recording.id)));if(recording.metadataBytes>0)ArchiveJson.metadata(small(staged(directory,ArchiveManifest.metadataPath(recording.id)),ArchiveManifest.MAX_METADATA_BYTES),recording.id);}
            bytes(staged(directory,ArchiveManifest.MANIFEST_PATH),manifest.encode());Stage stage=new Stage(directory,manifest,notes);accepted=true;return stage;
        }finally{if(!accepted)remove(directory);}
    }
}
