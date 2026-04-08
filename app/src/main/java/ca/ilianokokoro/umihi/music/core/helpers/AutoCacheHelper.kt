package ca.ilianokokoro.umihi.music.core.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import ca.ilianokokoro.umihi.music.core.workers.AutoCacheWorker
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.runBlocking

object AutoCacheHelper {
    
    fun scheduleAutoDownload(context: Context, song: Song) {
        val datastoreRepository = DatastoreRepository(context)
        val settings = runBlocking { datastoreRepository.getSettings() }
        
        if (settings.wifiOnly && !isWifiConnected(context)) {
            UmihiHelper.printd("AutoCache: Skipped - WiFi required but not connected")
            return
        }
        
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("auto_cache_${song.youtubeId}")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
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
            .addTag("auto_cache")
            .build()
        
        workManager.enqueueUniqueWork(
            "auto_cache_${song.youtubeId}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
    
    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
