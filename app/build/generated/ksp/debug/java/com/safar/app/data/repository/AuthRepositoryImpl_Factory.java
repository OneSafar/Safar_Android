package com.safar.app.data.repository;

import com.safar.app.data.local.SafarDataStore;
import com.safar.app.data.remote.api.AuthApi;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> authApiProvider;

  private final Provider<SafarDataStore> dataStoreProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> authApiProvider,
      Provider<SafarDataStore> dataStoreProvider) {
    this.authApiProvider = authApiProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(authApiProvider.get(), dataStoreProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> authApiProvider,
      Provider<SafarDataStore> dataStoreProvider) {
    return new AuthRepositoryImpl_Factory(authApiProvider, dataStoreProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi authApi, SafarDataStore dataStore) {
    return new AuthRepositoryImpl(authApi, dataStore);
  }
}
