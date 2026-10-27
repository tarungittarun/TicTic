package app.space.notes.domain;

import java.util.*;

/** Schema-v5 immutable document. v1 constructor remains supported for migration/tests. */
public final class Note {
    public static final long TRASH_RETENTION_MS=7L*24*60*60*1000;
    public final String id,title,body;
    public final long createdAt,updatedAt,deletedAt,reminderAt;
    public final boolean pinned,archived,checklist,hideChecked;
    public final int color;
    public final List<Item> items;
    public final List<Mark> marks;
    public final List<String> labels;
    public final Diagram diagram;
    public final List<String> voiceSources;
    public static final class Item {
        public final String id,text;public final boolean checked;public final int depth;
        public Item(String id,String text,boolean checked,int depth) {
            if(id==null || id.isEmpty() || text==null || depth<0 || depth>3)throw new IllegalArgumentException("Invalid list item");
            this.id=id;this.text=text;this.checked=checked;this.depth=depth;
        }
        public Item change(String text,boolean checked,int depth){return new Item(id,text,checked,depth);}
    }
    public static final class Mark {
        public final String type;public final int start,end;
        public Mark(String type,int start,int end) {
            if(!Arrays.asList("bold","italic","underline","strike","h1","h2").contains(type) || start<0 || end<=start)
                throw new IllegalArgumentException("Invalid text mark");
            this.type=type;this.start=start;this.end=end;
        }
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,boolean pinned,boolean archived,int color) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,false,false,
            Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,Diagram.empty());
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,Collections.emptyList());
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram,List<String> voiceSources) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,0);
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram,List<String> voiceSources,long reminderAt) {
        if(reminderAt<0)throw new IllegalArgumentException("Invalid reminder time");this.reminderAt=reminderAt;
        Set<String> sourceIds=new LinkedHashSet<>();for(String source:voiceSources){UUID.fromString(source);if(!sourceIds.add(source))throw new IllegalArgumentException("Duplicate voice source");}
        this.voiceSources=Collections.unmodifiableList(new ArrayList<>(voiceSources));
        if(diagram==null)throw new IllegalArgumentException("Missing diagram");
        this.diagram=diagram;
        if(id==null || id.isEmpty() || title==null || body==null)throw new IllegalArgumentException("Missing document fields");
        if(createdAt<0 || updatedAt<createdAt || deletedAt<0 || color<0 || color>11)throw new IllegalArgumentException("Invalid document metadata");
        Set<String> ids=new HashSet<>();for(Item item:items)if(!ids.add(item.id))throw new IllegalArgumentException("Duplicate item ID");
        if(!checklist && !items.isEmpty())throw new IllegalArgumentException("Text note cannot contain list rows");
        if(checklist && !body.isEmpty())throw new IllegalArgumentException("Checklist body must be stored in its rows");
        if(checklist && !marks.isEmpty())throw new IllegalArgumentException("Checklist cannot contain rich-text spans");
        for(Mark mark:marks)if(mark.end>body.length())throw new IllegalArgumentException("Text mark outside body");
        this.id=id;this.title=title;this.body=body;this.createdAt=createdAt;this.updatedAt=updatedAt;this.deletedAt=deletedAt;
        this.pinned=pinned;this.archived=archived;this.color=color;this.checklist=checklist;this.hideChecked=hideChecked;
        this.items=Collections.unmodifiableList(new ArrayList<>(items));this.marks=Collections.unmodifiableList(new ArrayList<>(marks));
        this.labels=Collections.unmodifiableList(normalizeLabels(labels));
    }
    public static List<String> normalizeLabels(List<String> labels) {
        TreeSet<String> out=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(String label:labels) {
            String value=label.trim();if(value.isEmpty())continue;
            if(value.length()>64 || value.contains(",") || value.contains("\n"))throw new IllegalArgumentException("Labels: maximum 64 characters, no comma/newline");
            out.add(value);
        }
        if(out.size()>50)throw new IllegalArgumentException("Maximum 50 labels per note");return new ArrayList<>(out);
    }
    private Note copy(String title,String body,long now,long deleted,boolean pin,boolean archive,int colour,
                      boolean list,boolean hide,List<Item> rows,List<Mark> spans,List<String> tags) {
        return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deleted,pin,archive,colour,list,hide,rows,spans,tags,diagram,voiceSources,reminderAt);
    }
    public static Note create(long now){return new Note(UUID.randomUUID().toString(),"","",now,now,0,false,false,0);}
    public Note edit(String title,String body,long now){return copy(title,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,
        body.equals(this.body)?marks:Collections.emptyList(),labels);}
    public Note rich(String title,String body,List<Mark> marks,long now){return copy(title,body,now,deletedAt,pinned,archived,color,false,hideChecked,Collections.emptyList(),marks,labels);}
    public Note list(String title,List<Item> rows,boolean hide,long now){return copy(title,"",now,deletedAt,pinned,archived,color,true,hide,rows,Collections.emptyList(),labels);}
    public Note properties(int color,List<String> labels,long now){return copy(title,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels);}
    public Note state(boolean pinned,boolean archived,long deletedAt,long now){return copy(title,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels);}
    public Note withId(String id){return new Note(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt);}
    public Note diagram(Diagram value,long now){return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,value,voiceSources,reminderAt);}
    public Note voiceSource(String source,long now){List<String> ids=new ArrayList<>(voiceSources);if(!ids.contains(source))ids.add(source);return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,ids,reminderAt);}
    public Note reminder(long time,long now){return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,time);}
    public String plainText() {
        if(!checklist)return body;
        StringBuilder text=new StringBuilder();
        for(Item item:items){for(int i=0;i<item.depth;i++)text.append("  ");text.append(item.checked?"[x] ":"[ ] ").append(item.text).append('\n');}
        return text.toString();
    }
    public Note convert(long now) {
        if(checklist)return copy(title,plainText(),now,deletedAt,pinned,archived,color,false,false,Collections.emptyList(),Collections.emptyList(),labels);
        List<Item> rows=new ArrayList<>();
        for(String line:body.split("\n",-1)){
            int depth=0;while(depth<3 && line.startsWith("  ")){depth++;line=line.substring(2);}
            boolean checked=line.startsWith("[x] ") || line.startsWith("[X] ");
            if(checked || line.startsWith("[ ] "))line=line.substring(4);
            rows.add(new Item(UUID.randomUUID().toString(),line,checked,depth));
        }
        return list(title,rows,false,now);
    }
    public boolean expired(long now){return deletedAt>0 && now>=deletedAt && now-deletedAt>=TRASH_RETENTION_MS;}
    public boolean matches(String query){String q=query.trim().toLowerCase(Locale.ROOT);return diagram.plainText().toLowerCase(Locale.ROOT).contains(q) || title.toLowerCase(Locale.ROOT).contains(q)
        || plainText().toLowerCase(Locale.ROOT).contains(q) || String.join(" ",labels).toLowerCase(Locale.ROOT).contains(q);}
}
