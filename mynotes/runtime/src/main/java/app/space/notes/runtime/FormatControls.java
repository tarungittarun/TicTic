package app.space.notes.runtime;
import android.content.Context;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;
import java.util.*;
import app.space.notes.domain.TypingStyle;
/** Shared non-modal formatting controls; Notes and Doc retain independent data models. */
final class FormatControls {
 interface Host { TypingStyle style(); void apply(String type,String value); void perform(Runnable action); void font(); void size(); void number(); }
 final NotesDesign design; final Host host; final LinearLayout panel;
 final Map<String,Button> toggles=new LinkedHashMap<>(); Button fontChoice,sizeChoice;
 FormatControls(Context context,NotesDesign design,Host host){
  this.design=design;this.host=host; TypingStyle style=host.style();
  boolean largeText=context.getResources().getConfiguration().fontScale>=1.5f || context.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(12),dp(8),dp(12),dp(12));
        LinearLayout inline=row(panel);String[] types={"bold","italic","underline","strike","clear"},names={"Bold","Italic","Underline","Strike","Clear selection formatting"};
        for(int i=0;i<types.length;i++){if(largeText && i==3)inline=row(panel);final String type=types[i];Button control=type.equals("clear")?design.icon(names[i],"erase",()->host.perform(()->host.apply(type,""))):formatChoice(names[i],style.values.containsKey(type),()->host.apply(type,""));if(!type.equals("clear"))control.setText(new String[]{"B","I","U","S",""}[i]);control.setContentDescription(names[i]);control.setTooltipText(names[i]);if(!type.equals("clear"))toggles.put(type,control);control.setPadding(dp(8),dp(10),dp(8),dp(10));if(type.equals("bold"))control.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);if(type.equals("italic"))control.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.ITALIC);if(type.equals("underline"))control.setPaintFlags(control.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);if(type.equals("strike"))control.setPaintFlags(control.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);inline.addView(control,new LayoutParams(0,-2,1));}
        panel.addView(design.divider());panel.addView(design.label("Paragraph",14,design.muted));LinearLayout paragraphs=row(panel);
        String[] pt={"text","h1","h2"},pn={"Normal paragraph","Heading 1","Heading 2"};
        for(int i=0;i<pt.length;i++){if(largeText && i==1)paragraphs=row(panel);final String type=pt[i];boolean on=type.equals("text")?!style.values.containsKey("h1") && !style.values.containsKey("h2"):style.values.containsKey(type);Button choice=formatChoice(new String[]{"Normal","Heading","Subhead"}[i],on,()->host.apply(type,""));choice.setContentDescription(pn[i]);choice.setTooltipText(pn[i]);toggles.put(type,choice);paragraphs.addView(choice,new LayoutParams(0,-2,1));}
        panel.addView(design.divider());panel.addView(design.label("Font",13,design.muted));fontChoice=dropdown("Font",fontLabel(style.values.getOrDefault("font","sans-serif")),host::font);panel.addView(fontChoice,new LayoutParams(-1,-2));
        panel.addView(design.label("Size",13,design.muted));LinearLayout sizes=row(panel);
        sizeChoice=dropdown("Size (8–72 sp)",style.values.getOrDefault("size","18")+" sp",host::size);sizes.addView(sizeChoice,new LayoutParams(0,-2,1));
        Button smaller=design.icon("Decrease text size","minus",()->host.perform(()->host.apply("size",Integer.toString(Math.max(8,Integer.parseInt(host.style().values.getOrDefault("size","18"))-1)))));
        Button bigger=design.icon("Increase text size","add",()->host.perform(()->host.apply("size",Integer.toString(Math.min(72,Integer.parseInt(host.style().values.getOrDefault("size","18"))+1)))));
        sizes.addView(smaller,new LayoutParams(dp(48),dp(48)));sizes.addView(bigger,new LayoutParams(dp(48),dp(48)));
        panel.addView(design.divider());row(panel).addView(button("Number paragraphs",host::number),new LayoutParams(-1,-2));
 }
 void refresh(){TypingStyle style=host.style();for(Map.Entry<String,Button> entry:toggles.entrySet()){String type=entry.getKey();boolean on=type.equals("text")?!style.values.containsKey("h1") && !style.values.containsKey("h2"):style.values.containsKey(type);Button control=entry.getValue();control.setSelected(on);control.setBackground(design.choice(on));androidx.core.view.ViewCompat.setStateDescription(control,on?"On for this selection or typing":"Off or mixed");}fontChoice.setText(fontLabel(style.values.getOrDefault("font","sans-serif")));sizeChoice.setText(style.values.getOrDefault("size","18")+" sp");}
 private int dp(int value){return Math.round(value*panel.getResources().getDisplayMetrics().density);}
 private LinearLayout row(LinearLayout parent){LinearLayout row=new LinearLayout(panel.getContext());row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setBaselineAligned(false);parent.addView(row,new LayoutParams(-1,-2));return row;}
 private Button button(String name,Runnable action){return design.textAction(name,()->host.perform(action));}
    private Button dropdown(String name,String value,Runnable action){Button control=button(name,action);control.setText(value);control.setContentDescription(name);android.graphics.drawable.GradientDrawable border=design.shape(design.background,10);border.setStroke(dp(1),design.outline);control.setBackground(border);NotesIcon arrow=new NotesIcon("chevron-down",design.muted);arrow.setBounds(0,0,dp(20),dp(20));control.setCompoundDrawablesRelative(null,null,arrow,null);return control;}
    private String fontLabel(String value){switch(value){case "serif":return "Serif";case "monospace":return "Monospace";case "cursive":return "Cursive";default:return "Sans serif";}}
    private Button formatChoice(String name,boolean on,Runnable action){Button control=button(name,action);control.setSelected(on);control.setBackground(design.choice(on));androidx.core.view.ViewCompat.setStateDescription(control,on?"On for this selection or typing":"Off or mixed");return control;}

 }
