package app.space.notes.runtime;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** One native inference at a time, independent of disk saves and surviving Activity rotation. */
public final class VoiceTasks {
    public interface Listener {void complete(String id,String text,String failure,boolean previouslyCreated);}
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static final Set<String> running=ConcurrentHashMap.newKeySet();
    private static final Map<String,CopyOnWriteArrayList<Listener>> listeners=new ConcurrentHashMap<>();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private VoiceTasks() { }
    public static Runnable subscribe(Context context,Listener listener){String key=context.getFilesDir().getAbsolutePath();listeners.computeIfAbsent(key,k->new CopyOnWriteArrayList<>()).add(listener);return ()->{CopyOnWriteArrayList<Listener> list=listeners.get(key);if(list!=null){list.remove(listener);if(list.isEmpty())listeners.remove(key,list);}};}
    public static void transcribe(Context context,String id){
        Context app=context.getApplicationContext();String key=app.getFilesDir().getAbsolutePath(),job=key+"/"+id;synchronized(ArchiveRestoreJournal.transactionLock()){if(ArchiveRestoreJournal.suppressesAutomaticNote(app.getFilesDir(),id) || !running.add(job))return;}
        worker.execute(()->{
            String text="",failure="";
            try{VoiceFiles files=new VoiceFiles(app);files.result(id,"","Transcribing offline");boolean experimental=TrainedWhisper.selected(app);OfflineSpeech.Result recognized=experimental?TrainedWhisper.transcribe(app,files.audio(id),new AtomicBoolean()):OfflineSpeech.transcribeResult(app,files.audio(id),new AtomicBoolean());String candidate=recognized.text;text=recognized.reviewRequired?"":candidate;files.result(id,candidate,candidate.isEmpty()?"No speech recognized; audio retained":recognized.reviewRequired?"REVIEW":"DONE",recognized.raw,recognized.reviewRequired?(experimental?"Experimental trained Whisper · 593-step single-speaker adaptation. No calibrated confidence; review every result against audio.":"Standard Vosk: uncertain words. Review against audio; word confidence is not an accuracy guarantee."):"Standard Vosk · pause-based readability; review punctuation against audio.");}
            catch(Exception | LinkageError | OutOfMemoryError e){failure=e.getClass().getSimpleName()+": "+(e.getMessage()==null?"inference unavailable":e.getMessage());try{new VoiceFiles(app).result(id,"","Failed; audio retained · "+failure);}catch(Exception ignored){ }android.util.Log.w("MyNotes","voice-transcription outcome=failed "+e.getClass().getSimpleName());}
            finally{running.remove(job);}
            boolean created;try{created=new VoiceFiles(app).noteWasCreated(id);}catch(Exception unavailable){created=true;}
            final boolean previouslyCreated=created;
            final String result=text,error=failure;main.post(()->{List<Listener> current=listeners.get(key);if(current!=null)for(Listener listener:current)listener.complete(id,result,error,previouslyCreated);});
        });
    }
    public static boolean running(Context context,String id){return running.contains(context.getFilesDir().getAbsolutePath()+"/"+id);}
}
