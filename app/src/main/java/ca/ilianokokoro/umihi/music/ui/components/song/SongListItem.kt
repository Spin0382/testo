package ca.ilianokokoro.umihi.music.ui.components.song

import android.content.Context
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SquareImage
import ca.ilianokokoro.umihi.music.ui.components.dropdown.ModernDropdownItem
import java.io.File

@Composable
fun SongListItem(
    song: Song,
    onPress: () -> Unit,
    playNext: () -> Unit,
    addToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    download: (() -> Unit)? = null,
    delete: (() -> Unit)? = null,
    deleteCache: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val isCached = remember(song) {
        val audioDir = UmihiHelper.getDownloadDirectory(context, Constants.Downloads.AUDIO_FILES_FOLDER)
        val cachedFile = File(audioDir, context.getString(R.string.webm_extension, song.youtubeId))
        cachedFile.exists() && !song.downloaded
    }

    Box {
        ListItem(
            leadingContent = {
                Box(
                    modifier = modifier
                        .size(60.dp)
                        .aspectRatio(1f)
                ) {
                    SquareImage(
                        song.thumbnailPath ?: song.thumbnailHref,
                        modifier = modifier.matchParentSize()
                    )
                }
            },
            headlineContent = { Text(song.title, modifier = modifier.basicMarquee()) },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier.fillMaxHeight()
                ) {
                    if (song.downloaded || isCached) {
                        Icon(
                            modifier = modifier
                                .padding(end = 3.dp)
                                .size(16.dp),
                            imageVector = Icons.Rounded.DownloadForOffline,
                            contentDescription = stringResource(R.string.download),
                        )
                    }
                    Text(
                        "${song.artist} ${stringResource(R.string.dot)} ${song.duration}",
                        modifier = modifier.basicMarquee()
                    )
                }
            },
            trailingContent = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        ModernDropdownItem(
                            leadingIcon = Icons.Rounded.PlayCircleOutline,
                            text = stringResource(R.string.play_next),
                            onClick = {
                                playNext()
                                expanded = false
                            }
                        )
                        ModernDropdownItem(
                            leadingIcon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            text = stringResource(R.string.add_to_queue),
                            onClick = {
                                addToQueue()
                                expanded = false
                            }
                        )
                        if (download != null && !song.downloaded) {
                            ModernDropdownItem(
                                leadingIcon = Icons.Rounded.Download,
                                text = stringResource(R.string.download),
                                onClick = {
                                    download()
                                    expanded = false
                                }
                            )
                        }
                        if (delete != null && song.downloaded) {
                            ModernDropdownItem(
                                leadingIcon = Icons.Rounded.Delete,
                                text = stringResource(R.string.delete_download),
                                onClick = {
                                    delete()
                                    expanded = false
                                }
                            )
                        }
                        if (deleteCache != null && isCached) {
                            ModernDropdownItem(
                                leadingIcon = Icons.Outlined.Delete,
                                text = stringResource(R.string.delete_cache),
                                onClick = {
                                    deleteCache()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            },
            modifier = modifier.combinedClickable(
                onClick = onPress,
                onLongClick = { expanded = true }
            )
        )
    }
}
