package com.example.mytestapplication.network

data class MeasureRequest(
    val times: Int,
    val centralPointCount: Int,
    val measureId: Long,
    val deviceWaitTime: Int,
    val collectionCount: Int,
    val miscRemovalCount: Int
)

data class CancelMeasureRequest(
    val measureId: Long
)

data class DrawCircleRequest(
    val x: Int,
    val y: Int,
    val radius: Int,
    val color: String
)

data class BaseResponse(
    val status: String,
    val message: String? = null
)

data class DeviceStateData(
    val statusCode: String,
    val isMeasuring: Int
)

data class DeviceStateResponse(
    val status: String,
    val message: String? = null,
    val data: DeviceStateData? = null
)

class Device(var baseUrl: String) {

    /**
     * 开始测量
     */
    suspend fun measure(
        times: Int,
        centralPointCount: Int,
        measureId: Long,
        deviceWaitTime: Int,
        collectionCount: Int,
        miscRemovalCount: Int
    ): BaseResponse {
        val url = "$baseUrl/api/measure"
        val body = MeasureRequest(
            times = times,
            centralPointCount = centralPointCount,
            measureId = measureId,
            deviceWaitTime = deviceWaitTime,
            collectionCount = collectionCount,
            miscRemovalCount = miscRemovalCount
        )
        return when (val result = HttpClient.post(url, body, BaseResponse::class.java)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> BaseResponse("error", result.message)
        }
    }

    /**
     * 获取当前状态
     */
    suspend fun getCurrentState(): DeviceStateResponse {
        val url = "$baseUrl/api/getCurrentState"
        return when (val result = HttpClient.post(url, emptyMap<String, Any>(),DeviceStateResponse::class.java)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> DeviceStateResponse("error", result.message)
        }
    }

    /**
     * 取消测量
     */
    suspend fun cancelMeasure(measureId: Long): BaseResponse {
        val url = "$baseUrl/api/cancelMeasure"
        val body = CancelMeasureRequest(measureId)
        return when (val result = HttpClient.post(url, body, BaseResponse::class.java)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> BaseResponse("error", result.message)
        }
    }

    /**
     * 在屏幕上绘制圆形
     */
    suspend fun drawCircle(x: Int, y: Int, radius: Int, color: String): BaseResponse {
        val url = "$baseUrl/screen/draw-circle"
        val body = DrawCircleRequest(x, y, radius, color)
        return when (val result = HttpClient.post(url, body, BaseResponse::class.java)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> BaseResponse("error", result.message)
        }
    }

    /**
     * 测距
     */
    suspend fun distanceMeasured(measureId: Long): DistanceMeasuredResp {
        val url = "$baseUrl/api/lora/distance-measured"
        val body = DistanceMeasuredReq(measureId)
        return when (val result = HttpClient.post(url, body, DistanceMeasuredResp::class.java)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> DistanceMeasuredResp(false, 0.0, 0.0, 0, 0)
        }
    }
}

data class DistanceMeasuredReq(
    val measureId: Long
)

data class DistanceMeasuredResp(
    val success: Boolean,
    val distance_mm: Double,
    val distance_m: Double,
    val signal_strength: Int,
    val sequence: Int,
)


