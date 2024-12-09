package molczane.gk.project3.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.toComposeImageBitmap


data class RGBToCMYKState(
    val originalImage: ImageBitmap,//= loadImage("src/images/mountains.png"),
    val processedImage: ImageBitmap? = null,
    val cmykImages: List<ImageBitmap> = emptyList(),
    val bezierCurves: List<BezierCurve> = emptyList(),
    val selectedColor: Color = Color.Cyan,
    val showAllCurves: Boolean = true,
    val showAllPictures: Boolean = false
)

//fun RGBToCMYKState.loadImage(filePath: String): ImageBitmap {
//    val bufferedImage = ImageIO.read(File(filePath))
//    return bufferedImage.toComposeImageBitmap()
//}

fun loadImage(filePath: String): ImageBitmap {
    val bufferedImage = ImageIO.read(File(filePath))
    return bufferedImage.toComposeImageBitmap()
}

