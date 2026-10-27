package app.space.notes;
import android.content.*;
import android.graphics.*;
import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import static org.junit.Assert.*;
/** Short in the normal gate; a separate read-only workflow requests a real 90-minute native/disk soak. */
@RunWith(AndroidJUnit4.class)
public class NativeSoakTest {
 @Test public void repeatedNativeInferenceAndDurableDocumentCyclesStayBounded() throws Exception {
  Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();File root=new File(app.getCacheDir(),"soak-"+UUID.randomUUID());assertTrue(root.mkdir());
  Context isolated=new ContextWrapper(app){@Override public Context getApplicationContext(){return this;}@Override public File getFilesDir(){return root;}@Override public SharedPreferences getSharedPreferences(String name,int mode){return app.getSharedPreferences(root.getName()+"-"+name,mode);}};
  File wave=new File(root,"fixture.wav");try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("voice/test.wav");OutputStream out=new FileOutputStream(wave)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  String original;try(InputStream in=new FileInputStream(wave)){original=OfflineOcr.digest(in);}
  int minutes=Integer.parseInt(InstrumentationRegistry.getArguments().getString("soakMinutes","0"));assertTrue(minutes>=0 && minutes<=90);long started=SystemClock.elapsedRealtime(),lastReport=0;int iterations=0;long warmPss=0,peakPss=0;JSONArray samples=new JSONArray();List<Note> notes=new ArrayList<>();
  try {
   do {
    String spoken=OfflineSpeech.transcribe(app,wave,new AtomicBoolean());assertTrue(spoken,spoken.contains("one") && spoken.contains("zero"));
    Bitmap bitmap=Bitmap.createBitmap(1200,400,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(72);canvas.drawText("MEETING NOTES",70,140,paint);canvas.drawText("Monday at 10 AM",70,250,paint);
    try{String recognized=OfflineOcr.recognizeBitmap(app,bitmap,"eng",new OfflineOcr.Cancellation()).text;assertTrue(recognized,recognized.toUpperCase(Locale.ROOT).contains("MEETING"));}finally{bitmap.recycle();}
    String id=UUID.randomUUID().toString();VoiceFiles files=new VoiceFiles(isolated);try(RandomAccessFile out=new RandomAccessFile(files.pending(id),"rw")){PcmWave.header(out,0);out.write(new byte[3201]);}assertTrue(files.list().contains(id));try(RandomAccessFile in=new RandomAccessFile(files.audio(id),"r")){assertEquals(3200,PcmWave.inspect(in).bytes);}files.result(id,spoken,"DONE");
    Note note=VoiceTranscripts.apply(VoiceTranscripts.placeholder(id,System.currentTimeMillis()),id,files.result(id).getString("text"),System.currentTimeMillis());notes.add(note);if(notes.size()>32)notes.remove(0);NotesRepository repository=new NotesRepository(isolated);repository.save(notes);List<Note> read=repository.localSnapshot();assertEquals(notes.size(),read.size());for(int i=0;i<read.size();i++)assertTrue(NoteMerge.same(notes.get(i),read.get(i)));assertEquals(notes.size(),BackupCodec.decode(BackupCodec.encode(notes)).size());
    // Test-fixture cleanup only. Product recordings are never automatically removed.
    assertTrue(files.audio(id).delete());assertTrue(files.transcript(id).delete());iterations++;long pss=Debug.getPss();if(iterations==3)warmPss=pss;if(iterations>=3){peakPss=Math.max(peakPss,pss);assertTrue("PSS grew beyond a 128 MiB post-warmup allowance: "+pss+" vs "+warmPss,pss<=warmPss+128*1024);}
    long elapsed=SystemClock.elapsedRealtime()-started;if(elapsed-lastReport>=60000 || minutes==0){lastReport=elapsed;JSONObject sample=new JSONObject().put("elapsed_ms",elapsed).put("iterations",iterations).put("pss_kib",pss).put("native_allocated_bytes",Debug.getNativeHeapAllocatedSize());samples.put(sample);android.util.Log.i("MyNotesSoak",sample.toString());Bundle progress=new Bundle();progress.putString("stream","Native soak "+sample+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(2,progress);}
   }while(iterations<4 || (minutes>0 && SystemClock.elapsedRealtime()-started<minutes*60000L));
   try(InputStream in=new FileInputStream(wave)){assertEquals(original,OfflineOcr.digest(in));}
   JSONObject report=new JSONObject().put("schema",1).put("iterations",iterations).put("elapsed_ms",SystemClock.elapsedRealtime()-started).put("requested_minutes",minutes).put("warm_pss_kib",warmPss).put("peak_pss_kib",peakPss).put("source_audio_sha256",original).put("samples",samples).put("scope","debug runtime, real native English OCR/ASR, PCM recovery, private atomic repository and portable backup; not physical battery/thermal or language-accuracy qualification");
   try(OutputStream out=new FileOutputStream(new File(app.getFilesDir(),"soak-report.json"))){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  }finally{LocalReminders.reconcile(isolated,Collections.emptyList());remove(root);}
 }
 private void remove(File f){File[] children=f.listFiles();if(children!=null)for(File c:children)remove(c);f.delete();}
}
