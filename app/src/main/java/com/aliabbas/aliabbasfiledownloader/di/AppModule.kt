package com.aliabbas.aliabbasfiledownloader.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.aliabbas.aliabbasfiledownloader.db.AppDB
import com.aliabbas.aliabbasfiledownloader.db.DownloadEntityDAO
import com.aliabbas.aliabbasfiledownloader.processHelper.FileDownloader
import com.aliabbas.aliabbasfiledownloader.repositories.MainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDB(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        AppDB::class.java,
        "MY_LOCAL_DB"
    ).fallbackToDestructiveMigration().build()

    @Singleton
    @Provides
    fun provideDownloadEntityDao(db: AppDB) = db.getDownloadEntityDao()

    @Provides
    @Singleton
    fun provideMainRepository(downloadEntityDAO: DownloadEntityDAO) = MainRepository(downloadEntityDAO)


    @Provides
    @Singleton
    fun provideFileDownloader(mainRepository: MainRepository) = FileDownloader(mainRepository)
}