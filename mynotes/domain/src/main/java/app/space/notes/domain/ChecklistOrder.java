package app.space.notes.domain;
import java.util.*;
/** Positions are insertion edges in the existing rows, never unchecked external indexes. */
public final class ChecklistOrder {
 private ChecklistOrder(){}
 public static List<Note.Item> move(List<Note.Item> rows,String id,int insertion){
  if(insertion<0 || insertion>rows.size())throw new IllegalArgumentException("Invalid checklist position");
  int from=-1;Set<String> seen=new HashSet<>();for(int i=0;i<rows.size();i++){if(!seen.add(rows.get(i).id))throw new IllegalArgumentException("Duplicate checklist item");if(rows.get(i).id.equals(id))from=i;}
  if(from<0)throw new IllegalArgumentException("Checklist item is no longer available");
  int to=insertion>from?insertion-1:insertion;if(from==to)return rows;
  List<Note.Item> result=new ArrayList<>(rows);Note.Item item=result.remove(from);result.add(to,item);return Collections.unmodifiableList(result);
 }
}
