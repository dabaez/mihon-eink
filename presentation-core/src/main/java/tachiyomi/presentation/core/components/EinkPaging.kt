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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
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

    val canPage: Boolean
        get() = canPageBackward || canPageForward

    fun page(forward: Boolean) {
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
 * In e-ink mode, a swipe over anything scrollable inside this jumps it by a page once the swipe is lifted, instead of
 * scrolling it under the finger, and clicks nothing. Best at the root of a window, and a dialog is a window of its own.
 * Scrollables are found through the semantics they have for accessibility, so this covers all of them.
 */
@Composable
fun Modifier.einkSwipePaging(): Modifier {
    if (!LocalEinkMode.current) return this
    val root = LocalView.current as? RootForTest ?: return this
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val placement = remember { Placement() }
    return onPlaced { placement.coordinates = it }
        .pointerInput(root, isRtl) {
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val coordinates = placement.coordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                    val position = coordinates.localToRoot(down.position)
                    val targets by lazy(LazyThreadSafetyMode.NONE) {
                        root.semanticsOwner.unmergedRootSemanticsNode.swipeTargetsAt(position)
                    }
                    val moved = awaitSwipe(
                        down = down,
                        takes = { vertical ->
                            targets.none { it.ownsSwipes() } && targets.any { it.isScrollable(vertical) }
                        },
                        // Even a swipe across a list mustn't click the item it ends on
                        takesUnclaimed = { targets.none { it.ownsSwipes() } && targets.isNotEmpty() },
                    ) ?: return@awaitEachGesture

                    val vertical = abs(moved.y) > abs(moved.x)
                    // Swiping up or left shows what's below or to the right, as dragging would
                    val forward = if (vertical) moved.y < 0 else moved.x < 0
                    launch { targets.asReversed().firstOrNull { it.page(vertical, forward, isRtl) } }
                }
            }
        }
}

/**
 * Keeps [einkSwipePaging] from taking swipes that start on this, for a control that drags in its own way.
 */
fun Modifier.ownsEinkSwipes(): Modifier = semantics { this[OwnsSwipes] = Unit }

/**
 * In e-ink mode, calls [onSwipe] once a sideways swipe is lifted, for swipe actions that mustn't slide. [onSwipe] is
 * told whether the finger moved towards the end, which is right in left-to-right layouts.
 */
@Composable
fun Modifier.onEinkSideSwipe(onSwipe: (towardsEnd: Boolean) -> Unit): Modifier {
    if (!LocalEinkMode.current) return this
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    return pointerInput(isRtl) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val moved = awaitSwipe(down, takes = { vertical -> !vertical }) ?: return@awaitEachGesture
            if (abs(moved.x) > abs(moved.y)) currentOnSwipe((moved.x > 0) != isRtl)
        }
    }
}

/**
 * Follows [down]'s gesture to its end and takes it once it moves past the touch slop along an axis [takes] accepts,
 * or once it's well past it with nothing else having taken it and [takesUnclaimed]. A taken gesture is consumed from
 * then on, so nothing under it scrolls or clicks. Returns how far a taken gesture moved, or null for any other: a
 * tap, a pinch, or a drag something else took first.
 */
private suspend fun AwaitPointerEventScope.awaitSwipe(
    down: PointerInputChange,
    takes: (vertical: Boolean) -> Boolean,
    takesUnclaimed: () -> Boolean = { false },
): Offset? {
    var taken = false
    var previous: PointerInputChange? = null
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.fastFirstOrNull { it.id == down.id } ?: return null
        val moved = change.position - down.position
        if (!taken) {
            if (event.changes.size > 1 || change.isConsumed || previous?.isConsumed == true) return null
            previous = change
            val slop = viewConfiguration.touchSlop
            if (moved.getDistance() > slop) {
                val vertical = abs(moved.y) > abs(moved.x)
                // Past the slop on the other axis too, a scrollable along that one would start dragging
                val across = if (vertical) abs(moved.x) else abs(moved.y)
                // Twice the slop leaves time for a slider, say, to take it first
                taken = takes(vertical) ||
                    (across > slop && takes(!vertical)) ||
                    (moved.getDistance() > slop * 2 && takesUnclaimed())
            }
        }
        if (taken) change.consume()
        if (!change.pressed) return if (taken) moved else null
    }
}

