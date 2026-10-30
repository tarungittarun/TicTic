package app.space.notes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable rectangular rich-text cells; rows is a read-only plain-text projection. */
public final class TextTable {
    public static final int MAX_ROWS=100,MAX_COLUMNS=20,MAX_CELLS=1000;
    public static final int MAX_CELL_CHARACTERS=4096,MAX_CHARACTERS=65536;
    public final List<List<String>> rows;
    public final List<List<RichText>> cells;
    public final int columns;public final boolean hasHeader;
    public TextTable(List<? extends List<String>> source){this(promote(source),false);}
    public static TextTable rich(List<? extends List<RichText>> source){return new TextTable(source,false);}
    public static TextTable rich(List<? extends List<RichText>> source,boolean header){return new TextTable(source,header);}
    public TextTable withHeader(boolean header){return header==hasHeader?this:new TextTable(cells,header);}
    private TextTable(List<? extends List<RichText>> source,boolean header){
        hasHeader=header;
        if(source==null || source.isEmpty() || source.size()>MAX_ROWS || source.get(0)==null)throw new IllegalArgumentException("Invalid table row count");
        int width=source.get(0).size();dimensions(source.size(),width);int characters=0;
        List<List<RichText>> richRows=new ArrayList<>();List<List<String>> plainRows=new ArrayList<>();
        for(List<RichText> row:source){
            if(row==null || row.size()!=width)throw new IllegalArgumentException("Table must be rectangular");
            List<RichText> formatted=new ArrayList<>();List<String> plain=new ArrayList<>();
            for(RichText cell:row){if(cell==null || cell.text.length()>MAX_CELL_CHARACTERS)throw new IllegalArgumentException("Invalid table cell");characters+=cell.text.length();if(characters>MAX_CHARACTERS)throw new IllegalArgumentException("Table text limit exceeded");formatted.add(cell);plain.add(cell.text);}
            richRows.add(Collections.unmodifiableList(formatted));plainRows.add(Collections.unmodifiableList(plain));
        }
        columns=width;cells=Collections.unmodifiableList(richRows);rows=Collections.unmodifiableList(plainRows);
    }
    private static List<List<RichText>> promote(List<? extends List<String>> source){
        if(source==null || source.isEmpty() || source.size()>MAX_ROWS || source.get(0)==null)throw new IllegalArgumentException("Invalid table rows");dimensions(source.size(),source.get(0).size());List<List<RichText>> result=new ArrayList<>();
        for(List<String> row:source){if(row==null || row.size()!=source.get(0).size())throw new IllegalArgumentException("Table must be rectangular");List<RichText> cells=new ArrayList<>();for(String cell:row)cells.add(new RichText(cell,Collections.emptyList()));result.add(cells);}return result;
    }
    private static void dimensions(int rows,int columns){if(rows<1 || rows>MAX_ROWS || columns<1 || columns>MAX_COLUMNS || rows*columns>MAX_CELLS)throw new IllegalArgumentException("Invalid table dimensions");}
    public static TextTable empty(int rows,int columns){dimensions(rows,columns);List<List<String>> result=new ArrayList<>();for(int i=0;i<rows;i++)result.add(new ArrayList<>(Collections.nCopies(columns,"")));return new TextTable(result);}
    public TextTable withCell(int row,int column,String text){index(row,cells.size());index(column,columns);RichText old=cells.get(row).get(column);if(old.text.equals(text))return this;if(!old.marks.isEmpty())throw new IllegalStateException("Supply formatted cell content explicitly");return withRichCell(row,column,new RichText(text,Collections.emptyList()));}
    public TextTable withRichCell(int row,int column,RichText text){index(row,cells.size());index(column,columns);if(text==cells.get(row).get(column))return this;List<List<RichText>> next=mutableRows();next.get(row).set(column,text);return rich(next,hasHeader);}
    public TextTable insertRow(int edge){edge(edge,cells.size());if(hasHeader && edge==0)throw new IllegalArgumentException("Add rows below the header, or turn the header off first");dimensions(cells.size()+1,columns);List<List<RichText>> next=mutableRows();next.add(edge,new ArrayList<>(Collections.nCopies(columns,new RichText("",Collections.emptyList()))));return rich(next,hasHeader);}
    public TextTable insertColumn(int edge){edge(edge,columns);dimensions(cells.size(),columns+1);List<List<RichText>> next=mutableRows();for(List<RichText> row:next)row.add(edge,new RichText("",Collections.emptyList()));return rich(next,hasHeader);}
    /** UI must provide confirmation and a document-history transaction. */
    public TextTable deleteRow(int row){index(row,cells.size());if(cells.size()==1)throw new IllegalArgumentException("Cannot delete the last row");List<List<RichText>> next=mutableRows();next.remove(row);return rich(next,hasHeader && row!=0);}
    /** UI must provide confirmation and a document-history transaction. */
    public TextTable deleteColumn(int column){index(column,columns);if(columns==1)throw new IllegalArgumentException("Cannot delete the last column");List<List<RichText>> next=mutableRows();for(List<RichText> row:next)row.remove(column);return rich(next,hasHeader);}
    private List<List<RichText>> mutableRows(){List<List<RichText>> next=new ArrayList<>();for(List<RichText> row:cells)next.add(new ArrayList<>(row));return next;}
    private static void index(int value,int size){if(value<0 || value>=size)throw new IllegalArgumentException("Invalid table index");}
    private static void edge(int value,int size){if(value<0 || value>size)throw new IllegalArgumentException("Invalid table insertion edge");}
}
