package com.example.mytestapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class MeasureState {
    INITIALIZED, // 初始化
    MEASURING,   // 测量中
    FAILED,      // 失败
    TIMEOUT,     // 超时
    COMPLETED    // 完成
}

@Entity(
    tableName = "measurement_results",
    foreignKeys = [ForeignKey(
        entity = ControlPoint::class,
        parentColumns = ["id"],
        childColumns = ["controlPointId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MeasurementResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measureId: Long, // 测量唯一标识
    val state: MeasureState = MeasureState.INITIALIZED,

    val deviceInstallationHeight: String,
    val controlPointId: Long,
    val monitoringStationInstallationHeight: String,
    val floorNumber: String,
    val pointNumber: String,
    val centerPointPairs: String,
    val internalParameters: String = "",

    val rawData: String = "",
    val processDetail: String = "",
    val heightResult: String = "",
    val result: String = "",

    val createTime: Long = System.currentTimeMillis()
)
