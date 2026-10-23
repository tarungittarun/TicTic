package app.space.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One note row. The JSON mirror in prefs stays the exchange format; Room is the in-app store. */
@Entity(tableName = "notes")
public class NoteEntity {

    // Room refuses a nullable String primary key -- SQLite itself considers one a bug --
    // and the codegen says so in terms only the codegen can teach.
    @NonNull
    @PrimaryKey
    public String id = "";

    public String title;
    public String body;
    public long updatedAt;
    public int color;
    public boolean pinned;
    public boolean archived;
}
