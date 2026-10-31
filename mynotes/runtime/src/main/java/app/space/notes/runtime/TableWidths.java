package app.space.notes.runtime;

import android.text.*;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import app.space.notes.domain.*;

/** Bounded content measurement shared by editing; never changes the document model. */
final class TableWidths {
 private TableWidths(){}
 static int[] screen(TextTable table,DisplayMetrics metrics){
  int[] widths=new int[table.columns];for(int column=0;column<widths.length;column++)widths[column]=column(table,column,metrics);return widths;
 }
 static int column(TextTable table,int column,DisplayMetrics metrics){
  if(table.columnWidth!=0)return table.columnWidth;
  TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(18*metrics.scaledDensity);
  float widest=0;for(java.util.List<RichText> row:table.cells){RichText rich=row.get(column);SpannableStringBuilder text=new SpannableStringBuilder(rich.text);TextStyles.write(text,rich.marks,metrics);widest=Math.max(widest,Layout.getDesiredWidth(text,paint));}
  return Math.max(80,Math.min(320,(int)Math.ceil(widest/metrics.density)+24));
 }
}
