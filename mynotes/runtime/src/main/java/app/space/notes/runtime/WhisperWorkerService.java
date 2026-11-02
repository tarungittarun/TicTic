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
    private static final AtomicBoolean PROCESS_ACTIVE=new AtomicBoolean();
    private boolean destroyed;private String activeRequest="";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean active=new AtomicBoolean(),cancelled=new AtomicBoolean();
    private final Messenger endpoint=new Messenger(new Handler(Looper.getMainLooper()){
        @Override public void handleMessage(Message message){
            if(message.sendingUid!=android.os.Process.myUid())return;
            String request=message.getData().getString("request","");
            if(message.what==CANCEL){if(request.equals(activeRequest))cancelled.set(true);return;}
            if(message.what!=RUN || message.replyTo==null)return;
            final Messenger reply=message.replyTo;
            if(destroyed || !request.matches("[a-f0-9-]{36}")){send(reply,FAILED,"Experimental service request unavailable; original audio retained");return;}
            if(!active.compareAndSet(false,true)){send(reply,FAILED,"Experimental engine is busy; original audio retained");return;}
            activeRequest=request;cancelled.set(false);final String path=message.getData().getString("path","");
            worker.execute(()->{if(!PROCESS_ACTIVE.compareAndSet(false,true)){send(reply,FAILED,"Previous experimental recognition is still stopping. Retry shortly; original audio retained.");active.set(false);return;}try{
                File audio=new File(path).getCanonicalFile();String file=audio.getPath();
                if(!(file.startsWith(getFilesDir().getCanonicalPath()+File.separator) || file.startsWith(getCacheDir().getCanonicalPath()+File.separator)) || !audio.isFile())throw new java.io.IOException("Original recording unavailable in private storage");
                OfflineSpeech.Result result=TrainedWhisper.transcribeInWorker(WhisperWorkerService.this,audio,cancelled,phase->send(reply,PROGRESS,phase));
                send(reply,OK,result.raw);
            }catch(Exception | OutOfMemoryError error){send(reply,FAILED,cancelled.get()?"Experimental recognition cancelled; original audio retained":"Experimental recognition could not finish. Choose standard offline English; original audio retained.");}
            finally{PROCESS_ACTIVE.set(false);active.set(false);}});
        }
    });
    private void send(Messenger reply,int what,String text){try{Message response=Message.obtain(null,what);Bundle data=new Bundle();data.putString("text",text);response.setData(data);reply.send(response);}catch(RemoteException disconnected){if(what!=FAILED && !(disconnected instanceof DeadObjectException))send(reply,FAILED,"Experimental result could not be delivered; original audio retained");}}
    @Override public IBinder onBind(Intent intent){return endpoint.getBinder();}
    @Override public void onDestroy(){destroyed=true;cancelled.set(true);worker.shutdown();super.onDestroy();}
}
