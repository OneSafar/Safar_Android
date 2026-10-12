package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.*
import org.junit.Test

class YoutubeWatchBrowsingTest {
    private fun node(parent: Int? = 0, text: String? = null, description: String? = null,
        id: String? = null, top: Int, bottom: Int, left: Int = 0, right: Int = 1080,
        clazz: String = "android.view.ViewGroup", clickable: Boolean = false) = YoutubeV2Node(
        text, description, id, clazz, true, clickable, parentIndex = parent,
        left = left, top = top, right = right, bottom = bottom)
    private fun player() = listOf(
        node(null, top = 0, bottom = 2400),
        node(id = "watch_player", top = 100, bottom = 710, clazz = "android.view.SurfaceView"))
    private fun parse(nodes: List<YoutubeV2Node>) = YoutubeStudyV2Parser.parse(
        YoutubeV2Snapshot(YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 2.75f, 1080, 2400, nodes))

    @Test fun `anonymous recommendation channel cannot become the playing owner`() {
        val nodes = player() + listOf(
            node(top = 710, bottom = 1350), // 2 recommendation, no resource IDs
            node(2, top = 710, bottom = 1120, clazz = "android.widget.ImageView"),
            node(2, text = "Recommended entertainment video", top = 1130, bottom = 1200),
            node(2, top = 1210, bottom = 1300, right = 90, clazz = "android.widget.ImageView"),
            node(2, text = "Other Channel", top = 1220, bottom = 1280, left = 110),
            node(2, text = "@otherchannel 100K views 1 day ago", top = 1290, bottom = 1340))
        val result = parse(nodes)
        assertTrue(result.watchScreenConfirmed)
        assertNull(result.title)
        assertFalse(result.hasOwnerEvidence)
    }
    @Test fun `recommendation with offscreen thumbnail cannot supply metadata`() {
        val nodes = player() + listOf(
            node(top = 400, bottom = 950),
            node(2, top = 400, bottom = 700, clazz = "android.widget.ImageView").copy(visibleToUser = false),
            node(2, text = "Recommended entertainment video", top = 720, bottom = 780),
            node(2, text = "@otherchannel 100K views 1 day ago", top = 790, bottom = 850))
        assertNull(parse(nodes).title)
        assertFalse(parse(nodes).hasOwnerEvidence)
    }
    @Test fun `anonymous comments panel cannot supply uploader or video title`() {
        val nodes = player() + listOf(
            node(top = 710, bottom = 2400),
            node(2, text = "Comments", top = 760, bottom = 820),
            node(2, text = "Top", top = 850, bottom = 910),
            node(2, text = "@randomviewer · 1 day ago", top = 960, bottom = 1020),
            node(2, description = "Like this comment along with 23 other people", top = 1060, bottom = 1150,
                right = 520, clickable = true),
            node(6, top = 1080, bottom = 1130, right = 60, clazz = "android.widget.ImageView"))
        val result = parse(nodes)
        assertNull(result.title)
        assertFalse(result.hasOwnerEvidence)
    }
    @Test fun `comment reaction accessibility label is never a channel name`() {
        assertFalse(YoutubeStudyV2Parser.isPlausibleOwnerLabel("Like this comment along with 23 other people"))
        assertFalse(YoutubeStudyV2Parser.isPlausibleOwnerLabel("Dislike this comment"))
    }
    @Test fun `watch uploader remains valid before comments and recommendations`() {
        val nodes = player() + listOf(
            node(text = "Current study lecture", id = "video_title", top = 725, bottom = 780),
            node(text = "@teacher 100K views 1 day ago", top = 790, bottom = 845),
            node(text = "Comments 32", top = 900, bottom = 960),
            node(text = "@randomviewer · 1 day ago", top = 1000, bottom = 1060),
            node(text = "@otherchannel 500K views 1 day ago", top = 1100, bottom = 1160))
        assertEquals("Current study lecture", parse(nodes).title)
        assertEquals("@teacher", parse(nodes).exactHandle)
    }
    @Test fun `comments and watch actions do not arm a new video tap`() {
        assertTrue(isCurrentWatchPageInteraction("comments_entry_point", "View all 32 comments"))
        assertTrue(isCurrentWatchPageInteraction(null, "Comments 32"))
        assertTrue(isCurrentWatchPageInteraction(null, "Like this comment along with 23 other people"))
        assertTrue(isCurrentWatchPageInteraction(null, "Newest"))
        assertFalse(isCurrentWatchPageInteraction("compact_video", "Another lesson - play video"))
        assertFalse(isCurrentWatchPageInteraction("player_control_next", "Next video"))
    }
    @Test fun `scroll and comments retain decision but new tap does not`() {
        val transition = YoutubeFullscreenTransition()
        val current = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true, title = "Study lecture", exactHandle = "@teacher")
        transition.observe(current, 100)
        val hidden = parse(player() + listOf(node(text = "Comments", top = 760, bottom = 820),
            node(text = "@randomviewer · 1 day ago", top = 900, bottom = 960)))
        assertEquals(current.stableKey, transition.observe(hidden, 30_000).stableKey)
        transition.clear()
        assertFalse(transition.observe(hidden, 30_100).hasOwnerEvidence)
    }
}
