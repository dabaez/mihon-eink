package tachiyomi.presentation.core.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.HourglassEmpty
import tachiyomi.presentation.core.util.LocalEinkMode

/**
 * An indeterminate [CircularProgressIndicator], or in e-ink mode a still hourglass of the same size, as a spinner
 * that can't spin reads as stuck.
 */
@Composable
fun CircularLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
) {
    if (LocalEinkMode.current) {
        Icon(
            imageVector = MaterialSymbols.Rounded.HourglassEmpty,
            contentDescription = null,
            modifier = modifier.size(40.dp),
            tint = color,
        )
        return
    }
    CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = strokeWidth)
}

/**
 * A combined [CircularProgressIndicator] that always rotates.
 *
 * By always rotating we give the feedback to the user that the application isn't 'stuck'. In e-ink mode
 * it stands still instead, as every frame of a rotation redraws the screen.
 */
@Composable
fun CombinedCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    if (LocalEinkMode.current) {
        CircularProgressIndicator(progress = progress, modifier = modifier)
        return
    }
    AnimatedContent(
        targetState = progress() == 0f,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "progressState",
        modifier = modifier,
    ) { indeterminate ->
        if (indeterminate) {
            // Indeterminate
            CircularProgressIndicator()
        } else {
            // Determinate
            val infiniteTransition = rememberInfiniteTransition(label = "infiniteRotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotation",
            )
            val animatedProgress by animateFloatAsState(
                targetValue = progress(),
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress",
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@Preview
@Composable
private fun CombinedCircularProgressIndicatorPreview() {
    var progress by remember { mutableFloatStateOf(0f) }
    MaterialTheme {
        Scaffold(
            bottomBar = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        progress = when (progress) {
                            0f -> 0.15f
                            0.15f -> 0.25f
                            0.25f -> 0.5f
                            0.5f -> 0.75f
                            0.75f -> 0.95f
                            else -> 0f
                        }
                    },
                ) {
                    Text("change")
                }
            },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                CombinedCircularProgressIndicator(progress = { progress })
            }
        }
    }
}
