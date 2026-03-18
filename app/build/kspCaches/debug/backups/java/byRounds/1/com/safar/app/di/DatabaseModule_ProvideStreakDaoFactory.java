package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.StreakDao;
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
public final class DatabaseModule_ProvideStreakDaoFactory implements Factory<StreakDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideStreakDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public StreakDao get() {
    return provideStreakDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideStreakDaoFactory create(Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideStreakDaoFactory(dbProvider);
  }

  public static StreakDao provideStreakDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideStreakDao(db));
  }
}
