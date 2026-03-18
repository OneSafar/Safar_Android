package com.safar.app.data.repository;

import com.safar.app.data.remote.api.DhyanApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DhyanRepositoryImpl_Factory implements Factory<DhyanRepositoryImpl> {
  private final Provider<DhyanApi> apiProvider;

  public DhyanRepositoryImpl_Factory(Provider<DhyanApi> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public DhyanRepositoryImpl get() {
    return newInstance(apiProvider.get());
  }

  public static DhyanRepositoryImpl_Factory create(Provider<DhyanApi> apiProvider) {
    return new DhyanRepositoryImpl_Factory(apiProvider);
  }

  public static DhyanRepositoryImpl newInstance(DhyanApi api) {
    return new DhyanRepositoryImpl(api);
  }
}
