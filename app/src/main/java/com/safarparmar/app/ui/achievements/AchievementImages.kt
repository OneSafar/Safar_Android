package com.safarparmar.app.ui.achievements

import android.net.Uri
import com.safarparmar.app.BuildConfig

/**
 * Maps achievement ids to CDN-relative artwork paths and resolves absolute URLs.
 * Shared by Dashboard, Achievements, and Nishtha analytics.
 */
object AchievementImages {
    val paths: Map<String, String> = mapOf(
        "G001" to "/Achievments/Badges/Badge (1).webp",
        "G002" to "/Achievments/Badges/Badge (2).webp",
        "G003" to "/Achievments/Badges/Badge (3).webp",
        "G004" to "/Achievments/Badges/Badge (4).webp",
        "F001" to "/Achievments/Badges/Special_Badge (2).webp",
        "F002" to "/Achievments/Badges/Special_Badge (5).webp",
        "F003" to "/Achievments/Badges/Special_Badge (4).webp",
        "F004" to "/Achievments/Badges/Badge (6).webp",
        "F005" to "/Achievments/Badges/Badge (7).webp",
        "S001" to "/Achievments/Badges/Badge (8).webp",
        "S002" to "/Achievments/Badges/Special_Badge (1).webp",
        "ET006" to "/Achievments/Badges/Special_Badge (3).webp",
        "SP001" to "/Achievments/Badges/Badge (4).webp",
        "SP002" to "/Achievments/Badges/Badge (8).webp",
        "D001" to "/Achievments/Badges/Special_Badge (1).webp",
        "D002" to "/Achievments/Badges/Special_Badge (3).webp",
        "K001" to "/Achievments/Badges/Badge (6).webp",
        "M001" to "/Achievments/Badges/Badge (7).webp",
        "T005" to "/Achievments/Titles/Title (5).webp",
        "T006" to "/Achievments/Titles/Title (3).webp",
        "T007" to "/Achievments/Titles/Title (7).webp",
        "T008" to "/Achievments/Titles/Title (6).webp",
        "T001" to "/Achievments/Titles/Title (8).webp",
        "T002" to "/Achievments/Titles/Title (2).webp",
        "T003" to "/Achievments/Titles/Title (1).webp",
        "T004" to "/Achievments/Titles/Title (4).webp",
        "ET001" to "/Achievments/Titles/Special_Title (2).webp",
        "ET002" to "/Achievments/Titles/Special_Title (1).webp",
        "ET003" to "/Achievments/Titles/Special_Title (5).webp",
        "ET004" to "/Achievments/Titles/Special_Title (3).webp",
        "ET005" to "/Achievments/Titles/Special_Title (4).webp",
        "T010" to "/Achievments/Titles/Special_Title (1).webp",
        "T011" to "/Achievments/Titles/Special_Title (2).webp",
        "T012" to "/Achievments/Titles/Special_Title (3).webp",
        "T013" to "/Achievments/Titles/Special_Title (4).webp",
        "T014" to "/Achievments/Titles/Special_Title (5).webp",
        "T009" to "/Achievments/svgviewer-output.svg",
    )

    fun pathFor(id: String): String? = paths[id]

    fun urlFor(id: String): String? {
        val path = paths[id] ?: return null
        return resolveUrl(path)
    }

    fun resolveUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return try {
            val origin = BuildConfig.BASE_URL.trimEnd('/').let {
                val uri = Uri.parse(it)
                "${uri.scheme}://${uri.host}"
            }
            "$origin$path"
        } catch (_: Exception) {
            null
        }
    }
}
