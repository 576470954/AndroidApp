package com.example.mytestapplication.data.database

import androidx.room.*
import com.example.mytestapplication.data.model.SystemConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemConfigDao {
    @Query("SELECT * FROM system_configs WHERE id = 1")
    fun getConfig(): Flow<SystemConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SystemConfig)
}
