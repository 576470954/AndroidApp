package com.example.mytestapplication.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.ControlPoint
import com.example.mytestapplication.ui.common.VerticalDivider
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
                    val scope = rememberCoroutineScope()
                    
                    var isDeleteMode by remember { mutableStateOf(false) }
                    val selectedIds = remember { mutableStateListOf<Long>() }

                    ControlPointScreen(
                        points = points,
                        selectedIds = selectedIds,
                        isDeleteMode = isDeleteMode,
                        onBack = { finish() },
                        onCreateNew = { navigateToCreateControlPoint(this) },
                        onEditPoint = { navigateToEditControlPoint(this, it) },
                        onImport = { navigateToImportControlPoint(this) },
                        onEnterDeleteMode = {
                            isDeleteMode = true
                            selectedIds.clear()
                        },
                        onCancelDelete = {
                            isDeleteMode = false
                            selectedIds.clear()
                        },
                        onConfirmDelete = {
                            if (selectedIds.isEmpty()) {
                                Toast.makeText(this, "请选择要删除的点", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    controlPointDao.deleteControlPointsByIds(selectedIds.toList())
                                    selectedIds.clear()
                                    isDeleteMode = false
                                }
                            }
                        },
                        onToggleSelect = { id ->
                            if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
                        }
                    )
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
            putExtra("CP_SERIAL", point.serialNumber)
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
    isDeleteMode: Boolean,
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onEditPoint: (ControlPoint) -> Unit,
    onImport: () -> Unit,
    onEnterDeleteMode: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
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
            val horizontalScrollState = rememberScrollState()
            Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                // Table Header
                Row(
                    modifier = Modifier
                        .width(750.dp)
                        .background(Color(0xFF2196F3))
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CPHeaderCell(if (isDeleteMode) "选择" else "序号", 70.dp)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CPHeaderCell("点名", 100.dp)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CPHeaderCell("描述", 120.dp)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CPHeaderCell("x", 120.dp)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CPHeaderCell("y", 120.dp)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CPHeaderCell("h", 100.dp)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CPHeaderCell("备注", 120.dp)
                }

                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(points, key = { it.id }) { point ->
                        Row(
                            modifier = Modifier
                                .width(750.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .height(48.dp)
                                .clickable { 
                                    if (!isDeleteMode) onEditPoint(point) 
                                    else onToggleSelect(point.id)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(70.dp), contentAlignment = Alignment.Center) {
                                if (isDeleteMode) {
                                    Checkbox(
                                        checked = selectedIds.contains(point.id),
                                        onCheckedChange = { onToggleSelect(point.id) }
                                    )
                                } else {
                                    Text(points.indexOf(point).plus(1).toString(), fontSize = 14.sp)
                                }
                            }
                            VerticalDivider(color = MaterialTheme.colorScheme.outline)
                            CPDataCell(point.name, 100.dp)
                            VerticalDivider(color = MaterialTheme.colorScheme.outline)
                            CPDataCell(point.description, 120.dp)
                            VerticalDivider(color = MaterialTheme.colorScheme.outline)
                            CPDataCell(point.x, 120.dp)
                            VerticalDivider(color = MaterialTheme.colorScheme.outline)
                            CPDataCell(point.y, 120.dp)
                            VerticalDivider(color = MaterialTheme.colorScheme.outline)
                            CPDataCell(point.h, 100.dp)
                            VerticalDivider(color = MaterialTheme.colorScheme.outline)
                            CPDataCell(point.remark, 120.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isDeleteMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onCreateNew, modifier = Modifier.weight(1f)) { Text("新建") }
                Button(onClick = onImport, modifier = Modifier.weight(1f)) { Text("导入") }
                Button(onClick = onEnterDeleteMode, modifier = Modifier.weight(1f)) { Text("删除") }
                Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("返回") }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onCancelDelete, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(onClick = onConfirmDelete, modifier = Modifier.weight(1f)) { Text("确认删除") }
            }
        }
    }
}

@Composable
fun CPHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        color = Color.White
    )
}

@Composable
fun CPDataCell(text: String, width: androidx.compose.ui.unit.Dp, maxLines: Int = 1) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontSize = 14.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}
