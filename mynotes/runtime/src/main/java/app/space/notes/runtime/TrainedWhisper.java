package app.space.notes.runtime;

import android.content.Context;
import app.space.notes.domain.PcmWave;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Actual trained Whisper blocks, bundled CPU inference. Experimental opt-in, never a downloader. */
public final class TrainedWhisper {
    private TrainedWhisper(){}
    private static native long create(String model)throws IOException;
    private static native byte[] decode(long session,float[] pcm,int count)throws IOException;
    private static native void cancel(long session);
    private static native void release(long session);
    public static boolean selected(Context context){return context.getSharedPreferences("mynotes-ui",0).getBoolean("trained-whisper",false);}
    public static void select(Context context,boolean selected){context.getSharedPreferences("mynotes-ui",0).edit().putBoolean("trained-whisper",selected).apply();}
    public static OfflineSpeech.Result transcribe(Context context,File wave,AtomicBoolean cancelled)throws Exception{
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Speech inference must run off UI thread");
        if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");
        return WhisperProcessClient.transcribe(context,wave,cancelled);
    }
    static OfflineSpeech.Result transcribeInWorker(Context context,File wave,AtomicBoolean cancelled)throws Exception{
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Speech inference must run off UI thread");
        if(cancelled.get())throw new InterruptedException("Cancelled; audio retained");
        android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();((android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memory);
        if(memory.lowMemory || memory.availMem<384L*1024*1024)throw new IOException("Not enough free memory for experimental speech. Choose standard offline English; original audio retained.");
        File model=install(context,cancelled);
        try{System.loadLibrary("notes_whisper");}catch(LinkageError unavailable){throw new IOException("Experimental CPU engine is unavailable on this device. Choose the standard engine; audio retained.",unavailable);}
        final Object lock=new Object();final long[] session={create(model.getPath())};if(session[0]==0)throw new IOException("Experimental model unavailable; original audio retained");
        ScheduledExecutorService observer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"notes-whisper-cancellation");t.setDaemon(true);return t;});
        observer.scheduleWithFixedDelay(()->{if(cancelled.get())synchronized(lock){if(session[0]!=0)cancel(session[0]);}},0,100,TimeUnit.MILLISECONDS);
        StringBuilder text=new StringBuilder();
        try(RandomAccessFile input=new RandomAccessFile(wave,"r")){
            PcmWave.Data data=PcmWave.inspect(input);input.seek(data.offset);long remaining=data.bytes;
            byte[] bytes=new byte[480000*2];float[] pcm=new float[480000];
            while(remaining>0){
                if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");
                int size=(int)Math.min(bytes.length,remaining);input.readFully(bytes,0,size);remaining-=size;boolean nonzero=false;
                for(int i=0;i<size/2;i++){short value=(short)((bytes[i*2]&255)|(bytes[i*2+1]<<8));pcm[i]=value/32768f;nonzero|=value!=0;}
                if(nonzero){byte[] output;try{output=decode(session[0],pcm,size/2);}catch(IOException failure){if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");throw failure;}if(cancelled.get())throw new InterruptedException("Cancelled; original audio retained");String segment=new String(output,StandardCharsets.UTF_8).trim();if(!segment.isEmpty()){if(text.length()>0)text.append(' ');text.append(segment);}}
                if(text.length()>100000)throw new IOException("Transcript exceeds safe editing size; original audio retained");
            }
            // No fabricated word confidence: experimental results always require user review.
            return new OfflineSpeech.Result(text.toString(),text.toString(),Double.NaN);
        }finally{observer.shutdownNow();synchronized(lock){release(session[0]);session[0]=0;}}
    }
    private static synchronized File install(Context context,AtomicBoolean cancelled)throws Exception{
        JSONObject manifest;try(InputStream input=context.getAssets().open("voice/trained-whisper.json")){manifest=new JSONObject(new String(LocalOpenFile.bounded(input,65536),StandardCharsets.UTF_8));}
        if(manifest.getLong("training_run")!=41393453222L || !manifest.getString("checkpoint_sha256").equals("84dcc55f0c1bcdc53e62d90a50c4dbe284669222f13ecbcbf458d62af42bc338"))throw new IOException("Wrong experimental model provenance");
        String hash=manifest.getString("sha256");if(!hash.matches("[a-f0-9]{64}"))throw new IOException("Invalid model manifest");
        File folder=new File(context.getFilesDir(),"trained-whisper-v1");if(!folder.isDirectory() && !folder.mkdirs())throw new IOException("Cannot prepare private model storage");
        File target=new File(folder,hash+".bin");if(target.isFile() && target.length()==manifest.getLong("bytes") && hash.equals(digest(target,cancelled)))return target;
        File pending=File.createTempFile("install-",".pending",folder);
        try{
            long size=0;try(InputStream input=context.getAssets().open("voice/trained-whisper.bin");FileOutputStream output=new FileOutputStream(pending)){
                byte[] buffer=new byte[65536];for(int count;(count=input.read(buffer))!=-1;){if(cancelled.get())throw new InterruptedException("Cancelled; audio retained");size+=count;if(size>manifest.getLong("bytes"))throw new IOException("Bundled model exceeds manifest");output.write(buffer,0,count);}output.getFD().sync();
            }
            if(size!=manifest.getLong("bytes") || !hash.equals(digest(pending,cancelled)))throw new IOException("Bundled model verification failed");
            if(target.exists() && !target.delete())throw new IOException("Cannot replace damaged model");if(!pending.renameTo(target))throw new IOException("Cannot install verified model");return target;
        }finally{pending.delete();}
    }
    private static String digest(File path,AtomicBoolean cancelled)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(path)){byte[] buffer=new byte[65536];for(int size;(size=input.read(buffer))!=-1;){if(cancelled.get())throw new InterruptedException("Cancelled; audio retained");hash.update(buffer,0,size);}}
        StringBuilder out=new StringBuilder();for(byte b:hash.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
    }
}
