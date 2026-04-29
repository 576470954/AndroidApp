package cn.latlaser.coordinate.data.database

import androidx.room.TypeConverter
import cn.latlaser.coordinate.data.model.MeasureState

class Converters {
    @TypeConverter
    fun fromMeasureState(value: MeasureState): String {
        return value.name
    }

    @TypeConverter
    fun toMeasureState(value: String): MeasureState {
        return try {
            MeasureState.valueOf(value)
        } catch (e: Exception) {
            MeasureState.INITIALIZED
        }
    }
}
