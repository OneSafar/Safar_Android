package com.safar.app.ui.nishtha.badges;

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
public final class BadgesViewModel_Factory implements Factory<BadgesViewModel> {
  private final Provider<NishthaRepository> repoProvider;

  public BadgesViewModel_Factory(Provider<NishthaRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public BadgesViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static BadgesViewModel_Factory create(Provider<NishthaRepository> repoProvider) {
    return new BadgesViewModel_Factory(repoProvider);
  }

  public static BadgesViewModel newInstance(NishthaRepository repo) {
    return new BadgesViewModel(repo);
  }
}
