package com.pomodorotimer.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pomodorotimer.app.ui.screens.timer.TimerScreen
import com.pomodorotimer.app.ui.screens.timer.TimerViewModel
import com.pomodorotimer.app.ui.screens.tasks.TasksScreen
import com.pomodorotimer.app.ui.screens.tasks.TasksViewModel

object Routes {
    const val TIMER = "timer"
    const val TASKS = "tasks"
}

@Composable
fun PomodoroNavGraph(
    navController: NavHostController,
    timerViewModel: TimerViewModel,
    tasksViewModel: TasksViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TIMER,
        enterTransition = {
            fadeIn(tween(600)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(600)
            )
        },
        exitTransition = {
            fadeOut(tween(400)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400)
            )
        },
        popEnterTransition = {
            fadeIn(tween(600)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(600)
            )
        },
        popExitTransition = {
            fadeOut(tween(400)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400)
            )
        }
    ) {
        composable(Routes.TIMER) {
            TimerScreen(
                viewModel = timerViewModel,
                onNavigateToTasks = {
                    navController.navigate(Routes.TASKS)
                }
            )
        }

        composable(Routes.TASKS) {
            TasksScreen(
                viewModel = tasksViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
