package app.space.notes.runtime;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.*;
import java.util.*;
import app.space.notes.domain.Note;

public final class TextStyles {
    private TextStyles() { }
    private static final class Heading extends RelativeSizeSpan implements LineHeightSpan.WithDensity {
        final String type;
        Heading(String type){super(type.equals("h1")?1.5f:1.25f);this.type=type;}
        @Override public void updateDrawState(android.text.TextPaint paint){super.updateDrawState(paint);paint.setTypeface(android.graphics.Typeface.create(paint.getTypeface(),(paint.getTypeface()==null?0:paint.getTypeface().getStyle())|Typeface.BOLD));}
        @Override public void updateMeasureState(android.text.TextPaint paint){super.updateMeasureState(paint);paint.setTypeface(android.graphics.Typeface.create(paint.getTypeface(),(paint.getTypeface()==null?0:paint.getTypeface().getStyle())|Typeface.BOLD));}
        @Override public void chooseHeight(CharSequence text,int start,int end,int spanStartV,int v,android.graphics.Paint.FontMetricsInt fm){
            int extra=Math.max(1,Math.round((fm.descent-fm.ascent)*0.12f));fm.descent+=extra;fm.bottom=Math.max(fm.bottom,fm.descent);
        }
        @Override public void chooseHeight(CharSequence text,int start,int end,int spanStartV,int v,android.graphics.Paint.FontMetricsInt fm,android.text.TextPaint paint){chooseHeight(text,start,end,spanStartV,v,fm);}

    }
    private static final class FontSize extends MetricAffectingSpan {
        final int size;FontSize(int size){this.size=size;}
        public void updateDrawState(android.text.TextPaint paint){paint.setTextSize(size*android.content.res.Resources.getSystem().getDisplayMetrics().scaledDensity);}
        public void updateMeasureState(android.text.TextPaint paint){updateDrawState(paint);}
    }
    private static final class Numbered implements LeadingMarginSpan {
        final String value;final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);final int margin;
        Numbered(String value){this.value=value;android.util.DisplayMetrics metrics=android.content.res.Resources.getSystem().getDisplayMetrics();paint.setTextSize(18*metrics.scaledDensity);margin=(int)Math.ceil(paint.measureText(value+".")+8*metrics.density);}
        public int getLeadingMargin(boolean first){return margin;}
        public void drawLeadingMargin(android.graphics.Canvas canvas,android.graphics.Paint original,int x,int dir,int top,int baseline,int bottom,CharSequence text,int start,int end,boolean first,android.text.Layout layout){
            if(first && text instanceof Spanned && ((Spanned)text).getSpanStart(this)==start){paint.setColor(original.getColor());float width=paint.measureText(value+".");canvas.drawText(value+".",dir>0?x:x-width,baseline,paint);}
        }
    }
    public static List<Note.Mark> read(Spanned text) {
        List<Note.Mark> out=new ArrayList<>();
        for(Object span:text.getSpans(0,text.length(),Object.class)) {
            if((text.getSpanFlags(span)&Spanned.SPAN_COMPOSING)!=0)continue;
            int start=text.getSpanStart(span),end=text.getSpanEnd(span);
            if(start<0 || end<=start || end>text.length())continue;
            if(span instanceof StyleSpan) {
                int style=((StyleSpan)span).getStyle();
                if((style&Typeface.BOLD)!=0)out.add(new Note.Mark("bold",start,end));
                if((style&Typeface.ITALIC)!=0)out.add(new Note.Mark("italic",start,end));
            } else if(span instanceof UnderlineSpan)out.add(new Note.Mark("underline",start,end));
            else if(span instanceof StrikethroughSpan)out.add(new Note.Mark("strike",start,end));
            else if(span instanceof TypefaceSpan)out.add(new Note.Mark("font",start,end,((TypefaceSpan)span).getFamily()));
            else if(span instanceof FontSize)out.add(new Note.Mark("size",start,end,Integer.toString(((FontSize)span).size)));
            else if(span instanceof URLSpan)out.add(new Note.Mark("link",start,end,((URLSpan)span).getURL()));
            else if(span instanceof Numbered)out.add(new Note.Mark("number",start,end,((Numbered)span).value));
            else if(span instanceof Heading)out.add(new Note.Mark(((Heading)span).type,start,end));
        }
        out.sort(Comparator.comparing((Note.Mark m)->m.type).thenComparingInt(m->m.start).thenComparingInt(m->m.end));return out;
    }
    public static void write(Spannable text,List<Note.Mark> marks) {
        for(Object span:text.getSpans(0,text.length(),Object.class))
            if((text.getSpanFlags(span)&Spanned.SPAN_COMPOSING)==0 && (span instanceof StyleSpan || span instanceof UnderlineSpan || span instanceof StrikethroughSpan || span instanceof Heading || span instanceof TypefaceSpan || span instanceof FontSize || span instanceof URLSpan || span instanceof Numbered))text.removeSpan(span);
        for(Note.Mark mark:app.space.notes.domain.TextFormatting.normalizeParagraphs(marks,text.toString()))writeRange(text,mark,0);
        for(Heading span:text.getSpans(0,text.length(),Heading.class))text.setSpan(span,text.getSpanStart(span),text.getSpanEnd(span),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }
    public static void writeRange(Spannable text,Note.Mark mark,int offset) {
            Object span;
            switch(mark.type) {
                case "bold":span=new StyleSpan(Typeface.BOLD);break;
                case "italic":span=new StyleSpan(Typeface.ITALIC);break;
                case "underline":span=new UnderlineSpan();break;
                case "strike":span=new StrikethroughSpan();break;
                case "font":span=new TypefaceSpan(mark.value);break;
                case "size":span=new FontSize(Integer.parseInt(mark.value));break;
                case "link":span=new URLSpan(mark.value);break;
                case "number":span=new Numbered(mark.value);break;
                default:span=new Heading(mark.type);
            }
            if(offset>=0 && mark.end+offset<=text.length())text.setSpan(span,mark.start+offset,mark.end+offset,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    /** Fresh Editable makes DynamicLayout remeasure metrics after formatting, not just repaint glyphs. */
    public static void reflow(android.widget.EditText editor,List<Note.Mark> marks){
        int start=editor.getSelectionStart(),end=editor.getSelectionEnd();
        android.text.SpannableStringBuilder formatted=new android.text.SpannableStringBuilder(editor.getText().toString());
        write(formatted,marks);editor.setText(formatted,android.widget.TextView.BufferType.EDITABLE);
        if(start>=0 && end>=0)editor.setSelection(Math.min(start,formatted.length()),Math.min(end,formatted.length()));
        editor.requestLayout();editor.invalidate();
    }
}
