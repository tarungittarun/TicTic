package app.space.notes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable plain-text table draft. Not yet part of the persisted Note schema or editor.
 * Structural edits return snapshots suitable for a future document transaction/Undo layer.
 * No constructor normalizes, truncates, pads or silently discards supplied cell text.
 */
public final class TextTable {
    public static final int MAX_ROWS=100, MAX_COLUMNS=20, MAX_CELLS=1000;
    public static final int MAX_CELL_CHARACTERS=4096, MAX_CHARACTERS=65536;
    public final List<List<String>> rows;
    public final int columns;

    public TextTable(List<? extends List<String>> source) {
        if(source==null || source.isEmpty() || source.size()>MAX_ROWS)
            throw new IllegalArgumentException("Invalid table row count");
        if(source.get(0)==null)throw new IllegalArgumentException("Missing table row");
        int width=source.get(0).size();
        if(width<1 || width>MAX_COLUMNS || source.size()*width>MAX_CELLS)
            throw new IllegalArgumentException("Invalid table dimensions");
        List<List<String>> copy=new ArrayList<>();int characters=0;
        for(List<String> row:source){
            if(row==null || row.size()!=width)throw new IllegalArgumentException("Table must be rectangular");
            List<String> cells=new ArrayList<>();
            for(String cell:row){
                if(cell==null || cell.length()>MAX_CELL_CHARACTERS)
                    throw new IllegalArgumentException("Invalid table cell");
                characters+=cell.length();
                if(characters>MAX_CHARACTERS)throw new IllegalArgumentException("Table text limit exceeded");
                cells.add(cell);
            }
            copy.add(Collections.unmodifiableList(cells));
        }
        columns=width;rows=Collections.unmodifiableList(copy);
    }
    public static TextTable empty(int rowCount,int columnCount){
        if(rowCount<1 || rowCount>MAX_ROWS || columnCount<1 || columnCount>MAX_COLUMNS || rowCount*columnCount>MAX_CELLS)
            throw new IllegalArgumentException("Invalid table dimensions");
        List<List<String>> rows=new ArrayList<>();
        for(int i=0;i<rowCount;i++)rows.add(new ArrayList<>(Collections.nCopies(columnCount,"")));
        return new TextTable(rows);
    }
    public TextTable withCell(int row,int column,String text){
        index(row,rows.size());index(column,columns);
        if(text!=null && text.equals(rows.get(row).get(column)))return this;
        List<List<String>> next=mutableRows();next.get(row).set(column,text);return new TextTable(next);
    }
    public TextTable insertRow(int edge){
        edge(edge,rows.size());
        if(rows.size()==MAX_ROWS || (rows.size()+1)*columns>MAX_CELLS)
            throw new IllegalArgumentException("Table row limit exceeded");
        List<List<String>> next=mutableRows();next.add(edge,new ArrayList<>(Collections.nCopies(columns,"")));
        return new TextTable(next);
    }
    public TextTable insertColumn(int edge){
        edge(edge,columns);
        if(columns==MAX_COLUMNS || rows.size()*(columns+1)>MAX_CELLS)
            throw new IllegalArgumentException("Table column limit exceeded");
        List<List<String>> next=mutableRows();for(List<String> row:next)row.add(edge,"");return new TextTable(next);
    }
    /** Caller must provide deletion confirmation and document-level Undo before committing. */
    public TextTable deleteRow(int row){
        index(row,rows.size());if(rows.size()==1)throw new IllegalArgumentException("Cannot delete the last row");
        List<List<String>> next=mutableRows();next.remove(row);return new TextTable(next);
    }
    /** Caller must provide deletion confirmation and document-level Undo before committing. */
    public TextTable deleteColumn(int column){
        index(column,columns);if(columns==1)throw new IllegalArgumentException("Cannot delete the last column");
        List<List<String>> next=mutableRows();for(List<String> row:next)row.remove(column);return new TextTable(next);
    }
    private List<List<String>> mutableRows(){
        List<List<String>> next=new ArrayList<>();for(List<String> row:rows)next.add(new ArrayList<>(row));return next;
    }
    private static void index(int value,int size){if(value<0 || value>=size)throw new IllegalArgumentException("Invalid table index");}
    private static void edge(int value,int size){if(value<0 || value>size)throw new IllegalArgumentException("Invalid table insertion edge");}
}
