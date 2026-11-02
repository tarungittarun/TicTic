package app.space.notes.runtime;

import android.content.*;
import android.os.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Binder death becomes a recoverable error. Request-scoped cancellation cannot stop a later job. */
final class WhisperProcessClient {
    private WhisperProcessClient(){}
    static OfflineSpeech.Result transcribe(Context supplied,File audio,AtomicBoolean cancelled)throws Exception{
        Context context=supplied.getApplicationContext();CountDownLatch done=new CountDownLatch(1);Object lifecycle=new Object();AtomicBoolean finished=new AtomicBoolean();String requestId=java.util.UUID.randomUUID().toString();
        AtomicReference<Messenger> remote=new AtomicReference<>();AtomicReference<String> result=new AtomicReference<>(),failure=new AtomicReference<>(),phase=new AtomicReference<>("connecting");
        Messenger reply=new Messenger(new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message message){
                if(finished.get() || done.getCount()==0)return;String value=message.getData().getString("text","");
                if(message.what==WhisperWorkerService.PROGRESS){phase.set(value);Diagnostics.record("speech.experimental.phase",value);if((context.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)android.util.Log.i("NotesCpuPhase",value);return;}
                if(message.what==WhisperWorkerService.OK)result.set(value);else if(message.what==WhisperWorkerService.FAILED)failure.set(value);else return;done.countDown();
            }
        });
        ServiceConnection connection=new ServiceConnection(){
            @Override public void onServiceConnected(ComponentName name,IBinder binder){synchronized(lifecycle){
                if(finished.get())return;if(cancelled.get()){failure.set("Cancelled; original audio retained");done.countDown();return;}
                try{Messenger endpoint=new Messenger(binder);remote.set(endpoint);Message request=Message.obtain(null,WhisperWorkerService.RUN);Bundle data=new Bundle();data.putString("request",requestId);data.putString("path",audio.getAbsolutePath());request.setData(data);request.replyTo=reply;endpoint.send(request);}catch(RemoteException error){failure.set("Experimental CPU engine disconnected; original audio retained");done.countDown();}
            }}
            @Override public void onServiceDisconnected(ComponentName name){if(finished.get() || done.getCount()==0)return;failure.set("Experimental CPU process stopped or crashed. Notes and original audio are retained; choose standard offline English.");done.countDown();}
            @Override public void onBindingDied(ComponentName name){onServiceDisconnected(name);}
            @Override public void onNullBinding(ComponentName name){onServiceDisconnected(name);}
        };
        boolean bound=false;long began=SystemClock.elapsedRealtime();
        try{
            bound=context.bindService(new Intent(context,WhisperWorkerService.class),connection,Context.BIND_AUTO_CREATE);
            if(!bound)throw new IOException("Experimental CPU service unavailable; original audio retained");
            while(!done.await(100,TimeUnit.MILLISECONDS)){
                if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");
                if(SystemClock.elapsedRealtime()-began>30*60*1000L)throw new IOException("Experimental speech time limit reached during "+phase.get()+"; original audio retained");
            }
            if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");
            if(failure.get()!=null)throw new IOException(failure.get());
            if(result.get()==null)throw new IOException("Experimental CPU service returned no result; original audio retained");
            return new OfflineSpeech.Result(result.get(),result.get(),Double.NaN);
        }finally{
            synchronized(lifecycle){finished.set(true);Messenger endpoint=remote.get();if(endpoint!=null)try{Message stop=Message.obtain(null,WhisperWorkerService.CANCEL);Bundle data=new Bundle();data.putString("request",requestId);stop.setData(data);endpoint.send(stop);}catch(RemoteException ignored){}}
            if(bound)try{context.unbindService(connection);}catch(IllegalArgumentException alreadyDisconnected){Diagnostics.record("speech.experimental.connection.closed","already disconnected");}
        }
    }
}
