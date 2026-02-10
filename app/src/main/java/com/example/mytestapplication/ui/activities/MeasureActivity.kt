package com.example.mytestapplication.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.database.MeasurementResultDao
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.data.model.MeasurementResult
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import com.example.mytestapplication.ui.common.VerticalDivider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var equipmentHeight by remember { mutableStateOf("") }
    var stationHeight by remember { mutableStateOf("0") }
    var floorNumber by remember { mutableStateOf("1") }
    var pointNumber by remember { mutableStateOf("C1") }
    var centerPointPairs by remember { mutableStateOf("4") }
    var measureCount by remember { mutableStateOf("1") }

    // 记录当前楼层已测量的点数进度
    var currentFloorProgress by remember { mutableIntStateOf(0) }

    // Control Point Selection
    var selectedPointId by remember { mutableStateOf<Long?>(null) }
    var selectedPointName by remember { mutableStateOf("未选择") }
    var xValue by remember { mutableStateOf("0.000") }
    var yValue by remember { mutableStateOf("0.000") }
    var hValue by remember { mutableStateOf("0.000") }
    var showPointSelector by remember { mutableStateOf(false) }

    // Floor Settings
    var floorOrderAsc by remember { mutableStateOf(true) }
    var floorInterval by remember { mutableStateOf("1") }
    var showFloorSettingsDialog by remember { mutableStateOf(false) }

    // Point Settings
    var pointOrderAsc by remember { mutableStateOf(true) }
    var pointInterval by remember { mutableStateOf("1") }
    var pointsPerFloor by remember { mutableStateOf("4") }
    var showPointSettingsDialog by remember { mutableStateOf(false) }

    // Center Point Pairs Dropdown
    val centerPointOptions = listOf("4", "6", "8", "12", "16")
    var centerPointPairsExpanded by remember { mutableStateOf(false) }

    // --- DIALOGS ---
    if (showPointSelector) {
        ControlPointSelectionDialog(
            points = points,
            onDismiss = { showPointSelector = false },
            onPointSelected = { point ->
                selectedPointId = point.id
                selectedPointName = point.name
                xValue = point.x
                yValue = point.y
                hValue = point.h
                showPointSelector = false
            }
        )
    }

    if (showFloorSettingsDialog) {
        FloorSettingsDialog(
            isAscending = floorOrderAsc,
            interval = floorInterval,
            onDismiss = { showFloorSettingsDialog = false },
            onConfirm = { isAsc, newInterval ->
                floorOrderAsc = isAsc
                floorInterval = newInterval
                showFloorSettingsDialog = false
            }
        )
    }

    if (showPointSettingsDialog) {
        PointSettingsDialog(
            isAscending = pointOrderAsc,
            interval = pointInterval,
            pointsPerFloor = pointsPerFloor,
            onDismiss = { showPointSettingsDialog = false },
            onConfirm = { isAsc, newInterval, newPointsPerFloor ->
                pointOrderAsc = isAsc
                pointInterval = newInterval
                pointsPerFloor = newPointsPerFloor
                showPointSettingsDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "高层放样", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp))

        // XYZ Table
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell("x", Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableHeaderCell("y", Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableHeaderCell("h", Modifier.weight(1f))
            }
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableCell(xValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(yValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(hValue, Modifier.weight(1f))
            }
        }

        MeasureInputRow(label = "设备安装高*", value = equipmentHeight, onValueChange = { equipmentHeight = it }, unit = "mm")

        // Control Point Row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "控制点*:", modifier = Modifier.width(100.dp), fontSize = 16.sp)
            Text(text = selectedPointName, modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline).clickable { showPointSelector = true }.padding(12.dp), fontSize = 16.sp)
            Button(
                onClick = { showPointSelector = true },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { 
                Text("选择", fontSize = 12.sp, maxLines = 1, softWrap = false) 
            }
        }

        MeasureInputRow(label = "监测站安装高*", value = stationHeight, onValueChange = { stationHeight = it }, unit = "mm")

        // Floor Number Row with Settings
        SettingsInputRow(label = "楼层号*", value = floorNumber, onValueChange = { floorNumber = it }, onSettingsClick = { showFloorSettingsDialog = true })

        // Point Number Row with Settings
        SettingsInputRow(label = "点号*", value = pointNumber, onValueChange = { pointNumber = it }, keyboardType = KeyboardType.Text, onSettingsClick = { showPointSettingsDialog = true })

        // Center Point Pairs Dropdown
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "中心点对数*:", modifier = Modifier.width(100.dp), fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = centerPointPairsExpanded,
                onExpandedChange = { centerPointPairsExpanded = !centerPointPairsExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = centerPointPairs,
                    onValueChange = {}, 
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = centerPointPairsExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = centerPointPairsExpanded, onDismissRequest = { centerPointPairsExpanded = false }) {
                    centerPointOptions.forEach {
                        DropdownMenuItem(
                            text = { Text(it) }, 
                            onClick = { centerPointPairs = it; centerPointPairsExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(68.dp)) // Spacer to align with other rows
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // Bottom Action Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack, 
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { 
                Text("返回", fontSize = 13.sp, maxLines = 1, softWrap = false) 
            }
            Button(
                onClick = { 
                    // 所有字段校验
                    if (equipmentHeight.isBlank()) {
                        Toast.makeText(context, "设备安装高不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedPointId == null) {
                        Toast.makeText(context, "请先选择控制点", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (stationHeight.isBlank()) {
                        Toast.makeText(context, "监测站安装高不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (floorNumber.isBlank()) {
                        Toast.makeText(context, "楼层号不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pointNumber.isBlank()) {
                        Toast.makeText(context, "点号不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (centerPointPairs.isBlank()) {
                        Toast.makeText(context, "中心点对数不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 模拟测量逻辑
                    Toast.makeText(context, "正在测量 $pointNumber...", Toast.LENGTH_SHORT).show()
                    
                    scope.launch(Dispatchers.IO) {
                        val count = measureCount.toIntOrNull() ?: 1
                        repeat(count) {
                            val randomRawData = List(8) { Random.nextDouble(0.0, 100.0).format(3) }.joinToString(",")
                            val randomCoords = "${Random.nextDouble(0.0, 1000.0).format(3)},${Random.nextDouble(0.0, 1000.0).format(3)}"
                            
                            val result = MeasurementResult(
                                deviceInstallationHeight = equipmentHeight,
                                controlPointId = selectedPointId!!,
                                monitoringStationInstallationHeight = stationHeight,
                                floorNumber = floorNumber,
                                pointNumber = pointNumber,
                                centerPointPairs = centerPointPairs,
                                rawData = randomRawData,
                                centerPointCoordinates = randomCoords
                            )
                            measurementResultDao.insert(result)
                        }
                    }

                    val pInterval = pointInterval.toIntOrNull() ?: 1
                    val fInterval = floorInterval.toIntOrNull() ?: 1
                    val pMax = pointsPerFloor.toIntOrNull() ?: 4
                    
                    currentFloorProgress++
                    
                    if (currentFloorProgress >= pMax) {
                        // 达到每层点数，更新楼层并重置点号
                        currentFloorProgress = 0
                        
                        val currentF = floorNumber.toIntOrNull() ?: 1
                        val nextF = if (floorOrderAsc) currentF + fInterval else currentF - fInterval
                        floorNumber = nextF.toString()
                        
                        pointNumber = "C1" // 重置为默认点号
                    } else {
                        // 点号后缀变化
                        val prefix = pointNumber.takeWhile { !it.isDigit() }
                        val suffixStr = pointNumber.dropWhile { !it.isDigit() }
                        val suffix = suffixStr.toIntOrNull() ?: 1
                        val nextSuffix = if (pointOrderAsc) suffix + pInterval else suffix - pInterval
                        pointNumber = if (prefix.isEmpty()) nextSuffix.toString() else "$prefix$nextSuffix"
                    }
                }, 
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) { 
                Text("测量", fontSize = 13.sp, maxLines = 1, softWrap = false) 
            }
            OutlinedTextField(
                value = measureCount, 
                onValueChange = { measureCount = it }, 
                label = { Text("次数", fontSize = 11.sp) }, 
                modifier = Modifier.weight(0.7f), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )
        }
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
fun FloorSettingsDialog(
    isAscending: Boolean,
    interval: String,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String) -> Unit
) {
    var isAsc by remember { mutableStateOf(isAscending) }
    var currentInterval by remember { mutableStateOf(interval) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("楼层号设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("排序方式", fontWeight = FontWeight.Bold)
                Row {
                    Row(Modifier.selectable(selected = isAsc, onClick = { isAsc = true })) {
                        RadioButton(selected = isAsc, onClick = { isAsc = true })
                        Text("正序", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Spacer(Modifier.width(16.dp))
                    Row(Modifier.selectable(selected = !isAsc, onClick = { isAsc = false })) {
                        RadioButton(selected = !isAsc, onClick = { isAsc = false })
                        Text("倒序", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                OutlinedTextField(
                    value = currentInterval,
                    onValueChange = { value ->
                        if (value.isEmpty() || (value.toIntOrNull() in 1..100)) {
                            currentInterval = value
                        }
                    },
                    label = { Text("间隔层数 (1-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(isAsc, currentInterval) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun PointSettingsDialog(
    isAscending: Boolean,
    interval: String,
    pointsPerFloor: String,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, String, String) -> Unit
) {
    var isAsc by remember { mutableStateOf(isAscending) }
    var currentInterval by remember { mutableStateOf(interval) }
    var currentPointsPerFloor by remember { mutableStateOf(pointsPerFloor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("点号设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Order
                Text("排序方式", fontWeight = FontWeight.Bold)
                Row {
                    Row(Modifier.selectable(selected = isAsc, onClick = { isAsc = true })) {
                        RadioButton(selected = isAsc, onClick = { isAsc = true })
                        Text("正序", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Spacer(Modifier.width(16.dp))
                    Row(Modifier.selectable(selected = !isAsc, onClick = { isAsc = false })) {
                        RadioButton(selected = !isAsc, onClick = { isAsc = false })
                        Text("倒序", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                // Interval
                OutlinedTextField(
                    value = currentInterval,
                    onValueChange = { value ->
                        if (value.isEmpty() || (value.toIntOrNull() in 1..10)) {
                            currentInterval = value
                        }
                    },
                    label = { Text("间隔数 (1-10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                // Points per floor
                OutlinedTextField(
                    value = currentPointsPerFloor,
                    onValueChange = { currentPointsPerFloor = it },
                    label = { Text("每层几个点") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(isAsc, currentInterval, currentPointsPerFloor) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ControlPointSelectionDialog(
    points: List<ControlPoint>,
    onDismiss: () -> Unit,
    onPointSelected: (ControlPoint) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择控制点") },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    items(points) { point ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPointSelected(point) }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(text = point.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(text = "x:${point.x}  y:${point.y}  h:${point.h}", fontSize = 12.sp, color = Color.Gray)
                            Divider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun SettingsInputRow(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    onSettingsClick: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "$label:", modifier = Modifier.width(100.dp), fontSize = 16.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
        Button(
            onClick = onSettingsClick, 
            modifier = Modifier.height(56.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("设置", fontSize = 12.sp, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun MeasureInputRow(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    unit: String? = null,
    keyboardType: KeyboardType = KeyboardType.Number
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "$label:", modifier = Modifier.width(100.dp), fontSize = 16.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
        if (unit != null) {
            Text(text = unit, modifier = Modifier.width(68.dp), fontSize = 16.sp, textAlign = TextAlign.Center)
        } else {
            Spacer(modifier = Modifier.width(68.dp))
        }
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 16.sp)
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, textAlign = TextAlign.Center, fontSize = 14.sp)
}
