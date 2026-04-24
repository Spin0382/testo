package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.getQueue
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.song.QueueSongListItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    changeVisibility: (visible: Boolean) -> Unit,
    currentSong: Song,
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val player = PlayerManager.currentController ?: return

    val queue by remember(player) {
        derivedStateOf {
            val q: List<Song> = player.getQueue()
            q.ifEmpty { songs }
        }
    }

    var startIndex by remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LaunchedEffect(queue) {
        if (queue.isNotEmpty()) {
            val idx = queue.indexOfFirst { it.youtubeId == currentSong.youtubeId }
            if (idx >= 0) lazyListState.animateScrollToItem(idx)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { changeVisibility(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                text = stringResource(R.string.playing_now),
                style = MaterialTheme.typography.titleLarge
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (queue.isEmpty()) {
                    item { Text(stringResource(R.string.queue_empty), textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize()) }
                } else {
                    itemsIndexed(items = queue, key = { _, song -> song.uid }) { index, song ->
                        ReorderableItem(reorderableLazyListState, key = song.uid) { _ ->
                            QueueSongListItem(
                                song = song,
                                isCurrentSong = currentSong == song,
                                onPress = { player.seekTo(index, C.TIME_UNSET) },
                                onDelete = { player.removeMediaItem(index) },
                                scope = rememberCoroutineScope(),
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    startIndex = index
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    player.moveMediaItem(startIndex, index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