private class Placement {
    var coordinates: LayoutCoordinates? = null
}

private val OwnsSwipes = SemanticsPropertyKey<Unit>("OwnsEinkSwipes")

/**
 * The nodes under [position] that scroll or own swipes, outermost first.
 */
private fun SemanticsNode.swipeTargetsAt(position: Offset): List<SemanticsNode> {
    val targets = mutableListOf<SemanticsNode>()
    fun visit(node: SemanticsNode) {
        // Like pages a pager isn't showing, which keep where they last were
        if (!node.layoutInfo.isPlaced) return
        val config = node.config
        val isTarget = node.ownsSwipes() ||
            SemanticsProperties.VerticalScrollAxisRange in config ||
            SemanticsProperties.HorizontalScrollAxisRange in config
        if (isTarget && node.boundsInRoot.contains(position)) targets += node
        node.children.fastForEach(::visit)
    }
    visit(this)
    return targets
}

// Sliders drag their own way too
private fun SemanticsNode.ownsSwipes(): Boolean = OwnsSwipes in config || SemanticsActions.SetProgress in config

private fun SemanticsNode.isScrollable(vertical: Boolean): Boolean =
    scrollAxisRange(vertical) in config && SemanticsActions.ScrollByOffset in config

private fun scrollAxisRange(vertical: Boolean) =
    if (vertical) SemanticsProperties.VerticalScrollAxisRange else SemanticsProperties.HorizontalScrollAxisRange

/**
 * Pages this towards what's below or to the right when [forward], and returns whether it could. The amount's sign
 * follows what accessibility services do.
 */
private suspend fun SemanticsNode.page(vertical: Boolean, forward: Boolean, isRtl: Boolean): Boolean {
    if (!isScrollable(vertical)) return false
    val pageAction = when {
        vertical -> if (forward) SemanticsActions.PageDown else SemanticsActions.PageUp
        else -> if (forward) SemanticsActions.PageRight else SemanticsActions.PageLeft
    }
    // Pagers turn a whole page
    config.getOrNull(pageAction)?.action?.let { return it() }

    val viewport = config.getOrNull(SemanticsActions.GetScrollViewportLength)?.action
        ?.let { getLength -> mutableListOf<Float>().takeIf(getLength)?.firstOrNull() }
        ?: (if (vertical) size.height else size.width).toFloat()
    var amount = viewport * PAGE_FRACTION
    val range = config[scrollAxisRange(vertical)]
    if (!forward) amount = -amount
    if (range.reverseScrolling) amount = -amount
    if (isRtl && !vertical) amount = -amount
    val canScroll = if (amount > 0) range.value() < range.maxValue() else range.value() > 0
    if (!canScroll) return false
    val scrollBy = config.getOrNull(SemanticsActions.ScrollByOffset) ?: return false
    return scrollBy(if (vertical) Offset(0f, amount) else Offset(amount, 0f)) != Offset.Zero
}

/**
 * These padding values with room at the end for [EinkPageButtons] when they show, so they cover nothing of the list.
 */
@Composable
fun PaddingValues.plusEinkPageButtons(pager: EinkPager?): PaddingValues =
    if (pager?.canPage == true) this + PaddingValues(end = PageButtonsWidth) else this

/**
 * Lets this item of a list padded by [plusEinkPageButtons] run on under the buttons, for a backdrop that would look cut
 * off otherwise.
 */
fun Modifier.underEinkPageButtons(pager: EinkPager?): Modifier {
    if (pager == null) return this
    return layout { measurable, constraints ->
        val room = if (pager.canPage) PageButtonsWidth.roundToPx() else 0
        val placeable = measurable.measure(
            constraints.copy(minWidth = constraints.minWidth + room, maxWidth = constraints.maxWidth + room),
        )
        layout(constraints.maxWidth, placeable.height) { placeable.placeRelative(0, 0) }
    }
}

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
    if (pager?.canPage != true) return
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

// A little of the last page stays in view to read on from
private const val PAGE_FRACTION = 0.9f

private val PageButtonsWidth = 48.dp
