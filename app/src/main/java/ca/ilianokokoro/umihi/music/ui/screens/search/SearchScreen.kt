@file:OptIn(ExperimentalMaterial3Api::class)

package ca.ilianokokoro.umihi.music.ui.screens.search

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.extensions.addNext
import ca.ilianokokoro.umihi.music.extensions.addToQueue
import ca.ilianokokoro.umihi.music.extensions.playSong
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.ErrorMessage
import ca.ilianokokoro.umihi.music.ui.components.LoadingAnimation
import ca.ilianokokoro.umihi.music.ui.components.dialog.AddToPlaylistDialog
import ca.ilianokokoro.umihi.music.ui.components.song.SongListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    application: Application,
    searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(application = application)
    )
) {
    val uiState = searchViewModel.uiState.collectAsStateWithLifecycle().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    val localPlaylists = remember { mutableStateListOf<PlaylistInfo>() }

    LaunchedEffect(Unit) {
        if (uiState.search.isBlank()) {
            focusRequester.requestFocus()
        }
        localPlaylists.clear()
        localPlaylists.addAll(AppDatabase.getInstance(application).playlistRepository().getLocalPlaylists())
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (uiState.screenState is ScreenState.Error) {
            ErrorMessage(
                ex = uiState.screenState.exception,
                onRetry = searchViewModel::search
            )
        } else {
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                value = uiState.search,
                onValueChange = { searchViewModel.onSearchFieldChange(it) },
                label = { Text(stringResource(R.string.search)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        searchViewModel.search()
                    }
                ),
                maxLines = 1
            )

            when (uiState.screenState) {
                ScreenState.Loading -> LoadingAnimation()
                is ScreenState.Success -> {
                    val songs = uiState.screenState.results
                    if (songs.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = songs,
                                key = { song -> song.uid }
                            ) { song ->
                                SongListItem(
                                    song = song,
                                    onPress = { PlayerManager.currentController?.playSong(song) },
                                    playNext = { PlayerManager.currentController?.addNext(song, context) },
                                    addToQueue = { PlayerManager.currentController?.addToQueue(song, context) },
                                    download = { searchViewModel.downloadSong(song) },
                                    delete = { searchViewModel.deleteSong(song) },
                                    deleteCache = { searchViewModel.deleteCache(song) },
                                    addToPlaylist = {
                                        songToAdd = song
                                        showAddToPlaylistDialog = true
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(stringResource(R.string.no_results))
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
