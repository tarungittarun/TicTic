package app.space.notes.domain;
import java.util.*;

/** UTF-16 ranges match Android's editor indices; removes only the selected part. */
public final class TextFormatting {
    private TextFormatting() { }
    public static List<Note.Mark> apply(List<Note.Mark> source,String type,int start,int end) {
        if(start<0 || end<=start)throw new IllegalArgumentException("Select text first");
        boolean covered=!type.equals("clear");int cursor=start;
        List<Note.Mark> matching=new ArrayList<>();
        for(Note.Mark m:source)if(m.type.equals(type))matching.add(m);
        matching.sort(Comparator.comparingInt(m->m.start));
        for(Note.Mark m:matching)if(m.start<=cursor && m.end>cursor)cursor=m.end;
        covered=covered && cursor>=end;
        List<Note.Mark> result=new ArrayList<>();
        for(Note.Mark m:source) {
            boolean relevant=type.equals("clear") || m.type.equals(type) || (type.startsWith("h") && m.type.startsWith("h"));
            if(!relevant || m.end<=start || m.start>=end){result.add(m);continue;}
            if(m.start<start)result.add(new Note.Mark(m.type,m.start,start));
            if(m.end>end)result.add(new Note.Mark(m.type,end,m.end));
        }
        if(!type.equals("clear") && !covered)result.add(new Note.Mark(type,start,end));
        return result;
    }
}
