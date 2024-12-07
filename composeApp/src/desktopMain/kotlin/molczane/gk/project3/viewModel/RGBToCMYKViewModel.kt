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

    fun updateBezierPoint(color: Color, pointIndex: Int, newPoint: Offset) {
        // Aktualizacja punktu kontrolnego krzywej
        val updatedCurves = _state.value.bezierCurves.toMutableList()
        val curveIndex = colorToIndex(color)
        updatedCurves[curveIndex] = updatedCurves[curveIndex].copy(
            controlPoints = updatedCurves[curveIndex].controlPoints.toMutableList().apply {
                this[pointIndex] = newPoint
            }
        )
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
                    Offset(0.1f, 0.2f), // Punkt kontrolny 1
                    Offset(0.8f, 0.7f), // Punkt kontrolny 2
                    Offset(1f, 1f)    // Koniec
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
        val c = 1 - r
        val m = 1 - g
        val y = 1 - b
        val k = minOf(c, m, y)

        return CMYK(
            cyan = (c - k) / (1 - k),
            magenta = (m - k) / (1 - k),
            yellow = (y - k) / (1 - k),
            black = k
        )
    }

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

                // Rysowanie pikseli na kanałach C, M, Y, K
                drawPixel(cyanBitmap, x, y, Color(cyan, cyan, cyan))
                drawPixel(magentaBitmap, x, y, Color(magenta, magenta, magenta))
                drawPixel(yellowBitmap, x, y, Color(yellow, yellow, yellow))
                drawPixel(blackBitmap, x, y, Color(black, black, black))
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
