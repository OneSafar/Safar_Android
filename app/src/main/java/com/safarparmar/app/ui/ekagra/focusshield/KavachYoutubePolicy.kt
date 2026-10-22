package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2AccessibilityService
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Parser
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Preferences

/** YouTube Focus owns YouTube while connected; Kavach still owns every other app. */
internal object KavachYoutubePolicy {
    fun isManagedByYoutubeFocus(context: Context, packageName: String): Boolean =
        packageName == YoutubeStudyV2Parser.YOUTUBE_PACKAGE && isManagedByYoutubeFocus(
            packageName,
            focusEnabled = YoutubeStudyV2Preferences.isEnabled(context),
            serviceConnected = YoutubeStudyV2AccessibilityService.isConnected,
        )

    fun isManagedByYoutubeFocus(
        packageName: String,
        focusEnabled: Boolean,
        serviceConnected: Boolean,
    ): Boolean = packageName == YoutubeStudyV2Parser.YOUTUBE_PACKAGE &&
        focusEnabled && serviceConnected
}
