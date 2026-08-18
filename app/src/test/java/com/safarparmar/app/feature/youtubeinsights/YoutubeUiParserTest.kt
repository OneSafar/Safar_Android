package com.safarparmar.app.feature.youtubeinsights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeUiParserTest {
    @Test fun `detects Shorts without persisting a title`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/reel_watch_player"),
            YoutubeUiNode(contentDescription = "Pause"),
        ))
        assertEquals(YoutubeContentKind.SHORTS, result.kind)
        assertTrue(result.isPlaying)
        assertNull(result.channelName)
    }

    @Test fun `extracts channel from stable owner id`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_player"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/channel_name", text = "Khan Academy"),
            YoutubeUiNode(contentDescription = "Pause"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("Khan Academy", result.channelName)
        assertTrue(result.isPlaying)
    }

    @Test fun `play control means paused`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "Play"),
        ))
        assertFalse(result.isPlaying)
    }

    @Test fun `non player screen is not measured`() {
        assertEquals(
            YoutubeContentKind.NON_PLAYBACK,
            YoutubeUiParser.parse(snapshot(YoutubeUiNode(viewId = "search_results"))).kind,
        )
    }

    @Test fun `permanent Shorts navigation label does not classify a normal video as Shorts`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_player"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/pivot_shorts", text = "Shorts"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/channel_name", text = "Khan Academy"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("Khan Academy", result.channelName)
        assertTrue(result.isPlaying)
    }

    @Test fun `selected Shorts destination is treated as Shorts playback`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "pivot_shorts", text = "Shorts", selected = true),
        ))
        assertEquals(YoutubeContentKind.SHORTS, result.kind)
    }

    @Test fun `advertisements are left unidentified`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(viewId = "skip_ad_button", contentDescription = "Skip ad"),
        ))
        assertEquals(YoutubeContentKind.UNKNOWN, result.kind)
        assertFalse(result.isPlaying)
    }

    @Test fun `hidden controls on a player are treated as active playback`() {
        val result = YoutubeUiParser.parse(snapshot(YoutubeUiNode(viewId = "watch_player")))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertTrue(result.isPlaying)
    }

    @Test fun `current youtube subscribe semantics identify channel`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "Subscribe to SAFARPARMAR."),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("SAFARPARMAR", result.channelName)
    }

    @Test fun `official artist accessibility label identifies channel`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "Rick Astley, Official Artist Channel 45.3 lakh subscribers"),
        ))
        assertEquals("Rick Astley", result.channelName)
    }

    @Test fun `go to channel semantics identify owner on current youtube`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "Go to channel Harish Burnwal"),
        ))
        assertEquals("Harish Burnwal", result.channelName)
    }

    @Test fun `subscribed channel semantics identify owner`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "Subscribed to Khan Academy."),
        ))
        assertEquals("Khan Academy", result.channelName)
    }

    @Test fun `clicked video card extracts owner before player opens`() {
        val description = "A useful lesson – 22 minutes – Go to channel Khan Academy – " +
            "Khan Academy - 2 million views - 6 days ago – play video"
        assertEquals("Khan Academy", YoutubeUiParser.channelFromClickedVideo(description))
    }

    @Test fun `non video card cannot prime channel blocking`() {
        assertNull(YoutubeUiParser.channelFromClickedVideo("Go to channel Khan Academy"))
    }

    @Test fun `subscribed handle with subscriber count identifies owner`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "@studywithme, 1.2M subscribers"),
        ))
        assertEquals("@studywithme", result.channelName)
    }

    @Test fun `normalizes handles and whitespace`() {
        assertEquals("khan academy", YoutubeUiParser.normalizeChannel("  @Khan   Academy "))
    }

    private fun snapshot(vararg nodes: YoutubeUiNode) = YoutubeUiSnapshot(
        nodes = nodes.toList(),
        packageName = YoutubeUiParser.YOUTUBE_PACKAGE,
    )
}
