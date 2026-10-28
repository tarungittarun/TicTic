package app.space.notes.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.zip.*;

/** Streaming archive transport. Read only an immutable, app-owned staged file; sinks must stage, never mutate live data. */
public final class ArchiveZip {
    private ArchiveZip() { }
    public static final long MAX_CONTAINER_BYTES=ArchiveManifest.MAX_TOTAL_BYTES+16L*1024*1024;
    private static final int BUFFER_BYTES=64*1024, MAX_CENTRAL_BYTES=8*1024*1024, MAX_NAME_BYTES=80, MAX_EXTRA_BYTES=128;
    public interface Source { InputStream open(String canonicalPath)throws IOException; }
    public interface Sink { OutputStream create(String canonicalPath,long bytes)throws IOException; }
    private static final class Expected {
        final long bytes;final String hash;
        Expected(long bytes,String hash){this.bytes=bytes;this.hash=hash;}
    }
    private static Map<String,Expected> entries(ArchiveManifest manifest){
        Map<String,Expected> entries=new LinkedHashMap<>();entries.put(ArchiveManifest.LIBRARY_PATH,new Expected(manifest.libraryBytes,manifest.librarySha256));
        for(ArchiveManifest.Recording recording:manifest.recordings){entries.put(ArchiveManifest.audioPath(recording.id),new Expected(recording.audioBytes,recording.audioSha256));if(recording.metadataBytes>0)entries.put(ArchiveManifest.metadataPath(recording.id),new Expected(recording.metadataBytes,recording.metadataSha256));}return entries;
    }
    private static void check(BooleanSupplier cancelled)throws InterruptedIOException{if(Thread.currentThread().isInterrupted() || (cancelled!=null && cancelled.getAsBoolean()))throw new InterruptedIOException("Archive operation cancelled; source retained");}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    private static String hex(byte[] bytes){StringBuilder value=new StringBuilder(bytes.length*2);for(byte b:bytes)value.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));return value.toString();}
    /** Owns and closes output and every opened source stream. A failed/partial destination is not a valid backup. */
    public static void write(OutputStream output,ArchiveManifest manifest,Source source,BooleanSupplier cancelled,LongConsumer progress)throws IOException{
        if(output==null || manifest==null || source==null)throw new IOException("Missing archive source or destination");byte[] metadata=manifest.encode();long[] completed={0};
        try(ZipOutputStream zip=new ZipOutputStream(output,StandardCharsets.UTF_8)){
            check(cancelled);ZipEntry first=new ZipEntry(ArchiveManifest.MANIFEST_PATH);first.setTime(315619200000L);zip.putNextEntry(first);zip.write(metadata);zip.closeEntry();completed[0]=metadata.length;if(progress!=null)progress.accept(completed[0]);
            for(Map.Entry<String,Expected> item:entries(manifest).entrySet()){
                check(cancelled);ZipEntry entry=new ZipEntry(item.getKey());entry.setTime(315619200000L);zip.putNextEntry(entry);
                try(InputStream in=source.open(item.getKey())){if(in==null)throw new IOException("Archive source unavailable");copy(in,zip,item.getValue(),-1,cancelled,progress,completed);}
                zip.closeEntry();
            }
            zip.finish();
        }
    }
    /** Validates central/local headers before ZipFile allocation, then actual byte counts, CRC and SHA while staging. */
    public static ArchiveManifest read(File immutableStage,Sink sink,BooleanSupplier cancelled,LongConsumer progress)throws IOException{
        check(cancelled);List<Header> headers=preflight(immutableStage,cancelled);long[] completed={0};
        try(ZipFile zip=new ZipFile(immutableStage,ZipFile.OPEN_READ,StandardCharsets.UTF_8)){
            if(zip.size()!=headers.size())throw new IOException("Archive directory count mismatch");ZipEntry first=zip.getEntry(ArchiveManifest.MANIFEST_PATH);
            if(first==null || first.getSize()<1 || first.getSize()>ArchiveManifest.MAX_MANIFEST_BYTES)throw new IOException("Missing or oversized archive manifest");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=zip.getInputStream(first)){copy(in,bytes,new Expected(first.getSize(),null),first.getCrc(),cancelled,progress,completed);}
            ArchiveManifest manifest=ArchiveManifest.decode(bytes.toByteArray());Map<String,Expected> expected=entries(manifest);
            if(zip.size()!=expected.size()+1)throw new IOException("Archive has missing or surplus entries");
            for(Header header:headers)if(!header.name.equals(ArchiveManifest.MANIFEST_PATH) && !expected.containsKey(header.name))throw new IOException("Archive contains an unlisted entry");
            for(Map.Entry<String,Expected> item:expected.entrySet()){
                check(cancelled);ZipEntry entry=zip.getEntry(item.getKey());if(entry==null || entry.isDirectory() || entry.getSize()!=item.getValue().bytes)throw new IOException("Archive entry length mismatch");
                OutputStream target=sink==null?new OutputStream(){public void write(int b){}public void write(byte[] b,int off,int length){}}:sink.create(item.getKey(),item.getValue().bytes);
                if(target==null)throw new IOException("Cannot stage archive entry");
                try(InputStream in=zip.getInputStream(entry);OutputStream out=target){copy(in,out,item.getValue(),entry.getCrc(),cancelled,progress,completed);}
            }
            if(completed[0]>ArchiveManifest.MAX_TOTAL_BYTES)throw new IOException("Archive exceeds uncompressed bounds");return manifest;
        }
    }
    private static void copy(InputStream in,OutputStream out,Expected expected,long requiredCrc,BooleanSupplier cancelled,LongConsumer progress,long[] completed)throws IOException{
        MessageDigest digest=sha();CRC32 crc=new CRC32();byte[] buffer=new byte[BUFFER_BYTES];long size=0;int stalled=0;
        while(true){check(cancelled);int count=in.read(buffer);if(count<0)break;if(count==0){if(++stalled>32)throw new IOException("Archive stream made no progress");continue;}stalled=0;size+=count;
            if(size>expected.bytes || completed[0]+count>ArchiveManifest.MAX_TOTAL_BYTES)throw new IOException("Archive stream exceeds declared bounds");out.write(buffer,0,count);digest.update(buffer,0,count);crc.update(buffer,0,count);completed[0]+=count;if(progress!=null)progress.accept(completed[0]);
        }
        if(size!=expected.bytes)throw new IOException("Truncated archive entry");if(expected.hash!=null && !expected.hash.equals(hex(digest.digest())))throw new IOException("Archive SHA-256 mismatch");if(requiredCrc>=0 && crc.getValue()!=requiredCrc)throw new IOException("Archive CRC mismatch");out.flush();
    }
    private static final class Header {
        final String name;final int flags,method;final long crc,compressed,bytes,offset;
        Header(String name,int flags,int method,long crc,long compressed,long bytes,long offset){this.name=name;this.flags=flags;this.method=method;this.crc=crc;this.compressed=compressed;this.bytes=bytes;this.offset=offset;}
    }
    private static int u16(RandomAccessFile in)throws IOException{return in.readUnsignedByte()|(in.readUnsignedByte()<<8);}
    private static long u32(RandomAccessFile in)throws IOException{return (long)u16(in)|((long)u16(in)<<16);}
    private static String name(RandomAccessFile in,int length)throws IOException{
        if(length<1 || length>MAX_NAME_BYTES)throw new IOException("Invalid archive entry name");byte[] bytes=new byte[length];in.readFully(bytes);for(byte b:bytes)if(b<=0 || b>126)throw new IOException("Noncanonical archive entry name");return new String(bytes,StandardCharsets.US_ASCII);
    }
    private static void extra(RandomAccessFile in,int length)throws IOException{
        if(length<0 || length>MAX_EXTRA_BYTES)throw new IOException("Archive extra fields exceed bounds");long end=in.getFilePointer()+length;
        while(in.getFilePointer()<end){if(end-in.getFilePointer()<4)throw new IOException("Malformed archive extra field");int type=u16(in),size=u16(in);if(type!=0x5455 || (size!=1 && size!=5 && size!=9 && size!=13) || size>end-in.getFilePointer())throw new IOException("Unsupported or malformed archive extra fields");in.seek(in.getFilePointer()+size);}if(in.getFilePointer()!=end)throw new IOException("Malformed archive extra fields");
    }
    /** Format v1 deliberately excludes ZIP64, comments, prefixes, concatenation, links and arbitrary directory entries. */
    private static List<Header> preflight(File file,BooleanSupplier cancelled)throws IOException{
        if(file==null || !file.isFile() || file.length()<22 || file.length()>MAX_CONTAINER_BYTES)throw new IOException("Archive container exceeds bounds");
        try(RandomAccessFile in=new RandomAccessFile(file,"r")){
            long length=in.length();in.seek(length-22);if(u32(in)!=0x06054b50L || u16(in)!=0 || u16(in)!=0)throw new IOException("Unsupported archive footer");int diskCount=u16(in),count=u16(in);long centralBytes=u32(in),centralOffset=u32(in);int comment=u16(in);
            if(comment!=0 || count!=diskCount || count<2 || count>ArchiveManifest.MAX_RECORDINGS*2+2 || centralBytes>MAX_CENTRAL_BYTES || centralOffset+centralBytes!=length-22)throw new IOException("Invalid or oversized archive directory");
            in.seek(centralOffset);List<Header> headers=new ArrayList<>();Set<String> names=new HashSet<>();long aggregate=0;
            for(int i=0;i<count;i++){
                check(cancelled);if(u32(in)!=0x02014b50L)throw new IOException("Invalid archive directory header");u16(in);int version=u16(in),flags=u16(in),method=u16(in);u16(in);u16(in);long crc=u32(in),compressed=u32(in),bytes=u32(in);int nameBytes=u16(in),extraBytes=u16(in),commentBytes=u16(in),disk=u16(in);u16(in);long attributes=u32(in),offset=u32(in);String name=name(in,nameBytes);extra(in,extraBytes);
                int type=(int)(attributes>>>16)&0170000;
                if(version>20 || (flags&~(8|2048|2|4))!=0 || (method!=ZipEntry.STORED && method!=ZipEntry.DEFLATED) || commentBytes!=0 || disk!=0 || (attributes&16)!=0 || (type!=0 && type!=0100000) || compressed==0xffffffffL || bytes==0xffffffffL || offset==0xffffffffL || !names.add(name))throw new IOException("Unsupported or duplicate archive entry");
                aggregate+=bytes;if(aggregate>ArchiveManifest.MAX_TOTAL_BYTES)throw new IOException("Archive exceeds uncompressed bounds");headers.add(new Header(name,flags,method,crc,compressed,bytes,offset));
            }
            if(in.getFilePointer()!=length-22 || !headers.get(0).name.equals(ArchiveManifest.MANIFEST_PATH))throw new IOException("Invalid archive manifest/directory ordering");long next=0;
            for(Header header:headers){
                check(cancelled);if(header.offset!=next || header.offset>=centralOffset)throw new IOException("Overlapping or prefixed archive entries");in.seek(header.offset);if(u32(in)!=0x04034b50L)throw new IOException("Invalid local archive header");int version=u16(in),flags=u16(in),method=u16(in);u16(in);u16(in);long crc=u32(in),compressed=u32(in),bytes=u32(in);int nameBytes=u16(in),extraBytes=u16(in);String name=name(in,nameBytes);extra(in,extraBytes);
                if(version>20 || flags!=header.flags || method!=header.method || !name.equals(header.name))throw new IOException("Local/directory archive headers differ");
                boolean descriptor=(flags&8)!=0;if(!descriptor && (crc!=header.crc || compressed!=header.compressed || bytes!=header.bytes))throw new IOException("Local/directory archive lengths differ");
                if(descriptor && ((crc!=0 && crc!=header.crc) || (compressed!=0 && compressed!=header.compressed) || (bytes!=0 && bytes!=header.bytes)))throw new IOException("Inconsistent archive descriptor header");
                next=in.getFilePointer()+header.compressed;if(next>centralOffset)throw new IOException("Archive entry crosses its directory");in.seek(next);
                if(descriptor){long marker=u32(in),actualCrc=marker==0x08074b50L?u32(in):marker;if(actualCrc!=header.crc || u32(in)!=header.compressed || u32(in)!=header.bytes)throw new IOException("Archive data descriptor mismatch");next=in.getFilePointer();}
            }
            if(next!=centralOffset)throw new IOException("Trailing or concatenated archive payload");return headers;
        }
    }
}
