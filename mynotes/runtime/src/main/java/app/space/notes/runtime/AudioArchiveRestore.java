package app.space.notes.runtime;

import android.content.Context;
import app.space.notes.domain.ArchiveManifest;
import app.space.notes.domain.ArchiveMerge;
import app.space.notes.domain.Note;
import app.space.notes.domain.VoiceTranscripts;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Source-pinned coordinator for verified audio files plus the authoritative note-library save. */
public final class AudioArchiveRestore {
    private AudioArchiveRestore() { }
    public static final class Result {
        public final boolean destinationComplete,recoveryPending;
        public final int addedAudio,reusedAudio,conflictCopies,addedMetadata;
        private Result(boolean destinationComplete,boolean recoveryPending,ArchiveMerge.Plan plan){this.destinationComplete=destinationComplete;this.recoveryPending=recoveryPending;addedAudio=plan.addedAudio;reusedAudio=plan.reusedAudio;conflictCopies=plan.conflictCopies;addedMetadata=plan.addedMetadata;}
    }
    private static void check(BooleanSupplier cancelled)throws InterruptedIOException{if(Thread.currentThread().isInterrupted() || (cancelled!=null && cancelled.getAsBoolean()))throw new InterruptedIOException("Archive restore cancelled before the library commit");}
    private static String libraryHash(List<Note> notes)throws IOException{try{return ArchiveRestoreJournal.sha256(DocumentCodec.encode(notes));}catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to verify the restore; original library retained",pressure);}}
    private static boolean samePlan(ArchiveMerge.Plan first,ArchiveMerge.Plan second)throws IOException{
        if(first.recordings.size()!=second.recordings.size() || first.addedAudio!=second.addedAudio || first.reusedAudio!=second.reusedAudio || first.conflictCopies!=second.conflictCopies || first.addedMetadata!=second.addedMetadata)return false;
        for(int i=0;i<first.recordings.size();i++){ArchiveMerge.Target a=first.recordings.get(i),b=second.recordings.get(i);if(!a.sourceId.equals(b.sourceId) || !a.targetId.equals(b.targetId) || a.addAudio!=b.addAudio || a.addMetadata!=b.addMetadata || a.conflictCopy!=b.conflictCopy)return false;}
        return Arrays.equals(DocumentCodec.encode(first.notes.notes),DocumentCodec.encode(second.notes.notes));
    }
    private static void sameLibrary(List<Note> expected,List<Note> actual)throws IOException{if(!Arrays.equals(DocumentCodec.encode(expected),DocumentCodec.encode(actual)))throw new IOException("Library changed; review the archive again");}

    /**
     * Rechecks the reviewed plan, journals ownership before publishing any files, installs
     * only verified audio/transcripts, and commits notes through NotesRepository's compare-and-apply.
     */
    public static Result apply(Context context,NotesRepository repository,List<Note> current,
                               AudioArchiveFiles.Stage stage,ArchiveMerge.Plan reviewed,
                               BooleanSupplier cancelled,AudioArchiveFiles.Progress progress)throws Exception{
        if(context==null || repository==null || current==null || stage==null || reviewed==null)throw new IllegalArgumentException("Missing archive restore input");
        if(stage.isClosed() || !stage.belongsTo(context))throw new IOException("Archive preview belongs to a different private stage; inspect it again");
        byte[] baseBytes=DocumentCodec.encode(current);List<Note> durable=repository.localSnapshot();sameLibrary(current,durable);
        if(progress!=null)progress.update("Rechecking recording conflicts",0,stage.manifest.recordings.size());
        ArchiveMerge.Plan fresh=AudioArchiveFiles.preview(context,stage,current,cancelled);if(!samePlan(reviewed,fresh))throw new IOException("Recordings changed after preview; review the archive again");
        Map<String,ArchiveManifest.Recording> recordings=new HashMap<>();for(ArchiveManifest.Recording recording:stage.manifest.recordings)recordings.put(recording.id,recording);
        Set<String> resultNoteIds=new HashSet<>();for(Note note:fresh.notes.notes)resultNoteIds.add(note.id);
        List<ArchiveRestoreJournal.Entry> entries=new ArrayList<>();Map<String,String> metadataContentHashes=new HashMap<>();
        for(ArchiveMerge.Target target:fresh.recordings){
            ArchiveManifest.Recording recording=recordings.get(target.sourceId);if(recording==null)throw new IOException("Archive plan no longer matches its manifest");
            byte[] metadata=AudioArchiveFiles.remappedMetadata(stage,recording,target.targetId);String metadataHash=metadata==null?null:ArchiveRestoreJournal.sha256(metadata);
            if(metadata!=null)metadataContentHashes.put(target.targetId,ArchiveJson.metadataContentHash(metadata,target.targetId));
            entries.add(new ArchiveRestoreJournal.Entry(target.sourceId,target.targetId,recording.audioBytes,recording.audioSha256,metadataHash,target.addAudio,target.addMetadata,recording.noteWasCreated,resultNoteIds.contains(VoiceTranscripts.noteId(target.targetId))));
        }
        String baseHash=ArchiveRestoreJournal.sha256(baseBytes);String resultHash=libraryHash(fresh.notes.notes);
        ArchiveRestoreJournal.State journal=new ArchiveRestoreJournal.State(stage.token(),baseHash,resultHash,1,entries);
        VoiceFiles voices=new VoiceFiles(context);
        synchronized(ArchiveRestoreJournal.transactionLock()) {
            for(ArchiveMerge.Target target:fresh.recordings){
                if(VoiceTasks.running(context,target.sourceId) || VoiceTasks.running(context,target.targetId))throw new IOException("A recording is being transcribed; wait for it to finish, then review the archive again");
                ArchiveManifest.Recording recording=recordings.get(target.sourceId);
                if(recording.metadataBytes>0){byte[] currentMetadata=voices.archiveMetadata(target.targetId);String expected=metadataContentHashes.get(target.targetId);
                    if(target.addMetadata ? currentMetadata!=null : currentMetadata==null)throw new IOException("Transcript changed after preview; review the archive again");
                    if(currentMetadata!=null && (expected==null || !expected.equals(ArchiveJson.metadataContentHash(currentMetadata,target.targetId))))throw new IOException("Transcript changed after preview; review the archive again");
                }else if(target.addMetadata)throw new IOException("Archive plan refers to a transcript missing from its manifest");
            }
            synchronized(VoiceFiles.AUDIO_LOCK) {
                for(ArchiveMerge.Target target:fresh.recordings){File audio=voices.audio(target.targetId);if(target.addAudio ? audio.exists() : !audio.isFile())throw new IOException("Recordings changed after preview; review the archive again");}
                ArchiveRestoreJournal.begin(context,journal);
            }
        }
        boolean destinationComplete=false;
        try{
            check(cancelled);AudioArchiveFiles.install(context,stage,fresh,cancelled,progress);check(cancelled);
            if(progress!=null)progress.update("Saving the verified note merge",1,1);check(cancelled);
            try{destinationComplete=repository.applyRestoreIfCurrent(current,fresh.notes.notes);}
            catch(Exception saveFailure){if(!resultHash.equals(libraryHash(repository.localSnapshot())))throw saveFailure;destinationComplete=false;}
            List<Note> committed=repository.localSnapshot();if(!resultHash.equals(libraryHash(committed)))throw new IOException("The merged library could not be verified; original notes are retained");
            try{ArchiveRestoreJournal.markCommitted(context);ArchiveRestoreJournal.recover(context,committed);stage.close();}
            catch(Exception finalization){try{stage.close();}catch(Exception cleanup){finalization.addSuppressed(cleanup);}if(progress!=null)progress.update("Restore saved; private cleanup will retry",1,1);return new Result(destinationComplete,true,fresh);}
            if(progress!=null)progress.update("Full backup restored",1,1);return new Result(destinationComplete,false,fresh);
        }catch(Exception failure){
            try{
                List<Note> latest=repository.localSnapshot();if(resultHash.equals(libraryHash(latest))){
                    try{ArchiveRestoreJournal.markCommitted(context);ArchiveRestoreJournal.recover(context,latest);}
                    catch(Exception finalization){failure.addSuppressed(finalization);try{stage.close();}catch(Exception cleanup){failure.addSuppressed(cleanup);}return new Result(false,true,fresh);}
                    return new Result(false,false,fresh);
                }
                ArchiveRestoreJournal.recover(context,latest);
            }catch(Exception recovery){failure.addSuppressed(recovery);}
            try{stage.close();}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw failure;
        }
    }

    /** Must run after private notes load and before VoiceFiles.list/late-ASR recovery. */
    public static void recoverBeforeVoiceScan(Context context,List<Note> currentNotes)throws Exception{ArchiveRestoreJournal.recover(context,currentNotes);}
}
