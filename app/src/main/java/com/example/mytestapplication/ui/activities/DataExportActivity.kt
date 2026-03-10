package com.example.mytestapplication.ui.activities

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.MeasurementResultWithControlPoint
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class DataExportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val measurementResultDao = database.measurementResultDao()

        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val results by measurementResultDao.getAllResultsWithControlPoint().collectAsState(initial = emptyList())
                    ExportScreen(
                        results = results,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExportScreen(results: List<MeasurementResultWithControlPoint>, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedIds = remember { mutableStateListOf<Long>() }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val fileSdf = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }

    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var folderPath by remember { mutableStateOf("") }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        selectedFolderUri = uri
        folderPath = uri?.path ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("数据导出", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedIds.size == results.size && results.isNotEmpty(),
                        onCheckedChange = {
                            if (selectedIds.size == results.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(results.map { it.measurementResult.id })
                            }
                        },
                        modifier = Modifier.weight(0.5f)
                    )
                    HeaderCell("时间", 2f)
                    HeaderCell("控制点", 1.5f)
                    HeaderCell("楼层号", 1f)
                    HeaderCell("点号", 1f)
                }
            }

            items(results, key = { it.measurementResult.id }) { item ->
                val isSelected = selectedIds.contains(item.measurementResult.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { 
                            if (isSelected) selectedIds.remove(item.measurementResult.id)
                            else selectedIds.add(item.measurementResult.id)
                         },
                        modifier = Modifier.weight(0.5f)
                    )
                    DataCell(sdf.format(Date(item.measurementResult.createTime)), 2f)
                    DataCell(item.controlPoint.name, 1.5f)
                    DataCell(item.measurementResult.floorNumber, 1f)
                    DataCell(item.measurementResult.pointNumber, 1f)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = folderPath,
                onValueChange = {},
                modifier = Modifier.weight(1f),
                label = { Text("选择导出目录") },
                readOnly = true
            )
            Button(onClick = { folderPickerLauncher.launch(null) }) {
                Text("浏览")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("返回")
            }
            
            // 结果导出按钮
            Button(
                onClick = { 
                    if (selectedIds.isEmpty()) {
                        Toast.makeText(context, "请选择要导出的数据", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        exportData(context, results.filter { selectedIds.contains(it.measurementResult.id) }, fileSdf, isDetail = false)
                    }
                }, 
                modifier = Modifier.weight(1f)
            ) {
                Text("结果导出")
            }

            // 详情导出按钮
            Button(
                onClick = { 
                    if (selectedIds.isEmpty()) {
                        Toast.makeText(context, "请选择要导出的数据", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        exportData(context, results.filter { selectedIds.contains(it.measurementResult.id) }, fileSdf, isDetail = true)
                    }
                }, 
                modifier = Modifier.weight(1f)
            ) {
                Text("详情导出")
            }
        }
    }
}

private suspend fun exportData(
    context: android.content.Context,
    dataToExport: List<MeasurementResultWithControlPoint>,
    fileSdf: SimpleDateFormat,
    isDetail: Boolean
) {
    val prefix = if (isDetail) "Detail" else "Result"
    val fileName = "${prefix}_${fileSdf.format(Date())}.csv"
    
    val header = if (isDetail) {
        "光源站安装高,监测站安装高,控制点名称,控制点X,控制点Y,控制点H,楼层号,点号,中心点对数,原始数据,计算过程,计算结果,创建时间\n"
    } else {
        "光源站安装高,监测站安装高,控制点名称,控制点X,控制点Y,控制点H,楼层号,点号,中心点对数,计算结果,创建时间\n"
    }
    
    val content = StringBuilder(header)
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    dataToExport.forEach { item ->
        content.append("${item.measurementResult.deviceInstallationHeight},")
        content.append("${item.measurementResult.monitoringStationInstallationHeight},")
        content.append("${item.controlPoint.name},")
        content.append("${item.controlPoint.x},${item.controlPoint.y},${item.controlPoint.h},")
        content.append("${item.measurementResult.floorNumber},")
        content.append("${item.measurementResult.pointNumber},")
        content.append("${item.measurementResult.centerPointPairs},")
        
        if (isDetail) {
            content.append("\"${item.measurementResult.rawData}\",")
            content.append("\"${item.measurementResult.processDetail}\",") // JSON
            content.append("\"${item.measurementResult.result}\",") // JSON
        } else {
            content.append("\"${item.measurementResult.result}\",") // 仅结果
        }

        content.append("${sdf.format(Date(item.measurementResult.createTime))}\n")
    }

    withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(content.toString())
                        }
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "成功导出到 'Documents'", Toast.LENGTH_LONG).show() }
                } ?: throw Exception("Failed to create MediaStore entry.")
            } else {
                 withContext(Dispatchers.Main) { Toast.makeText(context, "旧版安卓系统导出逻辑尚未完善", Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
}


@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(8.dp),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RowScope.DataCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(8.dp),
        textAlign = TextAlign.Center,
        fontSize = 14.sp
    )
}
