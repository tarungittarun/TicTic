package app.space.notes.runtime;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.space.notes.domain.ChecklistAppearance;
import java.util.Arrays;
import java.util.function.Consumer;

/** App-owned, cancel-safe dark-pill picker for one checklist's visual appearance. */
final class ChecklistAppearancePicker {
    private ChecklistAppearancePicker(){}

    static void show(NotesDesign design,NotesSheets sheets,ChecklistAppearance original,Consumer<ChecklistAppearance> apply){
        if(original==null || apply==null)throw new IllegalArgumentException("Missing checklist appearance");
        final ChecklistAppearance.Glyph[] glyph={original.glyph};final int[] theme={original.themeColor};
        final Button[][] colorButtonRef={null};
        LinearLayout panel=sheets.panel("Checklist appearance",null);
        TextView explanation=design.label("Visual only · plain text and checklist status stay unchanged.",13,design.muted);panel.addView(explanation);
        TextView markerHeading=design.label("Marker",14,design.muted);markerHeading.setContentDescription("Checklist marker choices");panel.addView(markerHeading);
        ChecklistAppearance.Glyph[] glyphs=ChecklistAppearance.Glyph.values();Button[] glyphButtons=new Button[glyphs.length];
        int glyphColumns=columns(design,3);
        for(int start=0;start<glyphs.length;start+=glyphColumns){
            LinearLayout line=new LinearLayout(design.context);line.setBaselineAligned(false);panel.addView(line,new LinearLayout.LayoutParams(-1,-2));
            for(int column=0;column<glyphColumns;column++){
                int index=start+column;LinearLayout.LayoutParams cell=cell(design);
                if(index>=glyphs.length){line.addView(new View(design.context),cell);continue;}
                ChecklistAppearance.Glyph value=glyphs[index];Button option=choiceButton(design,value.label+" checklist marker",value.label);
                option.setTag("checklist-glyph:"+value.id);
                ChecklistMarkerDrawable sample=new ChecklistMarkerDrawable(value,ChecklistMarkerRenderer.color(new ChecklistAppearance(value,theme[0]),design),design.ink,design.ink,design.context.getResources().getDisplayMetrics().density);
                sample.setBounds(0,0,design.dp(24),design.dp(24));option.setCompoundDrawablesRelative(null,sample,null,null);option.setCompoundDrawablePadding(design.dp(2));
                option.setOnClickListener(view->{glyph[0]=value;refresh(glyphButtons,glyph[0],colorButtonRef[0],theme[0],design);});glyphButtons[index]=option;line.addView(option,cell);
            }
        }
        TextView colorHeading=design.label("Theme colour",14,design.muted);colorHeading.setContentDescription("Checklist theme colour choices");panel.addView(colorHeading);
        String[] palette=Arrays.copyOfRange(NotesBoard.NAMES,1,NotesBoard.NAMES.length);String[] colors=new String[palette.length+1];colors[0]="Default";System.arraycopy(palette,0,colors,1,palette.length);Button[] colorButtons=new Button[colors.length];colorButtonRef[0]=colorButtons;int colorColumns=columns(design,3);
        for(int start=0;start<colors.length;start+=colorColumns){
            LinearLayout line=new LinearLayout(design.context);line.setBaselineAligned(false);panel.addView(line,new LinearLayout.LayoutParams(-1,-2));
            for(int column=0;column<colorColumns;column++){
                int index=start+column;LinearLayout.LayoutParams cell=cell(design);
                if(index>=colors.length){line.addView(new View(design.context),cell);continue;}
                final int value=index==0?ChecklistAppearance.DEFAULT_THEME_COLOR:index;Button option=choiceButton(design,"Checklist theme colour "+colors[index],colors[index]);option.setTag("checklist-theme:"+value);
                GradientDrawable swatch=new GradientDrawable();swatch.setShape(GradientDrawable.OVAL);swatch.setColor(value==ChecklistAppearance.DEFAULT_THEME_COLOR?design.accent:NotesBoard.colour(value,false));swatch.setStroke(design.dp(1),design.outline);swatch.setBounds(0,0,design.dp(24),design.dp(24));option.setCompoundDrawablesRelative(null,swatch,null,null);option.setCompoundDrawablePadding(design.dp(2));
                option.setOnClickListener(view->{theme[0]=value;refresh(glyphButtons,glyph[0],colorButtons,theme[0],design);});colorButtons[index]=option;line.addView(option,cell);
            }
        }
        refresh(glyphButtons,glyph[0],colorButtons,theme[0],design);
        LinearLayout footer=new LinearLayout(design.context);Button save=design.textAction("Apply",()->{ChecklistAppearance result=new ChecklistAppearance(glyph[0],theme[0]);sheets.close();apply.accept(result);});design.primary(save);save.setContentDescription("Apply checklist appearance");
        Button cancel=design.textAction("Cancel",sheets::close);cancel.setContentDescription("Cancel checklist appearance");footer.addView(save,new LinearLayout.LayoutParams(0,-2,1));footer.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));sheets.footer(panel,footer);sheets.show(panel,"Checklist appearance");
    }

    private static LinearLayout.LayoutParams cell(NotesDesign design){LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1);cell.setMargins(design.dp(3),design.dp(2),design.dp(3),design.dp(2));return cell;}
    private static Button choiceButton(NotesDesign design,String description,String label){
        Button button=design.textAction(label,()->{});button.setTextSize(13);button.setGravity(Gravity.CENTER);button.setMinHeight(design.dp(56));button.setPadding(design.dp(3),design.dp(4),design.dp(3),design.dp(4));button.setContentDescription(description);button.setTooltipText(description);return button;
    }
    private static int columns(NotesDesign design,int normal){
        return design.context.getResources().getConfiguration().fontScale>=1.35f || design.context.getResources().getConfiguration().screenWidthDp<340?2:normal;
    }
    private static void refresh(Button[] glyphButtons,ChecklistAppearance.Glyph glyph,Button[] colorButtons,int theme,NotesDesign design){
        if(glyphButtons!=null)for(int i=0;i<glyphButtons.length;i++)if(glyphButtons[i]!=null){boolean selected=ChecklistAppearance.Glyph.values()[i]==glyph;glyphButtons[i].setSelected(selected);glyphButtons[i].setBackground(design.choice(selected));}
        if(colorButtons!=null)for(int i=0;i<colorButtons.length;i++)if(colorButtons[i]!=null){int value=i==0?ChecklistAppearance.DEFAULT_THEME_COLOR:i;boolean selected=value==theme;colorButtons[i].setSelected(selected);colorButtons[i].setBackground(design.choice(selected));}
    }
}
