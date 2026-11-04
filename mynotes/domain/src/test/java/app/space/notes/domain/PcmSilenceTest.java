package app.space.notes.domain;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PcmSilenceTest {
 private File wave(int bytes)throws Exception{File file=File.createTempFile("silence-",".wav");file.deleteOnExit();try(RandomAccessFile out=new RandomAccessFile(file,"rw")){out.setLength(PcmWave.HEADER+bytes);PcmWave.header(out,bytes);}return file;}
 @Test public void completeZeroPcmIsSilentAndPositionIsRestored()throws Exception{File file=wave(128000);try(RandomAccessFile in=new RandomAccessFile(file,"r")){in.seek(9);assertTrue(PcmSilence.isDigitalSilence(in,()->false));assertEquals(9,in.getFilePointer());}finally{file.delete();}}
 @Test public void singleQuietSampleAtEndIsNotDiscarded()throws Exception{File file=wave(128000);try(RandomAccessFile in=new RandomAccessFile(file,"rw")){in.seek(in.length()-2);in.write(1);assertFalse(PcmSilence.isDigitalSilence(in,()->false));}finally{file.delete();}}
 @Test public void negativeQuietSampleIsNotSilence()throws Exception{File file=wave(32);try(RandomAccessFile in=new RandomAccessFile(file,"rw")){in.seek(PcmWave.HEADER);in.write(255);in.write(255);assertFalse(PcmSilence.isDigitalSilence(in,()->false));}finally{file.delete();}}
 @Test(expected=InterruptedException.class) public void cancellationIsNeverReportedAsSilence()throws Exception{File file=wave(32);try(RandomAccessFile in=new RandomAccessFile(file,"r")){PcmSilence.isDigitalSilence(in,()->true);}finally{file.delete();}}
 @Test(expected=IOException.class) public void emptyAudioIsNotSuccessfulSilence()throws Exception{File file=wave(0);try(RandomAccessFile in=new RandomAccessFile(file,"r")){PcmSilence.isDigitalSilence(in,()->false);}finally{file.delete();}}
 @Test(expected=IOException.class) public void truncatedAudioIsNotSuccessfulSilence()throws Exception{File file=wave(32);try(RandomAccessFile in=new RandomAccessFile(file,"rw")){in.setLength(in.length()-2);PcmSilence.isDigitalSilence(in,()->false);}finally{file.delete();}}
}
