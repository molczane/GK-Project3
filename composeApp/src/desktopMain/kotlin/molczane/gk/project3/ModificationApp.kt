package molczane.gk.project3

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

import molczane.gk.project3.view.RGBToCMYKApp
import molczane.gk.project3.viewModel.RGBToCMYKViewModel

@Composable
@Preview
fun ModificationApp(viewModel: RGBToCMYKViewModel) {
    MaterialTheme {
        RGBToCMYKApp(viewModel)
    }
}