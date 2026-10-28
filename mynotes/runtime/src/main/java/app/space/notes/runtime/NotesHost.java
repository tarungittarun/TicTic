package app.space.notes.runtime;
/** OS capabilities supplied by either standalone or future suite host. */
public interface NotesHost {
    void showMessage(String message);
    void shareText(String title,String text);
    void chooseLocalFolder();
    void openExistingFolder();
    void exportBackup();
    void importBackup();
    default void chooseNoteImage(String noteId){showMessage("Image import is unavailable in this host.");}
    default void exportPdf(){showMessage("PDF export is not available in this host.");}
    default void chooseOcrImage(String language){showMessage("Image OCR is unavailable in this host.");}
    default void requestVoiceCapture(){showMessage("Voice capture is unavailable in this host.");}
    default void exportVoice(String id){showMessage("Voice export is unavailable in this host.");}
    default void requestReminderPermission(){showMessage("Enable notifications in Android settings, then choose Reminder again.");}
    default void requestHomeWidget(){showMessage("Long-press your home screen, choose Widgets, then MyNotes library. Remove and re-add it any time; notes stay in the app.");}
    default void exportDiagnostics(){showMessage("Diagnostic export is unavailable in this host.");}
    /** Embedded hosts own their native theme and policy; standalone explicitly opts out. */
    default boolean managesAppearance(){return true;}
    default NotesPalette palette(){return null;}
    default String appearanceMode(){return "system";}
    default void appearanceChanged(){showMessage("Appearance is managed by the host.");}
    String versionLabel();
}
