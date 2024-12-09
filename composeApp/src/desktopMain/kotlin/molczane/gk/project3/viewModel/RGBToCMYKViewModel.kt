package molczane.gk.project3.viewModel

import java.io.File
import java.io.FileReader
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import molczane.gk.project3.model.*
import molczane.gk.project3.predefinedCurves.predefinedCurves
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class RGBToCMYKViewModel : ViewModel() {
    private val _state = MutableStateFlow(RGBToCMYKState(originalImage = loadImage("src/images/mountains-3.png")))
    val state: StateFlow<RGBToCMYKState> get() = _state

    private val bezierCurveStorage = mutableMapOf<Color, BezierCurve>()

    private var _showAllPictures = MutableStateFlow(true)
    val showAllPictures: StateFlow<Boolean> get() = _showAllPictures

    init {
        // Inicjalizacja przykładowych krzywych Béziera dla każdego koloru
        _state.value = _state.value.copy(
            originalImage = loadImage("src/images/mountains-3.png")
        )
        _state.value = _state.value.copy(
            bezierCurves = initializeDefaultCurves(),
        )
        _state.value = _state.value.copy(
            cmykImages = convertRGBToCMYK(_state.value.originalImage, _state.value.bezierCurves)
        )
        updateProcessedImageFromCMYKImages()
    }

    fun changeImage(imagePath: String?) {
        if (imagePath != null) {
            val newImage = loadImage(imagePath)
            _state.value = _state.value.copy(originalImage = newImage)
            _state.value = _state.value.copy(
                cmykImages = convertRGBToCMYK(_state.value.originalImage, _state.value.bezierCurves)
            )
            updateProcessedImageFromCMYKImages()
        }
    }

    fun saveImages() {
        val cyanBufferedImage = imageBitmapToBufferedImage(_state.value.cmykImages[0])
        val magentaBufferedImage = imageBitmapToBufferedImage(_state.value.cmykImages[1])
        val yellowBufferedImage = imageBitmapToBufferedImage(_state.value.cmykImages[2])
        val blackBufferedImage = imageBitmapToBufferedImage(_state.value.cmykImages[3])
        val processedBufferedImage = _state.value.processedImage?.let { imageBitmapToBufferedImage(it) }

        ImageIO.write(cyanBufferedImage, "png", File("src/processedImages/cyan.png"))
        ImageIO.write(magentaBufferedImage, "png", File("src/processedImages/magenta.png"))
        ImageIO.write(yellowBufferedImage, "png", File("src/processedImages/yellow.png"))
        ImageIO.write(blackBufferedImage, "png", File("src/processedImages/black.png"))
        ImageIO.write(processedBufferedImage, "png", File("src/processedImages/processed.png"))
    }

    fun imageBitmapToBufferedImage(imageBitmap: ImageBitmap): BufferedImage {
        val pixelMap = imageBitmap.toPixelMap()
        val bufferedImage = BufferedImage(pixelMap.width, pixelMap.height, BufferedImage.TYPE_INT_ARGB)

        for (x in 0 until pixelMap.width) {
            for (y in 0 until pixelMap.height) {
                val color = pixelMap[x, y]
                val alpha = (color.alpha * 255).roundToInt()
                val red = (color.red * 255).roundToInt()
                val green = (color.green * 255).roundToInt()
                val blue = (color.blue * 255).roundToInt()

                val argb = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                bufferedImage.setRGB(x, y, argb)
            }
        }

        return bufferedImage
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

    fun saveCurve(index: Int) {
        // Zapis krzywych do mapy (symulacja zapisu do pliku)
        serializeCurvesToFile(index)
    }

    fun loadCurve(filePath: String) {
        val (loadedCurve, index) = deserializeBezierCurveFromFilePath(filePath)
        if (loadedCurve != null && index != null) {
            val updatedCurves = _state.value.bezierCurves.toMutableList()
            when(index) {
                0 -> loadedCurve.color = Color.Cyan
                1 -> loadedCurve.color = Color.Magenta
                2 -> loadedCurve.color = Color.Yellow
                3 -> loadedCurve.color = Color.Black
            }
            updatedCurves[index] = loadedCurve
            _state.value = _state.value.copy(bezierCurves = updatedCurves)
            _state.value = _state.value.copy(
                cmykImages = convertRGBToCMYK(_state.value.originalImage, _state.value.bezierCurves)
            )
            updateProcessedImageFromCMYKImages()
            when(index) {
                0 -> runBlocking { updateCImage() }
                1 -> updateMImage()
                2 -> updateYImage()
                3 -> updateKImage()
            }
        }
//        if (loadedCurve != null) {
//            updateCurveForSelectedChannel(loadedCurve)
//        }
    }
    
    private fun deserializeBezierCurveFromFilePath(filePath: String): Pair<BezierCurve?, Int?> {
        return try {
            val fileContent = FileReader(filePath).use { it.readText() }
            val loadedCurve = Json.decodeFromString<BezierCurve>(fileContent)
            val index = filePath.dropLast(5).last().toString().toIntOrNull()
            Pair(loadedCurve, index)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
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

    fun showAllPictures(show: Boolean) {
        _showAllPictures.value = show
    }

    private val predefinedCurves = predefinedCurves()

    fun updateBCR() {
        _state.update { it.copy(bezierCurves = it.bezierCurves.toMutableList().apply {
            this[colorToIndex(Color.Black)] = predefinedCurves.bcr
        }) }
        updateKImage()
        updateProcessedImageFromCMYKImages()
    }
    
    fun updateUCR() {
        _state.update { it.copy(bezierCurves = it.bezierCurves.toMutableList().apply {
            this[colorToIndex(Color.Black)] = predefinedCurves.ucr
        }) }
        updateKImage()
        updateProcessedImageFromCMYKImages()
    }
    
    fun update0percent() {
        _state.update { it.copy(bezierCurves = it.bezierCurves.toMutableList().apply {
            this[colorToIndex(Color.Black)] = predefinedCurves.percent0
        }) }
        updateKImage()
        updateProcessedImageFromCMYKImages()
    }
    
    fun update100percent() {
        _state.update { it.copy(bezierCurves = it.bezierCurves.toMutableList().apply {
            this[colorToIndex(Color.Black)] = predefinedCurves.percent100
        }) }
        updateKImage()
        updateProcessedImageFromCMYKImages()
    }

    private fun colorToIndex(color: Color): Int {
        // Zamiana koloru na indeks w liście krzywych
        return listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Black).indexOf(color)
    }

    fun getBezierCurveByColor(color: Color): BezierCurve? {
        return _state.value.bezierCurves.find { it.color == color }
    }

    fun rgbToCMY(r: Float, g: Float, b: Float): CMY {
        // Normalize RGB values to the range [0, 1]
        return CMY(1f - r, 1f - g, 1f - b)
    }

    fun cmykToRgb(c: Float, m: Float, y: Float, k: Float): RGB {
        // Ensure input values are in the range [0, 1]
        val cyan = c
        val magenta = m
        val yellow = y
        val black = k

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

    suspend fun updateCImage() {
        val cyanImage = convertRGBToCMYKCyan(_state.value.originalImage, _state.value.bezierCurves)
        val updatedImages = _state.value.cmykImages.toMutableList()
        if (updatedImages.isNotEmpty()) {
            updatedImages[0] = cyanImage // Zastąpienie obrazu Cyan
        } else {
            updatedImages.add(cyanImage) // Dodanie obrazu Cyan, jeśli lista jest pusta
        }
        _state.update { it.copy(cmykImages = updatedImages) }
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
    
    private fun serializeCurvesToFile(index: Int) {
        val outputDirectory = File("src/curves")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val outputFile = File(outputDirectory, "curve_$index.json")
        outputFile.writeText(Json.encodeToString(_state.value.bezierCurves[index]))
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

    fun convertRGBtoGrayScale() {
        val image = _state.value.originalImage
        val width = image.width
        val height = image.height

        val imageBitmap = ImageBitmap(width, height)

        val canvas = Canvas(imageBitmap)
        val paint = Paint()

        val pixelMap = image.toPixelMap()

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelMap[x, y]

                // Extract RGB values
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                // Calculate grayscale value
                val gray = (0.299*((r * 255).roundToInt()) + 0.587*((g * 255).roundToInt()) + 0.114*((b * 255).roundToInt())).roundToInt()

                // Draw pixels on respective canvases
                paint.color = Color(gray, gray, gray)
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        _state.value = _state.value.copy(processedImage = imageBitmap)
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

                val cmy = rgbToCMY(r, g, b)

                val cmykTransformed = transformCMYK(cmy.cyan, cmy.magenta, cmy.yellow, bezierCurves[0], bezierCurves[1], bezierCurves[2], bezierCurves[3])
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

                val cmy = rgbToCMY(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmy.cyan,
                    cmy.magenta,
                    cmy.yellow,
                    bezierCurves[0],
                    bezierCurves[1],
                    bezierCurves[2],
                    bezierCurves[3]
                )

                // Adjust CMYK values using Bézier curves
                val magenta = cmykTransformed.magenta

                // Convert adjusted CMYK values back to RGB for visualization
                val magentaColor = cmykToRgb(0f, magenta, 0f, 0f)

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

                val cmy = rgbToCMY(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmy.cyan,
                    cmy.magenta,
                    cmy.yellow,
                    bezierCurves[0],
                    bezierCurves[1],
                    bezierCurves[2],
                    bezierCurves[3]
                )

                // Adjust CMYK values using Bézier curves
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

                val cmy = rgbToCMY(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmy.cyan,
                    cmy.magenta,
                    cmy.yellow,
                    bezierCurves[0],
                    bezierCurves[1],
                    bezierCurves[2],
                    bezierCurves[3]
                )

                // Adjust CMYK values using Bézier curves
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

    // function that sums CMYK images to one image
    fun updateProcessedImageFromCMYKImages() {
        val image = _state.value.originalImage
        val width = image.width
        val height = image.height

        val imageBitmap = ImageBitmap(width, height)

        val canvas = Canvas(imageBitmap)
        val paint = Paint()

        val imagePixelMap = image.toPixelMap()

        // Iterate through all pixels
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = imagePixelMap[x, y]

                // Extract RGB values
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                val cmy = rgbToCMY(r, g, b)

                val cmykTransformed = transformCMYK(
                    cmy.cyan,
                    cmy.magenta,
                    cmy.yellow,
                    _state.value.bezierCurves[0],
                    _state.value.bezierCurves[1],
                    _state.value.bezierCurves[2],
                    _state.value.bezierCurves[3]
                )

                // Convert adjusted CMYK values back to RGB for visualization
                val color = cmykToRgb(cmykTransformed.cyan, cmykTransformed.magenta, cmykTransformed.yellow, cmykTransformed.black)

                // Draw pixels on respective canvases
                paint.color = Color(color.red, color.green, color.blue)

                // Draw pixels on respective canvases
                paint.alpha = 1f

                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }

        _state.value = _state.value.copy(processedImage = imageBitmap)
    }

    suspend fun convertRGBToCMYKCyan(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve>
    ): ImageBitmap {
        val width = image.width
        val height = image.height

        // Prepare an empty ImageBitmap for Cyan output
        val cyanBitmap = ImageBitmap(width, height)
        val pixelMap = image.toPixelMap()

        // Define the number of parallel tasks
        val numTasks = 100
        val chunkSize = height / numTasks

        val canvas = Canvas(cyanBitmap)
        val paint = Paint()

        coroutineScope {
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

                            val cmy = rgbToCMY(r, g, b)

                            val cmykTransformed = transformCMYK(
                                cmy.cyan,
                                cmy.magenta,
                                cmy.yellow,
                                bezierCurves[0],
                                bezierCurves[1],
                                bezierCurves[2],
                                bezierCurves[3]
                            )

                            // Adjust CMYK values using Bézier curves
                            //val black = evaluateBezier(bezierCurves[3], cmyk.black)
                            val cyan = cmykTransformed.cyan

                            // Convert adjusted CMYK values back to RGB for visualization
                            val cyanColor = cmykToRgb(cyan, 0f, 0f, 0f)

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
}
