package com.cicadahk.foldsense.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cicadahk.foldsense.FoldSense
import com.cicadahk.foldsense.WindowMetricsHelper

class MainActivity : AppCompatActivity() {

    private var foldSense: FoldSense? = null
    private lateinit var statusView: TextView
    private lateinit var detailsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusView = findViewById(R.id.status)
        detailsView = findViewById(R.id.details)
        refreshDetails("idle")
    }

    override fun onStart() {
        super.onStart()
        foldSense = FoldSense.attach(this).addListener { unfolded, swDp, state ->
            runOnUiThread {
                statusView.text = if (unfolded) {
                    "Likely UNFOLDED ($state)"
                } else {
                    "Likely FOLDED ($state)"
                }
                refreshDetails("transition swDp=$swDp")
            }
        }
        refreshDetails("listening")
    }

    override fun onStop() {
        foldSense?.detach()
        foldSense = null
        super.onStop()
    }

    private fun refreshDetails(label: String) {
        val sense = foldSense
        val sw = sense?.currentSmallestWidthDp()
            ?: resources.configuration.smallestScreenWidthDp
        val w = WindowMetricsHelper.currentWidthPx(this)
        val h = WindowMetricsHelper.currentHeightPx(this)
        detailsView.text = buildString {
            appendLine("event: $label")
            appendLine("smallestWidthDp: $sw")
            appendLine("state: ${sense?.currentState() ?: "—"}")
            appendLine("likelyUnfolded: ${sense?.isLikelyUnfolded() == true}")
            appendLine("windowPx: ${w}x${h}")
            appendLine("landscapeBounds: ${FoldSense.isLandscapeBoundsOrFalse(this@MainActivity)}")
            appendLine("FoldSense ${FoldSense.VERSION}")
        }
    }
}
