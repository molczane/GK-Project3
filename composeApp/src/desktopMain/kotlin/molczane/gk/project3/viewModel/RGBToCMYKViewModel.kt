package molczane.gk.project3.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import molczane.gk.project3.model.RGBToCMYKState
import molczane.gk.project3.model.BezierCurve
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import molczane.gk.project3.model.CMYK

class RGBToCMYKViewModel : ViewModel() {
    private val _state = MutableStateFlow(RGBToCMYKState())
    val state: StateFlow<RGBToCMYKState> get() = _state

    private val bezierCurveStorage = mutableMapOf<Color, BezierCurve>()

    init {
        // Inicjalizacja przykładowych krzywych Béziera dla każdego koloru
        _state.value = _state.value.copy(
            bezierCurves = initializeDefaultCurves()
        )
        _state.value = _state.value.copy(
            cmykImages = convertRGBToCMYK(_state.value.originalImage, _state.value.bezierCurves)
        )
    }

    fun selectColor(color: Color) {
        _state.value = _state.value.copy(selectedColor = color)
    }

    fun showAllCurves() {
        _state.value = _state.value.copy(showAllCurves = !_state.value.showAllCurves)
    }

    fun convertToBlackAndWhite() {
        // Przetwarzanie obrazu na czarno-biały
        val grayscaleImage = processToGrayscale(_state.value.originalImage)
        _state.value = _state.value.copy(processedImage = grayscaleImage)
    }

    fun saveCurves() {
        // Zapis krzywych do mapy (symulacja zapisu do pliku)
        bezierCurveStorage[_state.value.selectedColor] = getCurrentBezierCurve()
    }

    fun loadCurves() {
        // Załaduj krzywe z mapy, jeśli istnieją
        val loadedCurve = bezierCurveStorage[_state.value.selectedColor]
        if (loadedCurve != null) {
            updateCurveForSelectedChannel(loadedCurve)
        }
    }

    fun updateControlPoint(curve: BezierCurve, pointIndex: Int, newOffset: Offset) {
        val updatedCurves = _state.value.bezierCurves.map { currentCurve ->
            if (currentCurve == curve) {
                currentCurve.copy(
                    controlPoints = currentCurve.controlPoints.toMutableList().apply {
                        this[pointIndex] = newOffset
                    }
                )
            } else {
                currentCurve
            }
        }

        _state.value = _state.value.copy(bezierCurves = updatedCurves)
    }


    private fun processToGrayscale(image: ImageBitmap): ImageBitmap {
        // Logika konwersji obrazu na czarno-biały
        return image // Na razie zwraca oryginalny obraz
    }

    private fun getCurrentBezierCurve(): BezierCurve {
        // Pobierz aktualnie edytowaną krzywą
        return _state.value.bezierCurves[colorToIndex(_state.value.selectedColor)]
    }

    private fun updateCurveForSelectedChannel(newCurve: BezierCurve) {
        // Zaktualizuj krzywą dla wybranego koloru
        val updatedCurves = _state.value.bezierCurves.toMutableList()
        updatedCurves[colorToIndex(_state.value.selectedColor)] = newCurve
        _state.value = _state.value.copy(bezierCurves = updatedCurves)
    }

