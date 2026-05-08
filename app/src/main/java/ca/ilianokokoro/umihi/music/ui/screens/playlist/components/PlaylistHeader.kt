package ca.ilianokokoro.umihi.music.ui.screens.playlist.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.models.Playlist
import ca.ilianokokoro.umihi.music.ui.components.playlist.PlaylistInfo

@Composable
fun PlaylistHeader(
    modifier: Modifier = Modifier,
    isDownloading: Boolean,
    onOpenPlayer: () -> Unit,
    onDownloadPlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onCancelDownload: () -> Unit,
    onShufflePlaylist: () -> Unit,
    playlist: Playlist,
    isLocalPlaylist: Boolean = false
) {
    val player = PlayerManager.currentController
    val isShuffleOn = player?.shuffleModeEnabled ?: false

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            PlaylistInfo(
                playlist = playlist,
                isDownloading = isDownloading,
                onDownloadPressed = onDownloadPlaylist,
                onDeletePressed = onDeletePlaylist,
                onCancelDownload = onCancelDownload,
                isLocal = isLocalPlaylist
            )
            ActionButtons(
                buttonEnabled = !playlist.songs.isEmpty(),
                onPlayClicked = {
                    onOpenPlayer()
                    onPlayPlaylist()
                },
                onShuffleClicked = {
                    onOpenPlayer()
                    onShufflePlaylist()
                },
                isShuffleOn = isShuffleOn
            )
        }
    }
}
