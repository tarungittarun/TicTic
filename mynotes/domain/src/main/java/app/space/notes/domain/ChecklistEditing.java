package app.space.notes.domain;

import java.util.*;

/** Immutable row edits shared by legacy Notes and canonical Docs checklists. */
public final class ChecklistEditing {
    private ChecklistEditing() { }

    public static final class Split {
        public final List<Note.Item> rows;
        public final String insertedId;
        private Split(List<Note.Item> rows,String insertedId){this.rows=rows;this.insertedId=insertedId;}
    }

    /** Split one row at a selection, keeping the prefix on its identity and inserting the suffix next. */
    public static Split afterEnter(List<Note.Item> rows,String id,int selectionStart,int selectionEnd){
        if(rows==null || id==null)throw new IllegalArgumentException("Missing checklist row");
        int index=-1;Note.Item original=null;
        for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(id)){if(index>=0)throw new IllegalArgumentException("Duplicate checklist item");index=i;original=rows.get(i);}
        if(original==null)throw new IllegalArgumentException("Checklist item is no longer available");
        if(selectionStart<0 || selectionEnd<selectionStart || selectionEnd>original.text.length())throw new IllegalArgumentException("Invalid checklist selection");
        String prefix=original.text.substring(0,selectionStart),suffix=original.text.substring(selectionEnd);
        Note.Item retained=new Note.Item(original.id,prefix,original.checked,original.depth,marksBetween(original.marks,0,selectionStart));
        String insertedId=UUID.randomUUID().toString();Note.Item next=new Note.Item(insertedId,suffix,false,original.depth,marksBetween(original.marks,selectionEnd,original.text.length()));
        List<Note.Item> result=new ArrayList<>(rows);result.set(index,retained);result.add(index+1,next);
        return new Split(Collections.unmodifiableList(result),insertedId);
    }

    /** Remove one checklist row by stable identity without changing siblings. */
    public static List<Note.Item> removeRow(List<Note.Item> rows,String id){
        if(rows==null || id==null)throw new IllegalArgumentException("Missing checklist row");
        int index=-1;for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(id)){if(index>=0)throw new IllegalArgumentException("Duplicate checklist item");index=i;}
        if(index<0)throw new IllegalArgumentException("Checklist item is no longer available");
        List<Note.Item> result=new ArrayList<>(rows);result.remove(index);return Collections.unmodifiableList(result);
    }

    /** Drop whitespace-only rows only at the end; an empty row between real items is preserved. */
    public static List<Note.Item> withoutTrailingBlankRows(List<Note.Item> rows){
        if(rows==null)throw new IllegalArgumentException("Missing checklist rows");
        int end=rows.size();while(end>0 && rows.get(end-1).text.trim().isEmpty())end--;
        return end==rows.size()?rows:Collections.unmodifiableList(new ArrayList<>(rows.subList(0,end)));
    }

    /** Prune trailing blank rows from every canonical checklist without changing other blocks. */
    public static DocumentContent withoutTrailingBlankRows(DocumentContent content){
        if(content==null)throw new IllegalArgumentException("Missing document content");
        List<DocumentContent.Block> changed=null;
        for(int i=0;i<content.blocks.size();i++){
            DocumentContent.Block block=content.blocks.get(i);if(block.kind!=DocumentContent.Kind.CHECKLIST)continue;
            List<Note.Item> rows=withoutTrailingBlankRows(block.items);if(rows==block.items)continue;
            if(changed==null)changed=new ArrayList<>(content.blocks);
            changed.set(i,DocumentContent.Block.checklist(block.id,block.name,rows,block.hideChecked,block.checklistAppearance));
        }
        return changed==null?content:new DocumentContent(changed,content.writingLines,content.notesWorkspace);
    }

    /** Return the same list when nothing is checked; otherwise clear completion and preserve row data. */
    public static List<Note.Item> uncheckAll(List<Note.Item> rows){
        if(rows==null)throw new IllegalArgumentException("Missing checklist rows");
        List<Note.Item> result=null;
        for(int i=0;i<rows.size();i++)if(rows.get(i).checked){
            if(result==null)result=new ArrayList<>(rows);
            Note.Item item=rows.get(i);result.set(i,item.change(item.text,false,item.depth));
        }
        return result==null?rows:Collections.unmodifiableList(result);
    }

    private static List<Note.Mark> marksBetween(List<Note.Mark> marks,int start,int end){
        List<Note.Mark> result=new ArrayList<>();for(Note.Mark mark:marks){int from=Math.max(mark.start,start),to=Math.min(mark.end,end);if(to>from)result.add(new Note.Mark(mark.type,from-start,to-start,mark.value));}return result;
    }
}
