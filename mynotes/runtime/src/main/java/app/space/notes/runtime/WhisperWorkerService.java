package app.space.notes.runtime;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Private same-UID bound process. Native faults must not kill the Notes activity/process. */
public final class WhisperWorkerService extends Service {
    static final int RUN=1,CANCEL=2,OK=3,FAILED=4,PROGRESS=5;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean active=new AtomicBoolean(),cancelled=new AtomicBoolean();
    private final Messenger endpoint=new Messenger(new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message message){
            if(message.sendingUid!=android.os.Process.myUid())return;
            if(message.what==CANCEL){cancelled.set(true);return;}
            if(message.what!=RUN || message.replyTo==null)return;
            final Messenger reply=message.replyTo;
            if(!active.compareAndSet(false,true)){send(reply,FAILED,"Experimental engine is busy; original audio retained");return;}
            cancelled.set(false);final String path=message.getData().getString("path","");
            worker.execute(()->{try{
                File audio=new File(path).getCanonicalFile();String file=audio.getPath();
                if(!(file.startsWith(getFilesDir().getCanonicalPath()+File.separator) || file.startsWith(getCacheDir().getCanonicalPath()+File.separator)) || !audio.isFile())throw new java.io.IOException("Original recording unavailable in private storage");
                OfflineSpeech.Result result=TrainedWhisper.transcribeInWorker(WhisperWorkerService.this,audio,cancelled,phase->send(reply,PROGRESS,phase));
                send(reply,OK,result.raw);
            }catch(Exception | OutOfMemoryError error){send(reply,FAILED,cancelled.get()?"Experimental recognition cancelled; original audio retained":"Experimental recognition could not finish. Choose standard offline English; original audio retained.");}
            finally{active.set(false);}});
        }
    });
    private void send(Messenger reply,int what,String text){try{Message response=Message.obtain(null,what);Bundle data=new Bundle();data.putString("text",text);response.setData(data);reply.send(response);}catch(RemoteException disconnected){/* Caller left; never delete its source audio. */}}
    @Override public IBinder onBind(Intent intent){return endpoint.getBinder();}
    @Override public void onDestroy(){cancelled.set(true);worker.shutdown();super.onDestroy();}
}
