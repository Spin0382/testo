package ca.ilianokokoro.umihi.music.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.helpers.DownloadHelper
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoCacheWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val songId = inputData.getString(SONG_ID_KEY) ?: return@withContext Result.failure()
            
            // Verificar si ya está descargado
            val localSongRepo = AppDatabase.getInstance(appContext).songRepository()
            val existingSong = localSongRepo.getSong(songId)
            
            if (existingSong?.audioFilePath != null) {
                // Ya está descargado, no hacer nada
                return@withContext Result.success()
            }
            
            try {
                // Obtener info completa de la canción
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
                
                // Crear objeto Song básico para descargar
                val songToDownload = Song(
                    youtubeId = songId,
                    title = fullSong!!.title,
                    youtubeUrl = "https://music.youtube.com/watch?v=$songId"
                )
                
                // Descargar audio
                val audioPath = DownloadHelper.downloadAudio(appContext, songToDownload)
                
                // Descargar thumbnail
                val thumbnailPath = DownloadHelper.downloadImage(
                    appContext,
                    fullSong!!.thumbnailHref,
                    songId
                )
                
                // Guardar en base de datos
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
    
    companion object {
        const val SONG_ID_KEY = "song_id"
        const val SONG_TITLE_KEY = "song_title"
    }
}
