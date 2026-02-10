package com.example.mytestapplication.ui.activities

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
import androidx.lifecycle.lifecycleScope
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.database.ControlPointDao
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.launch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


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
                        controlPointDao = controlPointDao,
                        onSave = { serial, name, desc, x, y, h, remark, isFinish ->
                            lifecycleScope.launch {
                                val cp = ControlPoint(
                                    serialNumber = serial,
                                    name = name,
                                    description = desc,
                                    x = x,
                                    y = y,
                                    h = h,
                                    remark = remark
                                )
                                controlPointDao.insertControlPoint(cp)
                                if (isFinish) {
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
fun CreateControlPointScreen(
    controlPointDao: ControlPointDao,
    onSave: (String, String, String, String, String, String, String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var serialNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var x by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    var h by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    LaunchedEffect(serialNumber) {
        if (serialNumber.isEmpty()) {
            val allSerials = controlPointDao.getAllSerialNumbers()
            val nextSerial = allSerials.mapNotNull { it.toIntOrNull() }.maxOrNull()?.plus(1)?.toString() ?: "1"
            serialNumber = nextSerial
            name = "K$nextSerial"
        }
    }

    fun validateAndSave(isFinish: Boolean) {
        if (serialNumber.isBlank() || name.isBlank() || x.isBlank() || y.isBlank() || h.isBlank()) {
            Toast.makeText(context, "请填写必填项（序号、点名、x、y、h）", Toast.LENGTH_SHORT).show()
            return
        }
        onSave(serialNumber, name, description, x, y, h, remark, isFinish)
        if (!isFinish) {
            name = ""
            description = ""
            x = ""
            y = ""
            h = ""
            remark = ""
            serialNumber = "" 
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "新建控制点", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = { Text("序号*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("点名*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("x*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("y*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = h, onValueChange = { h = it }, label = { Text("h*") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = remark, 
            onValueChange = { remark = it }, 
            label = { Text("备注") }, 
            modifier = Modifier.fillMaxWidth().height(150.dp),
            minLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("返回")
            }
            Button(
                onClick = { validateAndSave(false) }, 
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
            Button(
                onClick = { validateAndSave(true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("确定")
            }
        }
    }
}
