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
import app.space.notes.domain.Diagram;
import app.space.notes.domain.LabelOperations;
import app.space.notes.domain.IncomingText;
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
    private boolean sortCreated,diagramOpen,filterExpanded;
    private String diagramSelection;
    private LinearLayout selectionBar;
    private final Map<View,Integer> homeChrome=new LinkedHashMap<>();
    private boolean chromeHidden;
    private final Set<String> selectedIds=new HashSet<>();
    private List<Note> undoBatch=new ArrayList<>();
    private boolean undoFull;
    private String undoCreatedId;
    private Note undo;
    private final Deque<Runnable> deferredResults=new ArrayDeque<>();
    private static final ExecutorService ocrWorker=Executors.newSingleThreadExecutor(),captureWorker=Executors.newSingleThreadExecutor();
    private OfflineOcr.Cancellation ocrCancellation;
    private long ocrSequence;
    private boolean ocrPanel,voicePage;
    private VoiceCapture capture;
    private android.media.MediaPlayer voicePlayer;
    private Runnable unsubscribeVoice;
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
        unsubscribeVoice=VoiceTasks.subscribe(context,this::voiceComplete);
        disk.execute(()->{
            try {
                repository=new NotesRepository(context.getApplicationContext());
                List<Note> loaded=repository.load();
                boolean recovered=false;
                try{VoiceFiles files=new VoiceFiles(context);for(String id:files.list()){
                    org.json.JSONObject result;try{result=files.result(id);}catch(Exception invalidTranscript){log("voice-recovery","one unreadable transcript skipped; audio retained");continue;}if(!result.optString("state").equals("DONE"))continue;
                    String voiceId=app.space.notes.domain.VoiceTranscripts.noteId(id);Note current=null;for(Note n:loaded)if(n.id.equals(voiceId))current=n;
                    if(current==null && files.noteWasCreated(id))continue; // Respect notes deliberately purged/deleted after an earlier successful insertion.
                    if(current!=null && current.voiceSources.contains(id))files.noteCreated(id);
                    if(current==null){current=app.space.notes.domain.VoiceTranscripts.placeholder(id,System.currentTimeMillis());loaded.add(current);recovered=true;}
                    Note changed=app.space.notes.domain.VoiceTranscripts.apply(current,id,result.optString("text",""),System.currentTimeMillis());if(changed!=current){loaded.set(loaded.indexOf(current),changed);recovered=true;}
                }}catch(Exception voiceFailure){log("voice-recovery","audio retained; transcript metadata needs review");}
                if(recovered){try{repository.save(loaded);markVoiceNotesCreated(loaded);}catch(Exception pendingVoiceSave){log("voice-recovery","transcript recovery save needs retry");}}
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
    private void deliverDeferred() {while(!deferredResults.isEmpty() && !closed)deferredResults.removeFirst().run();}
    private void whenReady(Runnable action) {
        if(closed)return;
        if(!ready){if(deferredResults.size()>=8){host.showMessage("Library is still opening. Please retry this action in a moment.");return;}deferredResults.addLast(action);return;}
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
        Button v=new Button(context); v.setText(label); v.setSingleLine(true); v.setAllCaps(false); v.setTextColor(ink);
        GradientDrawable shape=new GradientDrawable(); shape.setColor(surface); shape.setCornerRadius(dp(22));
        v.setBackground(shape); v.setPadding(dp(12),dp(4),dp(12),dp(4));
        v.setOnClickListener(w->{if(!busy)action.run();else host.showMessage("Storage change in progress. Please wait.");}); return v;
    }
    private void row(String[] labels,Runnable[] actions) {rowIn(root,labels,actions);}
    private void rowIn(LinearLayout parent,String[] labels,Runnable[] actions) {
        HorizontalScrollView scroll=new HorizontalScrollView(context);scroll.setFillViewport(true);
        LinearLayout row=new LinearLayout(context);row.setGravity(android.view.Gravity.CENTER_VERTICAL);scroll.addView(row);
        for(int i=0;i<labels.length;i++) {
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(dp(2),dp(4),dp(2),dp(4));
            Button control=button(labels[i],actions[i]);control.setMinHeight(dp(48));control.setMinWidth(dp(80));row.addView(control,p);
        }
        parent.addView(scroll);
    }
    private void header(String name) {headerIn(root,name);}
    private void headerIn(LinearLayout parent,String name) {
        TextView h=text(name,28); h.setTypeface(null,Typeface.BOLD); parent.addView(h);
        status=text((repository==null?"Opening storage":repository.status())+" · "+host.versionLabel(),12); status.setTextColor(muted); parent.addView(status);
    }
    private void renderHome() {
        voicePage=false;ocrPanel=false;storagePage=false;prepared=null;editing=null; title=null; body=null;visible.clear();diagramOpen=false;root.removeAllViews(); header("MyNotes · "+(activeLabel.isEmpty()?page:activeLabel));
        status.setVisibility(View.GONE);
        boolean compact=context.getResources().getConfiguration().screenHeightDp<450;
        if(compact){root.getChildAt(0).setVisibility(View.GONE);root.setPadding(dp(10),dp(4),dp(10),dp(4));}
        else root.setPadding(dp(18),dp(14),dp(18),dp(10));
        row(new String[]{"Notes","Archive","Trash","Storage"},new Runnable[]{()->navigate("Notes"),()->navigate("Archive"),()->navigate("Trash"),this::renderStorage});
        search=new EditText(context);search.setSingleLine(true);search.setTextColor(ink);search.setHintTextColor(muted);search.setHint("Search notes and labels");search.setText(homeQuery);root.addView(search);
        search.addTextChangedListener(watcher(()->{homeQuery=search.getText().toString();main.removeCallbacks(filter);main.postDelayed(filter,150);querySequence++;}));
        LinearLayout filters=new LinearLayout(context);filters.setOrientation(LinearLayout.VERTICAL);filters.setVisibility(filterExpanded?View.VISIBLE:View.GONE);root.addView(filters);
        rowIn(filters,new String[]{grid?"List view":"Grid view",sortCreated?"Sort: created":"Sort: edited"},new Runnable[]{()->{grid=!grid;saveViewPreferences();renderHome();},()->{sortCreated=!sortCreated;saveViewPreferences();renderHome();}});
        TreeSet<String> labels=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note n:notes)if(n.deletedAt==0)labels.addAll(n.labels);
        HorizontalScrollView strip=new HorizontalScrollView(context);LinearLayout chips=new LinearLayout(context);strip.addView(chips);
        chips.addView(button(activeLabel.isEmpty()?"All labels ✓":"All labels",()->{activeLabel="";selectedIds.clear();renderHome();}));
        chips.addView(button("Manage labels",this::manageLabels));chips.addView(button("Home widget",host::requestHomeWidget));
        for(String label:labels)chips.addView(button(label+(label.equals(activeLabel)?" ✓":""),()->{activeLabel=label;selectedIds.clear();renderHome();}));filters.addView(strip);
        homeChrome.clear();chromeHidden=false;for(int i=0;i<root.getChildCount();i++)homeChrome.put(root.getChildAt(i),root.getChildAt(i).getVisibility());
        selectionBar=new LinearLayout(context);selectionBar.setOrientation(LinearLayout.VERTICAL);
        rowIn(selectionBar,new String[]{"Pin","Archive",page.equals("Trash")?"Restore":"Trash"},new Runnable[]{()->bulkState("pin"),()->bulkState("archive"),()->bulkState(page.equals("Trash")?"restore":"trash")});
        rowIn(selectionBar,new String[]{"Labels","Colour","Share","Merge","Done"},new Runnable[]{this::renderLabels,this::renderColours,this::shareSelected,this::mergeSelected,()->{selectedIds.clear();updateSelection();}});root.addView(selectionBar);
        board=new NotesBoard(context,darkTheme,new NotesBoard.Listener(){
            public void open(Note n){if(!selectedIds.isEmpty()){toggleSelection(n);return;}editing=n.id;renderEditor();}
            public void select(Note n){toggleSelection(n);}
        },grid);
        emptyHint=text("No matching notes. Create a note or change the filters.",14);emptyHint.setVisibility(View.GONE);root.addView(emptyHint);
        root.addView(board,new LinearLayout.LayoutParams(-1,0,1));
        row(new String[]{"+ Text","+ Checklist",filterExpanded?"Hide filters":"Filters","Undo"},new Runnable[]{this::newNote,()->newChecklist(),()->{filterExpanded=!filterExpanded;filters.setVisibility(filterExpanded?View.VISIBLE:View.GONE);},this::undo});
        updateSelection();filterNow();
    }
    private void saveViewPreferences(){context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).edit().putBoolean("grid",grid).putBoolean("sortCreated",sortCreated).apply();}
    private void toggleSelection(Note n){if(!selectedIds.add(n.id))selectedIds.remove(n.id);updateSelection();log("selection","count="+selectedIds.size());}
    private void updateSelection(){
        if(!selectedIds.isEmpty() && !chromeHidden){for(View view:homeChrome.keySet()){homeChrome.put(view,view.getVisibility());view.setVisibility(View.GONE);}chromeHidden=true;}
        else if(selectedIds.isEmpty() && chromeHidden){for(Map.Entry<View,Integer> entry:homeChrome.entrySet())entry.getKey().setVisibility(entry.getValue());chromeHidden=false;}
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
                if(included && n.matches(query) && (label.isEmpty() || n.labels.stream().anyMatch(tag->tag.equalsIgnoreCase(label))))result.add(n);
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
        voicePage=false;ocrPanel=false;diagramOpen=false;root.setPadding(dp(18),dp(14),dp(18),dp(10));
        Note n=find(editing);if(n==null){renderHome();return;}
        main.removeCallbacks(filter);querySequence++;search=null;board=null;selectionBar=null;
        root.removeAllViews();ScrollView editorScroll=new ScrollView(context);editorScroll.setFillViewport(true);
        LinearLayout editor=new LinearLayout(context);editor.setOrientation(LinearLayout.VERTICAL);editorScroll.addView(editor);root.addView(editorScroll,new LinearLayout.LayoutParams(-1,-1));headerIn(editor,n.deletedAt>0?"Trash note":n.checklist?"Checklist":"Edit note");
        title=new EditText(context);title.setHint("Title");title.setTextSize(24);title.setTextColor(ink);title.setHintTextColor(muted);title.setText(n.title);editor.addView(title);
        body=null;boolean trashed=n.deletedAt>0;
        if(n.checklist){
            ScrollView scroll=new ScrollView(context);LinearLayout rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);editor.addView(scroll,new LinearLayout.LayoutParams(-1,dp(320)));
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
                rowIn(editor,new String[]{"+ Item","↑","↓","Indent","Outdent","Remove"},new Runnable[]{()->listAction("add"),()->listAction("up"),()->listAction("down"),()->listAction("indent"),()->listAction("outdent"),()->listAction("remove")});
                rowIn(editor,new String[]{n.hideChecked?"Show checked":"Hide checked","To text"},new Runnable[]{()->listAction("hide"),this::convertNote});
            }
        }else{
            body=new EditText(context);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);body.setHint("Write your note…");body.setTextColor(ink);body.setHintTextColor(muted);body.setGravity(android.view.Gravity.TOP);body.setText(n.body);
            TextStyles.write(body.getText(),n.marks);body.setEnabled(!trashed);editor.addView(body,new LinearLayout.LayoutParams(-1,dp(320)));
            body.addTextChangedListener(watcher(()->{if(!binding){invalidateFullUndo();scheduleDraft();}}));
            if(!trashed){
                HorizontalScrollView tools=new HorizontalScrollView(context);LinearLayout strip=new LinearLayout(context);tools.addView(strip);
                String[] labels={"Bold","Italic","Underline","Strike","Heading 1","Heading 2","Clear"};String[] types={"bold","italic","underline","strike","h1","h2","clear"};
                for(int i=0;i<labels.length;i++){final String type=types[i];Button action=button(labels[i],()->format(type));action.setMinWidth(dp(64));strip.addView(action,new LinearLayout.LayoutParams(-2,dp(48)));}editor.addView(tools);
            }
        }
        if(!n.diagram.nodes.isEmpty())editor.addView(text("Diagram · "+n.diagram.nodes.size()+" shapes · "+n.diagram.edges.size()+" arrows",14));
        rowIn(editor,new String[]{trashed?"View diagram":"Diagram","Export PDF"},new Runnable[]{this::renderDiagram,host::exportPdf});
        if(!trashed)rowIn(editor,new String[]{"Scan image","Record voice","Voice inbox"},new Runnable[]{this::chooseScan,host::requestVoiceCapture,this::renderVoiceInbox});
        if(n.reminderAt>0)editor.addView(text("Reminder · "+java.text.DateFormat.getDateTimeInstance().format(new Date(n.reminderAt))+(n.reminderAt<=System.currentTimeMillis()?" · past due":" · approximate delivery"),14));
        if(!trashed)rowIn(editor,new String[]{"Reminder","Clear reminder"},new Runnable[]{this::chooseReminder,()->setReminder(editing,0)});
        title.setEnabled(!trashed);title.addTextChangedListener(watcher(()->{if(!binding){invalidateFullUndo();scheduleDraft();}}));
        if(trashed)rowIn(editor,new String[]{"Back","Restore","Share"},new Runnable[]{()->back(),()->changeState("restore"),this::shareSelected});
        else{
            rowIn(editor,new String[]{"Labels","Colour","Share","Copy"},new Runnable[]{this::renderLabels,this::renderColours,this::shareSelected,this::duplicateNote});
            rowIn(editor,new String[]{"Back",n.pinned?"Unpin":"Pin",n.archived?"Unarchive":"Archive","Trash"},new Runnable[]{()->back(),()->changeState("pin"),()->changeState("archive"),()->changeState("trash")});
            if(!n.checklist)rowIn(editor,new String[]{"Convert to checklist","Undo"},new Runnable[]{this::convertNote,this::undo});
        }
    }
    public void openNote(String id){whenReady(()->{persistDraft();Note n=find(id);if(n==null || n.deletedAt>0){host.showMessage("This note is unavailable or in Trash.");return;}selectedIds.clear();editing=n.id;renderEditor();});}
    public void chooseReminder(){whenReady(()->{
        persistDraft();Note note=find(editing);if(note==null || note.deletedAt>0)return;
        if(android.os.Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){host.requestReminderPermission();return;}
        final String id=note.id;Calendar time=Calendar.getInstance();time.setTimeInMillis(note.reminderAt>System.currentTimeMillis()?note.reminderAt:System.currentTimeMillis()+3600000);
        android.app.DatePickerDialog date=new android.app.DatePickerDialog(context,(view,year,month,day)->{
            time.set(Calendar.YEAR,year);time.set(Calendar.MONTH,month);time.set(Calendar.DAY_OF_MONTH,day);
            new android.app.TimePickerDialog(context,(clock,hour,minute)->{time.set(Calendar.HOUR_OF_DAY,hour);time.set(Calendar.MINUTE,minute);time.set(Calendar.SECOND,0);time.set(Calendar.MILLISECOND,0);if(time.getTimeInMillis()<=System.currentTimeMillis()){host.showMessage("Choose a future date and time.");return;}setReminder(id,time.getTimeInMillis());},time.get(Calendar.HOUR_OF_DAY),time.get(Calendar.MINUTE),android.text.format.DateFormat.is24HourFormat(context)).show();
        },time.get(Calendar.YEAR),time.get(Calendar.MONTH),time.get(Calendar.DAY_OF_MONTH));date.getDatePicker().setMinDate(System.currentTimeMillis()-1000);date.show();
    });}
    private void setReminder(String id,long at){whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;captureUndo(Arrays.asList(note),true);replace(note.reminder(at,System.currentTimeMillis()));queueSave();if(id.equals(editing))renderEditor();host.showMessage(at==0?"Reminder cleared.":"Local reminder saved. Android may delay delivery during battery saving. Force-stop disables alarms until reopening; notification settings must stay enabled.");});}
    private void renderDiagram(){
        persistDraft();Note note=find(editing);if(note==null){renderHome();return;}
        diagramOpen=true;title=null;body=null;search=null;querySequence++;root.removeAllViews();
        ScrollView scroll=new ScrollView(context);LinearLayout panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);scroll.addView(panel);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        headerIn(panel,"Diagram");panel.addView(text("Select a shape, then edit or move it. Connect starts at the selected shape; choose the destination below. Arrow labels appear between shapes. Text labels are clipped in shapes; all text is retained in the library.",14));
        boolean readOnly=note.deletedAt>0;
        DiagramView canvas=new DiagramView(context,note.diagram,darkTheme,new DiagramView.Listener(){
            public void selected(String id){diagramSelection=id;}
            public void changed(Diagram value){if(!readOnly)saveDiagram(value);else renderDiagram();}
        });canvas.show(note.diagram,diagramSelection);panel.addView(canvas,new LinearLayout.LayoutParams(-1,-2));
        if(!readOnly){
            rowIn(panel,new String[]{"Process","Decision","Start / End","Input / Output","Ellipse","Text box"},new Runnable[]{()->addShape("process"),()->addShape("decision"),()->addShape("terminator"),()->addShape("data"),()->addShape("ellipse"),()->addShape("text")});
            rowIn(panel,new String[]{"Edit shape","Connect","Delete shape","Wider","Narrower","Taller","Shorter"},new Runnable[]{this::editShape,this::connectShape,()->diagramChange("delete"),()->diagramChange("wider"),()->diagramChange("narrower"),()->diagramChange("taller"),()->diagramChange("shorter")});
            rowIn(panel,new String[]{"←","↑","↓","→","Undo diagram"},new Runnable[]{()->diagramChange("left"),()->diagramChange("up"),()->diagramChange("down"),()->diagramChange("right"),this::undoDiagram});
        }
        panel.addView(text("Shapes — tap to select without dragging:",16));
        for(Diagram.Node node:note.diagram.nodes)panel.addView(button((node.id.equals(diagramSelection)?"Selected · ":"")+node.shape+" · "+node.text,()->{diagramSelection=node.id;canvas.show(find(editing).diagram,diagramSelection);}));
        panel.addView(text("Arrows — tap to edit label or remove:",16));
        for(Diagram.Edge edge:note.diagram.edges)panel.addView(button(note.diagram.find(edge.from).text+" → "+note.diagram.find(edge.to).text+" · "+edge.label,()->{if(!readOnly)editArrow(edge.id);}));
        rowIn(panel,new String[]{"Back to note","Export PDF"},new Runnable[]{()->{diagramOpen=false;renderEditor();},host::exportPdf});
    }
    private void saveDiagram(Diagram value){Note n=find(editing);if(n==null || n.deletedAt>0)return;captureUndo(Arrays.asList(n),true);replace(n.diagram(value,System.currentTimeMillis()));queueSave();}
    private void addShape(String shape){Note n=find(editing);if(n==null || n.deletedAt>0)return;
        int index=n.diagram.nodes.size();String id=UUID.randomUUID().toString();int w=shape.equals("decision")?220:200,h=shape.equals("decision")?140:100;
        Diagram.Node node=new Diagram.Node(id,shape,shape.equals("terminator")?"Start / End":shape.equals("decision")?"Decision?":"New "+shape,30+(index%3)*240,30+((index/3)%5)*170,w,h,n.color);
        saveDiagram(n.diagram.put(node));diagramSelection=id;renderDiagram();
    }
    private Diagram.Node selectedShape(){Note n=find(editing);Diagram.Node node=n==null?null:n.diagram.find(diagramSelection);if(node==null)host.showMessage("Select a shape first, using the canvas or shape list.");return node;}
    private void editShape(){Diagram.Node node=selectedShape();if(node==null)return;
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);EditText input=new EditText(context);input.setText(node.text);input.setHint("Shape text");content.addView(input);
        Spinner colors=new Spinner(context);colors.setAdapter(new ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,NotesBoard.NAMES));colors.setSelection(node.color);content.addView(colors);
        new android.app.AlertDialog.Builder(context).setTitle("Edit shape").setView(content).setPositiveButton("Save",(d,w)->{saveDiagram(find(editing).diagram.put(node.edit(input.getText().toString(),colors.getSelectedItemPosition())));renderDiagram();}).setNegativeButton("Cancel",null).show();
    }
    private void connectShape(){Diagram.Node from=selectedShape();if(from==null)return;List<Diagram.Node> others=new ArrayList<>();for(Diagram.Node n:find(editing).diagram.nodes)if(!n.id.equals(from.id))others.add(n);
        if(others.isEmpty()){host.showMessage("Add another shape first.");return;}String[] names=new String[others.size()];for(int i=0;i<names.length;i++)names[i]=others.get(i).shape+" · "+others.get(i).text;
        new android.app.AlertDialog.Builder(context).setTitle("Arrow destination").setItems(names,(dialog,index)->{saveDiagram(find(editing).diagram.connect(from.id,others.get(index).id,""));renderDiagram();}).setNegativeButton("Cancel",null).show();
    }
    private void editArrow(String id){Note n=find(editing);for(Diagram.Edge edge:n.diagram.edges)if(edge.id.equals(id)){
        EditText input=new EditText(context);input.setText(edge.label);input.setHint("Arrow label, e.g. Yes / No");new android.app.AlertDialog.Builder(context).setTitle("Arrow label").setView(input).setPositiveButton("Save",(d,w)->{saveDiagram(find(editing).diagram.labelEdge(id,input.getText().toString()));renderDiagram();}).setNeutralButton("Remove arrow",(d,w)->{saveDiagram(find(editing).diagram.removeEdge(id));renderDiagram();}).setNegativeButton("Cancel",null).show();return;
    }}
    private void diagramChange(String action){Diagram.Node node=selectedShape();if(node==null)return;Diagram value=find(editing).diagram;
        switch(action){
            case "delete":value=value.remove(node.id);diagramSelection=null;break;
            case "left":value=value.put(node.move(node.x-20,node.y));break;
            case "right":value=value.put(node.move(node.x+20,node.y));break;
            case "up":value=value.put(node.move(node.x,node.y-20));break;
            case "down":value=value.put(node.move(node.x,node.y+20));break;
            case "wider":value=value.put(node.resize(node.width+40,node.height));break;
            case "narrower":value=value.put(node.resize(node.width-40,node.height));break;
            case "taller":value=value.put(node.resize(node.width,node.height+30));break;
            case "shorter":value=value.put(node.resize(node.width,node.height-30));break;
        }
        saveDiagram(value);renderDiagram();
    }
    private void undoDiagram(){undo();if(editing!=null)renderDiagram();}
    public void exportPdf(android.net.Uri uri){whenReady(()->{
        persistDraft();List<Note> snapshot=new ArrayList<>(targets());if(snapshot.isEmpty()){host.showMessage("Open or select notes before exporting PDF.");return;}
        busy=true;disk.execute(()->{
            java.io.File temporary=null;String message;
            try{
                LocalBackupFiles.requireLocal(uri);temporary=java.io.File.createTempFile("mynotes-pdf-",".pdf",context.getCacheDir());int pages;
                try(java.io.OutputStream out=new java.io.FileOutputStream(temporary)){pages=PdfExporter.write(snapshot,out);}
                try(java.io.InputStream in=new java.io.FileInputStream(temporary);java.io.OutputStream out=context.getContentResolver().openOutputStream(uri,"rwt")){
                    if(out==null)throw new java.io.IOException("Cannot open PDF destination");byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);out.flush();
                }
                String expected;try(java.io.InputStream in=new java.io.FileInputStream(temporary)){expected=streamDigest(in);}
                try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)){if(!expected.equals(streamDigest(in)))throw new java.io.IOException("PDF readback mismatch");}
                message="PDF verified · "+pages+" pages. Notes and editable diagrams remain in MyNotes.";log("pdf","verified pages="+pages);
            }catch(Exception e){message="PDF not verified: "+e.getMessage()+". Notes are unchanged; a partial destination file may need removing.";log("pdf","failed "+e.getClass().getSimpleName());}
            finally{if(temporary!=null)temporary.delete();}
            final String result=message;main.post(()->{busy=false;if(!closed)host.showMessage(result);});
        });
    });}
    private static String streamDigest(java.io.InputStream in) throws Exception {
        if(in==null)throw new java.io.IOException("Cannot reopen PDF");java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)digest.update(buffer,0,count);
        StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
    }
    private void chooseScan(){persistDraft();String[] names={"English (bundled)","Hindi / Devanagari (bundled)","Kannada (bundled)"};String[] codes={"eng","hin","kan"};new android.app.AlertDialog.Builder(context).setTitle("Offline OCR language").setItems(names,(d,index)->host.chooseOcrImage(codes[index])).setNegativeButton("Cancel",null).show();}
    public void recognizeImage(android.net.Uri uri,String language){whenReady(()->{
        persistDraft();if(ocrCancellation!=null)ocrCancellation.cancel();final OfflineOcr.Cancellation cancellation=new OfflineOcr.Cancellation();ocrCancellation=cancellation;final long ticket=++ocrSequence;final String target=editing;
        ocrPanel=true;title=null;body=null;search=null;querySequence++;root.removeAllViews();header("Offline OCR");root.addView(text("Recognizing on this device with a bundled "+language+" model. No image is uploaded. Other languages/handwriting and difficult photos may be inaccurate; review before saving.",16));root.addView(new ProgressBar(context));row(new String[]{"Cancel OCR"},new Runnable[]{this::cancelOcr});
        ocrWorker.execute(()->{
            try{OfflineOcr.Result result=OfflineOcr.recognize(context,uri,language,cancellation);main.post(()->{if(closed || ticket!=ocrSequence || cancellation.cancelled())return;renderOcrReview(result,target);});}
            catch(Exception | LinkageError | OutOfMemoryError e){log("ocr","failed "+e.getClass().getSimpleName());main.post(()->{if(closed || ticket!=ocrSequence || cancellation.cancelled())return;ocrPanel=false;returnToNoteOrHome();host.showMessage("OCR did not change any note: "+e.getMessage()+". Original image is unchanged; try another image or language.");});}
        });
    });}
    private void cancelOcr(){if(ocrCancellation!=null)ocrCancellation.cancel();ocrSequence++;ocrPanel=false;returnToNoteOrHome();}
    private void renderOcrReview(OfflineOcr.Result result,String target){
        root.removeAllViews();header("Review recognized text");root.addView(text("Offline · "+result.language+" · engine confidence "+result.confidence+" / 100 (not an accuracy guarantee). Check names, numbers and line breaks. Original image is unchanged.",14));
        EditText input=new EditText(context);input.setTextColor(ink);input.setHintTextColor(muted);input.setHint("Recognized text — edit before saving");input.setGravity(android.view.Gravity.TOP);input.setText(result.text);root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        row(new String[]{target==null?"Save as note":"Append to note","Save as new note","Cancel"},new Runnable[]{()->saveOcrText(input.getText().toString(),target),()->saveOcrText(input.getText().toString(),null),this::cancelOcr});
        if(result.text.isEmpty())host.showMessage("No text recognized. Nothing has been added. Try a sharper image or another language.");
    }
    private void saveOcrText(String value,String target){
        if(value.trim().isEmpty()){host.showMessage("There is no recognized text to save. Notes remain unchanged.");return;}
        Note current=target==null?null:find(target);long now=System.currentTimeMillis();
        if(target!=null && (current==null || current.deletedAt>0)){host.showMessage("Original note is unavailable or in Trash. Use Save as new note.");return;}
        if(current==null){current=Note.create(now).edit("Scanned text",value,now);notes.add(current);}
        else{captureUndo(Arrays.asList(current),true);if(current.checklist){List<Note.Item> rows=new ArrayList<>(current.items);for(String line:value.split("\\n",-1))rows.add(new Note.Item(UUID.randomUUID().toString(),line,false,0));current=current.list(current.title,rows,current.hideChecked,now);}else current=current.rich(current.title,current.body+(current.body.isEmpty()?"":"\n\n")+value,current.marks,now);replace(current);}
        ocrPanel=false;editing=current.id;queueSave();renderEditor();host.showMessage("Reviewed OCR text saved locally. Original image is unchanged.");
    }
    public void startVoiceCapture(){whenReady(()->{
        persistDraft();if(capture!=null){host.showMessage("A recording is already active.");return;}
        try{
            capture=new VoiceCapture(context,new VoiceCapture.Listener(){
                public void started(String id){main.post(()->{if(closed)return;voicePage=true;title=null;body=null;search=null;root.removeAllViews();header("Recording voice");root.addView(text("Microphone active · audio stays on this device. Stop to save and automatically transcribe offline in English. Leaving the app stops capture. Other languages/accents may be inaccurate; review the transcript against audio.",17));row(new String[]{"Stop and transcribe"},new Runnable[]{()->{if(capture!=null)capture.stop();if(status!=null)status.setText("Finishing durable audio…");}});});}
                public void saved(String id){VoiceTasks.transcribe(context,id);main.post(()->{capture=null;if(closed)return;addVoicePlaceholder(id);if(voicePage)renderVoiceInbox();host.showMessage("Audio saved locally. Offline English transcription started automatically.");});}
                public void failed(String message){main.post(()->{capture=null;if(closed)return;voicePage=false;returnToNoteOrHome();host.showMessage("Recording stopped: "+message+". Other features remain available; check Voice inbox for recovered audio.");});}
            });captureWorker.execute(capture);
        }catch(Exception e){capture=null;host.showMessage("Recording could not start: "+e.getMessage()+". Notes are unchanged.");}
    });}
    private void addVoicePlaceholder(String id){String noteId=app.space.notes.domain.VoiceTranscripts.noteId(id);if(find(noteId)==null){notes.add(app.space.notes.domain.VoiceTranscripts.placeholder(id,System.currentTimeMillis()));queueSave();}}
    private void voiceComplete(String id,String text,String failure){
        if(closed)return;if(busy){main.postDelayed(()->voiceComplete(id,text,failure),200);return;}
        whenReady(()->{
        if(closed)return;persistDraft();addVoicePlaceholder(id);Note note=find(app.space.notes.domain.VoiceTranscripts.noteId(id));
        if(!text.isEmpty() && note!=null && note.deletedAt==0){Note changed=app.space.notes.domain.VoiceTranscripts.apply(note,id,text,System.currentTimeMillis());if(changed!=note){replace(changed);queueSave();}}
        if(voicePage)renderVoiceInbox();else if(editing==null && !storagePage)filterNow();
        if(!failure.isEmpty())host.showMessage("Offline transcription unavailable. Audio is retained in Voice inbox; retry or export it. Other notes features are unaffected.");
    });}
    private void renderVoiceInbox(){
        persistDraft();voicePage=true;storagePage=true;editing=null;title=null;body=null;search=null;querySequence++;root.removeAllViews();header("Voice inbox");
        root.addView(text("Original WAV recordings are kept privately, independent of note Trash. JSON backups exclude audio. Export important recordings to your local folder/SD card; never clear app data before doing so. Retry uses the bundled English model, never a cloud service.",14));
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(context);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(text("Reading local recordings…",14));
        row(new String[]{"Record voice","Refresh","Back"},new Runnable[]{host::requestVoiceCapture,this::renderVoiceInbox,()->{voicePage=false;stopPlayback();renderHome();}});
        disk.execute(()->{
            try{VoiceFiles files=new VoiceFiles(context);List<String> ids=files.list();Map<String,org.json.JSONObject> results=new LinkedHashMap<>();for(String id:ids){try{results.put(id,files.result(id));}catch(Exception invalid){results.put(id,new org.json.JSONObject().put("state","Transcript unreadable; audio retained").put("text",""));}}
                main.post(()->{if(closed || !voicePage)return;content.removeAllViews();if(ids.isEmpty())content.addView(text("No recordings yet. Microphone access is optional and only requested when you tap Record voice.",15));
                    for(String id:ids){java.io.File audio=files.audio(id);org.json.JSONObject result=results.get(id);content.addView(text(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(audio.lastModified()))+" · "+audio.length()+" bytes",17));content.addView(text(result.optString("state"),14));String transcript=result.optString("text","");if(!transcript.isEmpty())content.addView(text(transcript.length()>500?transcript.substring(0,500)+"…":transcript,15));
                        rowIn(content,new String[]{"Play","Stop playback","Export WAV","Retry transcript","Open note"},new Runnable[]{()->playVoice(id),this::stopPlayback,()->host.exportVoice(id),()->{VoiceTasks.transcribe(context,id);host.showMessage("Offline transcription queued. Original audio is retained.");},()->{addVoicePlaceholder(id);voicePage=false;editing=app.space.notes.domain.VoiceTranscripts.noteId(id);renderEditor();}});
                    }
                });
            }catch(Exception e){main.post(()->{if(!closed && voicePage){content.removeAllViews();content.addView(text("Voice inbox could not be read. Files were not deleted. "+e.getMessage(),15));}});}
        });
    }
    private void playVoice(String id){stopPlayback();try{java.io.File file=new VoiceFiles(context).audio(id);android.media.MediaPlayer player=new android.media.MediaPlayer();voicePlayer=player;player.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build());player.setDataSource(file.getPath());player.setOnPreparedListener(p->p.start());player.setOnCompletionListener(p->stopPlayback());player.setOnErrorListener((p,what,extra)->{stopPlayback();host.showMessage("Playback failed. Original WAV is retained; try exporting it.");return true;});player.prepareAsync();}catch(Exception e){stopPlayback();host.showMessage("Cannot play recording: "+e.getMessage());}}
    private void stopPlayback(){if(voicePlayer!=null){voicePlayer.release();voicePlayer=null;}}
    public void exportVoice(String id,android.net.Uri uri){whenReady(()->{disk.execute(()->{
        String message;try{LocalBackupFiles.requireLocal(uri);java.io.File audio=new VoiceFiles(context).audio(id);String expected;try(java.io.InputStream in=new java.io.FileInputStream(audio)){expected=streamDigest(in);}try(java.io.InputStream in=new java.io.FileInputStream(audio);java.io.OutputStream out=context.getContentResolver().openOutputStream(uri,"rwt")){if(out==null)throw new java.io.IOException("Cannot write WAV destination");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.flush();}try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)){if(!expected.equals(streamDigest(in)))throw new java.io.IOException("WAV readback mismatch");}message="WAV export verified. Private original is also retained.";}
        catch(Exception e){message="WAV export not verified: "+e.getMessage()+". Private original is retained; a partial destination may need removing.";}
        final String result=message;main.post(()->{if(!closed)host.showMessage(result);});
    });});}
    public void pause(){if(capture!=null)capture.stop();stopPlayback();flush();}
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
        for(Note n:chosen){if(text.length()>0)text.append("\n\n────────\n\n");if(!n.title.isEmpty())text.append(n.title).append('\n');text.append(n.plainText());}
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
        CheckBox additive=new CheckBox(context);additive.setText("Add to existing labels instead of replacing");additive.setTextColor(ink);root.addView(additive);
        row(new String[]{"Save labels","Cancel"},new Runnable[]{()->{
            try{List<String> labels=Note.normalizeLabels(Arrays.asList(input.getText().toString().split(",",-1)));captureUndo(chosen,false);
                for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.properties(current.color,additive.isChecked()?LabelOperations.add(current.labels,labels):labels,System.currentTimeMillis()));}
                queueSave();returnToNoteOrHome();log("labels","applied count="+chosen.size());
            }catch(IllegalArgumentException e){host.showMessage(e.getMessage());}
        },this::returnToNoteOrHome});
    }
    private void manageLabels(){
        persistDraft();editing=null;selectedIds.clear();title=null;body=null;search=null;querySequence++;root.removeAllViews();storagePage=true;header("Manage labels");
        root.addView(text("Rename or remove labels across Notes, Archive and Trash. Notes are never deleted. Renaming to an existing label combines them; Undo on Home restores the previous labels. Unused labels are not retained.",14));
        ScrollView scroll=new ScrollView(context);LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TreeSet<String> known=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note n:notes)known.addAll(n.labels);
        for(String label:known){long count=notes.stream().filter(n->n.labels.stream().anyMatch(t->t.equalsIgnoreCase(label))).count();list.addView(button(label+" · "+count+" notes",()->{
            EditText input=new EditText(context);input.setText(label);input.setHint("Label name");android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("Rename label").setView(input).setPositiveButton("Rename",null).setNeutralButton("Remove label",(d,w)->renameLabel(label,"")).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(input.getText().toString().trim().isEmpty()){host.showMessage("Enter a label name, or use Remove label.");return;}if(renameLabel(label,input.getText().toString()))dialog.dismiss();}));dialog.show();
        }));}
        row(new String[]{"Back"},new Runnable[]{this::renderHome});
    }
    private boolean renameLabel(String old,String replacement){
        try{List<Note> changed=LabelOperations.rename(notes,old,replacement,System.currentTimeMillis());captureUndo(new ArrayList<>(notes),false);notes.clear();notes.addAll(changed);activeLabel="";queueSave();manageLabels();return true;}
        catch(IllegalArgumentException e){host.showMessage(e.getMessage());return false;}
    }
    public void receiveText(IncomingText incoming){whenReady(()->{
        if(capture!=null)capture.stop();persistDraft();Note note=Note.create(System.currentTimeMillis()).edit(incoming.title,incoming.body,System.currentTimeMillis());notes.add(note);selectedIds.clear();editing=note.id;queueSave();renderEditor();host.showMessage("Shared text saved locally. Source is unchanged.");log("share-in","text accepted");
    });}
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
    private void renderModelInfo(){
        persistDraft();editing=null;title=null;body=null;search=null;storagePage=true;root.removeAllViews();header("Offline models & licenses");
        TextView details=text("Reading bundled notices…",14);ScrollView scroll=new ScrollView(context);scroll.addView(details);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));row(new String[]{"Back"},new Runnable[]{this::renderStorage});
        disk.execute(()->{StringBuilder value=new StringBuilder("All inference runs locally on CPU. No network permission, platform SpeechRecognizer or model downloader. OCR: Tesseract4Android 4.9.0, tessdata_fast 4.1.0 English/Hindi/Kannada. Speech: Vosk 0.3.75 with bundled English small-en-us-0.15. Accuracy varies: review recognized text against the original. First use installs verified models in private storage; speech needs additional disk/RAM. Microphone is optional and only requested on Record voice. Voice inbox originals are excluded from JSON backup and never automatically deleted.\n\n");try{for(String name:context.getAssets().list("licenses")){value.append("\n\n===== ").append(name).append(" =====\n");try(java.io.InputStream in=context.getAssets().open("licenses/"+name)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);value.append(bytes.toString("UTF-8"));}}}catch(Exception e){value.append("Notices could not be read: ").append(e.getMessage());}main.post(()->{if(!closed && details.getParent()!=null)details.setText(value);});});
    }
    private void renderStorage() {
        storagePage=true;prepared=null;editing=null;search=null;title=null;body=null;querySequence++;main.removeCallbacks(filter);
        voicePage=false;root.removeAllViews();header("Storage & recovery");
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        rowIn(content,new String[]{"Voice inbox","Offline models & licenses"},new Runnable[]{this::renderVoiceInbox,this::renderModelInfo});
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
            try { repository.save(snapshot);markVoiceNotesCreated(snapshot); log("save","ok revision="+ticket+" count="+snapshot.size());
                main.post(()->{if(!closed && !dirty && ticket==revision && status!=null)status.setText("Saved · "+repository.status());});
            } catch(Exception e) { log("save","failed "+e.getClass().getSimpleName());
                main.post(()->{if(!closed){if(status!=null)status.setText(repository.status()+" · retry needed");
                    host.showMessage("Save incomplete. "+repository.status()+". Check storage access, then use Retry save.");}});
            }
        });
    }
    private void markVoiceNotesCreated(List<Note> snapshot){try{VoiceFiles files=new VoiceFiles(context);for(Note note:snapshot)for(String source:note.voiceSources)if(note.id.equals(app.space.notes.domain.VoiceTranscripts.noteId(source)))files.noteCreated(source);}catch(Exception pendingMarker){log("voice-source-marker","pending; original audio retained");}}
    private void log(String action,String outcome) { Log.i("MyNotes",action+" outcome="+outcome); }
    public boolean back() {
        if(busy){host.showMessage("Please wait for the current storage operation.");return true;}
        if(ocrPanel){cancelOcr();return true;}
        if(voicePage){if(capture!=null){capture.stop();host.showMessage("Stopping capture and preserving audio…");return true;}voicePage=false;stopPlayback();renderHome();return true;}
        if(diagramOpen){diagramOpen=false;renderEditor();return true;}
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
        closed=true;if(ocrCancellation!=null)ocrCancellation.cancel();if(capture!=null)capture.stop();stopPlayback();if(unsubscribeVoice!=null)unsubscribeVoice.run();deferredResults.clear(); main.removeCallbacksAndMessages(null); searchWorker.shutdown();
    }
}
