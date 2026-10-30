package app.space.notes.domain;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TextTableTest {
    private TextTable sample(){return new TextTable(Arrays.asList(Arrays.asList(" A\n", "B"),Arrays.asList("C","D")));}
    @Test public void takesDeepImmutableSnapshotWithoutChangingWhitespace(){
        List<String> row=new ArrayList<>(Arrays.asList(" A\n","B"));
        List<List<String>> source=new ArrayList<>();source.add(row);TextTable table=new TextTable(source);
        row.set(0,"changed");source.clear();assertEquals(" A\n",table.rows.get(0).get(0));
        assertThrows(UnsupportedOperationException.class,()->table.rows.clear());
        assertThrows(UnsupportedOperationException.class,()->table.rows.get(0).clear());
    }
    @Test public void replacementLeavesUndoSnapshotIntact(){
        TextTable old=sample(),next=old.withCell(1,0,"new");
        assertEquals("C",old.rows.get(1).get(0));assertEquals("new",next.rows.get(1).get(0));
        assertSame(old,old.withCell(1,0,"C"));
    }
    @Test public void structuralInsertionAndDeletionPreserveEveryUnaffectedCell(){
        TextTable old=sample();
        TextTable next=old.insertRow(1).insertColumn(1);
        assertEquals(Arrays.asList(" A\n","","B"),next.rows.get(0));
        assertEquals(Arrays.asList("","",""),next.rows.get(1));
        assertEquals(Arrays.asList("C","","D"),next.rows.get(2));
        assertEquals(old.rows,next.deleteRow(1).deleteColumn(1).rows);
        assertEquals(old.rows,old.insertRow(0).deleteRow(0).rows);
        assertEquals(old.rows,old.insertColumn(2).deleteColumn(2).rows);
        assertEquals(old.rows,old.insertRow(2).deleteRow(2).rows);
    }
    @Test public void rejectsRaggedNullEmptyAndOversizedInputsWithoutPadding(){
        assertThrows(IllegalArgumentException.class,()->new TextTable(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,()->new TextTable(null));
        assertThrows(IllegalArgumentException.class,()->new TextTable(Arrays.asList(Arrays.asList("a"),Arrays.asList("b","c"))));
        assertThrows(IllegalArgumentException.class,()->new TextTable(Arrays.asList(Arrays.asList((String)null))));
        assertThrows(IllegalArgumentException.class,()->new TextTable(Arrays.asList((List<String>)null)));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(101,1));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(1,21));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(100,11));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(Integer.MAX_VALUE,Integer.MAX_VALUE));
    }
    @Test public void enforcesCellAndAggregateTextLimitsAtomically(){
        String full=String.join("",Collections.nCopies(4096,"x"));
        TextTable table=TextTable.empty(1,17);
        for(int c=0;c<16;c++)table=table.withCell(0,c,full);
        final TextTable atLimit=table;
        assertThrows(IllegalArgumentException.class,()->atLimit.withCell(0,16,"x"));
        assertEquals("",atLimit.rows.get(0).get(16));
        assertThrows(IllegalArgumentException.class,()->atLimit.withCell(0,0,full+"x"));
    }
    @Test public void structuralLimitsAndBadIndexesCannotChangeOriginal(){
        TextTable table=sample();
        assertThrows(IllegalArgumentException.class,()->table.withCell(-1,0,"x"));
        assertThrows(IllegalArgumentException.class,()->table.withCell(0,2,"x"));
        assertThrows(IllegalArgumentException.class,()->table.insertRow(3));
        assertThrows(IllegalArgumentException.class,()->table.insertColumn(-1));
        assertThrows(IllegalArgumentException.class,()->table.deleteRow(2));
        assertThrows(IllegalArgumentException.class,()->table.deleteColumn(-1));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(1,1).deleteRow(0));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(1,1).deleteColumn(0));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(100,10).insertRow(0));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(100,10).insertColumn(0));
        assertThrows(IllegalArgumentException.class,()->TextTable.empty(1,20).insertColumn(0));
        assertEquals("D",table.rows.get(1).get(1));
    }
}
