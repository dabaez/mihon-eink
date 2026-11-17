package eu.kanade.tachiyomi.util.system

import android.animation.ValueAnimator
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import tachiyomi.core.common.preference.Preference

/**
 * Makes every Compose animation finish at once while [einkMode] is on, whatever the system's animation scale.
 * Applies to windows created afterwards, so it has to run before any.
 */
@OptIn(InternalComposeUiApi::class)
fun installEinkMotionDurationScale(einkMode: Preference<Boolean>) {
    val scale = object : MotionDurationScale {
        override val scaleFactor: Float
            get() = if (einkMode.get()) 0f else ValueAnimator.getDurationScale()
    }
    WindowRecomposerPolicy.setFactory { rootView -> rootView.createLifecycleAwareWindowRecomposer(scale) }
}
