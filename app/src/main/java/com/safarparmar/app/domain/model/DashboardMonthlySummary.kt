package com.safarparmar.app.domain.model

import androidx.compose.runtime.Immutable
import com.safarparmar.app.util.Resource

@Immutable
data class DashboardMonthlySummary(
    val month: String = "",
    val consistencyScore: Double = 0.0,
    val completionRate: Double = 0.0,
    val focusDepth: Double = 0.0,
)

fun MonthlyReport.toDashboardSummary() = DashboardMonthlySummary(month, consistencyScore, completionRate, focusDepth)

fun Resource<MonthlyReport>.toDashboardSummary(): Resource<DashboardMonthlySummary> = when (this) {
    is Resource.Success -> Resource.Success(data.toDashboardSummary())
    is Resource.Error -> Resource.Error(message, code, errorCode)
    is Resource.Loading -> Resource.Loading()
}

/** Only missing additive routes can fall back. Preserve authentication and other errors. */
suspend fun loadDashboardMonthlySummary(
    enabled: Boolean,
    compact: suspend () -> Resource<DashboardMonthlySummary>,
    legacy: suspend () -> Resource<DashboardMonthlySummary>,
): Resource<DashboardMonthlySummary> {
    if (!enabled) return legacy()
    val result = compact()
    return if (result is Resource.Error && result.code in setOf(404, 405)) legacy() else result
}
