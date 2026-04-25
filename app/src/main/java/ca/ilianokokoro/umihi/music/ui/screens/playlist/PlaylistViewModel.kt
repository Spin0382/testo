package ca.ilianokokoro.umihi.music.ui.screens.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printd
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.data.repositories.DownloadRepository
import ca.ilianokokoro.umihi.music.data.repositories.PlaylistRepository
import ca.ilianokokoro.umihi.music.extensions.playPlaylist
import ca.ilianokokoro.umihi.music.extensions.shufflePlaylist
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaylistViewModel(playlistInfo: PlaylistInfo, application: Application) :
    AndroidViewModel(application) {
    private val _playlist = playlistInfo
    private val _uiState = MutableStateFlow(
        PlaylistState(screenState = ScreenState.Loading(_playlist))
    )
    val uiState = _uiState.asStateFlow()

    private val playlistRepository = PlaylistRepository()
    private val localPlaylistRepository = AppDatabase.getInstance(application).playlistRepository()
    private val localSongRepository = AppDatabase.getInstance(application).songRepository()
    private val datastoreRepository = DatastoreRepository(application)
    private val downloadRepository = DownloadRepository(application)

    init {
        observeSongDownloads()
        viewModelScope.launch {
            getPlaylistInfoAsync()
            observerDownloadJob()
        }
    }

    private fun observeSongDownloads() {
        viewModelScope.launch {
            localPlaylistRepository.observePlaylistById(_playlist.id).collect { localPlaylist ->
                if (localPlaylist != null) {
                    _uiState.update { currentState ->
                        val screenState = currentState.screenState
                        if (screenState is ScreenState.Success) {
                            currentState.copy(
                                screenState = screenState.copy(
                                    playlist = updatePlaylistFrom(screenState.playlist, localPlaylist)
                                )
                            )
                        } else currentState
                    }
                }
            }
        }
    }

    suspend fun observerDownloadJob() {
        val playlist = getPlaylist() ?: return
        val existingJobFlow = downloadRepository.getExistingJobFlow(playlist)
        existingJobFlow.collect { workInfos ->
            val workInfo = workInfos.firstOrNull() ?: return@collect
            _uiState.update {
                it.copy(
                    isDownloading = workInfo.state == WorkInfo.State.ENQUEUED ||
                            workInfo.state == WorkInfo.State.RUNNING ||
                            workInfo.state == WorkInfo.State.BLOCKED
                )
            }
        }
    }

    fun refreshPlaylistInfo() {
        viewModelScope.launch {
            _uiState.update { _uiState.value.copy(isRefreshing = true) }
            getPlaylistInfoAsync()
            _uiState.update { _uiState.value.copy(isRefreshing = false) }
        }
    }

    fun getPlaylistInfo() {
        viewModelScope.launch { getPlaylistInfoAsync() }
    }

    fun refreshAfterAdd() {
        viewModelScope.launch { getPlaylistInfoAsync() }
    }

    fun playPlaylist(startingSong: Song? = null) {
        val playlist = getPlaylist() ?: return
        viewModelScope.launch {
            PlayerManager.currentController?.playPlaylist(
                playlist, startingSong?.let { playlist.songs.indexOf(it) } ?: 0
            )
        }
    }

    fun shufflePlaylist() {
        val playlist = getPlaylist() ?: return
        viewModelScope.launch { PlayerManager.currentController?.shufflePlaylist(playlist) }
    }

    fun downloadPlaylist() {
        val playlist = getPlaylist() ?: return
        viewModelScope.launch {
            if (!playlist.downloaded) downloadRepository.downloadPlaylist(playlist)
        }
    }

    fun deletePlaylist() {
        val playlist = getPlaylist() ?: return
        viewModelScope.launch {
            downloadRepository.deletePlaylist(playlist)
            // Emitir evento para cerrar pantalla
            _uiState.update { it.copy(screenState = ScreenState.Error(Exception("deleted"))) }
        }
    }

    fun cancelDownload() {
        if (!uiState.value.isDownloading) return
        val playlist = getPlaylist() ?: return
        viewModelScope.launch { downloadRepository.cancelPlaylistDownload(playlist) }
    }

    fun downloadSong(song: Song) {
        val playlist = getPlaylist() ?: return
        if (song.downloaded) return
        viewModelScope.launch { downloadRepository.downloadSong(playlist, song) }
    }

    fun deleteSong(song: Song) {
        val playlist = getPlaylist() ?: return
        if (!song.downloaded) return
        viewModelScope.launch {
            downloadRepository.deleteSong(playlist, song)
            getPlaylistInfoAsync()
        }
    }

    fun removeSongFromPlaylist(song: Song) {
        val playlist = getPlaylist() ?: return
        viewModelScope.launch {
            localPlaylistRepository.removeSongFromPlaylist(playlist.info.id, song.youtubeId)
            getPlaylistInfoAsync()
        }
    }

    private suspend fun getPlaylistInfoAsync() {
        try {
            // Si es una playlist local, NUNCA consultamos el servidor
            if (_playlist.id.startsWith("local_")) {
                val localPlaylist = localPlaylistRepository.getPlaylistById(_playlist.id)
                if (localPlaylist != null) {
                    _uiState.update {
                        it.copy(screenState = ScreenState.Success(playlist = localPlaylist))
                    }
                } else {
                    _uiState.update {
                        it.copy(screenState = ScreenState.Error(Exception("Playlist no encontrada")))
                    }
                }
                return
            }

            // Para el resto (remotas, downloads), comportamiento normal
            val localPlaylist = localPlaylistRepository.getPlaylistById(_playlist.id)
            val settings = datastoreRepository.getSettings()

            if (settings.cookies.isEmpty()) {
                if (_playlist.id == Constants.Downloads.DOWNLOADED_PLAYLIST_ID) {
                    _uiState.update {
                        it.copy(screenState = ScreenState.Success(playlist = localPlaylist?.copy(songs = localPlaylist.songs.filter { it.downloaded }) ?: Playlist(_playlist)))
                    }
                } else {
                    _uiState.update {
                        it.copy(screenState = ScreenState.Error(Exception("Playlist no disponible sin conexión")))
                    }
                }
                return
            }

            playlistRepository.retrieveOne(Playlist(_playlist), settings).collect { apiResult ->
                _uiState.update {
                    it.copy(
                        screenState = when (apiResult) {
                            is ApiResult.Error -> {
                                if (localPlaylist != null)
                                    ScreenState.Success(playlist = localPlaylist.copy(songs = localPlaylist.songs.filter { it.downloaded }))
                                else
                                    ScreenState.Error(Exception("Playlist is not downloaded"))
                            }
                            ApiResult.Loading -> ScreenState.Loading(_playlist)
                            is ApiResult.Success -> {
                                val remotePlaylist = apiResult.data
                                ScreenState.Success(playlist = updatePlaylistFrom(remotePlaylist, localPlaylist))
                            }
                        }
                    )
                }
            }
        } catch (ex: Exception) {
            printe(message = ex.toString(), exception = ex)
            _uiState.update { it.copy(screenState = ScreenState.Error(ex)) }
        }
    }

    private fun updatePlaylistFrom(oldPlaylist: Playlist, updatedPlaylist: Playlist?): Playlist {
        if (updatedPlaylist != null) {
            val localMap = updatedPlaylist.songs.associateBy { it.youtubeId }
            val updatedSongs = oldPlaylist.songs.map { remoteSong ->
                localMap[remoteSong.youtubeId]?.copy(uid = java.util.UUID.randomUUID().toString()) ?: remoteSong
            }
            return oldPlaylist.copy(songs = updatedSongs)
        }
        return oldPlaylist
    }

    private fun getPlaylist(): Playlist? {
        val screenState = _uiState.value.screenState
        return if (screenState is ScreenState.Success) screenState.playlist else null
    }

    companion object {
        fun Factory(playlistInfo: PlaylistInfo, application: Application): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PlaylistViewModel(playlistInfo, application) }
            }
    }
}
