package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.DailyStatsDao;
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
public final class DatabaseModule_ProvideDailyStatsDaoFactory implements Factory<DailyStatsDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideDailyStatsDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DailyStatsDao get() {
    return provideDailyStatsDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDailyStatsDaoFactory create(
      Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideDailyStatsDaoFactory(dbProvider);
  }

  public static DailyStatsDao provideDailyStatsDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDailyStatsDao(db));
  }
}
