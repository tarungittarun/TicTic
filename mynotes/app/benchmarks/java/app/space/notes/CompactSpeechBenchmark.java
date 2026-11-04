package app.space.notes;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Opt-in PUBLIC acoustic/CPU study. Never compiled into production or used as an output oracle. */
@RunWith(AndroidJUnit4.class)
public class CompactSpeechBenchmark {
 private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 private byte[] asset(String name)throws Exception{try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);return out.toByteArray();}}
 private String digest(byte[] data)throws Exception{return OfflineOcr.digest(new ByteArrayInputStream(data));}
 private String normalized(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9']+"," ").trim();}
 private void persist(String variant,int shard,JSONArray rows,JSONObject warmup)throws Exception{
  JSONObject report=new JSONObject().put("scope","Public CMU KSP/SLT B0065-B0100 plus silence; two speakers, KSP used in earlier adaptation. Not unseen-Indian-speaker/phone acceptance. Exact F16 weights, no training or default adoption. Balanced ordered phases; times are observational emulator end-to-end wall time, not causal phone speed or peak RAM.").put("variant",variant).put("shard",shard).put("expected",146).put("training_steps",0).put("adopted",false).put("warmup",warmup).put("cases",rows);
  File directory=new File(context().getFilesDir(),"speech-kernel-study");assertTrue(directory.isDirectory()||directory.mkdirs());File temporary=new File(directory,"partial.tmp");Files.write(temporary.toPath(),report.toString().getBytes(StandardCharsets.UTF_8));Files.move(temporary.toPath(),new File(directory,variant+"-"+shard+".json").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
 }
 private JSONObject recognize(String id,String reference,byte[] input,double seconds,String kind)throws Exception{
  File wave=File.createTempFile("public-kernel-",".wav",context().getCacheDir());Files.write(wave.toPath(),input);JSONObject row=new JSONObject().put("id",id).put("reference",reference).put("input_sha256",digest(input)).put("input_bytes",input.length).put("seconds",seconds).put("kind",kind);long began=android.os.SystemClock.elapsedRealtime();
  try{OfflineSpeech.Result result=TrainedWhisper.transcribe(context(),wave,new AtomicBoolean());RecognitionMetrics.Score score=RecognitionMetrics.words(normalized(reference),normalized(result.raw));row.put("status","OK").put("recognized",result.raw).put("normalized_wer",score.rate()).put("word_edits",score.edits).put("reference_words",score.referenceUnits);assertArrayEquals(input,Files.readAllBytes(wave.toPath()));row.put("audio_unchanged",true);}
  catch(Exception failure){row.put("status","ERROR").put("error",failure.toString()).put("recognized","");}
  finally{row.put("elapsed_ms",android.os.SystemClock.elapsedRealtime()-began);wave.delete();}return row;
 }
 @Test public void fixedPublicClipsRetainEveryHypothesisAndTime()throws Exception{
  int shard=Integer.parseInt(InstrumentationRegistry.getArguments().getString("speechShard","0"));assertTrue(shard==0||shard==1);String variant=InstrumentationRegistry.getArguments().getString("speechKernel","standard");assertTrue(variant.equals("standard")||variant.equals("flash"));
  JSONObject manifest=new JSONObject(new String(asset("module-corpus/manifest.json"),StandardCharsets.UTF_8));JSONArray records=manifest.getJSONArray("records"),rows=new JSONArray();List<JSONObject> selected=new ArrayList<>();
  for(int i=0;i<records.length();i++){JSONObject r=records.getJSONObject(i);String id=r.getString("id");int prompt=Integer.parseInt(id.substring(id.lastIndexOf('b')+1));if(prompt>=65&&prompt<=100)selected.add(r);}assertEquals(72,selected.size());
  JSONObject first=selected.get(shard);byte[] warm=asset("module-corpus/"+first.getString("file"));assertEquals(first.getString("sha256"),digest(warm));JSONObject warmup=recognize("warmup-"+first.getString("id"),first.getString("reference"),warm,first.getDouble("seconds"),"warmup-excluded-from-comparison");persist(variant,shard,rows,warmup);
  for(int i=shard;i<selected.size();i+=2){JSONObject record=selected.get(i);byte[] original=asset("module-corpus/"+record.getString("file"));assertEquals(record.getString("sha256"),digest(original));long offset,length;
   File probe=File.createTempFile("public-pcm-",".wav",context().getCacheDir());try{Files.write(probe.toPath(),original);try(RandomAccessFile file=new RandomAccessFile(probe,"r")){PcmWave.Data data=PcmWave.inspect(file);offset=data.offset;length=data.bytes;}}finally{probe.delete();}
   double energy=0;for(long p=offset;p<offset+length;p+=2){int at=(int)p;double value=(short)((original[at]&255)|(original[at+1]<<8));energy+=value*value;}double rms=Math.sqrt(energy/(length/2));
   for(int transform=0;transform<4;transform++){byte[] input=original.clone();Random random=new Random(1729);int clipped=0;
    for(long p=offset;p<offset+length;p+=2){int at=(int)p,value=(short)((input[at]&255)|(input[at+1]<<8));double adjusted=transform==1?value*.25:transform==2?value+(random.nextDouble()*2-1)*Math.sqrt(3)*rms/Math.sqrt(10):transform==3?value*3:value;int result=(int)Math.round(adjusted);if(result>32767||result< -32768)clipped++;result=Math.max(-32768,Math.min(32767,result));input[at]=(byte)result;input[at+1]=(byte)(result>>8);}
    JSONObject row=recognize(record.getString("id")+"-v"+transform,record.getString("reference"),input,record.getDouble("seconds"),new String[]{"clean","gain-quarter","noise-10dB","gain-three-clipped"}[transform]);row.put("speaker",record.getString("speaker")).put("clipped_samples",clipped);rows.put(row);persist(variant,shard,rows,warmup);
   }
  }
  for(int seconds:new int[]{shard==0?1:15,shard==0?5:30}){File probe=File.createTempFile("public-silence-",".wav",context().getCacheDir());byte[] input;try{try(RandomAccessFile file=new RandomAccessFile(probe,"rw")){PcmWave.header(file,seconds*32000);file.write(new byte[seconds*32000]);}input=Files.readAllBytes(probe.toPath());}finally{probe.delete();}rows.put(recognize("silence-"+seconds,"",input,seconds,"silence-control"));persist(variant,shard,rows,warmup);}
  assertEquals(146,rows.length());assertEquals("Warmup failures retained", "OK",warmup.getString("status"));for(int i=0;i<rows.length();i++)assertEquals(rows.getJSONObject(i).toString(),"OK",rows.getJSONObject(i).getString("status"));
 }
}
