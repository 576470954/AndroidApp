package cn.latlaser.coordinate.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.latlaser.coordinate.data.database.AppDatabase
import cn.latlaser.coordinate.data.database.ProjectDao
import cn.latlaser.coordinate.data.model.Project
import cn.latlaser.coordinate.network.HttpServerService
import cn.latlaser.coordinate.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class ScreenMode { NORMAL, COPY, DELETE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val projectDao = database.projectDao()

        // 启动本地 HTTP 服务器前台服务
        val serviceIntent = Intent(this, HttpServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            MyApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val projects by projectDao.getAllProjects().collectAsState(initial = emptyList())
                    ProjectScreen(projects = projects, projectDao = projectDao)
                }
            }
        }
    }
}

private fun navigateToCreateProject(context: Context) {
    context.startActivity(Intent(context, CreateProjectActivity::class.java))
}

private fun navigateToEditProject(context: Context, project: Project) {
    val intent = Intent(context, EditProjectActivity::class.java).apply {
        putExtra("PROJECT_ID", project.id)
        putExtra("PROJECT_SERIAL", project.serialNumber)
        putExtra("PROJECT_NAME", project.name)
        putExtra("PROJECT_ADMIN", project.admin)
        putExtra("PROJECT_DESCRIPTION", project.description)
        putExtra("PROJECT_CREATED_AT", project.createdAt)
    }
    context.startActivity(intent)
}

