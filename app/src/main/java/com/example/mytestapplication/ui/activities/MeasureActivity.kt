package com.example.mytestapplication.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.database.MeasurementResultDao
import com.example.mytestapplication.data.database.ProjectDao
import com.example.mytestapplication.data.database.SystemConfigDao
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.data.model.MeasureState
import com.example.mytestapplication.data.model.MeasurementResult
import com.example.mytestapplication.data.model.SystemConfig
import com.example.mytestapplication.network.Device
import com.example.mytestapplication.network.PointCoord
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import com.example.mytestapplication.ui.common.VerticalDivider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class MeasureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent.getLongExtra("PROJECT_ID", -1L)
        val database = AppDatabase.getDatabase(this)
        val controlPointDao = database.controlPointDao()
        val measurementResultDao = database.measurementResultDao()
        val systemConfigDao = database.systemConfigDao()
        val projectDao = database.projectDao()

        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val points by controlPointDao.getAllControlPoints().collectAsState(initial = emptyList())
                    MeasureScreen(
                        projectId = projectId,
                        points = points,
                        onBack = { finish() },
                        measurementResultDao = measurementResultDao,
                        systemConfigDao = systemConfigDao,
                        projectDao = projectDao
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    projectId: Long,
    points: List<ControlPoint>,
    onBack: () -> Unit,
    measurementResultDao: MeasurementResultDao,
    systemConfigDao: SystemConfigDao,
    projectDao: ProjectDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 监听所有测量结果
    val allResults by measurementResultDao.getAllResults().collectAsState(initial = emptyList())

    // 记录当前会话的测量 ID，初始为 null（即重置状态）
    var currentSessionMeasureId by remember { mutableStateOf<Long?>(null) }

    // 获取项目中的设备地址
    var deviceBaseUrl by remember { mutableStateOf("http://192.168.1.149:8080") }
    LaunchedEffect(projectId) {
        if (projectId != -1L) {
            val allProjects = withContext(Dispatchers.IO) { projectDao.getAllProjects().first() }
            val project = allProjects.find { it.id == projectId }
            project?.let {
                deviceBaseUrl = it.deviceUrl
            }
        }
    }

    val device = remember(deviceBaseUrl) { Device(deviceBaseUrl) }

    // 基本设置状态
    var equipmentHeight by remember { mutableStateOf("0") }
    var stationHeight by remember { mutableStateOf("0") }
    var floorNumber by remember { mutableStateOf("1") }
    var pointNumber by remember { mutableStateOf("C1") }
    var centerPointPairs by remember { mutableStateOf("1") }
    var measureCountInput by remember { mutableStateOf("1") }

    // 内部参数状态
    var isInternalExpanded by remember { mutableStateOf(false) }
    var rangeCalibration by remember { mutableStateOf("0") }
    var stationCalibrationH by remember { mutableStateOf("0") }
    var standardSurfaceCalibration by remember { mutableStateOf("0") }
    var shellWheelbaseCalibration by remember { mutableStateOf("0.075") }
    var light2Calibration by remember { mutableStateOf("0") }
    var deviceWaitTime by remember { mutableStateOf("3") }
    var collectionCount by remember { mutableStateOf("1") }
    var miscRemovalCount by remember { mutableStateOf("0") }
    var lightSpotSize by remember { mutableStateOf("2") }
    var lightSpotColor by remember { mutableStateOf("RED") }

    var isFactoryEditable by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    // 初始加载内部参数
    LaunchedEffect(Unit) {
        val config = withContext(Dispatchers.IO) { systemConfigDao.getConfig().first() }
        config?.let {
            rangeCalibration = it.rangeCalibration
            stationCalibrationH = it.stationCalibrationH
            standardSurfaceCalibration = it.standardSurfaceCalibration
            shellWheelbaseCalibration = it.shellWheelbaseCalibration
            light2Calibration = it.light2Calibration
            deviceWaitTime = it.deviceWaitTime
            collectionCount = it.collectionCount
            miscRemovalCount = it.miscRemovalCount
            lightSpotSize = it.lightSpotSize
            lightSpotColor = it.lightSpotColor
        }
    }

    // 第一行展示的实时坐标
    var xValue by remember { mutableStateOf("") }
    var yValue by remember { mutableStateOf("") }
    var hLabel by remember { mutableStateOf("H(靶面)") }
    var hMenuExpanded by remember { mutableStateOf(false) }
    var distanceResult by remember { mutableDoubleStateOf(-1.0) }

    // 控制点选择状态
    var selectedPointId by remember { mutableStateOf<Long?>(null) }
    var selectedPointName by remember { mutableStateOf("未选择") }
    var selectedPointH by remember { mutableDoubleStateOf(0.0) }
    var showPointSelector by remember { mutableStateOf(false) }

    // 测量
    var showMeasureStateDialog by remember { mutableStateOf(false) }

    // 根据公式计算显示的 H 值
    val hValueDisplay = remember(hLabel, equipmentHeight, stationHeight, selectedPointH, rangeCalibration, stationCalibrationH, standardSurfaceCalibration, shellWheelbaseCalibration, light2Calibration, distanceResult) {
        if (distanceResult < 0) {
            ""
        } else {
            val heights = calculateHeights(
                equipmentHeight,
                stationHeight,
                selectedPointH,
                rangeCalibration,
                stationCalibrationH,
                standardSurfaceCalibration,
                shellWheelbaseCalibration,
                light2Calibration,
                distanceResult
            )

            val result = when (hLabel) {
                "H(地面)" -> heights.groundH
                "H(墙面)" -> heights.wallH
                else -> heights.targetH
            }
            "%.3f".format(result)
        }
    }


    // 步进状态
    var currentFloorProgress by remember { mutableIntStateOf(0) }
    var floorOrderAsc by remember { mutableStateOf(true) }
    var floorInterval by remember { mutableStateOf("1") }
    var showFloorSettingsDialog by remember { mutableStateOf(false) }
    var pointOrderAsc by remember { mutableStateOf(true) }
    var pointInterval by remember { mutableStateOf("1") }
    var pointsPerFloor by remember { mutableStateOf("4") }
    var showPointSettingsDialog by remember { mutableStateOf(false) }

    val centerPointOptions = listOf("1", "4", "8")

    // 监听测量结果，仅处理当前会话的任务
    LaunchedEffect(allResults, currentSessionMeasureId) {
        if (currentSessionMeasureId == null) {
            // 这里原本会在重置时清空坐标，但由于用户希望坐标不随测量结果变化，
            // 且通常在选择控制点时已填入，保持其不变可能是更合理的解读。
            return@LaunchedEffect
        }

        val currentTask = allResults.find { it.measureId == currentSessionMeasureId }
        if (currentTask != null) {
            if (currentTask.state == MeasureState.COMPLETED && currentTask.result.isNotBlank()) {
                try {
                    showMeasureStateDialog = true

                    val coord = Gson().fromJson(currentTask.result, PointCoord::class.java)
                    val internalParams = Gson().fromJson(currentTask.internalParameters, SystemConfig::class.java)
                    if (coord != null) {
                        // 测量结果仅用于绘制 circle，不再更新 xValue 和 yValue
                        scope.launch {
                            val resp = device.drawCircle(
                                x = coord.x.toInt(),
                                y = coord.y.toInt(),
                                radius = internalParams.lightSpotSize.toIntOrNull() ?: 2,
                                color = internalParams.lightSpotColor
                            )
                            Toast.makeText(context, if (resp.status == "success") "绘制命令已发送" else "绘制失败: ${resp.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "结果格式错误", Toast.LENGTH_SHORT).show()
                    }


                } catch (e: Exception) {
                    Toast.makeText(context, "解析结果失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("输入出厂密码") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text("密码") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (password == "123456") {
                        isFactoryEditable = true
                        showPasswordDialog = false
                    } else {
                        Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确定") }
            }
        )
    }

    if (showPointSelector) {
        ControlPointSelectionDialog(points, { showPointSelector = false }) { point ->
            selectedPointId = point.id
            selectedPointName = point.name
            selectedPointH = point.h.toDoubleOrNull() ?: 0.0
            xValue = "%.3f".format(point.x.toDoubleOrNull() ?: 0.0)
            yValue = "%.3f".format(point.y.toDoubleOrNull() ?: 0.0)
            showPointSelector = false
        }
    }
    if (showFloorSettingsDialog) {
        FloorSettingsDialog(floorOrderAsc, floorInterval, { showFloorSettingsDialog = false }) { isAsc, inv ->
            floorOrderAsc = isAsc; floorInterval = inv; showFloorSettingsDialog = false
        }
    }
    if (showPointSettingsDialog) {
        PointSettingsDialog(pointOrderAsc, pointInterval, pointsPerFloor, { showPointSettingsDialog = false }) { isAsc, inv, perF ->
            pointOrderAsc = isAsc; pointInterval = inv; pointsPerFloor = perF; showPointSettingsDialog = false
        }
    }

    if (showMeasureStateDialog) {
        AlertDialog(
            onDismissRequest = { showMeasureStateDialog = false },
            title = { Text("测量结果") },
            text = { Text("测量完成") },
            confirmButton = { Button(onClick = {
                showMeasureStateDialog = false
                // 步进
                currentFloorProgress++
                if (currentFloorProgress >= (pointsPerFloor.toIntOrNull() ?: 4)) {
                    currentFloorProgress = 0
                    val nextF = (floorNumber.toIntOrNull() ?: 1) + (if (floorOrderAsc) 1 else -1) * (floorInterval.toIntOrNull() ?: 1)
                    floorNumber = nextF.toString()
                    pointNumber = "C1"
                } else {
                    val prefix = pointNumber.takeWhile { !it.isDigit() }
                    val suffix = (pointNumber.dropWhile { !it.isDigit() }.toIntOrNull() ?: 1) + (if (pointOrderAsc) 1 else -1) * (pointInterval.toIntOrNull() ?: 1)
                    pointNumber = "$prefix$suffix"
                }
                //
                distanceResult = -1.0
             }) { Text("测量下个点") } },

            dismissButton = { Button(onClick = {
                showMeasureStateDialog = false
            }) { Text("确定") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "高层放样", style = MaterialTheme.typography.headlineMedium)

        // --- 实时坐标展示表 ---
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell("X", Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableHeaderCell("Y", Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.clickable { hMenuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = hLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = hMenuExpanded, onDismissRequest = { hMenuExpanded = false }) {
                        listOf("H(靶面)", "H(地面)", "H(墙面)").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { hLabel = it; hMenuExpanded = false })
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableCell(xValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(yValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(hValueDisplay, Modifier.weight(1f))
            }
        }

        MeasureInputRow("光源站安装高*", equipmentHeight, { newValue ->
            val newEqH = newValue.toDoubleOrNull()
            val currentL = shellWheelbaseCalibration.toDoubleOrNull() ?: 0.0
            equipmentHeight = newValue
            if (newEqH != null && newEqH < currentL) {
                Toast.makeText(context, "光源站安装高小于壳体轴距标定值（见说明书附件B）", Toast.LENGTH_SHORT).show()
            }
        }, "m")

        MeasureInputRow("监测站安装高*", stationHeight, { stationHeight = it }, "m")

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("控制点*:", Modifier.width(100.dp), fontSize = 16.sp)
            Text(selectedPointName, Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline).clickable { showPointSelector = true }.padding(12.dp))
            Button(onClick = { showPointSelector = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("选择", fontSize = 12.sp) }
        }

        SettingsInputRow("楼层号*", floorNumber, { floorNumber = it }, { showFloorSettingsDialog = true })
        SettingsInputRow("点号*", pointNumber, { pointNumber = it }, { showPointSettingsDialog = true }, KeyboardType.Text)

        MeasureDropdownRow(
            label = "中心点对数",
            selectedValue = centerPointPairs,
            options = centerPointOptions,
            onValueChange = { centerPointPairs = it },
            showAsterisk = true
        )

        // --- 内部参数设置 ---
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant).padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isInternalExpanded = !isInternalExpanded }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("内部参数设置", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(if (isInternalExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
            AnimatedVisibility(visible = isInternalExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasureInputRow("测距校准值", rangeCalibration, { rangeCalibration = it }, "m", enabled = isFactoryEditable)
                    MeasureInputRow("监测站标定高", stationCalibrationH, { stationCalibrationH = it }, "m", enabled = isFactoryEditable)
                    MeasureInputRow("标准面标定", standardSurfaceCalibration, { standardSurfaceCalibration = it }, "m", enabled = isFactoryEditable)
                    MeasureInputRow("壳体轴距标定", shellWheelbaseCalibration, { shellWheelbaseCalibration = it }, "m", enabled = isFactoryEditable)
                    MeasureInputRow("2号光源标定", light2Calibration, { light2Calibration = it }, "m", enabled = isFactoryEditable)

                    MeasureInputRow("设备等待时间", deviceWaitTime, { deviceWaitTime = it }, "秒")
                    MeasureInputRow("采集次数", collectionCount, { collectionCount = it }, null)
                    MeasureInputRow("杂项去除数", miscRemovalCount, { val v = it.toIntOrNull() ?: 0; if (v < (collectionCount.toIntOrNull() ?: 1)) miscRemovalCount = it }, null)
                    MeasureInputRow("设置光斑大小",lightSpotSize , {
                        if (it.toIntOrNull() !in 1..100) {
                            Toast.makeText(context, "光斑大小1-100", Toast.LENGTH_SHORT).show()
                        } else {
                            lightSpotSize = it
                        } }, "px")
                    MeasureDropdownRow(
                        label = "设置光斑颜色",
                        selectedValue = lightSpotColor,
                        options = listOf("RED", "GREEN", "BLUE", "BLACK"),
                        onValueChange = { lightSpotColor = it },
                        displayMapper = {
                            when (it) {
                                "RED" -> "红色"
                                "GREEN" -> "绿色"
                                "BLUE" -> "蓝色"
                                "BLACK" -> "黑色"
                                else -> it
                            }
                        }
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isFactoryEditable) {
                            Button(onClick = { showPasswordDialog = true }, modifier = Modifier.weight(1f)) {
                                Text("修改出厂标定")
                            }
                        } else {
                            Button(onClick = { isFactoryEditable = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                Text("退出修改")
                            }
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        systemConfigDao.saveConfig(SystemConfig(
                                            rangeCalibration = rangeCalibration,
                                            stationCalibrationH = stationCalibrationH,
                                            standardSurfaceCalibration = standardSurfaceCalibration,
                                            shellWheelbaseCalibration = shellWheelbaseCalibration,
                                            light2Calibration = light2Calibration,
                                            deviceWaitTime = deviceWaitTime,
                                            collectionCount = collectionCount,
                                            miscRemovalCount = miscRemovalCount,
                                            lightSpotSize = lightSpotSize,
                                            lightSpotColor = lightSpotColor
                                        ))
                                    }
                                    Toast.makeText(context, "内部参数已保存", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存内部参数")
                        }
                    }

                    if (isFactoryEditable) {
                        Text("已开启出厂编辑模式", color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (isFactoryEditable) {
            Button(
                onClick = { isFactoryEditable = false },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("返回")
            }
        }

        BottomActionRow(onBack, {
            if (equipmentHeight.isBlank() || selectedPointId == null || floorNumber.isBlank() || pointNumber.isBlank()) {
                Toast.makeText(context, "请填完必填项", Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    val currentTime = System.currentTimeMillis()
                    val tenMinutesAgo = currentTime - 10 * 60 * 1000
                    withContext(Dispatchers.IO) {
                        measurementResultDao.updateExpiredStates(MeasureState.MEASURING, MeasureState.TIMEOUT, tenMinutesAgo)
                    }

                    val hasActive = withContext(Dispatchers.IO) {
                        measurementResultDao.hasActiveTasks(MeasureState.MEASURING, tenMinutesAgo)
                    }
                    if (hasActive) {
                        Toast.makeText(context, "存在测量中任务", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val stateResp = device.getCurrentState()
                    if (stateResp.status != "success" ) {
                        Toast.makeText(context, "请求失败：${stateResp.message ?: "未知"}", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    if (stateResp.data?.isMeasuring != 0) {
                        Toast.makeText(context, "开始测量失败，设备已处于测量中状态", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val measureId = currentTime
                    // 开启测量时，将当前会话 ID 与此任务绑定
                    currentSessionMeasureId = measureId

                    val currentPointToSave = pointNumber
                    val currentFloorToSave = floorNumber

                    // 封装内部参数为 JSON
                    val internalParamsJson = Gson().toJson(SystemConfig(
                        rangeCalibration = rangeCalibration,
                        stationCalibrationH = stationCalibrationH,
                        standardSurfaceCalibration = standardSurfaceCalibration,
                        shellWheelbaseCalibration = shellWheelbaseCalibration,
                        light2Calibration = light2Calibration,
                        deviceWaitTime = deviceWaitTime,
                        collectionCount = collectionCount,
                        miscRemovalCount = miscRemovalCount,
                        lightSpotSize = lightSpotSize,
                        lightSpotColor = lightSpotColor
                    ))

                    val localId = withContext(Dispatchers.IO) {
                        measurementResultDao.insert(MeasurementResult(
                            measureId = measureId,
                            state = MeasureState.MEASURING,
                            deviceInstallationHeight = equipmentHeight,
                            controlPointId = selectedPointId!!,
                            monitoringStationInstallationHeight = stationHeight,
                            floorNumber = currentFloorToSave,
                            pointNumber = currentPointToSave,
                            centerPointPairs = centerPointPairs,
                            internalParameters = internalParamsJson
                        ))
                    }

                    // 测量高程测距
                    val distanceResp = device.distanceMeasured(
                        measureId = measureId,
                    )
                    if (distanceResp.success) {
                        distanceResult = distanceResp.distance_m
                        
                        val heights = calculateHeights(
                            equipmentHeight,
                            stationHeight,
                            selectedPointH,
                            rangeCalibration,
                            stationCalibrationH,
                            standardSurfaceCalibration,
                            shellWheelbaseCalibration,
                            light2Calibration,
                            distanceResult
                        )
                        val heightResultStr = "靶面:%.3f, 地面:%.3f, 墙面:%.3f".format(heights.targetH, heights.groundH, heights.wallH)
                        withContext(Dispatchers.IO) {
                            measurementResultDao.updateHeightResult(localId, heightResultStr)
                        }
                    } else {
                        withContext(Dispatchers.IO) { measurementResultDao.updateState(localId, MeasureState.FAILED) }
                        Toast.makeText(context, "测距失败", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val response = device.measure(
                        times = measureCountInput.toIntOrNull() ?: 1,
                        centralPointCount = centerPointPairs.toIntOrNull() ?: 1,
                        measureId = measureId,
                        deviceWaitTime = deviceWaitTime.toIntOrNull() ?: 3,
                        collectionCount = collectionCount.toIntOrNull() ?: 1,
                        miscRemovalCount = miscRemovalCount.toIntOrNull() ?: 0
                    )

                    if (response.status != "success") {
                        withContext(Dispatchers.IO) { measurementResultDao.updateState(localId, MeasureState.FAILED) }
                        Toast.makeText(context, "测量坐标失败: ${response.message}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "测量已启动", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }, measureCountInput, { measureCountInput = it })
    }
}

@Composable
fun MeasureInputRow(label: String, value: String, onValueChange: (String) -> Unit, unit: String? = null, enabled: Boolean = true, kbType: KeyboardType = KeyboardType.Number) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:", Modifier.width(100.dp), fontSize = 14.sp)
        OutlinedTextField(value, onValueChange, Modifier.weight(1f), enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = kbType), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 14.sp))
        unit?.let { Text(it, Modifier.width(40.dp), fontSize = 14.sp) } ?: Spacer(Modifier.width(40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureDropdownRow(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    displayMapper: (String) -> String = { it },
    showAsterisk: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val labelText = if (showAsterisk) "$label*:" else "$label:"
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(labelText, Modifier.width(100.dp), fontSize = 14.sp)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = displayMapper(selectedValue),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayMapper(option)) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
fun BottomActionRow(onBack: () -> Unit, onMeasure: () -> Unit, count: String, onCountChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onBack, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("返回", fontSize = 12.sp, maxLines = 1) }
        Button(onMeasure, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("测量", fontSize = 12.sp, maxLines = 1) }
        OutlinedTextField(count, onCountChange, Modifier.weight(0.7f), label = { Text("次数", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
    }
}

@Composable
fun SettingsInputRow(label: String, value: String, onValueChange: (String) -> Unit, onClick: () -> Unit, kbType: KeyboardType = KeyboardType.Number) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:", Modifier.width(100.dp), fontSize = 14.sp)
        OutlinedTextField(value, onValueChange, Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = kbType), singleLine = true)
        Button(onClick, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("设置", fontSize = 12.sp) }
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier) = Text(text, modifier, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
@Composable
fun TableCell(text: String, modifier: Modifier) = Text(text, modifier, textAlign = TextAlign.Center, fontSize = 14.sp)

@Composable
fun ControlPointSelectionDialog(points: List<ControlPoint>, onDismiss: () -> Unit, onSelect: (ControlPoint) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择控制点") }, text = {
        Box(Modifier.heightIn(max = 400.dp)) {
            LazyColumn { items(points) { p ->
                Column(Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(8.dp)) {
                    Text(p.name, fontWeight = FontWeight.Bold)
                    Text("x:${p.x} y:${p.y} h:${p.h}", fontSize = 12.sp, color = Color.Gray)
                    HorizontalDivider()
                }
            }}
        }
    }, confirmButton = { TextButton(onDismiss) { Text("取消") } })
}

@Composable
fun FloorSettingsDialog(isAsc: Boolean, inv: String, onDismiss: () -> Unit, onConfirm: (Boolean, String) -> Unit) {
    var asc by remember { mutableStateOf(isAsc) }; var i by remember { mutableStateOf(inv) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("楼层号设置") }, text = {
        Column {
            Row { RadioButton(asc, { asc = true }); Text("正序", Modifier.align(Alignment.CenterVertically)); Spacer(Modifier.width(8.dp)); RadioButton(!asc, { asc = false }); Text("倒序", Modifier.align(Alignment.CenterVertically)) }
            OutlinedTextField(i, { i = it }, label = { Text("间隔") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }, confirmButton = { Button({ onConfirm(asc, i) }) { Text("确定") } })
}

@Composable
fun PointSettingsDialog(isAsc: Boolean, inv: String, perF: String, onDismiss: () -> Unit, onConfirm: (Boolean, String, String) -> Unit) {
    var asc by remember { mutableStateOf(isAsc) }; var i by remember { mutableStateOf(inv) }; var p by remember { mutableStateOf(perF) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("点号设置") }, text = {
        Column {
            Row { RadioButton(asc, { asc = true }); Text("正序", Modifier.align(Alignment.CenterVertically)); Spacer(Modifier.width(8.dp)); RadioButton(!asc, { asc = false }); Text("倒序", Modifier.align(Alignment.CenterVertically)) }
            OutlinedTextField(i, { i = it }, label = { Text("间隔") }); OutlinedTextField(p, { p = it }, label = { Text("每层点数") })
        }
    }, confirmButton = { Button({ onConfirm(asc, i, p) }) { Text("确定") } })
}

fun calculateEquipmentHeightOriginal(shellWheelbaseCalibration: Double, equipmentHeight: Double): Double {
    // 小于壳体轴标定，则直接返回
    if (abs(equipmentHeight) <= shellWheelbaseCalibration) {
        return equipmentHeight
    }

    return if (equipmentHeight >= 0) {
        sqrt(equipmentHeight * equipmentHeight - shellWheelbaseCalibration * shellWheelbaseCalibration)
    } else {
        - sqrt(equipmentHeight * equipmentHeight - shellWheelbaseCalibration * shellWheelbaseCalibration)
    }

}

data class HeightResultValues(
    val targetH: Double,
    val groundH: Double,
    val wallH: Double
)

fun calculateHeights(
    equipmentHeight: String,
    stationHeight: String,
    selectedPointH: Double,
    rangeCalibration: String,
    stationCalibrationH: String,
    standardSurfaceCalibration: String,
    shellWheelbaseCalibration: String,
    light2Calibration: String,
    distanceResult: Double
): HeightResultValues {
    val eqH = equipmentHeight.toDoubleOrNull() ?: 0.0
    val swC = shellWheelbaseCalibration.toDoubleOrNull() ?: 0.0
    val eqHReal = calculateEquipmentHeightOriginal(swC, eqH)
    val stH = stationHeight.toDoubleOrNull() ?: 0.0
    val rCal = rangeCalibration.toDoubleOrNull() ?: 0.0
    val sCalH = stationCalibrationH.toDoubleOrNull() ?: 0.0
    val stdSurfaceCalibration = standardSurfaceCalibration.toDoubleOrNull() ?: 0.0
    val l2Cal = light2Calibration.toDoubleOrNull() ?: 0.0

    val targetH = eqHReal + selectedPointH + sCalH + distanceResult + rCal
    val groundH = targetH - stH - sCalH - stdSurfaceCalibration
    val wallH = targetH + l2Cal - sCalH - stdSurfaceCalibration

    return HeightResultValues(targetH, groundH, wallH)
}
