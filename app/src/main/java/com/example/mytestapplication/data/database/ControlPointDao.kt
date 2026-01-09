package com.example.mytestapplication.data.database

import androidx.room.*
import com.example.mytestapplication.data.model.ControlPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlPointDao {
    @Query("SELECT * FROM control_points ORDER BY createdAt DESC")
    fun getAllControlPoints(): Flow<List<ControlPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControlPoint(controlPoint: ControlPoint)

    @Update
    suspend fun updateControlPoint(controlPoint: ControlPoint)

    @Delete
    suspend fun deleteControlPoint(controlPoint: ControlPoint)

    @Query("DELETE FROM control_points WHERE id IN (:ids)")
    suspend fun deleteControlPointsByIds(ids: List<Long>)
}
