package com.safar.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.safar.app.data.local.entity.EkagraSessions;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EkagraDao_Impl implements EkagraDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EkagraSessions> __insertionAdapterOfEkagraSessions;

  public EkagraDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEkagraSessions = new EntityInsertionAdapter<EkagraSessions>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `ekagra_sessions` (`id`,`taskName`,`focusDuration`,`breakDuration`,`completedPomodoros`,`totalFocusTime`,`date`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EkagraSessions entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTaskName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getTaskName());
        }
        statement.bindLong(3, entity.getFocusDuration());
        statement.bindLong(4, entity.getBreakDuration());
        statement.bindLong(5, entity.getCompletedPomodoros());
        statement.bindLong(6, entity.getTotalFocusTime());
        statement.bindLong(7, entity.getDate());
      }
    };
  }

  @Override
  public Object insert(final EkagraSessions session, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfEkagraSessions.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EkagraSessions>> getAllSessions() {
    final String _sql = "SELECT * FROM ekagra_sessions ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ekagra_sessions"}, new Callable<List<EkagraSessions>>() {
      @Override
      @NonNull
      public List<EkagraSessions> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskName = CursorUtil.getColumnIndexOrThrow(_cursor, "taskName");
          final int _cursorIndexOfFocusDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "focusDuration");
          final int _cursorIndexOfBreakDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "breakDuration");
          final int _cursorIndexOfCompletedPomodoros = CursorUtil.getColumnIndexOrThrow(_cursor, "completedPomodoros");
          final int _cursorIndexOfTotalFocusTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalFocusTime");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<EkagraSessions> _result = new ArrayList<EkagraSessions>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EkagraSessions _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTaskName;
            if (_cursor.isNull(_cursorIndexOfTaskName)) {
              _tmpTaskName = null;
            } else {
              _tmpTaskName = _cursor.getString(_cursorIndexOfTaskName);
            }
            final int _tmpFocusDuration;
            _tmpFocusDuration = _cursor.getInt(_cursorIndexOfFocusDuration);
            final int _tmpBreakDuration;
            _tmpBreakDuration = _cursor.getInt(_cursorIndexOfBreakDuration);
            final int _tmpCompletedPomodoros;
            _tmpCompletedPomodoros = _cursor.getInt(_cursorIndexOfCompletedPomodoros);
            final int _tmpTotalFocusTime;
            _tmpTotalFocusTime = _cursor.getInt(_cursorIndexOfTotalFocusTime);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            _item = new EkagraSessions(_tmpId,_tmpTaskName,_tmpFocusDuration,_tmpBreakDuration,_tmpCompletedPomodoros,_tmpTotalFocusTime,_tmpDate);
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
  public Flow<List<EkagraSessions>> getSessionsInRange(final long from, final long to) {
    final String _sql = "SELECT * FROM ekagra_sessions WHERE date >= ? AND date <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    _argIndex = 2;
    _statement.bindLong(_argIndex, to);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ekagra_sessions"}, new Callable<List<EkagraSessions>>() {
      @Override
      @NonNull
      public List<EkagraSessions> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTaskName = CursorUtil.getColumnIndexOrThrow(_cursor, "taskName");
          final int _cursorIndexOfFocusDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "focusDuration");
          final int _cursorIndexOfBreakDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "breakDuration");
          final int _cursorIndexOfCompletedPomodoros = CursorUtil.getColumnIndexOrThrow(_cursor, "completedPomodoros");
          final int _cursorIndexOfTotalFocusTime = CursorUtil.getColumnIndexOrThrow(_cursor, "totalFocusTime");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<EkagraSessions> _result = new ArrayList<EkagraSessions>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EkagraSessions _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTaskName;
            if (_cursor.isNull(_cursorIndexOfTaskName)) {
              _tmpTaskName = null;
            } else {
              _tmpTaskName = _cursor.getString(_cursorIndexOfTaskName);
            }
            final int _tmpFocusDuration;
            _tmpFocusDuration = _cursor.getInt(_cursorIndexOfFocusDuration);
            final int _tmpBreakDuration;
            _tmpBreakDuration = _cursor.getInt(_cursorIndexOfBreakDuration);
            final int _tmpCompletedPomodoros;
            _tmpCompletedPomodoros = _cursor.getInt(_cursorIndexOfCompletedPomodoros);
            final int _tmpTotalFocusTime;
            _tmpTotalFocusTime = _cursor.getInt(_cursorIndexOfTotalFocusTime);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            _item = new EkagraSessions(_tmpId,_tmpTaskName,_tmpFocusDuration,_tmpBreakDuration,_tmpCompletedPomodoros,_tmpTotalFocusTime,_tmpDate);
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
  public Object getTotalFocusTimeInRange(final long from, final long to,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(totalFocusTime) FROM ekagra_sessions WHERE date >= ? AND date <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    _argIndex = 2;
    _statement.bindLong(_argIndex, to);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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
