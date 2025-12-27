package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import javax.swing.JColorChooser

class JvmColorPicker : ColorPicker {
    override fun pickColor(initialColor: Color, onColorSelected: (Color) -> Unit) {
        val selectedColor = JColorChooser.showDialog(
            null,
            "Select Track Color",
            java.awt.Color(initialColor.toArgb())
        )
        if (selectedColor != null) {
            onColorSelected(Color(selectedColor.rgb))
        }
    }
}

@Composable
actual fun rememberColorPicker(): ColorPicker = remember { JvmColorPicker() }
