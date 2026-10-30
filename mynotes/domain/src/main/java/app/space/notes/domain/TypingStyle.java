package app.space.notes.domain;
import java.util.*;
/** Text-free reusable formatting. Links and list numbering are never captured into presets. */
public final class TypingStyle {
    private static final List<String> TYPES=Arrays.asList("bold","italic","underline","strike","font","size","h1","h2");
    public final Map<String,String> values;
    public TypingStyle(Map<String,String> source){
        if(source==null || source.size()>8)throw new IllegalArgumentException("Invalid style");TreeMap<String,String> copy=new TreeMap<>();
        for(Map.Entry<String,String> entry:source.entrySet()){if(!TYPES.contains(entry.getKey()))throw new IllegalArgumentException("Unsupported preset style");new Note.Mark(entry.getKey(),0,1,entry.getValue());copy.put(entry.getKey(),entry.getValue());}
        if(copy.containsKey("h1") && copy.containsKey("h2"))throw new IllegalArgumentException("Choose one heading");values=Collections.unmodifiableMap(copy);
    }
    public static TypingStyle normal(){return new TypingStyle(Collections.emptyMap());}
    public TypingStyle with(String type,String value){Map<String,String> next=new TreeMap<>(values);if(!TYPES.contains(type))throw new IllegalArgumentException("Select text to add links or numbering");if(type.equals("h1") || type.equals("h2")){next.remove("h1");next.remove("h2");}if(value==null)next.remove(type);else next.put(type,value);return new TypingStyle(next);}
    public TypingStyle withoutHeading(){Map<String,String> next=new TreeMap<>(values);next.remove("h1");next.remove("h2");return new TypingStyle(next);}
    public static TypingStyle capture(RichText rich,int start,int end){
        if(start<0 || end<start || end>rich.text.length())throw new IllegalArgumentException("Invalid style selection");
        if(start==end){if(rich.text.isEmpty())return normal();start=start==0?0:start-1;end=start+1;}
        Map<String,String> result=new TreeMap<>();for(String type:TYPES){for(Note.Mark candidate:rich.marks){if(!candidate.type.equals(type) || candidate.start>start || candidate.end<=start)continue;int cursor=start;List<Note.Mark> same=new ArrayList<>();for(Note.Mark mark:rich.marks)if(mark.type.equals(type) && mark.value.equals(candidate.value))same.add(mark);same.sort(Comparator.comparingInt(mark->mark.start));for(Note.Mark mark:same)if(mark.start<=cursor && mark.end>cursor)cursor=mark.end;if(cursor>=end){result.put(type,candidate.value);break;}}}
        if(result.containsKey("h1"))result.remove("h2");return new TypingStyle(result);
    }
    /** Exact replacement of preset-owned styles only; unselected formatting/URLs/numbering survive. */
    public RichText apply(RichText rich,int start,int end){
        if(start<0 || end<=start || end>rich.text.length())throw new IllegalArgumentException("Select text first");rich.slice(start,end);
        List<Note.Mark> marks=new ArrayList<>();for(Note.Mark mark:rich.marks){if(!TYPES.contains(mark.type) || mark.type.startsWith("h") || mark.end<=start || mark.start>=end){marks.add(mark);continue;}if(mark.start<start)marks.add(new Note.Mark(mark.type,mark.start,start,mark.value));if(mark.end>end)marks.add(new Note.Mark(mark.type,end,mark.end,mark.value));}
        marks=TextFormatting.paragraph(marks,rich.text,"text",start,end);
        for(Map.Entry<String,String> entry:values.entrySet()){if(entry.getKey().startsWith("h"))marks=TextFormatting.paragraph(marks,rich.text,entry.getKey(),start,end);else marks.add(new Note.Mark(entry.getKey(),start,end,entry.getValue()));}
        marks.sort(Comparator.comparing((Note.Mark mark)->mark.type).thenComparingInt(mark->mark.start).thenComparingInt(mark->mark.end));List<Note.Mark> merged=new ArrayList<>();for(Note.Mark mark:marks){Note.Mark previous=merged.isEmpty()?null:merged.get(merged.size()-1);if(previous!=null && TYPES.contains(mark.type) && !mark.type.startsWith("h") && previous.type.equals(mark.type) && previous.value.equals(mark.value) && mark.start<=previous.end)merged.set(merged.size()-1,new Note.Mark(mark.type,previous.start,Math.max(mark.end,previous.end),mark.value));else merged.add(mark);}return new RichText(rich.text,merged);
    }
    public String encode(){StringBuilder out=new StringBuilder();for(Map.Entry<String,String> entry:values.entrySet())out.append(entry.getKey()).append('=').append(entry.getValue()).append(';');return out.toString();}
    public static TypingStyle decode(String text){if(text==null || text.length()>256)throw new IllegalArgumentException("Invalid saved style");Map<String,String> values=new TreeMap<>();if(!text.isEmpty())for(String part:text.split(";")){int at=part.indexOf('=');if(at<1 || values.put(part.substring(0,at),part.substring(at+1))!=null)throw new IllegalArgumentException("Invalid saved style");}TypingStyle style=new TypingStyle(values);if(!style.encode().equals(text))throw new IllegalArgumentException("Noncanonical saved style");return style;}
}
