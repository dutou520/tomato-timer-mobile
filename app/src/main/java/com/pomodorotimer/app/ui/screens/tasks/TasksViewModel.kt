package com.pomodorotimer.app.ui.screens.tasks

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodorotimer.app.MainActivity
import com.pomodorotimer.app.R
import com.pomodorotimer.app.data.db.AppDatabase
import com.pomodorotimer.app.data.db.TaskEntity
import com.pomodorotimer.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingTask: TaskEntity? = null
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao())

        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks)
            }
        }

        createReminderChannel()
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingTask = null)
    }

    fun showEditDialog(task: TaskEntity) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, editingTask = task)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingTask = null)
    }

    fun addTask(title: String, description: String, hasReminder: Boolean, reminderMinutes: Int) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = description,
                hasReminder = hasReminder,
                reminderMinutes = reminderMinutes
            )
            repository.insertTask(task)
            dismissDialog()
        }
    }

    fun updateTask(id: Long, title: String, description: String, hasReminder: Boolean, reminderMinutes: Int) {
        viewModelScope.launch {
            val existing = repository.getTaskById(id) ?: return@launch
            val updated = existing.copy(
                title = title,
                description = description,
                hasReminder = hasReminder,
                reminderMinutes = reminderMinutes
            )
            repository.updateTask(updated)
            dismissDialog()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleCompletion(task.id, !task.isCompleted)
        }
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch {
            repository.deleteCompletedTasks()
        }
    }

    fun getPendingTaskCount(): Int {
        return _uiState.value.tasks.count { !it.isCompleted }
    }

    private fun createReminderChannel() {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "任务提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "待办事项提醒"
        }
        val manager = getApplication<Application>()
            .getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun scheduleReminder(task: TaskEntity) {
        if (!task.hasReminder) return

        val context = getApplication<Application>()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setContentTitle("任务提醒")
            .setContentText(task.title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(task.id.toInt() + 2000, notification)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "task_reminder_channel"
    }
}