    private fun initializeDefaultCurves(): List<BezierCurve> {
        // Tworzenie różnych krzywych Béziera dla kolorów Cyan, Magenta, Yellow, Black
        return listOf(
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),       // Start point
                    Offset(200f, 300f),   // Control point 1
                    Offset(400f, 500f),   // Control point 2
                    Offset(600f, 600f)    // End point
                ),
                color = Color.Cyan
            ),
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),       // Start point
                    Offset(150f, 250f),   // Control point 1
                    Offset(450f, 400f),   // Control point 2
                    Offset(600f, 600f)    // End point
                ),
                color = Color.Magenta
            ),
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),       // Start point
                    Offset(100f, 200f),   // Control point 1
                    Offset(500f, 450f),   // Control point 2
                    Offset(600f, 600f)    // End point
                ),
                color = Color.Yellow
            ),
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),       // Start point
                    Offset(50f, 50f),    // Control point 1
                    Offset(550f, 50f),   // Control point 2
                    Offset(600f, 600f)      // End point
                ),
                color = Color.Black
            )
        )
    }

    private fun colorToIndex(color: Color): Int {
        // Zamiana koloru na indeks w liście krzywych
        return listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Black).indexOf(color)
    }

    fun getBezierCurveByColor(color: Color): BezierCurve? {
        return _state.value.bezierCurves.find { it.color == color }
    }


    fun rgbToCmyk(r: Float, g: Float, b: Float): CMYK {
        // Normalize RGB values to the range [0, 1]
        val rPrime = r
        val gPrime = g
        val bPrime = b

        // Calculate Black (K) component
        val k = 1 - maxOf(rPrime, gPrime, bPrime)

        // Prevent division by zero when K is 1
        val denominator = 1 - k
        val cyan = if (denominator == 0f) 0f else (1 - rPrime - k) / denominator
        val magenta = if (denominator == 0f) 0f else (1 - gPrime - k) / denominator
        val yellow = if (denominator == 0f) 0f else (1 - bPrime - k) / denominator

        return CMYK(
            cyan = cyan,
            magenta = magenta,
            yellow = yellow,
            black = k
        )
    }


    fun cmykToRgb(c: Float, m: Float, y: Float, k: Float): RGB {
        // Ensure input values are in the range [0, 1]
        val cyan = c.coerceIn(0f, 1f)
        val magenta = m.coerceIn(0f, 1f)
        val yellow = y.coerceIn(0f, 1f)
        val black = k.coerceIn(0f, 1f)

        // Calculate RGB values
        val r = 255 * (1 - cyan) * (1 - black)
        val g = 255 * (1 - magenta) * (1 - black)
        val b = 255 * (1 - yellow) * (1 - black)

        // Return the result as an RGB object
        return RGB(
            red = r.toInt(),
            green = g.toInt(),
            blue = b.toInt()
        )
    }

    // Data class for RGB representation
    data class RGB(
        val red: Int,
        val green: Int,
        val blue: Int
    )

    fun updateCImage() {
        val cyanImage = convertRGBToCMYKCyan(_state.value.originalImage, _state.value.bezierCurves)
        val updatedImages = _state.value.cmykImages.toMutableList()
        if (updatedImages.isNotEmpty()) {
            updatedImages[0] = cyanImage // Zastąpienie obrazu Cyan
        } else {
            updatedImages.add(cyanImage) // Dodanie obrazu Cyan, jeśli lista jest pusta
        }
        _state.value = _state.value.copy(cmykImages = updatedImages)
    }

    fun updateMImage() {
        val magentaImage = convertRGBToCMYKMagenta(_state.value.originalImage, _state.value.bezierCurves)
        val updatedImages = _state.value.cmykImages.toMutableList()
        if (updatedImages.isNotEmpty()) {
            updatedImages[1] = magentaImage // Zastąpienie obrazu Cyan
        } else {
            updatedImages.add(magentaImage) // Dodanie obrazu Cyan, jeśli lista jest pusta
        }
        _state.value = _state.value.copy(cmykImages = updatedImages)
    }

    fun updateYImage() {
        val yellowImage = convertRGBToCMYKYellow(_state.value.originalImage, _state.value.bezierCurves)
        val updatedImages = _state.value.cmykImages.toMutableList()
        if (updatedImages.isNotEmpty()) {
            updatedImages[2] = yellowImage // Zastąpienie obrazu Cyan
        } else {
            updatedImages.add(yellowImage) // Dodanie obrazu Cyan, jeśli lista jest pusta
        }
        _state.value = _state.value.copy(cmykImages = updatedImages)
    }

    fun updateKImage() {
        val blackImage = convertRGBToCMYKBlack(_state.value.originalImage, _state.value.bezierCurves)
        val updatedImages = _state.value.cmykImages.toMutableList()
        if (updatedImages.isNotEmpty()) {
            updatedImages[3] = blackImage // Zastąpienie obrazu Cyan
        } else {
            updatedImages.add(blackImage) // Dodanie obrazu Cyan, jeśli lista jest pusta
        }
        _state.value = _state.value.copy(cmykImages = updatedImages)
    }

    fun updateCMYKimages() {
        _state.value = _state.value.copy(
            cmykImages = convertRGBToCMYK(_state.value.originalImage, _state.value.bezierCurves)
        )
    }

    fun convertRGBToCMYK(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve>
    ): List<ImageBitmap> {
        val width = image.width
        val height = image.height

        // Create ImageBitmaps for each CMYK channel
        val cyanBitmap = ImageBitmap(width, height)
        val magentaBitmap = ImageBitmap(width, height)
        val yellowBitmap = ImageBitmap(width, height)
        val blackBitmap = ImageBitmap(width, height)

        val pixelMap = image.toPixelMap()
        val paint = Paint()

        // Create Canvas for each channel
        val cyanCanvas = Canvas(cyanBitmap)
        val magentaCanvas = Canvas(magentaBitmap)
        val yellowCanvas = Canvas(yellowBitmap)
        val blackCanvas = Canvas(blackBitmap)

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelMap[x, y]

                // Extract RGB values
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                // Convert RGB -> CMYK
                val cmyk = rgbToCmyk(r, g, b)

                // Adjust CMYK values using Bézier curves
//                val cyan = evaluateBezier(bezierCurves[0], cmyk.cyan)
//                val magenta = evaluateBezier(bezierCurves[1], cmyk.magenta)
//                val yellow = evaluateBezier(bezierCurves[2], cmyk.yellow)
//                val black = evaluateBezier(bezierCurves[3], cmyk.black)

                val cmykTransformed = transformCMYK(cmyk.cyan, cmyk.magenta, cmyk.yellow, bezierCurves[0], bezierCurves[1], bezierCurves[2], bezierCurves[3])
                val cyan = cmykTransformed.cyan
                val magenta = cmykTransformed.magenta
                val yellow = cmykTransformed.yellow
                val black = cmykTransformed.black

                // Convert adjusted CMYK values back to RGB for visualization
                val cyanColor = cmykToRgb(cyan, 0f, 0f, 0f)
                val magentaColor = cmykToRgb(0f, magenta, 0f, 0f)
                val yellowColor = cmykToRgb(0f, 0f, yellow, 0f)
                val blackColor = cmykToRgb(0f, 0f, 0f, black)

                // Draw pixels on respective canvases
                paint.color = Color(cyanColor.red, cyanColor.green, cyanColor.blue)
                cyanCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)

                paint.color = Color(magentaColor.red, magentaColor.green, magentaColor.blue)
                magentaCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)

                paint.color = Color(yellowColor.red, yellowColor.green, yellowColor.blue)
                yellowCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)

                paint.color = Color(blackColor.red, blackColor.green, blackColor.blue)
                blackCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        return listOf(cyanBitmap, magentaBitmap, yellowBitmap, blackBitmap)
    }

    fun convertRGBToCMYKMagenta(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve>
    ): ImageBitmap {
        val width = image.width
        val height = image.height

        // Create ImageBitmaps for each CMYK channel
        val magentaBitmap = ImageBitmap(width, height)

        val pixelMap = image.toPixelMap()
        val paint = Paint()

        // Create Canvas for each channel
        val magentaCanvas = Canvas(magentaBitmap)

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelMap[x, y]

                // Extract RGB values
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                // Convert RGB -> CMYK
                val cmyk = rgbToCmyk(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmyk.cyan,
                    cmyk.magenta,
                    cmyk.yellow,
                    bezierCurves[0],
                    bezierCurves[1],
                    bezierCurves[2],
                    bezierCurves[3]
                )

                // Adjust CMYK values using Bézier curves
                //val magenta = evaluateBezier(bezierCurves[1], cmyk.magenta)
                val magenta = cmykTransformed.magenta

                // Convert adjusted CMYK values back to RGB for visualization
                val magentaColor = cmykToRgb(0f, magenta, 0f, 0f)
                //val magentaColor = cmykTransformed.magenta

                // Draw pixels on respective canvases
                paint.color = Color(magentaColor.red, magentaColor.green, magentaColor.blue)
                magentaCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        return magentaBitmap
    }

    fun convertRGBToCMYKYellow(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve>
    ): ImageBitmap {
        val width = image.width
        val height = image.height

        // Create ImageBitmaps for each CMYK channel
        val yellowBitmap = ImageBitmap(width, height)

        val pixelMap = image.toPixelMap()
        val paint = Paint()

        // Create Canvas for each channel
        val yellowCanvas = Canvas(yellowBitmap)

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelMap[x, y]

                // Extract RGB values
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                // Convert RGB -> CMYK
                val cmyk = rgbToCmyk(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmyk.cyan,
                    cmyk.magenta,
                    cmyk.yellow,
                    bezierCurves[0],
                    bezierCurves[1],
                    bezierCurves[2],
                    bezierCurves[3]
                )

                // Adjust CMYK values using Bézier curves
                //val yellow = evaluateBezier(bezierCurves[2], cmyk.yellow)
                val yellow = cmykTransformed.yellow

                // Convert adjusted CMYK values back to RGB for visualization
                val yellowColor = cmykToRgb(0f, 0f, yellow, 0f)

                // Draw pixels on respective canvases
                paint.color = Color(yellowColor.red, yellowColor.green, yellowColor.blue)
                yellowCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        return yellowBitmap
    }

    fun convertRGBToCMYKBlack(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve>
    ): ImageBitmap {
        val width = image.width
        val height = image.height

        // Create ImageBitmaps for each CMYK channel
        val blackBitmap = ImageBitmap(width, height)

        val pixelMap = image.toPixelMap()
        val paint = Paint()

        // Create Canvas for each channel
        val blackCanvas = Canvas(blackBitmap)

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelMap[x, y]

                // Extract RGB values
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                // Convert RGB -> CMYK
                val cmyk = rgbToCmyk(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmyk.cyan,
                    cmyk.magenta,
                    cmyk.yellow,
                    bezierCurves[0],
                    bezierCurves[1],
                    bezierCurves[2],
                    bezierCurves[3]
                )

                // Adjust CMYK values using Bézier curves
                //val black = evaluateBezier(bezierCurves[3], cmyk.black)
                val black = cmykTransformed.black

                // Convert adjusted CMYK values back to RGB for visualization
                val blackColor = cmykToRgb(0f, 0f, 0f, black)

                // Draw pixels on respective canvases
                paint.color = Color(blackColor.red, blackColor.green, blackColor.blue)
                blackCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        return blackBitmap
    }

