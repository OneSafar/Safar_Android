package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.*
import org.junit.Test

class YoutubeWatchMetadataTest {
    private val title = "SSC CGL 2027 | PARMAR'S MATHS FOUNDATION BATCH | COMPLETE STRATEGY"
    private fun node(parent: Int?, id: String? = null, text: String? = null,
        top: Int, bottom: Int, left: Int = 0, right: Int = 1080, clazz: String = "android.view.ViewGroup") =
        YoutubeV2Node(text = text, viewId = id, parentIndex = parent, visibleToUser = true,
            top = top, bottom = bottom, left = left, right = right, className = clazz)

    private fun watch(): List<YoutubeV2Node> = listOf(
        node(null, top = 0, bottom = 2400), // 0
        node(0, "watch_player", top = 100, bottom = 710), // 1
        // The drawing surface can extend behind the watch metadata during a transition.
        node(1, top = 100, bottom = 910, clazz = "android.view.SurfaceView"), // 2
        node(0, "watch_list", top = 710, bottom = 2400), // 3
        node(3, top = 710, bottom = 910), // 4 header
        node(4, text = title, top = 735, bottom = 805), // 5
        node(4, top = 820, bottom = 890, right = 290), // 6 handle wrapper
        node(6, text = "@parmarssc", top = 825, bottom = 880, right = 290), // 7
        node(4, top = 820, bottom = 890, left = 290), // 8 stats wrapper
        node(8, text = "1.1K likes 29K views 6 hr ago", top = 825, bottom = 880, left = 290), // 9
        node(3, top = 915, bottom = 1060), // 10 actions
        node(10, text = "like this video along with 1,119 other people", top = 940, bottom = 1010), // 11
        node(3, "comments", top = 1080, bottom = 1400), // 12
        node(12, text = "@different 100K views 1 day ago", top = 1100, bottom = 1160),
    )
    private fun parse(nodes: List<YoutubeV2Node>) = YoutubeStudyV2Parser.parse(
        YoutubeV2Snapshot(YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 2.75f, 1080, 2400, nodes))

    @Test fun `watch header wins over overlapping surface and Like control`() {
        val observation = parse(watch())
        assertEquals(title, observation.title)
        assertEquals("@parmarssc", observation.exactHandle)
    }

    @Test fun `nested handle and statistics resolve without borrowing comments`() {
        val nodes = watch().toMutableList()
        nodes[2] = nodes[2].copy(bottom = 710)
        assertEquals("@parmarssc", parse(nodes).exactHandle)
    }

    @Test fun `missing owner does not borrow a recommended channel`() {
        val nodes = watch().toMutableList()
        nodes[7] = nodes[7].copy(text = null)
        assertNull(parse(nodes).exactHandle)
    }

    @Test fun `player actions cannot substitute for a missing title`() {
        val nodes = watch().toMutableList()
        nodes[5] = nodes[5].copy(text = null)
        assertNull(parse(nodes).title)
    }

    @Test fun `scrolled anonymous recommendation cannot become the playing owner`() {
        val nodes = listOf(
            node(null, top = 0, bottom = 2400),
            node(0, "watch_player", top = 100, bottom = 710),
            node(0, "watch_list", top = 710, bottom = 2400),
            node(2, top = 710, bottom = 1400),
            node(3, top = 300, bottom = 700, clazz = "android.widget.ImageView").copy(visibleToUser = false),
            node(3, text = "Recommended entertainment video", top = 720, bottom = 790),
            node(3, top = 800, bottom = 1020, right = 220, clazz = "android.widget.ImageView"),
            node(3, text = "Shark Tank India", top = 800, bottom = 920, left = 230),
            node(3, text = "@sharktankindia 100K views 1 day ago", top = 930, bottom = 1000),
        )
        val result = parse(nodes)
        assertTrue(result.watchScreenConfirmed)
        assertNull(result.title)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test fun `description close button is never an owner card`() {
        val nodes = watch().mapIndexed { index, node ->
            if (index >= 4) node.copy(visibleToUser = false) else node
        } + listOf(
            node(0, "engagement_panel", top = 710, bottom = 2400),
            node(14, text = "Close", top = 740, bottom = 880, left = 920).copy(clickable = true),
            node(15, top = 760, bottom = 850, left = 940, clazz = "android.widget.ImageView"),
        )
        assertFalse(parse(nodes).hasOwnerEvidence)
        assertFalse(YoutubeStudyV2Parser.isPlausibleOwnerLabel("Close"))
        assertFalse(YoutubeStudyV2Parser.isPlausibleOwnerLabel("Quote"))
    }
}
