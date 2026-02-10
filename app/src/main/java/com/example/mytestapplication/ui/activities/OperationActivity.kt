package com.example.mytestapplication.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.ui.theme.MytestApplicationTheme

class OperationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "未知项目"
        
        setContent {
            MytestApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OperationScreen(
                        projectName = projectName,
                        onBackToList = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun OperationScreen(projectName: String, onBackToList: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = projectName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { 
                    context.startActivity(Intent(context, ControlPointActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("控制点设置", fontSize = 18.sp)
            }

            Button(
                onClick = onBackToList,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("项目列表", fontSize = 18.sp)
            }

            Button(
                onClick = { 
                    context.startActivity(Intent(context, MeasureActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("高层放样", fontSize = 18.sp)
            }

            Button(
                onClick = { 
                    context.startActivity(Intent(context, DataExportActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("数据导出", fontSize = 18.sp)
            }
        }
    }
}
