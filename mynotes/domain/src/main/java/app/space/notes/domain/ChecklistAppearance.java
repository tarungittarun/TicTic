package app.space.notes.domain;

/** Per-checklist visual choices. They never participate in a checklist's plain-text projection. */
public final class ChecklistAppearance {
    /** Theme IDs 1–11 are named swatches; Default follows them and uses the editor accent. */
    public static final int NAMED_THEME_COLOR_COUNT=11;
    public static final int THEME_COLOR_COUNT=NAMED_THEME_COLOR_COUNT+1;
    public static final int DEFAULT_THEME_COLOR=THEME_COLOR_COUNT;
    public static final ChecklistAppearance DEFAULT=new ChecklistAppearance(Glyph.CHECK,DEFAULT_THEME_COLOR);

    public enum Glyph {
        CHECK("check","Check"),
        DOT("dot","Dot"),
        SQUARE("square","Square"),
        DIAMOND("diamond","Diamond"),
        STAR("star","Star");

        public final String id,label;
        Glyph(String id,String label){this.id=id;this.label=label;}

        public static Glyph fromId(String id){
            if(id==null)throw new IllegalArgumentException("Missing checklist marker");
            for(Glyph glyph:values())if(glyph.id.equals(id))return glyph;
            throw new IllegalArgumentException("Unknown checklist marker");
        }
    }

    /** IDs 1–11 map to the named app swatches; 12 means use the current default accent. */
    public final Glyph glyph;
    public final int themeColor;

    public ChecklistAppearance(Glyph glyph,int themeColor){
        if(glyph==null)throw new IllegalArgumentException("Missing checklist marker");
        if(themeColor<1 || themeColor>THEME_COLOR_COUNT)throw new IllegalArgumentException("Unknown checklist theme colour");
        this.glyph=glyph;this.themeColor=themeColor;
    }

    public static ChecklistAppearance of(String glyph,int themeColor){return new ChecklistAppearance(Glyph.fromId(glyph),themeColor);}
    public boolean isDefault(){return glyph==Glyph.CHECK && themeColor==DEFAULT_THEME_COLOR;}
    public ChecklistAppearance withGlyph(Glyph value){return value==glyph?this:new ChecklistAppearance(value,themeColor);}
    public ChecklistAppearance withThemeColor(int value){return value==themeColor?this:new ChecklistAppearance(glyph,value);}

    @Override public boolean equals(Object other){
        if(this==other)return true;
        if(!(other instanceof ChecklistAppearance))return false;
        ChecklistAppearance value=(ChecklistAppearance)other;
        return glyph==value.glyph && themeColor==value.themeColor;
    }
    @Override public int hashCode(){return 31*glyph.hashCode()+themeColor;}
    @Override public String toString(){return glyph.id+":"+themeColor;}
}
