package ca.ilianokokoro.umihi.music.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.helpers.YoutubeHelper
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.data.repositories.PlaylistRepository
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeState())
    val uiState = _uiState.asStateFlow()

    private val playlistRepository = PlaylistRepository()
    private val songRepository = SongRepository()
    private val datastoreRepository = DatastoreRepository(application)

    fun getPlaylists() {
        viewModelScope.launch {
            val settings = datastoreRepository.getSettings()
            if (settings.cookies.isEmpty()) {
                _uiState.update {
                    _uiState.value.copy(screenState = ScreenState.LoggedOut)
                }
                return@launch
            }

            playlistRepository.retrieveAll(settings).collect { apiResult ->
                _uiState.update {
                    _uiState.value.copy(
                        screenState = when (apiResult) {
                            is ApiResult.Loading -> ScreenState.Loading
                            is ApiResult.Error -> ScreenState.Error(apiResult.exception)
                            is ApiResult.Success -> ScreenState.LoggedIn(apiResult.data)
                        }
                    )
                }
            }
        }
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            getPlaylists()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun addFromLink(link: String, onSuccess: (Song) -> Unit) {
        viewModelScope.launch {
            val videoId = YoutubeHelper.extractYouTubeVideoId(link) ?: return@launch
            songRepository.getSongInfo(videoId).collect { result ->
                if (result is ApiResult.Success) {
                    onSuccess(result.data)
                }
            }
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(application)
            }
        }
    }
}
