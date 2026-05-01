package com.pomodorotimer.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.pomodorotimer.app.service.TimerService

class PomodoroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Timer foreground service channel
        val timerChannel = NotificationChannel(
            TimerService.CHANNEL_ID,
            "番茄钟专注",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "番茄钟正在后台专注中"
            setSound(null, null)
        }
        manager.createNotificationChannel(timerChannel)

        // Timer completion channel
        val completeChannel = NotificationChannel(
            "pomodoro_complete",
            "番茄钟完成",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "番茄钟倒计时完成通知"
            enableVibration(true)
        }
        manager.createNotificationChannel(completeChannel)

        // Task reminder channel
        val reminderChannel = NotificationChannel(
            "task_reminder_channel",
            "任务提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "待办事项提醒"
        }
        manager.createNotificationChannel(reminderChannel)
    }
}
