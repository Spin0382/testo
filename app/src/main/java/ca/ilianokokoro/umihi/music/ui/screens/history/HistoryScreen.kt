package ca.ilianokokoro.umihi.music.ui.screens.history

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.addNext
import ca.ilianokokoro.umihi.music.extensions.addToQueue
import ca.ilianokokoro.umihi.music.extensions.isCurrentSong
import ca.ilianokokoro.umihi.music.extensions.playSongPreserveQueue
import ca.ilianokokoro.umihi.music.models.HistorySong
import ca.ilianokokoro.umihi.music.ui.components.song.SongListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    application: Application,
    onSongClick: (HistorySong) -> Unit,
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(application))
) {
    val historySongs by historyViewModel.historySongs.collectAsStateWithLifecycle()
    val groupedSongs = groupSongsByDate(historySongs)
    val scope = rememberCoroutineScope()
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (historySongs.isNotEmpty()) {
                IconButton(
                    onClick = { historyViewModel.clearHistory() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.clear_history)
                    )
                }
            }
            
            if (historySongs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_history),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    groupedSongs.forEach { (dateHeader, songs) ->
                        item(key = dateHeader) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(
                            items = songs,
                            key = { it.id }
                        ) { song ->
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val controller = PlayerManager.currentController
                            val songObj = song.toSong()
                            val isCurrent = controller?.isCurrentSong(songObj) == true
                            
                            var cacheKey by remember { mutableStateOf(0) }
                            
                            SongListItem(
                                song = songObj,
                                onPress = {
                                    if (isCurrent) {
                                        // Ya está sonando: solo abrir reproductor
                                        onSongClick(song)
                                    } else {
                                        // Canción diferente: reproducir sin perder la cola
                                        controller?.playSongPreserveQueue(songObj)
                                        onSongClick(song)
                                    }
                                },
                                playNext = { 
                                    controller?.addNext(songObj, context) 
                                },
                                addToQueue = { 
                                    controller?.addToQueue(songObj, context) 
                                },
                                download = { historyViewModel.downloadSong(song) },
                                delete = { historyViewModel.deleteSong(song) },
                                deleteCache = {
                                    historyViewModel.deleteCache(song)
                                    cacheKey++
                                    scope.launch {
                                        delay(100)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun groupSongsByDate(songs: List<HistorySong>): List<Pair<String, List<HistorySong>>> {
    val calendar = Calendar.getInstance()
    val today = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = calendar.time
    
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    
    return songs.groupBy { song ->
        val songDate = Date(song.timestamp)
        when {
            dayFormat.format(songDate) == dayFormat.format(today) -> "Hoy"
            dayFormat.format(songDate) == dayFormat.format(yesterday) -> "Ayer"
            else -> dateFormat.format(songDate)
        }
    }.toList().sortedByDescending { (_, songs) -> songs.first().timestamp }
}

fun HistorySong.toSong(): ca.ilianokokoro.umihi.music.models.Song {
    return ca.ilianokokoro.umihi.music.models.Song(
        youtubeId = this.youtubeId,
        title = this.title,
        artist = this.artist,
        duration = this.duration,
        thumbnailHref = this.thumbnailHref
    )
}
