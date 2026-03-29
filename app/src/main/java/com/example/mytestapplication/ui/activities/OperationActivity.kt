package com.example.mytestapplication.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.database.MeasurementResultDao
import com.example.mytestapplication.data.database.ProjectDao
import com.example.mytestapplication.data.model.MeasureState
import com.example.mytestapplication.data.model.Project
import com.example.mytestapplication.network.Device
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OperationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent.getLongExtra("PROJECT_ID", -1L)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "未知项目"
        val database = AppDatabase.getDatabase(this)
        val projectDao = database.projectDao()
        val measurementResultDao = database.measurementResultDao()

        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OperationScreen(
                        projectId = projectId,
                        projectName = projectName,
                        projectDao = projectDao,
                        measurementResultDao = measurementResultDao,
                        onBackToList = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun OperationScreen(
    projectId: Long,
    projectName: String,
    projectDao: ProjectDao,
    measurementResultDao: MeasurementResultDao,
    onBackToList: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var project by remember { mutableStateOf<Project?>(null) }
    var showDeviceSettings by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (projectId != -1L) {
            val allProjects = withContext(Dispatchers.IO) { projectDao.getAllProjects().first() }
            project = allProjects.find { it.id == projectId }
        }
    }

    if (showDeviceSettings && project != null) {
        DeviceSettingsDialog(
            currentUrl = project?.deviceUrl ?: "http://192.168.1.149:8080",
            onSave = { newUrl ->
                scope.launch {
                    project?.let {
                        val updatedProject = it.copy(deviceUrl = newUrl)
                        withContext(Dispatchers.IO) { projectDao.updateProject(updatedProject) }
                        project = updatedProject
                        Toast.makeText(context, "设备地址已保存", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onGetState = { url ->
                scope.launch {
                    val device = Device(url)
                    val resp = device.getCurrentState()
                    val message = if (resp.status != "success") {
                        "请求失败：${resp.message ?: "未知"}"
                    } else if (resp.data?.isMeasuring != 0) {
                        "设备测量中"
                    } else {
                        "设备状态正常，未开启测量"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onCancelMeasure = { url ->
                scope.launch {
                    val device = Device(url)
                    val activeResults = withContext(Dispatchers.IO) {
                        measurementResultDao.getAllResults().first().find { it.state == MeasureState.MEASURING }
                    }
                    if (activeResults != null) {
                        val resp = device.cancelMeasure(activeResults.measureId)
                        if (resp.status == "success") {
                            withContext(Dispatchers.IO) {
                                measurementResultDao.updateState(activeResults.id, MeasureState.FAILED)
                            }
                            Toast.makeText(context, "已取消测量", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "取消失败: ${resp.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "当前没有测量中的任务", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showDeviceSettings = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = projectName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    context.startActivity(Intent(context, ControlPointActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("控制点设置", fontSize = 18.sp)
            }

            Button(
                onClick = onBackToList,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("项目列表", fontSize = 18.sp)
            }

            Button(
                onClick = { showDeviceSettings = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("设备", fontSize = 18.sp)
            }

            Button(
                onClick = {
                    val intent = Intent(context, MeasureActivity::class.java).apply {
                        putExtra("PROJECT_ID", projectId)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("高层放样", fontSize = 18.sp)
            }

            Button(
                onClick = {
                    context.startActivity(Intent(context, DataExportActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("数据导出", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun DeviceSettingsDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onGetState: (String) -> Unit,
    onCancelMeasure: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设备控制") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("设备地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { onGetState(url) }, modifier = Modifier.fillMaxWidth()) { Text("获取设备状态") }
                Button(onClick = { onCancelMeasure(url) }, modifier = Modifier.fillMaxWidth()) { Text("取消当前测量") }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(url)
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
