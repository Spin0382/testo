package ca.ilianokokoro.umihi.music.ui.components.song

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SquareImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QueueSongListItem(
    song: Song,
    isCurrentSong: Boolean,
    onPress: () -> Unit,
    onDelete: () -> Unit,
    scope: CoroutineScope,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val deleteThreshold = -150f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStarted()
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragStopped()
                        scope.launch {
                            if (offsetX < deleteThreshold) {
                                onDelete()
                            }
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-200f, 0f)
                    }
                )
            }
            .offset { offsetX.roundToInt().toIntOffset() }
            .background(
                if (offsetX < deleteThreshold) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = onPress
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SquareImage(
                uri = song.thumbnailPath ?: song.thumbnailHref,
                modifier = Modifier.size(48.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCurrentSong) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (offsetX < deleteThreshold) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun androidx.compose.ui.unit.DpOffset.toIntOffset(): androidx.compose.ui.unit.IntOffset {
    return androidx.compose.ui.unit.IntOffset(x.roundToInt(), y.roundToInt())
}

@Composable
fun Modifier.offset(offset: androidx.compose.ui.unit.IntOffset): Modifier {
    return this.then(Modifier.graphicsLayer {
        translationX = offset.x.toFloat()
    })
}
