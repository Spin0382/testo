@file:OptIn(ExperimentalMaterial3Api::class)

package ca.ilianokokoro.umihi.music.ui.screens.home

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onSettingsButtonPress: () -> Unit,
    onPlaylistPressed: (playlistInfo: PlaylistInfo) -> Unit,
    application: Application,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(application = application))
) {
    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showAddLinkDialog by remember { mutableStateOf(false) }
    var youtubeLink by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

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
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.screenState) {
                is ScreenState.LoggedIn -> {
                    val playlists = uiState.screenState.playlistInfos
                    if (playlists.isEmpty()) {
                        Text(stringResource(R.string.no_playlists), textAlign = TextAlign.Center)
                    } else {
                        PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = homeViewModel::refreshPlaylists) {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING)
                            ) {
                                itemsIndexed(items = playlists, key = { _, p -> p.id }) { _, playlist ->
                                    PlaylistCard(playlistInfo = playlist, onClicked = { onPlaylistPressed(playlist) })
                                }
                            }
                        }
                    }
                }
                ScreenState.LoggedOut -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.log_in_message), textAlign = TextAlign.Center)
                    FilledTonalButton(onClick = onSettingsButtonPress) { Text(stringResource(R.string.open_settings)) }
                }
                ScreenState.Loading -> LoadingAnimation()
                is ScreenState.Error -> ErrorMessage(ex = uiState.screenState.exception, onRetry = homeViewModel::getPlaylists)
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
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Añadir por link")
        }
    }

    if (showAddLinkDialog) {
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false; addError = null },
            title = { Text("Añadir por link de YouTube") },
            text = {
                Column {
                    OutlinedTextField(
                        value = youtubeLink,
                        onValueChange = { youtubeLink = it },
                        label = { Text("URL de YouTube") },
                        placeholder = { Text("https://youtu.be/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (addError != null) {
                        Text(addError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isAdding) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (youtubeLink.isBlank()) {
                        addError = "Introduce un enlace"
                        return@TextButton
                    }
                    isAdding = true
                    addError = null
                    scope.launch {
                        val result = homeViewModel.extractSongFromLink(youtubeLink)
                        isAdding = false
                        result.onSuccess { song ->
                            PlayerManager.currentController?.addToQueue(song, context)
                            Toast.makeText(context, "Añadido a la cola: ${song.title}", Toast.LENGTH_SHORT).show()
                            showAddLinkDialog = false
                            youtubeLink = ""
                        }.onFailure { e ->
                            addError = e.message ?: "Error desconocido"
                        }
                    }
                }, enabled = !isAdding) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLinkDialog = false; youtubeLink = ""; addError = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
