package app.space.notes;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import app.space.notes.domain.*;
import app.space.notes.runtime.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Explicit separate benchmark source set; never part of the production APK. */
@RunWith(AndroidJUnit4.class)
public class ModuleCorpusBenchmark {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private JSONObject asset(String name)throws Exception{try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("module-corpus/"+name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);return new JSONObject(out.toString("UTF-8"));}}
    private String digest(byte[] bytes)throws Exception{return OfflineOcr.digest(new ByteArrayInputStream(bytes));}
    private int shard(){return Integer.parseInt(InstrumentationRegistry.getArguments().getString("corpusShard","0"));}
    private void persist(String module,JSONArray rows,int expected)throws Exception{
        JSONObject report=new JSONObject().put("timing_scope","wall-clock recognition plus scoring, and for speech transcript codec/dedup/source-preservation checks; excludes fixture synthesis; first invocation includes cold initialization").put("schema",1).put("module",module).put("expected",expected).put("cases",rows).put("model_changed",false).put("training_steps",0).put("source_scope","Public synthetic/CMU evaluation only; emulator CPU times, not physical phone latency");
        File dir=new File(context().getFilesDir(),"module-benchmark");assertTrue(dir.isDirectory()||dir.mkdirs());
        File temporary=new File(dir,module+".tmp");try(OutputStream out=new FileOutputStream(temporary)){out.write(report.toString().getBytes(StandardCharsets.UTF_8));}Files.move(temporary.toPath(),new File(dir,module+".json").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Bundle status=new Bundle();status.putString("stream","module-corpus "+module+" "+rows.length()+"/"+expected+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(2,status);
    }
    private String normalized(String text){return java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+"," ");}
    private String englishWords(String text){return normalized(text).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9']+"," ").trim();}
    private void scores(JSONObject row,String reference,String actual)throws Exception{
        RecognitionMetrics.Score cer=RecognitionMetrics.characters(normalized(reference),normalized(actual));RecognitionMetrics.Score wer=RecognitionMetrics.words(normalized(reference),normalized(actual));
        row.put("reference",reference).put("recognized",actual).put("cer",cer.rate()).put("character_edits",cer.edits).put("reference_characters",cer.referenceUnits).put("strict_wer",wer.rate()).put("word_edits",wer.edits).put("reference_words",wer.referenceUnits);
    }
    // PUBLIC fixture diagnostics only. Never added to ordinary OCR diagnostics or
    // production Result, and never used as an oracle to select application output.
    private void segmentationProbe(JSONObject row,byte[] png)throws Exception{
        if(!"OK".equals(row.optString("status")) || row.getBoolean("accuracy_target_met"))return;
        long began=SystemClock.elapsedRealtime();Bitmap source=BitmapFactory.decodeByteArray(png,0,png.length),prepared=null;
        com.googlecode.tesseract.android.TessBaseAPI engine=new com.googlecode.tesseract.android.TessBaseAPI();
        try{prepared=OfflineOcr.prepareBitmap(source);if(!engine.init(new File(context().getFilesDir(),"offline-ocr-v1").getAbsolutePath(),"eng+hin+kan"))throw new IOException("Diagnostic engine init");engine.setVariable("preserve_interword_spaces","1");JSONArray alternatives=new JSONArray();
            for(int mode:new int[]{3,11,6}){engine.setPageSegMode(mode);engine.setImage(prepared);String text=engine.getUTF8Text();JSONObject candidate=new JSONObject().put("page_segmentation_mode",mode).put("confidence",engine.meanConfidence());scores(candidate,row.getString("reference"),text==null?"":text);alternatives.put(candidate);}row.put("public_segmentation_probe",alternatives);
        }catch(Exception failure){row.put("probe_error",failure.toString());}
        finally{engine.recycle();if(prepared!=null)prepared.recycle();if(source!=null)source.recycle();row.put("probe_elapsed_ms",SystemClock.elapsedRealtime()-began);}
    }
    /** Separately invoked frozen references; never an input to application selection. */
    @Test public void heldOutOcrReferencesMeasureIndependentRecognition()throws Exception{
        JSONObject corpus=asset("ocr-heldout-v1.json");JSONArray references=corpus.getJSONArray("records"),conditions=corpus.getJSONArray("conditions"),rows=new JSONArray();int errors=0;
        assertEquals(32,references.length());assertEquals(4,conditions.length());
        for(int text=0;text<references.length();text++)for(int variant=0;variant<conditions.length();variant++){
            JSONObject reference=references.getJSONObject(text),condition=conditions.getJSONObject(variant);JSONArray lines=reference.getJSONArray("lines");
            Bitmap image=Bitmap.createBitmap(1600,850,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);boolean dark=condition.getBoolean("dark");canvas.drawColor(dark?Color.BLACK:Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(dark?Color.WHITE:Color.BLACK);paint.setTextSize(condition.getInt("font_px"));paint.setTypeface(Typeface.create(condition.getString("font"),Typeface.NORMAL));canvas.rotate((float)condition.getDouble("rotation"),800,425);StringBuilder expected=new StringBuilder();
            for(int line=0;line<lines.length();line++){String value=lines.getString(line);canvas.drawText(value,70,170+line*150,paint);if(line>0)expected.append(' ');expected.append(value);}
            ByteArrayOutputStream png=new ByteArrayOutputStream();assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,png));JSONObject row=new JSONObject().put("id",reference.getString("id")+"-"+variant).put("kind",reference.getString("kind")).put("condition",condition).put("width",1600).put("height",850).put("pixels",1600*850).put("input_bytes",png.size()).put("input_sha256",digest(png.toByteArray())).put("language_request","auto");
            long began=SystemClock.elapsedRealtime(),cpu=android.os.Debug.threadCpuTimeNanos();
            try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(context(),image,"auto",new OfflineOcr.Cancellation());row.put("status","OK").put("confidence",result.confidence).put("selected_language",result.language);scores(row,expected.toString(),result.text);row.put("accuracy_target_met",row.getDouble("cer")<=.2);}
            catch(Exception failure){errors++;row.put("status","ERROR").put("error",failure.toString()).put("reference",expected.toString()).put("recognized","");}
            finally{row.put("elapsed_ms",SystemClock.elapsedRealtime()-began).put("calling_thread_cpu_ms",(android.os.Debug.threadCpuTimeNanos()-cpu)/1000000.0);image.recycle();}
            rows.put(row);persist("ocr-holdout",rows,128);
        }
        assertEquals(128,rows.length());assertEquals("Operational errors retained; accuracy separately reported",0,errors);
    }
    @Test public void variedImageCorpusMeasuresGroundTruthAndElapsedTime()throws Exception{
        String[][] lines={{"MEETING NOTES","Monday at 10 AM"},{"RECEIPT","Rice 250 Sugar 50","TOTAL 300"},{"TOTAL 25 + 5 = 30","Price $50 INR 500"},{"Contact support@example.com","https://example.com/page"},{"भारत नमस्ते","आज बैठक है"},{"ಕನ್ನಡ ಭಾಷೆ","ಭಾರತ ನಮಸ್ಕಾರ"},{"Meeting ಭಾರತ ಕನ್ನಡ","Total 25 + 5 = 30"},{}};
        String[] families={"sans-serif","serif","monospace","sans-serif-condensed"};int[] sizes={28,48,72};JSONArray rows=new JSONArray();int index=0,errors=0;
        for(int text=0;text<lines.length;text++)for(String family:families)for(int size:sizes)for(int dark=0;dark<2;dark++)for(int tilt=0;tilt<2;tilt++){
            int width=tilt==0?1600:2400,height=tilt==0?700:1000;Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(dark==1?Color.BLACK:Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(dark==1?Color.WHITE:Color.BLACK);paint.setTextSize(size);paint.setTypeface(Typeface.create(family,(text%2==0)?Typeface.BOLD:Typeface.ITALIC));canvas.rotate(tilt==1?2:0,width/2f,height/2f);for(int line=0;line<lines[text].length;line++)canvas.drawText(lines[text][line],70,150+line*130,paint);
            String reference=String.join(" ",lines[text]);JSONObject row=new JSONObject().put("id",String.format(Locale.ROOT,"ocr-%04d",index++)).put("kind",text==7?"blank-control":text==4?"Hindi":text==5?"Kannada":text==6?"mixed-script":text==2?"symbols":text==3?"URL-email":"printed-English").put("font",family).put("font_px",size).put("input_dark",dark==1).put("rotation_degrees",tilt*2).put("width",width).put("height",height).put("pixels",width*height).put("language_request","auto");
            ByteArrayOutputStream png=new ByteArrayOutputStream();assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,png));File specimens=new File(context().getFilesDir(),"module-benchmark/ocr-inputs");assertTrue(specimens.isDirectory()||specimens.mkdirs());Files.write(new File(specimens,row.getString("id")+".png").toPath(),png.toByteArray());row.put("input_bytes",png.size()).put("input_sha256",digest(png.toByteArray()));long began=SystemClock.elapsedRealtime();long cpu=android.os.Debug.threadCpuTimeNanos();
            try{OfflineOcr.Result result=OfflineOcr.recognizeBitmap(context(),image,"auto",new OfflineOcr.Cancellation());row.put("status","OK").put("confidence",result.confidence).put("selected_language",result.language);scores(row,reference,result.text);row.put("accuracy_target_met",row.getDouble("cer")<=.2);}
            catch(Exception failure){errors++;row.put("status","ERROR").put("error",failure.getClass().getSimpleName()+": "+failure.getMessage()).put("reference",reference).put("recognized","");}
            finally{row.put("elapsed_ms",SystemClock.elapsedRealtime()-began).put("calling_thread_cpu_ms",(android.os.Debug.threadCpuTimeNanos()-cpu)/1000000.0);image.recycle();}
            // Probe runs after the timed production call; its extra passes have a separate timer.
            segmentationProbe(row,png.toByteArray());row.put("heap_used_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()).put("native_heap_bytes",android.os.Debug.getNativeHeapAllocatedSize());rows.put(row);persist("ocr",rows,384);
        }
        assertEquals(384,rows.length());assertEquals("All operational errors are retained; accuracy is separately scored",0,errors);
    }
    @Test public void publicVoiceCorpusMeasuresTranscriptsDurationsAndPreservation()throws Exception{
        JSONObject manifest=asset("manifest.json");JSONArray records=manifest.getJSONArray("records"),rows=new JSONArray();int shard=shard(),errors=0;assertTrue(shard==0||shard==1);int expected=402;
        for(int i=shard;i<records.length();i+=2){JSONObject entry=records.getJSONObject(i);byte[] original;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("module-corpus/"+entry.getString("file"))){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);original=out.toByteArray();}assertEquals(entry.getString("sha256"),digest(original));
            for(int variant=0;variant<4;variant++){byte[] input=original.clone();long offset,length;File wave=File.createTempFile("module-eval-",".wav",context().getCacheDir());Files.write(wave.toPath(),input);try(RandomAccessFile file=new RandomAccessFile(wave,"r")){PcmWave.Data data=PcmWave.inspect(file);offset=data.offset;length=data.bytes;}
                double energy=0;for(long p=offset;p<offset+length;p+=2){int at=(int)p;double sample=(short)((input[at]&255)|(input[at+1]<<8));energy+=sample*sample;}double rms=Math.sqrt(energy/(length/2));Random random=new Random(1729);int clipped=0;
                for(long p=offset;p<offset+length;p+=2){int at=(int)p,value=(short)((input[at]&255)|(input[at+1]<<8));double adjusted=variant==1?value*.25:variant==2?value+(random.nextDouble()*2-1)*Math.sqrt(3)*rms/Math.sqrt(10):variant==3?value*3.0:value;int result=(int)Math.round(adjusted);if(result>32767||result< -32768)clipped++;result=Math.max(-32768,Math.min(32767,result));input[at]=(byte)result;input[at+1]=(byte)(result>>8);}
                JSONObject row=new JSONObject().put("id",entry.getString("id")+"-v"+variant).put("speaker",entry.getString("speaker")).put("kind",manifest.getJSONArray("variants").getString(variant)).put("duration_seconds",entry.getDouble("seconds")).put("input_bytes",input.length).put("source_sha256",entry.getString("sha256")).put("input_sha256",digest(input)).put("sample_rate",16000).put("channels",1).put("clipped_samples",clipped).put("recognizer","bundled-default-Vosk");Files.write(wave.toPath(),input);long began=SystemClock.elapsedRealtime();
                try{String result=OfflineSpeech.transcribe(context(),wave,new AtomicBoolean());row.put("status","OK");scores(row,entry.getString("reference"),result);RecognitionMetrics.Score fair=RecognitionMetrics.words(englishWords(entry.getString("reference")),englishWords(result));row.put("normalized_wer",fair.rate()).put("normalized_word_edits",fair.edits).put("normalized_reference_words",fair.referenceUnits).put("accuracy_target_met",fair.rate()<=.35);
                    String id=UUID.nameUUIDFromBytes(row.getString("id").getBytes(StandardCharsets.UTF_8)).toString();Note note=VoiceTranscripts.placeholder(id,1),saved=VoiceTranscripts.apply(note,id,result,2);Note restored=BackupCodec.decode(BackupCodec.encode(Collections.singletonList(saved))).get(0);if(!result.trim().isEmpty()){assertTrue(restored.voiceSources.contains(id));assertTrue(restored.body.contains(result));assertSame(saved,VoiceTranscripts.apply(saved,id,result,3));}row.put("transcript_roundtrip_and_dedup",true);assertEquals(digest(input),digest(Files.readAllBytes(wave.toPath())));row.put("audio_unchanged",true);}
                catch(Exception failure){errors++;row.put("status","ERROR").put("error",failure.getClass().getSimpleName()+": "+failure.getMessage()).put("reference",entry.getString("reference")).put("recognized","");}
                finally{row.put("elapsed_ms",SystemClock.elapsedRealtime()-began);wave.delete();}row.put("real_time_factor",row.getLong("elapsed_ms")/1000.0/entry.getDouble("seconds"));row.put("heap_used_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()).put("native_heap_bytes",android.os.Debug.getNativeHeapAllocatedSize());rows.put(row);persist("voice-"+shard,rows,expected);
            }
        }
        for(int seconds:new int[]{shard==0?1:15,shard==0?5:30}){File wave=File.createTempFile("module-silence-",".wav",context().getCacheDir());try(RandomAccessFile file=new RandomAccessFile(wave,"rw")){PcmWave.header(file,seconds*32000);file.write(new byte[seconds*32000]);}long began=SystemClock.elapsedRealtime();JSONObject row=new JSONObject().put("id","silence-"+seconds).put("kind","silence-control").put("duration_seconds",seconds).put("input_bytes",wave.length()).put("recognizer","bundled-default-Vosk");try{String result=OfflineSpeech.transcribe(context(),wave,new AtomicBoolean());scores(row,"",result);row.put("status","OK").put("normalized_wer",RecognitionMetrics.words("",englishWords(result)).rate()).put("accuracy_target_met",result.trim().isEmpty());}catch(Exception failure){errors++;row.put("status","ERROR").put("error",failure.toString()).put("reference","").put("recognized","");}finally{row.put("elapsed_ms",SystemClock.elapsedRealtime()-began);wave.delete();}row.put("real_time_factor",row.getLong("elapsed_ms")/1000.0/seconds);row.put("heap_used_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()).put("native_heap_bytes",android.os.Debug.getNativeHeapAllocatedSize());rows.put(row);persist("voice-"+shard,rows,expected);}
        assertEquals(expected,rows.length());assertEquals("Operational errors retained, not counted as accuracy passes",0,errors);
    }
}
