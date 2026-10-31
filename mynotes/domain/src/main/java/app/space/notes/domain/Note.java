package app.space.notes.domain;

import java.util.*;

/** Schema-v6 immutable document. v1 constructor remains supported for migration/tests. */
public final class Note {
    public final Map<String,String> contentNames;
    public static final long TRASH_RETENTION_MS=7L*24*60*60*1000;
    public final String id,title,body,folder;
    public final int background;
    public final long createdAt,updatedAt,deletedAt,reminderAt;
    public final boolean pinned,archived,checklist,hideChecked;
    public final int color;
    public final List<Item> items;
    public final List<Mark> marks;
    public final List<String> labels;
    public final Diagram diagram;
    public final List<String> voiceSources;
    public final List<NoteImage> images;
    /** Non-null only for canonical schema7 content; legacy fields then are read-only projections. */
    public final DocumentContent content;
    public static final class Item {
        public final String id,text;public final boolean checked;public final int depth;public final List<Mark> marks;
        public Item(String id,String text,boolean checked,int depth) {this(id,text,checked,depth,Collections.emptyList());}
        public Item(String id,String text,boolean checked,int depth,List<Mark> marks) {
            if(id==null || id.isEmpty() || text==null || depth<0 || depth>3)throw new IllegalArgumentException("Invalid list item");
            new RichText(text,marks);this.marks=Collections.unmodifiableList(new ArrayList<>(marks));
            this.id=id;this.text=text;this.checked=checked;this.depth=depth;
        }
        public Item change(String text,boolean checked,int depth){if(!this.text.equals(text) && !marks.isEmpty())throw new IllegalStateException("Supply formatted row text explicitly");return new Item(id,text,checked,depth,marks);}
    }
    public static final class Mark {
        public final String type,value;public final int start,end;
        public Mark(String type,int start,int end) {this(type,start,end,"");}
        public Mark(String type,int start,int end,String value) {
            if(value==null || start<0 || end<=start)
                throw new IllegalArgumentException("Invalid text mark");
            if(Arrays.asList("bold","italic","underline","strike","h1","h2").contains(type)){if(!value.isEmpty())throw new IllegalArgumentException("Unexpected text style value");}
            else if("font".equals(type)){if(!Arrays.asList("sans-serif","serif","monospace","cursive").contains(value))throw new IllegalArgumentException("Unsupported font");}
            else if("size".equals(type) || "number".equals(type)){int size;try{size=Integer.parseInt(value);}catch(NumberFormatException bad){throw new IllegalArgumentException("Invalid style number");}if(!Integer.toString(size).equals(value) || size<(type.equals("size")?8:1) || size>(type.equals("size")?72:9999))throw new IllegalArgumentException("Style number out of range");}
            else if("link".equals(type)){SafeLinks.requireWeb(value);}
            else throw new IllegalArgumentException("Unknown text style");
            this.type=type;this.start=start;this.end=end;this.value=value;
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
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,"",0);
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram,List<String> voiceSources,long reminderAt,String folder,int background) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,background,Collections.emptyList());
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram,List<String> voiceSources,long reminderAt,String folder,int background,List<NoteImage> images) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,background,images,null);
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram,List<String> voiceSources,long reminderAt,String folder,int background,List<NoteImage> images,DocumentContent content) {
        this(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,background,images,content,Collections.emptyMap());
    }
    public Note(String id,String title,String body,long createdAt,long updatedAt,long deletedAt,
                boolean pinned,boolean archived,int color,boolean checklist,boolean hideChecked,
                List<Item> items,List<Mark> marks,List<String> labels,Diagram diagram,List<String> voiceSources,long reminderAt,String folder,int background,List<NoteImage> images,DocumentContent content,Map<String,String> contentNames) {
        if(contentNames==null || contentNames.size()>10)throw new IllegalArgumentException("Too many content names");
        TreeMap<String,String> names=new TreeMap<>();for(Map.Entry<String,String> entry:contentNames.entrySet()){
            String key=entry.getKey(),name=entry.getValue();if(key==null || name==null || name.trim().isEmpty() || name.length()>200 || name.contains("\n") || name.contains("\r"))throw new IllegalArgumentException("Invalid content name");
            if(!key.equals("diagram") && !key.equals("reminder")){if(!key.startsWith("image:"))throw new IllegalArgumentException("Unknown named content");UUID.fromString(key.substring(6));}
            RichText.requireText(name);names.put(key,name);
        }this.contentNames=Collections.unmodifiableMap(names);
        this.content=content;
        if(content!=null){body=content.plainText();checklist=false;hideChecked=false;items=Collections.emptyList();marks=Collections.emptyList();images=content.images();diagram=content.diagramProjection();}
        if(images.size()>8)throw new IllegalArgumentException("Maximum eight images per note");Set<String> imageIds=new HashSet<>();for(NoteImage image:images)if(!imageIds.add(image.id))throw new IllegalArgumentException("Duplicate image ID");this.images=Collections.unmodifiableList(new ArrayList<>(images));for(String key:this.contentNames.keySet())if(key.startsWith("image:") && !imageIds.contains(key.substring(6)))throw new IllegalArgumentException("Name references a missing image");
        if(folder==null || (!folder.isEmpty() && !FolderNames.require(folder).equals(folder)))throw new IllegalArgumentException("Invalid folder name");
        if(background<0 || background>15)throw new IllegalArgumentException("Unknown note background");this.folder=folder;this.background=background;
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
        if(content==null)for(Item item:items)if(!item.marks.isEmpty())throw new IllegalArgumentException("Formatted checklist rows require canonical content");
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
        return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deleted,pin,archive,colour,list,hide,rows,spans,tags,diagram,voiceSources,reminderAt,folder,background,images,content,contentNames);
    }
    public static Note create(long now){return new Note(UUID.randomUUID().toString(),"","",now,now,0,false,false,0);}
    public Note edit(String title,String body,long now){if(content!=null && !body.equals(this.body))throw new IllegalStateException("Edit canonical content, not its text projection");return copy(title,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,
        body.equals(this.body)?marks:Collections.emptyList(),labels);}
    public Note rich(String title,String body,List<Mark> marks,long now){if(content!=null)throw new IllegalStateException("Edit canonical content, not its text projection");return copy(title,body,now,deletedAt,pinned,archived,color,false,hideChecked,Collections.emptyList(),marks,labels);}
    public Note list(String title,List<Item> rows,boolean hide,long now){if(content!=null)throw new IllegalStateException("Edit a checklist section, not the whole document");return copy(title,"",now,deletedAt,pinned,archived,color,true,hide,rows,Collections.emptyList(),labels);}
    public Note properties(int color,List<String> labels,long now){return copy(title,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels);}
    public Note state(boolean pinned,boolean archived,long deletedAt,long now){return copy(title,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels);}
    public Note withId(String id){return new Note(id,title,body,createdAt,updatedAt,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,background,images,content,contentNames);}
    public Note diagram(Diagram value,long now){if(content!=null)return withContent(content.withDiagram(value),now);return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,value,voiceSources,reminderAt,folder,background,images,content,contentNames);}
    public Note voiceSource(String source,long now){List<String> ids=new ArrayList<>(voiceSources);if(!ids.contains(source))ids.add(source);return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,ids,reminderAt,folder,background,images,content,contentNames);}
    public Note reminder(long time,long now){return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,time,folder,background,images,content,contentNames);}
    public Note folder(String value,long now){String normalized=value.isEmpty()?"":FolderNames.require(value);return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,normalized,background,images,content,contentNames);}
    public Note background(int value,long now){return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,value,images,content!=null && value==3?content.withWritingLines(true):content,contentNames);}
    public Note images(List<NoteImage> value,long now){if(content!=null)return withContent(content.withImages(value),now);return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,background,value,null,namesForImages(value));}
    public Note withContent(DocumentContent value,long now){if(value==null)throw new IllegalArgumentException("Cannot discard canonical document content");return new Note(id,title,"",createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color,false,false,Collections.emptyList(),Collections.emptyList(),labels,Diagram.empty(),voiceSources,reminderAt,folder,background,Collections.emptyList(),value,namesForImages(value.images()));}
    private Map<String,String> namesForImages(List<NoteImage> images){Map<String,String> names=new TreeMap<>(contentNames);Set<String> ids=new HashSet<>();for(NoteImage image:images)ids.add("image:"+image.id);names.keySet().removeIf(key->key.startsWith("image:") && !ids.contains(key));return names;}
    public Note namedContent(String key,String name,long now){Map<String,String> names=new TreeMap<>(contentNames);if(name.trim().isEmpty())names.remove(key);else names.put(key,name.trim());return withContentNames(names,now);}
    public Note withContentNames(Map<String,String> names,long now){return new Note(id,title,body,createdAt,Math.max(now,updatedAt),deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels,diagram,voiceSources,reminderAt,folder,background,images,content,names);}
    public String contentName(String key,String fallback){return contentNames.getOrDefault(key,fallback);}
    public Note title(String value,long now){return copy(value,body,now,deletedAt,pinned,archived,color,checklist,hideChecked,items,marks,labels);}
    public String plainText() {
        if(content!=null)return content.plainText();
        if(!checklist)return body;
        StringBuilder text=new StringBuilder();
        for(Item item:items){for(int i=0;i<item.depth;i++)text.append("  ");text.append(item.checked?"[x] ":"[ ] ").append(item.text).append('\n');}
        return text.toString();
    }
    public Note convert(long now) {
        if(content!=null)throw new IllegalStateException("Convert selected content, not the whole document");
        if(checklist){
            StringBuilder text=new StringBuilder();
            for(int row=0;row<items.size();row++){if(row>0)text.append('\n');Item item=items.get(row);for(int i=0;i<item.depth;i++)text.append("  ");text.append(item.text);}
            return copy(title,text.toString(),now,deletedAt,pinned,archived,color,false,false,Collections.emptyList(),Collections.emptyList(),labels);
        }
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
    public boolean matches(String query){String q=query.trim().toLowerCase(Locale.ROOT);return String.join(" ",contentNames.values()).toLowerCase(Locale.ROOT).contains(q) || diagram.plainText().toLowerCase(Locale.ROOT).contains(q) || title.toLowerCase(Locale.ROOT).contains(q)
        || plainText().toLowerCase(Locale.ROOT).contains(q) || String.join(" ",labels).toLowerCase(Locale.ROOT).contains(q);}
}
