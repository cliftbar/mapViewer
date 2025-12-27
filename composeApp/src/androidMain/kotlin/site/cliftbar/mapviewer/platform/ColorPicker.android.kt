package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.graphics.drawable.GradientDrawable

class AndroidColorPicker(private val context: android.content.Context) : ColorPicker {
    override fun pickColor(initialColor: Color, onColorSelected: (Color) -> Unit) {
        // Since Android doesn't have a native color picker, 
        // a simple custom dialog using standard Android views is the closest "native" feel.
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        var currentColor = initialColor.toArgb()

        val preview = TextView(context).apply {
            height = 200
            val shape = GradientDrawable().apply {
                setColor(currentColor)
                cornerRadius = 20f
            }
            background = shape
        }
        layout.addView(preview)

        fun createSlider(label: String, initialValue: Int, onValueChange: (Int) -> Unit): SeekBar {
            val textView = TextView(context).apply { text = label }
            layout.addView(textView)
            val seekBar = SeekBar(context).apply {
                max = 255
                progress = initialValue
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        onValueChange(progress)
                        val shape = preview.background as GradientDrawable
                        shape.setColor(currentColor)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            layout.addView(seekBar)
            return seekBar
        }

        val r = createSlider("Red", (initialColor.red * 255).toInt()) { 
            currentColor = (currentColor and 0xFF00FFFF.toInt()) or (it shl 16)
        }
        val g = createSlider("Green", (initialColor.green * 255).toInt()) {
            currentColor = (currentColor and 0xFFFF00FF.toInt()) or (it shl 8)
        }
        val b = createSlider("Blue", (initialColor.blue * 255).toInt()) {
            currentColor = (currentColor and 0xFFFFFF00.toInt()) or it
        }

        AlertDialog.Builder(context)
            .setTitle("Select Color")
            .setView(layout)
            .setPositiveButton("Select") { _, _ ->
                onColorSelected(Color(currentColor))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

@Composable
actual fun rememberColorPicker(): ColorPicker {
    val context = LocalContext.current
    return remember(context) { AndroidColorPicker(context) }
}
