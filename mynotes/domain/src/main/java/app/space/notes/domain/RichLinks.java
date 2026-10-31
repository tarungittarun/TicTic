package app.space.notes.domain;
import java.util.*;
/** Atomic inline link insertion; preserves surrounding text/marks and rejects unsafe URLs. */
public final class RichLinks {
 private RichLinks(){}
 public static RichText replace(RichText source,int start,int end,String label,String url){
  if(start<0 || end<start || end>source.text.length())throw new IllegalArgumentException("Invalid link selection");
  SafeLinks.requireWeb(url);if(label==null || label.trim().isEmpty())throw new IllegalArgumentException("Enter text to display");
  if(start==end)return insert(source,start,label,url);
  if(label.equals(source.text.substring(start,end))){List<Note.Mark> kept=new ArrayList<>();for(Note.Mark mark:source.marks){if(!mark.type.equals("link") || mark.end<=start || mark.start>=end)kept.add(mark);else{if(mark.start<start)kept.add(new Note.Mark("link",mark.start,start,mark.value));if(mark.end>end)kept.add(new Note.Mark("link",end,mark.end,mark.value));}}kept.add(new Note.Mark("link",start,end,SafeLinks.requireWeb(url)));return new RichText(source.text,kept);}
  RichText left=source.slice(0,start),right=source.slice(end,source.text.length());List<Note.Mark> marks=new ArrayList<>(left.marks);int shift=start;
  for(Note.Mark mark:right.marks)marks.add(new Note.Mark(mark.type,mark.start+shift,mark.end+shift,mark.value));
  RichText removed=new RichText(left.text+right.text,marks);RichText linked=insert(removed,start,label,url);List<Note.Mark> formatted=new ArrayList<>(linked.marks);
  for(Note.Mark mark:source.marks)if(!mark.type.equals("link") && mark.start<=start && mark.end>=end && end>start)formatted.add(new Note.Mark(mark.type,start,start+label.length(),mark.value));
  return new RichText(linked.text,formatted);
 }
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
