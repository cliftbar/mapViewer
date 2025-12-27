package site.cliftbar.mapviewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
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
import site.cliftbar.mapviewer.config.AppTheme

@Composable
fun App(
    database: MapViewerDB,
    configRepository: ConfigRepository = remember { ConfigRepository(database) },
    trackRepository: TrackRepository = remember { TrackRepository(database) }
) {
    val config by configRepository.activeConfig.collectAsState()

    val darkTheme = when (config.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    ) {
        TabNavigator(MapScreen(configRepository, trackRepository)) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        TabNavigationItem(MapScreen(configRepository, trackRepository))
                        TabNavigationItem(TrackManagementScreen(trackRepository))
                        TabNavigationItem(SettingsScreen(configRepository))
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current.key == tab.key,
        onClick = { tabNavigator.current = tab },
        icon = { Text(tab.options.title) },
        label = { Text(tab.options.title) }
    )
}