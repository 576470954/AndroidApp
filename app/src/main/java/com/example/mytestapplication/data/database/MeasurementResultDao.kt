package com.example.mytestapplication.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.mytestapplication.data.model.MeasurementResult
import com.example.mytestapplication.data.model.MeasurementResultWithControlPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementResultDao {
    @Insert
    suspend fun insert(measurementResult: MeasurementResult): Long

    @Query("SELECT * FROM measurement_results WHERE controlPointId = :controlPointId ORDER BY createTime DESC")
    fun getResultsForControlPoint(controlPointId: Long): Flow<List<MeasurementResult>>

    @Query("SELECT * FROM measurement_results ORDER BY createTime DESC")
    fun getAllResults(): Flow<List<MeasurementResult>>

    @Transaction
    @Query("SELECT * FROM measurement_results ORDER BY createTime DESC")
    fun getAllResultsWithControlPoint(): Flow<List<MeasurementResultWithControlPoint>>

    @Query("DELETE FROM measurement_results WHERE id = :id")
    suspend fun deleteById(id: Long)
}
