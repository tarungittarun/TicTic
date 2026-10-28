package app.space.notes.domain;
import java.util.*;
/** Conservative pause-based readability, not a semantic punctuation-restoration model.
 * Raw recognizer output must be retained separately. Never infer questions or exclamations.
 */
public final class TranscriptReadability {
 private TranscriptReadability(){}
 public static final class Word {
  public final String text;public final double start,end;
  public Word(String text,double start,double end){if(text==null || text.trim().isEmpty() || !Double.isFinite(start) || !Double.isFinite(end) || start<0 || end<start)throw new IllegalArgumentException("Invalid timed word");this.text=text.trim();this.start=start;this.end=end;}
 }
 public static String format(List<Word> words){StringBuilder out=new StringBuilder();boolean sentence=true;double end=0;for(Word word:words){if(out.length()>0){double pause=word.start-end;if(pause>=.95){if(!terminal(out.charAt(out.length()-1)))out.append('.');sentence=true;}else if(pause>=.45 && !terminal(out.charAt(out.length()-1)))out.append(',');out.append(' ');}String text=word.text;if(sentence){int cp=text.codePointAt(0);text=new String(Character.toChars(Character.toUpperCase(cp)))+text.substring(Character.charCount(cp));}out.append(text);sentence=terminal(text.charAt(text.length()-1));end=word.end;}if(out.length()>0 && !terminal(out.charAt(out.length()-1)))out.append('.');return out.toString();}
 public static String sentence(String text){String value=text.trim();if(value.isEmpty())return value;int cp=value.codePointAt(0);value=new String(Character.toChars(Character.toUpperCase(cp)))+value.substring(Character.charCount(cp));return terminal(value.charAt(value.length()-1))?value:value+".";}
 private static boolean terminal(char c){return c=='.' || c=='!' || c=='?' || c=='।';}
}
