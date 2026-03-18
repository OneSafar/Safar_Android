package com.safar.app.di;

import com.safar.app.data.local.SafarDatabase;
import com.safar.app.data.local.dao.MessageDao;
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
public final class DatabaseModule_ProvideMessageDaoFactory implements Factory<MessageDao> {
  private final Provider<SafarDatabase> dbProvider;

  public DatabaseModule_ProvideMessageDaoFactory(Provider<SafarDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public MessageDao get() {
    return provideMessageDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideMessageDaoFactory create(Provider<SafarDatabase> dbProvider) {
    return new DatabaseModule_ProvideMessageDaoFactory(dbProvider);
  }

  public static MessageDao provideMessageDao(SafarDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMessageDao(db));
  }
}
