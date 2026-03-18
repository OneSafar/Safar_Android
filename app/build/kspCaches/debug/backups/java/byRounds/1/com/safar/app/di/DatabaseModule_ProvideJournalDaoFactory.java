package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.JournalDao;
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
public final class DatabaseModule_ProvideJournalDaoFactory implements Factory<JournalDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideJournalDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public JournalDao get() {
    return provideJournalDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideJournalDaoFactory create(Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideJournalDaoFactory(dbProvider);
  }

  public static JournalDao provideJournalDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideJournalDao(db));
  }
}
