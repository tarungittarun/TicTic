package app.space.notes.domain;
/** Bounded Android IPC input. This is not a cap on manually authored notes. */
public final class IncomingText {
    public final String title,body;
    public IncomingText(String title,String body){
        if(body==null || body.trim().isEmpty())throw new IllegalArgumentException("No plain text was shared");
        this.title=title==null?"":title;this.body=body;
        if(this.title.length()+body.length()>100000)throw new IllegalArgumentException("Shared text exceeds the 100,000-character IPC safety limit. Use a backup for large libraries.");
    }
}
