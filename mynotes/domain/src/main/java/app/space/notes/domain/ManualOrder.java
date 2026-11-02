package app.space.notes.domain;
import java.util.*;
/** Reorder visible slots only: filtering/folders never shift hidden documents. */
public final class ManualOrder {
 private ManualOrder(){}
 public static List<String> replaceVisible(List<String> all,List<String> visible){
  Set<String> full=new HashSet<>(all),chosen=new HashSet<>(visible);
  if(full.size()!=all.size() || chosen.size()!=visible.size() || !full.containsAll(chosen))throw new IllegalArgumentException("Invalid arrangement");
  List<String> result=new ArrayList<>(all);int next=0;for(int i=0;i<result.size();i++)if(chosen.contains(result.get(i)))result.set(i,visible.get(next++));return Collections.unmodifiableList(result);
 }
}
