package com.safar.app.data.local.dao;

import android.database.Cursor;
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
import com.safar.app.data.local.entity.StreakEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class StreakDao_Impl implements StreakDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StreakEntity> __insertionAdapterOfStreakEntity;

  private final EntityDeletionOrUpdateAdapter<StreakEntity> __updateAdapterOfStreakEntity;

  public StreakDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStreakEntity = new EntityInsertionAdapter<StreakEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `streaks` (`type`,`currentStreak`,`longestStreak`,`lastActivityDate`,`totalCount`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StreakEntity entity) {
        statement.bindString(1, entity.getType());
        statement.bindLong(2, entity.getCurrentStreak());
        statement.bindLong(3, entity.getLongestStreak());
        if (entity.getLastActivityDate() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getLastActivityDate());
        }
        statement.bindLong(5, entity.getTotalCount());
      }
    };
    this.__updateAdapterOfStreakEntity = new EntityDeletionOrUpdateAdapter<StreakEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `streaks` SET `type` = ?,`currentStreak` = ?,`longestStreak` = ?,`lastActivityDate` = ?,`totalCount` = ? WHERE `type` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StreakEntity entity) {
        statement.bindString(1, entity.getType());
        statement.bindLong(2, entity.getCurrentStreak());
        statement.bindLong(3, entity.getLongestStreak());
        if (entity.getLastActivityDate() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getLastActivityDate());
        }
        statement.bindLong(5, entity.getTotalCount());
        statement.bindString(6, entity.getType());
      }
    };
  }

  @Override
  public Object insert(final StreakEntity streak, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStreakEntity.insert(streak);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final StreakEntity streak, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfStreakEntity.handle(streak);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<StreakEntity> getStreak(final String type) {
    final String _sql = "SELECT * FROM streaks WHERE type = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"streaks"}, new Callable<StreakEntity>() {
      @Override
      @Nullable
      public StreakEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longestStreak");
          final int _cursorIndexOfLastActivityDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActivityDate");
          final int _cursorIndexOfTotalCount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCount");
          final StreakEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final Long _tmpLastActivityDate;
            if (_cursor.isNull(_cursorIndexOfLastActivityDate)) {
              _tmpLastActivityDate = null;
            } else {
              _tmpLastActivityDate = _cursor.getLong(_cursorIndexOfLastActivityDate);
            }
            final int _tmpTotalCount;
            _tmpTotalCount = _cursor.getInt(_cursorIndexOfTotalCount);
            _result = new StreakEntity(_tmpType,_tmpCurrentStreak,_tmpLongestStreak,_tmpLastActivityDate,_tmpTotalCount);
          } else {
            _result = null;
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
  public Flow<List<StreakEntity>> getAllStreaks() {
    final String _sql = "SELECT * FROM streaks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"streaks"}, new Callable<List<StreakEntity>>() {
      @Override
      @NonNull
      public List<StreakEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longestStreak");
          final int _cursorIndexOfLastActivityDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActivityDate");
          final int _cursorIndexOfTotalCount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCount");
          final List<StreakEntity> _result = new ArrayList<StreakEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StreakEntity _item;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final Long _tmpLastActivityDate;
            if (_cursor.isNull(_cursorIndexOfLastActivityDate)) {
              _tmpLastActivityDate = null;
            } else {
              _tmpLastActivityDate = _cursor.getLong(_cursorIndexOfLastActivityDate);
            }
            final int _tmpTotalCount;
            _tmpTotalCount = _cursor.getInt(_cursorIndexOfTotalCount);
            _item = new StreakEntity(_tmpType,_tmpCurrentStreak,_tmpLongestStreak,_tmpLastActivityDate,_tmpTotalCount);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
