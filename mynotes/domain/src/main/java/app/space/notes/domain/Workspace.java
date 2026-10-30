package app.space.notes.domain;
/** A presentation boundary, not a destructive conversion or a separate copy of the library. */
public final class Workspace {
    private Workspace(){}
    public static boolean isDocument(Note note){return note.content!=null;}
    public static boolean includes(Note note,boolean documents){return isDocument(note)==documents;}
    public static Note create(long now,boolean documents){Note note=Note.create(now);return documents?note.withContent(DocumentContent.fromLegacy(note),now):note;}
    /** Explicit copy: source identity, timestamps, attachments and original storage remain unchanged. */
    public static Note copyToDocument(Note source,long now){Note copy=NoteOperations.duplicate(source,now);return copy.withContent(DocumentContent.fromLegacy(copy),now);}
}
