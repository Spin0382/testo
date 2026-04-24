package ca.ilianokokoro.umihi.music.ui.components.song

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SquareImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    var deleteThresholdReached by remember { mutableStateOf(false) }
    val deleteThreshold = -150f

    // Provide haptic when threshold first crossed
    LaunchedEffect(offsetX.value) {
        if (offsetX.value < deleteThreshold && !deleteThresholdReached) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            deleteThresholdReached = true
        } else if (offsetX.value >= deleteThreshold && deleteThresholdReached) {
            deleteThresholdReached = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        onDragStarted()
                    },
                    onDragEnd = {
                        onDragStopped()
                        if (offsetX.value < deleteThreshold) {
                            // Execute delete
                            scope.launch {
                                onDelete()
                            }
                        }
                        // Animate back to zero
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // Only move if more horizontal then vertical (handled by detectHorizontalDragGestures)
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount).coerceIn(-200f, 0f)
                            offsetX.snapTo(newValue)
                        }
                    }
                )
            }
            .background(
                if (offsetX.value < deleteThreshold) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(onClick = onPress),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
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
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isCurrentSong) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(song.title, style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal),
                            maxLines = 1, modifier = Modifier.basicMarquee())
                    }
                    Text(song.artist, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Icon(Icons.Outlined.DragHandle, contentDescription = null,
                    modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (offsetX.value < deleteThreshold) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
