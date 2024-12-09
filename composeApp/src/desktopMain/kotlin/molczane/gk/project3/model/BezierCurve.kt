package molczane.gk.project3.model

import ColorSerializer
import OffsetSerializer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class BezierCurve(
    val controlPoints: List<@Serializable(with = OffsetSerializer::class) Offset>, // Apply OffsetSerializer to each Offset
    @Serializable(with = ColorSerializer::class)
    val color: Color // Apply ColorSerializer to the Color field
)
