package com.example.mytestapplication.data.database

import androidx.room.*
import com.example.mytestapplication.data.model.MeasureState
import com.example.mytestapplication.data.model.MeasurementResult
import com.example.mytestapplication.data.model.MeasurementResultWithControlPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementResultDao {
    @Insert
    suspend fun insert(measurementResult: MeasurementResult): Long

    @Update
    suspend fun update(measurementResult: MeasurementResult)

    @Query("UPDATE measurement_results SET state = :state WHERE id = :id")
    suspend fun updateState(id: Long, state: MeasureState)

    @Query("UPDATE measurement_results SET heightResult = :heightResult WHERE id = :id")
    suspend fun updateHeightResult(id: Long, heightResult: String)

    @Query("UPDATE measurement_results SET rawData = :rawData, result = :result, state = :state WHERE measureId = :measureId")
    suspend fun updateMeasureResultByMeasureId(measureId: Long, rawData: String, result: String, state: MeasureState)

    @Query("UPDATE measurement_results SET rawData = :rawData, result = :result, processDetail = :processDetail, state = :state WHERE measureId = :measureId")
    suspend fun updateMeasureResultFull(measureId: Long, rawData: String, result: String, processDetail: String, state: MeasureState)

    @Query("UPDATE measurement_results SET state = :newState WHERE state = :oldState AND createTime < :threshold")
    suspend fun updateExpiredStates(oldState: MeasureState, newState: MeasureState, threshold: Long)

    @Query("SELECT COUNT(*) > 0 FROM measurement_results WHERE state = :state AND createTime >= :threshold")
    suspend fun hasActiveTasks(state: MeasureState, threshold: Long): Boolean

    @Query("SELECT * FROM measurement_results WHERE controlPointId = :controlPointId ORDER BY createTime DESC")
    fun getResultsForControlPoint(controlPointId: Long): Flow<List<MeasurementResult>>

    @Query("SELECT * FROM measurement_results ORDER BY createTime DESC")
    fun getAllResults(): Flow<List<MeasurementResult>>

    @Transaction
    @Query("SELECT * FROM measurement_results ORDER BY createTime DESC")
    fun getAllResultsWithControlPoint(): Flow<List<MeasurementResultWithControlPoint>>

    @Query("DELETE FROM measurement_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM measurement_results WHERE measureId = :measureId LIMIT 1")
    suspend fun getResultByMeasureId(measureId: Long): MeasurementResult?
}
