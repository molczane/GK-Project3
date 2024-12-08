package molczane.gk.project3

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import molczane.gk.project3.view.CMYKImagesView
import molczane.gk.project3.viewModel.RGBToCMYKViewModel

fun main() = application {
    val viewModel = RGBToCMYKViewModel()
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 800.dp, height = 600.dp), // Fixed size
        resizable = false, // Prevent resizing
        title = "GK-Project3",
    ) {
        CMYKImagesView(viewModel)
    }
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 800.dp, height = 600.dp), // Fixed size
        resizable = false, // Prevent resizing
        title = "GK-Project3",
    ) {
        ModificationApp(viewModel)
    }

}