package com.safarparmar.app.feature.youtube.detector

import android.view.accessibility.AccessibilityNodeInfo
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.DetectionConfidence
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adversarial stress test suite for Milestone 3 structural subtree parsing,
 * genuine handle enforcement, and Shorts evasion detection.
 *
 * Authored by: Milestone 3 Challenger 1
 */
class YouTubeUiParserChallengerTest {

    // ===========================================================================
    // 1. SUBTREE POLLUTION ATTACKS
    // ===========================================================================

    @Test
    fun `subtree pollution - deeply nested recommendations with deceptive titles and handles are isolated`() {
        // Deeply nested tree:
        // root -> watch_player
        //      -> watch_metadata (Title: "Clean Energy", Channel: "Physics Girl", Handle: "@physicsgirl")
        //      -> watch_next
        //           -> related_videos (depth 1)
        //                -> results_list (depth 2)
        //                     -> compact_video_renderer (depth 3)
        //                          -> item_container (depth 4)
        //                               -> video_title: "DECEPTIVE: Hack Your Brain" (depth 5)
        //                               -> channel_name: "BrainHacker" (depth 5)
        //                               -> text: "@brainhacker" (depth 5)
        //                               -> text: "@nested_spam_handle" (depth 5)

        val deceptiveTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "DECEPTIVE: Hack Your Brain",
        )
        val deceptiveChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "BrainHacker",
        )
        val deceptiveHandle1 = createNode(
            text = "@brainhacker",
        )
        val deceptiveHandle2 = createNode(
            text = "@nested_spam_handle",
        )
        val itemContainer = createNode(
            children = listOf(deceptiveTitle, deceptiveChannel, deceptiveHandle1, deceptiveHandle2),
        )
        val compactRenderer = createNode(
            viewId = "com.google.android.youtube:id/compact_video_renderer",
            children = listOf(itemContainer),
        )
        val resultsList = createNode(
            viewId = "com.google.android.youtube:id/results_list",
            children = listOf(compactRenderer),
        )
        val relatedVideos = createNode(
            viewId = "com.google.android.youtube:id/related_videos",
            children = listOf(resultsList),
        )
        val watchNext = createNode(
            viewId = "com.google.android.youtube:id/watch_next",
            children = listOf(relatedVideos),
        )

        // Legitimate watch metadata
        val validTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Clean Energy Revolution",
        )
        val validChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "Physics Girl",
        )
        val validHandle = createNode(
            text = "@physicsgirl",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(validTitle, validChannel, validHandle),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        val root = createNode(
            children = listOf(player, watchMetadata, watchNext),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertEquals("Clean Energy Revolution", parsed.title)
        assertEquals("Physics Girl", parsed.channelName)
        assertEquals("@physicsgirl", parsed.channelHandle)
        assertEquals("@physicsgirl", parsed.channelIdentity.verifiedHandle)
        assertEquals("Physics Girl", parsed.channelIdentity.displayName)
        assertEquals("handle:@physicsgirl", parsed.channelIdentity.canonicalKey)

        // Verify deceptive nodes did NOT pollute
        assertNotEquals("DECEPTIVE: Hack Your Brain", parsed.title)
        assertNotEquals("BrainHacker", parsed.channelName)
        assertNotEquals("@brainhacker", parsed.channelHandle)
        assertNotEquals("@nested_spam_handle", parsed.channelHandle)
    }

    @Test
    fun `subtree pollution - comments thread with author mentions and replies is isolated`() {
        // Comments tree:
        // root -> watch_player
        //      -> watch_metadata (Title: "World History", Channel: "CrashCourse", no handle in metadata)
        //      -> comments_entry_point_layout
        //           -> comments_container
        //                -> comments_list
        //                     -> comment_thread (10 levels deep)
        //                          -> comment_item
        //                               -> author: "@history_hater"
        //                               -> text: "Hey @celebrity you are wrong"
        //                               -> comment_replies
        //                                    -> author: "@toxic_troll"
        //                                    -> text: "agree with @history_hater"

        val authorReply = createNode(
            viewId = "com.google.android.youtube:id/author",
            text = "@toxic_troll",
        )
        val textReply = createNode(
            text = "agree with @history_hater",
        )
        val commentReplies = createNode(
            viewId = "com.google.android.youtube:id/comment_replies",
            children = listOf(authorReply, textReply),
        )

        val commentAuthor = createNode(
            viewId = "com.google.android.youtube:id/author",
            text = "@history_hater",
        )
        val commentText = createNode(
            text = "Hey @celebrity you are wrong",
        )
        val commentItem = createNode(
            viewId = "com.google.android.youtube:id/comment_item",
            children = listOf(commentAuthor, commentText, commentReplies),
        )
        val commentThread = createNode(
            viewId = "com.google.android.youtube:id/comment_thread",
            children = listOf(commentItem),
        )
        val commentsList = createNode(
            viewId = "com.google.android.youtube:id/comments_list",
            children = listOf(commentThread),
        )
        val commentsContainer = createNode(
            viewId = "com.google.android.youtube:id/comments_container",
            children = listOf(commentsList),
        )
        val commentsEntryPoint = createNode(
            viewId = "com.google.android.youtube:id/comments_entry_point_layout",
            children = listOf(commentsContainer),
        )

        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "World History 101",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "CrashCourse",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        val root = createNode(
            children = listOf(player, watchMetadata, commentsEntryPoint),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertEquals("World History 101", parsed.title)
        assertEquals("CrashCourse", parsed.channelName)

        // Strict: verifiedHandle must be NULL because no real handle in watch_metadata
        assertNull("Comments author handles MUST NOT pollute verifiedHandle", parsed.channelIdentity.verifiedHandle)
        assertEquals("CrashCourse", parsed.channelIdentity.displayName)
        assertEquals("name:crashcourse", parsed.channelIdentity.canonicalKey)
        assertEquals("", parsed.channelHandle)
    }

    @Test
    fun `subtree pollution - fallback watch subtree without watch_metadata ID still excludes suggestions and comments`() {
        // Obfuscated YouTube layout where watch_metadata container ID is absent,
        // but watch signals (subscribe button with content description, video_title) are present.
        // Suggestions and comments must STILL be pruned via EXCLUDED_SUBTREES.

        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Autonomous Driving Deep Dive",
        )
        val subscribeButton = createNode(
            contentDescription = "Subscribe to Two Minute Papers.",
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/movie_player",
        )

        // Suggestions with deceptive data
        val suggestedRenderer = createNode(
            viewId = "com.google.android.youtube:id/compact_video_renderer",
            children = listOf(
                createNode(
                    viewId = "com.google.android.youtube:id/video_title",
                    text = "Suggested Clickbait",
                ),
                createNode(
                    text = "@clickbait_king",
                ),
            ),
        )
        val relatedContainer = createNode(
            viewId = "com.google.android.youtube:id/related_videos",
            children = listOf(suggestedRenderer),
        )

        val root = createNode(
            children = listOf(player, watchTitle, subscribeButton, relatedContainer),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertEquals("Autonomous Driving Deep Dive", parsed.title)
        assertEquals("Two Minute Papers", parsed.channelName)
        assertNull(parsed.channelIdentity.verifiedHandle)
        assertEquals("name:two minute papers", parsed.channelIdentity.canonicalKey)
    }

    @Test
    fun `video title starting with at symbol does not pollute channel verified handle`() {
        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "@nasa",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "Space Enthusiast",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )
        val root = createNode(
            children = listOf(player, watchMetadata),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals("Space Enthusiast", parsed.channelName)
        assertNull("Channel verifiedHandle must NOT be polluted by video title '@nasa'", parsed.channelIdentity.verifiedHandle)
    }

    @Test
    fun `subtree pollution - comment mentioning Remix does NOT cause watch video to be classified as Shorts`() {
        // Attack scenario: A regular watch video has comments where a user comments "Remix"
        // Because comments are in comments_entry_point_layout, it MUST be pruned and NOT trigger Shorts detection!
        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Linear Algebra Lecture 1",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "MIT OpenCourseWare",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        // Comment with text "Remix"
        val commentText = createNode(
            text = "Remix",
        )
        val commentThread = createNode(
            viewId = "com.google.android.youtube:id/comment_thread",
            children = listOf(commentText),
        )
        val commentsEntryPoint = createNode(
            viewId = "com.google.android.youtube:id/comments_entry_point_layout",
            children = listOf(commentThread),
        )

        val root = createNode(
            children = listOf(player, watchMetadata, commentsEntryPoint),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals("Watch video with comment saying 'Remix' MUST NOT be classified as Shorts", ScreenType.WATCH, parsed.screenType)
        assertFalse("isShorts must be false", parsed.isShorts)
    }

    @Test
    fun `subtree pollution - suggested shorts shelf inside related_videos does NOT cause watch video to be classified as Shorts`() {
        // Attack scenario: YouTube renders a horizontal Shorts carousel/shelf inside recommendations below a video
        // The watch video must remain WATCH, not be hijacked by the suggested Shorts carousel
        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Operating Systems Principles",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "Stanford",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        val shortsCardInSuggestions = createNode(
            viewId = "com.google.android.youtube:id/reel_scrim_shorts",
            contentDescription = "Dislike this short",
        )
        val relatedVideos = createNode(
            viewId = "com.google.android.youtube:id/related_videos",
            children = listOf(shortsCardInSuggestions),
        )

        val root = createNode(
            children = listOf(player, watchMetadata, relatedVideos),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals("Watch video with suggested shorts in related_videos must remain WATCH screen", ScreenType.WATCH, parsed.screenType)
        assertFalse("isShorts must be false", parsed.isShorts)
    }

    @Test
    fun `watch video with standard YouTube Remix action button in action bar is NOT classified as Shorts`() {
        // YouTube action bar on standard videos includes: Like, Dislike, Share, Remix, Download
        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Calculus 3: Multi-variable",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "Khan Academy",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        // Standard YouTube action bar Remix button
        val remixActionButton = createNode(
            contentDescription = "Remix",
            text = "Remix",
        )
        val actionBar = createNode(
            viewId = "com.google.android.youtube:id/button_bar",
            children = listOf(remixActionButton),
        )

        val root = createNode(
            children = listOf(player, watchMetadata, actionBar),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals("Watch video with Remix action button must be classified as WATCH, not SHORTS", ScreenType.WATCH, parsed.screenType)
        assertFalse("isShorts must be false", parsed.isShorts)
        assertEquals("Calculus 3: Multi-variable", parsed.title)
    }

    // ===========================================================================
    // 2. FAKE HANDLE BAN ADVERSARIAL STRESS TESTING
    // ===========================================================================

    @Test
    fun `fake handle ban - display names with various whitespace and punctuation never produce fake handles`() {
        val testNames = listOf(
            "Khan Academy",
            "  MIT OpenCourseWare  ",
            "3Blue 1Brown",
            "Dr. Becky",
            "CrashCourse Kids",
            "C++ Weekly With Jason Turner",
            "SciShow Space",
            "A B C D E",
        )

        for (name in testNames) {
            val identity = ChannelIdentity.from(handle = null, displayName = name)
            assertNull("verifiedHandle must be null for display name '$name'", identity.verifiedHandle)
            assertEquals(name.trim(), identity.displayName)
            assertFalse("identity.hasHandle must be false", identity.hasHandle)
            assertTrue("identity.hasDisplayName must be true", identity.hasDisplayName)
            assertTrue("canonicalKey must start with 'name:'", identity.canonicalKey.startsWith("name:"))
            assertFalse("canonicalKey must NOT contain '@'", identity.canonicalKey.contains("@"))
        }
    }

    @Test
    fun `fake handle ban - handles without leading at are rejected as fake or invalid`() {
        // If an upstream source passes a raw handle string without '@', normalizeHandle must reject it
        // if it's treated as a handle or verify genuine format
        val invalidHandleInputs = listOf(
            "@",
            "@@@",
            "@   ",
            "   ",
            "",
            "@hello world", // contains space
            "@user/name",   // invalid char /
            "@bad!char",    // invalid char !
            "@user#123",    // invalid char #
        )

        for (input in invalidHandleInputs) {
            val normalized = ChannelIdentity.normalizeHandle(input)
            assertEquals("Input '$input' must normalize to empty string", "", normalized)
        }
    }

    @Test
    fun `fake handle ban - display name with embedded email or at-symbol does not create fake handle`() {
        val identity = ChannelIdentity.from(
            handle = null,
            displayName = "Contact us at info@edu.org",
        )

        assertNull(identity.verifiedHandle)
        assertEquals("Contact us at info@edu.org", identity.displayName)
        assertEquals("name:contact us at info@edu.org", identity.canonicalKey)
    }

    @Test
    fun `fake handle ban - fullwidth at-symbol is normalized cleanly when genuine`() {
        val identity = ChannelIdentity.from(
            handle = "＠veritasium",
            displayName = "Veritasium",
        )

        assertEquals("@veritasium", identity.verifiedHandle)
        assertEquals("handle:@veritasium", identity.canonicalKey)
    }

    // ===========================================================================
    // 3. SHORTS DETECTION EVASION STRESS TESTING
    // ===========================================================================

    @Test
    fun `shorts evasion - detected via reel_player_page container without reel_watch_player ID`() {
        val reelPage = createNode(
            viewId = "com.google.android.youtube:id/reel_player_page",
            children = listOf(
                createNode(text = "Tricky Shorts Video"),
                createNode(text = "@shorts_creator"),
            ),
        )
        val root = createNode(children = listOf(reelPage))

        val parsed = YouTubeUiParser.parse(root)
        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts evasion - detected via reel_scrim_shorts ID when player ID is stripped`() {
        val scrimNode = createNode(
            viewId = "com.google.android.youtube:id/reel_scrim_shorts",
        )
        val root = createNode(children = listOf(scrimNode))

        val parsed = YouTubeUiParser.parse(root)
        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts evasion - detected via Shorts tab text when contentDescription is missing`() {
        val shortsTab = createNode(
            text = "Shorts",
            isSelected = true,
        )
        val root = createNode(children = listOf(shortsTab))

        val parsed = YouTubeUiParser.parse(root)
        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts evasion - detected via Remix text when contentDescription is missing`() {
        val remixNode = createNode(
            text = "Remix",
        )
        val root = createNode(children = listOf(remixNode))

        val parsed = YouTubeUiParser.parse(root)
        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts evasion - detected via Dislike this short content description heuristic`() {
        val dislikeShortNode = createNode(
            contentDescription = "Dislike this short along with 12k others",
        )
        val root = createNode(children = listOf(dislikeShortNode))

        val parsed = YouTubeUiParser.parse(root)
        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts evasion - detected via Sound used in this short heuristic`() {
        val soundNode = createNode(
            contentDescription = "Sound used in this short - Original Audio",
        )
        val root = createNode(children = listOf(soundNode))

        val parsed = YouTubeUiParser.parse(root)
        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts evasion negative test - regular watch video containing the word Shorts in title is NOT classified as Shorts`() {
        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "How to Make YouTube Shorts in 2026",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "Video Creators",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )
        val root = createNode(
            children = listOf(player, watchMetadata),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals("Regular video with 'Shorts' in title must be ScreenType.WATCH", ScreenType.WATCH, parsed.screenType)
        assertFalse("isShorts must be false for normal video", parsed.isShorts)
        assertEquals("How to Make YouTube Shorts in 2026", parsed.title)
    }

    @Test
    fun `shorts evasion negative test - unselected Shorts bottom tab does NOT trigger Shorts screen`() {
        // On Home screen, the Shorts tab exists in bottom nav but is NOT selected (Home tab is selected)
        val homeTab = createNode(
            contentDescription = "Home",
            isSelected = true,
        )
        val shortsTab = createNode(
            contentDescription = "Shorts",
            isSelected = false,
        )
        val feed = createNode(
            viewId = "com.google.android.youtube:id/feed_recycler",
        )
        val root = createNode(
            children = listOf(feed, homeTab, shortsTab),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertNotEquals(ScreenType.SHORTS, parsed.screenType)
        assertFalse(parsed.isShorts)
        assertEquals(ScreenType.FEED, parsed.screenType)
    }

    // ===========================================================================
    // 4. CHANNEL PROFILE PAGE EDGE CASES
    // ===========================================================================

    @Test
    fun `channel page - presence of watch player overrides channel header to WATCH screen`() {
        // If a mini-player or watch player is active while browsing a channel page,
        // it must be treated as WATCH so player policy takes precedence
        val channelHeader = createNode(
            viewId = "com.google.android.youtube:id/channel_header",
            children = listOf(
                createNode(viewId = "com.google.android.youtube:id/channel_title", text = "Veritasium"),
            ),
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(
                createNode(viewId = "com.google.android.youtube:id/video_title", text = "Playing Video"),
                createNode(viewId = "com.google.android.youtube:id/channel_name", text = "Veritasium"),
            ),
        )
        val root = createNode(
            children = listOf(player, watchMetadata, channelHeader),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertFalse("isChannelPage must be false when active player is present", parsed.isChannelPage)
        assertEquals("Playing Video", parsed.title)
    }

    @Test
    fun `channel page - bare channel header without handle uses display name for canonical key`() {
        val channelTitle = createNode(
            viewId = "com.google.android.youtube:id/channel_title",
            text = "Stand-up Maths",
        )
        val channelHeader = createNode(
            viewId = "com.google.android.youtube:id/c3_channel_header",
            children = listOf(channelTitle),
        )
        val root = createNode(children = listOf(channelHeader))

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.CHANNEL_PAGE, parsed.screenType)
        assertTrue(parsed.isChannelPage)
        assertNull(parsed.channelIdentity.verifiedHandle)
        assertEquals("Stand-up Maths", parsed.channelIdentity.displayName)
        assertEquals("name:stand-up maths", parsed.channelIdentity.canonicalKey)
    }

    // ===========================================================================
    // Helper to construct mock AccessibilityNodeInfo trees
    // ===========================================================================

    private fun createNode(
        viewId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        isSelected: Boolean = false,
        children: List<AccessibilityNodeInfo> = emptyList(),
    ): AccessibilityNodeInfo {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.viewIdResourceName } returns viewId
        every { node.text } returns text
        every { node.contentDescription } returns contentDescription
        every { node.isSelected } returns isSelected
        every { node.childCount } returns children.size
        children.forEachIndexed { index, child ->
            every { node.getChild(index) } returns child
        }
        return node
    }
}
