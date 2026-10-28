package app.space.notes.runtime;
import android.view.*;
import android.widget.*;
import java.util.*;
import app.space.notes.domain.LibraryFilter;
/** Draft options with an explicit Apply. Horizontal movement switches tabs, vertical scroll stays native. */
final class LibraryFilterView extends LinearLayout {
 private final NotesDesign d;private final FrameLayout page;private final Button[] tabs=new Button[3];private final List<String> labels;private final Set<String> selected=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);private final Runnable manage;
 private CheckBox allBox,emptyBox;private final Map<String,CheckBox> boxes=new LinkedHashMap<>();private boolean binding;
 private int tab;private boolean all,unlabelled,grid;private String sort;
 LibraryFilterView(NotesDesign d,LibraryFilter initial,Collection<String> available,java.util.function.Consumer<LibraryFilter> apply,Runnable manage){super(d.context);this.d=d;this.manage=manage;labels=new ArrayList<>(available);selected.addAll(initial.labels);all=initial.allNotes;unlabelled=initial.includeUnlabelled;grid=initial.grid;sort=initial.sort;setOrientation(VERTICAL);
  LinearLayout row=new LinearLayout(getContext());String[] names={"View","Sort","Labels"};for(int i=0;i<3;i++){final int index=i;tabs[i]=d.textAction(names[i],()->show(index));tabs[i].setGravity(Gravity.CENTER);tabs[i].setPadding(0,0,0,0);row.addView(tabs[i],new LayoutParams(0,-2,1));}Button confirm=d.icon("Apply filters","check",()->apply.accept(new LibraryFilter(all,unlabelled,selected,sort,grid)));confirm.setBackground(d.ripple(d.surface,12));row.addView(confirm,new LayoutParams(d.dp(48),d.dp(48)));addView(row);
  page=new FrameLayout(getContext()){
   float x,y;boolean claimed;
   @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();claimed=false;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE && Math.abs(e.getX()-x)>d.dp(32) && Math.abs(e.getX()-x)>1.5f*Math.abs(e.getY()-y))claimed=true;return claimed;}
   @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_UP){float dx=e.getX()-x;if(Math.abs(dx)>d.dp(48))show(Math.max(0,Math.min(2,tab+(dx<0?1:-1))));claimed=false;performClick();}else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)claimed=false;return true;}
   @Override public boolean performClick(){super.performClick();return true;}
  };int pageHeight=Math.max(112,Math.min(220,(int)(getResources().getConfiguration().screenHeightDp*.5f)-92-(int)(52*getResources().getConfiguration().fontScale)));addView(page,new LayoutParams(-1,d.dp(pageHeight)));show(0);
 }
 private void show(int index){Diagnostics.record("ui.filter.tab","index="+index);tab=index;for(int i=0;i<3;i++){tabs[i].setSelected(i==tab);tabs[i].setBackground(d.ripple(i==tab?d.surface:android.graphics.Color.TRANSPARENT,12));}page.removeAllViews();ScrollView scroll=new ScrollView(getContext());LinearLayout content=new LinearLayout(getContext());content.setOrientation(VERTICAL);scroll.addView(content);page.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
  if(tab==0){content.addView(d.label("Display your notes",14,d.muted));RadioGroup group=new RadioGroup(getContext());RadioButton tiles=radio("Grid view",grid),list=radio("List view",!grid);group.addView(tiles);group.addView(list);group.setOnCheckedChangeListener((g,id)->grid=id==tiles.getId());content.addView(group);}
  else if(tab==1){content.addView(d.label("Pinned notes stay first",14,d.muted));RadioGroup group=new RadioGroup(getContext());String[] ids={"edited","created","title"},names={"Last edited — newest first","Created — newest first","Title — A to Z"};for(int i=0;i<ids.length;i++){RadioButton option=radio(names[i],sort.equals(ids[i]));option.setTag(ids[i]);group.addView(option);}group.setOnCheckedChangeListener((g,id)->sort=(String)g.findViewById(id).getTag());content.addView(group);}
  else{boxes.clear();allBox=check("All notes",all);content.addView(allBox);allBox.setOnCheckedChangeListener((v,on)->{if(binding)return;all=on;if(!on){selected.clear();unlabelled=false;}sync();});
   emptyBox=check("Without a label",all||unlabelled);content.addView(emptyBox);emptyBox.setOnCheckedChangeListener((v,on)->{if(binding)return;materialize();all=false;unlabelled=on;sync();});for(String name:labels){CheckBox option=check(name,all||selected.contains(name));content.addView(option);boxes.put(name,option);option.setOnCheckedChangeListener((v,on)->{if(binding)return;materialize();all=false;if(on)selected.add(name);else selected.remove(name);sync();});}LinearLayout actions=new LinearLayout(getContext());actions.addView(d.textAction("Select all",()->{all=false;selected.clear();selected.addAll(labels);unlabelled=true;sync();}),new LayoutParams(0,-2,1));actions.addView(d.textAction("Select none",()->{all=false;selected.clear();unlabelled=false;sync();}),new LayoutParams(0,-2,1));content.addView(actions);content.addView(d.label("Matches any selected label. None means no notes.",13,d.muted));content.addView(d.textAction("Manage labels",manage));}
 }
 private void sync(){binding=true;allBox.setChecked(all);emptyBox.setChecked(all||unlabelled);for(Map.Entry<String,CheckBox> item:boxes.entrySet())item.getValue().setChecked(all||selected.contains(item.getKey()));binding=false;}
 private void materialize(){if(all){selected.clear();selected.addAll(labels);unlabelled=true;}}
 private CheckBox check(String name,boolean value){CheckBox box=new CheckBox(getContext());box.setText(name);box.setTextColor(d.ink);box.setMinHeight(d.dp(48));box.setChecked(value);return box;}
 private RadioButton radio(String name,boolean value){RadioButton button=new RadioButton(getContext());button.setId(View.generateViewId());button.setText(name);button.setTextColor(d.ink);button.setMinHeight(d.dp(48));button.setChecked(value);return button;}
}
