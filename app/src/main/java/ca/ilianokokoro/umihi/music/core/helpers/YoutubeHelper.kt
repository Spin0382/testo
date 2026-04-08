package ca.ilianokokoro.umihi.music.core.helpers

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printd
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.models.UmihiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import java.util.Locale

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

    fun getBestThumbnailUrl(thumbnailElement: JsonElement): String {
        val url =
            thumbnailElement.jsonObject["musicThumbnailRenderer"]?.jsonObject?.get("thumbnail")?.jsonObject?.get(
                "thumbnails"
            )?.jsonArray?.last()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: ""
        return url
    }

    fun getSongInfo(songMap: JsonElement, songInfoIndex: SongInfoType): String {
        return songMap.jsonObject["flexColumns"]
            ?.jsonArray?.getOrNull(songInfoIndex.index)
            ?.jsonObject?.get("musicResponsiveListItemFlexColumnRenderer")
            ?.jsonObject?.get("text")
            ?.jsonObject?.get("runs")
            ?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("text")
            ?.jsonPrimitive?.contentOrNull ?: ""
    }

    fun extractPlaylists(
        jsonString: String,
        settings: UmihiSettings
    ): List<PlaylistInfo> {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        val playlistInfos = mutableListOf<PlaylistInfo>()

        val tabs = json["contents"]
            ?.jsonObject?.get("singleColumnBrowseResultsRenderer")
            ?.jsonObject?.get("tabs")
            ?.jsonArray

        val selectedTab = tabs?.firstOrNull {
            it.jsonObject["tabRenderer"]
                ?.jsonObject?.get("selected")
                ?.jsonPrimitive?.booleanOrNull == true
        }?.jsonObject?.get("tabRenderer")?.jsonObject

        val sectionList = selectedTab?.get("content")
            ?.jsonObject?.get("sectionListRenderer")
            ?.jsonObject?.get("contents")
            ?.jsonArray

        sectionList?.forEach { section ->
            val renderer = section.jsonObject["gridRenderer"]?.jsonObject ?: return@forEach

            renderer["items"]?.jsonArray?.forEach { item ->
                val playlistRenderer = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                    ?: return@forEach

                val title = playlistRenderer["title"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.contentOrNull ?: return@forEach

                val playlistId = playlistRenderer["navigationEndpoint"]
                    ?.jsonObject?.get("browseEndpoint")
                    ?.jsonObject?.get("browseId")
                    ?.jsonPrimitive?.contentOrNull ?: return@forEach

                val thumbnailRenderer = playlistRenderer["thumbnailRenderer"]
                    ?.jsonObject?.get("musicThumbnailRenderer")
                    ?.jsonObject

                val thumbnailUrl = thumbnailRenderer?.get("thumbnail")
                    ?.jsonObject?.get("thumbnails")
                    ?.jsonArray?.lastOrNull()
                    ?.jsonObject?.get("url")
                    ?.jsonPrimitive?.contentOrNull ?: ""

                val subtitle = playlistRenderer["subtitle"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.contentOrNull ?: ""

                playlistInfos.add(
                    PlaylistInfo(
                        id = playlistId,
                        title = title,
                        coverHref = thumbnailUrl,
                        description = subtitle
                    )
                )
            }
        }

        return playlistInfos
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

        var attempts = 0

        repeat(retries) { attempt ->
            try {
                attempts++
                val streamUrl = withContext(Dispatchers.IO) {
                    val extractor = service.getStreamExtractor(song.youtubeUrl)
                    extractor.fetchPage()
                    extractor.audioStreams.maxBy { it.averageBitrate }.content
                }
                
                if (streamUrl.isNotEmpty()) {
                    return streamUrl
                }
            } catch (e: Exception) {
                printe("Attempt $attempts failed for ${song.youtubeId}: ${e.message}")
                if (attempts >= retries) {
                    throw e
                }
                delay(1000L * attempts)
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
