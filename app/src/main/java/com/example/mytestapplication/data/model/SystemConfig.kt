package com.example.mytestapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_configs")
data class SystemConfig(
    @PrimaryKey val id: Int = 1, // 只有一行记录，所有项目共用
    val rangeCalibration: String = "0",
    val stationCalibrationH: String = "0",
    val standardSurfaceCalibration: String = "0",
    val shellWheelbaseCalibration: String = "75",
    val light2Calibration: String = "0",
    val deviceWaitTime: String = "3",
    val collectionCount: String = "1",
    val miscRemovalCount: String = "0",
    val lightSpotSize: String = "2",
    val lightSpotColor: String = "RED"
)
