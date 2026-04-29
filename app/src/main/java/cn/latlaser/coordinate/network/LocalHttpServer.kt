package cn.latlaser.coordinate.network

import android.content.Context
import cn.latlaser.coordinate.data.database.AppDatabase
import cn.latlaser.coordinate.data.model.MeasureState
import cn.latlaser.coordinate.data.model.SystemConfig
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
    var misced: List<List<Pair<Double,Double>>>,
    var avgMisced: List<Pair<Double, Double>>,
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
        dao: cn.latlaser.coordinate.data.database.MeasurementResultDao
    ) = withContext(Dispatchers.IO) {
        val rawDataStr = gson.toJson(request.data)
        
        // 根据 measureId 从数据库取出杂项去除数
        val record = dao.getResultByMeasureId(request.measureId)

        if (record != null) {
            try {
                val miscCount = record.let {
                    val config = gson.fromJson(it.internalParameters, SystemConfig::class.java)
                    config.miscRemovalCount.toIntOrNull() ?: 0
                }

                val steps = mutableListOf<ProcessStep>()

                request.data?.forEach { group ->

                    val misced = mutableListOf<List<Pair<Double,Double>>>()
                    val p0misc = group.p0?.toPointList()?.misc2D(miscCount)
                    val p1misc = group.p1?.toPointList()?.misc2D(miscCount)
                    val p2misc = group.p2?.toPointList()?.misc2D(miscCount)
                    val p3misc = group.p3?.toPointList()?.misc2D(miscCount)
                    val p4misc = group.p4?.toPointList()?.misc2D(miscCount)
                    val p5misc = group.p5?.toPointList()?.misc2D(miscCount)
                    val p6misc = group.p6?.toPointList()?.misc2D(miscCount)
                    val p7misc = group.p7?.toPointList()?.misc2D(miscCount)

                    p0misc?.let { misced.add(it) }
                    p1misc?.let { misced.add(it) }
                    p2misc?.let { misced.add(it) }
                    p3misc?.let { misced.add(it) }
                    p4misc?.let { misced.add(it) }
                    p5misc?.let { misced.add(it) }
                    p6misc?.let { misced.add(it) }
                    p7misc?.let { misced.add(it) }

                    val avgMisced = mutableListOf<Pair<Double, Double>>()
                    val p0 = p0misc?.averagePoint()
                    val p1 = p1misc?.averagePoint()
                    val p2 = p2misc?.averagePoint()
                    val p3 = p3misc?.averagePoint()
                    val p4 = p4misc?.averagePoint()
                    val p5 = p5misc?.averagePoint()
                    val p6 = p6misc?.averagePoint()
                    val p7 = p7misc?.averagePoint()

                    p0?.let { avgMisced.add(it) }
                    p1?.let { avgMisced.add(it) }
                    p2?.let { avgMisced.add(it) }
                    p3?.let { avgMisced.add(it) }
                    p4?.let { avgMisced.add(it) }
                    p5?.let { avgMisced.add(it) }
                    p6?.let { avgMisced.add(it) }
                    p7?.let { avgMisced.add(it) }

                    val available = listOfNotNull(p0, p1, p2, p3, p4, p5, p6, p7)
                    val roundIntersections = mutableListOf<PointCoord>()
                    var roundAvg: PointCoord? = null

                    when {
                        available.size == 1 -> {
                            val p = available[0]
                            roundAvg = PointCoord(p.first, p.second)
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
                    steps.add(ProcessStep(available.size, misced, avgMisced, roundIntersections, roundAvg))
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
            } catch (e: Exception) {
                dao.updateMeasureResultFull(request.measureId, rawDataStr, "", e.message ?: "", MeasureState.FAILED)
            }
        }



    }

    private suspend fun updateResultWithDetail(
        measureId: Long,
        rawData: String,
        result: String,
        processDetail: String,
        dao: cn.latlaser.coordinate.data.database.MeasurementResultDao
    ) {
        dao.updateMeasureResultFull(measureId, rawData, result, processDetail, MeasureState.COMPLETED)
    }

    private fun calculateFourPointIntersection(
        pA: Pair<Double, Double>, pB: Pair<Double, Double>, pC: Pair<Double, Double>, pD: Pair<Double, Double>
    ): Pair<Double, Double>? {
        val x1 = pA.first; val y1 = pA.second
        val x2 = pB.first; val y2 = pB.second
        val x3 = pC.first; val y3 = pC.second
        val x4 = pD.first; val y4 = pD.second

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

    fun List<List<Double>>.toPointList(): List<Pair<Double, Double>> {
        return mapNotNull {
            if (it.size == 2) it[0] to it[1]
            else throw IllegalArgumentException("坐标数组必须包含且仅包含 2 个元素 (x,y)，当前长度：${it.size}，数组：$it")
        }
    }

    /**
     * 二维坐标平差（真正按【二维距离】去除杂项，不是按X排序）
     * 逻辑：
     * 1. 算中心点
     * 2. 按距离中心点远近排序
     * 3. 移除最远的 removeCount 个点（真正的去杂项）
     * 4. 若 removeCount 奇数 → 再移除一个最远点
     * 5. 最后算平均 x、平均 y
     */
    fun List<Pair<Double, Double>>.misc2D(removeCount: Int): List<Pair<Double, Double>>? {
        if (isEmpty() || size <= removeCount) return this
        if (removeCount < 0) return this

        // 1. 先算整体中心点（x平均、y平均）
        val originAvgX = map { it.first }.average()
        val originAvgY = map { it.second }.average()

        // 2. 按【到中心点的距离】从小到大排序（最近 → 最远）
        val sortedByDistance = sortedBy { (x, y) ->
            val dx = x - originAvgX
            val dy = y - originAvgY
            dx * dx + dy * dy // 距离平方（等价距离排序，更快）
        }

        // 3. 去除最远的 removeCount 个点
        val trimmed = sortedByDistance.dropLast(removeCount)
        if (trimmed.isEmpty()) return null

        return trimmed
    }
    

    fun List<Pair<Double, Double>>.averagePoint(): Pair<Double, Double>? {
        if (isEmpty()) return null
        val xAvg = map { it.first }.average()
        val yAvg = map { it.second }.average()
        return xAvg to yAvg
    }

    private suspend fun updateFailedStatus(
        measureId: Long, message: String?, dao: cn.latlaser.coordinate.data.database.MeasurementResultDao
    ) {
        dao.updateMeasureResultByMeasureId(measureId, "", message ?: "设备返回失败", MeasureState.FAILED)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}
