package com.safar.app.data.repository;

import com.safar.app.data.local.dao.EkagraDao;
import com.safar.app.data.remote.api.EkagraApi;
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
public final class EkagraRepositoryImpl_Factory implements Factory<EkagraRepositoryImpl> {
  private final Provider<EkagraApi> apiProvider;

  private final Provider<EkagraDao> daoProvider;

  public EkagraRepositoryImpl_Factory(Provider<EkagraApi> apiProvider,
      Provider<EkagraDao> daoProvider) {
    this.apiProvider = apiProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public EkagraRepositoryImpl get() {
    return newInstance(apiProvider.get(), daoProvider.get());
  }

  public static EkagraRepositoryImpl_Factory create(Provider<EkagraApi> apiProvider,
      Provider<EkagraDao> daoProvider) {
    return new EkagraRepositoryImpl_Factory(apiProvider, daoProvider);
  }

  public static EkagraRepositoryImpl newInstance(EkagraApi api, EkagraDao dao) {
    return new EkagraRepositoryImpl(api, dao);
  }
}
