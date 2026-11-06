package app.space.notes.domain;

import java.util.Locale;

/** Display-only provenance for an already-matched note. Never changes search inclusion. */
public final class SearchPreview {
    public final String source,text;
    /** UTF-16 offsets in the displayed text; -1 means no displayed match. */
    public final int start,end;
    private SearchPreview(String source,String text,int start,int end){
        this.source=source;this.text=text;this.start=start;this.end=end;
    }
    public static SearchPreview forNote(Note note,String query){
        String q=query.trim().toLowerCase(Locale.ROOT);
        if(q.isEmpty())return null;
        SearchPreview p=within("Body",note.plainText(),q);
        if(p==null)p=within("Title",note.title,q);
        if(p==null)p=within("Labels",String.join(" ",note.labels),q);
        if(p==null)p=within("Content names",String.join(" ",note.contentNames.values()),q);
        if(p==null)p=within("Diagram",note.diagram.plainText(),q);
        return p;
    }
    private static SearchPreview within(String source,String value,String q){
        String folded=value.toLowerCase(Locale.ROOT);int at=folded.indexOf(q);
        if(at<0)return null;
        int from=originalOffset(value,folded,at,false),to=originalOffset(value,folded,at+q.length(),true);
        // A query can itself exceed a bounded display budget. Do not imply that a
        // clipped fragment is the entire match or highlight unrelated characters.
        if(to-from>160)return new SearchPreview(source,"Long matching passage — open note to inspect",-1,-1);
        int left=Math.max(0,from-40),right=Math.min(value.length(),Math.max(to,left+160));
        if(left>0 && Character.isLowSurrogate(value.charAt(left)))left--;
        if(right<value.length() && Character.isLowSurrogate(value.charAt(right)))right++;
        String prefix=left>0?"…":"",suffix=right<value.length()?"…":"";
        return new SearchPreview(source,prefix+value.substring(left,right)+suffix,prefix.length()+from-left,prefix.length()+to-left);
    }
    private static int originalOffset(String value,String folded,int offset,boolean end){
        if(value.length()==folded.length())return offset;
        // ROOT case conversion can expand a code point (e.g. dotted capital I).
        // Map back to the whole original code point rather than shifting a highlight.
        int normalized=0;
        for(int i=0;i<value.length();){
            int cp=value.codePointAt(i),next=i+Character.charCount(cp);
            int size=cp<128?1:new String(Character.toChars(cp)).toLowerCase(Locale.ROOT).length();
            if(offset==normalized)return i;
            if(offset<normalized+size)return end?next:i;
            normalized+=size;i=next;
        }
        return value.length();
    }
}
