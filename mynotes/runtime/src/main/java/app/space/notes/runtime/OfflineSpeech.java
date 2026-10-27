package app.space.notes.runtime;
import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;
import org.json.*;
import org.vosk.*;
import app.space.notes.domain.PcmWave;

/** Bundled Vosk CPU inference. Never invokes SpeechRecognizer, a service, or network. */
public final class OfflineSpeech {
    private OfflineSpeech() { }
    public static String transcribe(Context context,File wave,AtomicBoolean cancelled) throws Exception {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Speech inference must run off UI thread");
        if(cancelled.get())throw new InterruptedException("Transcription cancelled; audio retained");File model=install(context,cancelled);StringBuilder transcript=new StringBuilder();
        LibVosk.vosk_set_log_level(-1);
        try(Model nativeModel=new Model(model.getPath());Recognizer recognizer=new Recognizer(nativeModel,PcmWave.RATE);RandomAccessFile input=new RandomAccessFile(wave,"r")){
            PcmWave.Data data=PcmWave.inspect(input);input.seek(data.offset);long remaining=data.bytes;byte[] buffer=new byte[8000];
            while(remaining>0){if(cancelled.get())throw new InterruptedException("Transcription cancelled; audio retained");int count=input.read(buffer,0,(int)Math.min(buffer.length,remaining));if(count<0)throw new IOException("Audio was truncated");remaining-=count;if(recognizer.acceptWaveForm(buffer,count))append(transcript,recognizer.getResult());}
            append(transcript,recognizer.getFinalResult());return transcript.toString();
        }
    }
    private static void append(StringBuilder out,String json) throws JSONException {String text=new JSONObject(json).optString("text","").trim();if(!text.isEmpty()){if(out.length()>0)out.append(' ');out.append(text);}}
    private static synchronized File install(Context context,AtomicBoolean cancelled) throws Exception {
        JSONObject manifest;try(InputStream in=context.getAssets().open("voice/model.json")){manifest=new JSONObject(readSmall(in));}
        String hash=manifest.getString("sha256");File parent=new File(context.getFilesDir(),"offline-speech-v1"),model=new File(parent,"model-"+hash.substring(0,16));if(!parent.isDirectory() && !parent.mkdirs())throw new IOException("Cannot create private speech model storage");
        JSONObject expected=manifest.getJSONObject("files");
        if(model.isDirectory() && verified(model,expected,cancelled))return model;
        File temporary=new File(parent,"install-"+UUID.randomUUID());if(!temporary.mkdir())throw new IOException("Cannot prepare speech model");
        try{
            // Verify compressed bytes before extracting any entries; temp archive is private and removed.
            File archive=new File(parent,"archive-"+UUID.randomUUID()+".zip");
            try{
                MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=context.getAssets().open("voice/speech-model.zip");OutputStream out=new FileOutputStream(archive)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(cancelled.get())throw new InterruptedException("Cancelled");digest.update(buffer,0,n);out.write(buffer,0,n);}}
                if(archive.length()!=manifest.getLong("bytes") || !hex(digest.digest()).equals(hash))throw new IOException("Bundled speech archive checksum mismatch");
                Set<String> found=new HashSet<>();try(ZipFile zip=new ZipFile(archive)){Enumeration<? extends ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){
                    ZipEntry entry=entries.nextElement();if(entry.isDirectory())continue;if(cancelled.get())throw new InterruptedException("Cancelled");String name=entry.getName();
                    if(!expected.has(name) || !found.add(name))throw new IOException("Unexpected/duplicate model entry");String relative=name.substring(name.indexOf('/')+1);File target=new File(temporary,relative);if(!target.getCanonicalPath().startsWith(temporary.getCanonicalPath()+File.separator))throw new IOException("Unsafe speech model path");
                    File dir=target.getParentFile();if(!dir.isDirectory() && !dir.mkdirs())throw new IOException("Cannot unpack speech model");JSONObject info=expected.getJSONObject(name);long count=0;
                    try(InputStream in=zip.getInputStream(entry);FileOutputStream out=new FileOutputStream(target)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){count+=n;if(count>info.getLong("bytes"))throw new IOException("Speech model member too large");out.write(buffer,0,n);}out.flush();out.getFD().sync();}
                }}
                if(found.size()!=expected.length() || !verified(temporary,expected,cancelled))throw new IOException("Incomplete or damaged speech model");
            }finally{archive.delete();}
            if(model.exists())deleteOwned(model);if(!temporary.renameTo(model))throw new IOException("Cannot install verified speech model");return model;
        }finally{deleteOwned(temporary);}
    }
    private static boolean verified(File directory,JSONObject manifest,AtomicBoolean cancelled) throws Exception {
        Iterator<String> keys=manifest.keys();while(keys.hasNext()){if(cancelled.get())throw new InterruptedException("Cancelled");String name=keys.next(),relative=name.substring(name.indexOf('/')+1);File f=new File(directory,relative);JSONObject expected=manifest.getJSONObject(name);
            if(!f.isFile() || f.length()!=expected.getLong("bytes"))return false;try(InputStream in=new FileInputStream(f)){MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);if(!hex(digest.digest()).equals(expected.getString("sha256")))return false;}
        }return true;
    }
    private static String readSmall(InputStream in) throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>65536)throw new IOException("Oversized model manifest");out.write(buffer,0,n);}return out.toString("UTF-8");}
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private static void deleteOwned(File file){if(!file.exists())return;File[] children=file.listFiles();if(children!=null)for(File child:children)deleteOwned(child);file.delete();}
}
