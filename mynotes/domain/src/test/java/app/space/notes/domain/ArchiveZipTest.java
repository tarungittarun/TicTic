package app.space.notes.domain;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

/** Transport fixtures are deliberately small; runtime must separately validate note JSON and real PCM. */
public class ArchiveZipTest {
    private static final String ID="00000000-0000-0000-0000-000000000001";
    private String sha(byte[] bytes)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private Map<String,byte[]> payload(){Map<String,byte[]> data=new LinkedHashMap<>();data.put("library.json","Fixture library".getBytes(StandardCharsets.UTF_8));data.put(ArchiveManifest.audioPath(ID),new byte[1024]);data.put(ArchiveManifest.metadataPath(ID),"Fixture metadata".getBytes(StandardCharsets.UTF_8));return data;}
    private ArchiveManifest manifest(Map<String,byte[]> data)throws Exception{return new ArchiveManifest(data.get("library.json").length,sha(data.get("library.json")),Arrays.asList(new ArchiveManifest.Recording(ID,1234,data.get(ArchiveManifest.audioPath(ID)).length,sha(data.get(ArchiveManifest.audioPath(ID))),data.get(ArchiveManifest.metadataPath(ID)).length,sha(data.get(ArchiveManifest.metadataPath(ID))),true)));}
    private byte[] archive(ArchiveManifest manifest,Map<String,byte[]> data)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();ArchiveZip.write(out,manifest,name->new ByteArrayInputStream(data.get(name)),null,null);return out.toByteArray();}
    private File file(byte[] bytes)throws Exception{File file=File.createTempFile("notes-archive-",".zip");file.deleteOnExit();try(OutputStream out=new FileOutputStream(file)){out.write(bytes);}return file;}
    private void reject(byte[] bytes)throws Exception{File file=file(bytes);AtomicInteger mutations=new AtomicInteger();try{assertThrows(IOException.class,()->ArchiveZip.read(file,(name,size)->{mutations.incrementAndGet();return new ByteArrayOutputStream();},null,null));assertEquals("Header/namespace rejection must precede staging",0,mutations.get());}finally{assertTrue(file.delete());}}
    private int central(byte[] bytes,String name){ByteBuffer data=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);byte[] wanted=name.getBytes(StandardCharsets.US_ASCII);for(int i=0;i+46+wanted.length<=bytes.length;i++)if(data.getInt(i)==0x02014b50 && (data.getShort(i+28)&65535)==wanted.length){boolean match=true;for(int j=0;j<wanted.length;j++)if(bytes[i+46+j]!=wanted[j])match=false;if(match)return i;}throw new AssertionError("Fixture central entry absent");}
    private byte[] replaceName(byte[] bytes,String from,String to){byte[] copy=bytes.clone(),a=from.getBytes(StandardCharsets.US_ASCII),b=to.getBytes(StandardCharsets.US_ASCII);assertEquals(a.length,b.length);for(int i=0;i+a.length<=copy.length;i++){boolean same=true;for(int j=0;j<a.length;j++)if(copy[i+j]!=a[j])same=false;if(same){System.arraycopy(b,0,copy,i,b.length);i+=a.length-1;}}return copy;}
    @Test public void streamingRoundTripChecksEveryPayloadAndCountsProgress()throws Exception{
        Map<String,byte[]> input=payload();ArchiveManifest manifest=manifest(input);byte[] archive=archive(manifest,input);File file=file(archive);Map<String,byte[]> restored=new HashMap<>();long[] progress={0};
        try{ArchiveManifest result=ArchiveZip.read(file,(name,size)->new ByteArrayOutputStream(){@Override public void close(){restored.put(name,toByteArray());}},null,bytes->progress[0]=bytes);assertArrayEquals(manifest.encode(),result.encode());assertEquals(input.keySet(),restored.keySet());for(String name:input.keySet())assertArrayEquals(input.get(name),restored.get(name));assertEquals(manifest.payloadBytes+manifest.encode().length,progress[0]);}finally{assertTrue(file.delete());}
    }
    @Test public void emptyRecordingListStillHasVerifiedLibraryAndManifest()throws Exception{
        byte[] library="[]".getBytes(StandardCharsets.US_ASCII);ArchiveManifest manifest=new ArchiveManifest(library.length,sha(library),Collections.emptyList());File file=file(archive(manifest,Collections.singletonMap("library.json",library)));try{assertTrue(ArchiveZip.read(file,null,null,null).recordings.isEmpty());}finally{assertTrue(file.delete());}
    }
    @Test public void sourceLengthAndDigestChangesCannotBecomeSuccessfulExports()throws Exception{
        Map<String,byte[]> data=payload();ArchiveManifest manifest=manifest(data);data.put("library.json",new byte[manifest.libraryBytes+1]);assertThrows(IOException.class,()->archive(manifest,data));data.put("library.json",new byte[manifest.libraryBytes-1]);assertThrows(IOException.class,()->archive(manifest,data));data.put("library.json",new byte[manifest.libraryBytes]);assertThrows(IOException.class,()->archive(manifest,data));
    }
    @Test public void cancellationAndZeroProgressFailWithoutUnboundedLoops()throws Exception{
        Map<String,byte[]> data=payload();ArchiveManifest manifest=manifest(data);assertThrows(InterruptedIOException.class,()->ArchiveZip.write(new ByteArrayOutputStream(),manifest,name->new ByteArrayInputStream(data.get(name)),()->true,null));
        assertThrows(IOException.class,()->ArchiveZip.write(new ByteArrayOutputStream(),manifest,name->new InputStream(){public int read(){return 0;}public int read(byte[] b,int off,int length){return 0;}},null,null));File file=file(archive(manifest,data));try{assertThrows(InterruptedIOException.class,()->ArchiveZip.read(file,null,()->true,null));}finally{assertTrue(file.delete());}
    }
    @Test public void traversalAndUnlistedNamesAreRejectedBeforeAnySinkOpens()throws Exception{
        Map<String,byte[]> data=payload();byte[] bytes=archive(manifest(data),data);reject(replaceName(bytes,"library.json","../evil.json"));reject(replaceName(bytes,"library.json","/unsafe.json"));
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){for(String name:Arrays.asList("manifest.bin","library.json","surplus.bin")){zip.putNextEntry(new ZipEntry(name));zip.write(name.equals("manifest.bin")?manifest(data).encode():new byte[1]);zip.closeEntry();}}reject(out.toByteArray());
    }
    @Test public void duplicateDirectoryEntriesAndLocalNameAliasesAreRefused()throws Exception{
        Map<String,byte[]> data=payload();byte[] bytes=archive(manifest(data),data);byte[] alias=bytes.clone();int central=central(alias,"library.json");int local=ByteBuffer.wrap(alias).order(ByteOrder.LITTLE_ENDIAN).getInt(central+42);alias[local+30]='X';reject(alias);
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){for(String name:Arrays.asList("manifest.bin","library.json","other__.json")){zip.putNextEntry(new ZipEntry(name));zip.write(name.equals("manifest.bin")?manifest(data).encode():new byte[1]);zip.closeEntry();}}reject(replaceName(out.toByteArray(),"other__.json","library.json"));
    }
    @Test public void explicitCrcVerificationCatchesConsistentHeaderDescriptorTampering()throws Exception{
        Map<String,byte[]> data=payload();byte[] bytes=archive(manifest(data),data);ByteBuffer view=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);int central=central(bytes,"library.json"),local=view.getInt(central+42),compressed=view.getInt(central+20);int start=local+30+(view.getShort(local+26)&65535)+(view.getShort(local+28)&65535),descriptor=start+compressed;assertEquals(0x08074b50,view.getInt(descriptor));view.putInt(central+16,view.getInt(central+16)^1);view.putInt(descriptor+4,view.getInt(descriptor+4)^1);File file=file(bytes);try{IOException error=assertThrows(IOException.class,()->ArchiveZip.read(file,null,null,null));assertTrue(error.getMessage().contains("CRC"));}finally{assertTrue(file.delete());}
    }
    @Test public void truncatedPrefixedConcatenatedOrCommentedContainersAreRefused()throws Exception{
        Map<String,byte[]> data=payload();byte[] bytes=archive(manifest(data),data);reject(Arrays.copyOf(bytes,bytes.length-1));reject(Arrays.copyOf(bytes,bytes.length+1));byte[] prefix=new byte[bytes.length+1];System.arraycopy(bytes,0,prefix,1,bytes.length);reject(prefix);
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){zip.setComment("unsupported");zip.putNextEntry(new ZipEntry("manifest.bin"));zip.write(manifest(data).encode());zip.closeEntry();zip.putNextEntry(new ZipEntry("library.json"));zip.write(data.get("library.json"));zip.closeEntry();}reject(out.toByteArray());
    }
    @Test public void linksEncryptionUnknownMethodsAndOversizedDirectoriesFailPreflight()throws Exception{
        Map<String,byte[]> data=payload();byte[] bytes=archive(manifest(data),data);int central=central(bytes,"library.json");byte[] link=bytes.clone();ByteBuffer.wrap(link).order(ByteOrder.LITTLE_ENDIAN).putInt(central+38,(0120000|0777)<<16);reject(link);
        byte[] encrypted=bytes.clone();encrypted[central+8]|=1;reject(encrypted);byte[] method=bytes.clone();ByteBuffer.wrap(method).order(ByteOrder.LITTLE_ENDIAN).putShort(central+10,(short)99);reject(method);byte[] count=bytes.clone();ByteBuffer.wrap(count).order(ByteOrder.LITTLE_ENDIAN).putShort(count.length-12,(short)65535);reject(count);
    }
    @Test public void sourceAndSinkFailuresCloseTheirOwnedStreams()throws Exception{
        Map<String,byte[]> data=payload();ArchiveManifest manifest=manifest(data);AtomicBoolean closed=new AtomicBoolean();assertThrows(IOException.class,()->ArchiveZip.write(new ByteArrayOutputStream(),manifest,name->new ByteArrayInputStream(new byte[1]){@Override public void close(){closed.set(true);}},null,null));assertTrue(closed.get());File file=file(archive(manifest,data));try{assertThrows(IOException.class,()->ArchiveZip.read(file,(name,size)->{throw new IOException("Fixture disk full");},null,null));}finally{assertTrue(file.delete());}
    }
}
