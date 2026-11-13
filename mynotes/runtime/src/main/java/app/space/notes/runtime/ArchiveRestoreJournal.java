package app.space.notes.runtime;

import android.content.Context;
import android.util.AtomicFile;
import app.space.notes.domain.ArchiveManifest;
import app.space.notes.domain.Note;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Durable ownership journal for the only multi-file audio/library restore operation. */
final class ArchiveRestoreJournal {
    private static final byte[] MAGIC="MYNOTES-RESTORE\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int VERSION=1, PREPARED=1, COMMITTED=2;
    private static final int MAX_JOURNAL_BYTES=4*1024*1024;
    private static final Object TRANSACTION_LOCK=new Object();
    private static String cachedJournalPath;
    private static State cachedState;
    private ArchiveRestoreJournal() { }
    static Object transactionLock(){return TRANSACTION_LOCK;}

    static final class Entry {
        final String sourceId,targetId,audioSha256,metadataSha256;
        final long audioBytes;
        final boolean removeAudio,removeMetadata,sourceNoteWasCreated,targetNoteInResult;
        Entry(String sourceId,String targetId,long audioBytes,String audioSha256,String metadataSha256,
              boolean removeAudio,boolean removeMetadata,boolean sourceNoteWasCreated,boolean targetNoteInResult){
            this.sourceId=sourceId;this.targetId=targetId;this.audioBytes=audioBytes;this.audioSha256=audioSha256;this.metadataSha256=metadataSha256;
            this.removeAudio=removeAudio;this.removeMetadata=removeMetadata;this.sourceNoteWasCreated=sourceNoteWasCreated;this.targetNoteInResult=targetNoteInResult;
        }
        boolean protectAfterCommit(){return sourceNoteWasCreated || targetNoteInResult;}
    }
    static final class State {
        final String stageToken,baseLibrarySha256,resultLibrarySha256;
        final int phase;
        final List<Entry> entries;
        State(String stageToken,String baseLibrarySha256,String resultLibrarySha256,int phase,List<Entry> entries){
            this.stageToken=stageToken;this.baseLibrarySha256=baseLibrarySha256;this.resultLibrarySha256=resultLibrarySha256;this.phase=phase;this.entries=new ArrayList<>(entries);validate(this);
        }
        State committed(){return new State(stageToken,baseLibrarySha256,resultLibrarySha256,COMMITTED,entries);}
    }

