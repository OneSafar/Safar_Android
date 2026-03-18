package com.safar.app.ui.nishtha.goals;

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
public final class GoalsViewModel_Factory implements Factory<GoalsViewModel> {
  private final Provider<NishthaRepository> repoProvider;

  public GoalsViewModel_Factory(Provider<NishthaRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GoalsViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static GoalsViewModel_Factory create(Provider<NishthaRepository> repoProvider) {
    return new GoalsViewModel_Factory(repoProvider);
  }

  public static GoalsViewModel newInstance(NishthaRepository repo) {
    return new GoalsViewModel(repo);
  }
}
