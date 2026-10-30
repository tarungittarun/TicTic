package app.space.notes.runtime;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import app.space.notes.domain.Note;
import java.util.*;
import java.util.function.BooleanSupplier;

/** In-memory editing history only. No text is logged or persisted outside the note.
 * Native composing spans are not copied; only app-owned formatting is restored. */
final class TextEditHistory implements TextWatcher {
 static final int MAX_STEPS=50,MAX_CHARACTERS=500000;
 static final class State {
  final String text;final List<Note.Mark> marks;final int anchor,focus;
  State(EditText field){text=field.getText().toString();marks=new ArrayList<>(TextStyles.read(field.getText()));anchor=Math.max(0,field.getSelectionStart());focus=Math.max(0,field.getSelectionEnd());}
  boolean same(State other){if(!text.equals(other.text) || marks.size()!=other.marks.size())return false;for(int i=0;i<marks.size();i++){Note.Mark a=marks.get(i),b=other.marks.get(i);if(!a.type.equals(b.type) || a.start!=b.start || a.end!=b.end)return false;}return true;}
 }
 private final EditText field;private final BooleanSupplier suspended;private final Runnable changed;
 private final Deque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
 private State before;private boolean applying,simple,lastSimple;private int lastCaret=-1,expectedCaret;private long lastAt;
 TextEditHistory(EditText field,BooleanSupplier suspended,Runnable changed){this.field=field;this.suspended=suspended;this.changed=changed;field.addTextChangedListener(this);}
 EditText field(){return field;}
 State snapshot(){return new State(field);}
 boolean canUndo(){return !undo.isEmpty();}
 boolean canRedo(){return !redo.isEmpty();}
 @Override public void beforeTextChanged(CharSequence text,int start,int count,int after){if(applying || suspended.getAsBoolean()){before=null;return;}before=snapshot();simple=(count==0 && after==1) || (count==1 && after==0);}
 @Override public void onTextChanged(CharSequence text,int start,int before,int count){expectedCaret=start+count;}
 @Override public void afterTextChanged(Editable text){if(before==null || applying || suspended.getAsBoolean())return;State value=before;before=null;record(value,simple);}
 void recordCommand(State value){record(value,false);}
 private void record(State value,boolean single){State current=snapshot();if(value.same(current))return;long now=android.os.SystemClock.uptimeMillis();boolean grouped=single && lastSimple && now-lastAt<750 && value.anchor==value.focus && value.focus==lastCaret && !undo.isEmpty();if(!grouped)undo.addLast(value);redo.clear();trim(undo);lastAt=now;lastSimple=single;lastCaret=single?expectedCaret:current.focus;changed.run();}
 private void trim(Deque<State> states){int chars=0;for(State value:states)chars+=value.text.length();while(!states.isEmpty() && (states.size()>MAX_STEPS || chars>MAX_CHARACTERS)){chars-=states.removeFirst().text.length();}}
 boolean undo(){return step(undo,redo);}
 boolean redo(){return step(redo,undo);}
 private boolean step(Deque<State> from,Deque<State> to){if(from.isEmpty())return false;State current=snapshot(),target=from.removeLast();to.addLast(current);trim(to);applying=true;try{android.view.inputmethod.BaseInputConnection.removeComposingSpans(field.getText());field.setText(target.text);TextStyles.write(field.getText(),target.marks);field.setSelection(Math.min(target.anchor,field.length()),Math.min(target.focus,field.length()));}finally{applying=false;}lastSimple=false;lastCaret=-1;changed.run();return true;}
}
