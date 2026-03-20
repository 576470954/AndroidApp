package com.example.mytestapplication.network

import android.content.Context
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.MeasureState
import com.google.gson.Gson
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

// --- 基础数据模型 ---

data class MeasureResultData(
    val p0: List<List<Double>>? = null,
    val p1: List<List<Double>>? = null,
    val p2: List<List<Double>>? = null,
    val p3: List<List<Double>>? = null,
    val p4: List<List<Double>>? = null,
    val p5: List<List<Double>>? = null,
    val p6: List<List<Double>>? = null,
    val p7: List<List<Double>>? = null
)

data class UploadMeasureResultRequest(
    val measureId: Long,
    val status: String,
    val message: String? = null,
    val data: List<MeasureResultData>? = null
)

// --- 用于 processDetail 和 result 的 JSON 模型 ---

data class PointCoord(val x: Double, val y: Double)

data class ProcessStep(
    val pointCount: Int,
    val intersections: List<PointCoord>,
    val roundAverage: PointCoord?
)

data class ProcessDetail(
    val steps: List<ProcessStep>,
    val finalAverage: FinalAverage?
)

data class FinalAverage(
    val x: Double,
    val y: Double,
    val validRounds: Int
)

object LocalHttpServer {
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    fun start(context: Context, port: Int = 8000) {
        if (server != null) return

        val database = AppDatabase.getDatabase(context)
        val measurementResultDao = database.measurementResultDao()

        scope.launch {
            try {
                server = embeddedServer(CIO, port = port) {
                    install(ContentNegotiation) {
                        gson {
                            setPrettyPrinting()
                            serializeNulls()
                        }
                    }
                    routing {
                        post("/api/uploadMeasureResult") {
                            try {
                                val request = call.receive<UploadMeasureResultRequest>()

                                // --- 打印请求内容 ---
                                println("收到测量结果上报请求: ")
                                println("MeasureID: ${request.measureId}")
                                println("Status: ${request.status}")
                                println("Message: ${request.message}")
                                println("Data: ${gson.toJson(request.data)}")
                                // -----------------

                                if (request.status == "success" && request.data != null) {
                                    processAndSaveResults(request, measurementResultDao)
                                } else {
                                    updateFailedStatus(request.measureId, request.message, measurementResultDao)
                                }
                                call.respond(BaseResponse("success", "已处理测量结果"))
                            } catch (e: Exception) {
                                println("解析上报请求失败: ${e.message}")
                                e.printStackTrace()
                                call.respond(BaseResponse("error", "处理失败: ${e.message}"))
                            }
                        }
                    }
                }.start(wait = false)
            } catch (e: Exception) {
                println("服务器启动异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun processAndSaveResults(
        request: UploadMeasureResultRequest,
        dao: com.example.mytestapplication.data.database.MeasurementResultDao
    ) = withContext(Dispatchers.IO) {
        val rawDataStr = gson.toJson(request.data)
        val steps = mutableListOf<ProcessStep>()

        request.data?.forEach { group ->
            val p0 = group.p0?.firstOrNull()
            val p1 = group.p1?.firstOrNull()
            val p2 = group.p2?.firstOrNull()
            val p3 = group.p3?.firstOrNull()
            val p4 = group.p4?.firstOrNull()
            val p5 = group.p5?.firstOrNull()
            val p6 = group.p6?.firstOrNull()
            val p7 = group.p7?.firstOrNull()

            val available = listOfNotNull(p0, p1, p2, p3, p4, p5, p6, p7)
            var roundIntersections = mutableListOf<PointCoord>()
            var roundAvg: PointCoord? = null

            when {
                available.size == 1 -> {
                    val p = available[0]
                    roundAvg = PointCoord(p[0], p[1])
                    roundIntersections.add(roundAvg)
                }
                p0 != null && p1 != null && p2 != null && p3 != null && available.size == 4 -> {
                    val inter = calculateFourPointIntersection(p0, p1, p2, p3)
                    if (inter != null) {
                        roundAvg = PointCoord(inter.first, inter.second)
                        roundIntersections.add(roundAvg)
                    }
                }
                p0 != null && p1 != null && p2 != null && p3 != null &&
                        p4 != null && p5 != null && p6 != null && p7 != null -> {
                    val inter1 = calculateFourPointIntersection(p0, p2, p4, p6)
                    val inter2 = calculateFourPointIntersection(p1, p3, p5, p7)
                    
                    if (inter1 != null) roundIntersections.add(PointCoord(inter1.first, inter1.second))
                    if (inter2 != null) roundIntersections.add(PointCoord(inter2.first, inter2.second))

                    if (roundIntersections.isNotEmpty()) {
                        roundAvg = PointCoord(
                            roundIntersections.map { it.x }.average(),
                            roundIntersections.map { it.y }.average()
                        )
                    }
                }
            }
            steps.add(ProcessStep(available.size, roundIntersections, roundAvg))
        }

        val validSteps = steps.filter { it.roundAverage != null }
        var finalResultStr = ""
        var processDetailStr = ""

        if (validSteps.isNotEmpty()) {
            val finalX = validSteps.map { it.roundAverage!!.x }.average()
            val finalY = validSteps.map { it.roundAverage!!.y }.average()
            
            val finalAverage = FinalAverage(finalX, finalY, validSteps.size)
            processDetailStr = gson.toJson(ProcessDetail(steps, finalAverage))
            finalResultStr = gson.toJson(PointCoord(finalX, finalY))
        } else {
            processDetailStr = gson.toJson(ProcessDetail(steps, null))
        }

        updateResultWithDetail(request.measureId, rawDataStr, finalResultStr, processDetailStr, dao)
    }

    private suspend fun updateResultWithDetail(
        measureId: Long,
        rawData: String,
        result: String,
        processDetail: String,
        dao: com.example.mytestapplication.data.database.MeasurementResultDao
    ) {
        dao.updateMeasureResultFull(measureId, rawData, result, processDetail, MeasureState.COMPLETED)
    }

    private fun calculateFourPointIntersection(
        pA: List<Double>, pB: List<Double>, pC: List<Double>, pD: List<Double>
    ): Pair<Double, Double>? {
        if (pA.size < 2 || pB.size < 2 || pC.size < 2 || pD.size < 2) return null
        val x1 = pA[0]; val y1 = pA[1]
        val x2 = pB[0]; val y2 = pB[1]
        val x3 = pC[0]; val y3 = pC[1]
        val x4 = pD[0]; val y4 = pD[1]

        val a1 = y3 - y1
        val b1 = x1 - x3
        val c1 = x3 * y1 - x1 * y3

        val a2 = y4 - y2
        val b2 = x2 - x4
        val c2 = x4 * y2 - x2 * y4

        val denominator = a1 * b2 - a2 * b1
        return if (abs(denominator) > 1e-9) {
            val x = (b1 * c2 - b2 * c1) / denominator
            val y = (a2 * c1 - a1 * c2) / denominator
            Pair(x, y)
        } else {
            null
        }
    }

    private suspend fun updateFailedStatus(
        measureId: Long, message: String?, dao: com.example.mytestapplication.data.database.MeasurementResultDao
    ) {
        dao.updateMeasureResultByMeasureId(measureId, "", message ?: "设备返回失败", MeasureState.FAILED)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}
