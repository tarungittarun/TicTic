package app.space.core.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One calendar event row; whenMs == -1 means all-day, matching CalendarRules.NO_TIME. */
@Entity(tableName = "events")
public class EventEntity {

    @PrimaryKey
    public String id;

    public String title;
    public String notes;
    public long whenMs;
    public int reminderMinutes;
}
