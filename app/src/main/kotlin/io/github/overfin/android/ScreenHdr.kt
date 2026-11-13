package io.github.overfin.android

import android.app.Activity
import android.os.Build
import android.view.Display
import android.webkit.JavascriptInterface

// What the screen shows in HDR, for the site: WebView doesn't say it to
// pages (its "dynamic-range: high" stays false on HDR phones), so the site
// would have the server convert HDR videos the screen shows as they are.
// The page reads it as OverfinApp.hdr(): the names the site's capabilities
// use, as JSON. It says nothing else.
class ScreenHdr(private val activity: Activity) {
    @JavascriptInterface
    fun hdr(): String = names(types()).joinToString(",", "[", "]") { "\"$it\"" }

    @Suppress("DEPRECATION")
    private fun types(): IntArray {
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display else activity.windowManager.defaultDisplay
        display ?: return IntArray(0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) display.mode.supportedHdrTypes
        else display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)
    }

    companion object {
        /** NAME is what the page calls it. */
        const val NAME = "OverfinApp"

        /**
         * names turns Android's HDR types into the site's names. A screen
         * with HDR10+ shows HDR10, its base; Dolby Vision alone names
         * nothing, as the site has no name for it.
         */
        fun names(types: IntArray): List<String> = buildList {
            if (Display.HdrCapabilities.HDR_TYPE_HDR10 in types || Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS in types) add("hdr10")
            if (Display.HdrCapabilities.HDR_TYPE_HLG in types) add("hlg")
        }
    }
}
