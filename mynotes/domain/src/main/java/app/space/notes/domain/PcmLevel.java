package app.space.notes.domain;
/** Read-only signed little-endian PCM16 peak meter. Never normalizes/clips stored audio. */
public final class PcmLevel {
 private PcmLevel(){}
 public static int peak(byte[] data,int count){if(data==null || count<0 || count>data.length)throw new IllegalArgumentException("Invalid PCM meter buffer");int peak=0;for(int i=0;i+1<count;i+=2){int sample=(short)((data[i]&255)|(data[i+1]<<8));peak=Math.max(peak,Math.abs(sample));}return peak;}
}
