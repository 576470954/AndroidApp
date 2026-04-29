package cn.latlaser.coordinate.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.latlaser.coordinate.data.model.Project
import cn.latlaser.coordinate.data.model.ControlPoint
import cn.latlaser.coordinate.data.model.MeasurementResult
import cn.latlaser.coordinate.data.model.SystemConfig

@Database(entities = [Project::class, ControlPoint::class, MeasurementResult::class, SystemConfig::class], version = 13, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun controlPointDao(): ControlPointDao
    abstract fun measurementResultDao(): MeasurementResultDao
    abstract fun systemConfigDao(): SystemConfigDao

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
