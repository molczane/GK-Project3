package molczane.gk.project3.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import molczane.gk.project3.model.BezierCurve
import molczane.gk.project3.viewModel.RGBToCMYKViewModel

@Composable
fun RGBToCMYKApp(viewModel: RGBToCMYKViewModel) {
    val state = viewModel.state.collectAsState()

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Lewa strona - podgląd obrazu
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Image(
                    bitmap = state.value.originalImage,
                    contentDescription = "Oryginalny obraz",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
            }

            // Prawa strona - krzywe Béziera i przyciski
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Panel krzywych Béziera
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .background(Color.Gray)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Zastosowanie transformacji układu współrzędnych
                        withTransform({
                            translate(left = 0f, top = 0.0f) // Przesunięcie początku układu współrzędnych do lewego dolnego rogu
                            scale(scaleX = 1f, scaleY = -1f)       // Odwrócenie osi Y
                        }) {
                            if (state.value.showAllCurves) {
                                state.value.bezierCurves.forEach { curve ->
                                    drawBezierCurve(curve) { pointIndex, newOffset ->
                                        viewModel.updateControlPoint(curve, pointIndex, newOffset)
                                    }
                                }
                            } else {
                                val selectedCurve = viewModel.getBezierCurveByColor(state.value.selectedColor)
                                if (selectedCurve != null) {
                                    drawBezierCurve(selectedCurve) { pointIndex, newOffset ->
                                        viewModel.updateControlPoint(selectedCurve, pointIndex, newOffset)
                                    }
                                }
                            }
                        }
                    }
//                    Canvas(modifier = Modifier.fillMaxSize()) {
//                        if (state.value.showAllCurves) {
//                            state.value.bezierCurves.forEach { curve ->
//                                drawBezierCurve(curve)
//                            }
//                        } else {
//                            val selectedCurve = viewModel.getBezierCurveByColor(state.value.selectedColor)
//                            if (selectedCurve != null) {
//                                drawBezierCurve(selectedCurve)
//                            }
//                        }
//                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Przyciski sterujące
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { viewModel.showAllCurves() }) { Text("Show All Curves") }
                        Button(onClick = { viewModel.convertToBlackAndWhite() }) { Text("Black and White") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { viewModel.saveCurves() }) { Text("Save Curve") }
                        Button(onClick = { viewModel.loadCurves() }) { Text("Load Curve") }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wybór koloru
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Black).forEach { color ->
                        Button(
                            onClick = { viewModel.selectColor(color) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = color)
                        ) {
                            Text(colorName(color))
                        }
                    }
                }
            }
        }
    }
}

fun colorName(color: Color): String {
    return when (color) {
        Color.Cyan -> "Cyan"
        Color.Magenta -> "Magenta"
        Color.Yellow -> "Yellow"
        Color.Black -> "Black"
        else -> "Unknown"
    }
}


fun DrawScope.drawBezierCurve(bezierCurve: BezierCurve) {
    val controlPoints = bezierCurve.controlPoints
    val path = Path().apply {
        reset()
        if (controlPoints.isNotEmpty()) {
            moveTo(controlPoints.first().x * size.width, controlPoints.first().y * size.height)
            cubicTo(
                controlPoints[1].x * size.width, controlPoints[1].y * size.height, // Punkt kontrolny 1
                controlPoints[2].x * size.width, controlPoints[2].y * size.height, // Punkt kontrolny 2
                controlPoints.last().x * size.width, controlPoints.last().y * size.height // Punkt końcowy
            )
        }
    }

    drawPath(
        path = path,
        color = bezierCurve.color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Rysowanie punktów kontrolnych
    controlPoints.forEach { point ->
        drawCircle(
            color = bezierCurve.color,
            radius = 6.dp.toPx(),
            center = Offset(point.x * size.width, point.y * size.height)
        )
    }
}

fun DrawScope.drawBezierCurve(
    bezierCurve: BezierCurve,
    onControlPointMoved: (Int, Offset) -> Unit
) {
    val controlPoints = bezierCurve.controlPoints
    val path = Path().apply {
        reset()
        if (controlPoints.isNotEmpty()) {
            moveTo(controlPoints.first().x * size.width, controlPoints.first().y * size.height)
            cubicTo(
                controlPoints[1].x * size.width, controlPoints[1].y * size.height, // Punkt kontrolny 1
                controlPoints[2].x * size.width, controlPoints[2].y * size.height, // Punkt kontrolny 2
                controlPoints.last().x * size.width, controlPoints.last().y * size.height // Punkt końcowy
            )
        }
    }

    // Rysowanie krzywej Béziera
    drawPath(
        path = path,
        color = bezierCurve.color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Rysowanie punktów kontrolnych z możliwością przeciągania
    controlPoints.forEachIndexed { index, point ->
        val absoluteOffset = Offset(point.x * size.width, point.y * size.height)

        // Punkt kontrolny
        drawCircle(
            color = Color.Red,
            radius = 4.dp.toPx(),
            center = absoluteOffset
        )

        // Obsługa przeciągania punktu
        handleDragPoint(
            initialOffset = absoluteOffset,
            onPointDragged = { newOffset ->
                val relativeOffset = Offset(
                    newOffset.x / size.width,
                    newOffset.y / size.height
                )
                onControlPointMoved(index, relativeOffset)
            }
        )
    }
}

fun DrawScope.handleDragPoint(
    initialOffset: Offset,
    onPointDragged: (Offset) -> Unit
) {
    Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume() // Zatrzymanie propagacji zdarzenia
            val newOffset = initialOffset + dragAmount
            onPointDragged(newOffset)
        }
    }
}
