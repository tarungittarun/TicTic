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
    public static int write(List<Note> notes,OutputStream output) throws Exception {
        if(notes.isEmpty())throw new IOException("No notes selected");int pageNumber=0;
        PdfDocument pdf=new PdfDocument();
        try{
            for(Note note:notes){
                if(note.content!=null){pageNumber=canonical(pdf,note,pageNumber);continue;}
                android.text.SpannableStringBuilder content=new android.text.SpannableStringBuilder(note.title.isEmpty()?"Untitled":note.title);
                content.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,content.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);content.append("\n\n");int offset=content.length();content.append(note.plainText());
                if(!note.checklist){for(Note.Mark mark:app.space.notes.domain.TextFormatting.normalizeParagraphs(note.marks,note.body))TextStyles.writeRange(content,mark,offset);}
                if(!note.labels.isEmpty())content.append("\n\nLabels: ").append(String.join(", ",note.labels));
                TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(14);
                StaticLayout layout=StaticLayout.Builder.obtain(content,0,content.length(),paint,499).setIncludePad(false).setLineSpacing(3,1).build();
                int first=0;
                while(first<layout.getLineCount()){
                    if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection. Notes remain unchanged.");
                    int top=layout.getLineTop(first),last=first;
                    while(last+1<layout.getLineCount() && layout.getLineBottom(last+1)-top<=710)last++;
                    PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();
                    c.save();c.clipRect(48,48,547,48+layout.getLineBottom(last)-top);c.translate(48,48-top);layout.draw(c);c.restore();footer(c,pageNumber);pdf.finishPage(page);first=last+1;
                }
                for(app.space.notes.domain.NoteImage image:note.images){if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection");Bitmap bitmap=NoteImageFiles.decode(image);if(bitmap==null)throw new IOException("An image could not be decoded; no complete PDF was produced");try{PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();float scale=Math.min(499f/bitmap.getWidth(),710f/bitmap.getHeight());float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;c.drawBitmap(bitmap,null,new RectF(48+(499-width)/2,48,48+(499+width)/2,48+height),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));footer(c,pageNumber);pdf.finishPage(page);}finally{bitmap.recycle();}}
                if(!note.diagram.nodes.isEmpty()){
                    if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection");
                    PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();c.save();c.translate(48,72);c.scale(499f/800,499f/800);DiagramRenderer.draw(c,note.diagram,false,null);c.restore();
                    TextPaint title=new TextPaint(Paint.ANTI_ALIAS_FLAG);title.setColor(Color.BLACK);title.setTextSize(14);c.drawText("Diagram · "+(note.title.length()>50?note.title.substring(0,50)+"…":note.title),48,40,title);footer(c,pageNumber);pdf.finishPage(page);
                }
            }
            pdf.writeTo(output);output.flush();return pageNumber;
        } finally {pdf.close();}
    }
    private static int canonical(PdfDocument pdf,Note note,int first)throws Exception{
        Flow flow=new Flow(pdf,first);flow.text(new app.space.notes.domain.RichText(note.title.isEmpty()?"Untitled":note.title,java.util.Collections.emptyList()));
        for(app.space.notes.domain.DocumentContent.Block block:note.content.blocks){
            switch(block.kind){
                case TEXT:flow.text(block.text);break;
                case CHECKLIST:
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
        final PdfDocument pdf;int number;PdfDocument.Page page;Canvas canvas;float y=48;
        Flow(PdfDocument pdf,int number){this.pdf=pdf;this.number=number;}
        void next()throws IOException{finish();if(number>=200)throw new IOException("PDF exceeds 200 pages; notes remain unchanged");page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++number).create());canvas=page.getCanvas();y=48;}
        void finish(){if(page!=null){footer(canvas,number);pdf.finishPage(page);page=null;canvas=null;}}
        void space(float height)throws IOException{if(page==null || y+height>758)next();}
        StaticLayout layout(CharSequence text,int width){TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(14);return StaticLayout.Builder.obtain(text,0,text.length(),paint,Math.max(1,width)).setIncludePad(false).setLineSpacing(3,1).build();}
        CharSequence styled(app.space.notes.domain.RichText rich){
            SpannableStringBuilder text=new SpannableStringBuilder(rich.text);
            for(Note.Mark mark:rich.marks){if(mark.type.equals("number"))continue;if(mark.type.equals("size"))text.setSpan(new android.text.style.AbsoluteSizeSpan(Integer.parseInt(mark.value)),mark.start,mark.end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);else TextStyles.writeRange(text,mark,0);}
            java.util.List<Note.Mark> numbering=new java.util.ArrayList<>();for(Note.Mark mark:rich.marks)if(mark.type.equals("number"))numbering.add(mark);numbering.sort((a,b)->Integer.compare(b.start,a.start));for(Note.Mark mark:numbering)text.insert(mark.start,mark.value+". ");
            return text;
        }
        void text(app.space.notes.domain.RichText rich)throws Exception{
            StaticLayout layout=layout(styled(rich),499);int line=0;
            while(line<layout.getLineCount()){space(Math.min(710,layout.getLineBottom(line)-layout.getLineTop(line)));int top=layout.getLineTop(line),last=line;while(last+1<layout.getLineCount() && layout.getLineBottom(last+1)-top<=758-y)last++;float height=Math.min(758-y,layout.getLineBottom(last)-top);canvas.save();canvas.clipRect(48,y,547,y+height);canvas.translate(48,y-top);layout.draw(canvas);canvas.restore();y+=height;line=last+1;if(line<layout.getLineCount())next();}y+=12;
        }
        void image(app.space.notes.domain.NoteImage image)throws Exception{Bitmap bitmap=NoteImageFiles.decode(image);if(bitmap==null)throw new IOException("Image could not be decoded; no complete PDF produced");try{float scale=Math.min(499f/bitmap.getWidth(),650f/bitmap.getHeight()),width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;space(height);canvas.drawBitmap(bitmap,null,new RectF(48+(499-width)/2,y,48+(499+width)/2,y+height),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));y+=height+16;}finally{bitmap.recycle();}}
        void diagram(app.space.notes.domain.Diagram diagram)throws Exception{float height=1000*499f/800;space(height);canvas.save();canvas.translate(48,y);canvas.scale(499f/800,499f/800);DiagramRenderer.draw(canvas,diagram,false,null);canvas.restore();y+=height+16;}
        void table(app.space.notes.domain.TextTable table)throws Exception{
            float width=499f/table.columns;Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);border.setColor(Color.DKGRAY);border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(.5f);
            for(java.util.List<app.space.notes.domain.RichText> row:table.cells){java.util.List<StaticLayout> cells=new java.util.ArrayList<>();int total=0;for(app.space.notes.domain.RichText cell:row){StaticLayout layout=layout(styled(cell),(int)width-8);cells.add(layout);total=Math.max(total,layout.getHeight());}int offset=0;do{space(24);float height=Math.min(total-offset+8,758-y);for(int c=0;c<cells.size();c++){float x=48+c*width;canvas.drawRect(x,y,x+width,y+height,border);canvas.save();canvas.clipRect(x+4,y+4,x+width-4,y+height-4);canvas.translate(x+4,y+4-offset);cells.get(c).draw(canvas);canvas.restore();}offset+=(int)(height-8);y+=height;if(offset<total)next();}while(offset<total);}
            y+=12;
        }
    }
    private static void footer(Canvas canvas,int number){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.DKGRAY);p.setTextSize(10);canvas.drawText("MyNotes · "+number,48,813,p);}
}
