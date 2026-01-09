package com.example.mytestapplication.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytestapplication.data.database.AppDatabase
import com.example.mytestapplication.data.model.Project
import com.example.mytestapplication.ui.theme.MytestApplicationTheme
import com.example.mytestapplication.ui.common.VerticalDivider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val projectDao = database.projectDao()

        setContent {
            MytestApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val projects by projectDao.getAllProjects().collectAsState(initial = emptyList())
                    ProjectScreen(projects = projects)
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
        putExtra("PROJECT_NAME", project.name)
    }
    context.startActivity(intent)
}

@Composable
fun ProjectScreen(projects: List<Project>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "项目列表",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "序号", weight = 1f, isHeader = true)
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(text = "名称", weight = 2f, isHeader = true)
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(text = "管理员", weight = 1.5f, isHeader = true)
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(text = "描述", weight = 3f, isHeader = true)
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                TableCell(text = "操作", weight = 2f, isHeader = true)
            }

            projects.forEach { project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(
                        text = project.serialNumber, 
                        weight = 1f, 
                        modifier = Modifier.clickable { navigateToOperation(context, project) }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    TableCell(
                        text = project.name, 
                        weight = 2f, 
                        modifier = Modifier.clickable { navigateToOperation(context, project) }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    TableCell(text = project.admin, weight = 1.5f)
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    TableCell(
                        text = project.description,
                        weight = 3f,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, project.description, Toast.LENGTH_LONG).show()
                        }
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { navigateToEditProject(context, project) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("编辑", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navigateToCreateProject(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("创建项目")
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .weight(weight)
            .padding(8.dp),
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (isHeader) 16.sp else 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Preview(showBackground = true)
@Composable
fun ProjectScreenPreview() {
    MytestApplicationTheme {
        val sampleProjects = listOf(
            Project(1, "001", "项目A", "管理员A", "这是一个预览描述..."),
            Project(2, "002", "项目B", "管理员B", "这是另一个预览描述。")
        )
        Surface(color = MaterialTheme.colorScheme.background) {
            ProjectScreen(projects = sampleProjects)
        }
    }
}
