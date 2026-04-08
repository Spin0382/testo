package ca.ilianokokoro.umihi.music.core.helpers

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printd
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList

object YoutubeHelper {
    private val client = OkHttpClient()

    fun extractYouTubeVideoId(url: String): String? {
        val uri = url.toUri()

        return when {
            uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
            uri.host?.contains("youtube.com") == true || uri.host?.contains("music.youtube.com") == true -> uri.getQueryParameter(
                "v"
            )
            else -> null
        }
    }

    suspend fun getSongPlayerUrl(
        context: Context,
        song: Song,
        allowLocal: Boolean = false
    ): String {
        val localSongRepository = AppDatabase.getInstance(context).songRepository()
        var savedSong: Song? = null
        try {
            savedSong = localSongRepository.getSong(song.youtubeId)
        } catch (ex: Exception) {
            Toast.makeText(context, "Failed to get song from local repository", Toast.LENGTH_LONG)
                .show()
            printe(ex.toString())
        }

        if (savedSong != null) {
            if (allowLocal && savedSong.audioFilePath != null) {
                printd("${song.youtubeId} : Was downloaded")
                return savedSong.audioFilePath
            }

            if (savedSong.streamUrl != null) {
                if (isYoutubeUrlValid(savedSong.streamUrl)) {
                    printd("${song.youtubeId} : Got url from saved")
                    return savedSong.streamUrl
                }
                printd("${song.youtubeId} : Saved url was invalid")
            }
        }

        val newUri = getSongUrlFromYoutube(song)
        
        // ⭐⭐⭐ AUTO-CACHÉ: Programar descarga silenciosa para futuro offline ⭐⭐⭐
        // Solo si allowLocal está activado Y la canción no está ya descargada
        if (allowLocal && (savedSong == null || savedSong.audioFilePath == null)) {
            try {
                AutoCacheHelper.scheduleAutoDownload(context, song)
                printd("${song.youtubeId} : Scheduled auto-download for offline use")
            } catch (e: Exception) {
                // Si falla la programación, no interrumpir la reproducción
                printe("${song.youtubeId} : Failed to schedule auto-download: ${e.message}")
            }
        }
        
        localSongRepository.setStreamUrl(songId = song.youtubeId, streamUrl = newUri)
        printd("${song.youtubeId} : Got url from YouTube and saved song")
        return newUri
    }

    private suspend fun getSongUrlFromYoutube(
        song: Song,
        retries: Int = Constants.YoutubeApi.RETRY_COUNT
    ): String {
        val service = ServiceList.YouTube

        repeat(retries) { attempt ->
            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    val extractor = service.getStreamExtractor(song.youtubeUrl)
                    extractor.fetchPage()
                    extractor.audioStreams.maxBy { it.averageBitrate }.content
                }
                
                if (streamUrl.isNotEmpty()) {
                    return streamUrl
                }
            } catch (e: Exception) {
                printe("Attempt ${attempt + 1} failed for ${song.youtubeId}: ${e.message}")
                if (attempt == retries - 1) {
                    throw e
                }
                delay(1000L * (attempt + 1))
            }
        }
        
        throw Exception("Failed to get stream URL after $retries attempts")
    }

    private fun isYoutubeUrlValid(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
