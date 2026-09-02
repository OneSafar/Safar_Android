package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeStudyV2ParserTest {
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
