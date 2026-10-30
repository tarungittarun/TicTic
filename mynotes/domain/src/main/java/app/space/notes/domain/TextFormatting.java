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
            if(m.start<start)result.add(new Note.Mark(m.type,m.start,start,m.value));
            if(m.end>end)result.add(new Note.Mark(m.type,end,m.end,m.value));
        }
        if(!type.equals("clear") && !covered)result.add(new Note.Mark(type,start,end));
        return result;
    }
    /** Headings are paragraph styles. A selection ending at a next-line start excludes that line. */
    public static List<Note.Mark> paragraph(List<Note.Mark> source,String text,String type,int start,int end) {
        if(!type.equals("h1") && !type.equals("h2") && !type.equals("text"))throw new IllegalArgumentException("Unknown paragraph style");
        if(start<0 || end<start || end>text.length())throw new IllegalArgumentException("Invalid selection");
        List<Note.Mark> result=normalizeParagraphs(source,text);
        int first=text.lastIndexOf('\n',Math.max(-1,start-1))+1;
        int last=end>start?end-1:start;
        int boundary=text.indexOf('\n',Math.min(last,text.length()));if(boundary<0)boundary=text.length();
        boolean allCovered=!type.equals("text"),any=false;
        for(int at=first;at<=boundary;){int stop=text.indexOf('\n',at);if(stop<0 || stop>boundary)stop=boundary;
            if(stop>at){any=true;boolean covered=false;for(Note.Mark m:result)if(m.type.equals(type) && m.start<=at && m.end>=stop)covered=true;allCovered &= covered;}
            if(stop>=boundary)break;at=stop+1;
        }
        if(!any)return result;
        List<Note.Mark> kept=new ArrayList<>();for(Note.Mark m:result){if((m.type.equals("h1") || m.type.equals("h2")) && m.start<boundary && m.end>first)continue;kept.add(m);}
        if(!type.equals("text") && !allCovered){for(int at=first;at<=boundary;){int stop=text.indexOf('\n',at);if(stop<0 || stop>boundary)stop=boundary;if(stop>at)kept.add(new Note.Mark(type,at,stop));if(stop>=boundary)break;at=stop+1;}}
        return kept;
    }
    /** Interpret old character-range headings as paragraphs without changing text or inline emphasis. */
    public static List<Note.Mark> normalizeParagraphs(List<Note.Mark> source,String text){
        List<Note.Mark> out=new ArrayList<>();Map<Integer,Note.Mark> headings=new TreeMap<>();
        for(Note.Mark m:source){if(!m.type.equals("h1") && !m.type.equals("h2")){out.add(m);continue;}
            int from=text.lastIndexOf('\n',Math.max(-1,m.start-1))+1;
            while(from<text.length() && from<m.end){int stop=text.indexOf('\n',from);if(stop<0)stop=text.length();if(stop>from)headings.put(from,new Note.Mark(m.type,from,stop));from=stop+1;}
        }
        out.addAll(headings.values());return out;
    }
}
