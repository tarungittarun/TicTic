package app.space.core.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes")
    List<NoteEntity> selectAll();

    @Query("SELECT COUNT(*) FROM notes")
    int countAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NoteEntity> notes);

    @Query("DELETE FROM notes")
    void deleteAll();
}
