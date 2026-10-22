package com.safarparmar.app.feature.youtubestudyv2

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeDetectionReportTest {
    @Test fun `shared report preserves the tree for parser replay`() {
        val snapshot = YoutubeV2Snapshot(YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 2f, 720, 1600, listOf(
            YoutubeV2Node(viewId = "watch_player", visibleToUser = true, right = 720, bottom = 480),
            YoutubeV2Node(text = "@PrimeVideoIN", contentDescription = "Title \"हिन्दी\"\n97K likes",
                parentIndex = 0, visibleToUser = true, top = 500, right = 720, bottom = 600),
        ))
        val parsed = YoutubeStudyV2Parser.parse(snapshot)
        val report = YoutubeDetectionReport(appVersion = "test", appVersionCode = 1,
            capturedAtEpochMs = 123, nodeLimitReached = false, snapshot = snapshot,
            parsed = parsed, effective = parsed)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val restored = gson.fromJson(gson.toJson(report), YoutubeDetectionReport::class.java)
        assertEquals(report, restored)
        assertEquals(parsed, YoutubeStudyV2Parser.parse(restored.snapshot))
    }
}
