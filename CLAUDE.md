# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build artifacts
./gradlew clean

# Run Room database schema export (requires ksp task)
./gradlew kspDebugKotlin

# Full clean rebuild
./gradlew clean assembleDebug
```

Note: The Gradle wrapper points to a local distribution at `D:/soft/gradle-8.5/gradle-8.5`. The Android SDK is at `E:\AndroidSDK`.

## Architecture Overview

This is a single-module Android app following a **simplified MVVM** pattern with **no dependency injection framework**.

### Layer Structure

- **`data/db/`** — Room database layer: `TaskEntity` (entity), `TaskDao` (DAO), `AppDatabase` (singleton via double-checked locking)
- **`data/repository/`** — `TaskRepository` wraps the DAO, exposes `Flow<List<TaskEntity>>`
- **`ui/screens/timer/`** — `TimerViewModel` + `TimerScreen`. Timer states: IDLE → RUNNING → PAUSED → FINISHED. Phases: FOCUS (25min), SHORT_BREAK (5min), LONG_BREAK (15min). Long break every 4 pomodoros.
- **`ui/screens/tasks/`** — `TasksViewModel` + `TasksScreen` + `AddTaskDialog`. Full CRUD with task reminders.
- **`ui/components/`** — `TaskItem` reusable composable
- **`ui/navigation/`** — `NavGraph.kt` with two routes (`"timer"` as start, `"tasks"`), animated transitions
- **`ui/theme/`** — Morandi Brown color palette (light scheme only, no dark mode), custom Typography
- **`service/`** — `TimerService` foreground service for background timer execution
- **`PomodoroApp.kt`** — Application class, creates 3 notification channels for foreground/alerts/reminders
- **`MainActivity.kt`** — Single activity, requests POST_NOTIFICATIONS on Android 13+, hosts NavHost

### Key Details

- **Navigation routes**: `"timer"` (start destination), `"tasks"` — defined in `NavGraph.kt` with fade + slide animations
- **Notification channels**: `pomodoro_timer_channel` (LOW), `pomodoro_complete` (HIGH), `task_reminder_channel` (DEFAULT)
- **Room database**: `"pomodoro_database"`, version 1, no schema export. Only one entity: `TaskEntity`
- **No tests exist** — no test directories or dependencies in the project
- **No CI/CD** — not a git repository, no workflow files
- **No DI** — ViewModels create dependencies manually via `Application` context
- **Permissions**: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `VIBRATE`
- **All UI strings are in Chinese** (app name "番茄钟")

### Dependencies

| Library | Version |
|---|---|
| Kotlin | 1.9.22 |
| AGP | 8.2.2 |
| Compose BOM | 2024.01.00 |
| Compose Compiler | 1.5.8 |
| Navigation Compose | 2.7.6 |
| Room | 2.6.1 |
| Lifecycle | 2.7.0 |
| Activity Compose | 1.8.2 |
| Coroutines | 1.7.3 |
| KSP | 1.9.22-1.0.17 |
| Gradle | 8.5 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
