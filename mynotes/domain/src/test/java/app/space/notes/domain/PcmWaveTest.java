package app.space.notes.domain;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PcmWaveTest {
    @Test public void writeAndInspectStreamingWave() throws Exception {File f=File.createTempFile("pcm-",".wav");try(RandomAccessFile out=new RandomAccessFile(f,"rw")){PcmWave.header(out,0);out.write(new byte[3200]);PcmWave.header(out,3200);PcmWave.Data data=PcmWave.inspect(out);assertEquals(44,data.offset);assertEquals(3200,data.bytes);}finally{f.delete();}}
    @Test public void interruptedHeaderRecoveredFromActualBytes() throws Exception {File f=File.createTempFile("pcm-",".part");try{try(RandomAccessFile out=new RandomAccessFile(f,"rw")){PcmWave.header(out,0);out.write(new byte[3201]);}PcmWave.repair(f);try(RandomAccessFile in=new RandomAccessFile(f,"r")){assertEquals(3200,PcmWave.inspect(in).bytes);assertEquals(3244,in.length());}}finally{f.delete();}}
    @Test(expected=IOException.class) public void malformedWaveRejected() throws Exception {File f=File.createTempFile("pcm-",".wav");try(RandomAccessFile in=new RandomAccessFile(f,"rw")){in.writeBytes("not a wave");PcmWave.inspect(in);}finally{f.delete();}}
    @Test(expected=IOException.class) public void unsupportedRateRefused() throws Exception {File f=File.createTempFile("pcm-",".wav");try(RandomAccessFile out=new RandomAccessFile(f,"rw")){PcmWave.header(out,0);out.seek(24);out.write(new byte[]{0,0,0,0});PcmWave.inspect(out);}finally{f.delete();}}
    @Test(expected=IOException.class) public void truncatedAudioRefused() throws Exception {File f=File.createTempFile("pcm-",".wav");try(RandomAccessFile out=new RandomAccessFile(f,"rw")){PcmWave.header(out,1000);PcmWave.inspect(out);}finally{f.delete();}}
    @Test public void repairDoesNotFabricateAudioForShortHeader() throws Exception {File f=File.createTempFile("pcm-",".part");try{try(RandomAccessFile out=new RandomAccessFile(f,"rw")){out.write(new byte[12]);}boolean refused=false;try{PcmWave.repair(f);}catch(IOException expected){refused=true;}assertTrue(refused);assertEquals(12,f.length());}finally{f.delete();}}
}
