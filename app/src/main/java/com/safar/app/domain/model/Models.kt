package com.safar.app.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val exam: String?,
    val stage: String?,
    val gender: String?
)
//
//enum class Mood(val emoji: String, val label: String) {
//    JOYFUL("😄", "Joyful"),
//    HAPPY("😊", "Happy"),
//    NEUTRAL("😐", "Neutral"),
//    SAD("😢", "Sad"),
//    ANXIOUS("😰", "Anxious"),
//    ANGRY("😠", "Angry")
//}
//
//data class MoodCheckIn(
//    val id: Long,
//    val mood: Mood,
//    val intensity: Int,
//    val reason: String?,
//    val contextTags: List<String>,
//    val note: String?,
//    val timestamp: Long
//)
//
//data class JournalEntry(
//    val id: Long,
//    val title: String,
//    val content: String,
//    val moodTag: String?,
//    val inspirationPrompt: String?,
//    val timestamp: Long,
//    val updatedAt: Long
//)
//
//data class Goal(
//    val id: Long,
//    val title: String,
//    val description: String?,
//    val category: String,
//    val targetValue: Float,
//    val currentValue: Float,
//    val unit: String?,
//    val isRepeating: Boolean,
//    val repeatFrequency: String?,
//    val isCompleted: Boolean,
//    val deadline: Long?,
//    val createdAt: Long,
//    val completedAt: Long?
//) {
//    val progress: Float get() = if (targetValue > 0) (currentValue / targetValue).coerceIn(0f, 1f) else 0f
//}
//
//data class Streak(
//    val type: StreakType,
//    val currentStreak: Int,
//    val longestStreak: Int,
//    val lastActivityDate: Long?,
//    val totalCount: Int
//)
//
//enum class StreakType(val label: String) {
//    CHECK_IN("Check-In Streak"),
//    LOGIN("Login Streak"),
//    GOAL("Goal Streak")
//}
//
//data class Badge(
//    val id: String,
//    val title: String,
//    val description: String,
//    val category: String,
//    val rarity: BadgeRarity,
//    val iconName: String,
//    val isUnlocked: Boolean,
//    val unlockedAt: Long?,
//    val progress: Float,
//    val maxProgress: Float
//)
//
//enum class BadgeRarity(val label: String) {
//    COMMON("Common"),
//    RARE("Rare"),
//    EPIC("Epic"),
//    LEGENDARY("Legendary")
//}
//
//data class Post(
//    val id: String,
//    val authorId: String,
//    val authorName: String,
//    val authorPhotoUrl: String?,
//    val content: String,
//    val category: PostCategory,
//    val isAnonymous: Boolean,
//    val reactionsCount: Int,
//    val commentsCount: Int,
//    val hasReacted: Boolean,
//    val timestamp: Long
//)
//
//enum class PostCategory(val label: String) {
//    ALL("All"),
//    ACADEMIC("Academic"),
//    THOUGHTS("Thoughts")
//}
//
//data class Message(
//    val id: String,
//    val conversationId: String,
//    val senderId: String,
//    val content: String,
//    val isRead: Boolean,
//    val timestamp: Long
//)
//
//data class Conversation(
//    val id: String,
//    val participantId: String,
//    val participantName: String,
//    val participantPhotoUrl: String?,
//    val lastMessage: String?,
//    val unreadCount: Int,
//    val timestamp: Long
//)
//
//data class EkagraSession(
//    val id: Long,
//    val taskName: String?,
//    val focusDuration: Int,
//    val breakDuration: Int,
//    val completedPomodoros: Int,
//    val totalFocusTime: Int,
//    val date: Long
//)
//
//data class Course(
//    val id: String,
//    val title: String,
//    val description: String,
//    val thumbnailUrl: String?,
//    val youtubeUrl: String?,
//    val price: Float,
//    val isPurchased: Boolean,
//    val modules: List<CourseModule>?
//)
//
//data class CourseModule(
//    val id: String,
//    val title: String,
//    val duration: Int,
//    val youtubeUrl: String?,
//    val isCompleted: Boolean
//)
//
//data class BreathingTechnique(
//    val id: String,
//    val name: String,
//    val description: String,
//    val inhaleSeconds: Int,
//    val holdSeconds: Int,
//    val exhaleSeconds: Int,
//    val holdAfterExhaleSeconds: Int,
//    val cycles: Int,
//    val colorHex: String
//)
//
//data class Analytics(
//    val consistencyScore: Float,
//    val completionRate: Float,
//    val focusDepth: Float,
//    val skillRadar: Map<String, Float>,
//    val powerHour: Map<String, Float>,
//    val moodInsight: Map<String, Float>,
//    val heatmap: Map<String, Int>,
//    val weeklyMoodChart: List<MoodDataPoint>,
//    val monthlyStats: List<DailyStatPoint>
//)
//
//data class MoodDataPoint(val day: String, val mood: String, val intensity: Float)
//data class DailyStatPoint(val date: Long, val score: Float)
//data class Quote(val text: String, val author: String)
