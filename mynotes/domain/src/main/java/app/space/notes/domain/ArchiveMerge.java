package app.space.notes.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only restore policy: preserve different audio/transcripts as stable copies, remap every note link. */
public final class ArchiveMerge {
    private ArchiveMerge() { }
    public static final class Content {
        public final long bytes;
        public final String audioHash,metadataContentHash;
        public Content(long bytes,String audioHash,String metadataContentHash){if(bytes<0)throw new IllegalArgumentException("Negative recording length");hash(audioHash);if(metadataContentHash!=null)hash(metadataContentHash);this.bytes=bytes;this.audioHash=audioHash;this.metadataContentHash=metadataContentHash;}
    }
    public interface Inventory { Content lookup(String id)throws IOException; }
    public static final class Target {
        public final String sourceId,targetId;
        public final boolean addAudio,addMetadata,conflictCopy;
        private Target(String source,String target,Content incoming,Content current){sourceId=source;targetId=target;addAudio=current==null;addMetadata=incoming.metadataContentHash!=null && (current==null || current.metadataContentHash==null);conflictCopy=!source.equals(target);}
    }
    public static final class Plan {
        public final List<Target> recordings;
        public final NoteMerge.Plan notes;
        public final int addedAudio,reusedAudio,conflictCopies,addedMetadata;
        private Plan(List<Target> targets,NoteMerge.Plan notes){recordings=Collections.unmodifiableList(new ArrayList<>(targets));this.notes=notes;int added=0,reused=0,copies=0,metadata=0;for(Target target:targets){if(target.addAudio)added++;else reused++;if(target.conflictCopy)copies++;if(target.addMetadata)metadata++;}addedAudio=added;reusedAudio=reused;conflictCopies=copies;addedMetadata=metadata;}
    }
    private static void hash(String value){if(value==null || !value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid recording content hash");}
    private static boolean compatible(Content incoming,Content current){return current!=null && incoming.bytes==current.bytes && incoming.audioHash.equals(current.audioHash) && (incoming.metadataContentHash==null || current.metadataContentHash==null || incoming.metadataContentHash.equals(current.metadataContentHash));}
    private static String conflict(String source,Content content,int suffix){return UUID.nameUUIDFromBytes(("MyNotes archive recording conflict v1:"+source+":"+content.bytes+":"+content.audioHash+":"+(content.metadataContentHash==null?"":content.metadataContentHash)+":"+suffix).getBytes(StandardCharsets.UTF_8)).toString();}
    public static Plan preview(ArchiveManifest manifest,Map<String,String> metadataContentHashes,List<Note> existing,List<Note> incoming,Inventory inventory)throws IOException{
        if(manifest==null || metadataContentHashes==null || inventory==null)throw new IllegalArgumentException("Missing archive merge inputs");
        Set<String> originalIds=new HashSet<>(),used=new HashSet<>(),expectedMetadata=new HashSet<>();for(ArchiveManifest.Recording recording:manifest.recordings){originalIds.add(recording.id);if(recording.metadataBytes>0)expectedMetadata.add(recording.id);}if(!expectedMetadata.equals(metadataContentHashes.keySet()))throw new IllegalArgumentException("Metadata identities must exactly match manifest");for(String id:expectedMetadata)hash(metadataContentHashes.get(id));
        List<Target> targets=new ArrayList<>();Map<String,String> mapping=new HashMap<>();
        for(ArchiveManifest.Recording recording:manifest.recordings){Content content=new Content(recording.audioBytes,recording.audioSha256,metadataContentHashes.get(recording.id));String target=recording.id;Content current=inventory.lookup(target);
            if(used.contains(target) || (current!=null && !compatible(content,current))){boolean found=false;for(int suffix=0;suffix<=ArchiveManifest.MAX_RECORDINGS;suffix++){target=conflict(recording.id,content,suffix);if(originalIds.contains(target) || used.contains(target))continue;current=inventory.lookup(target);if(current==null || compatible(content,current)){found=true;break;}}if(!found)throw new IOException("Too many recording identity conflicts; nothing restored");}
            used.add(target);mapping.put(recording.id,target);targets.add(new Target(recording.id,target,content,current));
        }
        Map<String,String> noteIds=new HashMap<>();for(Map.Entry<String,String> entry:mapping.entrySet())if(!entry.getKey().equals(entry.getValue()))noteIds.put(VoiceTranscripts.noteId(entry.getKey()),VoiceTranscripts.noteId(entry.getValue()));List<Note> remapped=new ArrayList<>();for(Note note:incoming){String id=noteIds.getOrDefault(note.id,note.id);List<String> sources=new ArrayList<>();for(String source:note.voiceSources)sources.add(mapping.getOrDefault(source,source));remapped.add(new Note(id,note.title,note.body,note.createdAt,note.updatedAt,note.deletedAt,note.pinned,note.archived,note.color,note.checklist,note.hideChecked,note.items,note.marks,note.labels,note.diagram,sources,note.reminderAt,note.folder,note.background,note.images,note.content));}
        return new Plan(targets,NoteMerge.preview(existing,remapped));
    }
}
