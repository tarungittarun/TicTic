package app.space.notes.runtime;
import android.app.*;
import android.graphics.RectF;
import android.view.*;
import android.widget.*;
import app.space.notes.domain.ImageTextSelection;
/** Full-screen selection; text is reviewed before insertion, visual crops are saved as pixels. */
final class ImageTextSelectionDialog {
 private ImageTextSelectionDialog(){}
 static Dialog show(NotesDesign d,OfflineOcr.Scan scan,java.util.function.Consumer<String> useText,java.util.function.Consumer<RectF> keepImage){
  OfflineOcr.Cancellation[] reading={null};AlertDialog[] child={null};Dialog dialog=new Dialog(d.context);dialog.setOnDismissListener(ignored->{if(reading[0]!=null){reading[0].cancel();reading[0]=null;}if(child[0]!=null){child[0].dismiss();child[0]=null;}});dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout root=new LinearLayout(d.context);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(d.dp(8),d.dp(8),d.dp(8),d.dp(8));root.setBackgroundColor(d.background);
  LinearLayout heading=new LinearLayout(d.context);TextView title=d.heading("Select from image");title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));heading.addView(d.icon("Close image selection","close",dialog::dismiss),new LinearLayout.LayoutParams(d.dp(48),d.dp(48)));root.addView(heading);
  TextView help=d.label("Drag a box, then tap Read this area.",13,d.muted);help.setSingleLine(true);help.setEllipsize(android.text.TextUtils.TruncateAt.END);help.setContentDescription("Text regions only, not universal object or emoji recognition. Drag any visual area to keep it as an image. Original image stays unchanged.");help.setMinHeight(d.dp(48));help.setOnClickListener(v->showChild(d,child,new AlertDialog.Builder(d.context).setTitle("Read text from an image").setMessage("Tap highlighted words or drag a box around the text you want. Check the selected text shown below the image, then tap Use selected text. Nothing is saved until you confirm in the next screen.\n\nPinch to zoom and use two fingers to move around. After dragging a box, tap Read this area to read that part again. Zoom alone only enlarges the picture. Tiny text and symbols may be wrong or missing.\n\nKeep selected image saves a picture of your selection instead. Your original stays unchanged. Everything is processed on this device.").setPositiveButton("OK",null).create()));View info=d.icon("Read text from an image","info",()->help.performClick());heading.addView(info,heading.getChildCount()-1,new LinearLayout.LayoutParams(d.dp(48),d.dp(48)));root.addView(help);
  ImageTextSelection selection=new ImageTextSelection(scan.result.words);ImageTextSelectionView view=new ImageTextSelectionView(d.context,scan.image,selection);LinearLayout body=new LinearLayout(d.context);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));body.addView(view);ScrollView side=new ScrollView(d.context);side.setFillViewport(false);LinearLayout controls=new LinearLayout(d.context);controls.setOrientation(LinearLayout.VERTICAL);side.addView(controls);body.addView(side);TextView count=d.label("",13,d.muted);count.setSingleLine(true);count.setEllipsize(android.text.TextUtils.TruncateAt.END);controls.addView(count);TextView preview=d.label("",15,d.ink);preview.setMaxLines(2);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);preview.setContentDescription("Selected text preview");controls.addView(preview);
  LinearLayout tools=row(controls,d);Button all=d.textAction("Select all",()->view.all(true)),clear=d.textAction("Clear",()->view.all(false));tools.addView(all);tools.addView(clear);tools.addView(d.textAction("Choose words",()->{if(selection.words.isEmpty())return;String[] labels=new String[selection.words.size()];boolean[] checked=new boolean[labels.length];for(int i=0;i<labels.length;i++){labels[i]=selection.words.get(i).text;checked[i]=selection.selected(i);}showChild(d,child,new AlertDialog.Builder(d.context).setTitle("Choose recognized words").setMultiChoiceItems(labels,checked,(box,which,value)->{if(selection.selected(which)!=value)selection.toggle(which);view.refresh();}).setPositiveButton("Done",null).create());}));tools.addView(d.textAction("Reset zoom",view::resetZoom));
  LinearLayout actions=row(controls,d);Button extract=d.textAction("Use selected text",()->{try{String text=selection.text();if(!text.trim().isEmpty()){useText.accept(text);dialog.dismiss();}}catch(IllegalArgumentException invalid){android.widget.Toast.makeText(d.context,invalid.getMessage()+". Existing text is unchanged.",Toast.LENGTH_LONG).show();}});Button keep=d.textAction("Keep selected image",()->{RectF crop=view.crop();if(crop!=null && crop.width()>0 && crop.height()>0){dialog.dismiss();keepImage.accept(crop);}});extract.setContentDescription("Use selected text");keep.setContentDescription("Keep selected image");actions.addView(extract);actions.addView(keep);View actionContainer=(View)actions.getParent();controls.removeView(actionContainer);controls.addView(actionContainer,1);
  Button[] reread={null};reread[0]=d.textAction("Read this area",()->{
   RectF box=view.crop();if(box==null || box.width()<=0 || box.height()<=0)return;
   reread[0].setEnabled(false);reread[0].setText("Reading area…");
   reading[0]=ImageRegionText.read(d.context,scan,box,(recognized,failure)->{
    reading[0]=null;if(!dialog.isShowing())return;reread[0].setText("Read this area");reread[0].setEnabled(true);
    if(!failure.isEmpty()){Toast.makeText(d.context,failure,Toast.LENGTH_LONG).show();return;}
    if(recognized.trim().isEmpty()){Toast.makeText(d.context,"No text found in this area. Try another selection.",Toast.LENGTH_LONG).show();return;}
    EditText review=new EditText(d.context);review.setText(recognized);review.setTextColor(d.ink);review.setTextSize(17);review.setMinLines(3);review.setMaxLines(8);review.setContentDescription("Review text read from selected area");
    showChild(d,child,new AlertDialog.Builder(d.context).setTitle("Check this text").setMessage("Small text and symbols may be wrong. Edit before using. Your image is unchanged.").setView(review).setNegativeButton("Cancel",null).setPositiveButton("Use this text",(boxDialog,which)->{String text=review.getText().toString();if(!text.trim().isEmpty()){useText.accept(text);dialog.dismiss();}}).create());
   });
  });tools.addView(reread[0],0);
  // Activity retains this dialog on rotation. Reflow existing views, never recreate
  // the selection, crop, callbacks or bitmap; window bounds also cover multi-window.
  final int[] mode={-1,-1};root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
   if(r<=l || b<=t)return;boolean wide=r-l>b-t;int next=wide?1:0;if(mode[0]==next && mode[1]==r-l)return;mode[0]=next;mode[1]=r-l;int sideWidth=Math.min(d.dp(240),Math.max(d.dp(176),(r-l)/3));
   body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
   view.setLayoutParams(wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
   side.setLayoutParams(wide?new LinearLayout.LayoutParams(sideWidth,-1):new LinearLayout.LayoutParams(-1,-2));
   tools.setOrientation(wide?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);actions.setOrientation(wide?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
   tools.setLayoutParams(new android.widget.FrameLayout.LayoutParams(wide?sideWidth:-2,-2));actions.setLayoutParams(new android.widget.FrameLayout.LayoutParams(wide?sideWidth:-2,-2));
   help.setVisibility(wide?View.GONE:View.VISIBLE);info.setVisibility(wide?View.VISIBLE:View.GONE);
   extract.setText(wide?"Use text":"Use selected text");keep.setText(wide?"Keep image":"Keep selected image");
  });
  view.onChanged(()->{int n=selection.count();try{preview.setText(n==0?"":selection.text());}catch(IllegalArgumentException invalid){preview.setText("Selection is too large. Choose a smaller area.");}count.setText(selection.words.isEmpty()?"No text found. You can still save an image area.":n+" of "+selection.words.size()+" words selected");extract.setEnabled(n>0);RectF box=view.crop();keep.setEnabled(box!=null && box.width()>0 && box.height()>0);});view.all(true);
  dialog.setContentView(root);dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);return dialog;
 }
 private static void showChild(NotesDesign d,AlertDialog[] child,AlertDialog next){if(child[0]!=null)child[0].dismiss();child[0]=next;next.setOnDismissListener(ignored->{if(child[0]==next)child[0]=null;});d.show(next);}
 private static LinearLayout row(LinearLayout root,NotesDesign d){HorizontalScrollView scroll=new HorizontalScrollView(d.context);scroll.setHorizontalScrollBarEnabled(true);LinearLayout row=new LinearLayout(d.context);scroll.addView(row);root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return row;}
}
