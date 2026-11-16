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
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import app.space.notes.domain.SafeLinks;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/** Adds transient, safe web-link affordances to ordinary checklist text. */
final class ChecklistLinks {
    private ChecklistLinks(){}

    static void install(EditText field,Consumer<String> open){
        InputFilter[] current=field.getFilters(),filters=Arrays.copyOf(current,current.length+1);
        filters[current.length]=(source,start,end,dest,dstart,dend)->plainInsertion(source,start,end);
        field.setFilters(filters);
        field.setMovementMethod(LinkMovementMethod.getInstance());
        refresh(field,open);
        field.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){}
            public void afterTextChanged(Editable text){refresh(field,open);}
        });
    }

    private static CharSequence plainInsertion(CharSequence source,int start,int end){
        if(!(source instanceof Spanned))return null;
        SpannableStringBuilder plain=new SpannableStringBuilder(source,start,end);boolean changed=false;
        for(Object span:plain.getSpans(0,plain.length(),Object.class)){
            int flags=plain.getSpanFlags(span);
            if(span instanceof NoCopySpan || span instanceof SuggestionSpan || (flags&Spanned.SPAN_COMPOSING)!=0)continue;
            if(span instanceof CharacterStyle || span instanceof ParagraphStyle){plain.removeSpan(span);changed=true;}
        }
        return changed?plain:null;
    }

    private static void refresh(EditText field,Consumer<String> open){
        Editable text=field.getText();
        for(Object span:text.getSpans(0,text.length(),Object.class)){
            int flags=text.getSpanFlags(span);
            if(span instanceof NoCopySpan || span instanceof SuggestionSpan || (flags&Spanned.SPAN_COMPOSING)!=0)continue;
            if(span instanceof CharacterStyle || span instanceof ParagraphStyle)text.removeSpan(span);
        }
        String value=text.toString();
        Matcher matcher=Patterns.WEB_URL.matcher(value);
        while(matcher.find()){
            int start=matcher.start(),end=trimPunctuation(value,start,matcher.end());
            if(end<=start || embeddedInWord(value,start) || embeddedNonWebScheme(value,start))continue;
            String raw=value.substring(start,end),address=raw;
            String lower=raw.toLowerCase(Locale.ROOT);
            if(!lower.startsWith("http://") && !lower.startsWith("https://")){
                if(raw.contains("://"))continue;
                address="https://"+raw;
            }
            try{
                SafeLinks.requireWeb(address);
                text.setSpan(new ChecklistUrlSpan(address,open),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }catch(IllegalArgumentException ignored){/* Non-web or malformed text stays ordinary text. */}
        }
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
        int depth=0;
        for(int i=start;i<end;i++){
            char value=text.charAt(i);
            if(value==open)depth++;
            else if(value==close)depth--;
        }
        return depth<0;
    }

    private static boolean embeddedInWord(String text,int start){
        if(start<=0)return false;
        char previous=text.charAt(start-1);
        return Character.isLetterOrDigit(previous) || previous=='_' || previous=='.' || previous=='@';
    }

    private static boolean embeddedNonWebScheme(String text,int start){
        int colon=-1;
        if(start>0 && text.charAt(start-1)==':')colon=start-1;
        else if(start>=3 && text.regionMatches(start-3,"://",0,3))colon=start-3;
        if(colon<0)return false;
        int begin=colon;
        while(begin>0){char value=text.charAt(begin-1);if(Character.isLetterOrDigit(value) || value=='+' || value=='.' || value=='-')begin--;else break;}
        if(begin>=colon || !Character.isLetter(text.charAt(begin)))return false;
        String scheme=text.substring(begin,colon).toLowerCase(Locale.ROOT);
        return !scheme.equals("http") && !scheme.equals("https");
    }

    private static final class ChecklistUrlSpan extends ClickableSpan {
        private final String address;
        private final Consumer<String> open;
        ChecklistUrlSpan(String address,Consumer<String> open){this.address=address;this.open=open;}
        @Override public void onClick(View widget){if(widget.isEnabled())open.accept(address);}
    }
}
