package com.safar.app.service;

import com.safar.app.data.local.SafarDataStore;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SafarFirebaseMessagingService_MembersInjector implements MembersInjector<SafarFirebaseMessagingService> {
  private final Provider<SafarDataStore> dataStoreProvider;

  public SafarFirebaseMessagingService_MembersInjector(Provider<SafarDataStore> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  public static MembersInjector<SafarFirebaseMessagingService> create(
      Provider<SafarDataStore> dataStoreProvider) {
    return new SafarFirebaseMessagingService_MembersInjector(dataStoreProvider);
  }

  @Override
  public void injectMembers(SafarFirebaseMessagingService instance) {
    injectDataStore(instance, dataStoreProvider.get());
  }

  @InjectedFieldSignature("com.safar.app.service.SafarFirebaseMessagingService.dataStore")
  public static void injectDataStore(SafarFirebaseMessagingService instance,
      SafarDataStore dataStore) {
    instance.dataStore = dataStore;
  }
}
