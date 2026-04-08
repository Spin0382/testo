package ca.ilianokokoro.umihi.music.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ca.ilianokokoro.umihi.music.models.HistorySong
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(song: HistorySong)
    
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistory(): Flow<List<HistorySong>>
    
    @Query("DELETE FROM history")
    suspend fun clearAll()
    
    @Query("DELETE FROM history WHERE youtubeId = :youtubeId")
    suspend fun deleteByYoutubeId(youtubeId: String)
}
