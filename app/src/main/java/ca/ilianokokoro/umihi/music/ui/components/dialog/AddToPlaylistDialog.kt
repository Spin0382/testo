package ca.ilianokokoro.umihi.music.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.models.PlaylistInfo

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistInfo>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (PlaylistInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("No tienes playlists locales. Crea una primero.")
            } else {
                LazyColumn {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistSelected(playlist) }
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
