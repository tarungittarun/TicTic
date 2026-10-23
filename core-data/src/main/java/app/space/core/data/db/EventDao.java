package app.space.core.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {

    @Query("SELECT * FROM events")
    List<EventEntity> selectAll();

    @Query("SELECT COUNT(*) FROM events")
    int countAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EventEntity> events);

    @Query("DELETE FROM events")
    void deleteAll();
}
