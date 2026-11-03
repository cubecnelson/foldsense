package com.cicadahk.foldsense

/**
 * Callback when FoldSense detects a likely fold / unfold transition.
 */
fun interface FoldListener {
    /**
     * @param unfolded `true` when [smallestWidthDp] increased vs the previous value
     *                 (typical unfold on foldables); `false` when it decreased.
     * @param smallestWidthDp current [android.content.res.Configuration.smallestScreenWidthDp]
     * @param state coarse [FoldState] after this change
     */
    fun onFoldChanged(unfolded: Boolean, smallestWidthDp: Int, state: FoldState)
}
