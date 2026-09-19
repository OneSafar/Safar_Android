package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeStudyV2ParserTest {
    @Test fun `fullscreen exit control cannot become a channel name`() {
        val nodes = baseWatchNodes() + listOf(
            node(description = "Exit full screen", clazz = "ImageView", top = 800, bottom = 900, left = 900, right = 1050).copy(clickable = true),
        )
        assertNull(YoutubeStudyV2Parser.parse(snapshot(nodes)).displayName)
        assertFalse(YoutubeStudyV2Parser.isPlausibleOwnerLabel("Exit full screen"))
    }

    @Test fun `confirmed Shorts outranks a leftover mini player`() {
        val nodes = listOf(
            node(id = "modern_miniplayer"),
            node(id = "pivot_shorts", description = "Shorts").copy(selected = true),
        )
        assertEquals(YoutubeV2ContentKind.SHORTS, YoutubeStudyV2Parser.parse(snapshot(nodes)).kind)
    }

    @Test fun `landscape player is fullscreen even when owner row is hidden`() {
        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            packageName = YoutubeStudyV2Parser.YOUTUBE_PACKAGE, density = 1f,
            screenWidth = 1920, screenHeight = 1080,
            nodes = listOf(node(id = "fullscreen_player", left = 0, top = 0, right = 1920, bottom = 1080)),
        ))
        assertTrue(result.fullscreen)
        assertTrue(result.watchScreenConfirmed)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test fun `fullscreen HUD title and bare handle identify uploader`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 2400, bottom = 1080),
            node(text = "Terrorist Or Prisoner Of A Terror State?", clazz = "TextView", parent = 0, left = 180, top = 28, right = 1500, bottom = 92),
            node(text = "@thedeshbhakt", clazz = "TextView", parent = 0, left = 180, top = 94, right = 600, bottom = 145),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertTrue(result.fullscreen)
        assertEquals("Terrorist Or Prisoner Of A Terror State?", result.title)
        assertEquals("@thedeshbhakt", result.exactHandle)
        assertTrue(result.hasOwnerEvidence)
    }

    @Test fun `fullscreen HUD handle supplements a stale title-only watch header`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "watch_list", clazz = "RecyclerView", parent = 0, left = 0, top = 0, right = 2400, bottom = 1080),
            node(parent = 2, left = 0, top = 0, right = 1600, bottom = 170),
            node(text = "WTF Happened to Perplexity?!", clazz = "TextView", parent = 3, left = 180, top = 28, right = 1500, bottom = 92),
            node(text = "WTF Happened to Perplexity?!", clazz = "TextView", parent = 0, left = 180, top = 28, right = 1500, bottom = 92),
            node(text = "@UtsavTechie", clazz = "TextView", parent = 0, left = 180, top = 94, right = 600, bottom = 145),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertEquals("@utsavtechie", result.exactHandle)
        assertTrue(result.hasOwnerEvidence)
    }

    @Test fun `duplicate fullscreen HUD nodes for one handle are accepted`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 2400, bottom = 1080),
            node(text = "WTF Happened to Perplexity?!", clazz = "TextView", parent = 0, left = 180, top = 28, right = 1500, bottom = 92),
            node(text = "@UtsavTechie", clazz = "TextView", parent = 0, left = 180, top = 94, right = 600, bottom = 145),
            node(text = "", description = "@UtsavTechie", clazz = "ViewGroup", parent = 0, left = 180, top = 94, right = 600, bottom = 145),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertEquals("@utsavtechie", result.exactHandle)
        assertTrue(result.hasOwnerEvidence)
    }

    @Test fun `fullscreen caption mention is not accepted as uploader`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 2400, bottom = 1080),
            node(text = "Watch @someone for the full explanation", clazz = "TextView", parent = 0, left = 500, top = 430, right = 1900, bottom = 500),
            node(text = "@someone", clazz = "TextView", parent = 0, left = 500, top = 510, right = 900, bottom = 565),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertNull(result.exactHandle)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test fun `fullscreen sponsored side panel is distinguished from in-player ad playback`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 1400, bottom = 1080),
            node(text = "Sponsored", clazz = "TextView", parent = 0, left = 1460, top = 35, right = 1900, bottom = 110),
            node(text = "Install", clazz = "Button", parent = 0, left = 1500, top = 850, right = 2300, bottom = 1010),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertTrue(result.fullscreen)
        assertEquals(YoutubeFullscreenSidePanel.SPONSORED, result.fullscreenSidePanel)
        assertFalse(result.adPlaying)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test fun `fullscreen live chat handles are not uploader evidence`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 1400, bottom = 1080),
            node(text = "Live chat", clazz = "TextView", parent = 0, left = 1460, top = 30, right = 1900, bottom = 105),
            node(text = "@randomviewer", clazz = "TextView", parent = 0, left = 1550, top = 300, right = 2100, bottom = 370),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertEquals(YoutubeFullscreenSidePanel.LIVE_CHAT, result.fullscreenSidePanel)
        assertNull(result.exactHandle)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test fun `sponsored text inside the player is not classified as a side panel`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 2400, bottom = 1080),
            node(id = "fullscreen_player", clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 2400, bottom = 1080),
            node(text = "Sponsored", clazz = "TextView", parent = 0, left = 120, top = 800, right = 500, bottom = 870),
        )

        val result = YoutubeStudyV2Parser.parse(YoutubeV2Snapshot(
            YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 3f, 2400, 1080, nodes,
        ))

        assertEquals(YoutubeFullscreenSidePanel.NONE, result.fullscreenSidePanel)
    }

    @Test fun `active shorts controls override selected Home tab`() {
        val nodes = listOf(
            node(id = "pivot_home").copy(selected = true),
            node(id = "reel_player", left = 0, top = 0, right = 1080, bottom = 1800),
            node(description = "Dislike this short"),
        )
        assertEquals(YoutubeV2ContentKind.SHORTS, YoutubeStudyV2Parser.parse(snapshot(nodes)).kind)
    }

    @Test fun `channel URL is accepted only from owner metadata`() {
        val id = "UC_x5XG1OV2P6uZZ5FSM9Ttw"
        val owner = node(id = "video_owner", text = "https://www.youtube.com/channel/$id", top = 900, bottom = 1000)
        assertEquals(id, YoutubeStudyV2Parser.parse(snapshot(baseWatchNodes() + owner)).exactChannelId)
        assertNull(YoutubeStudyV2Parser.parse(snapshot(baseWatchNodes() + owner.copy(viewId = "comment"))).exactChannelId)
    }

    @Test
    fun `floating mini player on home is playback without borrowing feed channel identity`() {
        val result = YoutubeStudyV2Parser.parse(snapshot(listOf(
            node(id = "modern_miniplayer", left = 600, top = 1200, right = 1060, bottom = 1500),
            node(id = "video_owner", text = "Productive feed recommendation"),
            node(text = "@productive_channel"),
        )))
        assertEquals(YoutubeV2ContentKind.MINI_PLAYER, result.kind)
        assertFalse(result.hasOwnerEvidence)
        assertFalse(result.canResumeWatchSession)
        assertTrue(shouldBlockYoutubePip(false, YoutubeChannelClassification.DISTRACTING))
    }

    @Test
    fun `legacy mini player is recognized and hidden mini player is ignored`() {
        val visible = node(id = "miniplayer_container")
        assertEquals(YoutubeV2ContentKind.MINI_PLAYER, YoutubeStudyV2Parser.parse(snapshot(listOf(visible))).kind)
        assertEquals(YoutubeV2ContentKind.NON_PLAYBACK, YoutubeStudyV2Parser.parse(snapshot(listOf(visible.copy(visibleToUser = false)))).kind)
    }

    @Test
    fun `mini player closes through close control not its playback toggle`() {
        assertTrue(YoutubeStudyV2Parser.isMiniPlayerCloseControl("com.google.android.youtube:id/modern_miniplayer_close", null))
        assertTrue(YoutubeStudyV2Parser.isMiniPlayerCloseControl(null, "Close player"))
        assertTrue(YoutubeStudyV2Parser.isMiniPlayerCloseControl(null, "Close minimised player"))
        assertFalse(YoutubeStudyV2Parser.isMiniPlayerCloseControl("modern_miniplayer_play_pause", "Pause"))
        assertFalse(YoutubeStudyV2Parser.isMiniPlayerCloseControl("modern_miniplayer_expand", "Expand"))
        assertFalse(YoutubeStudyV2Parser.isMiniPlayerCloseControl("close_button", "Close ad panel"))
    }

    @Test
    fun `structural owner row extracts exact handle instead of title keyword`() {
        val nodes = baseWatchNodes() + listOf(
            node(id = "video_owner", parent = 0, left = 0, top = 900, right = 1080, bottom = 1050),
            node(clazz = "ImageView", parent = 3, left = 30, top = 920, right = 150, bottom = 1040),
            node(text = "Parmar SSC", clazz = "TextView", parent = 3, left = 165, top = 930, right = 500, bottom = 990),
            node(text = "@ParmarSSC", clazz = "TextView", parent = 3, left = 165, top = 990, right = 500, bottom = 1040),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals("@parmarssc", result.exactHandle)
        assertEquals("Parmar SSC", result.displayName)
    }

    @Test
    fun `display name without exact handle never becomes identity`() {
        val nodes = baseWatchNodes() + listOf(
            node(id = "video_owner", parent = 0, left = 0, top = 900, right = 1080, bottom = 1050),
            node(clazz = "ImageView", parent = 3, left = 30, top = 920, right = 150, bottom = 1040),
            node(text = "Unacademy", clazz = "TextView", parent = 3, left = 165, top = 930, right = 500, bottom = 1000),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertEquals("Unacademy", result.displayName)
        assertNull(result.exactHandle)
    }

    @Test
    fun `home preview surface without watch marker is not playback`() {
        val nodes = listOf(
            node(clazz = "SurfaceView", left = 0, top = 200, right = 1080, bottom = 800),
            node(text = "SSC", clazz = "TextView", parent = 0, left = 10, top = 810, right = 300, bottom = 880),
        )
        assertEquals(YoutubeV2ContentKind.NON_PLAYBACK, YoutubeStudyV2Parser.parse(snapshot(nodes)).kind)
    }

    @Test
    fun `confirmed reel watch is classified as shorts`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "reel_watch", clazz = "FrameLayout", parent = 0, left = 0, top = 0, right = 1080, bottom = 1800),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.SHORTS, result.kind)
    }

    @Test
    fun `shorts text and thumbnail in home feed are not a shorts viewer`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(text = "Shorts", clazz = "TextView", parent = 0, left = 0, top = 500, right = 300, bottom = 560),
            node(clazz = "SurfaceView", parent = 0, left = 0, top = 600, right = 500, bottom = 1000),
        )
        assertEquals(YoutubeV2ContentKind.NON_PLAYBACK, YoutubeStudyV2Parser.parse(snapshot(nodes)).kind)
    }

    @Test
    fun `home feed with pivot_home selected is always non playback even with surface and watch player markers`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 800),
            node(id = "pivot_home", description = "Home", text = "Home", parent = 0, left = 0, top = 1800, right = 200, bottom = 1920).copy(selected = true),
            node(clazz = "ImageView", parent = 0, left = 30, top = 920, right = 150, bottom = 1040),
            node(text = "Home", clazz = "TextView", parent = 0, left = 165, top = 930, right = 500, bottom = 990),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertEquals(YoutubeV2ContentKind.NON_PLAYBACK, result.kind)
        assertFalse(result.watchScreenConfirmed)
        assertNull(result.displayName)
    }

    @Test
    fun `shorts screen with background watch_player is classified as shorts and never video`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            // YouTube main activity keeps watch_player in hierarchy in background
            node(id = "watch_player", clazz = "FrameLayout", parent = 0, left = 0, top = 0, right = 1080, bottom = 800),
            // Shorts player is active
            node(id = "reel_player", clazz = "FrameLayout", parent = 0, left = 0, top = 0, right = 1080, bottom = 1920),
            node(clazz = "SurfaceView", parent = 2, left = 0, top = 0, right = 1080, bottom = 1920),
            node(description = "Dislike this short", clazz = "Button", parent = 2, left = 900, top = 1400, right = 1050, bottom = 1550),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.SHORTS, result.kind)
    }

    @Test
    fun `navigation tab labels like Home or Shorts are never valid channel labels`() {
        assertFalse(YoutubeStudyV2Parser.cleanOwnerText("Home")?.let { it.length in 2..120 && listOf("home").none(it.lowercase()::contains) } ?: false)
        val nodes = baseWatchNodes() + listOf(
            node(id = "video_owner", parent = 0, left = 0, top = 900, right = 1080, bottom = 1050),
            node(clazz = "ImageView", parent = 3, left = 30, top = 920, right = 150, bottom = 1040),
            node(text = "Home", clazz = "TextView", parent = 3, left = 165, top = 930, right = 500, bottom = 1000),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertNull(result.displayName)
    }

    @Test
    fun `shorts tab selected in pivot bar is immediately classified as shorts`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "pivot_shorts", description = "Shorts", parent = 0, left = 200, top = 1800, right = 400, bottom = 1920).copy(selected = true),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.SHORTS, result.kind)
    }

    @Test
    fun `modern reel player container is classified as shorts without reel_watch`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "reel_player", clazz = "FrameLayout", parent = 0, left = 0, top = 0, right = 1080, bottom = 1800),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.SHORTS, result.kind)
    }

    @Test
    fun `shorts player view id is recognized as shorts`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "shorts_player", clazz = "FrameLayout", parent = 0, left = 0, top = 0, right = 1080, bottom = 1800),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.SHORTS, result.kind)
    }

    @Test
    fun `screen with remix button or dislike short button is classified as shorts`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(clazz = "SurfaceView", parent = 0, left = 0, top = 0, right = 1080, bottom = 1920),
            node(description = "Dislike this short", clazz = "Button", parent = 0, left = 900, top = 1400, right = 1050, bottom = 1550),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.SHORTS, result.kind)
    }

    @Test
    fun `regular video with Dislike this video and unselected pivot_shorts is classified as VIDEO and never shorts`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 800),
            node(id = "video_title", text = "Regular Study Video", clazz = "TextView", parent = 0, left = 20, top = 810, right = 900, bottom = 880),
            node(description = "Dislike this video", clazz = "Button", parent = 0, left = 200, top = 890, right = 350, bottom = 950),
            node(id = "pivot_shorts", description = "Shorts", parent = 0, left = 200, top = 1800, right = 400, bottom = 1920),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.VIDEO, result.kind)
    }

    @Test
    fun `tablet watch frame and accessibility owner card are detected without a surface view`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1800, bottom = 2880),
            node(id = "watch_player", clazz = "FrameLayout", parent = 0, left = 0, top = 60, right = 1800, bottom = 1073),
            node(id = "watch_list", clazz = "RecyclerView", parent = 0, left = 0, top = 1073, right = 1800, bottom = 2880),
            node(
                description = "MohitVerse and ComicVerse",
                clazz = "ViewGroup",
                parent = 2,
                clickable = true,
                left = 30,
                top = 1226,
                right = 519,
                bottom = 1346,
            ),
            node(clazz = "ImageView", parent = 3, left = 30, top = 1246, right = 85, bottom = 1301),
        )
        val result = YoutubeStudyV2Parser.parse(
            YoutubeV2Snapshot(YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 2f, 1800, 2880, nodes),
        )
        assertTrue(result.watchScreenConfirmed)
        assertEquals("MohitVerse and ComicVerse", result.displayName)
    }

    @Test
    fun `subscriber metadata is removed before exact display alias matching`() {
        assertEquals("PARMAR SSC", YoutubeStudyV2Parser.cleanOwnerText("PARMAR SSC 24.7 lakh subscribers"))
        assertEquals("PARMAR SSC", YoutubeStudyV2Parser.cleanOwnerText("Go to channel PARMAR SSC"))
        assertEquals("PARMAR SSC", YoutubeStudyV2Parser.cleanOwnerText("PARMAR SSC · 1.4K likes 72K views 1 day ago"))
        val mergedText = "@parmarssc 1.4K likes 72K views 1 day ago 1 product ...more"
        assertEquals("@parmarssc", YoutubeStudyV2Parser.verifiedUploaderHandle(mergedText))
    }

    @Test
    fun `exact handle in bounded watch metadata wins while owner card is still rendering`() {
        val nodes = baseWatchNodes() + listOf(
            node(
                description = "SAFAR_PARMAR 125K subscribers",
                clazz = "ViewGroup",
                parent = 0,
                clickable = true,
                left = 20,
                top = 885,
                right = 520,
                bottom = 1010,
            ),
            node(clazz = "ImageView", parent = 3, left = 20, top = 900, right = 100, bottom = 980),
            node(text = "@SAFARPARMAR 33K views 10d ago", clazz = "TextView", parent = 0, left = 20, top = 810, right = 500, bottom = 860),
            // A handle outside the bounded owner region must not be selected.
            node(text = "@wrongcomment", clazz = "TextView", parent = 0, left = 20, top = 1700, right = 300, bottom = 1760),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertEquals("@safarparmar", result.exactHandle)
        assertEquals("SAFAR_PARMAR", result.displayName)
        assertTrue(result.hasOwnerEvidence)
    }

    @Test
    fun `handle mentioned in video title is never treated as channel owner`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 800),
            node(id = "video_title", text = "Why @wronghandle is trending", clazz = "TextView", parent = 0, left = 20, top = 810, right = 900, bottom = 880),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertNull(result.exactHandle)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test
    fun `video title mentions of other channels are never mistaken for real uploader`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 800),
            node(id = "video_title", text = "Collab with @MrBeast and @CarryMinati", clazz = "TextView", parent = 0, left = 20, top = 810, right = 1000, bottom = 880),
            node(id = "video_owner", parent = 0, left = 0, top = 900, right = 1080, bottom = 1050),
            node(clazz = "ImageView", parent = 3, left = 30, top = 920, right = 150, bottom = 1040),
            node(text = "The RawKnee Show", clazz = "TextView", parent = 3, left = 165, top = 930, right = 500, bottom = 990),
            node(text = "@therawkneeshow", clazz = "TextView", parent = 3, left = 165, top = 990, right = 500, bottom = 1040),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertEquals("@therawkneeshow", result.exactHandle)
        assertEquals("The RawKnee Show", result.displayName)
    }

    @Test
    fun `title mention cannot replace uploader handle when title resource id is missing`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 800),
            // Some YouTube builds expose no useful title resource id.
            node(text = "Mumbai vs Delhi with @AshishChanchlaniVines", clazz = "TextView", parent = 0, left = 20, top = 810, right = 1000, bottom = 880),
            node(text = "@tanmaybhat 4.1 lakh likes 78 lakh views 4 yr ago", clazz = "TextView", parent = 0, left = 20, top = 885, right = 1000, bottom = 945),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertEquals("@tanmaybhat", result.exactHandle)
    }

    @Test
    fun `standalone title mention without uploader proof is rejected`() {
        val nodes = baseWatchNodes() + node(
            text = "@kanizsurka",
            clazz = "TextView",
            parent = 0,
            left = 20,
            top = 885,
            right = 400,
            bottom = 945,
        )

        assertNull(YoutubeStudyV2Parser.parse(snapshot(nodes)).exactHandle)
    }

    @Test
    fun `merged title mentions are ignored and uploader segment wins`() {
        val merged = "MUMBAI VS DELHI with @AshishChanchlaniVines @tanmaybhat 4.1 lakh likes 78 lakh views 4 yr ago"

        assertEquals("@tanmaybhat", YoutubeStudyV2Parser.verifiedUploaderHandle(merged))
    }

    @Test
    fun `title mention with no separate uploader metadata is rejected`() {
        val title = "Interview with @KanizSurka about comedy"

        assertNull(YoutubeStudyV2Parser.verifiedUploaderHandle(title))
    }

    @Test
    fun `structural owner handle works on a compact low density screen`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 720, bottom = 1280),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 40, right = 720, bottom = 450),
            node(id = "video_owner", parent = 0, left = 8, top = 500, right = 600, bottom = 570),
            node(clazz = "ImageView", parent = 2, left = 12, top = 505, right = 62, bottom = 555),
            node(text = "Study Channel", clazz = "TextView", parent = 2, left = 70, top = 505, right = 300, bottom = 530),
            node(text = "@StudyChannel", clazz = "TextView", parent = 2, left = 70, top = 532, right = 300, bottom = 560),
        )

        val result = YoutubeStudyV2Parser.parse(
            YoutubeV2Snapshot(YoutubeStudyV2Parser.YOUTUBE_PACKAGE, 1f, 720, 1280, nodes),
        )

        assertEquals("@studychannel", result.exactHandle)
        assertEquals("Study Channel", result.displayName)
    }

    @Test
    fun `pre-roll advertiser is marked as ad and never becomes owner evidence`() {
        val nodes = baseWatchNodes() + listOf(
            node(description = "Visit advertiser", clazz = "Button", parent = 1, clickable = true, left = 700, top = 120, right = 1000, bottom = 180),
            node(description = "Expand ad panel", clazz = "ViewGroup", parent = 0, clickable = true, left = 20, top = 820, right = 500, bottom = 900),
            node(clazz = "ImageView", parent = 4, left = 20, top = 830, right = 80, bottom = 890),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertTrue(result.adPlaying)
        assertFalse(result.hasOwnerEvidence)
    }

    @Test
    fun `session rejects a watch tree until video card was tapped`() {
        val session = YoutubeStudyV2Session()
        val observation = YoutubeV2Observation(
            kind = YoutubeV2ContentKind.VIDEO,
            watchScreenConfirmed = true,
            title = "SSC strategy",
            exactHandle = "@parmarssc",
        )
        assertFalse(session.acceptStable(observation, 1_000))
        session.onVideoTap(1_100)
        assertTrue(session.acceptStable(observation, 1_400))
    }

    @Test
    fun `autoplay title change remains inside the tapped watch session`() {
        val session = YoutubeStudyV2Session()
        val first = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true, "One", "@channel")
        val second = first.copy(title = "Two")
        session.onVideoTap(1_000)
        assertTrue(session.acceptStable(first, 1_500))
        assertTrue(session.acceptStable(second, 2_000))
        assertEquals(YoutubeStudyV2Session.State.MONITORING, session.state)
    }

    @Test
    fun `unrelated handle mention in title does not override uploader display name`() {
        assertTrue(YoutubeStudyV2Parser.isHandleCompatibleWithDisplay("@officialrelentx", "RelentX"))
        assertTrue(YoutubeStudyV2Parser.isHandleCompatibleWithDisplay("@parmarssc", "PARMAR SSC"))
        assertTrue(YoutubeStudyV2Parser.isHandleCompatibleWithDisplay("@dhruvrathee", "Dhruv Rathee"))
        assertTrue(YoutubeStudyV2Parser.isHandleCompatibleWithDisplay("@primevideoin", "Prime Video India"))
        assertFalse(YoutubeStudyV2Parser.isHandleCompatibleWithDisplay("@mythpat", "Tanmay Bhat"))
        assertFalse(YoutubeStudyV2Parser.isHandleCompatibleWithDisplay("@mrbeast", "Tanmay Bhat"))
    }

    @Test
    fun `prime video india with samay raina title mention resolves prime video handle`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 700),
            node(
                id = "video_title",
                text = "Indian Ads Vs American Ads By @SamayRaina",
                clazz = "TextView",
                parent = 0,
                left = 32,
                top = 720,
                right = 1000,
                bottom = 780,
            ),
            node(
                text = "@PrimeVideoIN 103K likes 3.5M views 4y ago #StandUpCom...more",
                clazz = "TextView",
                parent = 0,
                left = 32,
                top = 790,
                right = 1000,
                bottom = 850,
            ),
            node(
                description = "Go to channel Prime Video India",
                clazz = "Button",
                clickable = true,
                parent = 0,
                left = 32,
                top = 860,
                right = 150,
                bottom = 960,
            ),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertEquals("@primevideoin", result.exactHandle)
        assertEquals("Prime Video India", result.displayName)
    }

    @Test
    fun `modern youtube watch screen with handle in description preview extracts handle`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 700),
            node(id = "video_title", text = "I FIXED My Whole Life In 1 Night, Learn H...", clazz = "TextView", parent = 0, left = 32, top = 720, right = 1000, bottom = 780),
            node(id = "description", text = "@akshathsharma 5.4K likes 375K views 9d ago #ex...more", clazz = "TextView", parent = 0, left = 32, top = 790, right = 1000, bottom = 850),
            node(id = "channel_avatar", description = "Go to channel", clazz = "ImageView", parent = 0, clickable = true, left = 32, top = 860, right = 112, bottom = 940),
            node(text = "Subscribe", clazz = "Button", parent = 0, left = 130, top = 860, right = 300, bottom = 940),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertEquals("@akshathsharma", result.exactHandle)
    }

    @Test
    fun `modern youtube watch screen with handle in description preview and display name in avatar description`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 700),
            node(id = "video_title", text = "I FIXED My Whole Life In 1 Night, Learn H...", clazz = "TextView", parent = 0, left = 32, top = 720, right = 1000, bottom = 780),
            node(id = "description", text = "@akshathsharma 5.4K likes 375K views 9d ago #ex...more", clazz = "TextView", parent = 0, left = 32, top = 790, right = 1000, bottom = 850),
            node(id = "channel_avatar", description = "Go to channel Akshath sharma", clazz = "ImageView", parent = 0, clickable = true, left = 32, top = 860, right = 112, bottom = 940),
            node(text = "Subscribe", clazz = "Button", parent = 0, left = 130, top = 860, right = 300, bottom = 940),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertEquals("@akshathsharma", result.exactHandle)
        assertEquals("Akshath sharma", result.displayName)
    }

    @Test
    fun `modern youtube watch screen with sibling handle and engagement nodes extracts handle and title without resource ids`() {
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400),
            node(id = "watch_player", clazz = "FrameLayout", parent = 0, left = 0, top = 74, right = 1080, bottom = 681),
            // Parent container for title and metadata
            node(parent = 0, left = 0, top = 681, right = 1080, bottom = 879, clazz = "ViewGroup"),
            // [81] Title node with no resource id and ViewGroup class
            node(parent = 2, left = 0, top = 723, right = 1080, bottom = 793, text = "I FIXED My Whole Life In 1 Night, Learn How In 146 Seconds", clazz = "ViewGroup"),
            // [83] Bare handle node
            node(parent = 2, left = 32, top = 809, right = 331, bottom = 858, text = "@akshathsharma  ", clazz = "ViewGroup"),
            // [84] Sibling engagement node
            node(parent = 2, left = 331, top = 809, right = 1048, bottom = 858, text = "5.4 thousand likes. 375K views. 9 days ago. See #execution videos ...more", clazz = "ViewGroup"),
            // Owner action bar row
            node(parent = 0, left = 32, top = 879, right = 362, bottom = 1005, clazz = "ViewGroup"),
            // [134] Avatar button with subscriber count (clickable, with child ImageView)
            node(parent = 6, left = 32, top = 879, right = 116, bottom = 1005, description = "Akshath sharma 61.9K subscribers", clazz = "Button", clickable = true),
            // [142] Child ImageView of avatar button
            node(parent = 7, left = 32, top = 900, right = 116, bottom = 984, clazz = "ImageView"),
            // [143] Subscribe button
            node(parent = 6, left = 137, top = 902, right = 362, bottom = 986, description = "Subscribe to Akshath sharma.", clazz = "Button", clickable = true),
        )
        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))
        assertTrue(result.watchScreenConfirmed)
        assertEquals("@akshathsharma", result.exactHandle)
        assertEquals("Akshath sharma", result.displayName)
        assertEquals("I FIXED My Whole Life In 1 Night, Learn How In 146 Seconds", result.title)
    }

    @Test
    fun `sibling handle and engagement text without owner ids resolves like real device log`() {
        // Mirrors an on-device capture where YouTube renders the title, handle,
        // and engagement metadata as three id-less sibling nodes under a shared
        // parent, rather than the video_owner/video_title id'd containers other
        // tests assume. Node indices below mirror [68]/[81]/[83]/[84] from the log.
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400), // 0: root
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 74, right = 1080, bottom = 681), // 1: player
            node(parent = 0, left = 0, top = 681, right = 1080, bottom = 2337), // 2: metadata row container (id=null, "[68]")
            node(
                text = "I FIXED My Whole Life In 1 Night, Learn How In 146 Seconds",
                clazz = "ViewGroup", // real device puts title text on a ViewGroup, not TextView
                parent = 2,
                left = 0,
                top = 723,
                right = 1080,
                bottom = 793,
            ), // 3: title, id=null ("[81]")
            node(
                text = "@akshathsharma  ",
                clazz = "ViewGroup",
                parent = 2,
                left = 32,
                top = 809,
                right = 331,
                bottom = 858,
            ), // 4: bare handle, id=null, no engagement text of its own ("[83]")
            node(
                text = "5.4 thousand likes. 375K views. 9 days ago. See #execution videos ...more",
                clazz = "ViewGroup",
                parent = 2,
                left = 331,
                top = 809,
                right = 1048,
                bottom = 858,
            ), // 5: sibling engagement text, same row, id=null ("[84]")
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.VIDEO, result.kind)
        assertEquals("I FIXED My Whole Life In 1 Night, Learn How In 146 Seconds", result.title)
        assertEquals("@akshathsharma", result.exactHandle)
        assertTrue(result.hasOwnerEvidence)
    }

    @Test
    fun `bare handle sibling without vertically overlapping engagement text is rejected`() {
        // Same shape as above, but the engagement text sibling is a different row
        // (e.g. a comment or unrelated metadata) rather than the same line as the
        // handle. Proof must not be borrowed across rows.
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 74, right = 1080, bottom = 681),
            node(parent = 0, left = 0, top = 681, right = 1080, bottom = 2337),
            node(
                text = "A title without SSC",
                clazz = "ViewGroup",
                parent = 2,
                left = 0,
                top = 723,
                right = 1080,
                bottom = 793,
            ),
            node(
                text = "@akshathsharma  ",
                clazz = "ViewGroup",
                parent = 2,
                left = 32,
                top = 809,
                right = 331,
                bottom = 858,
            ),
            node(
                text = "5.4 thousand likes. 375K views. 9 days ago.",
                clazz = "ViewGroup",
                parent = 2,
                left = 32,
                top = 1400, // far below, no vertical overlap with the handle row
                right = 1048,
                bottom = 1450,
            ),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertNull(result.exactHandle)
    }

    @Test
    fun `title with pipes and numbers is preserved and not replaced by action buttons`() {
        val title = "SSC Steno Exam 2026 | 11 September 1st Shift Review | Students ने बताया सच"
        val nodes = listOf(
            node(parent = null, left = 0, top = 0, right = 1080, bottom = 2400),
            node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 74, right = 1080, bottom = 682),
            node(parent = 0, left = 0, top = 682, right = 1080, bottom = 2337),
            node(
                text = title,
                clazz = "ViewGroup",
                parent = 2,
                left = 0,
                top = 724,
                right = 1080,
                bottom = 794,
            ),
            node(
                text = "@Shikshaaspot  ",
                clazz = "ViewGroup",
                parent = 2,
                left = 32,
                top = 810,
                right = 300,
                bottom = 859,
            ),
            node(
                text = "Subscribe",
                clazz = "Button",
                parent = 2,
                left = 700,
                top = 810,
                right = 900,
                bottom = 859,
            ),
            node(
                text = "5.4K views 9 days ago",
                clazz = "ViewGroup",
                parent = 2,
                left = 32,
                top = 812,
                right = 400,
                bottom = 858,
            ),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertTrue(result.watchScreenConfirmed)
        assertEquals(YoutubeV2ContentKind.VIDEO, result.kind)
        assertEquals(title, result.title)
        assertEquals("@shikshaaspot", result.exactHandle)
        assertTrue(result.hasOwnerEvidence)
    }


    private fun baseWatchNodes() = listOf(
        node(parent = null, left = 0, top = 0, right = 1080, bottom = 1920),
        node(id = "watch_player", clazz = "SurfaceView", parent = 0, left = 0, top = 100, right = 1080, bottom = 800),
        node(id = "video_title", text = "A title without SSC", clazz = "TextView", parent = 0, left = 20, top = 810, right = 900, bottom = 880),
    )

    private fun snapshot(nodes: List<YoutubeV2Node>) = YoutubeV2Snapshot(
        packageName = YoutubeStudyV2Parser.YOUTUBE_PACKAGE,
        density = 3f,
        screenWidth = 1080,
        screenHeight = 1920,
        nodes = nodes,
    )

    private fun node(
        text: String? = null,
        description: String? = null,
        id: String? = null,
        clazz: String? = "ViewGroup",
        parent: Int? = null,
        clickable: Boolean = false,
        left: Int = 0,
        top: Int = 0,
        right: Int = 1,
        bottom: Int = 1,
    ) = YoutubeV2Node(
        text = text,
        contentDescription = description,
        viewId = id,
        className = clazz,
        visibleToUser = true,
        clickable = clickable,
        parentIndex = parent,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )
}
