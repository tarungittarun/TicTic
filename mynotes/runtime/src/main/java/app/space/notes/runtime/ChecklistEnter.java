package app.space.notes.runtime;

import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import java.util.Arrays;

/** Converts a checklist-row Enter action into one ordered split, for hardware and IME input. */
final class ChecklistEnter {
    interface Listener { void split(String originalText,int start,int end); }
    private ChecklistEnter() { }

    static void install(EditText input,Listener listener){
        final class Pending {
            boolean scheduled;String text;int start,end;
            void schedule(String original,int from,int to){
                if(scheduled)return;scheduled=true;text=original;start=from;end=to;
                input.post(()->{if(!scheduled)return;scheduled=false;listener.split(text,start,end);});
            }
        }
        Pending pending=new Pending();
        InputFilter[] existing=input.getFilters(),filters=Arrays.copyOf(existing,existing.length+1);
        filters[existing.length]=(source,start,end,dest,dstart,dend)->{
            if(end-start==1 && source.charAt(start)=='\n'){
                pending.schedule(dest.toString(),dstart,dend);
                return dest.subSequence(dstart,dend);
            }
            return null;
        };
        input.setFilters(filters);
        input.setImeOptions(EditorInfo.IME_ACTION_NEXT|EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_FLAG_NO_FULLSCREEN);
        input.setOnKeyListener((view,keyCode,event)->{
            if(keyCode!=KeyEvent.KEYCODE_ENTER)return false;
            if(event.getAction()==KeyEvent.ACTION_DOWN && event.getRepeatCount()==0)pending.schedule(input.getText().toString(),input.getSelectionStart(),input.getSelectionEnd());
            return true;
        });
        input.setOnEditorActionListener((view,action,event)->{
            boolean enter=action==EditorInfo.IME_ACTION_NEXT || action==EditorInfo.IME_ACTION_DONE || event!=null && event.getKeyCode()==KeyEvent.KEYCODE_ENTER;
            if(!enter)return false;
            if(event==null || event.getAction()==KeyEvent.ACTION_DOWN)pending.schedule(input.getText().toString(),input.getSelectionStart(),input.getSelectionEnd());
            return true;
        });
    }
}
