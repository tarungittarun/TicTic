package app.space.core.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One calendar event row; whenMs == -1 means all-day, matching CalendarRules.NO_TIME. */
@Entity(tableName = "events")
public class EventEntity {

    // Room refuses a nullable String primary key -- SQLite itself considers one a bug --
    // and the codegen says so in terms only the codegen can teach.
    @NonNull
    @PrimaryKey
    public String id = "";

    public String title;
    public String notes;
    public long whenMs;
    public int reminderMinutes;
}
