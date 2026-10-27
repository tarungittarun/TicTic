package app.space.notes.runtime;
/** OS capabilities supplied by either standalone or future suite host. */
public interface NotesHost {
    void showMessage(String message);
    void shareText(String title,String text);
    void chooseLocalFolder();
    void openExistingFolder();
    void exportBackup();
    void importBackup();
    String versionLabel();
}
