package com.safarparmar.app.ui.leaderboard

import org.junit.Assert.assertEquals
import org.junit.Test

class LeaderboardPaginationTest {
    @Test fun fivePagesPartitionTop100WithoutGapsOrDuplicates() {
        val pages = (1..5).map { leaderboardRankRange(it).toList() }
        pages.forEach { assertEquals(20, it.size) }
        assertEquals((1..100).toList(), pages.flatten())
        assertEquals((81..100).toList(), pages.last())
    }
    @Test fun outOfRangePagesStayWithinTop100() {
        assertEquals(1..20, leaderboardRankRange(0))
        assertEquals(81..100, leaderboardRankRange(6))
    }
}
