package app.space.notes.domain;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Stable source identity makes repeated/recovered native results idempotent, without touching audio. */
public final class VoiceTranscripts {
    private VoiceTranscripts() { }
    public static String noteId(String recording){UUID.fromString(recording);return UUID.nameUUIDFromBytes(("MyNotes voice v1:"+recording).getBytes(StandardCharsets.UTF_8)).toString();}
    public static String pending(String id){return "Audio saved in Voice inbox: "+id+"\nAutomatic offline transcription pending.";}
    public static Note placeholder(String id,long now){return Note.create(now).withId(noteId(id)).edit("Voice note",pending(id),now);}
    public static Note apply(Note note,String id,String transcript,long now){
        UUID.fromString(id);if(note.deletedAt>0 || note.voiceSources.contains(id) || transcript.trim().isEmpty())return note;
        String value="Automatic transcript (review against audio):\n"+transcript;Note result;
        if(note.checklist){List<Note.Item> rows=new ArrayList<>(note.items);for(String line:value.split("\n",-1))rows.add(new Note.Item(UUID.randomUUID().toString(),line,false,0));result=note.list(note.title,rows,note.hideChecked,now);}
        else{String prefix=note.body.equals(pending(id))?"Audio saved in Voice inbox: "+id:note.body;result=note.rich(note.title,prefix+(prefix.isEmpty()?"":"\n\n")+value,note.marks,now);}
        return result.voiceSource(id,now);
    }
}
