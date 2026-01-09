package com.example.mytestapplication.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.Project
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.launch

class CreateProjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val projectDao = database.projectDao()

        setContent {
            MytestApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CreateProjectScreen(
                        onSave = { serial, name, admin, desc ->
                            val project = Project(serialNumber = serial, name = name, admin = admin, description = desc)
                            lifecycleScope.launch {
                                projectDao.insertProject(project)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateProjectScreen(onSave: (String, String, String, String) -> Unit) {
    var serialNumber by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "新建项目", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("序号") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("管理员") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = projectDescription, onValueChange = { projectDescription = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Button(
            onClick = { onSave(serialNumber, projectName, adminName, projectDescription) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("创建")
        }
    }
}
