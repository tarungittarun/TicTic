package app.space.notes.domain;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Non-destructive restore: same IDs never overwrite a different existing document. */
public final class NoteMerge {
    private NoteMerge() { }
    public static final class Plan {
        public final List<Note> notes;
        public final int added, unchanged, conflicts;
        private Plan(List<Note> notes,int added,int unchanged,int conflicts) {
            this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
            this.added=added;this.unchanged=unchanged;this.conflicts=conflicts;
        }
    }
    public static boolean same(Note a,Note b) {
        return a.title.equals(b.title) && a.body.equals(b.body) && a.createdAt==b.createdAt
            && a.updatedAt==b.updatedAt && a.deletedAt==b.deletedAt && a.pinned==b.pinned
            && a.archived==b.archived && a.color==b.color && structure(a).equals(structure(b));
    }
    private static boolean plain(Note n){return !n.checklist && !n.hideChecked && n.items.isEmpty() && n.marks.isEmpty() && n.labels.isEmpty() && n.diagram.nodes.isEmpty();}
    private static String structure(Note n) {
        if(plain(n))return "";
        StringBuilder out=new StringBuilder(n.checklist+":"+n.hideChecked+":"+n.items.size()+"#");
        for(Note.Item item:n.items)out.append(item.id.length()).append(':').append(item.id).append(item.text.length()).append(':').append(item.text).append(':').append(item.checked).append(':').append(item.depth).append(';');
        out.append(n.marks.size()).append('#');
        for(Note.Mark mark:n.marks)out.append(mark.type).append(':').append(mark.start).append(':').append(mark.end).append(';');
        out.append(n.labels.size()).append('#');for(String label:n.labels)out.append(label.length()).append(':').append(label);
        if(!n.diagram.nodes.isEmpty())out.append("diagram:").append(n.diagram.fingerprint());
        return out.toString();
    }
    private static Note copy(Note n,String id) {
        return n.withId(id);
    }
    public static Plan preview(List<Note> existing,List<Note> incoming) {
        LinkedHashMap<String,Note> all=new LinkedHashMap<>();
        for(Note n:existing)if(all.put(n.id,n)!=null)throw new IllegalArgumentException("Duplicate current ID");
        Set<String> inputIds=new HashSet<>(); int added=0,unchanged=0,conflicts=0;
        for(Note n:incoming) {
            if(!inputIds.add(n.id))throw new IllegalArgumentException("Duplicate incoming ID");
            Note previous=all.get(n.id);
            if(previous==null) {all.put(n.id,n);added++;continue;}
            if(same(previous,n)) {unchanged++;continue;}
            // Derive a stable conflict-copy identity from all fields using length-delimited values.
            // Re-importing the same conflicting version does not create unlimited copies.
            List<String> values=new ArrayList<>(Arrays.asList(n.id,n.title,n.body,Long.toString(n.createdAt),Long.toString(n.updatedAt),
                Long.toString(n.deletedAt),Boolean.toString(n.pinned),Boolean.toString(n.archived),Integer.toString(n.color)));
            if(!plain(n))values.add(structure(n)); // Preserve 0.3.0 conflict IDs for plain notes.
            StringBuilder key=new StringBuilder("MyNotes restore conflict v1:");
            for(String value:values)key.append(value.length()).append(':').append(value);
            int suffix=0;String id;
            do {
                id=UUID.nameUUIDFromBytes((key.toString()+":"+suffix++).getBytes(StandardCharsets.UTF_8)).toString();
            } while(all.containsKey(id) && !same(all.get(id),n));
            if(all.containsKey(id)) {unchanged++;continue;}
            all.put(id,copy(n,id));added++;conflicts++;
        }
        return new Plan(new ArrayList<>(all.values()),added,unchanged,conflicts);
    }
}
