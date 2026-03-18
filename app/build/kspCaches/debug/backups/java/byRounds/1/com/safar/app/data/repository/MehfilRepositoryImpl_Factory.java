package com.safar.app.data.repository;

import com.safar.app.data.local.dao.MessageDao;
import com.safar.app.data.local.dao.PostDao;
import com.safar.app.data.remote.api.MehfilApi;
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
public final class MehfilRepositoryImpl_Factory implements Factory<MehfilRepositoryImpl> {
  private final Provider<MehfilApi> apiProvider;

  private final Provider<PostDao> postDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  public MehfilRepositoryImpl_Factory(Provider<MehfilApi> apiProvider,
      Provider<PostDao> postDaoProvider, Provider<MessageDao> messageDaoProvider) {
    this.apiProvider = apiProvider;
    this.postDaoProvider = postDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
  }

  @Override
  public MehfilRepositoryImpl get() {
    return newInstance(apiProvider.get(), postDaoProvider.get(), messageDaoProvider.get());
  }

  public static MehfilRepositoryImpl_Factory create(Provider<MehfilApi> apiProvider,
      Provider<PostDao> postDaoProvider, Provider<MessageDao> messageDaoProvider) {
    return new MehfilRepositoryImpl_Factory(apiProvider, postDaoProvider, messageDaoProvider);
  }

  public static MehfilRepositoryImpl newInstance(MehfilApi api, PostDao postDao,
      MessageDao messageDao) {
    return new MehfilRepositoryImpl(api, postDao, messageDao);
  }
}
