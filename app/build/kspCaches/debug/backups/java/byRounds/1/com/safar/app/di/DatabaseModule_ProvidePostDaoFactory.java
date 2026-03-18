package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.PostDao;
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
public final class DatabaseModule_ProvidePostDaoFactory implements Factory<PostDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvidePostDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PostDao get() {
    return providePostDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePostDaoFactory create(Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvidePostDaoFactory(dbProvider);
  }

  public static PostDao providePostDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePostDao(db));
  }
}
