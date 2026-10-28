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
    private final FrameLayout shell;
    private final NotesDesign design;
    private final NotesSheets sheets;
    private LinearLayout formattingPanel,writingDock,editorTopChrome,homeTopChrome,homeDockChrome;
    private Button undoControl;
    private String caretNote;private int caretAnchor,caretFocus;
    private Runnable pageBack;
    private String pendingParagraphStyle;

    private NotesRepository repository;
    private final int bg, ink, surface, muted;
    private final Bundle restored;
    private boolean ready, closed, binding, dirty, busy;
    private String page="Notes", editing;
    private long revision,persistedRevision,querySequence;
    private TextView status;
    private EditText title, body, search;
    private static final class WritingField extends EditText {
        Runnable selectionChanged;
        WritingField(Context c){super(c);setSelectAllOnFocus(false);}
        @Override public boolean onTouchEvent(android.view.MotionEvent event){
            // Keep selection-handle drags in the native editor, not its enclosing ScrollView.
            if(getParent()!=null){if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN || (event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE && hasSelection()))getParent().requestDisallowInterceptTouchEvent(true);else if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE || event.getActionMasked()==android.view.MotionEvent.ACTION_UP || event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);}
            return super.onTouchEvent(event);
        }
        @Override protected void onSelectionChanged(int start,int end){super.onSelectionChanged(start,end);if(selectionChanged!=null)selectionChanged.run();}
    }

    private NotesBoard board;
    private TextView emptyHint;
    private boolean darkTheme,grid=true;
    private String activeLabel="",homeQuery="",focusedItem;
    private boolean sortCreated,diagramOpen;
    private app.space.notes.domain.LibraryFilter libraryFilter=app.space.notes.domain.LibraryFilter.defaults();
    private DrawingPad drawingPad;private String drawingNote;private long stashedDrawing=-1;
    private String activeFolder;private long folderPageSequence;
    private String diagramSelection,diagramHistoryNote;
    private app.space.notes.domain.DiagramHistory diagramHistory=new app.space.notes.domain.DiagramHistory();
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
    private final Set<String> knownRecordings=ConcurrentHashMap.newKeySet();
    private VoiceCapture capture;
    private final List<VoicePlaybackCard> voiceCards=new ArrayList<>();private long voiceInboxSequence;
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
        Diagnostics.initialize(context);
        boolean dark=NotesAppearance.dark(context,host);
        darkTheme=dark;
        android.content.SharedPreferences ui=context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE);
        grid=ui.getBoolean("grid",true);sortCreated=ui.getBoolean("sortCreated",false);libraryFilter=new app.space.notes.domain.LibraryFilter(true,true,Collections.emptyList(),ui.getString("sort",sortCreated?"created":"edited"),grid);
        if(restored!=null && restored.containsKey("filterAll")){List<String> labels=restored.getStringArrayList("filterLabels");libraryFilter=new app.space.notes.domain.LibraryFilter(restored.getBoolean("filterAll"),restored.getBoolean("filterUnlabelled"),labels==null?Collections.emptyList():labels,libraryFilter.sort,grid);homeQuery=restored.getString("homeQuery","");}
        design=new NotesDesign(context,dark,host.palette());bg=design.background;ink=design.ink;surface=design.surface;muted=design.muted;
        root=new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(14),dp(18),dp(10)); root.setBackgroundColor(bg);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(Diagnostics.enabled() && (r-l!=or-ol || b-t!=ob-ot))Diagnostics.record("ui.viewport","width_px="+(r-l)+" height_px="+(b-t)+" font_scale="+context.getResources().getConfiguration().fontScale+" night="+darkTheme);});
        shell=new LibraryGestureLayout(context,this::canSwipeLibrary,()->{View launcher=root.findViewWithTag("library-launcher");if(launcher!=null)launcher.performClick();});shell.setBackgroundColor(bg);shell.addView(root,new FrameLayout.LayoutParams(-1,-1));sheets=new NotesSheets(design,shell);shell.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->adaptEditorViewport());root.getViewTreeObserver().addOnGlobalLayoutListener(this::adaptEditorViewport);
        TextView loading=text("Opening your notes…",18); root.addView(loading);
        unsubscribeVoice=VoiceTasks.subscribe(context,this::voiceComplete);
        disk.execute(()->{
            try {
                repository=new NotesRepository(context.getApplicationContext());
                List<Note> loaded=repository.load();
                try{LocalSystemSurfaces.refresh(context,true);}catch(Exception unavailable){log("reminder.refresh","OS refresh pending; notes retained");}
                boolean recovered=false,catalogReadable=true;Set<String> pendingVoiceDeletionMarkers=new HashSet<>();
                try{VoiceFiles files=new VoiceFiles(context);for(String id:files.list()){
                    knownRecordings.add(id);String voiceId=app.space.notes.domain.VoiceTranscripts.noteId(id);Note current=null;for(Note n:loaded)if(n.id.equals(voiceId))current=n;
                    // Even an untranscribed/failed placeholder was deliberately created and must stay deleted after purge.
                    if(current!=null)try{files.noteCreated(id);}catch(Exception pendingMarker){pendingVoiceDeletionMarkers.add(voiceId);log("voice-source-marker","purge delayed until durable deletion guard is writable");}
                    org.json.JSONObject result;try{result=files.result(id);}catch(Exception invalidTranscript){log("voice-recovery","one unreadable transcript skipped; audio retained");continue;}if(!result.optString("state").equals("DONE"))continue;
                    if(current==null && files.noteWasCreated(id))continue;
                    if(current==null){current=app.space.notes.domain.VoiceTranscripts.placeholder(id,System.currentTimeMillis());loaded.add(current);recovered=true;}
                    Note changed=app.space.notes.domain.VoiceTranscripts.apply(current,id,result.optString("text",""),System.currentTimeMillis());if(changed!=current){loaded.set(loaded.indexOf(current),changed);recovered=true;}
                }}catch(Exception voiceFailure){catalogReadable=false;log("voice-recovery","audio retained; automatic purge delayed until voice catalogue can be checked");}
                if(recovered){try{repository.save(loaded);markVoiceNotesCreated(loaded);}catch(Exception pendingVoiceSave){log("voice-recovery","transcript recovery save needs retry");}}
                long now=System.currentTimeMillis();final boolean canCheckVoiceGuards=catalogReadable;
                boolean purged=loaded.removeIf(n->n.expired(now) && canCheckVoiceGuards && !pendingVoiceDeletionMarkers.contains(n.id));
                if(purged) { try { repository.save(loaded); } catch(Exception pending) { log("purge","private recovery retained; folder pending"); } }
                main.post(()->{ if(closed)return; notes.addAll(loaded); ready=true;
                    if(restored!=null) {
                        page=restored.getString("page","Notes");activeFolder=restored.getString("activeFolder");
                        String id=restored.getString("editing");
                        if(id!=null && find(id)!=null) {
                            Note n=find(id);
                            try {byte[] draft=restored.getByteArray("draftDocument");if(draft!=null){List<Note> decoded=DocumentCodec.decode(draft);if(decoded.size()==1 && decoded.get(0).id.equals(id))n=decoded.get(0);}}catch(Exception invalid){log("draft-restore","invalid snapshot ignored");}
                            replace(n);editing=id;if(id.equals(restored.getString("drawingNote"))){renderDrawing(id);queueSave();deliverDeferred();return;}caretNote=id;caretAnchor=restored.getInt("bodyAnchor",0);caretFocus=restored.getInt("bodyFocus",caretAnchor);renderEditor();if(body!=null && restored.getBoolean("bodyFocused",false))body.requestFocus();queueSave(); deliverDeferred(); return;
                        }
                    }
                    renderHome(); log("load","ok count="+notes.size()); deliverDeferred();
                });
            } catch(Exception e) { main.post(()->{
                if(closed)return; clearPage(); root.addView(text("Library could not be opened. Existing data was not replaced.",18));
                root.addView(text("Close and retry. Do not clear app data.",14));
                log("load","failed "+e.getClass().getSimpleName());Diagnostics.failure("load.failure",e);
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
    private boolean canSwipeLibrary(){return editing==null && !storagePage && !voicePage && search!=null && !sheets.open();}
    public View view() { return shell; }
    private int dp(int n) { return Math.round(n*context.getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size) {
        TextView v=new TextView(context); v.setText(value); v.setTextSize(size); v.setTextColor(ink);
        v.setPadding(0,dp(6),0,dp(6)); return v;
    }
    private void clearPage(){drawingPad=null;drawingNote=null;stashedDrawing=-1;disposePlayback();voiceInboxSequence++;folderPageSequence++;editorTopChrome=null;homeTopChrome=null;homeDockChrome=null;pageBack=null;undoControl=null;sheets.reset();formattingPanel=null;writingDock=null;pendingParagraphStyle=null;root.removeAllViews();}
    private void act(String label,Runnable action){if(busy){host.showMessage("Storage change in progress. Please wait.");return;}Diagnostics.record("ui.tap","control="+Diagnostics.controlName(label)+" screen="+(editing==null?page:"editor"));action.run();}
    private Button button(String label,Runnable action){return design.textAction(label,()->act(label,action));}
    private Button icon(String label,String symbol,Runnable action){return design.icon(label,symbol,()->act(label,action));}
    private LinearLayout menu(String name,Button launcher){LinearLayout panel=sheets.panel(name,launcher);launcher.setOnClickListener(v->act(name,()->{sheets.show(panel,name);android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(root.getWindowToken(),0);}));return panel;}
    private void menuRows(LinearLayout panel,String[] names,Runnable[] actions){for(int i=0;i<names.length;i++){Button row=button(names[i],actions[i]);panel.addView(row,new LinearLayout.LayoutParams(-1,-2));}}
    private LinearLayout tools(LinearLayout parent){LinearLayout bar=new LinearLayout(context);bar.setGravity(android.view.Gravity.CENTER_VERTICAL);bar.setPadding(0,dp(6),0,dp(6));parent.addView(bar);return bar;}
    private void tool(LinearLayout bar,String label,String symbol,Runnable action){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(48),dp(48));p.setMargins(dp(3),0,dp(3),0);bar.addView(icon(label,symbol,action),p);}
    /** Progressive disclosure keeps action labels readable without filling the editor with buttons. */
    private LinearLayout section(LinearLayout parent,String name){
        LinearLayout panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);
        Button toggle=button(name+"  ▾",()->{});toggle.setContentDescription(name);toggle.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);toggle.setMinHeight(dp(48));
        panel.setTag(toggle);panel.setVisibility(View.GONE);toggle.setOnClickListener(v->{boolean open=panel.getVisibility()!=View.VISIBLE;panel.setVisibility(open?View.VISIBLE:View.GONE);toggle.setText(name+(open?"  ▴":"  ▾"));if(open)panel.post(()->panel.requestRectangleOnScreen(new android.graphics.Rect(0,0,panel.getWidth(),Math.min(panel.getHeight(),dp(240))),true));Diagnostics.record("ui.section",name+(open?" opened":" closed"));});
        parent.addView(toggle,new LinearLayout.LayoutParams(-1,-2));parent.addView(panel);return panel;
    }
    private void settingRow(LinearLayout parent,String name,String description,Runnable action){
        LinearLayout entry=new LinearLayout(context);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(4),dp(8),dp(4),dp(8));entry.addView(button(name,action),new LinearLayout.LayoutParams(-1,-2));if(!description.isEmpty()){TextView hint=text(description,13);hint.setTextColor(muted);hint.setPadding(dp(16),0,dp(16),dp(10));entry.addView(hint);}parent.addView(entry);parent.addView(design.divider());
    }
    private void chooseAppearance(){if(host.managesAppearance()){host.showMessage("Appearance follows the embedding host's design and settings.");return;}String[] ids={"system","light","dark"};new android.app.AlertDialog.Builder(context).setTitle("Appearance").setSingleChoiceItems(new String[]{"System","Light","Dark"},Arrays.asList(ids).indexOf(NotesAppearance.choice(context)),(dialog,index)->{persistDraft();NotesAppearance.choose(context,ids[index]);dialog.dismiss();host.appearanceChanged();}).setNegativeButton("Cancel",null).show();}
    private void renderSettings(){
        persistDraft();editing=null;title=null;body=null;search=null;querySequence++;main.removeCallbacks(filter);storagePage=true;voicePage=false;clearPage();header("Settings");
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        settingRow(content,"Storage & recovery","Main storage folder, recovery and portable backups",()->{renderStorage();pageBack=this::renderSettings;});
        settingRow(content,"Home widget","Place your pinned and recent notes on Home",host::requestHomeWidget);
        settingRow(content,"About & offline models","Privacy, recording storage, bundled models and licenses",this::renderModelInfo);
        settingRow(content,"Appearance",host.managesAppearance()?"Follows the embedding host":"System, Light or Dark",this::chooseAppearance);
        settingRow(content,"Diagnostics","Optional local issue logs — export only when you choose",this::renderDiagnostics);
        TextView about=text(host.versionLabel()+" · offline, no account",12);about.setTextColor(muted);content.addView(about);
    }
    private void renderDiagnostics(){persistDraft();editing=null;title=null;body=null;search=null;querySequence++;storagePage=true;clearPage();header("Diagnostics",this::renderSettings);ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(design.heading("Local diagnostics"));
        Switch enabled=new Switch(context);enabled.setText("Collect diagnostic logs");enabled.setTextColor(ink);enabled.setTextSize(16);enabled.setMinHeight(dp(56));enabled.setChecked(Diagnostics.enabled());content.addView(enabled);
        content.addView(text("Enable before reproducing a problem. App events and device context stay on this device until you choose to export.",14));
        TextView state=text("Local logs: "+Diagnostics.bytes()+" bytes · about 1 MiB maximum",12);state.setTextColor(muted);content.addView(state);
        enabled.setOnCheckedChangeListener((v,value)->{Diagnostics.setEnabled(value);state.setText(value?"Recording on this device":"Collection stopped. Existing logs stay until deleted.");});
        settingRow(content,"Export diagnostics","Reproduce the issue first. Review the exported text before sharing.",host::exportDiagnostics);
        settingRow(content,"Delete diagnostic logs","Removes app-local logs, not previously exported copies.",()->{Diagnostics.clear();state.setText("Local diagnostic logs deleted.");});
        content.addView(text("No note text, labels, search, clipboard, recordings, transcripts, images or storage paths are collected. No screenshots or system-wide logcat. Nothing uploads automatically. Review exports before sharing. Crashes and OS kills can leave gaps.",14));
    }
    private void row(String[] labels,Runnable[] actions) {rowIn(root,labels,actions);}
    private void rowIn(LinearLayout parent,String[] labels,Runnable[] actions){
        LinearLayout actionsRow=new LinearLayout(context);int count=0;for(String label:labels)if(!label.equals("Back"))count++;boolean stacked=count>2 || (context.getResources().getConfiguration().fontScale>1.3f && context.getResources().getConfiguration().screenWidthDp<600);actionsRow.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        for(int i=0;i<labels.length;i++){if(labels[i].equals("Back"))continue;Button action=button(labels[i],actions[i]);LinearLayout.LayoutParams lp=stacked?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);actionsRow.addView(action,lp);}if(actionsRow.getChildCount()>0)parent.addView(actionsRow);
    }
    private void header(String name,Runnable back){header(name);pageBack=back;}
    private void header(String name) {headerIn(root,name);}
    private void headerIn(LinearLayout parent,String name){
        LinearLayout bar=tools(parent);bar.addView(icon("Back","back",this::back),new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView heading=design.heading(name);bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        status=text("",12);status.setTextColor(muted);status.setVisibility(View.GONE);parent.addView(status);
    }
    private void decorateLibrary(LinearLayout panel){String[] icons={"note","archive","trash","folder","mic","settings"};for(int i=0;i<panel.getChildCount() && i<icons.length;i++)if(panel.getChildAt(i) instanceof Button){Button button=(Button)panel.getChildAt(i);NotesIcon glyph=new NotesIcon(icons[i],ink);glyph.setBounds(0,0,dp(24),dp(24));button.setCompoundDrawablesRelative(glyph,null,null,null);button.setCompoundDrawablePadding(dp(16));}}
    private void renderHome(){
        voicePage=false;ocrPanel=false;storagePage=false;prepared=null;editing=null;title=null;body=null;visible.clear();diagramOpen=false;clearPage();root.setPadding(dp(16),dp(4),dp(16),dp(8));
        LinearLayout top=tools(root);homeTopChrome=top;Button library=icon("Library","menu",()->{});library.setTag("library-launcher");top.addView(library,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView brand=design.heading(activeFolder!=null?(activeFolder.isEmpty()?"Main folder":activeFolder):(activeLabel.isEmpty()?page:activeLabel));brand.setSingleLine(true);brand.setEllipsize(android.text.TextUtils.TruncateAt.END);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));if(!undoBatch.isEmpty() || undo!=null)top.addView(icon("Undo","undo",this::undo),new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout navigation=menu("Library",library);menuRows(navigation,new String[]{"Notes","Archive","Trash","Folders","Voice inbox","Settings"},new Runnable[]{()->navigate("Notes"),()->navigate("Archive"),()->navigate("Trash"),()->renderFolders(false),this::renderVoiceInbox,this::renderSettings});decorateLibrary(navigation);
        status=text("",12);status.setVisibility(View.GONE);root.addView(status);
        LinearLayout searchBar=new LinearLayout(context);searchBar.setGravity(android.view.Gravity.CENTER_VERTICAL);searchBar.setBackground(design.shape(surface,24));root.addView(searchBar);
        search=new EditText(context);search.setSingleLine(true);design.entry(search,16);search.setPadding(dp(18),dp(12),dp(4),dp(12));search.setOnFocusChangeListener((v,f)->root.post(this::adaptEditorViewport));search.setHint("Search notes and labels");search.setText(homeQuery);searchBar.addView(search,new LinearLayout.LayoutParams(0,-2,1));
        Button filterControl=icon("Filters","filter",()->{});searchBar.addView(filterControl,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout filters=menu("Filter & view",filterControl);
        TreeSet<String> labels=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note note:notes)if(note.deletedAt==0)labels.addAll(note.labels);filters.addView(new LibraryFilterView(design,libraryFilter,labels,value->{libraryFilter=value;grid=value.grid;sortCreated=value.sort.equals("created");activeLabel="";selectedIds.clear();saveViewPreferences();renderHome();},this::manageLabels));
        search.addTextChangedListener(watcher(()->{homeQuery=search.getText().toString();main.removeCallbacks(filter);main.postDelayed(filter,150);querySequence++;}));
        selectionBar=new LinearLayout(context);selectionBar.setOrientation(LinearLayout.VERTICAL);LinearLayout selectionTools=tools(selectionBar);tool(selectionTools,"Done","close",()->{selectedIds.clear();updateSelection();});TextView count=text("",16);count.setTag("selection-count");selectionTools.addView(count,new LinearLayout.LayoutParams(0,-2,1));tool(selectionTools,"Pin","pin",()->bulkState("pin"));tool(selectionTools,"Archive","archive",()->bulkState("archive"));tool(selectionTools,page.equals("Trash")?"Restore":"Trash",page.equals("Trash")?"undo":"trash",()->bulkState(page.equals("Trash")?"restore":"trash"));Button selectionMore=icon("Selection actions","more",()->{});selectionTools.addView(selectionMore,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout selectedMenu=menu("Selected notes",selectionMore);selectedMenu.addView(button("Move to folder",()->renderFolders(true)));menuRows(selectedMenu,new String[]{"Labels","Colour","Share","Copy","Merge"},new Runnable[]{this::renderLabels,this::renderColours,this::shareSelected,this::duplicateSelected,this::mergeSelected});root.addView(selectionBar);
        board=new NotesBoard(context,design,new NotesBoard.Listener(){public void open(Note n){if(!selectedIds.isEmpty()){toggleSelection(n);return;}editing=n.id;renderEditor();}public void select(Note n){toggleSelection(n);}},grid);
        emptyHint=text(homeQuery.isEmpty()?"A little space for your ideas.\nCreate a note below. It stays on your device.":"No matching notes. Try another search.",17);emptyHint.setTextColor(muted);emptyHint.setGravity(android.view.Gravity.CENTER);emptyHint.setPadding(dp(20),dp(28),dp(20),dp(28));emptyHint.setVisibility(View.GONE);root.addView(emptyHint);root.addView(board,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout dock=tools(root);homeDockChrome=dock;tool(dock,"+ Checklist","checklist",this::newChecklist);tool(dock,"Record voice","mic",host::requestVoiceCapture);View gap=new View(context);dock.addView(gap,new LinearLayout.LayoutParams(0,1,1));Button create=button(context.getResources().getConfiguration().fontScale>1.3f?"Note":"New note",this::newNote);create.setContentDescription("New text note");create.setTooltipText("New text note");create.setTextColor(design.accentInk);create.setBackground(design.ripple(Color.parseColor("#E8C58E"),18));create.setMinHeight(dp(54));create.setPadding(dp(18),dp(8),dp(18),dp(8));dock.addView(create);
        homeChrome.clear();chromeHidden=false;for(View chrome:new View[]{top,searchBar,dock})homeChrome.put(chrome,chrome.getVisibility());updateSelection();filterNow();root.post(this::adaptEditorViewport);
    }
    private void saveViewPreferences(){context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).edit().putBoolean("grid",grid).putBoolean("sortCreated",sortCreated).putString("sort",libraryFilter.sort).apply();}
    private void toggleSelection(Note n){if(!selectedIds.add(n.id))selectedIds.remove(n.id);updateSelection();log("selection","count="+selectedIds.size());}
    private void updateSelection(){
        if(!selectedIds.isEmpty() && !chromeHidden){for(View view:homeChrome.keySet()){homeChrome.put(view,view.getVisibility());view.setVisibility(View.GONE);}chromeHidden=true;}
        else if(selectedIds.isEmpty() && chromeHidden){for(Map.Entry<View,Integer> entry:homeChrome.entrySet())entry.getKey().setVisibility(entry.getValue());chromeHidden=false;}
        if(selectionBar!=null){selectionBar.setVisibility(selectedIds.isEmpty()?View.GONE:View.VISIBLE);TextView count=selectionBar.findViewWithTag("selection-count");if(count!=null)count.setText(String.valueOf(selectedIds.size()));count.setContentDescription(selectedIds.size()+" selected notes");}
        if(board!=null)board.show(visible,selectedIds,!page.equals("Trash"));
        if(status!=null && !selectedIds.isEmpty())status.setText(selectedIds.size()+" selected · tap cards to change selection");root.post(this::adaptEditorViewport);
    }
    private void navigate(String name) { activeFolder=null;page=name;selectedIds.clear();activeLabel="";libraryFilter=new app.space.notes.domain.LibraryFilter(true,true,Collections.emptyList(),libraryFilter.sort,grid);renderHome(); }
    private void newNote() {
        long now=System.currentTimeMillis();Note n=Note.create(now).folder(activeFolder==null?"":activeFolder,now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);notes.add(n);editing=n.id;selectedIds.clear();
        queueSave(); renderEditor(); if(body!=null){body.requestFocus();body.post(()->{if(!closed && body!=null)((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(body,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});}
    }
    private void newChecklist(){long now=System.currentTimeMillis();Note n=Note.create(now).folder(activeFolder==null?"":activeFolder,now).convert(now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);notes.add(n);editing=n.id;selectedIds.clear();queueSave();renderEditor();}
    private void filterNow() {
        if(closed || editing!=null || search==null)return;
        final String query=search.getText().toString(), currentPage=page,label=activeLabel;
        final app.space.notes.domain.LibraryFilter filterState=libraryFilter;final String folder=activeFolder;
        final long ticket=++querySequence;
        final List<Note> snapshot=new ArrayList<>(notes);
        searchWorker.execute(()->{
            List<Note> result=new ArrayList<>();
            for(Note n:snapshot) {
                boolean included=currentPage.equals("Trash")?n.deletedAt>0:
                    n.deletedAt==0 && (currentPage.equals("Archive")==n.archived);
                if(included && (folder==null || n.folder.equalsIgnoreCase(folder)) && n.matches(query) && filterState.matches(n))result.add(n);
            }
            result.sort(filterState.comparator());
            main.post(()->{
                if(closed || editing!=null || ticket!=querySequence)return;
                visible.clear();visible.addAll(result);board.show(result,selectedIds,!currentPage.equals("Trash"));
                if(emptyHint!=null)emptyHint.setVisibility(result.isEmpty()?View.VISIBLE:View.GONE);
            });
        });
    }
    private void renderEditor(){
        voicePage=false;ocrPanel=false;diagramOpen=false;Note n=find(editing);if(n==null){renderHome();return;}main.removeCallbacks(filter);querySequence++;search=null;board=null;selectionBar=null;clearPage();root.setPadding(dp(20),dp(4),dp(20),dp(8));
        LinearLayout top=tools(root);editorTopChrome=top;tool(top,"Back","back",()->{persistDraft();renderHome();});View spacer=new View(context);top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));boolean trashed=n.deletedAt>0;if(!trashed){tool(top,n.pinned?"Unpin":"Pin",n.pinned?"pinned":"pin",()->changeState("pin"));tool(top,n.archived?"Unarchive":"Archive","archive",()->changeState("archive"));}undoControl=icon("Undo","undo",this::undo);top.addView(undoControl,new LinearLayout.LayoutParams(dp(48),dp(48)));syncUndoControl();
        ScrollView document=new ScrollView(context);document.setBackground(new NoteBackground(n.background,n.color==0?surface:NotesBoard.colour(n.color,darkTheme),darkTheme,context.getResources().getDisplayMetrics().density));document.setFillViewport(true);document.setClipToPadding(false);document.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)document.postOnAnimation(()->revealWritingCaret(document));});LinearLayout editor=new LinearLayout(context);editor.setOrientation(LinearLayout.VERTICAL);FrameLayout.LayoutParams column=new FrameLayout.LayoutParams(Math.min(dp(720),dp(context.getResources().getConfiguration().screenWidthDp-40)),-2,android.view.Gravity.CENTER_HORIZONTAL);document.addView(editor,column);root.addView(document,new LinearLayout.LayoutParams(-1,0,1));
        title=new EditText(context);design.entry(title,26);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setOnFocusChangeListener((v,f)->root.post(this::adaptEditorViewport));title.setHint("Title");title.setText(n.title);title.setEnabled(!trashed);editor.addView(title,new LinearLayout.LayoutParams(-1,-2));
        body=null;
        if(n.checklist){LinearLayout rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);editor.addView(rows);
            int checked=0;TextView footer=text("",12);
            for(Note.Item item:n.items){
                if(item.checked)checked++;
                LinearLayout line=new LinearLayout(context);line.setGravity(android.view.Gravity.CENTER_VERTICAL);line.setPadding(dp(item.depth*16),0,0,0);
                CheckBox tick=new CheckBox(context);tick.setContentDescription("Complete list item");tick.setChecked(item.checked);tick.setMinWidth(dp(48));tick.setMinHeight(dp(48));tick.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{design.accent,muted}));tick.setEnabled(!trashed);line.addView(tick);
                EditText input=new EditText(context);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setHint("List item");design.entry(input,18);input.setText(item.text);input.setEnabled(!trashed);
                input.setPaintFlags(item.checked?input.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG:input.getPaintFlags()&~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                line.addView(input,new LinearLayout.LayoutParams(0,-2,1));rows.addView(line);
                if(n.hideChecked && item.checked)line.setVisibility(View.GONE);
                input.setOnFocusChangeListener((v,focused)->{if(focused)focusedItem=item.id;root.post(this::adaptEditorViewport);});
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
                    long completed=changed.stream().filter(i->i.checked).count();footer.setText(completed+" checked");footer.setVisibility(completed>0?View.VISIBLE:View.GONE);
                });
            }
            footer.setText(checked+" checked");footer.setVisibility(checked>0?View.VISIBLE:View.GONE);rows.addView(footer);if(!trashed){Button addItem=button("+ List item",()->listAction("add"));addItem.setContentDescription("Add checklist item");rows.addView(addItem);}
        }else{
            body=new WritingField(context);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);design.entry(body,18);body.setOnFocusChangeListener((v,f)->root.post(this::adaptEditorViewport));body.setHint("Write your note…");body.setGravity(android.view.Gravity.TOP);body.setMinLines(1);body.setText(n.body);TextStyles.write(body.getText(),n.marks);if(n.id.equals(caretNote))body.setSelection(Math.max(0,Math.min(caretAnchor,body.length())),Math.max(0,Math.min(caretFocus,body.length())));body.setEnabled(!trashed);editor.addView(body,new LinearLayout.LayoutParams(-1,-2));body.addTextChangedListener(paragraphWatcher());((WritingField)body).selectionChanged=this::updateFormatting;editor.setOnClickListener(v->{if(body!=null && !trashed){body.requestFocus();body.setSelection(body.length());((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(body,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}});
        }
        for(app.space.notes.domain.NoteImage image:n.images){LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackground(design.shape(surface,16));card.addView(new NoteImageView(context,image),new LinearLayout.LayoutParams(-1,dp(220)));card.addView(text(image.name,13));if(!trashed)card.addView(button("Remove image",()->{persistDraft();Note current=find(n.id);if(current==null)return;captureUndo(Arrays.asList(current),true);List<app.space.notes.domain.NoteImage> keep=new ArrayList<>(current.images);keep.removeIf(value->value.id.equals(image.id));replace(current.images(keep,System.currentTimeMillis()));queueSave();renderEditor();}));editor.addView(card,new LinearLayout.LayoutParams(-1,-2));}
        if(!n.diagram.nodes.isEmpty()){Button diagram=button("Diagram · "+n.diagram.nodes.size()+(n.diagram.nodes.size()==1?" shape":" shapes"),this::renderDiagram);diagram.setBackground(design.ripple(surface,16));NotesIcon glyph=new NotesIcon("diagram",ink);glyph.setBounds(0,0,dp(24),dp(24));diagram.setCompoundDrawablesRelative(glyph,null,null,null);diagram.setCompoundDrawablePadding(dp(10));editor.addView(diagram);}
        if(n.reminderAt>0){LinearLayout reminderRow=new LinearLayout(context);reminderRow.setGravity(android.view.Gravity.CENTER_VERTICAL);reminderRow.setBackground(design.shape(surface,16));
            Button reminder=button("Reminder · "+java.text.DateFormat.getDateTimeInstance().format(new Date(n.reminderAt)),this::chooseReminder);reminder.setContentDescription("Edit reminder");reminder.setTextColor(design.accent);NotesIcon bell=new NotesIcon("bell",design.accent);bell.setBounds(0,0,dp(22),dp(22));reminder.setCompoundDrawables(bell,null,null,null);reminder.setCompoundDrawablePadding(dp(8));reminderRow.addView(reminder,new LinearLayout.LayoutParams(0,-2,1));reminderRow.addView(icon("Clear reminder","close",()->setReminder(n.id,0)),new LinearLayout.LayoutParams(dp(48),dp(48)));editor.addView(reminderRow,new LinearLayout.LayoutParams(-1,-2));}
        if(!n.labels.isEmpty()){TextView chips=text(String.join("  ·  ",n.labels),13);chips.setTextColor(design.accent);editor.addView(chips);}
        status=text(dirty || persistedRevision<revision?"Saving locally…":"Saved · on this device",11);status.setTextColor(muted);status.setPadding(0,dp(4),0,dp(4));status.setVisibility(View.GONE);root.addView(status,1);
        LinearLayout dock=tools(root);writingDock=dock;Button add=icon("Add to note","add",()->{});dock.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout capture=menu("Add to your note",add);menuRows(capture,new String[]{trashed?"View diagram":"Diagram"},new Runnable[]{this::renderDiagram});if(!trashed)menuRows(capture,new String[]{"Image","Drawing","Scan image","Record voice","Reminder"},new Runnable[]{()->{persistDraft();host.chooseNoteImage(n.id);},()->renderDrawing(n.id),this::chooseScan,host::requestVoiceCapture,this::chooseReminder});
        Button formattingControl=null;if(!trashed && !n.checklist){formattingControl=icon("Text formatting","format",this::toggleFormatting);dock.addView(formattingControl,new LinearLayout.LayoutParams(dp(48),dp(48)));}
        if(!trashed){dock.addView(icon("Colour","palette",this::renderColours),new LinearLayout.LayoutParams(dp(48),dp(48)));dock.addView(icon("Labels","label",this::renderLabels),new LinearLayout.LayoutParams(dp(48),dp(48)));}
        View spring=new View(context);dock.addView(spring,new LinearLayout.LayoutParams(0,1,1));Button more=icon("More actions","more",()->{});dock.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout actions=menu("Note actions",more);
        if(trashed)menuRows(actions,new String[]{"Restore","Share"},new Runnable[]{()->changeState("restore"),this::shareSelected});else{menuRows(actions,new String[]{"Share","Export PDF","Move to folder","Background"},new Runnable[]{this::shareSelected,host::exportPdf,()->renderFolders(true),this::renderBackgrounds});if(!n.checklist)menuRows(actions,new String[]{"Convert to checklist"},new Runnable[]{this::convertNote});else menuRows(actions,new String[]{"+ Item","↑","↓","Indent","Outdent","Remove",n.hideChecked?"Show checked":"Hide checked","To text"},new Runnable[]{()->listAction("add"),()->listAction("up"),()->listAction("down"),()->listAction("indent"),()->listAction("outdent"),()->listAction("remove"),()->listAction("hide"),this::convertNote});}
        TextView dates=text("Edited "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(n.updatedAt))+"\nCreated "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(n.createdAt)),12);dates.setTextColor(muted);actions.addView(dates);
        if(!trashed && !n.checklist){writingDock=dock;buildFormatting(formattingControl);}
        title.addTextChangedListener(watcher(()->{if(!binding){invalidateFullUndo();scheduleDraft();}}));root.post(this::adaptEditorViewport);
    }
    private void buildFormatting(Button launcher){
        formattingPanel=new LinearLayout(context);formattingPanel.setOrientation(LinearLayout.VERTICAL);formattingPanel.setBackground(design.shape(surface,14));formattingPanel.setVisibility(View.GONE);formattingPanel.setContentDescription("Text formatting panel");formattingPanel.setTag(launcher);LinearLayout.LayoutParams formatWidth=new LinearLayout.LayoutParams(Math.min(dp(640),dp(context.getResources().getConfiguration().screenWidthDp-40)),-2);formatWidth.gravity=android.view.Gravity.CENTER_HORIZONTAL;root.addView(formattingPanel,formatWidth);
        LinearLayout paragraphs=tools(formattingPanel);String[] labels={"Text","Heading 1","Heading 2"},names={"Text","H1","H2"},types={"text","h1","h2"};
        for(int i=0;i<labels.length;i++){final String type=types[i];Button pick=button(names[i],()->format(type));pick.setContentDescription(labels[i]);pick.setTooltipText(labels[i]);pick.setTag("style:"+types[i]);pick.setPadding(dp(8),dp(10),dp(8),dp(10));pick.setGravity(android.view.Gravity.CENTER);paragraphs.addView(pick,new LinearLayout.LayoutParams(0,-2,1));}
        Button close=icon("Close formatting","close",this::toggleFormatting);paragraphs.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));writingDock.setTag(close);paragraphs.setPadding(0,dp(2),0,dp(2));
        LinearLayout inline=tools(formattingPanel);inline.setPadding(0,dp(2),0,dp(2));String[] labels2={"Bold","Italic","Underline","Strike","Clear"},names2={"B","I","U","S","Clear"},types2={"bold","italic","underline","strike","clear"};
        for(int i=0;i<labels2.length;i++){final String type=types2[i];Button pick=i==4?icon("Clear","erase",()->format(type)):button(names2[i],()->format(type));pick.setTag("style:"+types2[i]);pick.setContentDescription(labels2[i]);pick.setTooltipText(labels2[i]);pick.setTypeface(null,i==0?Typeface.BOLD:i==1?Typeface.ITALIC:Typeface.NORMAL);if(i==2)pick.setPaintFlags(pick.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);if(i==3)pick.setPaintFlags(pick.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);pick.setGravity(android.view.Gravity.CENTER);inline.addView(pick,new LinearLayout.LayoutParams(0,-2,1));}
    }
    private void revealWritingCaret(ScrollView document){
        View focused=root.findFocus();if(!(focused instanceof EditText))return;EditText field=(EditText)focused;android.text.Layout layout=field.getLayout();if(layout==null || document.getHeight()<=0)return;
        int line=layout.getLineForOffset(Math.max(0,Math.min(field.length(),field.getSelectionEnd())));int top=field.getCompoundPaddingTop()+layout.getLineTop(line)-field.getScrollY();View current=field;
        while(current!=document && current.getParent() instanceof View){top+=current.getTop();current=(View)current.getParent();if(current!=document)top-=current.getScrollY();}
        if(current!=document)return;int bottom=top+layout.getLineBottom(line)-layout.getLineTop(line);int scroll=document.getScrollY(),height=document.getHeight()-document.getPaddingBottom();if(top<scroll)scroll=top;else if(bottom>scroll+height)scroll=bottom-height;document.scrollTo(0,Math.max(0,scroll));
    }
    private boolean keyboardVisible(){if(android.os.Build.VERSION.SDK_INT>=30){android.view.WindowInsets insets=shell.getRootWindowInsets();return insets!=null && insets.isVisible(android.view.WindowInsets.Type.ime());}android.graphics.Rect frame=new android.graphics.Rect();shell.getWindowVisibleDisplayFrame(frame);return shell.getRootView().getHeight()-frame.height()>dp(120);}
    /** Keep an actual writing viewport above a landscape IME; Back remains an Android system action. */
    private void adaptEditorViewport(){
        if(closed || shell.getHeight()<=0)return;boolean compact=shell.getHeight()<dp(240),changed=false;
        if(editorTopChrome!=null){changed|=viewportVisibility(editorTopChrome,compact?View.GONE:View.VISIBLE);View focused=root.findFocus();if(title!=null)changed|=viewportVisibility(title,compact && focused instanceof EditText && focused!=title?View.GONE:View.VISIBLE);if(writingDock!=null){int padding=compact?0:dp(6);if(writingDock.getPaddingTop()!=padding){writingDock.setPadding(0,padding,0,padding);changed=true;}}}
        if(homeTopChrome!=null && homeDockChrome!=null && selectedIds.isEmpty() && !chromeHidden){boolean searching=compact && keyboardVisible();changed|=viewportVisibility(homeTopChrome,searching?View.GONE:View.VISIBLE);changed|=viewportVisibility(homeDockChrome,searching?View.GONE:View.VISIBLE);}
        // A size/insets callback may run during traversal: schedule the changed child measurements afterwards.
        if(changed)root.post(()->{if(!closed){root.forceLayout();root.requestLayout();}});
    }
    private boolean viewportVisibility(View view,int visibility){if(view.getVisibility()==visibility)return false;view.setVisibility(visibility);return true;}
    private void toggleFormatting(){if(formattingPanel==null)return;boolean show=formattingPanel.getVisibility()!=View.VISIBLE;formattingPanel.setVisibility(show?View.VISIBLE:View.GONE);if(writingDock!=null)writingDock.setVisibility(show?View.GONE:View.VISIBLE);if(body!=null){body.requestFocus();if(show && shell.getHeight()<dp(240))((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(body.getWindowToken(),0);}updateFormatting();}
    private void updateFormatting(){
        if(binding || body==null || formattingPanel==null || formattingPanel.getVisibility()!=View.VISIBLE)return;String active=pendingParagraphStyle==null?"text":pendingParagraphStyle;int at=Math.max(0,body.getSelectionStart());List<Note.Mark> marks=TextStyles.read(body.getText());for(Note.Mark mark:marks)if(mark.start<=at && mark.end>=at && (mark.type.equals("h1") || mark.type.equals("h2")))active=mark.type;
        for(int i=0;i<formattingPanel.getChildCount();i++){View row=formattingPanel.getChildAt(i);if(!(row instanceof ViewGroup))continue;ViewGroup buttons=(ViewGroup)row;for(int j=0;j<buttons.getChildCount();j++){View view=buttons.getChildAt(j);Object tag=view.getTag();if(!(tag instanceof String) || !tag.toString().startsWith("style:"))continue;String type=tag.toString().substring(6);boolean selected=type.equals(active);if(!type.equals("text") && !type.startsWith("h"))for(Note.Mark mark:marks)if(mark.type.equals(type) && mark.start<=at && mark.end>at)selected=true;if(view.isSelected()!=selected){view.setSelected(selected);view.setBackground(design.ripple(selected?(darkTheme?0x335F4A2E:0x22D8B16D):Color.TRANSPARENT,8));}}}
    }
    private TextWatcher paragraphWatcher(){return new TextWatcher(){int newlineFrom=-1,newlineTo,changedAt;boolean wasHeading;
        public void beforeTextChanged(CharSequence value,int start,int count,int after){wasHeading=false;for(Note.Mark mark:TextStyles.read(body.getText()))if((mark.type.equals("h1") || mark.type.equals("h2")) && mark.start<=start && mark.end>=start)wasHeading=true;}
        public void onTextChanged(CharSequence value,int start,int before,int count){newlineFrom=-1;changedAt=start;if(binding)return;int end=Math.min(value.length(),start+count);for(int at=start;at<end;at++)if(value.charAt(at)=='\n'){newlineFrom=at+1;newlineTo=end;break;}}
        public void afterTextChanged(Editable value){if(binding)return;invalidateFullUndo();if(pendingParagraphStyle!=null && value.length()>0 && newlineFrom<0){String style=pendingParagraphStyle;pendingParagraphStyle=null;binding=true;try{TextStyles.write(value,TextFormatting.paragraph(TextStyles.read(value),value.toString(),style,Math.min(changedAt,value.length()),Math.min(changedAt,value.length())));body.requestLayout();body.invalidate();}finally{binding=false;}}if(wasHeading && newlineFrom>=0){binding=true;try{TextStyles.write(value,TextFormatting.paragraph(TextStyles.read(value),value.toString(),"text",newlineFrom,newlineTo));body.requestLayout();body.invalidate();}finally{binding=false;}}scheduleDraft();updateFormatting();}
    };}
    public void openNote(String id){whenReady(()->{persistDraft();Note n=find(id);if(n==null || n.deletedAt>0){host.showMessage("This note is unavailable or in Trash.");return;}selectedIds.clear();editing=n.id;renderEditor();});}
    public void chooseReminder(){whenReady(()->{
        persistDraft();Note note=find(editing);if(note==null || note.deletedAt>0)return;
        if(android.os.Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){host.requestReminderPermission();return;}
        android.app.NotificationManager notifications=(android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.NotificationChannel channel=notifications.getNotificationChannel(LocalReminders.CHANNEL);
        if(!notifications.areNotificationsEnabled() || (channel!=null && channel.getImportance()==android.app.NotificationManager.IMPORTANCE_NONE)){
            new android.app.AlertDialog.Builder(context).setTitle("Reminder notifications are off").setMessage("Enable MyNotes reminders in Android notification settings. A saved date alone cannot notify while notifications are blocked.").setPositiveButton("Notification settings",(dialog,which)->{android.content.Intent settings=new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.getPackageName());context.startActivity(settings);}).setNegativeButton("Cancel",null).show();return;
        }
        final String id=note.id;Calendar time=Calendar.getInstance();time.setTimeInMillis(note.reminderAt>System.currentTimeMillis()?note.reminderAt:System.currentTimeMillis()+3600000);
        android.app.DatePickerDialog date=new android.app.DatePickerDialog(context,(view,year,month,day)->{
            time.set(Calendar.YEAR,year);time.set(Calendar.MONTH,month);time.set(Calendar.DAY_OF_MONTH,day);
            new android.app.TimePickerDialog(context,(clock,hour,minute)->{time.set(Calendar.HOUR_OF_DAY,hour);time.set(Calendar.MINUTE,minute);time.set(Calendar.SECOND,0);time.set(Calendar.MILLISECOND,0);if(time.getTimeInMillis()<=System.currentTimeMillis()){host.showMessage("Choose a future date and time.");return;}setReminder(id,time.getTimeInMillis());},time.get(Calendar.HOUR_OF_DAY),time.get(Calendar.MINUTE),android.text.format.DateFormat.is24HourFormat(context)).show();
        },time.get(Calendar.YEAR),time.get(Calendar.MONTH),time.get(Calendar.DAY_OF_MONTH));date.getDatePicker().setMinDate(System.currentTimeMillis()-1000);date.show();
    });}
    private void setReminder(String id,long at){whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;captureUndo(Arrays.asList(note),true);replace(note.reminder(at,System.currentTimeMillis()));queueSave();if(id.equals(editing))renderEditor();host.showMessage(at==0?"Reminder cleared.":"Local reminder saved. Android may delay delivery during battery saving. Force-stop disables alarms until reopening; notification settings must stay enabled.");});}
    private void renderDiagram(){
        persistDraft();Note note=find(editing);if(note==null){renderHome();return;}diagramOpen=true;title=null;body=null;search=null;querySequence++;clearPage();root.setPadding(dp(16),dp(4),dp(16),dp(8));
        LinearLayout top=tools(root);tool(top,"Back to note","back",()->{diagramOpen=false;renderEditor();});TextView name=design.heading("Diagram");top.addView(name,new LinearLayout.LayoutParams(0,-2,1));tool(top,"Export PDF","export",host::exportPdf);
        status=text("",12);status.setVisibility(View.GONE);root.addView(status);boolean readOnly=note.deletedAt>0;ensureDiagramHistory(note);
        ScrollView scroll=new ScrollView(context);LinearLayout area=new LinearLayout(context);area.setOrientation(LinearLayout.VERTICAL);scroll.addView(area);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        DiagramView canvas=new DiagramView(context,note.diagram,design,new DiagramView.Listener(){public void selected(String id){diagramSelection=id;}public void changed(Diagram value){if(!readOnly)saveDiagram(value);else renderDiagram();}});canvas.show(note.diagram,diagramSelection);area.addView(canvas,new LinearLayout.LayoutParams(-1,-2));
        if(note.diagram.nodes.isEmpty()){TextView hint=text("Start with a shape. Tap it to select; drag to move. Use Connect to add an arrow.",14);hint.setTextColor(muted);area.addView(hint);}
        LinearLayout dock=tools(root);
        if(!readOnly){Button add=icon("Add shape","add",()->{});dock.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout shapes=menu("Add shape",add);for(String shape:Diagram.SHAPES){Button option=button(Diagram.shapeName(shape),()->addShape(shape));DiagramShapeIcon preview=new DiagramShapeIcon(shape,darkTheme);preview.setBounds(0,0,dp(56),dp(40));option.setCompoundDrawablesRelative(preview,null,null,null);option.setCompoundDrawablePadding(dp(16));shapes.addView(option,new LinearLayout.LayoutParams(-1,-2));}
            Button edit=button("Edit shape",()->{});dock.addView(edit,new LinearLayout.LayoutParams(0,-2,1));LinearLayout editTools=menu("Selected shape",edit);menuRows(editTools,new String[]{"Edit shape","Connect","Delete shape","Wider","Narrower","Taller","Shorter"},new Runnable[]{this::editShape,this::connectShape,()->diagramChange("delete"),()->diagramChange("wider"),()->diagramChange("narrower"),()->diagramChange("taller"),()->diagramChange("shorter")});
            Button undoDiagram=icon("Undo diagram","undo",()->stepDiagramHistory(false));undoDiagram.setTag("diagram-undo");undoDiagram.setEnabled(diagramHistory.canUndo(note.diagram));dock.addView(undoDiagram,new LinearLayout.LayoutParams(dp(48),dp(48)));
            Button redoDiagram=icon("Redo diagram","redo",()->stepDiagramHistory(true));redoDiagram.setTag("diagram-redo");redoDiagram.setEnabled(diagramHistory.canRedo(note.diagram));dock.addView(redoDiagram,new LinearLayout.LayoutParams(dp(48),dp(48)));
            Button grouping=button("Group",this::groupShapes);dock.addView(grouping,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout arrowTools=tools(area);arrowTools.addView(button("Edit arrows",this::chooseArrow));}

    }
    private void saveDiagram(Diagram value){Note n=find(editing);if(n==null || n.deletedAt>0)return;ensureDiagramHistory(n);diagramHistory.record(n.diagram,value);captureUndo(Arrays.asList(n),true);replace(n.diagram(value,System.currentTimeMillis()));queueSave();View undo=root.findViewWithTag("diagram-undo"),redo=root.findViewWithTag("diagram-redo");if(undo!=null)undo.setEnabled(diagramHistory.canUndo(value));if(redo!=null)redo.setEnabled(diagramHistory.canRedo(value));}
    private void ensureDiagramHistory(Note note){if(!note.id.equals(diagramHistoryNote)){diagramHistoryNote=note.id;diagramHistory=new app.space.notes.domain.DiagramHistory();}}
    private void stepDiagramHistory(boolean redo){Note n=find(editing);if(n==null || n.deletedAt>0)return;ensureDiagramHistory(n);Diagram next=redo?diagramHistory.redo(n.diagram):diagramHistory.undo(n.diagram);if(next==n.diagram)return;replace(n.diagram(next,System.currentTimeMillis()));queueSave();renderDiagram();}
    private void groupShapes(){Note n=find(editing);if(n==null || n.deletedAt>0)return;String[] names=new String[n.diagram.nodes.size()];boolean[] checked=new boolean[names.length];Set<String> ids=new HashSet<>();for(int i=0;i<names.length;i++){Diagram.Node shape=n.diagram.nodes.get(i);names[i]=Diagram.shapeName(shape.shape)+" · "+shape.text;checked[i]=shape.id.equals(diagramSelection);if(checked[i])ids.add(shape.id);}
        new android.app.AlertDialog.Builder(context).setTitle("Group shapes").setMultiChoiceItems(names,checked,(dialog,index,on)->{String id=n.diagram.nodes.get(index).id;if(on)ids.add(id);else ids.remove(id);}).setPositiveButton("Group",(dialog,which)->{if(ids.size()<2){host.showMessage("Choose at least two shapes to group.");return;}saveDiagram(n.diagram.group(ids));renderDiagram();}).setNeutralButton("Ungroup selected",(dialog,which)->{Diagram.Node shape=n.diagram.find(diagramSelection);if(shape==null || shape.group.isEmpty()){host.showMessage("Tap a grouped shape first.");return;}saveDiagram(n.diagram.ungroup(shape.id));renderDiagram();}).setNegativeButton("Cancel",null).show();
    }
    private void chooseArrow(){Note n=find(editing);if(n==null || n.deletedAt>0)return;if(n.diagram.edges.isEmpty()){host.showMessage("Use Edit shape → Connect to add an arrow.");return;}String[] names=new String[n.diagram.edges.size()];for(int i=0;i<names.length;i++){Diagram.Edge edge=n.diagram.edges.get(i);names[i]=n.diagram.find(edge.from).text+" → "+n.diagram.find(edge.to).text+" · "+edge.label;}new android.app.AlertDialog.Builder(context).setTitle("Edit arrow").setItems(names,(dialog,index)->editArrow(n.diagram.edges.get(index).id)).setNegativeButton("Cancel",null).show();}
    private void addShape(String shape){Note n=find(editing);if(n==null || n.deletedAt>0)return;
        int index=n.diagram.nodes.size();String id=UUID.randomUUID().toString();int w=shape.equals("decision")?220:200,h=shape.equals("decision")?140:100;
        Diagram.Node node=new Diagram.Node(id,shape,Diagram.shapeName(shape),30+(index%3)*240,30+((index/3)%5)*170,w,h,n.color);
        saveDiagram(n.diagram.put(node));diagramSelection=id;renderDiagram();
    }
    private Diagram.Node selectedShape(){Note n=find(editing);Diagram.Node node=n==null?null:n.diagram.find(diagramSelection);if(node==null)host.showMessage("Tap a shape on the canvas first.");return node;}
    private void editShape(){Diagram.Node node=selectedShape();if(node==null)return;
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);EditText input=new EditText(context);input.setText(node.text);input.setHint("Shape text");content.addView(input);
        Spinner colors=new Spinner(context);String[] colourNames=NotesBoard.NAMES.clone();colourNames[0]="Paper";content.setPadding(dp(20),dp(8),dp(20),dp(8));design.entry(input,18);content.addView(design.label("Shape colour",13,muted));colors.setContentDescription("Shape colour");colors.setAdapter(new ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,colourNames));colors.setSelection(node.color);content.addView(colors);
        new android.app.AlertDialog.Builder(context).setTitle("Edit shape").setView(content).setPositiveButton("Save",(d,w)->{saveDiagram(find(editing).diagram.put(node.edit(input.getText().toString(),colors.getSelectedItemPosition())));renderDiagram();}).setNegativeButton("Cancel",null).show();
    }
    private void connectShape(){Diagram.Node from=selectedShape();if(from==null)return;List<Diagram.Node> others=new ArrayList<>();for(Diagram.Node n:find(editing).diagram.nodes)if(!n.id.equals(from.id))others.add(n);
        if(others.isEmpty()){host.showMessage("Add another shape first.");return;}String[] names=new String[others.size()];for(int i=0;i<names.length;i++)names[i]=Diagram.shapeName(others.get(i).shape)+" · "+others.get(i).text;
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
            }catch(Exception e){message="PDF not verified: "+e.getMessage()+". Notes are unchanged; a partial destination file may need removing.";log("pdf","failed "+e.getClass().getSimpleName());Diagnostics.failure("pdf.failure",e);}
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
        ocrPanel=true;title=null;body=null;search=null;querySequence++;clearPage();header("Offline OCR");root.addView(text("Recognizing on this device with a bundled "+language+" model. No image is uploaded. Other languages/handwriting and difficult photos may be inaccurate; review before saving.",16));root.addView(new ProgressBar(context));row(new String[]{"Cancel OCR"},new Runnable[]{this::cancelOcr});
        ocrWorker.execute(()->{
            try{OfflineOcr.Result result=OfflineOcr.recognize(context,uri,language,cancellation);main.post(()->{if(closed || ticket!=ocrSequence || cancellation.cancelled())return;renderOcrReview(result,target);});}
            catch(Exception | LinkageError | OutOfMemoryError e){log("ocr","failed "+e.getClass().getSimpleName());Diagnostics.failure("ocr.failure",e);main.post(()->{if(closed || ticket!=ocrSequence || cancellation.cancelled())return;ocrPanel=false;returnToNoteOrHome();host.showMessage("OCR did not change any note: "+e.getMessage()+". Original image is unchanged; try another image or language.");});}
        });
    });}
    private void cancelOcr(){if(ocrCancellation!=null)ocrCancellation.cancel();ocrSequence++;ocrPanel=false;returnToNoteOrHome();}
    private void renderOcrReview(OfflineOcr.Result result,String target){
        clearPage();header("Review recognized text");root.addView(text("Offline · "+result.language+" · confidence "+result.confidence+" / 100\nReview names, numbers and line breaks. Original image is unchanged.",13));
        EditText input=new EditText(context);design.entry(input,18);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setHint("Recognized text — edit before saving");input.setGravity(android.view.Gravity.TOP);input.setText(result.text);root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        if(target==null)row(new String[]{"Save as note"},new Runnable[]{()->saveOcrText(input.getText().toString(),null)});else row(new String[]{"Append to note","Save as new note"},new Runnable[]{()->saveOcrText(input.getText().toString(),target),()->saveOcrText(input.getText().toString(),null)});
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
                public void started(String id){main.post(()->{if(closed)return;voicePage=true;title=null;body=null;search=null;clearPage();header("Recording voice");
                    LinearLayout recording=new LinearLayout(context);recording.setOrientation(LinearLayout.VERTICAL);recording.setGravity(android.view.Gravity.CENTER);ScrollView recordingScroll=new ScrollView(context);recordingScroll.setFillViewport(true);recordingScroll.addView(recording);root.addView(recordingScroll,new LinearLayout.LayoutParams(-1,0,1));TextView live=design.heading("Microphone on");recording.addView(live);android.widget.Chronometer timer=new android.widget.Chronometer(context);timer.setTextColor(design.accent);timer.setTextSize(36);timer.setBase(android.os.SystemClock.elapsedRealtime());timer.start();recording.addView(timer);TextView privacy=text("Audio stays on this device",14);privacy.setTextColor(muted);recording.addView(privacy);
                    recording.addView(text("Stop to save the original audio and start bundled offline English transcription. Review the transcript; language, accent and noise affect accuracy. Leaving the app stops recording safely.",14));Button stop=button("Stop and transcribe",()->{if(capture!=null)capture.stop();timer.stop();live.setText("Saving audio…");});stop.setGravity(android.view.Gravity.CENTER);stop.setTextColor(design.accentInk);stop.setBackground(design.ripple(Color.parseColor("#E8C58E"),18));root.addView(stop,new LinearLayout.LayoutParams(-1,-2));});}
                public void saved(String id){VoiceTasks.transcribe(context,id);main.post(()->{capture=null;if(closed)return;addVoicePlaceholder(id);if(voicePage)renderVoiceInbox();host.showMessage("Audio saved locally. Offline English transcription started automatically.");});}
                public void failed(String message){main.post(()->{capture=null;if(closed)return;voicePage=false;returnToNoteOrHome();host.showMessage("Recording stopped: "+message+". Other features remain available; check Voice inbox for recovered audio.");});}
            });captureWorker.execute(capture);
        }catch(Exception e){capture=null;host.showMessage("Recording could not start: "+e.getMessage()+". Notes are unchanged.");}
    });}
    private void addVoicePlaceholder(String id){knownRecordings.add(id);String noteId=app.space.notes.domain.VoiceTranscripts.noteId(id);if(find(noteId)==null){notes.add(app.space.notes.domain.VoiceTranscripts.placeholder(id,System.currentTimeMillis()));queueSave();}}
    private void voiceComplete(String id,String text,String failure,boolean previouslyCreated){
        if(closed)return;if(busy){main.postDelayed(()->voiceComplete(id,text,failure,previouslyCreated),200);return;}
        whenReady(()->{
        if(closed)return;persistDraft();String target=app.space.notes.domain.VoiceTranscripts.noteId(id);
        if(!app.space.notes.domain.VoiceTranscripts.mayApplyToLibrary(find(target)!=null,previouslyCreated)){if(voicePage)renderVoiceInbox();return;}
        addVoicePlaceholder(id);Note note=find(target);
        if(!text.isEmpty() && note!=null && note.deletedAt==0){Note changed=app.space.notes.domain.VoiceTranscripts.apply(note,id,text,System.currentTimeMillis());if(changed!=note){replace(changed);queueSave();}}
        if(voicePage)renderVoiceInbox();else if(editing==null && !storagePage)filterNow();
        if(!failure.isEmpty())host.showMessage("Offline transcription unavailable. Audio is retained in Voice inbox; retry or export it. Other notes features are unaffected.");
    });}
    private void renderVoiceInbox(){
        persistDraft();voicePage=true;storagePage=true;editing=null;title=null;body=null;search=null;querySequence++;clearPage();header("Voice inbox");
        final long inboxToken=voiceInboxSequence;
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(context);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(text("Reading local recordings…",14));
        LinearLayout controls=tools(root);tool(controls,"Record voice","mic",host::requestVoiceCapture);View spacer=new View(context);controls.addView(spacer,new LinearLayout.LayoutParams(0,1,1));tool(controls,"Refresh","retry",this::renderVoiceInbox);
        disk.execute(()->{
            try{VoiceFiles files=new VoiceFiles(context);List<String> ids=files.list();Map<String,Integer> durations=new HashMap<>();for(String id:ids)try(java.io.RandomAccessFile wave=new java.io.RandomAccessFile(files.audio(id),"r")){durations.put(id,(int)Math.min(Integer.MAX_VALUE,app.space.notes.domain.PcmWave.inspect(wave).bytes*1000/(app.space.notes.domain.PcmWave.RATE*2)));}catch(Exception unavailable){durations.put(id,0);}Map<String,org.json.JSONObject> results=new LinkedHashMap<>();for(String id:ids){try{results.put(id,files.result(id));}catch(Exception invalid){results.put(id,new org.json.JSONObject().put("state","Transcript unreadable; audio retained").put("text",""));}}
                main.post(()->{if(closed || !voicePage || inboxToken!=voiceInboxSequence)return;content.removeAllViews();if(ids.isEmpty())content.addView(text("No recordings yet. Tap the microphone to record.",15));
                    for(String id:ids){org.json.JSONObject result=results.get(id);final VoicePlaybackCard[] self=new VoicePlaybackCard[1];self[0]=new VoicePlaybackCard(design,files.audio(id),result.optString("state"),result.optString("text"),durations.get(id),()->{for(VoicePlaybackCard other:voiceCards)if(other!=self[0])other.pause();},host::showMessage,()->host.exportVoice(id),()->{VoiceTasks.transcribe(context,id);host.showMessage("Offline transcription queued. Original audio is retained.");},()->{addVoicePlaceholder(id);voicePage=false;editing=app.space.notes.domain.VoiceTranscripts.noteId(id);renderEditor();});voiceCards.add(self[0]);content.addView(self[0]);}
                });
            }catch(Exception e){main.post(()->{if(!closed && voicePage && inboxToken==voiceInboxSequence){content.removeAllViews();content.addView(text("Voice inbox could not be read. Files were not deleted. "+e.getMessage(),15));}});}
        });
    }
    private void stopPlayback(){for(VoicePlaybackCard card:voiceCards)card.release();}
    private void disposePlayback(){for(VoicePlaybackCard card:voiceCards)card.dispose();voiceCards.clear();}
    public void exportVoice(String id,android.net.Uri uri){whenReady(()->{disk.execute(()->{
        String message;try{LocalBackupFiles.requireLocal(uri);java.io.File audio=new VoiceFiles(context).audio(id);String expected;try(java.io.InputStream in=new java.io.FileInputStream(audio)){expected=streamDigest(in);}try(java.io.InputStream in=new java.io.FileInputStream(audio);java.io.OutputStream out=context.getContentResolver().openOutputStream(uri,"rwt")){if(out==null)throw new java.io.IOException("Cannot write WAV destination");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.flush();}try(java.io.InputStream in=context.getContentResolver().openInputStream(uri)){if(!expected.equals(streamDigest(in)))throw new java.io.IOException("WAV readback mismatch");}message="WAV export verified. Private original is also retained.";}
        catch(Exception e){message="WAV export not verified: "+e.getMessage()+". Private original is retained; a partial destination may need removing.";}
        final String result=message;main.post(()->{if(!closed)host.showMessage(result);});
    });});}
    public void pause(){stashDrawing();if(capture!=null)capture.stop();stopPlayback();flush();}
    private void scheduleDraft(){dirty=true;if(status!=null)status.setText("Unsaved changes…");main.removeCallbacks(saveDraft);main.postDelayed(saveDraft,350);}
    private void syncUndoControl(){if(undoControl!=null){boolean available=undo!=null || !undoBatch.isEmpty();undoControl.setEnabled(available);undoControl.setAlpha(available?1f:.38f);}}
    private void invalidateFullUndo(){undoCreatedId=null;if(undoFull){undoBatch.clear();undoFull=false;}syncUndoControl();}
    private void captureUndo(List<Note> before,boolean full){undoCreatedId=null;undo=null;undoBatch=new ArrayList<>(before);undoFull=full;syncUndoControl();}
    private void format(String type){
        if(body==null)return;int anchor=body.getSelectionStart(),focus=body.getSelectionEnd();int start=Math.min(anchor,focus),end=Math.max(anchor,focus);if(start<0)return;
        boolean paragraph=type.equals("h1") || type.equals("h2") || type.equals("text");if(!paragraph && start==end){host.showMessage("Select text to apply inline formatting. Headings apply to the current paragraph.");return;}
        if(paragraph){String text=body.getText().toString();int lineStart=text.lastIndexOf('\n',Math.max(-1,start-1))+1;int lineEnd=text.indexOf('\n',start);if(lineEnd<0)lineEnd=text.length();if(lineStart==lineEnd){pendingParagraphStyle=type.equals("text")?null:type;updateFormatting();return;}}
        persistDraft();Note n=find(editing);if(n==null)return;captureUndo(Arrays.asList(n),true);
        List<Note.Mark> marks=paragraph?TextFormatting.paragraph(TextStyles.read(body.getText()),body.getText().toString(),type,start,end):TextFormatting.apply(TextStyles.read(body.getText()),type,start,end);
        binding=true;try{TextStyles.reflow(body,marks);body.setSelection(anchor,focus);}finally{binding=false;}
        dirty=true;persistDraft();updateFormatting();body.post(()->{if(body!=null && body.getLayout()!=null)Diagnostics.record("format.layout","lines="+body.getLayout().getLineCount()+" first_line_height="+body.getLayout().getLineBottom(0)+" viewport_height="+body.getHeight());});log("format","applied type="+type+" range="+start+".."+end);
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
    private void duplicateSelected(){List<Note> chosen=targets();if(editing!=null || chosen.isEmpty())return;for(Note n:chosen){if(n.deletedAt>0){host.showMessage("Restore trashed notes before copying.");return;}}for(Note n:chosen)notes.add(NoteOperations.duplicate(n,System.currentTimeMillis()));selectedIds.clear();queueSave();renderHome();host.showMessage("Copies created; originals unchanged.");}
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
        List<Note> chosen=propertyTargets();if(chosen.isEmpty())return;title=null;body=null;search=null;querySequence++;clearPage();header("Labels",this::returnToNoteOrHome);
        if(chosen.size()>1)root.addView(text(chosen.size()+" selected notes. Choose labels to add, or switch off Add to replace their label sets.",14));
        LinearLayout create=tools(root);EditText input=new EditText(context);design.entry(input,16);input.setHint("Create label");input.setSingleLine(true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(64)});create.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        ScrollView scroll=new ScrollView(context);LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        java.util.Map<String,CheckBox> choices=new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);TreeSet<String> known=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note n:notes)known.addAll(n.labels);
        java.util.function.BiConsumer<String,Boolean> add=(label,checked)->{CheckBox box=choices.get(label);if(box==null){box=new CheckBox(context);box.setText(label);box.setTextColor(ink);box.setTextSize(16);box.setMinHeight(dp(52));choices.put(label,box);list.addView(box);}box.setChecked(checked);};
        for(String label:known)add.accept(label,chosen.size()==1 && chosen.get(0).labels.stream().anyMatch(v->v.equalsIgnoreCase(label)));
        create.addView(icon("Create label","add",()->{try{List<String> value=Note.normalizeLabels(Arrays.asList(input.getText().toString()));if(value.isEmpty()){host.showMessage("Enter a label name.");return;}add.accept(value.get(0),true);input.setText("");}catch(IllegalArgumentException e){host.showMessage(e.getMessage());}}),new LinearLayout.LayoutParams(dp(48),dp(48)));
        CheckBox additive=new CheckBox(context);additive.setText("Add to existing labels instead of replacing");additive.setTextColor(ink);additive.setMinHeight(dp(52));additive.setChecked(chosen.size()>1);if(chosen.size()>1)root.addView(additive);
        row(new String[]{"Save labels","Cancel"},new Runnable[]{()->{try{List<String> checked=new ArrayList<>();for(java.util.Map.Entry<String,CheckBox> entry:choices.entrySet())if(entry.getValue().isChecked())checked.add(entry.getKey());List<String> labels=Note.normalizeLabels(checked);captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.properties(current.color,additive.isChecked()?LabelOperations.add(current.labels,labels):labels,System.currentTimeMillis()));}queueSave();returnToNoteOrHome();log("labels","applied count="+chosen.size());}catch(IllegalArgumentException e){host.showMessage(e.getMessage());}},this::returnToNoteOrHome});
    }
    private void manageLabels(){
        persistDraft();editing=null;selectedIds.clear();title=null;body=null;search=null;querySequence++;clearPage();storagePage=true;header("Manage labels");
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
        title=null;body=null;search=null;querySequence++;clearPage();header("Note colour",this::returnToNoteOrHome);
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        // Large system text needs wider cells: names must not break in the middle of a word.
        int columns=context.getResources().getConfiguration().fontScale>=1.5f?2:3;
        for(int row=0;row<(NotesBoard.NAMES.length+columns-1)/columns;row++){
            LinearLayout line=new LinearLayout(context);
            for(int col=0;col<columns;col++){
                final int colour=row*columns+col;if(colour>=NotesBoard.NAMES.length)break;Button pick=button(NotesBoard.NAMES[colour],()->{
                    captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.properties(colour,current.labels,System.currentTimeMillis()));}
                    queueSave();returnToNoteOrHome();log("colour","applied count="+chosen.size()+" colour="+colour);
                });
                GradientDrawable swatch=new GradientDrawable();swatch.setShape(GradientDrawable.OVAL);swatch.setColor(NotesBoard.colour(colour,darkTheme));swatch.setStroke(dp(chosen.size()==1 && chosen.get(0).color==colour?3:1),chosen.size()==1 && chosen.get(0).color==colour?design.accent:design.outline);swatch.setBounds(0,0,dp(44),dp(44));pick.setCompoundDrawables(null,swatch,null,null);pick.setCompoundDrawablePadding(dp(6));pick.setBackground(design.ripple(Color.TRANSPARENT,12));pick.setTextColor(ink);pick.setTextSize(13);pick.setGravity(android.view.Gravity.CENTER);pick.setSelected(chosen.size()==1 && chosen.get(0).color==colour);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);pick.setMinHeight(dp((int)(82+22*context.getResources().getConfiguration().fontScale)));lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(pick,lp);
            }content.addView(line);
        }
        row(new String[]{"Cancel"},new Runnable[]{this::returnToNoteOrHome});
    }
    private java.io.File drawingDraft(String id){return new java.io.File(context.getFilesDir(),"drawing-draft-"+UUID.nameUUIDFromBytes(id.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".png");}
    private void stashDrawing(){if(drawingPad==null || drawingNote==null || !drawingPad.ready() || stashedDrawing==drawingPad.revision())return;stashedDrawing=drawingPad.revision();android.graphics.Bitmap snapshot=drawingPad.snapshot();java.io.File target=drawingDraft(drawingNote);disk.execute(()->{android.util.AtomicFile file=new android.util.AtomicFile(target);java.io.FileOutputStream out=null;try{out=file.startWrite();if(!snapshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out))throw new java.io.IOException("Cannot save drawing draft");file.finishWrite(out);out=null;}catch(Exception failure){if(out!=null)file.failWrite(out);log("drawing-draft","save pending");}finally{snapshot.recycle();}});}
    private void leaveDrawing(){stashDrawing();renderEditor();}
    private void renderDrawing(String id){persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;clearPage();drawingNote=id;editing=id;title=null;body=null;search=null;storagePage=true;querySequence++;header("Drawing",this::leaveDrawing);DrawingPad pad=new DrawingPad(context);drawingPad=pad;
        ScrollView scroll=new ScrollView(context);scroll.addView(pad,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));TextView hint=text("Draw on the paper. Your unfinished drawing stays on this device until saved.",13);root.addView(hint);
        LinearLayout colors=tools(root);String[] names={"Black","Blue","Red","Eraser"};int[] values={Color.BLACK,0xff1555aa,0xffb3261e,Color.WHITE};for(int i=0;i<names.length;i++){final int value=values[i];colors.addView(button(names[i],()->pad.color(value)),new LinearLayout.LayoutParams(0,-2,1));}
        LinearLayout actions=tools(root);actions.addView(button("Undo",pad::undo),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("Redo",pad::redo),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("Save drawing",()->{if(!pad.ready()){host.showMessage("Drawing is still opening.");return;}Note current=find(id);if(current==null || current.deletedAt>0)return;if(current.images.size()>=8){host.showMessage("This note already has eight images. Your drawing draft is retained.");stashDrawing();return;}stashDrawing();android.graphics.Bitmap bitmap=pad.snapshot();disk.execute(()->{try{app.space.notes.domain.NoteImage image=NoteImageFiles.encode(bitmap,"Drawing");main.post(()->{if(closed)return;addImage(id,image,0,()->disk.execute(()->new android.util.AtomicFile(drawingDraft(id)).delete()));});}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed)host.showMessage("Drawing was not attached; its draft is retained. "+failure.getMessage());});}finally{bitmap.recycle();}});}),new LinearLayout.LayoutParams(0,-2,1));
        disk.execute(()->{android.graphics.Bitmap base=null;try{java.io.File file=drawingDraft(id);if(file.exists()){base=android.graphics.BitmapFactory.decodeFile(file.getPath());if(base==null)throw new java.io.IOException("Drawing draft could not be decoded");}}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed && drawingPad==pad)host.showMessage("Draft preview unavailable. The draft file was kept.");});return;}final android.graphics.Bitmap value=base;main.post(()->{if(closed || drawingPad!=pad){if(value!=null)value.recycle();return;}pad.loaded(value);});});
    }
    public void attachImage(String id,android.net.Uri uri){whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0){host.showMessage("Original note is unavailable; no image was imported.");return;}disk.execute(()->{try{app.space.notes.domain.NoteImage image=NoteImageFiles.importImage(context,uri);main.post(()->{if(!closed)addImage(id,image);});}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed)host.showMessage("Image was not added: "+failure.getMessage()+". Your note is unchanged.");});}});});}
    private void addImage(String id,app.space.notes.domain.NoteImage image){addImage(id,image,0,()->{});}
    private void addImage(String id,app.space.notes.domain.NoteImage image,int attempt,Runnable accepted){whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;try{if(note.images.stream().anyMatch(value->value.id.equals(image.id)))return;List<app.space.notes.domain.NoteImage> images=new ArrayList<>(note.images);images.add(image);Note changed=note.images(images,System.currentTimeMillis());List<Note> candidate=new ArrayList<>(notes);candidate.set(candidate.indexOf(note),changed);
        disk.execute(()->{try{BackupCodec.encode(candidate);main.post(()->{if(closed)return;persistDraft();if(find(id)!=note){if(attempt<3)addImage(id,image,attempt+1,accepted);else host.showMessage("The note changed while adding the image. Try again; drawing drafts are retained.");return;}captureUndo(Arrays.asList(note),true);replace(changed);queueSave();accepted.run();if(id.equals(editing))renderEditor();else host.showMessage("Image added to its original note.");});}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed)host.showMessage("Image not added: the complete library must still fit a verified backup. Your notes and drawing draft were kept.");});}});
    }catch(IllegalArgumentException failure){host.showMessage(failure.getMessage());}});}
    private void renderBackgrounds(){
        List<Note> chosen=propertyTargets();if(chosen.isEmpty())return;title=null;body=null;search=null;querySequence++;clearPage();header("Background",this::returnToNoteOrHome);
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<NoteBackground.NAMES.length;i++){final int value=i;Button option=button(NoteBackground.NAMES[i],()->{captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.background(value,System.currentTimeMillis()));}queueSave();returnToNoteOrHome();});option.setBackground(new NoteBackground(i,surface,darkTheme,context.getResources().getDisplayMetrics().density));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(72));lp.setMargins(0,dp(5),0,dp(5));content.addView(option,lp);}
    }
    private void renderFolders(boolean moving){
        persistDraft();List<Note> chosen=moving?propertyTargets():Collections.emptyList();if(moving && chosen.isEmpty())return;title=null;body=null;search=null;querySequence++;storagePage=true;clearPage();header(moving?"Move to folder":"Folders",moving?this::returnToNoteOrHome:this::renderHome);final long token=folderPageSequence;
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(text("Reading your main storage folder…",14));
        disk.execute(()->{try{List<String> folders=repository.folders().list();main.post(()->{if(closed || token!=folderPageSequence)return;content.removeAllViews();content.addView(text("Real folders inside your selected main storage location. Notes are stored as portable JSON files; the library also keeps recovery snapshots.",14));
            if(!moving)content.addView(button("All notes",()->{activeFolder=null;renderHome();}));
            java.util.function.Consumer<String> choose=folder->{if(moving){captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.folder(folder,System.currentTimeMillis()));}queueSave();activeFolder=null;returnToNoteOrHome();}else{activeFolder=folder;page="Notes";selectedIds.clear();editing=null;renderHome();}};
            content.addView(button("Main folder",()->choose.accept("")));for(String folder:folders){Button option=button(folder,()->choose.accept(folder));NotesIcon icon=new NotesIcon("folder",ink);icon.setBounds(0,0,dp(24),dp(24));option.setCompoundDrawablesRelative(icon,null,null,null);option.setCompoundDrawablePadding(dp(12));content.addView(option,new LinearLayout.LayoutParams(-1,-2));}
            content.addView(button("Create folder",()->{EditText input=new EditText(context);design.entry(input,16);input.setHint("Folder name");android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("Create real folder").setView(input).setPositiveButton("Create",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name;try{name=app.space.notes.domain.FolderNames.require(input.getText().toString());}catch(IllegalArgumentException invalid){input.setError(invalid.getMessage());return;}if(busy)return;busy=true;dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);disk.execute(()->{String error=null;try{repository.folders().create(name);}catch(Exception failure){error=failure.getMessage();}final String problem=error;main.post(()->{busy=false;if(closed)return;if(problem==null){dialog.dismiss();if(token==folderPageSequence)renderFolders(moving);}else{dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);input.setError(problem);}});});}));dialog.show();}));
        });}catch(Exception failure){main.post(()->{if(closed || token!=folderPageSequence)return;content.removeAllViews();content.addView(text(failure.getMessage()==null?"Main storage folder is unavailable":failure.getMessage(),15));content.addView(button("Storage settings",()->{renderStorage();pageBack=()->renderFolders(moving);}));});}});
    }
    private void renderModelInfo(){
        persistDraft();editing=null;title=null;body=null;search=null;storagePage=true;clearPage();header("About & offline models",this::renderSettings);
        TextView details=text("Reading bundled notices…",14);ScrollView scroll=new ScrollView(context);scroll.addView(details);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));row(new String[]{"Back"},new Runnable[]{this::renderStorage});
        disk.execute(()->{StringBuilder value=new StringBuilder("Original recordings stay privately on this device, independent of note Trash. JSON backups exclude audio. Export important recordings before clearing data or uninstalling. Transcripts use conservative pause-based commas and sentence breaks, not a semantic punctuation model. Questions and exclamations are not guessed. Review punctuation against audio; raw recognized text is retained with the recording.\n\nAll inference runs locally on CPU. No network permission, platform SpeechRecognizer or model downloader. OCR: Tesseract4Android 4.9.0, tessdata_fast 4.1.0 English/Hindi/Kannada. Speech: Vosk 0.3.75 with bundled English small-en-us-0.15. Accuracy varies: review recognized text against the original. First use installs verified models in private storage; speech needs additional disk/RAM. Microphone is optional and only requested on Record voice. Voice inbox originals are excluded from JSON backup and never automatically deleted.\n\n");try{for(String name:context.getAssets().list("licenses")){value.append("\n\n===== ").append(name).append(" =====\n");try(java.io.InputStream in=context.getAssets().open("licenses/"+name)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);value.append(bytes.toString("UTF-8"));}}}catch(Exception e){value.append("Notices could not be read: ").append(e.getMessage());}main.post(()->{if(!closed && details.getParent()!=null)details.setText(value);});});
    }
    private void renderStorage(){
        storagePage=true;prepared=null;editing=null;search=null;title=null;body=null;querySequence++;main.removeCallbacks(filter);voicePage=false;clearPage();header("Storage & recovery");
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView current=text(repository==null?"Opening storage":repository.status(),16);current.setTextColor(design.accent);content.addView(current);content.addView(text("A private recovery copy stays on this device. Folder changes are verified before switching; previous files are kept.",14));
        settingRow(content,"Choose folder","Choose the main folder on this device or SD card; the library is verified before switching.",host::chooseLocalFolder);
        settingRow(content,"Open library","Open an existing local folder and preview a non-destructive merge.",host::openExistingFolder);
        settingRow(content,"Backups","Back up notes, diagrams, folder placement and backgrounds. Export original recordings separately.",this::renderBackups);
        if(repository!=null && repository.status().toLowerCase(java.util.Locale.ROOT).contains("pending"))settingRow(content,"Retry save","Reconnect your local folder first; private edits are retained.",this::flush);
        settingRow(content,"Use device storage","Keep working from the private on-device library.",()->moveStorage(null));
        settingRow(content,"Folders","Create real subfolders and move notes",()->renderFolders(false));
        content.addView(text("Use local device or SD storage. Export a backup and important recordings before uninstalling or clearing app data. Previous library snapshots are kept for recovery.",13));
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
            } catch(Exception e) {log("storage-move","failed "+e.getClass().getSimpleName());Diagnostics.failure("storage-move.failure",e);message="Storage change incomplete: "+e.getMessage();}
            final String result=message;
            main.post(()->{busy=false;if(!closed){renderStorage();host.showMessage(result);}});
        });
    }
    private LinearLayout scrollContent(String heading) {
        editing=null;title=null;body=null;search=null;storagePage=true;querySequence++;main.removeCallbacks(filter);
        clearPage();header(heading);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return content;
    }
    private void renderBackups() {
        prepared=null;LinearLayout content=scrollContent("Backup & restore");pageBack=this::renderStorage;
        content.addView(text("Portable backups include text notes, checklists, diagrams, formatting, colours and labels. Original audio is separate: export important WAVs from Voice inbox.",16));
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
                catch(Exception e) {message="Backup not verified: "+e.getMessage();log("backup-export","failed "+e.getClass().getSimpleName());Diagnostics.failure("backup-export.failure",e);}
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
            } catch(Exception e) {log("restore-preview","failed "+e.getClass().getSimpleName());Diagnostics.failure("restore-preview.failure",e);
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
            } catch(Exception e) {log("restore","failed "+e.getClass().getSimpleName());Diagnostics.failure("restore.failure",e);
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
        if(body!=null && editing!=null){caretNote=editing;caretAnchor=Math.max(0,body.getSelectionStart());caretFocus=Math.max(0,body.getSelectionEnd());}
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
                main.post(()->{persistedRevision=Math.max(persistedRevision,ticket);if(!closed && !dirty && ticket==revision && status!=null){status.setText("Saved · "+repository.status());status.setVisibility(View.GONE);}});
            } catch(Exception e) { log("save","failed "+e.getClass().getSimpleName());Diagnostics.failure("save.failure",e);
                main.post(()->{if(!closed){if(status!=null){status.setText(repository.status()+" · retry needed");status.setVisibility(View.VISIBLE);}
                    host.showMessage("Save incomplete. "+repository.status()+". Check storage access, then use Retry save.");}});
            }
        });
    }
    private void markVoiceNotesCreated(List<Note> snapshot){try{VoiceFiles files=new VoiceFiles(context);Set<String> ids=new HashSet<>();for(Note note:snapshot)ids.add(note.id);for(String source:knownRecordings)if(ids.contains(app.space.notes.domain.VoiceTranscripts.noteId(source)))files.noteCreated(source);}catch(Exception pendingMarker){log("voice-source-marker","pending; original audio retained");}}
    private void log(String action,String outcome) { Log.i("MyNotes",action+" outcome="+outcome); Diagnostics.record(action,outcome); }
    public boolean back() {
        if(sheets.close())return true;
        if(formattingPanel!=null && formattingPanel.getVisibility()==View.VISIBLE){toggleFormatting();return true;}
        if(pageBack!=null){Runnable next=pageBack;pageBack=null;next.run();return true;}
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
    public void flush() { Diagnostics.record("lifecycle.flush","requested"); if(ready && !busy){persistDraft();queueSave();} }
    public void saveState(Bundle state) {
        stashDrawing();state.putString("drawingNote",drawingNote);persistDraft();state.putBoolean("filterAll",libraryFilter.allNotes);state.putBoolean("filterUnlabelled",libraryFilter.includeUnlabelled);state.putStringArrayList("filterLabels",new ArrayList<>(libraryFilter.labels));state.putString("homeQuery",homeQuery);state.putString("activeFolder",activeFolder);state.putString("page",page);state.putString("editing",editing);if(body!=null){state.putInt("bodyAnchor",body.getSelectionStart());state.putInt("bodyFocus",body.getSelectionEnd());state.putBoolean("bodyFocused",body.hasFocus());}
        if(editing!=null && find(editing)!=null){try{byte[] draft=DocumentCodec.encode(Arrays.asList(find(editing)));if(draft.length<=32768)state.putByteArray("draftDocument",draft);}catch(Exception e){log("draft-snapshot","failed "+e.getClass().getSimpleName());Diagnostics.failure("draft-snapshot.failure",e);}}
    }
    public void close() { Diagnostics.record("lifecycle.close","controller");
        closed=true;disposePlayback();if(ocrCancellation!=null)ocrCancellation.cancel();if(capture!=null)capture.stop();stopPlayback();if(unsubscribeVoice!=null)unsubscribeVoice.run();deferredResults.clear(); main.removeCallbacksAndMessages(null); searchWorker.shutdown();
    }
}
