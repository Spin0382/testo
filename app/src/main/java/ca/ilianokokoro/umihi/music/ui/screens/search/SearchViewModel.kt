package ca.ilianokokoro.umihi.music.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.data.repositories.DownloadRepository
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SearchState())
    val uiState = _uiState.asStateFlow()

    private val songRepository = SongRepository()
    private val downloadRepository = DownloadRepository(application)
    private val localSongRepository = AppDatabase.getInstance(application).songRepository()

    fun search() {
        viewModelScope.launch {
            val query = uiState.value.search
            if (query.isBlank()) {
                _uiState.update {
                    _uiState.value.copy(
                        screenState = ScreenState.Success(results = listOf())
                    )
                }
                return@launch
            }

            songRepository.search(query).collect { apiResult ->
                _uiState.update {
                    _uiState.value.copy(
                        screenState = when (apiResult) {
                            ApiResult.Loading -> ScreenState.Loading
                            is ApiResult.Error -> ScreenState.Error(apiResult.exception)
                            is ApiResult.Success -> ScreenState.Success(results = apiResult.data)
                        }
                    )
                }
            }
        }
    }

    fun onSearchFieldChange(newValue: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(search = newValue)
            }
        }
    }

    fun downloadSong(song: Song) {
        if (song.downloaded) return
        viewModelScope.launch {
            val playlist = Playlist(
                info = PlaylistInfo(
                    id = Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
                    title = "Downloads",
                    coverHref = ""
                ),
                songs = listOf(song)
            )
            downloadRepository.downloadSong(playlist, song)
        }
    }

    fun deleteSong(song: Song) {
        if (!song.downloaded) return
        viewModelScope.launch {
            val playlist = Playlist(
                info = PlaylistInfo(
                    id = Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
                    title = "Downloads",
                    coverHref = ""
                ),
                songs = listOf(song)
            )
            downloadRepository.deleteSong(playlist, song)
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SearchViewModel(application)
            }
        }
    }
}
