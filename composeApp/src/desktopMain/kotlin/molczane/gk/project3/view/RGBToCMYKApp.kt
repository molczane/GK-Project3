package molczane.gk.project3.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gk_project3.composeapp.generated.resources.Res
import kotlinx.coroutines.launch
import molczane.gk.project3.model.BezierCurve
import molczane.gk.project3.viewModel.RGBToCMYKViewModel
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun RGBToCMYKApp(viewModel: RGBToCMYKViewModel) {
    val state by mutableStateOf( viewModel.state.collectAsState() )

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Lewa strona - krzywe Béziera i przyciski
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Panel krzywych Béziera
                Row(
                    modifier = Modifier
                        .height(300.dp)
                        .width(300.dp)
                        .weight(1f)
                        .border(.8.dp, color = Color.Gray),
                    horizontalArrangement = Arrangement.Center
                ) {
                    var selectedCurve: BezierCurve? = null
                    var selectedPointIndex: Int? = null

                    val coroutineScope = rememberCoroutineScope()

                    Canvas(
                        modifier = Modifier
                            .background(Color.LightGray)
                            .height(300.dp)
                            .width(300.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->

                                        // find the closest control point of selected Bézier curve with maximum distance of 10 px
                                        selectedCurve = viewModel.getBezierCurveByColor(state.value.selectedColor)
                                        val newBezierControlPoints = selectedCurve?.controlPoints?.map {
                                            Offset(it.x, it.y)
                                        }
                                        val newOffset = Offset(offset.x, size.height - offset.y)
                                        if (selectedCurve != null) {
                                            selectedPointIndex = newBezierControlPoints!!.indexOfFirst {
                                                (newOffset - it).getDistance() < 20.dp.toPx()
                                            }.takeIf { it != -1 }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        // update the position of the selected control point
                                        if (selectedCurve != null && selectedPointIndex != null) {
                                            println("Dragged from ${selectedCurve!!.controlPoints[selectedPointIndex!!]}!")
                                            println("Dragged by ${dragAmount}!")
                                            val newOffset =
                                                selectedCurve!!.controlPoints[selectedPointIndex!!] + Offset(
                                                    dragAmount.x,
                                                    -dragAmount.y
                                                )
                                            viewModel.updateControlPoint(
                                                selectedCurve!!,
                                                selectedPointIndex!!,
                                                newOffset
                                            )
                                            // update position of selected curve
                                            selectedCurve = viewModel.getBezierCurveByColor(state.value.selectedColor)
                                            println("Dragged to ${newOffset}!")
                                        }

                                        change.consume()
                                    },
                                    onDragEnd = {
                                        if (selectedCurve != null) {
                                            when (selectedCurve!!.color) {
                                                Color.Cyan -> coroutineScope.launch {
                                                    viewModel.updateCImage()
                                                }
                                                Color.Magenta -> viewModel.updateMImage()
                                                Color.Yellow -> viewModel.updateYImage()
                                                Color.Black -> viewModel.updateKImage()
                                            }
                                            viewModel.updateProcessedImageFromCMYKImages()
                                        }
                                        selectedCurve = null
                                        selectedPointIndex = null
                                    }
                                )
                            }
                    ) {
                        // Zastosowanie transformacji układu współrzędnych
                        withTransform({
                            translate(
                                left = 0f,
                                top = 0.0f
                            ) // Przesunięcie początku układu współrzędnych do lewego dolnego rogu
                            scale(scaleX = 1f, scaleY = -1f)       // Odwrócenie osi Y
                        }) {
                            if (state.value.showAllCurves) {
                                state.value.bezierCurves.forEach { curve ->
                                    drawBezierCurve(curve) { pointIndex, newOffset ->
                                        viewModel.updateControlPoint(curve, pointIndex, newOffset)
                                    }
                                }
                            }
                            val selectedCurve = viewModel.getBezierCurveByColor(state.value.selectedColor)
                            if (selectedCurve != null) {
                                drawBezierCurveWithControlPoints(selectedCurve) { pointIndex, newOffset ->
                                    viewModel.updateControlPoint(selectedCurve, pointIndex, newOffset)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().weight(0.7f),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Black).forEach { color ->
                                Button(
                                    onClick = { viewModel.selectColor(color) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = color),
                                    modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                                ) {
                                    if (colorName(color) == "Black") {
                                        Text(colorName(color), color = Color.White, fontSize = 9.sp)
                                    } else {
                                        Text(colorName(color), fontSize = 9.sp)
                                    }
                                }
                            }
                            Button(
                                onClick = { viewModel.showAllCurves() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Show All Curves", fontSize = 9.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { viewModel.convertToBlackAndWhite() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("0% cofnięcia", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { viewModel.convertToBlackAndWhite() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("100% cofnięcia", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { viewModel.convertToBlackAndWhite() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("UCR", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { viewModel.convertToBlackAndWhite() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("GCR", fontSize = 9.sp)
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { viewModel.showAllPictures(!viewModel.showAllPictures.value) },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Show all pictures", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { viewModel.changeImage(openFileDialog()) },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Change picture", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { viewModel.saveCurves(colorNumber(state.value.selectedColor)) },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Save Curve", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { viewModel.loadCurves() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Load Curve", fontSize = 9.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { viewModel.convertToBlackAndWhite() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Black and White", fontSize = 9.sp)
                            }

                            Button(
                                onClick = { viewModel.saveImages() },
                                modifier = Modifier.weight(1f).padding(3.dp).fillMaxWidth()
                            ) {
                                Text("Save pictures", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Prawa strona - obraz oryginalny i przetworzony
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Original image", fontSize = 12.sp)
                Image(
                    bitmap = state.value.originalImage,
                    contentDescription = "Oryginalny obraz",
                    modifier = Modifier.fillMaxWidth().padding(4.dp).weight(1f)
                )
                Text("Processed image", fontSize = 12.sp)
                if(state.value.processedImage != null) {
                    Image(
                        bitmap = state.value.processedImage!!,
                        contentDescription = "Przetworzony obraz",
                        modifier = Modifier.fillMaxWidth().padding(4.dp).weight(1f)
                    )
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

fun colorNumber(color: Color): Int {
    return when (color) {
        Color.Cyan -> 0
        Color.Magenta -> 1
        Color.Yellow -> 2
        Color.Black -> 3
        else -> -1
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
            moveTo(controlPoints.first().x, controlPoints.first().y)
            cubicTo(
                controlPoints[1].x , controlPoints[1].y, // Punkt kontrolny 1
                controlPoints[2].x, controlPoints[2].y, // Punkt kontrolny 2
                controlPoints.last().x, controlPoints.last().y  // Punkt końcowy
            )
        }
    }

    // Rysowanie krzywej Béziera
    drawPath(
        path = path,
        color = bezierCurve.color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

fun DrawScope.drawBezierCurveWithControlPoints(
    bezierCurve: BezierCurve,
    onControlPointMoved: (Int, Offset) -> Unit
) {
    val controlPoints = bezierCurve.controlPoints
    val path = Path().apply {
        reset()
        if (controlPoints.isNotEmpty()) {
            moveTo(controlPoints.first().x, controlPoints.first().y)
            cubicTo(
                controlPoints[1].x, controlPoints[1].y, // Punkt kontrolny 1
                controlPoints[2].x, controlPoints[2].y, // Punkt kontrolny 2
                controlPoints.last().x , controlPoints.last().y // Punkt końcowy
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
        val absoluteOffset = Offset(point.x, point.y)

        // Punkt kontrolny
        drawCircle(
            color = Color.Red,
            radius = 4.dp.toPx(),
            center = absoluteOffset
        )
    }
}

fun openFileDialog(defaultDirectory: String = System.getProperty("user.home")): String? {
    val fileDialog = FileDialog(null as Frame?, "Select a File", FileDialog.LOAD)
    fileDialog.directory = "src/images" // Set the default directory
    fileDialog.isVisible = true
    return fileDialog.file?.let { fileDialog.directory + it }
}