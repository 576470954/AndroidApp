package com.example.mytestapplication.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.launch

class CreateControlPointActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val controlPointDao = database.controlPointDao()

        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CreateControlPointScreen(
                        onSave = { name, desc, x, y, h, remark ->
                            lifecycleScope.launch {
                                val cp = ControlPoint(
                                    name = name,
                                    description = desc,
                                    x = x,
                                    y = y,
                                    h = h,
                                    remark = remark
                                )
                                controlPointDao.insertControlPoint(cp)
                                finish()
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
fun CreateControlPointScreen(
    onSave: (String, String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var x by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    var h by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "新建控制点", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("点名") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("x") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("y") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("h") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("返回")
            }
            Button(onClick = { onSave(name, description, x, y, h, remark) }, modifier = Modifier.weight(1f)) {
                Text("保存")
            }
            Button(onClick = { onSave(name, description, x, y, h, remark) }, modifier = Modifier.weight(1f)) {
                Text("确定")
            }
        }
    }
}
