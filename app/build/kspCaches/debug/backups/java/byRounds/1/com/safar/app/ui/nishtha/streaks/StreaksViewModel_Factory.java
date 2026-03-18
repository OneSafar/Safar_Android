package com.safar.app.ui.nishtha.streaks;

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
public final class StreaksViewModel_Factory implements Factory<StreaksViewModel> {
  private final Provider<NishthaRepository> repoProvider;

  public StreaksViewModel_Factory(Provider<NishthaRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public StreaksViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static StreaksViewModel_Factory create(Provider<NishthaRepository> repoProvider) {
    return new StreaksViewModel_Factory(repoProvider);
  }

  public static StreaksViewModel newInstance(NishthaRepository repo) {
    return new StreaksViewModel(repo);
  }
}
