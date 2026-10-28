package app.space.notes.domain;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public class ArchiveManifestTest {
    private static final String A="00000000-0000-0000-0000-000000000001", B="00000000-0000-0000-0000-000000000002";
    private static final String HASH=String.join("",Collections.nCopies(64,"a"));
    private ArchiveManifest.Recording recording(String id){return new ArchiveManifest.Recording(id,123456789L,32044,HASH,12,HASH,true);}
    @Test public void deterministicRoundTripPreservesAudioMetadataAndSourceGuard()throws Exception{
        ArchiveManifest original=new ArchiveManifest(123,HASH,Arrays.asList(recording(B),recording(A)));
        ArchiveManifest read=ArchiveManifest.decode(original.encode());assertEquals(123,read.libraryBytes);assertEquals(HASH,read.librarySha256);assertEquals(A,read.recordings.get(0).id);assertEquals(B,read.recordings.get(1).id);
        ArchiveManifest.Recording entry=read.recordings.get(0);assertEquals(123456789L,entry.createdAt);assertEquals(32044,entry.audioBytes);assertEquals(12,entry.metadataBytes);assertTrue(entry.noteWasCreated);assertEquals(HASH,entry.audioSha256);assertEquals(HASH,entry.metadataSha256);assertArrayEquals(original.encode(),read.encode());
    }
    @Test public void emptyLibraryArchiveAndMissingTranscriptAreDistinct()throws Exception{
        assertTrue(ArchiveManifest.decode(new ArchiveManifest(2,HASH,Collections.emptyList()).encode()).recordings.isEmpty());
        ArchiveManifest.Recording bare=new ArchiveManifest.Recording(A,0,44,HASH,0,null,false);ArchiveManifest read=ArchiveManifest.decode(new ArchiveManifest(2,HASH,Arrays.asList(bare)).encode());assertEquals(null,read.recordings.get(0).metadataSha256);assertFalse(read.recordings.get(0).noteWasCreated);
    }
    @Test public void canonicalOwnedPathsCannotEscapeArchiveNamespace(){
        assertEquals("audio/"+A+".wav",ArchiveManifest.audioPath(A));assertEquals("audio/"+A+".json",ArchiveManifest.metadataPath(A));
        for(String id:Arrays.asList("../escape","/absolute","1-1-1-1-1","00000000-0000-0000-0000-00000000000A",A+"/other","",null))assertThrows(IllegalArgumentException.class,()->ArchiveManifest.audioPath(id));
    }
    @Test public void duplicatesAndExternalListMutationAreRejectedOrIsolated(){
        assertThrows(IllegalArgumentException.class,()->new ArchiveManifest(1,HASH,Arrays.asList(recording(A),recording(A))));
        List<ArchiveManifest.Recording> entries=new ArrayList<>();entries.add(recording(A));ArchiveManifest manifest=new ArchiveManifest(1,HASH,entries);entries.clear();assertEquals(1,manifest.recordings.size());assertThrows(UnsupportedOperationException.class,()->manifest.recordings.clear());
    }
    @Test public void lengthsDigestsDatesAndOptionalMetadataAreBounded(){
        assertThrows(IllegalArgumentException.class,()->new ArchiveManifest(0,HASH,Collections.emptyList()));assertThrows(IllegalArgumentException.class,()->new ArchiveManifest(ArchiveManifest.MAX_LIBRARY_BYTES+1,HASH,Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,()->new ArchiveManifest(1,"not a digest",Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,()->new ArchiveManifest.Recording(A,-1,44,HASH,0,null,false));assertThrows(IllegalArgumentException.class,()->new ArchiveManifest.Recording(A,0,43,HASH,0,null,false));assertThrows(IllegalArgumentException.class,()->new ArchiveManifest.Recording(A,0,ArchiveManifest.MAX_AUDIO_BYTES+1,HASH,0,null,false));
        assertThrows(IllegalArgumentException.class,()->new ArchiveManifest.Recording(A,0,44,HASH,ArchiveManifest.MAX_METADATA_BYTES+1,HASH,false));assertThrows(IllegalArgumentException.class,()->new ArchiveManifest.Recording(A,0,44,HASH,0,HASH,false));assertThrows(IllegalArgumentException.class,()->new ArchiveManifest.Recording(A,0,44,HASH,1,null,false));
    }
    @Test public void aggregateUncompressedBytesAreBoundedBeforeAnyExtraction(){
        List<ArchiveManifest.Recording> huge=new ArrayList<>();for(int i=0;i<4;i++)huge.add(new ArchiveManifest.Recording(new UUID(0,i).toString(),0,ArchiveManifest.MAX_AUDIO_BYTES,HASH,0,null,false));assertThrows(IllegalArgumentException.class,()->new ArchiveManifest(1,HASH,huge));
        assertThrows(IllegalArgumentException.class,()->new ArchiveManifest(1,HASH,Collections.nCopies(ArchiveManifest.MAX_RECORDINGS+1,recording(A))));
    }
    @Test public void everyTruncationAndTrailingDataFailsClosed()throws Exception{
        byte[] good=new ArchiveManifest(1,HASH,Arrays.asList(recording(A))).encode();for(int i=0;i<good.length;i++){byte[] cut=Arrays.copyOf(good,i);assertThrows(IOException.class,()->ArchiveManifest.decode(cut));}
        assertThrows(IOException.class,()->ArchiveManifest.decode(Arrays.copyOf(good,good.length+1)));assertThrows(IOException.class,()->ArchiveManifest.decode(new byte[ArchiveManifest.MAX_MANIFEST_BYTES+1]));
    }
    @Test public void unknownVersionAndNoncanonicalBooleanAreRefused()throws Exception{
        byte[] good=new ArchiveManifest(1,HASH,Arrays.asList(recording(A))).encode();byte[] badMagic=good.clone();badMagic[0]^=1;assertThrows(IOException.class,()->ArchiveManifest.decode(badMagic));byte[] badVersion=good.clone();badVersion[18]=2;assertThrows(IOException.class,()->ArchiveManifest.decode(badVersion));byte[] badBoolean=good.clone();badBoolean[badBoolean.length-1]=2;assertThrows(IOException.class,()->ArchiveManifest.decode(badBoolean));
    }
}
