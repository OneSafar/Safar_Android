package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.BadgeDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DatabaseModule_ProvideBadgeDaoFactory implements Factory<BadgeDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideBadgeDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BadgeDao get() {
    return provideBadgeDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBadgeDaoFactory create(Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideBadgeDaoFactory(dbProvider);
  }

  public static BadgeDao provideBadgeDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBadgeDao(db));
  }
}
