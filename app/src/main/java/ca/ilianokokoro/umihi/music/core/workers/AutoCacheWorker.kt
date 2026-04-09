package ca.ilianokokoro.umihi.music.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.DownloadHelper
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AutoCacheWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val songId = inputData.getString(SONG_ID_KEY) ?: return@withContext Result.failure()
            val songTitle = inputData.getString(SONG_TITLE_KEY) ?: ""
            
            val datastoreRepository = DatastoreRepository(appContext)
            val settings = datastoreRepository.getSettings()
            
            // Verificar si el archivo ya existe
            val audioDir = UmihiHelper.getDownloadDirectory(appContext, Constants.Downloads.AUDIO_FILES_FOLDER)
            val audioFile = File(audioDir, appContext.getString(R.string.webm_extension, songId))
            
            if (audioFile.exists()) {
                UmihiHelper.printd("AutoCacheWorker: File already exists for $songId")
                return@withContext Result.success()
            }
            
            try {
                val songRepository = SongRepository()
                var fullSong: Song? = null
                
                songRepository.getSongInfo(songId).collect { result ->
                    if (result is ApiResult.Success) {
                        fullSong = result.data
                    }
                }
                
                if (fullSong == null) {
                    printe("AutoCacheWorker: Failed to get song info for $songId")
                    return@withContext Result.failure()
                }
                
                val songToDownload = Song(
                    youtubeId = songId,
                    title = fullSong!!.title,
                    artist = fullSong!!.artist
                )
                
                // Descargar audio (solo archivo, NO guardar en BD de Descargas)
                val audioPath = DownloadHelper.downloadAudio(appContext, songToDownload)
                
                // Descargar thumbnail
                DownloadHelper.downloadImage(
                    appContext,
                    fullSong!!.thumbnailHref,
                    songId
                )
                
                UmihiHelper.printd("AutoCacheWorker: Downloaded $songTitle successfully")
                
                // Verificar límite de caché
                enforceCacheLimit(settings.cacheLimit)
                
                Result.success()
            } catch (e: Exception) {
                printe("AutoCacheWorker failed for $songId: ${e.message}")
                Result.failure()
            }
        }
    }
    
    private suspend fun enforceCacheLimit(cacheLimit: Int) {
        if (cacheLimit == 3) return // Ilimitado
        
        val maxSizeBytes = when (cacheLimit) {
            0 -> 500L * 1024 * 1024
            1 -> 1024L * 1024 * 1024
            2 -> 2048L * 1024 * 1024
            else -> return
        }
        
        val audioDir = UmihiHelper.getDownloadDirectory(appContext, Constants.Downloads.AUDIO_FILES_FOLDER)
        val imageDir = UmihiHelper.getDownloadDirectory(appContext, Constants.Downloads.THUMBNAILS_FOLDER)
        
        val files = audioDir.listFiles() ?: return
        var totalSize = files.sumOf { it.length() }
        
        if (totalSize <= maxSizeBytes) return
        
        // Ordenar por fecha de modificación (más antiguos primero)
        files.sortBy { it.lastModified() }
        
        for (file in files) {
            if (totalSize <= maxSizeBytes) break
            
            val songId = file.nameWithoutExtension
            totalSize -= file.length()
            file.delete()
            
            // Eliminar thumbnail
            File(imageDir, appContext.getString(R.string.jpg_extension, songId)).delete()
            
            UmihiHelper.printd("AutoCacheWorker: Deleted old cache file: $songId")
        }
    }
    
    companion object {
        const val SONG_ID_KEY = "song_id"
        const val SONG_TITLE_KEY = "song_title"
    }
}
