package app.space.notes.runtime;
/** Minimal capability boundary; no Activity/application ID in domain or repository. */
public interface NotesHost {
    void showMessage(String message);
}
