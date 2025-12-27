package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

interface ColorPicker {
    fun pickColor(initialColor: Color, onColorSelected: (Color) -> Unit)
}

@Composable
expect fun rememberColorPicker(): ColorPicker
