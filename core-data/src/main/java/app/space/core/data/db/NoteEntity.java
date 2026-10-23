package app.space.core.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One note row. The JSON mirror in prefs stays the exchange format; Room is the in-app store. */
@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey
    public String id;

    public String title;
    public String body;
    public long updatedAt;
    public int color;
    public boolean pinned;
    public boolean archived;
}
