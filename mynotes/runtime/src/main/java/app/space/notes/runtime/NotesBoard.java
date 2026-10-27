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
    public interface Listener {void open(Note note);void select(Note note);}
    public static final String[] NAMES={"Default","Red","Orange","Yellow","Green","Teal","Blue","Dark blue","Purple","Pink","Brown","Gray"};
    private static final String[] DARK={"#2A2C31","#60302C","#5F4122","#5D5526","#2C4F3A","#24514E","#2A4E6C","#33487C","#4A3570","#63374F","#4C3D33","#3E4147"};
    private static final String[] LIGHT={"#FFFFFF","#F28B82","#FCBCA4","#FDF49F","#CCFF90","#A7FFEB","#CBF0F8","#AECBFA","#D7AEFB","#FBCFE8","#E6C9A8","#E8EAED"};
    public static int colour(int index,boolean dark){return Color.parseColor((dark?DARK:LIGHT)[index]);}
    private final List<Object> rows=new ArrayList<>();private final Set<String> selection=new HashSet<>();
    private final boolean dark;private final Listener listener;private final BoardAdapter adapter=new BoardAdapter();
    public NotesBoard(Context c,boolean dark,Listener listener,boolean grid){
        super(c);this.dark=dark;this.listener=listener;setItemAnimator(null);setAdapter(adapter);layout(grid);
    }
    public void layout(boolean grid){setLayoutManager(grid?new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL):new LinearLayoutManager(getContext()));}
    public void show(List<Note> notes,Set<String> selected,boolean sections){
        rows.clear();selection.clear();selection.addAll(selected);
        boolean pinned=false,others=false;
        for(Note note:notes){
            if(sections && note.pinned && !pinned){rows.add("Pinned");pinned=true;}
            if(sections && !note.pinned && !others){rows.add(pinned?"Others":"Notes");others=true;}
            rows.add(note);
        }
        adapter.notifyDataSetChanged();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(int size){TextView v=new TextView(getContext());v.setTextSize(size);return v;}
    private final class Holder extends ViewHolder {
        final TextView title,body,tags;final LinearLayout box;
        Holder(LinearLayout box,TextView title,TextView body,TextView tags){super(box);this.box=box;this.title=title;this.body=body;this.tags=tags;}
    }
    private final class BoardAdapter extends Adapter<Holder> {
        @Override public int getItemCount(){return rows.size();}
        @Override public int getItemViewType(int position){return rows.get(position) instanceof String?0:1;}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
            LinearLayout box=new LinearLayout(getContext());box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(12),dp(12),dp(12));
            RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(dp(4),dp(4),dp(4),dp(4));box.setLayoutParams(lp);
            TextView title=label(type==0?13:18),body=label(15),tags=label(11);
            title.setTypeface(null,Typeface.BOLD);title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);tags.setMaxLines(3);tags.setEllipsize(TextUtils.TruncateAt.END);box.addView(title);
            if(type!=0){body.setMaxLines(7);body.setEllipsize(TextUtils.TruncateAt.END);box.addView(body);box.addView(tags);}
            return new Holder(box,title,body,tags);
        }
        @Override public void onBindViewHolder(Holder h,int position){
            Object row=rows.get(position);
            if(h.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)
                ((StaggeredGridLayoutManager.LayoutParams)h.itemView.getLayoutParams()).setFullSpan(row instanceof String);
            if(row instanceof String){h.title.setText((String)row);h.title.setTextColor(Color.parseColor(dark?"#B9BCC6":"#625C50"));h.box.setBackground(null);h.box.setOnClickListener(null);h.box.setOnLongClickListener(null);return;}
            Note n=(Note)row;boolean chosen=selection.contains(n.id);int ink=Color.parseColor(dark?"#F5F5F5":"#202124");
            GradientDrawable bg=new GradientDrawable();bg.setColor(colour(n.color,dark));bg.setCornerRadius(dp(16));
            if(chosen)bg.setStroke(dp(3),Color.parseColor("#E8B33C"));h.box.setBackground(bg);
            h.title.setText((chosen?"✓  ":n.pinned?"★  ":"")+(n.title.isEmpty()?(n.checklist?"Checklist":"Untitled"):n.title));
            h.title.setTextColor(ink);h.body.setTextColor(ink);h.tags.setTextColor(ink);
            String preview=n.plainText();h.body.setText(preview.length()>400?preview.substring(0,400)+"…":preview);
            h.body.setVisibility(preview.isEmpty()?GONE:VISIBLE);
            h.tags.setText((n.labels.isEmpty()?"":String.join(" · ",n.labels)+"\n")+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(n.updatedAt)));
            h.box.setContentDescription((chosen?"Selected. ":"")+h.title.getText()+". "+(preview.length()>400?preview.substring(0,400):preview)+". Long press to select.");
            h.box.setOnClickListener(v->listener.open(n));h.box.setOnLongClickListener(v->{listener.select(n);return true;});
        }
        @Override public void onViewAttachedToWindow(Holder h){super.onViewAttachedToWindow(h);int p=h.getBindingAdapterPosition();if(p>=0 && h.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams)((StaggeredGridLayoutManager.LayoutParams)h.itemView.getLayoutParams()).setFullSpan(rows.get(p) instanceof String);}
    }
}
