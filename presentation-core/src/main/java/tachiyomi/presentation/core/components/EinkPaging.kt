package tachiyomi.presentation.core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.LocalEinkMode
import tachiyomi.presentation.core.util.plus
import kotlin.math.abs

/**
 * Moves a list a page at a time, for e-ink mode, where lists mustn't scroll under the finger.
 */
@Stable
class EinkPager internal constructor(
    private val state: ScrollableState,
    private val scope: CoroutineScope,
    private val visibleHeight: () -> Int,
) {
    val canPageBackward: Boolean
        get() = state.canScrollBackward

    val canPageForward: Boolean
        get() = state.canScrollForward

    fun page(forward: Boolean) {
        // A little of the last page stays in view to read on from
        val distance = visibleHeight() * PAGE_FRACTION
        scope.launch { state.scrollBy(if (forward) distance else -distance) }
    }
}

/**
 * A pager for [state] in e-ink mode, otherwise null.
 */
@Composable
fun rememberEinkPager(state: LazyListState): EinkPager? = rememberEinkPager(state) {
    with(state.layoutInfo) { viewportSize.height - beforeContentPadding - afterContentPadding }
}

/**
 * A pager for [state] in e-ink mode, otherwise null.
 */
@Composable
fun rememberEinkPager(state: LazyGridState): EinkPager? = rememberEinkPager(state) {
    with(state.layoutInfo) { viewportSize.height - beforeContentPadding - afterContentPadding }
}

/**
 * A pager for [state] in e-ink mode, otherwise null.
 */
@Composable
fun rememberEinkPager(state: ScrollState): EinkPager? = rememberEinkPager(state) { state.viewportSize }

@Composable
private fun rememberEinkPager(state: ScrollableState, visibleHeight: () -> Int): EinkPager? {
    if (!LocalEinkMode.current) return null
    val scope = rememberCoroutineScope()
    return remember(state, scope) { EinkPager(state, scope, visibleHeight) }
}

/**
 * Pages with [pager] once a vertical swipe is lifted. The swipe neither scrolls the list nor clicks what's under
 * it. Does nothing without a pager.
 */
fun Modifier.einkSwipePaging(pager: EinkPager?): Modifier {
    if (pager == null) return this
    return pointerInput(pager) {
        awaitEachGesture {
            // The initial pass sees the swipe before the items do, so consuming it keeps them from clicking
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var isSwiping = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.fastFirstOrNull { it.id == down.id } ?: break
                val moved = change.position - down.position
                if (
                    !isSwiping &&
                    event.changes.size == 1 &&
                    moved.getDistance() > viewConfiguration.touchSlop &&
                    abs(moved.y) > abs(moved.x)
                ) {
                    isSwiping = true
                }
                if (isSwiping) change.consume()
                if (!change.pressed) {
                    if (isSwiping) pager.page(forward = moved.y < 0)
                    break
                }
            }
        }
    }
}

/**
 * These padding values with room at the end for [EinkPageButtons], so they cover nothing of the list.
 */
@Composable
fun PaddingValues.plusEinkPageButtons(pager: EinkPager?): PaddingValues =
    if (pager == null) this else this + PaddingValues(end = PageButtonsWidth)

/**
 * Lays out [content] with [EinkPageButtons] over it when there's a [pager], and as is otherwise.
 */
@Composable
fun EinkPagingBox(pager: EinkPager?, content: @Composable () -> Unit) {
    if (pager == null) {
        content()
        return
    }
    Box(propagateMinConstraints = true) {
        content()
        EinkPageButtons(pager)
    }
}

/**
 * Buttons on the end edge that page with [pager]. It fills its parent to place them, and shows nothing without a
 * pager or anything to page to.
 */
@Composable
fun EinkPageButtons(pager: EinkPager?, modifier: Modifier = Modifier) {
    if (pager == null || !(pager.canPageBackward || pager.canPageForward)) return
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageButton(
                icon = MaterialSymbols.Rounded.KeyboardArrowUp,
                contentDescription = stringResource(MR.strings.eink_page_up),
                enabled = pager.canPageBackward,
                onClick = { pager.page(forward = false) },
            )
            PageButton(
                icon = MaterialSymbols.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(MR.strings.eink_page_down),
                enabled = pager.canPageForward,
                onClick = { pager.page(forward = true) },
            )
        }
    }
}

@Composable
private fun PageButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

private const val PAGE_FRACTION = 0.9f

private val PageButtonsWidth = 48.dp
