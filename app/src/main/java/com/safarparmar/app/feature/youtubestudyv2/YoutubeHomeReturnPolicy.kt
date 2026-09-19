package com.safarparmar.app.feature.youtubestudyv2

/** Bounded navigation: never issue a global Back that could finish YouTube. */
internal class YoutubeHomeReturnPolicy {
    enum class Action { SELECT_HOME, OPEN_HOME, HOME_READY, WAIT, ABORT }
    private var attempts = 0
    private var selectedHome = false
    private var openedHome = false

    fun next(youtubeForeground: Boolean, watchScreen: Boolean, homeAvailable: Boolean, homeSelected: Boolean): Action {
        if (!youtubeForeground || ++attempts > 18) return Action.ABORT
        if (homeAvailable && !selectedHome) {
            selectedHome = true
            return Action.SELECT_HOME
        }
        if (!watchScreen && homeAvailable && homeSelected) return Action.HOME_READY
        if (!openedHome && attempts >= 3) {
            openedHome = true
            selectedHome = false
            return Action.OPEN_HOME
        }
        return Action.WAIT
    }
}
