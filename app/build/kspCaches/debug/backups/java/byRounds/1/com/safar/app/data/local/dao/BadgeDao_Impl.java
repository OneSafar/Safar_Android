package com.safar.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.safar.app.data.local.entity.BadgeEntity;
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
public final class BadgeDao_Impl implements BadgeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BadgeEntity> __insertionAdapterOfBadgeEntity;

  private final EntityDeletionOrUpdateAdapter<BadgeEntity> __updateAdapterOfBadgeEntity;

  public BadgeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBadgeEntity = new EntityInsertionAdapter<BadgeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `badges` (`id`,`title`,`description`,`category`,`rarity`,`iconName`,`isUnlocked`,`unlockedAt`,`progress`,`maxProgress`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BadgeEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getCategory());
        statement.bindString(5, entity.getRarity());
        statement.bindString(6, entity.getIconName());
        final int _tmp = entity.isUnlocked() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getUnlockedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getUnlockedAt());
        }
        statement.bindDouble(9, entity.getProgress());
        statement.bindDouble(10, entity.getMaxProgress());
      }
    };
    this.__updateAdapterOfBadgeEntity = new EntityDeletionOrUpdateAdapter<BadgeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `badges` SET `id` = ?,`title` = ?,`description` = ?,`category` = ?,`rarity` = ?,`iconName` = ?,`isUnlocked` = ?,`unlockedAt` = ?,`progress` = ?,`maxProgress` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BadgeEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getCategory());
        statement.bindString(5, entity.getRarity());
        statement.bindString(6, entity.getIconName());
        final int _tmp = entity.isUnlocked() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getUnlockedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getUnlockedAt());
        }
        statement.bindDouble(9, entity.getProgress());
        statement.bindDouble(10, entity.getMaxProgress());
        statement.bindString(11, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final BadgeEntity badge, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBadgeEntity.insert(badge);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<BadgeEntity> badges,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBadgeEntity.insert(badges);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final BadgeEntity badge, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBadgeEntity.handle(badge);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BadgeEntity>> getAllBadges() {
    final String _sql = "SELECT * FROM badges ORDER BY isUnlocked DESC, rarity ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"badges"}, new Callable<List<BadgeEntity>>() {
      @Override
      @NonNull
      public List<BadgeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfRarity = CursorUtil.getColumnIndexOrThrow(_cursor, "rarity");
          final int _cursorIndexOfIconName = CursorUtil.getColumnIndexOrThrow(_cursor, "iconName");
          final int _cursorIndexOfIsUnlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isUnlocked");
          final int _cursorIndexOfUnlockedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "unlockedAt");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfMaxProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "maxProgress");
          final List<BadgeEntity> _result = new ArrayList<BadgeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BadgeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpRarity;
            _tmpRarity = _cursor.getString(_cursorIndexOfRarity);
            final String _tmpIconName;
            _tmpIconName = _cursor.getString(_cursorIndexOfIconName);
            final boolean _tmpIsUnlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUnlocked);
            _tmpIsUnlocked = _tmp != 0;
            final Long _tmpUnlockedAt;
            if (_cursor.isNull(_cursorIndexOfUnlockedAt)) {
              _tmpUnlockedAt = null;
            } else {
              _tmpUnlockedAt = _cursor.getLong(_cursorIndexOfUnlockedAt);
            }
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final float _tmpMaxProgress;
            _tmpMaxProgress = _cursor.getFloat(_cursorIndexOfMaxProgress);
            _item = new BadgeEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategory,_tmpRarity,_tmpIconName,_tmpIsUnlocked,_tmpUnlockedAt,_tmpProgress,_tmpMaxProgress);
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
  public Flow<List<BadgeEntity>> getUnlockedBadges() {
    final String _sql = "SELECT * FROM badges WHERE isUnlocked = 1 ORDER BY unlockedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"badges"}, new Callable<List<BadgeEntity>>() {
      @Override
      @NonNull
      public List<BadgeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfRarity = CursorUtil.getColumnIndexOrThrow(_cursor, "rarity");
          final int _cursorIndexOfIconName = CursorUtil.getColumnIndexOrThrow(_cursor, "iconName");
          final int _cursorIndexOfIsUnlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isUnlocked");
          final int _cursorIndexOfUnlockedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "unlockedAt");
          final int _cursorIndexOfProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "progress");
          final int _cursorIndexOfMaxProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "maxProgress");
          final List<BadgeEntity> _result = new ArrayList<BadgeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BadgeEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpRarity;
            _tmpRarity = _cursor.getString(_cursorIndexOfRarity);
            final String _tmpIconName;
            _tmpIconName = _cursor.getString(_cursorIndexOfIconName);
            final boolean _tmpIsUnlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUnlocked);
            _tmpIsUnlocked = _tmp != 0;
            final Long _tmpUnlockedAt;
            if (_cursor.isNull(_cursorIndexOfUnlockedAt)) {
              _tmpUnlockedAt = null;
            } else {
              _tmpUnlockedAt = _cursor.getLong(_cursorIndexOfUnlockedAt);
            }
            final float _tmpProgress;
            _tmpProgress = _cursor.getFloat(_cursorIndexOfProgress);
            final float _tmpMaxProgress;
            _tmpMaxProgress = _cursor.getFloat(_cursorIndexOfMaxProgress);
            _item = new BadgeEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategory,_tmpRarity,_tmpIconName,_tmpIsUnlocked,_tmpUnlockedAt,_tmpProgress,_tmpMaxProgress);
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
