package app.space.core.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * The suite's one database.
 *
 * <p>allowMainThreadQueries is deliberate, not an oversight: every screen in this suite builds
 * synchronously on the main thread -- that is the architecture the whole shell was extracted
 * from -- and the tables are capped at 500 notes and 1000 events. An async layer here would
 * mean rewriting thirteen screens' data flow for no user-visible gain; the comment is the
 * contract's target, and a future sprint may cash it.
 */
@Database(entities = {NoteEntity.class, EventEntity.class}, version = 1, exportSchema = false)
public abstract class MySpaceDatabase extends RoomDatabase {

    public abstract NoteDao noteDao();
    public abstract EventDao eventDao();

    private static MySpaceDatabase instance;

    public static synchronized MySpaceDatabase get(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            MySpaceDatabase.class, "myspace.db")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    /** Closes and forgets the instance; used when a restore replaces the ground under it. */
    public static synchronized void shutdown() {
        if (instance != null) {
            try { instance.close(); } catch (Exception ignored) { }
            instance = null;
        }
    }
}
