package app.space.notes.runtime;

import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import app.space.notes.domain.*;
import java.util.*;

/** Canonical block editor: edits the authoritative model, never its legacy text projection. */
final class DocumentEditorView extends LinearLayout {
    interface Host {
        Note current();void change(Note next,String group);void undo(boolean redo);boolean canUndo(boolean redo);
        void cursor(String field,int anchor,int focus);
        void back();void visual(DocumentContent.Block block);void diagram(DocumentContent.Block block);
        void addVisual(boolean drawing);void action(String action);
    }
    private final Host host;private final NotesDesign design;private final Bundle state;
    private final Map<String,EditText> fields=new LinkedHashMap<>();
    private EditText selected;private String selectedBlock,selectedRow,focusAfterRender;
    private final TextView saveStatus;TextView saveStatus(){return saveStatus;}
    private LinearLayout topControls,documentControls;private View formatControls;private int topHeight,documentHeight,statusHeight;
    private int dragDirection;
    private final Runnable dragTick=new Runnable(){public void run(){if(dragDirection!=0 && scroll!=null && isAttachedToWindow()){scroll.scrollBy(0,dragDirection*dp(16));postDelayed(this,50);}}};
    private void stopDragScroll(){dragDirection=0;removeCallbacks(dragTick);}
    @Override protected void onDetachedFromWindow(){stopDragScroll();super.onDetachedFromWindow();}
    private void trackDrag(DragEvent event,View target){
        if(event.getAction()==DragEvent.ACTION_DRAG_ENDED || event.getAction()==DragEvent.ACTION_DROP){stopDragScroll();return;}
        if(readOnly() || event.getAction()!=DragEvent.ACTION_DRAG_LOCATION)return;int[] at=new int[2],viewport=new int[2];target.getLocationOnScreen(at);scroll.getLocationOnScreen(viewport);float y=at[1]+event.getY()-viewport[1];int direction=y<dp(48)?-1:y>scroll.getHeight()-dp(48)?1:0;if(direction!=dragDirection){stopDragScroll();dragDirection=direction;if(direction!=0)post(dragTick);}
    }
    private ScrollView scroll;private Button undo,redo;private boolean building,formattingChange,paragraphBoundaryChange,insertionUsesCaret=true,selectionLocked,restoringSelection;
    static final class BlockDrag {final String id;BlockDrag(String id){this.id=id;}}
    static final class RowDrag {final String block,item;RowDrag(String block,String item){this.block=block;this.item=item;}}
    static boolean internalDrag(DragEvent event){return event.getLocalState() instanceof BlockDrag || event.getLocalState() instanceof RowDrag;}
    DocumentEditorView(Context context,NotesDesign design,Host host,Bundle state){super(context);this.host=host;this.design=design;this.state=state;saveStatus=design.label("Saved · on this device",12,design.muted);setOrientation(VERTICAL);addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->post(this::adaptViewport));render();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private boolean readOnly(){return host.current().deletedAt>0;}
    void saveState(){if(selectionLocked)return;for(Map.Entry<String,EditText> entry:fields.entrySet())if(entry.getValue().hasFocus()){state.putString("note",host.current().id);state.putString("field",entry.getKey());state.putInt("start",entry.getValue().getSelectionStart());state.putInt("end",entry.getValue().getSelectionEnd());}if(scroll!=null)state.putInt("scroll",scroll.getScrollY());}
    private Button button(String name,Runnable action){Button b=design.textAction(name,()->{try{saveState();selectionLocked=true;action.run();}catch(IllegalArgumentException|IllegalStateException failure){selectionLocked=false;message(failure.getMessage());}});b.setAllCaps(false);return b;}
    private void message(String text){android.widget.Toast.makeText(getContext(),text,Toast.LENGTH_LONG).show();}
    private LinearLayout row(LinearLayout parent){LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);parent.addView(row,new LayoutParams(-1,-2));return row;}
    private void dragHandle(Button handle,Object source,String mime){
        final float[] down=new float[2];final boolean[] active={false},moving={false};int slop=android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();
        handle.setOnTouchListener((view,event)->{switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:if(view.getParent()!=null)view.getParent().requestDisallowInterceptTouchEvent(true);down[0]=event.getX();down[1]=event.getY();active[0]=true;moving[0]=false;handle.setPressed(true);return true;
            case MotionEvent.ACTION_MOVE:if(active[0] && !moving[0] && Math.hypot(event.getX()-down[0],event.getY()-down[1])>slop){moving[0]=true;handle.setPressed(false);if(!view.startDragAndDrop(new ClipData("MyNotes content",new String[]{mime},new ClipData.Item("")),new DragShadowBuilder(view),source,0))message("Drag could not start. Try the handle again.");}return true;
            case MotionEvent.ACTION_UP:if(view.getParent()!=null)view.getParent().requestDisallowInterceptTouchEvent(false);handle.setPressed(false);if(active[0] && !moving[0])handle.performClick();active[0]=false;return true;
            case MotionEvent.ACTION_CANCEL:if(view.getParent()!=null)view.getParent().requestDisallowInterceptTouchEvent(false);handle.setPressed(false);active[0]=false;return true;
            default:return true;
        }});
    }
    private void addButton(LinearLayout row,String label,Runnable action){row.addView(button(label,action),new LayoutParams(-2,-2));}
    private void horizontally(LinearLayout parent,java.util.function.Consumer<LinearLayout> populate){HorizontalScrollView horizontal=new HorizontalScrollView(getContext());LinearLayout row=new LinearLayout(getContext());horizontal.addView(row);parent.addView(horizontal,new LayoutParams(-1,-2));populate.accept(row);formatControls=horizontal;}
    private void render(){
        stopDragScroll();saveState();building=true;removeAllViews();fields.clear();selected=null;selectedBlock=null;selectedRow=null;
        Note note=host.current();LinearLayout top=row(this);topControls=top;addButton(top,"Back",host::back);undo=button("Undo",()->host.undo(false));redo=button("Redo",()->host.undo(true));top.addView(undo);top.addView(redo);undo.setEnabled(!readOnly() && host.canUndo(false));redo.setEnabled(!readOnly() && host.canUndo(true));
        scroll=new ScrollView(getContext());scroll.setFillViewport(true);scroll.setOnDragListener((v,event)->{if(!internalDrag(event))return false;trackDrag(event,v);return true;});scroll.setBackground(new NoteBackground(note.background,note.color==0?design.surface:NotesBoard.colour(note.color,design.dark),design.dark,getResources().getDisplayMetrics().density,true));
        LinearLayout content=new LinearLayout(getContext());content.setOrientation(VERTICAL);content.setPadding(dp(12),0,dp(12),dp(16));scroll.addView(content);addView(scroll,new LayoutParams(-1,0,1));
        EditText title=field("title",new RichText(note.title,Collections.emptyList()),(text)->host.change(host.current().title(text.text,System.currentTimeMillis()),"title"),null,null);title.setHint("Title");title.setTextSize(26);content.addView(title,new LayoutParams(-1,-2));
        for(DocumentContent.Block block:note.content.blocks)renderBlock(content,block);
        if(!readOnly()){
            addButton(row(content),"+ Text",()->{DocumentContent.Block block=DocumentContent.Block.text(UUID.randomUUID().toString(),new RichText("",Collections.emptyList()));focusInserted(block);content(host.current().content.insert(host.current().content.blocks.size(),block),true,null);});
            horizontally(this,tools->{addButton(tools,"Insert",this::insertMenu);addButton(tools,"Format",this::formatMenu);addButton(tools,"Labels",()->host.action("Labels"));addButton(tools,"Colour",()->host.action("Colour"));addButton(tools,"Selection → checklist",this::convertSelection);Button guides=button(host.current().content.writingLines?"Lines · On":"Lines · Off",()->host.action("lines"));guides.setContentDescription("Writing lines");tools.addView(guides);});
        }
        addView(saveStatus,new LayoutParams(-1,-2));LinearLayout bottom=row(this);documentControls=bottom;addButton(bottom,"Document",this::documentMenu);
        if(focusAfterRender!=null){state.putString("note",note.id);state.putString("field",focusAfterRender);state.putInt("start",0);state.putInt("end",0);state.putBoolean("showKeyboard",true);focusAfterRender=null;}building=false;
        final String restoreNote=state.getString("note"),restoreField=state.getString("field");final int restoreStart=state.getInt("start"),restoreEnd=state.getInt("end");
        post(()->{if(!isAttachedToWindow() || !note.id.equals(restoreNote))return;EditText focus=fields.get(restoreField);if(focus!=null){restoringSelection=true;try{int start=Math.max(0,Math.min(focus.length(),restoreStart)),end=Math.max(0,Math.min(focus.length(),restoreEnd));focus.setSelection(start,end);focus.requestFocus();focus.setSelection(start,end);selectionLocked=true;postOnAnimation(this::restoreAfterDialog);}finally{restoringSelection=false;}}scroll.scrollTo(0,state.getInt("scroll"));if(state.getBoolean("showKeyboard") && focus!=null){state.putBoolean("showKeyboard",false);focus.postOnAnimation(()->((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(focus,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));}adaptViewport();});
    }
    private NotesController.WritingField field(String key,RichText value,java.util.function.Consumer<RichText> changed,String block,String item){
        NotesController.WritingField input=new NotesController.WritingField(getContext());design.entry(input,18);input.setLinkTextColor(design.accent);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setGravity(Gravity.TOP);input.setText(value.text);TextStyles.write(input.getText(),value.marks,input.getResources().getDisplayMetrics());input.setEnabled(!readOnly());input.setTag(key);String description="Title";if(block!=null){DocumentContent.Block section=host.current().content.block(block);description=section.name.isEmpty()?(item==null?"Text section":"List item"):section.name;if(key.startsWith("cell:")){String[] parts=key.split(":");description+=", row "+(Integer.parseInt(parts[parts.length-2])+1)+", column "+(Integer.parseInt(parts[parts.length-1])+1);}}input.setContentDescription(description);input.setHint(key.equals("title")?"Title":key.startsWith("cell:")?"Cell":item!=null?"List item":"Write text…");fields.put(key,input);
        input.ruling(block!=null && host.current().content.writingLines,design.outline);input.undoText=()->{host.undo(false);return true;};input.redoText=()->{host.undo(true);return true;};
        input.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN)selectionLocked=false;return false;});
        input.setOnFocusChangeListener((v,focus)->{if(focus){selected=input;selectedBlock=block;selectedRow=item;post(this::adaptViewport);}});
        final RichText[] lastGood={value};final boolean[] editingText={false},headingAtEdit={false};
        final int[] pendingNumber={-1},pendingStart={-1},lastNewline={-1},insertedEnd={0};final String[] numberAtEdit={null};
        input.selectionChanged=()->{if(!building && !editingText[0] && !restoringSelection){host.cursor(key,input.getSelectionStart(),input.getSelectionEnd());if(!selectionLocked && input.hasFocus() && input.hasWindowFocus())saveState();}};
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){
                editingText[0]=true;if(!formattingChange)selectionLocked=false;paragraphBoundaryChange=false;for(int i=start;i<Math.min(s.length(),start+count);i++)if(s.charAt(i)=='\n')paragraphBoundaryChange=true;numberAtEdit[0]=null;headingAtEdit[0]=false;
                if(!building){host.cursor(key,input.getSelectionStart(),input.getSelectionEnd());for(Note.Mark mark:TextStyles.read(input.getText()))if(mark.start<=start && mark.end>=start){if(mark.type.equals("number"))numberAtEdit[0]=mark.value;if(mark.type.equals("h1") || mark.type.equals("h2"))headingAtEdit[0]=true;}}
                if(start<pendingStart[0]){pendingNumber[0]=-1;pendingStart[0]=-1;}
            }
            public void onTextChanged(CharSequence s,int start,int before,int count){lastNewline[0]=-1;insertedEnd[0]=Math.min(s.length(),start+count);if(!formattingChange)for(int i=start;i<insertedEnd[0];i++)if(s.charAt(i)=='\n'){lastNewline[0]=i;paragraphBoundaryChange=true;}}
            public void afterTextChanged(Editable text){
                if(building || readOnly()){editingText[0]=false;return;}
                try{
                    List<Note.Mark> marks=TextStyles.read(text);
                    if(!formattingChange){
                        if(lastNewline[0]>=0 && headingAtEdit[0])marks=TextFormatting.paragraph(marks,text.toString(),"text",lastNewline[0]+1,insertedEnd[0]);
                        marks=ParagraphNumbering.normalize(marks,text.toString());
                        if(lastNewline[0]>=0 && numberAtEdit[0]!=null && lastNewline[0]+1==text.length()){
                            int value=-1;for(Note.Mark mark:marks)if(mark.type.equals("number") && mark.end==lastNewline[0])value=Integer.parseInt(mark.value)+1;pendingNumber[0]=value>0 && value<=9999?value:-1;pendingStart[0]=text.length();
                        }else if(pendingNumber[0]>0 && pendingStart[0]>=0 && text.length()>pendingStart[0]){
                            int end=text.toString().indexOf('\n',pendingStart[0]);if(end<0)end=text.length();if(end>pendingStart[0])marks.add(new Note.Mark("number",pendingStart[0],end,Integer.toString(pendingNumber[0])));pendingNumber[0]=-1;pendingStart[0]=-1;
                        }
                        TextStyles.write(text,marks,input.getResources().getDisplayMetrics());
                    }
                    RichText rich=new RichText(text.toString(),TextStyles.read(text));if(!lastGood[0].same(rich)){changed.accept(rich);lastGood[0]=rich;}refreshHistory();
                }catch(IllegalArgumentException|IllegalStateException failure){building=true;try{input.setText(lastGood[0].text);TextStyles.write(input.getText(),lastGood[0].marks,input.getResources().getDisplayMetrics());input.setSelection(input.length());}finally{building=false;}input.setError("Change not applied: "+failure.getMessage());message("Change was not applied; previous content is retained.");}
                finally{editingText[0]=false;paragraphBoundaryChange=false;if(!building)host.cursor(key,input.getSelectionStart(),input.getSelectionEnd());}
            }
        });return input;
    }

    private void refreshHistory(){undo.setEnabled(!readOnly() && host.canUndo(false));redo.setEnabled(!readOnly() && host.canUndo(true));}
    private void content(DocumentContent next,boolean structural,String group){host.change(host.current().withContent(next,System.currentTimeMillis()),formattingChange || paragraphBoundaryChange?null:group);if(structural)render();else refreshHistory();}
    private String label(DocumentContent.Block block){if(!block.name.isEmpty())return block.name;String kind=block.kind.name().toLowerCase(java.util.Locale.ROOT);return Character.toUpperCase(kind.charAt(0))+kind.substring(1);}
    private void adaptViewport(){
        if(getHeight()<=0 || topControls==null || documentControls==null)return;
        topHeight=Math.max(topHeight,topControls.getMeasuredHeight());documentHeight=Math.max(documentHeight,documentControls.getMeasuredHeight());statusHeight=Math.max(statusHeight,saveStatus.getMeasuredHeight());
        int format=formatControls==null?0:formatControls.getMeasuredHeight(),line=selected==null?dp(24):selected.getLineHeight();boolean compact=getHeight()<dp(240) || getHeight()-topHeight-documentHeight-statusHeight-format<Math.max(dp(80),line*2);
        int visibility=compact?GONE:VISIBLE;boolean changed=topControls.getVisibility()!=visibility || documentControls.getVisibility()!=visibility;
        topControls.setVisibility(visibility);documentControls.setVisibility(visibility);if(!saveStatus.getText().toString().contains("retry"))saveStatus.setVisibility(visibility);
        EditText title=fields.get("title");if(title!=null){int show=compact && !title.hasFocus()?GONE:VISIBLE;changed|=title.getVisibility()!=show;title.setVisibility(show);}
        if(changed)requestLayout();if(selected!=null && selected.hasFocus())selected.post(()->{if(selected!=null && selected.isAttachedToWindow()){android.graphics.Rect caret=new android.graphics.Rect();selected.getFocusedRect(caret);selected.requestRectangleOnScreen(caret,true);}});
    }
    private void renderBlock(LinearLayout parent,DocumentContent.Block block){
        LinearLayout card=new LinearLayout(getContext());card.setOrientation(VERTICAL);card.setPadding(0,dp(4),0,dp(8));parent.addView(card,new LayoutParams(-1,-2));
        LinearLayout header=row(card);if(!readOnly()){Button handle=button("⋮⋮",()->blockMenu(block.id));handle.setTag("block-handle:"+block.id);handle.setContentDescription("Reorder "+(block.name.isEmpty()?block.kind.name().toLowerCase(java.util.Locale.ROOT):block.name));handle.setTooltipText("Drag content; use its name for insert, rename and remove controls");dragHandle(handle,new BlockDrag(block.id),"application/vnd.mynotes.document-block");header.addView(handle,new LayoutParams(dp(48),dp(48)));addButton(header,label(block),()->blockMenu(block.id));}
        card.setOnDragListener((v,event)->{if(internalDrag(event))trackDrag(event,v);if(readOnly() || !(event.getLocalState() instanceof BlockDrag))return false;if(event.getAction()==DragEvent.ACTION_DROP){String source=((BlockDrag)event.getLocalState()).id;int at=host.current().content.blocks.indexOf(host.current().content.block(block.id));content(host.current().content.move(source,at+(event.getY()>v.getHeight()/2?1:0)),true,null);}return true;});
        switch(block.kind){
            case TEXT:card.addView(field("text:"+block.id,block.text,rich->{DocumentContent current=host.current().content;content(current.replace(block.id,DocumentContent.Block.text(block.id,rich)),false,"text:"+block.id);},block.id,null),new LayoutParams(-1,-2));break;
            case CHECKLIST:renderChecklist(card,block);break;
            case IMAGE:case DRAWING:card.addView(new NoteImageView(getContext(),block.image),new LayoutParams(-1,dp(220)));if(!readOnly())addButton(row(card),block.kind==DocumentContent.Kind.DRAWING?"Edit drawing":"Edit image",()->host.visual(host.current().content.block(block.id)));break;
            case DIAGRAM:addButton(row(card),(readOnly()?"View ":"Edit ")+label(block)+" · "+block.diagram.nodes.size()+" shapes",()->host.diagram(host.current().content.block(block.id)));break;
            case TABLE:renderTable(card,block);break;
        }
    }

    private void renderChecklist(LinearLayout card,DocumentContent.Block block){
        for(Note.Item item:block.items){if(block.hideChecked && item.checked)continue;LinearLayout line=row(card);line.setPadding(dp(item.depth*16),0,0,0);
            if(!readOnly()){Button handle=button("⋮⋮",()->rowMenu(block.id,item.id));handle.setTag("row-handle:"+item.id);handle.setContentDescription("Reorder list item "+(block.items.indexOf(item)+1));dragHandle(handle,new RowDrag(block.id,item.id),"application/vnd.mynotes.checklist-item");line.addView(handle,new LayoutParams(dp(48),dp(48)));}
            CheckBox checked=new CheckBox(getContext());checked.setChecked(item.checked);checked.setEnabled(!readOnly());checked.setContentDescription("Checked · "+item.text);checked.setOnCheckedChangeListener((v,on)->changeRow(block.id,item.id,old->old.change(old.text,on,old.depth),block.hideChecked,null));line.addView(checked);
            line.addView(field("row:"+item.id,new RichText(item.text,item.marks),rich->changeRow(block.id,item.id,old->new Note.Item(old.id,rich.text,old.checked,old.depth,rich.marks),false,"row:"+item.id),block.id,item.id),new LayoutParams(0,-2,1));
            line.setOnDragListener((v,event)->{if(internalDrag(event))trackDrag(event,v);if(readOnly() || !(event.getLocalState() instanceof RowDrag))return false;RowDrag drag=(RowDrag)event.getLocalState();if(!block.id.equals(drag.block))return false;if(event.getAction()==DragEvent.ACTION_DROP){DocumentContent current=host.current().content;DocumentContent.Block list=current.block(block.id);int at=0;for(int i=0;i<list.items.size();i++)if(list.items.get(i).id.equals(item.id))at=i;List<Note.Item> moved=ChecklistOrder.move(list.items,drag.item,at+(event.getY()>v.getHeight()/2?1:0));content(current.replace(block.id,DocumentContent.Block.checklist(block.id,moved,list.hideChecked)),true,null);}return true;});
        }
        if(!readOnly()){addButton(row(card),"+ List item",()->{DocumentContent current=host.current().content;DocumentContent.Block list=current.block(block.id);List<Note.Item> rows=new ArrayList<>(list.items);rows.add(new Note.Item(UUID.randomUUID().toString(),"",false,0));content(current.replace(block.id,DocumentContent.Block.checklist(block.id,rows,list.hideChecked)),true,null);});addButton(row(card),block.hideChecked?"Show checked":"Hide checked",()->content(host.current().content.replace(block.id,DocumentContent.Block.checklist(block.id,host.current().content.block(block.id).items,!host.current().content.block(block.id).hideChecked)),true,null));}
    }
    private void changeRow(String block,String item,java.util.function.Function<Note.Item,Note.Item> change,boolean structural,String group){DocumentContent current=host.current().content;DocumentContent.Block list=current.block(block);List<Note.Item> rows=new ArrayList<>(list.items);for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(item))rows.set(i,change.apply(rows.get(i)));content(current.replace(block,DocumentContent.Block.checklist(block,rows,list.hideChecked)),structural,group);}
    private void rowMenu(String block,String item){selectedBlock=block;selectedRow=item;selected=fields.get("row:"+item);if(selected!=null)selected.requestFocus();choose("List item",new String[]{"Indent","Outdent","Remove item"},choice->{DocumentContent current=host.current().content;DocumentContent.Block list=current.block(block);if(choice==2){confirm("Remove this list item?",()->{List<Note.Item> rows=new ArrayList<>(list.items);rows.removeIf(row->row.id.equals(item));content(current.replace(block,DocumentContent.Block.checklist(block,rows,list.hideChecked)),true,null);});}else changeRow(block,item,row->row.change(row.text,row.checked,Math.max(0,Math.min(3,row.depth+(choice==0?1:-1)))),true,null);});}

    private void renderTable(LinearLayout card,DocumentContent.Block block){
        HorizontalScrollView horizontal=new HorizontalScrollView(getContext());LinearLayout table=new LinearLayout(getContext());table.setOrientation(VERTICAL);horizontal.addView(table);card.addView(horizontal);
        for(int r=0;r<block.table.rows.size();r++){LinearLayout row=row(table);for(int c=0;c<block.table.columns;c++){final int ri=r,ci=c;EditText cell=field("cell:"+block.id+":"+r+":"+c,block.table.cells.get(r).get(c),rich->{DocumentContent current=host.current().content;DocumentContent.Block old=current.block(block.id);content(current.replace(block.id,DocumentContent.Block.table(block.id,old.name,old.table.withRichCell(ri,ci,rich))),false,"cell:"+block.id+":"+ri+":"+ci);},block.id,null);cell.setMinWidth(dp(140));cell.setBackground(design.shape(design.surface,3));row.addView(cell,new LayoutParams(dp(160),-2));}}
        if(!readOnly())addButton(row(card),"Table rows and columns",()->choose("Table structure",new String[]{"Insert row","Insert column","Delete row","Delete column"},action->{DocumentContent.Block current=host.current().content.block(block.id);int count=action%2==0?current.table.rows.size():current.table.columns;String[] positions=new String[action<2?count+1:count];for(int i=0;i<positions.length;i++)positions[i]=(action%2==0?"Row ":"Column ")+(i+1);choose(action<2?"Insert before position":"Delete position",positions,position->{Runnable apply=()->{DocumentContent all=host.current().content;DocumentContent.Block old=all.block(block.id);TextTable next=action==0?old.table.insertRow(position):action==1?old.table.insertColumn(position):action==2?old.table.deleteRow(position):old.table.deleteColumn(position);content(all.replace(block.id,DocumentContent.Block.table(block.id,old.name,next)),true,null);};if(action>=2)confirm("Delete this row/column and its cells? Undo remains available.",apply);else apply.run();});}));
    }
    private void focusInserted(DocumentContent.Block block){if(block.text!=null)focusAfterRender="text:"+block.id;else if(block.table!=null)focusAfterRender="cell:"+block.id+":0:0";else if(!block.items.isEmpty())focusAfterRender="row:"+block.items.get(0).id;}
    private void insert(DocumentContent.Block block){focusInserted(block);DocumentContent current=host.current().content;int edge=current.blocks.size();if(selectedBlock!=null){for(int i=0;i<current.blocks.size();i++)if(current.blocks.get(i).id.equals(selectedBlock))edge=i+1;if(insertionUsesCaret && selected!=null && selectedRow==null && current.block(selectedBlock).text!=null && selected.getSelectionEnd()>=0){DocumentContent next=current.insertAtText(selectedBlock,Math.max(selected.getSelectionStart(),selected.getSelectionEnd()),block);content(next,true,null);return;}}content(current.insert(edge,block),true,null);}
    String insertionAnchor(){return selectedBlock;}
    int insertionOffset(){return insertionUsesCaret && selected!=null && selectedRow==null && selectedBlock!=null && host.current().content.block(selectedBlock).text!=null?Math.max(selected.getSelectionStart(),selected.getSelectionEnd()):-1;}
    private void insertMenu(){insertMenu(true);}
    private void insertMenu(boolean atCaret){insertionUsesCaret=atCaret;choose(atCaret?"Insert at cursor":"Insert after section",new String[]{"Text","Checklist","Table","Image","Drawing","Diagram","Reminder"},choice->{String id=UUID.randomUUID().toString();DocumentContent current=host.current().content;switch(choice){case 0:insert(DocumentContent.Block.text(id,new RichText("",Collections.emptyList())));break;case 1:insert(DocumentContent.Block.checklist(id,Arrays.asList(new Note.Item(UUID.randomUUID().toString(),"",false,0)),false));break;case 2:insert(DocumentContent.Block.table(id,current.nextVisualName(DocumentContent.Kind.TABLE),TextTable.empty(2,2)));break;case 3:host.addVisual(false);break;case 4:host.addVisual(true);break;case 5:DocumentContent.Block diagram=DocumentContent.Block.diagram(id,current.nextVisualName(DocumentContent.Kind.DIAGRAM),Diagram.empty());insert(diagram);host.diagram(diagram);break;case 6:host.action("Reminder");break;}});}
    private void blockMenu(String id){if(!id.equals(selectedBlock)){selected=null;selectedRow=null;}selectedBlock=id;DocumentContent.Block block=host.current().content.block(id);List<String> options=new ArrayList<>(Arrays.asList("Insert after","Text before","Remove section"));if(block.kind!=DocumentContent.Kind.TEXT && block.kind!=DocumentContent.Kind.CHECKLIST)options.add("Rename");choose("Content controls",options.toArray(new String[0]),choice->{if(choice==0)insertMenu(false);else if(choice==1){DocumentContent current=host.current().content;content(current.insert(current.blocks.indexOf(current.block(id)),DocumentContent.Block.text(UUID.randomUUID().toString(),new RichText("",Collections.emptyList()))),true,null);}else if(choice==2)confirm("Remove this section and its content? Undo remains available.",()->content(host.current().content.remove(id),true,null));else prompt("Content name",block.name,name->content(host.current().content.replace(id,host.current().content.block(id).renamed(name)),true,null));});}

    private void convertSelection(){if(selected==null || selectedBlock==null || selectedRow!=null || host.current().content.block(selectedBlock).text==null){message("Select text in a text section first.");return;}int start=Math.min(selected.getSelectionStart(),selected.getSelectionEnd()),end=Math.max(selected.getSelectionStart(),selected.getSelectionEnd());content(host.current().content.selectionToChecklist(selectedBlock,start,end),true,null);}
    private void formatMenu(){if(selected==null || selectedBlock==null){message("Select text or place the cursor in a text section first.");return;}
        choose("Text format",new String[]{"Bold","Italic","Underline","Strike","Heading 1","Heading 2","Normal paragraph","Font","Size (8–72 sp)","Hyperlink","Open link","Number paragraphs","Clear selection formatting"},choice->{String[] types={"bold","italic","underline","strike","h1","h2","text"};if(choice<7)applyFormat(types[choice],"");else if(choice==7)choose("Font",new String[]{"Sans serif","Serif","Monospace","Cursive"},font->applyFormat("font",new String[]{"sans-serif","serif","monospace","cursive"}[font]));else if(choice==8)prompt("Size in sp (8–72)","18",value->applyFormat("size",value));else if(choice==9)prompt("Web address (http/https)","https://",value->applyFormat("link",SafeLinks.requireWeb(value)));else if(choice==10)openLink();else if(choice==11)numberParagraphs();else applyFormat("clear","");});
    }
    private void applyFormat(String type,String value){int start=Math.min(selected.getSelectionStart(),selected.getSelectionEnd()),end=Math.max(selected.getSelectionStart(),selected.getSelectionEnd());if(start<0 || end<start)throw new IllegalArgumentException("Select text first");List<Note.Mark> marks=TextStyles.read(selected.getText());
        if(type.equals("text") || type.equals("h1") || type.equals("h2"))marks=TextFormatting.paragraph(marks,selected.getText().toString(),type,start,end);
        else if(value.isEmpty())marks=TextFormatting.apply(marks,type,start,end);
        else{if(start==end)throw new IllegalArgumentException("Select text first");List<Note.Mark> next=new ArrayList<>();for(Note.Mark mark:marks){if(!mark.type.equals(type) || mark.end<=start || mark.start>=end)next.add(mark);else{if(mark.start<start)next.add(new Note.Mark(mark.type,mark.start,start,mark.value));if(mark.end>end)next.add(new Note.Mark(mark.type,end,mark.end,mark.value));}}next.add(new Note.Mark(type,start,end,value));marks=next;}
        formattingChange=true;try{TextStyles.reflow(selected,marks);}finally{formattingChange=false;}
    }
    private void numberParagraphs(){String text=selected.getText().toString();int from=Math.max(0,Math.min(selected.getSelectionStart(),selected.getSelectionEnd())),to=Math.max(selected.getSelectionStart(),selected.getSelectionEnd());from=text.lastIndexOf('\n',Math.max(-1,from-1))+1;int stop=text.indexOf('\n',to>from?to-1:to);if(stop<0)stop=text.length();List<Note.Mark> marks=new ArrayList<>();for(Note.Mark mark:TextStyles.read(selected.getText()))if(!mark.type.equals("number") || mark.end<=from || mark.start>=stop)marks.add(mark);int number=1;for(int at=from;at<stop;){int end=text.indexOf('\n',at);if(end<0 || end>stop)end=stop;if(end>at)marks.add(new Note.Mark("number",at,end,Integer.toString(number++)));at=end+1;}formattingChange=true;try{TextStyles.reflow(selected,marks);}finally{formattingChange=false;}}
    private void openLink(){int at=selected.getSelectionStart();for(Note.Mark mark:TextStyles.read(selected.getText()))if(mark.type.equals("link") && mark.start<=at && mark.end>=at){String url=SafeLinks.requireWeb(mark.value);confirm("Open in your browser? The website is external to MyNotes.\n"+url,()->{try{getContext().startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException unavailable){message("No browser is available.");}});return;}message("Place the cursor in a hyperlink first.");}
    private void documentMenu(){String[] labels=readOnly()?new String[]{"Restore","Share","Export PDF","File location"}:new String[]{"Share","Export PDF","File location","Move to folder","Background","Labels","Colour",host.current().pinned?"Unpin":"Pin",host.current().archived?"Unarchive":"Archive"};choose("Document actions",labels,index->host.action(labels[index]));}
    private void restoreAfterDialog(){if(!isAttachedToWindow() || !selectionLocked)return;EditText field=fields.get(state.getString("field"));if(field!=null){restoringSelection=true;try{field.setSelection(Math.max(0,Math.min(field.length(),state.getInt("start"))),Math.max(0,Math.min(field.length(),state.getInt("end"))));}finally{restoringSelection=false;}}selectionLocked=false;}
    private void choose(String title,String[] labels,java.util.function.IntConsumer action){saveState();selectionLocked=true;android.app.AlertDialog menu=new android.app.AlertDialog.Builder(getContext()).setTitle(title).setItems(labels,(dialog,index)->{try{action.accept(index);}catch(IllegalArgumentException|IllegalStateException failure){message(failure.getMessage());}}).setNegativeButton("Cancel",null).create();menu.setOnDismissListener(dialog->postOnAnimation(this::restoreAfterDialog));design.show(menu);}
    private void prompt(String title,String value,java.util.function.Consumer<String> action){saveState();selectionLocked=true;EditText input=new EditText(getContext());design.entry(input,18);input.setText(value);android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(getContext()).setTitle(title).setView(input).setPositiveButton("Apply",null).setNegativeButton("Cancel",null).create();dialog.setOnDismissListener(closed->postOnAnimation(this::restoreAfterDialog));design.show(dialog);dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{try{action.accept(input.getText().toString());dialog.dismiss();}catch(IllegalArgumentException|IllegalStateException failure){input.setError(failure.getMessage());}});}
    private void confirm(String text,Runnable action){design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Confirm change").setMessage(text).setPositiveButton("Continue",(dialog,which)->{try{action.run();}catch(IllegalArgumentException|IllegalStateException failure){message(failure.getMessage());}}).setNegativeButton("Cancel",null).create());}
}
