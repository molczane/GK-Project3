package molczane.gk.project3.predefinedCurves

import androidx.compose.ui.geometry.Offset
import molczane.gk.project3.model.BezierCurve
import androidx.compose.ui.graphics.Color

data class predefinedCurves(
    val percent100: BezierCurve =  BezierCurve(
        controlPoints = listOf(
            Offset(0f, 0f),       // Start point
            Offset(200f, 200f),   // Control point 1
            Offset(400f, 400f),   // Control point 2
            Offset(600f, 600f)    // End point
        ),
        color = Color.Black
    ),
    val percent0: BezierCurve =  BezierCurve(
        controlPoints = listOf(
            Offset(0f, 0f),       // Start point
            Offset(200f, 0f),   // Control point 1
            Offset(400f, 0f),   // Control point 2
            Offset(600f, 0f)    // End point
        ),
        color = Color.Black
    ),
    val ucr: BezierCurve =  BezierCurve(
        controlPoints = listOf(
            Offset(0f, 0f),       // Start point
            Offset(500f, 0f),   // Control point 1
            Offset(550f, 400f),   // Control point 2
            Offset(600f, 600f)    // End point
        ),
        color = Color.Black
    ),
    val bcr: BezierCurve = BezierCurve(
        controlPoints = listOf(
            Offset(0f, 0f),       // Start point
            Offset(500f, 0f),    // Control point 1
            Offset(550f, 300f),   // Control point 2
            Offset(600f, 600f)    // End point
        ),
        color = Color.Black
    )
)
