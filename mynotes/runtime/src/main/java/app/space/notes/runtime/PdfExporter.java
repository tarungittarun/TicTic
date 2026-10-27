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
                android.text.SpannableStringBuilder content=new android.text.SpannableStringBuilder(note.title.isEmpty()?"Untitled":note.title);
                content.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,content.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);content.append("\n\n");int offset=content.length();content.append(note.plainText());
                if(!note.checklist){for(Note.Mark mark:note.marks)TextStyles.writeRange(content,mark,offset);}
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
                if(!note.diagram.nodes.isEmpty()){
                    if(pageNumber>=200)throw new IOException("PDF export exceeds 200 pages; split the selection");
                    PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNumber).create());Canvas c=page.getCanvas();c.save();c.translate(48,72);c.scale(499f/800,499f/800);DiagramRenderer.draw(c,note.diagram,false,null);c.restore();
                    TextPaint title=new TextPaint(Paint.ANTI_ALIAS_FLAG);title.setColor(Color.BLACK);title.setTextSize(14);c.drawText("Diagram · "+(note.title.length()>50?note.title.substring(0,50)+"…":note.title),48,40,title);footer(c,pageNumber);pdf.finishPage(page);
                }
            }
            pdf.writeTo(output);output.flush();return pageNumber;
        } finally {pdf.close();}
    }
    private static void footer(Canvas canvas,int number){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.DKGRAY);p.setTextSize(10);canvas.drawText("MyNotes · "+number,48,813,p);}
}
