package com.safar.app.di;

import com.safar.app.data.remote.api.NishthaApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideNishthaApiFactory implements Factory<NishthaApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideNishthaApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public NishthaApi get() {
    return provideNishthaApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideNishthaApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideNishthaApiFactory(retrofitProvider);
  }

  public static NishthaApi provideNishthaApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideNishthaApi(retrofit));
  }
}
