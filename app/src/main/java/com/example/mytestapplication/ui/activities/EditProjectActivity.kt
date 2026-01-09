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

class EditProjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val projectDao = database.projectDao()

        val id = intent.getLongExtra("PROJECT_ID", -1)
        val serial = intent.getStringExtra("PROJECT_SERIAL") ?: ""
        val name = intent.getStringExtra("PROJECT_NAME") ?: ""
        val admin = intent.getStringExtra("PROJECT_ADMIN") ?: ""
        val desc = intent.getStringExtra("PROJECT_DESCRIPTION") ?: ""
        val createdAt = intent.getLongExtra("PROJECT_CREATED_AT", System.currentTimeMillis())

        setContent {
            MytestApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EditProjectScreen(
                        initialSerial = serial,
                        initialName = name,
                        initialAdmin = admin,
                        initialDesc = desc,
                        onSave = { updatedSerial, updatedName, updatedAdmin, updatedDesc ->
                            val updatedProject = Project(
                                id = id,
                                serialNumber = updatedSerial,
                                name = updatedName,
                                admin = updatedAdmin,
                                description = updatedDesc,
                                createdAt = createdAt,
                                updatedAt = System.currentTimeMillis()
                            )
                            lifecycleScope.launch {
                                projectDao.updateProject(updatedProject)
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
fun EditProjectScreen(
    initialSerial: String,
    initialName: String,
    initialAdmin: String,
    initialDesc: String,
    onSave: (String, String, String, String) -> Unit
) {
    var serialNumber by remember { mutableStateOf(initialSerial) }
    var projectName by remember { mutableStateOf(initialName) }
    var adminName by remember { mutableStateOf(initialAdmin) }
    var projectDescription by remember { mutableStateOf(initialDesc) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "编辑项目", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("序号") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("管理员") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = projectDescription, onValueChange = { projectDescription = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Button(
            onClick = { onSave(serialNumber, projectName, adminName, projectDescription) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }
    }
}
