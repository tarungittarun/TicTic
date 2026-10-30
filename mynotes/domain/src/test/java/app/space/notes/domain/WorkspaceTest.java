package app.space.notes.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class WorkspaceTest {
 @Test public void creationNeverDependsOnAnOldGlobalEditorPreference(){assertNull(Workspace.create(1,false).content);assertNotNull(Workspace.create(1,true).content);}
 @Test public void copiesNeverChangeOrReuseTheOriginalIdentity(){Note source=Note.create(1).rich("Original","Keep",Arrays.asList(new Note.Mark("bold",0,4)),2);Note copy=Workspace.copyToDocument(source,3);assertNotEquals(source.id,copy.id);assertNull(source.content);assertNotNull(copy.content);assertEquals("Keep",source.body);assertEquals("Keep",copy.content.blocks.get(0).text.text);assertEquals("bold",copy.content.blocks.get(0).text.marks.get(0).type);assertEquals(2,source.updatedAt);}
 @Test public void typedWorkspacesDoNotFlattenExistingDocuments(){Note note=Workspace.create(1,false),doc=Workspace.create(2,true);assertTrue(Workspace.includes(note,false));assertFalse(Workspace.includes(doc,false));assertTrue(Workspace.includes(doc,true));assertFalse(Workspace.includes(note,true));assertSame(doc.content,DocumentContent.fromLegacy(doc));}
}
