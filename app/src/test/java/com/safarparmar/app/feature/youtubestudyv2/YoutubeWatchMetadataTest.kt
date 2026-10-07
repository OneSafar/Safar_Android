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

    @Test fun `merged title uploader and statistics identify the screenshot channels`() {
        for ((videoTitle, handle, stats) in listOf(
            Triple("i tried new bannable wall...", "@GrimGuyLIVE", "712 likes 36K views 10 hr ago more"),
            Triple("WIFE CATCHES HUSBAND CHEATING", "@binks69shorts", "24K likes 3.7 lakh views 3 yr ago more"),
        )) {
            val nodes = watch().mapIndexed { index, item ->
                when (index) {
                    4 -> item.copy(contentDescription = "$videoTitle $handle $stats")
                    in 5..9 -> item.copy(text = null, visibleToUser = false)
                    else -> item
                }
            }
            val result = parse(nodes)
            assertEquals(handle.lowercase(), result.exactHandle)
            assertEquals(videoTitle, result.title)
            assertTrue(result.hasOwnerEvidence)
        }
    }

    @Test fun `empty leading watch item cannot hide nested uploader metadata`() {
        val nodes = watch().toMutableList()
        // Insert an empty first list item without disturbing existing parent indices.
        nodes[4] = node(3, top = 710, bottom = 720)
        nodes += node(3, top = 720, bottom = 910)
        nodes[5] = nodes[5].copy(parentIndex = 14)
        nodes[6] = nodes[6].copy(parentIndex = 14)
        nodes[8] = nodes[8].copy(parentIndex = 14)
        assertEquals("@parmarssc", parse(nodes).exactHandle)
        assertEquals(title, parse(nodes).title)
    }

    @Test fun `control inside watch header cannot exclude the whole owner item`() {
        val nodes = watch() + node(4, "fullscreen_button", text = "Enter fullscreen",
            top = 720, bottom = 780, left = 960).copy(clickable = true)
        val observation = parse(nodes)
        assertEquals(title, observation.title)
        assertEquals("@parmarssc", observation.exactHandle)
    }

    @Test fun `watch header wins over overlapping surface and Like control`() {
        val observation = parse(watch())
        assertEquals(title, observation.title)
        assertEquals("@parmarssc", observation.exactHandle)
    }

    @Test fun `inline more button must not hide the uploader beside it`() {
        val nodes = watch().toMutableList()
        nodes[5] = nodes[5].copy(text = "WIFE CATCHES HUSBAND CHEATING")
        nodes[7] = nodes[7].copy(text = "@binks69shorts")
        nodes[9] = nodes[9].copy(text = "24K likes 3.7 lakh views 3 yr ago")
        nodes += node(4, text = "more", top = 825, bottom = 880, left = 960)
            .copy(clickable = true)
        val observation = parse(nodes)
        assertEquals("@binks69shorts", observation.exactHandle)
        assertEquals("WIFE CATCHES HUSBAND CHEATING", observation.title)
    }

    @Test fun `title only header does not discard the separate semantic owner handle`() {
        val nodes = watch().toMutableList()
        nodes[7] = nodes[7].copy(text = null)
        nodes += node(0, "video_owner", text = "@parmarssc", top = 915, bottom = 935)
        assertEquals("@parmarssc", parse(nodes).exactHandle)
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
