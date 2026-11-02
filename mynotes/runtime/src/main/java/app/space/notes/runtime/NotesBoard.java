package app.space.notes.runtime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.text.DateFormat;
import java.util.*;
import app.space.notes.domain.Note;

/** Virtualised masonry/list board. Search swaps data, never the input or view tree. */
public final class NotesBoard extends RecyclerView {
    public interface Listener {void open(Note note);void select(Note note);default void workspace(boolean documents){} default boolean canReorder(){return false;} default void reorder(List<String> ids){} }
    public static final String[] NAMES={"Default","Red","Orange","Yellow","Green","Teal","Blue","Dark blue","Purple","Pink","Brown","Gray"};
    private static final String[] DARK={"#2A2C31","#60302C","#5F4122","#5D5526","#2C4F3A","#24514E","#2A4E6C","#33487C","#4A3570","#63374F","#4C3D33","#3E4147"};
    private static final String[] LIGHT={"#FFFFFF","#F28B82","#FCBCA4","#FDF49F","#CCFF90","#A7FFEB","#CBF0F8","#AECBFA","#D7AEFB","#FBCFE8","#E6C9A8","#E8EAED"};
    public static int colour(int index,boolean dark){return Color.parseColor((dark?DARK:LIGHT)[index]);}
    private final List<Object> rows=new ArrayList<>();private final Set<String> selection=new HashSet<>();
    private final NotesDesign design;private final boolean dark;private final Listener listener;private final BoardAdapter adapter=new BoardAdapter();
    private ItemTouchHelper dragHelper;private List<Object> dragBefore;private boolean dragging,dropAccepted;private Note dragSource;
    public NotesBoard(Context c,boolean dark,Listener listener,boolean grid){
        this(c,new NotesDesign(c,dark),listener,grid);
    }
    public NotesBoard(Context c,NotesDesign design,Listener listener,boolean grid){
        super(c);this.design=design;this.dark=design.dark;this.listener=listener;setItemAnimator(null);setAdapter(adapter);layout(grid);
        dragHelper=new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP|ItemTouchHelper.DOWN|ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT,0){
            @Override public boolean isLongPressDragEnabled(){return false;}
            @Override public boolean isItemViewSwipeEnabled(){return false;}
            @Override public boolean canDropOver(RecyclerView r,ViewHolder a,ViewHolder b){int x=a.getBindingAdapterPosition(),y=b.getBindingAdapterPosition();return x>=0 && y>=0 && x<rows.size() && y<rows.size() && rows.get(x) instanceof Note && rows.get(y) instanceof Note && ((Note)rows.get(x)).pinned==((Note)rows.get(y)).pinned;}
            @Override public boolean onMove(RecyclerView r,ViewHolder a,ViewHolder b){if(!canDropOver(r,a,b))return false;int from=a.getBindingAdapterPosition(),to=b.getBindingAdapterPosition();Object item=rows.remove(from);rows.add(to,item);adapter.notifyItemMoved(from,to);return true;}
            @Override public void onSwiped(ViewHolder h,int direction){}
            @Override public void onSelectedChanged(ViewHolder h,int state){if(state==ItemTouchHelper.ACTION_STATE_DRAG){dragBefore=new ArrayList<>(rows);int source=h==null?-1:h.getBindingAdapterPosition();dragSource=source>=0 && rows.get(source) instanceof Note?(Note)rows.get(source):null;dragging=true;dropAccepted=false;}super.onSelectedChanged(h,state);}
            @Override public void clearView(RecyclerView r,ViewHolder h){super.clearView(r,h);if(dragBefore==null)return;List<Object> before=dragBefore;dragBefore=null;dragging=false;if(!dropAccepted || !listener.canReorder()){rows.clear();rows.addAll(before);adapter.notifyDataSetChanged();return;}List<String> ids=new ArrayList<>();for(Object row:rows)if(row instanceof Note)ids.add(((Note)row).id);if(!rows.equals(before))listener.reorder(ids);else if(dragSource!=null)listener.select(dragSource);dragSource=null;}
        });dragHelper.attachToRecyclerView(this);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(dragging && (event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL))dropAccepted=event.getActionMasked()==MotionEvent.ACTION_UP;return super.dispatchTouchEvent(event);}
    private float swipeX,swipeY;private boolean workspaceSwipe;
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){swipeX=e.getX();swipeY=e.getY();workspaceSwipe=false;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && !dragging && selection.isEmpty() && Math.abs(e.getX()-swipeX)>dp(48) && Math.abs(e.getX()-swipeX)>1.5f*Math.abs(e.getY()-swipeY)){workspaceSwipe=true;return true;}
        return super.onInterceptTouchEvent(e);
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){swipeX=e.getX();swipeY=e.getY();workspaceSwipe=false;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE && !dragging && selection.isEmpty() && Math.abs(e.getX()-swipeX)>dp(48) && Math.abs(e.getX()-swipeX)>1.5f*Math.abs(e.getY()-swipeY))workspaceSwipe=true;if(!workspaceSwipe)return super.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP){boolean docs=e.getX()<swipeX;workspaceSwipe=false;performClick();listener.workspace(docs);}else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)workspaceSwipe=false;return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    public void layout(boolean grid){int width=getResources().getConfiguration().screenWidthDp;float scale=getResources().getConfiguration().fontScale;int columns=scale>1.3f?Math.max(1,Math.min(3,width/320)):Math.max(2,Math.min(4,width/240));setLayoutManager(grid?new StaggeredGridLayoutManager(columns,StaggeredGridLayoutManager.VERTICAL):new LinearLayoutManager(getContext()));}
    public void show(List<Note> notes,Set<String> selected,boolean sections){
        List<Object> previous=new ArrayList<>(rows);Set<String> oldSelection=new HashSet<>(selection);rows.clear();selection.clear();selection.addAll(selected);
        boolean pinned=false,others=false;
        for(Note note:notes){
            if(sections && note.pinned && !pinned){rows.add("Pinned");pinned=true;}
            if(sections && !note.pinned && !others){if(pinned)rows.add("Others");others=true;}
            rows.add(note);
        }
        if(rows.equals(previous)){for(int i=0;i<rows.size();i++)if(rows.get(i) instanceof Note){String id=((Note)rows.get(i)).id;if(oldSelection.contains(id)!=selection.contains(id))adapter.notifyItemChanged(i,"selection");}}else adapter.notifyDataSetChanged();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(int size){TextView v=new TextView(getContext());v.setTextSize(size);return v;}
    private final class Holder extends ViewHolder {
        boolean touching;app.space.notes.domain.NoteImage previewImage;final TextView title,body,tags;final LinearLayout box;final FrameLayout preview;
        Holder(LinearLayout box,TextView title,TextView body,TextView tags,FrameLayout preview){super(box);this.box=box;this.title=title;this.body=body;this.tags=tags;this.preview=preview;}
    }
    private final class BoardAdapter extends Adapter<Holder> {
        @Override public int getItemCount(){return rows.size();}
        @Override public int getItemViewType(int position){return rows.get(position) instanceof String?0:1;}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
            LinearLayout box=new LinearLayout(getContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(12),dp(12),dp(12));box.setMinimumHeight(type==0?0:dp(64));
            RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(dp(4),dp(4),dp(4),dp(4));box.setLayoutParams(lp);
            TextView title=label(type==0?13:18),body=label(15),tags=label(11);
            title.setTypeface(null,Typeface.BOLD);title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);tags.setMaxLines(3);tags.setEllipsize(TextUtils.TruncateAt.END);box.addView(title);
            FrameLayout preview=null;if(type!=0){preview=new FrameLayout(getContext());preview.setVisibility(GONE);box.addView(preview,new LinearLayout.LayoutParams(-1,dp(140)));body.setMaxLines(7);body.setEllipsize(TextUtils.TruncateAt.END);box.addView(body);box.addView(tags);}
            return new Holder(box,title,body,tags,preview);
        }
        @Override public void onBindViewHolder(Holder h,int position){
            Object row=rows.get(position);
            if(h.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)
                ((StaggeredGridLayoutManager.LayoutParams)h.itemView.getLayoutParams()).setFullSpan(row instanceof String);
            if(row instanceof String){h.title.setText((String)row);h.title.setTextColor(design.muted);h.box.setBackground(null);h.box.setOnClickListener(null);h.box.setOnLongClickListener(null);return;}
            Note n=(Note)row;if(h.preview!=null){app.space.notes.domain.NoteImage image=n.images.isEmpty()?null:n.images.get(0);if(h.previewImage!=image){h.preview.removeAllViews();h.previewImage=image;if(image!=null)h.preview.addView(new NoteImageView(getContext(),image),new FrameLayout.LayoutParams(-1,-1));}h.preview.setVisibility(image==null?GONE:VISIBLE);}boolean chosen=selection.contains(n.id);int ink=n.color==0?design.ink:Color.parseColor(dark?"#F5F5F5":"#202124");
            GradientDrawable bg=new GradientDrawable();bg.setColor(n.color==0?design.surface:colour(n.color,dark));bg.setCornerRadius(dp(16));
            bg.setStroke(dp(chosen?2:1),chosen?design.accent:design.outline);h.box.setBackground(n.background==0?bg:new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{new NoteBackground(n.background,n.color==0?design.surface:colour(n.color,dark),dark,getResources().getDisplayMetrics().density),border(chosen)}));
            h.title.setText((chosen?"✓  ":"")+(n.title.isEmpty()?(n.checklist?"Checklist":"Untitled"):n.title));
            h.title.setTextColor(ink);h.body.setTextColor(ink);h.tags.setTextColor(ink);h.body.setLineSpacing(dp(2),1.05f);
            h.title.setVisibility(n.title.isEmpty() && !n.checklist && !chosen && !n.pinned && !n.body.isEmpty()?GONE:VISIBLE);NotesIcon pin=n.pinned?new NotesIcon("pinned",ink):null;if(pin!=null)pin.setBounds(0,0,dp(16),dp(16));h.title.setCompoundDrawablesRelative(pin,null,null,null);h.title.setCompoundDrawablePadding(dp(5));
            String preview=n.plainText();if(!n.diagram.nodes.isEmpty())preview+=(preview.isEmpty()?"":"\n")+"▧ Diagram · "+n.diagram.nodes.size()+" shapes";String excerpt=preview.length()>400?preview.substring(0,400)+"…":preview;android.text.SpannableStringBuilder rich=new android.text.SpannableStringBuilder(excerpt);
            if(!n.checklist)for(Note.Mark mark:app.space.notes.domain.TextFormatting.normalizeParagraphs(n.marks,n.body)){int end=Math.min(mark.end,Math.min(n.body.length(),excerpt.length()));if(mark.start<end)TextStyles.writeRange(rich,new Note.Mark(mark.type,mark.start,end),0);}h.body.setText(rich);
            h.body.setVisibility(preview.isEmpty()?GONE:VISIBLE);
            List<String> metadata=new ArrayList<>(n.labels);if(app.space.notes.domain.Workspace.isDocument(n))metadata.add(0,"Doc");if(!n.diagram.nodes.isEmpty())metadata.add("◇ Diagram");if(!n.folder.isEmpty())metadata.add(n.folder);if(!n.images.isEmpty())metadata.add(n.images.size()+" image"+(n.images.size()==1?"":"s"));h.tags.setText(String.join(" · ",metadata));h.tags.setVisibility(metadata.isEmpty()?GONE:VISIBLE);
            h.box.setContentDescription((chosen?"Selected. ":"")+(n.pinned?"Pinned. ":"")+h.title.getText()+". "+(preview.length()>400?preview.substring(0,400):preview)+". Long press and drag to arrange, or long press to select when arrangement is unavailable.");
            h.touching=false;h.box.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN)h.touching=true;else if(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL)h.touching=false;return false;});
            h.box.setOnClickListener(v->listener.open(n));h.box.setOnLongClickListener(v->{if(h.touching && listener.canReorder()){v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);dragHelper.startDrag(h);}else listener.select(n);return true;});
        }
        @Override public void onViewAttachedToWindow(Holder h){super.onViewAttachedToWindow(h);int p=h.getBindingAdapterPosition();if(p>=0 && h.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)((StaggeredGridLayoutManager.LayoutParams)h.itemView.getLayoutParams()).setFullSpan(rows.get(p) instanceof String);}
    }
    private android.graphics.drawable.Drawable border(boolean selected){GradientDrawable d=new GradientDrawable();d.setColor(Color.TRANSPARENT);d.setCornerRadius(dp(16));d.setStroke(dp(selected?2:1),selected?design.accent:design.outline);return d;}
}
