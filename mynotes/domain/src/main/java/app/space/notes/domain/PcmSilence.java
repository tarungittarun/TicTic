package app.space.notes.domain;
import java.io.*;
import java.util.function.BooleanSupplier;

/** Exact digital zero only. Never classifies quiet speech/noise by a threshold. */
public final class PcmSilence {
 private PcmSilence(){}
 public static boolean isDigitalSilence(RandomAccessFile input,BooleanSupplier cancelled)throws IOException,InterruptedException {
  long position=input.getFilePointer();
  try {
   if(cancelled.getAsBoolean())throw new InterruptedException("Cancelled; original audio retained");
   PcmWave.Data data=PcmWave.inspect(input);
   if(data.bytes==0)throw new IOException("Recording contains no audio samples; original audio retained");
   input.seek(data.offset);byte[] buffer=new byte[65536];long remaining=data.bytes;
   while(remaining>0){
    if(cancelled.getAsBoolean())throw new InterruptedException("Cancelled; original audio retained");
    int size=input.read(buffer,0,(int)Math.min(buffer.length,remaining));
    if(size<0)throw new EOFException("Audio was truncated; original audio retained");
    remaining-=size;for(int i=0;i<size;i++)if(buffer[i]!=0)return false;
   }
   if(cancelled.getAsBoolean())throw new InterruptedException("Cancelled; original audio retained");
   return true;
  } finally {input.seek(position);}
 }
}
