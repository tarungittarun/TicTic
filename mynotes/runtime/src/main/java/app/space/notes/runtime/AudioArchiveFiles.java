package app.space.notes.runtime;

import android.content.Context;
import android.net.Uri;
import app.space.notes.domain.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Bounded archive snapshot, staging, and verified file installation primitives. */
public final class AudioArchiveFiles {
    private AudioArchiveFiles() { }
    private static final long HEADROOM=64L*1024*1024;
    public interface Progress { void update(String phase,long completed,long total); }
    public static final class Summary {
        public final int noteCount,recordingCount;
        public final long libraryBytes,audioBytes,metadataBytes,payloadBytes;
        private Summary(int noteCount,int recordingCount,long libraryBytes,long audioBytes,long metadataBytes){this.noteCount=noteCount;this.recordingCount=recordingCount;this.libraryBytes=libraryBytes;this.audioBytes=audioBytes;this.metadataBytes=metadataBytes;this.payloadBytes=libraryBytes+audioBytes+metadataBytes;}
    }
    public static final class Stage implements Closeable {
        public final ArchiveManifest manifest;
        public final List<Note> notes;
        private final File directory;
        private boolean closed;
        private Stage(File directory,ArchiveManifest manifest,List<Note> notes){this.directory=directory;this.manifest=manifest;this.notes=Collections.unmodifiableList(new ArrayList<>(notes));}
        public String token(){return directory.getName();}
        boolean isClosed(){return closed;}
        boolean belongsTo(Context context)throws IOException{return directory.getCanonicalFile().getParentFile().equals(new File(context.getFilesDir(),"archive-staging-v1").getCanonicalFile());}
        public File audio(String id)throws IOException{return staged(directory,ArchiveManifest.audioPath(id));}
        public File metadata(String id)throws IOException{return staged(directory,ArchiveManifest.metadataPath(id));}
        @Override public void close()throws IOException{if(!closed){remove(directory);closed=true;}}
    }
    private static void check(BooleanSupplier cancelled)throws InterruptedIOException{if(Thread.currentThread().isInterrupted() || (cancelled!=null && cancelled.getAsBoolean()))throw new InterruptedIOException("Archive cancelled; originals retained");}
    private static File directory(Context context)throws IOException{
        File parent=new File(context.getFilesDir(),"archive-staging-v1");if(!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())throw new IOException("Cannot prepare private archive staging");File directory=new File(parent,UUID.randomUUID().toString());if(!directory.mkdir())throw new IOException("Cannot create private archive stage");return directory;
    }
    static void removeStageToken(Context context,String token)throws IOException{
        ArchiveManifest.requireId(token);File parent=new File(context.getFilesDir(),"archive-staging-v1");File target=new File(parent,token);
        if(!target.getCanonicalFile().getParentFile().equals(parent.getCanonicalFile()))throw new IOException("Refusing unsafe archive-stage cleanup");remove(target);
    }
    /** Called before the live voice catalogue is scanned after process death. */
    static void cleanupStaleStages(Context context)throws IOException{
        File parent=new File(context.getFilesDir(),"archive-staging-v1");if(!parent.exists())return;if(!parent.isDirectory())throw new IOException("Private archive staging is not a directory");File[] children=parent.listFiles();if(children==null)throw new IOException("Cannot inspect private archive staging");
        for(File child:children){if(!child.isDirectory() || !child.getCanonicalFile().getParentFile().equals(parent.getCanonicalFile()))throw new IOException("Unexpected private archive-stage entry");try{ArchiveManifest.requireId(child.getName());}catch(IllegalArgumentException invalid){throw new IOException("Unexpected private archive-stage identity");}remove(child);}
        if(!parent.delete() && parent.exists() && parent.list()!=null && parent.list().length==0)throw new IOException("Private archive staging cleanup needs retry");
    }
    /** Remove only private copy-first temporaries left by a process death during restore. */
    static void cleanupRestoreTemporaries(Context context)throws IOException{
        File parent=new File(context.getFilesDir(),"voice-inbox-v1");if(!parent.exists())return;if(!parent.isDirectory())throw new IOException("Private voice inbox is not a directory");File[] children=parent.listFiles();if(children==null)throw new IOException("Cannot inspect private voice inbox");
        for(File child:children){String name=child.getName();if(!name.startsWith(".archive-restore-") || !name.endsWith(".part"))continue;if(!child.isFile() || !child.getCanonicalFile().getParentFile().equals(parent.getCanonicalFile()))throw new IOException("Unexpected archive restore temporary");if(!child.delete() && child.exists())throw new IOException("Private archive restore temporary cleanup needs retry");}
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
    static byte[] remappedMetadata(Stage stage,ArchiveManifest.Recording recording,String targetId)throws Exception{
        if(recording.metadataBytes<=0)return null;ArchiveManifest.requireId(targetId);byte[] bytes=small(stage.metadata(recording.id),ArchiveManifest.MAX_METADATA_BYTES);org.json.JSONObject source=ArchiveJson.metadata(bytes,recording.id);org.json.JSONObject remapped=new org.json.JSONObject(source.toString()).put("id",targetId);byte[] result=remapped.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);ArchiveJson.metadata(result,targetId);return result;
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
            check(cancelled);String id=ids.get(i);ArchiveManifest.requireId(id);if(VoiceTasks.running(context,id))throw new IOException("Wait for recording transcription to finish before creating a full backup");File audio=voices.audio(id);long length=audio.length();if(length<44 || length>ArchiveManifest.MAX_AUDIO_BYTES)throw new IOException("Recording exceeds archive bounds; export original separately");wave(audio);String audioHash=hash(audio,ArchiveManifest.MAX_AUDIO_BYTES,cancelled);byte[] metadata=voices.archiveMetadata(id);String metadataHash=null;int metadataBytes=0;
            if(metadata!=null){ArchiveJson.metadata(metadata,id);metadataBytes=metadata.length;space(directory,metadataBytes);File target=staged(directory,ArchiveManifest.metadataPath(id));bytes(target,metadata);sources.put(ArchiveManifest.metadataPath(id),target);metadataHash=hex(digest().digest(metadata));}
            total+=length+metadataBytes;if(total>ArchiveManifest.MAX_TOTAL_BYTES-ArchiveManifest.MAX_MANIFEST_BYTES)throw new IOException("Recordings exceed one archive's total bounds");sources.put(ArchiveManifest.audioPath(id),audio);recordings.add(new ArchiveManifest.Recording(id,audio.lastModified(),length,audioHash,metadataBytes,metadataHash,voices.noteWasCreated(id)));if(progress!=null)progress.update("Preparing recordings",i+1,ids.size());
        }
        return new ArchiveManifest(library.length,hex(digest().digest(library)),recordings);
    }
    /** Validated pre-picker estimate; the later streaming write independently rechecks every source. */
    public static Summary estimate(Context context,List<Note> notes,BooleanSupplier cancelled,Progress progress)throws Exception{
        check(cancelled);byte[] library=BackupCodec.encode(notes);ArchiveJson.library(library);VoiceFiles voices=new VoiceFiles(context);List<String> ids=voices.list();if(ids.size()>ArchiveManifest.MAX_RECORDINGS)throw new IOException("Too many recordings for one archive");long audioBytes=0,metadataBytes=0;
        for(int i=0;i<ids.size();i++){check(cancelled);String id=ids.get(i);ArchiveManifest.requireId(id);if(VoiceTasks.running(context,id))throw new IOException("Wait for recording transcription to finish before creating a full backup");File audio=voices.audio(id);long length=audio.length();if(length<44 || length>ArchiveManifest.MAX_AUDIO_BYTES)throw new IOException("Recording exceeds archive bounds; export original separately");wave(audio);audioBytes+=length;if(audioBytes>ArchiveManifest.MAX_TOTAL_BYTES)throw new IOException("Recordings exceed one archive's total bounds");byte[] metadata=voices.archiveMetadata(id);if(metadata!=null){ArchiveJson.metadata(metadata,id);metadataBytes+=metadata.length;}if(library.length+audioBytes+metadataBytes>ArchiveManifest.MAX_TOTAL_BYTES-ArchiveManifest.MAX_MANIFEST_BYTES)throw new IOException("Archive exceeds the supported total size");if(progress!=null)progress.update("Checking recordings",i+1,ids.size());}
        return new Summary(notes.size(),ids.size(),library.length,audioBytes,metadataBytes);
    }
    /** Does not report success until local-provider output reopens and verifies byte-for-byte and internally. */
    public static ArchiveManifest write(Context context,Uri destination,List<Note> notes,BooleanSupplier cancelled,Progress progress)throws Exception{
        LocalBackupFiles.requireLocal(destination);File directory=directory(context);try{
            Map<String,File> sources=new HashMap<>();ArchiveManifest manifest=prepare(context,notes,directory,sources,cancelled,progress);space(directory,manifest.payloadBytes+manifest.encode().length);File archive=new File(directory,"archive.zip");
            try(OutputStream out=new FileOutputStream(archive)){ArchiveZip.write(out,manifest,path->{File source=sources.get(path);if(source==null)throw new IOException("Archive source disappeared");return new FileInputStream(source);},cancelled,done->{if(progress!=null)progress.update("Packing backup",done,manifest.payloadBytes+ArchiveManifest.MAX_MANIFEST_BYTES);});}
            String written;MessageDigest hash=digest();check(cancelled);try(OutputStream out=context.getContentResolver().openOutputStream(destination,"rwt");InputStream in=new FileInputStream(archive)){if(out==null)throw new IOException("Cannot write local archive");byte[] buffer=new byte[64*1024];long done=0;int count;while((count=in.read(buffer))!=-1){check(cancelled);out.write(buffer,0,count);hash.update(buffer,0,count);done+=count;if(progress!=null)progress.update("Writing backup",done,archive.length());}out.flush();written=hex(hash.digest());}
            check(cancelled);long length=archive.length();String reopened=transfer(context.getContentResolver().openInputStream(destination),archive,Math.min(ArchiveZip.MAX_CONTAINER_BYTES,length),directory,cancelled,progress,"Reopening backup");if(!written.equals(reopened) || archive.length()!=length)throw new IOException("Archive readback differs; do not rely on this destination");ArchiveManifest verified=ArchiveZip.read(archive,null,cancelled,done->{if(progress!=null)progress.update("Verifying backup",done,manifest.payloadBytes+ArchiveManifest.MAX_MANIFEST_BYTES);});if(!Arrays.equals(manifest.encode(),verified.encode()))throw new IOException("Archive readback manifest changed");return verified;
        }catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to export this archive; originals retained");}finally{remove(directory);}
    }
    /** Copies/validates into private staging. No current note, recording, source marker or transcript is mutated. */
    public static Stage inspect(Context context,Uri source,BooleanSupplier cancelled,Progress progress)throws Exception{
        LocalBackupFiles.requireLocal(source);File directory=directory(context);boolean accepted=false;try{
            space(directory,0);File archive=new File(directory,"archive.zip");transfer(context.getContentResolver().openInputStream(source),archive,ArchiveZip.MAX_CONTAINER_BYTES,directory,cancelled,progress,"Reading archive");
            ArchiveManifest manifest=ArchiveZip.read(archive,(path,length)->{space(directory,length);return new FileOutputStream(staged(directory,path));},cancelled,done->{if(progress!=null)progress.update("Checking archive",done,ArchiveManifest.MAX_TOTAL_BYTES);});
            List<Note> notes=ArchiveJson.library(small(staged(directory,ArchiveManifest.LIBRARY_PATH),ArchiveManifest.MAX_LIBRARY_BYTES));
            for(ArchiveManifest.Recording recording:manifest.recordings){check(cancelled);wave(staged(directory,ArchiveManifest.audioPath(recording.id)));if(recording.metadataBytes>0)ArchiveJson.metadata(small(staged(directory,ArchiveManifest.metadataPath(recording.id)),ArchiveManifest.MAX_METADATA_BYTES),recording.id);}
            bytes(staged(directory,ArchiveManifest.MANIFEST_PATH),manifest.encode());Stage stage=new Stage(directory,manifest,notes);accepted=true;return stage;
        }catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to validate this archive; originals retained");}finally{if(!accepted)remove(directory);}
    }
    /**
     * Idempotently installs only verified, missing archive recordings. Existing audio is
     * never replaced. The caller must commit the merged library after this method succeeds;
     * if a process stops between these steps, retrying the same archive safely reuses files.
     */
    static void install(Context context,Stage stage,ArchiveMerge.Plan plan,BooleanSupplier cancelled,Progress progress)throws Exception{
        if(stage==null || plan==null || stage.closed)throw new IOException("Archive preview is no longer available");
        Map<String,ArchiveManifest.Recording> sourceEntries=new HashMap<>();for(ArchiveManifest.Recording recording:stage.manifest.recordings)sourceEntries.put(recording.id,recording);
        Set<String> seenSources=new HashSet<>(),seenTargets=new HashSet<>();if(plan.recordings.size()!=sourceEntries.size())throw new IOException("Archive plan does not match its manifest");
        for(ArchiveMerge.Target target:plan.recordings){
            check(cancelled);ArchiveManifest.Recording recording=sourceEntries.get(target.sourceId);if(recording==null || !seenSources.add(target.sourceId) || !seenTargets.add(target.targetId))throw new IOException("Archive plan has duplicate or missing recording identities");ArchiveManifest.requireId(target.targetId);
            VoiceFiles voices=new VoiceFiles(context);File destination=voices.audio(target.targetId);
            if(target.addAudio){if(destination.exists())throw new IOException("A recording appeared after preview; review the archive again");installAudio(stage.audio(target.sourceId),destination,recording,cancelled,progress);}
            else verifyAudio(destination,recording,cancelled);
            if(recording.metadataBytes>0){
                byte[] archiveMetadata=small(stage.metadata(recording.id),ArchiveManifest.MAX_METADATA_BYTES);String expected=ArchiveJson.metadataContentHash(archiveMetadata,recording.id);byte[] current=voices.archiveMetadata(target.targetId);
                if(target.addMetadata){if(current!=null)throw new IOException("A transcript appeared after preview; review the archive again");VoiceFiles.ArchiveMetadataInstall installed=voices.installArchiveMetadataIfAbsentWithResult(target.targetId,remappedMetadata(stage,recording,target.targetId));if(!installed.installed)throw new IOException("A transcript changed after preview; review the archive again");current=installed.bytes;}
                else if(current==null)throw new IOException("A transcript disappeared after preview; review the archive again");
                if(!expected.equals(ArchiveJson.metadataContentHash(current,target.targetId)))throw new IOException("Recording transcript changed after preview; review the archive again");
            }else if(target.addMetadata)throw new IOException("Archive plan refers to a transcript missing from its manifest");
            if(progress!=null)progress.update("Restoring recordings",seenSources.size(),sourceEntries.size());
        }
        if(!seenSources.equals(sourceEntries.keySet()))throw new IOException("Archive plan omitted a recording");
    }
    private static void installAudio(File source,File destination,ArchiveManifest.Recording expected,BooleanSupplier cancelled,Progress progress)throws IOException{
        File parent=destination.getParentFile();if(parent==null || !parent.isDirectory())throw new IOException("Voice inbox is unavailable; no archive imported");File temporary=File.createTempFile(".archive-restore-",".part",parent);
        try{
            MessageDigest digest=digest();long total=0;try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(temporary)){byte[] buffer=new byte[64*1024];int count,stalled=0;while((count=in.read(buffer))!=-1){check(cancelled);if(count==0){if(++stalled>32)throw new IOException("Archive recording made no progress");continue;}stalled=0;total+=count;if(total>expected.audioBytes)throw new IOException("Archive recording exceeds its declared length");out.write(buffer,0,count);digest.update(buffer,0,count);if(progress!=null)progress.update("Restoring recordings",total,expected.audioBytes);}out.getFD().sync();}
            if(total!=expected.audioBytes || !expected.audioSha256.equals(hex(digest.digest())))throw new IOException("Archive recording failed digest verification");wave(temporary);
            synchronized(VoiceFiles.AUDIO_LOCK){if(destination.exists())throw new IOException("A recording appeared after preview; review the archive again");java.nio.file.Files.move(temporary.toPath(),destination.toPath());}
            if(temporary.exists() && !temporary.delete())throw new IOException("Private archive temporary file needs cleanup");if(expected.createdAt>0)destination.setLastModified(expected.createdAt);verifyAudio(destination,expected,cancelled);
        }finally{if(temporary.exists() && !temporary.delete())throw new IOException("Private archive temporary file needs cleanup");}
    }
    private static void verifyAudio(File file,ArchiveManifest.Recording expected,BooleanSupplier cancelled)throws IOException{
        if(!file.isFile() || file.length()!=expected.audioBytes)throw new IOException("A local recording changed after preview; review the archive again");wave(file);if(!expected.audioSha256.equals(hash(file,ArchiveManifest.MAX_AUDIO_BYTES,cancelled)))throw new IOException("A local recording differs from the preview; nothing was overwritten");
    }
    /** Read-only conflict inventory and note-link preview; encoding must fit before any transaction starts. */
    public static ArchiveMerge.Plan preview(Context context,Stage stage,List<Note> current,BooleanSupplier cancelled)throws Exception{
        try{if(stage.closed)throw new IOException("Archive preview has already been discarded");Map<String,String> fingerprints=new HashMap<>();for(ArchiveManifest.Recording recording:stage.manifest.recordings){check(cancelled);if(recording.metadataBytes>0)fingerprints.put(recording.id,ArchiveJson.metadataContentHash(small(stage.metadata(recording.id),ArchiveManifest.MAX_METADATA_BYTES),recording.id));}
        VoiceFiles voices=new VoiceFiles(context);Map<String,ArchiveMerge.Content> cache=new HashMap<>();Set<String> absent=new HashSet<>();ArchiveMerge.Plan plan=ArchiveMerge.preview(stage.manifest,fingerprints,current,stage.notes,id->{
            check(cancelled);if(cache.containsKey(id))return cache.get(id);if(absent.contains(id))return null;File audio=voices.audio(id);if(!audio.exists()){byte[] orphanMetadata=voices.archiveMetadata(id);if(orphanMetadata==null){absent.add(id);return null;}String orphanFingerprint=ArchiveJson.metadataContentHash(orphanMetadata,id);ArchiveMerge.Content orphan=new ArchiveMerge.Content(0,"0000000000000000000000000000000000000000000000000000000000000000",orphanFingerprint);cache.put(id,orphan);return orphan;}if(!audio.isFile())throw new IOException("Current recording is not a regular file; nothing imported");long length=audio.length();String audioHash=length>ArchiveManifest.MAX_AUDIO_BYTES?String.join("",Collections.nCopies(64,"0")):hash(audio,ArchiveManifest.MAX_AUDIO_BYTES,cancelled);byte[] metadata=voices.archiveMetadata(id);String fingerprint=metadata==null?null:ArchiveJson.metadataContentHash(metadata,id);ArchiveMerge.Content content=new ArchiveMerge.Content(length,audioHash,fingerprint);cache.put(id,content);return content;
        });DocumentCodec.encode(plan.notes.notes);return plan;}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to preview this archive; originals retained");}
    }

}
