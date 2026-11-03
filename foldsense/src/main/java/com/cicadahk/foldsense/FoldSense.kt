package com.cicadahk.foldsense

import android.app.Activity
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Build
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Detects foldable unfold / fold via [Configuration.smallestScreenWidthDp]
 * (and optional API 30+ [WindowMetricsHelper]) **without** androidx.window.
 *
 * Typical usage from an Activity or Fragment (`requireActivity()`):
 *
 * ```
 * private var foldSense: FoldSense? = null
 *
 * override fun onStart() {
 *     super.onStart()
 *     foldSense = FoldSense.attach(this).also { sense ->
 *         sense.addListener { unfolded, swDp, state ->
 *             // handle layout switch
 *         }
 *     }
 * }
 *
 * override fun onStop() {
 *     foldSense?.detach()
 *     foldSense = null
 *     super.onStop()
 * }
 * ```
 *
 * Manifest tip (optional): if your Activity declares
 * `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout"`,
 * still register FoldSense — it uses [Activity.registerComponentCallbacks] and
 * does not require you to forward [Activity.onConfigurationChanged].
 */
class FoldSense private constructor(
    activity: Activity
) : ComponentCallbacks {

    private val activityRef = WeakReference(activity)
    private val listeners = CopyOnWriteArraySet<FoldListener>()

    @Volatile
    private var lastSmallestWidthDp: Int = -1

    @Volatile
    private var state: FoldState = FoldState.UNKNOWN

    @Volatile
    private var attached: Boolean = false

    /** Current coarse [FoldState]. */
    fun currentState(): FoldState = state

    /** Current [Configuration.smallestScreenWidthDp], or `-1` if detached. */
    fun currentSmallestWidthDp(): Int {
        val activity = activityRef.get() ?: return lastSmallestWidthDp
        return activity.resources.configuration.smallestScreenWidthDp
    }

    /**
     * `true` when the last observed transition increased smallest width,
     * or when [state] is [FoldState.UNFOLDED].
     */
    fun isLikelyUnfolded(): Boolean = state == FoldState.UNFOLDED

    fun addListener(listener: FoldListener): FoldSense {
        listeners.add(listener)
        return this
    }

    fun removeListener(listener: FoldListener): FoldSense {
        listeners.remove(listener)
        return this
    }

    /**
     * Start listening. Safe to call twice (no-op if already attached).
     */
    fun attach(): FoldSense {
        val activity = activityRef.get() ?: return this
        if (attached) return this
        lastSmallestWidthDp = activity.resources.configuration.smallestScreenWidthDp
        state = FoldState.UNKNOWN
        activity.registerComponentCallbacks(this)
        attached = true
        return this
    }

    /**
     * Stop listening and clear listeners. Call from `onStop` / `onDestroy`.
     */
    fun detach() {
        if (!attached) {
            listeners.clear()
            return
        }
        activityRef.get()?.unregisterComponentCallbacks(this)
        attached = false
        listeners.clear()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val sw = newConfig.smallestScreenWidthDp
        val previous = lastSmallestWidthDp
        if (previous > 0 && sw != previous) {
            val unfolded = sw > previous
            state = if (unfolded) FoldState.UNFOLDED else FoldState.FOLDED
            lastSmallestWidthDp = sw
            listeners.forEach { listener ->
                listener.onFoldChanged(unfolded, sw, state)
            }
        } else {
            lastSmallestWidthDp = sw
        }
    }

    override fun onLowMemory() {
        // no-op
    }

    companion object {
        const val VERSION = "1.0.0"

        /**
         * Creates a [FoldSense] bound to [activity] and immediately [attach]s it.
         */
        @JvmStatic
        fun attach(activity: Activity): FoldSense {
            return FoldSense(activity).attach()
        }

        /**
         * Snapshot helper: compare current SW dp against a previously stored value.
         * Returns `null` if unchanged or [previousSwDp] &lt;= 0.
         */
        @JvmStatic
        fun detectTransition(previousSwDp: Int, currentSwDp: Int): Boolean? {
            if (previousSwDp <= 0 || currentSwDp == previousSwDp) return null
            return currentSwDp > previousSwDp
        }

        /**
         * API 30+ landscape-bounds hint; `false` on older APIs.
         */
        @JvmStatic
        fun isLandscapeBoundsOrFalse(activity: Activity): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetricsHelper.isLandscapeBounds(activity)
            } else {
                false
            }
        }
    }
}
