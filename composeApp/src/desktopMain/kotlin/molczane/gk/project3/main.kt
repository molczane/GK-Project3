package molczane.gk.project3

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import molczane.gk.project3.view.CMYKImagesView
import molczane.gk.project3.viewModel.RGBToCMYKViewModel

fun main() = application {
    val viewModel = RGBToCMYKViewModel()
    Window(
        onCloseRequest = ::exitApplication,
        title = "GK-Project3",
    ) {
        CMYKImagesView(viewModel)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "GK-Project3",
    ) {
        ModificationApp(viewModel)
    }

}