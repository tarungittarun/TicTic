package app.space.notes.runtime;

import android.content.Context;
import android.net.Uri;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.text.*;
import android.text.style.*;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Offline prepared-paper preview, NOT a full Word layout engine or editable conversion.
 * Uses stored paper geometry, explicit/cached page breaks, inline images and default headers.
 * System-font substitution and unsupported floating/table/section layout are disclosed.
 */
final class WordPageFile {
    static final class Result {final LocalPdfPreview.Document pdf;final String text,details;Result(LocalPdfPreview.Document pdf,String text,String details){this.pdf=pdf;this.text=text;this.details=details;}}
    private static final String SW="http://purl.oclc.org/ooxml/wordprocessingml/main",SR="http://purl.oclc.org/ooxml/officeDocument/relationships";
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main",R="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private final Map<String,byte[]> files;private final Map<String,Bitmap> images=new HashMap<>();private final List<Bitmap> allocated=new ArrayList<>();
    private final List<StringBuilder> pageText=new ArrayList<>();private final List<List<PdfTextLayer.Glyph>> pageGlyphs=new ArrayList<>();private int selectionGlyphs;private String pageHeader="",pageFooter="";
    private final LinkedHashSet<String> warnings=new LinkedHashSet<>();private final StringBuilder plain=new StringBuilder();private int pixels,characters,paragraphs,keptImages;
    private float width=595,height=842,left=72,right=72,top=72,bottom=72,defaultSize=11;
    private String headerPart,footerPart;private final List<Paragraph> body=new ArrayList<>();
    private static final class Paragraph {final SpannableStringBuilder text=new SpannableStringBuilder();Layout.Alignment align=Layout.Alignment.ALIGN_NORMAL;boolean pageBreak;float before=0,after=0,lineMultiple=1,lineExtra=0,lineHeight=0;boolean atLeast;}
    private WordPageFile(Map<String,byte[]> files){this.files=files;warnings.add("Approximate pagination: Android system fonts replace Word fonts; line wrapping may differ.");warnings.add("Floating drawings, merged tables, fields, advanced styles and multiple-section geometry are not faithfully reproduced. Use Open original for full fidelity.");}
    static Result open(Context context,Uri uri)throws Exception{
        LocalDeviceDocuments.requireReadable(uri);byte[] bytes;try(InputStream input=context.getContentResolver().openInputStream(uri)){bytes=LocalOpenFile.bounded(input,32*1024*1024);}
        return preview(context,bytes);
    }
    static Result preview(Context context,byte[] bytes)throws Exception{
        WordPageFile document=new WordPageFile(archive(bytes));File target=File.createTempFile("word-pages-",".pdf",context.getCacheDir());boolean done=false;
        try{
            document.prepare();document.render(target);com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context.getApplicationContext());LocalPdfPreview.Document pdf=new LocalPdfPreview.Document(target);try{List<String> pageSources=new ArrayList<>();for(StringBuilder page:document.pageText)pageSources.add(page.toString());pdf.sourceText(pageSources);List<PdfTextLayer> layers=new ArrayList<>();for(List<PdfTextLayer.Glyph> glyphs:document.pageGlyphs)layers.add(new PdfTextLayer(glyphs));pdf.sourceLayers(layers);}catch(Exception | OutOfMemoryError failure){pdf.close();throw failure;}done=true;
            return new Result(pdf,document.plain.toString(),"Prepared-paper preview (approximate), not an original-layout guarantee.\n"+document.width+" × "+document.height+" pt; "+document.keptImages+" decoded image references.\n"+android.text.TextUtils.join("\n",document.warnings));
        }finally{document.releaseImages();if(!done)target.delete();}
    }
    private static Map<String,byte[]> archive(byte[] bytes)throws Exception{
        if(bytes==null || bytes.length>32*1024*1024)throw new IOException("Word preview input exceeds 32 MiB");Map<String,byte[]> result=new LinkedHashMap<>();Set<String> names=new HashSet<>();int entries=0,total=0,retained=0;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){String name=entry.getName();if(name.length()>512 || ++entries>4096 || !names.add(name) || name.startsWith("/") || name.contains("\\") || Arrays.asList(name.split("/")).contains(".."))throw new IOException("Unsafe Word archive");
            boolean keep=name.equals("word/document.xml") || name.equals("word/styles.xml") || name.matches("word/(?:header|footer)[^/]*\\.xml") || name.startsWith("word/_rels/") || name.startsWith("word/media/");ByteArrayOutputStream out=keep?new ByteArrayOutputStream():null;int size=0;byte[] buffer=new byte[8192];
            for(int n;(n=zip.read(buffer))!=-1;){size+=n;total+=n;if(size>16*1024*1024 || total>64*1024*1024)throw new IOException("Word decompression safety limit");if(keep){retained+=n;if(retained>32*1024*1024)throw new IOException("Word preview part budget exceeded");out.write(buffer,0,n);}}if(keep)result.put(name,out.toByteArray());
        }}return result;
    }
    private XmlPullParser parser(byte[] bytes)throws Exception{
        if(bytes==null)throw new IOException("Required Word body/relationship part missing");String value=LocalOpenFile.plain(bytes),upper=value.toUpperCase(Locale.ROOT);if(upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY"))throw new IOException("External XML declarations are not allowed");XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);p.setInput(new StringReader(value));return p;
    }
    private static String attr(XmlPullParser p,String name){String value=p.getAttributeValue(W,name);if(value==null)value=p.getAttributeValue(SW,name);return value==null?p.getAttributeValue(null,name):value;}
    private static String relation(XmlPullParser p,String name){String value=p.getAttributeValue(R,name);return value==null?p.getAttributeValue(SR,name):value;}
    private static boolean word(XmlPullParser p){return W.equals(p.getNamespace()) || SW.equals(p.getNamespace());}
    private static float number(String value,float fallback,float minimum,float maximum){try{float parsed=Float.parseFloat(value);return Float.isFinite(parsed)?Math.max(minimum,Math.min(maximum,parsed)):fallback;}catch(Exception invalid){return fallback;}}
    private Map<String,String> relationships(String part)throws Exception{
        String parent=part.substring(0,part.lastIndexOf('/')+1),name=part.substring(part.lastIndexOf('/')+1);byte[] bytes=files.get(parent+"_rels/"+name+".rels");Map<String,String> links=new HashMap<>();int relationshipCount=0;if(bytes==null)return links;XmlPullParser p=parser(bytes);int depth=0;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Word XML nesting limit");if(p.getName().equals("Relationship") && ++relationshipCount>16384)throw new IOException("Word relationship safety limit");if(p.getName().equals("Relationship") && !"External".equals(p.getAttributeValue(null,"TargetMode"))){String id=p.getAttributeValue(null,"Id"),target=p.getAttributeValue(null,"Target");if(id!=null && target!=null){java.net.URI resolved=java.net.URI.create("/"+part).resolve(target.replace(" ","%20"));if(resolved.getScheme()==null && resolved.getAuthority()==null && resolved.getPath().startsWith("/word/"))links.put(id,resolved.getPath().substring(1));}}}else if(event==XmlPullParser.END_TAG)depth--;}
        return links;
    }
    private void prepare()throws Exception{
        Map<String,String> links=relationships("word/document.xml");XmlPullParser p=parser(files.get("word/document.xml"));int depth=0;boolean rootSeen=false,bodySeen=false;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Word XML nesting limit");String tag=p.getName();if(depth==1 && tag.equals("document") && word(p))rootSeen=true;if(depth==2 && tag.equals("body") && word(p))bodySeen=true;if(!word(p))continue;if(tag.equals("pgSz")){width=number(attr(p,"w"),11900,4320,40000)/20;height=number(attr(p,"h"),16840,4320,40000)/20;}else if(tag.equals("pgMar")){left=number(attr(p,"left"),1440,0,4000)/20;right=number(attr(p,"right"),1440,0,4000)/20;top=number(attr(p,"top"),1440,0,4000)/20;bottom=number(attr(p,"bottom"),1440,0,4000)/20;}else if(tag.equals("headerReference") && (headerPart==null || "default".equals(attr(p,"type"))))headerPart=links.get(relation(p,"id"));else if(tag.equals("footerReference") && (footerPart==null || "default".equals(attr(p,"type"))))footerPart=links.get(relation(p,"id"));}else if(event==XmlPullParser.END_TAG)depth--;}
        if(!rootSeen || !bodySeen)throw new IOException("Not a supported Word document body; use Open original. Original unchanged");
        if(width-left-right<72 || height-top-bottom<72)throw new IOException("Word margins leave no safe preview area");
        // Only the document default size is reused; arbitrary styles remain explicitly approximate.
        byte[] styles=files.get("word/styles.xml");if(styles!=null){p=parser(styles);depth=0;boolean defaults=false;for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Word style nesting limit");if(p.getName().equals("docDefaults"))defaults=true;else if(defaults && p.getName().equals("sz"))defaultSize=number(attr(p,"val"),22,8,144)/2;}else if(event==XmlPullParser.END_TAG){if(p.getName().equals("docDefaults"))defaults=false;depth--;}}}
        body.addAll(part("word/document.xml",true));if(body.isEmpty())body.add(new Paragraph());
    }
    private Bitmap image(String name)throws Exception{
        if(name==null || !files.containsKey(name)){warnings.add("An image relationship was missing; its placeholder is shown.");return null;}if(images.containsKey(name))return images.get(name);String lower=name.toLowerCase(Locale.ROOT);
        if(!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp"))){warnings.add("Unsupported image/vector type omitted; original file still contains it.");return null;}
        byte[] bytes=files.get(name);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);long count=(long)bounds.outWidth*bounds.outHeight;
        if(bounds.outWidth<1 || bounds.outHeight<1 || count>4000000 || pixels+count>8000000){warnings.add("Oversized images omitted (4 MP per image / 8 MP total safety budget).");return null;}
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap==null){warnings.add("An image could not be decoded; original retained.");return null;}pixels+=count;images.put(name,bitmap);allocated.add(bitmap);return bitmap;
    }
    private void append(Paragraph paragraph,String value,boolean bold,boolean italic,boolean underline,float size,int color,String family)throws Exception{
        if(paragraph.text.length()+value.length()>50000)throw new IOException("A Word paragraph exceeds the 50,000 character shaping safety limit; original unchanged");if((characters+=value.length())>1000000)throw new IOException("Word page preview exceeds one million characters; original unchanged");int start=paragraph.text.length();paragraph.text.append(value);int end=paragraph.text.length();if(end==start)return;
        paragraph.text.setSpan(new AbsoluteSizeSpan(Math.round(size)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(bold || italic)paragraph.text.setSpan(new StyleSpan((bold?Typeface.BOLD:0)|(italic?Typeface.ITALIC:0)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(underline)paragraph.text.setSpan(new UnderlineSpan(),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);paragraph.text.setSpan(new ForegroundColorSpan(color),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(family!=null)paragraph.text.setSpan(new TypefaceSpan(family),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    private List<Paragraph> part(String name,boolean collectText)throws Exception{
        List<Paragraph> result=new ArrayList<>();if(name==null || !files.containsKey(name))return result;Map<String,String> links=relationships(name);XmlPullParser p=parser(files.get(name));Paragraph paragraph=null;Deque<Paragraph> parents=new ArrayDeque<>();int depth=0,deleted=0;boolean bold=false,italic=false,underline=false;float size=defaultSize,imageWidth=0,imageHeight=0;int color=Color.BLACK;String family=null;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){
            if(event==XmlPullParser.START_TAG){if(++depth>64)throw new IOException("Word XML nesting limit");String tag=p.getName();boolean wordTag=word(p);if(wordTag && tag.equals("del")){deleted=depth;continue;}if(deleted>0)continue;
                if(wordTag && tag.equals("p")){if(++paragraphs>20000)throw new IOException("Word paragraph safety limit");if(paragraph!=null)parents.push(paragraph);paragraph=new Paragraph();}
                else if(wordTag && tag.equals("r")){bold=false;italic=false;underline=false;size=defaultSize;color=Color.BLACK;family=null;imageWidth=imageHeight=0;}
                else if(paragraph!=null){
                    if(!wordTag && !tag.equals("extent") && !tag.equals("blip") && !tag.equals("imagedata"))continue;
                    if(tag.equals("b"))bold=!"0".equals(attr(p,"val")) && !"false".equals(attr(p,"val"));else if(tag.equals("i"))italic=!"0".equals(attr(p,"val")) && !"false".equals(attr(p,"val"));else if(tag.equals("u"))underline=!"none".equals(attr(p,"val"));else if(tag.equals("sz"))size=number(attr(p,"val"),defaultSize*2,8,144)/2;
                    else if(tag.equals("color")){String value=attr(p,"val");if(value!=null && value.matches("[a-fA-F0-9]{6}"))color=Color.parseColor("#"+value);}
                    else if(tag.equals("rFonts")){String value=attr(p,"ascii");if(value!=null){String font=value.toLowerCase(Locale.ROOT);family=font.contains("times") || font.contains("georgia")?"serif":font.contains("courier") || font.contains("consolas")?"monospace":"sans-serif";}}
                    else if(tag.equals("jc")){String align=attr(p,"val");paragraph.align="center".equals(align)?Layout.Alignment.ALIGN_CENTER:"right".equals(align)?Layout.Alignment.ALIGN_OPPOSITE:Layout.Alignment.ALIGN_NORMAL;}
                    else if(tag.equals("spacing")){paragraph.before=number(attr(p,"before"),0,0,1440)/20;paragraph.after=number(attr(p,"after"),0,0,1440)/20;String line=attr(p,"line"),rule=attr(p,"lineRule");if(line!=null){float amount=number(line,240,1,7200);if(rule==null || rule.equals("auto"))paragraph.lineMultiple=Math.max(.25f,Math.min(10,amount/240f));else{paragraph.lineHeight=amount/20;paragraph.atLeast="atLeast".equals(rule);warnings.add("Authored fixed/minimum line height uses substituted Android font metrics.");}}}
                    else if(tag.equals("sym")){warnings.add("Legacy symbol-font glyph cannot be mapped faithfully; placeholder shown. Unicode text symbols remain as authored.");append(paragraph,"[Symbol]",bold,italic,underline,size,color,family);}
                    else if(tag.equals("t")){String value=p.nextText();depth--;append(paragraph,value,bold,italic,underline,size,color,family);}
                    else if(tag.equals("tab"))append(paragraph,"\t",bold,italic,underline,size,color,family);
                    else if(tag.equals("extent")){imageWidth=number(p.getAttributeValue(null,"cx"),0,0,25400000)/12700;imageHeight=number(p.getAttributeValue(null,"cy"),0,0,25400000)/12700;}
                    else if(tag.equals("blip") || tag.equals("imagedata")){String id=relation(p,tag.equals("blip")?"embed":"id");Bitmap bitmap=image(links.get(id));if(bitmap==null)append(paragraph,"[Image omitted — use original reader]",false,false,false,defaultSize,Color.DKGRAY,null);else{float w=imageWidth>0?imageWidth:bitmap.getWidth()*.75f,h=imageHeight>0?imageHeight:bitmap.getHeight()*.75f;float scale=Math.min(1,Math.min((width-left-right)/Math.max(1,w),(height-top-bottom-32)/Math.max(1,h)));if(scale<1)warnings.add("An inline image was scaled to fit paper bounds; original file unchanged.");int at=paragraph.text.length();paragraph.text.append('\uFFFC');ImageSpan span=new ImageSpan(bitmap);span.getDrawable().setBounds(0,0,Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));paragraph.text.setSpan(span,at,at+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);keptImages++;}}
                    else if(tag.equals("lastRenderedPageBreak") || (tag.equals("pageBreakBefore") && !"0".equals(attr(p,"val")) && !"false".equals(attr(p,"val"))) || (tag.equals("br") && "page".equals(attr(p,"type")))){if(paragraph.text.length()>0){result.add(paragraph);if(collectText)plain.append(paragraph.text.toString()).append('\n');}Paragraph next=new Paragraph();next.pageBreak=true;next.align=paragraph.align;paragraph=next;}
                    else if(tag.equals("br"))append(paragraph,"\n",bold,italic,underline,size,color,family);
                }
                if(tag.equals("tbl"))warnings.add("Tables are shown in cell/paragraph order, not original merged-cell geometry.");
            }else if(event==XmlPullParser.END_TAG){boolean hidden=deleted>0;if(deleted==depth)deleted=0;if(!hidden && word(p) && p.getName().equals("p") && paragraph!=null){result.add(paragraph);if(collectText)plain.append(paragraph.text.toString()).append('\n');paragraph=parents.isEmpty()?null:parents.pop();}depth--;}
        }return result;
    }
    private static final class AuthoredLineHeight implements LineHeightSpan {
        final int height;final boolean atLeast;AuthoredLineHeight(float height,boolean atLeast){this.height=Math.max(1,Math.round(height));this.atLeast=atLeast;}
        @Override public void chooseHeight(CharSequence text,int start,int end,int spanStart,int v,Paint.FontMetricsInt fm){int natural=fm.descent-fm.ascent;if(natural<1 || (atLeast && natural>=height))return;int descent=Math.round(fm.descent*height/(float)natural);fm.ascent=descent-height;fm.descent=descent;fm.top=fm.ascent;fm.bottom=fm.descent;}
    }
    private CharSequence authoredText(Paragraph p){SpannableStringBuilder result=new SpannableStringBuilder(p.text);if(p.lineHeight>0 && result.length()>0)result.setSpan(new AuthoredLineHeight(p.lineHeight,p.atLeast),0,result.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return result;}
    private StaticLayout layout(CharSequence text,Layout.Alignment alignment,int columns){return layout(text,alignment,columns,1,0);}
    private StaticLayout layout(CharSequence text,Layout.Alignment alignment,int columns,float multiple,float extra){TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(defaultSize);paint.setColor(Color.BLACK);return StaticLayout.Builder.obtain(text,0,text.length(),paint,columns).setAlignment(alignment).setIncludePad(false).setLineSpacing(extra,multiple).build();}
    private void render(File target)throws Exception{
        int contentWidth=(int)(width-left-right);SpannableStringBuilder header=new SpannableStringBuilder(),footer=new SpannableStringBuilder();for(Paragraph p:part(headerPart,false)){int at=header.length();header.append(p.text).append('\n');header.setSpan(new AlignmentSpan.Standard(p.align),at,header.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}for(Paragraph p:part(footerPart,false)){int at=footer.length();footer.append(p.text).append('\n');footer.setSpan(new AlignmentSpan.Standard(p.align),at,footer.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}pageHeader=header.toString();pageFooter=footer.toString();String sourceBody=plain.toString();plain.setLength(0);plain.append(header).append('\n').append(sourceBody).append(footer);StaticLayout headerLayout=header.length()==0?null:layout(header,Layout.Alignment.ALIGN_NORMAL,contentWidth),footerLayout=footer.length()==0?null:layout(footer,Layout.Alignment.ALIGN_NORMAL,contentWidth);
        float bodyTop=Math.max(top,headerLayout==null?top:headerLayout.getHeight()+20),bodyBottom=Math.min(height-bottom,footerLayout==null?height-bottom:height-footerLayout.getHeight()-20);if(bodyBottom-bodyTop<72)throw new IOException("Word header/footer leaves no safe body area; use original reader");
        PdfDocument pdf=new PdfDocument();PdfDocument.Page page=null;int pageNumber=0;float y=bodyTop;
        try{
            for(Paragraph paragraph:body){StaticLayout text=layout(authoredText(paragraph),paragraph.align,contentWidth,paragraph.lineMultiple,paragraph.lineExtra);if(page==null || paragraph.pageBreak){if(page!=null)finishPage(pdf,page,footerLayout);page=newPage(pdf,++pageNumber,headerLayout,footerLayout);y=bodyTop;}y+=paragraph.before;int line=0;
                while(line<text.getLineCount()){
                    int end=line;while(end<text.getLineCount() && text.getLineBottom(end)-text.getLineTop(line)<=bodyBottom-y)end++;
                    if(end==line){if(y<=bodyTop+Math.max(1,paragraph.before)){
                        float lineHeight=text.getLineBottom(line)-text.getLineTop(line);
                        float fit=Math.min(1,(bodyBottom-bodyTop)/Math.max(1,lineHeight));
                        Canvas canvas=page.getCanvas();canvas.save();canvas.clipRect(left,bodyTop,width-right,bodyBottom);canvas.translate(left,bodyTop);canvas.scale(fit,fit);canvas.translate(0,-text.getLineTop(line));text.draw(canvas);canvas.restore();
                        recordText(text,line,line+1,left,bodyTop-text.getLineTop(line)*fit,fit);pageText.get(pageNumber-1).append(text.getText(),text.getLineStart(line),text.getLineEnd(line)).append('\n');
                        warnings.add("Oversized line/image scaled to fit the available paper body.");y=bodyTop+lineHeight*fit;line++;continue;
                    }finishPage(pdf,page,footerLayout);page=newPage(pdf,++pageNumber,headerLayout,footerLayout);y=bodyTop;continue;}
                    float block=text.getLineBottom(end-1)-text.getLineTop(line);Canvas canvas=page.getCanvas();canvas.save();canvas.clipRect(left,y,width-right,y+block);canvas.translate(left,y-text.getLineTop(line));text.draw(canvas);canvas.restore();recordText(text,line,end,left,y-text.getLineTop(line),1);pageText.get(pageNumber-1).append(text.getText(),text.getLineStart(line),text.getLineEnd(end-1)).append('\n');y+=block;line=end;if(line<text.getLineCount()){finishPage(pdf,page,footerLayout);page=newPage(pdf,++pageNumber,headerLayout,footerLayout);y=bodyTop;}
                }y+=paragraph.after;
            }
            if(page!=null){finishPage(pdf,page,footerLayout);page=null;}for(StringBuilder source:pageText)source.append(pageFooter);try(OutputStream file=new FileOutputStream(target);OutputStream bounded=new FilterOutputStream(file){long count;@Override public void write(int b)throws IOException{if(++count>32L*1024*1024)throw new IOException("Rendered Word preview exceeds 32 MiB safety limit");out.write(b);}@Override public void write(byte[] b,int off,int length)throws IOException{if((count+=length)>32L*1024*1024)throw new IOException("Rendered Word preview exceeds 32 MiB safety limit");out.write(b,off,length);}}){pdf.writeTo(bounded);}
        }finally{if(page!=null)try{finishPage(pdf,page,footerLayout);}catch(RuntimeException ignored){}pdf.close();}
    }
    /** Geometry is taken from the same shaped Layout that is painted, not re-OCRed or guessed from a rendered PDF font. */
    private void recordText(StaticLayout layout,int first,int last,float x,float y,float scale){
        List<PdfTextLayer.Glyph> glyphs=pageGlyphs.get(pageGlyphs.size()-1);CharSequence value=layout.getText();
        java.text.BreakIterator characters=java.text.BreakIterator.getCharacterInstance(Locale.ROOT);characters.setText(value.toString());
        int start=layout.getLineStart(first),limit=layout.getLineEnd(last-1);android.graphics.Path path=new android.graphics.Path();RectF rect=new RectF();
        for(int at=start;at<limit;){int end=characters.following(at);if(end==java.text.BreakIterator.DONE || end>limit)end=limit;
            if(selectionGlyphs>=50000 || glyphs.size()>=5000){warnings.add("On-page selection is bounded for very large documents; Page text retains the prepared source text.");break;}
            String cluster=value.subSequence(at,end).toString();boolean image=value instanceof Spanned && ((Spanned)value).getSpans(at,end,ImageSpan.class).length>0;
            if(!image && !cluster.equals("\n") && !cluster.equals("\r")){path.reset();layout.getSelectionPath(at,end,path);path.computeBounds(rect,true);if(!rect.isEmpty()){
                glyphs.add(new PdfTextLayer.Glyph(cluster,(x+rect.left*scale)/width,(y+rect.top*scale)/height,(x+rect.right*scale)/width,(y+rect.bottom*scale)/height));selectionGlyphs++;
            }}at=end;
        }
    }
    private void finishPage(PdfDocument pdf,PdfDocument.Page page,StaticLayout footer){if(footer!=null)recordText(footer,0,footer.getLineCount(),left,height-footer.getHeight()-10,1);pdf.finishPage(page);}
    private PdfDocument.Page newPage(PdfDocument pdf,int number,StaticLayout header,StaticLayout footer)throws IOException{
        if(number>1000)throw new IOException("Word preview exceeds 1,000 pages; original unchanged");pageText.add(new StringBuilder(pageHeader));pageGlyphs.add(new ArrayList<>());PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(Math.round(width),Math.round(height),number).create());try{Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);if(header!=null){canvas.save();canvas.translate(left,10);header.draw(canvas);canvas.restore();recordText(header,0,header.getLineCount(),left,10,1);}if(footer!=null){canvas.save();canvas.translate(left,height-footer.getHeight()-10);footer.draw(canvas);canvas.restore();}return page;}catch(RuntimeException | Error failure){try{pdf.finishPage(page);}catch(RuntimeException ignored){}throw failure;}
    }
    private void releaseImages(){for(Bitmap bitmap:allocated)if(!bitmap.isRecycled())bitmap.recycle();allocated.clear();images.clear();}
}
