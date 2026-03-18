package com.safar.app.ui.onboarding;

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
public final class OnboardingViewModel_Factory implements Factory<OnboardingViewModel> {
  private final Provider<SafarDataStore> dataStoreProvider;

  public OnboardingViewModel_Factory(Provider<SafarDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public OnboardingViewModel get() {
    return newInstance(dataStoreProvider.get());
  }

  public static OnboardingViewModel_Factory create(Provider<SafarDataStore> dataStoreProvider) {
    return new OnboardingViewModel_Factory(dataStoreProvider);
  }

  public static OnboardingViewModel newInstance(SafarDataStore dataStore) {
    return new OnboardingViewModel(dataStore);
  }
}
