package app.space.notes.domain;
import java.util.*;
/** Paragraph-scoped numbering survives newline insertion/deletion instead of duplicating prefixes. */
public final class ParagraphNumbering {
    private ParagraphNumbering(){}
    public static List<Note.Mark> normalize(List<Note.Mark> source,String text){
        List<Note.Mark> result=new ArrayList<>(),numbers=new ArrayList<>();for(Note.Mark mark:source)if(mark.type.equals("number"))numbers.add(mark);else result.add(mark);
        numbers.sort(Comparator.comparingInt(mark->mark.start));Map<Integer,Note.Mark> paragraphs=new TreeMap<>();
        for(Note.Mark mark:numbers){int value=Integer.parseInt(mark.value),at=text.lastIndexOf('\n',Math.max(-1,mark.start-1))+1;while(at<text.length() && at<mark.end){int end=text.indexOf('\n',at);if(end<0)end=text.length();if(end>at){if(value>9999)throw new IllegalArgumentException("Numbered list exceeds 9999 paragraphs");paragraphs.putIfAbsent(at,new Note.Mark("number",at,end,Integer.toString(value++)));}at=end+1;}}
        Note.Mark previous=null;for(Note.Mark mark:paragraphs.values()){int value=previous!=null && previous.end+1==mark.start?Integer.parseInt(previous.value)+1:Integer.parseInt(mark.value);if(value>9999)throw new IllegalArgumentException("Numbered list exceeds 9999 paragraphs");previous=new Note.Mark("number",mark.start,mark.end,Integer.toString(value));result.add(previous);}return result;
    }
}
