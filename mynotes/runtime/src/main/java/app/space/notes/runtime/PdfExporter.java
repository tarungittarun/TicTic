package app.space.notes.runtime;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.text.*;
import java.io.*;
import java.util.List;
import app.space.notes.domain.Note;

/** Paged vector export. Work is performed by the controller's disk executor, never the UI. */
public final class PdfExporter {
    private PdfExporter() { }
    public static int write(List<Note> notes,OutputStream output) throws Exception {return write(notes,output,false);}
    public static int write(List<Note> notes,OutputStream output,boolean printGuides) throws Exception {
        if(notes.isEmpty())throw new IOException("No notes selected");int pageNumber=0;
        PdfDocument pdf=new PdfDocument();java.util.List<PdfLinks.Link> links=new java.util.ArrayList<>();File temporary=null;
        try{
            for(Note note:notes){
                if(note.content!=null){pageNumber=canonical(pdf,note,pageNumber,links,printGuides);continue;}
                android.text.SpannableStringBuilder content=new android.text.SpannableStringBuilder(note.title.isEmpty()?"Untitled":note.title);
                content.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,content.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);content.append("\n\n");int offset=content.length();content.append(note.plainText());
                if(!note.checklist){for(Note.Mark mark:app.space.notes.domain.TextFormatting.normalizeParagraphs(note.marks,note.body))TextStyles.writeRange(content,mark,offset);}
                if(!note.labels.isEmpty())content.append("\n\nLabels: ").append(String.join(", ",note.labels));
                TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.linkColor=Color.rgb(32,71,146);paint.setTextSize(14);
                StaticLayout layout=StaticLayout.Builder.obtain(content,0,content.length(),paint,499).setIncludePad(false).setLineSpacing(3,1).build();
                int first=0;
                while(first<layout.getLineCount()){
                    if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection. Notes remain unchanged.");
                    int top=layout.getLineTop(first),last=first;
                    while(last+1<layout.getLineCount() && layout.getLineBottom(last+1)-top<=710)last++;
                    PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();
                    c.save();c.clipRect(48,48,547,48+layout.getLineBottom(last)-top);c.translate(48,48-top);layout.draw(c);c.restore();recordLinks(links,pageNumber,layout,first,last,48,48-top,48,48,547,48+layout.getLineBottom(last)-top);footer(c,pageNumber);pdf.finishPage(page);first=last+1;
                }
                for(app.space.notes.domain.NoteImage image:note.images){if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection");Bitmap bitmap=NoteImageFiles.decode(image);if(bitmap==null)throw new IOException("An image could not be decoded; no complete PDF was produced");try{PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();float imageTop=48,availableHeight=710;String caption=note.contentNames.get("image:"+image.id);if(caption!=null){TextPaint captionPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);captionPaint.setColor(Color.BLACK);captionPaint.setTextSize(14);StaticLayout captionLayout=StaticLayout.Builder.obtain(caption,0,caption.length(),captionPaint,499).setIncludePad(false).build();c.save();c.translate(48,48);captionLayout.draw(c);c.restore();imageTop+=captionLayout.getHeight()+12;availableHeight-=captionLayout.getHeight()+12;}float scale=Math.min(499f/bitmap.getWidth(),availableHeight/bitmap.getHeight());float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;c.drawBitmap(bitmap,null,new RectF(48+(499-width)/2,imageTop,48+(499+width)/2,imageTop+height),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));footer(c,pageNumber);pdf.finishPage(page);}finally{bitmap.recycle();}}
                if(!note.diagram.nodes.isEmpty()){
                    if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection");
                    PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();c.save();c.translate(48,72);c.scale(499f/800,499f/800);DiagramRenderer.draw(c,note.diagram,false,null);c.restore();
                    TextPaint title=new TextPaint(Paint.ANTI_ALIAS_FLAG);title.setColor(Color.BLACK);title.setTextSize(14);String caption=note.contentName("diagram","Diagram · "+note.title);c.drawText(android.text.TextUtils.ellipsize(caption,title,499,android.text.TextUtils.TruncateAt.END).toString(),48,40,title);footer(c,pageNumber);pdf.finishPage(page);
                }
            }
            temporary=File.createTempFile("mynotes-linked-pdf-",".pdf");try(OutputStream staging=new FileOutputStream(temporary)){pdf.writeTo(staging);}PdfLinks.annotate(temporary,links);try(InputStream in=new FileInputStream(temporary)){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)output.write(buffer,0,count);}output.flush();return pageNumber;
        } catch(OutOfMemoryError pressure){throw new IOException("Not enough memory to export PDF safely; notes unchanged",pressure);} finally {pdf.close();if(temporary!=null)temporary.delete();}
    }
    private static int canonical(PdfDocument pdf,Note note,int first,java.util.List<PdfLinks.Link> links,boolean printGuides)throws Exception{
        Flow flow=new Flow(pdf,first,links);flow.text(new app.space.notes.domain.RichText(note.title.isEmpty()?"Untitled":note.title,java.util.Collections.emptyList()));
        flow.rules=printGuides && note.content.writingLines;
        for(app.space.notes.domain.DocumentContent.Block block:note.content.blocks){
            switch(block.kind){
                case TEXT:flow.text(block.text);break;
                case CHECKLIST:
                    if(!block.name.isEmpty())flow.text(new app.space.notes.domain.RichText(block.name,java.util.Collections.emptyList()));
                    for(Note.Item item:block.items){if(block.hideChecked && item.checked)continue;String prefix=String.join("",java.util.Collections.nCopies(item.depth,"  "))+(item.checked?"[x] ":"[ ] ");java.util.List<Note.Mark> marks=new java.util.ArrayList<>();for(Note.Mark mark:item.marks)marks.add(new Note.Mark(mark.type,mark.start+prefix.length(),mark.end+prefix.length(),mark.value));flow.text(new app.space.notes.domain.RichText(prefix+item.text,marks));}break;
                case IMAGE:case DRAWING:flow.text(new app.space.notes.domain.RichText(block.name,java.util.Collections.emptyList()));flow.image(block.image);break;
                case DIAGRAM:flow.text(new app.space.notes.domain.RichText(block.name,java.util.Collections.emptyList()));flow.diagram(block.diagram);break;
                case TABLE:flow.text(new app.space.notes.domain.RichText(block.name,java.util.Collections.emptyList()));flow.table(block.table);break;
            }
        }
        if(!note.labels.isEmpty())flow.text(new app.space.notes.domain.RichText("Labels: "+String.join(", ",note.labels),java.util.Collections.emptyList()));
        flow.finish();return flow.number;
    }
    private static final class Flow {
        final PdfDocument pdf;final java.util.List<PdfLinks.Link> links;boolean rules;int number;PdfDocument.Page page;Canvas canvas;float y=48;
        Flow(PdfDocument pdf,int number,java.util.List<PdfLinks.Link> links){this.pdf=pdf;this.number=number;this.links=links;}
        void next()throws IOException{finish();if(number>=200)throw new IOException("PDF exceeds 200 pages; notes remain unchanged");page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++number).create());canvas=page.getCanvas();y=48;}
        void finish(){if(page!=null){footer(canvas,number);pdf.finishPage(page);page=null;canvas=null;}}
        void space(float height)throws IOException{if(page==null || y+height>758)next();}
        StaticLayout layout(CharSequence text,int width){TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.linkColor=Color.rgb(32,71,146);paint.setTextSize(14);return StaticLayout.Builder.obtain(text,0,text.length(),paint,Math.max(1,width)).setIncludePad(false).setLineSpacing(3,1).build();}
        CharSequence styled(app.space.notes.domain.RichText rich){
            SpannableStringBuilder text=new SpannableStringBuilder(rich.text);
            for(Note.Mark mark:rich.marks){if(mark.type.equals("number"))continue;if(mark.type.equals("size"))text.setSpan(new android.text.style.AbsoluteSizeSpan(Integer.parseInt(mark.value)),mark.start,mark.end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);else TextStyles.writeRange(text,mark,0);}
            java.util.List<Note.Mark> numbering=new java.util.ArrayList<>();for(Note.Mark mark:rich.marks)if(mark.type.equals("number") || mark.type.equals("link"))numbering.add(mark);numbering.sort((a,b)->Integer.compare(b.type.equals("number")?b.start:b.end,a.type.equals("number")?a.start:a.end));for(Note.Mark mark:numbering)if(mark.type.equals("number"))text.insert(mark.start,mark.value+". ");else if(!rich.text.substring(mark.start,mark.end).equals(mark.value))text.insert(mark.end," ("+mark.value+")");
            return text;
        }
        void text(app.space.notes.domain.RichText rich)throws Exception{
            StaticLayout layout=layout(styled(rich),499);int line=0;
            while(line<layout.getLineCount()){space(Math.min(710,layout.getLineBottom(line)-layout.getLineTop(line)));int top=layout.getLineTop(line),last=line;while(last+1<layout.getLineCount() && layout.getLineBottom(last+1)-top<=758-y)last++;float height=Math.min(758-y,layout.getLineBottom(last)-top);canvas.save();canvas.clipRect(48,y,547,y+height);canvas.translate(48,y-top);if(rules)guides(canvas,layout,line,last,0,499);layout.draw(canvas);canvas.restore();recordLinks(links,number,layout,line,last,48,y-top,48,y,547,y+height);y+=height;line=last+1;if(line<layout.getLineCount())next();}y+=12;
        }
        void image(app.space.notes.domain.NoteImage image)throws Exception{Bitmap bitmap=NoteImageFiles.decode(image);if(bitmap==null)throw new IOException("Image could not be decoded; no complete PDF produced");try{float scale=Math.min(499f/bitmap.getWidth(),650f/bitmap.getHeight()),width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;space(height);canvas.drawBitmap(bitmap,null,new RectF(48+(499-width)/2,y,48+(499+width)/2,y+height),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));y+=height+16;}finally{bitmap.recycle();}}
        void diagram(app.space.notes.domain.Diagram diagram)throws Exception{float height=1000*499f/800;space(height);canvas.save();canvas.translate(48,y);canvas.scale(499f/800,499f/800);DiagramRenderer.draw(canvas,diagram,false,null);canvas.restore();y+=height+16;}
        void table(app.space.notes.domain.TextTable table)throws Exception{
            Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);border.setColor(Color.DKGRAY);border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(.5f);
            // Wide tables use legible column panels. Never cut glyphs at arbitrary vertical pixels.
            for(int first=0;first<table.columns;first+=4){
                int columns=Math.min(4,table.columns-first);float width=499f/columns;
                if(table.columns>4)text(new app.space.notes.domain.RichText("Columns "+(first+1)+"–"+(first+columns),java.util.Collections.emptyList()));
                for(int rowIndex=0;rowIndex<table.cells.size();rowIndex++){
                    java.util.List<app.space.notes.domain.RichText> row=table.cells.get(rowIndex);
                    java.util.List<StaticLayout> cells=new java.util.ArrayList<>();for(int column=first;column<first+columns;column++){SpannableStringBuilder value=new SpannableStringBuilder(styled(row.get(column)));if(table.hasHeader && rowIndex==0 && value.length()>0)value.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);cells.add(layout(value,(int)width-12));}
                    int[] line=new int[columns];boolean remaining=true;
                    while(remaining){
                        float minimum=0;for(int column=0;column<columns;column++){StaticLayout cell=cells.get(column);if(line[column]<cell.getLineCount())minimum=Math.max(minimum,cell.getLineBottom(line[column])-cell.getLineTop(line[column]));}
                        space(minimum+12);float available=758-y-12,height=0;int[] end=line.clone();
                        for(int column=0;column<columns;column++){StaticLayout cell=cells.get(column);if(line[column]>=cell.getLineCount())continue;int top=cell.getLineTop(line[column]);while(end[column]<cell.getLineCount() && cell.getLineBottom(end[column])-top<=available)end[column]++;if(end[column]==line[column])throw new IOException("A table line cannot fit on a PDF page; notes remain unchanged");height=Math.max(height,cell.getLineBottom(end[column]-1)-top);}
                        remaining=false;
                        for(int column=0;column<columns;column++){StaticLayout cell=cells.get(column);float x=48+column*width;canvas.drawRect(x,y,x+width,y+height+12,border);if(end[column]>line[column]){int top=cell.getLineTop(line[column]),bottom=cell.getLineBottom(end[column]-1);canvas.save();canvas.clipRect(x+6,y+6,x+width-6,y+6+bottom-top);canvas.translate(x+6,y+6-top);if(rules)guides(canvas,cell,line[column],end[column]-1,0,width-12);cell.draw(canvas);canvas.restore();recordLinks(links,number,cell,line[column],end[column]-1,x+6,y+6-top,x+6,y+6,x+width-6,y+6+bottom-top);}line[column]=end[column];remaining|=line[column]<cell.getLineCount();}
                        y+=height+12;if(remaining)next();
                    }
                }
                y+=12;
            }
        }
    }
    private static void guides(Canvas canvas,StaticLayout layout,int first,int last,float left,float right){Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.rgb(185,193,201));paint.setStrokeWidth(.5f);for(int line=first;line<=last;line++){float y=layout.getLineBaseline(line)+2;canvas.drawLine(left,y,right,y,paint);}}
    private static void recordLinks(java.util.List<PdfLinks.Link> links,int page,StaticLayout layout,int first,int last,float dx,float dy,float left,float top,float right,float bottom)throws IOException{
        if(!(layout.getText() instanceof Spanned))return;Spanned text=(Spanned)layout.getText();for(android.text.style.URLSpan span:text.getSpans(layout.getLineStart(first),layout.getLineEnd(last),android.text.style.URLSpan.class)){for(int line=first;line<=last;line++){int from=Math.max(text.getSpanStart(span),layout.getLineStart(line)),to=Math.min(text.getSpanEnd(span),layout.getLineVisibleEnd(line));if(from>=to)continue;Path path=new Path();layout.getSelectionPath(from,to,path);RectF rect=new RectF();path.computeBounds(rect,true);rect.offset(dx,dy);if(rect.intersect(left,top,right,bottom) && rect.width()>0 && rect.height()>0){if(links.size()>=8192)throw new IOException("Too many PDF link rectangles; split the export");links.add(new PdfLinks.Link(page,rect.left,rect.top,rect.right,rect.bottom,span.getURL()));}}}
    }
    private static void footer(Canvas canvas,int number){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.DKGRAY);p.setTextSize(10);canvas.drawText("MyNotes · "+number,48,813,p);}
}
