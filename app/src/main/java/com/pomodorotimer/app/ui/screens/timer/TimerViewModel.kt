package com.pomodorotimer.app.ui.screens.timer

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodorotimer.app.R
import com.pomodorotimer.app.service.TimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

enum class TimerPhase {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.FOCUS,
    val state: TimerState = TimerState.IDLE,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedPomodoros: Int = 0,
    val progress: Float = 1f
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    companion object {
        const val FOCUS_MINUTES = 25
        const val SHORT_BREAK_MINUTES = 5
        const val LONG_BREAK_MINUTES = 15
        const val POMODOROS_BEFORE_LONG_BREAK = 4
        const val CHANNEL_ID = "pomodoro_complete"
        const val COMPLETE_NOTIFICATION_ID = 1002
    }

    init {
        createNotificationChannel()
    }

    fun startTimer() {
        if (_uiState.value.state == TimerState.RUNNING) return

        if (_uiState.value.state == TimerState.IDLE || _uiState.value.state == TimerState.FINISHED) {
            resetTimerForPhase(_uiState.value.phase)
        }

        _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)
        startCountdown()
        updateForegroundService()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
        updateForegroundService()
    }

    fun resumeTimer() {
        _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)
        startCountdown()
        updateForegroundService()
    }

    private fun startCountdown() {
        val startTimeMillis = System.currentTimeMillis()
        val initialRemaining = _uiState.value.remainingSeconds

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val elapsed = ((System.currentTimeMillis() - startTimeMillis) / 1000L).toInt()
                val newRemaining = (initialRemaining - elapsed).coerceAtLeast(0)

                val current = _uiState.value
                _uiState.value = current.copy(
                    remainingSeconds = newRemaining,
                    progress = if (current.totalSeconds > 0) newRemaining.toFloat() / current.totalSeconds else 0f
                )

                if (newRemaining <= 0) {
                    _uiState.value = _uiState.value.copy(state = TimerState.FINISHED, progress = 0f)
                    onTimerComplete()
                    break
                }

                delay(500L)
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val phase = _uiState.value.phase
        resetTimerForPhase(phase)
        _uiState.value = _uiState.value.copy(state = TimerState.IDLE)
        updateForegroundService()
    }

    fun switchPhase(phase: TimerPhase) {
        timerJob?.cancel()
        resetTimerForPhase(phase)
        _uiState.value = _uiState.value.copy(
            phase = phase,
            state = TimerState.IDLE
        )
        updateForegroundService()
    }

    fun skipToNextPhase() {
        timerJob?.cancel()
        val currentPhase = _uiState.value.phase
        val completed = _uiState.value.completedPomodoros
        val nextPhase: TimerPhase
        val nextCompleted: Int

        when (currentPhase) {
            TimerPhase.FOCUS -> {
                nextCompleted = completed + 1
                nextPhase = if (nextCompleted % POMODOROS_BEFORE_LONG_BREAK == 0) {
                    TimerPhase.LONG_BREAK
                } else {
                    TimerPhase.SHORT_BREAK
                }
            }
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> {
                nextCompleted = completed
                nextPhase = TimerPhase.FOCUS
            }
        }

        resetTimerForPhase(nextPhase)
        _uiState.value = _uiState.value.copy(
            phase = nextPhase,
            state = TimerState.IDLE,
            completedPomodoros = nextCompleted
        )
        updateForegroundService()
    }

    private fun onTimerComplete() {
        triggerNotification()
        triggerStrongVibration()
        triggerSound()

        val currentPhase = _uiState.value.phase
        if (currentPhase == TimerPhase.FOCUS) {
            val completed = _uiState.value.completedPomodoros + 1
            val nextPhase = if (completed % POMODOROS_BEFORE_LONG_BREAK == 0) {
                TimerPhase.LONG_BREAK
            } else {
                TimerPhase.SHORT_BREAK
            }
            _uiState.value = _uiState.value.copy(
                completedPomodoros = completed,
                phase = nextPhase,
                state = TimerState.FINISHED
            )
        } else {
            _uiState.value = _uiState.value.copy(
                phase = TimerPhase.FOCUS,
                state = TimerState.FINISHED
            )
        }

        startTimer()
    }

    private fun resetTimerForPhase(phase: TimerPhase) {
        val seconds = when (phase) {
            TimerPhase.FOCUS -> FOCUS_MINUTES * 60
            TimerPhase.SHORT_BREAK -> SHORT_BREAK_MINUTES * 60
            TimerPhase.LONG_BREAK -> LONG_BREAK_MINUTES * 60
        }
        _uiState.value = _uiState.value.copy(
            remainingSeconds = seconds,
            totalSeconds = seconds,
            progress = 1f
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "番茄钟完成",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "番茄钟倒计时完成通知"
            enableVibration(true)
        }
        val manager = getApplication<Application>()
            .getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun triggerNotification() {
        val context = getApplication<Application>()
        val (title, message) = when (_uiState.value.phase) {
            TimerPhase.FOCUS -> "专注完成" to "太棒了！专注时间到，起来活动一下吧"
            TimerPhase.SHORT_BREAK -> "休息结束" to "休息完毕，准备开始下一轮专注"
            TimerPhase.LONG_BREAK -> "长休息结束" to "长休息结束，调整状态开始新的挑战吧"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    private fun triggerSound() {
        try {
            val context = getApplication<Application>()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        } catch (_: Exception) {
            // 静默失败 — 提示音非关键功能
        }
    }

    fun triggerClickHaptic() {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                vibrator?.vibrate(VibrationEffect.createOneShot(10, 255))
            }
        } catch (_: Exception) { }
    }

    private fun triggerStrongVibration() {
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let {
            if (it.hasVibrator()) {
                val timings = longArrayOf(0, 500, 300, 500, 300, 500)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        }
    }

    private fun updateForegroundService() {
        val context = getApplication<Application>()
        val state = _uiState.value
        val intent = Intent(context, TimerService::class.java)

        if (state.phase == TimerPhase.FOCUS &&
            state.state != TimerState.IDLE &&
            state.state != TimerState.FINISHED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) return
            }
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        getApplication<Application>().stopService(
            Intent(getApplication(), TimerService::class.java)
        )
        super.onCleared()
    }
}
