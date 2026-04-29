package cn.latlaser.coordinate.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MeasurementResultWithControlPoint(
    @Embedded val measurementResult: MeasurementResult,
    @Relation(
        parentColumn = "controlPointId",
        entityColumn = "id"
    )
    val controlPoint: ControlPoint
)
