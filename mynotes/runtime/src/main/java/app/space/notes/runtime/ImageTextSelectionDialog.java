package app.space.notes.runtime;
import android.app.*;
import android.graphics.RectF;
import android.view.*;
import android.widget.*;
import app.space.notes.domain.ImageTextSelection;

/** Compact, on-device image selection and OCR review. */
final class ImageTextSelectionDialog {
 private ImageTextSelectionDialog(){}
 static Dialog show(NotesDesign d,OfflineOcr.Scan scan,java.util.function.Consumer<String> useText,java.util.function.Consumer<RectF> keepImage){
  OfflineOcr.Cancellation[] reading={null};AlertDialog[] child={null};Dialog dialog=new Dialog(d.context);dialog.setOnDismissListener(ignored->{if(reading[0]!=null){reading[0].cancel();reading[0]=null;}if(child[0]!=null){child[0].dismiss();child[0]=null;}});dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  AdaptiveRoot root=new AdaptiveRoot(d.context);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(d.dp(8),d.dp(8),d.dp(8),d.dp(8));root.setBackgroundColor(d.background);
  LinearLayout heading=new LinearLayout(d.context);TextView title=d.heading("Select from image");title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));heading.addView(d.icon("Close image selection","close",dialog::dismiss),new LinearLayout.LayoutParams(d.dp(48),d.dp(48)));root.addView(heading);

  ImageTextSelection selection=new ImageTextSelection(scan.result.words);ImageTextSelectionView view=new ImageTextSelectionView(d.context,scan.image,selection);
  LinearLayout body=new LinearLayout(d.context);body.setOrientation(LinearLayout.VERTICAL);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));body.addView(view,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout sidePanel=new LinearLayout(d.context);sidePanel.setOrientation(LinearLayout.VERTICAL);body.addView(sidePanel,new LinearLayout.LayoutParams(-1,-2));
  LinearLayout actions=new LinearLayout(d.context);
  Button extract=d.textAction("Use text",()->{try{String text=selection.text();if(!text.trim().isEmpty()){useText.accept(text);dialog.dismiss();}}catch(IllegalArgumentException invalid){Toast.makeText(d.context,invalid.getMessage(),Toast.LENGTH_LONG).show();}});
  Button keep=d.textAction("Keep image",()->{RectF crop=view.crop();if(crop!=null && crop.width()>0 && crop.height()>0){dialog.dismiss();keepImage.accept(crop);}});
  compactAction(extract,d);compactAction(keep,d);extract.setContentDescription("Use selected text");keep.setContentDescription("Keep selected image");actions.addView(extract);actions.addView(keep);sidePanel.addView(actions);

  Button[] allRef={null},clearRef={null},chooseWordsRef={null},copyRef={null},reread={null};TextView[] countRef={null};
  reread[0]=d.textAction("Read this area",()->{
   RectF box=view.crop();if(box==null || box.width()<=0 || box.height()<=0)return;
   view.setEnabled(false);for(Button control:new Button[]{allRef[0],clearRef[0],chooseWordsRef[0],copyRef[0],extract,keep})if(control!=null)control.setEnabled(false);reread[0].setEnabled(false);reread[0].setText("Reading area…");if(countRef[0]!=null)countRef[0].setText("Reading…");
   reading[0]=ImageRegionText.read(d.context,scan,box,(recognized,failure)->{
    reading[0]=null;if(!dialog.isShowing())return;view.setEnabled(true);for(Button control:new Button[]{allRef[0],clearRef[0],chooseWordsRef[0],copyRef[0],extract,keep})if(control!=null)control.setEnabled(true);reread[0].setText("Read this area");reread[0].setEnabled(true);view.refresh();
    if(!failure.isEmpty()){Toast.makeText(d.context,failure,Toast.LENGTH_LONG).show();return;}
    if(recognized.trim().isEmpty()){Toast.makeText(d.context,"No text found.",Toast.LENGTH_LONG).show();return;}
    EditText review=new EditText(d.context);review.setText(recognized);review.setTextColor(d.ink);review.setTextSize(17);review.setMinLines(3);review.setMaxLines(8);review.setContentDescription("Review text read from selected area");
    showChild(d,child,new AlertDialog.Builder(d.context).setTitle("Review text").setView(review).setNegativeButton("Cancel",null).setPositiveButton("Use text",(boxDialog,which)->{String text=review.getText().toString();if(!text.trim().isEmpty()){useText.accept(text);dialog.dismiss();}}).create());
   });
  });reread[0].setContentDescription("Read this area");sidePanel.addView(reread[0],new LinearLayout.LayoutParams(-1,d.dp(48)));

  ScrollView details=new ScrollView(d.context);details.setFillViewport(false);details.setVerticalScrollBarEnabled(true);details.setScrollbarFadingEnabled(false);LinearLayout controls=new LinearLayout(d.context);controls.setOrientation(LinearLayout.VERTICAL);details.addView(controls);sidePanel.addView(details);
  TextView count=d.label("",13,d.muted);count.setSingleLine(true);count.setEllipsize(android.text.TextUtils.TruncateAt.END);controls.addView(count);countRef[0]=count;
  TextView preview=d.label("",15,d.ink);preview.setMaxLines(2);preview.setEllipsize(android.text.TextUtils.TruncateAt.END);preview.setContentDescription("Selected text preview");controls.addView(preview);
  LinearLayout tools=row(controls,d);allRef[0]=d.icon("Select all","check",()->view.all(true));clearRef[0]=d.icon("Clear","close",()->view.all(false));tools.addView(allRef[0]);tools.addView(clearRef[0]);
  chooseWordsRef[0]=d.icon("Choose words","text",()->{if(selection.words.isEmpty())return;String[] labels=new String[selection.words.size()];boolean[] checked=new boolean[labels.length];for(int i=0;i<labels.length;i++){labels[i]=selection.words.get(i).text;checked[i]=selection.selected(i);}showChild(d,child,new AlertDialog.Builder(d.context).setTitle("Choose words").setMultiChoiceItems(labels,checked,(box,which,value)->{if(selection.selected(which)!=value)selection.toggle(which);view.refresh();}).setPositiveButton("Done",null).create());});tools.addView(chooseWordsRef[0]);
  copyRef[0]=d.icon("Copy selected text","copy",()->{try{String value=selection.text();if(value.trim().isEmpty())return;android.content.ClipboardManager clipboard=(android.content.ClipboardManager)d.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);if(clipboard==null){Toast.makeText(d.context,"Clipboard is unavailable.",Toast.LENGTH_LONG).show();return;}clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Selected image text",value));Toast.makeText(d.context,"Copied",Toast.LENGTH_SHORT).show();}catch(IllegalArgumentException invalid){Toast.makeText(d.context,"Choose a smaller selection.",Toast.LENGTH_LONG).show();}});tools.addView(copyRef[0]);
  for(int i=0;i<tools.getChildCount();i++)tools.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(d.dp(48),d.dp(48)));
  preview.setOnClickListener(v->{if(selection.count()>0)try{showChild(d,child,new AlertDialog.Builder(d.context).setTitle("Selected text").setMessage(selection.text()).setPositiveButton("Done",null).create());}catch(IllegalArgumentException invalid){Toast.makeText(d.context,"Choose a smaller area.",Toast.LENGTH_LONG).show();}});preview.setMinHeight(d.dp(48));d.primary(extract);

  final int[] mode={-1,-1,-1,-1};root.reflow=(width,height)->{
   if(width<=0 || height<=0)return;boolean wide=width>height;int fontMode=Math.round(d.context.getResources().getConfiguration().fontScale*100f);if(mode[0]==(wide?1:0) && mode[1]==width && mode[2]==height && mode[3]==fontMode)return;mode[0]=wide?1:0;mode[1]=width;mode[2]=height;mode[3]=fontMode;
   boolean stackActions=wide || fontMode>=150;int sideWidth=Math.min(d.dp(240),Math.max(d.dp(176),width/3));body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
   view.setLayoutParams(wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));sidePanel.setLayoutParams(wide?new LinearLayout.LayoutParams(sideWidth,-1):new LinearLayout.LayoutParams(-1,-2));
   tools.setOrientation(wide?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);actions.setOrientation(stackActions?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
   tools.setLayoutParams(new FrameLayout.LayoutParams(wide?sideWidth:-2,-2));actions.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));extract.setLayoutParams(new LinearLayout.LayoutParams(stackActions?-1:0,d.dp(48),stackActions?0:1));keep.setLayoutParams(new LinearLayout.LayoutParams(stackActions?-1:0,d.dp(48),stackActions?0:1));
   extract.setText("Use text");keep.setText("Keep image");extract.setMinHeight(d.dp(48));keep.setMinHeight(d.dp(48));
   int secondaryHeight=0;if(!wide){int contentWidth=Math.max(0,width-root.getPaddingLeft()-root.getPaddingRight());int topChrome=root.getPaddingTop()+root.getPaddingBottom()+measureHeightAtWidth(heading,contentWidth);int pinned=measureHeightAtWidth(actions,contentWidth)+measureHeightAtWidth(reread[0],contentWidth);int minimumImage=Math.round(height*.40f);secondaryHeight=Math.max(0,Math.min(d.dp(160),height-topChrome-pinned-minimumImage));}
   details.setLayoutParams(wide?new LinearLayout.LayoutParams(-1,0,1):new LinearLayout.LayoutParams(-1,secondaryHeight));
  };
  view.onChanged(()->{int n=selection.count();try{preview.setText(n==0?"":selection.text());}catch(IllegalArgumentException invalid){preview.setText("Selection too large.");}count.setText(selection.words.isEmpty()?"No text detected":n+" / "+selection.words.size());extract.setEnabled(n>0);copyRef[0].setEnabled(n>0);RectF box=view.crop();keep.setEnabled(box!=null && box.width()>0 && box.height()>0);});view.all(true);
  dialog.setContentView(root);dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,-1);return dialog;
 }
 private static final class AdaptiveRoot extends LinearLayout {
  java.util.function.BiConsumer<Integer,Integer> reflow;AdaptiveRoot(android.content.Context context){super(context);}
  @Override protected void onMeasure(int width,int height){if(reflow!=null)reflow.accept(View.MeasureSpec.getSize(width),View.MeasureSpec.getSize(height));super.onMeasure(width,height);}
 }
 private static int measureHeightAtWidth(View child,int width){if(child.getVisibility()!=View.VISIBLE)return 0;ViewGroup.LayoutParams params=child.getLayoutParams();int heightSpec=View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);if(params!=null && params.height>=0)heightSpec=View.MeasureSpec.makeMeasureSpec(params.height,View.MeasureSpec.EXACTLY);child.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0,width),View.MeasureSpec.EXACTLY),heightSpec);return child.getMeasuredHeight();}
 private static void compactAction(Button button,NotesDesign d){button.setMinHeight(d.dp(48));button.setMinimumHeight(d.dp(48));button.setSingleLine(true);button.setEllipsize(android.text.TextUtils.TruncateAt.END);button.setPadding(d.dp(12),0,d.dp(12),0);}
 private static void showChild(NotesDesign d,AlertDialog[] child,AlertDialog next){if(child[0]!=null)child[0].dismiss();child[0]=next;next.setOnDismissListener(ignored->{if(child[0]==next)child[0]=null;});d.show(next);}
 private static LinearLayout row(LinearLayout root,NotesDesign d){HorizontalScrollView scroll=new HorizontalScrollView(d.context);scroll.setHorizontalScrollBarEnabled(true);LinearLayout row=new LinearLayout(d.context);scroll.addView(row);root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return row;}
}
