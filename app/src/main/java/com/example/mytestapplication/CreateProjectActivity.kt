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

class CreateProjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                CreateProjectScreen()
            }
        }
    }
}

@Composable
fun CreateProjectScreen() {
    var projectId by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "新建项目",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = projectId,
            onValueChange = { projectId = it },
            label = { Text("序号") },
            modifier = Modifier.fillMaxWidth()
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

        Button(onClick = { /* TODO: Handle project creation */ }, modifier = Modifier.fillMaxWidth()) {
            Text("创建")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateProjectScreenPreview() {
    CreateProjectScreen()
}
