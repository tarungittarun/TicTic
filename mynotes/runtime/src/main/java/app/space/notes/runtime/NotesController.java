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
import app.space.notes.domain.DocumentContent;
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
    private LinearLayout formattingPanel,writingDock,editorTopChrome,homeTopChrome,homeDockChrome,homeWorkspaceChrome;
    private EditText lastWritingFocus;private int lastEditorViewportHeight;
    private Button undoControl,redoTextControl;
    private final Map<EditText,TextEditHistory> textHistories=new IdentityHashMap<>();
    private boolean noteUndoIsLatest;
    private ScrollView noteScroll;
    private DocumentEditorView canonicalEditor;private final Bundle canonicalState=new Bundle();private boolean canonicalStateRestored;
    private PersistentHistoryStore historyStore;private boolean historyStepping,historyNoticeShown;
    private final LinkedHashMap<String,app.space.notes.domain.DocumentHistory> savedHistories=new LinkedHashMap<>(16,.75f,true);
    private app.space.notes.domain.DocumentHistory historyFor(String id){app.space.notes.domain.DocumentHistory h=savedHistories.get(id);if(h==null){h=new app.space.notes.domain.DocumentHistory();savedHistories.put(id,h);while(savedHistories.size()>PersistentHistoryStore.MAX_NOTES)savedHistories.remove(savedHistories.keySet().iterator().next());}return h;}
    private void trimSavedHistories(){long total=0;for(app.space.notes.domain.DocumentHistory h:savedHistories.values())total+=h.retainedCharacterCount();while(total>app.space.notes.domain.DocumentHistory.MAX_CHARACTERS && !savedHistories.isEmpty()){String oldest=savedHistories.keySet().iterator().next();total-=savedHistories.remove(oldest).retainedCharacterCount();}}
    private boolean hasSavedHistory(boolean redo){if(editing==null)return false;app.space.notes.domain.DocumentHistory h=savedHistories.get(editing);return h!=null && (redo?h.canRedo():h.canUndo());}
    private boolean stepSavedHistory(boolean redo){persistDraft();Note current=find(editing);if(current==null || current.deletedAt>0 || !hasSavedHistory(redo))return false;app.space.notes.domain.DocumentHistory h=historyFor(current.id);Note next=redo?h.redo(current):h.undo(current);historyStepping=true;try{replace(next.state(next.pinned,next.archived,next.deletedAt,System.currentTimeMillis()));}finally{historyStepping=false;}app.space.notes.domain.DocumentHistory.Cursor cursor=h.cursor(next);if(cursor!=null){caretNote=next.id;caretAnchor=cursor.anchor;caretFocus=cursor.focus;}queueSave();renderEditor();return true;}
    private app.space.notes.domain.DocumentHistory canonicalHistory;private String canonicalHistoryNote,canonicalDiagram,pendingContentAnchor,pendingContentNote,pendingContentDigest;private int pendingContentOffset=-1;
    private String checklistDragState="idle";
    private String caretNote;private int caretAnchor,caretFocus;
    private Runnable pageBack;
    private ImageEditView imageEditor;
    private String imageEditNote,imageEditItem,imageEditDigest;
    private long imageEditVersion;
    private String pendingParagraphStyle;

    private NotesRepository repository;
    private final int bg, ink, surface, muted;
    private final Bundle restored;
    private boolean ready, closed, binding, dirty, busy;
    private String page="Notes", editing;private boolean documentsWorkspace;
    private volatile long revision,persistedRevision,querySequence;
    private TextView status;
    private EditText title, body, search;
    static final class WritingField extends EditText {
        private boolean ruled;private final android.graphics.Paint rulingPaint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        void ruling(boolean enabled,int color){ruled=enabled;rulingPaint.setColor(color);rulingPaint.setStrokeWidth(getResources().getDisplayMetrics().density);invalidate();}
        @Override protected void onDraw(android.graphics.Canvas canvas){if(ruled && getLayout()!=null){android.graphics.Rect bounds=new android.graphics.Rect();android.graphics.Rect visible=new android.graphics.Rect();canvas.getClipBounds(visible);int first=getLayout().getLineForVertical(Math.max(0,visible.top-getTotalPaddingTop())),last=getLayout().getLineForVertical(Math.max(0,visible.bottom-getTotalPaddingTop()));for(int line=first;line<=last;line++){int baseline=getLineBounds(line,bounds);float y=baseline+2*getResources().getDisplayMetrics().density;canvas.drawLine(getCompoundPaddingLeft(),y,getWidth()-getCompoundPaddingRight(),y,rulingPaint);}}super.onDraw(canvas);}
        Runnable selectionChanged,beforeFocusTransition,afterFocusTransition;java.util.function.BooleanSupplier undoText,redoText;
        @Override public boolean onDragEvent(android.view.DragEvent event){if(event.getLocalState() instanceof ChecklistDrag)return false;if(DocumentEditorView.internalDrag(event))return false;if(event.getAction()==android.view.DragEvent.ACTION_DROP && event.getLocalState()==null && event.getClipData()!=null){for(int i=0;i<event.getClipData().getItemCount();i++){android.content.ClipData.Item item=event.getClipData().getItemAt(i);if(item.getUri()!=null || item.getIntent()!=null){android.widget.Toast.makeText(getContext(),"Use Open note file for compatible local documents. Only plain text can be dropped into writing fields.",android.widget.Toast.LENGTH_LONG).show();return false;}}}return super.onDragEvent(event);}
        @Override public boolean onTextContextMenuItem(int id){if(id==android.R.id.undo && undoText!=null)return undoText.getAsBoolean();if(id==android.R.id.redo && redoText!=null)return redoText.getAsBoolean();return super.onTextContextMenuItem(id);}
        WritingField(Context c){super(c);setSelectAllOnFocus(false);}
        // Lazy native selection-controller creation may normalize the first reverse range.
        // Retry only the exact application-requested range, once; native touch handles stay native.
        @Override public void setSelection(int start,int end){super.setSelection(start,end);if(start>end && (getSelectionStart()!=start || getSelectionEnd()!=end))super.setSelection(start,end);}

        @Override public boolean onTouchEvent(android.view.MotionEvent event){
            // Keep selection-handle drags in the native editor, not its enclosing ScrollView.
            if(getParent()!=null){if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN || (event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE && hasSelection()))getParent().requestDisallowInterceptTouchEvent(true);else if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE || event.getActionMasked()==android.view.MotionEvent.ACTION_UP || event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);}
            return super.onTouchEvent(event);
        }
        @Override protected void onFocusChanged(boolean focused,int direction,android.graphics.Rect previous){if(beforeFocusTransition!=null)beforeFocusTransition.run();try{super.onFocusChanged(focused,direction,previous);}finally{if(afterFocusTransition!=null)afterFocusTransition.run();}}
        @Override public void onWindowFocusChanged(boolean focused){if(beforeFocusTransition!=null)beforeFocusTransition.run();try{super.onWindowFocusChanged(focused);}finally{if(afterFocusTransition!=null)afterFocusTransition.run();}}
        @Override protected void onSelectionChanged(int start,int end){super.onSelectionChanged(start,end);if(selectionChanged!=null)selectionChanged.run();}
    }

    private NotesBoard board;
    private TextView emptyHint;
    private boolean darkTheme,grid=true;
    private String activeLabel="",homeQuery="",focusedItem;
    private boolean sortCreated,diagramOpen;
    private app.space.notes.domain.LibraryFilter libraryFilter=app.space.notes.domain.LibraryFilter.defaults();
    private DrawingPad drawingPad;private String drawingNote,drawingImage;private long stashedDrawing=-1;
    private String activeFolder;private long folderPageSequence;private Runnable afterFolderSelection;private Runnable voiceReturn=this::renderHome;
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
    private String auxiliaryPage;
    private PreparedImport prepared;
    private final Set<DocumentGrant> documentGrants=Collections.newSetFromMap(new IdentityHashMap<>());
    private final class DocumentGrant implements Runnable {private final Runnable release;private boolean released;DocumentGrant(Runnable release){this.release=release;}public void run(){if(released)return;released=true;documentGrants.remove(this);try{release.run();}catch(RuntimeException failure){log("document-grant","release requested; provider unavailable");}}}
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
        if(restored!=null){canonicalDiagram=restored.getString("canonicalDiagram");pendingContentAnchor=restored.getString("pendingContentAnchor");pendingContentNote=restored.getString("pendingContentNote");pendingContentOffset=restored.getInt("pendingContentOffset",-1);pendingContentDigest=restored.getString("pendingContentDigest");}
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
                historyStore=new PersistentHistoryStore(context);final Map<String,app.space.notes.domain.DocumentHistory> restoredHistories=historyStore.load(loaded);
                main.post(()->{ if(closed)return; savedHistories.putAll(restoredHistories);trimSavedHistories();notes.addAll(loaded); ready=true;if(historyStore.notice()!=null){historyNoticeShown=true;host.showMessage(historyStore.notice());}
                    if(restored!=null) {
                        documentsWorkspace=restored.getBoolean("documentsWorkspace",false);page=restored.getString("page","Notes");activeFolder=restored.getString("activeFolder");
                        String id=restored.getString("editing");
                        if(id!=null && find(id)!=null) {
                            Note n=find(id);
                            try {byte[] draft=restored.getByteArray("draftDocument");if(draft!=null){List<Note> decoded=DocumentCodec.decode(draft);if(decoded.size()==1 && decoded.get(0).id.equals(id))n=decoded.get(0);}}catch(Exception invalid){log("draft-restore","invalid snapshot ignored");}
                            replace(n);editing=id;if(id.equals(restored.getString("drawingNote"))){renderDrawing(id,restored.getString("drawingImage"));queueSave();deliverDeferred();return;}if(id.equals(restored.getString("imageEditNote"))){app.space.notes.domain.ImageEditPlan plan=null;try{if(n.updatedAt==restored.getLong("imageEditVersion",-1))plan=app.space.notes.domain.ImageEditPlan.restore(restored.getIntArray("imageEditPlan"));}catch(IllegalArgumentException invalid){log("image-edit.restore","invalid preview ignored");}renderImageEditor(id,restored.getString("imageEditItem"),plan);queueSave();deliverDeferred();return;}caretNote=id;caretAnchor=restored.getInt("bodyAnchor",0);caretFocus=restored.getInt("bodyFocus",caretAnchor);renderEditor();if(body!=null && restored.getBoolean("bodyFocused",false))body.requestFocus();queueSave(); deliverDeferred(); return;
                        }
                    }
                    if(restored!=null){String auxiliary=restored.getString("auxiliaryPage","");if(!auxiliary.isEmpty()){switch(auxiliary){case "Settings":renderSettings();break;case "Storage & recovery":renderStorage();break;case "Backups":renderBackups();break;case "About & offline models":renderModelInfo();break;case "Diagnostics":renderDiagnostics();break;case "Manage labels":manageLabels();break;case "Folders":renderFolders(false);break;default:renderHome();}deliverDeferred();return;}}
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
    private void clearPage(){if(canonicalEditor!=null){canonicalEditor.saveState();canonicalEditor=null;}textHistories.clear();redoTextControl=null;noteScroll=null;lastWritingFocus=null;lastEditorViewportHeight=0;auxiliaryPage=null;imageEditor=null;imageEditNote=null;imageEditItem=null;imageEditDigest=null;drawingPad=null;drawingNote=null;drawingImage=null;stashedDrawing=-1;disposePlayback();voiceInboxSequence++;folderPageSequence++;editorTopChrome=null;homeTopChrome=null;homeDockChrome=null;homeWorkspaceChrome=null;pageBack=null;undoControl=null;sheets.reset();formattingPanel=null;writingDock=null;pendingParagraphStyle=null;root.removeAllViews();}
    private void act(String label,Runnable action){if(busy){host.showMessage("Storage change in progress. Please wait.");return;}Diagnostics.record("ui.tap","control="+Diagnostics.controlName(label)+" screen="+(editing==null?page:"editor"));action.run();}
    private Button button(String label,Runnable action){return design.textAction(label,()->act(label,action));}
    private Button icon(String label,String symbol,Runnable action){return design.icon(label,symbol,()->act(label,action));}
    private LinearLayout menu(String name,Button launcher){LinearLayout panel=sheets.panel(name,launcher);launcher.setOnClickListener(v->act(name,()->{sheets.show(panel,name);android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(root.getWindowToken(),0);}));return panel;}
    private String actionSymbol(String name){if(name.equals("Clear canvas"))return "eraser";if(name.startsWith("Delete") || name.startsWith("Remove"))return "trash";if(name.startsWith("Copy"))return "copy";switch(name){case "Share":return "share";case "Export PDF":return "export";case "Move to folder":case "Folders":return "folder";case "Background":return "palette";case "Edit shape":return "edit";case "Connect":return "diagram";case "Drawing help":return "info";case "Group shapes":return "group";case "Edit arrows":return "diagram";case "Add item":return "add";case "Move item up":return "up";case "Move item down":return "down";case "Indent item":return "indent";case "Outdent item":return "outdent";case "Convert to checklist":case "Hide checked":case "Show checked":return "checklist";case "Convert to text":return "text";case "Archive":case "Unarchive":return "archive";case "Restore":return "undo";case "Merge":return "merge";default:return null;}}
    private void menuRows(LinearLayout panel,String[] names,Runnable[] actions){for(int i=0;i<names.length;i++){Runnable action=actions[i];Button row=button(names[i],()->{sheets.close();action.run();});String symbol=actionSymbol(names[i]);if(symbol!=null){NotesIcon glyph=new NotesIcon(symbol,design.accent);glyph.setBounds(0,0,dp(24),dp(24));row.setCompoundDrawablesRelative(glyph,null,null,null);row.setCompoundDrawablePadding(dp(14));}panel.addView(row,new LinearLayout.LayoutParams(-1,-2));}}

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
        LinearLayout entry=new LinearLayout(context);entry.setGravity(android.view.Gravity.CENTER_VERTICAL);entry.setPadding(dp(16),dp(8),dp(12),dp(8));entry.setBackground(design.ripple(surface,18));entry.setElevation(dp(1));
        String glyph=name.contains("folder") || name.contains("Storage") || name.contains("library")?"folder":name.contains("backup") || name.contains("Backup")?"archive":name.contains("Appearance")?"palette":name.contains("Delete")?"trash":name.contains("Export")?"share":name.contains("record")?"mic":name.contains("widget")?"widget":name.contains("Diagnostics")?"diagnostics":name.contains("About")?"info":"settings";
        TextView symbol=text("",1);NotesIcon picture=new NotesIcon(glyph,design.accent);picture.setBounds(0,0,dp(24),dp(24));symbol.setCompoundDrawables(null,picture,null,null);entry.addView(symbol,new LinearLayout.LayoutParams(dp(36),dp(36)));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);Button title=button(name,action);title.setPadding(dp(8),dp(4),dp(4),dp(4));title.setMinHeight(dp(44));copy.addView(title,new LinearLayout.LayoutParams(-1,-2));if(!description.isEmpty()){TextView hint=text(description,13);hint.setTextColor(muted);hint.setPadding(dp(8),0,dp(8),dp(8));hint.setOnClickListener(v->act(name,action));copy.addView(hint);}entry.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        TextView next=text("",1);NotesIcon arrow=new NotesIcon("next",muted);arrow.setBounds(0,0,dp(18),dp(18));next.setCompoundDrawables(arrow,null,null,null);next.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);entry.addView(next);entry.setOnClickListener(v->act(name,action));LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(-1,-2);spacing.setMargins(0,dp(5),0,dp(5));parent.addView(entry,spacing);
    }
    private void chooseAppearance(){if(host.managesAppearance()){host.showMessage("Appearance follows the embedding host's design and settings.");return;}String[] ids={"system","light","dark"};choiceSheet("Appearance",new String[]{"System","Light","Dark"},Arrays.asList(ids).indexOf(NotesAppearance.choice(context)),index->{persistDraft();NotesAppearance.choose(context,ids[index]);host.appearanceChanged();});}
    private void choiceSheet(String title,String[] names,int selected,java.util.function.IntConsumer choose){
        LinearLayout panel=sheets.panel(title,null);for(int i=0;i<names.length;i++){final int index=i;Button option=button(names[i],()->{sheets.close();choose.accept(index);});option.setSelected(i==selected);option.setBackground(design.choice(i==selected));NotesIcon mark=new NotesIcon(i==selected?"check":"circle",i==selected?design.accent:muted);mark.setBounds(0,0,dp(24),dp(24));option.setCompoundDrawablesRelative(mark,null,null,null);option.setCompoundDrawablePadding(dp(14));panel.addView(option,new LinearLayout.LayoutParams(-1,-2));}panel.addView(button("Cancel",()->sheets.close()));sheets.show(panel,title);
    }

    private void confirmSheet(String title,String message,String positive,Runnable confirmed){LinearLayout panel=sheets.panel(title,null);TextView explanation=text(message,15);explanation.setPadding(dp(16),dp(8),dp(16),dp(16));panel.addView(explanation);Button action=button(positive,()->{sheets.close();confirmed.run();});design.primary(action);NotesIcon glyph=new NotesIcon(positive.equals("Clear canvas")?"eraser":"trash",design.primaryInk);glyph.setBounds(0,0,dp(24),dp(24));action.setCompoundDrawablesRelative(glyph,null,null,null);action.setCompoundDrawablePadding(dp(12));panel.addView(action);panel.addView(button("Cancel",()->sheets.close()));sheets.show(panel,title);}
    private void drawingHelp(){LinearLayout panel=sheets.panel("Drawing help",null);panel.addView(text("The workspace opens at full paper width. Pinch to zoom and use two fingers to pan through a tall page. Fit shows the whole paper and may add margins. Back keeps a draft; Save attaches or updates this drawing in your note. Grid lines are a guide only and are not copied or saved into the image.",15));panel.addView(button("Done",()->sheets.close()));sheets.show(panel,"Drawing help");}
    private void renderSettings(){
        persistDraft();editing=null;title=null;body=null;search=null;querySequence++;main.removeCallbacks(filter);storagePage=true;voicePage=false;clearPage();header("Settings");
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(text("Make it yours",13));
        settingRow(content,"Appearance",host.managesAppearance()?"Follows the embedding host":"Choose System, Light or Dark",this::chooseAppearance);
        settingRow(content,"Home widget","Pinned and recent notes on your home screen",host::requestHomeWidget);
        content.addView(text("Your notes & files",13));
        settingRow(content,"Storage & recovery","Choose where files live and manage backups",()->{renderStorage();pageBack=this::renderSettings;});
        content.addView(text("Help & privacy",13));
        settingRow(content,"Diagnostics","Collect and share local issue logs only when you choose",this::renderDiagnostics);
        settingRow(content,"About & offline models","App version, offline features and licences",this::renderModelInfo);
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
    private LinearLayout rowIn(LinearLayout parent,String[] labels,Runnable[] actions){
        LinearLayout actionsRow=new LinearLayout(context);actionsRow.setBaselineAligned(false);actionsRow.setGravity(android.view.Gravity.CENTER_VERTICAL);int count=0;for(String label:labels)if(!label.equals("Back"))count++;boolean stacked=count>2 || (context.getResources().getConfiguration().fontScale>1.3f && context.getResources().getConfiguration().screenWidthDp<600);actionsRow.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        for(int i=0;i<labels.length;i++){if(labels[i].equals("Back"))continue;Button action=button(labels[i],actions[i]);if(i==0 && (labels[i].startsWith("Save") || labels[i].startsWith("Append") || labels[i].equals("Confirm merge"))){design.primary(action);NotesIcon done=new NotesIcon("check",design.primaryInk);done.setBounds(0,0,dp(24),dp(24));action.setCompoundDrawablesRelative(done,null,null,null);action.setCompoundDrawablePadding(dp(8));}LinearLayout.LayoutParams lp=stacked?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1);actionsRow.addView(action,lp);}if(actionsRow.getChildCount()>0)parent.addView(actionsRow);return actionsRow;
    }
    private void header(String name,Runnable back){header(name);pageBack=back;}
    private void header(String name) {headerIn(root,name);}
    private void headerIn(LinearLayout parent,String name){
        if(parent==root && Arrays.asList("Settings","Storage & recovery","Backups","About & offline models","Diagnostics","Manage labels","Folders").contains(name))auxiliaryPage=name;
        LinearLayout bar=tools(parent);bar.addView(icon("Back","back",this::back),new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView heading=design.heading(name);bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        status=text("",12);status.setTextColor(muted);status.setVisibility(View.GONE);parent.addView(status);
    }
    private void decorateLibrary(LinearLayout panel){String[] icons={"note","note","archive","trash","folder","mic","settings","note"};for(int i=0;i<panel.getChildCount() && i<icons.length;i++)if(panel.getChildAt(i) instanceof Button){Button button=(Button)panel.getChildAt(i);NotesIcon glyph=new NotesIcon(icons[i],ink);glyph.setBounds(0,0,dp(24),dp(24));button.setCompoundDrawablesRelative(glyph,null,null,null);button.setCompoundDrawablePadding(dp(16));}}
    private void captureGroup(LinearLayout panel,String name){TextView heading=text(name,12);heading.setTextColor(muted);heading.setPadding(dp(16),dp(6),dp(16),dp(2));if(android.os.Build.VERSION.SDK_INT>=28)heading.setAccessibilityHeading(true);panel.addView(heading);}
    private void captureAction(LinearLayout panel,String name,String symbol,Runnable action){Button option=button(name,()->{sheets.close();action.run();});NotesIcon glyph=new NotesIcon(symbol,design.accent);glyph.setBounds(0,0,dp(24),dp(24));option.setCompoundDrawablesRelative(glyph,null,null,null);option.setCompoundDrawablePadding(dp(16));panel.addView(option,new LinearLayout.LayoutParams(-1,-2));}

    private void captureGrid(LinearLayout panel,String[] names,String[] symbols,Runnable[] actions){Button measuring=button("",()->{});float widest=dp(64);for(String name:names)for(String word:name.split(" "))widest=Math.max(widest,measuring.getPaint().measureText(word));int available=dp(Math.min(560,context.getResources().getConfiguration().screenWidthDp)-32),columns=Math.max(1,Math.min(names.length,(int)(available/(widest+dp(24)))));for(int start=0;start<names.length;start+=columns){LinearLayout row=new LinearLayout(context);row.setBaselineAligned(false);for(int column=0;column<columns;column++){int index=start+column;LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);cell.setMargins(dp(2),dp(2),dp(2),dp(2));if(index>=names.length){row.addView(new View(context),cell);continue;}Runnable action=actions[index];Button option=button(names[index],()->{sheets.close();action.run();});option.setGravity(android.view.Gravity.CENTER);option.setPadding(dp(6),dp(6),dp(6),dp(6));option.setMinHeight(dp(64));option.setBackground(design.ripple(surface,14));NotesIcon glyph=new NotesIcon(symbols[index],design.accent);glyph.setBounds(0,0,dp(24),dp(24));option.setCompoundDrawables(null,glyph,null,null);option.setCompoundDrawablePadding(dp(4));row.addView(option,cell);}panel.addView(row,new LinearLayout.LayoutParams(-1,-2));}}

    private void renderHome(){
        afterFolderSelection=null;        voicePage=false;ocrPanel=false;storagePage=false;prepared=null;editing=null;title=null;body=null;visible.clear();diagramOpen=false;clearPage();root.setPadding(dp(16),dp(4),dp(16),dp(8));
        LinearLayout top=tools(root);homeTopChrome=top;Button library=icon("Library","menu",()->{});library.setTag("library-launcher");
        TextView brand=design.heading(activeFolder!=null?(activeFolder.isEmpty()?"Main folder":activeFolder):(activeLabel.isEmpty()?(page.equals("Notes") && documentsWorkspace?"Doc":page):activeLabel));brand.setSingleLine(true);brand.setEllipsize(android.text.TextUtils.TruncateAt.END);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));if(!undoBatch.isEmpty() || undo!=null)top.addView(icon("Undo","undo",this::undo),new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout navigation=menu("Library",library);menuRows(navigation,new String[]{"Notes","Doc","Archive","Trash","Folders","Voice inbox","Settings","Open note file"},new Runnable[]{()->openWorkspace(false),()->openWorkspace(true),()->navigate("Archive"),()->navigate("Trash"),()->renderFolders(false),this::renderVoiceInbox,this::renderSettings,host::chooseNoteDocument});decorateLibrary(navigation);
        LinearLayout workspaceBar=tools(root);homeWorkspaceChrome=workspaceBar;workspaceBar.setContentDescription("Notes and Doc workspaces");
        for(boolean docs:new boolean[]{false,true}){Button tab=button(docs?"Doc":"Notes",()->openWorkspace(docs));tab.setSelected(documentsWorkspace==docs && page.equals("Notes"));tab.setBackground(design.choice(tab.isSelected()));workspaceBar.addView(tab,new LinearLayout.LayoutParams(0,-2,1));}
        if(!page.equals("Notes"))workspaceBar.setVisibility(View.GONE);
        if(page.equals("Trash"))top.addView(icon("Empty Trash","trash",()->confirmPermanentDelete(true)),new LinearLayout.LayoutParams(dp(48),dp(48)));
        status=text("",12);status.setVisibility(View.GONE);root.addView(status);
        LinearLayout searchBar=new LinearLayout(context);searchBar.setGravity(android.view.Gravity.CENTER_VERTICAL);searchBar.setBackground(design.shape(surface,24));root.addView(searchBar);
        search=new EditText(context);search.setSingleLine(true);design.entry(search,16);search.setPadding(dp(18),dp(12),dp(4),dp(12));search.setOnFocusChangeListener((v,f)->root.post(this::adaptEditorViewport));search.setHint(documentsWorkspace && page.equals("Notes")?"Search Docs and labels":"Search notes and labels");search.setText(homeQuery);searchBar.addView(search,new LinearLayout.LayoutParams(0,-2,1));
        Button filterControl=icon("Filters","filter",()->{});searchBar.addView(filterControl,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout filters=menu("Filter & view",filterControl);
        TreeSet<String> labels=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(Note note:notes)if(note.deletedAt==0)labels.addAll(note.labels);filters.addView(new LibraryFilterView(design,libraryFilter,labels,value->{libraryFilter=value;grid=value.grid;sortCreated=value.sort.equals("created");activeLabel="";selectedIds.clear();saveViewPreferences();renderHome();},this::manageLabels,sheets::refresh));
        search.addTextChangedListener(watcher(()->{homeQuery=search.getText().toString();main.removeCallbacks(filter);main.postDelayed(filter,150);querySequence++;}));
        selectionBar=new LinearLayout(context);selectionBar.setOrientation(LinearLayout.VERTICAL);LinearLayout selectionTools=tools(selectionBar);tool(selectionTools,"Done","close",()->{selectedIds.clear();updateSelection();});TextView count=text("",16);count.setTag("selection-count");selectionTools.addView(count,new LinearLayout.LayoutParams(0,-2,1));if(!page.equals("Trash")){tool(selectionTools,"Pin","pin",()->bulkState("pin"));tool(selectionTools,"Archive","archive",()->bulkState("archive"));}tool(selectionTools,page.equals("Trash")?"Restore":"Trash",page.equals("Trash")?"undo":"trash",()->bulkState(page.equals("Trash")?"restore":"trash"));Button selectionMore=icon("Selection actions","more",()->{});selectionTools.addView(selectionMore,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout selectedMenu=menu("Selected notes",selectionMore);if(page.equals("Trash"))selectedMenu.addView(button("Delete permanently",()->confirmPermanentDelete(false)));else selectedMenu.addView(button("Move to folder",()->renderFolders(true)));if(!page.equals("Trash"))menuRows(selectedMenu,new String[]{"Labels","Colour","Share","Copy","Merge"},new Runnable[]{this::renderLabels,this::renderColours,this::shareSelected,this::duplicateSelected,this::mergeSelected});root.addView(selectionBar);
        board=new NotesBoard(context,design,new NotesBoard.Listener(){public void open(Note n){if(!selectedIds.isEmpty()){toggleSelection(n);return;}editing=n.id;renderEditor();}public void select(Note n){toggleSelection(n);}},grid);
        emptyHint=text(page.equals("Trash")?"Trash is empty.\nRestore deleted notes here, or delete them permanently.":!libraryFilter.allNotes && libraryFilter.labels.isEmpty() && !libraryFilter.includeUnlabelled?"No labels selected.\nChoose labels or All notes in Filters.":homeQuery.isEmpty()?"A little space for your ideas.\nCreate a note below. It stays on your device.":"No matching notes. Try another search.",17);emptyHint.setTextColor(muted);emptyHint.setGravity(android.view.Gravity.CENTER);int hintPad=context.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?8:28;emptyHint.setPadding(dp(20),dp(hintPad),dp(20),dp(hintPad));emptyHint.setVisibility(View.GONE);FrameLayout libraryArea=new FrameLayout(context);root.addView(libraryArea,new LinearLayout.LayoutParams(-1,0,1));libraryArea.addView(board,new FrameLayout.LayoutParams(-1,-1));libraryArea.addView(emptyHint,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout dock=tools(root);homeDockChrome=dock;dock.addView(library,new LinearLayout.LayoutParams(dp(48),dp(48)));if(!page.equals("Trash")){if(!documentsWorkspace)tool(dock,"+ Checklist","checklist",this::newChecklist);tool(dock,"Record voice","mic",host::requestVoiceCapture);Button searchButton=icon("Search","search",()->{});dock.addView(searchButton,new LinearLayout.LayoutParams(dp(48),dp(48)));SearchGestures.install(searchButton,()->focusHomeSearch(false),()->focusHomeSearch(true),filterControl::performClick);View gap=new View(context);dock.addView(gap,new LinearLayout.LayoutParams(0,1,1));Button create=button(documentsWorkspace?"New Doc":context.getResources().getConfiguration().fontScale>1.3f?"Note":"New note",documentsWorkspace?this::newDocument:this::newNote);create.setContentDescription(documentsWorkspace?"New document":"New text note");create.setTooltipText(documentsWorkspace?"New document":"New text note");design.primary(create);create.setMinHeight(dp(54));create.setPadding(dp(18),dp(8),dp(18),dp(8));dock.addView(create);}
        homeChrome.clear();chromeHidden=false;for(View chrome:new View[]{top,workspaceBar,searchBar,dock})homeChrome.put(chrome,chrome.getVisibility());updateSelection();filterNow();root.post(this::adaptEditorViewport);
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
    private void openWorkspace(boolean documents){persistDraft();documentsWorkspace=documents;homeQuery="";navigate("Notes");}
    private void navigate(String name) { activeFolder=null;page=name;selectedIds.clear();activeLabel="";libraryFilter=new app.space.notes.domain.LibraryFilter(true,true,Collections.emptyList(),libraryFilter.sort,grid);renderHome(); }
    private void newNote() {
        page="Notes";documentsWorkspace=false;long now=System.currentTimeMillis();Note n=Note.create(now).folder(activeFolder==null?"":activeFolder,now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);notes.add(n);editing=n.id;selectedIds.clear();
        queueSave(); renderEditor(); if(body!=null){EditText target=body;target.requestFocus();target.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){public void onLayoutChange(View view,int l,int t,int r,int b,int ol,int ot,int or,int ob){if(r<=l || b<=t)return;target.removeOnLayoutChangeListener(this);target.post(()->{if(closed || body!=target || !target.isAttachedToWindow())return;target.requestFocus();((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(target,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});}});}
    }
    private void newDocument(){
        page="Notes";documentsWorkspace=true;long now=System.currentTimeMillis();Note n=app.space.notes.domain.Workspace.create(now,true).folder(activeFolder==null?"":activeFolder,now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);
        canonicalState.clear();canonicalState.putString("note",n.id);canonicalState.putString("field","text:"+n.content.blocks.get(0).id);canonicalState.putInt("start",0);canonicalState.putInt("end",0);canonicalState.putBoolean("showKeyboard",true);notes.add(n);editing=n.id;selectedIds.clear();queueSave();renderEditor();
    }
    private void focusHomeSearch(boolean keyboard){if(search==null || !search.isAttachedToWindow())return;search.setShowSoftInputOnFocus(keyboard);search.requestFocus();search.setSelection(search.length());if(keyboard)search.post(()->{if(search!=null && search.hasFocus())((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(search,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});else search.setShowSoftInputOnFocus(true);}
    private void newChecklist(){page="Notes";documentsWorkspace=false;long now=System.currentTimeMillis();Note n=Note.create(now).folder(activeFolder==null?"":activeFolder,now).convert(now).properties(0,activeLabel.isEmpty()?Collections.emptyList():Arrays.asList(activeLabel),now);notes.add(n);editing=n.id;selectedIds.clear();queueSave();renderEditor();}
    private void filterNow() {
        if(closed || editing!=null || search==null)return;
        final String query=search.getText().toString(), currentPage=page,label=activeLabel;
        final app.space.notes.domain.LibraryFilter filterState=libraryFilter;final String folder=activeFolder;final boolean documents=documentsWorkspace;
        final long ticket=++querySequence;
        final List<Note> snapshot=new ArrayList<>(notes);
        searchWorker.execute(()->{
            List<Note> result=new ArrayList<>();
            for(Note n:snapshot) {
                boolean included=currentPage.equals("Trash")?n.deletedAt>0:
                    n.deletedAt==0 && (currentPage.equals("Archive")==n.archived);
                if(included && (!currentPage.equals("Notes") || app.space.notes.domain.Workspace.includes(n,documents)) && (folder==null || n.folder.equalsIgnoreCase(folder)) && n.matches(query) && filterState.matches(n))result.add(n);
            }
            result.sort(filterState.comparator());
            main.post(()->{
                if(closed || editing!=null || ticket!=querySequence)return;
                visible.clear();visible.addAll(result);board.show(result,selectedIds,!currentPage.equals("Trash"));
                if(emptyHint!=null)emptyHint.setVisibility(result.isEmpty()?View.VISIBLE:View.GONE);
            });
        });
    }
    private void upgradeDocumentEditor(){
        persistDraft();Note source=find(editing);if(source==null || source.deletedAt>0 || busy)return;
        if(source.content!=null){renderEditor();return;}
        confirmSheet("Copy this note to Doc?","The original note stays in its current location, unchanged. A separate editable Doc copy keeps its text, formatting, checklist states and visuals. A verified recovery snapshot is retained in Backups.","Create Doc copy",()->{
            persistDraft();final Note before=find(editing);if(before==null || busy)return;final List<Note> base=new ArrayList<>(notes),candidate=new ArrayList<>(notes);
            final Note upgraded=app.space.notes.domain.Workspace.copyToDocument(before,System.currentTimeMillis());candidate.add(upgraded);busy=true;clearPage();title=null;body=null;root.addView(text("Verifying document and keeping the original recovery snapshot…",17));
            disk.execute(()->{try{boolean complete=repository.upgradeDocument(base,candidate);main.post(()->{busy=false;notes.clear();notes.addAll(candidate);if(closed)return;editing=upgraded.id;documentsWorkspace=true;canonicalState.clear();renderEditor();host.showMessage(complete?"Doc copy created. Your original is unchanged.":"Doc retained privately; folder update is pending. Use Retry save.");});}catch(Exception failure){Diagnostics.failure("document-upgrade.failure",failure);main.post(()->{busy=false;if(!closed){renderEditor();host.showMessage("Doc copy was not created; original retained: "+failure.getMessage());}});}});
        });
    }
    private void renderCanonicalEditor(Note note){
        clearPage();title=null;body=null;search=null;querySequence++;root.setPadding(dp(4),dp(4),dp(4),dp(4));
        canonicalHistoryNote=note.id;canonicalHistory=historyFor(note.id);
        if(restored!=null && !canonicalStateRestored){Bundle saved=restored.getBundle("canonicalEditor");if(saved!=null)canonicalState.putAll(saved);canonicalStateRestored=true;}
        final String id=note.id;
        canonicalEditor=new DocumentEditorView(context,design,new DocumentEditorView.Host(){
            public Note current(){Note value=find(id);return value==null?note:value;}
            public void change(Note next,String group){canonicalHistory=historyFor(id);Note before=current();canonicalHistory.record(before,next,group,android.os.SystemClock.uptimeMillis());replace(next);scheduleDraft();}
            public void cursor(String field,int anchor,int focus){canonicalHistory=historyFor(id);canonicalHistory.rememberCursor(current(),new app.space.notes.domain.DocumentHistory.Cursor(field,anchor,focus));}
            public boolean canUndo(boolean redo){canonicalHistory=historyFor(id);return redo?canonicalHistory.canRedo():canonicalHistory.canUndo();}
            public void undo(boolean redo){canonicalHistory=historyFor(id);Note before=current(),next=redo?canonicalHistory.redo(before):canonicalHistory.undo(before);if(next!=before){if(canonicalEditor!=null)canonicalEditor.saveState();canonicalEditor=null;app.space.notes.domain.DocumentHistory.Cursor cursor=canonicalHistory.cursor(next);if(cursor!=null){canonicalState.putString("note",next.id);canonicalState.putString("field",cursor.field);canonicalState.putInt("start",cursor.anchor);canonicalState.putInt("end",cursor.focus);}Note restoredNote=next.state(next.pinned,next.archived,next.deletedAt,System.currentTimeMillis());if(cursor!=null)canonicalHistory.rememberCursor(restoredNote,cursor);historyStepping=true;try{replace(restoredNote);}finally{historyStepping=false;}queueSave();renderEditor();}}
            public void back(){persistDraft();renderHome();}
            public void visual(app.space.notes.domain.DocumentContent.Block block){persistDraft();if(block.kind==app.space.notes.domain.DocumentContent.Kind.DRAWING)renderDrawing(id,block.image.id);else renderImageEditor(id,block.image.id,null);}
            public void diagram(app.space.notes.domain.DocumentContent.Block block){persistDraft();canonicalDiagram=block.id;diagramHistoryNote=null;renderDiagram();}
            public void addVisual(boolean drawing){persistDraft();pendingContentAnchor=canonicalEditor==null?null:canonicalEditor.insertionAnchor();pendingContentNote=id;pendingContentOffset=canonicalEditor==null?-1:canonicalEditor.insertionOffset();pendingContentDigest=pendingContentOffset>=0?contentDigest(current().content.block(pendingContentAnchor).text):null;if(drawing)renderDrawing(id);else host.chooseNoteImage(id);}
            public void action(String action){persistDraft();switch(action){case "Find in document":NoteFindDialog.show(design,canonicalEditor,searchWorker);break;case "Rename reminder":renameNoteContent("reminder","Reminder");break;case "Share":shareSelected();break;case "Export PDF":host.exportPdf();break;case "File location":showDocumentLocation();break;case "Move to folder":renderFolders(true);break;case "Background":renderBackgrounds();break;case "Labels":renderLabels();break;case "Colour":renderColours();break;case "Reminder":chooseReminder();break;case "Pin":case "Unpin":changeState("pin");break;case "Archive":case "Unarchive":changeState("archive");break;case "Restore":changeState("restore");break;case "lines":Note before=current();change(before.withContent(before.content.withWritingLines(!before.content.writingLines),System.currentTimeMillis()),null);renderEditor();break;default:throw new IllegalArgumentException("Unknown document action");}}
        },canonicalState);status=canonicalEditor.saveStatus();root.addView(canonicalEditor,new LinearLayout.LayoutParams(-1,0,1));
    }
    private static String contentDigest(app.space.notes.domain.RichText text){try{return GenerationProtocol.digest(text.fingerprint().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception unavailable){throw new IllegalStateException("Cannot verify the insertion point safely",unavailable);}}
    private void recordCanonicalChange(Note before,Note after){if(before.content!=null)historyFor(before.id).record(before,after,null,android.os.SystemClock.uptimeMillis());}
    private Diagram currentDiagram(Note note){if(note.content!=null && canonicalDiagram!=null){try{return note.content.block(canonicalDiagram).diagram;}catch(IllegalArgumentException missing){canonicalDiagram=null;}}return note.diagram;}
    private Note replaceDiagram(Note note,Diagram diagram){if(note.content!=null && canonicalDiagram!=null){app.space.notes.domain.DocumentContent.Block block=note.content.block(canonicalDiagram);return note.withContent(note.content.replace(block.id,app.space.notes.domain.DocumentContent.Block.diagram(block.id,block.name,diagram)),System.currentTimeMillis());}return note.diagram(diagram,System.currentTimeMillis());}
    private void renderEditor(){
        Note canonical=find(editing);if(canonical!=null)documentsWorkspace=canonical.content!=null;if(canonical!=null && canonical.content!=null){voicePage=false;ocrPanel=false;diagramOpen=false;renderCanonicalEditor(canonical);return;}
        voicePage=false;ocrPanel=false;diagramOpen=false;Note n=find(editing);if(n==null){renderHome();return;}main.removeCallbacks(filter);querySequence++;search=null;board=null;selectionBar=null;clearPage();root.setPadding(dp(20),dp(4),dp(20),dp(8));
        LinearLayout top=tools(root);editorTopChrome=top;tool(top,"Back","back",()->{persistDraft();renderHome();});View spacer=new View(context);top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));boolean trashed=n.deletedAt>0;if(!trashed){tool(top,n.pinned?"Unpin":"Pin",n.pinned?"pinned":"pin",()->changeState("pin"));tool(top,n.archived?"Unarchive":"Archive","archive",()->changeState("archive"));}undoControl=icon("Undo","undo",this::undo);top.addView(undoControl,new LinearLayout.LayoutParams(dp(48),dp(48)));redoTextControl=icon("Redo text","redo",()->stepTextHistory(true));top.addView(redoTextControl,new LinearLayout.LayoutParams(dp(48),dp(48)));syncUndoControl();
        ScrollView document=new ScrollView(context);noteScroll=document;document.setBackground(new NoteBackground(n.background,n.color==0?surface:NotesBoard.colour(n.color,darkTheme),darkTheme,context.getResources().getDisplayMetrics().density,n.background==3));document.setFillViewport(true);document.setClipToPadding(false);document.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)document.postOnAnimation(()->revealWritingCaret(document));});LinearLayout editor=new LinearLayout(context);editor.setOrientation(LinearLayout.VERTICAL);FrameLayout.LayoutParams column=new FrameLayout.LayoutParams(Math.min(dp(720),dp(context.getResources().getConfiguration().screenWidthDp-40)),-2,android.view.Gravity.CENTER_HORIZONTAL);document.addView(editor,column);root.addView(document,new LinearLayout.LayoutParams(-1,0,1));
        title=new WritingField(context);design.entry(title,26);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setOnFocusChangeListener((v,f)->{if(f){lastWritingFocus=(EditText)v;syncUndoControl();}root.post(this::adaptEditorViewport);});title.setHint("Title");title.setText(n.title);title.setEnabled(!trashed);editor.addView(title,new LinearLayout.LayoutParams(-1,-2));
        if(n.reminderAt>0){LinearLayout reminderRow=new LinearLayout(context);reminderRow.setGravity(android.view.Gravity.CENTER_VERTICAL);reminderRow.setBackground(design.shape(surface,16));
            String schedule=java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(n.reminderAt))+"\n"+android.text.format.DateFormat.getTimeFormat(context).format(new Date(n.reminderAt));Button reminder=button(n.contentName("reminder","Reminder")+"\n"+schedule,this::chooseReminder);reminder.setTag("find-caption");reminder.setContentDescription("Edit reminder");reminder.setTooltipText("Edit reminder");androidx.core.view.ViewCompat.setStateDescription(reminder,reminder.getText());reminder.setTextColor(design.accent);NotesIcon bell=new NotesIcon("bell",design.accent);bell.setBounds(0,0,dp(22),dp(22));reminder.setCompoundDrawables(bell,null,null,null);reminder.setCompoundDrawablePadding(dp(8));reminderRow.addView(reminder,new LinearLayout.LayoutParams(0,-2,1));if(!trashed)reminderRow.addView(icon("Rename reminder","edit",()->renameNoteContent("reminder","Reminder")),new LinearLayout.LayoutParams(dp(48),dp(48)));reminderRow.addView(icon("Clear reminder","close",()->setReminder(n.id,0)),new LinearLayout.LayoutParams(dp(48),dp(48)));editor.addView(reminderRow,new LinearLayout.LayoutParams(-1,-2));}
        if(!n.labels.isEmpty()){TextView chips=text(String.join("  ·  ",n.labels),13);chips.setTextColor(design.accent);editor.addView(chips);}

        body=null;
        if(n.checklist){LinearLayout rows=new LinearLayout(context);rows.setOrientation(LinearLayout.VERTICAL);editor.addView(rows);
            int checked=0;TextView footer=text("",12);
            for(Note.Item item:n.items){
                if(item.checked)checked++;
                LinearLayout line=new LinearLayout(context);line.setGravity(android.view.Gravity.CENTER_VERTICAL);line.setPadding(dp(item.depth*16),0,0,0);
                if(!trashed)line.addView(checklistHandle(n.id,item.id,n.items.indexOf(item)+1),new LinearLayout.LayoutParams(dp(48),dp(48)));wireChecklistDrop(line,n.id,item.id);
                CheckBox tick=new CheckBox(context);tick.setContentDescription("Complete list item");tick.setChecked(item.checked);tick.setMinWidth(dp(48));tick.setMinHeight(dp(48));tick.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{design.accent,muted}));tick.setEnabled(!trashed);line.addView(tick);
                EditText input=new WritingField(context);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setHint("List item");input.setTag(item.id);design.entry(input,18);input.setText(item.text);input.setEnabled(!trashed);
                input.setPaintFlags(item.checked?input.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG:input.getPaintFlags()&~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                line.addView(input,new LinearLayout.LayoutParams(0,-2,1));if(!trashed)line.addView(icon("Actions for list item "+(n.items.indexOf(item)+1),"more",()->listItemOptions(item.id)),new LinearLayout.LayoutParams(dp(48),dp(48)));rows.addView(line);
                if(n.hideChecked && item.checked)line.setVisibility(View.GONE);
                input.setOnFocusChangeListener((v,focused)->{if(focused){focusedItem=item.id;lastWritingFocus=(EditText)v;syncUndoControl();}root.post(this::adaptEditorViewport);});
                if(item.id.equals(focusedItem) && !trashed)input.requestFocus();
                input.addTextChangedListener(watcher(()->{
                    if(binding)return;invalidateFullUndo();Note current=find(editing);if(current==null)return;
                    List<Note.Item> changed=new ArrayList<>(current.items);
                    for(int i=0;i<changed.size();i++)if(changed.get(i).id.equals(item.id))changed.set(i,changed.get(i).change(input.getText().toString(),changed.get(i).checked,changed.get(i).depth));
                    replace(current.list(title.getText().toString(),changed,current.hideChecked,System.currentTimeMillis()));scheduleDraft();
                }));
                if(!trashed)installTextHistory(input);
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
            document.setOnDragListener((view,event)->checklistScrollDrag(event,n.id));
            footer.setText(checked+" checked");footer.setVisibility(checked>0?View.VISIBLE:View.GONE);rows.addView(footer);if(!trashed){Button addItem=button("+ List item",()->listAction("add"));addItem.setContentDescription("Add checklist item");rows.addView(addItem);}
        }else{
            body=new WritingField(context);((WritingField)body).ruling(n.background==3,darkTheme?0x55ffffff:0x44000000);body.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);design.entry(body,18);body.setOnFocusChangeListener((v,f)->{if(f){lastWritingFocus=(EditText)v;syncUndoControl();}root.post(this::adaptEditorViewport);});body.setHint("Write your note…");body.setGravity(android.view.Gravity.TOP);body.setMinLines(1);body.setText(n.body);TextStyles.write(body.getText(),n.marks,body.getResources().getDisplayMetrics());if(n.id.equals(caretNote))body.setSelection(Math.max(0,Math.min(caretAnchor,body.length())),Math.max(0,Math.min(caretFocus,body.length())));body.setEnabled(!trashed);editor.addView(body,new LinearLayout.LayoutParams(-1,-2));body.addTextChangedListener(paragraphWatcher());((WritingField)body).selectionChanged=()->{updateFormatting();syncUndoControl();};editor.setOnClickListener(v->{if(body!=null && !trashed){body.requestFocus();body.setSelection(body.length());((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(body,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}});
        }
        for(app.space.notes.domain.NoteImage image:n.images){boolean drawing=image.name.equals("Drawing");String kind=drawing?"Drawing":"Image";LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(4),dp(8),dp(8));GradientDrawable outline=design.shape(surface,16);outline.setStroke(dp(1),design.outline);card.setBackground(outline);LinearLayout itemHeader=new LinearLayout(context);itemHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);TextView type=text(n.contentName("image:"+image.id,kind),14);type.setTag("find-caption");type.setMaxLines(2);type.setEllipsize(android.text.TextUtils.TruncateAt.END);NotesIcon badge=new NotesIcon(drawing?"edit":"image",design.accent);badge.setBounds(0,0,dp(20),dp(20));type.setCompoundDrawablesRelative(badge,null,null,null);type.setCompoundDrawablePadding(dp(8));itemHeader.addView(type,new LinearLayout.LayoutParams(0,-2,1));if(!trashed)itemHeader.addView(icon(drawing?"Edit drawing":"Edit image","edit",()->{if(drawing)renderDrawing(n.id,image.id);else renderImageEditor(n.id,image.id,null);}),new LinearLayout.LayoutParams(dp(48),dp(48)));if(!trashed){Button more=icon(kind+" actions","more",()->{});itemHeader.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout actions=menu(kind+" actions",more);menuRows(actions,new String[]{"Rename "+kind.toLowerCase(java.util.Locale.ROOT),"Remove "+kind.toLowerCase(java.util.Locale.ROOT)},new Runnable[]{()->renameNoteContent("image:"+image.id,kind),()->confirmSheet("Remove "+kind.toLowerCase(java.util.Locale.ROOT)+"?","Only this visual item is removed. The rest of the note is kept; Undo is available.","Remove "+kind.toLowerCase(java.util.Locale.ROOT),()->{persistDraft();Note current=find(n.id);if(current==null)return;captureUndo(Arrays.asList(current),true);List<app.space.notes.domain.NoteImage> keep=new ArrayList<>(current.images);keep.removeIf(value->value.id.equals(image.id));replace(current.images(keep,System.currentTimeMillis()));queueSave();renderEditor();})});}card.addView(itemHeader);card.addView(new NoteImageView(context,image),new LinearLayout.LayoutParams(-1,dp(220)));LinearLayout.LayoutParams cardLayout=new LinearLayout.LayoutParams(-1,-2);cardLayout.setMargins(0,dp(12),0,0);editor.addView(card,cardLayout);}

        if(!n.diagram.nodes.isEmpty()){Button diagram=button(n.contentName("diagram","Diagram")+" · "+n.diagram.nodes.size()+(n.diagram.nodes.size()==1?" shape":" shapes"),this::renderDiagram);diagram.setTag("find-caption");diagram.setMaxLines(2);diagram.setEllipsize(android.text.TextUtils.TruncateAt.END);diagram.setBackground(design.ripple(surface,16));NotesIcon glyph=new NotesIcon("diagram",ink);glyph.setBounds(0,0,dp(24),dp(24));diagram.setCompoundDrawablesRelative(glyph,null,null,null);diagram.setCompoundDrawablePadding(dp(10));LinearLayout.LayoutParams diagramLayout=new LinearLayout.LayoutParams(-1,-2);diagramLayout.setMargins(0,dp(12),0,0);editor.addView(diagram,diagramLayout);if(!trashed)editor.addView(button("Rename diagram",()->renameNoteContent("diagram","Diagram")));}

        status=text(dirty || persistedRevision<revision?"Saving locally…":"Saved · on this device",11);status.setTextColor(muted);status.setPadding(0,dp(4),0,dp(4));status.setVisibility(View.GONE);root.addView(status,1);
        LinearLayout dock=tools(root);writingDock=dock;Button add=icon("Add to note","add",()->{});dock.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout capture=menu("Add to note",add);captureGroup(capture,"Visual content");if(trashed)captureAction(capture,"View diagram","diagram",this::renderDiagram);else{captureGrid(capture,new String[]{"Diagram","Image","Drawing"},new String[]{"diagram","image","edit"},new Runnable[]{this::renderDiagram,()->{persistDraft();host.chooseNoteImage(n.id);},()->renderDrawing(n.id)});captureGroup(capture,"Capture text");captureGrid(capture,new String[]{"Read image","Record voice"},new String[]{"scan","mic"},new Runnable[]{this::chooseScan,host::requestVoiceCapture});captureGroup(capture,"Schedule this note");captureAction(capture,"Reminder","bell",this::chooseReminder);}


        Button formattingControl=null;if(!trashed && !n.checklist){formattingControl=icon("Text formatting","format",this::toggleFormatting);dock.addView(formattingControl,new LinearLayout.LayoutParams(dp(48),dp(48)));}
        if(!trashed){dock.addView(icon("Colour","palette",this::renderColours),new LinearLayout.LayoutParams(dp(48),dp(48)));dock.addView(icon("Labels","label",this::renderLabels),new LinearLayout.LayoutParams(dp(48),dp(48)));}
        View spring=new View(context);dock.addView(spring,new LinearLayout.LayoutParams(0,1,1));Button more=icon("More actions","more",()->{});dock.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout actions=menu("Note actions",more);
        menuRows(actions,new String[]{"Find in note"},new Runnable[]{()->NoteFindDialog.show(design,noteScroll,searchWorker)});
        if(trashed)menuRows(actions,new String[]{"Restore","Share"},new Runnable[]{()->changeState("restore"),this::shareSelected});else{menuRows(actions,new String[]{"Share","Export PDF","Move to folder","Background","File location"},new Runnable[]{this::shareSelected,host::exportPdf,()->renderFolders(true),this::renderBackgrounds,this::showDocumentLocation});if(!n.checklist)menuRows(actions,new String[]{"Convert to checklist"},new Runnable[]{this::convertNote});}
        if(!trashed)menuRows(actions,new String[]{"Copy to Doc"},new Runnable[]{this::upgradeDocumentEditor});
        TextView dates=text("Edited "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(n.updatedAt))+"\nCreated "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new Date(n.createdAt)),12);dates.setTextColor(muted);actions.addView(dates);
        if(!trashed && !n.checklist){writingDock=dock;buildFormatting(formattingControl);installTextHistory(body);}
        if(!trashed && n.checklist){Button lists=icon("Checklist options","checklist",()->{});dock.addView(lists,0,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout options=menu("Checklist options",lists);menuRows(options,new String[]{n.hideChecked?"Show checked":"Hide checked","Convert to text"},new Runnable[]{()->listAction("hide"),this::convertNote});}
        title.addTextChangedListener(watcher(()->{if(!binding){invalidateFullUndo();scheduleDraft();}}));if(!trashed)installTextHistory(title);root.post(this::adaptEditorViewport);
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
        if(editorTopChrome!=null){int height=shell.getHeight();boolean shrank=lastEditorViewportHeight>0 && height<lastEditorViewportHeight;lastEditorViewportHeight=height;EditText remembered=lastWritingFocus;
            // A zero-height intermediate ScrollView during IME resize can drop its focused child.
            // Recover focus once for this measured shrink, never force or rewrite a native selection.
            if(shrank && compact && remembered!=null && keyboardVisible())root.postOnAnimation(()->{if(!closed && lastWritingFocus==remembered && root.findFocus()==null && remembered.isAttachedToWindow() && remembered.isShown() && remembered.isEnabled() && shell.hasWindowFocus() && shell.getHeight()<dp(240) && keyboardVisible() && !sheets.open())remembered.requestFocus();});
            changed|=viewportVisibility(editorTopChrome,compact?View.GONE:View.VISIBLE);View focused=root.findFocus();if(focused==null && compact && keyboardVisible())focused=remembered;if(title!=null)changed|=viewportVisibility(title,compact && focused instanceof EditText && focused!=title?View.GONE:View.VISIBLE);if(writingDock!=null){int padding=compact?0:dp(6);if(writingDock.getPaddingTop()!=padding){writingDock.setPadding(0,padding,0,padding);changed=true;}}}
        if(homeTopChrome!=null && homeDockChrome!=null && selectedIds.isEmpty() && !chromeHidden){boolean searching=compact && keyboardVisible();changed|=viewportVisibility(homeTopChrome,searching?View.GONE:View.VISIBLE);changed|=viewportVisibility(homeDockChrome,searching?View.GONE:View.VISIBLE);if(homeWorkspaceChrome!=null)changed|=viewportVisibility(homeWorkspaceChrome,keyboardVisible() || !page.equals("Notes")?View.GONE:View.VISIBLE);}
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
            design.show(new android.app.AlertDialog.Builder(context).setTitle("Reminder notifications are off").setMessage("Enable MyNotes reminders in Android notification settings. A saved date alone cannot notify while notifications are blocked.").setPositiveButton("Notification settings",(dialog,which)->{android.content.Intent settings=new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.getPackageName());context.startActivity(settings);}).setNegativeButton("Cancel",null).create());return;
        }
        final String id=note.id;Calendar time=Calendar.getInstance();time.setTimeInMillis(note.reminderAt>System.currentTimeMillis()?note.reminderAt:System.currentTimeMillis()+3600000);
        android.app.DatePickerDialog date=new android.app.DatePickerDialog(context,(view,year,month,day)->{
            time.set(Calendar.YEAR,year);time.set(Calendar.MONTH,month);time.set(Calendar.DAY_OF_MONTH,day);
            design.show(new android.app.TimePickerDialog(context,(clock,hour,minute)->{time.set(Calendar.HOUR_OF_DAY,hour);time.set(Calendar.MINUTE,minute);time.set(Calendar.SECOND,0);time.set(Calendar.MILLISECOND,0);if(time.getTimeInMillis()<=System.currentTimeMillis()){host.showMessage("Choose a future date and time.");return;}setReminder(id,time.getTimeInMillis());},time.get(Calendar.HOUR_OF_DAY),time.get(Calendar.MINUTE),android.text.format.DateFormat.is24HourFormat(context)));
        },time.get(Calendar.YEAR),time.get(Calendar.MONTH),time.get(Calendar.DAY_OF_MONTH));date.getDatePicker().setMinDate(System.currentTimeMillis()-1000);design.show(date);
    });}
    private void setReminder(String id,long at){whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;captureUndo(Arrays.asList(note),true);replace(note.reminder(at,System.currentTimeMillis()));queueSave();if(id.equals(editing))renderEditor();host.showMessage(at==0?"Reminder cleared.":"Local reminder saved. Android may delay delivery during battery saving. Force-stop disables alarms until reopening; notification settings must stay enabled.");});}
    private void renderDiagram(){
        persistDraft();Note note=find(editing);if(note==null){renderHome();return;}diagramOpen=true;title=null;body=null;search=null;querySequence++;clearPage();root.setPadding(dp(16),dp(4),dp(16),dp(8));
        LinearLayout top=tools(root);tool(top,"Back to note","back",()->{diagramOpen=false;renderEditor();});TextView name=design.heading("Diagram");top.addView(name,new LinearLayout.LayoutParams(0,-2,1));tool(top,"Export PDF","export",host::exportPdf);
        status=text("",12);status.setVisibility(View.GONE);root.addView(status);boolean readOnly=note.deletedAt>0;ensureDiagramHistory(note);
        ScrollView scroll=new ScrollView(context);LinearLayout area=new LinearLayout(context);area.setOrientation(LinearLayout.VERTICAL);scroll.addView(area);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        final Button[] editControl={null};DiagramView canvas=new DiagramView(context,currentDiagram(note),design,new DiagramView.Listener(){public void selected(String id){diagramSelection=id;if(editControl[0]!=null)controlAvailable(editControl[0],id!=null);}public void changed(Diagram value){if(!readOnly)saveDiagram(value);else renderDiagram();}});canvas.setEnabled(!readOnly);canvas.show(currentDiagram(note),readOnly?null:diagramSelection);area.addView(canvas,new LinearLayout.LayoutParams(-1,-2));
        if(currentDiagram(note).nodes.isEmpty()){TextView hint=text("Start with a shape. Tap it to select; drag to move. Use Connect to add an arrow.",14);hint.setTextColor(muted);area.addView(hint);}
        LinearLayout dock=tools(root);dock.setVisibility(readOnly?View.GONE:View.VISIBLE);dock.setGravity(android.view.Gravity.CENTER);dock.setBackground(design.shape(surface,18));
        if(!readOnly){Button add=icon("Add shape","add",()->{});add.setTag("diagram-add-shape");dock.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout shapes=menu("Add shape",add);for(String shape:Diagram.SHAPES){Button option=button(Diagram.shapeName(shape),()->addShape(shape));DiagramShapeIcon preview=new DiagramShapeIcon(shape,darkTheme);preview.setBounds(0,0,dp(56),dp(40));option.setCompoundDrawablesRelative(preview,null,null,null);option.setCompoundDrawablePadding(dp(16));shapes.addView(option,new LinearLayout.LayoutParams(-1,-2));}
            Button edit=icon("Edit shape","edit",()->{});editControl[0]=edit;controlAvailable(edit,currentDiagram(note).find(diagramSelection)!=null);LinearLayout editTools=menu("Selected shape",edit);menuRows(editTools,new String[]{"Edit shape","Connect","Delete shape"},new Runnable[]{this::editShape,this::connectShape,this::deleteSelectedShape});
            Button undoDiagram=icon("Undo diagram","undo",()->stepDiagramHistory(false));undoDiagram.setTag("diagram-undo");controlAvailable(undoDiagram,diagramHistory.canUndo(currentDiagram(note)));dock.addView(undoDiagram,new LinearLayout.LayoutParams(dp(48),dp(48)));
            Button redoDiagram=icon("Redo diagram","redo",()->stepDiagramHistory(true));redoDiagram.setTag("diagram-redo");controlAvailable(redoDiagram,diagramHistory.canRedo(currentDiagram(note)));dock.addView(redoDiagram,new LinearLayout.LayoutParams(dp(48),dp(48)));
            dock.addView(edit,new LinearLayout.LayoutParams(dp(48),dp(48)));Button more=icon("Diagram options","more",()->{});dock.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout options=menu("Diagram options",more);menuRows(options,new String[]{"Group shapes","Edit arrows"},new Runnable[]{this::groupShapes,this::chooseArrow});}

    }
    private void saveDiagram(Diagram value){Note n=find(editing);if(n==null || n.deletedAt>0)return;ensureDiagramHistory(n);diagramHistory.record(currentDiagram(n),value);captureUndo(Arrays.asList(n),true);Note changed=replaceDiagram(n,value);recordCanonicalChange(n,changed);replace(changed);queueSave();View undo=root.findViewWithTag("diagram-undo"),redo=root.findViewWithTag("diagram-redo");if(undo!=null)controlAvailable(undo,diagramHistory.canUndo(value));if(redo!=null)controlAvailable(redo,diagramHistory.canRedo(value));}
    private void ensureDiagramHistory(Note note){if(!note.id.equals(diagramHistoryNote)){diagramHistoryNote=note.id;diagramHistory=new app.space.notes.domain.DiagramHistory();}}
    private void stepDiagramHistory(boolean redo){Note n=find(editing);if(n==null || n.deletedAt>0)return;ensureDiagramHistory(n);Diagram next=redo?diagramHistory.redo(currentDiagram(n)):diagramHistory.undo(currentDiagram(n));if(next==currentDiagram(n))return;Note changed=replaceDiagram(n,next);recordCanonicalChange(n,changed);replace(changed);queueSave();renderDiagram();}
    private void groupShapes(){Note n=find(editing);if(n==null || n.deletedAt>0)return;Diagram.Node selectedBefore=currentDiagram(n).find(diagramSelection);if(currentDiagram(n).nodes.size()<2 && (selectedBefore==null || selectedBefore.group.isEmpty())){LinearLayout help=sheets.panel("Group shapes",null);help.addView(text("Add at least two shapes before grouping. Your diagram is unchanged.",15));Button add=button("Add shape",()->{sheets.close();View launcher=root.findViewWithTag("diagram-add-shape");if(launcher!=null)launcher.performClick();});design.primary(add);help.addView(add);help.addView(button("Cancel",()->sheets.close()));sheets.show(help,"Group shapes");return;}String[] names=new String[currentDiagram(n).nodes.size()];boolean[] checked=new boolean[names.length];Set<String> ids=new HashSet<>();for(int i=0;i<names.length;i++){Diagram.Node shape=currentDiagram(n).nodes.get(i);names[i]=Diagram.shapeName(shape.shape)+" · "+shape.text;checked[i]=shape.id.equals(diagramSelection);if(checked[i])ids.add(shape.id);}
        final android.app.AlertDialog[] dialogRef=new android.app.AlertDialog[1];Runnable validate=()->{if(dialogRef[0]==null)return;Button group=dialogRef[0].getButton(android.content.DialogInterface.BUTTON_POSITIVE);boolean ready=ids.size()>=2;group.setEnabled(ready);group.setAlpha(ready?1f:.45f);group.setContentDescription(ready?"Group selected shapes":"Group selected shapes. Choose at least two shapes.");};
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("Group shapes").setMultiChoiceItems(names,checked,(choice,index,on)->{String id=currentDiagram(n).nodes.get(index).id;if(on)ids.add(id);else ids.remove(id);validate.run();}).setPositiveButton("Group",(choice,which)->{if(ids.size()<2)return;saveDiagram(currentDiagram(n).group(ids));renderDiagram();}).setNeutralButton("Ungroup selected",(choice,which)->{Diagram.Node shape=currentDiagram(n).find(diagramSelection);if(shape==null || shape.group.isEmpty())return;saveDiagram(currentDiagram(n).ungroup(shape.id));renderDiagram();}).setNegativeButton("Cancel",null).create();dialogRef[0]=dialog;design.show(dialog);validate.run();Diagram.Node selected=currentDiagram(n).find(diagramSelection);Button ungroup=dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);boolean grouped=selected!=null && !selected.group.isEmpty();ungroup.setEnabled(grouped);ungroup.setAlpha(grouped?1f:.45f);
    }
    private void chooseArrow(){Note n=find(editing);if(n==null || n.deletedAt>0)return;if(currentDiagram(n).edges.isEmpty()){host.showMessage("Use Edit shape → Connect to add an arrow.");return;}String[] names=new String[currentDiagram(n).edges.size()];for(int i=0;i<names.length;i++){Diagram.Edge edge=currentDiagram(n).edges.get(i);names[i]=currentDiagram(n).find(edge.from).text+" → "+currentDiagram(n).find(edge.to).text+" · "+edge.label;}design.show(new android.app.AlertDialog.Builder(context).setTitle("Edit arrow").setItems(names,(dialog,index)->editArrow(currentDiagram(n).edges.get(index).id)).setNegativeButton("Cancel",null).create());}
    private void addShape(String shape){Note n=find(editing);if(n==null || n.deletedAt>0)return;
        int index=currentDiagram(n).nodes.size();String id=UUID.randomUUID().toString();int w=shape.equals("decision")?220:200,h=shape.equals("decision")?140:100;
        Diagram.Node node=new Diagram.Node(id,shape,Diagram.shapeName(shape),30+(index%3)*240,30+((index/3)%5)*170,w,h,n.color);
        saveDiagram(currentDiagram(n).put(node));diagramSelection=id;renderDiagram();
    }
    private Diagram.Node selectedShape(){Note n=find(editing);Diagram.Node node=n==null?null:currentDiagram(n).find(diagramSelection);if(node==null)host.showMessage("Tap a shape on the canvas first.");return node;}
    private void editShape(){Diagram.Node node=selectedShape();if(node==null)return;
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);EditText input=new EditText(context);input.setText(node.text);input.setHint("Shape text");content.addView(input);
        Spinner colors=new Spinner(context);String[] colourNames=NotesBoard.NAMES.clone();colourNames[0]="Paper";content.setPadding(dp(20),dp(8),dp(20),dp(8));design.entry(input,18);content.addView(design.label("Shape colour",13,muted));colors.setContentDescription("Shape colour");colors.setAdapter(new ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,colourNames));colors.setSelection(node.color);content.addView(colors);
        design.show(new android.app.AlertDialog.Builder(context).setTitle("Edit shape").setView(content).setPositiveButton("Save",(d,w)->{saveDiagram(currentDiagram(find(editing)).put(node.edit(input.getText().toString(),colors.getSelectedItemPosition())));renderDiagram();}).setNegativeButton("Cancel",null).create());
    }
    private void connectShape(){Diagram.Node from=selectedShape();if(from==null)return;List<Diagram.Node> others=new ArrayList<>();for(Diagram.Node n:currentDiagram(find(editing)).nodes)if(!n.id.equals(from.id))others.add(n);
        if(others.isEmpty()){host.showMessage("Add another shape first.");return;}String[] names=new String[others.size()];for(int i=0;i<names.length;i++)names[i]=Diagram.shapeName(others.get(i).shape)+" · "+others.get(i).text;
        design.show(new android.app.AlertDialog.Builder(context).setTitle("Arrow destination").setItems(names,(dialog,index)->{saveDiagram(currentDiagram(find(editing)).connect(from.id,others.get(index).id,""));renderDiagram();}).setNegativeButton("Cancel",null).create());
    }
    private void editArrow(String id){Note n=find(editing);for(Diagram.Edge edge:currentDiagram(n).edges)if(edge.id.equals(id)){
        EditText input=new EditText(context);design.entry(input,16);input.setText(edge.label);input.setHint("Arrow label, e.g. Yes / No");design.show(new android.app.AlertDialog.Builder(context).setTitle("Arrow label").setView(input).setPositiveButton("Save",(d,w)->{saveDiagram(currentDiagram(find(editing)).labelEdge(id,input.getText().toString()));renderDiagram();}).setNeutralButton("Remove arrow",(d,w)->{saveDiagram(currentDiagram(find(editing)).removeEdge(id));renderDiagram();}).setNegativeButton("Cancel",null).create());return;
    }}
    private void deleteSelectedShape(){Diagram.Node node=selectedShape();if(node==null)return;Diagram value=currentDiagram(find(editing)).remove(node.id);diagramSelection=null;saveDiagram(value);renderDiagram();}
    public void exportPdf(android.net.Uri uri){exportPdf(uri,false);}
    public void exportPdf(android.net.Uri uri,boolean printGuides){whenReady(()->{
        persistDraft();List<Note> snapshot=new ArrayList<>(targets());if(snapshot.isEmpty()){host.showMessage("Open or select notes before exporting PDF.");return;}
        busy=true;disk.execute(()->{
            java.io.File temporary=null;String message;
            try{
                LocalBackupFiles.requireLocal(uri);temporary=java.io.File.createTempFile("mynotes-pdf-",".pdf",context.getCacheDir());int pages;
                try(java.io.OutputStream out=new java.io.FileOutputStream(temporary)){pages=PdfExporter.write(snapshot,out,printGuides);}
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
    private void chooseScan(){persistDraft();String[] names={"English","Hindi / Devanagari","Kannada"};String[] codes={"eng","hin","kan"};choiceSheet("Read image · choose a language",names,-1,index->host.chooseOcrImage(codes[index]));}
    public void recognizeImage(android.net.Uri uri,String language){whenReady(()->{
        persistDraft();if(ocrCancellation!=null)ocrCancellation.cancel();final OfflineOcr.Cancellation cancellation=new OfflineOcr.Cancellation();ocrCancellation=cancellation;final long ticket=++ocrSequence;final String target=editing;
        ocrPanel=true;title=null;body=null;search=null;querySequence++;clearPage();header("Read image");root.addView(text("Reading this image on your device. No image is uploaded. Handwriting and difficult photos may be inaccurate; review the extracted words before saving.",16));root.addView(new ProgressBar(context));row(new String[]{"Cancel reading"},new Runnable[]{this::cancelOcr});
        ocrWorker.execute(()->{
            try{OfflineOcr.Result result=OfflineOcr.recognize(context,uri,language,cancellation);main.post(()->{if(closed || ticket!=ocrSequence || cancellation.cancelled())return;renderOcrReview(result,target);});}
            catch(Exception | LinkageError | OutOfMemoryError e){log("ocr","failed "+e.getClass().getSimpleName());Diagnostics.failure("ocr.failure",e);main.post(()->{if(closed || ticket!=ocrSequence || cancellation.cancelled())return;ocrPanel=false;returnToNoteOrHome();host.showMessage("OCR did not change any note: "+e.getMessage()+". Original image is unchanged; try another image or language.");});}
        });
    });}
    private void cancelOcr(){if(ocrCancellation!=null)ocrCancellation.cancel();ocrSequence++;ocrPanel=false;returnToNoteOrHome();}
    private void renderOcrReview(OfflineOcr.Result result,String target){
        clearPage();header("Review recognized text");String language=result.language.equals("eng")?"English":result.language.equals("hin")?"Hindi / Devanagari":result.language.equals("kan")?"Kannada":result.language;root.addView(text("Offline · "+language+" · confidence "+result.confidence+" / 100\nReview names, numbers and line breaks. Original image is unchanged.",13));TextView emptyResult=text("No text to save. Type text below, or go Back and try another image or language.",14);emptyResult.setTextColor(muted);root.addView(emptyResult);
        EditText input=new EditText(context);design.entry(input,18);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setHint("Recognized text — edit before saving");input.setGravity(android.view.Gravity.TOP);input.setText(result.text);root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=target==null?rowIn(root,new String[]{"Save as note"},new Runnable[]{()->saveOcrText(input.getText().toString(),null)}):rowIn(root,new String[]{"Append to note","Save as new note"},new Runnable[]{()->saveOcrText(input.getText().toString(),target),()->saveOcrText(input.getText().toString(),null)});
        Runnable availability=()->{boolean hasText=!input.getText().toString().trim().isEmpty();emptyResult.setVisibility(hasText?View.GONE:View.VISIBLE);for(int i=0;i<actions.getChildCount();i++)controlAvailable(actions.getChildAt(i),hasText);};input.addTextChangedListener(watcher(availability));availability.run();
    }
    private void saveOcrText(String value,String target){
        if(value.trim().isEmpty()){host.showMessage("There is no recognized text to save. Notes remain unchanged.");return;}
        Note current=target==null?null:find(target);long now=System.currentTimeMillis();
        if(target!=null && (current==null || current.deletedAt>0)){host.showMessage("Original note is unavailable or in Trash. Use Save as new note.");return;}
        if(current==null){current=Note.create(now).edit("Text from image",value,now);notes.add(current);}
        else{captureUndo(Arrays.asList(current),true);if(current.content!=null)current=current.withContent(current.content.insert(current.content.blocks.size(),app.space.notes.domain.DocumentContent.Block.text(UUID.randomUUID().toString(),new app.space.notes.domain.RichText(value,Collections.emptyList()))),now);else if(current.checklist){List<Note.Item> rows=new ArrayList<>(current.items);for(String line:value.split("\\n",-1))rows.add(new Note.Item(UUID.randomUUID().toString(),line,false,0));current=current.list(current.title,rows,current.hideChecked,now);}else current=current.rich(current.title,current.body+(current.body.isEmpty()?"":"\n\n")+value,current.marks,now);replace(current);}
        ocrPanel=false;editing=current.id;queueSave();renderEditor();host.showMessage("Reviewed text saved locally. Original image is unchanged.");
    }
    public void startVoiceCapture(){whenReady(()->{
        persistDraft();if(capture!=null){host.showMessage("A recording is already active.");return;}
        try{
            capture=new VoiceCapture(context,new VoiceCapture.Listener(){
                public void started(String id){main.post(()->{if(closed)return;voicePage=true;title=null;body=null;search=null;clearPage();header("Recording voice");
                    LinearLayout recording=new LinearLayout(context);recording.setOrientation(LinearLayout.VERTICAL);recording.setGravity(android.view.Gravity.CENTER);ScrollView recordingScroll=new ScrollView(context);recordingScroll.setFillViewport(true);recordingScroll.addView(recording);root.addView(recordingScroll,new LinearLayout.LayoutParams(-1,0,1));TextView live=design.heading("Microphone on");recording.addView(live);android.widget.Chronometer timer=new android.widget.Chronometer(context);timer.setTextColor(design.accent);timer.setTextSize(36);timer.setBase(android.os.SystemClock.elapsedRealtime());timer.start();recording.addView(timer);TextView privacy=text("Audio stays on this device",14);privacy.setTextColor(muted);recording.addView(privacy);
                    recording.addView(text("Stop to save the original audio and start bundled offline English transcription. Review the transcript; language, accent and noise affect accuracy. Leaving the app stops recording safely.",14));Button stop=button("Stop and transcribe",()->{if(capture!=null)capture.stop();timer.stop();live.setText("Saving audio…");});stop.setGravity(android.view.Gravity.CENTER);stop.setTextColor(design.accentInk);stop.setBackground(design.ripple(Color.parseColor("#E8C58E"),18));root.addView(stop,new LinearLayout.LayoutParams(-1,-2));});}
                public void saved(String id){VoiceTasks.transcribe(context,id);main.post(()->{capture=null;if(closed)return;addVoicePlaceholder(id);if(voicePage)refreshVoiceInbox();host.showMessage("Audio saved locally. Offline English transcription started automatically.");});}
                public void failed(String message){main.post(()->{capture=null;if(closed)return;voicePage=false;returnToNoteOrHome();host.showMessage("Recording stopped: "+message+". Other features remain available; check Voice inbox for recovered audio.");});}
            });captureWorker.execute(capture);
        }catch(Exception e){capture=null;host.showMessage("Recording could not start: "+e.getMessage()+". Notes are unchanged.");}
    });}
    private void addVoicePlaceholder(String id){knownRecordings.add(id);String noteId=app.space.notes.domain.VoiceTranscripts.noteId(id);if(find(noteId)==null){notes.add(app.space.notes.domain.VoiceTranscripts.placeholder(id,System.currentTimeMillis()));queueSave();}}
    private void voiceComplete(String id,String text,String failure,boolean previouslyCreated){
        if(closed)return;if(busy){main.postDelayed(()->voiceComplete(id,text,failure,previouslyCreated),200);return;}
        whenReady(()->{
        if(closed)return;persistDraft();String target=app.space.notes.domain.VoiceTranscripts.noteId(id);
        if(!app.space.notes.domain.VoiceTranscripts.mayApplyToLibrary(find(target)!=null,previouslyCreated)){if(voicePage)refreshVoiceInbox();return;}
        addVoicePlaceholder(id);Note note=find(target);
        if(!text.isEmpty() && note!=null && note.deletedAt==0){Note changed=app.space.notes.domain.VoiceTranscripts.apply(note,id,text,System.currentTimeMillis());if(changed!=note){replace(changed);queueSave();}}
        if(voicePage)refreshVoiceInbox();else if(editing==null && !storagePage)filterNow();
        if(!failure.isEmpty())host.showMessage("Offline transcription unavailable. Audio is retained in Voice inbox; retry or export it. Other notes features are unaffected.");
    });}
    private void renderVoiceInbox(){renderVoiceInbox(this::renderHome);}
    private void renderVoiceInbox(Runnable parent){voiceReturn=parent;refreshVoiceInbox();}
    private void leaveVoiceInbox(){if(capture!=null){capture.stop();pageBack=this::leaveVoiceInbox;host.showMessage("Stopping capture and preserving audio…");return;}voicePage=false;voiceReturn.run();}
    private void refreshVoiceInbox(){
        persistDraft();voicePage=true;storagePage=true;editing=null;title=null;body=null;search=null;querySequence++;clearPage();header("Voice inbox",this::leaveVoiceInbox);
        final long inboxToken=voiceInboxSequence;
        LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(context);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(text("Reading local recordings…",14));
        LinearLayout controls=tools(root);tool(controls,"Record voice","mic",host::requestVoiceCapture);View spacer=new View(context);controls.addView(spacer,new LinearLayout.LayoutParams(0,1,1));tool(controls,"Refresh","retry",this::refreshVoiceInbox);
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
    private void scheduleDraft(){dirty=true;app.space.notes.domain.DocumentHistory editHistory=savedHistories.get(editing);if(editHistory!=null)editHistory.invalidateRedo();syncUndoControl();if(status!=null){status.setText("Unsaved changes…");if(canonicalEditor!=null)status.setVisibility(View.VISIBLE);}main.removeCallbacks(saveDraft);main.postDelayed(saveDraft,350);}
    private TextEditHistory currentTextHistory(){EditText field=lastWritingFocus!=null?lastWritingFocus:body!=null?body:title;return field==null || !field.isEnabled()?null:textHistories.get(field);}
    private void installTextHistory(EditText field){if(field==null || textHistories.containsKey(field))return;TextEditHistory history=new TextEditHistory(field,()->binding,this::syncUndoControl);textHistories.put(field,history);if(field instanceof WritingField){WritingField writing=(WritingField)field;writing.undoText=()->stepTextHistory(false);writing.redoText=()->stepTextHistory(true);}syncUndoControl();}
    private boolean stepTextHistory(boolean redo){TextEditHistory history=currentTextHistory();if(history==null || (redo?!history.canRedo():!history.canUndo()))return stepSavedHistory(redo);persistDraft();Note before=find(editing);boolean previous=binding;binding=true;boolean applied;historyStepping=true;try{applied=redo?history.redo():history.undo();if(applied){Note note=find(editing);EditText field=history.field();if(note!=null && note.checklist && field.getTag() instanceof String){List<Note.Item> rows=new ArrayList<>(note.items);for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(field.getTag())){Note.Item item=rows.get(i);rows.set(i,item.change(field.getText().toString(),item.checked,item.depth));}replace(note.list(note.title,rows,note.hideChecked,System.currentTimeMillis()));}binding=previous;invalidateFullUndo();dirty=true;persistDraft();Note target=find(editing);if(before!=null && target!=null)historyFor(before.id).align(before,target,redo);}}finally{binding=previous;historyStepping=false;}if(applied){queueSave();if(history.field().hasFocus())((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(history.field());updateFormatting();syncUndoControl();}return applied;}
    private void syncUndoControl(){TextEditHistory history=currentTextHistory();boolean typing=history!=null && history.canUndo() && !noteUndoIsLatest;if(undoControl!=null){boolean available=typing || undo!=null || !undoBatch.isEmpty() || hasSavedHistory(false);undoControl.setEnabled(available);undoControl.setAlpha(available?1f:.38f);undoControl.setContentDescription("Undo");undoControl.setTooltipText(typing?"Undo text":"Undo note action");androidx.core.view.ViewCompat.setStateDescription(undoControl,typing?"Text edit":"Note action");}if(redoTextControl!=null)controlAvailable(redoTextControl,(history!=null && history.canRedo() && !noteUndoIsLatest) || hasSavedHistory(true));}
    private void invalidateFullUndo(){noteUndoIsLatest=false;undoCreatedId=null;if(undoFull){undoBatch.clear();undoFull=false;}syncUndoControl();}
    private void captureUndo(List<Note> before,boolean full){noteUndoIsLatest=true;undoCreatedId=null;undo=null;undoBatch=new ArrayList<>(before);undoFull=full;syncUndoControl();}
    private void format(String type){
        if(body==null)return;int anchor=body.getSelectionStart(),focus=body.getSelectionEnd();int start=Math.min(anchor,focus),end=Math.max(anchor,focus);if(start<0)return;
        boolean paragraph=type.equals("h1") || type.equals("h2") || type.equals("text");if(!paragraph && start==end){host.showMessage("Select text to apply inline formatting. Headings apply to the current paragraph.");return;}
        if(paragraph){String text=body.getText().toString();int lineStart=text.lastIndexOf('\n',Math.max(-1,start-1))+1;int lineEnd=text.indexOf('\n',start);if(lineEnd<0)lineEnd=text.length();if(lineStart==lineEnd){pendingParagraphStyle=type.equals("text")?null:type;updateFormatting();return;}}
        persistDraft();Note n=find(editing);if(n==null)return;TextEditHistory history=textHistories.get(body);TextEditHistory.State beforeStyle=history==null?null:history.snapshot();captureUndo(Arrays.asList(n),true);
        List<Note.Mark> marks=paragraph?TextFormatting.paragraph(TextStyles.read(body.getText()),body.getText().toString(),type,start,end):TextFormatting.apply(TextStyles.read(body.getText()),type,start,end);
        binding=true;try{TextStyles.reflow(body,marks);body.setSelection(anchor,focus);}finally{binding=false;}
        if(history!=null){history.recordCommand(beforeStyle);noteUndoIsLatest=false;syncUndoControl();}dirty=true;persistDraft();updateFormatting();body.post(()->{if(body!=null && body.getLayout()!=null)Diagnostics.record("format.layout","lines="+body.getLayout().getLineCount()+" first_line_height="+body.getLayout().getLineBottom(0)+" viewport_height="+body.getHeight());});log("format","applied type="+type+" range="+start+".."+end);
    }
    private static final class ChecklistDrag {final String note,item;ChecklistDrag(String note,String item){this.note=note;this.item=item;}}
    private Button checklistHandle(String noteId,String itemId,int position){Button handle=icon("Reorder list item "+position,"drag",()->{Note note=find(noteId);if(note==null || !note.id.equals(editing) || note.deletedAt>0)return;String[] positions=new String[note.items.size()];int selected=-1;for(int i=0;i<positions.length;i++){positions[i]="Position "+(i+1);if(note.items.get(i).id.equals(itemId))selected=i;}final int from=selected;choiceSheet("Move list item",positions,selected,to->moveChecklistItem(noteId,itemId,to>from?to+1:to));});handle.setTooltipText("Drag to reorder; accessibility click chooses a position");handle.setOnTouchListener((view,event)->{if(event.getActionMasked()!=android.view.MotionEvent.ACTION_DOWN)return false;checklistDragState="starting";boolean started=view.startDragAndDrop(new android.content.ClipData("MyNotes list item",new String[]{"application/vnd.mynotes.checklist-item"},new android.content.ClipData.Item("")),new View.DragShadowBuilder(view),new ChecklistDrag(noteId,itemId),0);checklistDragState="started="+started;if(started)view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);return started;});return handle;}
    private ChecklistDrag checklistDrag(android.view.DragEvent event,String noteId){if(!(event.getLocalState() instanceof ChecklistDrag))return null;ChecklistDrag drag=(ChecklistDrag)event.getLocalState();Note note=find(noteId);return note!=null && note.checklist && note.deletedAt==0 && noteId.equals(editing) && noteId.equals(drag.note)?drag:null;}
    private void checklistAutoScroll(float y){if(noteScroll==null)return;if(y<dp(64))noteScroll.smoothScrollBy(0,-dp(32));else if(y>noteScroll.getHeight()-dp(64))noteScroll.smoothScrollBy(0,dp(32));}
    private boolean checklistScrollDrag(android.view.DragEvent event,String noteId){ChecklistDrag drag=checklistDrag(event,noteId);if(drag==null)return false;if(event.getAction()==android.view.DragEvent.ACTION_DRAG_LOCATION)checklistAutoScroll(event.getY());if(event.getAction()==android.view.DragEvent.ACTION_DROP){Note note=find(noteId);checklistDragState="scroll drop";moveChecklistItem(noteId,drag.item,note.items.size());}return true;}
    private void wireChecklistDrop(View row,String noteId,String targetId){row.setOnDragListener((view,event)->{ChecklistDrag drag=checklistDrag(event,noteId);if(drag==null)return false;switch(event.getAction()){case android.view.DragEvent.ACTION_DRAG_ENTERED:view.setBackground(design.choice(true));break;case android.view.DragEvent.ACTION_DRAG_EXITED:case android.view.DragEvent.ACTION_DRAG_ENDED:view.setBackgroundColor(Color.TRANSPARENT);break;case android.view.DragEvent.ACTION_DRAG_LOCATION:if(noteScroll!=null){int[] a=new int[2],b=new int[2];view.getLocationOnScreen(a);noteScroll.getLocationOnScreen(b);checklistAutoScroll(a[1]-b[1]+event.getY());}break;case android.view.DragEvent.ACTION_DROP:checklistDragState="row drop";Note note=find(noteId);int index=-1;for(int i=0;i<note.items.size();i++)if(note.items.get(i).id.equals(targetId))index=i;if(index>=0)moveChecklistItem(noteId,drag.item,index+(event.getY()>view.getHeight()/2f?1:0));break;}return true;});}
    private void moveChecklistItem(String noteId,String itemId,int insertion){persistDraft();Note note=find(noteId);if(note==null || note.deletedAt>0 || !note.checklist || !noteId.equals(editing))return;try{List<Note.Item> moved=app.space.notes.domain.ChecklistOrder.move(note.items,itemId,insertion);if(moved==note.items){checklistDragState="no-op";return;}checklistDragState="moved";captureUndo(Arrays.asList(note),true);replace(note.list(note.title,moved,note.hideChecked,System.currentTimeMillis()));focusedItem=itemId;queueSave();renderEditor();}catch(IllegalArgumentException stale){host.showMessage("List changed. Choose the item again; nothing was moved.");}}
    private void listItemOptions(String itemId){
        persistDraft();Note note=find(editing);if(note==null || note.deletedAt>0 || !note.checklist)return;int index=-1;for(int i=0;i<note.items.size();i++)if(note.items.get(i).id.equals(itemId))index=i;if(index<0)return;focusedItem=itemId;Note.Item item=note.items.get(index);
        ((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);
        LinearLayout panel=sheets.panel("List item "+(index+1),null);String[] names={"Indent item","Outdent item","Remove item"},actions={"indent","outdent","remove"};boolean[] allowed={item.depth<3,item.depth>0,true};
        for(int i=0;i<names.length;i++){String action=actions[i];Button option=button(names[i],()->{sheets.close();focusedItem=itemId;listAction(action);});String symbol=actionSymbol(names[i]);if(symbol!=null){NotesIcon glyph=new NotesIcon(symbol,design.accent);glyph.setBounds(0,0,dp(24),dp(24));option.setCompoundDrawablesRelative(glyph,null,null,null);option.setCompoundDrawablePadding(dp(14));}controlAvailable(option,allowed[i]);panel.addView(option,new LinearLayout.LayoutParams(-1,-2));}sheets.show(panel,"List item "+(index+1));
    }
    private void listAction(String action){
        persistDraft();Note n=find(editing);if(n==null || n.deletedAt>0 || !n.checklist)return;
        List<Note.Item> rows=new ArrayList<>(n.items);int index=-1;
        for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(focusedItem))index=i;
        boolean adding=action.equals("add") || action.equals("add-after");
        if(!adding && !action.equals("hide") && index<0){host.showMessage("Choose an item's actions, or tap a list item first.");return;}
        if(index>=0 && ((action.equals("up") && index==0) || (action.equals("down") && index+1==rows.size()) || (action.equals("indent") && rows.get(index).depth>=3) || (action.equals("outdent") && rows.get(index).depth==0)))return;
        captureUndo(Arrays.asList(n),true);
        if(adding){Note.Item next=new Note.Item(UUID.randomUUID().toString(),"",false,index<0?0:rows.get(index).depth);rows.add(action.equals("add-after") && index>=0?index+1:rows.size(),next);focusedItem=next.id;}
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
    private void renameNoteContent(String key,String fallback){
        persistDraft();Note before=find(editing);if(before==null || before.deletedAt>0)return;String id=before.id;
        EditText name=new EditText(context);design.entry(name,16);name.setSingleLine(true);name.setHint("Content name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});name.setText(before.contentName(key,fallback));name.selectAll();
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("Rename "+fallback.toLowerCase(java.util.Locale.ROOT)).setView(name).setPositiveButton("Save name",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{Note current=find(id);if(current==null || current.deletedAt>0 || !id.equals(editing)){dialog.dismiss();return;}try{String value=name.getText().toString().trim();if(value.equals(current.contentName(key,fallback)) || value.isEmpty() && !current.contentNames.containsKey(key)){dialog.dismiss();return;}Note next=current.namedContent(key,value,System.currentTimeMillis());captureUndo(Arrays.asList(current),true);replace(next);queueSave();dialog.dismiss();renderEditor();}catch(IllegalArgumentException invalid){name.setError(invalid.getMessage());}}));design.show(dialog);
    }
    private void convertNote(){persistDraft();Note n=find(editing);captureUndo(Arrays.asList(n),true);replace(n.convert(System.currentTimeMillis()));queueSave();renderEditor();}
    private void confirmPermanentDelete(boolean all){
        if(busy){host.showMessage("Please wait for the current storage operation.");return;}
        Set<String> ids=new HashSet<>();for(Note n:notes)if(n.deletedAt>0 && (all || selectedIds.contains(n.id)))ids.add(n.id);
        if(ids.isEmpty()){host.showMessage("Trash is empty.");return;}
        design.show(new android.app.AlertDialog.Builder(context).setTitle(all?"Empty Trash?":"Delete permanently?").setMessage("Permanently remove "+ids.size()+" trashed notes from this library? This cannot be undone. Existing backups and original voice recordings are kept separately.").setNegativeButton("Cancel",null).setPositiveButton("Delete permanently",(dialog,which)->{
            busy=true;disk.execute(()->{try{
                repository.schedulePortableDeletion(ids);VoiceFiles files=new VoiceFiles(context);for(String source:files.list())if(ids.contains(app.space.notes.domain.VoiceTranscripts.noteId(source)))files.noteCreated(source);
                main.post(()->{busy=false;if(closed)return;persistDraft();notes.removeIf(n->n.deletedAt>0 && ids.contains(n.id));undo=null;undoBatch.clear();undoCreatedId=null;selectedIds.clear();editing=null;queueSave();renderHome();log("trash.purge","requested count="+ids.size());});
            }catch(Exception failure){Diagnostics.failure("trash.purge.failure",failure);main.post(()->{busy=false;if(!closed)host.showMessage("Nothing was deleted. Could not protect recording recovery; please retry.");});}});
        }).create());
    }
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
            EditText input=new EditText(context);design.entry(input,16);input.setText(label);input.setHint("Label name");android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("Rename label").setView(input).setPositiveButton("Rename",null).setNeutralButton("Remove label",(d,w)->renameLabel(label,"")).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(input.getText().toString().trim().isEmpty()){host.showMessage("Enter a label name, or use Remove label.");return;}if(renameLabel(label,input.getText().toString()))dialog.dismiss();}));design.show(dialog);
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
    private java.io.File drawingDraft(String id){return drawingDraft(id,null);}
    private java.io.File drawingDraft(String id,String image){String key=image==null?id:id+"\u0000"+image;return new java.io.File(context.getFilesDir(),"drawing-draft-"+UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".png");}
    private boolean stashDrawing(){
        DrawingPad pad=drawingPad;if(pad==null || drawingNote==null || !pad.ready() || stashedDrawing==pad.revision())return true;
        final android.graphics.Bitmap snapshot;try{snapshot=pad.snapshot();}catch(OutOfMemoryError failure){host.showMessage("Not enough memory to save the drawing. Keep this page open and try again.");return false;}
        final long captured=pad.revision();stashedDrawing=captured;java.io.File target=drawingDraft(drawingNote,drawingImage);
        disk.execute(()->{android.util.AtomicFile file=new android.util.AtomicFile(target);java.io.FileOutputStream out=null;try{out=file.startWrite();if(!snapshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out))throw new java.io.IOException("Cannot save drawing draft");file.finishWrite(out);out=null;}catch(Exception | OutOfMemoryError failure){if(out!=null)file.failWrite(out);log("drawing-draft","save pending");main.post(()->{if(drawingPad==pad && stashedDrawing==captured)stashedDrawing=-1;if(!closed)host.showMessage("Drawing draft save failed. The last saved version was kept; check device storage.");});}finally{snapshot.recycle();}});return true;
    }
    private void leaveDrawing(){if(stashDrawing())renderEditor();}
    private void deleteSavedDrawingDraft(java.io.File file,String expected){disk.execute(()->{try{android.util.AtomicFile draft=new android.util.AtomicFile(file);try(java.io.InputStream in=draft.openRead()){if(!expected.equals(DrawingClipboard.digest(in)))return;}draft.delete();}catch(java.io.IOException missing){/* A newer or unavailable draft is never removed. */}});}
    private void copyDrawing(DrawingPad pad){sheets.close();if(!pad.ready() || pad.saving()){host.showMessage("Wait for the drawing before copying.");return;}final android.graphics.Bitmap snapshot;try{snapshot=pad.snapshot();}catch(OutOfMemoryError pressure){host.showMessage("Not enough memory to copy; your drawing is retained.");return;}disk.execute(()->{try{android.net.Uri uri=DrawingClipboard.write(context,snapshot);main.post(()->{if(!closed)try{DrawingClipboard.publish(context,uri);host.showMessage("Drawing copied as an image. Paste into an app that supports images.");}catch(Exception denied){host.showMessage("Clipboard was not updated. Your drawing is retained.");}});}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed)host.showMessage("Drawing could not be copied. Your note and drawing are unchanged.");});}finally{snapshot.recycle();}});}
    private void deleteDrawing(String id,String imageId,DrawingPad pad){if(pad.saving()){host.showMessage("Wait for drawing save to finish.");return;}confirmSheet("Delete drawing?",imageId==null?"Delete this unsaved drawing draft? Your existing note is unchanged.":"Remove this attached drawing from the note? Other content is kept. You can Undo the note change after returning.","Delete drawing",()->{if(imageId!=null){Note current=find(id);if(current==null)return;captureUndo(Arrays.asList(current),true);List<app.space.notes.domain.NoteImage> keep=new ArrayList<>(current.images);keep.removeIf(value->value.id.equals(imageId));replace(current.images(keep,System.currentTimeMillis()));queueSave(()->disk.execute(()->new android.util.AtomicFile(drawingDraft(id,imageId)).delete()));}else disk.execute(()->new android.util.AtomicFile(drawingDraft(id,null)).delete());drawingPad=null;drawingNote=null;renderEditor();});}
    private void saveDrawing(DrawingPad pad,String id){
        if(busy){host.showMessage("Wait for the storage operation before saving this drawing.");return;}
        if(!pad.ready()){host.showMessage("Drawing is still opening.");return;}if(pad.saving())return;
        Note current=find(id);if(current==null || current.deletedAt>0)return;if(current.images.size()>=8 && drawingImage==null){host.showMessage("This note already has eight images. Your drawing draft is retained.");stashDrawing();return;}
        if(!stashDrawing())return;final android.graphics.Bitmap bitmap;try{bitmap=pad.snapshot();}catch(OutOfMemoryError failure){host.showMessage("Not enough memory to attach the drawing. Its draft was kept.");return;}
        final String replacing=drawingImage;final app.space.notes.domain.NoteImage original=current.images.stream().filter(value->value.id.equals(replacing)).findFirst().orElse(null);if(replacing!=null && original==null){bitmap.recycle();host.showMessage("This drawing was removed. Its draft was kept.");return;}final java.io.File draft=drawingDraft(id,replacing);
        pad.saving(true);disk.execute(()->{try{String sourceDigest=DrawingClipboard.digest(bitmap);app.space.notes.domain.NoteImage encoded=NoteImageFiles.encode(bitmap,"Drawing");final app.space.notes.domain.NoteImage image=original==null?encoded:new app.space.notes.domain.NoteImage(original.id,encoded.name,encoded.mime,encoded.width,encoded.height,encoded.data);main.post(()->{if(closed)return;addImage(id,image,0,()->deleteSavedDrawingDraft(draft,sourceDigest),()->pad.saving(false),original==null?null:original.data);});}catch(Exception | OutOfMemoryError failure){main.post(()->{pad.saving(false);if(!closed)host.showMessage("Drawing was not attached; its draft is retained. "+failure.getMessage());});}finally{bitmap.recycle();}});
    }
    private void controlAvailable(View control,boolean enabled){control.setEnabled(enabled);control.setAlpha(enabled?1f:.35f);}
    private void inkSwatch(Button control,int color){GradientDrawable swatch=new GradientDrawable();swatch.setShape(GradientDrawable.OVAL);swatch.setColor(color);swatch.setStroke(dp(1),ink);swatch.setBounds(0,0,dp(24),dp(24));control.setCompoundDrawables(null,swatch,null,null);}
    private void renderDrawing(String id){renderDrawing(id,null);}
    private void renderDrawing(String id,String imageId){persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;final app.space.notes.domain.NoteImage original=note.images.stream().filter(value->value.id.equals(imageId)).findFirst().orElse(null);if(imageId!=null && original==null){host.showMessage("This drawing is no longer available.");renderEditor();return;}clearPage();drawingNote=id;drawingImage=imageId;editing=id;title=null;body=null;search=null;storagePage=true;querySequence++;header("Drawing",this::leaveDrawing);DrawingPad pad=new DrawingPad(context);drawingPad=pad;
        root.setPadding(dp(8),0,dp(8),dp(4));LinearLayout top=(LinearLayout)root.getChildAt(0);Button save=icon("Save drawing","save",()->saveDrawing(pad,id));save.setEnabled(false);save.setBackground(design.choice(true));NotesIcon saveGlyph=new NotesIcon("save",design.accent);saveGlyph.setBounds(0,0,dp(24),dp(24));save.setCompoundDrawables(null,saveGlyph,null,null);top.addView(save,new LinearLayout.LayoutParams(dp(48),dp(48)));Button more=icon("Drawing options","more",()->{});top.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout options=menu("Drawing options",more);Button grid=button("Grid lines",()->{pad.grid(!pad.grid());context.getSharedPreferences("notes-drawing",0).edit().putBoolean("grid",pad.grid()).apply();});options.addView(grid);pad.grid(context.getSharedPreferences("notes-drawing",0).getBoolean("grid",false));grid.setSelected(pad.grid());grid.setOnClickListener(v->act("Grid lines",()->{pad.grid(!pad.grid());grid.setSelected(pad.grid());sheets.close();grid.setText(pad.grid()?"Grid lines · On":"Grid lines · Off");context.getSharedPreferences("notes-drawing",0).edit().putBoolean("grid",pad.grid()).apply();}));grid.setText(pad.grid()?"Grid lines · On":"Grid lines · Off");
        menuRows(options,new String[]{"Copy drawing as image","Clear canvas","Delete drawing","Drawing help"},new Runnable[]{()->copyDrawing(pad),()->confirmSheet("Clear canvas?","Clear all marks on this canvas. Undo can restore them while this drawing editor stays open.","Clear canvas",pad::clear),()->deleteDrawing(id,imageId,pad),this::drawingHelp});
        root.addView(pad,new LinearLayout.LayoutParams(-1,0,1));
        android.widget.HorizontalScrollView toolScroll=new android.widget.HorizontalScrollView(context);toolScroll.setHorizontalScrollBarEnabled(false);LinearLayout toolRow=new LinearLayout(context);toolRow.setGravity(android.view.Gravity.CENTER_VERTICAL);toolRow.setPadding(0,dp(4),0,dp(4));toolScroll.addView(toolRow,new android.widget.HorizontalScrollView.LayoutParams(-2,-2));root.addView(toolScroll,new LinearLayout.LayoutParams(-1,-2));
        String[] symbols={"edit","pencil","highlighter","eraser","line","rectangle","oval"};List<Button> toolChoices=new ArrayList<>();Runnable selection=()->{for(int i=0;i<toolChoices.size();i++){Button choice=toolChoices.get(i);boolean active=i==pad.tool();choice.setSelected(active);choice.setBackground(design.choice(active));choice.setTextColor(active?design.accent:ink);}};
        for(int i=0;i<DrawingPad.TOOLS.length;i++){final int index=i;Button choice=button(DrawingPad.TOOLS[i],()->{pad.tool(index);selection.run();});choice.setSingleLine(true);choice.setMinHeight(dp(48));choice.setPadding(dp(12),dp(6),dp(12),dp(6));NotesIcon glyph=new NotesIcon(symbols[i],design.accent);glyph.setBounds(0,0,dp(20),dp(20));choice.setCompoundDrawablesRelative(glyph,null,null,null);choice.setCompoundDrawablePadding(dp(6));toolChoices.add(choice);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMargins(dp(2),0,dp(2),0);toolRow.addView(choice,lp);}selection.run();
        LinearLayout dock=tools(root);dock.setGravity(android.view.Gravity.CENTER);dock.setBackground(design.shape(surface,18));Button inkChoice=icon("Ink colour","palette",()->{});inkSwatch(inkChoice,pad.color());inkChoice.setOnClickListener(v->act("Ink colour",()->{String[] names={"Black","Blue","Red","Green","Violet","Orange","White"};int[] values={Color.BLACK,0xff1555aa,0xffb3261e,0xff167352,0xff7042a0,0xffc06419,Color.WHITE};int selected=-1;for(int i=0;i<values.length;i++)if(values[i]==pad.color())selected=i;choiceSheet("Ink colour",names,selected,index->{pad.color(values[index]);inkSwatch(inkChoice,pad.color());selection.run();});}));dock.addView(inkChoice,new LinearLayout.LayoutParams(0,dp(48),1));
        dock.addView(icon("Stroke width","stroke",()->choiceSheet("Stroke width",new String[]{"Fine · 2","Regular · 4","Medium · 8","Bold · 16","Broad · 32"},pad.width()<=2?0:pad.width()<=4?1:pad.width()<=8?2:pad.width()<=16?3:4,index->pad.width(new float[]{2,4,8,16,32}[index]))),new LinearLayout.LayoutParams(0,dp(48),1));Button undo=icon("Undo","undo",pad::undo),redo=icon("Redo","redo",pad::redo);dock.addView(undo,new LinearLayout.LayoutParams(0,dp(48),1));dock.addView(redo,new LinearLayout.LayoutParams(0,dp(48),1));dock.addView(icon("Fit paper","fit",pad::fit),new LinearLayout.LayoutParams(0,dp(48),1));pad.onStateChanged(()->{controlAvailable(undo,pad.canUndo());controlAvailable(redo,pad.canRedo());controlAvailable(save,pad.ready() && !pad.saving());});
        disk.execute(()->{android.graphics.Bitmap base=null;try{java.io.File file=drawingDraft(id,imageId);if(file.exists() || new java.io.File(file.getPath()+".bak").exists()){android.util.AtomicFile draft=new android.util.AtomicFile(file);android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(java.io.InputStream in=draft.openRead()){android.graphics.BitmapFactory.decodeStream(in,null,bounds);}if(bounds.outWidth!=1024 || bounds.outHeight<128 || bounds.outHeight>4096)throw new java.io.IOException("Invalid drawing draft dimensions");try(java.io.InputStream in=draft.openRead()){base=android.graphics.BitmapFactory.decodeStream(in);}if(base==null)throw new java.io.IOException("Drawing draft could not be decoded");}else if(original!=null){base=NoteImageFiles.decode(original);if(base==null)throw new java.io.IOException("Saved drawing could not be decoded");}}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed && drawingPad==pad)host.showMessage("Draft preview unavailable. The draft file was kept.");});return;}final android.graphics.Bitmap value=base;main.post(()->{if(closed || drawingPad!=pad){if(value!=null)value.recycle();return;}pad.loaded(value);save.setEnabled(true);});});
    }
    private void imageEditingHelp(){LinearLayout panel=sheets.panel("Edit image",null);TextView help=text("Crop: drag a highlighted corner, or choose Crop presets without dragging. Rotate and Mirror affect the preview. Undo and Redo restore preview steps. Save changes this attachment only. Back lets you discard edits. Everything stays on this device.",15);help.setPadding(dp(16),dp(8),dp(16),dp(16));panel.addView(help);panel.addView(button("Done",()->sheets.close()));sheets.show(panel,"Edit image");}
    private void leaveImageEditor(){pageBack=this::leaveImageEditor;ImageEditView view=imageEditor;if(view!=null && view.saving()){host.showMessage("Saving image. Please wait.");return;}if(view!=null && view.dirty())confirmSheet("Discard image edits?","The saved image has not changed.","Discard edits",this::renderEditor);else renderEditor();}
    private Button imageTool(LinearLayout row,String label,String glyph,Runnable action){Button control=button(label,action);control.setTextSize(12);control.setMinHeight(dp(64));control.setMinWidth(dp(72));control.setPadding(dp(10),dp(6),dp(10),dp(6));NotesIcon symbol=new NotesIcon(glyph,design.accent);symbol.setBounds(0,0,dp(24),dp(24));control.setCompoundDrawables(null,symbol,null,null);row.addView(control,new LinearLayout.LayoutParams(-2,-2));return control;}
    private void renderImageEditor(String id,String imageId,app.space.notes.domain.ImageEditPlan restoredPlan){
        persistDraft();Note note=find(id);if(note==null || note.deletedAt>0)return;final app.space.notes.domain.NoteImage original=note.images.stream().filter(value->value.id.equals(imageId)).findFirst().orElse(null);if(original==null){host.showMessage("This image is no longer available.");renderEditor();return;}
        ((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);clearPage();editing=id;imageEditNote=id;imageEditItem=imageId;imageEditVersion=note.updatedAt;title=null;body=null;search=null;storagePage=true;querySequence++;header("Edit image",this::leaveImageEditor);pageBack=this::leaveImageEditor;
        root.setPadding(dp(8),0,dp(8),dp(4));ImageEditView view=new ImageEditView(context);imageEditor=view;
        LinearLayout top=(LinearLayout)root.getChildAt(0);Button save=icon("Save image edits","save",()->saveImageEdit(id,original,view));save.setBackground(design.choice(true));top.addView(save,new LinearLayout.LayoutParams(dp(48),dp(48)));Button more=icon("Image options","more",()->{});top.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout options=menu("Image options",more);
        menuRows(options,new String[]{"Crop presets","Reset all edits","Image editing help"},new Runnable[]{()->{if(!view.ready() || view.saving())return;choiceSheet("Crop presets",new String[]{"Full image","Square","4:3","16:9"},-1,index->{if(!view.ready() || view.saving())return;view.edit(index==0?view.plan().fullCrop():view.plan().aspect(index==1?1:index==2?4:16,index==1?1:index==2?3:9));view.cropMode(true);});},()->{if(view.ready() && !view.saving())view.edit(new app.space.notes.domain.ImageEditPlan(original.width,original.height));},this::imageEditingHelp});
        TextView progress=text("Opening image…",14);root.addView(progress,new LinearLayout.LayoutParams(-1,-2));root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        android.widget.HorizontalScrollView scroll=new android.widget.HorizontalScrollView(context);scroll.setHorizontalScrollBarEnabled(true);LinearLayout row=new LinearLayout(context);row.setGravity(android.view.Gravity.CENTER_VERTICAL);scroll.addView(row,new android.widget.HorizontalScrollView.LayoutParams(-2,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        Button crop=imageTool(row,"Crop","crop",()->view.cropMode(!view.cropMode()));Button left=imageTool(row,"Rotate left","rotate-left",()->view.edit(view.plan().rotateLeft()));Button right=imageTool(row,"Rotate right","rotate-right",()->view.edit(view.plan().rotateRight()));Button mirror=imageTool(row,"Mirror","mirror",()->view.edit(view.plan().mirror()));Button undo=imageTool(row,"Undo","undo",view::undo),redo=imageTool(row,"Redo","redo",view::redo);
        view.onStateChanged(()->{boolean available=view.ready() && !view.saving();for(Button control:new Button[]{crop,left,right,mirror,more})controlAvailable(control,available);controlAvailable(save,available && view.dirty());controlAvailable(undo,view.canUndo());controlAvailable(redo,view.canRedo());crop.setSelected(view.cropMode());crop.setBackground(design.choice(view.cropMode()));progress.setText(view.saving()?"Saving image…":view.ready()?view.plan().cropWidth()+" × "+view.plan().cropHeight()+" pixels":"Opening image…");});
        disk.execute(()->{android.graphics.Bitmap decoded=null;try{decoded=NoteImageFiles.decode(original);if(decoded==null)throw new java.io.IOException("Image could not be decoded");final android.graphics.Bitmap value=decoded;byte[] digestBytes=java.security.MessageDigest.getInstance("SHA-256").digest(original.data.getBytes(java.nio.charset.StandardCharsets.US_ASCII));final String digest=java.util.Base64.getEncoder().encodeToString(digestBytes);final boolean resume=restoredPlan!=null && restored!=null && digest.equals(restored.getString("imageEditDigest"));main.post(()->{if(closed || imageEditor!=view){value.recycle();return;}imageEditDigest=digest;view.loaded(value,resume?restoredPlan:null);if(resume){view.restoreHistory(restored.getIntArray("imageEditUndo"),restored.getIntArray("imageEditRedo"));view.cropMode(restored.getBoolean("imageEditCrop",false));}else if(restoredPlan!=null)host.showMessage("The image changed while this editor was closed. Latest saved image shown; earlier preview edits were not applied.");});}catch(Exception | OutOfMemoryError failure){if(decoded!=null)decoded.recycle();main.post(()->{if(!closed && imageEditor==view){progress.setText("Image could not be opened. The saved image is unchanged.");host.showMessage("Image preview unavailable. Go back and retry; the original was kept.");}});}});
    }
    private void saveImageEdit(String id,app.space.notes.domain.NoteImage original,ImageEditView view){
        if(imageEditor!=view || !view.ready() || view.saving() || !view.dirty())return;
        final app.space.notes.domain.ImageEditPlan plan=view.plan();view.saving(true);
        disk.execute(()->{android.graphics.Bitmap decoded=null,result=null;try{decoded=NoteImageFiles.decode(original);if(decoded==null)throw new java.io.IOException("Original image could not be opened");result=ImageEditView.render(decoded,plan);app.space.notes.domain.NoteImage encoded=NoteImageFiles.encode(result,original.name);final app.space.notes.domain.NoteImage replacement=new app.space.notes.domain.NoteImage(original.id,original.name,encoded.mime,encoded.width,encoded.height,encoded.data);main.post(()->{if(closed || imageEditor!=view)return;addImage(id,replacement,0,()->{},()->{if(imageEditor==view)view.saving(false);},original.data);});}catch(Exception | OutOfMemoryError failure){main.post(()->{if(!closed && imageEditor==view){view.saving(false);host.showMessage("Image edits were not saved. The original is unchanged; retry or go back.");}});}finally{if(result!=null)result.recycle();if(decoded!=null)decoded.recycle();}});
    }
    public void attachImage(String id,android.net.Uri uri){whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0){host.showMessage("Original note is unavailable; no image was imported.");return;}disk.execute(()->{try{app.space.notes.domain.NoteImage image=NoteImageFiles.importImage(context,uri);main.post(()->{if(!closed)addImage(id,image);});}catch(Exception | OutOfMemoryError failure){Diagnostics.failure("image.import.failure",failure);main.post(()->{if(!closed)host.showMessage("Image was not added: "+failure.getMessage()+". Your note is unchanged.");});}});});}
    private void addImage(String id,app.space.notes.domain.NoteImage image){addImage(id,image,0,()->{},()->{});}
    private void addImage(String id,app.space.notes.domain.NoteImage image,int attempt,Runnable accepted,Runnable rejected){addImage(id,image,attempt,accepted,rejected,null);}
    private void addImage(String id,app.space.notes.domain.NoteImage image,int attempt,Runnable accepted,Runnable rejected,String originalData){if(busy){rejected.run();host.showMessage("Storage is busy. Your visual edit was not applied; retry when ready.");return;}whenReady(()->{persistDraft();Note note=find(id);if(note==null || note.deletedAt>0){rejected.run();host.showMessage("Original note is unavailable. The image was not saved.");return;}try{List<app.space.notes.domain.NoteImage> images=new ArrayList<>(note.images);int index=-1;for(int i=0;i<images.size();i++)if(images.get(i).id.equals(image.id))index=i;if(originalData==null){if(index>=0){rejected.run();return;}images.add(image);}else{if(index<0 || !images.get(index).data.equals(originalData)){rejected.run();host.showMessage("This visual item changed elsewhere. Your edit was not applied.");return;}images.set(index,image);}final long candidateRevision=revision;Note changed=note.images(images,System.currentTimeMillis());if(originalData==null && changed.content!=null && id.equals(pendingContentNote) && pendingContentAnchor!=null){try{app.space.notes.domain.DocumentContent.Block added=null;for(app.space.notes.domain.DocumentContent.Block block:changed.content.blocks)if(block.image!=null && block.image.id.equals(image.id))added=block;if(added!=null){int edge=changed.content.blocks.indexOf(changed.content.block(pendingContentAnchor))+1;app.space.notes.domain.DocumentContent ordered=changed.content;app.space.notes.domain.DocumentContent.Block anchor=ordered.block(pendingContentAnchor);
            if(pendingContentOffset>=0 && anchor.text!=null && Objects.equals(pendingContentDigest,contentDigest(anchor.text)))ordered=ordered.remove(added.id).insertAtText(anchor.id,pendingContentOffset,added);else ordered=ordered.move(added.id,edge);changed=changed.withContent(ordered,System.currentTimeMillis());}}catch(IllegalArgumentException missing){host.showMessage("Insertion point changed; visual kept at the end. Drag it to the desired position.");}}final Note imageCandidate=changed;List<Note> candidate=new ArrayList<>(notes);candidate.set(candidate.indexOf(note),changed);
        disk.execute(()->{try{BackupCodec.encode(candidate);main.post(()->{if(closed)return;persistDraft();if(revision!=candidateRevision || find(id)!=note){if(attempt<3)addImage(id,image,attempt+1,accepted,rejected,originalData);else{rejected.run();host.showMessage("The library changed while saving the image. Your edit was not applied; try again.");}return;}captureUndo(Arrays.asList(note),true);recordCanonicalChange(note,imageCandidate);replace(imageCandidate);pendingContentAnchor=null;pendingContentNote=null;pendingContentDigest=null;pendingContentOffset=-1;queueSave(accepted);if(id.equals(editing))renderEditor();else host.showMessage((image.name.equals("Drawing")?"Drawing":"Image")+" added to its original note.");});}catch(Exception | OutOfMemoryError failure){main.post(()->{rejected.run();if(!closed)host.showMessage("Image not saved: the complete library must still fit a verified backup. Your notes are unchanged; your edit remains open.");});}});
    }catch(IllegalArgumentException failure){rejected.run();host.showMessage(failure.getMessage());}});}
    private void renderBackgrounds(){
        List<Note> chosen=propertyTargets();if(chosen.isEmpty())return;title=null;body=null;search=null;querySequence++;clearPage();header("Background",this::returnToNoteOrHome);
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<NoteBackground.NAMES.length;i++){final int value=i;Button option=button(NoteBackground.NAMES[i],()->{captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.background(value,System.currentTimeMillis()));}queueSave();returnToNoteOrHome();});option.setBackground(new NoteBackground(i,surface,darkTheme,context.getResources().getDisplayMetrics().density));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(72));lp.setMargins(0,dp(5),0,dp(5));content.addView(option,lp);}
    }
    private void renderFolders(boolean moving){renderFolders(moving,moving?this::returnToNoteOrHome:this::renderHome);}
    private void renderFolders(boolean moving,Runnable parent){
        persistDraft();List<Note> chosen=moving?propertyTargets():Collections.emptyList();if(moving && chosen.isEmpty())return;title=null;body=null;search=null;querySequence++;storagePage=true;clearPage();header(moving?"Move to folder":"Folders",parent);final long token=folderPageSequence;
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(text("Reading your main storage folder…",14));
        final List<Note> folderSnapshot=new ArrayList<>(notes);
        disk.execute(()->{try{List<String> folders=repository.listFolders(folderSnapshot);main.post(()->{if(closed || token!=folderPageSequence)return;content.removeAllViews();content.addView(text(repository.appOnlyStorage()?"Private on this device.":"Saved in your selected local folder.",16));content.addView(text(moving?"Choose where to put the selected notes. Nothing is uploaded.":"Folders organise notes on this device. Tap a folder to see its notes. Main folder means notes not placed in a subfolder.",14));
            if(!moving)content.addView(button("All notes",()->{activeFolder=null;renderHome();}));
            java.util.function.Consumer<String> choose=folder->{if(moving){captureUndo(chosen,false);for(Note old:chosen){Note current=find(old.id);if(current!=null)replace(current.folder(folder,System.currentTimeMillis()));}queueSave();activeFolder=null;returnToNoteOrHome();}else{activeFolder=folder;page="Notes";selectedIds.clear();editing=null;renderHome();}};
            content.addView(button("Main folder",()->choose.accept("")));for(String folder:folders){Button option=button(folder,()->choose.accept(folder));NotesIcon icon=new NotesIcon("folder",ink);icon.setBounds(0,0,dp(24),dp(24));option.setCompoundDrawablesRelative(icon,null,null,null);option.setCompoundDrawablePadding(dp(12));content.addView(option,new LinearLayout.LayoutParams(-1,-2));}
            content.addView(button("Create folder",()->{EditText input=new EditText(context);design.entry(input,16);input.setHint("Folder name");android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(context).setTitle("Create folder").setView(input).setPositiveButton("Create",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String name;try{name=app.space.notes.domain.FolderNames.require(input.getText().toString());}catch(IllegalArgumentException invalid){input.setError(invalid.getMessage());return;}if(busy)return;busy=true;dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);disk.execute(()->{String error=null;try{repository.createFolder(name,folderSnapshot);}catch(Exception failure){Diagnostics.failure("folders.create.failure",failure);error=failure.getMessage();}final String problem=error;main.post(()->{busy=false;if(closed)return;if(problem==null){dialog.dismiss();if(token==folderPageSequence)renderFolders(moving,parent);}else{dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);input.setError(problem);}});});}));design.show(dialog);}));
            if(!moving){content.addView(text("Want a separate copy? Export a local backup to Internal storage or SD card.",14));content.addView(button("Backups",()->{renderBackups();pageBack=()->renderFolders(false,parent);}));content.addView(button("Storage settings",this::renderStorage));}
        });}catch(Exception failure){main.post(()->{if(closed || token!=folderPageSequence)return;Diagnostics.failure("folders.open.failure",failure);content.removeAllViews();content.addView(text(failure.getMessage()==null?"Main storage folder is unavailable":failure.getMessage(),15));Button chooseRoot=button("Choose main folder",()->{afterFolderSelection=()->renderFolders(moving,parent);host.chooseLocalFolder();});design.primary(chooseRoot);content.addView(chooseRoot);content.addView(button("Storage settings",()->{renderStorage();pageBack=()->renderFolders(moving,parent);}));});}});
    }
    private void renderModelInfo(){
        voicePage=false;persistDraft();editing=null;title=null;body=null;search=null;storagePage=true;clearPage();header("About & offline models",this::renderSettings);
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content.addView(design.heading("MyNotes"));content.addView(text(host.versionLabel(),14));content.addView(text("Your notes stay with you. No account, no ads, no Internet permission.",16));settingRow(content,"Voice recordings","Original audio stays in Voice inbox. Export it separately from note backups.",()->renderVoiceInbox(this::renderModelInfo));LinearLayout technical=section(content,"Offline models & licences");TextView details=text("Reading bundled notices…",14);technical.addView(details);
        disk.execute(()->{StringBuilder value=new StringBuilder("Original recordings stay privately on this device, independent of note Trash. JSON backups exclude audio. Export important recordings before clearing data or uninstalling. Transcripts use conservative pause-based commas and sentence breaks, not a semantic punctuation model. Questions and exclamations are not guessed. Review punctuation against audio; raw recognized text is retained with the recording.\n\nAll inference runs locally on CPU. No network permission, platform SpeechRecognizer or model downloader. OCR: Tesseract4Android 4.9.0, tessdata_fast 4.1.0 English/Hindi/Kannada. Speech: Vosk 0.3.75 with bundled English small-en-us-0.15. Accuracy varies: review recognized text against the original. First use installs verified models in private storage; speech needs additional disk/RAM. Microphone is optional and only requested on Record voice. Voice inbox originals are excluded from JSON backup and never automatically deleted.\n\n");try{for(String name:context.getAssets().list("licenses")){value.append("\n\n===== ").append(name).append(" =====\n");try(java.io.InputStream in=context.getAssets().open("licenses/"+name)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);value.append(bytes.toString("UTF-8"));}}}catch(Exception e){value.append("Notices could not be read: ").append(e.getMessage());}main.post(()->{if(!closed && details.getParent()!=null)details.setText(value);});});
    }
    private void showDocumentLocation(){persistDraft();Note note=find(editing);if(note==null)return;LinearLayout panel=sheets.panel("File location",null);TextView location=text("Checking saved location…",15);location.setTag("saved-document-location");location.setTextIsSelectable(true);location.setPadding(dp(16),dp(8),dp(16),dp(16));panel.addView(location);panel.addView(button("Done",()->sheets.close()));sheets.show(panel,"File location");disk.execute(()->{String value;try{value=repository.documentLocation(note);}catch(Exception failure){value="Saved location is unavailable. Current private notes were not changed.";}final String result=value;main.post(()->{if(!closed && location.getParent()!=null){location.setText(result);sheets.refresh();}});});}
    private void renderStorage(){
        afterFolderSelection=null;        storagePage=true;prepared=null;editing=null;search=null;title=null;body=null;querySequence++;main.removeCallbacks(filter);voicePage=false;clearPage();header("Storage & recovery",this::renderSettings);
        ScrollView scroll=new ScrollView(context);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView current=text(repository==null?"Opening storage":repository.status(),16);current.setTextColor(design.accent);content.addView(current);content.addView(text("Everything stays local. Folders organise your notes; backups are separate copies you choose to export. There is no cloud sync.",16));
        settingRow(content,"Folders","Organise notes on this device. Creating a folder does not move your storage location.",()->renderFolders(false,this::renderStorage));
        settingRow(content,"Backups","Export or restore a verified local copy in Internal storage or SD card. Original recordings are separate.",this::renderBackups);
        content.addView(text("Edit history stays privately on this device: up to50 undo/redo steps per note,64 recently edited notes,16 MiB on disk. Older history may be trimmed; current notes are never trimmed. History is not part of exported backups. After a crash, only history matching the saved note is restored.",14));
        settingRow(content,"Reset edit history","Use only if history cannot be verified. Deletes history, never your current notes.",()->confirmSheet("Reset edit history?","Current notes, attachments and backups are kept. Saved Undo/Redo steps will be removed.","Reset history",()->{busy=true;disk.execute(()->{historyStore.reset();main.post(()->{busy=false;savedHistories.clear();canonicalHistory=null;canonicalHistoryNote=null;undoBatch.clear();undo=null;undoCreatedId=null;noteUndoIsLatest=false;historyNoticeShown=false;if(!closed){renderStorage();host.showMessage("Edit history reset. Current notes were kept.");}});});}));
        content.addView(text("Advanced: storage location and recovery",16));content.addView(text("Most people can keep private device storage. Changing the main folder is optional, not required to organise notes or make a backup.",14));
        settingRow(content,"Open note file","Choose a local .mynote.json document. Review before adding it; the original file stays unchanged.",host::chooseNoteDocument);
        settingRow(content,"Choose folder","Storage location: copy and verify your current library into a local main folder.",host::chooseLocalFolder);
        settingRow(content,"Open library","Open notes from a MyNotes library folder. Review the merge before changing your storage location.",host::openExistingFolder);
        if(repository!=null && repository.status().toLowerCase(java.util.Locale.ROOT).contains("pending"))settingRow(content,"Retry save","Reconnect your local folder first; private edits are retained.",this::flush);
        settingRow(content,"Use device storage","App-only storage: private to MyNotes, not visible in Files.",()->design.show(new android.app.AlertDialog.Builder(context).setTitle("Use app-only storage?").setMessage("This disconnects the visible main folder. Existing files stay where they are. Your notes and folder organisation remain available privately on this device.").setPositiveButton("Use app-only storage",(dialog,index)->moveStorage(null)).setNegativeButton("Cancel",null).create()));

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
            main.post(()->{busy=false;if(!closed){Runnable next=afterFolderSelection;afterFolderSelection=null;if(next!=null)next.run();else renderStorage();host.showMessage(result);}});
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
        content.addView(text("A local note backup includes text, checklists, diagrams, images, drawings, note formatting and folder placement. Saved named presets, edit history, empty folders and original recordings are not included.",16));
        settingRow(content,"Export backup","Choose a local file. Success is reported only after complete readback verification.",host::exportBackup);
        settingRow(content,"Import backup","Choose a backup or exported .mynote.json file. Review a merge before anything is saved.",host::importBackup);
        settingRow(content,"Before document editor","Review the private library snapshot kept before the first document-editor upgrade.",()->prepareDocumentCheckpoint());
        settingRow(content,"Previous snapshot","Recover the checkpoint from before the latest restore or open-library operation.",()->prepareImport(null,false,true));
        content.addView(text("Backups are readable JSON, not encrypted. Original recordings are separate — export them from Voice inbox.",14));
        LinearLayout details=section(content,"Backup details");details.addView(text("Use the local Internal storage/SD-card provider. The current portable backup limit is 8 MiB, checked on export and import. Images are copied into notes at a bounded resolution; an addition that would break a complete verified backup is refused. Larger libraries need a future streaming format.",14));
        details.addView(text("Backup envelopes verify a checksum. Visible per-note files are schema-validated (not encrypted or signed). Import previews a merge: identical notes are skipped; a changed note with the same ID becomes a separate copy. Re-importing the same conflict copy does not duplicate it. The previous snapshot is merged too, never used to delete newer work. Keep the source and old installation until the restored result is verified.",14));
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
    public void openNoteDocument(android.net.Uri uri,Runnable release){DocumentGrant grant=new DocumentGrant(release);documentGrants.add(grant);if(closed || (!ready && deferredResults.size()>=8)){grant.run();if(!closed)host.showMessage("Library is opening. Retry Open note file in a moment.");return;}whenReady(()->{if(busy || prepared!=null || drawingPad!=null || imageEditor!=null || diagramOpen || ocrPanel || capture!=null){grant.run();host.showMessage("Save or leave the current visual, recording or file review before opening another document. Your current edits were kept.");return;}prepareImport(uri,false,false,grant);});}
    public void importBackup(android.net.Uri uri) {whenReady(()->prepareImport(uri,false,false));}
    public void openExistingFolder(android.net.Uri uri) {whenReady(()->prepareImport(uri,true,false));}
    private void prepareImport(android.net.Uri source,boolean folder,boolean previous) {prepareImport(source,folder,previous,()->{});}
    private void prepareImport(android.net.Uri source,boolean folder,boolean previous,Runnable release) {
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
            } finally {main.post(release);}
        });
    }
    private void prepareDocumentCheckpoint(){persistDraft();if(busy)return;busy=true;final List<Note> base=new ArrayList<>(notes);disk.execute(()->{try{List<Note> incoming=repository.documentUpgradeSnapshot();PreparedImport plan=new PreparedImport(base,NoteMerge.preview(base,incoming),null,null);main.post(()->{busy=false;if(!closed){prepared=plan;renderPreview();}});}catch(Exception failure){main.post(()->{busy=false;if(!closed)host.showMessage("No readable document-upgrade checkpoint: "+failure.getMessage());});}});}
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
    private void replace(Note n) { for(int i=0;i<notes.size();i++)if(notes.get(i).id.equals(n.id)){Note before=notes.get(i);if(!historyStepping && before!=n)historyFor(n.id).record(before,n,before.content==null && dirty?"legacy:"+n.id:null,android.os.SystemClock.uptimeMillis());trimSavedHistories();notes.set(i,n);return;} }
    private void persistDraft() {
        if(canonicalEditor!=null)canonicalEditor.saveState();
        Note canonical=find(editing);if(canonical!=null)documentsWorkspace=canonical.content!=null;if(canonical!=null && canonical.content!=null){main.removeCallbacks(saveDraft);if(dirty){dirty=false;queueSave();}return;}
        main.removeCallbacks(saveDraft);
        if(body!=null && editing!=null){caretNote=editing;caretAnchor=Math.max(0,body.getSelectionStart());caretFocus=Math.max(0,body.getSelectionEnd());}
        if(!ready || editing==null || title==null)return;
        Note n=find(editing);if(n==null || n.deletedAt>0)return;
        Note candidate=n.checklist?n.list(title.getText().toString(),n.items,n.hideChecked,n.updatedAt):
            n.rich(title.getText().toString(),body.getText().toString(),TextStyles.read(body.getText()),n.updatedAt);
        boolean changed=!NoteMerge.same(n,candidate);
        if(changed && body!=null)historyFor(n.id).rememberCursor(n,new app.space.notes.domain.DocumentHistory.Cursor("legacy:body",Math.max(0,body.getSelectionStart()),Math.max(0,body.getSelectionEnd())));
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
        if(!noteUndoIsLatest && stepTextHistory(false))return;
        persistDraft();
        if(undoBatch.isEmpty()){if(!stepSavedHistory(false))host.showMessage("Nothing to undo");return;}
        for(Note old:undoBatch){Note current=find(old.id);if(current!=null){Note target=undoFull?old.state(old.pinned,old.archived,old.deletedAt,System.currentTimeMillis()):current.state(old.pinned,old.archived,old.deletedAt,System.currentTimeMillis()).properties(old.color,old.labels,System.currentTimeMillis()).folder(old.folder,System.currentTimeMillis()).background(old.background,System.currentTimeMillis());historyFor(old.id).align(current,target,false);historyStepping=true;try{replace(target);}finally{historyStepping=false;}}}
        if(undoCreatedId!=null){String created=undoCreatedId;notes.removeIf(n->n.id.equals(created));if(created.equals(editing))editing=null;undoCreatedId=null;}
        undoBatch.clear();undoFull=false;queueSave();if(editing!=null)renderEditor();else renderHome();
    }
    private void queueSave(){queueSave(()->{});}
    private void queueSave(Runnable persisted) {
        if(historyStepping)return;
        savedHistories.keySet().removeIf(id->find(id)==null);final List<Note> snapshot=new ArrayList<>(notes);final List<PersistentHistoryStore.Entry> historySnapshot=new ArrayList<>();for(Map.Entry<String,app.space.notes.domain.DocumentHistory> entry:savedHistories.entrySet()){Note n=find(entry.getKey());if(n!=null)historySnapshot.add(new PersistentHistoryStore.Entry(n,entry.getValue().snapshot()));} final long ticket=++revision;
        if(status!=null){status.setText("Saving locally…");if(canonicalEditor!=null)status.setVisibility(View.VISIBLE);}
        disk.execute(()->{
            // Newer immutable snapshots include earlier edits. Avoid a backlog of full media saves.
            if(ticket!=revision){log("save.coalesced","revision="+ticket);return;}
            try { repository.save(snapshot);if(historyStore!=null)try{historyStore.save(historySnapshot);}catch(Exception | OutOfMemoryError historyFailure){main.post(()->{if(!closed && !historyNoticeShown){historyNoticeShown=true;host.showMessage("Notes saved. Edit history could not be saved or verified; use Storage & recovery to review/reset history. Do not clear app data.");}});}markVoiceNotesCreated(snapshot);persisted.run(); log("save","ok revision="+ticket+" count="+snapshot.size());
                main.post(()->{persistedRevision=Math.max(persistedRevision,ticket);if(!closed && !dirty && ticket==revision && status!=null){status.setText("Saved · "+repository.status());status.setVisibility(canonicalEditor==null?View.GONE:View.VISIBLE);}});
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
        if(editing==null && activeFolder!=null){activeFolder=null;renderFolders(false);return true;}
        if(editing==null)return false;
        persistDraft(); renderHome(); return true;
    }
    public void flush() { Diagnostics.record("lifecycle.flush","requested"); if(ready && !busy){persistDraft();queueSave();} }
    public void saveState(Bundle state) {
        if(imageEditor!=null){imageEditor.settleGesture();state.putString("imageEditNote",imageEditNote);state.putString("imageEditItem",imageEditItem);state.putLong("imageEditVersion",imageEditVersion);state.putString("imageEditDigest",imageEditDigest);state.putIntArray("imageEditUndo",imageEditor.historyState(false));state.putIntArray("imageEditRedo",imageEditor.historyState(true));state.putBoolean("imageEditCrop",imageEditor.cropMode());if(imageEditor.plan()!=null)state.putIntArray("imageEditPlan",imageEditor.plan().state());}
        if(canonicalEditor!=null)canonicalEditor.saveState();state.putBundle("canonicalEditor",canonicalState);state.putString("canonicalDiagram",canonicalDiagram);state.putString("pendingContentAnchor",pendingContentAnchor);state.putString("pendingContentNote",pendingContentNote);state.putInt("pendingContentOffset",pendingContentOffset);state.putString("pendingContentDigest",pendingContentDigest);stashDrawing();state.putString("auxiliaryPage",auxiliaryPage);state.putString("drawingNote",drawingNote);state.putString("drawingImage",drawingImage);persistDraft();state.putBoolean("filterAll",libraryFilter.allNotes);state.putBoolean("filterUnlabelled",libraryFilter.includeUnlabelled);state.putStringArrayList("filterLabels",new ArrayList<>(libraryFilter.labels));state.putString("homeQuery",homeQuery);state.putString("activeFolder",activeFolder);state.putBoolean("documentsWorkspace",documentsWorkspace);state.putString("page",page);state.putString("editing",editing);if(body!=null){state.putInt("bodyAnchor",body.getSelectionStart());state.putInt("bodyFocus",body.getSelectionEnd());state.putBoolean("bodyFocused",body.hasFocus());}else if(editing!=null && editing.equals(caretNote)){state.putInt("bodyAnchor",caretAnchor);state.putInt("bodyFocus",caretFocus);state.putBoolean("bodyFocused",false);}
        if(editing!=null && find(editing)!=null){try{byte[] draft=DocumentCodec.encode(Arrays.asList(find(editing)));if(draft.length<=32768)state.putByteArray("draftDocument",draft);}catch(Exception e){log("draft-snapshot","failed "+e.getClass().getSimpleName());Diagnostics.failure("draft-snapshot.failure",e);}}
    }
    public void close() { Diagnostics.record("lifecycle.close","controller");
        closed=true;for(DocumentGrant grant:new ArrayList<>(documentGrants))grant.run();disposePlayback();if(ocrCancellation!=null)ocrCancellation.cancel();if(capture!=null)capture.stop();stopPlayback();if(unsubscribeVoice!=null)unsubscribeVoice.run();deferredResults.clear(); main.removeCallbacksAndMessages(null); searchWorker.shutdown();
    }
}
