package app.space.notes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable Unicode text and lossless marks for ordered document fields. */
public final class RichText {
    public static final int MAX_MARKS=4096;
    public final String text;
    public final List<Note.Mark> marks;
    public RichText(String text,List<Note.Mark> marks){
        if(text==null || marks==null || marks.size()>MAX_MARKS)throw new IllegalArgumentException("Missing rich text");
        requireText(text);for(Note.Mark mark:marks){
            if(mark==null || mark.end>text.length())throw new IllegalArgumentException("Text mark outside text");
            requireText(mark.value);boundary(text,mark.start);boundary(text,mark.end);
        }
        this.text=text;this.marks=Collections.unmodifiableList(new ArrayList<>(marks));
    }
    public boolean same(RichText other){if(other==null || !text.equals(other.text) || marks.size()!=other.marks.size())return false;for(int i=0;i<marks.size();i++){Note.Mark a=marks.get(i),b=other.marks.get(i);if(a.start!=b.start || a.end!=b.end || !a.type.equals(b.type) || !a.value.equals(b.value))return false;}return true;}
    private static final class Addition {final int at,order;final String text;Addition(int at,int order,String text){this.at=at;this.order=order;this.text=text;}}
    /** Sharing/search projection retains the visible number and the safe link destination. */
    public static String plainText(String source,List<Note.Mark> marks){
        List<Addition> additions=new ArrayList<>();for(Note.Mark mark:marks){if(mark.type.equals("number"))additions.add(new Addition(mark.start,0,mark.value+". "));else if(mark.type.equals("link") && !source.substring(mark.start,mark.end).equals(mark.value))additions.add(new Addition(mark.end,1," ("+mark.value+")"));}
        if(additions.isEmpty())return source;additions.sort(java.util.Comparator.comparingInt((Addition value)->value.at).reversed().thenComparingInt(value->value.order));StringBuilder result=new StringBuilder(source);for(Addition addition:additions)result.insert(addition.at,addition.text);return result.toString();
    }
    public String plainText(){return plainText(text,marks);}
    public String fingerprint(){StringBuilder value=new StringBuilder().append(text.length()).append(':').append(text).append(':').append(marks.size());for(Note.Mark mark:marks)value.append(':').append(mark.type).append(':').append(mark.start).append(':').append(mark.end).append(':').append(mark.value.length()).append(':').append(mark.value);return value.toString();}
    public RichText slice(int start,int end){
        if(start<0 || end<start || end>text.length())throw new IllegalArgumentException("Invalid text selection");
        boundary(text,start);boundary(text,end);
        List<Note.Mark> result=new ArrayList<>();
        for(Note.Mark mark:marks){
            int from=Math.max(start,mark.start),to=Math.min(end,mark.end);
            if(from<to)result.add(new Note.Mark(mark.type,from-start,to-start,mark.value));
        }
        return new RichText(text.substring(start,end),result);
    }
    /** Three immutable fragments; the caller must commit their structural change atomically. */
    public List<RichText> selection(int start,int end){
        RichText selected=slice(start,end);
        List<RichText> result=new ArrayList<>();result.add(slice(0,start));result.add(selected);result.add(slice(end,text.length()));
        return Collections.unmodifiableList(result);
    }
    public static void requireText(String text){for(int i=0;i<text.length();i++){char value=text.charAt(i);if(Character.isHighSurrogate(value)){if(++i>=text.length() || !Character.isLowSurrogate(text.charAt(i)))throw new IllegalArgumentException("Invalid Unicode text; source retained");}else if(Character.isLowSurrogate(value))throw new IllegalArgumentException("Invalid Unicode text; source retained");}}
    private static void boundary(String text,int offset){
        if(offset>0 && offset<text.length() && Character.isHighSurrogate(text.charAt(offset-1)) && Character.isLowSurrogate(text.charAt(offset)))
            throw new IllegalArgumentException("Selection splits a Unicode character");
    }
}
