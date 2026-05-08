package ca.ilianokokoro.umihi.music.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.YoutubeHelper
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.data.repositories.PlaylistRepository
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeState(screenState = ScreenState.LoggedIn(emptyList())))
    val uiState = _uiState.asStateFlow()

    private val playlistRepository = PlaylistRepository()
    private val songRepository = SongRepository()
    private val datastoreRepository = DatastoreRepository(application)
    private val localPlaylistRepository = AppDatabase.getInstance(application).playlistRepository()

    init {
        // Solo carga el contenido local inmediatamente; las remotas, bajo demanda (pull)
        loadLocalAndDownloads()
    }

    private fun loadLocalAndDownloads() {
        viewModelScope.launch {
            val downloadsPlaylist = PlaylistInfo(
                id = Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
                title = "Downloads",
                coverHref = ""
            )
            val localPlaylists = try {
                localPlaylistRepository.getLocalPlaylists()
            } catch (e: Exception) {
                emptyList()
            }
            val combined = buildList {
                add(downloadsPlaylist)
                addAll(localPlaylists)
            }
            _uiState.update { it.copy(screenState = ScreenState.LoggedIn(combined)) }
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val downloadsPlaylist = PlaylistInfo(
                id = Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
                title = "Downloads",
                coverHref = ""
            )

            val settings = datastoreRepository.getSettings()
            val remotePlaylists = if (!settings.cookies.isEmpty()) {
                try {
                    var resultList = emptyList<PlaylistInfo>()
                    playlistRepository.retrieveAll(settings).collect { apiResult ->
                        if (apiResult is ApiResult.Success) {
                            resultList = apiResult.data
                        }
                    }
                    resultList
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            val localPlaylists = try {
                localPlaylistRepository.getLocalPlaylists()
            } catch (e: Exception) {
                emptyList()
            }

            val combined = buildList {
                add(downloadsPlaylist)
                addAll(remotePlaylists)
                addAll(localPlaylists)
            }

            _uiState.update {
                it.copy(
                    screenState = ScreenState.LoggedIn(combined),
                    isRefreshing = false
                )
            }
        }
    }

    suspend fun extractSongFromLink(link: String): Result<Song> {
        val videoId = YoutubeHelper.extractYouTubeVideoId(link)
            ?: return Result.failure(Exception("Invalid YouTube link"))
        return try {
            val result = songRepository.getSongInfo(videoId).firstOrNull { it is ApiResult.Success }
                ?: throw Exception("Could not fetch song info")
            val song = (result as ApiResult.Success<Song>).data
            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(application) }
        }
    }
}
