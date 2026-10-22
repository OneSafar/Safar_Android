package com.safarparmar.app.feature.youtubestudyv2

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class YoutubeDeviceReportReplayTest {
    private fun snapshot(name: String): YoutubeV2Snapshot =
        requireNotNull(javaClass.getResourceAsStream("/youtube/$name.json")).reader().use {
            Gson().fromJson(it, YoutubeV2Snapshot::class.java)
        }

    @Test fun `EZVAL device report identifies the playing uploader`() {
        val result = YoutubeStudyV2Parser.parse(snapshot("ezval-watch"))
        assertEquals("@ezval789", result.exactHandle)
        assertEquals("20 Minutes of BEST Plays from VCT Champions Shanghai 2026 (Group Stage)", result.title)
        assertTrue(result.canResumeWatchSession)
        assertEquals(YoutubeFullscreenSidePanel.NONE, result.fullscreenSidePanel)
    }

    @Test fun `Mylan device report identifies the playing uploader`() {
        val result = YoutubeStudyV2Parser.parse(snapshot("mylan-watch"))
        assertEquals("@mylanval", result.exactHandle)
        assertEquals("FNS Watch Party Moments, But The Aura Loss Is Immeasurable", result.title)
        assertTrue(result.canResumeWatchSession)
        assertEquals(YoutubeFullscreenSidePanel.NONE, result.fullscreenSidePanel)
    }
    @Test fun `open panel inside the wrapper still cannot supply the uploader`() {
        val original = snapshot("mylan-watch")
        val list = original.nodes.indexOfFirst { it.viewId?.endsWith("/watch_list") == true }
        val wrapper = original.nodes.indexOfFirst { it.viewId?.endsWith("/engagement_panel_wrapper") == true }
        val hiddenWatchList = original.nodes.mapIndexed { index, node ->
            var ancestor: Int? = index
            var inWatchList = false
            while (ancestor != null) {
                if (ancestor == list) inWatchList = true
                ancestor = original.nodes[ancestor].parentIndex
            }
            if (inWatchList) node.copy(visibleToUser = false) else node
        }
        val panel = YoutubeV2Node(viewId = "engagement_panel", parentIndex = wrapper,
            visibleToUser = true, top = 718, right = 1080, bottom = 2352)
        val comment = YoutubeV2Node(text = "@commenter 100K views 1 day ago",
            parentIndex = hiddenWatchList.size, visibleToUser = true,
            top = 800, right = 1080, bottom = 900)
        val result = YoutubeStudyV2Parser.parse(original.copy(nodes = hiddenWatchList + panel + comment))
        assertNull(result.exactHandle)
        assertFalse(result.hasOwnerEvidence)
    }

}
