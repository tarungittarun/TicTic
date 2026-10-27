package app.space.notes.runtime;
/** OS capabilities supplied by either standalone or future suite host. */
public interface NotesHost {
    void showMessage(String message);
    void shareText(String title,String text);
    void chooseLocalFolder();
    void openExistingFolder();
    void exportBackup();
    void importBackup();
    default void exportPdf(){showMessage("PDF export is not available in this host.");}
    String versionLabel();
}
