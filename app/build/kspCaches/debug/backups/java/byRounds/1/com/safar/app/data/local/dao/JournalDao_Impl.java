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
import com.safar.app.data.local.entity.JournalEntryEntity;
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
public final class JournalDao_Impl implements JournalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<JournalEntryEntity> __insertionAdapterOfJournalEntryEntity;

  private final EntityDeletionOrUpdateAdapter<JournalEntryEntity> __deletionAdapterOfJournalEntryEntity;

  private final EntityDeletionOrUpdateAdapter<JournalEntryEntity> __updateAdapterOfJournalEntryEntity;

  public JournalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfJournalEntryEntity = new EntityInsertionAdapter<JournalEntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `journal_entries` (`id`,`title`,`content`,`moodTag`,`inspirationPrompt`,`timestamp`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final JournalEntryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getContent());
        if (entity.getMoodTag() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMoodTag());
        }
        if (entity.getInspirationPrompt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getInspirationPrompt());
        }
        statement.bindLong(6, entity.getTimestamp());
        statement.bindLong(7, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfJournalEntryEntity = new EntityDeletionOrUpdateAdapter<JournalEntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `journal_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final JournalEntryEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfJournalEntryEntity = new EntityDeletionOrUpdateAdapter<JournalEntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `journal_entries` SET `id` = ?,`title` = ?,`content` = ?,`moodTag` = ?,`inspirationPrompt` = ?,`timestamp` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final JournalEntryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getContent());
        if (entity.getMoodTag() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMoodTag());
        }
        if (entity.getInspirationPrompt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getInspirationPrompt());
        }
        statement.bindLong(6, entity.getTimestamp());
        statement.bindLong(7, entity.getUpdatedAt());
        statement.bindLong(8, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final JournalEntryEntity entry,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfJournalEntryEntity.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final JournalEntryEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfJournalEntryEntity.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final JournalEntryEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfJournalEntryEntity.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<JournalEntryEntity>> getAllEntries() {
    final String _sql = "SELECT * FROM journal_entries ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"journal_entries"}, new Callable<List<JournalEntryEntity>>() {
      @Override
      @NonNull
      public List<JournalEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMoodTag = CursorUtil.getColumnIndexOrThrow(_cursor, "moodTag");
          final int _cursorIndexOfInspirationPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "inspirationPrompt");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<JournalEntryEntity> _result = new ArrayList<JournalEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final JournalEntryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMoodTag;
            if (_cursor.isNull(_cursorIndexOfMoodTag)) {
              _tmpMoodTag = null;
            } else {
              _tmpMoodTag = _cursor.getString(_cursorIndexOfMoodTag);
            }
            final String _tmpInspirationPrompt;
            if (_cursor.isNull(_cursorIndexOfInspirationPrompt)) {
              _tmpInspirationPrompt = null;
            } else {
              _tmpInspirationPrompt = _cursor.getString(_cursorIndexOfInspirationPrompt);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new JournalEntryEntity(_tmpId,_tmpTitle,_tmpContent,_tmpMoodTag,_tmpInspirationPrompt,_tmpTimestamp,_tmpUpdatedAt);
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
  public Object getEntryById(final long id,
      final Continuation<? super JournalEntryEntity> $completion) {
    final String _sql = "SELECT * FROM journal_entries WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<JournalEntryEntity>() {
      @Override
      @Nullable
      public JournalEntryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMoodTag = CursorUtil.getColumnIndexOrThrow(_cursor, "moodTag");
          final int _cursorIndexOfInspirationPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "inspirationPrompt");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final JournalEntryEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMoodTag;
            if (_cursor.isNull(_cursorIndexOfMoodTag)) {
              _tmpMoodTag = null;
            } else {
              _tmpMoodTag = _cursor.getString(_cursorIndexOfMoodTag);
            }
            final String _tmpInspirationPrompt;
            if (_cursor.isNull(_cursorIndexOfInspirationPrompt)) {
              _tmpInspirationPrompt = null;
            } else {
              _tmpInspirationPrompt = _cursor.getString(_cursorIndexOfInspirationPrompt);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new JournalEntryEntity(_tmpId,_tmpTitle,_tmpContent,_tmpMoodTag,_tmpInspirationPrompt,_tmpTimestamp,_tmpUpdatedAt);
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
  public Flow<List<JournalEntryEntity>> getEntriesInRange(final long from, final long to) {
    final String _sql = "SELECT * FROM journal_entries WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, from);
    _argIndex = 2;
    _statement.bindLong(_argIndex, to);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"journal_entries"}, new Callable<List<JournalEntryEntity>>() {
      @Override
      @NonNull
      public List<JournalEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfMoodTag = CursorUtil.getColumnIndexOrThrow(_cursor, "moodTag");
          final int _cursorIndexOfInspirationPrompt = CursorUtil.getColumnIndexOrThrow(_cursor, "inspirationPrompt");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<JournalEntryEntity> _result = new ArrayList<JournalEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final JournalEntryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpMoodTag;
            if (_cursor.isNull(_cursorIndexOfMoodTag)) {
              _tmpMoodTag = null;
            } else {
              _tmpMoodTag = _cursor.getString(_cursorIndexOfMoodTag);
            }
            final String _tmpInspirationPrompt;
            if (_cursor.isNull(_cursorIndexOfInspirationPrompt)) {
              _tmpInspirationPrompt = null;
            } else {
              _tmpInspirationPrompt = _cursor.getString(_cursorIndexOfInspirationPrompt);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new JournalEntryEntity(_tmpId,_tmpTitle,_tmpContent,_tmpMoodTag,_tmpInspirationPrompt,_tmpTimestamp,_tmpUpdatedAt);
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
