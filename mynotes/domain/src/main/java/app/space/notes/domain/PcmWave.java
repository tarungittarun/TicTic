package app.space.notes.domain;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Streaming RIFF PCM16 mono/16 kHz framing; recover recording length from durable bytes. */
public final class PcmWave {
    public static final int RATE=16000,HEADER=44;
    private PcmWave() { }
    public static void header(RandomAccessFile file,long bytes) throws IOException {
        if(bytes<0 || bytes>0xffffffffL-36 || (bytes&1)!=0)throw new IOException("Invalid PCM byte length");long position=file.getFilePointer();file.seek(0);
        file.writeBytes("RIFF");le32(file,bytes+36);file.writeBytes("WAVEfmt ");le32(file,16);le16(file,1);le16(file,1);le32(file,RATE);le32(file,RATE*2);le16(file,2);le16(file,16);file.writeBytes("data");le32(file,bytes);file.seek(Math.max(HEADER,position));
    }
    public static void repair(File file) throws IOException {try(RandomAccessFile out=new RandomAccessFile(file,"rw")){if(out.length()<HEADER)throw new IOException("Recording has no complete WAV header");long bytes=(out.length()-HEADER)&~1L;out.setLength(bytes+HEADER);header(out,bytes);out.getFD().sync();}}
    public static final class Data {
        public final long offset,bytes;
        Data(long offset,long bytes){this.offset=offset;this.bytes=bytes;}
    }
    public static Data inspect(RandomAccessFile file) throws IOException {
        file.seek(0);if(!word(file).equals("RIFF")){throw new IOException("Choose a PCM WAV recording");}long riff=unsigned32(file);if(!word(file).equals("WAVE") || riff+8>file.length())throw new IOException("Truncated WAV");
        boolean format=false;long end=riff+8;
        while(file.getFilePointer()+8<=end){String chunk=word(file);long count=unsigned32(file),start=file.getFilePointer();if(count>end-start)throw new IOException("Truncated WAV chunk");
            if(chunk.equals("fmt ")){if(count<16 || unsigned16(file)!=1 || unsigned16(file)!=1 || unsigned32(file)!=RATE || unsigned32(file)!=RATE*2 || unsigned16(file)!=2 || unsigned16(file)!=16)throw new IOException("WAV must be 16 kHz mono PCM16");format=true;}
            if(chunk.equals("data")){if(!format || (count&1)!=0)throw new IOException("Invalid WAV data");return new Data(start,count);}
            long next=start+count+(count&1);if(next>end)throw new IOException("Truncated WAV padding");file.seek(next);
        }throw new IOException("WAV has no audio data");
    }
    private static String word(RandomAccessFile in) throws IOException {byte[] bytes=new byte[4];in.readFully(bytes);return new String(bytes,StandardCharsets.US_ASCII);}
    private static int unsigned16(RandomAccessFile in) throws IOException {return in.readUnsignedByte() | (in.readUnsignedByte()<<8);}
    private static long unsigned32(RandomAccessFile in) throws IOException {return Integer.toUnsignedLong(in.readUnsignedByte() | in.readUnsignedByte()<<8 | in.readUnsignedByte()<<16 | in.readUnsignedByte()<<24);}
    private static void le16(RandomAccessFile out,int n) throws IOException {out.write(n&255);out.write(n>>>8&255);}
    private static void le32(RandomAccessFile out,long n) throws IOException {for(int i=0;i<4;i++)out.write((int)(n>>>(8*i))&255);}
}
