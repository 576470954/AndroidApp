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
import androidx.compose.foundation.selection.selectable
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
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.data.model.MeasurementResult
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import com.example.mytestapplication.ui.common.VerticalDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.tan
import kotlin.random.Random

class MeasureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val controlPointDao = database.controlPointDao()
        val measurementResultDao = database.measurementResultDao()

        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val points by controlPointDao.getAllControlPoints().collectAsState(initial = emptyList())
                    MeasureScreen(
                        points = points, 
                        onBack = { finish() },
                        measurementResultDao = measurementResultDao
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureScreen(
    points: List<ControlPoint>, 
    onBack: () -> Unit,
    measurementResultDao: MeasurementResultDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 基本设置状态
    var equipmentHeight by remember { mutableStateOf("75") } // 默认值设为 75
    var stationHeight by remember { mutableStateOf("0") }
    var floorNumber by remember { mutableStateOf("1") }
    var pointNumber by remember { mutableStateOf("C1") }
    var centerPointPairs by remember { mutableStateOf("1") }
    var measureCountInput by remember { mutableStateOf("1") }

    // 内部参数状态
    var isInternalExpanded by remember { mutableStateOf(false) }
    var rangeCalibration by remember { mutableStateOf("0") }
    var stationCalibrationH by remember { mutableStateOf("0") }
    var shellWheelbaseCalibration by remember { mutableStateOf("75") }
    var light2Calibration by remember { mutableStateOf("0") }
    var deviceWaitTime by remember { mutableStateOf("3") }
    var collectionCount by remember { mutableStateOf("1") }
    var miscRemovalCount by remember { mutableStateOf("0") }
    
    var isFactoryEditable by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // 第一行展示的实时坐标
    var xValue by remember { mutableStateOf("0.000") }
    var yValue by remember { mutableStateOf("0.000") }
    var hLabel by remember { mutableStateOf("H(靶面)") }
    var hMenuExpanded by remember { mutableStateOf(false) }
    
    // 控制点选择状态
    var selectedPointId by remember { mutableStateOf<Long?>(null) }
    var selectedPointName by remember { mutableStateOf("未选择") }
    var selectedPointH by remember { mutableStateOf(0.0) } // 控制点 H (作为测距数)
    var showPointSelector by remember { mutableStateOf(false) }

    // 根据公式计算显示的 H 值
    val hValueDisplay = remember(hLabel, equipmentHeight, stationHeight, selectedPointH, rangeCalibration, stationCalibrationH, shellWheelbaseCalibration, light2Calibration) {
        val eqH = equipmentHeight.toDoubleOrNull() ?: 0.0
        val swC = shellWheelbaseCalibration.toDoubleOrNull() ?: 0.0
        val eqHReal = calculateEquipmentHeightOriginal(swC, eqH)
        val stH = stationHeight.toDoubleOrNull() ?: 0.0
        val rCal = rangeCalibration.toDoubleOrNull() ?: 0.0
        val sCalH = stationCalibrationH.toDoubleOrNull() ?: 0.0
        val l2Cal = light2Calibration.toDoubleOrNull() ?: 0.0
        
        // 靶面高程 = 光源站安装高度 + 控制点 H (作为测距数) + 测距校准值 + 监测站标定高
        val targetH = eqHReal + selectedPointH + rCal + sCalH
        
        val result = when (hLabel) {
            "H(地面)" -> targetH - stH
            "H(墙面)" -> targetH + l2Cal
            else -> targetH // H(靶面)
        }
        "%.3f".format(result)
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

    val centerPointOptions = listOf("1", "4", "8", "16")
    var centerPointPairsExpanded by remember { mutableStateOf(false) }

    // --- 弹窗逻辑 ---
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
            Divider(color = MaterialTheme.colorScheme.outline)
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableCell(xValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(yValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(hValueDisplay, Modifier.weight(1f))
            }
        }

        // 光源站安装高修改校验
        MeasureInputRow("光源站安装高*", equipmentHeight, { newValue ->
            val newEqH = newValue.toDoubleOrNull()
            val currentL = shellWheelbaseCalibration.toDoubleOrNull() ?: 0.0
            if (newEqH != null && newEqH < currentL) {
                Toast.makeText(context, "光源站安装高不能小于壳体轴距标定值", Toast.LENGTH_SHORT).show()
            } else {
                equipmentHeight = newValue
            }
        }, "mm")

        MeasureInputRow("监测站安装高*", stationHeight, { stationHeight = it }, "mm")

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("控制点*:", Modifier.width(100.dp), fontSize = 16.sp)
            Text(selectedPointName, Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline).clickable { showPointSelector = true }.padding(12.dp))
            Button(onClick = { showPointSelector = true }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("选择", fontSize = 12.sp) }
        }

        SettingsInputRow("楼层号*", floorNumber, { floorNumber = it }, { showFloorSettingsDialog = true })
        SettingsInputRow("点号*", pointNumber, { pointNumber = it }, { showPointSettingsDialog = true }, KeyboardType.Text)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("中心点对数*:", Modifier.width(100.dp), fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = centerPointPairsExpanded,
                onExpandedChange = { centerPointPairsExpanded = !centerPointPairsExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(value = centerPointPairs, onValueChange = {}, readOnly = true, 
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = centerPointPairsExpanded) }, modifier = Modifier.menuAnchor())
                ExposedDropdownMenu(centerPointPairsExpanded, { centerPointPairsExpanded = false }) {
                    centerPointOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { centerPointPairs = it; centerPointPairsExpanded = false }) }
                }
            }
            Spacer(Modifier.width(68.dp))
        }

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
                    MeasureInputRow("测距校准值", rangeCalibration, { rangeCalibration = it }, "mm", enabled = isFactoryEditable)
                    MeasureInputRow("监测站标定高", stationCalibrationH, { stationCalibrationH = it }, "mm", enabled = isFactoryEditable)
                    
                    // 壳体轴距标定修改校验
                    MeasureInputRow("壳体轴距标定", shellWheelbaseCalibration, { newValue ->
                        val newL = newValue.toDoubleOrNull()
                        val currentEqH = equipmentHeight.toDoubleOrNull() ?: 0.0
                        if (newL != null && newL > currentEqH) {
                            Toast.makeText(context, "壳体轴距标定不能大于光源站安装高", Toast.LENGTH_SHORT).show()
                        } else {
                            shellWheelbaseCalibration = newValue
                        }
                    }, "mm", enabled = isFactoryEditable)

                    MeasureInputRow("2号光源标定", light2Calibration, { light2Calibration = it }, "mm", enabled = isFactoryEditable)
                    MeasureInputRow("设备等待时间", deviceWaitTime, { deviceWaitTime = it }, "秒")
                    MeasureInputRow("采集次数", collectionCount, { collectionCount = it }, null)
                    MeasureInputRow("杂项去除数", miscRemovalCount, { val v = it.toIntOrNull() ?: 0; if (v < (collectionCount.toIntOrNull() ?: 1)) miscRemovalCount = it }, null)
                    
                    if (!isFactoryEditable) {
                        Button(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("修改出厂标定")
                        }
                    } else {
                        Text("已开启出厂编辑模式", color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        BottomActionRow(onBack, {
            if (equipmentHeight.isBlank() || selectedPointId == null || floorNumber.isBlank() || pointNumber.isBlank()) {
                Toast.makeText(context, "请填完必填项", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "正在测量 $pointNumber...", Toast.LENGTH_SHORT).show()
                
                xValue = "%.3f".format(Random.nextDouble(0.0, 1000.0))
                yValue = "%.3f".format(Random.nextDouble(0.0, 1000.0))

                scope.launch(Dispatchers.IO) {
                    val count = measureCountInput.toIntOrNull() ?: 1
                    repeat(count) {
                        val result = MeasurementResult(
                            deviceInstallationHeight = equipmentHeight,
                            controlPointId = selectedPointId!!,
                            monitoringStationInstallationHeight = stationHeight,
                            floorNumber = floorNumber,
                            pointNumber = pointNumber,
                            centerPointPairs = centerPointPairs,
                            rawData = List(8) { "%.3f".format(Random.nextDouble(0.0, 100.0)) }.joinToString(","),
                            centerPointCoordinates = "$xValue,$yValue"
                        )
                        measurementResultDao.insert(result)
                    }
                }

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

@Composable
fun BottomActionRow(onBack: () -> Unit, onMeasure: () -> Unit, count: String, onCountChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onBack, Modifier.weight(1f)) { Text("返回") }
        Button(onMeasure, Modifier.weight(1f)) { Text("测量") }
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
                    Text("x:${p.x} y:${p.y} h:${p.h}", fontSize = 12.sp, color = Color.Gray); Divider()
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
    // 避免除以0或无效数值导致 asin 崩溃
    if (shellWheelbaseCalibration <= 0 || equipmentHeight <= shellWheelbaseCalibration) {
        return 0.0
    }
    
    val ratio = shellWheelbaseCalibration / equipmentHeight
    // asin 的参数必须在 [-1, 1] 之间
    if (ratio > 1.0) return 0.0
    
    val theta = asin(ratio)
    val tanTheta = tan(theta)
    
    // 避免 tanTheta 为 0
    if (tanTheta == 0.0) return 0.0
    
    return shellWheelbaseCalibration / tanTheta
}
