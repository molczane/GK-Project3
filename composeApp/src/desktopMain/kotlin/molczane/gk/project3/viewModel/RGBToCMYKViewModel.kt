package molczane.gk.project3.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import molczane.gk.project3.model.RGBToCMYKState
import molczane.gk.project3.model.BezierCurve
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
                    Offset(0f, 0f),   // Początek
                    Offset(0.2f, 0.3f), // Punkt kontrolny 1
                    Offset(0.6f, 0.8f), // Punkt kontrolny 2
                    Offset(1f, 1f)    // Koniec
                ),
                color = Color.Cyan
            ),
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),   // Początek
                    Offset(0.3f, 0.4f), // Punkt kontrolny 1
                    Offset(0.5f, 0.6f), // Punkt kontrolny 2
                    Offset(1f, 1f)    // Koniec
                ),
                color = Color.Magenta
            ),
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),   // Początek
                    Offset(0.4f, 0.2f), // Punkt kontrolny 1
                    Offset(0.7f, 0.9f), // Punkt kontrolny 2
                    Offset(1f, 1f)    // Koniec
                ),
                color = Color.Yellow
            ),
            BezierCurve(
                controlPoints = listOf(
                    Offset(0f, 0f),   // Początek
                    Offset(0.1f, 0.0f), // Punkt kontrolny 1
                    Offset(0.8f, 0.0f), // Punkt kontrolny 2
                    Offset(1f, 0f)    // Koniec
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


    fun convertRGBToCMYK(
        image: ImageBitmap,
        bezierCurves: List<BezierCurve> // Krzywe dla każdego kanału: "C", "M", "Y", "K"
    ): List<ImageBitmap> {
        val width = image.width
        val height = image.height

        // Kanały wynikowe dla C, M, Y, K
        val cyanBitmap = ImageBitmap(width, height)
        val magentaBitmap = ImageBitmap(width, height)
        val yellowBitmap = ImageBitmap(width, height)
        val blackBitmap = ImageBitmap(width, height)

        val pixelMap = image.toPixelMap()

        // Iteracja przez piksele obrazu RGB
        // Iteracja przez piksele obrazu RGB
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = pixelMap[x, y]

                // Pobieranie wartości RGB
                val r = pixelColor.red
                val g = pixelColor.green
                val b = pixelColor.blue

                // Konwersja RGB -> CMYK
                val cmyk = rgbToCmyk(r, g, b)

                // Modyfikacja wartości CMYK na podstawie krzywych Béziera
                val cyan = evaluateBezier(bezierCurves[0], cmyk.cyan)
                val magenta = evaluateBezier(bezierCurves[1], cmyk.magenta)
                val yellow = evaluateBezier(bezierCurves[2], cmyk.yellow)
                val black = evaluateBezier(bezierCurves[3], cmyk.black)

                val cyanRGB = cmykToRgb(cyan, 0f, 0f, 0f)
                val magentaRGB = cmykToRgb(0f, magenta, 0f, 0f)
                val yellowRGB = cmykToRgb(0f, 0f, yellow, 0f)
                val blackRGB = cmykToRgb(0f, 0f, 0f, black)

                // Rysowanie pikseli na kanałach C, M, Y, K
                drawPixel(cyanBitmap, x, y, Color(cyanRGB.red, cyanRGB.blue, cyanRGB.green))
                drawPixel(magentaBitmap, x, y, Color(magentaRGB.red, magentaRGB.blue, magentaRGB.green))
                drawPixel(yellowBitmap, x, y, Color(yellowRGB.red, yellowRGB.blue, yellowRGB.green))
                drawPixel(blackBitmap, x, y,Color(blackRGB.red, blackRGB.blue, blackRGB.green))
            }
        }

        return listOf(cyanBitmap, magentaBitmap, yellowBitmap, blackBitmap)
    }

    fun evaluateBezier(bezierCurve: BezierCurve, value: Float): Float {
        val p0 = bezierCurve.controlPoints[0]
        val p1 = bezierCurve.controlPoints[1]
        val p2 = bezierCurve.controlPoints[2]
        val p3 = bezierCurve.controlPoints[3]

        // Bezier 3-go stopnia: B(t) = (1-t)^3 * P0 + 3(1-t)^2*t * P1 + 3(1-t)*t^2 * P2 + t^3 * P3
        val t = value
        val oneMinusT = 1 - t

        val bezierValue = (oneMinusT * oneMinusT * oneMinusT * p0.y +
                3 * oneMinusT * oneMinusT * t * p1.y +
                3 * oneMinusT * t * t * p2.y +
                t * t * t * p3.y)

        return bezierValue
    }

    fun drawPixel(bitmap: ImageBitmap, x: Int, y: Int, color: Color) {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat(), paint)
    }

}
