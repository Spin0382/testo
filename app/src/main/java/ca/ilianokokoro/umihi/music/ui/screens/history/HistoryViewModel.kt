package ca.ilianokokoro.umihi.music.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.data.repositories.DownloadRepository
import ca.ilianokokoro.umihi.music.models.HistorySong
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyDao = AppDatabase.getInstance(application).historyDao()
    private val downloadRepository = DownloadRepository(application)
    
    val historySongs = historyDao.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }
    
    fun deleteFromHistory(song: HistorySong) {
        viewModelScope.launch {
            historyDao.deleteByYoutubeId(song.youtubeId)
        }
    }
    
    fun downloadSong(historySong: HistorySong) {
        viewModelScope.launch {
            val song = Song(
                youtubeId = historySong.youtubeId,
                title = historySong.title,
                artist = historySong.artist,
                duration = historySong.duration,
                thumbnailHref = historySong.thumbnailHref
            )
            if (song.downloaded) return@launch
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
    
    fun deleteSong(historySong: HistorySong) {
        viewModelScope.launch {
            val song = Song(
                youtubeId = historySong.youtubeId,
                title = historySong.title,
                artist = historySong.artist,
                duration = historySong.duration,
                thumbnailHref = historySong.thumbnailHref
            )
            if (!song.downloaded) return@launch
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
                HistoryViewModel(application)
            }
        }
    }
}
