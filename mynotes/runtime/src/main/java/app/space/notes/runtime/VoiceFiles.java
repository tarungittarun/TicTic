package app.space.notes.runtime;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import org.json.JSONObject;
import app.space.notes.domain.PcmWave;

/** Independent local voice inbox. Audio is durable before any model is invoked. Never auto-delete. */
public final class VoiceFiles {
    static final Object AUDIO_LOCK=new Object();
    private static final Object TRANSCRIPT_LOCK=new Object();
    private final File directory;
    public VoiceFiles(Context context) throws IOException {directory=new File(context.getFilesDir(),"voice-inbox-v1");if(!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory())throw new IOException("Cannot create private voice inbox");}
    private String checked(String id){UUID.fromString(id);return id;}
    public File audio(String id){return new File(directory,checked(id)+".wav");}
    public File pending(String id){return new File(directory,checked(id)+".wav.part");}
    public File transcript(String id){return new File(directory,checked(id)+".transcript.json");}
    public synchronized File complete(String id) throws Exception {synchronized(AUDIO_LOCK){File source=pending(id);File destination=audio(id);if(destination.exists()){if(!source.exists())return destination;throw new IOException("Refusing to overwrite a recording");}PcmWave.repair(source);if(!source.renameTo(destination))throw new IOException("Recording remains in private recovery; reopen Voice inbox");return destination;}}
    public synchronized List<String> list() throws IOException {
        File[] files=directory.listFiles();if(files==null)throw new IOException("Cannot read voice inbox");List<String> ids=new ArrayList<>();
        for(File f:files){String name=f.getName();if(name.endsWith(".wav.part")){
            String id=name.substring(0,name.length()-9);try{checked(id);if(f.length()>=44 && !audio(id).exists()){try(RandomAccessFile candidate=new RandomAccessFile(f,"rw");java.nio.channels.FileLock lock=candidate.getChannel().tryLock()){if(lock==null)continue;synchronized(AUDIO_LOCK){if(!audio(id).exists()){PcmWave.repair(f);if(!f.renameTo(audio(id)))continue;}}}}}catch(Exception damaged){continue;}
        }}
        files=directory.listFiles();if(files==null)throw new IOException("Cannot reopen voice inbox");for(File f:files)if(f.getName().endsWith(".wav")){String id=f.getName().substring(0,f.getName().length()-4);try{checked(id);ids.add(id);}catch(IllegalArgumentException invalid){/* Only owned UUID names are recordings. */}}
        ids.sort((a,b)->Long.compare(audio(b).lastModified(),audio(a).lastModified()));return ids;
    }
    private boolean durableNoteMarker(String id)throws IOException{
        AtomicFile marker=new AtomicFile(new File(directory,checked(id)+".note-created"));if(!marker.getBaseFile().exists() && !new File(marker.getBaseFile()+".bak").exists())return false;byte[] bytes;try(InputStream in=marker.openRead()){bytes=LocalOpenFile.bounded(in,1);}if(bytes.length!=1 || bytes[0]!=1)throw new IOException("Voice creation marker cannot be verified; audio retained");return true;
    }
    public boolean noteWasCreated(String id)throws IOException{synchronized(ArchiveRestoreJournal.transactionLock()){checked(id);if(ArchiveRestoreJournal.suppressesAutomaticNote(directory.getParentFile(),id))return true;synchronized(TRANSCRIPT_LOCK){return durableNoteMarker(id);}}}
    public synchronized void noteCreated(String id) throws IOException {synchronized(TRANSCRIPT_LOCK){if(!audio(id).isFile() || durableNoteMarker(id))return;AtomicFile marker=new AtomicFile(new File(directory,checked(id)+".note-created"));FileOutputStream out=null;try{out=marker.startWrite();out.write(1);out.getFD().sync();marker.finishWrite(out);out=null;}finally{if(out!=null)marker.failWrite(out);}}}
    public synchronized void result(String id,String text,String state)throws Exception{result(id,text,state,text,"");}
    public synchronized void result(String id,String text,String state,String rawText,String formatting) throws Exception {synchronized(TRANSCRIPT_LOCK){
        // A failed retry or transient RUNNING update must not erase a previously successful transcript.
        if(text.isEmpty() && (transcript(id).exists() || new File(transcript(id)+".bak").exists())){try{JSONObject previous=result(id);text=previous.optString("text","");rawText=previous.optString("rawText",text);formatting=previous.optString("formatting","");}catch(Exception damaged){/* Original audio remains independent. */}}

        if(!audio(id).isFile())throw new IOException("Recording unavailable; transcript not written");AtomicFile target=new AtomicFile(transcript(id));FileOutputStream stream=null;
        try{stream=target.startWrite();stream.write(new JSONObject().put("version",1).put("id",id).put("text",text).put("rawText",rawText).put("formatting",formatting).put("state",state).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));target.finishWrite(stream);stream=null;}
        catch(Exception e){if(stream!=null)target.failWrite(stream);throw e;}
    }}
    public synchronized JSONObject result(String id) throws Exception {synchronized(TRANSCRIPT_LOCK){
        AtomicFile file=new AtomicFile(transcript(id));if(!file.getBaseFile().exists() && !new File(file.getBaseFile()+".bak").exists())return new JSONObject().put("state","Audio retained; no transcript yet").put("text","");try(InputStream in=file.openRead()){return ArchiveJson.metadata(LocalOpenFile.bounded(in,app.space.notes.domain.ArchiveManifest.MAX_METADATA_BYTES),id);}
    }}
    /** Bounded atomic snapshot for a user-requested archive; never reads a live partial transcript. */
    public byte[] archiveMetadata(String id)throws IOException{synchronized(TRANSCRIPT_LOCK){
        AtomicFile file=new AtomicFile(transcript(id));if(!file.getBaseFile().exists() && !new File(file.getBaseFile()+".bak").exists())return null;
        try(InputStream in=file.openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count,stalled=0;while((count=in.read(buffer))!=-1){if(count==0){if(++stalled>32)throw new IOException("Transcript stream made no progress");continue;}stalled=0;if(out.size()+count>app.space.notes.domain.ArchiveManifest.MAX_METADATA_BYTES)throw new IOException("Transcript exceeds archive bounds; original retained");out.write(buffer,0,count);}return out.toByteArray();
        }
    }}
    /** Result distinguishes a newly installed archive transcript from an existing local one. */
    public static final class ArchiveMetadataInstall {
        public final byte[] bytes;
        public final boolean installed;
        private ArchiveMetadataInstall(byte[] bytes,boolean installed){this.bytes=bytes;this.installed=installed;}
    }
    /** Restore a transcript only when absent; never overwrite newer local recognition/review. */
    public byte[] installArchiveMetadataIfAbsent(String id,byte[] bytes)throws IOException{return installArchiveMetadataIfAbsentWithResult(id,bytes).bytes;}
    public ArchiveMetadataInstall installArchiveMetadataIfAbsentWithResult(String id,byte[] bytes)throws IOException{synchronized(TRANSCRIPT_LOCK){
        checked(id);if(!audio(id).isFile())throw new IOException("Recording unavailable; transcript not restored");ArchiveJson.metadata(bytes,id);
        byte[] current=archiveMetadata(id);if(current!=null)return new ArchiveMetadataInstall(current,false);
        AtomicFile target=new AtomicFile(transcript(id));FileOutputStream stream=null;
        try{stream=target.startWrite();stream.write(bytes);stream.getFD().sync();target.finishWrite(stream);stream=null;byte[] installed=archiveMetadata(id);if(!java.util.Arrays.equals(bytes,installed))throw new IOException("Recording transcript restore failed verification");return new ArchiveMetadataInstall(installed,true);}
        catch(Exception failure){if(stream!=null)target.failWrite(stream);throw failure instanceof IOException?(IOException)failure:new IOException("Recording transcript could not be restored",failure);}
    }}
    /** Exact-hash cleanup for a transcript that this archive transaction installed into an absent slot. */
    public boolean removeArchiveMetadataIfMatches(String id,String expectedSha256)throws IOException{synchronized(TRANSCRIPT_LOCK){
        checked(id);byte[] current=archiveMetadata(id);if(current==null || !ArchiveRestoreJournal.sha256(current).equals(expectedSha256))return false;new AtomicFile(transcript(id)).delete();File base=transcript(id);return !base.exists() && !new File(base+".bak").exists();
    }}
    /** Exact-hash cleanup for a recording that this archive transaction installed into an absent slot. */
    public boolean removeArchiveAudioIfMatches(String id,long expectedBytes,String expectedSha256)throws IOException{synchronized(TRANSCRIPT_LOCK){synchronized(AUDIO_LOCK){
        File file=audio(checked(id));if(!file.isFile() || file.length()!=expectedBytes)return false;
        try(InputStream in=new FileInputStream(file)){java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[64*1024];long total=0;int count;while((count=in.read(buffer))!=-1){total+=count;if(total>expectedBytes)return false;digest.update(buffer,0,count);}if(total!=expectedBytes || !ArchiveRestoreJournal.hex(digest.digest()).equals(expectedSha256))return false;}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        if(!file.delete())return false;return !file.exists();
    }}}

}
