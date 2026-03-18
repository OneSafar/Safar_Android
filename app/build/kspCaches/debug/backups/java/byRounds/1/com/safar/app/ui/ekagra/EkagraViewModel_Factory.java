package com.safar.app.ui.ekagra;

import com.safar.app.domain.repository.EkagraRepository;
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
public final class EkagraViewModel_Factory implements Factory<EkagraViewModel> {
  private final Provider<EkagraRepository> repoProvider;

  public EkagraViewModel_Factory(Provider<EkagraRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public EkagraViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static EkagraViewModel_Factory create(Provider<EkagraRepository> repoProvider) {
    return new EkagraViewModel_Factory(repoProvider);
  }

  public static EkagraViewModel newInstance(EkagraRepository repo) {
    return new EkagraViewModel(repo);
  }
}
