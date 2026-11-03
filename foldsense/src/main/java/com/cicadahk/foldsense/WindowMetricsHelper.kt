package com.cicadahk.foldsense

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Optional API 30+ helpers using [android.view.WindowMetrics] — still **no**
 * androidx.window dependency.
 */
object WindowMetricsHelper {

    /**
     * Returns whether the current window bounds are wider than tall.
     * Useful as a secondary hint on some foldables in tabletop / open posture;
     * product logic should combine this with [FoldSense.isLikelyUnfolded].
     */
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.R)
    fun isLandscapeBounds(activity: Activity): Boolean {
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        return bounds.width() > bounds.height()
    }

    /**
     * Current window width in px, or `-1` if API &lt; 30.
     */
    @JvmStatic
    fun currentWidthPx(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.width
        }
    }

    /**
     * Current window height in px, or display height on older APIs.
     */
    @JvmStatic
    fun currentHeightPx(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.height
        }
    }
}
