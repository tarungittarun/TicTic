package app.space.notes.runtime;

import android.text.Editable;
import android.text.InputFilter;
import android.text.NoCopySpan;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ParagraphStyle;
import android.text.style.SuggestionSpan;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import app.space.notes.domain.Note;
import app.space.notes.domain.SafeLinks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/** Safe web links coexist with, and never erase, persisted checklist text formatting. */
final class ChecklistLinks {
    private ChecklistLinks(){}

    static void install(EditText field,Consumer<String> open){
        InputFilter[] current=field.getFilters(),filters=Arrays.copyOf(current,current.length+1);
        filters[current.length]=(source,start,end,dest,dstart,dend)->plainInsertion(source,start,end,field.getResources().getDisplayMetrics());
        field.setFilters(filters);
        field.setMovementMethod(LinkMovementMethod.getInstance());
        refresh(field,open);
        field.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){}
            public void afterTextChanged(Editable text){refresh(field,open);}
        });
    }

    /** Keep app-owned visual styles on pasted text, while removing foreign click handlers. */
    private static CharSequence plainInsertion(CharSequence source,int start,int end,android.util.DisplayMetrics metrics){
        if(!(source instanceof Spanned))return null;
        SpannableStringBuilder safe=new SpannableStringBuilder(source,start,end);
        boolean changed=false;
        for(Object span:safe.getSpans(0,safe.length(),Object.class)){
            int flags=safe.getSpanFlags(span);
            if(span instanceof NoCopySpan || span instanceof SuggestionSpan || (flags&Spanned.SPAN_COMPOSING)!=0)continue;
            if(span instanceof URLSpan){try{SafeLinks.requireWeb(((URLSpan)span).getURL());}catch(IllegalArgumentException invalid){safe.removeSpan(span);changed=true;}}
        }
        List<Note.Mark> styles=new ArrayList<>();
        for(Note.Mark mark:TextStyles.read(safe))if(Arrays.asList("bold","italic","underline","strike","font","size","h1","h2","link").contains(mark.type))styles.add(mark);
        for(Object span:safe.getSpans(0,safe.length(),Object.class)){
            int flags=safe.getSpanFlags(span);
            if(span instanceof NoCopySpan || span instanceof SuggestionSpan || (flags&Spanned.SPAN_COMPOSING)!=0)continue;
            if(span instanceof CharacterStyle || span instanceof ParagraphStyle){safe.removeSpan(span);changed=true;}
        }
        if(!changed)return null;
        TextStyles.write(safe,styles,metrics);
        return safe;
    }

    static void refresh(EditText field,Consumer<String> open){
        Editable text=field.getText();
        for(AutoChecklistUrlSpan span:text.getSpans(0,text.length(),AutoChecklistUrlSpan.class))text.removeSpan(span);
        for(URLSpan span:text.getSpans(0,text.length(),URLSpan.class)){
            if(span instanceof ChecklistUrlSpan)continue;
            int start=text.getSpanStart(span),end=text.getSpanEnd(span);String address=span.getURL();text.removeSpan(span);
            if(start<0 || end<=start || end>text.length())continue;
            try{SafeLinks.requireWeb(address);text.setSpan(new ChecklistUrlSpan(address,open),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
            catch(IllegalArgumentException invalid){/* Invalid or non-web marks are never clickable. */}
        }
        String value=text.toString();Matcher matcher=Patterns.WEB_URL.matcher(value);
        while(matcher.find()){
            int start=matcher.start(),end=trimPunctuation(value,start,matcher.end());
            if(end<=start || embeddedInWord(value,start) || embeddedNonWebScheme(value,start) || overlapsLink(text,start,end))continue;
            String raw=value.substring(start,end),address=raw;String lower=raw.toLowerCase(Locale.ROOT);
            if(!lower.startsWith("http://") && !lower.startsWith("https://")){
                if(raw.contains("://"))continue;
                address="https://"+raw;
            }
            try{SafeLinks.requireWeb(address);text.setSpan(new AutoChecklistUrlSpan(address,open),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
            catch(IllegalArgumentException ignored){/* Non-web or malformed text stays ordinary text. */}
        }
    }

    private static boolean overlapsLink(Editable text,int start,int end){
        for(ClickableSpan span:text.getSpans(start,end,ClickableSpan.class))if(text.getSpanStart(span)<end && text.getSpanEnd(span)>start)return true;
        return false;
    }

    private static int trimPunctuation(String text,int start,int end){
        while(end>start){
            char last=text.charAt(end-1);
            if(".,!?;:".indexOf(last)>=0){end--;continue;}
            if(last==')' && unmatchedClose(text,start,end,')','(')){end--;continue;}
            if(last==']' && unmatchedClose(text,start,end,']','[')){end--;continue;}
            if(last=='}' && unmatchedClose(text,start,end,'}','{')){end--;continue;}
            break;
        }
        return end;
    }

    private static boolean unmatchedClose(String text,int start,int end,char close,char open){
        int depth=0;for(int i=start;i<end;i++){char value=text.charAt(i);if(value==open)depth++;else if(value==close)depth--;}
        return depth<0;
    }
    private static boolean embeddedInWord(String text,int start){
        if(start<=0)return false;char previous=text.charAt(start-1);
        return Character.isLetterOrDigit(previous) || previous=='_' || previous=='.' || previous=='@';
    }
    private static boolean embeddedNonWebScheme(String text,int start){
        int colon=-1;if(start>0 && text.charAt(start-1)==':')colon=start-1;else if(start>=3 && text.regionMatches(start-3,"://",0,3))colon=start-3;
        if(colon<0)return false;int begin=colon;
        while(begin>0){char value=text.charAt(begin-1);if(Character.isLetterOrDigit(value) || value=='+' || value=='.' || value=='-')begin--;else break;}
        if(begin>=colon || !Character.isLetter(text.charAt(begin)))return false;
        String scheme=text.substring(begin,colon).toLowerCase(Locale.ROOT);return !scheme.equals("http") && !scheme.equals("https");
    }
    private static final class ChecklistUrlSpan extends URLSpan {
        private final Consumer<String> open;
        ChecklistUrlSpan(String address,Consumer<String> open){super(address);this.open=open;}
        @Override public void onClick(View widget){if(widget.isEnabled())open.accept(getURL());}
    }
    private static final class AutoChecklistUrlSpan extends ClickableSpan {
        private final String address;private final Consumer<String> open;
        AutoChecklistUrlSpan(String address,Consumer<String> open){this.address=address;this.open=open;}
        @Override public void onClick(View widget){if(widget.isEnabled())open.accept(address);}
    }
}
