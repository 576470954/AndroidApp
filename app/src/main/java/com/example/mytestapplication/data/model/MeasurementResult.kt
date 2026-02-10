package com.example.mytestapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
    val deviceInstallationHeight: String,
    val controlPointId: Long,
    val monitoringStationInstallationHeight: String,
    val floorNumber: String,
    val pointNumber: String,
    val centerPointPairs: String,
    val rawData: String,
    val centerPointCoordinates: String,
    val createTime: Long = System.currentTimeMillis()
)
