package com.safar.app.ui.splash;

import com.safar.app.data.local.SafarDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<SafarDataStore> dataStoreProvider;

  public SplashViewModel_Factory(Provider<SafarDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(dataStoreProvider.get());
  }

  public static SplashViewModel_Factory create(Provider<SafarDataStore> dataStoreProvider) {
    return new SplashViewModel_Factory(dataStoreProvider);
  }

  public static SplashViewModel newInstance(SafarDataStore dataStore) {
    return new SplashViewModel(dataStore);
  }
}
