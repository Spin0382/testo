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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.models.HistorySong
import ca.ilianokokoro.umihi.music.ui.components.song.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    application: Application,
    onSongClick: (HistorySong) -> Unit,
    historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(application))
) {
    val historySongs by historyViewModel.historySongs.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                actions = {
                    if (historySongs.isNotEmpty()) {
                        IconButton(onClick = { historyViewModel.clearHistory() }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.clear_history)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                    items(
                        items = historySongs,
                        key = { it.id }
                    ) { song ->
                        SongListItem(
                            song = song.toSong(),
                            onPress = { onSongClick(song) },
                            playNext = { /* TODO */ },
                            addToQueue = { /* TODO */ },
                            download = null,
                            delete = { historyViewModel.deleteFromHistory(song) }
                        )
                    }
                }
            }
        }
    }
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
