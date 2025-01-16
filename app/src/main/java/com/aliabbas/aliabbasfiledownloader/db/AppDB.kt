package com.aliabbas.aliabbasfiledownloader.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadEntity::class], version = 4)
abstract class AppDB : RoomDatabase() {

    abstract fun getDownloadEntityDao() : DownloadEntityDAO

}