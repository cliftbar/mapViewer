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
import site.cliftbar.mapviewer.ui.screens.MapScreen
import site.cliftbar.mapviewer.ui.screens.SettingsScreen
import site.cliftbar.mapviewer.ui.screens.TrackManagementScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        TabNavigator(MapScreen()) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        TabNavigationItem(MapScreen())
                        TabNavigationItem(TrackManagementScreen())
                        TabNavigationItem(SettingsScreen())
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