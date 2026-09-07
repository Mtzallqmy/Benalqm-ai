package ai.moataz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.moataz.app.ui.theme.MoatazDanger

/**
 * Compatibility bridge for the Material 3 API used by the native client.
 * Keeps the call-site stable while delegating icon-button colors to the
 * correct Material 3 factory.
 */
@Composable
fun ButtonDefaults.filledIconButtonColors(
    containerColor: Color,
    contentColor: Color,
): IconButtonColors = IconButtonDefaults.filledIconButtonColors(
    containerColor = containerColor,
    contentColor = contentColor,
)

/**
 * Nullable overload used when the screen state is collected through a
 * delegated Compose State. Kotlin cannot smart-cast a property reached
 * through that delegated state, even after a null check at the call-site.
 */
@Composable
fun ErrorBanner(
    message: CharSequence?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = message?.toString().orEmpty()
    if (text.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MoatazDanger.copy(alpha = 0.96f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Text(
                text = text,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}
