# Task Pulse 🔔

A focused Android reminder app for reminders you cannot afford to miss.

## Tech Stack
- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM** + Clean Architecture
- **Hilt** (Dependency Injection)
- **Room** (Local database)
- **WorkManager** + **AlarmManager** (Exact alarms)
- **Coroutines + Flow**

## Features
- ✅ Create tasks with date, time, recurrence
- ✅ Full-screen overlay reminder at exact scheduled time
- ✅ Works on lock screen
- ✅ Snooze reminders
- ✅ Priority levels (Low / Medium / High / Critical)
- ✅ Categories with color tags
- ✅ Calendar view
- ✅ Statistics screen
- ✅ Auto-reschedule on device reboot
- ✅ Dark theme
- ✅ Smart scheduling with local parsing; Gemini can improve parsing when configured

## Setup

### 1. Clone & Open
```bash
git clone <your-repo>
```
Open in **Android Studio Ladybug** or newer.

### 2. Build
```bash
./gradlew assembleDebug
```

For release builds:
```bash
./gradlew assembleRelease
```

### 3. Reminder Reliability Permissions
For best reminder reliability, enable these from the app Settings screen:
1. **Post Notifications** (Android 13+)
2. **Display over other apps** for overlay reminders
3. **Alarms & reminders** for exact reminder timing
4. **Battery unrestricted** for better background reliability

### 4. Testing Overlay on Emulator
Overlay windows may not work correctly on all emulators.
**Test on a physical device** for the overlay reminder feature.

## Architecture

```
app/
├── data/          # Room DB, Repositories, DataStore
├── domain/        # Models, UseCases, Repository interfaces
├── presentation/  # Compose screens + ViewModels
├── overlay/       # OverlayService + OverlayScreen
└── worker/        # AlarmScheduler, BroadcastReceivers, WorkManager
```

## Key Files
| File | Purpose |
|------|---------|
| `OverlayService.kt` | Foreground service that shows full-screen reminder |
| `OverlayScreen.kt` | Compose UI for the overlay popup |
| `ExactAlarmScheduler.kt` | Schedules reminder alarms via AlarmManager |
| `TaskAlarmReceiver.kt` | BroadcastReceiver triggered by alarm |
| `BootReceiver.kt` | Re-schedules alarms after device reboot |

## Notes
- Min SDK: 26 (Android 8.0)
- Uses `AlarmManager.setAlarmClock` for high-priority reminder timing
- On Android 12+: requires exact alarm capability for on-time reminders
- Font: Plus Jakarta Sans (via Google Fonts)
