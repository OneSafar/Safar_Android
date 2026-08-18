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

    @Test fun `by inside a video title is never parsed as the channel`() {
        val description = "GK strategy by SSC for 2026 – 12 minutes – 20K views – play video"
        assertTrue(YoutubeUiParser.isClickedVideoCard(description))
        assertNull(YoutubeUiParser.channelFromClickedVideo(description))
    }

    @Test fun `current youtube card extracts channel immediately before view metadata`() {
        val description = "SLAM SUMMER CLASSIC VOL 8 – 1 hour, 33 minutes –  – " +
            "NBA - 30K views - Streamed 15 hours ago – play video"
        assertEquals("NBA", YoutubeUiParser.channelFromClickedVideo(description))
    }

    @Test fun `scheduled card extracts channel without confusing its title`() {
        val description = "SSC EXAMS 2026 NITTO SERIES –  –  – " +
            "PARMAR SSC - Scheduled for 19/08/26, 9:30 - Upcoming – play video"
        assertEquals("PARMAR SSC", YoutubeUiParser.channelFromClickedVideo(description))
    }

    @Test fun `video title stored in ambiguous channel title id is ignored`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(viewId = "channel_title", text = "GK strategy SSC 2026"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertNull(result.channelName)
    }

    @Test fun `subscribed handle with subscriber count identifies owner`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "@studywithme, 1.2M subscribers"),
        ))
        assertEquals("@studywithme", result.channelName)
    }

    @Test fun `watch metadata handle with likes identifies owner`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "@NBA 560 likes 30K views 17 hr ago"),
        ))
        assertEquals("@NBA", result.channelName)
    }

    @Test fun `scheduled player shell is non playback`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(viewId = "playerless_thumbnail"),
            YoutubeUiNode(contentDescription = "Notify me"),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertFalse(result.isPlaying)
    }

    @Test fun `hidden playerless thumbnail does not suppress normal playback`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(viewId = "playerless_thumbnail"),
            YoutubeUiNode(contentDescription = "@NBA 560 likes 30K views 17 hr ago"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("@NBA", result.channelName)
        assertTrue(result.isPlaying)
    }

    @Test fun `normalizes handles and whitespace`() {
        assertEquals("khan academy", YoutubeUiParser.normalizeChannel("  @Khan   Academy "))
    }

    @Test fun `home feed with thumbnail cards is non playback and extracts no channel`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/pivot_home", text = "Home", selected = true),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/results_list"),
            YoutubeUiNode(contentDescription = "Amazing Video 1 – 15 minutes – Go to channel DistractingChannel1 – 2M views – play video"),
            YoutubeUiNode(contentDescription = "Amazing Video 2 – 8 minutes – Go to channel DistractingChannel2 – 500K views – play video"),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertNull(result.channelName)
    }

    @Test fun `search results with inline preview does not misidentify feed as active watch screen`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/search_results"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/player_view"),
            YoutubeUiNode(contentDescription = "Search Result – Go to channel SomeChannel – play video"),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertNull(result.channelName)
    }

    @Test fun `search results with collapsed watch panel are still browsing`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/search_results"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_panel"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/channel_name", text = "Distracting Channel"),
            YoutubeUiNode(contentDescription = "Pause"),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertNull(result.channelName)
        assertFalse(result.isPlaying)
    }

    @Test fun `selected home with collapsed watch layout is still browsing`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/pivot_home", text = "Home", selected = true),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_layout"),
            YoutubeUiNode(contentDescription = "Subscribe to Distracting Channel."),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertNull(result.channelName)
    }

    @Test fun `modern miniplayer overrides retained definitive watch player`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "browse_fragment_layout_coordinator_layout"),
            YoutubeUiNode(viewId = "results"),
            YoutubeUiNode(viewId = "modern_miniplayer_subtitle_text", text = "NBA"),
            YoutubeUiNode(viewId = "watch_player"),
            YoutubeUiNode(contentDescription = "@NBA 560 likes 30K views"),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertNull(result.channelName)
    }

    @Test fun `watch screen ignores related video cards and extracts active channel`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_player"),
            YoutubeUiNode(contentDescription = "Subscribe to PhysicsWallah."),
            // Related / Up next recommendations in the list below:
            YoutubeUiNode(contentDescription = "Distracting gaming video – 10 minutes – Go to channel GamingChannel – play video"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("PhysicsWallah", result.channelName)
        assertTrue(result.isPlaying)
    }

    @Test fun `clicked search result card for PARMAR SSC extracts owner accurately`() {
        val description = "SSC STENO 2026 | LAST DAYS GK STRATEGY – 11 minutes, 50 seconds – " +
            "Go to channel PARMAR SSC – 31K views – 7 hours ago – play video"
        assertEquals("PARMAR SSC", YoutubeUiParser.channelFromClickedVideo(description))
    }

    @Test fun `section headers like Channels that you watch are never extracted as channels`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(contentDescription = "Channels that you watch"),
            YoutubeUiNode(contentDescription = "Channels from your search"),
        ))
        assertEquals(YoutubeContentKind.NON_PLAYBACK, result.kind)
        assertNull(result.channelName)
    }

    @Test fun `watch screen with PARMAR SSC subscribed button extracts active channel`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_player"),
            YoutubeUiNode(contentDescription = "Subscribed to PARMAR SSC."),
            YoutubeUiNode(contentDescription = "Go to channel PARMAR CLIPS – play video"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("PARMAR SSC", result.channelName)
        assertTrue(result.isPlaying)
    }

    @Test fun `watch screen extracts handle and strictly ignores recommended video channels below comments`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_player"),
            YoutubeUiNode(text = "SSC CGL 2026 | BLITZ SERIES REAS..."),
            YoutubeUiNode(text = "@parmarssc 250 likes 4,437 views 7 hr ago 1 pro... ...more"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/subscribe_button", text = "Subscribe"),
            YoutubeUiNode(text = "Comments 75"),
            // Recommended video below comments:
            YoutubeUiNode(text = "50x Icon Picks/Packs Decides My Team!"),
            YoutubeUiNode(viewId = "com.google.android.youtube:id/channel_name", text = "BorasLegend • 85 watching"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertEquals("@parmarssc", result.channelName)
        assertTrue(result.isPlaying)
    }

    @Test fun `watch screen with no active channel in header does not fallback to recommended videos below comments`() {
        val result = YoutubeUiParser.parse(snapshot(
            YoutubeUiNode(viewId = "com.google.android.youtube:id/watch_player"),
            YoutubeUiNode(text = "Some Video Title"),
            YoutubeUiNode(text = "Comments 120"),
            // Recommended video below comments:
            YoutubeUiNode(viewId = "com.google.android.youtube:id/channel_name", text = "DistractingChannel"),
        ))
        assertEquals(YoutubeContentKind.VIDEO, result.kind)
        assertNull(result.channelName) // Must be null, NOT DistractingChannel!
        assertTrue(result.isPlaying)
    }

    private fun snapshot(vararg nodes: YoutubeUiNode) = YoutubeUiSnapshot(
        nodes = nodes.toList(),
        packageName = YoutubeUiParser.YOUTUBE_PACKAGE,
    )
}
