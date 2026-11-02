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
        private PDDocument text;private List<String> sourcePageText;private boolean textClaimed,textClosed,rendererClosed;
        Document(File file)throws IOException{
            temporary=file;ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer pdf=null;
            try{pdf=new PdfRenderer(fd);if(pdf.getPageCount()<1 || pdf.getPageCount()>1000){throw new IOException("PDF page count exceeds safe preview limit");}
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
        synchronized void claimText(){textClaimed=true;}
        synchronized void closeText(){if(text!=null)try{text.close();}catch(IOException | RuntimeException ignored){}text=null;sourcePageText=null;textClosed=true;cleanup();}
        private void cleanup(){if(rendererClosed && (!textClaimed || textClosed))temporary.delete();}
        @Override public synchronized void close(){if(rendererClosed)return;rendererClosed=true;try{renderer.close();}finally{try{descriptor.close();}catch(IOException ignored){}cleanup();}}
    }
    static Document open(Context context,Uri source)throws Exception{
        LocalDeviceDocuments.requireReadable(source);PDFBoxResourceLoader.init(context.getApplicationContext());File temporary=File.createTempFile("pdf-preview-",".pdf",context.getCacheDir());boolean done=false;
        try{try(InputStream in=context.getContentResolver().openInputStream(source);OutputStream out=new FileOutputStream(temporary)){if(in==null)throw new IOException("Cannot read PDF");byte[] buffer=new byte[8192];long bytes=0;for(int n;(n=in.read(buffer))!=-1;){if((bytes+=n)>32*1024*1024)throw new IOException("PDF preview is limited to 32 MiB");out.write(buffer,0,n);}}Document document=new Document(temporary);done=true;return document;}finally{if(!done)temporary.delete();}
    }
    private final Document document;private final NotesDesign design;private final RecyclerView pages;private final LinearLayoutManager layout;private final TextView position;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(),textWorker=Executors.newSingleThreadExecutor();
    private final Set<Holder> holders=new HashSet<>();private final EditText query;private volatile boolean closed,indexingStopped;private volatile int searchGeneration;
    private final List<Integer> matches=new ArrayList<>();private int matchCursor;private long totalMatches;
    LocalPdfPreview(Context context,NotesDesign design,Document document){this(context,design,document,null,null,"");}
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name){this(context,design,document,host,uri,name,null);}
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name,java.util.function.BiConsumer<String,Boolean> index){
        this(context,design,document,host,uri,name,index,"application/pdf","Viewed PDF","");
    }
    LocalPdfPreview(Context context,NotesDesign design,Document document,NotesHost host,Uri uri,String name,java.util.function.BiConsumer<String,Boolean> index,String mime,String kind,String details){
        super(context);this.document=document;this.design=design;setOrientation(VERTICAL);
        TextView title=design.label(name+" · "+kind+" · not a saved Note/Doc",13,design.muted);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setMinHeight(design.dp(48));title.setOnClickListener(v->design.show(new android.app.AlertDialog.Builder(context).setTitle(name).setMessage(details.isEmpty()?"Read-only continuous pages. Find and page text use embedded text, not OCR. Original bytes are unchanged. Pinch to zoom, drag to pan while zoomed, double-tap to reset. Long-press a page or use Select page text to copy embedded text. Scanned pages need OCR. Open original for other reader-specific options; another reader controls its own privacy and network access.":details).setPositiveButton("Close",null).create()));addView(title);
        LinearLayout search=new LinearLayout(context);search.setVisibility(GONE);query=new EditText(context);query.setHint("Find embedded text");query.setSingleLine(true);design.entry(query,15);search.addView(query,new LayoutParams(0,-2,1));search.addView(design.icon("Find","search",this::find),new LayoutParams(design.dp(48),design.dp(48)));search.addView(design.icon("Next match","down",this::nextMatch),new LayoutParams(design.dp(48),design.dp(48)));
        LinearLayout controls=new LinearLayout(context);controls.setGravity(Gravity.CENTER_VERTICAL);position=design.label("1 / "+document.ratios.length,14,design.ink);position.setMaxLines(2);position.setEllipsize(android.text.TextUtils.TruncateAt.END);controls.addView(position,new LayoutParams(0,-2,1));controls.addView(design.icon("Find in pages","search",()->{search.setVisibility(search.getVisibility()==VISIBLE?GONE:VISIBLE);if(search.getVisibility()==VISIBLE)query.requestFocus();}),new LayoutParams(design.dp(48),design.dp(48)));
        controls.addView(design.icon("Select page text","text",this::showText),new LayoutParams(design.dp(48),design.dp(48)));
        if(host!=null)controls.addView(design.icon("Share original file","share",()->host.shareViewedFile(uri,mime)),new LayoutParams(design.dp(48),design.dp(48)));
        controls.addView(design.icon("Read options","more",()->{String[] items=host==null?new String[]{"Go to page","Page text · count / select / copy","View details"}:new String[]{"Go to page","Page text · count / select / copy","View details","Save original copy","Open original in another reader"};design.show(new android.app.AlertDialog.Builder(context).setTitle("Read options").setItems(items,(dialog,which)->{if(which==0)goToPage();else if(which==1)showText();else if(which==2)title.performClick();else if(which==3)host.saveViewedCopy(uri,name,mime);else if(which==4)host.openOriginal(uri,mime);}).setNegativeButton("Close",null).create());}),new LayoutParams(design.dp(48),design.dp(48)));addView(controls);addView(search);
        pages=new RecyclerView(context);pages.setContentDescription("Continuous PDF pages, scroll vertically");layout=new LinearLayoutManager(context);pages.setLayoutManager(layout);pages.setItemViewCacheSize(1);pages.getRecycledViewPool().setMaxRecycledViews(0,2);pages.setAdapter(new PageAdapter());addView(pages,new LayoutParams(-1,0,1));
        pages.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView v,int dx,int dy){position.setText(matches.isEmpty()?(currentPage()+1)+" / "+document.ratios.length:totalMatches+" matches · page "+(currentPage()+1)+" / "+document.ratios.length);}});document.claimText();if(index!=null)indexText(index);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(oldw>0 && w!=oldw && pages!=null)pages.post(()->{if(!closed && pages.getAdapter()!=null){int page=currentPage();pages.getAdapter().notifyDataSetChanged();layout.scrollToPositionWithOffset(page,0);}});}
    private int currentPage(){int best=Math.max(0,layout.findFirstVisibleItemPosition()),visible=0;for(int i=0;i<pages.getChildCount();i++){View child=pages.getChildAt(i);int pixels=Math.max(0,Math.min(pages.getHeight(),child.getBottom())-Math.max(0,child.getTop()));int index=pages.getChildAdapterPosition(child);if(index>=0 && pixels>visible){best=index;visible=pixels;}}return best;}
    private void indexText(java.util.function.BiConsumer<String,Boolean> consumer){textWorker.execute(()->{StringBuilder body=new StringBuilder();boolean partial=false;try{for(int page=0;page<document.ratios.length;page++){if(closed)return;if(indexingStopped){partial=true;break;}body.append(document.text(page)).append('\n');if(body.length()>ViewedFiles.MAX_TEXT){body.setLength(ViewedFiles.MAX_TEXT+1);partial=true;break;}}}catch(Exception | OutOfMemoryError error){partial=true;Diagnostics.failure("pdf.index.failure",error);}if(closed)return;String value=body.toString();boolean incomplete=partial;post(()->{if(!closed)consumer.accept(value,incomplete);});});}
    private void goToPage(){EditText field=new EditText(getContext());field.setInputType(2);field.setHint("1–"+document.ratios.length);design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Go to page").setView(field).setPositiveButton("Go",(d,w)->{try{int page=Integer.parseInt(field.getText().toString())-1;if(page>=0 && page<document.ratios.length)pages.scrollToPosition(page);else position.setText("Choose a page from 1 to "+document.ratios.length);}catch(NumberFormatException invalid){position.setText("Enter a page number");}}).setNegativeButton("Cancel",null).create());}
    private void showText(){showText(currentPage());}
    private void showText(int page){indexingStopped=true;position.setText("Reading page text…");textWorker.execute(()->{try{if(closed)return;String full=document.text(page);final boolean partial=full.length()>128000;String value=full.substring(0,Math.min(full.length(),128000));post(()->{if(closed)return;TextView text=design.label(value.isEmpty()?"No embedded text on this page. Scanned pages require OCR.":value,17,design.ink);text.setTextIsSelectable(true);text.setPadding(design.dp(16),design.dp(12),design.dp(16),design.dp(12));ScrollView scroll=new ScrollView(getContext());scroll.addView(text);design.show(new android.app.AlertDialog.Builder(getContext()).setTitle("Page "+(page+1)+" · "+value.codePointCount(0,value.length())+" characters"+(partial?" · first 128k shown":"")).setView(scroll).setPositiveButton(partial?"Copy shown text":"Copy page text",(d,w)->{try{if(!value.isEmpty())((ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PDF page text",value));}catch(RuntimeException unavailable){position.setText("Clipboard unavailable; original unchanged");}}).setNegativeButton("Close",null).create());position.setText((page+1)+" / "+document.ratios.length);});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.text.failure",error);post(()->{if(!closed)position.setText("Text unavailable; visual pages and original retained");});}});}
    private void find(){indexingStopped=true;String needle=query.getText().toString().trim().toLowerCase(Locale.ROOT);int token=++searchGeneration;matches.clear();totalMatches=0;if(needle.isEmpty()){position.setText("Enter text to find");return;}if(needle.length()>512){position.setText("Search text is too long");return;}position.setText("Searching embedded text…");textWorker.execute(()->{List<Integer> found=new ArrayList<>();long count=0;try{for(int page=0;page<document.ratios.length;page++){if(closed || token!=searchGeneration)return;String text=document.text(page).toLowerCase(Locale.ROOT);for(int at=0;(at=text.indexOf(needle,at))>=0;at+=needle.length()){count++;if(found.size()<10000)found.add(page);}}final long total=count;post(()->{if(closed || token!=searchGeneration)return;matches.addAll(found);totalMatches=total;matchCursor=0;position.setText(total+" matches · "+document.ratios.length+" pages"+(total>found.size()?" · first 10,000 navigable":""));if(!matches.isEmpty())pages.scrollToPosition(matches.get(0));});}catch(Exception | OutOfMemoryError error){Diagnostics.failure("pdf.search.failure",error);post(()->{if(!closed && token==searchGeneration)position.setText("Search incomplete; visual pages and original retained");});}});}
    private void nextMatch(){if(matches.isEmpty()){find();return;}matchCursor=(matchCursor+1)%matches.size();int page=matches.get(matchCursor);pages.scrollToPosition(page);position.setText("Match "+(matchCursor+1)+" / "+matches.size()+" · page "+(page+1));}
    private final class Holder extends RecyclerView.ViewHolder {final ImageView image;volatile int token;int index;Holder(ImageView view){super(view);image=view;}void clear(){token++;image.setImageDrawable(null);if(image instanceof ZoomablePageImage)((ZoomablePageImage)image).resetZoom();}}
    private final class PageAdapter extends RecyclerView.Adapter<Holder>{
        @Override public int getItemCount(){return document.ratios.length;}
        @Override public Holder onCreateViewHolder(android.view.ViewGroup parent,int type){ImageView image=new ZoomablePageImage(getContext());image.setBackgroundColor(Color.WHITE);Holder holder=new Holder(image);image.setOnLongClickListener(v->{showText(holder.index);return true;});holders.add(holder);return holder;}
        @Override public void onBindViewHolder(Holder holder,int index){holder.clear();holder.index=index;int token=holder.token;int width=Math.max(1,pages.getWidth()>0?pages.getWidth():getResources().getDisplayMetrics().widthPixels);RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,Math.max(1,(int)(width*document.ratios[index])));lp.setMargins(0,design.dp(4),0,design.dp(4));holder.image.setLayoutParams(lp);holder.image.setContentDescription("PDF page "+(index+1)+" of "+document.ratios.length);worker.execute(()->{if(closed || token!=holder.token)return;Bitmap bitmap=null;try(PdfRenderer.Page page=document.renderer.openPage(index)){double scale=Math.min(2,Math.sqrt(2000000.0/Math.max(1L,(long)page.getWidth()*page.getHeight())));bitmap=Bitmap.createBitmap(Math.max(1,(int)(page.getWidth()*scale)),Math.max(1,(int)(page.getHeight()*scale)),Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);Bitmap result=bitmap;post(()->{if(closed || token!=holder.token)result.recycle();else holder.image.setImageBitmap(result);});}catch(Exception | OutOfMemoryError error){if(bitmap!=null)bitmap.recycle();Diagnostics.failure("pdf.preview.failure",error);post(()->{if(!closed)position.setText("Page unavailable · original unchanged");});}});}
        @Override public void onViewRecycled(Holder holder){holder.clear();}
    }
    @Override public void close(){if(closed)return;closed=true;searchGeneration++;for(Holder holder:holders)holder.clear();holders.clear();pages.setAdapter(null);worker.execute(document::close);worker.shutdown();textWorker.execute(document::closeText);textWorker.shutdown();}
}
