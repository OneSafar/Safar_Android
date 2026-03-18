package com.safar.app.data.repository;

import com.safar.app.data.local.dao.UserDao;
import com.safar.app.data.remote.api.UserApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class UserRepositoryImpl_Factory implements Factory<UserRepositoryImpl> {
  private final Provider<UserApi> apiProvider;

  private final Provider<UserDao> daoProvider;

  public UserRepositoryImpl_Factory(Provider<UserApi> apiProvider, Provider<UserDao> daoProvider) {
    this.apiProvider = apiProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public UserRepositoryImpl get() {
    return newInstance(apiProvider.get(), daoProvider.get());
  }

  public static UserRepositoryImpl_Factory create(Provider<UserApi> apiProvider,
      Provider<UserDao> daoProvider) {
    return new UserRepositoryImpl_Factory(apiProvider, daoProvider);
  }

  public static UserRepositoryImpl newInstance(UserApi api, UserDao dao) {
    return new UserRepositoryImpl(api, dao);
  }
}
