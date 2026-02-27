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
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.launch

class EditControlPointActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val controlPointDao = database.controlPointDao()

        val id = intent.getLongExtra("CP_ID", -1)
        val serialInit = intent.getStringExtra("CP_SERIAL") ?: ""
        val nameInit = intent.getStringExtra("CP_NAME") ?: ""
        val descInit = intent.getStringExtra("CP_DESC") ?: ""
        val xInit = intent.getStringExtra("CP_X") ?: ""
        val yInit = intent.getStringExtra("CP_Y") ?: ""
        val hInit = intent.getStringExtra("CP_H") ?: ""
        val remarkInit = intent.getStringExtra("CP_REMARK") ?: ""

        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditControlPointScreen(
                        initialSerial = serialInit,
                        initialName = nameInit,
                        initialDesc = descInit,
                        initialX = xInit,
                        initialY = yInit,
                        initialH = hInit,
                        initialRemark = remarkInit,
                        onSave = { serial, name, desc, x, y, h, remark ->
                            lifecycleScope.launch {
                                val cp = ControlPoint(
                                    id = id,
                                    serialNumber = serial,
                                    name = name,
                                    description = desc,
                                    x = x,
                                    y = y,
                                    h = h,
                                    remark = remark
                                )
                                controlPointDao.updateControlPoint(cp)
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
fun EditControlPointScreen(
    initialSerial: String,
    initialName: String,
    initialDesc: String,
    initialX: String,
    initialY: String,
    initialH: String,
    initialRemark: String,
    onSave: (String, String, String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var serialNumber by remember { mutableStateOf(initialSerial) }
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDesc) }
    var x by remember { mutableStateOf(initialX) }
    var y by remember { mutableStateOf(initialY) }
    var h by remember { mutableStateOf(initialH) }
    var remark by remember { mutableStateOf(initialRemark) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "编辑控制点", style = MaterialTheme.typography.headlineMedium)

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
                onClick = { 
                    if (serialNumber.isBlank()) {
                        Toast.makeText(context, "序号不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (name.isBlank()) {
                        Toast.makeText(context, "点名不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (x.isBlank()) {
                        Toast.makeText(context, "x不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (y.isBlank()) {
                        Toast.makeText(context, "y不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (h.isBlank()) {
                        Toast.makeText(context, "h不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(serialNumber, name, description, x, y, h, remark) 
                }, 
                modifier = Modifier.weight(1f)
            ) {
                Text("确定")
            }
        }
    }
}
