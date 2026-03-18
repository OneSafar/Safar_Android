package com.safar.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.safar.app.data.local.entity.DailyStatsEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DailyStatsDao_Impl implements DailyStatsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyStatsEntity> __insertionAdapterOfDailyStatsEntity;

  private final EntityDeletionOrUpdateAdapter<DailyStatsEntity> __updateAdapterOfDailyStatsEntity;

  public DailyStatsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyStatsEntity = new EntityInsertionAdapter<DailyStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_stats` (`date`,`checkInsCount`,`goalsCompleted`,`journalEntries`,`focusMinutes`,`consistencyScore`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyStatsEntity entity) {
        statement.bindLong(1, entity.getDate());
        statement.bindLong(2, entity.getCheckInsCount());
        statement.bindLong(3, entity.getGoalsCompleted());
        statement.bindLong(4, entity.getJournalEntries());
        statement.bindLong(5, entity.getFocusMinutes());
        statement.bindDouble(6, entity.getConsistencyScore());
      }
    };
    this.__updateAdapterOfDailyStatsEntity = new EntityDeletionOrUpdateAdapter<DailyStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `daily_stats` SET `date` = ?,`checkInsCount` = ?,`goalsCompleted` = ?,`journalEntries` = ?,`focusMinutes` = ?,`consistencyScore` = ? WHERE `date` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyStatsEntity entity) {
        statement.bindLong(1, entity.getDate());
        statement.bindLong(2, entity.getCheckInsCount());
        statement.bindLong(3, entity.getGoalsCompleted());
        statement.bindLong(4, entity.getJournalEntries());
        statement.bindLong(5, entity.getFocusMinutes());
        statement.bindDouble(6, entity.getConsistencyScore());
        statement.bindLong(7, entity.getDate());
      }
    };
  }

  @Override
  public Object insert(final DailyStatsEntity stats, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyStatsEntity.insert(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DailyStatsEntity stats, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDailyStatsEntity.handle(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DailyStatsEntity>> getStatsInRange(final long from, final long to) {
    final String _sql = "SELECT * FROM daily_stats WHERE date >= ? AND date <= ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    _argIndex = 2;
    _statement.bindLong(_argIndex, to);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_stats"}, new Callable<List<DailyStatsEntity>>() {
      @Override
      @NonNull
      public List<DailyStatsEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfCheckInsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInsCount");
          final int _cursorIndexOfGoalsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsCompleted");
          final int _cursorIndexOfJournalEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "journalEntries");
          final int _cursorIndexOfFocusMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "focusMinutes");
          final int _cursorIndexOfConsistencyScore = CursorUtil.getColumnIndexOrThrow(_cursor, "consistencyScore");
          final List<DailyStatsEntity> _result = new ArrayList<DailyStatsEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStatsEntity _item;
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final int _tmpCheckInsCount;
            _tmpCheckInsCount = _cursor.getInt(_cursorIndexOfCheckInsCount);
            final int _tmpGoalsCompleted;
            _tmpGoalsCompleted = _cursor.getInt(_cursorIndexOfGoalsCompleted);
            final int _tmpJournalEntries;
            _tmpJournalEntries = _cursor.getInt(_cursorIndexOfJournalEntries);
            final int _tmpFocusMinutes;
            _tmpFocusMinutes = _cursor.getInt(_cursorIndexOfFocusMinutes);
            final float _tmpConsistencyScore;
            _tmpConsistencyScore = _cursor.getFloat(_cursorIndexOfConsistencyScore);
            _item = new DailyStatsEntity(_tmpDate,_tmpCheckInsCount,_tmpGoalsCompleted,_tmpJournalEntries,_tmpFocusMinutes,_tmpConsistencyScore);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getStatsForDate(final long date,
      final Continuation<? super DailyStatsEntity> $completion) {
    final String _sql = "SELECT * FROM daily_stats WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailyStatsEntity>() {
      @Override
      @Nullable
      public DailyStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfCheckInsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInsCount");
          final int _cursorIndexOfGoalsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsCompleted");
          final int _cursorIndexOfJournalEntries = CursorUtil.getColumnIndexOrThrow(_cursor, "journalEntries");
          final int _cursorIndexOfFocusMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "focusMinutes");
          final int _cursorIndexOfConsistencyScore = CursorUtil.getColumnIndexOrThrow(_cursor, "consistencyScore");
          final DailyStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final int _tmpCheckInsCount;
            _tmpCheckInsCount = _cursor.getInt(_cursorIndexOfCheckInsCount);
            final int _tmpGoalsCompleted;
            _tmpGoalsCompleted = _cursor.getInt(_cursorIndexOfGoalsCompleted);
            final int _tmpJournalEntries;
            _tmpJournalEntries = _cursor.getInt(_cursorIndexOfJournalEntries);
            final int _tmpFocusMinutes;
            _tmpFocusMinutes = _cursor.getInt(_cursorIndexOfFocusMinutes);
            final float _tmpConsistencyScore;
            _tmpConsistencyScore = _cursor.getFloat(_cursorIndexOfConsistencyScore);
            _result = new DailyStatsEntity(_tmpDate,_tmpCheckInsCount,_tmpGoalsCompleted,_tmpJournalEntries,_tmpFocusMinutes,_tmpConsistencyScore);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
