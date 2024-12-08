package molczane.gk.project3.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import molczane.gk.project3.viewModel.RGBToCMYKViewModel

@Composable
fun CMYKImagesView(viewModel: RGBToCMYKViewModel) {
    val state by mutableStateOf( viewModel.state.collectAsState() )
    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Image(
                bitmap = state.value.cmykImages[0],
                contentDescription = "Cyan",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
            Image(
                bitmap = state.value.cmykImages[1],
                contentDescription = "Magenta",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
        }
        Row {
            Image(
                bitmap = state.value.cmykImages[2],
                contentDescription = "Yellow",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
            Image(
                bitmap = state.value.cmykImages[3],
                contentDescription = "Black",
                modifier = Modifier.weight(1f).padding(8.dp)
            )
        }
    }
}