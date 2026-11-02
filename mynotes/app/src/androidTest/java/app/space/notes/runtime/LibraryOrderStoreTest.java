package app.space.notes.runtime;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class LibraryOrderStoreTest {
 @Test public void roundTripIsImmutableAndExact()throws Exception{List<String> ids=Arrays.asList("a","b","c");List<String> restored=LibraryOrderStore.decode(LibraryOrderStore.encode(ids));assertEquals(ids,restored);try{restored.add("d");fail();}catch(UnsupportedOperationException expected){}}
 @Test public void malformedOrderRejectedBeforeRestore()throws Exception{for(String invalid:Arrays.asList("[1]","[\"\"]","[\"a\",\"a\"]","{}")){try{LibraryOrderStore.decode(invalid);fail(invalid);}catch(org.json.JSONException expected){}}}
}
