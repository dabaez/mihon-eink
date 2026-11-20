package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.presentation.core.components.onEinkPullRefresh
import tachiyomi.presentation.core.util.LocalEinkMode

/**
 * @param refreshing Whether the layout is currently refreshing
 * @param onRefresh Lambda which is invoked when a swipe to refresh gesture is completed.
 * @param enabled Whether the the layout should react to swipe gestures or not.
 * @param indicatorPadding Content padding for the indicator, to inset the indicator in if required.
 * @param content The content containing a vertically scrollable composable.
 */
@Composable
fun PullRefresh(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    Box(
        modifier = modifier
            .pullToRefresh(
                isRefreshing = refreshing,
                state = state,
                enabled = enabled,
                onRefresh = onRefresh,
            )
            .onEinkPullRefresh(enabled = enabled && !refreshing, onRefresh = onRefresh),
    ) {
        content()

        val indicatorModifier = Modifier
            .align(Alignment.TopCenter)
            .padding(indicatorPadding)
        if (LocalEinkMode.current) {
            // A spinner can't spin in e-ink mode, and a still one reads as stuck
            PullToRefreshDefaults.IndicatorBox(
                state = state,
                isRefreshing = refreshing,
                modifier = indicatorModifier,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            PullToRefreshDefaults.Indicator(
                modifier = indicatorModifier,
                isRefreshing = refreshing,
                state = state,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
