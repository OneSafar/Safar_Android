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
            node(text = "@SAFARPARMAR", clazz = "TextView", parent = 0, left = 20, top = 810, right = 300, bottom = 860),
            // A handle outside the bounded owner region must not be selected.
            node(text = "@wrongcomment", clazz = "TextView", parent = 0, left = 20, top = 1700, right = 300, bottom = 1760),
        )

        val result = YoutubeStudyV2Parser.parse(snapshot(nodes))

        assertEquals("@safarparmar", result.exactHandle)
        assertEquals("SAFAR_PARMAR", result.displayName)
        assertTrue(result.hasOwnerEvidence)
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
