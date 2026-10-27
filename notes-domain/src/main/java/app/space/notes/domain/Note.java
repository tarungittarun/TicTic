package app.space.notes.domain;

import java.util.Locale;
import java.util.UUID;

/** Version-one text document. Future rich payloads require an explicit schema migration. */
public final class Note {
    public static final long TRASH_RETENTION_MS = 7L * 24 * 60 * 60 * 1000;
    public final String id, title, body;
    public final long createdAt, updatedAt, deletedAt;
    public final boolean pinned, archived;
    public final int color;

    public Note(String id, String title, String body, long createdAt, long updatedAt,
                long deletedAt, boolean pinned, boolean archived, int color) {
        if (id == null || id.isEmpty() || title == null || body == null)
            throw new IllegalArgumentException("Missing document fields");
        if (createdAt < 0 || updatedAt < createdAt || deletedAt < 0 || color < 0 || color > 11)
            throw new IllegalArgumentException("Invalid document metadata");
        this.id=id; this.title=title; this.body=body; this.createdAt=createdAt;
        this.updatedAt=updatedAt; this.deletedAt=deletedAt;
        this.pinned=pinned; this.archived=archived; this.color=color;
    }
    public static Note create(long now) {
        return new Note(UUID.randomUUID().toString(), "", "", now, now, 0, false, false, 0);
    }
    public Note edit(String title, String body, long now) {
        return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color);
    }
    public Note state(boolean pinned, boolean archived, long deletedAt, long now) {
        return new Note(id,title,body,createdAt,Math.max(updatedAt,now),deletedAt,pinned,archived,color);
    }
    public boolean expired(long now) {
        return deletedAt > 0 && now >= deletedAt && now-deletedAt >= TRASH_RETENTION_MS;
    }
    public boolean matches(String query) {
        String q=query.trim().toLowerCase(Locale.ROOT);
        return title.toLowerCase(Locale.ROOT).contains(q) || body.toLowerCase(Locale.ROOT).contains(q);
    }
}
