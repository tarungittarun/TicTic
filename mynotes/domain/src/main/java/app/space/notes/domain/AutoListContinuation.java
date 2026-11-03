package app.space.notes.domain;

import java.util.regex.*;
import java.util.*;

/** Literal numbered paragraphs: portable text, no hidden prefix or import interpretation. */
public final class AutoListContinuation {
 private AutoListContinuation(){}
 private static final Pattern NUMBER=Pattern.compile("([1-9][0-9]{0,3})\\. (.*)");
 public static final class Edit {
  public final int start,end;public final String replacement;
  public final List<Edit> following=new ArrayList<>();
  Edit(int start,int end,String replacement){this.start=start;this.end=end;this.replacement=replacement;}
 }
 /** Backspace immediately after the literal prefix removes the prefix, not document text. */
 public static Edit beforeBackspace(String text,int cursor){
  if(cursor<=0 || cursor>text.length())return null;int from=text.lastIndexOf('\n',cursor-1)+1;
  Matcher match=NUMBER.matcher(text.substring(from,cursor));
  return match.matches() && match.group(2).isEmpty()?new Edit(from,cursor,""):null;
 }
 /** Called only for a single user-inserted newline, not paste, restore or programmatic formatting. */
 public static Edit afterEnter(String text,int newline){
  if(newline<0 || newline>=text.length() || text.charAt(newline)!='\n')return null;
  int from=text.lastIndexOf('\n',newline-1)+1;Matcher match=NUMBER.matcher(text.substring(from,newline));
  if(!match.matches())return null;
  if(match.group(2).isEmpty())return new Edit(from,newline+1,"");
  int number=Integer.parseInt(match.group(1));if(number>=9999)return null;
  Edit edit=new Edit(newline+1,newline+1,(number+1)+". ");
  // Replace only following literal prefixes, backwards in the UI, preserving their spans.
  int lineEnd=text.indexOf('\n',newline+1),next=number+2;
  while(lineEnd>=0 && lineEnd+1<text.length() && next<=9999){
   int start=lineEnd+1,end=text.indexOf('\n',start);if(end<0)end=text.length();
   Matcher following=NUMBER.matcher(text.substring(start,end));if(!following.matches())break;
   String expected=Integer.toString(next++);if(!expected.equals(following.group(1)))edit.following.add(new Edit(start,start+following.group(1).length(),expected));
   lineEnd=end<text.length()?end:-1;
  }
  return edit;
 }
}
