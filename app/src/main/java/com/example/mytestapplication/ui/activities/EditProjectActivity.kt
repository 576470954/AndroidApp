package com.example.mytestapplication.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.database.ProjectDao
import com.example.mytestapplication.data.model.Project
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
                        projectDao = projectDao,
                        initialId = id,
                        initialSerial = serial,
                        initialName = name,
                        initialAdmin = admin,
                        initialDesc = desc,
                        onSave = { updatedSerial, updatedName, updatedAdmin, updatedDesc ->
                            lifecycleScope.launch {
                                if (projectDao.isNameExistsExcludingId(updatedName, id)) {
                                    Toast.makeText(this@EditProjectActivity, "项目$updatedName 已存在", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updatedProject = Project(
                                        id = id,
                                        serialNumber = updatedSerial,
                                        name = updatedName,
                                        admin = updatedAdmin,
                                        description = updatedDesc,
                                        createdAt = createdAt,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    projectDao.updateProject(updatedProject)
                                    finish()
                                }
                            }
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun EditProjectScreen(
    projectDao: ProjectDao,
    initialId: Long,
    initialSerial: String,
    initialName: String,
    initialAdmin: String,
    initialDesc: String,
    onSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var serialNumber by remember { mutableStateOf(initialSerial) }
    var projectName by remember { mutableStateOf(initialName) }
    var adminName by remember { mutableStateOf(initialAdmin) }
    var projectDescription by remember { mutableStateOf(initialDesc) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "编辑项目", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("序号*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("名称*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("管理员") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = projectDescription, 
            onValueChange = { if (it.length <= 400) projectDescription = it }, 
            label = { Text("描述 (最多400字)") }, 
            modifier = Modifier.fillMaxWidth().weight(1f), 
            minLines = 10
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回")
            }
            Button(
                onClick = { 
                    if (serialNumber.isBlank()) {
                        Toast.makeText(context, "序号不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (projectName.isBlank()) {
                        Toast.makeText(context, "名称不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(serialNumber, projectName, adminName, projectDescription) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
        }
    }
}
