@file:OptIn(ExperimentalMaterial3Api::class)

package ca.ilianokokoro.umihi.music.ui.screens.home

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.ComposeHelper
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.addToQueue
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.ui.components.ErrorMessage
import ca.ilianokokoro.umihi.music.ui.components.LoadingAnimation
import ca.ilianokokoro.umihi.music.ui.components.playlist.PlaylistCard

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onSettingsButtonPress: () -> Unit,
    onPlaylistPressed: (playlistInfo: PlaylistInfo) -> Unit,
    application: Application,
    homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(application = application)
    )
) {
    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showAddLinkDialog by remember { mutableStateOf(false) }
    var youtubeLink by remember { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val loggedOut = uiState.screenState is ScreenState.LoggedOut
            val noPlaylistsFound =
                uiState.screenState is ScreenState.LoggedIn && uiState.screenState.playlistInfos.isEmpty()

            if (event == Lifecycle.Event.ON_RESUME && (loggedOut || noPlaylistsFound)) {
                homeViewModel.getPlaylists()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.screenState) {
                is ScreenState.LoggedIn -> {
                    val playlists = uiState.screenState.playlistInfos

                    if (playlists.isEmpty()) {
                        Text(
                            stringResource(R.string.no_playlists),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = homeViewModel::refreshPlaylists
                        ) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING)
                            ) {
                                itemsIndexed(
                                    items = playlists,
                                    key = { index, playlist ->
                                        ComposeHelper.getLazyKey(playlist, playlist.id, index)
                                    }
                                ) { _, playlist ->
                                    PlaylistCard(
                                        playlistInfo = playlist,
                                        onClicked = { onPlaylistPressed(playlist) }
                                    )
                                }
                            }
                        }
                    }
                }

                ScreenState.LoggedOut -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.log_in_message),
                        textAlign = TextAlign.Center
                    )
                    FilledTonalButton(
                        onClick = onSettingsButtonPress,
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.open_settings))
                    }
                }

                ScreenState.Loading -> LoadingAnimation()
                is ScreenState.Error -> ErrorMessage(
                    ex = uiState.screenState.exception,
                    onRetry = homeViewModel::getPlaylists
                )
            }
        }

        FloatingActionButton(
            onClick = {
                clipboardManager.getText()?.let { clipText ->
                    if (clipText.text.contains("youtube.com") || clipText.text.contains("youtu.be")) {
                        youtubeLink = clipText.text
                    }
                }
                showAddLinkDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Añadir por link")
        }
    }

    if (showAddLinkDialog) {
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            title = { Text("Añadir por link de YouTube") },
            text = {
                OutlinedTextField(
                    value = youtubeLink,
                    onValueChange = { youtubeLink = it },
                    label = { Text("URL de YouTube") },
                    placeholder = { Text("https://youtu.be/...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            homeViewModel.addFromLink(youtubeLink) { song ->
                                PlayerManager.currentController?.addToQueue(song, context)
                                Toast.makeText(context, "Añadido a la cola: ${song.title}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showAddLinkDialog = false
                        youtubeLink = ""
                    }
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddLinkDialog = false
                        youtubeLink = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
