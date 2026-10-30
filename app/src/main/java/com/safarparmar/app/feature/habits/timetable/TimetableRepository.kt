package com.safarparmar.app.feature.habits.timetable

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safarparmar.app.data.local.SafarDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.timetableData by preferencesDataStore("habit_timetable")
enum class SessionKind(val label: String, val symbol: String) { STUDY("Study", "📖"), BREAK("Break", "☕"), HABIT("Routine", "🌱"), OTHER("Other", "📝") }
enum class SessionRepeat(val label: String) { NONE("Does not repeat"), DAILY("Every day"), WEEKDAYS("Weekdays"), WEEKLY("Every week") }

@Keep
data class TimetableSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val subject: String = "General",
    val kind: SessionKind = SessionKind.STUDY,
    val date: String = LocalDate.now().toString(),
    val startMinute: Int = 540,
    val endMinute: Int = 600,
    val repeat: SessionRepeat = SessionRepeat.NONE,
    val reminderMinutes: Int? = null,
    val color: Int = 0,
    val trackAsHabit: Boolean = false,
    val completedDates: Set<String> = emptySet()
) {
    fun occursOn(day: LocalDate): Boolean {
        val first = LocalDate.parse(date)
        if (day < first) return false
        return when (repeat) {
            SessionRepeat.NONE -> day == first
            SessionRepeat.DAILY -> true
            SessionRepeat.WEEKDAYS -> day.dayOfWeek.value <= 5
            SessionRepeat.WEEKLY -> day.dayOfWeek == first.dayOfWeek
        }
    }
    fun isDone(day: LocalDate) = day.toString() in completedDates
    fun validate() {
        require(title.isNotBlank()) { "Enter a session title." }
        require(title.length <= 100 && subject.length <= 60) { "Use a shorter title or subject." }
        LocalDate.parse(date)
        require(startMinute in 0..1438 && endMinute in 1..1439 && endMinute > startMinute) { "End time must be after start time on the same day." }
        require(reminderMinutes == null || reminderMinutes in listOf(0, 5, 10, 15, 30))
        require(color in 0..4)
    }
}

fun sessionTime(minute: Int): String = LocalTime.of(minute / 60, minute % 60)
    .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

/** Find an occurrence by local wall time, including reminders falling on the previous date. */
internal fun nextTimetableReminder(session: TimetableSession, now: ZonedDateTime, from: LocalDate = now.toLocalDate()): Pair<LocalDate, ZonedDateTime>? {
    val minutes = session.reminderMinutes ?: return null
    val start = maxOf(from, LocalDate.parse(session.date))
    return (0L..7L).asSequence().map { start.plusDays(it) }.filter(session::occursOn)
        .map { day -> day to day.atTime(session.startMinute / 60, session.startMinute % 60).atZone(now.zone).minusMinutes(minutes.toLong()) }
        .firstOrNull { it.second > now }
}

/** Account-scoped local timetable. The habit API has no timetable endpoint. */
@Singleton
class TimetableRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    val owner: Flow<String?>,
    private val scheduleReminder: (String, TimetableSession) -> Unit,
    private val cancelReminder: (String, String) -> Unit
) {
    @Inject constructor(@ApplicationContext context: Context, accounts: SafarDataStore) : this(
        context.timetableData,
        combine(accounts.userId, accounts.isLoggedIn) { id, loggedIn -> id?.takeIf { loggedIn && it.isNotBlank() } }.distinctUntilChanged(),
        { user, session -> TimetableReminderWorker.schedule(context, user, session) },
        { user, id -> TimetableReminderWorker.cancel(context, user, id) }
    )
    private val gson = Gson()
    private fun key(owner: String) = stringPreferencesKey("sessions_$owner")
    private fun decode(json: String?): List<TimetableSession> = if (json == null) emptyList() else
        gson.fromJson(json, object : TypeToken<List<TimetableSession>>() {}.type)
    val sessions: Flow<List<TimetableSession>> = combine(owner, dataStore.data) { user, prefs ->
        if (user == null) emptyList() else decode(prefs[key(user)])
    }.flowOn(Dispatchers.Default)
    suspend fun save(session: TimetableSession) = withContext(Dispatchers.IO) {
        session.validate()
        val user = owner.first() ?: error("Sign in to save your timetable.")
        dataStore.edit { prefs ->
            check(owner.first() == user) { "Account changed. Try again." }
            val existing = decode(prefs[key(user)])
            // Always retain the latest completions when an editor has an older snapshot.
            val value = session.copy(completedDates = existing.find { it.id == session.id }?.completedDates ?: session.completedDates)
            prefs[key(user)] = gson.toJson(existing.filterNot { it.id == session.id } + value)
        }
        scheduleReminder(user, session)
    }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val user = owner.first() ?: error("Sign in to edit your timetable.")
        dataStore.edit { prefs ->
            check(owner.first() == user) { "Account changed. Try again." }
            prefs[key(user)] = gson.toJson(decode(prefs[key(user)]).filterNot { it.id == id })
        }
        cancelReminder(user, id)
    }
    suspend fun toggle(id: String, date: LocalDate) = withContext(Dispatchers.IO) {
        require(date <= LocalDate.now()) { "Future sessions cannot be completed early." }
        val user = owner.first() ?: error("Sign in to update your timetable.")
        dataStore.edit { prefs ->
            check(owner.first() == user) { "Account changed. Try again." }
            val values = decode(prefs[key(user)]).map { session ->
                if (session.id != id) session else {
                    require(session.occursOn(date)) { "This session is not scheduled for this date." }
                    val dates = session.completedDates
                    session.copy(completedDates = if (date.toString() in dates) dates - date.toString() else dates + date.toString())
                }
            }
            prefs[key(user)] = gson.toJson(values)
        }
    }
}
