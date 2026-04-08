package ca.ilianokokoro.umihi.music.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistorySong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val youtubeId: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnailHref: String,
    val timestamp: Long = System.currentTimeMillis()
)
