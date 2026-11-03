package com.cicadahk.foldsense

/**
 * Coarse fold posture inferred from screen configuration changes.
 *
 * This is **not** hinge-angle / [androidx.window] FoldingFeature — it tracks
 * [android.content.res.Configuration.smallestScreenWidthDp] deltas, which is
 * compatible with locked dependency catalogs that cannot add androidx.window.
 */
enum class FoldState {
    /** Device appears folded / compact (or posture unknown / unchanged). */
    FOLDED,

    /** Device appears unfolded / expanded (smallest width grew vs baseline). */
    UNFOLDED,

    /** Not enough samples yet to decide. */
    UNKNOWN
}
