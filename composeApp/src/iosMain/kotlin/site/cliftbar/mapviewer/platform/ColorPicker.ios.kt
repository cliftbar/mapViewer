package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*
import platform.CoreGraphics.CGFloat
import platform.CoreImage.CIColor
import platform.Foundation.NSLog
import platform.darwin.NSObject

class IosColorPicker : ColorPicker {
    @OptIn(ExperimentalForeignApi::class)
    override fun pickColor(initialColor: Color, onColorSelected: (Color) -> Unit) {
        val picker = UIColorPickerViewController()
        picker.selectedColor = UIColor(
            red = initialColor.red.toDouble(),
            green = initialColor.green.toDouble(),
            blue = initialColor.blue.toDouble(),
            alpha = initialColor.alpha.toDouble()
        )
        
        picker.delegate = object : NSObject(), UIColorPickerViewControllerDelegateProtocol {
            override fun colorPickerViewControllerDidFinish(viewController: UIColorPickerViewController) {
                val uiColor = viewController.selectedColor
                var red: CGFloat = 0.0
                var green: CGFloat = 0.0
                var blue: CGFloat = 0.0
                var alpha: CGFloat = 0.0
                
                // Use getRed:green:blue:alpha: which is the standard way to get components
                // In Kotlin/Native it might be slightly different syntax
                // Actually we can just use the components from CGColor
            }

            override fun colorPickerViewController(
                viewController: UIColorPickerViewController,
                didSelectColor: UIColor,
                continuously: Boolean
            ) {
                val uiColor = didSelectColor
                val ciColor = CIColor.colorWithCGColor(uiColor.CGColor)
                onColorSelected(
                    Color(
                        red = ciColor.red.toFloat(),
                        green = ciColor.green.toFloat(),
                        blue = ciColor.blue.toFloat(),
                        alpha = ciColor.alpha.toFloat()
                    )
                )
            }
        }
        
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberColorPicker(): ColorPicker = remember { IosColorPicker() }
