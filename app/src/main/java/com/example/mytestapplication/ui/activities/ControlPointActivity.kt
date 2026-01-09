package com.example.mytestapplication.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import kotlinx.coroutines.launch

class ControlPointActivity : ComponentActivity() {
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
                    val points by controlPointDao.getAllControlPoints().collectAsState(initial = emptyList())
                    val selectedIds = remember { mutableStateListOf<Long>() }
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    ControlPointScreen(
                        points = points,
                        selectedIds = selectedIds,
                        onBack = { finish() },
                        onCreateNew = { navigateToCreateControlPoint(this) },
                        onEditPoint = { navigateToEditControlPoint(this, it) },
                        onImport = { navigateToImportControlPoint(this) },
                        onDeleteRequest = {
                            if (selectedIds.isNotEmpty()) {
                                showDeleteConfirm = true
                            }
                        },
                        onToggleSelect = { id ->
                            if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
                        }
                    )

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("确认删除") },
                            text = { Text("确定要删除选中的 ${selectedIds.size} 个控制点吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    lifecycleScope.launch {
                                        controlPointDao.deleteControlPointsByIds(selectedIds.toList())
                                        selectedIds.clear()
                                        showDeleteConfirm = false
                                    }
                                }) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun navigateToCreateControlPoint(context: Context) {
        context.startActivity(Intent(context, CreateControlPointActivity::class.java))
    }

    private fun navigateToEditControlPoint(context: Context, point: ControlPoint) {
        val intent = Intent(context, EditControlPointActivity::class.java).apply {
            putExtra("CP_ID", point.id)
            putExtra("CP_NAME", point.name)
            putExtra("CP_DESC", point.description)
            putExtra("CP_X", point.x)
            putExtra("CP_Y", point.y)
            putExtra("CP_H", point.h)
            putExtra("CP_REMARK", point.remark)
        }
        context.startActivity(intent)
    }

    private fun navigateToImportControlPoint(context: Context) {
        val intent = Intent(context, ControlPointImportActivity::class.java).apply {
            putExtra("PROJECT_NAME", "当前项目")
        }
        context.startActivity(intent)
    }
}

@Composable
fun ControlPointScreen(
    points: List<ControlPoint>,
    selectedIds: List<Long>,
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onEditPoint: (ControlPoint) -> Unit,
    onImport: () -> Unit,
    onDeleteRequest: () -> Unit,
    onToggleSelect: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "控制点设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)) {
                    Box(modifier = Modifier.width(48.dp).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("选择", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    CPTableCell("序号", 60.dp, true)
                    CPTableCell("点名", 80.dp, true)
                    CPTableCell("描述", 100.dp, true)
                    CPTableCell("x", 100.dp, true)
                    CPTableCell("y", 100.dp, true)
                    CPTableCell("h", 80.dp, true)
                    CPTableCell("备注", 100.dp, true)
                }

                points.forEachIndexed { index, point ->
                    Row(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { onEditPoint(point) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .padding(4.dp)
                                .clickable(onClick = { /* 拦截点击 */ }) ,
                            contentAlignment = Alignment.Center
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(point.id),
                                onCheckedChange = { onToggleSelect(point.id) }
                            )
                        }
                        CPTableCell((index + 1).toString(), 60.dp)
                        CPTableCell(point.name, 80.dp)
                        CPTableCell(point.description, 100.dp)
                        CPTableCell(point.x, 100.dp)
                        CPTableCell(point.y, 100.dp)
                        CPTableCell(point.h, 80.dp)
                        CPTableCell(point.remark, 100.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onCreateNew, modifier = Modifier.weight(1f)) {
                Text("新建", fontSize = 12.sp)
            }
            Button(onClick = onImport, modifier = Modifier.weight(1f)) {
                Text("导入", fontSize = 12.sp)
            }
            Button(
                onClick = onDeleteRequest, 
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除", fontSize = 12.sp)
            }
            Button(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("返回", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CPTableCell(text: String, width: androidx.compose.ui.unit.Dp, isHeader: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
