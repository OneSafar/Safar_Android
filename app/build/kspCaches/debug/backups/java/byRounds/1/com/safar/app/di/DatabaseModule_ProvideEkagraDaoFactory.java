package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.EkagraDao;
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
public final class DatabaseModule_ProvideEkagraDaoFactory implements Factory<EkagraDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideEkagraDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public EkagraDao get() {
    return provideEkagraDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideEkagraDaoFactory create(Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideEkagraDaoFactory(dbProvider);
  }

  public static EkagraDao provideEkagraDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideEkagraDao(db));
  }
}
