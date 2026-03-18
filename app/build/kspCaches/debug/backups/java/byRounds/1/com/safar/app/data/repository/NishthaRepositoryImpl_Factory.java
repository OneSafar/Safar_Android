package com.safar.app.data.repository;

import com.google.gson.Gson;
import com.safar.app.data.local.dao.BadgeDao;
import com.safar.app.data.local.dao.GoalDao;
import com.safar.app.data.local.dao.JournalDao;
import com.safar.app.data.local.dao.MoodCheckInDao;
import com.safar.app.data.local.dao.StreakDao;
import com.safar.app.data.remote.api.NishthaApi;
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
public final class NishthaRepositoryImpl_Factory implements Factory<NishthaRepositoryImpl> {
  private final Provider<NishthaApi> apiProvider;

  private final Provider<MoodCheckInDao> checkInDaoProvider;

  private final Provider<JournalDao> journalDaoProvider;

  private final Provider<GoalDao> goalDaoProvider;

  private final Provider<StreakDao> streakDaoProvider;

  private final Provider<BadgeDao> badgeDaoProvider;

  private final Provider<Gson> gsonProvider;

  public NishthaRepositoryImpl_Factory(Provider<NishthaApi> apiProvider,
      Provider<MoodCheckInDao> checkInDaoProvider, Provider<JournalDao> journalDaoProvider,
      Provider<GoalDao> goalDaoProvider, Provider<StreakDao> streakDaoProvider,
      Provider<BadgeDao> badgeDaoProvider, Provider<Gson> gsonProvider) {
    this.apiProvider = apiProvider;
    this.checkInDaoProvider = checkInDaoProvider;
    this.journalDaoProvider = journalDaoProvider;
    this.goalDaoProvider = goalDaoProvider;
    this.streakDaoProvider = streakDaoProvider;
    this.badgeDaoProvider = badgeDaoProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public NishthaRepositoryImpl get() {
    return newInstance(apiProvider.get(), checkInDaoProvider.get(), journalDaoProvider.get(), goalDaoProvider.get(), streakDaoProvider.get(), badgeDaoProvider.get(), gsonProvider.get());
  }

  public static NishthaRepositoryImpl_Factory create(Provider<NishthaApi> apiProvider,
      Provider<MoodCheckInDao> checkInDaoProvider, Provider<JournalDao> journalDaoProvider,
      Provider<GoalDao> goalDaoProvider, Provider<StreakDao> streakDaoProvider,
      Provider<BadgeDao> badgeDaoProvider, Provider<Gson> gsonProvider) {
    return new NishthaRepositoryImpl_Factory(apiProvider, checkInDaoProvider, journalDaoProvider, goalDaoProvider, streakDaoProvider, badgeDaoProvider, gsonProvider);
  }

  public static NishthaRepositoryImpl newInstance(NishthaApi api, MoodCheckInDao checkInDao,
      JournalDao journalDao, GoalDao goalDao, StreakDao streakDao, BadgeDao badgeDao, Gson gson) {
    return new NishthaRepositoryImpl(api, checkInDao, journalDao, goalDao, streakDao, badgeDao, gson);
  }
}
