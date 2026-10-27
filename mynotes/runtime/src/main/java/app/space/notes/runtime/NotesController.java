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
    private ArrayAdapter<String> adapter;
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
                            Note n=find(id).edit(restored.getString("title",find(id).title),
                                restored.getString("body",find(id).body),System.currentTimeMillis());
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
    private void row(String[] labels,Runnable[] actions) {
        LinearLayout row=new LinearLayout(context);
        for(int i=0;i<labels.length;i++) {
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1); p.setMargins(dp(2),dp(4),dp(2),dp(4));
            row.addView(button(labels[i],actions[i]),p);
        }
        root.addView(row);
    }
    private void header(String name) {
        TextView h=text(name,28); h.setTypeface(null,Typeface.BOLD); root.addView(h);
        status=text((repository==null?"Opening storage":repository.status())+" · "+host.versionLabel(),12); status.setTextColor(muted); root.addView(status);
    }
    private void renderHome() {
        storagePage=false;prepared=null;editing=null; title=null; body=null; root.removeAllViews(); header("MyNotes · "+page);
        row(new String[]{"Notes","Archive","Trash","Storage"},new Runnable[]{()->navigate("Notes"),()->navigate("Archive"),()->navigate("Trash"),this::renderStorage});
        search=new EditText(context); search.setSingleLine(true); search.setTextColor(ink); search.setHintTextColor(muted);
        search.setHint("Search this page"); root.addView(search);
        search.addTextChangedListener(watcher(()->{ main.removeCallbacks(filter); main.postDelayed(filter,150); querySequence++; }));
        ListView list=new ListView(context); list.setDivider(null); list.setBackgroundColor(bg);
        adapter=new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,new ArrayList<>()) {
            @Override public View getView(int position,View old,ViewGroup parent) {
                TextView v=(TextView)super.getView(position,old,parent); v.setTextColor(ink);
                v.setTextSize(17); v.setPadding(dp(12),dp(16),dp(12),dp(16)); return v;
            }
        };
        list.setAdapter(adapter); TextView empty=text("No notes here. Create a note, or try another search.",15);
        root.addView(empty); list.setEmptyView(empty);
        root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        list.setOnItemClickListener((a,v,pos,id)->{ if(pos<visible.size()) { editing=visible.get(pos).id; renderEditor(); } });
        row(new String[]{"+ Text note","Undo","Retry save"},new Runnable[]{this::newNote,this::undo,this::flush});
        root.addView(text("Early text milestone · lists, media and diagrams are not available yet.",12));
        filterNow();
    }
    private void navigate(String name) { page=name; renderHome(); }
    private void newNote() {
        Note n=Note.create(System.currentTimeMillis()); notes.add(n); editing=n.id;
        queueSave(); renderEditor(); title.requestFocus();
    }
    private void filterNow() {
        if(closed || editing!=null || search==null)return;
        final String query=search.getText().toString(), currentPage=page;
        final long ticket=++querySequence;
        final List<Note> snapshot=new ArrayList<>(notes);
        searchWorker.execute(()->{
            List<Note> result=new ArrayList<>();
            for(Note n:snapshot) {
                boolean included=currentPage.equals("Trash")?n.deletedAt>0:
                    n.deletedAt==0 && (currentPage.equals("Archive")==n.archived);
                if(included && n.matches(query))result.add(n);
            }
            result.sort(Comparator.comparing((Note n)->!n.pinned)
                .thenComparing(Comparator.comparingLong((Note n)->n.updatedAt).reversed()));
            main.post(()->{
                if(closed || editing!=null || ticket!=querySequence)return;
                visible.clear(); visible.addAll(result); adapter.clear();
                for(Note n:result)adapter.add((n.pinned?"★  ":"")+(n.title.isEmpty()?"Untitled":n.title)+"\n"+
                    (n.body.length()>100?n.body.substring(0,100)+"…":n.body));
                adapter.notifyDataSetChanged();
            });
        });
    }
    private void renderEditor() {
        Note n=find(editing); if(n==null){renderHome();return;}
        main.removeCallbacks(filter); querySequence++; search=null;
        root.removeAllViews(); header(n.deletedAt>0?"Trash note":"Edit note");
        title=new EditText(context); title.setHint("Title"); title.setTextSize(24);
        title.setTextColor(ink); title.setHintTextColor(muted); title.setText(n.title); root.addView(title);
        body=new EditText(context); body.setHint("Write your note…"); body.setTextColor(ink); body.setHintTextColor(muted);
        body.setGravity(android.view.Gravity.TOP); body.setText(n.body);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        boolean trashed=n.deletedAt>0; title.setEnabled(!trashed); body.setEnabled(!trashed);
        TextWatcher change=watcher(()->{
            if(binding)return;
            dirty=true; status.setText("Unsaved changes…"); main.removeCallbacks(saveDraft); main.postDelayed(saveDraft,350);
        });
        title.addTextChangedListener(change); body.addTextChangedListener(change);
        if(trashed) row(new String[]{"Back","Restore"},new Runnable[]{()->back(),()->changeState("restore")});
        else row(new String[]{"Back",n.pinned?"Unpin":"Pin",n.archived?"Unarchive":"Archive","Trash"},
            new Runnable[]{()->back(),()->changeState("pin"),()->changeState("archive"),()->changeState("trash")});
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
        Note n=find(editing); if(n==null || n.deletedAt>0)return;
        String t=title.getText().toString(), b=body.getText().toString();
        if(n.title.equals(t) && n.body.equals(b))return;
        replace(n.edit(t,b,System.currentTimeMillis())); dirty=false; queueSave();
    }
    private void changeState(String action) {
        persistDraft(); Note n=find(editing); if(n==null)return; undo=n;
        long now=System.currentTimeMillis();
        Note changed=n.state(action.equals("pin")?!n.pinned:n.pinned,
            action.equals("archive")?!n.archived:n.archived,
            action.equals("trash")?now:action.equals("restore")?0:n.deletedAt,now);
        replace(changed); queueSave();
        if(action.equals("pin"))renderEditor(); else {renderHome();host.showMessage("Updated. Undo is available on Home.");}
    }
    private void undo() {
        if(undo==null){host.showMessage("Nothing to undo");return;}
        Note current=find(undo.id);
        if(current!=null) replace(current.state(undo.pinned,undo.archived,undo.deletedAt,System.currentTimeMillis()));
        undo=null; queueSave(); filterNow();
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
        if(prepared!=null){prepared=null;renderBackups();return true;}
        if(editing==null && storagePage){renderHome();return true;}
        if(editing==null)return false;
        persistDraft(); renderHome(); return true;
    }
    public void flush() { if(ready && !busy){persistDraft();queueSave();} }
    public void saveState(Bundle state) {
        state.putString("page",page); state.putString("editing",editing);
        if(editing!=null && title!=null){state.putString("title",title.getText().toString());state.putString("body",body.getText().toString());}
    }
    public void close() {
        closed=true; main.removeCallbacksAndMessages(null); searchWorker.shutdown();
    }
}
