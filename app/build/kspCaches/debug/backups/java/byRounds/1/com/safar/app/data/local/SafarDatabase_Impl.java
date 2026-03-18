package com.safar.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.safar.app.data.local.dao.BadgeDao;
import com.safar.app.data.local.dao.BadgeDao_Impl;
import com.safar.app.data.local.dao.DailyStatsDao;
import com.safar.app.data.local.dao.DailyStatsDao_Impl;
import com.safar.app.data.local.dao.EkagraDao;
import com.safar.app.data.local.dao.EkagraDao_Impl;
import com.safar.app.data.local.dao.GoalDao;
import com.safar.app.data.local.dao.GoalDao_Impl;
import com.safar.app.data.local.dao.JournalDao;
import com.safar.app.data.local.dao.JournalDao_Impl;
import com.safar.app.data.local.dao.MessageDao;
import com.safar.app.data.local.dao.MessageDao_Impl;
import com.safar.app.data.local.dao.MoodCheckInDao;
import com.safar.app.data.local.dao.MoodCheckInDao_Impl;
import com.safar.app.data.local.dao.PostDao;
import com.safar.app.data.local.dao.PostDao_Impl;
import com.safar.app.data.local.dao.StreakDao;
import com.safar.app.data.local.dao.StreakDao_Impl;
import com.safar.app.data.local.dao.UserDao;
import com.safar.app.data.local.dao.UserDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SafarDatabase_Impl extends SafarDatabase {
  private volatile UserDao _userDao;

  private volatile MoodCheckInDao _moodCheckInDao;

  private volatile JournalDao _journalDao;

  private volatile GoalDao _goalDao;

  private volatile StreakDao _streakDao;

  private volatile BadgeDao _badgeDao;

  private volatile PostDao _postDao;

  private volatile MessageDao _messageDao;

  private volatile EkagraDao _ekagraDao;

  private volatile DailyStatsDao _dailyStatsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `photoUrl` TEXT, `exam` TEXT, `stage` TEXT, `gender` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `mood_checkins` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mood` TEXT NOT NULL, `intensity` INTEGER NOT NULL, `reason` TEXT, `contextTags` TEXT NOT NULL, `note` TEXT, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `journal_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `moodTag` TEXT, `inspirationPrompt` TEXT, `timestamp` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `category` TEXT NOT NULL, `targetValue` REAL NOT NULL, `currentValue` REAL NOT NULL, `unit` TEXT, `isRepeating` INTEGER NOT NULL, `repeatFrequency` TEXT, `isCompleted` INTEGER NOT NULL, `deadline` INTEGER, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `streaks` (`type` TEXT NOT NULL, `currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, `lastActivityDate` INTEGER, `totalCount` INTEGER NOT NULL, PRIMARY KEY(`type`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `badges` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `rarity` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isUnlocked` INTEGER NOT NULL, `unlockedAt` INTEGER, `progress` REAL NOT NULL, `maxProgress` REAL NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `posts` (`id` TEXT NOT NULL, `authorId` TEXT NOT NULL, `authorName` TEXT NOT NULL, `authorPhotoUrl` TEXT, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `isAnonymous` INTEGER NOT NULL, `reactionsCount` INTEGER NOT NULL, `commentsCount` INTEGER NOT NULL, `hasReacted` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `content` TEXT NOT NULL, `isRead` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ekagra_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskName` TEXT, `focusDuration` INTEGER NOT NULL, `breakDuration` INTEGER NOT NULL, `completedPomodoros` INTEGER NOT NULL, `totalFocusTime` INTEGER NOT NULL, `date` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` (`date` INTEGER NOT NULL, `checkInsCount` INTEGER NOT NULL, `goalsCompleted` INTEGER NOT NULL, `journalEntries` INTEGER NOT NULL, `focusMinutes` INTEGER NOT NULL, `consistencyScore` REAL NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'af00d36d84389bff17453321c5800d3d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `mood_checkins`");
        db.execSQL("DROP TABLE IF EXISTS `journal_entries`");
        db.execSQL("DROP TABLE IF EXISTS `goals`");
        db.execSQL("DROP TABLE IF EXISTS `streaks`");
        db.execSQL("DROP TABLE IF EXISTS `badges`");
        db.execSQL("DROP TABLE IF EXISTS `posts`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `ekagra_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `daily_stats`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(8);
        _columnsUsers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("photoUrl", new TableInfo.Column("photoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("exam", new TableInfo.Column("exam", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("stage", new TableInfo.Column("stage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("gender", new TableInfo.Column("gender", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.safar.app.data.local.entity.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsMoodCheckins = new HashMap<String, TableInfo.Column>(7);
        _columnsMoodCheckins.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodCheckins.put("mood", new TableInfo.Column("mood", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodCheckins.put("intensity", new TableInfo.Column("intensity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodCheckins.put("reason", new TableInfo.Column("reason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodCheckins.put("contextTags", new TableInfo.Column("contextTags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodCheckins.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodCheckins.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMoodCheckins = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMoodCheckins = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMoodCheckins = new TableInfo("mood_checkins", _columnsMoodCheckins, _foreignKeysMoodCheckins, _indicesMoodCheckins);
        final TableInfo _existingMoodCheckins = TableInfo.read(db, "mood_checkins");
        if (!_infoMoodCheckins.equals(_existingMoodCheckins)) {
          return new RoomOpenHelper.ValidationResult(false, "mood_checkins(com.safar.app.data.local.entity.MoodCheckInEntity).\n"
                  + " Expected:\n" + _infoMoodCheckins + "\n"
                  + " Found:\n" + _existingMoodCheckins);
        }
        final HashMap<String, TableInfo.Column> _columnsJournalEntries = new HashMap<String, TableInfo.Column>(7);
        _columnsJournalEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntries.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntries.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntries.put("moodTag", new TableInfo.Column("moodTag", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntries.put("inspirationPrompt", new TableInfo.Column("inspirationPrompt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntries.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntries.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJournalEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJournalEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoJournalEntries = new TableInfo("journal_entries", _columnsJournalEntries, _foreignKeysJournalEntries, _indicesJournalEntries);
        final TableInfo _existingJournalEntries = TableInfo.read(db, "journal_entries");
        if (!_infoJournalEntries.equals(_existingJournalEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "journal_entries(com.safar.app.data.local.entity.JournalEntryEntity).\n"
                  + " Expected:\n" + _infoJournalEntries + "\n"
                  + " Found:\n" + _existingJournalEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsGoals = new HashMap<String, TableInfo.Column>(13);
        _columnsGoals.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("targetValue", new TableInfo.Column("targetValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("currentValue", new TableInfo.Column("currentValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("unit", new TableInfo.Column("unit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("isRepeating", new TableInfo.Column("isRepeating", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("repeatFrequency", new TableInfo.Column("repeatFrequency", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("deadline", new TableInfo.Column("deadline", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGoals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGoals = new TableInfo("goals", _columnsGoals, _foreignKeysGoals, _indicesGoals);
        final TableInfo _existingGoals = TableInfo.read(db, "goals");
        if (!_infoGoals.equals(_existingGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "goals(com.safar.app.data.local.entity.GoalEntity).\n"
                  + " Expected:\n" + _infoGoals + "\n"
                  + " Found:\n" + _existingGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsStreaks = new HashMap<String, TableInfo.Column>(5);
        _columnsStreaks.put("type", new TableInfo.Column("type", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreaks.put("currentStreak", new TableInfo.Column("currentStreak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreaks.put("longestStreak", new TableInfo.Column("longestStreak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreaks.put("lastActivityDate", new TableInfo.Column("lastActivityDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreaks.put("totalCount", new TableInfo.Column("totalCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStreaks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStreaks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStreaks = new TableInfo("streaks", _columnsStreaks, _foreignKeysStreaks, _indicesStreaks);
        final TableInfo _existingStreaks = TableInfo.read(db, "streaks");
        if (!_infoStreaks.equals(_existingStreaks)) {
          return new RoomOpenHelper.ValidationResult(false, "streaks(com.safar.app.data.local.entity.StreakEntity).\n"
                  + " Expected:\n" + _infoStreaks + "\n"
                  + " Found:\n" + _existingStreaks);
        }
        final HashMap<String, TableInfo.Column> _columnsBadges = new HashMap<String, TableInfo.Column>(10);
        _columnsBadges.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("rarity", new TableInfo.Column("rarity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("iconName", new TableInfo.Column("iconName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("isUnlocked", new TableInfo.Column("isUnlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("unlockedAt", new TableInfo.Column("unlockedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("progress", new TableInfo.Column("progress", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBadges.put("maxProgress", new TableInfo.Column("maxProgress", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBadges = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBadges = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBadges = new TableInfo("badges", _columnsBadges, _foreignKeysBadges, _indicesBadges);
        final TableInfo _existingBadges = TableInfo.read(db, "badges");
        if (!_infoBadges.equals(_existingBadges)) {
          return new RoomOpenHelper.ValidationResult(false, "badges(com.safar.app.data.local.entity.BadgeEntity).\n"
                  + " Expected:\n" + _infoBadges + "\n"
                  + " Found:\n" + _existingBadges);
        }
        final HashMap<String, TableInfo.Column> _columnsPosts = new HashMap<String, TableInfo.Column>(11);
        _columnsPosts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("authorId", new TableInfo.Column("authorId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("authorName", new TableInfo.Column("authorName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("authorPhotoUrl", new TableInfo.Column("authorPhotoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("isAnonymous", new TableInfo.Column("isAnonymous", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("reactionsCount", new TableInfo.Column("reactionsCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("commentsCount", new TableInfo.Column("commentsCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("hasReacted", new TableInfo.Column("hasReacted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPosts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPosts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPosts = new TableInfo("posts", _columnsPosts, _foreignKeysPosts, _indicesPosts);
        final TableInfo _existingPosts = TableInfo.read(db, "posts");
        if (!_infoPosts.equals(_existingPosts)) {
          return new RoomOpenHelper.ValidationResult(false, "posts(com.safar.app.data.local.entity.PostEntity).\n"
                  + " Expected:\n" + _infoPosts + "\n"
                  + " Found:\n" + _existingPosts);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(6);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("conversationId", new TableInfo.Column("conversationId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderId", new TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isRead", new TableInfo.Column("isRead", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.safar.app.data.local.entity.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsEkagraSessions = new HashMap<String, TableInfo.Column>(7);
        _columnsEkagraSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEkagraSessions.put("taskName", new TableInfo.Column("taskName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEkagraSessions.put("focusDuration", new TableInfo.Column("focusDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEkagraSessions.put("breakDuration", new TableInfo.Column("breakDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEkagraSessions.put("completedPomodoros", new TableInfo.Column("completedPomodoros", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEkagraSessions.put("totalFocusTime", new TableInfo.Column("totalFocusTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEkagraSessions.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEkagraSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEkagraSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEkagraSessions = new TableInfo("ekagra_sessions", _columnsEkagraSessions, _foreignKeysEkagraSessions, _indicesEkagraSessions);
        final TableInfo _existingEkagraSessions = TableInfo.read(db, "ekagra_sessions");
        if (!_infoEkagraSessions.equals(_existingEkagraSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "ekagra_sessions(com.safar.app.data.local.entity.EkagraSessions).\n"
                  + " Expected:\n" + _infoEkagraSessions + "\n"
                  + " Found:\n" + _existingEkagraSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyStats = new HashMap<String, TableInfo.Column>(6);
        _columnsDailyStats.put("date", new TableInfo.Column("date", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("checkInsCount", new TableInfo.Column("checkInsCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("goalsCompleted", new TableInfo.Column("goalsCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("journalEntries", new TableInfo.Column("journalEntries", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("focusMinutes", new TableInfo.Column("focusMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("consistencyScore", new TableInfo.Column("consistencyScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyStats = new TableInfo("daily_stats", _columnsDailyStats, _foreignKeysDailyStats, _indicesDailyStats);
        final TableInfo _existingDailyStats = TableInfo.read(db, "daily_stats");
        if (!_infoDailyStats.equals(_existingDailyStats)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_stats(com.safar.app.data.local.entity.DailyStatsEntity).\n"
                  + " Expected:\n" + _infoDailyStats + "\n"
                  + " Found:\n" + _existingDailyStats);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "af00d36d84389bff17453321c5800d3d", "fba922e5f0877562e9d02ae765d8730e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","mood_checkins","journal_entries","goals","streaks","badges","posts","messages","ekagra_sessions","daily_stats");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `mood_checkins`");
      _db.execSQL("DELETE FROM `journal_entries`");
      _db.execSQL("DELETE FROM `goals`");
      _db.execSQL("DELETE FROM `streaks`");
      _db.execSQL("DELETE FROM `badges`");
      _db.execSQL("DELETE FROM `posts`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `ekagra_sessions`");
      _db.execSQL("DELETE FROM `daily_stats`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MoodCheckInDao.class, MoodCheckInDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JournalDao.class, JournalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GoalDao.class, GoalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StreakDao.class, StreakDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BadgeDao.class, BadgeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PostDao.class, PostDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EkagraDao.class, EkagraDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DailyStatsDao.class, DailyStatsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public MoodCheckInDao moodCheckInDao() {
    if (_moodCheckInDao != null) {
      return _moodCheckInDao;
    } else {
      synchronized(this) {
        if(_moodCheckInDao == null) {
          _moodCheckInDao = new MoodCheckInDao_Impl(this);
        }
        return _moodCheckInDao;
      }
    }
  }

  @Override
  public JournalDao journalDao() {
    if (_journalDao != null) {
      return _journalDao;
    } else {
      synchronized(this) {
        if(_journalDao == null) {
          _journalDao = new JournalDao_Impl(this);
        }
        return _journalDao;
      }
    }
  }

  @Override
  public GoalDao goalDao() {
    if (_goalDao != null) {
      return _goalDao;
    } else {
      synchronized(this) {
        if(_goalDao == null) {
          _goalDao = new GoalDao_Impl(this);
        }
        return _goalDao;
      }
    }
  }

  @Override
  public StreakDao streakDao() {
    if (_streakDao != null) {
      return _streakDao;
    } else {
      synchronized(this) {
        if(_streakDao == null) {
          _streakDao = new StreakDao_Impl(this);
        }
        return _streakDao;
      }
    }
  }

  @Override
  public BadgeDao badgeDao() {
    if (_badgeDao != null) {
      return _badgeDao;
    } else {
      synchronized(this) {
        if(_badgeDao == null) {
          _badgeDao = new BadgeDao_Impl(this);
        }
        return _badgeDao;
      }
    }
  }

  @Override
  public PostDao postDao() {
    if (_postDao != null) {
      return _postDao;
    } else {
      synchronized(this) {
        if(_postDao == null) {
          _postDao = new PostDao_Impl(this);
        }
        return _postDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public EkagraDao ekagraDao() {
    if (_ekagraDao != null) {
      return _ekagraDao;
    } else {
      synchronized(this) {
        if(_ekagraDao == null) {
          _ekagraDao = new EkagraDao_Impl(this);
        }
        return _ekagraDao;
      }
    }
  }

  @Override
  public DailyStatsDao dailyStatsDao() {
    if (_dailyStatsDao != null) {
      return _dailyStatsDao;
    } else {
      synchronized(this) {
        if(_dailyStatsDao == null) {
          _dailyStatsDao = new DailyStatsDao_Impl(this);
        }
        return _dailyStatsDao;
      }
    }
  }
}
