package app.space.notes.runtime;
import android.app.AlertDialog;
import android.view.*;
import android.widget.*;
/** Shared labelled grid used by Notes Insert and the Doc Insert sheet. */
final class InsertPalette {
 static void grid(NotesDesign design,LinearLayout panel,String[] names,String[] symbols,Runnable[] actions){
  Button measuring=design.textAction("",()->{});float widest=design.dp(64);for(String name:names)for(String word:name.split(" "))widest=Math.max(widest,measuring.getPaint().measureText(word));int available=design.dp(Math.min(560,design.context.getResources().getConfiguration().screenWidthDp)-48),columns=Math.max(1,Math.min(names.length,(int)(available/(widest+design.dp(24)))));
  for(int start=0;start<names.length;start+=columns){LinearLayout row=new LinearLayout(design.context);row.setBaselineAligned(false);for(int column=0;column<columns;column++){int index=start+column;LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);cell.setMargins(design.dp(2),design.dp(2),design.dp(2),design.dp(2));if(index>=names.length){row.addView(new View(design.context),cell);continue;}Button option=design.textAction(names[index],actions[index]);option.setGravity(Gravity.CENTER);option.setPadding(design.dp(6),design.dp(6),design.dp(6),design.dp(6));option.setMinHeight(design.dp(64));option.setBackground(design.ripple(design.surface,14));NotesIcon glyph=new NotesIcon(symbols[index],design.accent);glyph.setBounds(0,0,design.dp(24),design.dp(24));option.setCompoundDrawables(null,glyph,null,null);option.setCompoundDrawablePadding(design.dp(4));row.addView(option,cell);}panel.addView(row,new LinearLayout.LayoutParams(-1,-2));}
 }
 static void show(NotesDesign design,String title,boolean clipboard,String pasteName,java.util.function.IntConsumer choose){
  LinearLayout panel=new LinearLayout(design.context);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(design.dp(16),design.dp(8),design.dp(16),design.dp(16));ScrollView viewport=new ScrollView(design.context);viewport.addView(panel);
  AlertDialog dialog=new AlertDialog.Builder(design.context).setCustomTitle(design.editorTitle(title)).setView(viewport).setNegativeButton("Close tools",null).create();
  String[][] labels={{"Checklist","Table"},{"Image","Drawing","Diagram"},{"Read image","Record voice"},{"Hyperlink","Reminder"}};String[][] icons={{"checklist","table"},{"image","edit","diagram"},{"scan","mic"},{"link","bell"}};int[][] choices={{0,1},{2,3,4},{8,9},{6,5}};String[] groups={"Lists & tables","Visual content","Capture text","Hyperlink & reminder"};
  for(int g=0;g<groups.length;g++){TextView caption=design.label(groups[g],14,design.muted);caption.setPadding(0,design.dp(12),0,design.dp(6));panel.addView(caption);Runnable[] actions=new Runnable[choices[g].length];for(int i=0;i<actions.length;i++){final int choice=choices[g][i];actions[i]=()->{dialog.dismiss();choose.accept(choice);};}grid(design,panel,labels[g],icons[g],actions);}
  if(clipboard){panel.addView(design.divider());grid(design,panel,new String[]{pasteName},new String[]{"paste"},new Runnable[]{()->{dialog.dismiss();choose.accept(7);}});}
  design.showEditorSheet(dialog);
 }
}
