package com.safar.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.safar.app.data.local.entity.PostEntity;
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
public final class PostDao_Impl implements PostDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PostEntity> __insertionAdapterOfPostEntity;

  private final EntityDeletionOrUpdateAdapter<PostEntity> __deletionAdapterOfPostEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public PostDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPostEntity = new EntityInsertionAdapter<PostEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `posts` (`id`,`authorId`,`authorName`,`authorPhotoUrl`,`content`,`category`,`isAnonymous`,`reactionsCount`,`commentsCount`,`hasReacted`,`timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PostEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAuthorId());
        statement.bindString(3, entity.getAuthorName());
        if (entity.getAuthorPhotoUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAuthorPhotoUrl());
        }
        statement.bindString(5, entity.getContent());
        statement.bindString(6, entity.getCategory());
        final int _tmp = entity.isAnonymous() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getReactionsCount());
        statement.bindLong(9, entity.getCommentsCount());
        final int _tmp_1 = entity.getHasReacted() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        statement.bindLong(11, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfPostEntity = new EntityDeletionOrUpdateAdapter<PostEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `posts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PostEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM posts";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final PostEntity post, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPostEntity.insert(post);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<PostEntity> posts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPostEntity.insert(posts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final PostEntity post, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPostEntity.handle(post);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PostEntity>> getAllPosts() {
    final String _sql = "SELECT * FROM posts ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"posts"}, new Callable<List<PostEntity>>() {
      @Override
      @NonNull
      public List<PostEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAuthorId = CursorUtil.getColumnIndexOrThrow(_cursor, "authorId");
          final int _cursorIndexOfAuthorName = CursorUtil.getColumnIndexOrThrow(_cursor, "authorName");
          final int _cursorIndexOfAuthorPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "authorPhotoUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsAnonymous = CursorUtil.getColumnIndexOrThrow(_cursor, "isAnonymous");
          final int _cursorIndexOfReactionsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsCount");
          final int _cursorIndexOfCommentsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "commentsCount");
          final int _cursorIndexOfHasReacted = CursorUtil.getColumnIndexOrThrow(_cursor, "hasReacted");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<PostEntity> _result = new ArrayList<PostEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PostEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAuthorId;
            _tmpAuthorId = _cursor.getString(_cursorIndexOfAuthorId);
            final String _tmpAuthorName;
            _tmpAuthorName = _cursor.getString(_cursorIndexOfAuthorName);
            final String _tmpAuthorPhotoUrl;
            if (_cursor.isNull(_cursorIndexOfAuthorPhotoUrl)) {
              _tmpAuthorPhotoUrl = null;
            } else {
              _tmpAuthorPhotoUrl = _cursor.getString(_cursorIndexOfAuthorPhotoUrl);
            }
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsAnonymous;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAnonymous);
            _tmpIsAnonymous = _tmp != 0;
            final int _tmpReactionsCount;
            _tmpReactionsCount = _cursor.getInt(_cursorIndexOfReactionsCount);
            final int _tmpCommentsCount;
            _tmpCommentsCount = _cursor.getInt(_cursorIndexOfCommentsCount);
            final boolean _tmpHasReacted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasReacted);
            _tmpHasReacted = _tmp_1 != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new PostEntity(_tmpId,_tmpAuthorId,_tmpAuthorName,_tmpAuthorPhotoUrl,_tmpContent,_tmpCategory,_tmpIsAnonymous,_tmpReactionsCount,_tmpCommentsCount,_tmpHasReacted,_tmpTimestamp);
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
  public Flow<List<PostEntity>> getPostsByCategory(final String category) {
    final String _sql = "SELECT * FROM posts WHERE category = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"posts"}, new Callable<List<PostEntity>>() {
      @Override
      @NonNull
      public List<PostEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAuthorId = CursorUtil.getColumnIndexOrThrow(_cursor, "authorId");
          final int _cursorIndexOfAuthorName = CursorUtil.getColumnIndexOrThrow(_cursor, "authorName");
          final int _cursorIndexOfAuthorPhotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "authorPhotoUrl");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsAnonymous = CursorUtil.getColumnIndexOrThrow(_cursor, "isAnonymous");
          final int _cursorIndexOfReactionsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "reactionsCount");
          final int _cursorIndexOfCommentsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "commentsCount");
          final int _cursorIndexOfHasReacted = CursorUtil.getColumnIndexOrThrow(_cursor, "hasReacted");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<PostEntity> _result = new ArrayList<PostEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PostEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAuthorId;
            _tmpAuthorId = _cursor.getString(_cursorIndexOfAuthorId);
            final String _tmpAuthorName;
            _tmpAuthorName = _cursor.getString(_cursorIndexOfAuthorName);
            final String _tmpAuthorPhotoUrl;
            if (_cursor.isNull(_cursorIndexOfAuthorPhotoUrl)) {
              _tmpAuthorPhotoUrl = null;
            } else {
              _tmpAuthorPhotoUrl = _cursor.getString(_cursorIndexOfAuthorPhotoUrl);
            }
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsAnonymous;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAnonymous);
            _tmpIsAnonymous = _tmp != 0;
            final int _tmpReactionsCount;
            _tmpReactionsCount = _cursor.getInt(_cursorIndexOfReactionsCount);
            final int _tmpCommentsCount;
            _tmpCommentsCount = _cursor.getInt(_cursorIndexOfCommentsCount);
            final boolean _tmpHasReacted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasReacted);
            _tmpHasReacted = _tmp_1 != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new PostEntity(_tmpId,_tmpAuthorId,_tmpAuthorName,_tmpAuthorPhotoUrl,_tmpContent,_tmpCategory,_tmpIsAnonymous,_tmpReactionsCount,_tmpCommentsCount,_tmpHasReacted,_tmpTimestamp);
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
