package io.github.overfin.android

import android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION
import android.view.Display.HdrCapabilities.HDR_TYPE_HDR10
import android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS
import android.view.Display.HdrCapabilities.HDR_TYPE_HLG
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenHdrTest {
    @Test
    fun names() {
        assertEquals(listOf<String>(), ScreenHdr.names(intArrayOf()))
        assertEquals(listOf("hdr10", "hlg"), ScreenHdr.names(intArrayOf(HDR_TYPE_HDR10, HDR_TYPE_HLG, HDR_TYPE_HDR10_PLUS)))
        assertEquals(listOf("hdr10"), ScreenHdr.names(intArrayOf(HDR_TYPE_HDR10_PLUS)))
        assertEquals(listOf<String>(), ScreenHdr.names(intArrayOf(HDR_TYPE_DOLBY_VISION)))
    }
}
