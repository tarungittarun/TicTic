package app.space.notes.runtime;
/** Optional host-owned design tokens. Pass a complete readable light or dark palette, not app globals. */
public final class NotesPalette {
 public final int background,surface,ink,muted,outline,accent,accentInk;
 public NotesPalette(int background,int surface,int ink,int muted,int outline,int accent,int accentInk){this.background=background;this.surface=surface;this.ink=ink;this.muted=muted;this.outline=outline;this.accent=accent;this.accentInk=accentInk;}
}
