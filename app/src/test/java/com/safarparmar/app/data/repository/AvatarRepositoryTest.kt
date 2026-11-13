package com.safarparmar.app.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.safarparmar.app.data.remote.api.AuthApi
import com.safarparmar.app.data.remote.dto.AvatarDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class AvatarRepositoryTest {

    private lateinit var authApi: AuthApi
    private lateinit var context: Context
    private lateinit var assetManager: AssetManager
    private lateinit var avatarRepository: AvatarRepository

    private val sampleJson = """
        [
          {
            "id": "char-1",
            "name": "The Thinker",
            "category": "characters",
            "webpUrl": "/avatars/webp/The_Thinker.webp",
            "svgUrl": "/avatars/svg/The_Thinker.svg"
          },
          {
            "id": "anim-1",
            "name": "Retro Robot",
            "category": "animals",
            "webpUrl": "/avatars/webp/Retro_Robot.webp",
            "svgUrl": "/avatars/svg/Retro_Robot.svg"
          }
        ]
    """.trimIndent()

    @Before
    fun setup() {
        authApi = mockk(relaxed = true)
        context = mockk(relaxed = true)
        assetManager = mockk(relaxed = true)
        every { context.assets } returns assetManager
        every { assetManager.open("avatars/avatars.json") } answers {
            ByteArrayInputStream(sampleJson.toByteArray(Charsets.UTF_8))
        }

        avatarRepository = AvatarRepository(
            authApi = authApi,
            context = context,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun getBundledAvatars_parsesCorrectly() {
        val list = avatarRepository.getBundledAvatars()
        assertEquals(2, list.size)
        assertEquals("char-1", list[0].id)
        assertEquals("The Thinker", list[0].name)
        assertEquals("characters", list[0].category)
        assertEquals("/avatars/webp/The_Thinker.webp", list[0].webpUrl)

        assertEquals("anim-1", list[1].id)
        assertEquals("Retro Robot", list[1].name)
        assertEquals("animals", list[1].category)
    }

    @Test
    fun realAssetFile_hasExactly70Avatars() {
        val realAssetFile = File("src/main/assets/avatars/avatars.json")
        assertTrue("realAssetFile must exist", realAssetFile.exists())
        every { assetManager.open("avatars/avatars.json") } answers {
            FileInputStream(realAssetFile)
        }

        val list = avatarRepository.getBundledAvatars()
        assertEquals(70, list.size)

        val characters = list.filter { it.category == "characters" }
        val animals = list.filter { it.category == "animals" }
        assertEquals(35, characters.size)
        assertEquals(35, animals.size)

        // Verify every webp file mentioned exists on disk in assets/avatars/webp/
        for (item in list) {
            val fileName = item.webpUrl.substringAfterLast("/")
            val webpFile = File("src/main/assets/avatars/webp", fileName)
            assertTrue("WebP file $fileName should exist for avatar ${item.name}", webpFile.exists())
        }
    }

    @Test
    fun getAvatars_fallsBackToBundledWhenApiFails() = runBlocking {
        coEvery { authApi.getAvatars() } throws RuntimeException("Network error")

        val list = avatarRepository.getAvatars(forceRefresh = true)
        assertEquals(2, list.size)
        assertEquals("The Thinker", list[0].name)
    }

    @Test
    fun getAvatars_usesRemoteWhenSuccessful() = runBlocking {
        val remoteList = listOf(
            AvatarDto(
                id = "char-99",
                name = "New Hero",
                category = "characters",
                webpUrl = "/avatars/webp/New_Hero.webp",
            )
        )
        coEvery { authApi.getAvatars() } returns Response.success(remoteList)

        val list = avatarRepository.getAvatars(forceRefresh = true)
        assertEquals(1, list.size)
        assertEquals("New Hero", list[0].name)
    }
}
