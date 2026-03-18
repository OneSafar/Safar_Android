package com.safar.app.data.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SafarDataStore_Factory implements Factory<SafarDataStore> {
  private final Provider<Context> contextProvider;

  public SafarDataStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SafarDataStore get() {
    return newInstance(contextProvider.get());
  }

  public static SafarDataStore_Factory create(Provider<Context> contextProvider) {
    return new SafarDataStore_Factory(contextProvider);
  }

  public static SafarDataStore newInstance(Context context) {
    return new SafarDataStore(context);
  }
}
