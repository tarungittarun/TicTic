package app.space.notes.runtime;

import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Continuous, lazy native PDF pages with offline text search, never source writes. */
final class LocalPdfPreview extends LinearLayout implements AutoCloseable {
    static final class Document implements AutoCloseable {
        final File temporary;final ParcelFileDescriptor descriptor;final PdfRenderer renderer;final float[] ratios;
        private final LinkedHashMap<Integer,String> textCache=new LinkedHashMap<>(16,.75f,true);private int cachedCharacters;private PDDocument text;private List<String> sourcePageText;private List<PdfTextLayer> sourcePageLayers;private boolean textClaimed,textClosed,rendererClosed;
        Document(File file)throws IOException{
            temporary=file;ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer pdf=null;
            try{pdf=new PdfRenderer(fd);if(pdf.getPageCount()<1 || pdf.getPageCount()>10000){throw new IOException("PDF exceeds the 10,000-page preview safety limit");}
                ratios=new float[pdf.getPageCount()];try(PdfRenderer.Page first=pdf.openPage(0)){Arrays.fill(ratios,Math.max(.25f,Math.min(8f,first.getHeight()/(float)Math.max(1,first.getWidth()))));} // Discover other page sizes lazily; do not open thousands of pages before first paint.
            }catch(IOException | RuntimeException | OutOfMemoryError error){if(pdf!=null)try{pdf.close();}catch(RuntimeException ignored){}try{fd.close();}catch(IOException ignored){}throw error;}
            descriptor=fd;renderer=pdf;
        }
        /** Separate serialized text worker; no interaction with the renderer's page lock. */
        void sourceText(List<String> pages)throws IOException{if(pages.size()!=ratios.length)throw new IOException("Prepared Word page/text count mismatch");for(String value:pages)if(value.length()>500000)throw new IOException("Prepared Word page exceeds text safety limit");sourcePageText=Collections.unmodifiableList(new ArrayList<>(pages));}
        String text(int page)throws IOException{
            if(sourcePageText!=null)return sourcePageText.get(page);
            if(textCache.containsKey(page))return textCache.get(page);
            if(text==null)text=PDDocument.load(temporary,MemoryUsageSetting.setupTempFileOnly(64L*1024*1024).setTempDir(temporary.getParentFile()));
            PDFTextStripper strip=new PDFTextStripper();strip.setStartPage(page+1);strip.setEndPage(page+1);
            // Stop hostile/huge page extraction while it is written, not after allocating it all.
            StringBuilder out=new StringBuilder();Writer bounded=new Writer(){public void write(char[] chars,int off,int count)throws IOException{if(out.length()+count>500000)throw new IOException("This page exceeds the safe text-layer limit; visual pages still available");out.append(chars,off,count);}public void flush(){}public void close(){}};
            strip.writeText(text,bounded);String value=out.toString();textCache.put(page,value);cachedCharacters+=value.length();while(cachedCharacters>2000000 || textCache.size()>32){Integer first=textCache.keySet().iterator().next();cachedCharacters-=textCache.remove(first).length();}return value;
        }
        PdfTextLayer selection(int page)throws IOException{
            if(sourcePageLayers!=null)return sourcePageLayers.get(page);
            if(sourcePageText!=null)throw new IOException("Prepared on-page geometry unavailable; use Page text");
            if(text==null)text=PDDocument.load(temporary,MemoryUsageSetting.setupTempFileOnly(64L*1024*1024).setTempDir(temporary.getParentFile()));
            com.tom_roush.pdfbox.pdmodel.PDPage source=text.getPage(page);if(source.getRotation()%360!=0)throw new IOException("Rotated pages need Page text for now");
            final float width=source.getCropBox().getWidth(),height=source.getCropBox().getHeight();final List<PdfTextLayer.Glyph> glyphs=new ArrayList<>();
            PDFTextStripper strip=new PDFTextStripper(){@Override protected void writeString(String value,List<com.tom_roush.pdfbox.text.TextPosition> positions)throws IOException{for(com.tom_roush.pdfbox.text.TextPosition p:positions){if(p.getDir()!=0)throw new IOException("Rotated writing needs Page text for now");if(glyphs.size()>=20000)throw new IOException("Page has too many glyphs for safe on-page selection; use Page text");String letter=p.getUnicode();if(letter!=null && !letter.isEmpty() && p.getWidth()>0 && p.getHeight()>0){float x=p.getX()/width,y=p.getY()/height,w=p.getWidth()/width,h=Math.max(p.getHeight(),p.getFontSizeInPt())/height;glyphs.add(new PdfTextLayer.Glyph(letter,x,Math.max(0,y-h*.85f),x+w,Math.min(1,y+h*.18f)));}}}};
            strip.setSortByPosition(true);strip.setStartPage(page+1);strip.setEndPage(page+1);strip.writeText(text,new Writer(){public void write(char[] c,int off,int count){}public void flush(){}public void close(){}});return new PdfTextLayer(glyphs);
        }
        void sourceLayers(List<PdfTextLayer> layers)throws IOException{if(layers.size()!=ratios.length)throw new IOException("Prepared page geometry count mismatch");sourcePageLayers=Collections.unmodifiableList(new ArrayList<>(layers));}
        synchronized void claimText(){textClaimed=true;}
        synchronized void closeText(){if(text!=null)try{text.close();}catch(IOException | RuntimeException ignored){}text=null;textCache.clear();cachedCharacters=0;sourcePageText=null;sourcePageLayers=null;textClosed=true;cleanup();}
        private void cleanup(){if(rendererClosed && (!textClaimed || textClosed))temporary.delete();}
        @Override public synchronized void close(){if(rendererClosed)return;rendererClosed=true;try{renderer.close();}finally{try{descriptor.close();}catch(IOException ignored){}cleanup();}}
    }
    static Document open(Context context,Uri source)throws Exception{
        LocalDeviceDocuments.requireReadable(source);PDFBoxResourceLoader.init(context.getApplicationContext());File temporary=File.createTempFile("pdf-preview-",".pdf",context.getCacheDir());boolean done=false;
        try{try(InputStream in=context.getContentResolver().openInputStream(source);OutputStream out=new FileOutputStream(temporary)){if(in==null)throw new IOException("Cannot read PDF");byte[] buffer=new byte[8192];long bytes=0;for(int n;(n=in.read(buffer))!=-1;){if((bytes+=n)>128L*1024*1024)throw new IOException("PDF preview is limited to 128 MiB");out.write(buffer,0,n);}}Document document=new Document(temporary);done=true;return document;}finally{if(!done)temporary.delete();}
    }
    private final Document document;private final NotesDesign design;private final RecyclerView pages;private final LinearLayoutManager layout;private final TextView position;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(),textWorker=Executors.newSingleThreadExecutor();
    private final Set<Holder> holders=new HashSet<>();private final EditText query;private volatile boolean closed,indexingStopped;private volatile int searchGeneration;
    private java.util.function.BiConsumer<String,Boolean> pendingIndex;
    private PdfMarkup markup;private boolean markupLoading;private LinearLayout markBar;private String markTool="Browse";private int markColor=Color.RED;private float markWidth=.003f;
    private void configureMarks(){if(markBar!=null){String[] tools={"Pen","Highlight","Erase"};for(int i=0;i<3;i++){markBar.getChildAt(i).setSelected(markTool.equals(tools[i]));markBar.getChildAt(i).setBackground(design.choice(markTool.equals(tools[i])));}}for(Holder holder:holders){PdfSelectionImage image=(PdfSelectionImage)holder.image;image.markup=markup;image.markupPage=holder.index;image.markupTool=markTool;image.markupColor=markColor;image.markupWidth=markWidth;image.markupChanged=this::persistMarks;image.invalidate();}}
    private void persistMarks(){if(markup==null)return;PdfMarkup store=markup;worker.execute(()->{try{store.persist();}catch(Exception error){Diagnostics.failure("pdf.markup.save",error);post(()->{if(!closed)android.widget.Toast.makeText(getContext(),"Annotation draft could not be saved. Export a marked copy before leaving.",1).show();});}});}
    private final List<Runnable> markupWaiters=new ArrayList<>();
    private void loadMarkup(Runnable ready){if(closed)return;if(markup!=null){ready.run();return;}markupWaiters.add(ready);if(markupLoading)return;markupLoading=true;worker.execute(()->{try{PdfMarkup loaded=PdfMarkup.open(getContext().getApplicationContext(),document.temporary,document.ratios.length);post(()->{markupLoading=false;if(!closed){markup=loaded;configureMarks();List<Runnable> pending=new ArrayList<>(markupWaiters);markupWaiters.clear();for(Runnable action:pending)action.run();}});}catch(Exception error){Diagnostics.failure("pdf.markup.load",error);post(()->{markupLoading=false;markupWaiters.clear();if(!closed)android.widget.Toast.makeText(getContext(),"Annotation tools unavailable; original and existing draft retained",1).show();});}});}
    private void editMarks(){loadMarkup(()->{if(markBar==null){markBar=new LinearLayout(getContext());String[] names={"Pen","Highlight","Erase","Undo","Redo","Mark options","Done marking"},icons={"pencil","highlighter","erase","undo","redo","more","check"};for(int i=0;i<names.length;i++){final int which=i;markBar.addView(design.icon(names[i],icons[i],()->{if(which<3){markTool=names[which];if(which==1){markColor=0xffffd600;if(markWidth<.015f)markWidth=.025f;}else if(which==0 && markWidth>.015f)markWidth=.003f;}else if(which==3){markup.undo();persistMarks();}else if(which==4){markup.redo();persistMarks();}else if(which==5)markOptions();else{markTool="Browse";markBar.setVisibility(GONE);}configureMarks();}),new LayoutParams(design.dp(48),design.dp(48)));}addView(markBar);}markTool="Pen";markBar.setVisibility(VISIBLE);configureMarks();android.widget.Toast.makeText(getContext(),"Private annotation draft · two fingers to zoom · export a marked copy for sharing. Drafts are not in Notes backups.",1).show();});}
    private void markOptions(){String[] choices={"Red","Blue","Yellow","Green","Fine stroke","Medium stroke","Wide highlighter"};design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Mark color and width").setItems(choices,(d,w)->{if(w<4)markColor=new int[]{Color.RED,0xff1565c0,0xffffd600,0xff188038}[w];else markWidth=new float[]{.002f,.005f,.025f}[w-4];configureMarks();}).setNegativeButton("Close",null).create());}
    private void exportMarks(NotesHost host,String name){loadMarkup(()->{android.app.AlertDialog progress=new android.app.AlertDialog.Builder(getContext()).setTitle("Preparing marked PDF copy").setView(new ProgressBar(getContext())).setCancelable(false).create();design.show(progress);worker.execute(()->{try{markup.persist();File copy=markup.export(getContext(),document.temporary);post(()->{progress.dismiss();if(!closed)host.saveAnnotatedPdf(copy,name.replaceFirst("(?i)\\.pdf$","")+"-marked.pdf");});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.markup.export",error);post(()->{progress.dismiss();if(!closed)android.widget.Toast.makeText(getContext(),"Could not prepare marked copy; original and private draft retained",1).show();});}});});}
    private Runnable onBack;private FrameLayout viewport;private HorizontalScrollView horizontal;private TextView pageRail;private boolean railDragging;private float fileScale=1;private android.app.AlertDialog searchDialog;private final Runnable hideRail=()->{if(pageRail!=null && !railDragging)pageRail.setVisibility(INVISIBLE);};
    void setOnBack(Runnable action){onBack=action;}
    private void showRail(int page,float fraction){if(closed || pageRail==null)return;page=Math.max(0,Math.min(document.ratios.length-1,page));pageRail.setText((page+1)+" / "+document.ratios.length);pageRail.setContentDescription("Page scroller: "+(page+1)+" of "+document.ratios.length);float progress=fraction>=0?fraction:page/(float)Math.max(1,document.ratios.length-1);pageRail.setTranslationY(progress*Math.max(0,viewport.getHeight()-design.dp(48)));pageRail.setVisibility(VISIBLE);removeCallbacks(hideRail);if(!railDragging)postDelayed(hideRail,1000);}
    private LinearLayout searchDock;private ProgressBar searchProgress;
    private void openSearch(){
        for(Holder holder:holders)((PdfSelectionImage)holder.image).clearSelection();
        if(searchDock==null){searchDock=new LinearLayout(getContext());searchDock.setOrientation(VERTICAL);searchDock.setBackgroundColor(design.surface);LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.addView(query,new LayoutParams(0,design.dp(48),1));searchProgress=new ProgressBar(getContext());searchProgress.setVisibility(GONE);row.addView(searchProgress,new LayoutParams(design.dp(24),design.dp(24)));row.addView(design.icon("Previous match","back",()->moveMatch(-1)),new LayoutParams(design.dp(48),design.dp(48)));row.addView(design.icon("Next match","down",this::nextMatch),new LayoutParams(design.dp(48),design.dp(48)));row.addView(design.icon("Close search","close",this::closeSearch),new LayoutParams(design.dp(48),design.dp(48)));searchDock.addView(row);position.setSingleLine(true);position.setEllipsize(android.text.TextUtils.TruncateAt.END);position.setPadding(design.dp(12),0,design.dp(12),design.dp(4));searchDock.addView(position);}
        if(searchDock.getParent()==null)addView(searchDock,Math.min(2,getChildCount()));query.requestFocus();query.post(()->{if(!closed && query.hasFocus())((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(query,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
    }
    private void closeSearch(){searchGeneration++;matches.clear();totalMatches=0;query.clearFocus();((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);if(searchDock!=null)removeView(searchDock);for(Holder holder:holders)((PdfSelectionImage)holder.image).clearSearch();}
    private void showMetadata(Uri uri,String name,String mime){LocalFileMetadata.show(getContext(),design,textWorker,uri,name,mime,ocrSupported?document.temporary.length():-1,"Pages: "+document.ratios.length+(ocrSupported?"":"\nPreview: Approximate Word page rendering"),()->closed);}
    private void resizeFilePages(){if(closed || viewport==null || viewport.getWidth()<1)return;int width=Math.max(1,Math.round(viewport.getWidth()*(fileZoom?fileScale:1)));if(pages.getLayoutParams().width==width)return;pages.getLayoutParams().width=width;pages.setMinimumWidth(width);pages.requestLayout();for(Holder holder:holders)if(holder.index>=0 && holder.image.getLayoutParams()!=null){holder.image.getLayoutParams().height=Math.max(1,Math.round(width*document.ratios[holder.index]));holder.image.requestLayout();}}
    private void scaleFile(float factor){if(closed)return;int page=currentPage();float top=savedPageTop(page);fileScale=Math.max(1,Math.min(5,fileScale*factor));resizeFilePages();pages.post(()->{if(!closed)scrollToPageViewport(page,top);});showRail(page,-1);}
    private int pendingZoomPage=-1;private float[] pendingPageZoom;private boolean fileZoom;private float[] sharedZoom;private final Map<Integer,float[]> pageZooms=new HashMap<>();
    private final List<Integer> matches=new ArrayList<>();private int matchCursor;private long totalMatches;private final boolean ocrSupported;private OfflineOcr.Cancellation ocrCancellation;private android.app.AlertDialog pageTextDialog,pageOcrDialog;
    LocalPdfPreview(Context context,NotesDesign design,Document document){this(context,design,document,null,null,"");}
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name){this(context,design,document,host,uri,name,null);}
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name,java.util.function.BiConsumer<String,Boolean> index){
        this(context,design,document,host,uri,name,index,"application/pdf","Viewed PDF","");
    }
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name,java.util.function.BiConsumer<String,Boolean> index,String mime,String kind,String details){
        super(context);this.document=document;this.design=design;ocrSupported="application/pdf".equals(mime);setOrientation(VERTICAL);fileZoom=context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).getBoolean("viewer-file-zoom",false);
        position=design.label("Search embedded text",14,design.muted);
        LinearLayout bar=new LinearLayout(context);bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(design.icon("Back","back",()->{if(onBack!=null)onBack.run();}),new LayoutParams(design.dp(48),design.dp(48)));
        TextView filename=design.label(name.isEmpty()?"Document":name,17,design.ink);filename.setOnClickListener(v->{for(Holder holder:holders)((PdfSelectionImage)holder.image).clearSelection();});filename.setSingleLine(true);filename.setEllipsize(android.text.TextUtils.TruncateAt.END);filename.setContentDescription("File name: "+name);bar.addView(filename,new LayoutParams(0,design.dp(48),1));
        bar.addView(design.icon("Find in pages","search",this::openSearch),new LayoutParams(design.dp(48),design.dp(48)));
        bar.addView(design.icon("Read options","more",()->{
            List<String> actions=new ArrayList<>(Arrays.asList("Page text · count / select / copy","View details","Zoom scope · "+(fileZoom?"Whole file":"Each page")));
            if(ocrSupported){actions.add("Recognize page offline");actions.add("Edit / mark PDF");if(host!=null)actions.add("Save marked PDF copy");}
            if(host!=null)actions.addAll(Arrays.asList("Share original file","Save original copy","Open original in another reader"));
            design.show(new android.app.AlertDialog.Builder(context).setTitle("File options").setItems(actions.toArray(new String[0]),(dialog,which)->{
                String action=actions.get(which);if(action.startsWith("Page text"))showText();else if(action.equals("View details"))showMetadata(uri,name,mime);
                else if(action.startsWith("Zoom scope"))chooseZoomScope();else if(action.equals("Recognize page offline"))recognizePage(currentPage());
                else if(action.equals("Edit / mark PDF"))editMarks();else if(action.equals("Save marked PDF copy"))exportMarks(host,name);else if(action.equals("Share original file"))host.shareViewedFile(uri,mime);else if(action.equals("Save original copy"))host.saveViewedCopy(uri,name,mime);else host.openOriginal(uri,mime);
            }).setNegativeButton("Close",null).create());
        }),new LayoutParams(design.dp(48),design.dp(48)));addView(bar);
        query=new EditText(context);query.setHint("Find in document");query.setSingleLine(true);query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);design.entry(query,16);
        query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || (event!=null && event.getKeyCode()==KeyEvent.KEYCODE_ENTER && event.getAction()==KeyEvent.ACTION_UP)){find();return true;}return false;});
        pages=new RecyclerView(context);pages.setContentDescription("Continuous PDF pages, scroll vertically");layout=new LinearLayoutManager(context);pages.setLayoutManager(layout);pages.setItemViewCacheSize(1);pages.getRecycledViewPool().setMaxRecycledViews(0,2);pages.setAdapter(new PageAdapter());
        viewport=new FrameLayout(context);horizontal=new HorizontalScrollView(context);horizontal.setHorizontalScrollBarEnabled(false);horizontal.setFillViewport(true);horizontal.addView(pages,new FrameLayout.LayoutParams(-1,-1));viewport.addView(horizontal,new FrameLayout.LayoutParams(-1,-1));
        pageRail=design.label("1 / "+document.ratios.length,14,design.ink);pageRail.setGravity(Gravity.CENTER);pageRail.setElevation(design.dp(6));pageRail.setBackgroundColor(design.surface);pageRail.setContentDescription("Page scroller: 1 of "+document.ratios.length);pageRail.setVisibility(INVISIBLE);
        FrameLayout.LayoutParams railParams=new FrameLayout.LayoutParams(design.dp(96),design.dp(48),Gravity.RIGHT|Gravity.TOP);viewport.addView(pageRail,railParams);addView(viewport,new LayoutParams(-1,0,1));
        pageRail.setOnTouchListener((v,event)->{switch(event.getActionMasked()){case MotionEvent.ACTION_DOWN:railDragging=true;case MotionEvent.ACTION_MOVE:int[] origin=new int[2];viewport.getLocationOnScreen(origin);float fraction=Math.max(0,Math.min(1,(event.getRawY()-origin[1]-design.dp(24))/Math.max(1,viewport.getHeight()-design.dp(48))));int target=Math.round(fraction*(document.ratios.length-1));layout.scrollToPositionWithOffset(target,0);showRail(target,fraction);return true;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:railDragging=false;showRail(currentPage(),-1);v.performClick();return true;default:return false;}});
        pages.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView v,int dx,int dy){if(!railDragging)showRail(currentPage(),-1);}});
        GestureDetector taps=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onSingleTapUp(MotionEvent event){View touched=pages.findChildViewUnder(event.getX(),event.getY());for(Holder holder:holders)if(holder.image!=touched)((PdfSelectionImage)holder.image).clearSelection();return false;}});
        pages.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener(){@Override public boolean onInterceptTouchEvent(RecyclerView v,MotionEvent event){taps.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_DOWN)showRail(currentPage(),-1);if(event.getActionMasked()==MotionEvent.ACTION_UP)showRail(currentPage(),-1);return false;}});
        viewport.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol)resizeFilePages();});
        document.claimText();pendingIndex=index;
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(oldw>0 && w!=oldw && pages!=null){
            // Capture before child layout changes the page dimensions, then rebind.
            int page=currentPage();float top=savedPageTop(page);float[] zoom=savedPageZoom(page);
            pages.post(()->{if(!closed && pages.getAdapter()!=null){pendingPageZoom=zoom;pendingZoomPage=page;pages.getAdapter().notifyDataSetChanged();scrollToPageViewport(page,top);}});
        }
    }
    private int currentPage(){if(pendingPageZoom!=null && pendingZoomPage>=0)return pendingZoomPage;int best=Math.max(0,layout.findFirstVisibleItemPosition()),visible=0;for(int i=0;i<pages.getChildCount();i++){View child=pages.getChildAt(i);int pixels=Math.max(0,Math.min(pages.getHeight(),child.getBottom())-Math.max(0,child.getTop()));int index=pages.getChildAdapterPosition(child);if(index>=0 && pixels>visible){best=index;visible=pixels;}}return best;}
    android.os.Bundle savedViewport(){android.os.Bundle state=new android.os.Bundle();int page=currentPage();state.putBoolean("fileZoom",fileZoom);state.putFloat("fileScale",fileScale);state.putInt("page",page);state.putFloat("pageTop",savedPageTop(page));float[] zoom=savedPageZoom(page);if(zoom!=null)state.putFloatArray("zoom",zoom);String text=query.getText().toString();state.putString("query",text.substring(0,Math.min(512,text.length())));return state;}
    void restoreViewport(android.os.Bundle state){if(state==null)return;fileZoom=state.getBoolean("fileZoom",fileZoom);int page=Math.max(0,Math.min(document.ratios.length-1,state.getInt("page",0)));pendingZoomPage=page;float[] zoom=state.getFloatArray("zoom");pendingPageZoom=zoom!=null && zoom.length==5?zoom.clone():null;if(fileZoom && pendingPageZoom!=null)sharedZoom=pendingPageZoom.clone();String text=state.getString("query","");query.setText(text.substring(0,Math.min(512,text.length())));fileScale=Math.max(1,Math.min(5,state.getFloat("fileScale",fileZoom && zoom!=null?zoom[0]:1)));if(fileZoom){pendingPageZoom=null;sharedZoom=null;}resizeFilePages();float top=state.getFloat("pageTop",0);pages.post(()->{if(!closed){scrollToPageViewport(page,top);position.setText((page+1)+" / "+document.ratios.length);}});}
    private float savedPageTop(int page){
        View child=layout.findViewByPosition(page);if(child==null)return 0;
        RecyclerView.LayoutParams params=(RecyclerView.LayoutParams)child.getLayoutParams();
        float height=Math.max(1,child.getHeight()+params.topMargin+params.bottomMargin);
        return Math.max(-1,Math.min(1,(child.getTop()-params.topMargin-pages.getPaddingTop())/height));
    }
    private void scrollToPageViewport(int page,float top){
        if(closed)return;
        if(pages.getWidth()<1){
            // View.post is not a layout barrier: attached views may run it before
            // their first measure. Resolve normalized offsets only at real width.
            final float requestedTop=top;
            pages.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
                @Override public void onLayoutChange(View view,int left,int upper,int right,int bottom,int oldLeft,int oldTop,int oldRight,int oldBottom){
                    if(closed || right>left){view.removeOnLayoutChangeListener(this);if(!closed)scrollToPageViewport(page,requestedTop);}
                }
            });return;
        }
        if(!Float.isFinite(top))top=0;top=Math.max(-1,Math.min(1,top));
        int height=Math.max(1,(int)(pages.getWidth()*document.ratios[page]))+design.dp(8);
        layout.scrollToPositionWithOffset(page,Math.round(top*height));
    }
    private float[] savedPageZoom(int page){
        if(pendingZoomPage==page && pendingPageZoom!=null)return pendingPageZoom.clone();
        RecyclerView.ViewHolder holder=pages.findViewHolderForAdapterPosition(page);
        if(holder instanceof Holder && ((Holder)holder).image instanceof ZoomablePageImage)return ((ZoomablePageImage)((Holder)holder).image).savedZoom();
        return null;
    }
    private void deferTextIndex(){
        if(pendingIndex==null || closed)return;java.util.function.BiConsumer<String,Boolean> consumer=pendingIndex;pendingIndex=null;
        // Paint first. History indexing must not compete with first-page rendering or a foreground selection/find request.
        postDelayed(()->{if(!closed && !indexingStopped)indexText(consumer);},250);
    }
    private void indexText(java.util.function.BiConsumer<String,Boolean> consumer){textWorker.execute(()->{StringBuilder body=new StringBuilder();boolean partial=false;try{for(int page=0;page<document.ratios.length;page++){if(page>=64){partial=true;break;}if(closed)return;if(indexingStopped){partial=true;break;}body.append(document.text(page)).append('\n');if(body.length()>ViewedFiles.MAX_TEXT){body.setLength(ViewedFiles.MAX_TEXT+1);partial=true;break;}}}catch(Exception | OutOfMemoryError error){partial=true;Diagnostics.failure("pdf.index.failure",error);}if(closed)return;String value=body.toString();boolean incomplete=partial;post(()->{if(!closed)consumer.accept(value,incomplete);});});}
    void setZoomScope(boolean wholeFile){
        float[] current=savedPageZoom(currentPage());fileZoom=wholeFile;
        getContext().getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).edit().putBoolean("viewer-file-zoom",fileZoom).apply();
        if(fileZoom){fileScale=current==null?1:current[0];sharedZoom=null;for(Holder holder:holders)if(holder.index>=0)((ZoomablePageImage)holder.image).resetZoom();}
        else{for(Holder holder:holders)if(holder.index>=0)pageZooms.put(holder.index,((ZoomablePageImage)holder.image).savedZoom());}for(Holder holder:holders)((ZoomablePageImage)holder.image).fileScaleChanged=fileZoom?this::scaleFile:null;resizeFilePages();
    }
    private void chooseZoomScope(){design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Zoom scope").setSingleChoiceItems(new String[]{"Each page · independent zoom","Whole file · same zoom on every page"},fileZoom?1:0,(dialog,which)->{setZoomScope(which==1);dialog.dismiss();}).setNegativeButton("Cancel",null).create());}
    private void showText(){showText(currentPage());}
    private void showText(int page){indexingStopped=true;position.setText("Reading page text…");textWorker.execute(()->{try{if(closed)return;String full=document.text(page);post(()->{if(!closed){showPageText(page,full,false,0);position.setText((page+1)+" / "+document.ratios.length);}});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.text.failure",error);post(()->{if(!closed)position.setText("Text unavailable; visual pages and original retained");});}});}
    private void showPageText(int page,String full,boolean recognized,int score){
        if(pageTextDialog!=null)pageTextDialog.dismiss();boolean partial=full.length()>128000;int end=Math.min(full.length(),128000);if(end>0 && end<full.length() && Character.isHighSurrogate(full.charAt(end-1)))end--;String value=full.substring(0,end);boolean blank=value.trim().isEmpty();
        TextView text=design.label(blank?(recognized?"No text recognized. Try another page or keep using the original PDF.":(ocrSupported?"No embedded text. Use Recognize page offline for a scanned page.":"No text available on this page. Open original in a compatible reader for other options.")):value,17,design.ink);text.setTextIsSelectable(true);text.setContentDescription(recognized?"Recognized page text — review before copying":"Embedded page text");text.setPadding(design.dp(16),design.dp(12),design.dp(16),design.dp(12));ScrollView scroll=new ScrollView(getContext());scroll.addView(text);
        LinearLayout review=new LinearLayout(getContext());review.setOrientation(VERTICAL);TextView meta=design.label((recognized?"Offline OCR · review accuracy":"Embedded PDF text")+" · "+value.codePointCount(0,value.length())+" characters"+(partial?" · first128k displayed":""),13,design.muted);meta.setPadding(design.dp(16),design.dp(8),design.dp(16),design.dp(8));review.addView(meta);View divider=new View(getContext());divider.setBackgroundColor(design.outline);review.addView(divider,new LayoutParams(-1,design.dp(1)));review.addView(scroll,new LayoutParams(-1,Math.min(design.dp(420),(int)(getResources().getDisplayMetrics().heightPixels*.55f))));
        android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(getContext()).setTitle((recognized?"Recognized text":"Page text")+" · Page "+(page+1)).setView(review).setPositiveButton(partial?"Copy shown text":"Copy page text",(d,w)->{try{if(!blank)((ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(recognized?"Reviewed PDF OCR":"PDF page text",value));}catch(RuntimeException unavailable){position.setText("Clipboard unavailable; original unchanged");}}).setNegativeButton("Close",null);
        if(ocrSupported)builder.setNeutralButton("Recognize page offline",(d,w)->recognizePage(page));
        pageTextDialog=builder.create();design.show(pageTextDialog);pageTextDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(!blank);
    }
    private void recognizePage(int page){
        if(closed || !ocrSupported)return;if(ocrCancellation!=null){position.setText("Page recognition already running; cancel it before retrying");return;}
        indexingStopped=true;OfflineOcr.Cancellation cancellation=new OfflineOcr.Cancellation();ocrCancellation=cancellation;
        android.app.AlertDialog progress=new android.app.AlertDialog.Builder(getContext()).setTitle("Recognize page "+(page+1)+" offline").setMessage("English, Hindi and Kannada · one page only. Recognition can be wrong; review before copying. No Note/Doc is created and the PDF stays unchanged. Cancel keeps the original.").setNegativeButton("Cancel",(d,w)->cancellation.cancel()).create();LinearLayout progressBody=new LinearLayout(getContext());progressBody.setPadding(design.dp(20),design.dp(16),design.dp(20),design.dp(16));progressBody.setGravity(Gravity.CENTER_VERTICAL);progressBody.addView(new ProgressBar(getContext()),new LayoutParams(design.dp(40),design.dp(40)));TextView progressLabel=design.label("Recognizing page text on this device…",16,design.ink);progressBody.addView(progressLabel,new LayoutParams(0,-2,1));progress.setView(progressBody);pageOcrDialog=progress;progress.setOnDismissListener(d->{cancellation.cancel();if(!closed && ocrCancellation==cancellation)position.setText("Page recognition cancelled · original unchanged");});design.show(progress);position.setText("Recognizing page offline…");
        // This same serial executor owns all PdfRenderer pages and queued close.
        worker.execute(()->{try{
            OfflineOcr.Result result=PdfPageOcr.recognize(getContext().getApplicationContext(),document,page,"auto",cancellation);
            post(()->{if(closed || cancellation.cancelled() || ocrCancellation!=cancellation)return;ocrCancellation=null;progress.dismiss();if(pageOcrDialog==progress)pageOcrDialog=null;showPageText(page,result.text,true,result.confidence);position.setText("OCR review · original PDF unchanged");});
        }catch(InterruptedException cancelled){post(()->{if(!closed)position.setText("Page recognition cancelled · original unchanged");});
        }catch(Exception | LinkageError | OutOfMemoryError failure){Diagnostics.failure("pdf.ocr.failure",failure);post(()->{if(!closed)position.setText("Page recognition unavailable · original and notes unchanged");});
        }finally{post(()->{if(ocrCancellation==cancellation){ocrCancellation=null;progress.dismiss();if(pageOcrDialog==progress)pageOcrDialog=null;}});}});
    }
    private String activeQuery="";
    private void find(){
        indexingStopped=true;String needle=query.getText().toString().trim().toLowerCase(Locale.ROOT);int token=++searchGeneration;activeQuery=needle;matches.clear();totalMatches=0;matchCursor=0;for(Holder holder:holders)((PdfSelectionImage)holder.image).clearSearch();if(searchProgress!=null)searchProgress.setVisibility(GONE);
        if(needle.isEmpty() || needle.length()>512){position.setText(needle.isEmpty()?"Enter text to find":"Search text is too long");return;}
        position.setText("Searching… first result will open immediately");if(searchProgress!=null)searchProgress.setVisibility(VISIBLE);
        textWorker.execute(()->searchPage(0,needle,token));
    }
    private void searchPage(int page,String needle,int generation){
        if(closed || generation!=searchGeneration)return;
        if(page>=document.ratios.length){post(()->{if(!closed && generation==searchGeneration){if(searchProgress!=null)searchProgress.setVisibility(GONE);position.setText(totalMatches==0?"No matches":"Match "+(matchCursor+1)+" / "+totalMatches);if(totalMatches==0)android.widget.Toast.makeText(getContext(),"No matches in embedded text",0).show();}});return;}
        try{String value=document.text(page).toLowerCase(Locale.ROOT);int count=0;for(int at=0;(at=value.indexOf(needle,at))>=0;at+=needle.length())count++;final int found=count;
            post(()->{if(closed || generation!=searchGeneration)return;boolean first=matches.isEmpty();totalMatches+=found;for(int n=0;n<found && matches.size()<10000;n++)matches.add(page);position.setText(totalMatches+" matches · searched "+(page+1)+" / "+document.ratios.length);if(first && !matches.isEmpty()){android.os.IBinder inputToken=query.getWindowToken();query.clearFocus();android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(inputToken,0);showMatch();}});
            // Yield between pages so foreground selection and page-text requests are not starved.
            if(!closed && generation==searchGeneration)textWorker.execute(()->searchPage(page+1,needle,generation));
        }catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.search.failure",error);post(()->{if(!closed && generation==searchGeneration){if(searchProgress!=null)searchProgress.setVisibility(GONE);position.setText("Search incomplete · original unchanged");}});}
    }
    private void nextMatch(){moveMatch(1);}
    private void moveMatch(int direction){if(matches.isEmpty()){find();return;}matchCursor=(matchCursor+direction+matches.size())%matches.size();showMatch();}
    private void showMatch(){if(matches.isEmpty())return;int page=matches.get(matchCursor);layout.scrollToPositionWithOffset(page,0);position.setText("Match "+(matchCursor+1)+" / "+matches.size()+" · page "+(page+1));showRail(page,-1);pages.post(()->{if(closed)return;RecyclerView.ViewHolder current=pages.findViewHolderForAdapterPosition(page);if(current instanceof Holder)highlightMatch((Holder)current);});}
    private void highlightMatch(Holder holder){if(matches.isEmpty() || holder.index!=matches.get(matchCursor))return;int page=holder.index,token=holder.token,generation=searchGeneration;int occurrence=0;for(int i=0;i<matchCursor;i++)if(matches.get(i)==page)occurrence++;final int selected=occurrence;String needle=activeQuery;textWorker.execute(()->{try{PdfTextLayer layer=document.selection(page);post(()->{if(!closed && token==holder.token && generation==searchGeneration){PdfSelectionImage image=(PdfSelectionImage)holder.image;image.highlightSearch(layer,needle,selected);if(image.searchTop()>=0)layout.scrollToPositionWithOffset(page,design.dp(24)-Math.round(image.searchTop()*image.getHeight()));}});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.search.geometry",error);}});}
    private final class Holder extends RecyclerView.ViewHolder {final ImageView image;volatile int token;int index=-1;Holder(ImageView view){super(view);image=view;}void clear(){token++;image.setImageDrawable(null);if(image instanceof PdfSelectionImage)((PdfSelectionImage)image).clearLayer();if(image instanceof ZoomablePageImage)((ZoomablePageImage)image).resetZoom();}}
    private final class PageAdapter extends RecyclerView.Adapter<Holder>{
        @Override public int getItemCount(){return document.ratios.length;}
        @Override public Holder onCreateViewHolder(android.view.ViewGroup parent,int type){final Holder[] reference={null};ImageView image=new PdfSelectionImage(getContext(),(view,point)->{Holder current=reference[0];if(current==null)return;int page=current.index,token=current.token;indexingStopped=true;position.setText("Loading on-page selection…");textWorker.execute(()->{try{PdfTextLayer layer=document.selection(page);post(()->{if(closed || token!=current.token)return;view.selectionReady(layer,point);position.setText(layer.glyphs.isEmpty()?(ocrSupported?"No embedded glyphs · use Recognize page offline":"Page text remains available for this page"):"Drag blue endpoints to select text · original unchanged");});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.selection.failure",error);post(()->{if(!closed && token==current.token){view.selectionFailed();position.setText("On-page selection unavailable here · Page text remains available");}});}});});image.setBackgroundColor(Color.WHITE);Holder holder=new Holder(image);reference[0]=holder;((ZoomablePageImage)image).interaction=()->showRail(currentPage(),-1);((ZoomablePageImage)image).zoomChanged=state->{if(closed)return;if(!fileZoom)pageZooms.put(holder.index,state.clone());showRail(currentPage(),-1);};holders.add(holder);return holder;}
        @Override public void onBindViewHolder(Holder holder,int index){holder.clear();holder.index=index;configureMarks();((ZoomablePageImage)holder.image).fileIsZoomed=()->fileScale>1;((ZoomablePageImage)holder.image).fileScaleChanged=fileZoom?LocalPdfPreview.this::scaleFile:null;float[] retained=fileZoom?null:pageZooms.get(index);if(retained!=null)((ZoomablePageImage)holder.image).restoreZoom(retained);if(pendingZoomPage==index && pendingPageZoom!=null){((ZoomablePageImage)holder.image).restoreZoom(pendingPageZoom);pendingPageZoom=null;pendingZoomPage=-1;}int token=holder.token;int width=Math.max(1,pages.getWidth()>0?pages.getWidth():getResources().getDisplayMetrics().widthPixels);RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,Math.max(1,(int)(width*document.ratios[index])));lp.setMargins(0,design.dp(4),0,design.dp(4));holder.image.setLayoutParams(lp);holder.image.setContentDescription("PDF page "+(index+1)+" of "+document.ratios.length);worker.execute(()->{if(closed || token!=holder.token)return;Bitmap bitmap=null;try(PdfRenderer.Page page=document.renderer.openPage(index)){document.ratios[index]=Math.max(.25f,Math.min(8f,page.getHeight()/(float)Math.max(1,page.getWidth())));double scale=Math.min(2,Math.sqrt(2000000.0/Math.max(1L,(long)page.getWidth()*page.getHeight())));bitmap=Bitmap.createBitmap(Math.max(1,(int)(page.getWidth()*scale)),Math.max(1,(int)(page.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);Bitmap result=bitmap;post(()->{if(closed || token!=holder.token)result.recycle();else{holder.image.getLayoutParams().height=Math.max(1,Math.round(Math.max(1,pages.getWidth())*document.ratios[index]));holder.image.requestLayout();holder.image.setImageBitmap(result);deferTextIndex();highlightMatch(holder);if(ocrSupported && markup==null && !markupLoading)loadMarkup(()->{});}});}catch(Exception | OutOfMemoryError error){if(bitmap!=null)bitmap.recycle();Diagnostics.failure("pdf.preview.failure",error);post(()->{if(!closed)position.setText("Page unavailable · original unchanged");});}});}
        @Override public void onViewRecycled(Holder holder){if(!fileZoom)pageZooms.put(holder.index,((ZoomablePageImage)holder.image).savedZoom());holder.index=-1;holder.clear();}
    }
    @Override public void close(){if(closed)return;closed=true;removeCallbacks(hideRail);if(searchDialog!=null)searchDialog.dismiss();pendingIndex=null;if(ocrCancellation!=null)ocrCancellation.cancel();if(pageOcrDialog!=null){pageOcrDialog.dismiss();pageOcrDialog=null;}if(pageTextDialog!=null){pageTextDialog.dismiss();pageTextDialog=null;}searchGeneration++;for(Holder holder:holders)holder.clear();holders.clear();pages.setAdapter(null);worker.execute(document::close);worker.shutdown();textWorker.execute(document::closeText);textWorker.shutdown();}
}
