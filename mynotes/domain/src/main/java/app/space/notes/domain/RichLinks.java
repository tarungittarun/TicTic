package app.space.notes.domain;
import java.util.*;
/** Atomic inline link insertion; preserves surrounding text/marks and rejects unsafe URLs. */
public final class RichLinks {
 private RichLinks(){}
 public static RichText insert(RichText source,int offset,String label,String url){
  String safe=SafeLinks.requireWeb(url);if(label==null || label.trim().isEmpty())throw new IllegalArgumentException("Enter text to display");
  RichText before=source.slice(0,offset),after=source.slice(offset,source.text.length());List<Note.Mark> marks=new ArrayList<>();
  for(Note.Mark mark:source.marks){
   if(mark.end<=offset)marks.add(mark);
   else if(mark.start>=offset)marks.add(new Note.Mark(mark.type,mark.start+label.length(),mark.end+label.length(),mark.value));
   else if(!mark.type.equals("link"))marks.add(new Note.Mark(mark.type,mark.start,mark.end+label.length(),mark.value));
   else{marks.add(new Note.Mark(mark.type,mark.start,offset,mark.value));marks.add(new Note.Mark(mark.type,offset+label.length(),mark.end+label.length(),mark.value));}
  }
  marks.add(new Note.Mark("link",offset,offset+label.length(),safe));return new RichText(before.text+label+after.text,marks);
 }
}
