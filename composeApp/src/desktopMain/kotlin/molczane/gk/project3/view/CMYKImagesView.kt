package molczane.gk.project3.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import molczane.gk.project3.viewModel.RGBToCMYKViewModel

@Composable
fun CMYKImagesView(viewModel: RGBToCMYKViewModel) {
    val images = viewModel.state.value.cmykImages
    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Image(
                bitmap = images[0],
                contentDescription = "Cyan",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
            Image(
                bitmap = images[1],
                contentDescription = "Magenta",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
        }
        Row {
            Image(
                bitmap = images[2],
                contentDescription = "Yellow",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
            Image(
                bitmap = images[3],
                contentDescription = "Black",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
        }
    }
}