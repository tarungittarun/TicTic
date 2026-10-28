package app.space.notes.domain;
import java.util.ArrayDeque;
import java.util.Deque;

/** Bounded immutable snapshots; stale histories never overwrite an independently changed diagram. */
public final class DiagramHistory {
 private final Deque<Diagram> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
 private String expected;
 private void sync(Diagram current){String value=current.fingerprint();if(expected!=null && !expected.equals(value)){undo.clear();redo.clear();}expected=value;}
 public void record(Diagram before,Diagram after){sync(before);if(before.fingerprint().equals(after.fingerprint()))return;undo.addLast(before);while(undo.size()>50)undo.removeFirst();redo.clear();expected=after.fingerprint();}
 public boolean canUndo(Diagram current){sync(current);return !undo.isEmpty();}
 public boolean canRedo(Diagram current){sync(current);return !redo.isEmpty();}
 public Diagram undo(Diagram current){sync(current);if(undo.isEmpty())return current;redo.addLast(current);Diagram value=undo.removeLast();expected=value.fingerprint();return value;}
 public Diagram redo(Diagram current){sync(current);if(redo.isEmpty())return current;undo.addLast(current);Diagram value=redo.removeLast();expected=value.fingerprint();return value;}
}
