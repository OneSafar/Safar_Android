package com.safar.app.ui.mehfil;

import com.safar.app.domain.repository.MehfilRepository;
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
public final class MehfilViewModel_Factory implements Factory<MehfilViewModel> {
  private final Provider<MehfilRepository> repoProvider;

  public MehfilViewModel_Factory(Provider<MehfilRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public MehfilViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static MehfilViewModel_Factory create(Provider<MehfilRepository> repoProvider) {
    return new MehfilViewModel_Factory(repoProvider);
  }

  public static MehfilViewModel newInstance(MehfilRepository repo) {
    return new MehfilViewModel(repo);
  }
}
