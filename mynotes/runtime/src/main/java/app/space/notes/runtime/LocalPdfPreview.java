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
        private PDDocument text;private List<String> sourcePageText;private List<PdfTextLayer> sourcePageLayers;private boolean textClaimed,textClosed,rendererClosed;
        Document(File file)throws IOException{
            temporary=file;ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer pdf=null;
            try{pdf=new PdfRenderer(fd);if(pdf.getPageCount()<1 || pdf.getPageCount()>10000){throw new IOException("PDF exceeds the 10,000-page preview safety limit");}
                ratios=new float[pdf.getPageCount()];for(int i=0;i<ratios.length;i++)try(PdfRenderer.Page page=pdf.openPage(i)){ratios[i]=Math.max(.25f,Math.min(8f,page.getHeight()/(float)Math.max(1,page.getWidth())));}
            }catch(IOException | RuntimeException | OutOfMemoryError error){if(pdf!=null)try{pdf.close();}catch(RuntimeException ignored){}try{fd.close();}catch(IOException ignored){}throw error;}
            descriptor=fd;renderer=pdf;
        }
        /** Separate serialized text worker; no interaction with the renderer's page lock. */
        void sourceText(List<String> pages)throws IOException{if(pages.size()!=ratios.length)throw new IOException("Prepared Word page/text count mismatch");for(String value:pages)if(value.length()>500000)throw new IOException("Prepared Word page exceeds text safety limit");sourcePageText=Collections.unmodifiableList(new ArrayList<>(pages));}
        String text(int page)throws IOException{
            if(sourcePageText!=null)return sourcePageText.get(page);
            if(text==null)text=PDDocument.load(temporary,MemoryUsageSetting.setupTempFileOnly(64L*1024*1024).setTempDir(temporary.getParentFile()));
            PDFTextStripper strip=new PDFTextStripper();strip.setStartPage(page+1);strip.setEndPage(page+1);
            // Stop hostile/huge page extraction while it is written, not after allocating it all.
            StringBuilder out=new StringBuilder();Writer bounded=new Writer(){public void write(char[] chars,int off,int count)throws IOException{if(out.length()+count>500000)throw new IOException("This page exceeds the safe text-layer limit; visual pages still available");out.append(chars,off,count);}public void flush(){}public void close(){}};
            strip.writeText(text,bounded);return out.toString();
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
        synchronized void closeText(){if(text!=null)try{text.close();}catch(IOException | RuntimeException ignored){}text=null;sourcePageText=null;sourcePageLayers=null;textClosed=true;cleanup();}
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
    private int pendingZoomPage=-1;private float[] pendingPageZoom;private boolean fileZoom;private float[] sharedZoom;private final Map<Integer,float[]> pageZooms=new HashMap<>();
    private final List<Integer> matches=new ArrayList<>();private int matchCursor;private long totalMatches;private final boolean ocrSupported;private OfflineOcr.Cancellation ocrCancellation;private android.app.AlertDialog pageTextDialog,pageOcrDialog;
    LocalPdfPreview(Context context,NotesDesign design,Document document){this(context,design,document,null,null,"");}
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name){this(context,design,document,host,uri,name,null);}
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name,java.util.function.BiConsumer<String,Boolean> index){
        this(context,design,document,host,uri,name,index,"application/pdf","Viewed PDF","");
    }
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name,java.util.function.BiConsumer<String,Boolean> index,String mime,String kind,String details){
        super(context);this.document=document;this.design=design;ocrSupported="application/pdf".equals(mime);setOrientation(VERTICAL);fileZoom=context.getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).getBoolean("viewer-file-zoom",false);
        TextView title=design.label(name+" · "+kind+" · not a saved Note/Doc",13,design.muted);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setContentDescription(name+" · file details");title.setOnClickListener(v->design.show(new android.app.AlertDialog.Builder(context).setTitle(name).setMessage(details.isEmpty()?"Read-only continuous pages. Find and page text use embedded text, not OCR. Original bytes are unchanged. Pinch to zoom, drag to pan while zoomed, double-tap to reset. Long-press embedded text directly on an unrotated PDF page, drag its blue endpoints and choose Copy. Page text remains an alternative for rotated or unsupported layouts; scanned pages need OCR. Use Recognize page offline for scanned pages (English/Hindi/Kannada). OCR is reviewed, may be wrong and never changes the PDF. Open original for other reader-specific options; another reader controls its own privacy and network access.":details).setPositiveButton("Close",null).create()));
        LinearLayout search=new LinearLayout(context);search.setVisibility(GONE);query=new EditText(context);query.setHint("Find embedded text");query.setSingleLine(true);design.entry(query,15);search.addView(query,new LayoutParams(0,-2,1));search.addView(design.icon("Find","search",this::find),new LayoutParams(design.dp(48),design.dp(48)));search.addView(design.icon("Next match","down",this::nextMatch),new LayoutParams(design.dp(48),design.dp(48)));
        LinearLayout controls=new LinearLayout(context);controls.setGravity(Gravity.CENTER_VERTICAL);position=design.label("1 / "+document.ratios.length,14,design.ink);position.setContentDescription(name+" · page position");position.setMaxLines(2);position.setEllipsize(android.text.TextUtils.TruncateAt.END);controls.addView(position,new LayoutParams(0,-2,1));controls.addView(design.icon("Find in pages","search",()->{search.setVisibility(search.getVisibility()==VISIBLE?GONE:VISIBLE);if(search.getVisibility()==VISIBLE)query.requestFocus();}),new LayoutParams(design.dp(48),design.dp(48)));
        controls.addView(design.icon("Select page text","text",this::showText),new LayoutParams(design.dp(48),design.dp(48)));
        if(host!=null)controls.addView(design.icon("Share original file","share",()->host.shareViewedFile(uri,mime)),new LayoutParams(design.dp(48),design.dp(48)));
        controls.addView(design.icon("Read options","more",()->{String[] base=host==null?new String[]{"Go to page","Page text · count / select / copy","View details"}:new String[]{"Go to page","Page text · count / select / copy","View details","Save original copy","Open original in another reader"};String[] items=java.util.Arrays.copyOf(base,base.length+(ocrSupported?2:1));items[base.length]="Zoom scope · "+(fileZoom?"Whole file":"Each page");if(ocrSupported)items[items.length-1]="Recognize page offline";design.show(new android.app.AlertDialog.Builder(context).setTitle("Read options").setItems(items,(dialog,which)->{if(which==base.length)chooseZoomScope();else if("Recognize page offline".equals(items[which]))recognizePage(currentPage());else if(which==0)goToPage();else if(which==1)showText();else if(which==2)title.performClick();else if(which==3)host.saveViewedCopy(uri,name,mime);else if(which==4)host.openOriginal(uri,mime);}).setNegativeButton("Close",null).create());}),new LayoutParams(design.dp(48),design.dp(48)));addView(controls);addView(search);
        pages=new RecyclerView(context);pages.setContentDescription("Continuous PDF pages, scroll vertically");layout=new LinearLayoutManager(context);pages.setLayoutManager(layout);pages.setItemViewCacheSize(1);pages.getRecycledViewPool().setMaxRecycledViews(0,2);pages.setAdapter(new PageAdapter());addView(pages,new LayoutParams(-1,0,1));
        pages.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView v,int dx,int dy){position.setText(matches.isEmpty()?(currentPage()+1)+" / "+document.ratios.length:totalMatches+" matches · page "+(currentPage()+1)+" / "+document.ratios.length);}});document.claimText();if(index!=null)indexText(index);
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
    android.os.Bundle savedViewport(){android.os.Bundle state=new android.os.Bundle();int page=currentPage();state.putBoolean("fileZoom",fileZoom);state.putInt("page",page);state.putFloat("pageTop",savedPageTop(page));float[] zoom=savedPageZoom(page);if(zoom!=null)state.putFloatArray("zoom",zoom);String text=query.getText().toString();state.putString("query",text.substring(0,Math.min(512,text.length())));return state;}
    void restoreViewport(android.os.Bundle state){if(state==null)return;fileZoom=state.getBoolean("fileZoom",fileZoom);int page=Math.max(0,Math.min(document.ratios.length-1,state.getInt("page",0)));pendingZoomPage=page;float[] zoom=state.getFloatArray("zoom");pendingPageZoom=zoom!=null && zoom.length==5?zoom.clone():null;if(fileZoom && pendingPageZoom!=null)sharedZoom=pendingPageZoom.clone();String text=state.getString("query","");query.setText(text.substring(0,Math.min(512,text.length())));if(!text.isEmpty())((View)query.getParent()).setVisibility(VISIBLE);float top=state.getFloat("pageTop",0);pages.post(()->{if(!closed){scrollToPageViewport(page,top);position.setText((page+1)+" / "+document.ratios.length);}});}
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
    private void indexText(java.util.function.BiConsumer<String,Boolean> consumer){textWorker.execute(()->{StringBuilder body=new StringBuilder();boolean partial=false;try{for(int page=0;page<document.ratios.length;page++){if(page>=64){partial=true;break;}if(closed)return;if(indexingStopped){partial=true;break;}body.append(document.text(page)).append('\n');if(body.length()>ViewedFiles.MAX_TEXT){body.setLength(ViewedFiles.MAX_TEXT+1);partial=true;break;}}}catch(Exception | OutOfMemoryError error){partial=true;Diagnostics.failure("pdf.index.failure",error);}if(closed)return;String value=body.toString();boolean incomplete=partial;post(()->{if(!closed)consumer.accept(value,incomplete);});});}
    void setZoomScope(boolean wholeFile){
        float[] current=savedPageZoom(currentPage());fileZoom=wholeFile;
        getContext().getSharedPreferences("mynotes-ui",Context.MODE_PRIVATE).edit().putBoolean("viewer-file-zoom",fileZoom).apply();
        if(fileZoom){sharedZoom=current;for(Holder holder:holders)if(holder.index>=0)((ZoomablePageImage)holder.image).restoreZoom(sharedZoom);}
        else{for(Holder holder:holders)if(holder.index>=0)pageZooms.put(holder.index,((ZoomablePageImage)holder.image).savedZoom());}
    }
    private void chooseZoomScope(){design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Zoom scope").setSingleChoiceItems(new String[]{"Each page · independent zoom","Whole file · same zoom on every page"},fileZoom?1:0,(dialog,which)->{setZoomScope(which==1);dialog.dismiss();}).setNegativeButton("Cancel",null).create());}
    private void goToPage(){EditText field=new EditText(getContext());field.setInputType(2);field.setHint("1–"+document.ratios.length);design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Go to page").setView(field).setPositiveButton("Go",(d,w)->{try{int page=Integer.parseInt(field.getText().toString())-1;if(page>=0 && page<document.ratios.length)pages.scrollToPosition(page);else position.setText("Choose a page from 1 to "+document.ratios.length);}catch(NumberFormatException invalid){position.setText("Enter a page number");}}).setNegativeButton("Cancel",null).create());}
    private void showText(){showText(currentPage());}
    private void showText(int page){indexingStopped=true;position.setText("Reading page text…");textWorker.execute(()->{try{if(closed)return;String full=document.text(page);post(()->{if(!closed){showPageText(page,full,false,0);position.setText((page+1)+" / "+document.ratios.length);}});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.text.failure",error);post(()->{if(!closed)position.setText("Text unavailable; visual pages and original retained");});}});}
    private void showPageText(int page,String full,boolean recognized,int score){
        if(pageTextDialog!=null)pageTextDialog.dismiss();boolean partial=full.length()>128000;int end=Math.min(full.length(),128000);if(end>0 && end<full.length() && Character.isHighSurrogate(full.charAt(end-1)))end--;String value=full.substring(0,end);boolean blank=value.trim().isEmpty();
        TextView text=design.label(blank?(recognized?"No text recognized. Try another page or keep using the original PDF.":(ocrSupported?"No embedded text. Use Recognize page offline for a scanned page.":"No text available on this page. Open original in a compatible reader for other options.")):value,17,design.ink);text.setTextIsSelectable(true);text.setContentDescription(recognized?"Recognized page text — review before copying":"Embedded page text");text.setPadding(design.dp(16),design.dp(12),design.dp(16),design.dp(12));ScrollView scroll=new ScrollView(getContext());scroll.addView(text);
        android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(getContext()).setTitle((recognized?"Review OCR · engine score "+score+" · ":"")+"Page "+(page+1)+" · "+value.codePointCount(0,value.length())+" characters"+(partial?" · first128k shown":"")).setView(scroll).setPositiveButton(partial?"Copy shown text":"Copy page text",(d,w)->{try{if(!blank)((ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(recognized?"Reviewed PDF OCR":"PDF page text",value));}catch(RuntimeException unavailable){position.setText("Clipboard unavailable; original unchanged");}}).setNegativeButton("Close",null);
        if(ocrSupported)builder.setNeutralButton("Recognize page offline",(d,w)->recognizePage(page));
        pageTextDialog=builder.create();design.show(pageTextDialog);pageTextDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(!blank);
    }
    private void recognizePage(int page){
        if(closed || !ocrSupported)return;if(ocrCancellation!=null){position.setText("Page recognition already running; cancel it before retrying");return;}
        indexingStopped=true;OfflineOcr.Cancellation cancellation=new OfflineOcr.Cancellation();ocrCancellation=cancellation;
        android.app.AlertDialog progress=new android.app.AlertDialog.Builder(getContext()).setTitle("Recognize page "+(page+1)+" offline").setMessage("English, Hindi and Kannada · one page only. Recognition can be wrong; review before copying. No Note/Doc is created and the PDF stays unchanged. Cancel keeps the original.").setNegativeButton("Cancel",(d,w)->cancellation.cancel()).create();pageOcrDialog=progress;progress.setOnDismissListener(d->{cancellation.cancel();if(!closed && ocrCancellation==cancellation)position.setText("Page recognition cancelled · original unchanged");});design.show(progress);position.setText("Recognizing page offline…");
        // This same serial executor owns all PdfRenderer pages and queued close.
        worker.execute(()->{try{
            OfflineOcr.Result result=PdfPageOcr.recognize(getContext().getApplicationContext(),document,page,"auto",cancellation);
            post(()->{if(closed || cancellation.cancelled() || ocrCancellation!=cancellation)return;ocrCancellation=null;progress.dismiss();if(pageOcrDialog==progress)pageOcrDialog=null;showPageText(page,result.text,true,result.confidence);position.setText("OCR review · original PDF unchanged");});
        }catch(InterruptedException cancelled){post(()->{if(!closed)position.setText("Page recognition cancelled · original unchanged");});
        }catch(Exception | LinkageError | OutOfMemoryError failure){Diagnostics.failure("pdf.ocr.failure",failure);post(()->{if(!closed)position.setText("Page recognition unavailable · original and notes unchanged");});
        }finally{post(()->{if(ocrCancellation==cancellation){ocrCancellation=null;progress.dismiss();if(pageOcrDialog==progress)pageOcrDialog=null;}});}});
    }
    private void find(){indexingStopped=true;String needle=query.getText().toString().trim().toLowerCase(Locale.ROOT);int token=++searchGeneration;matches.clear();totalMatches=0;if(needle.isEmpty()){position.setText("Enter text to find");return;}if(needle.length()>512){position.setText("Search text is too long");return;}position.setText("Searching embedded text…");textWorker.execute(()->{List<Integer> found=new ArrayList<>();long count=0;try{for(int page=0;page<document.ratios.length;page++){if(closed || token!=searchGeneration)return;String text=document.text(page).toLowerCase(Locale.ROOT);for(int at=0;(at=text.indexOf(needle,at))>=0;at+=needle.length()){count++;if(found.size()<10000)found.add(page);}}final long total=count;post(()->{if(closed || token!=searchGeneration)return;matches.addAll(found);totalMatches=total;matchCursor=0;position.setText(total+" matches · "+document.ratios.length+" pages"+(total>found.size()?" · first 10,000 navigable":""));if(!matches.isEmpty())pages.scrollToPosition(matches.get(0));});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.search.failure",error);post(()->{if(!closed && token==searchGeneration)position.setText("Search incomplete; visual pages and original retained");});}});}
    private void nextMatch(){if(matches.isEmpty()){find();return;}matchCursor=(matchCursor+1)%matches.size();int page=matches.get(matchCursor);pages.scrollToPosition(page);position.setText("Match "+(matchCursor+1)+" / "+matches.size()+" · page "+(page+1));}
    private final class Holder extends RecyclerView.ViewHolder {final ImageView image;volatile int token;int index=-1;Holder(ImageView view){super(view);image=view;}void clear(){token++;image.setImageDrawable(null);if(image instanceof PdfSelectionImage)((PdfSelectionImage)image).clearLayer();if(image instanceof ZoomablePageImage)((ZoomablePageImage)image).resetZoom();}}
    private final class PageAdapter extends RecyclerView.Adapter<Holder>{
        @Override public int getItemCount(){return document.ratios.length;}
        @Override public Holder onCreateViewHolder(android.view.ViewGroup parent,int type){final Holder[] reference={null};ImageView image=new PdfSelectionImage(getContext(),(view,point)->{Holder current=reference[0];if(current==null)return;int page=current.index,token=current.token;indexingStopped=true;position.setText("Loading on-page selection…");textWorker.execute(()->{try{PdfTextLayer layer=document.selection(page);post(()->{if(closed || token!=current.token)return;view.selectionReady(layer,point);position.setText(layer.glyphs.isEmpty()?(ocrSupported?"No embedded glyphs · use Recognize page offline":"Page text remains available for this page"):"Drag blue endpoints to select text · original unchanged");});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.selection.failure",error);post(()->{if(!closed && token==current.token){view.selectionFailed();position.setText("On-page selection unavailable here · Page text remains available");}});}});});image.setBackgroundColor(Color.WHITE);Holder holder=new Holder(image);reference[0]=holder;((ZoomablePageImage)image).zoomChanged=state->{if(closed)return;if(fileZoom){sharedZoom=state.clone();for(Holder other:holders)if(other!=holder && other.index>=0)((ZoomablePageImage)other.image).restoreZoom(state);}else pageZooms.put(holder.index,state.clone());};holders.add(holder);return holder;}
        @Override public void onBindViewHolder(Holder holder,int index){holder.clear();holder.index=index;float[] retained=fileZoom?sharedZoom:pageZooms.get(index);if(retained!=null)((ZoomablePageImage)holder.image).restoreZoom(retained);if(pendingZoomPage==index && pendingPageZoom!=null){((ZoomablePageImage)holder.image).restoreZoom(pendingPageZoom);pendingPageZoom=null;pendingZoomPage=-1;}int token=holder.token;int width=Math.max(1,pages.getWidth()>0?pages.getWidth():getResources().getDisplayMetrics().widthPixels);RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,Math.max(1,(int)(width*document.ratios[index])));lp.setMargins(0,design.dp(4),0,design.dp(4));holder.image.setLayoutParams(lp);holder.image.setContentDescription("PDF page "+(index+1)+" of "+document.ratios.length);worker.execute(()->{if(closed || token!=holder.token)return;Bitmap bitmap=null;try(PdfRenderer.Page page=document.renderer.openPage(index)){double scale=Math.min(2,Math.sqrt(2000000.0/Math.max(1L,(long)page.getWidth()*page.getHeight())));bitmap=Bitmap.createBitmap(Math.max(1,(int)(page.getWidth()*scale)),Math.max(1,(int)(page.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);Bitmap result=bitmap;post(()->{if(closed || token!=holder.token)result.recycle();else holder.image.setImageBitmap(result);});}catch(Exception | OutOfMemoryError error){if(bitmap!=null)bitmap.recycle();Diagnostics.failure("pdf.preview.failure",error);post(()->{if(!closed)position.setText("Page unavailable · original unchanged");});}});}
        @Override public void onViewRecycled(Holder holder){if(!fileZoom)pageZooms.put(holder.index,((ZoomablePageImage)holder.image).savedZoom());holder.index=-1;holder.clear();}
    }
    @Override public void close(){if(closed)return;closed=true;if(ocrCancellation!=null)ocrCancellation.cancel();if(pageOcrDialog!=null){pageOcrDialog.dismiss();pageOcrDialog=null;}if(pageTextDialog!=null){pageTextDialog.dismiss();pageTextDialog=null;}searchGeneration++;for(Holder holder:holders)holder.clear();holders.clear();pages.setAdapter(null);worker.execute(document::close);worker.shutdown();textWorker.execute(document::closeText);textWorker.shutdown();}
}
