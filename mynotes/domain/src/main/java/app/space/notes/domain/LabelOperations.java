package app.space.notes.domain;
import java.util.*;
/** Global rename/remove includes archive and Trash; labels live with documents, not a side DB. */
public final class LabelOperations {
    private LabelOperations() { }
    public static List<Note> rename(List<Note> source,String old,String replacement,long now){
        List<String> normalized=Note.normalizeLabels(Arrays.asList(replacement));
        if(!replacement.isEmpty() && normalized.size()!=1)throw new IllegalArgumentException("Enter one nonempty label");
        String next=normalized.isEmpty()?"":normalized.get(0);List<Note> result=new ArrayList<>();
        for(Note note:source){List<String> labels=new ArrayList<>();boolean changed=false;
            for(String label:note.labels)if(label.equalsIgnoreCase(old)){changed=true;if(!next.isEmpty())labels.add(next);}else labels.add(label);
            result.add(changed?note.properties(note.color,labels,now):note);
        }return result;
    }
    public static List<String> add(List<String> existing,List<String> added){List<String> labels=new ArrayList<>(existing);labels.addAll(added);return Note.normalizeLabels(labels);}
}
