package com.safarparmar.app.feature.kavachanalytics.data

import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory

/**
 * SAFAR's shipped starting point for app categories.
 *
 * These are only defaults. A student override always wins, is what syncs with their
 * account, and is what historical aggregates are reclassified to. Anything not
 * listed here stays [AppCategory.UNCLASSIFIED] — an app SAFAR has never heard of is
 * never assumed to be a distraction.
 */
object AppCategoryDefaults {

    private val DISTRACTING = setOf(
        "com.instagram.android",
        "com.instagram.lite",
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.snapchat.android",
        "com.twitter.android",
        "com.x.android",
        "com.reddit.frontpage",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.netflix.mediaclient",
        "in.startv.hotstar",
        "com.amazon.avod.thirdpartyclient",
        "com.spotify.music",
        "com.pinterest",
        "com.tinder",
        "com.bumble.app",
        "com.dream11.dream11",
        "com.mpl.androidapp",
        "com.supercell.clashofclans",
        "com.dts.freefireth",
        "com.activision.callofduty.shooter",
        "com.tencent.ig", // PUBG / BGMI family
        "com.pubg.imobile",
        "com.pubg.krmobile",
        "com.king.candycrushsaga",
        "com.sharechat.app",
        "in.mohalla.sharechat",
        "com.moj.core",
        "com.eterno", // Dailyhunt
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.discord",
    )

    private val PRODUCTIVE = setOf(
        "com.safarparmar.app",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.keep",
        "com.google.android.calendar",
        "com.microsoft.office.word",
        "com.microsoft.office.onenote",
        "com.microsoft.todos",
        "com.notion.id",
        "com.evernote",
        "com.adobe.reader",
        "com.google.android.apps.classroom",
        "com.byjus.thelearningapp",
        "com.unacademyapp",
        "org.khanacademy.android",
        "com.duolingo",
        "com.anydo",
        "com.todoist",
        "com.dictionary",
        "com.google.android.apps.translate",
        "com.wolfram.android.alpha",
        "com.zoom.us",
        "us.zoom.videomeetings",
        "com.google.android.apps.meetings",
        "com.microsoft.teams",
    )

    private val NEUTRAL = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts",
        "com.google.android.contacts",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.android.calculator2",
        "com.google.android.calculator",
        "com.android.camera",
        "com.google.android.GoogleCamera",
        "com.android.gallery3d",
        "com.google.android.apps.photos",
        "com.google.android.gm",
        "com.google.android.apps.maps",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "com.android.chrome",
        "org.mozilla.firefox",
    )

    /** Package → default category. Absent means [AppCategory.UNCLASSIFIED]. */
    val DEFAULTS: Map<String, AppCategory> = buildMap {
        DISTRACTING.forEach { put(it, AppCategory.DISTRACTING) }
        PRODUCTIVE.forEach { put(it, AppCategory.PRODUCTIVE) }
        NEUTRAL.forEach { put(it, AppCategory.NEUTRAL) }
    }

    fun categoryFor(packageName: String): AppCategory =
        DEFAULTS[packageName] ?: AppCategory.UNCLASSIFIED
}
