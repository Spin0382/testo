@file:OptIn(ExperimentalMaterial3Api::class)

package ca.ilianokokoro.umihi.music.ui.screens.playlist

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.extensions.addNext
import ca.ilianokokoro.umihi.music.extensions.addToQueue
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.ErrorMessage
import ca.ilianokokoro.umihi.music.ui.components.LoadingAnimation
import ca.ilianokokoro.umihi.music.ui.components.dialog.AddToPlaylistDialog
import ca.ilianokokoro.umihi.music.ui.components.song.SongListItem
import ca.ilianokokoro.umihi.music.ui.screens.playlist.components.PlaylistHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistScreen(
    playlistInfo: PlaylistInfo,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    application: Application,
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(playlistInfo = playlistInfo, application = application)
    )
) {
    val uiState = playlistViewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    val localPlaylists = remember { mutableStateListOf<PlaylistInfo>() }

    LaunchedEffect(Unit) {
        if (playlistInfo.id.startsWith("local_")) {
            localPlaylists.clear()
            localPlaylists.addAll(
                AppDatabase.getInstance(application).playlistRepository().getLocalPlaylists().filter { it.id != playlistInfo.id }
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.screenState is ScreenState.Error) {
            ErrorMessage(ex = uiState.screenState.exception, onRetry = playlistViewModel::getPlaylistInfo)
        } else {
            val playlist: Playlist = when (uiState.screenState) {
                is ScreenState.Loading -> Playlist(uiState.screenState.playlistInfo)
                is ScreenState.Success -> uiState.screenState.playlist
            }
            val songs = playlist.songs

            if (uiState.screenState is ScreenState.Loading || songs.isEmpty()) {
                PlaylistHeader(
                    onOpenPlayer = onOpenPlayer,
                    isDownloading = uiState.isDownloading,
                    onDownloadPlaylist = playlistViewModel::downloadPlaylist,
                    onShufflePlaylist = playlistViewModel::shufflePlaylist,
                    onPlayPlaylist = playlistViewModel::playPlaylist,
                    onDeletePlaylist = playlistViewModel::deletePlaylist,
                    onCancelDownload = playlistViewModel::cancelDownload,
                    playlist = playlist
                )
                if (uiState.screenState is ScreenState.Loading) LoadingAnimation()
                else {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = modifier.fillMaxSize()
                    ) { Text(stringResource(R.string.empty_playlist), textAlign = TextAlign.Center) }
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = playlistViewModel::refreshPlaylistInfo,
                    modifier = modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING)
                    ) {
                        item {
                            PlaylistHeader(
                                onOpenPlayer = onOpenPlayer,
                                isDownloading = uiState.isDownloading,
                                onDownloadPlaylist = playlistViewModel::downloadPlaylist,
                                onShufflePlaylist = playlistViewModel::shufflePlaylist,
                                onPlayPlaylist = playlistViewModel::playPlaylist,
                                onDeletePlaylist = playlistViewModel::deletePlaylist,
                                onCancelDownload = playlistViewModel::cancelDownload,
                                playlist = playlist
                            )
                        }
                        items(items = songs, key = { song -> song.uid }) { song ->
                            SongListItem(
                                song = song,
                                onPress = {
                                    onOpenPlayer()
                                    playlistViewModel.playPlaylist(song)
                                },
                                playNext = { PlayerManager.currentController?.addNext(song, application) },
                                addToQueue = { PlayerManager.currentController?.addToQueue(song, application) },
                                download = { playlistViewModel.downloadSong(song) },
                                delete = if (playlist.info.id.startsWith("local_"))
                                    null  // no aplica
                                else
                                    { playlistViewModel.deleteSong(song) },
                                deleteFromHistory = if (playlist.info.id.startsWith("local_"))
                                    {
                                        playlistViewModel.removeSongFromPlaylist(song)
                                        Toast.makeText(context, "Canción eliminada de la playlist", Toast.LENGTH_SHORT).show()
                                    }
                                else null,
                                addToPlaylist = {
                                    songToAdd = song
                                    showAddToPlaylistDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddToPlaylistDialog && songToAdd != null) {
        AddToPlaylistDialog(
            playlists = localPlaylists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                scope.launch {
                    AppDatabase.getInstance(application)
                        .playlistRepository()
                        .addSongToPlaylist(playlist.id, songToAdd!!)
                    Toast.makeText(context, "Añadido a ${playlist.title}", Toast.LENGTH_SHORT).show()
                    showAddToPlaylistDialog = false
                }
            }
        )
    }
}
