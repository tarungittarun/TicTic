package app.space.notes.runtime;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.*;
import java.util.*;
import app.space.notes.domain.Note;

public final class TextStyles {
    private TextStyles() { }
    private static final class Heading extends RelativeSizeSpan {
        final String type;
        Heading(String type){super(type.equals("h1")?1.55f:1.25f);this.type=type;}
    }
    public static List<Note.Mark> read(Spanned text) {
        List<Note.Mark> out=new ArrayList<>();
        for(Object span:text.getSpans(0,text.length(),Object.class)) {
            int start=text.getSpanStart(span),end=text.getSpanEnd(span);
            if(start<0 || end<=start || end>text.length())continue;
            if(span instanceof StyleSpan) {
                int style=((StyleSpan)span).getStyle();
                if((style&Typeface.BOLD)!=0)out.add(new Note.Mark("bold",start,end));
                if((style&Typeface.ITALIC)!=0)out.add(new Note.Mark("italic",start,end));
            } else if(span instanceof UnderlineSpan)out.add(new Note.Mark("underline",start,end));
            else if(span instanceof StrikethroughSpan)out.add(new Note.Mark("strike",start,end));
            else if(span instanceof Heading)out.add(new Note.Mark(((Heading)span).type,start,end));
        }
        out.sort(Comparator.comparing((Note.Mark m)->m.type).thenComparingInt(m->m.start).thenComparingInt(m->m.end));return out;
    }
    public static void write(Spannable text,List<Note.Mark> marks) {
        for(Object span:text.getSpans(0,text.length(),Object.class))
            if(span instanceof StyleSpan || span instanceof UnderlineSpan || span instanceof StrikethroughSpan || span instanceof Heading)text.removeSpan(span);
        for(Note.Mark mark:marks)writeRange(text,mark,0);
    }
    public static void writeRange(Spannable text,Note.Mark mark,int offset) {
            Object span;
            switch(mark.type) {
                case "bold":span=new StyleSpan(Typeface.BOLD);break;
                case "italic":span=new StyleSpan(Typeface.ITALIC);break;
                case "underline":span=new UnderlineSpan();break;
                case "strike":span=new StrikethroughSpan();break;
                default:span=new Heading(mark.type);
            }
            if(offset>=0 && mark.end+offset<=text.length())text.setSpan(span,mark.start+offset,mark.end+offset,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
