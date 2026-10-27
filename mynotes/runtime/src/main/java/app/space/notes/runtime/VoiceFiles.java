package app.space.notes.runtime;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import org.json.JSONObject;
import app.space.notes.domain.PcmWave;

/** Independent local voice inbox. Audio is durable before any model is invoked. Never auto-delete. */
public final class VoiceFiles {
    private static final Object TRANSCRIPT_LOCK=new Object();
    private final File directory;
    public VoiceFiles(Context context) throws IOException {directory=new File(context.getFilesDir(),"voice-inbox-v1");if(!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory())throw new IOException("Cannot create private voice inbox");}
    private String checked(String id){UUID.fromString(id);return id;}
    public File audio(String id){return new File(directory,checked(id)+".wav");}
    public File pending(String id){return new File(directory,checked(id)+".wav.part");}
    public File transcript(String id){return new File(directory,checked(id)+".transcript.json");}
    public synchronized File complete(String id) throws Exception {File source=pending(id);File destination=audio(id);if(destination.exists()){if(!source.exists())return destination;throw new IOException("Refusing to overwrite a recording");}PcmWave.repair(source);if(!source.renameTo(destination))throw new IOException("Recording remains in private recovery; reopen Voice inbox");return destination;}
    public synchronized List<String> list() throws IOException {
        File[] files=directory.listFiles();if(files==null)throw new IOException("Cannot read voice inbox");List<String> ids=new ArrayList<>();
        for(File f:files){String name=f.getName();if(name.endsWith(".wav.part")){
            String id=name.substring(0,name.length()-9);try{checked(id);if(f.length()>=44 && !audio(id).exists()){try(RandomAccessFile candidate=new RandomAccessFile(f,"rw");java.nio.channels.FileLock lock=candidate.getChannel().tryLock()){if(lock==null)continue;PcmWave.repair(f);if(!f.renameTo(audio(id)))continue;}}}catch(Exception damaged){continue;}
        }}
        files=directory.listFiles();if(files==null)throw new IOException("Cannot reopen voice inbox");for(File f:files)if(f.getName().endsWith(".wav")){String id=f.getName().substring(0,f.getName().length()-4);try{checked(id);ids.add(id);}catch(IllegalArgumentException invalid){/* Only owned UUID names are recordings. */}}
        ids.sort((a,b)->Long.compare(audio(b).lastModified(),audio(a).lastModified()));return ids;
    }
    public boolean noteWasCreated(String id){return new File(directory,checked(id)+".note-created").isFile();}
    public synchronized void noteCreated(String id) throws IOException {synchronized(TRANSCRIPT_LOCK){if(!audio(id).isFile() || noteWasCreated(id))return;AtomicFile marker=new AtomicFile(new File(directory,checked(id)+".note-created"));FileOutputStream out=null;try{out=marker.startWrite();out.write(1);marker.finishWrite(out);out=null;}finally{if(out!=null)marker.failWrite(out);}}}
    public synchronized void result(String id,String text,String state) throws Exception {synchronized(TRANSCRIPT_LOCK){
        if(!audio(id).isFile())throw new IOException("Recording unavailable; transcript not written");AtomicFile target=new AtomicFile(transcript(id));FileOutputStream stream=null;
        try{stream=target.startWrite();stream.write(new JSONObject().put("version",1).put("id",id).put("text",text).put("state",state).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));target.finishWrite(stream);stream=null;}
        catch(Exception e){if(stream!=null)target.failWrite(stream);throw e;}
    }}
    public synchronized JSONObject result(String id) throws Exception {synchronized(TRANSCRIPT_LOCK){
        AtomicFile file=new AtomicFile(transcript(id));if(!file.getBaseFile().exists() && !new File(file.getBaseFile()+".bak").exists())return new JSONObject().put("state","Audio retained; no transcript yet").put("text","");JSONObject result=new JSONObject(new String(file.readFully(),java.nio.charset.StandardCharsets.UTF_8));if(result.getInt("version")!=1 || !result.getString("id").equals(id))throw new IOException("Unknown or mismatched transcript metadata; audio retained");return result;
    }}
}
