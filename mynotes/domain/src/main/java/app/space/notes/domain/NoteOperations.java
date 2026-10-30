package app.space.notes.domain;
import java.util.*;
public final class NoteOperations {
    private NoteOperations() { }
    public static Note duplicate(Note source,long now){return new Note(UUID.randomUUID().toString(),source.title,source.body,now,now,0,source.pinned,false,source.color,source.checklist,source.hideChecked,source.items,source.marks,source.labels,source.diagram,source.voiceSources,0,source.folder,source.background,source.images,source.content);}
    public static Note merge(List<Note> sources,long now){
        if(sources.size()<2)throw new IllegalArgumentException("Select at least two notes");
        for(Note note:sources)if(note.content!=null)return mergeCanonical(sources,now);
        StringBuilder body=new StringBuilder();List<String> tags=new ArrayList<>();
        for(Note n:sources){if(!n.images.isEmpty())throw new IllegalArgumentException("Notes with images cannot be flattened by Merge. Copy or export them instead.");if(!n.diagram.nodes.isEmpty())throw new IllegalArgumentException("Diagram notes cannot be flattened by Merge. Copy or export them instead.");
            if(n.deletedAt>0)throw new IllegalArgumentException("Restore trashed notes before merging");
            if(body.length()>0)body.append("\n\n────────\n\n");
            if(!n.title.isEmpty())body.append(n.title).append('\n');body.append(n.plainText());tags.addAll(n.labels);
        }
        return Note.create(now).edit("Merged notes",body.toString(),now).properties(sources.get(0).color,Note.normalizeLabels(tags),now).folder(sources.get(0).folder,now).background(sources.get(0).background,now);
    }
    private static Note mergeCanonical(List<Note> sources,long now){
        List<DocumentContent.Block> blocks=new ArrayList<>();List<String> labels=new ArrayList<>();Set<String> rows=new HashSet<>(),images=new HashSet<>();List<String> voices=new ArrayList<>();
        for(Note source:sources){
            if(source.deletedAt>0)throw new IllegalArgumentException("Restore trashed notes before merging");
            labels.addAll(source.labels);for(String voice:source.voiceSources)if(!voices.contains(voice))voices.add(voice);
            if(!source.title.isEmpty())blocks.add(DocumentContent.Block.text(UUID.randomUUID().toString(),new RichText(source.title,Arrays.asList(new Note.Mark("h1",0,source.title.length())))));
            for(DocumentContent.Block block:DocumentContent.fromLegacy(source).blocks){String id=UUID.randomUUID().toString();switch(block.kind){
                case TEXT:blocks.add(DocumentContent.Block.text(id,block.text));break;
                case CHECKLIST:List<Note.Item> items=new ArrayList<>();for(Note.Item item:block.items){String row=item.id;if(!rows.add(row)){row=UUID.randomUUID().toString();rows.add(row);}items.add(new Note.Item(row,item.text,item.checked,item.depth,item.marks));}blocks.add(DocumentContent.Block.checklist(id,block.name,items,block.hideChecked));break;
                case IMAGE:case DRAWING:NoteImage image=block.image;if(!images.add(image.id)){image=new NoteImage(UUID.randomUUID().toString(),image.name,image.mime,image.width,image.height,image.data);images.add(image.id);}blocks.add(DocumentContent.Block.visual(id,block.name,block.kind,image));break;
                case DIAGRAM:blocks.add(DocumentContent.Block.diagram(id,block.name,block.diagram));break;
                case TABLE:blocks.add(DocumentContent.Block.table(id,block.name,block.table));break;
            }}
        }
        Note result=Note.create(now).edit("Merged notes","",now).properties(sources.get(0).color,Note.normalizeLabels(labels),now).folder(sources.get(0).folder,now).background(sources.get(0).background,now).withContent(new DocumentContent(blocks,DocumentContent.fromLegacy(sources.get(0)).writingLines),now);
        for(String voice:voices)result=result.voiceSource(voice,now);return result;
    }
}
