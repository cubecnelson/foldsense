package com.cicadahk.foldsense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pure JVM tests for [FoldSense.detectTransition] (no Robolectric).
 */
class FoldSenseDetectTest {

    @Test
    fun increaseMeansUnfold() {
        assertEquals(true, FoldSense.detectTransition(previousSwDp = 320, currentSwDp = 600))
    }

    @Test
    fun decreaseMeansFold() {
        assertEquals(false, FoldSense.detectTransition(previousSwDp = 600, currentSwDp = 320))
    }

    @Test
    fun unchangedIsNull() {
        assertNull(FoldSense.detectTransition(previousSwDp = 400, currentSwDp = 400))
    }

    @Test
    fun invalidPreviousIsNull() {
        assertNull(FoldSense.detectTransition(previousSwDp = 0, currentSwDp = 400))
        assertNull(FoldSense.detectTransition(previousSwDp = -1, currentSwDp = 400))
    }
}
