package app.space.notes.runtime;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import app.space.notes.domain.ChecklistAppearance;
import app.space.notes.domain.DocumentContent;
import app.space.notes.domain.Note;
import app.space.notes.domain.RichText;
import java.util.List;

/** Replaces only canonical checklist status prefixes visually; the underlying text stays interoperable. */
final class ChecklistPreview {
    private ChecklistPreview(){}

    static void decorate(SpannableStringBuilder preview,DocumentContent content,NotesDesign design){
        int offset=0;
        for(int index=0;index<content.blocks.size();index++){
            DocumentContent.Block block=content.blocks.get(index);
            if(index>0)offset++;
            String projection=plain(block);
            if(block.kind==DocumentContent.Kind.CHECKLIST){
                int rowOffset=block.name.isEmpty()?0:block.name.length()+1;
                for(int row=0;row<block.items.size();row++){
                    Note.Item item=block.items.get(row);
                    if(row>0)rowOffset++;
                    rowOffset+=item.depth*2;
                    int marker=offset+rowOffset;
                    if(marker>=0 && marker+4<=preview.length()){
                        ChecklistAppearance appearance=block.checklistAppearance;
                        preview.setSpan(new ChecklistMarkerSpan(appearance,item.checked,
                                ChecklistMarkerRenderer.color(appearance,design),design.ink,design.ink,
                                design.context.getResources().getDisplayMetrics().density),
                                marker,marker+4,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    rowOffset+=4+RichText.plainText(item.text,item.marks).length();
                }
            }
            offset+=projection.length();
        }
    }

    private static String plain(DocumentContent.Block block){
        if(block.text!=null)return block.text.plainText();
        if(block.kind==DocumentContent.Kind.CHECKLIST){
            StringBuilder rows=new StringBuilder();for(Note.Item item:block.items){
                if(rows.length()>0)rows.append('\n');
                for(int depth=0;depth<item.depth;depth++)rows.append("  ");
                rows.append(item.checked?"[x] ":"[ ] ").append(RichText.plainText(item.text,item.marks));
            }
            return (block.name.isEmpty()?"":block.name+"\n")+rows;
        }
        if(block.diagram!=null)return block.name+"\n"+block.diagram.plainText();
        if(block.table!=null){
            StringBuilder rows=new StringBuilder();List<List<RichText>> values=block.table.cells;
            for(int row=0;row<values.size();row++){
                if(row>0)rows.append('\n');List<RichText> cells=values.get(row);
                for(int column=0;column<cells.size();column++){
                    if(column>0)rows.append('\t');rows.append(cells.get(column).plainText());
                }
            }
            return block.name+"\n"+rows;
        }
        return block.name;
    }
}
