package com.example.mytestapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mytestapplication.data.model.Project
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.data.model.MeasurementResult

@Database(entities = [Project::class, ControlPoint::class, MeasurementResult::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun controlPointDao(): ControlPointDao
    abstract fun measurementResultDao(): MeasurementResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "project_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
