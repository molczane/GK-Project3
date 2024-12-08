import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Niestandardowy serializer dla klasy Color
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Color) {
        // Serializujemy kolor jako liczbę ARGB
        val argb = (value.alpha * 255).toInt() shl 24 or
                (value.red * 255).toInt() shl 16 or
                (value.green * 255).toInt() shl 8 or
                (value.blue * 255).toInt()
        encoder.encodeInt(argb)
    }

    override fun deserialize(decoder: Decoder): Color {
        // Deserializujemy liczbę ARGB na obiekt Color
        val argb = decoder.decodeInt()
        val a = (argb shr 24 and 0xFF) / 255f
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return Color(r, g, b, a)
    }
}
