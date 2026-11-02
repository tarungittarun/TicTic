package app.space.notes.runtime;
import java.util.*;
import org.json.*;
/** Portable presentation IDs only; never content, timestamps or source file order. */
final class LibraryOrderStore {
 static List<String> decode(String text)throws JSONException{
  if(text==null || text.length()>2000000)throw new JSONException("Arrangement exceeds limit");
  JSONArray array=new JSONArray(text);if(array.length()>20000)throw new JSONException("Too many arranged items");
  List<String> ids=new ArrayList<>();Set<String> unique=new HashSet<>();for(int i=0;i<array.length();i++){Object raw=array.get(i);if(!(raw instanceof String))throw new JSONException("Invalid item ID");String id=(String)raw;if(id.isEmpty() || id.length()>200 || !unique.add(id))throw new JSONException("Invalid or duplicate item ID");ids.add(id);}return Collections.unmodifiableList(ids);
 }
 static String encode(List<String> ids){return new JSONArray(ids).toString();}
}
