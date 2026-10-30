package app.space.notes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lossless UTF-16 text/legacy marks primitive for the future block editor. */
public final class RichText {
    public final String text;
    public final List<Note.Mark> marks;
    public RichText(String text,List<Note.Mark> marks){
        if(text==null || marks==null)throw new IllegalArgumentException("Missing rich text");
        for(Note.Mark mark:marks){
            if(mark==null || mark.end>text.length())throw new IllegalArgumentException("Text mark outside text");
            boundary(text,mark.start);boundary(text,mark.end);
        }
        this.text=text;this.marks=Collections.unmodifiableList(new ArrayList<>(marks));
    }
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
    private static void boundary(String text,int offset){
        if(offset>0 && offset<text.length() && Character.isHighSurrogate(text.charAt(offset-1)) && Character.isLowSurrogate(text.charAt(offset)))
            throw new IllegalArgumentException("Selection splits a Unicode character");
    }
}
