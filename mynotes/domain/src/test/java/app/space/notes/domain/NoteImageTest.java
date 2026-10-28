package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class NoteImageTest {
 private NoteImage image(){return new NoteImage(UUID.randomUUID().toString(),"Image","image/png",1,1,"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXioAAAAASUVORK5CYII=");}
 @Test public void mutationsAndCopiesPreserveEmbeddedBytes(){NoteImage image=image();Note n=Note.create(1).images(Arrays.asList(image),2);for(Note changed:Arrays.asList(n.edit("a","b",3),n.convert(3),n.background(2,3),n.folder("Work",3),n.reminder(1000,3),n.diagram(Diagram.empty(),3),n.withId("copy"),NoteOperations.duplicate(n,3))){assertEquals(1,changed.images.size());assertEquals(image.data,changed.images.get(0).data);}}
 @Test public void removalIsImmutable(){Note n=Note.create(1).images(Arrays.asList(image()),2);assertTrue(n.images(Collections.emptyList(),3).images.isEmpty());assertEquals(1,n.images.size());}
 @Test public void restoreCannotSilentlyLoseImages(){Note n=Note.create(1),changed=n.images(Arrays.asList(image()),1);assertFalse(NoteMerge.same(n,changed));assertEquals(1,NoteMerge.preview(Arrays.asList(n),Arrays.asList(changed)).conflicts);}
 @Test(expected=IllegalArgumentException.class) public void invalidImageBytesRejected(){new NoteImage(UUID.randomUUID().toString(),"Image","image/png",1,1,"AAAAAA==");}
 @Test(expected=IllegalArgumentException.class) public void oversizedDimensionsRejected(){new NoteImage(UUID.randomUUID().toString(),"Image","image/png",20000,1,"AAAAAA==");}
 @Test(expected=IllegalArgumentException.class) public void mergeCannotFlattenImages(){NoteOperations.merge(Arrays.asList(Note.create(1).images(Arrays.asList(image()),2),Note.create(1)),3);}
 @Test(expected=IllegalArgumentException.class) public void repeatedImageIdsRejected(){NoteImage image=image();Note.create(1).images(Arrays.asList(image,image),2);}
}
