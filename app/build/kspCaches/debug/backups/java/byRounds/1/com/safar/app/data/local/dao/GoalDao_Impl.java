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
import com.safar.app.data.local.entity.GoalEntity;
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
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GoalDao_Impl implements GoalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GoalEntity> __insertionAdapterOfGoalEntity;

  private final EntityDeletionOrUpdateAdapter<GoalEntity> __deletionAdapterOfGoalEntity;

  private final EntityDeletionOrUpdateAdapter<GoalEntity> __updateAdapterOfGoalEntity;

  public GoalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGoalEntity = new EntityInsertionAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `goals` (`id`,`title`,`description`,`category`,`targetValue`,`currentValue`,`unit`,`isRepeating`,`repeatFrequency`,`isCompleted`,`deadline`,`createdAt`,`completedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        statement.bindString(4, entity.getCategory());
        statement.bindDouble(5, entity.getTargetValue());
        statement.bindDouble(6, entity.getCurrentValue());
        if (entity.getUnit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getUnit());
        }
        final int _tmp = entity.isRepeating() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getRepeatFrequency() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getRepeatFrequency());
        }
        final int _tmp_1 = entity.isCompleted() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getDeadline() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getDeadline());
        }
        statement.bindLong(12, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getCompletedAt());
        }
      }
    };
    this.__deletionAdapterOfGoalEntity = new EntityDeletionOrUpdateAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `goals` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfGoalEntity = new EntityDeletionOrUpdateAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `goals` SET `id` = ?,`title` = ?,`description` = ?,`category` = ?,`targetValue` = ?,`currentValue` = ?,`unit` = ?,`isRepeating` = ?,`repeatFrequency` = ?,`isCompleted` = ?,`deadline` = ?,`createdAt` = ?,`completedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        statement.bindString(4, entity.getCategory());
        statement.bindDouble(5, entity.getTargetValue());
        statement.bindDouble(6, entity.getCurrentValue());
        if (entity.getUnit() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getUnit());
        }
        final int _tmp = entity.isRepeating() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getRepeatFrequency() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getRepeatFrequency());
        }
        final int _tmp_1 = entity.isCompleted() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getDeadline() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getDeadline());
        }
        statement.bindLong(12, entity.getCreatedAt());
        if (entity.getCompletedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getCompletedAt());
        }
        statement.bindLong(14, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final GoalEntity goal, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfGoalEntity.insertAndReturnId(goal);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final GoalEntity goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGoalEntity.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final GoalEntity goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfGoalEntity.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GoalEntity>> getPendingGoals() {
    final String _sql = "SELECT * FROM goals WHERE isCompleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<GoalEntity>>() {
      @Override
      @NonNull
      public List<GoalEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatFrequency");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfDeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "deadline");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<GoalEntity> _result = new ArrayList<GoalEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GoalEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final float _tmpTargetValue;
            _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final String _tmpRepeatFrequency;
            if (_cursor.isNull(_cursorIndexOfRepeatFrequency)) {
              _tmpRepeatFrequency = null;
            } else {
              _tmpRepeatFrequency = _cursor.getString(_cursorIndexOfRepeatFrequency);
            }
            final boolean _tmpIsCompleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp_1 != 0;
            final Long _tmpDeadline;
            if (_cursor.isNull(_cursorIndexOfDeadline)) {
              _tmpDeadline = null;
            } else {
              _tmpDeadline = _cursor.getLong(_cursorIndexOfDeadline);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new GoalEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategory,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpIsRepeating,_tmpRepeatFrequency,_tmpIsCompleted,_tmpDeadline,_tmpCreatedAt,_tmpCompletedAt);
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
  public Flow<List<GoalEntity>> getCompletedGoals() {
    final String _sql = "SELECT * FROM goals WHERE isCompleted = 1 ORDER BY completedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<GoalEntity>>() {
      @Override
      @NonNull
      public List<GoalEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatFrequency");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfDeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "deadline");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<GoalEntity> _result = new ArrayList<GoalEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GoalEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final float _tmpTargetValue;
            _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final String _tmpRepeatFrequency;
            if (_cursor.isNull(_cursorIndexOfRepeatFrequency)) {
              _tmpRepeatFrequency = null;
            } else {
              _tmpRepeatFrequency = _cursor.getString(_cursorIndexOfRepeatFrequency);
            }
            final boolean _tmpIsCompleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp_1 != 0;
            final Long _tmpDeadline;
            if (_cursor.isNull(_cursorIndexOfDeadline)) {
              _tmpDeadline = null;
            } else {
              _tmpDeadline = _cursor.getLong(_cursorIndexOfDeadline);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new GoalEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategory,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpIsRepeating,_tmpRepeatFrequency,_tmpIsCompleted,_tmpDeadline,_tmpCreatedAt,_tmpCompletedAt);
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
  public Flow<List<GoalEntity>> getAllGoals() {
    final String _sql = "SELECT * FROM goals ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<GoalEntity>>() {
      @Override
      @NonNull
      public List<GoalEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatFrequency");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfDeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "deadline");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final List<GoalEntity> _result = new ArrayList<GoalEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GoalEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final float _tmpTargetValue;
            _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final String _tmpRepeatFrequency;
            if (_cursor.isNull(_cursorIndexOfRepeatFrequency)) {
              _tmpRepeatFrequency = null;
            } else {
              _tmpRepeatFrequency = _cursor.getString(_cursorIndexOfRepeatFrequency);
            }
            final boolean _tmpIsCompleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp_1 != 0;
            final Long _tmpDeadline;
            if (_cursor.isNull(_cursorIndexOfDeadline)) {
              _tmpDeadline = null;
            } else {
              _tmpDeadline = _cursor.getLong(_cursorIndexOfDeadline);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new GoalEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategory,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpIsRepeating,_tmpRepeatFrequency,_tmpIsCompleted,_tmpDeadline,_tmpCreatedAt,_tmpCompletedAt);
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
  public Object getGoalById(final long id, final Continuation<? super GoalEntity> $completion) {
    final String _sql = "SELECT * FROM goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GoalEntity>() {
      @Override
      @Nullable
      public GoalEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfIsRepeating = CursorUtil.getColumnIndexOrThrow(_cursor, "isRepeating");
          final int _cursorIndexOfRepeatFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatFrequency");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfDeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "deadline");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final GoalEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final float _tmpTargetValue;
            _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final boolean _tmpIsRepeating;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRepeating);
            _tmpIsRepeating = _tmp != 0;
            final String _tmpRepeatFrequency;
            if (_cursor.isNull(_cursorIndexOfRepeatFrequency)) {
              _tmpRepeatFrequency = null;
            } else {
              _tmpRepeatFrequency = _cursor.getString(_cursorIndexOfRepeatFrequency);
            }
            final boolean _tmpIsCompleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp_1 != 0;
            final Long _tmpDeadline;
            if (_cursor.isNull(_cursorIndexOfDeadline)) {
              _tmpDeadline = null;
            } else {
              _tmpDeadline = _cursor.getLong(_cursorIndexOfDeadline);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _result = new GoalEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategory,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpIsRepeating,_tmpRepeatFrequency,_tmpIsCompleted,_tmpDeadline,_tmpCreatedAt,_tmpCompletedAt);
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

  @Override
  public Object getCompletedCountForDay(final long dayStart, final long dayEnd,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM goals WHERE isCompleted = 1 AND completedAt >= ? AND completedAt <= ?";
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
