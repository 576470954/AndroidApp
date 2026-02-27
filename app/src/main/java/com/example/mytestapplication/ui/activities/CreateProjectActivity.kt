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

class CreateProjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val projectDao = database.projectDao()

        setContent {
            MytestApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CreateProjectScreen(
                        projectDao = projectDao,
                        onSave = { serial, name, admin, desc ->
                            val project = Project(serialNumber = serial, name = name, admin = admin, description = desc)
                            lifecycleScope.launch {
                                if (projectDao.isNameExists(name)) {
                                    Toast.makeText(this@CreateProjectActivity, "项目$name 已存在", Toast.LENGTH_SHORT).show()
                                } else {
                                    projectDao.insertProject(project)
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
fun CreateProjectScreen(
    projectDao: ProjectDao, 
    onSave: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var serialNumber by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val allSerials = projectDao.getAllSerialNumbers()
        val nextSerial = allSerials.mapNotNull { it.toIntOrNull() }.maxOrNull()?.plus(1)?.toString() ?: "1"
        serialNumber = nextSerial

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        projectName = sdf.format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "新建项目", style = MaterialTheme.typography.headlineMedium)

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
                    if (serialNumber.isBlank() || projectName.isBlank()) {
                        Toast.makeText(context, "请填写必填项（序号、名称）", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(serialNumber, projectName, adminName, projectDescription) 
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("创建")
            }
        }
    }
}
