package cn.latlaser.coordinate.ui.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.latlaser.coordinate.data.database.AppDatabase
import cn.latlaser.coordinate.data.model.ControlPoint
import cn.latlaser.coordinate.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ControlPointImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "未知项目"
        val database = AppDatabase.getDatabase(this)
        val controlPointDao = database.controlPointDao()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    
                    ImportScreen(
                        projectName = projectName,
                        onBack = { finish() },
                        onConfirm = { uri ->
                            if (uri == null) {
                                Toast.makeText(context, "请先选择文件", Toast.LENGTH_SHORT).show()
                                return@ImportScreen
                            }
                            scope.launch {
                                try {
                                    val result = importControlPoints(uri, controlPointDao)
                                    if (result.success) {
                                        Toast.makeText(context, "导入成功，共${result.count}条数据", Toast.LENGTH_LONG).show()
                                        finish()
                                    } else {
                                        Toast.makeText(context, result.errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "解析失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun importControlPoints(
        uri: Uri, 
        dao: cn.latlaser.coordinate.data.database.ControlPointDao
    ): ImportResult = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri) ?: return@withContext ImportResult(false, 0, "无法打开文件")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val existingNames = dao.getAllPointNames().toMutableSet()
        val allSerials = dao.getAllSerialNumbers()
        var nextSerial = allSerials.mapNotNull { it.toIntOrNull() }.maxOrNull()?.plus(1) ?: 1
        
        val pointsToInsert = mutableListOf<ControlPoint>()
        val fileNamesSet = mutableSetOf<String>()
        
        var lineIndex = 0
        reader.useLines { lines ->
            lines.forEach { line ->
                lineIndex++
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) return@forEach
                
                // 跳过表头
                if (lineIndex == 1 && (trimmedLine.contains("点名") || trimmedLine.contains("备注"))) {
                    return@forEach
                }

                // 修改分割逻辑以支持空字段（描述和备注可为空）
                // 使用逗号作为主分隔符以支持空字段，同时也兼容其他分隔符
                val parts = if (trimmedLine.contains(",")) {
                    trimmedLine.split(",", "，", limit = 6).map { it.trim() }
                } else if (trimmedLine.contains("\t")) {
                    trimmedLine.split("\t", limit = 6).map { it.trim() }
                } else {
                    trimmedLine.split(Regex("\\s+"), limit = 6).map { it.trim() }
                }

                if (parts.size < 6) {
                    return@withContext ImportResult(false, 0, "第${lineIndex}行格式错误：必须包含 点名, 描述, X, Y, H, 备注")
                }

                val name = parts[0]
                val desc = parts[1]
                val x = parts[2]
                val y = parts[3]
                val h = parts[4]
                val remark = parts[5]

                // 必填项校验
                if (name.isEmpty() || x.isEmpty() || y.isEmpty() || h.isEmpty()) {
                    return@withContext ImportResult(false, 0, "第${lineIndex}行必填项缺失（点名、X、Y、H不能为空）")
                }

                // 重复性校验（数据库）
                if (existingNames.contains(name)) {
                    return@withContext ImportResult(false, 0, "第${lineIndex}行错误：点名「$name」在数据库中已存在")
                }

                // 重复性校验（文件内部）
                if (fileNamesSet.contains(name)) {
                    return@withContext ImportResult(false, 0, "第${lineIndex}行错误：点名「$name」在文件中重复")
                }

                fileNamesSet.add(name)
                pointsToInsert.add(ControlPoint(
                    serialNumber = (nextSerial++).toString(),
                    name = name,
                    description = desc,
                    x = x,
                    y = y,
                    h = h,
                    remark = remark
                ))
            }
        }

        if (pointsToInsert.isEmpty()) {
            return@withContext ImportResult(false, 0, "文件中没有有效数据")
        }

        dao.insertAll(pointsToInsert)
        return@withContext ImportResult(true, pointsToInsert.size, "")
    }

    data class ImportResult(val success: Boolean, val count: Int, val errorMsg: String)
}

@Composable
fun ImportScreen(projectName: String, onBack: () -> Unit, onConfirm: (Uri?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        fileName = uri?.lastPathSegment ?: ""
    }

    val templateCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val content = "点名, 描述, X, Y, H, 备注\n" +
                                "K1,,10.123,20.456,30.789,\n" +
                                "K2,描述示例2,11.123,21.456,31.789,备注示例2"
                        outputStream.write(content.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "模版下载成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "控制点导入-$projectName",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "支持格式：Csv, TXT, Dat",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "格式内容：点名, 描述, X, Y, H, 备注 (必须包含6列)",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { templateCreateLauncher.launch("control_point_template.txt") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("模版下载")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fileName,
                onValueChange = {},
                modifier = Modifier.weight(1f),
                label = { Text("选择文件") },
                readOnly = true,
                placeholder = { Text("未选择文件") }
            )
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.height(56.dp)
            ) {
                Text("浏览")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("返回")
            }
            Button(
                onClick = { onConfirm(selectedFileUri) },
                modifier = Modifier.weight(1f)
            ) {
                Text("确定")
            }
        }
    }
}
