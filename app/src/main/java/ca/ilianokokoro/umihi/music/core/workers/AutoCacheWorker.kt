package ca.ilianokokoro.umihi.music.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.ilianokokoro.umihi.music.core.ApiResult
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
            
            // Verificar límite de caché antes de descargar
            val cacheLimitBytes = when (settings.cacheLimit) {
                0 -> 500L * 1024 * 1024  // 500 MB
                1 -> 1024L * 1024 * 1024 // 1 GB
                2 -> 2048L * 1024 * 1024 // 2 GB
                else -> Long.MAX_VALUE    // Ilimitado
            }
            
            if (cacheLimitBytes < Long.MAX_VALUE) {
                enforceCacheLimit(cacheLimitBytes)
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
                
                Result.success()
            } catch (e: Exception) {
                printe("AutoCacheWorker failed for $songId: ${e.message}")
                Result.failure()
            }
        }
    }
    
    private suspend fun enforceCacheLimit(maxSizeBytes: Long) {
        val localSongRepo = AppDatabase.getInstance(appContext).songRepository()
        val allSongs = localSongRepo.getAllSongs()
        
        var currentSize = allSongs.sumOf { song ->
            song.audioFilePath?.let { File(it).length() } ?: 0L
        }
        
        if (currentSize <= maxSizeBytes) return
        
        // Ordenar por fecha de último acceso (más antiguas primero)
        val songsSortedByLastAccess = allSongs.sortedBy { it.lastAccessed }
        
        val audioDir = UmihiHelper.getDownloadDirectory(
            appContext,
            ca.ilianokokoro.umihi.music.core.Constants.Downloads.AUDIO_FILES_FOLDER
        )
        val imageDir = UmihiHelper.getDownloadDirectory(
            appContext,
            ca.ilianokokoro.umihi.music.core.Constants.Downloads.THUMBNAILS_FOLDER
        )
        
        for (song in songsSortedByLastAccess) {
            if (currentSize <= maxSizeBytes) break
            
            song.audioFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    currentSize -= file.length()
                    file.delete()
                }
            }
            
            song.thumbnailPath?.let { path ->
                File(path).delete()
            }
            
            // También eliminar de las carpetas por si acaso
            File(audioDir, appContext.getString(ca.ilianokokoro.umihi.music.R.string.webm_extension, song.youtubeId)).delete()
            File(imageDir, appContext.getString(ca.ilianokokoro.umihi.music.R.string.jpg_extension, song.youtubeId)).delete()
            
            // Actualizar en BD
            localSongRepo.update(song.copy(audioFilePath = null, thumbnailPath = null))
        }
    }
    
    companion object {
        const val SONG_ID_KEY = "song_id"
        const val SONG_TITLE_KEY = "song_title"
    }
}
