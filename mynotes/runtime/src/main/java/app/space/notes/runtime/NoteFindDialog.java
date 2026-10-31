package app.space.notes.runtime;
import android.app.AlertDialog;
import android.graphics.Rect;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import app.space.notes.domain.FindInNote;
import java.util.*;
import java.util.concurrent.*;
/** Search is read-only, bounded and off-thread. Jumping uses selection, never injected text marks. */
final class NoteFindDialog {
 private static void fields(View view,List<TextView> out){if(view instanceof EditText || view instanceof TextView && "find-caption".equals(view.getTag()))out.add((TextView)view);else if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)fields(group.getChildAt(i),out);}}
 static void show(NotesDesign design,View content,ExecutorService worker){
  android.content.Context context=content.getContext();List<TextView> targets=new ArrayList<>();fields(content,targets);List<String> snapshot=new ArrayList<>();for(TextView field:targets)snapshot.add(field.getText().toString());
  LinearLayout panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*context.getResources().getDisplayMetrics().density);panel.setPadding(pad,pad,pad,pad);
  EditText query=new EditText(context);design.entry(query,16);query.setSingleLine(true);query.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)});query.setHint(content instanceof DocumentEditorView?"Find in document":"Find in note");panel.addView(query,new LinearLayout.LayoutParams(-1,-2));
  TextView status=new TextView(context);status.setTextColor(design.ink);status.setText("Search title, writing and checklist items. Tap a result to jump to it.");panel.addView(status);
  ScrollView scroll=new ScrollView(context);LinearLayout results=new LinearLayout(context);results.setOrientation(LinearLayout.VERTICAL);scroll.addView(results);panel.addView(scroll,new LinearLayout.LayoutParams(-1,Math.max(pad*4,Math.min(pad*18,context.getResources().getDisplayMetrics().heightPixels/3))));
  AlertDialog dialog=new AlertDialog.Builder(context).setTitle(content instanceof DocumentEditorView?"Find in document":"Find in note").setView(panel).setNegativeButton("Close",null).create();final int[] generation={0};final Future<?>[] task={null};
  query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int after){}public void onTextChanged(CharSequence s,int a,int before,int count){}public void afterTextChanged(Editable value){int request=++generation[0];if(task[0]!=null)task[0].cancel(true);String needle=value.toString();results.removeAllViews();status.setText(needle.isEmpty()?"Enter text to find":"Searching…");if(needle.isEmpty())return;
   task[0]=worker.submit(()->{List<FindInNote.Match> matches=FindInNote.search(snapshot,needle);content.post(()->{if(!dialog.isShowing() || request!=generation[0])return;status.setText(matches.isEmpty()?"No matches":matches.size()>FindInNote.LIMIT?"100+ matches — refine your search":matches.size()+" matches");for(int i=0;i<Math.min(matches.size(),FindInNote.LIMIT);i++){FindInNote.Match match=matches.get(i);String text=snapshot.get(match.field);int start=Math.max(0,match.start-24),end=Math.min(text.length(),match.end+40);String hint=targets.get(match.field).getHint()==null?"Content":targets.get(match.field).getHint().toString();Button row=new Button(context);row.setTextColor(design.ink);row.setTextSize(16);row.setBackground(design.ripple(design.surface,12));row.setAllCaps(false);row.setMinHeight(pad*3);row.setText(hint+" · "+text.substring(start,end).replace('\n',' '));row.setContentDescription("Find result "+(i+1)+": "+row.getText());row.setOnClickListener(v->{TextView field=targets.get(match.field);android.os.IBinder token=query.getWindowToken();dialog.dismiss();if(!field.isAttachedToWindow() || !field.getText().toString().equals(snapshot.get(match.field)))return;((InputMethodManager)context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(token,0);if(field instanceof EditText){field.requestFocus();((EditText)field).setSelection(match.start,match.end);}field.postOnAnimation(()->{android.text.Layout layout=field.getLayout();if(layout==null)return;int line=layout.getLineForOffset(match.start);field.requestRectangleOnScreen(new Rect(0,layout.getLineTop(line)+field.getTotalPaddingTop(),field.getWidth(),layout.getLineBottom(line)+field.getTotalPaddingTop()),false);field.announceForAccessibility("Match selected: "+needle);});});results.addView(row,new LinearLayout.LayoutParams(-1,-2));}});});
  }});dialog.setOnDismissListener(d->{generation[0]++;if(task[0]!=null)task[0].cancel(true);});design.show(dialog);query.requestFocus();
 }
}
