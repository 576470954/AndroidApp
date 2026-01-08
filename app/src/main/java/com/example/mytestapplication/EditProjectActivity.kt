package com.example.mytestapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class EditProjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从 Intent 中获取数据
        val projectId = intent.getIntExtra("PROJECT_ID", -1).toString()
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: ""
        val projectDescription = intent.getStringExtra("PROJECT_DESCRIPTION") ?: ""

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                EditProjectScreen(projectId, projectName, projectDescription)
            }
        }
    }
}

@Composable
fun EditProjectScreen(id: String, name: String, description: String) {
    var projectId by remember { mutableStateOf(id) }
    var projectName by remember { mutableStateOf(name) }
    var projectDescription by remember { mutableStateOf(description) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "编辑项目",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = projectId,
            onValueChange = { projectId = it },
            label = { Text("序号") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true // 序号通常是不可编辑的
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            label = { Text("项目名") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = projectDescription,
            onValueChange = { projectDescription = it },
            label = { Text("描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(onClick = { /* TODO: Handle project update */ }, modifier = Modifier.fillMaxWidth()) {
            Text("保存")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditProjectScreenPreview() {
    EditProjectScreen("1", "预览项目", "这是一个预览用的描述。")
}
