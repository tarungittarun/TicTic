package app.space.notes.domain;
import java.util.*;

/** Immutable content copy with fresh identities; all rich payloads retained. */
public final class ContentCopies {
 private ContentCopies(){}
 public static DocumentContent.Block copy(DocumentContent.Block source){
  String id=UUID.randomUUID().toString();
  switch(source.kind){
   case TABLE:return DocumentContent.Block.table(id,source.name,source.table);
   case CHECKLIST:List<Note.Item> rows=new ArrayList<>();for(Note.Item row:source.items)rows.add(new Note.Item(UUID.randomUUID().toString(),row.text,row.checked,row.depth,row.marks));return DocumentContent.Block.checklist(id,source.name,rows,source.hideChecked);
   case IMAGE:case DRAWING:NoteImage image=source.image;return DocumentContent.Block.visual(id,source.name,source.kind,new NoteImage(UUID.randomUUID().toString(),image.name,image.mime,image.width,image.height,image.data));
   case DIAGRAM:return DocumentContent.Block.diagram(id,source.name,source.diagram);
   case TEXT:return DocumentContent.Block.text(id,source.text);
   default:throw new IllegalArgumentException("Unsupported content copy");
  }
 }
}
