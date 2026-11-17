package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Reports a one-finger swipe once it's lifted, so nothing has to move under the finger. A second finger
 * before the swipe starts leaves the gesture alone, for pinch zoom.
 */
class SwipeDetector(context: Context, private val onSwipe: (towards: Direction) -> Unit) {

    /**
     * The side a swipe shows, like dragging would: swiping to the left shows what's on the right.
     */
    enum class Direction { LEFT, UP, RIGHT, DOWN }

    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var start: PointF? = null

    /**
     * Whether the current gesture is a swipe, which the views under it mustn't handle anymore.
     */
    var isSwiping = false
        private set

    fun onTouchEvent(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                start = PointF(ev.x, ev.y)
                isSwiping = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> if (!isSwiping) start = null
            MotionEvent.ACTION_MOVE -> {
                val start = start ?: return
                if (!isSwiping && hypot(ev.x - start.x, ev.y - start.y) > slop) isSwiping = true
            }
            MotionEvent.ACTION_UP -> {
                val start = start
                if (isSwiping && start != null) onSwipe(direction(ev.x - start.x, ev.y - start.y))
                reset()
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
    }

    private fun reset() {
        start = null
        isSwiping = false
    }

    private fun direction(dx: Float, dy: Float) = if (abs(dx) > abs(dy)) {
        if (dx < 0) Direction.RIGHT else Direction.LEFT
    } else {
        if (dy < 0) Direction.DOWN else Direction.UP
    }
}
