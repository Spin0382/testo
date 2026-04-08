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
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
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
            
            val localSongRepo = AppDatabase.getInstance(appContext).songRepository()
            val datastoreRepository = DatastoreRepository(appContext)
            val settings = datastoreRepository.getSettings()
            
            val existingSong = localSongRepo.getSong(songId)
            
            if (existingSong?.audioFilePath != null) {
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
                    return@withContext Result.failure()
                }
                
                val songToDownload = Song(
                    youtubeId = songId,
                    title = fullSong!!.title,
                    artist = fullSong!!.artist
                )
                
                val audioPath = DownloadHelper.downloadAudio(appContext, songToDownload)
                val thumbnailPath = DownloadHelper.downloadImage(
                    appContext,
                    fullSong!!.thumbnailHref,
                    songId
                )
                
                val updatedSong = fullSong!!.copy(
                    audioFilePath = audioPath,
                    thumbnailPath = thumbnailPath?.path
                )
                
                localSongRepo.create(updatedSong)
                
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
        
        val localSongRepo = AppDatabase.getInstance(appContext).songRepository()
        val downloadedSongs = localSongRepo.getDownloadedSongs()
        
        val audioDir = UmihiHelper.getDownloadDirectory(appContext, Constants.Downloads.AUDIO_FILES_FOLDER)
        val imageDir = UmihiHelper.getDownloadDirectory(appContext, Constants.Downloads.THUMBNAILS_FOLDER)
        
        var totalSize = 0L
        val songFileMap = mutableMapOf<String, File>()
        
        for (song in downloadedSongs) {
            val audioFile = File(audioDir, appContext.getString(R.string.webm_extension, song.youtubeId))
            if (audioFile.exists()) {
                totalSize += audioFile.length()
                songFileMap[song.youtubeId] = audioFile
            }
        }
        
        if (totalSize <= maxSizeBytes) return
        
        // Ordenar archivos por fecha de modificación (más antiguos primero)
        val filesSorted = songFileMap.values.sortedBy { it.lastModified() }
        
        for (file in filesSorted) {
            if (totalSize <= maxSizeBytes) break
            
            val songId = file.nameWithoutExtension
            val song = localSongRepo.getSong(songId)
            
            if (song != null) {
                totalSize -= file.length()
                file.delete()
                File(imageDir, appContext.getString(R.string.jpg_extension, songId)).delete()
                localSongRepo.delete(song)
            }
        }
    }
    
    companion object {
        const val SONG_ID_KEY = "song_id"
        const val SONG_TITLE_KEY = "song_title"
    }
}
