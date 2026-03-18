package com.safar.app.ui.theme;

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
public final class ThemeViewModel_Factory implements Factory<ThemeViewModel> {
  private final Provider<SafarDataStore> dataStoreProvider;

  public ThemeViewModel_Factory(Provider<SafarDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ThemeViewModel get() {
    return newInstance(dataStoreProvider.get());
  }

  public static ThemeViewModel_Factory create(Provider<SafarDataStore> dataStoreProvider) {
    return new ThemeViewModel_Factory(dataStoreProvider);
  }

  public static ThemeViewModel newInstance(SafarDataStore dataStore) {
    return new ThemeViewModel(dataStore);
  }
}
