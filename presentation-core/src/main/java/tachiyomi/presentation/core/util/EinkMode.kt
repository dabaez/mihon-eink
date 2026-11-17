package tachiyomi.presentation.core.util

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the app is in e-ink mode, where nothing should animate or scroll.
 */
val LocalEinkMode = compositionLocalOf { false }
