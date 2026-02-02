package site.cliftbar.mapviewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.ui.screens.MapScreen
import site.cliftbar.mapviewer.ui.screens.SettingsScreen
import site.cliftbar.mapviewer.ui.screens.TrackManagementScreen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.saveable.rememberSaveable
import site.cliftbar.mapviewer.config.AppTheme

@Composable
fun App(
    database: MapViewerDB,
    configRepository: ConfigRepository,
    trackRepository: TrackRepository,
    tileRepository: site.cliftbar.mapviewer.map.TileRepository
) {
    val config by configRepository.activeConfig.collectAsState()

    LaunchedEffect(configRepository) {
        configRepository.initialize()
    }

    val darkTheme = when (config.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    CompositionLocalProvider(
        LocalConfigRepository provides configRepository,
        LocalTrackRepository provides trackRepository,
        LocalTileRepository provides tileRepository
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
        ) {
            val bottomPanelContent = remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
            var bottomPanelCollapsed by rememberSaveable { mutableStateOf(false) }
            CompositionLocalProvider(LocalBottomPanelContent provides bottomPanelContent) {
                TabNavigator(MapScreen()) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                CurrentTab()
                            }
                            BottomTabPanel(
                                content = bottomPanelContent.value,
                                collapsed = bottomPanelCollapsed,
                                onToggleCollapsed = { bottomPanelCollapsed = !bottomPanelCollapsed }
                            )
                        }
                    }
                }
            }
        }
    }
}

val LocalConfigRepository = staticCompositionLocalOf<ConfigRepository> {
    error("No ConfigRepository provided")
}

val LocalTrackRepository = staticCompositionLocalOf<TrackRepository> {
    error("No TrackRepository provided")
}

val LocalTileRepository = staticCompositionLocalOf<site.cliftbar.mapviewer.map.TileRepository> {
    error("No TileRepository provided")
}

val LocalBottomPanelContent = staticCompositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    error("No BottomPanelContent provided")
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current.key == tab.key,
        onClick = { tabNavigator.current = tab },
        icon = { Text(tab.options.title) },
        label = null,
        alwaysShowLabel = false
    )
}

@Composable
private fun BottomTabPanel(
    content: (@Composable () -> Unit)?,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 4.dp
        ) {
            Column {
                if (content != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                    ) {
                        IconButton(
                            onClick = onToggleCollapsed,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = if (collapsed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (collapsed) "Expand panel" else "Collapse panel"
                            )
                        }
                    }
                    if (!collapsed) {
                        Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                            content()
                        }
                    }
                }
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0)
                ) {
                    TabNavigationItem(MapScreen())
                    TabNavigationItem(TrackManagementScreen())
                    TabNavigationItem(SettingsScreen())
                }
            }
        }
    }
}
