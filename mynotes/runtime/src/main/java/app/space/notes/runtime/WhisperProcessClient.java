package app.space.notes.runtime;

import android.content.*;
import android.os.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Binder death becomes an explicit recoverable error, never an app-wide native crash. */
final class WhisperProcessClient {
    private WhisperProcessClient(){}
    static OfflineSpeech.Result transcribe(Context supplied,File audio,AtomicBoolean cancelled)throws Exception{
        Context context=supplied.getApplicationContext();CountDownLatch done=new CountDownLatch(1);
        AtomicReference<Messenger> remote=new AtomicReference<>();AtomicReference<String> result=new AtomicReference<>(),failure=new AtomicReference<>();
        Messenger reply=new Messenger(new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message message){String value=message.getData().getString("text","");if(message.what==WhisperWorkerService.OK)result.set(value);else failure.set(value);done.countDown();}
        });
        ServiceConnection connection=new ServiceConnection(){
            @Override public void onServiceConnected(ComponentName name,IBinder binder){try{Messenger endpoint=new Messenger(binder);remote.set(endpoint);Message request=Message.obtain(null,WhisperWorkerService.RUN);Bundle data=new Bundle();data.putString("path",audio.getAbsolutePath());request.setData(data);request.replyTo=reply;endpoint.send(request);}catch(RemoteException error){failure.set("Experimental CPU engine disconnected; original audio retained");done.countDown();}}
            @Override public void onServiceDisconnected(ComponentName name){failure.set("Experimental CPU process stopped or crashed. Notes and original audio are retained; choose standard offline English.");done.countDown();}
            @Override public void onBindingDied(ComponentName name){onServiceDisconnected(name);}
            @Override public void onNullBinding(ComponentName name){onServiceDisconnected(name);}
        };
        boolean bound=false;long began=SystemClock.elapsedRealtime();
        try{
            bound=context.bindService(new Intent(context,WhisperWorkerService.class),connection,Context.BIND_AUTO_CREATE);
            if(!bound)throw new IOException("Experimental CPU service unavailable; original audio retained");
            while(!done.await(100,TimeUnit.MILLISECONDS)){
                if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");
                if(SystemClock.elapsedRealtime()-began>30*60*1000L)throw new IOException("Experimental speech time limit reached; original audio retained");
            }
            if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");
            if(failure.get()!=null)throw new IOException(failure.get());
            if(result.get()==null)throw new IOException("Experimental CPU service returned no result; original audio retained");
            return new OfflineSpeech.Result(result.get(),result.get(),Double.NaN);
        }finally{
            Messenger endpoint=remote.get();if(endpoint!=null)try{endpoint.send(Message.obtain(null,WhisperWorkerService.CANCEL));}catch(RemoteException ignored){}
            if(bound)context.unbindService(connection);
        }
    }
}
