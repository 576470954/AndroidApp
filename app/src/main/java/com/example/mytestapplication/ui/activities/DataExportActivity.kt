package com.example.mytestapplication.ui.activities

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.ui.theme.MytestApplicationTheme

class DataExportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExportScreen(
                        onBack = { finish() },
                        onConfirm = { /* 确认导出逻辑暂不实现 */ }
                    )
                }
            }
        }
    }
}

@Composable
fun ExportScreen(onBack: () -> Unit, onConfirm: (Uri?) -> Unit) {
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var folderPath by remember { mutableStateOf("") }

    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        selectedFolderUri = uri
        folderPath = uri?.path ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 标题
        Text(
            text = "数据导出",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 48.dp),
            textAlign = TextAlign.Center
        )

        // 2. 文件夹选择框
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
                readOnly = true,
                placeholder = { Text("未选择目录") }
            )
            Button(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.height(56.dp)
            ) {
                Text("浏览")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. 底部按钮
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
                onClick = { onConfirm(selectedFolderUri) },
                modifier = Modifier.weight(1f)
            ) {
                Text("确定")
            }
        }
    }
}
