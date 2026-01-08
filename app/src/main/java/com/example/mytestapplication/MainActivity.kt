package com.example.mytestapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Project(val id: Int, val name: String, val description: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projects = listOf(
            Project(1, "项目A", "这是一个示例项目，用于演示 Compose 表格。请点击这一行来查看完整的描述信息。"),
            Project(2, "项目B", "这是另一个很棒的示例项目，展示动态渲染。它的描述也比较长，所以会被截断。"),
            Project(3, "项目C", "这是第三个项目，美化了 UI 并使用数据模型。这是一个非常长的描述，以确保它会被截断并需要点击才能看到全部内容。")
        )

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                ProjectScreen(projects = projects)
            }
        }
    }
}

// 跳转到新建页面
private fun navigateToCreateProject(context: Context) {
    context.startActivity(Intent(context, CreateProjectActivity::class.java))
}

// 跳转到编辑页面
private fun navigateToEditProject(context: Context, project: Project) {
    val intent = Intent(context, EditProjectActivity::class.java).apply {
        putExtra("PROJECT_ID", project.id)
        putExtra("PROJECT_NAME", project.name)
        putExtra("PROJECT_DESCRIPTION", project.description)
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

        // 表格占据剩余空间
        Column(modifier = Modifier.weight(1f)) {
            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCell(text = "序号", weight = 1f, isHeader = true)
                VerticalDivider()
                TableCell(text = "项目名", weight = 2f, isHeader = true)
                VerticalDivider()
                TableCell(text = "描述", weight = 3f, isHeader = true)
            }

            // 数据行
            projects.forEach { project ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray)
                        .height(48.dp)
                        .clickable { navigateToEditProject(context, project) }, // 整行点击跳转到编辑页
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = project.id.toString(), weight = 1f)
                    VerticalDivider()
                    TableCell(text = project.name, weight = 2f)
                    VerticalDivider()
                    // 描述单元格：点击弹框
                    TableCell(
                        text = project.description, 
                        weight = 3f, 
                        modifier = Modifier.clickable(onClick = { // 拦截点击事件，只做弹框
                            Toast.makeText(context, project.description, Toast.LENGTH_LONG).show()
                        })
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部创建按钮
        Button(
            onClick = { navigateToCreateProject(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("创建项目")
        }
    }
}

@Composable
private fun VerticalDivider() {
    Divider(color = Color.LightGray, modifier = Modifier.fillMaxHeight().width(1.dp))
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
    val sampleProjects = listOf(
        Project(1, "预览项目A", "这是一个预览用的描述..."),
        Project(2, "预览项目B", "这是另一个预览用的描述。")
    )
    Surface(color = MaterialTheme.colorScheme.background) {
        ProjectScreen(projects = sampleProjects)
    }
}
