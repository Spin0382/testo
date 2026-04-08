package ca.ilianokokoro.umihi.music.core.helpers

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ca.ilianokokoro.umihi.music.core.workers.AutoCacheWorker
import ca.ilianokokoro.umihi.music.models.Song

object AutoCacheHelper {
    
    fun scheduleAutoDownload(context: Context, song: Song) {
        // Solo programar si no hay ya un trabajo para esta canción
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Solo WiFi
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
        
        val request = OneTimeWorkRequestBuilder<AutoCacheWorker>()
            .setInputData(
                workDataOf(
                    AutoCacheWorker.SONG_ID_KEY to song.youtubeId,
                    AutoCacheWorker.SONG_TITLE_KEY to song.title
                )
            )
            .setConstraints(constraints)
            .addTag("auto_cache_${song.youtubeId}")
            .build()
        
        workManager.enqueue(request)
    }
}