    static String sha256(byte[] bytes){try{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    static String hex(byte[] bytes){StringBuilder out=new StringBuilder(bytes.length*2);for(byte value:bytes)out.append(Character.forDigit((value&255)>>>4,16)).append(Character.forDigit(value&15,16));return out.toString();}
    private static boolean digest(String value){return value!=null && value.matches("[0-9a-f]{64}");}
    private static File directory(Context context){return new File(context.getFilesDir(),"archive-restore-v1");}
    private static File journal(Context context){return new File(directory(context),"journal.bin");}
    private static boolean exists(File file){return file.exists() || new File(file+".bak").exists();}

    static void begin(Context context,State state)throws IOException{synchronized(TRANSACTION_LOCK){
        File file=journal(context);if(exists(file))throw new IOException("A previous audio restore needs recovery before another archive can be applied");
        write(context,state);
    }}
    static void markCommitted(Context context)throws IOException{synchronized(TRANSACTION_LOCK){
        State current=read(context);if(current.phase==COMMITTED)return;write(context,current.committed());
    }}
    private static void write(Context context,State state)throws IOException{synchronized(TRANSACTION_LOCK){
        byte[] bytes=encode(state);File directory=directory(context);if(!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory())throw new IOException("Cannot prepare private archive recovery journal");
        File base=journal(context);if(!base.getCanonicalFile().getParentFile().equals(directory.getCanonicalFile()))throw new IOException("Unsafe archive recovery journal path");clearCache(base);AtomicFile target=new AtomicFile(base);FileOutputStream out=null;
        try{out=target.startWrite();out.write(bytes);out.getFD().sync();target.finishWrite(out);out=null;if(!java.util.Arrays.equals(bytes,target.readFully()))throw new IOException("Archive recovery journal verification failed");cachedJournalPath=base.getAbsolutePath();cachedState=state;}
        catch(Exception failure){if(out!=null)target.failWrite(out);throw failure instanceof IOException?(IOException)failure:new IOException("Archive recovery journal could not be written",failure);}
    }}
    private static State read(Context context)throws IOException{return read(journal(context));}
    private static State read(File base)throws IOException{
        String path=base.getAbsolutePath();if(cachedState!=null && path.equals(cachedJournalPath))return cachedState;
        AtomicFile target=new AtomicFile(base);byte[] bytes;try(InputStream input=target.openRead();ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(output.size()+count>MAX_JOURNAL_BYTES)throw new IOException("Archive recovery journal exceeds bounds");output.write(buffer,0,count);}bytes=output.toByteArray();}
        State decoded=decode(bytes);cachedJournalPath=path;cachedState=decoded;return decoded;
    }
    private static void clearCache(File base){if(base.getAbsolutePath().equals(cachedJournalPath)){cachedJournalPath=null;cachedState=null;}}
    private static byte[] encode(State state)throws IOException{
        validate(state);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.write(MAGIC);out.writeInt(VERSION);out.writeByte(state.phase);out.writeUTF(state.stageToken);out.writeUTF(state.baseLibrarySha256);out.writeUTF(state.resultLibrarySha256);out.writeInt(state.entries.size());
        for(Entry entry:state.entries){out.writeUTF(entry.sourceId);out.writeUTF(entry.targetId);out.writeLong(entry.audioBytes);out.writeUTF(entry.audioSha256);out.writeBoolean(entry.metadataSha256!=null);if(entry.metadataSha256!=null)out.writeUTF(entry.metadataSha256);int flags=(entry.removeAudio?1:0)|(entry.removeMetadata?2:0)|(entry.sourceNoteWasCreated?4:0)|(entry.targetNoteInResult?8:0);out.writeByte(flags);}
        out.flush();if(bytes.size()>MAX_JOURNAL_BYTES)throw new IOException("Archive recovery journal exceeds bounds");return bytes.toByteArray();
    }
    private static State decode(byte[] bytes)throws IOException{
        if(bytes==null || bytes.length<32 || bytes.length>MAX_JOURNAL_BYTES)throw new IOException("Archive recovery journal exceeds bounds");
        try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));byte[] magic=new byte[MAGIC.length];in.readFully(magic);if(!java.util.Arrays.equals(magic,MAGIC) || in.readInt()!=VERSION)throw new IOException("Unsupported archive recovery journal");int phase=in.readUnsignedByte();String stage=in.readUTF(),base=in.readUTF(),result=in.readUTF();int count=in.readInt();if(count<0 || count>ArchiveManifest.MAX_RECORDINGS)throw new IOException("Archive recovery item count exceeds bounds");List<Entry> entries=new ArrayList<>(count);
            for(int i=0;i<count;i++){String source=in.readUTF(),target=in.readUTF();long audioBytes=in.readLong();String audioHash=in.readUTF();String metadataHash=in.readBoolean()?in.readUTF():null;int flags=in.readUnsignedByte();if((flags&~15)!=0)throw new IOException("Invalid archive recovery flags");entries.add(new Entry(source,target,audioBytes,audioHash,metadataHash,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0));}
            if(in.read()!=-1)throw new IOException("Trailing archive recovery journal bytes");try{return new State(stage,base,result,phase,entries);}catch(IllegalArgumentException invalid){throw new IOException("Invalid archive recovery journal",invalid);}
        }catch(IOException invalid){throw invalid;}catch(Exception invalid){throw new IOException("Archive recovery journal is incomplete");}
    }
    private static void validate(State state){
        if(state==null || (state.phase!=PREPARED && state.phase!=COMMITTED) || !digest(state.baseLibrarySha256) || !digest(state.resultLibrarySha256))throw new IllegalArgumentException("Invalid archive recovery state");
        ArchiveManifest.requireId(state.stageToken);if(state.entries==null || state.entries.size()>ArchiveManifest.MAX_RECORDINGS)throw new IllegalArgumentException("Invalid archive recovery item count");
        Set<String> sources=new HashSet<>(),targets=new HashSet<>();for(Entry entry:state.entries){if(entry==null)throw new IllegalArgumentException("Missing archive recovery item");ArchiveManifest.requireId(entry.sourceId);ArchiveManifest.requireId(entry.targetId);if(!sources.add(entry.sourceId) || !targets.add(entry.targetId))throw new IllegalArgumentException("Duplicate archive recovery identity");if(entry.audioBytes<44 || entry.audioBytes>ArchiveManifest.MAX_AUDIO_BYTES || !digest(entry.audioSha256))throw new IllegalArgumentException("Invalid archive recovery audio facts");if(entry.metadataSha256!=null && !digest(entry.metadataSha256))throw new IllegalArgumentException("Invalid archive recovery transcript facts");if(entry.removeMetadata && entry.metadataSha256==null)throw new IllegalArgumentException("Archive transcript cleanup has no digest");}
    }

    /** Fail closed for late ASR while a reviewed multi-file restore is unresolved. */
    static boolean suppressesAutomaticNote(File filesDirectory,String id){synchronized(TRANSACTION_LOCK){
        if(filesDirectory==null)return false;File file=new File(new File(filesDirectory,"archive-restore-v1"),"journal.bin");if(!exists(file)){clearCache(file);return false;}
        try{State state=read(file);for(Entry entry:state.entries){if(entry.targetId.equals(id))return state.phase==PREPARED || entry.protectAfterCommit();if(state.phase==PREPARED && entry.sourceId.equals(id))return true;}return false;}
        catch(Exception damaged){return true;}
    }}

    static void recover(Context context,List<Note> currentNotes)throws Exception{synchronized(TRANSACTION_LOCK){
        File file=journal(context);if(!exists(file)){clearCache(file);AudioArchiveFiles.cleanupRestoreTemporaries(context);AudioArchiveFiles.cleanupStaleStages(context);return;}
        State state=read(context);String current=sha256(DocumentCodec.encode(currentNotes));boolean committed=state.phase==COMMITTED || current.equals(state.resultLibrarySha256);
        if(committed){if(state.phase!=COMMITTED)write(context,state.committed());finishCommitted(context,state);}
        else if(current.equals(state.baseLibrarySha256))rollback(context,state);
        else preserveAmbiguous(context,state);
        AudioArchiveFiles.removeStageToken(context,state.stageToken);AudioArchiveFiles.cleanupStaleStages(context);AudioArchiveFiles.cleanupRestoreTemporaries(context);
        new AtomicFile(file).delete();clearCache(file);File parent=directory(context);if(parent.isDirectory() && parent.list()!=null && parent.list().length==0 && !parent.delete() && parent.exists())throw new IOException("Archive recovery journal cleanup needs retry");
    }}
    private static void finishCommitted(Context context,State state)throws IOException{
        VoiceFiles voices=new VoiceFiles(context);for(Entry entry:state.entries)if(entry.protectAfterCommit() && voices.audio(entry.targetId).isFile())voices.noteCreated(entry.targetId);
    }
    private static void rollback(Context context,State state)throws IOException{
        VoiceFiles voices=new VoiceFiles(context);for(int i=state.entries.size()-1;i>=0;i--){Entry entry=state.entries.get(i);boolean guard=false;
            if(entry.removeMetadata && entry.metadataSha256!=null){try{byte[] metadata=voices.archiveMetadata(entry.targetId);if(metadata!=null){if(sha256(metadata).equals(entry.metadataSha256)){if(!voices.removeArchiveMetadataIfMatches(entry.targetId,entry.metadataSha256))guard=true;}else guard=true;}}catch(IOException damaged){guard=true;}}
            if(entry.removeAudio){File audio=voices.audio(entry.targetId);if(audio.exists()){if(!audio.isFile() || audio.length()!=entry.audioBytes)guard=true;else try{if(sha256File(audio,entry.audioBytes).equals(entry.audioSha256)){if(!voices.removeArchiveAudioIfMatches(entry.targetId,entry.audioBytes,entry.audioSha256))guard=true;}else guard=true;}catch(IOException failure){guard=true;}}}
            if(guard && voices.audio(entry.targetId).isFile())voices.noteCreated(entry.targetId);
        }
    }
    private static void preserveAmbiguous(Context context,State state)throws IOException{
        VoiceFiles voices=new VoiceFiles(context);for(Entry entry:state.entries){if(!(entry.removeAudio || entry.removeMetadata || entry.protectAfterCommit()))continue;if(voices.audio(entry.targetId).isFile())voices.noteCreated(entry.targetId);}
    }
    private static String sha256File(File file,long maximum)throws IOException{
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(file)){byte[] buffer=new byte[64*1024];long total=0;int count;while((count=input.read(buffer))!=-1){total+=count;if(total>maximum)throw new IOException("Archive recovery audio exceeds its declared length");digest.update(buffer,0,count);}return hex(digest.digest());}}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
