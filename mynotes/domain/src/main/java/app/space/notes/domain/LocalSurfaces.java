package app.space.notes.domain;
import java.util.*;
/** Small bounded OS projections; never expose Trash/archive on a home-screen widget. */
public final class LocalSurfaces {
    private LocalSurfaces() { }
    public static List<Note> widget(List<Note> notes){List<Note> result=new ArrayList<>();for(Note n:notes)if(n.deletedAt==0 && !n.archived)result.add(n);result.sort(Comparator.comparing((Note n)->!n.pinned).thenComparing(Comparator.comparingLong((Note n)->n.updatedAt).reversed()).thenComparing(n->n.id));return new ArrayList<>(result.subList(0,Math.min(4,result.size())));}
    public static boolean scheduled(Note note,long now){return note.deletedAt==0 && note.reminderAt>now;}
}
