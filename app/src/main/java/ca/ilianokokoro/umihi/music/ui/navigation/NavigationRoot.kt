package ca.ilianokokoro.umihi.music.ui.navigation

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.ui.components.BackButton
import ca.ilianokokoro.umihi.music.ui.components.miniplayer.MiniPlayerWrapper
import ca.ilianokokoro.umihi.music.ui.screens.auth.AuthScreen
import ca.ilianokokoro.umihi.music.ui.screens.history.HistoryScreen
import ca.ilianokokoro.umihi.music.ui.screens.home.HomeScreen
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerScreen
import ca.ilianokokoro.umihi.music.ui.screens.playlist.PlaylistScreen
import ca.ilianokokoro.umihi.music.ui.screens.search.SearchScreen
import ca.ilianokokoro.umihi.music.ui.screens.settings.SettingsScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeScreenKey)
    val app = LocalContext.current.applicationContext as Application
    val currentScreen = backStack.last()
    val screenConfig = rememberScreenUiConfig(currentScreen)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),

        topBar = {
            TopAppBar(
                title = {
                    val hasTitle = screenConfig.titleId != 0 || screenConfig.title.isNotBlank()

                    AnimatedVisibility(
                        visible = hasTitle,
                        enter = fadeIn(tween(Constants.Animation.NAVIGATION_DURATION)),
                        exit = fadeOut(tween(Constants.Animation.NAVIGATION_DURATION))
                    ) {
                        when {
                            screenConfig.titleId != 0 ->
                                Text(stringResource(screenConfig.titleId))

                            screenConfig.title.isNotBlank() ->
                                Text(screenConfig.title)
                        }
                    }
                },
                navigationIcon = {
                    if (screenConfig.showBack) {
                        BackButton(onBack = backStack::safePop)
                    }
                }
            )
        },
        bottomBar = {
            Column {
                val miniPlayerModifier = if (screenConfig.showBottomBar) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                }

                MiniPlayerWrapper(
                    showMiniPlayer = screenConfig.showMiniPlayer,
                    onMiniPlayerPressed = { backStack.add(PlayerScreenKey) },
                    onSwipeUp = { backStack.add(PlayerScreenKey) },
                    onSwipeDown = { if (backStack.last() is PlayerScreenKey) backStack.safePop() },
                    modifier = miniPlayerModifier
                )

                AnimatedVisibility(
                    visible = screenConfig.showBottomBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {


                    BottomNavigationBar(
                        currentTab = screenConfig.selectedTab,
                        onTabSelected = { key ->
                            if (backStack.last() != key) backStack.add(key)
                        }
                    )
                }
            }
        }

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.BottomCenter
        ) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                onBack = backStack::safePop,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                                targetOffsetY = { -it }
                            ) + fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)))
                },
                popTransitionSpec = {
                    (slideInVertically(
                        animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                        initialOffsetY = { -it }
                    ) + fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)))
                },
                predictivePopTransitionSpec = {
                    (slideInVertically(
                        animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                        initialOffsetY = { -it }
                    ) + fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)))
                },
                entryProvider = { key ->
                    when (key) {
                        is HomeScreenKey -> NavEntry(key) {
                            HomeScreen(
                                onSettingsButtonPress = { backStack.add(SettingsScreenKey) },
                                onPlaylistPressed = { playlist ->
                                    backStack.add(PlaylistScreenKey(playlistInfo = playlist))
                                },
                                application = app
                            )
                        }

                        is SettingsScreenKey -> NavEntry(key) {
                            SettingsScreen(
                                openAuthScreen = { backStack.add(AuthScreenKey) },
                                application = app
                            )
                        }

                        is PlaylistScreenKey -> NavEntry(key) {
                            PlaylistScreen(
                                playlistInfo = key.playlistInfo,
                                onOpenPlayer = { backStack.add(PlayerScreenKey) },
                                onBack = backStack::safePop,
                                application = app
                            )
                        }

                        is AuthScreenKey -> NavEntry(key) {
                            AuthScreen(
                                onBack = backStack::safePop,
                                application = app
                            )
                        }

                        is PlayerScreenKey -> NavEntry(
                            key,
                            metadata = Constants.Animation.SLIDE_UP_TRANSITION
                        ) {
                            PlayerScreen(
                                onBack = backStack::safePop,
                                application = app
                            )
                        }

                        is SearchScreenKey -> NavEntry(key) {
                            SearchScreen(
                                application = app,
                            )
                        }

                        is HistoryScreenKey -> NavEntry(key) {
                            HistoryScreen(
                                application = app,
                                onSongClick = { _ ->
                                    backStack.add(PlayerScreenKey)
                                }
                            )
                        }

                        else -> throw RuntimeException(
                            app.getString(
                                R.string.invalid_navkey,
                                key.toString()
                            )
                        )
                    }
                }
            )
        }
    }
}


fun NavBackStack<NavKey>.safePop() {
    if (this.size > 1) {
        this.removeLastOrNull()
    } else {
        printe("Backstack Pop was called unsafely")
    }
}
