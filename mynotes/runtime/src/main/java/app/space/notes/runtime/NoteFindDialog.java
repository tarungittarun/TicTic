package app.space.notes.runtime;
import android.graphics.Rect;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import app.space.notes.domain.FindInNote;
import java.util.*;
import java.util.concurrent.*;

/** In-editor Find mode. Historical class name retained; no dialog or separate editing screen. */
final class NoteFindDialog {
 private static final Map<View,Session> sessions=new WeakHashMap<>();
 private static void fields(View view,List<TextView> out){if(view instanceof EditText || view instanceof TextView && "find-caption".equals(view.getTag()))out.add((TextView)view);else if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)fields(group.getChildAt(i),out);}}
 static boolean open(View content){return content!=null && sessions.containsKey(content);}
 static boolean close(View content){Session current=sessions.get(content);if(current==null)return false;current.close();return true;}
 static void show(NotesDesign design,View content,ExecutorService worker){
  if(content==null || !content.isAttachedToWindow())return;
  Session old=sessions.get(content);if(old!=null){old.query.requestFocus();return;}
  LinearLayout owner=content instanceof DocumentEditorView?(LinearLayout)content:content.getParent() instanceof LinearLayout?(LinearLayout)content.getParent():null;
  if(owner==null)return;
  Session session=new Session(design,content,owner,worker);sessions.put(content,session);session.attach();
 }
 private static final class Session implements View.OnAttachStateChangeListener {
  final NotesDesign design;final View content;final LinearLayout owner,bar;final ExecutorService worker;
  final EditText query;final TextView count;final Button result,previous,next;final LinearLayout searchRow,statusRow;boolean compact,more;String fullCount="";
  final List<TextView> targets=new ArrayList<>();final List<String> snapshot=new ArrayList<>();
  List<FindInNote.Match> matches=Collections.emptyList();Future<?> task;int generation,index;boolean closed;
  final Map<View,Integer> chrome=new LinkedHashMap<>();
  Session(NotesDesign design,View content,LinearLayout owner,ExecutorService worker){
   this.design=design;this.content=content;this.owner=owner;this.worker=worker;
   fields(content,targets);for(TextView field:targets)snapshot.add(field.getText().toString());
   bar=new LinearLayout(content.getContext());bar.setOrientation(LinearLayout.VERTICAL);bar.setTag("inline-find");bar.setPadding(0,0,0,design.dp(4));bar.setBackground(design.shape(design.background,12));
   androidx.core.view.ViewCompat.setAccessibilityPaneTitle(bar,content instanceof DocumentEditorView?"Find in document":"Find in note");
   LinearLayout row=new LinearLayout(content.getContext());searchRow=row;row.setGravity(Gravity.CENTER_VERTICAL);
   query=new EditText(content.getContext());design.entry(query,16);query.setSingleLine(true);query.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)});query.setHint(content instanceof DocumentEditorView?"Find in document":"Find in note");query.setContentDescription(query.getHint());row.addView(query,new LinearLayout.LayoutParams(0,-2,1));
   previous=design.icon("Previous match","up",()->move(-1));next=design.icon("Next match","down",()->move(1));
   row.addView(previous,new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));row.addView(next,new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));row.addView(design.icon("Close find","close",this::close),new LinearLayout.LayoutParams(design.dp(48),design.dp(48)));bar.addView(row);
   LinearLayout status=new LinearLayout(content.getContext());statusRow=status;status.setGravity(Gravity.CENTER_VERTICAL);count=design.label("Find in this note",12,design.muted);status.addView(count,new LinearLayout.LayoutParams(-2,-2));
   result=design.textAction("Type a word to find",()->{if(!matches.isEmpty()){FindInNote.Match match=matches.get(index);close();jump(match);}});result.setTextSize(13);result.setPadding(design.dp(8),0,design.dp(4),0);result.setMaxLines(1);result.setEllipsize(TextUtils.TruncateAt.END);result.setMinWidth(0);result.setMinimumWidth(0);result.setMinHeight(design.dp(48));status.addView(result,new LinearLayout.LayoutParams(0,-2,1));bar.addView(status);
   previous.setEnabled(false);previous.setAlpha(.35f);next.setEnabled(false);next.setAlpha(.35f);result.setEnabled(false);
   query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN);
   query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){hideKeyboard();if(!matches.isEmpty())jump(matches.get(index));return true;}return false;});
   query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int after){}public void onTextChanged(CharSequence s,int a,int before,int n){}public void afterTextChanged(Editable value){search(value.toString());}});
  }
  void attach(){
   for(int i=0;i<owner.getChildCount();i++){View child=owner.getChildAt(i);if("editor-top-chrome".equals(child.getTag()) || "editor-dock".equals(child.getTag()) || "Editor tools".contentEquals(child.getContentDescription()==null?"":child.getContentDescription())){chrome.put(child,child.getVisibility());child.setVisibility(View.GONE);}}
   owner.addView(bar,Math.min(1,owner.getChildCount()),new LinearLayout.LayoutParams(-1,-2));content.addOnAttachStateChangeListener(this);bar.addOnAttachStateChangeListener(this);owner.addOnLayoutChangeListener(resizeListener);hideKeyboard();query.requestFocus();resize();owner.requestLayout();
  }
  final View.OnLayoutChangeListener resizeListener=(v,l,t,r,b,ol,ot,or,ob)->resize();
  void resize(){boolean small=owner.getHeight()>0 && owner.getHeight()<design.dp(200);if(small==compact)return;compact=small;if(count.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)count.getParent()).removeView(count);if(compact){count.setGravity(Gravity.CENTER);searchRow.addView(count,1,new LinearLayout.LayoutParams(design.dp(48),-2));statusRow.setVisibility(View.GONE);}else{count.setGravity(Gravity.START);statusRow.addView(count,0,new LinearLayout.LayoutParams(-2,-2));statusRow.setVisibility(View.VISIBLE);}updateCount();if(!matches.isEmpty())reveal(matches.get(index));}
  void updateCount(){count.setText(compact?matches.isEmpty()?"0":(index+1)+"/"+matches.size()+(more?"+":""):fullCount);}
  void hideKeyboard(){((InputMethodManager)content.getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken()!=null?query.getWindowToken():content.getWindowToken(),0);}
  void search(String needle){
   int request=++generation;if(task!=null)task.cancel(true);matches=Collections.emptyList();index=0;previous.setEnabled(false);previous.setAlpha(.35f);next.setEnabled(false);next.setAlpha(.35f);result.setEnabled(false);result.setText(needle.isEmpty()?"Type a word to find":"Searching…");fullCount="";updateCount();if(needle.isEmpty())return;
   List<String> copy=new ArrayList<>(snapshot);
   task=worker.submit(()->{List<FindInNote.Match> found=FindInNote.search(copy,needle);content.post(()->{if(closed || request!=generation)return;matches=found.size()>FindInNote.LIMIT?new ArrayList<>(found.subList(0,FindInNote.LIMIT)):found;index=0;more=found.size()>FindInNote.LIMIT;fullCount=found.isEmpty()?"No matches":more?"100+ matches":found.size()+(found.size()==1?" match":" matches");updateCount();render();});});
  }
  void render(){boolean any=!matches.isEmpty();previous.setEnabled(matches.size()>1);previous.setAlpha(matches.size()>1?1f:.35f);next.setEnabled(any);next.setAlpha(any?1f:.35f);result.setEnabled(any);if(!any){result.setText("Try another word");return;}FindInNote.Match match=matches.get(index);String text=snapshot.get(match.field),hint=targets.get(match.field).getHint()==null?"Content":targets.get(match.field).getHint().toString();if(hint.equals("Write your note…"))hint="Writing";result.setText(hint+" · "+text.substring(Math.max(0,match.start-24),Math.min(text.length(),match.end+40)).replace('\n',' '));result.setContentDescription("Find result "+(index+1)+": "+result.getText());next.setTooltipText("Next match · "+(index+1)+" of "+matches.size());reveal(match);}
  void move(int direction){if(matches.isEmpty())return;index=(index+direction+matches.size())%matches.size();render();jump(matches.get(index));fullCount=(index+1)+" of "+matches.size();updateCount();}
  void jump(FindInNote.Match match){
   TextView field=targets.get(match.field);if(!field.isAttachedToWindow())return;
   if(!field.getText().toString().equals(snapshot.get(match.field))){for(int i=0;i<targets.size();i++)snapshot.set(i,targets.get(i).getText().toString());search(query.getText().toString());return;}
   hideKeyboard();if(field instanceof EditText){field.setVisibility(View.VISIBLE);field.requestFocus();((EditText)field).setSelection(match.start,match.end);}
   reveal(match);
  }
  void reveal(FindInNote.Match match){TextView field=targets.get(match.field);field.postOnAnimation(()->{if(!field.isAttachedToWindow() || !field.getText().toString().equals(snapshot.get(match.field)))return;android.text.Layout layout=field.getLayout();if(layout==null)return;int line=layout.getLineForOffset(match.start);field.requestRectangleOnScreen(new Rect(0,layout.getLineTop(line)+field.getTotalPaddingTop(),field.getWidth(),layout.getLineBottom(line)+field.getTotalPaddingTop()),false);});}

  void close(){if(closed)return;closed=true;generation++;if(task!=null)task.cancel(true);sessions.remove(content);owner.removeOnLayoutChangeListener(resizeListener);content.removeOnAttachStateChangeListener(this);bar.removeOnAttachStateChangeListener(this);if(bar.getParent()==owner)owner.removeView(bar);for(Map.Entry<View,Integer> entry:chrome.entrySet())entry.getKey().setVisibility(entry.getValue());owner.requestLayout();}
  public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){close();}
 }
}
