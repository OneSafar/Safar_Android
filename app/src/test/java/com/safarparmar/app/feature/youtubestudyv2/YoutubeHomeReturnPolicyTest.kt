package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Test
import com.safarparmar.app.feature.youtubestudyv2.YoutubeHomeReturnPolicy.Action

class YoutubeHomeReturnPolicyTest {
    @Test fun `Shorts tab returns through Home and waits for the player to disappear`() {
        val policy = YoutubeHomeReturnPolicy()
        assertEquals(Action.SELECT_HOME, policy.next(true, true, true, false))
        assertEquals(Action.WAIT, policy.next(true, true, true, true))
        assertEquals(Action.HOME_READY, policy.next(true, false, true, true))
    }

    @Test fun `deep linked player opens YouTube Home once and never uses Back`() {
        val policy = YoutubeHomeReturnPolicy()
        assertEquals(Action.WAIT, policy.next(true, true, false, false))
        assertEquals(Action.WAIT, policy.next(true, true, false, false))
        assertEquals(Action.OPEN_HOME, policy.next(true, true, false, false))
        repeat(15) { assertEquals(Action.WAIT, policy.next(true, true, false, false)) }
        assertEquals(Action.ABORT, policy.next(true, true, false, false))
    }

    @Test fun `leaving YouTube cancels navigation`() {
        assertEquals(Action.ABORT, YoutubeHomeReturnPolicy().next(false, false, false, false))
    }

    @Test fun `unselected Home is never treated as ready to refresh`() {
        val policy = YoutubeHomeReturnPolicy()
        assertEquals(Action.SELECT_HOME, policy.next(true, false, true, false))
        assertEquals(Action.WAIT, policy.next(true, false, true, false))
        assertEquals(Action.OPEN_HOME, policy.next(true, false, true, false))
    }
}
