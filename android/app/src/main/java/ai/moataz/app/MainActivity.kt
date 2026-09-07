package ai.moataz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.moataz.app.ui.MoatazApp
import ai.moataz.app.ui.theme.MoatazTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoatazTheme {
                val viewModel: MoatazViewModel = viewModel()
                MoatazApp(viewModel)
            }
        }
    }
}
