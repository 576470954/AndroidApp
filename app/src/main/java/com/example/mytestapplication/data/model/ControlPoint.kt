package com.example.mytestapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "control_points")
data class ControlPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val x: String,
    val y: String,
    val h: String,
    val remark: String,
    val createdAt: Long = System.currentTimeMillis()
)
