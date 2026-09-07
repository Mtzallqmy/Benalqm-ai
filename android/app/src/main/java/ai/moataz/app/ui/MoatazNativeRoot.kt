package ai.moataz.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.moataz.app.ControlCenterViewModel
import ai.moataz.app.MoatazViewModel
import ai.moataz.app.R

private enum class NativeSurface { Agent, ControlCenter }

/**
 * Native product root. The full DeerFlow agent workspace and its management
 * capabilities are two Compose surfaces in the same APK; neither path uses a
 * WebView or delegates the product UI to a browser.
 */
@Composable
fun MoatazNativeRoot(
    agentViewModel: MoatazViewModel,
    controlViewModel: ControlCenterViewModel,
) {
    val agentState by agentViewModel.state.collectAsStateWithLifecycle()
    var surface by remember { mutableStateOf(NativeSurface.Agent) }

    LaunchedEffect(agentState.activeThreadId) {
        controlViewModel.setActiveThread(agentState.activeThreadId)
    }

    BackHandler(enabled = surface == NativeSurface.ControlCenter) {
        surface = NativeSurface.Agent
    }

    AnimatedContent(
        targetState = surface,
        label = "moataz-native-surface",
        modifier = Modifier.fillMaxSize(),
    ) { target ->
        when (target) {
            NativeSurface.Agent -> {
                Box(Modifier.fillMaxSize()) {
                    MoatazApp(agentViewModel)
                    ExtendedFloatingActionButton(
                        onClick = {
                            controlViewModel.refreshAll()
                            surface = NativeSurface.ControlCenter
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 52.dp, end = 12.dp),
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                        text = { Text(stringResource(R.string.control_center)) },
                    )
                }
            }
            NativeSurface.ControlCenter -> ControlCenterScreen(
                viewModel = controlViewModel,
                onBack = { surface = NativeSurface.Agent },
            )
        }
    }
}
