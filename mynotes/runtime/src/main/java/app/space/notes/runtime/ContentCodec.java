package app.space.notes.runtime;

import app.space.notes.domain.*;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Canonical schema7 content: pixels and tables occur once, at their ordered block position. */
final class ContentCodec {
    private ContentCodec(){}
    static JSONObject encode(DocumentContent content)throws Exception{
        JSONArray array=new JSONArray();
        for(DocumentContent.Block block:content.blocks){
            JSONObject out=new JSONObject().put("id",block.id).put("kind",block.kind.name()).put("name",block.name);
            switch(block.kind){
                case TEXT:
                    JSONArray marks=new JSONArray();for(Note.Mark mark:block.text.marks)marks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));
                    out.put("text",block.text.text).put("marks",marks);break;
                case CHECKLIST:
                    JSONArray rows=new JSONArray();for(Note.Item item:block.items){JSONArray rowMarks=new JSONArray();for(Note.Mark mark:item.marks)rowMarks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));rows.put(new JSONObject().put("id",item.id).put("text",item.text).put("checked",item.checked).put("depth",item.depth).put("marks",rowMarks));}
                    out.put("rows",rows).put("hideChecked",block.hideChecked);break;
                case IMAGE:case DRAWING:
                    NoteImage image=block.image;out.put("image",new JSONObject().put("id",image.id).put("name",image.name).put("mime",image.mime).put("width",image.width).put("height",image.height).put("data",image.data));break;
                case DIAGRAM:out.put("diagram",DocumentCodec.encodeDiagram(block.diagram));break;
                case TABLE:
                    JSONArray table=new JSONArray();for(List<RichText> row:block.table.cells){JSONArray cells=new JSONArray();for(RichText cell:row){JSONArray cellMarks=new JSONArray();for(Note.Mark mark:cell.marks)cellMarks.put(new JSONObject().put("type",mark.type).put("start",mark.start).put("end",mark.end).put("value",mark.value));cells.put(new JSONObject().put("text",cell.text).put("marks",cellMarks));}table.put(cells);}out.put("rows",table);break;
                default:throw new IOException("Unsupported content kind");
            }array.put(out);
        }return new JSONObject().put("writingLines",content.writingLines).put("blocks",array);
    }
    static DocumentContent decode(JSONObject content)throws Exception{
        JSONArray array=content.getJSONArray("blocks");
        if(array.length()>DocumentContent.MAX_BLOCKS)throw new IOException("Too many content sections");
        List<DocumentContent.Block> blocks=new ArrayList<>();
        for(int i=0;i<array.length();i++){
            JSONObject in=array.getJSONObject(i);String id=in.getString("id"),name=in.getString("name");DocumentContent.Kind kind=DocumentContent.Kind.valueOf(in.getString("kind"));
            switch(kind){
                case TEXT:
                    List<Note.Mark> marks=new ArrayList<>();JSONArray ms=in.getJSONArray("marks");for(int j=0;j<ms.length();j++){JSONObject mark=ms.getJSONObject(j);marks.add(new Note.Mark(mark.getString("type"),mark.getInt("start"),mark.getInt("end"),mark.optString("value","")));}
                    if(!name.isEmpty())throw new IOException("Unexpected text section name");blocks.add(DocumentContent.Block.text(id,new RichText(in.getString("text"),marks)));break;
                case CHECKLIST:
                    List<Note.Item> items=new ArrayList<>();JSONArray rs=in.getJSONArray("rows");for(int j=0;j<rs.length();j++){JSONObject row=rs.getJSONObject(j);List<Note.Mark> rowMarks=new ArrayList<>();JSONArray styles=row.getJSONArray("marks");for(int k=0;k<styles.length();k++){JSONObject mark=styles.getJSONObject(k);rowMarks.add(new Note.Mark(mark.getString("type"),mark.getInt("start"),mark.getInt("end"),mark.getString("value")));}items.add(new Note.Item(row.getString("id"),row.getString("text"),row.getBoolean("checked"),row.getInt("depth"),rowMarks));}
                    if(!name.isEmpty())throw new IOException("Unexpected checklist section name");blocks.add(DocumentContent.Block.checklist(id,items,in.getBoolean("hideChecked")));break;
                case IMAGE:case DRAWING:
                    JSONObject image=in.getJSONObject("image");blocks.add(DocumentContent.Block.visual(id,name,kind,new NoteImage(image.getString("id"),image.getString("name"),image.getString("mime"),image.getInt("width"),image.getInt("height"),image.getString("data"))));break;
                case DIAGRAM:blocks.add(DocumentContent.Block.diagram(id,name,DocumentCodec.decodeDiagram(in.getJSONObject("diagram"))));break;
                case TABLE:
                    JSONArray table=in.getJSONArray("rows");if(table.length()<1 || table.length()>TextTable.MAX_ROWS)throw new IOException("Invalid table dimensions");
                    List<List<RichText>> rows=new ArrayList<>();for(int j=0;j<table.length();j++){JSONArray cells=table.getJSONArray(j);if(cells.length()<1 || cells.length()>TextTable.MAX_COLUMNS || table.length()*cells.length()>TextTable.MAX_CELLS)throw new IOException("Invalid table dimensions");List<RichText> values=new ArrayList<>();for(int c=0;c<cells.length();c++){JSONObject cell=cells.getJSONObject(c);JSONArray cellMarks=cell.getJSONArray("marks");List<Note.Mark> style=new ArrayList<>();for(int m=0;m<cellMarks.length();m++){JSONObject mark=cellMarks.getJSONObject(m);style.add(new Note.Mark(mark.getString("type"),mark.getInt("start"),mark.getInt("end"),mark.getString("value")));}values.add(new RichText(cell.getString("text"),style));}rows.add(values);}blocks.add(DocumentContent.Block.table(id,name,TextTable.rich(rows)));break;
                default:throw new IOException("Unsupported content kind");
            }
        }return new DocumentContent(blocks,content.getBoolean("writingLines"));
    }
}
