package app.space.notes.domain;

import java.util.ArrayList;
import java.util.List;

/** Bounded native-text pages; lossless offsets never split a Unicode surrogate pair. */
public final class TextPages {
    public static final int PAGE_CHARS=24000;
    private final String text;
    private final List<Integer> offsets=new ArrayList<>();
    public TextPages(String text){
        if(text==null || text.length()>4*1024*1024)throw new IllegalArgumentException("Text preview supports up to 4 million characters; original unchanged");
        RichText.requireText(text);this.text=text;offsets.add(0);
        int start=0;
        while(start<text.length()){
            int end=Math.min(text.length(),start+PAGE_CHARS);
            if(end<text.length()){
                int newline=text.lastIndexOf('\n',end-1);
                if(newline>start+PAGE_CHARS/2)end=newline+1;
                if(Character.isHighSurrogate(text.charAt(end-1)))end--;
            }
            offsets.add(end);start=end;
        }
        if(text.isEmpty())offsets.add(0);
    }
    public int count(){return offsets.size()-1;}
    public String page(int index){if(index<0 || index>=count())throw new IndexOutOfBoundsException();return text.substring(offsets.get(index),offsets.get(index+1));}
}
