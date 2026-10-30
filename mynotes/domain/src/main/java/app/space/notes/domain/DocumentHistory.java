package app.space.notes.domain;
import java.util.*;
/** Session-wide content/metadata snapshots. Shared image strings are counted once, never copied. */
public final class DocumentHistory {
    public static final int MAX_STEPS=50,MAX_CHARACTERS=12*1024*1024;
    private final Deque<Note> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    public static final class Cursor {public final String field;public final int anchor,focus;public Cursor(String field,int anchor,int focus){this.field=field;this.anchor=Math.max(0,anchor);this.focus=Math.max(0,focus);}}
    private final Map<Note,Cursor> cursors=new IdentityHashMap<>();
    public void rememberCursor(Note note,Cursor cursor){cursors.put(note,cursor);}
    public Cursor cursor(Note note){return cursors.get(note);}
    private String group;private long last;
    public void record(Note before,Note after,String field,long now){
        if(before==after)return;
        if(!before.id.equals(after.id))throw new IllegalArgumentException("History cannot cross documents");
        boolean coalesce=field!=null && field.equals(group) && now>=last && now-last<=750 && !undo.isEmpty();
        if(!coalesce)undo.addLast(before);group=field;last=now;redo.clear();trim();Set<Note> retained=Collections.newSetFromMap(new IdentityHashMap<Note,Boolean>());retained.addAll(undo);retained.addAll(redo);retained.add(before);retained.add(after);cursors.keySet().retainAll(retained);
    }
    public boolean canUndo(){return !undo.isEmpty();}public boolean canRedo(){return !redo.isEmpty();}
    public Note undo(Note current){if(undo.isEmpty())return current;redo.addLast(current);group=null;Note result=undo.removeLast();trim();return result;}
    public Note redo(Note current){if(redo.isEmpty())return current;undo.addLast(current);group=null;Note result=redo.removeLast();trim();return result;}
    private void trim(){while(undo.size()>MAX_STEPS)undo.removeFirst();while(redo.size()>MAX_STEPS)redo.removeFirst();while(retainedCharacters()>MAX_CHARACTERS){if(!undo.isEmpty())undo.removeFirst();else if(!redo.isEmpty())redo.removeFirst();else break;}}
    private long retainedCharacters(){Set<String> strings=Collections.newSetFromMap(new IdentityHashMap<String,Boolean>());for(Note note:undo)collect(note,strings);for(Note note:redo)collect(note,strings);long count=0;for(String text:strings)count+=text.length();return count;}
    private static void collect(Note note,Set<String> strings){strings.add(note.title);strings.add(note.body);for(NoteImage image:note.images)strings.add(image.data);if(note.content!=null)for(DocumentContent.Block block:note.content.blocks){strings.add(block.name);if(block.text!=null){strings.add(block.text.text);for(Note.Mark mark:block.text.marks)strings.add(mark.value);}for(Note.Item row:block.items){strings.add(row.text);for(Note.Mark mark:row.marks)strings.add(mark.value);}if(block.table!=null)for(List<RichText> row:block.table.cells)for(RichText cell:row){strings.add(cell.text);for(Note.Mark mark:cell.marks)strings.add(mark.value);}if(block.diagram!=null)for(Diagram.Node node:block.diagram.nodes)strings.add(node.text);}}
}
