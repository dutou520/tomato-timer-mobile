package com.pomodorotimer.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderHour: Int = 0,
    val reminderMinute: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null
)
