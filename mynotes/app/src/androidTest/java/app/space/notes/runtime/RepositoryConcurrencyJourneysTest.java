package app.space.notes.runtime;

import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RepositoryConcurrencyJourneysTest {
    @Test public void schemaUpgradeAndProjectionShareOneMonitorAndRetainOldCheckpoint()throws Exception{
        Context real=InstrumentationRegistry.getInstrumentation().getTargetContext();String prefix="projection-lock-"+UUID.randomUUID();File root=new File(real.getCacheDir(),prefix);assertTrue(root.mkdir());AtomicBoolean freeze=new AtomicBoolean(),sharedMonitor=new AtomicBoolean();CountDownLatch projecting=new CountDownLatch(1),release=new CountDownLatch(1),writerStarted=new CountDownLatch(1);
        Context isolated=new ContextWrapper(real){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return root;}@Override public SharedPreferences getSharedPreferences(String name,int mode){return real.getSharedPreferences(prefix+name,mode);}@Override public Object getSystemService(String name){if(Context.ALARM_SERVICE.equals(name)){if(freeze.get()){sharedMonitor.set(Thread.holdsLock(NotesRepository.SNAPSHOT_LOCK));projecting.countDown();try{if(!release.await(15,TimeUnit.SECONDS))throw new IllegalStateException("Synthetic projection barrier timed out");}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new IllegalStateException(cancelled);}}return null;}return super.getSystemService(name);}};
        ExecutorService workers=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"projection-fixture");t.setDaemon(true);return t;});
        try{
            Note before=Note.create(1).edit("Keep title","Keep body भारत 😀",2);Note after=before.withContent(new DocumentContent(Arrays.asList(DocumentContent.Block.text("writing",new RichText(before.body,Collections.emptyList())))),3);NotesRepository repository=new NotesRepository(isolated);repository.save(Arrays.asList(before));freeze.set(true);
            Future<?> projection=workers.submit(()->{LocalSystemSurfaces.refresh(isolated);return null;});assertTrue(projecting.await(15,TimeUnit.SECONDS));assertTrue("Projection must own the same monitor as schema writes, not a second class lock",sharedMonitor.get());
            Future<Boolean> migration=workers.submit(()->{writerStarted.countDown();return repository.upgradeDocument(Arrays.asList(before),Arrays.asList(after));});assertTrue(writerStarted.await(15,TimeUnit.SECONDS));assertFalse("Writer must not interleave a coherent projection",migration.isDone());release.countDown();projection.get(15,TimeUnit.SECONDS);assertTrue(migration.get(15,TimeUnit.SECONDS));
            assertEquals(after.content.fingerprint(),repository.localSnapshot().get(0).content.fingerprint());assertEquals(before.body,repository.documentUpgradeSnapshot().get(0).body);
        }finally{freeze.set(false);release.countDown();workers.shutdownNow();workers.awaitTermination(3,TimeUnit.SECONDS);remove(root);for(String name:Arrays.asList("mynotes-storage-v1","mynotes-alarms-v1","mynotes-delivered-v1"))real.getSharedPreferences(prefix+name,0).edit().clear().commit();}
    }
    private void remove(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);file.delete();}
}
