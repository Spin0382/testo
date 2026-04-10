package ca.ilianokokoro.umihi.music.ui.screens.history
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper
import java.io.File

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
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
        viewModelScope.launch {
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
            val song = Song(
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                youtubeId = historySong.youtubeId,
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                title = historySong.title,
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                artist = historySong.artist,
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                duration = historySong.duration,
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                thumbnailHref = historySong.thumbnailHref
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
            )
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
            if (!song.downloaded) return@launch
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
            val playlist = Playlist(
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                info = PlaylistInfo(
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                    id = Constants.Downloads.DOWNLOADED_PLAYLIST_ID,
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                    title = "Downloads",
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                    coverHref = ""
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                ),
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
                songs = listOf(song)
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
            )
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
            downloadRepository.deleteSong(playlist, song)
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
        }
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
        }
    }
    }
 
    fun deleteCache(historySong: HistorySong) {
        viewModelScope.launch {
            val audioDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.AUDIO_FILES_FOLDER)
            val cachedFile = File(audioDir, getApplication().getString(R.string.webm_extension, historySong.youtubeId))
            if (cachedFile.exists()) {
                cachedFile.delete()
            }
            val imageDir = UmihiHelper.getDownloadDirectory(getApplication(), Constants.Downloads.THUMBNAILS_FOLDER)
            File(imageDir, getApplication().getString(R.string.jpg_extension, historySong.youtubeId)).delete()
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
