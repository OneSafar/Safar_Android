package com.safarparmar.app.domain.model

import com.safarparmar.app.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DashboardMonthlySummaryTest {
    @Test fun `summary retains exact card values`() {
        val report = MonthlyReport(month = "2026-10", consistencyScore = 22.7, completionRate = 13.4, focusDepth = 30.1)
        assertEquals(DashboardMonthlySummary("2026-10", 22.7, 13.4, 30.1), report.toDashboardSummary())
        assertEquals(DashboardMonthlySummary(), MonthlyReport().toDashboardSummary())
    }
    @Test fun `default disabled never calls compact provider`() = runBlocking {
        var compactCalls = 0
        val result = loadDashboardMonthlySummary(false, { compactCalls++; Resource.Success(DashboardMonthlySummary("compact")) }, { Resource.Success(DashboardMonthlySummary("legacy")) })
        assertEquals(0, compactCalls); assertEquals("legacy", (result as Resource.Success).data.month)
    }
    @Test fun `only unavailable routes fall back`() = runBlocking {
        for (code in listOf(404, 405, 401, 403, 429, 500)) {
            var legacyCalls = 0
            val result = loadDashboardMonthlySummary(true, { Resource.Error("error", code, "test") }, { legacyCalls++; Resource.Success(DashboardMonthlySummary("legacy")) })
            assertEquals(if (code in listOf(404, 405)) 1 else 0, legacyCalls)
            if (code !in listOf(404, 405)) assertEquals(code, (result as Resource.Error).code)
        }
    }
}
