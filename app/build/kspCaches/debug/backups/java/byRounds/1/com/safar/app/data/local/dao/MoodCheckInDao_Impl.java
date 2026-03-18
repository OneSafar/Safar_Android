package com.safar.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.safar.app.data.local.entity.MoodCheckInEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class MoodCheckInDao_Impl implements MoodCheckInDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MoodCheckInEntity> __insertionAdapterOfMoodCheckInEntity;

  private final EntityDeletionOrUpdateAdapter<MoodCheckInEntity> __deletionAdapterOfMoodCheckInEntity;

  public MoodCheckInDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMoodCheckInEntity = new EntityInsertionAdapter<MoodCheckInEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `mood_checkins` (`id`,`mood`,`intensity`,`reason`,`contextTags`,`note`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MoodCheckInEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getMood());
        statement.bindLong(3, entity.getIntensity());
        if (entity.getReason() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getReason());
        }
        statement.bindString(5, entity.getContextTags());
        if (entity.getNote() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNote());
        }
        statement.bindLong(7, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfMoodCheckInEntity = new EntityDeletionOrUpdateAdapter<MoodCheckInEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `mood_checkins` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MoodCheckInEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final MoodCheckInEntity checkIn,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMoodCheckInEntity.insert(checkIn);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MoodCheckInEntity checkIn,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMoodCheckInEntity.handle(checkIn);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MoodCheckInEntity>> getAllCheckIns() {
    final String _sql = "SELECT * FROM mood_checkins ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mood_checkins"}, new Callable<List<MoodCheckInEntity>>() {
      @Override
      @NonNull
      public List<MoodCheckInEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfIntensity = CursorUtil.getColumnIndexOrThrow(_cursor, "intensity");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfContextTags = CursorUtil.getColumnIndexOrThrow(_cursor, "contextTags");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<MoodCheckInEntity> _result = new ArrayList<MoodCheckInEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodCheckInEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMood;
            _tmpMood = _cursor.getString(_cursorIndexOfMood);
            final int _tmpIntensity;
            _tmpIntensity = _cursor.getInt(_cursorIndexOfIntensity);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpContextTags;
            _tmpContextTags = _cursor.getString(_cursorIndexOfContextTags);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new MoodCheckInEntity(_tmpId,_tmpMood,_tmpIntensity,_tmpReason,_tmpContextTags,_tmpNote,_tmpTimestamp);
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
  public Flow<List<MoodCheckInEntity>> getCheckInsInRange(final long from, final long to) {
    final String _sql = "SELECT * FROM mood_checkins WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    _argIndex = 2;
    _statement.bindLong(_argIndex, to);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mood_checkins"}, new Callable<List<MoodCheckInEntity>>() {
      @Override
      @NonNull
      public List<MoodCheckInEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfIntensity = CursorUtil.getColumnIndexOrThrow(_cursor, "intensity");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfContextTags = CursorUtil.getColumnIndexOrThrow(_cursor, "contextTags");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<MoodCheckInEntity> _result = new ArrayList<MoodCheckInEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodCheckInEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMood;
            _tmpMood = _cursor.getString(_cursorIndexOfMood);
            final int _tmpIntensity;
            _tmpIntensity = _cursor.getInt(_cursorIndexOfIntensity);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpContextTags;
            _tmpContextTags = _cursor.getString(_cursorIndexOfContextTags);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new MoodCheckInEntity(_tmpId,_tmpMood,_tmpIntensity,_tmpReason,_tmpContextTags,_tmpNote,_tmpTimestamp);
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
  public Flow<List<MoodCheckInEntity>> getLastSevenCheckIns() {
    final String _sql = "SELECT * FROM mood_checkins ORDER BY timestamp DESC LIMIT 7";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mood_checkins"}, new Callable<List<MoodCheckInEntity>>() {
      @Override
      @NonNull
      public List<MoodCheckInEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfIntensity = CursorUtil.getColumnIndexOrThrow(_cursor, "intensity");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final int _cursorIndexOfContextTags = CursorUtil.getColumnIndexOrThrow(_cursor, "contextTags");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<MoodCheckInEntity> _result = new ArrayList<MoodCheckInEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodCheckInEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpMood;
            _tmpMood = _cursor.getString(_cursorIndexOfMood);
            final int _tmpIntensity;
            _tmpIntensity = _cursor.getInt(_cursorIndexOfIntensity);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            final String _tmpContextTags;
            _tmpContextTags = _cursor.getString(_cursorIndexOfContextTags);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new MoodCheckInEntity(_tmpId,_tmpMood,_tmpIntensity,_tmpReason,_tmpContextTags,_tmpNote,_tmpTimestamp);
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
  public Object getCheckInCountForDay(final long dayStart, final long dayEnd,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM mood_checkins WHERE timestamp >= ? AND timestamp <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayStart);
    _argIndex = 2;
    _statement.bindLong(_argIndex, dayEnd);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
