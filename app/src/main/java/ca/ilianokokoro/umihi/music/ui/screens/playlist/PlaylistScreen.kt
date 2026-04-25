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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    application: Application,
    playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.Factory(playlistInfo = playlistInfo, application = application)
    )
) {
    val uiState = playlistViewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    val localPlaylists = remember { mutableStateListOf<PlaylistInfo>() }

    // Refrescar al reanudar
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                playlistViewModel.refreshAfterAdd()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Cerrar pantalla si la playlist fue eliminada
    LaunchedEffect(uiState.screenState) {
        if (uiState.screenState is ScreenState.Error && (uiState.screenState.exception.message == "deleted")) {
            onBack()
        }
    }

    LaunchedEffect(playlistInfo.id) {
        if (playlistInfo.id.startsWith("local_")) {
            localPlaylists.clear()
            localPlaylists.addAll(
                AppDatabase.getInstance(application)
                    .playlistRepository()
                    .getLocalPlaylists()
                    .filter { it.id != playlistInfo.id }
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.screenState is ScreenState.Error && (uiState.screenState.exception.message != "deleted")) {
            ErrorMessage(ex = uiState.screenState.exception, onRetry = playlistViewModel::getPlaylistInfo)
        } else {
            val playlist: Playlist = when (uiState.screenState) {
                is ScreenState.Loading -> Playlist(uiState.screenState.playlistInfo)
                is ScreenState.Success -> uiState.screenState.playlist
                else -> Playlist(playlistInfo)
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
                    playlist = playlist,
                    isLocalPlaylist = playlist.info.id.startsWith("local_")
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
                                playlist = playlist,
                                isLocalPlaylist = playlist.info.id.startsWith("local_")
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
                                delete = if (!playlist.info.id.startsWith("local_")) { { playlistViewModel.deleteSong(song) } } else null,
                                deleteFromHistory = null,
                                removeFromPlaylist = if (playlist.info.id.startsWith("local_")) { { playlistViewModel.removeSongFromPlaylist(song) } } else null,
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
