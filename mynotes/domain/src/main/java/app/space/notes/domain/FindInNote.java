package app.space.notes.domain;
import java.util.*;
/** Literal, case-insensitive search with original UTF-16 offsets; no regex or persisted marks. */
public final class FindInNote {
 public static final int LIMIT=100;
 public static final class Match {public final int field,start,end;public Match(int field,int start,int end){this.field=field;this.start=start;this.end=end;}}
 private FindInNote(){}
 public static List<Match> search(List<String> fields,String query){
  if(query.isEmpty())return Collections.emptyList();List<Match> result=new ArrayList<>();
  for(int f=0;f<fields.size();f++){String text=fields.get(f);for(int at=0;at<=text.length()-query.length();at++){
   if(Thread.currentThread().isInterrupted())return Collections.emptyList();
   int end=at+query.length();if(at>0 && Character.isLowSurrogate(text.charAt(at)) && Character.isHighSurrogate(text.charAt(at-1)))continue;
   if(end<text.length() && end>0 && Character.isHighSurrogate(text.charAt(end-1)) && Character.isLowSurrogate(text.charAt(end)))continue;
   if(text.regionMatches(true,at,query,0,query.length())){result.add(new Match(f,at,end));if(result.size()>LIMIT)return Collections.unmodifiableList(result);at=end-1;}
  }}return Collections.unmodifiableList(result);
 }
}
