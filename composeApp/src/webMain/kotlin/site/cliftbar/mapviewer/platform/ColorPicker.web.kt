package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

class WebColorPicker : ColorPicker {
    override fun pickColor(initialColor: Color, onColorSelected: (Color) -> Unit) {
        // Web can use <input type="color">
    }
}

@Composable
actual fun rememberColorPicker(): ColorPicker = remember { WebColorPicker() }
