package molczane.gk.project3.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class BezierCurve(
    val controlPoints: List<Offset>, // Lista punktów kontrolnych
    val color: Color // Kolor odpowiadający tej krzywej
)
