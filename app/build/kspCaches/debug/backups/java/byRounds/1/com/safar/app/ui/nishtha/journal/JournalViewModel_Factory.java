package com.safar.app.ui.nishtha.journal;

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
public final class JournalViewModel_Factory implements Factory<JournalViewModel> {
  private final Provider<NishthaRepository> repoProvider;

  public JournalViewModel_Factory(Provider<NishthaRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public JournalViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static JournalViewModel_Factory create(Provider<NishthaRepository> repoProvider) {
    return new JournalViewModel_Factory(repoProvider);
  }

  public static JournalViewModel newInstance(NishthaRepository repo) {
    return new JournalViewModel(repo);
  }
}
