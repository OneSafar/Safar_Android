package com.safar.app.util;

import com.safar.app.data.local.SafarDataStore;
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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<SafarDataStore> dataStoreProvider;

  public AuthInterceptor_Factory(Provider<SafarDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(dataStoreProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<SafarDataStore> dataStoreProvider) {
    return new AuthInterceptor_Factory(dataStoreProvider);
  }

  public static AuthInterceptor newInstance(SafarDataStore dataStore) {
    return new AuthInterceptor(dataStore);
  }
}