private fun navigateToOperation(context: Context, project: Project) {
    val intent = Intent(context, OperationActivity::class.java).apply {
        putExtra("PROJECT_ID", project.id)
        putExtra("PROJECT_NAME", project.name)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(projects: List<Project>, projectDao: ProjectDao, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedProjectForMenu by remember { mutableStateOf<Project?>(null) }
    val scope = rememberCoroutineScope()

    var currentMode by remember { mutableStateOf(ScreenMode.NORMAL) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val expandedDescriptionIds = remember { mutableStateListOf<Long>() }
    
    val horizontalScrollState = rememberScrollState()

    if (selectedProjectForMenu != null) {
        AlertDialog(
            onDismissRequest = { selectedProjectForMenu = null },
            title = null,
            text = {
                Column {
                    TextButton(
                        onClick = {
                            val project = selectedProjectForMenu
                            selectedProjectForMenu = null
                            if (project != null) {
                                navigateToEditProject(context, project)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("编辑", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val project = selectedProjectForMenu
                            selectedProjectForMenu = null
                            if (project != null) {
                                scope.launch {
                                    copyProject(project, projectDao)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("复制", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val project = selectedProjectForMenu
                            selectedProjectForMenu = null
                            if (project != null) {
                                scope.launch {
                                    projectDao.deleteProject(project)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(), color = Color.Red)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProjectForMenu = null }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "项目列表",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                // Table Header
                Row(
                    modifier = Modifier
                        .width(800.dp)
                        .background(Color(0xFF2196F3))
                        .border(1.dp, Color.Gray)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell(text = if (currentMode == ScreenMode.NORMAL) "序号" else "选择", width = 80.dp)
                    VerticalDivider(color = Color.White) // 表头用白色线更清晰
                    TableHeaderCell(text = "名称", width = 150.dp)
                    VerticalDivider(color = Color.White)
                    TableHeaderCell(text = "管理员", width = 120.dp)
                    VerticalDivider(color = Color.White)
                    TableHeaderCell(text = "描述", width = 300.dp)
                    VerticalDivider(color = Color.White)
                    TableHeaderCell(text = "创建时间", width = 150.dp)
                }

                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(projects) { project ->
                        val isExpanded = expandedDescriptionIds.contains(project.id)
                        Row(
                            modifier = Modifier
                                .width(800.dp)
                                .height(IntrinsicSize.Min) // 关键：让高度自适应以使 VerticalDivider 填充
                                .border(1.dp, Color.Gray),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        onClick = {
                                            if (currentMode == ScreenMode.NORMAL) {
                                                navigateToOperation(context, project)
                                            } else {
                                                if (currentMode == ScreenMode.COPY) {
                                                    if (selectedIds.contains(project.id)) {
                                                        selectedIds.remove(project.id)
                                                    } else {
                                                        if (selectedIds.isEmpty()) {
                                                            selectedIds.add(project.id)
                                                        } else {
                                                            Toast.makeText(context, "复制项目仅支持单选", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else { // ScreenMode.DELETE
                                                    if (selectedIds.contains(project.id)) {
                                                        selectedIds.remove(project.id)
                                                    } else {
                                                        selectedIds.add(project.id)
                                                    }
                                                }
                                            }
                                        },
                                        onLongClick = { 
                                            if (currentMode == ScreenMode.NORMAL) {
                                                selectedProjectForMenu = project 
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentMode == ScreenMode.NORMAL) {
                                    Text(text = project.serialNumber, fontSize = 14.sp)
                                } else {
                                    Checkbox(
                                        checked = selectedIds.contains(project.id),
                                        onCheckedChange = { checked ->
                                            if (currentMode == ScreenMode.COPY) {
                                                if (checked) {
                                                    if (selectedIds.isEmpty()) {
                                                        selectedIds.add(project.id)
                                                    } else {
                                                        Toast.makeText(context, "复制项目仅支持单选", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    selectedIds.remove(project.id)
                                                }
                                            } else { // ScreenMode.DELETE
                                                if (checked) selectedIds.add(project.id)
                                                else selectedIds.remove(project.id)
                                            }
                                        }
                                    )
                                }
                            }
                            VerticalDivider(color = Color.Gray, modifier = Modifier.fillMaxHeight())
                            FixedTableCell(
                                text = project.name, 
                                width = 150.dp,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        if (currentMode == ScreenMode.NORMAL) {
                                            navigateToOperation(context, project)
                                        }
                                    },
                                    onLongClick = {
                                        if (currentMode == ScreenMode.NORMAL) {
                                            selectedProjectForMenu = project
                                        }
                                    }
                                )
                            )
                            VerticalDivider(color = Color.Gray, modifier = Modifier.fillMaxHeight())
                            FixedTableCell(text = project.admin, width = 120.dp)
                            VerticalDivider(color = Color.Gray, modifier = Modifier.fillMaxHeight())
                            FixedTableCell(
                                text = project.description,
                                width = 300.dp,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                modifier = Modifier.clickable {
                                    if (isExpanded) {
                                        expandedDescriptionIds.remove(project.id)
                                    } else {
                                        expandedDescriptionIds.add(project.id)
                                    }
                                }
                            )
                            VerticalDivider(color = Color.Gray, modifier = Modifier.fillMaxHeight())
                            FixedTableCell(text = sdf.format(Date(project.createdAt)), width = 150.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentMode == ScreenMode.NORMAL) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { navigateToCreateProject(context) }, 
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("新建项目", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { 
                        currentMode = ScreenMode.COPY
                        selectedIds.clear()
                    }, 
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("复制项目", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { 
                        currentMode = ScreenMode.DELETE
                        selectedIds.clear()
                    }, 
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("删除项目", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        currentMode = ScreenMode.NORMAL
                        selectedIds.clear()
                    }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("取消", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { 
                        if (selectedIds.isEmpty()) {
                            Toast.makeText(context, "请先选择项目", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            if (currentMode == ScreenMode.COPY) {
                                val projectToCopy = projects.find { it.id == selectedIds.first() }
                                if (projectToCopy != null) {
                                    copyProject(projectToCopy, projectDao)
                                }
                            } else if (currentMode == ScreenMode.DELETE) {
                                projectDao.deleteProjectsByIds(selectedIds.toList())
                            }
                            currentMode = ScreenMode.NORMAL
                            selectedIds.clear()
                        }
                    }, 
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("确定", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

private suspend fun copyProject(project: Project, projectDao: ProjectDao) {
    withContext(Dispatchers.IO) {
        val allSerials = projectDao.getAllSerialNumbers()
        val nextSerial = allSerials.mapNotNull { it.toIntOrNull() }.maxOrNull()?.plus(1)?.toString()
            ?: (allSerials.size + 1).toString()

        var newName = "${project.name}-副本"
        if (projectDao.isNameExists(newName)) {
            var counter = 2
            while (projectDao.isNameExists("${project.name}-副本$counter")) {
                counter++
            }
            newName = "${project.name}-副本$counter"
        }

        val newProject = project.copy(
            id = 0,
            serialNumber = nextSerial,
            name = newName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(newProject)
    }
}

@Composable
fun TableHeaderCell(text: String, width: Dp, textColor: Color = Color.White) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        color = textColor
    )
}

@Composable
fun FixedTableCell(
    text: String,
    width: Dp,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
