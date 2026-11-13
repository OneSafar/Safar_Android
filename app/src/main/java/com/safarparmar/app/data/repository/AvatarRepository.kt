package com.safarparmar.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safarparmar.app.data.remote.api.AuthApi
import com.safarparmar.app.data.remote.dto.AvatarDto
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.domain.model.AvatarItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor(
    private val authApi: AuthApi,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val gson = Gson()
    private var cachedAvatars: List<AvatarItem>? = null

    /**
     * Returns the full list of preset avatars.
     * Tries remote API first, and automatically falls back to the bundled 70 avatars from assets.
     */
    suspend fun getAvatars(forceRefresh: Boolean = false): List<AvatarItem> = withContext(ioDispatcher) {
        if (!forceRefresh && cachedAvatars != null && cachedAvatars!!.isNotEmpty()) {
            return@withContext cachedAvatars!!
        }

        try {
            val response = authApi.getAvatars()
            if (response.isSuccessful) {
                val dtoList = response.body()
                if (!dtoList.isNullOrEmpty()) {
                    val mapped = dtoList.map { dto ->
                        AvatarItem(
                            id = dto.id,
                            name = dto.name,
                            category = dto.category,
                            webpUrl = dto.webpUrl,
                            svgUrl = dto.svgUrl ?: "",
                        )
                    }
                    cachedAvatars = mapped
                    return@withContext mapped
                }
            }
        } catch (_: Exception) {
            // Network error / offline: silently fall back to bundled catalog
        }

        val bundled = getBundledAvatars()
        if (bundled.isNotEmpty()) {
            cachedAvatars = bundled
        }
        bundled
    }

    /**
     * Synchronously loads all 70 avatars from the bundled assets.
     * Takes ~2-4ms, ensuring zero waiting time on screen launch.
     */
    fun getBundledAvatars(): List<AvatarItem> {
        cachedAvatars?.let { if (it.isNotEmpty()) return it }
        return try {
            context.assets.open("avatars/avatars.json").use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<AvatarDto>>() {}.type
                val dtoList: List<AvatarDto> = gson.fromJson(json, type) ?: emptyList()
                val list = dtoList.map { dto ->
                    AvatarItem(
                        id = dto.id,
                        name = dto.name,
                        category = dto.category,
                        webpUrl = dto.webpUrl,
                        svgUrl = dto.svgUrl ?: "",
                    )
                }
                if (list.isNotEmpty()) {
                    cachedAvatars = list
                }
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
