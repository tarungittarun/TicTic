package app.space.notes;

import android.content.Context;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Debug-instrumentation-only observer. Never changes assertions or app behaviour.
 * Captures Java stacks without posting to the possibly blocked app main thread.
 * Contains code locations/test identifiers only, never note text or screenshots.
 */
public final class HangDiagnostics extends RunListener {
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{
        Thread thread=new Thread(r,"notes-ci-stack-observer");thread.setDaemon(true);return thread;
    });
    private String active;
    private long started;
    private int captured;

    @Override public void testRunStarted(Description description){
        worker.scheduleWithFixedDelay(this::observe,15,15,TimeUnit.SECONDS);
    }
    @Override public synchronized void testStarted(Description description){
        active=description.getClassName()+"#"+description.getMethodName();
        started=SystemClock.elapsedRealtime();captured=0;
        write("native-test-progress.txt","START "+active+" elapsedRealtime="+started+"\n");
    }
    @Override public synchronized void testFinished(Description description){
        write("native-test-progress.txt","FINISH "+description.getClassName()+"#"+description.getMethodName()+"\n");
        active=null;
    }
    @Override public void testRunFinished(Result result){worker.shutdownNow();}

    private synchronized void observe(){
        if(active==null)return;
        long elapsed=SystemClock.elapsedRealtime()-started;
        if((captured==0 && elapsed<45000) || (captured==1 && elapsed<120000) || captured>=2)return;
        StringBuilder out=new StringBuilder("CI observation only: a long test is not automatically a failure.\n");
        out.append(active).append(" elapsed_ms=").append(elapsed).append('\n');
        try{
            for(Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet()){
                Thread thread=entry.getKey();
                out.append('\n').append(thread.getName()).append(" id=").append(thread.getId()).append(" state=").append(thread.getState()).append('\n');
                StackTraceElement[] frames=entry.getValue();
                for(int i=0;i<Math.min(64,frames.length);i++)out.append("  at ").append(frames[i]).append('\n');
                if(out.length()>200000){out.setLength(200000);out.append("\nTRUNCATED\n");break;}
            }
            write("native-stall-threads.txt",out.toString());captured++;
        }catch(RuntimeException failure){write("native-stall-threads.txt","Observer failed: "+failure.getClass().getName()+"\n");captured++;}
    }
    private void write(String name,String text){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(FileOutputStream output=new FileOutputStream(new File(context.getFilesDir(),name))){
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }catch(java.io.IOException failure){
            android.util.Log.w("NotesCiDiagnostics","Could not write synthetic CI observation",failure);
        }
    }
}
