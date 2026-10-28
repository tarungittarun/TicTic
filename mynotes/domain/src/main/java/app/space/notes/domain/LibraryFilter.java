package app.space.notes.domain;
import java.util.*;
/** Immutable presentation snapshot; label selection is any-of and never moves files. */
public final class LibraryFilter {
 public final boolean allNotes,includeUnlabelled,grid;public final Set<String> labels;public final String sort;
 public LibraryFilter(boolean all,boolean unlabelled,Collection<String> labels,String sort,boolean grid){if(!Arrays.asList("edited","created","title").contains(sort))throw new IllegalArgumentException("Unknown sort");allNotes=all;includeUnlabelled=unlabelled;TreeSet<String> selected=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);selected.addAll(labels);this.labels=Collections.unmodifiableSet(selected);this.sort=sort;this.grid=grid;}
 public static LibraryFilter defaults(){return new LibraryFilter(true,true,Collections.emptySet(),"edited",true);}
 public boolean matches(Note n){if(allNotes)return true;if(n.labels.isEmpty())return includeUnlabelled;for(String label:n.labels)if(labels.contains(label))return true;return false;}
 public Comparator<Note> comparator(){Comparator<Note> order=sort.equals("title")?Comparator.comparing((Note n)->n.title,String.CASE_INSENSITIVE_ORDER):Comparator.comparingLong((Note n)->sort.equals("created")?n.createdAt:n.updatedAt).reversed();return Comparator.comparing((Note n)->!n.pinned).thenComparing(order).thenComparing(n->n.id);}
}
