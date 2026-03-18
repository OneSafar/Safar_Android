package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.MoodCheckInDao;
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
public final class DatabaseModule_ProvideMoodCheckInDaoFactory implements Factory<MoodCheckInDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideMoodCheckInDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public MoodCheckInDao get() {
    return provideMoodCheckInDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideMoodCheckInDaoFactory create(
      Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideMoodCheckInDaoFactory(dbProvider);
  }

  public static MoodCheckInDao provideMoodCheckInDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMoodCheckInDao(db));
  }
}
