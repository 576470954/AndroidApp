package cn.latlaser.coordinate.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serialNumber: String,
    val name: String,
    val admin: String = "",
    val description: String,
    val deviceUrl: String = "http://192.168.1.149:8080",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
