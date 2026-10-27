package app.space.notes.runtime;
/** OS capabilities supplied by either standalone or future suite host. */
public interface NotesHost {
    void showMessage(String message);
    void chooseLocalFolder();
    String versionLabel();
}
