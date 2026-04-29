package cn.latlaser.coordinate.data

import cn.latlaser.coordinate.data.database.MeasurementResultDao
import cn.latlaser.coordinate.data.model.MeasurementResult
import kotlinx.coroutines.flow.Flow

class MeasurementResultRepository(private val measurementResultDao: MeasurementResultDao) {

    val allResults: Flow<List<MeasurementResult>> = measurementResultDao.getAllResults()

    fun getResultsForControlPoint(controlPointId: Long): Flow<List<MeasurementResult>> {
        return measurementResultDao.getResultsForControlPoint(controlPointId)
    }

    suspend fun insert(measurementResult: MeasurementResult): Long {
        return measurementResultDao.insert(measurementResult)
    }

    suspend fun deleteById(id: Long) {
        measurementResultDao.deleteById(id)
    }
}
