package app.space.notes.domain;
import java.util.*;
public final class NoteOperations {
    private NoteOperations() { }
    public static Note duplicate(Note source,long now){return new Note(UUID.randomUUID().toString(),source.title,source.body,now,now,0,source.pinned,false,source.color,source.checklist,source.hideChecked,source.items,source.marks,source.labels,source.diagram);}
    public static Note merge(List<Note> sources,long now){
        if(sources.size()<2)throw new IllegalArgumentException("Select at least two notes");
        StringBuilder body=new StringBuilder();List<String> tags=new ArrayList<>();
        for(Note n:sources){if(!n.diagram.nodes.isEmpty())throw new IllegalArgumentException("Diagram notes cannot be flattened by Merge. Copy or export them instead.");
            if(n.deletedAt>0)throw new IllegalArgumentException("Restore trashed notes before merging");
            if(body.length()>0)body.append("\n\n────────\n\n");
            if(!n.title.isEmpty())body.append(n.title).append('\n');body.append(n.plainText());tags.addAll(n.labels);
        }
        return Note.create(now).edit("Merged notes",body.toString(),now).properties(sources.get(0).color,Note.normalizeLabels(tags),now);
    }
}
