package com.pomodorotimer.app.ui.screens.tasks

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodorotimer.app.R
import com.pomodorotimer.app.data.db.AppDatabase
import com.pomodorotimer.app.data.db.TaskEntity
import com.pomodorotimer.app.data.repository.TaskRepository
import com.pomodorotimer.app.service.TaskReminderReceiver
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

        checkDailyReset()
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

    fun addTask(title: String, description: String, hasReminder: Boolean, reminderHour: Int, reminderMinute: Int) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = description,
                hasReminder = hasReminder,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            val taskId = repository.insertTask(task)
            if (hasReminder) {
                scheduleReminder(task.copy(id = taskId))
            }
            dismissDialog()
        }
    }

    fun updateTask(id: Long, title: String, description: String, hasReminder: Boolean, reminderHour: Int, reminderMinute: Int) {
        viewModelScope.launch {
            val existing = repository.getTaskById(id) ?: return@launch
            val updated = existing.copy(
                title = title,
                description = description,
                hasReminder = hasReminder,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            repository.updateTask(updated)
            cancelReminder(existing)
            if (hasReminder) {
                scheduleReminder(updated)
            }
            dismissDialog()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            cancelReminder(task)
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

    private fun checkDailyReset() {
        val prefs = getApplication<Application>()
            .getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)

        val lastResetDate = prefs.getLong("last_reset_date", 0L)
        val currentTime = System.currentTimeMillis()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today4AM = calendar.timeInMillis

        if (lastResetDate < today4AM && currentTime >= today4AM) {
            viewModelScope.launch {
                repository.resetAllTasks()
                prefs.edit().putLong("last_reset_date", currentTime).apply()
            }
        }
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

    private fun scheduleReminder(task: TaskEntity) {
        val context = getApplication<Application>()

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.reminderHour)
            set(Calendar.MINUTE, task.reminderMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )
    }

    private fun cancelReminder(task: TaskEntity) {
        val context = getApplication<Application>()
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        pendingIntent?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "task_reminder_channel"
    }
}
