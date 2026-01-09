package com.example.mytestapplication.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import com.example.mytestapplication.ui.common.VerticalDivider

class MeasureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeasureScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun MeasureScreen(onBack: () -> Unit) {
    var equipmentHeight by remember { mutableStateOf("") }
    var stationHeight by remember { mutableStateOf("") }
    var floorNumber by remember { mutableStateOf("") }
    var pointNumber by remember { mutableStateOf("") }
    var centerPointPairs by remember { mutableStateOf("") }
    var measureResultInput by remember { mutableStateOf("") }
    
    var selectedPointName by remember { mutableStateOf("未选择") }
    var xValue by remember { mutableStateOf("0.000") }
    var yValue by remember { mutableStateOf("0.000") }
    var hValue by remember { mutableStateOf("0.000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "高层放样",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline)) {
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell("x", Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableHeaderCell("y", Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableHeaderCell("h", Modifier.weight(1f))
            }
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                TableCell(xValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(yValue, Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(hValue, Modifier.weight(1f))
            }
        }

        MeasureInputRow(label = "设备安装高", value = equipmentHeight, onValueChange = { equipmentHeight = it }, unit = "mm")

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "控制点:", modifier = Modifier.width(100.dp), fontSize = 16.sp)
            Text(
                text = selectedPointName,
                modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline).padding(8.dp),
                fontSize = 16.sp
            )
            Button(onClick = { 
                selectedPointName = "CP_Mock"
                xValue = "123.456"
                yValue = "789.012"
                hValue = "50.000"
            }) {
                Text("选择")
            }
        }

        MeasureInputRow(label = "检测站安装高", value = stationHeight, onValueChange = { stationHeight = it }, unit = "mm")
        MeasureInputRow(label = "楼层号", value = floorNumber, onValueChange = { floorNumber = it })
        MeasureInputRow(label = "点号", value = pointNumber, onValueChange = { pointNumber = it })
        MeasureInputRow(label = "中心点对数", value = centerPointPairs, onValueChange = { centerPointPairs = it })

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("返回")
            }
            Button(onClick = { /* 测量逻辑 */ }, modifier = Modifier.weight(1f)) {
                Text("测量")
            }
            OutlinedTextField(
                value = measureResultInput,
                onValueChange = { measureResultInput = it },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}

@Composable
fun MeasureInputRow(label: String, value: String, onValueChange: (String) -> Unit, unit: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "$label:", modifier = Modifier.width(100.dp), fontSize = 16.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        if (unit != null) {
            Text(text = unit, modifier = Modifier.width(40.dp), fontSize = 16.sp)
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontSize = 16.sp
    )
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        fontSize = 14.sp
    )
}
