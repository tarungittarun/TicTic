package app.space.notes.domain;
import java.util.*;
/** Session-wide content/metadata snapshots. Shared image strings are counted once, never copied. */
public final class DocumentHistory {
    public static final int MAX_STEPS=50,MAX_CHARACTERS=12*1024*1024;
    private final Deque<Note> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    public static final class Cursor {public final String field;public final int anchor,focus;public Cursor(String field,int anchor,int focus){this.field=field;this.anchor=Math.max(0,anchor);this.focus=Math.max(0,focus);}}
    private final Map<Note,Cursor> cursors=new IdentityHashMap<>();
    public void rememberCursor(Note note,Cursor cursor){cursors.put(note,cursor);prune(note);}
    public Cursor cursor(Note note){return cursors.get(note);}
    private String group;private long last;private Note recordedAfter;
    public static final class State {
        public final List<Note> undo,redo;public final Map<Note,Cursor> cursors;
        public State(List<Note> undo,List<Note> redo,Map<Note,Cursor> cursors){this.undo=Collections.unmodifiableList(new ArrayList<>(undo));this.redo=Collections.unmodifiableList(new ArrayList<>(redo));this.cursors=Collections.unmodifiableMap(new IdentityHashMap<>(cursors));}
    }
    public State snapshot(){return new State(new ArrayList<>(undo),new ArrayList<>(redo),cursors);}
    public static DocumentHistory restore(String id,State state){if(state.undo.size()>MAX_STEPS || state.redo.size()>MAX_STEPS)throw new IllegalArgumentException("History step limit");DocumentHistory result=new DocumentHistory();for(Note n:state.undo){if(!id.equals(n.id))throw new IllegalArgumentException("History identity mismatch");result.undo.addLast(n);}for(Note n:state.redo){if(!id.equals(n.id))throw new IllegalArgumentException("History identity mismatch");result.redo.addLast(n);}result.cursors.putAll(state.cursors);result.trim();result.prune();return result;}
    /** Align native text-command history without recording an Undo as a new edit. */
    public boolean align(Note current,Note target,boolean forward){if(!current.id.equals(target.id))throw new IllegalArgumentException("History cannot cross documents");Deque<Note> source=forward?redo:undo;int steps=0;for(Iterator<Note> it=source.descendingIterator();it.hasNext();){Note candidate=it.next();steps++;if(NoteMerge.same(candidate.state(candidate.pinned,candidate.archived,candidate.deletedAt,target.updatedAt),target)){for(int i=0;i<steps;i++)current=forward?redo(current):undo(current);return true;}}undo.clear();redo.clear();cursors.clear();group=null;recordedAfter=target;return false;}

    public void record(Note before,Note after,String field,long now){
        if(before==after || after==recordedAfter)return;recordedAfter=after;
        if(!before.id.equals(after.id))throw new IllegalArgumentException("History cannot cross documents");
        boolean coalesce=field!=null && field.equals(group) && now>=last && now-last<=750 && !undo.isEmpty();
        if(!coalesce)undo.addLast(before);group=field;last=now;redo.clear();trim();prune(before,after);
    }
    public void invalidateRedo(){redo.clear();}
    public boolean canUndo(){return !undo.isEmpty();}public boolean canRedo(){return !redo.isEmpty();}
    public Note undo(Note current){if(undo.isEmpty())return current;redo.addLast(current);group=null;Note result=undo.removeLast();trim();prune(current,result);return result;}
    public Note redo(Note current){if(redo.isEmpty())return current;undo.addLast(current);group=null;Note result=redo.removeLast();trim();prune(current,result);return result;}
    private void prune(Note... current){Set<Note> retained=Collections.newSetFromMap(new IdentityHashMap<Note,Boolean>());retained.addAll(undo);retained.addAll(redo);Collections.addAll(retained,current);cursors.keySet().retainAll(retained);}
    private void trim(){while(undo.size()>MAX_STEPS)undo.removeFirst();while(redo.size()>MAX_STEPS)redo.removeFirst();while(retainedCharacters()>MAX_CHARACTERS){if(!undo.isEmpty())undo.removeFirst();else if(!redo.isEmpty())redo.removeFirst();else break;}}
    public long retainedCharacterCount(){return retainedCharacters();}
    private long retainedCharacters(){Set<String> strings=Collections.newSetFromMap(new IdentityHashMap<String,Boolean>());for(Note note:undo)collect(note,strings);for(Note note:redo)collect(note,strings);long count=0;for(String text:strings)count+=text.length();return count;}
    private static void collect(Note note,Set<String> strings){strings.add(note.title);strings.add(note.body);for(Note.Item item:note.items){strings.add(item.text);for(Note.Mark mark:item.marks)strings.add(mark.value);}for(Note.Mark mark:note.marks)strings.add(mark.value);for(Diagram.Node node:note.diagram.nodes)strings.add(node.text);strings.addAll(note.labels);strings.addAll(note.voiceSources);for(NoteImage image:note.images)strings.add(image.data);if(note.content!=null)for(DocumentContent.Block block:note.content.blocks){strings.add(block.name);if(block.text!=null){strings.add(block.text.text);for(Note.Mark mark:block.text.marks)strings.add(mark.value);}for(Note.Item row:block.items){strings.add(row.text);for(Note.Mark mark:row.marks)strings.add(mark.value);}if(block.table!=null)for(List<RichText> row:block.table.cells)for(RichText cell:row){strings.add(cell.text);for(Note.Mark mark:cell.marks)strings.add(mark.value);}if(block.diagram!=null)for(Diagram.Node node:block.diagram.nodes)strings.add(node.text);}}
}