//    fun convertRGBToCMYKCyan(
//        image: ImageBitmap,
//        bezierCurves: List<BezierCurve>
//    ): ImageBitmap {
//        val width = image.width
//        val height = image.height
//
//        // Create ImageBitmaps for each CMYK channel
//        val cyanBitmap = ImageBitmap(width, height)
//
//        val pixelMap = image.toPixelMap()
//        val paint = Paint()
//
//        // Create Canvas for each channel
//        val cyanCanvas = Canvas(cyanBitmap)
//
//        // Iterate through all pixels
//        for (x in 0 until width) {
//            for (y in 0 until height) {
//                val pixelColor = pixelMap[x, y]
//
//                // Extract RGB values
//                val r = pixelColor.red
//                val g = pixelColor.green
//                val b = pixelColor.blue
//
//                // Convert RGB -> CMYK
//                val cmyk = rgbToCmyk(r, g, b)
//
//                // Adjust CMYK values using Bézier curves
//                val cyan = evaluateBezier(bezierCurves[0], cmyk.cyan)
//
//                // Convert adjusted CMYK values back to RGB for visualization
//                val cyanColor = cmykToRgb(cyan, 0f, 0f, 0f)
//
//                // Draw pixels on respective canvases
//                paint.color = Color(cyanColor.red, cyanColor.green, cyanColor.blue)
//                cyanCanvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
//            }
//        }
//
//        return cyanBitmap
//    }

    // function that sums CMYK images to one image
    fun updateProcessedImageFromCMYKImages() {
        val image = _state.value.originalImage
        val width = image.width
        val height = image.height

        val cyanImage = _state.value.cmykImages[0]
        val magentaImage = _state.value.cmykImages[1]
        val yellowImage = _state.value.cmykImages[2]
        val blackImage = _state.value.cmykImages[3]

        val imageBitmap = ImageBitmap(width, height)

        val canvas = Canvas(imageBitmap)
        val paint = Paint()

        val cyanPixelMap = cyanImage.toPixelMap()
        val magentaPixelMap = magentaImage.toPixelMap()
        val yellowPixelMap = yellowImage.toPixelMap()
        val blackPixelMap = blackImage.toPixelMap()

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val cyanPixelColor = cyanPixelMap[x, y]
                val magentaPixelColor = magentaPixelMap[x, y]
                val yellowPixelColor = yellowPixelMap[x, y]
                val blackPixelColor = blackPixelMap[x, y]

                // Extract RGB values
                val r = (cyanPixelColor.red + magentaPixelColor.red + yellowPixelColor.red + blackPixelColor.red) / 4f
                val g = (cyanPixelColor.green + magentaPixelColor.green + yellowPixelColor.green + blackPixelColor.green) / 4f
                val b = (cyanPixelColor.blue + magentaPixelColor.blue + yellowPixelColor.blue + blackPixelColor.blue) / 4f

                // Convert RGB -> CMYK
                val cmyk = rgbToCmyk(r, g, b)

                // Convert adjusted CMYK values back to RGB for visualization
                val color = cmykToRgb(cmyk.cyan, cmyk.magenta, cmyk.yellow, cmyk.black)

                // Draw pixels on respective canvases
                paint.color = Color(color.red, color.green, color.blue)
                paint.alpha = 1f

                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
                //canvas.drawRect(color = Color(color.red, color.green, color.blue), topLeft = Offset(x.toFloat(), y.toFloat()), size = Size(1f, 1f))
            }
        }

        _state.value = _state.value.copy(processedImage = imageBitmap)
    }

    fun convertRGBToCMYKCyan(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve>
    ): ImageBitmap {
        val width = image.width
        val height = image.height

        // Prepare an empty ImageBitmap for Cyan output
        val cyanBitmap = ImageBitmap(width, height)
        val pixelMap = image.toPixelMap()

        // Define the number of parallel tasks
        val numTasks = Runtime.getRuntime().availableProcessors()
        val chunkSize = height / numTasks

        val canvas = Canvas(cyanBitmap)
        val paint = Paint()

        runBlocking {
            (0 until numTasks).map { taskIndex ->
                async {
                    val startY = taskIndex * chunkSize
                    val endY = if (taskIndex == numTasks - 1) height else (startY + chunkSize)

                    for (y in startY until endY) {
                        for (x in 0 until width) {
                            val pixelColor = pixelMap[x, y]

                            // Extract RGB values
                            val r = pixelColor.red
                            val g = pixelColor.green
                            val b = pixelColor.blue

                            // Convert RGB -> CMYK
                            val k = 1 - maxOf(r, g, b)
                            val denominator = 1 - k
                            val cyan = if (denominator == 0f) 0f else (1 - r - k) / denominator

                            // Adjust Cyan value using Bézier curve
                            val adjustedCyan = evaluateBezier(bezierCurves[0], cyan)

                            // Convert adjusted Cyan to RGB for visualization
                            val cyanColor = cmykToRgb(adjustedCyan, 0f, 0f, 0f)

                            // Draw pixels on respective canvases
                            paint.color = Color(cyanColor.red, cyanColor.green, cyanColor.blue)
                            canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
                        }
                    }
                }
            }.awaitAll() // Wait for all tasks to complete
        }

        return cyanBitmap
    }


    fun evaluateBezier(bezierCurve: BezierCurve, value: Float): Float {
        val p0 = Offset(bezierCurve.controlPoints[0].x / 600 , bezierCurve.controlPoints[0].y / 600)
        val p1 = Offset(bezierCurve.controlPoints[1].x / 600 , bezierCurve.controlPoints[1].y / 600)
        val p2 = Offset(bezierCurve.controlPoints[2].x / 600 , bezierCurve.controlPoints[2].y / 600)
        val p3 = Offset(bezierCurve.controlPoints[3].x / 600 , bezierCurve.controlPoints[3].y / 600)

        // Bezier 3-go stopnia: B(t) = (1-t)^3 * P0 + 3(1-t)^2*t * P1 + 3(1-t)*t^2 * P2 + t^3 * P3
        val t = value
        val oneMinusT = 1 - t

        val bezierValue = (oneMinusT * oneMinusT * oneMinusT * p0.y +
                3 * oneMinusT * oneMinusT * t * p1.y +
                3 * oneMinusT * t * t * p2.y +
                t * t * t * p3.y)

        return bezierValue
    }

    fun transformCMYK(
        c: Float,
        m: Float,
        y: Float,
        bezierC: BezierCurve,
        bezierM: BezierCurve,
        bezierY: BezierCurve,
        grayRamp: BezierCurve
    ): CMYK {
        // 1. Oblicz K'
        val kPrime = minOf(c, m, y)

        // 2. Odczytaj wartości Béziera
        val fc = evaluateBezier(bezierC, kPrime)
        val fm = evaluateBezier(bezierM, kPrime)
        val fy = evaluateBezier(bezierY, kPrime)
        val fk = evaluateBezier(grayRamp, kPrime)

        // 3. Oblicz nowe wartości C', M', Y' i K
        val cPrime = (c - kPrime + fc)//.coerceIn(0f, 1f)
        val mPrime = (m - kPrime + fm)//.coerceIn(0f, 1f)
        val yPrime = (y - kPrime + fy)//.coerceIn(0f, 1f)
        val k = fk//.coerceIn(0f, 1f)

        return CMYK(cPrime, mPrime, yPrime, k)
    }

    fun drawPixel(bitmap: ImageBitmap, x: Int, y: Int, color: Color) {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat(), paint)
    }

}
