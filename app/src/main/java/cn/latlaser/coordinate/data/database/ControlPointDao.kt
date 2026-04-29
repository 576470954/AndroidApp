package cn.latlaser.coordinate.data.database

import androidx.room.*
import cn.latlaser.coordinate.data.model.ControlPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlPointDao {
    @Query("SELECT * FROM control_points ORDER BY CAST(serialNumber AS INTEGER) ASC")
    fun getAllControlPoints(): Flow<List<ControlPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControlPoint(controlPoint: ControlPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(controlPoints: List<ControlPoint>)

    @Update
    suspend fun updateControlPoint(controlPoint: ControlPoint)

    @Delete
    suspend fun deleteControlPoint(controlPoint: ControlPoint)

    @Query("DELETE FROM control_points WHERE id IN (:ids)")
    suspend fun deleteControlPointsByIds(ids: List<Long>)

    @Query("SELECT serialNumber FROM control_points")
    suspend fun getAllSerialNumbers(): List<String>

    @Query("SELECT name FROM control_points")
    suspend fun getAllPointNames(): List<String>
}
