package app.space.notes.runtime;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import app.space.notes.domain.Note;
import app.space.notes.domain.NoteMerge;
import app.space.notes.domain.TextFormatting;
import app.space.notes.domain.NoteOperations;

/** Standalone vertical slice. UI state lives on main; disk and filtering never do. */
public final class NotesController {
    private final Context context;
    private final NotesHost host;
    private final Handler main=new Handler(Looper.getMainLooper());
    // Shared serial queue prevents rotation from opening a stale library ahead of old writes.
    private static final ExecutorService disk=Executors.newSingleThreadExecutor();
    private final ExecutorService searchWorker=Executors.newSingleThreadExecutor();
    private final List<Note> notes=new ArrayList<>();
    private final List<Note> visible=new ArrayList<>();
    private final LinearLayout root;
    private NotesRepository repository;
    private final int bg, ink, surface, muted;
    private final Bundle restored;
    private boolean ready, closed, binding, dirty, busy;
    private String page="Notes", editing;
    private long revision, querySequence;
    private TextView status;
    private EditText title, body, search;
    private NotesBoard board;
    private TextView emptyHint;
    private boolean darkTheme,grid=true;
    private String activeLabel="",homeQuery="",focusedItem;
    private boolean sortCreated;
    private LinearLayout selectionBar;
    private final Set<String> selectedIds=new HashSet<>();
    private List<Note> undoBatch=new ArrayList<>();
    private boolean undoFull;
    private String undoCreatedId;
    private Note undo;
    private Runnable deferredResult;
    private boolean storagePage;
    private PreparedImport prepared;
    private static final class PreparedImport {
        final List<Note> base;
        final NoteMerge.Plan merge;
        final android.net.Uri folder;
        final String head;
        PreparedImport(List<Note> base,NoteMerge.Plan merge,android.net.Uri folder,String head) {
            this.base=Collections.unmodifiableList(new ArrayList<>(base));this.merge=merge;this.folder=folder;this.head=head;
        }
    }
    private final Runnable saveDraft=this::persistDraft;
    private final Runnable filter=this::filterNow;

