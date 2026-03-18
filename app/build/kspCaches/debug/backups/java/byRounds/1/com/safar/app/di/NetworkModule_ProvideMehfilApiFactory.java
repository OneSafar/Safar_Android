package com.safar.app.di;

import com.safar.app.data.remote.api.MehfilApi;
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
public final class NetworkModule_ProvideMehfilApiFactory implements Factory<MehfilApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideMehfilApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public MehfilApi get() {
    return provideMehfilApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideMehfilApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideMehfilApiFactory(retrofitProvider);
  }

  public static MehfilApi provideMehfilApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideMehfilApi(retrofit));
  }
}
