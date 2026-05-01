package com.pomodorotimer.app.data.repository

import com.pomodorotimer.app.data.db.TaskDao
import com.pomodorotimer.app.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun toggleCompletion(id: Long, isCompleted: Boolean) =
        taskDao.toggleCompletion(id, isCompleted)

    suspend fun deleteCompletedTasks() = taskDao.deleteCompletedTasks()

    suspend fun resetAllTasks() = taskDao.resetAllTasks()
}
