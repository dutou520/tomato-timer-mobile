package com.pomodorotimer.app.ui.screens.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pomodorotimer.app.ui.theme.LocalTimerColors

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onNavigateToTasks: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val timerColors = LocalTimerColors.current

    val phaseColor by animateColorAsState(
        targetValue = when (uiState.phase) {
            TimerPhase.FOCUS -> timerColors.activeColor
            TimerPhase.SHORT_BREAK -> timerColors.breakColor
            TimerPhase.LONG_BREAK -> timerColors.breakColor
        },
        animationSpec = tween(600),
        label = "phaseColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Phase selector pills
        PhaseSelector(
            currentPhase = uiState.phase,
            phaseColor = phaseColor,
            onPhaseSelected = {
                viewModel.triggerClickHaptic()
                viewModel.switchPhase(it)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Circular timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularTimer(
                progress = uiState.progress,
                phaseColor = phaseColor,
                trackColor = timerColors.progressTrackColor
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d".format(uiState.remainingSeconds / 60),
                        modifier = Modifier.weight(1f),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.W300,
                        color = timerColors.textColor,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                    Text(
                        text = ":",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.W300,
                        color = timerColors.textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "%02d".format(uiState.remainingSeconds % 60),
                        modifier = Modifier.weight(1f),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.W300,
                        color = timerColors.textColor,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }

                Text(
                    text = when (uiState.phase) {
                        TimerPhase.FOCUS -> "专注时间"
                        TimerPhase.SHORT_BREAK -> "短休息"
                        TimerPhase.LONG_BREAK -> "长休息"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Reset button
            AnimatedControlButton(
                icon = Icons.Default.Replay,
                contentDescription = "重置",
                enabled = uiState.state != TimerState.IDLE,
                onClick = {
                    viewModel.triggerClickHaptic()
                    viewModel.resetTimer()
                }
            )

            Spacer(modifier = Modifier.width(32.dp))

            // Main play/pause button
            val isPlaying = uiState.state == TimerState.RUNNING
            FilledIconButton(
                onClick = {
                    viewModel.triggerClickHaptic()
                    when (uiState.state) {
                        TimerState.IDLE, TimerState.FINISHED -> viewModel.startTimer()
                        TimerState.RUNNING -> viewModel.pauseTimer()
                        TimerState.PAUSED -> viewModel.resumeTimer()
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = phaseColor
                )
            ) {
                val icon = when (uiState.state) {
                    TimerState.RUNNING -> Icons.Default.Pause
                    TimerState.FINISHED -> Icons.Default.Replay
                    else -> Icons.Default.PlayArrow
                }
                Icon(
                    imageVector = icon,
                    contentDescription = if (isPlaying) "暂停" else "开始",
                    modifier = Modifier.size(36.dp),
                    tint = contentColorFor(phaseColor)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Skip button
            AnimatedControlButton(
                icon = Icons.Default.SkipNext,
                contentDescription = "跳过",
                enabled = uiState.state != TimerState.IDLE,
                onClick = {
                    viewModel.triggerClickHaptic()
                    viewModel.skipToNextPhase()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Completed pomodoros count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(4) { index ->
                val isFilled = index < uiState.completedPomodoros % 4
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isFilled) 12.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) phaseColor
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "已完成 ${uiState.completedPomodoros} 个番茄",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Task summary button
        androidx.compose.material3.OutlinedButton(
            onClick = onNavigateToTasks,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("查看今日任务")
        }
    }
}

@Composable
private fun PhaseSelector(
    currentPhase: TimerPhase,
    phaseColor: Color,
    onPhaseSelected: (TimerPhase) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TimerPhase.entries.forEach { phase ->
            val isSelected = phase == currentPhase
            val textColor by animateColorAsState(
                targetValue = if (isSelected) contentColorFor(phaseColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(300),
                label = "phaseTextColor"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) phaseColor else Color.Transparent,
                animationSpec = tween(300),
                label = "phaseBgColor"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (phase) {
                        TimerPhase.FOCUS -> "专注"
                        TimerPhase.SHORT_BREAK -> "短休"
                        TimerPhase.LONG_BREAK -> "长休"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun CircularTimer(
    progress: Float,
    phaseColor: Color,
    trackColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = size.width * 0.06f
        val radius = (size.width - strokeWidth) / 2
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

        // Track circle
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = phaseColor,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AnimatedControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.85f,
        animationSpec = tween(300),
        label = "controlBtnScale"
    )

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.4f
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
