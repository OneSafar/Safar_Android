package com.safar.app.ui.nishtha.analytics;

import com.safar.app.domain.repository.NishthaRepository;
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
public final class AnalyticsViewModel_Factory implements Factory<AnalyticsViewModel> {
  private final Provider<NishthaRepository> repoProvider;

  public AnalyticsViewModel_Factory(Provider<NishthaRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public AnalyticsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static AnalyticsViewModel_Factory create(Provider<NishthaRepository> repoProvider) {
    return new AnalyticsViewModel_Factory(repoProvider);
  }

  public static AnalyticsViewModel newInstance(NishthaRepository repo) {
    return new AnalyticsViewModel(repo);
  }
}
