package app.space.notes.domain;
import org.junit.Test;
import static org.junit.Assert.*;
public class PcmLevelTest {
 @Test public void silenceIsNotInventedSpeech(){assertEquals(0,PcmLevel.peak(new byte[3200],3200));}
 @Test public void signedFullScaleAndPartialBuffersAreMeasuredWithoutMutation(){byte[] samples={0,(byte)0x80,(byte)0xff,0x7f,1};byte[] original=samples.clone();assertEquals(32768,PcmLevel.peak(samples,5));assertEquals(0,PcmLevel.peak(samples,1));assertArrayEquals(original,samples);}
 @Test(expected=IllegalArgumentException.class) public void oversizedBufferCountIsRejected(){PcmLevel.peak(new byte[2],3);}
 @Test public void quietPositiveAndNegativeVoiceHaveSameMagnitude(){assertEquals(1000,PcmLevel.peak(new byte[]{(byte)0xe8,3},2));assertEquals(1000,PcmLevel.peak(new byte[]{0x18,(byte)0xfc},2));}
}
