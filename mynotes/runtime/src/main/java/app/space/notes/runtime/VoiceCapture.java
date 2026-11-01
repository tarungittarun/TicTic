package app.space.notes.runtime;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.*;
import java.io.*;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import app.space.notes.domain.PcmWave;

/** Foreground-only capture. Bytes/header are checkpointed; stop and disk I/O never block UI. */
public final class VoiceCapture implements Runnable {
    public interface Listener {void started(String id);void saved(String id);void failed(String message);default void level(int peak){}}
    private final Context context;private final VoiceFiles files;private final Listener listener;
    private final AtomicBoolean running=new AtomicBoolean(true);public final String id=UUID.randomUUID().toString();
    public VoiceCapture(Context context,Listener listener) throws IOException {this.context=context.getApplicationContext();files=new VoiceFiles(this.context);this.listener=listener;}
    public void stop(){running.set(false);}
    private static AudioRecord openSpeechRecorder(int minimum)throws IOException{RuntimeException last=null;for(int source:new int[]{MediaRecorder.AudioSource.VOICE_COMMUNICATION,MediaRecorder.AudioSource.VOICE_RECOGNITION}){AudioRecord recorder=null;try{recorder=new AudioRecord(source,PcmWave.RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(minimum,6400));if(recorder.getState()==AudioRecord.STATE_INITIALIZED){Diagnostics.record("voice.capture.source",source==MediaRecorder.AudioSource.VOICE_COMMUNICATION?"system speech processing":"recognition fallback");return recorder;}}catch(RuntimeException unsupported){last=unsupported;}if(recorder!=null)recorder.release();}throw new IOException("No supported microphone source",last);}
    @Override public void run(){
        AudioRecord recorder=null;android.media.audiofx.NoiseSuppressor suppression=null;
        try{
            if(!running.get()){listener.failed("Recording cancelled before microphone start; no audio was created.");return;}
            if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microphone permission was not granted. Other notes features are unaffected.");
            int minimum=AudioRecord.getMinBufferSize(PcmWave.RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(minimum<=0)throw new IOException("Device does not support mono 16 kHz recording");
            recorder=openSpeechRecorder(minimum);
            if(android.os.Build.VERSION.SDK_INT>=29)try{recorder.setPreferredMicrophoneDirection(android.media.MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER);}catch(RuntimeException unsupported){Diagnostics.record("voice.microphone.direction","device did not accept direction hint");}
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("Microphone is unavailable");try{if(android.media.audiofx.NoiseSuppressor.isAvailable()){suppression=android.media.audiofx.NoiseSuppressor.create(recorder.getAudioSessionId());if(suppression!=null)suppression.setEnabled(true);}}catch(RuntimeException unsupported){if(suppression!=null)suppression.release();suppression=null;}recorder.startRecording();if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IOException("Microphone did not start");
            try(RandomAccessFile output=new RandomAccessFile(files.pending(id),"rw");FileLock lock=output.getChannel().lock()){
                PcmWave.header(output,0);output.getFD().sync();listener.started(id);byte[] buffer=new byte[3200];long bytes=0,last=0,meterAt=0;
                while(running.get()){
                    int count=recorder.read(buffer,0,buffer.length);if(count<0)throw new IOException("Microphone read error "+count+"; captured bytes remain in Voice inbox recovery");if(count==0)continue;
                    output.write(buffer,0,count);bytes+=count;if(android.os.SystemClock.elapsedRealtime()-meterAt>=250){int peak=app.space.notes.domain.PcmLevel.peak(buffer,count);listener.level(peak);meterAt=android.os.SystemClock.elapsedRealtime();}
                    if(bytes-last>=32000){PcmWave.header(output,bytes&~1L);output.getFD().sync();last=bytes;}
                    if(bytes>=0xffffffffL-36-6400)running.set(false); // RIFF's actual 32-bit container boundary, not a note/library cap.
                }
                output.setLength(PcmWave.HEADER+(bytes&~1L));PcmWave.header(output,bytes&~1L);output.getFD().sync();
            }
            recorder.stop();files.complete(id);listener.saved(id);
        }catch(Exception | LinkageError error){listener.failed(error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());}
        finally{if(suppression!=null)try{suppression.release();}catch(RuntimeException ignored){}if(recorder!=null){try{if(recorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)recorder.stop();}catch(RuntimeException ignored){ }recorder.release();}running.set(false);}
    }
}
