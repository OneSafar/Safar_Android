package com.safarparmar.app.feature.youtube.detector

import android.view.accessibility.AccessibilityNodeInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit test suite for [YouTubeUiParser].
 *
 * Verifies:
 * 1. Subtree isolation: suggested videos (`related_videos`, `compact_video_renderer`) and
 *    comments (`comments_entry_point_layout`, `comment_thread`) NEVER pollute watch identity.
 * 2. Fake handle ban: synthetic handles (e.g. `@${channelName}`) are never generated when
 *    only display name is present.
 * 3. Genuine handles: properly extracted when '@' is present in watch metadata or channel name.
 * 4. Shorts detection: structural reel container, Remix button, and selected Shorts tab.
 * 5. Channel profile page: header extraction and `isChannelPage = true`.
 * 6. In-stream ad detection: restricted strictly to player container.
 * 7. ScreenType determination: WATCH, SHORTS, CHANNEL_PAGE, FEED, SEARCH, UNKNOWN.
 */
class YouTubeUiParserTest {

    @Test
    fun `subtree isolation - suggested video titles and channels do not pollute watch identity`() {
        val watchTitleNode = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Quantum Computing Explained",
        )
        val watchChannelNode = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "Veritasium",
        )
        val watchHandleNode = createNode(
            text = "@veritasium",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitleNode, watchChannelNode, watchHandleNode),
        )

        // Suggestions subtree with conflicting title, channel, and handle
        val suggestedTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "Top 10 Cat Fails 2026",
        )
        val suggestedChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "CatLoversClub",
        )
        val suggestedHandle = createNode(
            text = "@catlovers",
        )
        val compactVideoRenderer = createNode(
            viewId = "com.google.android.youtube:id/compact_video_renderer",
            children = listOf(suggestedTitle, suggestedChannel, suggestedHandle),
        )
        val relatedVideos = createNode(
            viewId = "com.google.android.youtube:id/related_videos",
            children = listOf(compactVideoRenderer),
        )

        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        val root = createNode(
            children = listOf(player, watchMetadata, relatedVideos),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertEquals("Quantum Computing Explained", parsed.title)
        assertEquals("Veritasium", parsed.channelName)
        assertEquals("@veritasium", parsed.channelHandle)
        assertEquals("@veritasium", parsed.channelIdentity.verifiedHandle)
        assertEquals("Veritasium", parsed.channelIdentity.displayName)
        assertEquals("handle:@veritasium", parsed.channelIdentity.canonicalKey)
        assertFalse(parsed.isShorts)
        assertFalse(parsed.isAd)
    }

    @Test
    fun `subtree isolation - user comments with at-mentions do not pollute channel handle`() {
        val watchTitle = createNode(
            viewId = "com.google.android.youtube:id/video_title",
            text = "BBC Earth Documentary",
        )
        val watchChannel = createNode(
            viewId = "com.google.android.youtube:id/channel_name",
            text = "BBC News",
        )
        // Note: No handle in watch metadata
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(watchTitle, watchChannel),
        )

        // Comments subtree with user @mentions
        val commentAuthor = createNode(
            viewId = "com.google.android.youtube:id/author",
            text = "@troll_user_42",
        )
        val commentText = createNode(
            text = "Hey @superstar check out this timestamp 04:15",
        )
        val commentThread = createNode(
            viewId = "com.google.android.youtube:id/comment_thread",
            children = listOf(commentAuthor, commentText),
        )
        val commentsEntryPoint = createNode(
            viewId = "com.google.android.youtube:id/comments_entry_point_layout",
            children = listOf(commentThread),
        )

        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )

        val root = createNode(
            children = listOf(player, watchMetadata, commentsEntryPoint),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertEquals("BBC Earth Documentary", parsed.title)
        assertEquals("BBC News", parsed.channelName)
        // Crucial: comments author was pruned, and NO synthetic @bbcnews handle was generated
        assertNull(parsed.channelIdentity.verifiedHandle)
        assertEquals("BBC News", parsed.channelIdentity.displayName)
        assertEquals("name:bbc news", parsed.channelIdentity.canonicalKey)
        assertEquals("", parsed.channelHandle)
    }

    @Test
    fun `fake handle ban - never synthesizes handle when only display name is available`() {
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(
                createNode(
                    viewId = "com.google.android.youtube:id/video_title",
                    text = "Introduction to Calculus",
                ),
                createNode(
                    viewId = "com.google.android.youtube:id/channel_name",
                    text = "Khan Academy",
                ),
            ),
        )

        val root = createNode(
            children = listOf(
                createNode(viewId = "com.google.android.youtube:id/watch_player"),
                watchMetadata,
            ),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertNull("Verified handle MUST be null when no @handle in UI", parsed.channelIdentity.verifiedHandle)
        assertEquals("Khan Academy", parsed.channelIdentity.displayName)
        assertEquals("name:khan academy", parsed.channelIdentity.canonicalKey)
        assertEquals("", parsed.channelHandle)
    }

    @Test
    fun `genuine handle - extracts handle when channel name text starts with at`() {
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(
                createNode(
                    viewId = "com.google.android.youtube:id/video_title",
                    text = "Essence of Linear Algebra",
                ),
                createNode(
                    viewId = "com.google.android.youtube:id/channel_name",
                    text = "@3blue1brown",
                ),
            ),
        )

        val root = createNode(
            children = listOf(
                createNode(viewId = "com.google.android.youtube:id/watch_player"),
                watchMetadata,
            ),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals("@3blue1brown", parsed.channelIdentity.verifiedHandle)
        assertEquals("handle:@3blue1brown", parsed.channelIdentity.canonicalKey)
    }

    @Test
    fun `shorts detection - structural reel container`() {
        val reelTitle = createNode(
            viewId = "com.google.android.youtube:id/reel_watch_player_title",
            text = "Incredible Magic Trick",
        )
        val reelChannel = createNode(
            viewId = "com.google.android.youtube:id/reel_channel_name",
            text = "@magician",
        )
        val reelPlayer = createNode(
            viewId = "com.google.android.youtube:id/reel_watch_player",
            children = listOf(reelTitle, reelChannel),
        )

        val root = createNode(
            children = listOf(reelPlayer),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
        assertEquals("Incredible Magic Trick", parsed.title)
        assertEquals("@magician", parsed.channelHandle)
    }

    @Test
    fun `shorts detection - Remix button signal`() {
        val remixButton = createNode(
            contentDescription = "Remix",
        )
        val title = createNode(
            text = "Viral Dance Trend",
        )

        val root = createNode(
            children = listOf(remixButton, title),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `shorts detection - selected bottom nav Shorts tab`() {
        val shortsTab = createNode(
            contentDescription = "Shorts",
            isSelected = true,
        )

        val root = createNode(
            children = listOf(shortsTab),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.SHORTS, parsed.screenType)
        assertTrue(parsed.isShorts)
    }

    @Test
    fun `channel profile page - extracts header and marks isChannelPage true`() {
        val channelTitle = createNode(
            viewId = "com.google.android.youtube:id/channel_title",
            text = "Marques Brownlee",
        )
        val channelHandle = createNode(
            text = "@mkbhd",
        )
        val channelHeader = createNode(
            viewId = "com.google.android.youtube:id/channel_header",
            children = listOf(channelTitle, channelHandle),
        )

        val root = createNode(
            children = listOf(channelHeader),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.CHANNEL_PAGE, parsed.screenType)
        assertTrue("isChannelPage must be true on channel profile screen", parsed.isChannelPage)
        assertFalse(parsed.isShorts)
        assertFalse(parsed.isAd)
        assertEquals("Marques Brownlee", parsed.channelName)
        assertEquals("@mkbhd", parsed.channelHandle)
        assertEquals("@mkbhd", parsed.channelIdentity.verifiedHandle)
        assertEquals("Marques Brownlee", parsed.channelIdentity.displayName)
        assertEquals("handle:@mkbhd", parsed.channelIdentity.canonicalKey)

        val canonical = parsed.toCanonicalContent()
        assertTrue(canonical.isChannelPage)
        assertEquals("handle:@mkbhd", canonical.channelIdentity.canonicalKey)
    }

    @Test
    fun `in-stream ad detected strictly within player container`() {
        val skipButton = createNode(
            viewId = "com.google.android.youtube:id/skip_ad_button",
            text = "Skip Ad",
        )
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
            children = listOf(skipButton),
        )

        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(
                createNode(
                    viewId = "com.google.android.youtube:id/video_title",
                    text = "Main Video Title",
                ),
                createNode(
                    viewId = "com.google.android.youtube:id/channel_name",
                    text = "Main Channel",
                ),
            ),
        )

        val root = createNode(
            children = listOf(player, watchMetadata),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertTrue("In-stream ad in player must set isAd = true", parsed.isAd)
    }

    @Test
    fun `ad marker in feed does not trigger in-stream ad`() {
        val player = createNode(
            viewId = "com.google.android.youtube:id/watch_player",
        )
        val watchMetadata = createNode(
            viewId = "com.google.android.youtube:id/watch_metadata",
            children = listOf(
                createNode(
                    viewId = "com.google.android.youtube:id/video_title",
                    text = "Space Exploration",
                ),
                createNode(
                    viewId = "com.google.android.youtube:id/channel_name",
                    text = "NASA",
                ),
            ),
        )
        // Sponsored ad in recommendations below video
        val feedAd = createNode(
            text = "Ad · Download App Today",
        )
        val relatedVideos = createNode(
            viewId = "com.google.android.youtube:id/related_videos",
            children = listOf(feedAd),
        )

        val root = createNode(
            children = listOf(player, watchMetadata, relatedVideos),
        )

        val parsed = YouTubeUiParser.parse(root)

        assertEquals(ScreenType.WATCH, parsed.screenType)
        assertFalse("Feed recommendation ad must NOT set isAd = true", parsed.isAd)
        assertEquals("NASA", parsed.channelName)
    }

    @Test
    fun `determineScreenType classifies screens accurately`() {
        // Shorts
        val shortsRoot = createNode(
            children = listOf(createNode(viewId = "com.google.android.youtube:id/reel_watch_player")),
        )
        assertEquals(ScreenType.SHORTS, YouTubeUiParser.determineScreenType(shortsRoot))

        // Watch
        val watchRoot = createNode(
            children = listOf(createNode(viewId = "com.google.android.youtube:id/watch_player")),
        )
        assertEquals(ScreenType.WATCH, YouTubeUiParser.determineScreenType(watchRoot))

        // Channel Page
        val channelRoot = createNode(
            children = listOf(createNode(viewId = "com.google.android.youtube:id/channel_header")),
        )
        assertEquals(ScreenType.CHANNEL_PAGE, YouTubeUiParser.determineScreenType(channelRoot))

        // Search
        val searchRoot = createNode(
            children = listOf(createNode(viewId = "com.google.android.youtube:id/search_results")),
        )
        assertEquals(ScreenType.SEARCH, YouTubeUiParser.determineScreenType(searchRoot))

        // Feed
        val feedRoot = createNode(
            children = listOf(createNode(viewId = "com.google.android.youtube:id/home_feed")),
        )
        assertEquals(ScreenType.FEED, YouTubeUiParser.determineScreenType(feedRoot))

        // Unknown
        val emptyRoot = createNode()
        assertEquals(ScreenType.UNKNOWN, YouTubeUiParser.determineScreenType(emptyRoot))
    }

    // ---------------------------------------------------------------------------
    // Helper to construct mock AccessibilityNodeInfo trees
    // ---------------------------------------------------------------------------

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
