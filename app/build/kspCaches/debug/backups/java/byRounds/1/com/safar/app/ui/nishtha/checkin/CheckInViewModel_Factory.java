package com.safar.app.ui.nishtha.checkin;

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
public final class CheckInViewModel_Factory implements Factory<CheckInViewModel> {
  private final Provider<NishthaRepository> repoProvider;

  public CheckInViewModel_Factory(Provider<NishthaRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public CheckInViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static CheckInViewModel_Factory create(Provider<NishthaRepository> repoProvider) {
    return new CheckInViewModel_Factory(repoProvider);
  }

  public static CheckInViewModel newInstance(NishthaRepository repo) {
    return new CheckInViewModel(repo);
  }
}