    public NotesController(Context context, NotesHost host, Bundle restored) {
        this.context=context; this.host=host; this.restored=restored;
        boolean dark=(context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        darkTheme=dark;
        android.content.SharedPreferences ui=context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE);
        grid=ui.getBoolean("grid",true);sortCreated=ui.getBoolean("sortCreated",false);
        bg=Color.parseColor(dark?"#17181D":"#FAF8F2");
        ink=Color.parseColor(dark?"#EDEDF0":"#24231F");
        surface=Color.parseColor(dark?"#292B32":"#EDE7D9");
        muted=Color.parseColor(dark?"#B9BCC6":"#625C50");
        root=new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(14),dp(18),dp(10)); root.setBackgroundColor(bg);
        TextView loading=text("Opening local library…",18); root.addView(loading);
        disk.execute(()->{
            try {
                repository=new NotesRepository(context.getApplicationContext());
                List<Note> loaded=repository.load();
                long now=System.currentTimeMillis();
                boolean purged=loaded.removeIf(n->n.expired(now));
                if(purged) { try { repository.save(loaded); } catch(Exception pending) { log("purge","private recovery retained; folder pending"); } }
                main.post(()->{ if(closed)return; notes.addAll(loaded); ready=true;
                    if(restored!=null) {
                        page=restored.getString("page","Notes");
                        String id=restored.getString("editing");
                        if(id!=null && find(id)!=null) {
                            Note n=find(id);
                            try {byte[] draft=restored.getByteArray("draftDocument");if(draft!=null){List<Note> decoded=DocumentCodec.decode(draft);if(decoded.size()==1 && decoded.get(0).id.equals(id))n=decoded.get(0);}}catch(Exception invalid){log("draft-restore","invalid snapshot ignored");}
                            replace(n); editing=id; renderEditor(); queueSave(); deliverDeferred(); return;
                        }
                    }
                    renderHome(); log("load","ok count="+notes.size()); deliverDeferred();
                });
            } catch(Exception e) { main.post(()->{
                if(closed)return; root.removeAllViews(); root.addView(text("Library could not be opened. Existing data was not replaced.",18));
                root.addView(text("Close and retry. Do not clear app data.",14));
                log("load","failed "+e.getClass().getSimpleName());
            }); }
        });
    }
    private void deliverDeferred() {Runnable next=deferredResult;deferredResult=null;if(next!=null)next.run();}
    private void whenReady(Runnable action) {
        if(closed)return;
        if(!ready){deferredResult=action;return;}
        if(busy){host.showMessage("Another storage operation is in progress.");return;}
        action.run();
    }
    public View view() { return root; }
    private int dp(int n) { return Math.round(n*context.getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size) {
        TextView v=new TextView(context); v.setText(value); v.setTextSize(size); v.setTextColor(ink);
        v.setPadding(0,dp(6),0,dp(6)); return v;
    }
    private Button button(String label,Runnable action) {
        Button v=new Button(context); v.setText(label); v.setAllCaps(false); v.setTextColor(ink);
        GradientDrawable shape=new GradientDrawable(); shape.setColor(surface); shape.setCornerRadius(dp(22));
        v.setBackground(shape); v.setPadding(dp(12),dp(4),dp(12),dp(4));
        v.setOnClickListener(w->{if(!busy)action.run();else host.showMessage("Storage change in progress. Please wait.");}); return v;
    }
    private void row(String[] labels,Runnable[] actions) {rowIn(root,labels,actions);}
    private void rowIn(LinearLayout parent,String[] labels,Runnable[] actions) {
        LinearLayout row=new LinearLayout(context);
        for(int i=0;i<labels.length;i++) {
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1); p.setMargins(dp(2),dp(4),dp(2),dp(4));
            row.addView(button(labels[i],actions[i]),p);
        }
        parent.addView(row);
    }
    private void header(String name) {
        TextView h=text(name,28); h.setTypeface(null,Typeface.BOLD); root.addView(h);
        status=text((repository==null?"Opening storage":repository.status())+" · "+host.versionLabel(),12); status.setTextColor(muted); root.addView(status);
    }
    private void renderHome() {
        storagePage=false;prepared=null;editing=null; title=null; body=null;visible.clear();root.removeAllViews(); header("MyNotes · "+(activeLabel.isEmpty()?page:activeLabel));
        row(new String[]{"Notes","Archive","Trash","Storage"},new Runnable[]{()->navigate("Notes"),()->navigate("Archive"),()->navigate("Trash"),this::renderStorage});
        search=new EditText(context);search.setSingleLine(true);search.setTextColor(ink);search.setHintTextColor(muted);search.setHint("Search notes and labels");search.setText(homeQuery);root.addView(search);
        search.addTextChangedListener(watcher(()->{homeQuery=search.getText().toString();main.removeCallbacks(filter);main.postDelayed(filter,150);querySequence++;}));
        row(new String[]{grid?"List view":"Grid view",sortCreated?"Sort: created":"Sort: edited"},new Runnable[]{()->{grid=!grid;saveViewPreferences();renderHome();},()->{sortCreated=!sortCreated;saveViewPreferences();renderHome();}});
        TreeSet<String> labels=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note n:notes)if(n.deletedAt==0)labels.addAll(n.labels);
        HorizontalScrollView strip=new HorizontalScrollView(context);LinearLayout chips=new LinearLayout(context);strip.addView(chips);
        chips.addView(button(activeLabel.isEmpty()?"All labels ✓":"All labels",()->{activeLabel="";selectedIds.clear();renderHome();}));
        for(String label:labels)chips.addView(button(label+(label.equals(activeLabel)?" ✓":""),()->{activeLabel=label;selectedIds.clear();renderHome();}));root.addView(strip);
        selectionBar=new LinearLayout(context);selectionBar.setOrientation(LinearLayout.VERTICAL);
        rowIn(selectionBar,new String[]{"Pin","Archive",page.equals("Trash")?"Restore":"Trash"},new Runnable[]{()->bulkState("pin"),()->bulkState("archive"),()->bulkState(page.equals("Trash")?"restore":"trash")});
        rowIn(selectionBar,new String[]{"Labels","Colour","Share","Merge","Done"},new Runnable[]{this::renderLabels,this::renderColours,this::shareSelected,this::mergeSelected,()->{selectedIds.clear();updateSelection();}});root.addView(selectionBar);
        board=new NotesBoard(context,darkTheme,new NotesBoard.Listener(){
            public void open(Note n){if(!selectedIds.isEmpty()){toggleSelection(n);return;}editing=n.id;renderEditor();}
            public void select(Note n){toggleSelection(n);}
        },grid);
        emptyHint=text("No matching notes. Create a note or change the filters.",14);emptyHint.setVisibility(View.GONE);root.addView(emptyHint);
        root.addView(board,new LinearLayout.LayoutParams(-1,0,1));
        row(new String[]{"+ Text","+ Checklist","Undo"},new Runnable[]{this::newNote,()->newChecklist(),this::undo});
        updateSelection();filterNow();
    }
    private void saveViewPreferences(){context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).edit().putBoolean("grid",grid).putBoolean("sortCreated",sortCreated).apply();}
    private void toggleSelection(Note n){if(!selectedIds.add(n.id))selectedIds.remove(n.id);updateSelection();log("selection","count="+selectedIds.size());}
    private void updateSelection(){
        if(selectionBar!=null)selectionBar.setVisibility(selectedIds.isEmpty()?View.GONE:View.VISIBLE);
        if(board!=null)board.show(visible,selectedIds,!page.equals("Trash"));
        if(status!=null && !selectedIds.isEmpty())status.setText(selectedIds.size()+" selected · tap cards to change selection");
    }
    private void navigate(String name) { page=name;selectedIds.clear();activeLabel="";renderHome(); }
    private void newNote() {
        long now=System.currentTimeMillis();Note n=Note.create(now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);notes.add(n);editing=n.id;selectedIds.clear();
        queueSave(); renderEditor(); title.requestFocus();
    }
    private void newChecklist(){long now=System.currentTimeMillis();Note n=Note.create(now).convert(now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);notes.add(n);editing=n.id;selectedIds.clear();queueSave();renderEditor();}
    private void filterNow() {
        if(closed || editing!=null || search==null)return;
        final String query=search.getText().toString(), currentPage=page,label=activeLabel;
        final boolean created=sortCreated;
        final long ticket=++querySequence;
        final List<Note> snapshot=new ArrayList<>(notes);
        searchWorker.execute(()->{
            List<Note> result=new ArrayList<>();
            for(Note n:snapshot) {
                boolean included=currentPage.equals("Trash")?n.deletedAt>0:
                    n.deletedAt==0 && (currentPage.equals("Archive")==n.archived);
                if(included && n.matches(query) && (label.isEmpty() || n.labels.contains(label)))result.add(n);
            }
            result.sort(Comparator.comparing((Note n)->!n.pinned)
                .thenComparing(Comparator.comparingLong((Note n)->created?n.createdAt:n.updatedAt).reversed()));
            main.post(()->{
                if(closed || editing!=null || ticket!=querySequence)return;
                visible.clear();visible.addAll(result);board.show(result,selectedIds,!currentPage.equals("Trash"));
                if(emptyHint!=null)emptyHint.setVisibility(result.isEmpty()?View.VISIBLE:View.GONE);
            });
        });
    }
    private void renderEditor() {
        Note n=find(editing);if(n==null){renderHome();return;}
        main.removeCallbacks(filter);querySequence++;search=null;board=null;selectionBar=null;
        root.removeAllViews();header(n.deletedAt>0?"Trash note":n.checklist?"Checklist":"Edit note");
        title=new EditText(context);title.setHint("Title");title.setTextSize(24);title.setTextColor(ink);title.setHintTextColor(muted);title.setText(n.title);root.addView(title);
        body=null;boolean trashed=n.deletedAt>0;
        if(n.checklist){
            ScrollView scroll=new ScrollView(context);LinearLayout rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            int checked=0;TextView footer=text("",12);
            for(Note.Item item:n.items){
                if(item.checked)checked++;
                LinearLayout line=new LinearLayout(context);line.setGravity(android.view.Gravity.CENTER_VERTICAL);line.setPadding(dp(item.depth*16),0,0,0);
                CheckBox tick=new CheckBox(context);tick.setContentDescription("Complete list item");tick.setChecked(item.checked);tick.setButtonTintList(android.content.res.ColorStateList.valueOf(ink));tick.setEnabled(!trashed);line.addView(tick);
                EditText input=new EditText(context);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setHint("List item");input.setTextColor(ink);input.setHintTextColor(muted);input.setText(item.text);input.setEnabled(!trashed);
                input.setPaintFlags(item.checked?input.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG:input.getPaintFlags()&~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                line.addView(input,new LinearLayout.LayoutParams(0,-2,1));rows.addView(line);
                if(n.hideChecked && item.checked)line.setVisibility(View.GONE);
                input.setOnFocusChangeListener((v,focused)->{if(focused)focusedItem=item.id;});
                if(item.id.equals(focusedItem) && !trashed)input.requestFocus();
                input.addTextChangedListener(watcher(()->{
                    if(binding)return;invalidateFullUndo();Note current=find(editing);if(current==null)return;
                    List<Note.Item> changed=new ArrayList<>(current.items);
                    for(int i=0;i<changed.size();i++)if(changed.get(i).id.equals(item.id))changed.set(i,changed.get(i).change(input.getText().toString(),changed.get(i).checked,changed.get(i).depth));
                    replace(current.list(title.getText().toString(),changed,current.hideChecked,System.currentTimeMillis()));scheduleDraft();
                }));
                tick.setOnCheckedChangeListener((v,value)->{
                    persistDraft();Note current=find(editing);if(current==null)return;captureUndo(Arrays.asList(current),true);
                    List<Note.Item> changed=new ArrayList<>(current.items);
                    for(int i=0;i<changed.size();i++)if(changed.get(i).id.equals(item.id))changed.set(i,changed.get(i).change(changed.get(i).text,value,changed.get(i).depth));
                    replace(current.list(title.getText().toString(),changed,current.hideChecked,System.currentTimeMillis()));queueSave();
                    input.setPaintFlags(value?input.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG:input.getPaintFlags()&~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    if(current.hideChecked && value)line.setVisibility(View.GONE);
                    footer.setText(changed.stream().filter(i->i.checked).count()+" checked · tap a row before using move/indent controls");
                });
            }
            footer.setText(checked+" checked · tap a row before using move/indent controls");rows.addView(footer);
            if(!trashed){
                row(new String[]{"+ Item","↑","↓","Indent","Outdent","Remove"},new Runnable[]{()->listAction("add"),()->listAction("up"),()->listAction("down"),()->listAction("indent"),()->listAction("outdent"),()->listAction("remove")});
                row(new String[]{n.hideChecked?"Show checked":"Hide checked","To text"},new Runnable[]{()->listAction("hide"),this::convertNote});
            }
        }else{
            body=new EditText(context);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);body.setHint("Write your note…");body.setTextColor(ink);body.setHintTextColor(muted);body.setGravity(android.view.Gravity.TOP);body.setText(n.body);
            TextStyles.write(body.getText(),n.marks);body.setEnabled(!trashed);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
            body.addTextChangedListener(watcher(()->{if(!binding){invalidateFullUndo();scheduleDraft();}}));
            if(!trashed){
                HorizontalScrollView tools=new HorizontalScrollView(context);LinearLayout strip=new LinearLayout(context);tools.addView(strip);
                String[] labels={"Bold","Italic","Underline","Strike","Heading 1","Heading 2","Clear"};String[] types={"bold","italic","underline","strike","h1","h2","clear"};
                for(int i=0;i<labels.length;i++){final String type=types[i];Button action=button(labels[i],()->format(type));action.setMinWidth(dp(64));strip.addView(action,new LinearLayout.LayoutParams(-2,dp(48)));}root.addView(tools);
            }
        }
        title.setEnabled(!trashed);title.addTextChangedListener(watcher(()->{if(!binding){invalidateFullUndo();scheduleDraft();}}));
        if(trashed)row(new String[]{"Back","Restore","Share"},new Runnable[]{()->back(),()->changeState("restore"),this::shareSelected});
        else{
            row(new String[]{"Labels","Colour","Share","Copy"},new Runnable[]{this::renderLabels,this::renderColours,this::shareSelected,this::duplicateNote});
            row(new String[]{"Back",n.pinned?"Unpin":"Pin",n.archived?"Unarchive":"Archive","Trash"},new Runnable[]{()->back(),()->changeState("pin"),()->changeState("archive"),()->changeState("trash")});
            if(!n.checklist)row(new String[]{"Convert to checklist","Undo"},new Runnable[]{this::convertNote,this::undo});
        }
    }
    private void scheduleDraft(){dirty=true;if(status!=null)status.setText("Unsaved changes…");main.removeCallbacks(saveDraft);main.postDelayed(saveDraft,350);}
    private void invalidateFullUndo(){undoCreatedId=null;if(undoFull){undoBatch.clear();undoFull=false;}}
    private void captureUndo(List<Note> before,boolean full){undoCreatedId=null;undo=null;undoBatch=new ArrayList<>(before);undoFull=full;}
    private void format(String type){
        if(body==null)return;int start=body.getSelectionStart(),end=body.getSelectionEnd();
        if(start<0 || end<=start){host.showMessage("Select the text to format first.");return;}
        persistDraft();Note n=find(editing);captureUndo(Arrays.asList(n),true);
        TextStyles.write(body.getText(),TextFormatting.apply(TextStyles.read(body.getText()),type,start,end));dirty=true;persistDraft();body.setSelection(start,end);log("format","applied type="+type+" range="+start+".."+end);
    }
    private void listAction(String action){
        persistDraft();Note n=find(editing);if(n==null)return;captureUndo(Arrays.asList(n),true);
        List<Note.Item> rows=new ArrayList<>(n.items);int index=-1;
        for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(focusedItem))index=i;
        if(action.equals("add")){Note.Item next=new Note.Item(UUID.randomUUID().toString(),"",false,index<0?0:rows.get(index).depth);rows.add(next);focusedItem=next.id;}
        else if(action.equals("hide")){replace(n.list(n.title,rows,!n.hideChecked,System.currentTimeMillis()));queueSave();renderEditor();return;}
        else if(index>=0){
            Note.Item item=rows.get(index);
            if(action.equals("up") && index>0)Collections.swap(rows,index,index-1);
            if(action.equals("down") && index+1<rows.size())Collections.swap(rows,index,index+1);
            if(action.equals("remove")){rows.remove(index);focusedItem=null;}
            if(action.equals("indent"))rows.set(index,item.change(item.text,item.checked,Math.min(3,item.depth+1)));
            if(action.equals("outdent"))rows.set(index,item.change(item.text,item.checked,Math.max(0,item.depth-1)));
        }else{host.showMessage("Tap a list item first.");return;}
        replace(n.list(n.title,rows,n.hideChecked,System.currentTimeMillis()));queueSave();renderEditor();log("checklist-"+action,"applied rows="+rows.size());
    }
    private void convertNote(){persistDraft();Note n=find(editing);captureUndo(Arrays.asList(n),true);replace(n.convert(System.currentTimeMillis()));queueSave();renderEditor();}
    private void duplicateNote(){persistDraft();Note n=find(editing);Note copy=NoteOperations.duplicate(n,System.currentTimeMillis());notes.add(copy);editing=copy.id;queueSave();renderEditor();host.showMessage("Copy created; original unchanged.");}
    private List<Note> targets(){
        List<Note> result=new ArrayList<>();
        if(editing!=null){Note n=find(editing);if(n!=null)result.add(n);return result;}
        Set<String> seen=new HashSet<>();
        for(Note n:visible)if(selectedIds.contains(n.id)){result.add(n);seen.add(n.id);}
        for(Note n:notes)if(selectedIds.contains(n.id) && !seen.contains(n.id))result.add(n);
        return result;
    }
    private void bulkState(String action){
        List<Note> chosen=targets();if(chosen.isEmpty())return;
        if(!action.equals("restore"))for(Note n:chosen)if(n.deletedAt>0){host.showMessage("Restore trashed notes before changing them.");return;}
        captureUndo(chosen,false);boolean pin=chosen.stream().anyMatch(n->!n.pinned);long now=System.currentTimeMillis();
        for(Note n:chosen)replace(n.state(action.equals("pin")?pin:n.pinned,action.equals("archive")?!page.equals("Archive"):n.archived,
            action.equals("trash")?now:action.equals("restore")?0:n.deletedAt,now));
        selectedIds.clear();queueSave();renderHome();log("bulk-"+action,"applied count="+chosen.size());
    }
    private void shareSelected(){
        persistDraft();List<Note> chosen=targets();if(chosen.isEmpty()){host.showMessage("Select a note to share.");return;}
        StringBuilder text=new StringBuilder();
        for(Note n:chosen){if(text.length()>0)text.append("

────────

");if(!n.title.isEmpty())text.append(n.title).append('
');text.append(n.plainText());}
        if(text.length()>100000){host.showMessage("Selection is too large for Android's share message. Use a backup instead.");return;}
        host.shareText(chosen.size()==1?chosen.get(0).title:"MyNotes",text.toString());log("share","chooser opened count="+chosen.size());
    }
    private void mergeSelected(){
        List<Note> chosen=targets();
        try{
            long now=System.currentTimeMillis();Note merged=NoteOperations.merge(chosen,now);captureUndo(chosen,false);
            for(Note n:chosen)replace(n.state(n.pinned,n.archived,now,now));notes.add(merged);undoCreatedId=merged.id;
            selectedIds.clear();editing=merged.id;queueSave();renderEditor();host.showMessage("Merged note created. Sources moved to Trash; Undo restores them.");log("merge","applied count="+chosen.size());
        }catch(IllegalArgumentException e){host.showMessage(e.getMessage());}
    }
    private void returnToNoteOrHome(){if(editing!=null)renderEditor();else renderHome();}
    private List<Note> propertyTargets(){
        persistDraft();List<Note> chosen=targets();
        for(Note n:chosen)if(n.deletedAt>0){host.showMessage("Restore trashed notes before changing them.");return Collections.emptyList();}
        return chosen;
    }
    private void renderLabels(){
        List<Note> chosen=propertyTargets();if(chosen.isEmpty())return;
        title=null;body=null;search=null;querySequence++;root.removeAllViews();header("Labels · "+chosen.size()+" notes");
        root.addView(text("Comma-separated labels; up to 50, maximum 64 characters each. Bulk Save replaces the label set on the selected notes.",14));
        EditText input=new EditText(context);input.setTextColor(ink);input.setHintTextColor(muted);input.setHint("Work, Personal, Ideas");
        input.setText(String.join(", ",chosen.get(0).labels));root.addView(input);
        TreeSet<String> known=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note n:notes)known.addAll(n.labels);
        TextView hints=text("Existing: "+String.join(" · ",known),14);root.addView(hints,new LinearLayout.LayoutParams(-1,0,1));
        row(new String[]{"Save labels","Cancel"},new Runnable[]{()->{
            try{List<String> labels=Note.normalizeLabels(Arrays.asList(input.getText().toString().split(",",-1)));captureUndo(chosen,false);
                for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.properties(current.color,labels,System.currentTimeMillis()));}
                queueSave();returnToNoteOrHome();log("labels","applied count="+chosen.size());
            }catch(IllegalArgumentException e){host.showMessage(e.getMessage());}
        },this::returnToNoteOrHome});
    }
    private void renderColours(){
        List<Note> chosen=propertyTargets();if(chosen.isEmpty())return;
        title=null;body=null;search=null;querySequence++;root.removeAllViews();header("Note colour");
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int row=0;row<4;row++){
            LinearLayout line=new LinearLayout(context);
            for(int col=0;col<3;col++){
                final int colour=row*3+col;Button pick=button(NotesBoard.NAMES[colour],()->{
                    captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.properties(colour,current.labels,System.currentTimeMillis()));}
                    queueSave();returnToNoteOrHome();log("colour","applied count="+chosen.size()+" colour="+colour);
                });
                GradientDrawable swatch=new GradientDrawable();swatch.setColor(NotesBoard.colour(colour,darkTheme));swatch.setCornerRadius(dp(18));pick.setBackground(swatch);pick.setTextColor(darkTheme?Color.WHITE:Color.parseColor("#202124"));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(64),1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(pick,lp);
            }content.addView(line);
        }
        row(new String[]{"Cancel"},new Runnable[]{this::returnToNoteOrHome});
    }
    private void renderStorage() {
        storagePage=true;prepared=null;editing=null;search=null;title=null;body=null;querySequence++;main.removeCallbacks(filter);
        root.removeAllViews();header("Storage & recovery");
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(text("Choose Internal storage or an SD-card folder. Cloud providers are refused: Notes stays offline.",17));
        content.addView(text("Choose a NEW EMPTY folder for this library. Notes copies and verifies all text notes before switching. The previous folder is kept; it is never deleted automatically.",15));
        content.addView(text("A private recovery copy is also kept on this device. If folder access fails, edits remain in this copy and show Pending, not saved-to-folder. Retry after reconnecting or choose another empty folder.",15));
        content.addView(text("After reinstall, Open library re-grants folder access and previews a non-destructive merge. Backups exports/restores a portable text-library file. Normal APK updates must still be installed without uninstalling.",14));
        content.addView(text("Only the standard local Android storage provider is supported. Android blocks some system/root folders. Folder generations are retained for recovery; large-library compaction is not implemented yet.",14));
        row(new String[]{"Choose folder","Retry save"},new Runnable[]{host::chooseLocalFolder,this::flush});
        row(new String[]{"Open library","Backups"},new Runnable[]{host::openExistingFolder,this::renderBackups});
        row(new String[]{"Use device storage","Back"},new Runnable[]{()->moveStorage(null),this::renderHome});
    }
    public void selectFolder(android.net.Uri uri) {
        whenReady(()->{persistDraft();moveStorage(uri);});
    }
    private void moveStorage(android.net.Uri uri) {
        busy=true;final List<Note> snapshot=new ArrayList<>(notes);
        if(status!=null)status.setText("Copying and verifying library…");
        disk.execute(()->{
            String message;
            try {
                if(uri==null)repository.usePrivate(snapshot);else repository.selectFolder(uri,snapshot);
                log("storage-move","verified count="+snapshot.size());
                message="Storage changed after verification. Previous files were retained.";
            } catch(Exception e) {log("storage-move","failed "+e.getClass().getSimpleName());message="Storage change incomplete: "+e.getMessage();}
            final String result=message;
            main.post(()->{busy=false;if(!closed){renderStorage();host.showMessage(result);}});
        });
    }
    private LinearLayout scrollContent(String heading) {
        editing=null;title=null;body=null;search=null;storagePage=true;querySequence++;main.removeCallbacks(filter);
        root.removeAllViews();header(heading);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return content;
    }
    private void renderBackups() {
        prepared=null;LinearLayout content=scrollContent("Backup & restore");
        content.addView(text("Portable backups include every current text note, pin, archive, colour and Trash timestamp. This milestone has no media attachments yet.",16));
        content.addView(text("Use the local Internal storage/SD-card provider. Backups are readable JSON, not encrypted. Export is confirmed only after reopening and checking the complete file.",15));
        content.addView(text("Import always previews a MERGE: current notes stay; identical notes are skipped; a changed note with the same ID becomes a separate copy. Re-importing that conflict copy is idempotent. No replacement/delete mode.",15));
        content.addView(text("Previous snapshot recovers the local checkpoint taken before the most recent confirmed restore/open-library. It is also merged, never used to silently delete current notes. Uninstall removes this private checkpoint.",14));
        content.addView(text("Current portable text-backup limit: 8 MiB (checked on export and import). Larger libraries need the future streaming backup format. Keep the old installation and your source until you verify restored notes.",14));
        row(new String[]{"Export backup","Import backup"},new Runnable[]{host::exportBackup,host::importBackup});
        row(new String[]{"Previous snapshot","Back"},new Runnable[]{()->prepareImport(null,false,true),this::renderStorage});
    }
    public void exportBackup(android.net.Uri uri) {
        whenReady(()->{
            persistDraft();busy=true;final List<Note> snapshot=new ArrayList<>(notes);
            if(status!=null)status.setText("Writing and verifying backup…");
            disk.execute(()->{
                String message;
                try {LocalBackupFiles.write(context.getContentResolver(),uri,snapshot);message="Backup verified: "+snapshot.size()+" text notes.";log("backup-export","verified count="+snapshot.size());}
                catch(Exception e) {message="Backup not verified: "+e.getMessage();log("backup-export","failed "+e.getClass().getSimpleName());}
                final String result=message;main.post(()->{busy=false;if(!closed){renderBackups();host.showMessage(result);}});
            });
        });
    }
    public void importBackup(android.net.Uri uri) {whenReady(()->prepareImport(uri,false,false));}
    public void openExistingFolder(android.net.Uri uri) {whenReady(()->prepareImport(uri,true,false));}
    private void prepareImport(android.net.Uri source,boolean folder,boolean previous) {
        persistDraft();busy=true;final List<Note> base=new ArrayList<>(notes);
        if(status!=null)status.setText("Validating library and preparing preview…");
        disk.execute(()->{
            try {
                GenerationProtocol.Snapshot library=folder?repository.inspectFolder(source):null;
                List<Note> incoming=previous?repository.previousSnapshot():folder?DocumentCodec.decode(library.payload):LocalBackupFiles.read(context.getContentResolver(),source);
                NoteMerge.Plan merge=NoteMerge.preview(base,incoming);
                log("restore-preview","validated incoming="+incoming.size()+" added="+merge.added+" conflicts="+merge.conflicts);
                PreparedImport plan=new PreparedImport(base,merge,folder?source:null,library==null?null:library.head);
                main.post(()->{busy=false;if(!closed){prepared=plan;renderPreview();}});
            } catch(Exception e) {log("restore-preview","failed "+e.getClass().getSimpleName());
                main.post(()->{busy=false;if(!closed){renderBackups();host.showMessage("Nothing imported: "+e.getMessage());}});
            }
        });
    }
    private void renderPreview() {
        PreparedImport plan=prepared;if(plan==null){renderBackups();return;}
        LinearLayout content=scrollContent("Review restore");
        content.addView(text(plan.folder==null?"Merge into current library":"Open this folder and merge both libraries",18));
        content.addView(text("Add "+plan.merge.added+" notes · skip "+plan.merge.unchanged+" identical notes · preserve "+plan.merge.conflicts+" changed-ID conflicts as copies.",16));
        content.addView(text("Result: "+plan.merge.notes.size()+" notes. Nothing has been imported yet. Confirm keeps existing notes and writes a previous-state checkpoint first.",15));
        if(plan.folder!=null)content.addView(text("Folder: "+android.provider.DocumentsContract.getTreeDocumentId(plan.folder),14));
        content.addView(text("First result titles (including archive and Trash):",14));
        for(int i=0;i<Math.min(8,plan.merge.notes.size());i++) {
            Note n=plan.merge.notes.get(i);String label=n.title.isEmpty()?"Untitled":n.title;
            content.addView(text(label.length()>120?label.substring(0,120)+"…":label,14));
        }
        row(new String[]{"Confirm merge","Cancel"},new Runnable[]{this::confirmImport,()->{log("restore","cancelled before mutation");renderBackups();}});
    }
    private void confirmImport() {
        final PreparedImport plan=prepared;
        if(plan==null)return;
        if(!notes.equals(plan.base)){prepared=null;renderBackups();host.showMessage("Library changed. Please preview again.");return;}
        busy=true;status.setText("Applying verified merge…");
        disk.execute(()->{
            try {
                boolean complete=true;
                if(plan.folder!=null)repository.openExisting(plan.folder,plan.head,plan.base,plan.merge.notes);
                else complete=repository.applyRestore(plan.base,plan.merge.notes);
                final boolean saved=complete;
                log("restore","applied count="+plan.merge.notes.size()+" destinationComplete="+saved);
                main.post(()->{busy=false;if(closed)return;notes.clear();notes.addAll(plan.merge.notes);undo=null;prepared=null;renderHome();
                    host.showMessage(saved?"Merge saved. Previous snapshot is available in Backups.":"Merge retained in private recovery. Folder update is pending; use Retry save.");});
            } catch(Exception e) {log("restore","failed "+e.getClass().getSimpleName());
                main.post(()->{busy=false;if(!closed){prepared=null;renderBackups();host.showMessage("Restore incomplete: "+e.getMessage()+". Source and checkpoint were retained.");}});
            }
        });
    }
    private TextWatcher watcher(Runnable action) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int count,int after) { }
            public void onTextChanged(CharSequence s,int a,int before,int count) { }
            public void afterTextChanged(Editable e) { action.run(); }
        };
    }
    private Note find(String id) { for(Note n:notes)if(n.id.equals(id))return n; return null; }
    private void replace(Note n) { for(int i=0;i<notes.size();i++)if(notes.get(i).id.equals(n.id)){notes.set(i,n);return;} }
    private void persistDraft() {
        main.removeCallbacks(saveDraft);
        if(!ready || editing==null || title==null)return;
        Note n=find(editing);if(n==null || n.deletedAt>0)return;
        Note candidate=n.checklist?n.list(title.getText().toString(),n.items,n.hideChecked,n.updatedAt):
            n.rich(title.getText().toString(),body.getText().toString(),TextStyles.read(body.getText()),n.updatedAt);
        boolean changed=!NoteMerge.same(n,candidate);
        if(changed)replace(n.checklist?candidate.list(candidate.title,candidate.items,candidate.hideChecked,System.currentTimeMillis()):
            candidate.rich(candidate.title,candidate.body,candidate.marks,System.currentTimeMillis()));
        if(changed || dirty){dirty=false;queueSave();}
    }
    private void changeState(String action) {
        persistDraft(); Note n=find(editing); if(n==null)return; captureUndo(Arrays.asList(n),false);
        long now=System.currentTimeMillis();
        Note changed=n.state(action.equals("pin")?!n.pinned:n.pinned,
            action.equals("archive")?!n.archived:n.archived,
            action.equals("trash")?now:action.equals("restore")?0:n.deletedAt,now);
        replace(changed); queueSave();
        if(action.equals("pin"))renderEditor(); else {renderHome();host.showMessage("Updated. Undo is available on Home.");}
    }
    private void undo() {
        persistDraft();
        if(undoBatch.isEmpty()){host.showMessage("Nothing to undo");return;}
        for(Note old:undoBatch){Note current=find(old.id);if(current!=null)replace(undoFull?old.state(old.pinned,old.archived,old.deletedAt,System.currentTimeMillis()):
            current.state(old.pinned,old.archived,old.deletedAt,System.currentTimeMillis()).properties(old.color,old.labels,System.currentTimeMillis()));}
        if(undoCreatedId!=null){String created=undoCreatedId;notes.removeIf(n->n.id.equals(created));if(created.equals(editing))editing=null;undoCreatedId=null;}
        undoBatch.clear();undoFull=false;queueSave();if(editing!=null)renderEditor();else renderHome();
    }
    private void queueSave() {
        final List<Note> snapshot=new ArrayList<>(notes); final long ticket=++revision;
        if(status!=null)status.setText("Saving locally…");
        disk.execute(()->{
            try { repository.save(snapshot); log("save","ok revision="+ticket+" count="+snapshot.size());
                main.post(()->{if(!closed && !dirty && ticket==revision && status!=null)status.setText("Saved · "+repository.status());});
            } catch(Exception e) { log("save","failed "+e.getClass().getSimpleName());
                main.post(()->{if(!closed){if(status!=null)status.setText(repository.status()+" · retry needed");
                    host.showMessage("Save incomplete. "+repository.status()+". Check storage access, then use Retry save.");}});
            }
        });
    }
    private void log(String action,String outcome) { Log.i("MyNotes",action+" outcome="+outcome); }
    public boolean back() {
        if(busy){host.showMessage("Please wait for the current storage operation.");return true;}
        if(editing==null && !selectedIds.isEmpty()){selectedIds.clear();renderHome();return true;}
        if(prepared!=null){prepared=null;renderBackups();return true;}
        if(editing==null && storagePage){renderHome();return true;}
        if(editing==null)return false;
        persistDraft(); renderHome(); return true;
    }
    public void flush() { if(ready && !busy){persistDraft();queueSave();} }
    public void saveState(Bundle state) {
        persistDraft();state.putString("page",page);state.putString("editing",editing);
        if(editing!=null && find(editing)!=null){try{byte[] draft=DocumentCodec.encode(Arrays.asList(find(editing)));if(draft.length<=32768)state.putByteArray("draftDocument",draft);}catch(Exception e){log("draft-snapshot","failed "+e.getClass().getSimpleName());}}
    }
    public void close() {
        closed=true; main.removeCallbacksAndMessages(null); searchWorker.shutdown();
    }
}
