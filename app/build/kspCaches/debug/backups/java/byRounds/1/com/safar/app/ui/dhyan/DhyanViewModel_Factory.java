package com.safar.app.ui.dhyan;

import com.safar.app.domain.repository.DhyanRepository;
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
public final class DhyanViewModel_Factory implements Factory<DhyanViewModel> {
  private final Provider<DhyanRepository> repoProvider;

  public DhyanViewModel_Factory(Provider<DhyanRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DhyanViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static DhyanViewModel_Factory create(Provider<DhyanRepository> repoProvider) {
    return new DhyanViewModel_Factory(repoProvider);
  }

  public static DhyanViewModel newInstance(DhyanRepository repo) {
    return new DhyanViewModel(repo);
  }
}
